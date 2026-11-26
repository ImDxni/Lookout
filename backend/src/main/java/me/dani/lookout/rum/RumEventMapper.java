package me.dani.lookout.rum;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.rum.rest.RumEventRequest;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RumEventMapper {

    private final JsonMapper jsonMapper;

    Optional<RumEvent> toEvent(JsonNode node, UUID siteId, int schemaVersion, Instant receivedAt) {
        return read(node).flatMap(request -> toEvent(request, siteId, schemaVersion, receivedAt));
    }

    private Optional<RumEvent> toEvent(RumEventRequest request, UUID siteId, int schemaVersion, Instant receivedAt) {
        UUID eventId = parseUuid(request.eventId());
        UUID sessionId = parseUuid(request.sessionId());
        RumEventType type = RumEventType.fromWireName(request.type()).orElse(null);
        if (eventId == null || sessionId == null || type == null) {
            return Optional.empty();
        }

        return RumEventSanitizer.data(type, dataOf(request))
                .map(data -> new RumEvent(
                        eventId,
                        receivedAt,
                        siteId,
                        sessionId,
                        type,
                        schemaVersion,
                        RumEventSanitizer.clientTime(request.clientTs(), receivedAt),
                        RumEventSanitizer.sinceStartMs(request.sinceStartMs()),
                        RumEventSanitizer.pagePath(request.pagePath()),
                        RumEventSanitizer.configuratorVersion(request.configuratorVersion()),
                        jsonMapper.writeValueAsString(data),
                        deviceJson(type, request.device())));
    }

    private Optional<RumEventRequest> read(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jsonMapper.treeToValue(node, RumEventRequest.class));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    private ObjectNode dataOf(RumEventRequest request) {
        return request.data() instanceof ObjectNode data ? data : jsonMapper.createObjectNode();
    }

    private String deviceJson(RumEventType type, JsonNode device) {
        if (type != RumEventType.START || !(device instanceof ObjectNode)) {
            return null;
        }
        return jsonMapper.writeValueAsString(device);
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
