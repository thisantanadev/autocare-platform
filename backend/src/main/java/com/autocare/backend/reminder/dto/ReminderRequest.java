package com.autocare.backend.reminder.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReminderRequest(
		@NotBlank @Size(max = 120) String title,
		@Size(max = 2000) String description,
		LocalDate dueDate,
		@Min(0) Integer dueMileage) {
}
