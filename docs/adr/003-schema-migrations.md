# 003. Database schema managed by Flyway inside the application

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
The schema must be versioned, reproducible on an empty database (tests, new environments) and identical everywhere. Early on, a separate Flyway Desktop project was created next to the backend, with its own migration folder and its own connection settings.

## Decision
- Migrations live in `backend/src/main/resources/db/migration` and are applied by Spring Boot's Flyway integration at application startup, before Hibernate.
- Naming: `V<n>__<what_it_does>.sql`; the description must describe the change (it is stored and validated by Flyway).
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate never changes the schema, it only checks that entities match it.
- **A migration that has been applied anywhere, or pushed, is never edited.** Every change is a new migration.
- Uniqueness rules that the API depends on are enforced in the database too (e.g. `lower(name)` for sites, `lower(email)` for users), not only in application code.
- The Flyway Desktop project was removed: a single source of truth for migrations.

## Alternatives considered
- **`ddl-auto=update`:** convenient, but not versioned, cannot drop or rename safely, and differs between environments.
- **Separate Flyway Desktop / CLI project:** two histories against the same database would conflict, and it was not on the application's classpath, so migrations were never applied by the app.
- **`flyway repair` after editing an applied migration:** it only rewrites the stored checksum and does not run anything; the edited database and a fresh one would silently diverge.
- **Liquibase:** equivalent capabilities; plain SQL migrations are simpler to read and to explain.

## Consequences
- Tests run every migration from scratch on a real Postgres (Testcontainers), so the full sequence is verified on every build.
- Mistakes cost an extra migration. Example: removing a redundant constraint from the already pushed `V3` caused a checksum mismatch; it was reverted and fixed with `V4__drop_redundant_email_constraint.sql`.
- Only migrations that never left the developer's machine may be rewritten, and then the local database must be recreated (`docker compose down -v`).
- Production can later move migrations to a separate CI/CD step (`spring.flyway.enabled=false` in the app) if they become long-running.
