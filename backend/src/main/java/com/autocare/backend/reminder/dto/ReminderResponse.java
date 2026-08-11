package com.autocare.backend.reminder.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.reminder.Reminder;
import com.autocare.backend.reminder.ReminderStatus;

public record ReminderResponse(
		UUID id,
		UUID vehicleId,
		String title,
		String description,
		LocalDate dueDate,
		Integer dueMileage,
		ReminderStatus status,
		boolean overdue,
		Instant completedAt,
		Instant createdAt,
		Instant updatedAt) {

	public static ReminderResponse from(Reminder reminder, LocalDate today, int currentMileage) {
		return new ReminderResponse(
				reminder.getId(),
				reminder.getVehicle().getId(),
				reminder.getTitle(),
				reminder.getDescription(),
				reminder.getDueDate(),
				reminder.getDueMileage(),
				reminder.getStatus(),
				reminder.isOverdue(today, currentMileage),
				reminder.getCompletedAt(),
				reminder.getCreatedAt(),
				reminder.getUpdatedAt());
	}
}
