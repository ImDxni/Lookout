# 004. Configuration profiles and secrets

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
The same application runs on a developer machine, in automated tests (locally and in CI) and in production. Secrets (database password, admin credentials, later SMTP) must never be committed, because the repository is public.

## Decision
- **Shared settings** live in `application.properties`; profile files contain only what differs:
  - `dev` (default via `spring.profiles.default`): debug logging, imports `optional:file:.env[.properties]`;
  - `prod`: datasource from `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, with no defaults, so a missing variable stops the startup.
- **Local development uses one `.env` file** (git-ignored, with a committed `.env.example`) read both by Docker Compose (Postgres container credentials, with `${VAR:?}` checks) and by Spring (admin credentials).
- **Database in dev** is provided by Spring Boot's Docker Compose support (`compose.yaml`); the connection details are discovered from the container, so no datasource properties are needed in dev.
- **Tests** do not depend on `.env`: Postgres comes from Testcontainers (`@ServiceConnection`) and the admin credentials are fixed fake values in the test configuration. No `src/test/resources/application.properties`, which would shadow the main one.
- **Application settings** are bound to a validated record (`LookoutProperties`, `@ConfigurationProperties` + Bean Validation), so invalid configuration fails at startup.
- **Postgres version is pinned** (`postgres:18`) in both Compose and Testcontainers.

## Alternatives considered
- **Secrets in `application-*.properties`:** end up in git.
- **IntelliJ run-configuration environment variables:** work only in the IDE, not with `./gradlew bootRun`, and are easy to lose.
- **A git-ignored `application-local.properties`:** a second file to keep aligned with `.env`.
- **A `test` profile file:** not needed while all integration tests share one base class with explicit properties.

## Consequences
- `bootRun`, the IDE and Docker Compose all work from `backend/` with no extra setup beyond copying `.env.example` to `.env`.
- The working directory must be `backend/` for both `compose.yaml` and `.env` to be found.
- Production must provide every variable explicitly; there are no silent defaults.
- CI needs no secrets for the build: everything required by tests is fake and local to the test run.
