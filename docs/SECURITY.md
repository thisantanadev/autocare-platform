# Security

This document describes how AutoCare handles authentication, tokens and secrets,
and which trade-offs were made deliberately. It is a portfolio project, not a
hardened production deployment — the "Known gaps" section is honest about that.

## Authentication

Passwords are hashed with **BCrypt** (Spring Security's `BCryptPasswordEncoder`,
default strength). Because BCrypt only consumes the first 72 bytes of input, the
registration contract caps passwords at 72 characters and requires at least 8,
rather than silently truncating a longer one.

Sessions use a split-token scheme:

| Token | Lifetime | Transport | Storage |
|---|---|---|---|
| Access token | 15 min (`JWT_ACCESS_TOKEN_TTL`) | `Authorization: Bearer` header | Browser memory only |
| Refresh token | 14 days (`JWT_REFRESH_TOKEN_TTL`) | `autocare_refresh` cookie | SHA-256 hash in `refresh_tokens` |

### Access tokens

Signed JWTs (HMAC-SHA256) carrying the user id. Validated on every request by
`JwtAuthenticationFilter`; the session policy is `STATELESS`, so no server-side
session exists.

The access token is held in a module-level variable in the SPA and is **never**
written to `localStorage` or `sessionStorage`. An XSS payload therefore cannot
simply read a persisted token out of storage — it would have to exfiltrate it
during the page's lifetime. `src/api/client.test.js` asserts that nothing is
persisted.

### Refresh tokens and rotation

The refresh cookie is set with:

```
HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth
```

`HttpOnly` keeps it out of reach of scripts, `SameSite=Strict` prevents
cross-site submission, and the narrow `Path` means it is not even attached to
ordinary API calls — only to the four auth endpoints that need it.

Refresh tokens are **rotated on every use**: presenting a valid token revokes it
and issues a new one. Only the SHA-256 hash is stored, so a database dump does
not yield usable tokens.

Reuse of an already-rotated or revoked token is treated as evidence of theft: in
addition to rejecting the request, **every active session for that user is
revoked**. This containment update is committed even though the request fails
(`noRollbackFor`), so an attacker replaying a stolen token locks out their own
stolen session rather than silently sharing the account.

### CSRF

CSRF protection is disabled, which is safe here only because of the split above:

- All data endpoints authenticate via the `Authorization` header, which a
  cross-site form or image request cannot set.
- The only cookie-authenticated endpoints — refresh and logout — are guarded by
  `SameSite=Strict` on a cookie scoped to `/api/v1/auth`.

If a cookie-authenticated data endpoint were ever added, CSRF protection would
have to be re-enabled.

## Authorization

There are no roles; the entire model is per-user ownership. Every service method
loads the vehicle with `findByIdAndUserId` and every child query is scoped
through `vehicles.user_id`.

Requests for another user's resource return **404, not 403**, so the API does not
reveal whether an id exists. `OwnershipIsolationIntegrationTest` verifies this
for vehicles, maintenance records, fuel entries and reminders.

## Secrets and configuration

Nothing security-relevant is hard-coded. The application **refuses to start**
when `JWT_SECRET` is missing, so a deployment can never fall back to a
well-known default key:

| Variable | Purpose | Notes |
|---|---|---|
| `JWT_SECRET` | HMAC-SHA256 signing key | Required; at least 32 bytes |
| `ANALYTICS_INTERNAL_TOKEN` | Shared secret for the analytics service | Analytics fails closed when unset |
| `DATABASE_PASSWORD` | PostgreSQL credential | |
| `COOKIE_SECURE` | `Secure` flag on the refresh cookie | Defaults to `true` |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins | Defaults to the Vite dev origin |
| `DEMO_USER_PASSWORD` | Password of the seeded demo account | `demo` profile only |

Generate real values with `openssl rand -base64 48` / `openssl rand -hex 32`.
`.env` is gitignored; only `.env.example` is committed.

**`COOKIE_SECURE=false` is for local HTTP only.** It is set in
`application-dev.yml` and in `docker-compose.yml` because the local stack is
served over plain HTTP on `localhost`. Any internet-facing deployment must serve
HTTPS and leave it at `true`.

## The internal analytics boundary

The analytics service is never exposed to browsers — in Docker Compose it is not
published to the host at all. Requests must carry `X-Internal-Token`, compared
with `secrets.compare_digest` to avoid leaking information through timing.

It **fails closed**: if `ANALYTICS_INTERNAL_TOKEN` is not configured, every
request is rejected rather than the service silently accepting anonymous
callers.

The backend sends only what the calculations need — dates, amounts, odometer
readings and categories. No names, e-mail addresses or user identifiers cross
that boundary, so a compromise of the analytics service exposes no personal
data. When it is unreachable the backend raises
`AnalyticsUnavailableException`, mapped to `503`, and the SPA shows a notice
while the rest of the vehicle history stays usable.

## Error handling

`GlobalExceptionHandler` returns a uniform contract and never leaks internals:
`server.error.include-stacktrace: never`, and each response carries a short
`traceId` that correlates with the server log for diagnosis without exposing
stack traces to clients.

## Known gaps

Deliberately out of scope for this project — listed so the omissions are not
mistaken for oversights:

- **No rate limiting or brute-force lockout** on login or registration. A real
  deployment needs both, plus a CAPTCHA or similar at the edge.
- **No e-mail verification or password reset flow.**
- **No refresh-token binding** to device fingerprint or IP.
- **No multi-factor authentication.**
- **No audit log** of security-relevant events beyond application logs.
- **No secret manager integration**; secrets arrive as environment variables.
- **Access tokens cannot be revoked before expiry** — the 15-minute TTL is the
  containment window. Revoking a refresh token stops renewal, not an
  already-issued access token.
- **Dependencies are not scanned** in CI (no Dependabot or CVE gate configured).

## Reporting

This is a portfolio project without a security support commitment. If you find a
problem, please open an issue describing it.
