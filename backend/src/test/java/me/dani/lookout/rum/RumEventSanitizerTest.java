package me.dani.lookout.rum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RumEventSanitizerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant RECEIVED_AT = Instant.parse("2026-11-26T12:00:00Z");

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            START          | {}
            CONTEXT_LOST   | {}
            ABANDON        | {}
            CRASH_DETECTED | {}
            READY          | {"timeToReadyMs": 0}
            READY          | {"timeToReadyMs": 1234.5}
            ERROR          | {"message": "boom"}
            ASSET_LOADED   | {"assetPath": "/models/chair.glb", "durationMs": 80}
            """)
    void acceptsEventsWithRequiredFields(RumEventType type, String data) {
        assertThat(RumEventSanitizer.data(type, object(data))).isPresent();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            READY        | {}
            READY        | {"timeToReadyMs": -1}
            READY        | {"timeToReadyMs": "fast"}
            ERROR        | {}
            ERROR        | {"message": "   "}
            ERROR        | {"message": 42}
            ASSET_LOADED | {"durationMs": 80}
            ASSET_LOADED | {"assetPath": "/models/chair.glb"}
            """)
    void rejectsEventsMissingRequiredFields(RumEventType type, String data) {
        assertThat(RumEventSanitizer.data(type, object(data))).isEmpty();
    }

    @Test
    void longTextIsTruncatedAndNonTextIsRemoved() {
        ObjectNode data = object("""
                {"message": "%s", "stack": 12, "kind": "%s"}
                """.formatted("m".repeat(5000), "k".repeat(500)));

        ObjectNode clean = RumEventSanitizer.data(RumEventType.ERROR, data).orElseThrow();

        assertThat(clean.get("message").stringValue()).hasSize(1000);
        assertThat(clean.get("kind").stringValue()).hasSize(128);
        assertThat(clean.has("stack")).isFalse();
    }

    @Test
    void originalDataIsNotModified() {
        ObjectNode data = object("{\"message\": \"%s\"}".formatted("m".repeat(5000)));

        RumEventSanitizer.data(RumEventType.ERROR, data);

        assertThat(data.get("message").stringValue()).hasSize(5000);
    }

    @Test
    void assetPathLosesQueryAndFragment() {
        ObjectNode data = object("""
                {"assetPath": "/models/chair.glb?token=secret#v2", "durationMs": 80}
                """);

        ObjectNode clean = RumEventSanitizer.data(RumEventType.ASSET_LOADED, data).orElseThrow();

        assertThat(clean.get("assetPath").stringValue()).isEqualTo("/models/chair.glb");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "/configurator,                          /configurator",
            "/configurator?email=a@example.com,      /configurator",
            "/configurator#step-2,                   /configurator",
            "/configurator#step?x=1,                 /configurator",
            "NULL,                                   NULL"
    })
    void pagePathKeepsOnlyThePath(String input, String expected) {
        assertThat(RumEventSanitizer.pagePath(input)).isEqualTo(expected);
    }

    @Test
    void pagePathAndVersionAreTruncated() {
        assertThat(RumEventSanitizer.pagePath("/" + "p".repeat(3000))).hasSize(2048);
        assertThat(RumEventSanitizer.configuratorVersion("v".repeat(100))).hasSize(64);
    }

    @Test
    void plausibleClientTimeIsKept() {
        Instant oneHourAgo = RECEIVED_AT.minus(Duration.ofHours(1));

        assertThat(RumEventSanitizer.clientTime((double) oneHourAgo.toEpochMilli(), RECEIVED_AT)).isEqualTo(oneHourAgo);
    }

    @Test
    void implausibleClientTimeIsDropped() {
        double eightDaysAgo = RECEIVED_AT.minus(Duration.ofDays(8)).toEpochMilli();
        double twoDaysAhead = RECEIVED_AT.plus(Duration.ofDays(2)).toEpochMilli();

        assertThat(RumEventSanitizer.clientTime(eightDaysAgo, RECEIVED_AT)).isNull();
        assertThat(RumEventSanitizer.clientTime(twoDaysAhead, RECEIVED_AT)).isNull();
        assertThat(RumEventSanitizer.clientTime(Double.NaN, RECEIVED_AT)).isNull();
        assertThat(RumEventSanitizer.clientTime(null, RECEIVED_AT)).isNull();
    }

    @Test
    void sinceStartIsRoundedAndNeverNegative() {
        assertThat(RumEventSanitizer.sinceStartMs(42.6)).isEqualTo(43);
        assertThat(RumEventSanitizer.sinceStartMs(-1.0)).isNull();
        assertThat(RumEventSanitizer.sinceStartMs(1e12)).isEqualTo(Integer.MAX_VALUE);
        assertThat(RumEventSanitizer.sinceStartMs(null)).isNull();
    }

    private static ObjectNode object(String json) {
        return Optional.of(JSON.readTree(json))
                .filter(ObjectNode.class::isInstance)
                .map(ObjectNode.class::cast)
                .orElseThrow();
    }
}
