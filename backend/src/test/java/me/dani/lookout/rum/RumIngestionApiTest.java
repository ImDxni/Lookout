package me.dani.lookout.rum;

import me.dani.lookout.ApiClient;
import me.dani.lookout.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.dani.lookout.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

class RumIngestionApiTest extends ApiIntegrationTest {

    private static final String ORIGIN = "https://shop.example.com";
    private static final String TEXT_PLAIN = "text/plain;charset=UTF-8";

    @Autowired
    private JdbcClient jdbc;

    private UUID siteId;
    private String siteKey;
    private ApiClient browser;

    @BeforeEach
    void createSite() {
        HttpResponse<String> created = loggedInClient().post("/api/sites", """
                {"name": "Shop %s", "allowedOrigins": ["%s"], "samplingRate": 1.0}
                """.formatted(UUID.randomUUID(), ORIGIN));
        siteId = UUID.fromString(json(created, "$.id"));
        siteKey = json(created, "$.publicKey");
        browser = anonymousClient();
    }

    @Test
    void validBatchIs202AndIsStored() {
        HttpResponse<String> response = send(batch(siteKey, startEvent(), readyEvent(1234.6)));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(countEvents()).isEqualTo(2);
    }

    @Test
    void endpointIsStatelessAndNeedsNoCsrf() {
        HttpResponse<String> response = send(batch(siteKey, startEvent()));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    void applicationJsonIsAlsoAccepted() {
        HttpResponse<String> response = browser.postRaw("/rum/events", batch(siteKey, startEvent()),
                "application/json", Map.of("Origin", ORIGIN));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void resendingTheSameBatchDoesNotDuplicateEvents() {
        String body = batch(siteKey, startEvent(), readyEvent(500));

        send(body);
        send(body);

        assertThat(countEvents()).isEqualTo(2);
    }

    @Test
    void sameEventTwiceInOneBatchIsStoredOnce() {
        String start = startEvent();

        send(batch(siteKey, start, start));

        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void invalidEventsAreDroppedAndValidOnesStored() {
        String unknownType = event("teleported", "{}");
        String badEventId = """
                {"eventId": "not-a-uuid", "sessionId": "%s", "type": "start"}
                """.formatted(UUID.randomUUID());
        String readyWithoutTime = event("ready", "{}");
        String errorWithoutMessage = event("error", "{\"kind\": \"TypeError\"}");
        String wrongFieldType = """
                {"eventId": "%s", "sessionId": "%s", "type": "start", "sinceStartMs": "soon"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        HttpResponse<String> response = send(batch(siteKey,
                unknownType, badEventId, readyWithoutTime, errorWithoutMessage, wrongFieldType, startEvent()));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void unknownSiteKeyIs403() {
        assertThat(send(batch("pk_unknown", startEvent())).statusCode()).isEqualTo(403);
    }

    @Test
    void originNotRegisteredIs403() {
        HttpResponse<String> response = browser.postRaw("/rum/events", batch(siteKey, startEvent()),
                TEXT_PLAIN, Map.of("Origin", "https://evil.example.com"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(countEvents()).isZero();
    }

    @Test
    void missingOriginIs403() {
        HttpResponse<String> response = browser.postRaw("/rum/events", batch(siteKey, startEvent()), TEXT_PLAIN, Map.of());

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void originIsComparedInNormalizedForm() {
        HttpResponse<String> response = browser.postRaw("/rum/events", batch(siteKey, startEvent()),
                TEXT_PLAIN, Map.of("Origin", "HTTPS://Shop.Example.com:443"));

        assertThat(response.statusCode()).isEqualTo(202);
    }

    @Test
    void malformedJsonIs400() {
        assertThat(send("{\"siteKey\": ").statusCode()).isEqualTo(400);
    }

    @Test
    void emptyBodyIs400() {
        assertThat(send("").statusCode()).isEqualTo(400);
    }

    @Test
    void batchWithoutEventsIs400() {
        assertThat(send("""
                {"siteKey": "%s", "schemaVersion": 1, "events": []}
                """.formatted(siteKey)).statusCode()).isEqualTo(400);
    }

    @Test
    void missingSchemaVersionIs400() {
        assertThat(send("""
                {"siteKey": "%s", "events": [%s]}
                """.formatted(siteKey, startEvent())).statusCode()).isEqualTo(400);
    }

    @Test
    void tooManyEventsIs413() {
        String[] events = IntStream.range(0, 51).mapToObj(i -> startEvent()).toArray(String[]::new);

        assertThat(send(batch(siteKey, events)).statusCode()).isEqualTo(413);
        assertThat(countEvents()).isZero();
    }

    @Test
    void bodyTooLargeIs413() {
        String hugeMessage = "x".repeat(40_000);

        assertThat(send(batch(siteKey, event("error", "{\"message\": \"%s\"}".formatted(hugeMessage)))).statusCode())
                .isEqualTo(413);
    }

    @Test
    void fieldsAreSanitizedBeforeStorage() {
        UUID eventId = UUID.randomUUID();
        String longMessage = "m".repeat(5000);
        String error = """
                {"eventId": "%s", "sessionId": "%s", "type": "error",
                 "clientTs": 1000, "sinceStartMs": 42.7,
                 "pagePath": "/configurator?email=someone@example.com#step2",
                 "data": {"message": "%s", "stack": 12},
                 "device": {"deviceMemory": 4}}
                """.formatted(eventId, UUID.randomUUID(), longMessage);

        send(batch(siteKey, error));

        Map<String, Object> row = jdbc.sql("""
                        SELECT page_path, client_ts, since_start_ms, device,
                               length(payload->>'message') AS message_length, (payload->'stack') IS NOT NULL AS has_stack
                        FROM rum_event WHERE event_id = :id
                        """)
                .param("id", eventId)
                .query()
                .singleRow();

        assertThat(row.get("page_path")).isEqualTo("/configurator");
        assertThat(row.get("client_ts")).isNull();
        assertThat(row.get("since_start_ms")).isEqualTo(43);
        assertThat(row.get("device")).isNull();
        assertThat(row.get("message_length")).isEqualTo(1000);
        assertThat(row.get("has_stack")).isEqualTo(false);
    }

    @Test
    void startEventKeepsDeviceAndPlausibleClientTime() {
        UUID eventId = UUID.randomUUID();
        long now = Instant.now().toEpochMilli();
        String start = """
                {"eventId": "%s", "sessionId": "%s", "type": "start", "clientTs": %d,
                 "device": {"deviceMemory": 4, "webglVersion": 2}}
                """.formatted(eventId, UUID.randomUUID(), now);

        send(batch(siteKey, start));

        Map<String, Object> row = jdbc.sql("SELECT client_ts, device->>'deviceMemory' AS memory FROM rum_event WHERE event_id = :id")
                .param("id", eventId)
                .query()
                .singleRow();

        assertThat(row.get("client_ts")).isNotNull();
        assertThat(row.get("memory")).isEqualTo("4");
    }

    @Test
    void adminApiIsStillProtected() {
        assertThat(browser.get("/api/sites").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> send(String body) {
        return browser.postRaw("/rum/events", body, TEXT_PLAIN, Map.of("Origin", ORIGIN));
    }

    private long countEvents() {
        return jdbc.sql("SELECT count(*) FROM rum_event WHERE site_id = :siteId")
                .param("siteId", siteId)
                .query(Long.class)
                .single();
    }

    private static String batch(String siteKey, String... events) {
        return """
                {"siteKey": "%s", "schemaVersion": 1, "events": [%s]}
                """.formatted(siteKey, List.of(events).stream().collect(Collectors.joining(",")));
    }

    private static String startEvent() {
        return event("start", "{}");
    }

    private static String readyEvent(double timeToReadyMs) {
        return event("ready", "{\"timeToReadyMs\": %s}".formatted(timeToReadyMs));
    }

    private static String event(String type, String data) {
        return """
                {"eventId": "%s", "sessionId": "%s", "type": "%s", "sinceStartMs": 10, "pagePath": "/configurator", "data": %s}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), type, data);
    }
}
