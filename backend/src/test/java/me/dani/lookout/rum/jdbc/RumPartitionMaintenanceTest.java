package me.dani.lookout.rum.jdbc;

import me.dani.lookout.ApiIntegrationTest;
import me.dani.lookout.config.LookoutProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RumPartitionMaintenanceTest extends ApiIntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private LookoutProperties properties;

    @Test
    void createsPartitionsAheadAndIsIdempotent() {
        YearMonth future = YearMonth.now(ZoneOffset.UTC).plusYears(2);
        RumPartitionMaintenance maintenance = at(future);

        RumPartitionMaintenance.Result first = maintenance.run();
        RumPartitionMaintenance.Result second = maintenance.run();

        List<String> expected = IntStream.rangeClosed(0, properties.rum().partitionsAhead())
                .mapToObj(i -> RumPartitionMaintenance.partitionName(future.plusMonths(i)))
                .toList();
        assertThat(first.created()).containsExactlyElementsOf(expected);
        assertThat(second.created()).isEmpty();
        assertThat(partitions()).containsAll(expected);

        expected.forEach(this::drop);
    }

    @Test
    void dropsPartitionsOlderThanRetention() {
        YearMonth old = YearMonth.now(ZoneOffset.UTC).minusYears(3);
        String oldPartition = RumPartitionMaintenance.partitionName(old);
        jdbc.sql("CREATE TABLE %s PARTITION OF rum_event FOR VALUES FROM ('%s') TO ('%s')"
                        .formatted(oldPartition, start(old), start(old.plusMonths(1))))
                .update();

        RumPartitionMaintenance.Result result = at(YearMonth.now(ZoneOffset.UTC)).run();

        assertThat(result.dropped()).contains(oldPartition);
        assertThat(partitions()).doesNotContain(oldPartition)
                .contains(RumPartitionMaintenance.partitionName(YearMonth.now(ZoneOffset.UTC)));
    }

    @Test
    void purgesSeenIdsOlderThanDedupWindow() {
        UUID oldId = UUID.randomUUID();
        UUID recentId = UUID.randomUUID();
        Instant now = Instant.now();
        insertSeen(oldId, now.minusSeconds(properties.rum().dedupWindowHours() * 3600L + 60));
        insertSeen(recentId, now);

        RumPartitionMaintenance.Result result = new RumPartitionMaintenance(jdbc, properties, Clock.fixed(now, ZoneOffset.UTC)).run();

        assertThat(result.purgedSeenIds()).isPositive();
        assertThat(seen(oldId)).isFalse();
        assertThat(seen(recentId)).isTrue();
    }

    private void insertSeen(UUID id, Instant seenAt) {
        jdbc.sql("INSERT INTO rum_event_seen (event_id, seen_at) VALUES (:id, :seenAt)")
                .param("id", id)
                .param("seenAt", seenAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    private boolean seen(UUID id) {
        return jdbc.sql("SELECT count(*) FROM rum_event_seen WHERE event_id = :id")
                .param("id", id)
                .query(Long.class)
                .single() > 0;
    }

    private RumPartitionMaintenance at(YearMonth month) {
        Clock clock = Clock.fixed(start(month).plusSeconds(3600), ZoneOffset.UTC);
        return new RumPartitionMaintenance(jdbc, properties, clock);
    }

    private List<String> partitions() {
        return jdbc.sql("""
                        SELECT child.relname FROM pg_inherits i
                        JOIN pg_class child ON child.oid = i.inhrelid
                        JOIN pg_class parent ON parent.oid = i.inhparent
                        WHERE parent.relname = 'rum_event'
                        """)
                .query(String.class)
                .list();
    }

    private void drop(String partition) {
        jdbc.sql("DROP TABLE IF EXISTS " + partition).update();
    }

    private static Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
