"""Builders for realistic (entirely fictional) test data."""

from datetime import date
from decimal import ROUND_HALF_UP, Decimal
from uuid import uuid4

from app.models import FuelItem, MaintenanceItem, VehicleAnalyticsRequest, VehicleInfo


def vehicle(current_mileage: int = 48000) -> VehicleInfo:
    return VehicleInfo(
        id=uuid4(), current_mileage=current_mileage, fuel_type="FLEX", manufacturing_year=2021
    )


def maintenance_item(
    cost: str = "100.00",
    service_date: date = date(2026, 7, 15),
    category: str = "OIL_CHANGE",
    title: str = "Oil change",
    mileage_at_service: int = 47000,
    next_service_date: date | None = None,
    next_service_mileage: int | None = None,
) -> MaintenanceItem:
    return MaintenanceItem(
        category=category,
        title=title,
        service_date=service_date,
        mileage_at_service=mileage_at_service,
        cost=Decimal(cost),
        next_service_date=next_service_date,
        next_service_mileage=next_service_mileage,
    )


def fuel_item(
    total_cost: str = "250.00",
    liters: str = "40.000",
    refuel_date: date = date(2026, 8, 1),
    odometer: int = 47500,
    full_tank: bool = True,
) -> FuelItem:
    liters_value = Decimal(liters)
    total_value = Decimal(total_cost)
    return FuelItem(
        refuel_date=refuel_date,
        odometer=odometer,
        liters=liters_value,
        total_cost=total_value,
        price_per_liter=(total_value / liters_value).quantize(
            Decimal("0.001"), rounding=ROUND_HALF_UP
        ),
        full_tank=full_tank,
    )


def request(
    maintenance: list[MaintenanceItem] | None = None,
    fuel: list[FuelItem] | None = None,
    reference_date: date = date(2026, 8, 11),
    current_mileage: int = 48000,
) -> VehicleAnalyticsRequest:
    return VehicleAnalyticsRequest(
        vehicle=vehicle(current_mileage),
        reference_date=reference_date,
        maintenance_records=maintenance or [],
        fuel_entries=fuel or [],
    )
