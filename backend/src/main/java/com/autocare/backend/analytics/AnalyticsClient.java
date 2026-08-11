package com.autocare.backend.analytics;

import com.autocare.backend.analytics.dto.AnalyticsReport;
import com.autocare.backend.analytics.dto.VehicleAnalyticsRequest;
import com.autocare.backend.common.error.AnalyticsUnavailableException;
import com.autocare.backend.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AnalyticsClient {

	private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);

	private final RestClient restClient;
	private final String internalToken;

	public AnalyticsClient(AppProperties properties, RestClient.Builder builder) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.analytics().timeout());
		requestFactory.setReadTimeout(properties.analytics().timeout());
		this.restClient = builder
				.baseUrl(properties.analytics().baseUrl())
				.requestFactory(requestFactory)
				.build();
		this.internalToken = properties.analytics().internalToken();
	}

	/**
	 * Analytics failures never break vehicle management: any transport or
	 * server error surfaces as a 503 with a friendly message.
	 */
	public AnalyticsReport analyzeVehicle(VehicleAnalyticsRequest payload) {
		try {
			return restClient.post()
					.uri("/internal/v1/analytics/vehicle")
					.header("X-Internal-Token", internalToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(AnalyticsReport.class);
		}
		catch (RestClientException ex) {
			log.warn("Analytics service call failed: {}", ex.getMessage());
			throw new AnalyticsUnavailableException();
		}
	}
}
