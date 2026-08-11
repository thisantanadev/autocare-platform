package com.autocare.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.autocare.backend.dashboard.dto.DashboardSummaryResponse;
import com.autocare.backend.dashboard.dto.DashboardSummaryResponse.ActivityItem;
import com.autocare.backend.dashboard.dto.DashboardSummaryResponse.CategoryExpense;
import com.autocare.backend.dashboard.dto.DashboardSummaryResponse.MonthlyExpense;
import com.autocare.backend.dashboard.dto.DashboardSummaryResponse.ReminderItem;
import com.autocare.backend.fuel.FuelEntry;
import com.autocare.backend.fuel.FuelEntryRepository;
import com.autocare.backend.maintenance.MaintenanceRecord;
import com.autocare.backend.maintenance.MaintenanceRecordRepository;
import com.autocare.backend.reminder.Reminder;
import com.autocare.backend.reminder.ReminderRepository;
import com.autocare.backend.vehicle.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

	private static final int MONTHS_IN_CHART = 6;
	private static final int MAX_UPCOMING_REMINDERS = 5;
	private static final int MAX_RECENT_ACTIVITY = 8;

	private final VehicleRepository vehicleRepository;
	private final MaintenanceRecordRepository maintenanceRepository;
	private final FuelEntryRepository fuelRepository;
	private final ReminderRepository reminderRepository;

	public DashboardService(VehicleRepository vehicleRepository,
			MaintenanceRecordRepository maintenanceRepository, FuelEntryRepository fuelRepository,
			ReminderRepository reminderRepository) {
		this.vehicleRepository = vehicleRepository;
		this.maintenanceRepository = maintenanceRepository;
		this.fuelRepository = fuelRepository;
		this.reminderRepository = reminderRepository;
	}

	public DashboardSummaryResponse getSummary(UUID userId) {
		LocalDate today = LocalDate.now();
		long vehicleCount = vehicleRepository.countByUserId(userId);
		BigDecimal maintenanceTotal = maintenanceRepository.totalCostByUserId(userId);
		BigDecimal fuelTotal = fuelRepository.totalCostByUserId(userId);

		List<CategoryExpense> categories = maintenanceRepository.totalsByCategory(userId).stream()
				.map(view -> new CategoryExpense(view.getCategory(), view.getTotal()))
				.toList();

		List<Reminder> activeReminders = reminderRepository.findActiveByUserIdWithVehicle(userId);
		long overdueCount = activeReminders.stream()
				.filter(reminder -> reminder.isOverdue(today, reminder.getVehicle().getCurrentMileage()))
				.count();
		List<ReminderItem> upcoming = activeReminders.stream()
				.limit(MAX_UPCOMING_REMINDERS)
				.map(reminder -> new ReminderItem(reminder.getId(), reminder.getVehicle().getId(),
						reminder.getVehicle().displayName(), reminder.getTitle(), reminder.getDueDate(),
						reminder.getDueMileage(),
						reminder.isOverdue(today, reminder.getVehicle().getCurrentMileage())))
				.toList();

		return new DashboardSummaryResponse(vehicleCount, maintenanceTotal, fuelTotal,
				maintenanceTotal.add(fuelTotal), buildMonthlyExpenses(userId, today), categories,
				overdueCount, upcoming, buildRecentActivity(userId));
	}

	private List<MonthlyExpense> buildMonthlyExpenses(UUID userId, LocalDate today) {
		YearMonth currentMonth = YearMonth.from(today);
		YearMonth firstMonth = currentMonth.minusMonths(MONTHS_IN_CHART - 1);
		LocalDate from = firstMonth.atDay(1);

		Map<YearMonth, BigDecimal> maintenanceByMonth = maintenanceRepository
				.findAllByVehicleUserIdAndServiceDateGreaterThanEqual(userId, from).stream()
				.collect(Collectors.groupingBy(record -> YearMonth.from(record.getServiceDate()),
						Collectors.reducing(BigDecimal.ZERO, MaintenanceRecord::getCost, BigDecimal::add)));
		Map<YearMonth, BigDecimal> fuelByMonth = fuelRepository
				.findAllByVehicleUserIdAndRefuelDateGreaterThanEqual(userId, from).stream()
				.collect(Collectors.groupingBy(entry -> YearMonth.from(entry.getRefuelDate()),
						Collectors.reducing(BigDecimal.ZERO, FuelEntry::getTotalCost, BigDecimal::add)));

		List<MonthlyExpense> months = new ArrayList<>(MONTHS_IN_CHART);
		for (int i = 0; i < MONTHS_IN_CHART; i++) {
			YearMonth month = firstMonth.plusMonths(i);
			BigDecimal maintenance = maintenanceByMonth.getOrDefault(month, BigDecimal.ZERO);
			BigDecimal fuel = fuelByMonth.getOrDefault(month, BigDecimal.ZERO);
			months.add(new MonthlyExpense(month.toString(), maintenance, fuel, maintenance.add(fuel)));
		}
		return months;
	}

	private List<ActivityItem> buildRecentActivity(UUID userId) {
		List<ActivityItem> activity = new ArrayList<>();
		for (MaintenanceRecord record : maintenanceRepository.findTop8ByVehicleUserIdOrderByCreatedAtDesc(userId)) {
			activity.add(new ActivityItem("MAINTENANCE", record.getVehicle().getId(),
					record.getVehicle().displayName(), record.getTitle(), record.getServiceDate(),
					record.getCost()));
		}
		for (FuelEntry entry : fuelRepository.findTop8ByVehicleUserIdOrderByCreatedAtDesc(userId)) {
			activity.add(new ActivityItem("FUEL", entry.getVehicle().getId(),
					entry.getVehicle().displayName(), entry.getLiters().toPlainString() + " L",
					entry.getRefuelDate(), entry.getTotalCost()));
		}
		return activity.stream()
				.sorted(Comparator.comparing(ActivityItem::date).reversed())
				.limit(MAX_RECENT_ACTIVITY)
				.toList();
	}
}
