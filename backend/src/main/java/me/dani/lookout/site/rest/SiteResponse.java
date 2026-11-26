package me.dani.lookout.site.rest;

import me.dani.lookout.site.jpa.Site;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;


public record SiteResponse(UUID id,
                           String name,
                           Set<String> allowedOrigins,
                           String publicKey,
                           BigDecimal samplingRate,
                           Instant createdAt) {


    public static SiteResponse from(Site site){
        return new SiteResponse(site.getId(),site.getName(),Set.copyOf(site.getAllowedOrigins()),site.getPublicKey(),site.getSamplingRate(),site.getCreatedAt());
    }
}
