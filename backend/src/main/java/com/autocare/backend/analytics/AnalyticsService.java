package com.autocare.backend.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autocare.backend.analytics.dto.AnalyticsReport;
import com.autocare.backend.analytics.dto.VehicleAnalyticsRequest;
import com.autocare.backend.fuel.FuelEntryRepository;
import com.autocare.backend.maintenance.MaintenanceRecordRepository;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

	private final VehicleService vehicleService;
	private final MaintenanceRecordRepository maintenanceRepository;
	private final FuelEntryRepository fuelRepository;
	private final AnalyticsClient analyticsClient;

	public AnalyticsService(VehicleService vehicleService, MaintenanceRecordRepository maintenanceRepository,
			FuelEntryRepository fuelRepository, AnalyticsClient analyticsClient) {
		this.vehicleService = vehicleService;
		this.maintenanceRepository = maintenanceRepository;
		this.fuelRepository = fuelRepository;
		this.analyticsClient = analyticsClient;
	}

	@Transactional(readOnly = true)
	public AnalyticsReport getVehicleAnalytics(UUID userId, UUID vehicleId) {
		Vehicle vehicle = vehicleService.getOwned(userId, vehicleId);

		List<VehicleAnalyticsRequest.MaintenanceItem> maintenanceItems = maintenanceRepository
				.findAllByVehicleIdOrderByServiceDateDesc(vehicle.getId()).stream()
				.map(record -> new VehicleAnalyticsRequest.MaintenanceItem(record.getCategory().name(),
						record.getTitle(), record.getServiceDate(), record.getMileageAtService(),
						record.getCost(), record.getNextServiceDate(), record.getNextServiceMileage()))
				.toList();

		List<VehicleAnalyticsRequest.FuelItem> fuelItems = fuelRepository
				.findAllByVehicleIdOrderByOdometerAsc(vehicle.getId()).stream()
				.map(entry -> new VehicleAnalyticsRequest.FuelItem(entry.getRefuelDate(), entry.getOdometer(),
						entry.getLiters(), entry.getTotalCost(), entry.getPricePerLiter(), entry.isFullTank()))
				.toList();

		VehicleAnalyticsRequest payload = new VehicleAnalyticsRequest(
				new VehicleAnalyticsRequest.VehicleInfo(vehicle.getId(), vehicle.getCurrentMileage(),
						vehicle.getFuelType().name(), vehicle.getManufacturingYear()),
				LocalDate.now(), maintenanceItems, fuelItems);

		return analyticsClient.analyzeVehicle(payload);
	}
}
