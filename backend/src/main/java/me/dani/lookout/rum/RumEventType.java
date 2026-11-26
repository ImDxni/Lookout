package me.dani.lookout.rum;

import java.util.Arrays;
import java.util.Optional;

public enum RumEventType {
    START("start"),
    ASSET_LOADED("asset_loaded"),
    READY("ready"),
    ERROR("error"),
    CONTEXT_LOST("context_lost"),
    ABANDON("abandon"),
    CRASH_DETECTED("crash_detected");

    private final String wireName;

    RumEventType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<RumEventType> fromWireName(String value) {
        return Arrays.stream(values())
                .filter(type -> type.wireName.equals(value))
                .findFirst();
    }
}
