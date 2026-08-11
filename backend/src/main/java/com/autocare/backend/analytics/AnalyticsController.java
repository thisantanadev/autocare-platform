package com.autocare.backend.analytics;

import java.util.UUID;

import com.autocare.backend.analytics.dto.AnalyticsReport;
import com.autocare.backend.auth.AuthPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping
	AnalyticsReport getVehicleAnalytics(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId) {
		return analyticsService.getVehicleAnalytics(principal.id(), vehicleId);
	}
}
