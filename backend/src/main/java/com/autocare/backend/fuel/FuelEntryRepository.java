package com.autocare.backend.fuel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, UUID> {

	Page<FuelEntry> findAllByVehicleId(UUID vehicleId, Pageable pageable);

	List<FuelEntry> findAllByVehicleIdOrderByOdometerAsc(UUID vehicleId);

	Optional<FuelEntry> findByIdAndVehicleUserId(UUID id, UUID userId);

	List<FuelEntry> findAllByVehicleUserIdAndRefuelDateGreaterThanEqual(UUID userId, LocalDate from);

	List<FuelEntry> findTop8ByVehicleUserIdOrderByCreatedAtDesc(UUID userId);

	@Query("select coalesce(sum(f.totalCost), 0) from FuelEntry f where f.vehicle.user.id = :userId")
	BigDecimal totalCostByUserId(@Param("userId") UUID userId);
}
