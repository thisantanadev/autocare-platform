package com.autocare.backend.reminder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	@Query("""
			select r from Reminder r
			where r.vehicle.id = :vehicleId
			order by r.status asc, r.dueDate asc nulls last, r.dueMileage asc nulls last
			""")
	List<Reminder> findAllByVehicleIdOrdered(@Param("vehicleId") UUID vehicleId);

	Optional<Reminder> findByIdAndVehicleUserId(UUID id, UUID userId);

	@Query("""
			select r from Reminder r
			join fetch r.vehicle
			where r.vehicle.user.id = :userId and r.status = com.autocare.backend.reminder.ReminderStatus.ACTIVE
			order by r.dueDate asc nulls last, r.dueMileage asc nulls last
			""")
	List<Reminder> findActiveByUserIdWithVehicle(@Param("userId") UUID userId);
}
