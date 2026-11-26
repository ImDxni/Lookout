# 000. Why this project

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
An e-commerce site embeds a 3D product configurator (BabylonJS, GLB models, KTX2 textures). It has three intermittent problems:

| Symptom | Why a plain uptime monitor misses it |
|---|---|
| Slow loading | The page answers 200 quickly; the 3D part is slow, and only on some networks and devices. |
| Model not showing | The page answers 200, but an asset returns 404, has wrong CORS headers, or the transcoder fails. |
| Crash on mobile | It happens only on the GPU of real devices; a headless browser on a server cannot reproduce it. |

There is no data to say which problem matters most, on which devices, or whether a fix helped.

Goals:
1. Measure, with real-user data, which problem weighs most and where.
2. Get an e-mail alert when the configurator breaks, before customers notice.
3. Measure the configurator before and after each fix.
4. Learn Spring Boot in depth on a real case.

Explicit non-goals for now: multi-tenancy, user registration, paid plans, replacing Sentry / Datadog / Uptime Kuma.

## Decision
Build **Lookout**, a self-hosted monitor made of:
- **RUM** (real user monitoring): a small TypeScript snippet in the configurator sends events to a public ingestion endpoint.
- **Synthetic checks**: scheduled HTTP / asset checks, and later a real browser check (Playwright) in a separate worker.
- **Alerting** by e-mail through a transactional outbox.
- A **Next.js dashboard** for a single administrator.

Architecture choices:
- **Modular monolith** in Spring Boot, packaged by feature (`site`, `auth`, `rum`, `analytics`, `alerting`, `notification`, `monitor`, `checker`, `incident`, `shared`), not by layer.
- **PostgreSQL only**: storage, partitioned event tables, work queues (`FOR UPDATE SKIP LOCKED`), percentiles.
- **One administrator, single tenant.**
- The browser worker will run from the same codebase with a dedicated Spring profile, in its own container, because Chromium is heavy.

## Alternatives considered
- **Microservices + Kafka:** the expected load (one site, events in the order of tens per second at peak) does not justify the operational cost. Postgres can act as the queue.
- **A time-series database (TimescaleDB, InfluxDB):** plain Postgres with declarative partitioning and hourly rollups is enough at this scale; revisit if queries become slow (to be evaluated in a later ADR).
- **Adopting an existing product (Sentry, Datadog, Uptime Kuma):** none covers the configurator-specific signals (WebGL context loss, crash detection on next visit, per-asset timings) without custom work, and the learning goal would be lost. They remain references to study.
- **Multi-tenant SaaS from day one:** adds data isolation, roles and registration with no benefit for a single site.

## Consequences
- Easy to run (one app + Postgres with Docker Compose) and to reason about in a single codebase.
- Module boundaries are a discipline, not enforced by the network; they can be verified with Spring Modulith.
- Horizontal scaling needs care in the places that assume one instance (scheduled jobs → ShedLock, queue claiming → `SKIP LOCKED`, sessions → see ADR 001).
- Adding tenants later would require a `tenant_id` dimension across tables and authorization rules.
