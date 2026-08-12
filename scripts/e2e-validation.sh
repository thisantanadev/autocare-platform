#!/usr/bin/env bash
#
# End-to-end validation of the AutoCare API against a running stack.
# Prints every step with the real HTTP status code and a PASS/FAIL verdict,
# and exits non-zero if any check fails.
#
# Covers the full user journey (register, login, vehicle, maintenance, fuel,
# reminders, dashboard, analytics), the validation and business rules, refresh
# token rotation with reuse detection, and cross-user data isolation.
#
# Prerequisites — PostgreSQL, the analytics service and the backend running:
#
#   psql -U postgres -c "CREATE ROLE autocare LOGIN PASSWORD 'autocare';"
#   psql -U postgres -c "CREATE DATABASE autocare OWNER autocare;"
#
#   cd analytics && ANALYTICS_INTERNAL_TOKEN=dev-only-internal-token \
#     uv run uvicorn app.main:app --port 8000
#
#   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
#
# Then:  bash scripts/e2e-validation.sh
#
# Needs curl and node. Each run registers fresh accounts with a timestamped
# e-mail, so it is safe to run repeatedly against the same database.
#
# Override the target with API_BASE, e.g. against the Docker stack:
#   API_BASE=http://localhost:8081/api/v1 bash scripts/e2e-validation.sh
#
set -uo pipefail

API="${API_BASE:-http://localhost:8080/api/v1}"
TMP="$(mktemp -d)"
RUN="$(date +%s)"
EMAIL_A="alice+${RUN}@autocare.dev"
EMAIL_B="bruno+${RUN}@autocare.dev"
PASSWORD="SenhaSegura123"

PASS=0
FAIL=0
declare -a ROWS

jget() {
  node -e '
const fs=require("fs");
let d;
try { d = JSON.parse(fs.readFileSync(process.argv[1],"utf8")); } catch { process.exit(0); }
let v=d;
for (const p of process.argv[2].split(".")) v = v?.[p];
console.log(v===undefined||v===null?"":v);
' "$1" "$2"
}

# check <label> <expected-status> <actual-status> [detail]
check() {
  local label="$1" expected="$2" actual="$3" detail="${4:-}"
  if [ "$actual" = "$expected" ]; then
    PASS=$((PASS + 1))
    printf '  PASS  %-52s %s\n' "$label" "$actual"
    ROWS+=("PASS|$label|$expected|$actual|$detail")
  else
    FAIL=$((FAIL + 1))
    printf '  FAIL  %-52s got %s, expected %s\n' "$label" "$actual" "$expected"
    ROWS+=("FAIL|$label|$expected|$actual|$detail")
  fi
}

# api <out-file> <method> <url> [curl args...] -> prints status code
api() {
  local out="$1" method="$2" url="$3"
  shift 3
  curl -s -o "$out" -w '%{http_code}' -X "$method" "$url" "$@"
}

echo "======================================================================"
echo " AutoCare end-to-end validation — real PostgreSQL 18"
echo " run id: $RUN"
echo "======================================================================"

echo
echo "-- 1. Register user A -------------------------------------------------"
ST=$(api "$TMP/reg_a.json" POST "$API/auth/register" \
  -H 'Content-Type: application/json' -c "$TMP/jar_a.txt" \
  -d "{\"name\":\"Alice Motorista\",\"email\":\"$EMAIL_A\",\"password\":\"$PASSWORD\"}")
check "POST /auth/register (user A)" 201 "$ST"
TOKEN_A=$(jget "$TMP/reg_a.json" accessToken)
USER_A=$(jget "$TMP/reg_a.json" user.id)
echo "        user id: $USER_A"
echo "        token type: $(jget "$TMP/reg_a.json" tokenType), expires in $(jget "$TMP/reg_a.json" expiresInSeconds)s"
echo "        refresh cookie set: $(grep -c autocare_refresh "$TMP/jar_a.txt" || true)"

echo
echo "-- 2. Duplicate registration is rejected ------------------------------"
ST=$(api "$TMP/dup.json" POST "$API/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Alice Again\",\"email\":\"$EMAIL_A\",\"password\":\"$PASSWORD\"}")
check "POST /auth/register (duplicate e-mail)" 409 "$ST" "$(jget "$TMP/dup.json" code)"

echo
echo "-- 3. Login ----------------------------------------------------------"
ST=$(api "$TMP/login.json" POST "$API/auth/login" \
  -H 'Content-Type: application/json' -c "$TMP/jar_a.txt" \
  -d "{\"email\":\"$EMAIL_A\",\"password\":\"$PASSWORD\"}")
check "POST /auth/login (correct password)" 200 "$ST"
TOKEN_A=$(jget "$TMP/login.json" accessToken)

ST=$(api "$TMP/badlogin.json" POST "$API/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL_A\",\"password\":\"senha-errada\"}")
check "POST /auth/login (wrong password)" 401 "$ST" "$(jget "$TMP/badlogin.json" code)"

echo
echo "-- 4. Identity and unauthenticated access ----------------------------"
ST=$(api "$TMP/me.json" GET "$API/auth/me" -H "Authorization: Bearer $TOKEN_A")
check "GET /auth/me (authenticated)" 200 "$ST" "$(jget "$TMP/me.json" email)"

ST=$(api "$TMP/noauth.json" GET "$API/vehicles")
check "GET /vehicles (no token)" 401 "$ST" "$(jget "$TMP/noauth.json" code)"

ST=$(api "$TMP/badtok.json" GET "$API/vehicles" -H "Authorization: Bearer not-a-real-token")
check "GET /vehicles (malformed token)" 401 "$ST"

echo
echo "-- 5. Create vehicle -------------------------------------------------"
ST=$(api "$TMP/veh.json" POST "$API/vehicles" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"brand":"Fiat","model":"Argo","manufacturingYear":2021,"modelYear":2022,
       "licensePlate":"abc-1d23","currentMileage":45000,"fuelType":"FLEX","nickname":"Carro da familia"}')
check "POST /vehicles" 201 "$ST"
VEHICLE_A=$(jget "$TMP/veh.json" id)
echo "        vehicle id: $VEHICLE_A"
echo "        plate normalized to: $(jget "$TMP/veh.json" licensePlate)  (sent 'abc-1d23')"
echo "        displayName: $(jget "$TMP/veh.json" displayName)"

echo
echo "-- 6. Vehicle validation and business rules --------------------------"
ST=$(api "$TMP/badplate.json" POST "$API/vehicles" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"brand":"VW","model":"Gol","manufacturingYear":2020,"modelYear":null,
       "licensePlate":"AB123","currentMileage":1000,"fuelType":"FLEX","nickname":null}')
check "POST /vehicles (invalid plate format)" 422 "$ST" "$(jget "$TMP/badplate.json" code)"

ST=$(api "$TMP/duppl.json" POST "$API/vehicles" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"brand":"VW","model":"Gol","manufacturingYear":2020,"modelYear":null,
       "licensePlate":"ABC1D23","currentMileage":1000,"fuelType":"FLEX","nickname":null}')
check "POST /vehicles (duplicate plate)" 422 "$ST" "$(jget "$TMP/duppl.json" code)"

ST=$(api "$TMP/blank.json" POST "$API/vehicles" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"brand":"","model":"Gol","manufacturingYear":2020,"modelYear":null,
       "licensePlate":null,"currentMileage":1000,"fuelType":"FLEX","nickname":null}')
check "POST /vehicles (blank brand)" 400 "$ST" "$(jget "$TMP/blank.json" code)"

echo
echo "-- 7. Register maintenance -------------------------------------------"
ST=$(api "$TMP/maint.json" POST "$API/vehicles/$VEHICLE_A/maintenance-records" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"category":"OIL_CHANGE","title":"Troca de oleo e filtro","description":"Oleo 5W30 sintetico",
       "serviceDate":"2026-07-15","mileageAtService":46000,"cost":320.00,"workshop":"Oficina do Ze",
       "nextServiceDate":"2027-01-15","nextServiceMileage":56000}')
check "POST /vehicles/{id}/maintenance-records" 201 "$ST"
MAINT_A=$(jget "$TMP/maint.json" id)

ST=$(api "$TMP/badnext.json" POST "$API/vehicles/$VEHICLE_A/maintenance-records" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"category":"BRAKES","title":"Pastilhas","description":null,
       "serviceDate":"2026-07-20","mileageAtService":46500,"cost":500.00,"workshop":null,
       "nextServiceDate":"2026-07-01","nextServiceMileage":null}')
check "POST maintenance (nextServiceDate before service)" 422 "$ST" "$(jget "$TMP/badnext.json" code)"

echo "        vehicle mileage after 46000 km service: $(api "$TMP/v2.json" GET "$API/vehicles/$VEHICLE_A" -H "Authorization: Bearer $TOKEN_A" >/dev/null; jget "$TMP/v2.json" currentMileage) km (was 45000)"

echo
echo "-- 8. Register refuelings (3 full tanks for consumption) -------------"
i=0
for entry in \
  '{"refuelDate":"2026-07-20","odometer":46500,"liters":40.000,"totalCost":248.00,"fullTank":true}' \
  '{"refuelDate":"2026-07-28","odometer":47000,"liters":42.500,"totalCost":263.50,"fullTank":true}' \
  '{"refuelDate":"2026-08-05","odometer":47600,"liters":41.200,"totalCost":259.56,"fullTank":true}'
do
  i=$((i + 1))
  ST=$(api "$TMP/fuel_$i.json" POST "$API/vehicles/$VEHICLE_A/fuel-entries" \
    -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d "$entry")
  check "POST /vehicles/{id}/fuel-entries (#$i)" 201 "$ST" "R\$/L $(jget "$TMP/fuel_$i.json" pricePerLiter)"
done
FUEL_A=$(jget "$TMP/fuel_1.json" id)

ST=$(api "$TMP/badfuel.json" POST "$API/vehicles/$VEHICLE_A/fuel-entries" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"refuelDate":"2027-01-01","odometer":48000,"liters":40.0,"totalCost":250.0,"fullTank":true}')
check "POST fuel-entries (future date)" 400 "$ST" "$(jget "$TMP/badfuel.json" code)"

echo
echo "-- 9. Create reminders -----------------------------------------------"
ST=$(api "$TMP/rem.json" POST "$API/vehicles/$VEHICLE_A/reminders" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"title":"Renovar seguro","description":null,"dueDate":"2026-09-01","dueMileage":null}')
check "POST /vehicles/{id}/reminders (date)" 201 "$ST"
REM_A=$(jget "$TMP/rem.json" id)

ST=$(api "$TMP/rem2.json" POST "$API/vehicles/$VEHICLE_A/reminders" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"title":"Trocar correia dentada","description":null,"dueDate":null,"dueMileage":48000}')
check "POST /vehicles/{id}/reminders (mileage)" 201 "$ST"

ST=$(api "$TMP/remover.json" POST "$API/vehicles/$VEHICLE_A/reminders" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"title":"Licenciamento atrasado","description":null,"dueDate":"2026-01-10","dueMileage":null}')
check "POST reminders (past date -> overdue)" 201 "$ST" "overdue=$(jget "$TMP/remover.json" overdue)"

ST=$(api "$TMP/remnone.json" POST "$API/vehicles/$VEHICLE_A/reminders" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d '{"title":"Sem alvo","description":null,"dueDate":null,"dueMileage":null}')
check "POST reminders (no date and no mileage)" 422 "$ST" "$(jget "$TMP/remnone.json" code)"

ST=$(api "$TMP/remdone.json" POST "$API/reminders/$REM_A/complete" -H "Authorization: Bearer $TOKEN_A")
check "POST /reminders/{id}/complete" 200 "$ST" "status=$(jget "$TMP/remdone.json" status)"
ST=$(api "$TMP/remopen.json" POST "$API/reminders/$REM_A/reopen" -H "Authorization: Bearer $TOKEN_A")
check "POST /reminders/{id}/reopen" 200 "$ST" "status=$(jget "$TMP/remopen.json" status)"

echo
echo "-- 10. Dashboard -----------------------------------------------------"
ST=$(api "$TMP/dash.json" GET "$API/dashboard" -H "Authorization: Bearer $TOKEN_A")
check "GET /dashboard" 200 "$ST"
echo "        vehicleCount:         $(jget "$TMP/dash.json" vehicleCount)"
echo "        maintenanceTotal:     $(jget "$TMP/dash.json" maintenanceTotal)"
echo "        fuelTotal:            $(jget "$TMP/dash.json" fuelTotal)"
echo "        combinedTotal:        $(jget "$TMP/dash.json" combinedTotal)"
echo "        overdueReminderCount: $(jget "$TMP/dash.json" overdueReminderCount)"
node -e '
const d=require(process.argv[1]);
console.log("        monthlyExpenses:      "+JSON.stringify(d.monthlyExpenses));
console.log("        expensesByCategory:   "+JSON.stringify(d.expensesByCategory));
console.log("        recentActivity:       "+d.recentActivity.length+" items");
console.log("        upcomingReminders:    "+d.upcomingReminders.map(r=>r.title+(r.overdue?" (atrasado)":"")).join(", "));
' "$TMP/dash.json"

echo
echo "-- 11. Analytics (via the Python service) ----------------------------"
ST=$(api "$TMP/an.json" GET "$API/vehicles/$VEHICLE_A/analytics" -H "Authorization: Bearer $TOKEN_A")
check "GET /vehicles/{id}/analytics" 200 "$ST"
node -e '
const d=require(process.argv[1]);
console.log("        totals:            "+JSON.stringify(d.totals));
console.log("        fuelStats:         "+JSON.stringify(d.fuelStats));
console.log("        trend:             "+JSON.stringify(d.trend));
console.log("        periodComparison:  "+JSON.stringify(d.periodComparison));
console.log("        costByCategory:    "+JSON.stringify(d.costByCategory));
console.log("        upcoming:          "+JSON.stringify(d.upcomingMaintenance));
console.log("        warnings:          "+JSON.stringify(d.warnings));
' "$TMP/an.json"

echo
echo "-- 12. Pagination ----------------------------------------------------"
ST=$(api "$TMP/page.json" GET "$API/vehicles/$VEHICLE_A/fuel-entries?page=0&size=2" -H "Authorization: Bearer $TOKEN_A")
check "GET fuel-entries?page=0&size=2" 200 "$ST" "content=$(jget "$TMP/page.json" content.length) totalElements=$(jget "$TMP/page.json" totalElements) totalPages=$(jget "$TMP/page.json" totalPages)"

echo
echo "-- 13. Refresh token rotation ----------------------------------------"
cp "$TMP/jar_a.txt" "$TMP/jar_old.txt"
ST=$(api "$TMP/ref1.json" POST "$API/auth/refresh" -b "$TMP/jar_a.txt" -c "$TMP/jar_a.txt")
check "POST /auth/refresh (valid cookie)" 200 "$ST"
ST=$(api "$TMP/ref2.json" POST "$API/auth/refresh" -b "$TMP/jar_old.txt")
check "POST /auth/refresh (reuse rotated cookie)" 401 "$ST" "$(jget "$TMP/ref2.json" code)"

echo
echo "-- 14. Data isolation between users ----------------------------------"
ST=$(api "$TMP/reg_b.json" POST "$API/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Bruno Motorista\",\"email\":\"$EMAIL_B\",\"password\":\"$PASSWORD\"}")
check "POST /auth/register (user B)" 201 "$ST"
TOKEN_B=$(jget "$TMP/reg_b.json" accessToken)

ST=$(api "$TMP/b_list.json" GET "$API/vehicles" -H "Authorization: Bearer $TOKEN_B")
check "GET /vehicles as B (own garage only)" 200 "$ST" "vehicles=$(jget "$TMP/b_list.json" length)"

ST=$(api "$TMP/x1.json" GET "$API/vehicles/$VEHICLE_A" -H "Authorization: Bearer $TOKEN_B")
check "GET A's vehicle as B" 404 "$ST" "$(jget "$TMP/x1.json" code)"
ST=$(api "$TMP/x2.json" GET "$API/maintenance-records/$MAINT_A" -H "Authorization: Bearer $TOKEN_B")
check "GET A's maintenance record as B" 404 "$ST" "$(jget "$TMP/x2.json" code)"
ST=$(api "$TMP/x3.json" GET "$API/fuel-entries/$FUEL_A" -H "Authorization: Bearer $TOKEN_B")
check "GET A's fuel entry as B" 404 "$ST" "$(jget "$TMP/x3.json" code)"
ST=$(api "$TMP/x4.json" GET "$API/reminders/$REM_A" -H "Authorization: Bearer $TOKEN_B")
check "GET A's reminder as B" 404 "$ST" "$(jget "$TMP/x4.json" code)"
ST=$(api "$TMP/x5.json" GET "$API/vehicles/$VEHICLE_A/analytics" -H "Authorization: Bearer $TOKEN_B")
check "GET A's analytics as B" 404 "$ST" "$(jget "$TMP/x5.json" code)"

ST=$(api "$TMP/x6.json" PUT "$API/vehicles/$VEHICLE_A" \
  -H "Authorization: Bearer $TOKEN_B" -H 'Content-Type: application/json' \
  -d '{"brand":"Hackeado","model":"X","manufacturingYear":2020,"modelYear":null,
       "licensePlate":null,"currentMileage":1,"fuelType":"FLEX","nickname":null}')
check "PUT A's vehicle as B" 404 "$ST" "$(jget "$TMP/x6.json" code)"
ST=$(api "$TMP/x7.json" DELETE "$API/vehicles/$VEHICLE_A" -H "Authorization: Bearer $TOKEN_B")
check "DELETE A's vehicle as B" 404 "$ST" "$(jget "$TMP/x7.json" code)"
ST=$(api "$TMP/x8.json" DELETE "$API/reminders/$REM_A" -H "Authorization: Bearer $TOKEN_B")
check "DELETE A's reminder as B" 404 "$ST" "$(jget "$TMP/x8.json" code)"
ST=$(api "$TMP/x9.json" POST "$API/reminders/$REM_A/complete" -H "Authorization: Bearer $TOKEN_B")
check "COMPLETE A's reminder as B" 404 "$ST" "$(jget "$TMP/x9.json" code)"
ST=$(api "$TMP/x10.json" POST "$API/vehicles/$VEHICLE_A/maintenance-records" \
  -H "Authorization: Bearer $TOKEN_B" -H 'Content-Type: application/json' \
  -d '{"category":"OTHER","title":"Injetado","description":null,"serviceDate":"2026-07-01",
       "mileageAtService":1,"cost":1.00,"workshop":null,"nextServiceDate":null,"nextServiceMileage":null}')
check "POST maintenance on A's vehicle as B" 404 "$ST" "$(jget "$TMP/x10.json" code)"

echo "        B's dashboard totals: combined=$(api "$TMP/b_dash.json" GET "$API/dashboard" -H "Authorization: Bearer $TOKEN_B" >/dev/null; jget "$TMP/b_dash.json" combinedTotal), vehicles=$(jget "$TMP/b_dash.json" vehicleCount)"

echo
echo "-- 15. A's data is intact after B's attempts -------------------------"
ST=$(api "$TMP/final.json" GET "$API/vehicles/$VEHICLE_A" -H "Authorization: Bearer $TOKEN_A")
check "GET A's vehicle as A (still intact)" 200 "$ST" "brand=$(jget "$TMP/final.json" brand) plate=$(jget "$TMP/final.json" licensePlate)"

echo
echo "-- 16. Logout --------------------------------------------------------"
ST=$(api "$TMP/logout.json" POST "$API/auth/logout" -b "$TMP/jar_a.txt" -c "$TMP/jar_a.txt")
check "POST /auth/logout" 204 "$ST"
ST=$(api "$TMP/afterlogout.json" POST "$API/auth/refresh" -b "$TMP/jar_a.txt")
check "POST /auth/refresh (after logout)" 401 "$ST" "$(jget "$TMP/afterlogout.json" code)"

echo
echo "======================================================================"
echo " RESULT: $PASS passed, $FAIL failed"
echo "======================================================================"
if [ "$FAIL" -ne 0 ]; then
  echo
  echo "Failed checks:"
  printf '%s\n' "${ROWS[@]}" | awk -F'|' '$1=="FAIL" {printf "  %s: expected %s, got %s\n", $2, $3, $4}'
fi
rm -rf "$TMP"
[ "$FAIL" -eq 0 ]
