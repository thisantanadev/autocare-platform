package com.autocare.backend.fuel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.common.web.PageResponse;
import com.autocare.backend.fuel.dto.FuelEntryRequest;
import com.autocare.backend.fuel.dto.FuelEntryResponse;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FuelEntryService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final int PRICE_SCALE = 3;

	private final FuelEntryRepository repository;
	private final VehicleService vehicleService;

	public FuelEntryService(FuelEntryRepository repository, VehicleService vehicleService) {
		this.repository = repository;
		this.vehicleService = vehicleService;
	}

	public FuelEntry create(UUID userId, UUID vehicleId, FuelEntryRequest request) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		FuelEntry entry = new FuelEntry(vehicle, request.refuelDate(), request.odometer(), request.liters(),
				request.totalCost(), pricePerLiter(request), request.fullTankOrDefault());
		repository.save(entry);
		vehicle.registerMileage(request.odometer());
		return entry;
	}

	@Transactional(readOnly = true)
	public PageResponse<FuelEntryResponse> list(UUID userId, UUID vehicleId, int page, int size) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
				Sort.by(Sort.Direction.DESC, "refuelDate", "odometer"));
		return PageResponse.from(repository.findAllByVehicleId(vehicle.getId(), pageable)
				.map(FuelEntryResponse::from));
	}

	@Transactional(readOnly = true)
	public FuelEntry getOwned(UUID userId, UUID entryId) {
		return repository.findByIdAndVehicleUserId(entryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Fuel entry"));
	}

	public FuelEntry update(UUID userId, UUID entryId, FuelEntryRequest request) {
		FuelEntry entry = getOwned(userId, entryId);
		entry.update(request.refuelDate(), request.odometer(), request.liters(), request.totalCost(),
				pricePerLiter(request), request.fullTankOrDefault());
		entry.getVehicle().registerMileage(request.odometer());
		return entry;
	}

	public void delete(UUID userId, UUID entryId) {
		repository.delete(getOwned(userId, entryId));
	}

	/**
	 * Price per liter is always derived server-side from total cost and liters
	 * so the three values can never contradict each other.
	 */
	private BigDecimal pricePerLiter(FuelEntryRequest request) {
		return request.totalCost().divide(request.liters(), PRICE_SCALE, RoundingMode.HALF_UP);
	}

	private int clampSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}
}
