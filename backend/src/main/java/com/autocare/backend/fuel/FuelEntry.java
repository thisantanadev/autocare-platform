package com.autocare.backend.fuel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.vehicle.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fuel_entries")
public class FuelEntry {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Vehicle vehicle;

	@Column(name = "refuel_date", nullable = false)
	private LocalDate refuelDate;

	@Column(nullable = false)
	private int odometer;

	@Column(nullable = false, precision = 8, scale = 3)
	private BigDecimal liters;

	@Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalCost;

	@Column(name = "price_per_liter", nullable = false, precision = 8, scale = 3)
	private BigDecimal pricePerLiter;

	@Column(name = "full_tank", nullable = false)
	private boolean fullTank;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected FuelEntry() {
	}

	public FuelEntry(Vehicle vehicle, LocalDate refuelDate, int odometer, BigDecimal liters,
			BigDecimal totalCost, BigDecimal pricePerLiter, boolean fullTank) {
		this.vehicle = vehicle;
		this.refuelDate = refuelDate;
		this.odometer = odometer;
		this.liters = liters;
		this.totalCost = totalCost;
		this.pricePerLiter = pricePerLiter;
		this.fullTank = fullTank;
	}

	public void update(LocalDate refuelDate, int odometer, BigDecimal liters, BigDecimal totalCost,
			BigDecimal pricePerLiter, boolean fullTank) {
		this.refuelDate = refuelDate;
		this.odometer = odometer;
		this.liters = liters;
		this.totalCost = totalCost;
		this.pricePerLiter = pricePerLiter;
		this.fullTank = fullTank;
	}

	public UUID getId() {
		return id;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public LocalDate getRefuelDate() {
		return refuelDate;
	}

	public int getOdometer() {
		return odometer;
	}

	public BigDecimal getLiters() {
		return liters;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public BigDecimal getPricePerLiter() {
		return pricePerLiter;
	}

	public boolean isFullTank() {
		return fullTank;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
