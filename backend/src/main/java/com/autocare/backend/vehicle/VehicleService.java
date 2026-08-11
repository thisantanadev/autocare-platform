package com.autocare.backend.vehicle;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.user.User;
import com.autocare.backend.user.UserRepository;
import com.autocare.backend.vehicle.dto.VehicleRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {

	// Accepts both the legacy Brazilian format (ABC1234) and the Mercosul
	// format (ABC1D23) after normalization.
	private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z]{3}\\d[A-Z0-9]\\d{2}$");

	private final VehicleRepository vehicleRepository;
	private final UserRepository userRepository;

	public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
		this.vehicleRepository = vehicleRepository;
		this.userRepository = userRepository;
	}

	public Vehicle create(UUID userId, VehicleRequest request) {
		validateYears(request);
		String plate = normalizePlate(request.licensePlate());
		if (plate != null && vehicleRepository.existsByUserIdAndLicensePlate(userId, plate)) {
			throw new BusinessRuleException("A vehicle with this license plate is already registered");
		}
		User owner = userRepository.getReferenceById(userId);
		Vehicle vehicle = new Vehicle(owner, request.brand().trim(), request.model().trim(),
				request.manufacturingYear(), request.modelYear(), plate, request.currentMileage(),
				request.fuelType(), trimToNull(request.nickname()));
		return vehicleRepository.save(vehicle);
	}

	@Transactional(readOnly = true)
	public List<Vehicle> list(UUID userId) {
		return vehicleRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public Vehicle getOwned(UUID userId, UUID vehicleId) {
		return vehicleRepository.findByIdAndUserId(vehicleId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle"));
	}

	public Vehicle update(UUID userId, UUID vehicleId, VehicleRequest request) {
		Vehicle vehicle = getOwned(userId, vehicleId);
		validateYears(request);
		String plate = normalizePlate(request.licensePlate());
		if (plate != null && vehicleRepository.existsByUserIdAndLicensePlateAndIdNot(userId, plate, vehicleId)) {
			throw new BusinessRuleException("A vehicle with this license plate is already registered");
		}
		int highestRecorded = highestRecordedMileage(vehicleId);
		if (request.currentMileage() < highestRecorded) {
			throw new BusinessRuleException("Current mileage cannot be lower than the highest recorded "
					+ "odometer reading (" + highestRecorded + " km)");
		}
		vehicle.updateDetails(request.brand().trim(), request.model().trim(), request.manufacturingYear(),
				request.modelYear(), plate, request.currentMileage(), request.fuelType(),
				trimToNull(request.nickname()));
		return vehicle;
	}

	public void delete(UUID userId, UUID vehicleId) {
		Vehicle vehicle = getOwned(userId, vehicleId);
		// Child records (maintenance, fuel, reminders) are removed by the
		// database ON DELETE CASCADE declared in the schema.
		vehicleRepository.delete(vehicle);
	}

	private int highestRecordedMileage(UUID vehicleId) {
		return Math.max(vehicleRepository.maxFuelOdometer(vehicleId),
				vehicleRepository.maxMaintenanceMileage(vehicleId));
	}

	private void validateYears(VehicleRequest request) {
		int nextYear = Year.now().getValue() + 1;
		if (request.manufacturingYear() > nextYear) {
			throw new BusinessRuleException("Manufacturing year cannot be in the future");
		}
		if (request.modelYear() != null && request.modelYear() < request.manufacturingYear()) {
			throw new BusinessRuleException("Model year cannot be earlier than the manufacturing year");
		}
	}

	private String normalizePlate(String licensePlate) {
		if (licensePlate == null || licensePlate.isBlank()) {
			return null;
		}
		String normalized = licensePlate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
		if (!PLATE_PATTERN.matcher(normalized).matches()) {
			throw new BusinessRuleException("License plate must follow the Brazilian format (e.g. ABC1D23)");
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
