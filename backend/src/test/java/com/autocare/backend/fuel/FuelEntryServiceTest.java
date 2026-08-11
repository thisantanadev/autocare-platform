package com.autocare.backend.fuel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autocare.backend.fuel.dto.FuelEntryRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelEntryServiceTest {

	@Mock
	private FuelEntryRepository repository;

	@Mock
	private VehicleService vehicleService;

	@InjectMocks
	private FuelEntryService service;

	private final UUID userId = UUID.randomUUID();
	private final UUID vehicleId = UUID.randomUUID();
	private Vehicle vehicle;

	@BeforeEach
	void setUp() {
		vehicle = new Vehicle(new User("Ana", "driver@example.com", "hash"), "Fiat", "Argo", 2021, null,
				null, 40000, FuelType.FLEX, null);
		when(vehicleService.getOwned(userId, vehicleId)).thenReturn(vehicle);
		when(repository.save(any(FuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private FuelEntryRequest request(int odometer, String liters, String totalCost) {
		return new FuelEntryRequest(LocalDate.now(), odometer, new BigDecimal(liters),
				new BigDecimal(totalCost), true);
	}

	@Test
	void calculatesPricePerLiterServerSide() {
		FuelEntry entry = service.create(userId, vehicleId, request(41000, "41.30", "250.69"));

		assertThat(entry.getPricePerLiter()).isEqualByComparingTo("6.070");
	}

	@Test
	void roundsPricePerLiterToThreeDecimalsHalfUp() {
		FuelEntry entry = service.create(userId, vehicleId, request(41000, "3.000", "10.00"));

		assertThat(entry.getPricePerLiter()).isEqualByComparingTo("3.333");
	}

	@Test
	void raisesVehicleMileageWhenOdometerIsHigher() {
		service.create(userId, vehicleId, request(41250, "40.00", "240.00"));

		assertThat(vehicle.getCurrentMileage()).isEqualTo(41250);
	}

	@Test
	void keepsVehicleMileageWhenRegisteringAnOlderEntry() {
		service.create(userId, vehicleId, request(39000, "40.00", "240.00"));

		assertThat(vehicle.getCurrentMileage()).isEqualTo(40000);
	}
}
