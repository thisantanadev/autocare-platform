package com.autocare.backend.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.vehicle.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "reminders")
public class Reminder {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Vehicle vehicle;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "due_mileage")
	private Integer dueMileage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReminderStatus status = ReminderStatus.ACTIVE;

	@Column(name = "completed_at")
	private Instant completedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Reminder() {
	}

	public Reminder(Vehicle vehicle, String title, String description, LocalDate dueDate, Integer dueMileage) {
		this.vehicle = vehicle;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.dueMileage = dueMileage;
	}

	public void update(String title, String description, LocalDate dueDate, Integer dueMileage) {
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.dueMileage = dueMileage;
	}

	/** Idempotent: completing twice keeps the original completion timestamp. */
	public void complete(Instant now) {
		if (status == ReminderStatus.ACTIVE) {
			status = ReminderStatus.COMPLETED;
			completedAt = now;
		}
	}

	public void reopen() {
		if (status == ReminderStatus.COMPLETED) {
			status = ReminderStatus.ACTIVE;
			completedAt = null;
		}
	}

	/**
	 * A reminder is overdue while it is still active and any of its due
	 * conditions has been reached (date passed or mileage crossed).
	 */
	public boolean isOverdue(LocalDate today, int currentMileage) {
		if (status != ReminderStatus.ACTIVE) {
			return false;
		}
		boolean dateReached = dueDate != null && !dueDate.isAfter(today);
		boolean mileageReached = dueMileage != null && currentMileage >= dueMileage;
		return dateReached || mileageReached;
	}

	public UUID getId() {
		return id;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Integer getDueMileage() {
		return dueMileage;
	}

	public ReminderStatus getStatus() {
		return status;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
