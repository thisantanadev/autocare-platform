package com.autocare.backend.maintenance;

import java.util.UUID;

import com.autocare.backend.auth.AuthPrincipal;
import com.autocare.backend.common.web.PageResponse;
import com.autocare.backend.maintenance.dto.MaintenanceRecordRequest;
import com.autocare.backend.maintenance.dto.MaintenanceRecordResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Maintenance records")
public class MaintenanceRecordController {

	private final MaintenanceRecordService service;

	public MaintenanceRecordController(MaintenanceRecordService service) {
		this.service = service;
	}

	@PostMapping("/vehicles/{vehicleId}/maintenance-records")
	ResponseEntity<MaintenanceRecordResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId, @Valid @RequestBody MaintenanceRecordRequest request) {
		MaintenanceRecordResponse body = MaintenanceRecordResponse
				.from(service.create(principal.id(), vehicleId, request));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/vehicles/{vehicleId}/maintenance-records")
	PageResponse<MaintenanceRecordResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(principal.id(), vehicleId, page, size);
	}

	@GetMapping("/maintenance-records/{recordId}")
	MaintenanceRecordResponse get(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID recordId) {
		return MaintenanceRecordResponse.from(service.getOwned(principal.id(), recordId));
	}

	@PutMapping("/maintenance-records/{recordId}")
	MaintenanceRecordResponse update(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID recordId, @Valid @RequestBody MaintenanceRecordRequest request) {
		return MaintenanceRecordResponse.from(service.update(principal.id(), recordId, request));
	}

	@DeleteMapping("/maintenance-records/{recordId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID recordId) {
		service.delete(principal.id(), recordId);
		return ResponseEntity.noContent().build();
	}
}
