package me.dani.lookout.shared.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static me.dani.lookout.shared.utils.Normalizer.normalizeEmail;
import static me.dani.lookout.shared.utils.Normalizer.normalizeOrigin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizerTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(normalizeEmail("  Admin@Example.COM ")).isEqualTo("admin@example.com");
    }

    @ParameterizedTest
    @CsvSource({
            "https://blog.example.com,         https://blog.example.com",
            "HTTPS://Blog.Example.COM,         https://blog.example.com",
            "https://blog.example.com/,        https://blog.example.com",
            "'  https://blog.example.com  ',   https://blog.example.com",
            "https://blog.example.com:443,     https://blog.example.com",
            "http://blog.example.com:80,       http://blog.example.com",
            "https://blog.example.com:8443,    https://blog.example.com:8443",
            "http://localhost:3000,            http://localhost:3000",
            "http://127.0.0.1:5173/,           http://127.0.0.1:5173"
    })
    void originIsNormalizedToBrowserForm(String input, String expected) {
        assertThat(normalizeOrigin(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "blog.example.com",
            "ftp://blog.example.com",
            "javascript:alert(1)",
            "https://",
            "https://blog.example.com/configurator",
            "https://blog.example.com?x=1",
            "https://blog.example.com#top",
            "https://user:pass@blog.example.com",
            "https://blog example.com"
    })
    void invalidOriginsAreRejected(String input) {
        assertThatThrownBy(() -> normalizeOrigin(input)).isInstanceOf(InvalidOriginException.class);
    }
}
