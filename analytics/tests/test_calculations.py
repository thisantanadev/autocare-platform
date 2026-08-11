from datetime import date
from decimal import Decimal

from app.calculations import (
    build_report,
    compute_cost_by_category,
    compute_fuel_stats,
    compute_monthly_costs,
    compute_period_comparison,
    compute_trend,
    compute_upcoming_maintenance,
)

from .factories import fuel_item, maintenance_item, request

REFERENCE = date(2026, 8, 11)


class TestTotals:
    def test_sums_maintenance_and_fuel(self):
        report = build_report(
            request(
                maintenance=[maintenance_item(cost="289.90")],
                fuel=[fuel_item(total_cost="250.00")],
            )
        )
        assert report.totals.maintenance_cost == Decimal("289.90")
        assert report.totals.fuel_cost == Decimal("250.00")
        assert report.totals.operating_cost == Decimal("539.90")

    def test_no_records_yields_zero_totals_and_warning(self):
        report = build_report(request())
        assert report.totals.operating_cost == Decimal("0.00")
        assert any(w.code == "NO_DATA" for w in report.warnings)


class TestMonthlyCosts:
    def test_groups_by_calendar_month_and_fills_gaps(self):
        months = compute_monthly_costs(
            [maintenance_item(cost="100.00", service_date=date(2026, 6, 10))],
            [fuel_item(total_cost="200.00", refuel_date=date(2026, 8, 1))],
            REFERENCE,
        )
        keys = [m.month for m in months]
        assert keys == ["2026-06", "2026-07", "2026-08"]
        assert months[0].maintenance_cost == Decimal("100.00")
        assert months[1].total == Decimal("0.00")  # empty month kept for charts
        assert months[2].fuel_cost == Decimal("200.00")

    def test_without_records_returns_empty_list(self):
        assert compute_monthly_costs([], [], REFERENCE) == []


class TestCostByCategory:
    def test_percentages_are_shares_of_maintenance_total(self):
        categories = compute_cost_by_category(
            [
                maintenance_item(cost="300.00", category="BRAKES"),
                maintenance_item(cost="100.00", category="TIRES"),
            ]
        )
        assert categories[0].category == "BRAKES"
        assert categories[0].percentage == Decimal("75.0")
        assert categories[1].percentage == Decimal("25.0")


class TestFuelEfficiency:
    def test_full_to_full_method(self):
        stats, warnings = compute_fuel_stats(
            [],
            [
                fuel_item(odometer=1000, liters="35.000", full_tank=True),
                fuel_item(odometer=1400, liters="40.000", full_tank=True),
                fuel_item(odometer=1800, liters="40.000", full_tank=True),
            ],
        )
        # 800 km on the 80 liters added after the first full tank.
        assert stats.average_consumption_km_per_liter == Decimal("10.00")
        assert not any(w.code == "INSUFFICIENT_FUEL_DATA" for w in warnings)

    def test_partial_fill_between_full_tanks_counts_as_consumed_fuel(self):
        stats, _ = compute_fuel_stats(
            [],
            [
                fuel_item(odometer=1000, liters="35.000", full_tank=True),
                fuel_item(odometer=1200, liters="10.000", full_tank=False),
                fuel_item(odometer=1400, liters="20.000", full_tank=True),
                fuel_item(odometer=1800, liters="40.000", full_tank=True),
            ],
        )
        # Intervals: 1000→1400 used 30 L, 1400→1800 used 40 L → 800 km / 70 L.
        assert stats.average_consumption_km_per_liter == Decimal("11.43")

    def test_requires_at_least_two_full_intervals(self):
        stats, warnings = compute_fuel_stats(
            [],
            [
                fuel_item(odometer=1000, full_tank=True),
                fuel_item(odometer=1400, full_tank=True),
            ],
        )
        assert stats.average_consumption_km_per_liter is None
        assert any(w.code == "INSUFFICIENT_FUEL_DATA" for w in warnings)

    def test_average_price_is_volume_weighted(self):
        stats, _ = compute_fuel_stats(
            [],
            [
                fuel_item(total_cost="100.00", liters="20.000", odometer=1000),
                fuel_item(total_cost="300.00", liters="40.000", odometer=1500),
            ],
        )
        # (100 + 300) / (20 + 40) = 6.667, not the mean of 5.00 and 7.50.
        assert stats.average_price_per_liter == Decimal("6.667")


class TestCostPerKm:
    def test_uses_odometer_span_of_all_records(self):
        stats, _ = compute_fuel_stats(
            [maintenance_item(cost="150.00", mileage_at_service=2000)],
            [
                fuel_item(total_cost="250.00", odometer=1000),
                fuel_item(total_cost="250.00", odometer=1800),
            ],
        )
        # (150 + 500) / (2000 - 1000) km
        assert stats.cost_per_km == Decimal("0.65")

    def test_requires_minimum_distance(self):
        stats, warnings = compute_fuel_stats(
            [],
            [
                fuel_item(odometer=1000),
                fuel_item(odometer=1050),
            ],
        )
        assert stats.cost_per_km is None
        assert any(w.code == "INSUFFICIENT_DISTANCE_DATA" for w in warnings)


class TestPeriodComparison:
    def test_compares_last_90_days_with_previous_90(self):
        comparison = compute_period_comparison(
            [maintenance_item(cost="150.00", service_date=date(2026, 4, 1))],
            [fuel_item(total_cost="300.00", refuel_date=date(2026, 8, 1))],
            REFERENCE,
        )
        assert comparison.current_period_total == Decimal("300.00")
        assert comparison.previous_period_total == Decimal("150.00")
        assert comparison.change_percentage == Decimal("100.0")

    def test_percentage_is_null_without_previous_spending(self):
        comparison = compute_period_comparison(
            [], [fuel_item(total_cost="300.00", refuel_date=date(2026, 8, 1))], REFERENCE
        )
        assert comparison.change_percentage is None


class TestTrend:
    def _three_previous_months(self):
        return [
            maintenance_item(cost="100.00", service_date=date(2026, 5, 10)),
            maintenance_item(cost="100.00", service_date=date(2026, 6, 10)),
            maintenance_item(cost="100.00", service_date=date(2026, 7, 10)),
        ]

    def test_up_when_current_month_exceeds_average_by_more_than_10_percent(self):
        trend = compute_trend(
            self._three_previous_months()
            + [maintenance_item(cost="150.00", service_date=date(2026, 8, 5))],
            [],
            REFERENCE,
        )
        assert trend.direction == "UP"
        assert trend.previous_three_month_average == Decimal("100.00")

    def test_down_when_current_month_is_more_than_10_percent_below(self):
        trend = compute_trend(
            self._three_previous_months()
            + [maintenance_item(cost="50.00", service_date=date(2026, 8, 5))],
            [],
            REFERENCE,
        )
        assert trend.direction == "DOWN"

    def test_stable_within_tolerance_band(self):
        trend = compute_trend(
            self._three_previous_months()
            + [maintenance_item(cost="105.00", service_date=date(2026, 8, 5))],
            [],
            REFERENCE,
        )
        assert trend.direction == "STABLE"


class TestUpcomingMaintenance:
    def test_statuses_and_ordering(self):
        items = compute_upcoming_maintenance(
            [
                maintenance_item(title="Scheduled far away", next_service_date=date(2026, 11, 20)),
                maintenance_item(title="Overdue by date", next_service_date=date(2026, 8, 10)),
                maintenance_item(title="Due soon by mileage", next_service_mileage=48500),
            ],
            current_mileage=48000,
            reference_date=REFERENCE,
        )
        assert [i.title for i in items] == [
            "Overdue by date",
            "Due soon by mileage",
            "Scheduled far away",
        ]
        assert [i.status for i in items] == ["OVERDUE", "DUE_SOON", "SCHEDULED"]

    def test_records_without_next_service_are_ignored(self):
        assert compute_upcoming_maintenance([maintenance_item()], 48000, REFERENCE) == []
