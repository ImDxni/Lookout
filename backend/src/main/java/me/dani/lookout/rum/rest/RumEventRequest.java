package me.dani.lookout.rum.rest;

import tools.jackson.databind.JsonNode;

public record RumEventRequest(
        String eventId,
        String sessionId,
        String type,
        Double clientTs,
        Double sinceStartMs,
        String pagePath,
        String configuratorVersion,
        JsonNode data,
        JsonNode device) {
}
