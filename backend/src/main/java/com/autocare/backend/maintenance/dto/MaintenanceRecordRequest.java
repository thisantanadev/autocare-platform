package com.autocare.backend.maintenance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autocare.backend.maintenance.MaintenanceCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record MaintenanceRecordRequest(
		@NotNull MaintenanceCategory category,
		@NotBlank @Size(max = 120) String title,
		@Size(max = 2000) String description,
		@NotNull @PastOrPresent LocalDate serviceDate,
		@NotNull @Min(0) Integer mileageAtService,
		// Cost may be zero (e.g. warranty service) but never negative.
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal cost,
		@Size(max = 120) String workshop,
		LocalDate nextServiceDate,
		@Min(0) Integer nextServiceMileage) {
}
