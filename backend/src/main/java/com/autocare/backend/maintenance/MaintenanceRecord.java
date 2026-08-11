package com.autocare.backend.maintenance;

import java.math.BigDecimal;
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
@Table(name = "maintenance_records")
public class MaintenanceRecord {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Vehicle vehicle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MaintenanceCategory category;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	@Column(name = "mileage_at_service", nullable = false)
	private int mileageAtService;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal cost;

	@Column(length = 120)
	private String workshop;

	@Column(name = "next_service_date")
	private LocalDate nextServiceDate;

	@Column(name = "next_service_mileage")
	private Integer nextServiceMileage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MaintenanceRecord() {
	}

	public MaintenanceRecord(Vehicle vehicle, MaintenanceCategory category, String title, String description,
			LocalDate serviceDate, int mileageAtService, BigDecimal cost, String workshop,
			LocalDate nextServiceDate, Integer nextServiceMileage) {
		this.vehicle = vehicle;
		this.category = category;
		this.title = title;
		this.description = description;
		this.serviceDate = serviceDate;
		this.mileageAtService = mileageAtService;
		this.cost = cost;
		this.workshop = workshop;
		this.nextServiceDate = nextServiceDate;
		this.nextServiceMileage = nextServiceMileage;
	}

	public void update(MaintenanceCategory category, String title, String description, LocalDate serviceDate,
			int mileageAtService, BigDecimal cost, String workshop, LocalDate nextServiceDate,
			Integer nextServiceMileage) {
		this.category = category;
		this.title = title;
		this.description = description;
		this.serviceDate = serviceDate;
		this.mileageAtService = mileageAtService;
		this.cost = cost;
		this.workshop = workshop;
		this.nextServiceDate = nextServiceDate;
		this.nextServiceMileage = nextServiceMileage;
	}

	public UUID getId() {
		return id;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public MaintenanceCategory getCategory() {
		return category;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LocalDate getServiceDate() {
		return serviceDate;
	}

	public int getMileageAtService() {
		return mileageAtService;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public String getWorkshop() {
		return workshop;
	}

	public LocalDate getNextServiceDate() {
		return nextServiceDate;
	}

	public Integer getNextServiceMileage() {
		return nextServiceMileage;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
