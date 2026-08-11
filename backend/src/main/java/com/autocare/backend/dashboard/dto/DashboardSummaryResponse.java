package com.autocare.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autocare.backend.maintenance.MaintenanceCategory;

public record DashboardSummaryResponse(
		long vehicleCount,
		BigDecimal maintenanceTotal,
		BigDecimal fuelTotal,
		BigDecimal combinedTotal,
		List<MonthlyExpense> monthlyExpenses,
		List<CategoryExpense> expensesByCategory,
		long overdueReminderCount,
		List<ReminderItem> upcomingReminders,
		List<ActivityItem> recentActivity) {

	public record MonthlyExpense(String month, BigDecimal maintenance, BigDecimal fuel, BigDecimal total) {
	}

	public record CategoryExpense(MaintenanceCategory category, BigDecimal total) {
	}

	public record ReminderItem(UUID id, UUID vehicleId, String vehicleName, String title, LocalDate dueDate,
			Integer dueMileage, boolean overdue) {
	}

	public record ActivityItem(String type, UUID vehicleId, String vehicleName, String title, LocalDate date,
			BigDecimal amount) {
	}
}
