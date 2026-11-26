package me.dani.lookout.site;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record SiteSnapshot(UUID id, Set<String> allowedOrigins, BigDecimal samplingRate) {

    public SiteSnapshot {
        allowedOrigins = Set.copyOf(allowedOrigins);
    }

    public boolean allowsOrigin(String normalizedOrigin) {
        return allowedOrigins.contains(normalizedOrigin);
    }
}
