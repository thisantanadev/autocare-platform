package com.autocare.backend.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mirror of the response contract exposed by the Python analytics service
 * (POST /internal/v1/analytics/vehicle). Nullable fields mean the service did
 * not have enough data for that metric; the companion warning explains why.
 */
public record AnalyticsReport(
		Totals totals,
		List<MonthlyCost> monthlyCosts,
		List<CategoryCost> costByCategory,
		FuelStats fuelStats,
		Trend trend,
		PeriodComparison periodComparison,
		List<UpcomingItem> upcomingMaintenance,
		List<Warning> warnings) {

	public record Totals(BigDecimal maintenanceCost, BigDecimal fuelCost, BigDecimal operatingCost) {
	}

	public record MonthlyCost(String month, BigDecimal maintenanceCost, BigDecimal fuelCost, BigDecimal total) {
	}

	public record CategoryCost(String category, BigDecimal total, BigDecimal percentage) {
	}

	public record FuelStats(BigDecimal totalLiters, BigDecimal averagePricePerLiter,
			BigDecimal averageConsumptionKmPerLiter, BigDecimal costPerKm) {
	}

	public record Trend(String direction, BigDecimal currentMonthTotal, BigDecimal previousThreeMonthAverage) {
	}

	public record PeriodComparison(int periodDays, BigDecimal currentPeriodTotal,
			BigDecimal previousPeriodTotal, BigDecimal changePercentage) {
	}

	public record UpcomingItem(String title, LocalDate nextServiceDate, Integer nextServiceMileage,
			String status) {
	}

	public record Warning(String code, String message) {
	}
}
