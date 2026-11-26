CREATE TABLE rum_event (
    event_id             UUID           NOT NULL,
    received_at          TIMESTAMPTZ    NOT NULL,
    site_id              UUID           NOT NULL,
    session_id           UUID           NOT NULL,
    type                 VARCHAR(32)    NOT NULL,
    schema_version       SMALLINT       NOT NULL,
    client_ts            TIMESTAMPTZ,
    since_start_ms       INTEGER,
    page_path            VARCHAR(2048),
    configurator_version VARCHAR(64),
    payload              JSONB          NOT NULL DEFAULT '{}',
    device               JSONB,

    CONSTRAINT pk_rum_event PRIMARY KEY (event_id, received_at),
    CONSTRAINT fk_rum_event_site FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE,
    CONSTRAINT ck_rum_event_schema_version CHECK (schema_version > 0),
    CONSTRAINT ck_rum_event_since_start CHECK (since_start_ms IS NULL OR since_start_ms >= 0)
) PARTITION BY RANGE (received_at);

CREATE INDEX idx_rum_event_site_received ON rum_event (site_id, received_at);
CREATE INDEX idx_rum_event_session ON rum_event (session_id);

DO $$
DECLARE
    base_month TIMESTAMP := date_trunc('month', now() AT TIME ZONE 'UTC');
    month_start TIMESTAMP;
BEGIN
    FOR i IN 0..2 LOOP
        month_start := base_month + make_interval(months => i);
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF rum_event FOR VALUES FROM (%L) TO (%L)',
            'rum_event_' || to_char(month_start, 'YYYY_MM'),
            month_start AT TIME ZONE 'UTC',
            (month_start + INTERVAL '1 month') AT TIME ZONE 'UTC'
        );
    END LOOP;
END $$;
