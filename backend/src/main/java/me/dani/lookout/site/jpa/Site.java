package me.dani.lookout.site.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import me.dani.lookout.shared.utils.Normalizer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "site")
@Getter
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_origins", nullable = false, columnDefinition = "text[]")
    private Set<String> allowedOrigins = new HashSet<>();

    @Column(name = "public_key", nullable = false, unique = true, updatable = false)
    private String publicKey;

    @Column(name = "sampling_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal samplingRate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Site() {}

    public Site(String name, Set<String> allowedOrigins, BigDecimal samplingRate, String publicKey) {
        this.name = name.strip();
        this.allowedOrigins = normalizeOrigins(allowedOrigins);
        this.samplingRate = samplingRate;
        this.publicKey = publicKey;
    }

    public void update(String name, Set<String> allowedOrigins, BigDecimal samplingRate){
        this.name = name.strip();
        this.allowedOrigins = normalizeOrigins(allowedOrigins);
        this.samplingRate = samplingRate;
    }

    private static Set<String> normalizeOrigins(Set<String> origins) {
        return origins.stream()
                .map(Normalizer::normalizeOrigin)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}