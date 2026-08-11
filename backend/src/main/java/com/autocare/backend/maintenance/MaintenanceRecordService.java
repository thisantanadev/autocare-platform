package com.autocare.backend.maintenance;

import java.util.UUID;

import com.autocare.backend.common.error.BusinessRuleException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.common.web.PageResponse;
import com.autocare.backend.maintenance.dto.MaintenanceRecordRequest;
import com.autocare.backend.maintenance.dto.MaintenanceRecordResponse;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaintenanceRecordService {

	private static final int MAX_PAGE_SIZE = 100;

	private final MaintenanceRecordRepository repository;
	private final VehicleService vehicleService;

	public MaintenanceRecordService(MaintenanceRecordRepository repository, VehicleService vehicleService) {
		this.repository = repository;
		this.vehicleService = vehicleService;
	}

	public MaintenanceRecord create(UUID userId, UUID vehicleId, MaintenanceRecordRequest request) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		validateNextService(request);
		MaintenanceRecord record = new MaintenanceRecord(vehicle, request.category(), request.title().trim(),
				trimToNull(request.description()), request.serviceDate(), request.mileageAtService(),
				request.cost(), trimToNull(request.workshop()), request.nextServiceDate(),
				request.nextServiceMileage());
		repository.save(record);
		vehicle.registerMileage(request.mileageAtService());
		return record;
	}

	@Transactional(readOnly = true)
	public PageResponse<MaintenanceRecordResponse> list(UUID userId, UUID vehicleId, int page, int size) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);
		Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
				Sort.by(Sort.Direction.DESC, "serviceDate", "createdAt"));
		return PageResponse.from(repository.findAllByVehicleId(vehicle.getId(), pageable)
				.map(MaintenanceRecordResponse::from));
	}

	@Transactional(readOnly = true)
	public MaintenanceRecord getOwned(UUID userId, UUID recordId) {
		return repository.findByIdAndVehicleUserId(recordId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Maintenance record"));
	}

	public MaintenanceRecord update(UUID userId, UUID recordId, MaintenanceRecordRequest request) {
		MaintenanceRecord record = getOwned(userId, recordId);
		validateNextService(request);
		record.update(request.category(), request.title().trim(), trimToNull(request.description()),
				request.serviceDate(), request.mileageAtService(), request.cost(),
				trimToNull(request.workshop()), request.nextServiceDate(), request.nextServiceMileage());
		record.getVehicle().registerMileage(request.mileageAtService());
		return record;
	}

	public void delete(UUID userId, UUID recordId) {
		repository.delete(getOwned(userId, recordId));
	}

	private void validateNextService(MaintenanceRecordRequest request) {
		if (request.nextServiceDate() != null && !request.nextServiceDate().isAfter(request.serviceDate())) {
			throw new BusinessRuleException("Next service date must be after the service date");
		}
		if (request.nextServiceMileage() != null && request.nextServiceMileage() <= request.mileageAtService()) {
			throw new BusinessRuleException("Next service mileage must be greater than the mileage at service");
		}
	}

	private int clampSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
