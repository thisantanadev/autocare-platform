package com.autocare.backend.vehicle;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that one user can never read, modify or delete another user's
 * vehicles and related records — the API answers 404 without revealing that
 * the resource exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnershipIsolationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String ownerToken;
	private String intruderToken;
	private String vehicleId;
	private String maintenanceId;
	private String fuelEntryId;
	private String reminderId;

	@BeforeEach
	void setUpTwoUsersWithData() throws Exception {
		ownerToken = registerAndGetToken("owner-" + System.nanoTime() + "@example.com");
		intruderToken = registerAndGetToken("intruder-" + System.nanoTime() + "@example.com");

		vehicleId = JsonPath.read(mockMvc.perform(post("/api/v1/vehicles")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand": "Fiat", "model": "Argo", "manufacturingYear": 2021,
								 "currentMileage": 30000, "fuelType": "FLEX"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");

		maintenanceId = JsonPath.read(mockMvc.perform(
						post("/api/v1/vehicles/" + vehicleId + "/maintenance-records")
								.header("Authorization", "Bearer " + ownerToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"category": "OIL_CHANGE", "title": "Oil change",
										 "serviceDate": "2026-06-01", "mileageAtService": 30500, "cost": 289.90}
										"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");

		fuelEntryId = JsonPath.read(mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/fuel-entries")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refuelDate": "2026-07-01", "odometer": 31000, "liters": 40.5,
								 "totalCost": 250.00, "fullTank": true}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");

		reminderId = JsonPath.read(mockMvc.perform(post("/api/v1/vehicles/" + vehicleId + "/reminders")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Licensing", "dueDate": "2027-01-01"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private String registerAndGetToken(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "User", "email": "%s", "password": "s3curePass!"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}

	@Test
	void anotherUserCannotSeeTheVehicle() throws Exception {
		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/vehicles").header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void anotherUserCannotModifyTheVehicle() throws Exception {
		mockMvc.perform(put("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + intruderToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand": "Hacked", "model": "Car", "manufacturingYear": 2021,
								 "currentMileage": 0, "fuelType": "FLEX"}
								"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void anotherUserCannotAccessChildRecords() throws Exception {
		mockMvc.perform(get("/api/v1/maintenance-records/" + maintenanceId)
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/fuel-entries/" + fuelEntryId)
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete")
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/maintenance-records")
						.header("Authorization", "Bearer " + intruderToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void ownerStillSeesEverything() throws Exception {
		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId)
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.brand").value("Fiat"));

		mockMvc.perform(get("/api/v1/vehicles/" + vehicleId + "/maintenance-records")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}
}
