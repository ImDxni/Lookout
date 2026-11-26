package me.dani.lookout.rum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dani.lookout.config.LookoutProperties;
import me.dani.lookout.rum.exceptions.RumBatchRejectedException;
import me.dani.lookout.rum.jdbc.RumEventWriter;
import me.dani.lookout.rum.rest.RumBatchRequest;
import me.dani.lookout.shared.utils.InvalidOriginException;
import me.dani.lookout.shared.utils.Normalizer;
import me.dani.lookout.site.SiteLookup;
import me.dani.lookout.site.SiteSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RumIngestService {

    private static final String UNKNOWN_SITE_OR_ORIGIN = "Unknown site or origin not allowed";

    private final SiteLookup siteLookup;
    private final RumEventMapper mapper;
    private final RumEventWriter writer;
    private final JsonMapper jsonMapper;
    private final LookoutProperties properties;
    private final Clock clock;

    public IngestResult ingest(String body, String originHeader) {
        LookoutProperties.Rum limits = properties.rum();

        if (body == null || body.isBlank()) {
            throw new RumBatchRejectedException(HttpStatus.BAD_REQUEST, "Empty body");
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > limits.maxBodyBytes()) {
            throw new RumBatchRejectedException(HttpStatus.CONTENT_TOO_LARGE, "Body too large");
        }

        RumBatchRequest batch = parse(body);
        if (batch.events() == null || batch.events().isEmpty()) {
            throw new RumBatchRejectedException(HttpStatus.BAD_REQUEST, "No events");
        }
        if (batch.events().size() > limits.maxEventsPerBatch()) {
            throw new RumBatchRejectedException(HttpStatus.CONTENT_TOO_LARGE, "Too many events");
        }
        if (batch.schemaVersion() == null || batch.schemaVersion() < 1) {
            throw new RumBatchRejectedException(HttpStatus.BAD_REQUEST, "Missing or invalid schemaVersion");
        }

        SiteSnapshot site = siteLookup.findByPublicKey(batch.siteKey())
                .filter(found -> originAllowed(found, originHeader))
                .orElseThrow(() -> new RumBatchRejectedException(HttpStatus.FORBIDDEN, UNKNOWN_SITE_OR_ORIGIN));

        Instant receivedAt = clock.instant();
        List<RumEvent> events = batch.events().stream()
                .map(node -> mapper.toEvent(node, site.id(), batch.schemaVersion(), receivedAt))
                .flatMap(Optional::stream)
                .toList();

        int stored = events.isEmpty() ? 0 : writer.write(events);
        IngestResult result = new IngestResult(batch.events().size(), events.size(), stored);

        log.debug("RUM batch for site {}: received={}, dropped={}, duplicates={}, stored={}",
                site.id(), result.received(), result.dropped(), result.duplicates(), result.stored());
        return result;
    }

    private RumBatchRequest parse(String body) {
        try {
            RumBatchRequest batch = jsonMapper.readValue(body, RumBatchRequest.class);
            if (batch == null) {
                throw new RumBatchRejectedException(HttpStatus.BAD_REQUEST, "Malformed JSON");
            }
            return batch;
        } catch (JacksonException e) {
            throw new RumBatchRejectedException(HttpStatus.BAD_REQUEST, "Malformed JSON");
        }
    }

    private static boolean originAllowed(SiteSnapshot site, String originHeader) {
        if (originHeader == null || originHeader.isBlank()) {
            return false;
        }
        try {
            return site.allowsOrigin(Normalizer.normalizeOrigin(originHeader));
        } catch (InvalidOriginException e) {
            return false;
        }
    }
}
