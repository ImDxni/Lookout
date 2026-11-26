package me.dani.lookout.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lookout")
public record LookoutProperties(@NotNull @Valid Admin admin, @NotNull @Valid Rum rum) {

    public record Admin(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password) {}

    public record Rum(
            @Min(1) @Max(500) int maxEventsPerBatch,
            @Min(1024) @Max(1_048_576) int maxBodyBytes,
            @Min(1) @Max(24) int partitionsAhead,
            @Min(1) int retentionDays,
            @Min(1) int dedupWindowHours) {}
}
