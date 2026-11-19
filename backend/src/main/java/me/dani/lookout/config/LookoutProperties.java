package me.dani.lookout.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lookout")
public record LookoutProperties(@NotNull @Valid Admin admin) {



    public record Admin(@NotBlank @Email String email, @NotBlank @Size(min=8) String password){}
}
