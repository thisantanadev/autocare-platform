"""Deterministic analytics calculations.

Every function here is pure: results depend only on the payload and the
reference date sent by the backend, which keeps responses reproducible and
easy to unit test. There is no artificial intelligence involved — only
arithmetic with documented assumptions.
"""

from collections import defaultdict
from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal

from .models import (
    AnalyticsReport,
    CategoryCost,
    FuelItem,
    FuelStats,
    MaintenanceItem,
    MonthlyCost,
    PeriodComparison,
    Totals,
    Trend,
    UpcomingItem,
    VehicleAnalyticsRequest,
    WarningItem,
)

TWO_PLACES = Decimal("0.01")
THREE_PLACES = Decimal("0.001")
ONE_PLACE = Decimal("0.1")

MONTHS_IN_REPORT = 12
COMPARISON_PERIOD_DAYS = 90
DUE_SOON_DAYS = 30
DUE_SOON_KM = 1000
# Full-to-full consumption needs at least 2 intervals (3 full-tank fills).
MIN_FULL_TANK_INTERVALS = 2
# Cost per km over a very short distance would be meaningless noise.
MIN_DISTANCE_KM = 100
# Spending within ±10% of the recent average counts as stable.
TREND_TOLERANCE = Decimal("0.10")


def build_report(request: VehicleAnalyticsRequest) -> AnalyticsReport:
    maintenance = request.maintenance_records
    fuel = request.fuel_entries
    today = request.reference_date

    warnings: list[WarningItem] = []
    if not maintenance and not fuel:
        warnings.append(
            WarningItem(code="NO_DATA", message="No maintenance or fuel records registered yet")
        )

    fuel_stats, fuel_warnings = compute_fuel_stats(maintenance, fuel)
    warnings.extend(fuel_warnings)

    return AnalyticsReport(
        totals=compute_totals(maintenance, fuel),
        monthly_costs=compute_monthly_costs(maintenance, fuel, today),
        cost_by_category=compute_cost_by_category(maintenance),
        fuel_stats=fuel_stats,
        trend=compute_trend(maintenance, fuel, today),
        period_comparison=compute_period_comparison(maintenance, fuel, today),
        upcoming_maintenance=compute_upcoming_maintenance(
            maintenance, request.vehicle.current_mileage, today
        ),
        warnings=warnings,
    )


def compute_totals(maintenance: list[MaintenanceItem], fuel: list[FuelItem]) -> Totals:
    maintenance_cost = _quantize2(sum((m.cost for m in maintenance), Decimal(0)))
    fuel_cost = _quantize2(sum((f.total_cost for f in fuel), Decimal(0)))
    return Totals(
        maintenance_cost=maintenance_cost,
        fuel_cost=fuel_cost,
        operating_cost=_quantize2(maintenance_cost + fuel_cost),
    )


def compute_monthly_costs(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem], reference_date: date
) -> list[MonthlyCost]:
    """Costs per calendar month for the last 12 months ending at the reference
    month. Months older than the first record are omitted; empty months in
    between appear with zero totals so charts have no gaps."""
    all_dates = [m.service_date for m in maintenance] + [f.refuel_date for f in fuel]
    if not all_dates:
        return []
    earliest = min(all_dates)

    maintenance_by_month: dict[str, Decimal] = defaultdict(lambda: Decimal(0))
    for record in maintenance:
        maintenance_by_month[_month_key(record.service_date)] += record.cost
    fuel_by_month: dict[str, Decimal] = defaultdict(lambda: Decimal(0))
    for entry in fuel:
        fuel_by_month[_month_key(entry.refuel_date)] += entry.total_cost

    months: list[MonthlyCost] = []
    for offset in range(MONTHS_IN_REPORT - 1, -1, -1):
        year, month = _shift_month(reference_date.year, reference_date.month, -offset)
        if (year, month) < (earliest.year, earliest.month):
            continue
        key = f"{year:04d}-{month:02d}"
        maintenance_total = _quantize2(maintenance_by_month.get(key, Decimal(0)))
        fuel_total = _quantize2(fuel_by_month.get(key, Decimal(0)))
        months.append(
            MonthlyCost(
                month=key,
                maintenance_cost=maintenance_total,
                fuel_cost=fuel_total,
                total=_quantize2(maintenance_total + fuel_total),
            )
        )
    return months


def compute_cost_by_category(maintenance: list[MaintenanceItem]) -> list[CategoryCost]:
    """Maintenance spending per category, with each share as a percentage of
    the maintenance total (not of the combined operating cost)."""
    totals: dict[str, Decimal] = defaultdict(lambda: Decimal(0))
    for record in maintenance:
        totals[record.category] += record.cost
    grand_total = sum(totals.values(), Decimal(0))
    result = []
    for category, total in sorted(totals.items(), key=lambda item: item[1], reverse=True):
        percentage = (
            (total * 100 / grand_total).quantize(ONE_PLACE, rounding=ROUND_HALF_UP)
            if grand_total > 0
            else Decimal(0)
        )
        result.append(
            CategoryCost(category=category, total=_quantize2(total), percentage=percentage)
        )
    return result


def compute_fuel_stats(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem]
) -> tuple[FuelStats, list[WarningItem]]:
    warnings: list[WarningItem] = []

    total_liters = sum((f.liters for f in fuel), Decimal(0))
    total_fuel_cost = sum((f.total_cost for f in fuel), Decimal(0))
    average_price = (
        (total_fuel_cost / total_liters).quantize(THREE_PLACES, rounding=ROUND_HALF_UP)
        if total_liters > 0
        else None
    )

    consumption = _full_to_full_consumption(fuel)
    if consumption is None and fuel:
        warnings.append(
            WarningItem(
                code="INSUFFICIENT_FUEL_DATA",
                message="Fuel efficiency needs at least 3 full-tank refuelings "
                "covering a positive distance",
            )
        )

    cost_per_km = _cost_per_km(maintenance, fuel)
    if cost_per_km is None and (maintenance or fuel):
        warnings.append(
            WarningItem(
                code="INSUFFICIENT_DISTANCE_DATA",
                message=f"Cost per km needs odometer readings spanning at least "
                f"{MIN_DISTANCE_KM} km",
            )
        )

    return (
        FuelStats(
            total_liters=total_liters.quantize(THREE_PLACES, rounding=ROUND_HALF_UP),
            average_price_per_liter=average_price,
            average_consumption_km_per_liter=consumption,
            cost_per_km=cost_per_km,
        ),
        warnings,
    )


def _full_to_full_consumption(fuel: list[FuelItem]) -> Decimal | None:
    """Full-to-full method: distance between consecutive full-tank fills
    divided by all fuel added in between (including the closing fill).
    Partial fills between two full tanks are counted as consumed fuel, so
    they do not distort the estimate."""
    entries = sorted(fuel, key=lambda f: f.odometer)
    previous_full_odometer: int | None = None
    pending_liters = Decimal(0)
    total_distance = Decimal(0)
    total_liters_used = Decimal(0)
    intervals = 0

    for entry in entries:
        if previous_full_odometer is None:
            if entry.full_tank:
                previous_full_odometer = entry.odometer
            continue
        pending_liters += entry.liters
        if entry.full_tank:
            distance = entry.odometer - previous_full_odometer
            if distance > 0:
                total_distance += distance
                total_liters_used += pending_liters
                intervals += 1
            previous_full_odometer = entry.odometer
            pending_liters = Decimal(0)

    if intervals < MIN_FULL_TANK_INTERVALS or total_liters_used <= 0:
        return None
    return (total_distance / total_liters_used).quantize(TWO_PLACES, rounding=ROUND_HALF_UP)


def _cost_per_km(maintenance: list[MaintenanceItem], fuel: list[FuelItem]) -> Decimal | None:
    """Total operating cost divided by the odometer span covered by the
    records. Assumes the registered history covers the spending period."""
    readings = [f.odometer for f in fuel] + [m.mileage_at_service for m in maintenance]
    if len(readings) < 2:
        return None
    distance = max(readings) - min(readings)
    if distance < MIN_DISTANCE_KM:
        return None
    total_cost = sum((m.cost for m in maintenance), Decimal(0)) + sum(
        (f.total_cost for f in fuel), Decimal(0)
    )
    return (total_cost / distance).quantize(TWO_PLACES, rounding=ROUND_HALF_UP)


def compute_trend(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem], reference_date: date
) -> Trend:
    """Compares the current calendar month with the average of the three
    previous calendar months. Spending within ±10% of that average counts
    as stable."""
    current_key = _month_key(reference_date)
    current_total = _total_for_month(maintenance, fuel, current_key)

    previous_sum = Decimal(0)
    for offset in range(1, 4):
        year, month = _shift_month(reference_date.year, reference_date.month, -offset)
        previous_sum += _total_for_month(maintenance, fuel, f"{year:04d}-{month:02d}")
    average = _quantize2(previous_sum / 3)

    if average == 0:
        direction = "UP" if current_total > 0 else "STABLE"
    else:
        ratio = current_total / average
        if ratio > 1 + TREND_TOLERANCE:
            direction = "UP"
        elif ratio < 1 - TREND_TOLERANCE:
            direction = "DOWN"
        else:
            direction = "STABLE"

    return Trend(
        direction=direction,
        current_month_total=_quantize2(current_total),
        previous_three_month_average=average,
    )


def compute_period_comparison(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem], reference_date: date
) -> PeriodComparison:
    """Last 90 days (inclusive of the reference date) against the 90 days
    immediately before. The percentage is omitted when the previous period
    had no spending, since division by zero has no meaningful answer."""
    current_start = reference_date - timedelta(days=COMPARISON_PERIOD_DAYS - 1)
    previous_start = current_start - timedelta(days=COMPARISON_PERIOD_DAYS)
    previous_end = current_start - timedelta(days=1)

    current_total = _total_between(maintenance, fuel, current_start, reference_date)
    previous_total = _total_between(maintenance, fuel, previous_start, previous_end)

    change = None
    if previous_total > 0:
        change = ((current_total - previous_total) * 100 / previous_total).quantize(
            ONE_PLACE, rounding=ROUND_HALF_UP
        )

    return PeriodComparison(
        period_days=COMPARISON_PERIOD_DAYS,
        current_period_total=_quantize2(current_total),
        previous_period_total=_quantize2(previous_total),
        change_percentage=change,
    )


def compute_upcoming_maintenance(
    maintenance: list[MaintenanceItem], current_mileage: int, reference_date: date
) -> list[UpcomingItem]:
    """Every record that declared a next service date or mileage is scored:
    OVERDUE when the date passed or the odometer crossed the target,
    DUE_SOON within 30 days or 1000 km, SCHEDULED otherwise."""
    rank = {"OVERDUE": 0, "DUE_SOON": 1, "SCHEDULED": 2}
    items = []
    for record in maintenance:
        if record.next_service_date is None and record.next_service_mileage is None:
            continue
        status = _next_service_status(record, current_mileage, reference_date)
        items.append(
            UpcomingItem(
                title=record.title,
                next_service_date=record.next_service_date,
                next_service_mileage=record.next_service_mileage,
                status=status,
            )
        )
    return sorted(
        items,
        key=lambda item: (rank[item.status], item.next_service_date or date.max),
    )


def _next_service_status(
    record: MaintenanceItem, current_mileage: int, reference_date: date
) -> str:
    date_target = record.next_service_date
    mileage_target = record.next_service_mileage
    if (date_target is not None and date_target <= reference_date) or (
        mileage_target is not None and mileage_target <= current_mileage
    ):
        return "OVERDUE"
    due_soon_date = reference_date + timedelta(days=DUE_SOON_DAYS)
    if (date_target is not None and date_target <= due_soon_date) or (
        mileage_target is not None and mileage_target <= current_mileage + DUE_SOON_KM
    ):
        return "DUE_SOON"
    return "SCHEDULED"


def _total_for_month(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem], month_key: str
) -> Decimal:
    total = sum(
        (m.cost for m in maintenance if _month_key(m.service_date) == month_key), Decimal(0)
    )
    total += sum((f.total_cost for f in fuel if _month_key(f.refuel_date) == month_key), Decimal(0))
    return total


def _total_between(
    maintenance: list[MaintenanceItem], fuel: list[FuelItem], start: date, end: date
) -> Decimal:
    total = sum((m.cost for m in maintenance if start <= m.service_date <= end), Decimal(0))
    total += sum((f.total_cost for f in fuel if start <= f.refuel_date <= end), Decimal(0))
    return total


def _month_key(value: date) -> str:
    return f"{value.year:04d}-{value.month:02d}"


def _shift_month(year: int, month: int, delta: int) -> tuple[int, int]:
    index = year * 12 + (month - 1) + delta
    return index // 12, index % 12 + 1


def _quantize2(value: Decimal) -> Decimal:
    return value.quantize(TWO_PLACES, rounding=ROUND_HALF_UP)
