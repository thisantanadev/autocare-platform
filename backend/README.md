# AutoCare Backend

Spring Boot 4 API on Java 21 — the system of record for users, vehicles,
maintenance records, fuel entries and reminders. Owns authentication,
authorization and every business rule; delegates analytics to the Python
service.

See the repository root `README.md`, `docs/ARCHITECTURE.md`, `docs/API.md` and
`docs/SECURITY.md` for the full picture.

## Commands

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # run on :8080
./mvnw test                                              # 52 tests
./mvnw -DskipTests package                               # build target/backend-*.jar
```

Requires a PostgreSQL database; Flyway applies `V1`–`V6` on first start.

## Profiles

| Profile | Purpose |
|---|---|
| _(none)_ | Production shape: `JWT_SECRET` required, `COOKIE_SECURE=true` |
| `dev` | Development JWT secret, `COOKIE_SECURE=false`, debug logging |
| `demo` | Seeds a fictional demo account (`demo@autocare.dev`) on first boot |
| `test` | H2 in PostgreSQL compatibility mode, Flyway disabled |

Profiles combine: `-Dspring-boot.run.profiles=dev,demo`.

## Layout

Packages are organized by feature, each following
`Controller → Service → Repository`:

```
com.autocare.backend
├── auth/          JWT issuing, refresh-token rotation, /auth endpoints
├── user/          User entity and repository
├── vehicle/       Vehicle CRUD, plate normalization, mileage rules
├── maintenance/   Maintenance records and categories
├── fuel/          Fuel entries, derived price per litre
├── reminder/      Reminders, due/overdue logic, complete/reopen
├── dashboard/     Cross-vehicle aggregation
├── analytics/     RestClient to the Python service + DTO mirror
├── demo/          Demo data seeder ("demo" profile only)
├── config/        Security, OpenAPI, typed configuration properties
└── common/        Error handling, API error contract, page response
```

## Conventions

- **Controllers are package-private** and handle HTTP only. Business rules live
  in services, which own the transaction boundary.
- **Entities are never serialized.** Every response is a DTO record with a static
  `from(...)` factory.
- **Ownership is enforced in every query,** via `findByIdAndUserId` and scoping
  through `vehicles.user_id`. Another user's resource returns `404`, not `403`,
  so ids are not disclosed.
- **Migrations are immutable and forward-only.** Never edit an applied migration;
  add the next `V*` file. Hibernate runs with `ddl-auto: validate` and never
  changes the schema.
- **Business rule violations throw `BusinessRuleException`** (→ `422`); missing
  resources throw `ResourceNotFoundException` (→ `404`). `GlobalExceptionHandler`
  maps everything to the shared `ApiErrorResponse` contract.
- **Configuration is typed** through `AppProperties`; the application refuses to
  start without `JWT_SECRET`.

## Testing

Tests run against H2 in PostgreSQL compatibility mode with a Hibernate-generated
schema, which keeps them fast but means the Flyway migrations are *not* exercised
there. CI verifies the migrations against a real PostgreSQL 18 service container
by booting the packaged application with `ddl-auto=validate`, so a successful
boot proves the migrations and the JPA mappings agree.
