package com.autocare.backend.fuel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record FuelEntryRequest(
		@NotNull @PastOrPresent LocalDate refuelDate,
		@NotNull @Min(0) Integer odometer,
		@NotNull @DecimalMin(value = "0.001") @Digits(integer = 5, fraction = 3) BigDecimal liters,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal totalCost,
		Boolean fullTank) {

	public boolean fullTankOrDefault() {
		return Boolean.TRUE.equals(fullTank);
	}
}
