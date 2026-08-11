package com.autocare.backend.dashboard;

import com.autocare.backend.auth.AuthPrincipal;
import com.autocare.backend.dashboard.dto.DashboardSummaryResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	DashboardSummaryResponse summary(@AuthenticationPrincipal AuthPrincipal principal) {
		return dashboardService.getSummary(principal.id());
	}
}
