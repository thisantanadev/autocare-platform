package com.autocare.backend.vehicle;

import java.time.Instant;
import java.util.UUID;

import com.autocare.backend.user.User;

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
@Table(name = "vehicles")
public class Vehicle {

	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@Column(nullable = false, length = 60)
	private String brand;

	@Column(nullable = false, length = 80)
	private String model;

	@Column(name = "manufacturing_year", nullable = false)
	private int manufacturingYear;

	@Column(name = "model_year")
	private Integer modelYear;

	@Column(name = "license_plate", length = 8)
	private String licensePlate;

	@Column(name = "current_mileage", nullable = false)
	private int currentMileage;

	@Enumerated(EnumType.STRING)
	@Column(name = "fuel_type", nullable = false, length = 20)
	private FuelType fuelType;

	@Column(length = 60)
	private String nickname;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Vehicle() {
	}

	public Vehicle(User user, String brand, String model, int manufacturingYear, Integer modelYear,
			String licensePlate, int currentMileage, FuelType fuelType, String nickname) {
		this.user = user;
		this.brand = brand;
		this.model = model;
		this.manufacturingYear = manufacturingYear;
		this.modelYear = modelYear;
		this.licensePlate = licensePlate;
		this.currentMileage = currentMileage;
		this.fuelType = fuelType;
		this.nickname = nickname;
	}

	public void updateDetails(String brand, String model, int manufacturingYear, Integer modelYear,
			String licensePlate, int currentMileage, FuelType fuelType, String nickname) {
		this.brand = brand;
		this.model = model;
		this.manufacturingYear = manufacturingYear;
		this.modelYear = modelYear;
		this.licensePlate = licensePlate;
		this.currentMileage = currentMileage;
		this.fuelType = fuelType;
		this.nickname = nickname;
	}

	/**
	 * Raises the current mileage when a maintenance or fuel record reports a
	 * higher odometer reading. Older historical records (lower readings) never
	 * reduce the current mileage.
	 */
	public void registerMileage(int reportedMileage) {
		if (reportedMileage > currentMileage) {
			currentMileage = reportedMileage;
		}
	}

	public String displayName() {
		return nickname != null && !nickname.isBlank() ? nickname : brand + " " + model;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getBrand() {
		return brand;
	}

	public String getModel() {
		return model;
	}

	public int getManufacturingYear() {
		return manufacturingYear;
	}

	public Integer getModelYear() {
		return modelYear;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public int getCurrentMileage() {
		return currentMileage;
	}

	public FuelType getFuelType() {
		return fuelType;
	}

	public String getNickname() {
		return nickname;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
