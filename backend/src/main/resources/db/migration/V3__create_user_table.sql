CREATE TABLE app_user (
                      id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                      email           VARCHAR(255)    NOT NULL,
                      password_hash   VARCHAR(255)     NOT NULL,
                      created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

                      CONSTRAINT uq_email UNIQUE (email)
);

CREATE UNIQUE INDEX uq_app_user_lower_email ON app_user (lower(email));