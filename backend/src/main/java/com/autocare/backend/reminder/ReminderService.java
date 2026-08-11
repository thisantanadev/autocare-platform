package com.autocare.backend.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.reminder.dto.ReminderRequest;
import com.autocare.backend.reminder.dto.ReminderResponse;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReminderService {

	private final ReminderRepository repository;
	private final VehicleService vehicleService;

	public ReminderService(ReminderRepository repository, VehicleService vehicleService) {
		this.repository = repository;
		this.vehicleService = vehicleService;
	}

	public ReminderResponse create(UUID userId, UUID vehicleId, ReminderRequest request) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		validateDueCondition(request);
		Reminder reminder = new Reminder(vehicle, request.title().trim(), trimToNull(request.description()),
				request.dueDate(), request.dueMileage());
		repository.save(reminder);
		return toResponse(reminder);
	}

	@Transactional(readOnly = true)
	public List<ReminderResponse> list(UUID userId, UUID vehicleId) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		return repository.findAllByVehicleIdOrdered(vehicle.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ReminderResponse get(UUID userId, UUID reminderId) {
		return toResponse(getOwned(userId, reminderId));
	}

	public ReminderResponse update(UUID userId, UUID reminderId, ReminderRequest request) {
		Reminder reminder = getOwned(userId, reminderId);
		validateDueCondition(request);
		reminder.update(request.title().trim(), trimToNull(request.description()), request.dueDate(),
				request.dueMileage());
		return toResponse(reminder);
	}

	public ReminderResponse complete(UUID userId, UUID reminderId) {
		Reminder reminder = getOwned(userId, reminderId);
		reminder.complete(Instant.now());
		return toResponse(reminder);
	}

	public ReminderResponse reopen(UUID userId, UUID reminderId) {
		Reminder reminder = getOwned(userId, reminderId);
		reminder.reopen();
		return toResponse(reminder);
	}

	public void delete(UUID userId, UUID reminderId) {
		repository.delete(getOwned(userId, reminderId));
	}

	private Reminder getOwned(UUID userId, UUID reminderId) {
		return repository.findByIdAndVehicleUserId(reminderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Reminder"));
	}

	private void validateDueCondition(ReminderRequest request) {
		if (request.dueDate() == null && request.dueMileage() == null) {
			throw new BusinessRuleException("A reminder needs a due date, a due mileage, or both");
		}
	}

	private ReminderResponse toResponse(Reminder reminder) {
		return ReminderResponse.from(reminder, LocalDate.now(), reminder.getVehicle().getCurrentMileage());
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
