package com.autocare.backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.autocare.backend.analytics.AnalyticsClient;
import com.autocare.backend.analytics.dto.AnalyticsReport;
import com.autocare.backend.analytics.dto.VehicleAnalyticsRequest;
import com.autocare.backend.common.error.AnalyticsUnavailableException;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end journey through the API: register, create a vehicle, record
 * maintenance/fuel/reminders and read the dashboard and analytics back.
 * The Python analytics service is mocked at the client boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HappyPathIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AnalyticsClient analyticsClient;

	private String token;
	private String vehicleId;

	@BeforeEach
	void registerUserAndVehicle() throws Exception {
		String email = "journey-" + System.nanoTime() + "@example.com";
		token = JsonPath.read(mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Ana Souza", "email": "%s", "password": "s3curePass!"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.accessToken");

		vehicleId = JsonPath.read(mockMvc.perform(post("/api/v1/vehicles")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand": "Chevrolet", "model": "Onix", "manufacturingYear": 2021,
								 "modelYear": 2022, "licensePlate": "abc1d23", "currentMileage": 30000,
								 "fuelType": "FLEX", "nickname": "Daily driver"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.licensePlate").value("ABC1D23"))
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void fullJourneyThroughDashboard() throws Exception {
		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel-entries")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refuelDate": "%s", "odometer": 30500, "liters": 40.0,
								 "totalCost": 240.00, "fullTank": true}
								""".formatted(LocalDate.now().minusDays(10))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pricePerLiter").value(6.000));

		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel-entries")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refuelDate": "%s", "odometer": 31000, "liters": 38.0,
								 "totalCost": 231.80, "fullTank": true}
								""".formatted(LocalDate.now().minusDays(2))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/maintenance-records")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"category": "OIL_CHANGE", "title": "Troca de óleo",
								 "serviceDate": "%s", "mileageAtService": 30800, "cost": 289.90}
								""".formatted(LocalDate.now().minusDays(5))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/reminders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Licenciamento", "dueDate": "%s"}
								""".formatted(LocalDate.now().minusDays(1))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.overdue").value(true));

		// Fuel and maintenance records raised the vehicle mileage to 31000.
		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentMileage").value(31000));

		mockMvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vehicleCount").value(1))
				.andExpect(jsonPath("$.fuelTotal").value(471.80))
				.andExpect(jsonPath("$.maintenanceTotal").value(289.90))
				.andExpect(jsonPath("$.combinedTotal").value(761.70))
				.andExpect(jsonPath("$.overdueReminderCount").value(1))
				.andExpect(jsonPath("$.expensesByCategory[0].category").value("OIL_CHANGE"))
				.andExpect(jsonPath("$.recentActivity.length()").value(3))
				.andExpect(jsonPath("$.monthlyExpenses.length()").value(6));
	}

	@Test
	void analyticsEndpointForwardsSanitizedVehicleData() throws Exception {
		AnalyticsReport report = new AnalyticsReport(
				new AnalyticsReport.Totals(new BigDecimal("289.90"), new BigDecimal("471.80"),
						new BigDecimal("761.70")),
				List.of(), List.of(),
				new AnalyticsReport.FuelStats(new BigDecimal("78.0"), new BigDecimal("6.05"), null, null),
				new AnalyticsReport.Trend("STABLE", new BigDecimal("761.70"), null),
				new AnalyticsReport.PeriodComparison(90, new BigDecimal("761.70"), BigDecimal.ZERO, null),
				List.of(), List.of(new AnalyticsReport.Warning("INSUFFICIENT_FUEL_DATA",
						"At least 3 full-tank entries are needed")));
		when(analyticsClient.analyzeVehicle(any())).thenReturn(report);

		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel-entries")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refuelDate": "%s", "odometer": 30500, "liters": 40.0,
								 "totalCost": 240.00, "fullTank": true}
								""".formatted(LocalDate.now().minusDays(3))))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/analytics")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totals.operatingCost").value(761.70))
				.andExpect(jsonPath("$.warnings[0].code").value("INSUFFICIENT_FUEL_DATA"));

		ArgumentCaptor<VehicleAnalyticsRequest> payload = ArgumentCaptor
				.forClass(VehicleAnalyticsRequest.class);
		verify(analyticsClient).analyzeVehicle(payload.capture());
		assertThat(payload.getValue().vehicle().id().toString()).isEqualTo(vehicleId);
		assertThat(payload.getValue().fuelEntries()).hasSize(1);
		assertThat(payload.getValue().vehicle().fuelType()).isEqualTo("FLEX");
	}

	@Test
	void analyticsOutageDegradesGracefully() throws Exception {
		when(analyticsClient.analyzeVehicle(any())).thenThrow(new AnalyticsUnavailableException());

		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/analytics")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("ANALYTICS_UNAVAILABLE"));

		// Core vehicle management keeps working while analytics is down.
		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void businessRulesAreEnforcedThroughTheApi() throws Exception {
		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel-entries")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refuelDate": "2026-07-01", "odometer": 31000, "liters": 0,
								 "totalCost": 100.00}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/reminders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "No due condition"}
								"""))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
	}
}
