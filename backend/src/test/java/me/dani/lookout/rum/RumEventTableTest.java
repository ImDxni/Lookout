package me.dani.lookout.rum;

import me.dani.lookout.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RumEventTableTest extends ApiIntegrationTest {

    private static final String INSERT = """
            INSERT INTO rum_event (event_id, received_at, site_id, session_id, type, schema_version, payload)
            VALUES (:eventId, :receivedAt, :siteId, :sessionId, 'start', 1, '{}'::jsonb)
            ON CONFLICT DO NOTHING
            """;

    @Autowired
    private JdbcClient jdbc;

    private UUID siteId;

    @BeforeEach
    void createSite() {
        siteId = UUID.randomUUID();
        jdbc.sql("INSERT INTO site (id, name, public_key) VALUES (:id, :name, :key)")
                .param("id", siteId)
                .param("name", "Rum " + siteId)
                .param("key", "pk_" + siteId)
                .update();
    }

    @Test
    void partitionsExistForCurrentAndNextTwoMonths() {
        List<String> partitions = jdbc.sql("""
                        SELECT c.relname FROM pg_inherits i
                        JOIN pg_class c ON c.oid = i.inhrelid
                        JOIN pg_class p ON p.oid = i.inhparent
                        WHERE p.relname = 'rum_event'
                        """)
                .query(String.class)
                .list();

        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy_MM");
        assertThat(partitions).contains(
                "rum_event_" + now.format(format),
                "rum_event_" + now.plusMonths(1).format(format),
                "rum_event_" + now.plusMonths(2).format(format));
    }

    @Test
    void sameEventIdAndReceivedAtIsStoredOnce() {
        UUID eventId = UUID.randomUUID();
        Instant receivedAt = Instant.now();

        int first = insert(eventId, receivedAt);
        int second = insert(eventId, receivedAt);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(countForSite()).isEqualTo(1);
    }

    @Test
    void rowLandsInTheCurrentMonthPartition() {
        insert(UUID.randomUUID(), Instant.now());

        String partition = jdbc.sql("SELECT tableoid::regclass::text FROM rum_event WHERE site_id = :siteId")
                .param("siteId", siteId)
                .query(String.class)
                .single();

        assertThat(partition).isEqualTo("rum_event_" + YearMonth.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy_MM")));
    }

    @Test
    void receivedAtWithoutPartitionIsRejected() {
        Instant farFuture = YearMonth.now(ZoneOffset.UTC).plusYears(5).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        assertThatThrownBy(() -> insert(UUID.randomUUID(), farFuture))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unknownSiteIsRejected() {
        assertThatThrownBy(() -> jdbc.sql(INSERT)
                .param("eventId", UUID.randomUUID())
                .param("receivedAt", Timestamp.from(Instant.now()))
                .param("siteId", UUID.randomUUID())
                .param("sessionId", UUID.randomUUID())
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheSiteDeletesItsEvents() {
        insert(UUID.randomUUID(), Instant.now());

        jdbc.sql("DELETE FROM site WHERE id = :id").param("id", siteId).update();

        assertThat(countForSite()).isZero();
    }

    private int insert(UUID eventId, Instant receivedAt) {
        return jdbc.sql(INSERT)
                .param("eventId", eventId)
                .param("receivedAt", Timestamp.from(receivedAt))
                .param("siteId", siteId)
                .param("sessionId", UUID.randomUUID())
                .update();
    }

    private long countForSite() {
        return jdbc.sql("SELECT count(*) FROM rum_event WHERE site_id = :siteId")
                .param("siteId", siteId)
                .query(Long.class)
                .single();
    }
}
