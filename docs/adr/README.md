# Architecture Decision Records

Each significant technical decision gets a short file here: `NNN-short-title.md` (e.g. `001-admin-auth.md`).

## Format

```markdown
# NNN. Title

- **Status:** Proposed | Accepted | Superseded by NNN
- **Date:** YYYY-MM-DD

## Context
What problem or constraint forces a decision?

## Decision
What was decided.

## Alternatives considered
What else was evaluated, and why it was rejected.

## Consequences
What becomes easier or harder because of this decision.
```

## Index

| # | Title | Status |
|---|-------|--------|
| [000](000-why-this-project.md) | Why this project | Accepted |
| [001](001-admin-authentication.md) | Admin authentication: server-side session with CSRF protection | Accepted |
| [002](002-admin-bootstrap.md) | Creating the single administrator | Accepted |
| [003](003-schema-migrations.md) | Database schema managed by Flyway inside the application | Accepted |
| [004](004-configuration-and-secrets.md) | Configuration profiles and secrets | Accepted |
| [005](005-api-design-and-errors.md) | Admin API design and error responses | Accepted |

Reference: https://adr.github.io
