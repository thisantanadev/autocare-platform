package com.autocare.backend.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

import com.autocare.backend.vehicle.FuelType;
import com.autocare.backend.vehicle.Vehicle;

public record VehicleResponse(
		UUID id,
		String brand,
		String model,
		int manufacturingYear,
		Integer modelYear,
		String licensePlate,
		int currentMileage,
		FuelType fuelType,
		String nickname,
		String displayName,
		Instant createdAt,
		Instant updatedAt) {

	public static VehicleResponse from(Vehicle vehicle) {
		return new VehicleResponse(
				vehicle.getId(),
				vehicle.getBrand(),
				vehicle.getModel(),
				vehicle.getManufacturingYear(),
				vehicle.getModelYear(),
				vehicle.getLicensePlate(),
				vehicle.getCurrentMileage(),
				vehicle.getFuelType(),
				vehicle.getNickname(),
				vehicle.displayName(),
				vehicle.getCreatedAt(),
				vehicle.getUpdatedAt());
	}
}
