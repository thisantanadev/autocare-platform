package com.autocare.backend.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.reminder.dto.ReminderRequest;
import com.autocare.backend.user.User;
import com.autocare.backend.vehicle.FuelType;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

	@Mock
	private ReminderRepository repository;

	@Mock
	private VehicleService vehicleService;

	@InjectMocks
	private ReminderService service;

	private final UUID userId = UUID.randomUUID();
	private Vehicle vehicle;

	@BeforeEach
	void setUp() {
		vehicle = new Vehicle(new User("Ana", "driver@example.com", "hash"), "Fiat", "Argo", 2021, null,
				null, 45000, FuelType.FLEX, null);
	}

	@Test
	void rejectsReminderWithoutAnyDueCondition() {
		UUID vehicleId = UUID.randomUUID();
		when(vehicleService.getOwned(userId, vehicleId)).thenReturn(vehicle);

		assertThatThrownBy(() -> service.create(userId, vehicleId,
				new ReminderRequest("Renew insurance", null, null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("due date");
	}

	@Test
	void completingKeepsTheOriginalCompletionTimestamp() {
		Reminder reminder = new Reminder(vehicle, "Check tires", null, LocalDate.now().plusDays(5), null);
		UUID reminderId = UUID.randomUUID();
		when(repository.findByIdAndVehicleUserId(reminderId, userId)).thenReturn(Optional.of(reminder));

		service.complete(userId, reminderId);
		Instant firstCompletion = reminder.getCompletedAt();
		service.complete(userId, reminderId);

		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
		assertThat(reminder.getCompletedAt()).isSameAs(firstCompletion);
	}

	@Test
	void reopeningClearsTheCompletionTimestamp() {
		Reminder reminder = new Reminder(vehicle, "Check tires", null, LocalDate.now().plusDays(5), null);
		reminder.complete(Instant.now());
		UUID reminderId = UUID.randomUUID();
		when(repository.findByIdAndVehicleUserId(reminderId, userId)).thenReturn(Optional.of(reminder));

		service.reopen(userId, reminderId);

		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);
		assertThat(reminder.getCompletedAt()).isNull();
	}

	@Test
	void remindersOfOtherUsersAreNotFound() {
		UUID reminderId = UUID.randomUUID();
		when(repository.findByIdAndVehicleUserId(reminderId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(userId, reminderId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void overdueIsDerivedFromDateAndMileage() {
		LocalDate today = LocalDate.now();

		Reminder byDate = new Reminder(vehicle, "Licensing", null, today.minusDays(1), null);
		Reminder byMileage = new Reminder(vehicle, "Timing belt", null, null, 45000);
		Reminder future = new Reminder(vehicle, "Next inspection", null, today.plusDays(30), 60000);
		Reminder completed = new Reminder(vehicle, "Old task", null, today.minusDays(10), null);
		completed.complete(Instant.now());

		assertThat(byDate.isOverdue(today, vehicle.getCurrentMileage())).isTrue();
		assertThat(byMileage.isOverdue(today, vehicle.getCurrentMileage())).isTrue();
		assertThat(future.isOverdue(today, vehicle.getCurrentMileage())).isFalse();
		assertThat(completed.isOverdue(today, vehicle.getCurrentMileage())).isFalse();
	}
}
