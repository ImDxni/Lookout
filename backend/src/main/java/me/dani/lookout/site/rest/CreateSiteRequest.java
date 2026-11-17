package me.dani.lookout.site.rest;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record CreateSiteRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Set<@NotBlank String> allowedOrigins,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal samplingRate) {}

