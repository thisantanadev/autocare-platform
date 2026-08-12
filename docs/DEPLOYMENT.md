# Deployment (Railway)

One Railway project, `autocare-platform`, with four services. Only the frontend
gets a public domain; the browser reaches the API through the frontend's nginx
proxy, so the API stays same-origin and the HttpOnly refresh cookie keeps
working exactly as it does locally.

```
Internet ──► frontend (public domain, nginx)
                 │  proxies /api  ──► backend (private) ──► analytics (private)
                 │                        └───────────────► Postgres (private)
                 └── serves the SPA
```

**No secret values appear in this repository.** Everything below lists variable
names only; the values live in Railway.

## Services

| Service | Root directory | Public domain | Health check |
|---|---|---|---|
| `frontend` | `frontend` | **yes** | `/health` |
| `backend` | `backend` | no | `/actuator/health/readiness` |
| `analytics` | `analytics` | no | `/health` |
| `Postgres` | — (Railway image) | no | managed |

Each application service carries a `railway.json` with its builder, health
check and watch patterns. **Root directories are not expressible in
`railway.json`** — set them per service in Railway (Settings → Source → Root
Directory), otherwise every service would build from the repository root.

Watch patterns are repository-root relative (`backend/**`, `analytics/**`,
`frontend/**`), so a change to one service does not rebuild the other two.

## Variables

Values marked *reference* use Railway's variable references, so nothing is
copied by hand and rotating the database credentials propagates automatically.

### backend

| Variable | Notes |
|---|---|
| `SERVER_PORT` | Port Tomcat listens on |
| `DATABASE_URL` | JDBC URL built from Postgres references, over the private domain |
| `DATABASE_USERNAME` | *reference* to the Postgres user |
| `DATABASE_PASSWORD` | *reference* to the Postgres password |
| `JWT_SECRET` | random, at least 48 bytes; the app refuses to start without it |
| `COOKIE_SECURE` | `true` — the public domain is HTTPS |
| `ANALYTICS_BASE_URL` | analytics private domain and port |
| `ANALYTICS_INTERNAL_TOKEN` | random; must match the analytics service |
| `SPRING_PROFILES_ACTIVE` | **empty** — never enable the `demo` profile publicly |
| `CORS_ALLOWED_ORIGINS` | the final public URL |

The JDBC URL is assembled from the Postgres service's **private** domain rather
than its public proxy host, so database traffic never leaves Railway's network:

```
jdbc:postgresql://${{Postgres.RAILWAY_PRIVATE_DOMAIN}}:5432/${{Postgres.PGDATABASE}}
```

Flyway applies `V1`–`V6` on the first boot, and `ddl-auto: validate` then checks
the JPA mappings against the migrated schema, so a successful start proves both
agree.

### analytics

| Variable | Notes |
|---|---|
| `PORT` | Port uvicorn binds |
| `ANALYTICS_INTERNAL_TOKEN` | the same value as the backend's |

The service fails closed: without the token every request is rejected. It has no
public domain, so it is reachable only from the backend.

### frontend

| Variable | Notes |
|---|---|
| `PORT` | Port nginx listens on |
| `BACKEND_URL` | backend private domain and port, used by `proxy_pass` |

`nginx.conf.template` is rendered at container start by the official nginx
entrypoint, which runs `envsubst` over `/etc/nginx/templates`. Only `PORT` and
`BACKEND_URL` are substituted (`NGINX_ENVSUBST_FILTER`), so nginx's own `$uri`,
`$host` and `$scheme` survive. Both default to the docker-compose values, which
is why the local stack needs no extra configuration.

## Private networking caveat

Railway's internal DNS resolves `<service>.railway.internal` to both IPv4 and
IPv6 in current environments, so the default IPv4 bindings work. If a service
is ever unreachable privately while healthy on its own port, the cause is
usually an IPv4-only listener:

- **analytics** — set `HOST=::` (the Dockerfile already reads `HOST`)
- **backend** — set `SERVER_ADDRESS=::`; Spring Boot binds it to `server.address`
- **frontend** — add `listen [::]:${PORT};` beside the existing `listen`

## Local development is unaffected

`docker-compose.yml` is unchanged and still the way to run the stack locally.
The image defaults (`PORT`, `BACKEND_URL`, `HOST`, `SERVER_PORT`) reproduce the
previous behaviour exactly, so `docker compose up --build` works as before —
including the `demo` profile, which stays local-only.
