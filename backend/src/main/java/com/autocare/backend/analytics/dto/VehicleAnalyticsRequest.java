package com.autocare.backend.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sanitized payload sent to the internal Python analytics service. It contains
 * no user identity data — only the vehicle history needed for calculations.
 */
public record VehicleAnalyticsRequest(
		VehicleInfo vehicle,
		LocalDate referenceDate,
		List<MaintenanceItem> maintenanceRecords,
		List<FuelItem> fuelEntries) {

	public record VehicleInfo(UUID id, int currentMileage, String fuelType, int manufacturingYear) {
	}

	public record MaintenanceItem(String category, String title, LocalDate serviceDate, int mileageAtService,
			BigDecimal cost, LocalDate nextServiceDate, Integer nextServiceMileage) {
	}

	public record FuelItem(LocalDate refuelDate, int odometer, BigDecimal liters, BigDecimal totalCost,
			BigDecimal pricePerLiter, boolean fullTank) {
	}
}
