package com.autocare.backend.vehicle;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.user.User;
import com.autocare.backend.user.UserRepository;
import com.autocare.backend.vehicle.dto.VehicleRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

	@Mock
	private VehicleRepository vehicleRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private VehicleService vehicleService;

	private final UUID userId = UUID.randomUUID();
	private final User owner = new User("Ana", "driver@example.com", "hash");

	private VehicleRequest request(String plate, Integer modelYear, int mileage) {
		return new VehicleRequest("Fiat", "Argo", 2021, modelYear, plate, mileage, FuelType.FLEX, null);
	}

	@Test
	void createNormalizesTheLicensePlate() {
		when(vehicleRepository.existsByUserIdAndLicensePlate(userId, "ABC1D23")).thenReturn(false);
		when(userRepository.getReferenceById(userId)).thenReturn(owner);
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

		vehicleService.create(userId, request(" abc-1d23 ", 2022, 30000));

		ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
		verify(vehicleRepository).save(saved.capture());
		assertThat(saved.getValue().getLicensePlate()).isEqualTo("ABC1D23");
	}

	@Test
	void createRejectsAnInvalidPlateFormat() {
		assertThatThrownBy(() -> vehicleService.create(userId, request("1234", 2022, 30000)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("License plate");
		verify(vehicleRepository, never()).save(any());
	}

	@Test
	void createRejectsADuplicatePlateForTheSameUser() {
		when(vehicleRepository.existsByUserIdAndLicensePlate(userId, "ABC1D23")).thenReturn(true);

		assertThatThrownBy(() -> vehicleService.create(userId, request("ABC1D23", 2022, 30000)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already registered");
	}

	@Test
	void createRejectsManufacturingYearInTheFuture() {
		VehicleRequest futureYear = new VehicleRequest("Fiat", "Argo", Year.now().getValue() + 2, null,
				null, 0, FuelType.FLEX, null);

		assertThatThrownBy(() -> vehicleService.create(userId, futureYear))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Manufacturing year");
	}

	@Test
	void createRejectsModelYearEarlierThanManufacturingYear() {
		assertThatThrownBy(() -> vehicleService.create(userId, request(null, 2020, 30000)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Model year");
	}

	@Test
	void updateRejectsMileageBelowTheHighestRecordedReading() {
		UUID vehicleId = UUID.randomUUID();
		Vehicle vehicle = new Vehicle(owner, "Fiat", "Argo", 2021, 2022, null, 50000, FuelType.FLEX, null);
		when(vehicleRepository.findByIdAndUserId(vehicleId, userId)).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.maxFuelOdometer(vehicleId)).thenReturn(48000);
		when(vehicleRepository.maxMaintenanceMileage(vehicleId)).thenReturn(47000);

		assertThatThrownBy(() -> vehicleService.update(userId, vehicleId, request(null, 2022, 40000)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("48000");
	}

	@Test
	void updateAllowsMileageCorrectionAboveRecordedReadings() {
		UUID vehicleId = UUID.randomUUID();
		Vehicle vehicle = new Vehicle(owner, "Fiat", "Argo", 2021, 2022, null, 50000, FuelType.FLEX, null);
		when(vehicleRepository.findByIdAndUserId(vehicleId, userId)).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.maxFuelOdometer(vehicleId)).thenReturn(48000);
		when(vehicleRepository.maxMaintenanceMileage(vehicleId)).thenReturn(47000);

		vehicleService.update(userId, vehicleId, request(null, 2022, 48500));

		assertThat(vehicle.getCurrentMileage()).isEqualTo(48500);
	}

	@Test
	void getOwnedHidesVehiclesOfOtherUsers() {
		UUID vehicleId = UUID.randomUUID();
		when(vehicleRepository.findByIdAndUserId(vehicleId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> vehicleService.getOwned(userId, vehicleId))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
