from datetime import date
from decimal import Decimal
from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, PlainSerializer
from pydantic.alias_generators import to_camel

# Money and ratio values are computed with Decimal for exactness and only
# converted to JSON numbers at the serialization boundary.
Money = Annotated[Decimal, PlainSerializer(float, return_type=float, when_used="json")]


class CamelModel(BaseModel):
    """Base model matching the camelCase wire contract of the Java backend."""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class VehicleInfo(CamelModel):
    id: UUID
    current_mileage: int = Field(ge=0)
    fuel_type: str
    manufacturing_year: int


class MaintenanceItem(CamelModel):
    category: str
    title: str
    service_date: date
    mileage_at_service: int = Field(ge=0)
    cost: Decimal = Field(ge=0)
    next_service_date: date | None = None
    next_service_mileage: int | None = Field(default=None, ge=0)


class FuelItem(CamelModel):
    refuel_date: date
    odometer: int = Field(ge=0)
    liters: Decimal = Field(gt=0)
    total_cost: Decimal = Field(gt=0)
    price_per_liter: Decimal = Field(gt=0)
    full_tank: bool = False


class VehicleAnalyticsRequest(CamelModel):
    vehicle: VehicleInfo
    reference_date: date
    maintenance_records: list[MaintenanceItem] = Field(default_factory=list)
    fuel_entries: list[FuelItem] = Field(default_factory=list)


class Totals(CamelModel):
    maintenance_cost: Money
    fuel_cost: Money
    operating_cost: Money


class MonthlyCost(CamelModel):
    month: str  # ISO year-month, e.g. "2026-08"
    maintenance_cost: Money
    fuel_cost: Money
    total: Money


class CategoryCost(CamelModel):
    category: str
    total: Money
    percentage: Money


class FuelStats(CamelModel):
    total_liters: Money
    average_price_per_liter: Money | None
    average_consumption_km_per_liter: Money | None
    cost_per_km: Money | None


class Trend(CamelModel):
    direction: Literal["UP", "DOWN", "STABLE"]
    current_month_total: Money
    previous_three_month_average: Money | None


class PeriodComparison(CamelModel):
    period_days: int
    current_period_total: Money
    previous_period_total: Money
    change_percentage: Money | None


class UpcomingItem(CamelModel):
    title: str
    next_service_date: date | None
    next_service_mileage: int | None
    status: Literal["OVERDUE", "DUE_SOON", "SCHEDULED"]


class WarningItem(CamelModel):
    code: str
    message: str


class AnalyticsReport(CamelModel):
    totals: Totals
    monthly_costs: list[MonthlyCost]
    cost_by_category: list[CategoryCost]
    fuel_stats: FuelStats
    trend: Trend
    period_comparison: PeriodComparison
    upcoming_maintenance: list[UpcomingItem]
    warnings: list[WarningItem]
