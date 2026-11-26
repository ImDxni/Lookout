package me.dani.lookout.rum.rest;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record RumBatchRequest(String siteKey, Integer schemaVersion, List<JsonNode> events) {
}
