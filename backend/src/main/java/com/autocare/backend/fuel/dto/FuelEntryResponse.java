package com.autocare.backend.fuel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.fuel.FuelEntry;

public record FuelEntryResponse(
		UUID id,
		UUID vehicleId,
		LocalDate refuelDate,
		int odometer,
		BigDecimal liters,
		BigDecimal totalCost,
		BigDecimal pricePerLiter,
		boolean fullTank,
		Instant createdAt,
		Instant updatedAt) {

	public static FuelEntryResponse from(FuelEntry entry) {
		return new FuelEntryResponse(
				entry.getId(),
				entry.getVehicle().getId(),
				entry.getRefuelDate(),
				entry.getOdometer(),
				entry.getLiters(),
				entry.getTotalCost(),
				entry.getPricePerLiter(),
				entry.isFullTank(),
				entry.getCreatedAt(),
				entry.getUpdatedAt());
	}
}
