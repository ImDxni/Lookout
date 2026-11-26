# 002. Creating the single administrator

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
There is one administrator and no registration (ADR 000). Someone has to create the first and only row in `app_user`. The repository is public, so no credential may live in the code or in migrations, and each environment (dev, prod) needs its own credentials.

## Decision
An `ApplicationRunner` (`AdminBootstrap`) runs at startup, after Flyway:
- if `app_user` is **empty**, it creates the admin from `lookout.admin.email` / `lookout.admin.password`, hashing the password with the `PasswordEncoder`;
- otherwise it does nothing, so restarts never duplicate or overwrite the admin.

The values come from environment variables (`LOOKOUT_ADMIN_EMAIL`, `LOOKOUT_ADMIN_PASSWORD`) through `LookoutProperties`, a validated record (`@NotBlank`, `@Email`, minimum length): if they are missing, the application refuses to start. In dev they are read from `backend/.env` (imported only by the `dev` profile); tests pass fixed fake values.

The e-mail is normalized (trimmed, lower-cased) inside the `AppUser` entity, and a unique index on `lower(email)` protects against concurrent bootstraps.

Only the e-mail is logged, never the password or its hash.

## Alternatives considered
- **Flyway migration inserting the user:** puts a hash in a public repo, identical in every environment; migrations are for schema, not for per-environment data.
- **"First user to register becomes admin" endpoint:** whoever reaches the server first takes control.
- **Manual SQL insert:** not reproducible, and the hash has to be computed by hand.
- **CLI command / dedicated profile:** works, but is more machinery than one user needs.

## Consequences
- The plain-text password lives in the environment of the running process. An option for later: accept a precomputed BCrypt hash instead.
- Changing the password is not possible by changing the variable (the bootstrap skips an existing admin); it will need an endpoint or a manual procedure.
- Two instances starting on an empty table at the same time: one insert fails on the unique index and that instance fails to start; acceptable for a single-instance deployment.
