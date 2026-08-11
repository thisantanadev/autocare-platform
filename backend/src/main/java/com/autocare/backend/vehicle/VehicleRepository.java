package com.autocare.backend.vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	List<Vehicle> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<Vehicle> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndLicensePlate(UUID userId, String licensePlate);

	boolean existsByUserIdAndLicensePlateAndIdNot(UUID userId, String licensePlate, UUID id);

	long countByUserId(UUID userId);

	// Read-only guard queries: a vehicle's mileage may never be corrected below
	// the highest odometer reading present in its history.
	@Query("select coalesce(max(f.odometer), 0) from FuelEntry f where f.vehicle.id = :vehicleId")
	int maxFuelOdometer(@Param("vehicleId") UUID vehicleId);

	@Query("select coalesce(max(m.mileageAtService), 0) from MaintenanceRecord m where m.vehicle.id = :vehicleId")
	int maxMaintenanceMileage(@Param("vehicleId") UUID vehicleId);
}
