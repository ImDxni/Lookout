CREATE TABLE site (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    allowed_origins TEXT[]          NOT NULL DEFAULT '{}',
    public_key      VARCHAR(64)     NOT NULL,
    sampling_rate   NUMERIC(5, 4)   NOT NULL DEFAULT 1.0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_site_public_key UNIQUE (public_key),
    CONSTRAINT ck_site_sampling_rate CHECK (sampling_rate >= 0 AND sampling_rate <= 1)
);