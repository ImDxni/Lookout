CREATE TABLE rum_event_seen (
    event_id UUID        PRIMARY KEY,
    seen_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rum_event_seen_seen_at ON rum_event_seen (seen_at);
