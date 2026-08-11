package com.autocare.backend.maintenance;

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

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

	Page<MaintenanceRecord> findAllByVehicleId(UUID vehicleId, Pageable pageable);

	List<MaintenanceRecord> findAllByVehicleIdOrderByServiceDateDesc(UUID vehicleId);

	Optional<MaintenanceRecord> findByIdAndVehicleUserId(UUID id, UUID userId);

	List<MaintenanceRecord> findAllByVehicleUserIdAndServiceDateGreaterThanEqual(UUID userId, LocalDate from);

	List<MaintenanceRecord> findTop8ByVehicleUserIdOrderByCreatedAtDesc(UUID userId);

	@Query("select coalesce(sum(m.cost), 0) from MaintenanceRecord m where m.vehicle.user.id = :userId")
	BigDecimal totalCostByUserId(@Param("userId") UUID userId);

	@Query("""
			select m.category as category, sum(m.cost) as total
			from MaintenanceRecord m
			where m.vehicle.user.id = :userId
			group by m.category
			order by sum(m.cost) desc
			""")
	List<CategoryTotalView> totalsByCategory(@Param("userId") UUID userId);

	interface CategoryTotalView {

		MaintenanceCategory getCategory();

		BigDecimal getTotal();
	}
}
