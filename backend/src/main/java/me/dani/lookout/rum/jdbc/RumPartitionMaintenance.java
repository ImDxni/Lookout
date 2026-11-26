package me.dani.lookout.rum.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dani.lookout.config.LookoutProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RumPartitionMaintenance {

    private static final String PARENT_TABLE = "rum_event";
    private static final Pattern PARTITION_NAME = Pattern.compile("^rum_event_(\\d{4})_(\\d{2})$");
    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");

    private final JdbcClient jdbc;
    private final LookoutProperties properties;
    private final Clock clock;

    public record Result(List<String> created, List<String> dropped, int purgedSeenIds) {}

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        runSafely();
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    public void nightly() {
        runSafely();
    }

    public Result run() {
        List<String> existing = existingPartitions();
        List<String> created = createUpcomingPartitions(existing);
        List<String> dropped = dropExpiredPartitions(existing);
        int purged = purgeSeenIds();
        if (!created.isEmpty() || !dropped.isEmpty() || purged > 0) {
            log.info("RUM maintenance: partitions created={} dropped={}, seen ids purged={}", created, dropped, purged);
        }
        return new Result(created, dropped, purged);
    }

    private int purgeSeenIds() {
        Instant cutoff = clock.instant().minus(Duration.ofHours(properties.rum().dedupWindowHours()));
        return jdbc.sql("DELETE FROM rum_event_seen WHERE seen_at < :cutoff")
                .param("cutoff", cutoff.atOffset(ZoneOffset.UTC))
                .update();
    }

    private void runSafely() {
        try {
            run();
        } catch (RuntimeException e) {
            log.error("RUM partition maintenance failed", e);
        }
    }

    private List<String> createUpcomingPartitions(List<String> existing) {
        YearMonth current = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC));
        List<String> created = new ArrayList<>();

        for (int i = 0; i <= properties.rum().partitionsAhead(); i++) {
            YearMonth month = current.plusMonths(i);
            String name = partitionName(month);
            if (existing.contains(name)) {
                continue;
            }
            jdbc.sql("CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s')"
                            .formatted(name, PARENT_TABLE, monthStart(month), monthStart(month.plusMonths(1))))
                    .update();
            created.add(name);
        }
        return created;
    }

    private List<String> dropExpiredPartitions(List<String> existing) {
        Instant cutoff = clock.instant().minus(Duration.ofDays(properties.rum().retentionDays()));
        List<String> dropped = new ArrayList<>();

        for (String name : existing) {
            Optional<YearMonth> month = monthOf(name);
            if (month.isEmpty()) {
                continue;
            }
            Instant upperBound = monthStart(month.get().plusMonths(1));
            if (!upperBound.isAfter(cutoff)) {
                jdbc.sql("DROP TABLE IF EXISTS %s".formatted(name)).update();
                dropped.add(name);
            }
        }
        return dropped;
    }

    private List<String> existingPartitions() {
        return jdbc.sql("""
                        SELECT child.relname
                        FROM pg_inherits i
                        JOIN pg_class child ON child.oid = i.inhrelid
                        JOIN pg_class parent ON parent.oid = i.inhparent
                        WHERE parent.relname = :parent
                        """)
                .param("parent", PARENT_TABLE)
                .query(String.class)
                .list();
    }

    static String partitionName(YearMonth month) {
        return PARENT_TABLE + "_" + month.format(SUFFIX);
    }

    private static Optional<YearMonth> monthOf(String partitionName) {
        Matcher matcher = PARTITION_NAME.matcher(partitionName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
    }

    private static Instant monthStart(YearMonth month) {
        return month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
