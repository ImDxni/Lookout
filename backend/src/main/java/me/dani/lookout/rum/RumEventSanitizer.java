package me.dani.lookout.rum;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

final class RumEventSanitizer {

    private static final int MAX_PATH_LENGTH = 2048;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final int MAX_KIND_LENGTH = 128;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_STACK_LENGTH = 4000;

    private static final Duration MAX_CLIENT_CLOCK_PAST = Duration.ofDays(7);
    private static final Duration MAX_CLIENT_CLOCK_FUTURE = Duration.ofDays(1);

    private RumEventSanitizer() {}

    static Optional<ObjectNode> data(RumEventType type, ObjectNode data) {
        if (!hasRequiredFields(type, data)) {
            return Optional.empty();
        }
        ObjectNode clean = data.deepCopy();
        limitText(clean, "message", MAX_MESSAGE_LENGTH);
        limitText(clean, "stack", MAX_STACK_LENGTH);
        limitText(clean, "kind", MAX_KIND_LENGTH);
        cleanPathField(clean, "assetPath");
        return Optional.of(clean);
    }

    static String pagePath(String path) {
        return truncate(withoutQueryAndFragment(path), MAX_PATH_LENGTH);
    }

    static String configuratorVersion(String version) {
        return truncate(version, MAX_VERSION_LENGTH);
    }

    static Instant clientTime(Double epochMillis, Instant receivedAt) {
        if (epochMillis == null || epochMillis.isNaN() || epochMillis.isInfinite()) {
            return null;
        }
        Instant clientTime = Instant.ofEpochMilli(Math.round(epochMillis));
        boolean tooOld = clientTime.isBefore(receivedAt.minus(MAX_CLIENT_CLOCK_PAST));
        boolean tooFarAhead = clientTime.isAfter(receivedAt.plus(MAX_CLIENT_CLOCK_FUTURE));
        return tooOld || tooFarAhead ? null : clientTime;
    }

    static Integer sinceStartMs(Double millis) {
        if (millis == null || millis.isNaN() || millis < 0) {
            return null;
        }
        return (int) Math.min(Math.round(millis), Integer.MAX_VALUE);
    }

    private static boolean hasRequiredFields(RumEventType type, ObjectNode data) {
        return switch (type) {
            case ASSET_LOADED -> isNonNegativeNumber(data.get("durationMs")) && isNonBlankText(data.get("assetPath"));
            case READY -> isNonNegativeNumber(data.get("timeToReadyMs"));
            case ERROR -> isNonBlankText(data.get("message"));
            case START, CONTEXT_LOST, ABANDON, CRASH_DETECTED -> true;
        };
    }

    private static boolean isNonNegativeNumber(JsonNode value) {
        return value != null && value.isNumber() && value.asDouble() >= 0;
    }

    private static boolean isNonBlankText(JsonNode value) {
        return value != null && value.isString() && !value.stringValue().isBlank();
    }

    private static void limitText(ObjectNode data, String field, int maxLength) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isString()) {
            data.put(field, truncate(value.stringValue(), maxLength));
        } else {
            data.remove(field);
        }
    }

    private static void cleanPathField(ObjectNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isString()) {
            data.put(field, pagePath(value.stringValue()));
        } else {
            data.remove(field);
        }
    }

    private static String withoutQueryAndFragment(String path) {
        if (path == null) {
            return null;
        }
        int end = path.length();
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        if (query >= 0) {
            end = Math.min(end, query);
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        return path.substring(0, end);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
