# API reference

Base path: `/api/v1`. All request and response bodies are JSON.
Interactive docs (Swagger UI) run at <http://localhost:8080/swagger-ui.html>;
the OpenAPI document is at `/v3/api-docs`.

## Authentication

Every endpoint except the four auth endpoints below requires:

```
Authorization: Bearer <accessToken>
```

Access tokens live 15 minutes. Renew them with `POST /auth/refresh`, which reads
the `autocare_refresh` HttpOnly cookie — send credentials with the request
(`withCredentials: true` in the browser, `-c/-b` cookie jars in curl).

### `POST /auth/register` → `201`

```json
{ "name": "Motorista Demo", "email": "demo@autocare.dev", "password": "DemoAutoCare123" }
```

Constraints: `name` ≤ 120, valid `email` ≤ 255, `password` 8–72 characters.

Response (also sets the refresh cookie):

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": { "id": "uuid", "name": "Motorista Demo", "email": "demo@autocare.dev", "createdAt": "2026-08-12T12:00:00Z" }
}
```

`409 EMAIL_ALREADY_IN_USE` if the address is taken.

### `POST /auth/login` → `200`

```json
{ "email": "demo@autocare.dev", "password": "DemoAutoCare123" }
```

Same response shape. `401 INVALID_CREDENTIALS` on a bad e-mail or password.

### `POST /auth/refresh` → `200`

Requires the refresh cookie; no body. Rotates the refresh token (the old one is
revoked) and returns a fresh access token in the same shape as login.
`401 INVALID_REFRESH_TOKEN` if the cookie is missing, expired or already used —
reuse also revokes every active session for that user.

### `POST /auth/logout` → `204`

Revokes the refresh token and clears the cookie. Safe to call when already
logged out.

### `GET /auth/me` → `200`

```json
{ "id": "uuid", "name": "Motorista Demo", "email": "demo@autocare.dev", "createdAt": "2026-08-12T12:00:00Z" }
```

## Vehicles

| Method | Path | Result |
|---|---|---|
| `GET` | `/vehicles` | `200` — array, newest first |
| `POST` | `/vehicles` | `201` |
| `GET` | `/vehicles/{vehicleId}` | `200` |
| `PUT` | `/vehicles/{vehicleId}` | `200` |
| `DELETE` | `/vehicles/{vehicleId}` | `204` — cascades to all child records |
| `GET` | `/vehicles/{vehicleId}/analytics` | `200` — see [Analytics](#analytics) |

Request body:

```json
{
  "brand": "Fiat",
  "model": "Argo",
  "manufacturingYear": 2021,
  "modelYear": 2022,
  "licensePlate": "ABC1D23",
  "currentMileage": 45000,
  "fuelType": "FLEX",
  "nickname": "Carro da família"
}
```

`modelYear`, `licensePlate` and `nickname` are optional (send `null` to omit).
`fuelType` is one of `GASOLINE`, `ETHANOL`, `FLEX`, `DIESEL`, `HYBRID`,
`ELECTRIC`.

Response adds `id`, `displayName` (the nickname when set, otherwise
brand + model), `createdAt` and `updatedAt`.

Business rules — all `422 BUSINESS_RULE_VIOLATION`:

- The plate must match the Brazilian format `ABC1D23` or `ABC1234`; spaces and
  hyphens are stripped and it is upper-cased before validation
- The plate must be unique per user
- `manufacturingYear` cannot be later than next year
- `modelYear` cannot be earlier than `manufacturingYear`
- On update, `currentMileage` cannot be lower than the highest odometer reading
  already recorded for the vehicle

## Maintenance records

| Method | Path | Result |
|---|---|---|
| `POST` | `/vehicles/{vehicleId}/maintenance-records` | `201` |
| `GET` | `/vehicles/{vehicleId}/maintenance-records?page=0&size=20` | `200` — paginated |
| `GET` | `/maintenance-records/{recordId}` | `200` |
| `PUT` | `/maintenance-records/{recordId}` | `200` |
| `DELETE` | `/maintenance-records/{recordId}` | `204` |

```json
{
  "category": "OIL_CHANGE",
  "title": "Troca de óleo e filtro",
  "description": "Óleo 5W30 sintético",
  "serviceDate": "2026-07-15",
  "mileageAtService": 44000,
  "cost": 320.00,
  "workshop": "Oficina do Zé",
  "nextServiceDate": "2027-01-15",
  "nextServiceMileage": 54000
}
```

`category` is one of `OIL_CHANGE`, `FILTERS`, `BRAKES`, `TIRES`, `ENGINE`,
`TRANSMISSION`, `SUSPENSION`, `ELECTRICAL`, `BATTERY`, `COOLING`, `INSPECTION`,
`BODYWORK`, `OTHER`.

`serviceDate` must not be in the future. `cost` may be `0` (warranty work) but
never negative. Business rules: `nextServiceDate` must be after `serviceDate`,
and `nextServiceMileage` must exceed `mileageAtService`.

Saving a record whose `mileageAtService` is higher than the vehicle's current
mileage advances the vehicle's odometer.

## Fuel entries

| Method | Path | Result |
|---|---|---|
| `POST` | `/vehicles/{vehicleId}/fuel-entries` | `201` |
| `GET` | `/vehicles/{vehicleId}/fuel-entries?page=0&size=20` | `200` — paginated |
| `GET` | `/fuel-entries/{entryId}` | `200` |
| `PUT` | `/fuel-entries/{entryId}` | `200` |
| `DELETE` | `/fuel-entries/{entryId}` | `204` |

```json
{
  "refuelDate": "2026-08-10",
  "odometer": 45000,
  "liters": 41.567,
  "totalCost": 280.50,
  "fullTank": true
}
```

`refuelDate` must not be in the future, `liters` ≥ 0.001, `totalCost` ≥ 0.01.
`fullTank` defaults to `false` when omitted; only full-tank entries feed the
average-consumption calculation. The response adds the derived `pricePerLiter`.
A higher `odometer` advances the vehicle's mileage.

## Reminders

| Method | Path | Result |
|---|---|---|
| `POST` | `/vehicles/{vehicleId}/reminders` | `201` |
| `GET` | `/vehicles/{vehicleId}/reminders` | `200` — array, not paginated |
| `GET` | `/reminders/{reminderId}` | `200` |
| `PUT` | `/reminders/{reminderId}` | `200` |
| `POST` | `/reminders/{reminderId}/complete` | `200` |
| `POST` | `/reminders/{reminderId}/reopen` | `200` |
| `DELETE` | `/reminders/{reminderId}` | `204` |

```json
{
  "title": "Renovar seguro",
  "description": null,
  "dueDate": "2026-09-01",
  "dueMileage": null
}
```

At least one of `dueDate` or `dueMileage` is required
(`422 BUSINESS_RULE_VIOLATION`). The response adds `status` (`ACTIVE` or
`COMPLETED`), a computed `overdue` flag — true when the due date has passed or
the vehicle has reached the due mileage — and `completedAt`.

## Dashboard

### `GET /dashboard` → `200`

Cross-vehicle summary for the authenticated user:

```json
{
  "vehicleCount": 2,
  "maintenanceTotal": 1200.00,
  "fuelTotal": 2400.00,
  "combinedTotal": 3600.00,
  "monthlyExpenses": [{ "month": "2026-08", "maintenance": 600.00, "fuel": 1200.00, "total": 1800.00 }],
  "expensesByCategory": [{ "category": "OIL_CHANGE", "total": 600.00 }],
  "overdueReminderCount": 1,
  "upcomingReminders": [
    { "id": "uuid", "vehicleId": "uuid", "vehicleName": "Carro da família",
      "title": "Renovar seguro", "dueDate": "2026-09-01", "dueMileage": null, "overdue": true }
  ],
  "recentActivity": [
    { "type": "FUEL", "vehicleId": "uuid", "vehicleName": "Carro da família",
      "title": "Abastecimento", "date": "2026-08-10", "amount": 280.50 }
  ]
}
```

`type` is `MAINTENANCE` or `FUEL`.

## Analytics

### `GET /vehicles/{vehicleId}/analytics` → `200`

Computed by the Python service. Any metric can be `null` when there is not
enough data, with a matching `warnings` entry explaining why:

```json
{
  "totals": { "maintenanceCost": 1200.00, "fuelCost": 2400.00, "operatingCost": 3600.00 },
  "monthlyCosts": [{ "month": "2026-07", "maintenanceCost": 600.00, "fuelCost": 1200.00, "total": 1800.00 }],
  "costByCategory": [{ "category": "OIL_CHANGE", "total": 600.00, "percentage": 50.00 }],
  "fuelStats": {
    "totalLiters": 300.000,
    "averagePricePerLiter": 6.20,
    "averageConsumptionKmPerLiter": 11.44,
    "costPerKm": 0.62
  },
  "trend": { "direction": "STABLE", "currentMonthTotal": 900.00, "previousThreeMonthAverage": 880.00 },
  "periodComparison": {
    "periodDays": 90,
    "currentPeriodTotal": 2000.00,
    "previousPeriodTotal": 1800.00,
    "changePercentage": 11.10
  },
  "upcomingMaintenance": [
    { "title": "Revisão dos 50.000 km", "nextServiceDate": "2026-09-01",
      "nextServiceMileage": 50000, "status": "DUE_SOON" }
  ],
  "warnings": [{ "code": "INSUFFICIENT_FUEL_DATA", "message": "Fuel efficiency needs at least 3 full-tank refuelings" }]
}
```

`trend.direction` is `UP`, `DOWN` or `STABLE`. `upcomingMaintenance[].status` is
`OVERDUE`, `DUE_SOON` or `SCHEDULED`. Warning codes: `NO_DATA`,
`INSUFFICIENT_FUEL_DATA`, `INSUFFICIENT_DISTANCE_DATA`.

Returns `503 ANALYTICS_UNAVAILABLE` when the analytics service cannot be
reached. Vehicle data is unaffected — the SPA keeps the rest of the page usable.

## Pagination

Paginated endpoints accept `page` (0-based) and `size`, and respond with:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

## Errors

Every error shares one contract:

```json
{
  "timestamp": "2026-08-12T12:00:00Z",
  "status": 422,
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "A vehicle with this license plate is already registered",
  "path": "/api/v1/vehicles",
  "fieldErrors": [{ "field": "brand", "message": "must not be blank" }],
  "traceId": "1be5e52a"
}
```

`fieldErrors` is populated for validation failures. `traceId` correlates with the
server log; stack traces are never returned.

| Status | Code | Meaning |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Bean Validation rejected the body |
| `400` | `MALFORMED_REQUEST` | Unparseable JSON |
| `400` | `INVALID_PARAMETER` | Bad path or query parameter |
| `401` | `UNAUTHORIZED` | Missing or invalid access token |
| `401` | `INVALID_CREDENTIALS` | Wrong e-mail or password |
| `401` | `INVALID_REFRESH_TOKEN` | Refresh cookie missing, expired or reused |
| `403` | `FORBIDDEN` | Authenticated but not allowed |
| `404` | `RESOURCE_NOT_FOUND` | Absent — or owned by another user |
| `405` | `METHOD_NOT_ALLOWED` | Wrong HTTP method |
| `409` | `EMAIL_ALREADY_IN_USE` | Address already registered |
| `409` | `DATA_INTEGRITY_VIOLATION` | Database constraint violated |
| `422` | `BUSINESS_RULE_VIOLATION` | A domain rule rejected the request |
| `503` | `ANALYTICS_UNAVAILABLE` | Analytics service unreachable |
| `500` | `INTERNAL_ERROR` | Unexpected failure |

## Example session

```bash
# Register, keeping the refresh cookie in a jar
curl -sc jar.txt -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Motorista","email":"a@b.dev","password":"segredo123"}'

TOKEN=... # accessToken from the response

# Create a vehicle
curl -s -X POST http://localhost:8080/api/v1/vehicles \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"brand":"Fiat","model":"Argo","manufacturingYear":2021,"modelYear":null,
       "licensePlate":"ABC1D23","currentMileage":45000,"fuelType":"FLEX","nickname":null}'

# Renew the access token using the cookie jar
curl -sb jar.txt -c jar.txt -X POST http://localhost:8080/api/v1/auth/refresh
```
