package com.autocare.backend.analytics;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autocare.backend.analytics.dto.VehicleAnalyticsRequest;
import com.autocare.backend.common.error.AnalyticsUnavailableException;
import com.autocare.backend.config.AppProperties;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsClientTest {

	@Test
	void mapsConnectionFailuresToAnalyticsUnavailable() {
		// Nothing listens on this port, so the call fails at the transport level.
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("secret-0123456789abcdef0123456789abcdef", Duration.ofMinutes(15),
						Duration.ofDays(14)),
				new AppProperties.Cookie(false),
				new AppProperties.Cors(List.of()),
				new AppProperties.Analytics("http://127.0.0.1:59999", "token", Duration.ofSeconds(1)));
		AnalyticsClient client = new AnalyticsClient(properties, RestClient.builder());

		VehicleAnalyticsRequest payload = new VehicleAnalyticsRequest(
				new VehicleAnalyticsRequest.VehicleInfo(UUID.randomUUID(), 10000, "FLEX", 2021),
				LocalDate.now(), List.of(), List.of());

		assertThatThrownBy(() -> client.analyzeVehicle(payload))
				.isInstanceOf(AnalyticsUnavailableException.class);
	}
}
