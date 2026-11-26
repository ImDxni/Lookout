package me.dani.lookout.rum.jdbc;

import lombok.RequiredArgsConstructor;
import me.dani.lookout.rum.RumEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RumEventWriter {

    private static final String INSERT = """
            WITH first_seen AS (
                INSERT INTO rum_event_seen (event_id, seen_at)
                VALUES (:eventId, :receivedAt)
                ON CONFLICT (event_id) DO NOTHING
                RETURNING event_id
            )
            INSERT INTO rum_event (
                event_id, received_at, site_id, session_id, type, schema_version,
                client_ts, since_start_ms, page_path, configurator_version, payload, device)
            SELECT
                event_id, :receivedAt, :siteId, :sessionId, :type, :schemaVersion,
                :clientTs, :sinceStartMs, :pagePath, :configuratorVersion,
                CAST(:payload AS jsonb), CAST(:device AS jsonb)
            FROM first_seen
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public int write(List<RumEvent> events) {
        SqlParameterSource[] batch = events.stream()
                .map(RumEventWriter::toParameters)
                .toArray(SqlParameterSource[]::new);

        int[] counts = jdbc.batchUpdate(INSERT, batch);
        return Arrays.stream(counts).map(count -> Math.max(count, 0)).sum();
    }

    private static SqlParameterSource toParameters(RumEvent event) {
        return new MapSqlParameterSource()
                .addValue("eventId", event.eventId())
                .addValue("receivedAt", utc(event.receivedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("siteId", event.siteId())
                .addValue("sessionId", event.sessionId())
                .addValue("type", event.type().wireName())
                .addValue("schemaVersion", event.schemaVersion(), Types.SMALLINT)
                .addValue("clientTs", utc(event.clientTs()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("sinceStartMs", event.sinceStartMs(), Types.INTEGER)
                .addValue("pagePath", event.pagePath(), Types.VARCHAR)
                .addValue("configuratorVersion", event.configuratorVersion(), Types.VARCHAR)
                .addValue("payload", event.payloadJson(), Types.VARCHAR)
                .addValue("device", event.deviceJson(), Types.VARCHAR);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
