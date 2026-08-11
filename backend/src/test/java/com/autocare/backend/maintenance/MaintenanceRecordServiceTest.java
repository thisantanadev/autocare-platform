package com.autocare.backend.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.maintenance.dto.MaintenanceRecordRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordServiceTest {

	@Mock
	private MaintenanceRecordRepository repository;

	@Mock
	private VehicleService vehicleService;

	@InjectMocks
	private MaintenanceRecordService service;

	private final UUID userId = UUID.randomUUID();
	private final UUID vehicleId = UUID.randomUUID();
	private Vehicle vehicle;

	@BeforeEach
	void setUp() {
		vehicle = new Vehicle(new User("Ana", "driver@example.com", "hash"), "Fiat", "Argo", 2021, null,
				null, 45000, FuelType.FLEX, null);
		when(vehicleService.getOwned(userId, vehicleId)).thenReturn(vehicle);
		lenient().when(repository.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private MaintenanceRecordRequest request(LocalDate serviceDate, int mileage, LocalDate nextDate,
			Integer nextMileage) {
		return new MaintenanceRecordRequest(MaintenanceCategory.OIL_CHANGE, "Oil change", null, serviceDate,
				mileage, new BigDecimal("289.90"), null, nextDate, nextMileage);
	}

	@Test
	void rejectsNextServiceDateNotAfterServiceDate() {
		LocalDate serviceDate = LocalDate.now().minusDays(10);

		assertThatThrownBy(() -> service.create(userId, vehicleId,
				request(serviceDate, 45000, serviceDate, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Next service date");
	}

	@Test
	void rejectsNextServiceMileageNotGreaterThanServiceMileage() {
		assertThatThrownBy(() -> service.create(userId, vehicleId,
				request(LocalDate.now().minusDays(10), 45000, null, 45000)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Next service mileage");
	}

	@Test
	void raisesVehicleMileageWhenServiceMileageIsHigher() {
		service.create(userId, vehicleId, request(LocalDate.now().minusDays(1), 46200, null, null));

		assertThat(vehicle.getCurrentMileage()).isEqualTo(46200);
	}

	@Test
	void keepsVehicleMileageWhenRegisteringAnOlderService() {
		service.create(userId, vehicleId, request(LocalDate.now().minusDays(300), 30000, null, null));

		assertThat(vehicle.getCurrentMileage()).isEqualTo(45000);
	}
}
