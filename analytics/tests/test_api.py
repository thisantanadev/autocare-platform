from uuid import uuid4

from app.config import settings

from .conftest import TEST_TOKEN

VALID_PAYLOAD = {
    "vehicle": {
        "id": str(uuid4()),
        "currentMileage": 48000,
        "fuelType": "FLEX",
        "manufacturingYear": 2021,
    },
    "referenceDate": "2026-08-11",
    "maintenanceRecords": [
        {
            "category": "OIL_CHANGE",
            "title": "Troca de óleo",
            "serviceDate": "2026-07-15",
            "mileageAtService": 47000,
            "cost": 289.90,
            "nextServiceDate": "2026-12-15",
            "nextServiceMileage": 57000,
        }
    ],
    "fuelEntries": [
        {
            "refuelDate": "2026-08-01",
            "odometer": 47500,
            "liters": 40.0,
            "totalCost": 250.00,
            "pricePerLiter": 6.25,
            "fullTank": True,
        }
    ],
}

ENDPOINT = "/internal/v1/analytics/vehicle"


def test_health_needs_no_authentication(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_rejects_requests_without_token(client):
    assert client.post(ENDPOINT, json=VALID_PAYLOAD).status_code == 401


def test_rejects_requests_with_wrong_token(client):
    response = client.post(
        ENDPOINT, json=VALID_PAYLOAD, headers={"X-Internal-Token": "wrong-token"}
    )
    assert response.status_code == 401


def test_fails_closed_when_token_is_not_configured(client):
    settings.analytics_internal_token = ""
    response = client.post(ENDPOINT, json=VALID_PAYLOAD, headers={"X-Internal-Token": ""})
    assert response.status_code == 401


def test_analyzes_vehicle_with_camel_case_contract(client):
    response = client.post(
        ENDPOINT, json=VALID_PAYLOAD, headers={"X-Internal-Token": TEST_TOKEN}
    )
    assert response.status_code == 200
    body = response.json()
    # camelCase keys and plain JSON numbers, as the Java backend expects.
    assert body["totals"]["operatingCost"] == 539.9
    assert body["totals"]["maintenanceCost"] == 289.9
    assert body["fuelStats"]["averagePricePerLiter"] == 6.25
    assert body["periodComparison"]["periodDays"] == 90
    assert body["upcomingMaintenance"][0]["status"] == "SCHEDULED"
    assert isinstance(body["monthlyCosts"], list)


def test_rejects_invalid_payload(client):
    payload = {**VALID_PAYLOAD, "fuelEntries": [{"refuelDate": "2026-08-01", "odometer": -5}]}
    response = client.post(ENDPOINT, json=payload, headers={"X-Internal-Token": TEST_TOKEN})
    assert response.status_code == 422
