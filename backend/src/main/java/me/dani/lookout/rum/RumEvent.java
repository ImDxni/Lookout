package me.dani.lookout.rum;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RumEvent(
        UUID eventId,
        Instant receivedAt,
        UUID siteId,
        UUID sessionId,
        RumEventType type,
        int schemaVersion,
        Instant clientTs,
        Integer sinceStartMs,
        String pagePath,
        String configuratorVersion,
        String payloadJson,
        String deviceJson) {

    public RumEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(type, "type");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        if (sinceStartMs != null && sinceStartMs < 0) {
            throw new IllegalArgumentException("sinceStartMs must be >= 0");
        }
        if (payloadJson == null) {
            payloadJson = "{}";
        }
    }
}
