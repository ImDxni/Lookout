# 001. Admin authentication: server-side session with CSRF protection

- **Status:** Accepted
- **Date:** 2026-11-26

## Context
The admin API (`/api/**`) must be protected. There is exactly one administrator (ADR 000) and the client is a Next.js dashboard that will be served from the same domain as the API. The public RUM endpoint (`/rum/**`) is a separate concern and does not use this mechanism.

## Decision
- **Session cookie, not tokens.** After login the server stores the `SecurityContext` in the HTTP session; the browser holds only `JSESSIONID` (`HttpOnly`).
- **JSON login endpoint** `POST /api/auth/login` implemented in a controller with the `AuthenticationManager` (`DaoAuthenticationProvider` + `UserDetailsService` backed by `app_user` + delegating `PasswordEncoder`, BCrypt by default). Because it is a custom endpoint, it explicitly does what Spring's login filters would do:
  1. save the context with `HttpSessionSecurityContextRepository` (Spring Security 6+ requires an explicit save);
  2. run a `CompositeSessionAuthenticationStrategy`: `ChangeSessionIdAuthenticationStrategy` (session fixation) + `CsrfAuthenticationStrategy` (new CSRF token after login);
  3. read the new, deferred CSRF token so that the login response already carries the new `XSRF-TOKEN` cookie.
- **CSRF stays enabled** for every state-changing request, login included, using the SPA setup (`csrf.spa()`): token in a JS-readable `XSRF-TOKEN` cookie, sent back in the `X-XSRF-TOKEN` header. The `CookieCsrfTokenRepository` is a bean shared by the filter chain and the login strategy.
- **Logout** uses Spring's logout filter at `POST /api/auth/logout`, answering `204` (`HttpStatusReturningLogoutSuccessHandler`). Session invalidation makes `JSESSIONID` useless, so no explicit cookie deletion is configured.
- **401 vs 403:** unauthenticated requests get `401` from an `HttpStatusEntryPoint`; `403` is reserved for refused requests (missing CSRF token, `denyAll`).
- **Authorization rules**, first match wins: `/api/auth/me` authenticated → `/api/auth/**` public → `/api/**` authenticated → Swagger, `/error`, `/actuator/health` public → everything else denied.
- Bad credentials and unknown e-mails return the same `401 "Wrong credentials"` (no user enumeration).

## Alternatives considered
- **JWT in `localStorage`:** readable by any injected script (XSS steals the token); revocation before expiry needs a denylist, which brings back server-side state.
- **JWT in an `HttpOnly` cookie:** still needs CSRF protection, and still has the revocation problem. No benefit over a session for one admin on one domain.
- **`formLogin()`:** built for HTML pages; it answers with redirects and would need custom success/failure handlers anyway. A dedicated endpoint makes the flow explicit.
- **HTTP Basic:** credentials on every request, no logout.
- **Disabling CSRF with a stateless API:** used temporarily while building the CRUD; wrong once authentication is a cookie, because the browser attaches it automatically to cross-site requests.

## Consequences
- The dashboard must read `XSRF-TOKEN` from `document.cookie` and send `X-XSRF-TOKEN` on POST/PUT/DELETE, and must refresh it after login and logout (the token rotates).
- The API and the dashboard must share a domain (reverse proxy), otherwise the cookie setup becomes cross-site (`SameSite`, CORS with credentials). To be recorded in the deployment ADR.
- Sessions live in the application's memory: a restart logs the admin out, and multiple instances would need sticky sessions or Spring Session JDBC.
- Swagger UI cannot perform the JSON login with CSRF by itself; the API docs are public, the endpoints behind them are not.
- Still to configure: `Secure` cookie flag in production, `SameSite`, session timeout.
- Covered by end-to-end tests (`AuthFlowTest`): 401 without login, 403 without CSRF, login/logout, session id change, CSRF token rotation.
