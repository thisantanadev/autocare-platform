package com.autocare.backend.maintenance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.maintenance.MaintenanceCategory;
import com.autocare.backend.maintenance.MaintenanceRecord;

public record MaintenanceRecordResponse(
		UUID id,
		UUID vehicleId,
		MaintenanceCategory category,
		String title,
		String description,
		LocalDate serviceDate,
		int mileageAtService,
		BigDecimal cost,
		String workshop,
		LocalDate nextServiceDate,
		Integer nextServiceMileage,
		Instant createdAt,
		Instant updatedAt) {

	public static MaintenanceRecordResponse from(MaintenanceRecord record) {
		return new MaintenanceRecordResponse(
				record.getId(),
				record.getVehicle().getId(),
				record.getCategory(),
				record.getTitle(),
				record.getDescription(),
				record.getServiceDate(),
				record.getMileageAtService(),
				record.getCost(),
				record.getWorkshop(),
				record.getNextServiceDate(),
				record.getNextServiceMileage(),
				record.getCreatedAt(),
				record.getUpdatedAt());
	}
}
