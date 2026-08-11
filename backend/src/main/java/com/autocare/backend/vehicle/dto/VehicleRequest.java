package com.autocare.backend.vehicle.dto;

import com.autocare.backend.vehicle.FuelType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
		@NotBlank @Size(max = 60) String brand,
		@NotBlank @Size(max = 80) String model,
		@NotNull @Min(1900) Integer manufacturingYear,
		@Min(1900) Integer modelYear,
		@Size(max = 10) String licensePlate,
		@NotNull @Min(0) Integer currentMileage,
		@NotNull FuelType fuelType,
		@Size(max = 60) String nickname) {
}
