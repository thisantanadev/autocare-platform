# Architecture

## Overview

```
                    ┌──────────────────────────┐
  Browser ────────► │  frontend (nginx / Vite) │
                    │  React 19 SPA            │
                    └────────────┬─────────────┘
                                 │  /api/* proxied to the same origin,
                                 │  so the refresh cookie is first-party
                                 ▼
                    ┌──────────────────────────┐        ┌──────────────────┐
                    │  backend (Spring Boot 4) │ ─────► │  analytics       │
                    │  system of record        │  POST  │  (FastAPI)       │
                    └────────────┬─────────────┘        │  stateless       │
                                 │                      └──────────────────┘
                                 ▼
                    ┌──────────────────────────┐
                    │  PostgreSQL 18 + Flyway  │
                    └──────────────────────────┘
```

The browser only ever talks to one origin. nginx (production) and the Vite dev
server (development) both proxy `/api` to the backend, which means the
`autocare_refresh` cookie is a first-party cookie in every environment and no
cross-site cookie handling is needed.

## Service boundaries

### backend — the only stateful service

Owns authentication, authorization, validation, business rules and persistence.
Package layout is by feature, not by layer:

```
com.autocare.backend
├── auth/          JWT issuing, refresh-token rotation, /auth endpoints
├── user/          User entity and repository
├── vehicle/       Vehicle CRUD, plate normalization, mileage rules
├── maintenance/   Maintenance records + categories
├── fuel/          Fuel entries + derived price per litre
├── reminder/      Reminders, due/overdue logic, complete/reopen
├── dashboard/     Cross-vehicle aggregation for the dashboard
├── analytics/     RestClient to the Python service + DTO mirror
├── demo/          Demo data seeder (only under the "demo" profile)
├── config/        Security, OpenAPI, typed configuration properties
└── common/        Error handling, API error contract, page response
```

Each feature follows `Controller → Service → Repository`. Controllers are
package-private and handle only HTTP concerns; services hold the business rules
and own the transaction boundary; entities are never serialized directly — every
response goes through a DTO record with a static `from(...)` factory.

### analytics — pure calculation

A stateless FastAPI service exposing one internal endpoint:

```
POST /internal/v1/analytics/vehicle    (requires X-Internal-Token)
GET  /health
```

It receives a sanitized snapshot of one vehicle's maintenance and fuel records
and returns totals, monthly costs, cost by category, fuel statistics, a trend, a
period comparison and upcoming maintenance. It has no database, no user
identifiers and no browser exposure.

Metrics that cannot be computed honestly are returned as `null` with a
`warnings` entry explaining why — for example, fuel efficiency requires at least
three full-tank refuelings. The SPA translates those warning codes to
Portuguese; it never renders an invented number.

**Why a separate service?** Calculation is CPU-bound, has no persistence needs
and evolves independently from the data model. Keeping it out of the Java
process means it can be scaled or restarted on its own, and it makes the failure
boundary explicit: analytics is a non-essential dependency.

### frontend — presentation only

React 19 with React Router. No state-management library: server data is fetched
per page through the `useAsyncData` hook, and the only cross-cutting client
state is the authenticated user in `AuthContext`.

```
src/
├── api/          One module per resource, all sharing the axios client
├── auth/         AuthContext: user, login, register, logout
├── components/   Presentational building blocks + charts
├── hooks/        useAsyncData
├── pages/        One component per route
├── utils/        Formatting (pt-BR) and validation mirrors
└── styles/       The single global stylesheet / design system
```

Client-side validation mirrors the backend constraints for fast feedback, and is
explicitly documented as non-authoritative — every rule is enforced again on the
server.

## Data model

```
users ──┬── refresh_tokens          (hashed token, expiry, revoked_at)
        └── vehicles ──┬── maintenance_records
                       ├── fuel_entries
                       └── reminders
```

Every child row reaches its owner through `vehicles.user_id`, and each service
method loads vehicles with `findByIdAndUserId`. A resource belonging to another
user therefore surfaces as `404`, never `403`, so the API does not disclose
whether an id exists. `OwnershipIsolationIntegrationTest` covers this.

Deleting a vehicle cascades to its children in the database schema
(`ON DELETE CASCADE`) rather than in application code.

Migrations are immutable and forward-only, `V1` through `V6`:

| Migration | Table |
|---|---|
| `V1` | `users` |
| `V2` | `refresh_tokens` |
| `V3` | `vehicles` |
| `V4` | `maintenance_records` |
| `V5` | `fuel_entries` |
| `V6` | `reminders` |

Hibernate runs with `ddl-auto: validate`: it never modifies the schema, only
asserts that the mappings match what Flyway produced.

## Request flow

A typical authenticated read:

1. The SPA calls `GET /api/v1/vehicles` with `Authorization: Bearer <access>`.
2. `JwtAuthenticationFilter` validates the signature and expiry and populates an
   `AuthPrincipal`.
3. The controller delegates to the service, which scopes every query to the
   authenticated user id.
4. Entities are mapped to DTO records and serialized.

If the access token has expired, the axios response interceptor performs a
single `POST /auth/refresh` — shared between all requests that hit `401`
simultaneously, so an expired token never causes a refresh stampede — then
replays the original request once.

## Deliberate trade-offs

- **No cross-service transactions.** Analytics is read-only, so eventual
  consistency is not a concern.
- **Access token in memory only.** Surviving a page reload is handled by the
  refresh cookie, not by `localStorage`. See `docs/SECURITY.md`.
- **H2 for backend tests, PostgreSQL for migration verification.** Fast tests
  without pretending H2 is PostgreSQL; the real server is exercised in CI.
- **Single global stylesheet.** At this scale, one reviewable design system beats
  per-component CSS indirection.
- **No route-level code splitting yet.** The production bundle is ~692 kB
  (~207 kB gzipped), dominated by Recharts. Worth splitting if the app grows;
  not worth the indirection today.
