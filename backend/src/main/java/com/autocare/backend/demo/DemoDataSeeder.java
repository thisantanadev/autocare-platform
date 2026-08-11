package com.autocare.backend.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.autocare.backend.fuel.FuelEntry;
import com.autocare.backend.fuel.FuelEntryRepository;
import com.autocare.backend.maintenance.MaintenanceCategory;
import com.autocare.backend.maintenance.MaintenanceRecord;
import com.autocare.backend.maintenance.MaintenanceRecordRepository;
import com.autocare.backend.reminder.Reminder;
import com.autocare.backend.reminder.ReminderRepository;
import com.autocare.backend.user.User;
import com.autocare.backend.user.UserRepository;
import com.autocare.backend.vehicle.FuelType;
import com.autocare.backend.vehicle.Vehicle;
import com.autocare.backend.vehicle.VehicleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a demonstration account with realistic (entirely fictional) data.
 * Only active under the explicit "demo" profile — never in production.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
	private static final String DEMO_EMAIL = "demo@autocare.dev";

	private final UserRepository userRepository;
	private final VehicleRepository vehicleRepository;
	private final MaintenanceRecordRepository maintenanceRepository;
	private final FuelEntryRepository fuelRepository;
	private final ReminderRepository reminderRepository;
	private final PasswordEncoder passwordEncoder;
	private final String demoPassword;

	public DemoDataSeeder(UserRepository userRepository, VehicleRepository vehicleRepository,
			MaintenanceRecordRepository maintenanceRepository, FuelEntryRepository fuelRepository,
			ReminderRepository reminderRepository, PasswordEncoder passwordEncoder,
			@Value("${DEMO_USER_PASSWORD:DemoAutoCare123}") String demoPassword) {
		this.userRepository = userRepository;
		this.vehicleRepository = vehicleRepository;
		this.maintenanceRepository = maintenanceRepository;
		this.fuelRepository = fuelRepository;
		this.reminderRepository = reminderRepository;
		this.passwordEncoder = passwordEncoder;
		this.demoPassword = demoPassword;
	}

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(DEMO_EMAIL)) {
			log.info("Demo user already present, skipping seed");
			return;
		}
		LocalDate today = LocalDate.now();

		User user = userRepository.save(
				new User("Motorista Demo", DEMO_EMAIL, passwordEncoder.encode(demoPassword)));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(user, "Chevrolet", "Onix", 2021, 2022,
				"BRA2E19", 48350, FuelType.FLEX, "Carro do dia a dia"));

		maintenanceRepository.saveAll(List.of(
				new MaintenanceRecord(vehicle, MaintenanceCategory.OIL_CHANGE, "Troca de óleo e filtro",
						"Óleo sintético 5W30 e filtro de óleo", today.minusDays(210), 41800,
						new BigDecimal("289.90"), "Oficina São Jorge", today.minusDays(30), 51800),
				new MaintenanceRecord(vehicle, MaintenanceCategory.BRAKES, "Troca de pastilhas de freio",
						"Pastilhas dianteiras e verificação dos discos", today.minusDays(150), 43900,
						new BigDecimal("458.00"), "Oficina São Jorge", null, null),
				new MaintenanceRecord(vehicle, MaintenanceCategory.TIRES, "Rodízio e balanceamento",
						null, today.minusDays(95), 45200, new BigDecimal("120.00"), "Pneus Center", null,
						null),
				new MaintenanceRecord(vehicle, MaintenanceCategory.FILTERS, "Filtro de ar e de cabine",
						null, today.minusDays(60), 46300, new BigDecimal("165.50"), "Oficina São Jorge",
						null, null),
				new MaintenanceRecord(vehicle, MaintenanceCategory.INSPECTION, "Revisão dos 48 mil km",
						"Revisão completa conforme manual", today.minusDays(20), 48000,
						new BigDecimal("610.00"), "Concessionária", today.plusDays(160), 58000)));

		seedFuelEntries(vehicle, today);

		reminderRepository.saveAll(List.of(
				new Reminder(vehicle, "Licenciamento anual", "Pagar o licenciamento no Detran",
						today.plusDays(45), null),
				new Reminder(vehicle, "Troca da correia dentada", null, null, 60000),
				new Reminder(vehicle, "Calibrar os pneus", null, today.minusDays(5), null)));

		log.info("Demo data seeded for {}", DEMO_EMAIL);
	}

	private void seedFuelEntries(Vehicle vehicle, LocalDate today) {
		int[] odometers = { 41500, 42300, 43100, 43900, 44700, 45500, 46300, 47000, 47700, 48350 };
		String[] liters = { "38.20", "40.10", "39.50", "41.30", "38.90", "40.60", "39.80", "37.40", "40.20",
				"41.00" };
		String[] totals = { "225.38", "240.60", "235.03", "250.69", "231.46", "247.66", "240.79", "228.51",
				"245.62", "251.74" };
		for (int i = 0; i < odometers.length; i++) {
			LocalDate date = today.minusDays(3L + (odometers.length - 1 - i) * 18L);
			BigDecimal litersValue = new BigDecimal(liters[i]);
			BigDecimal totalValue = new BigDecimal(totals[i]);
			BigDecimal pricePerLiter = totalValue.divide(litersValue, 3, RoundingMode.HALF_UP);
			// One partial fill keeps the efficiency calculation honest about gaps.
			boolean fullTank = i != 7;
			fuelRepository.save(new FuelEntry(vehicle, date, odometers[i], litersValue, totalValue,
					pricePerLiter, fullTank));
		}
	}
}
