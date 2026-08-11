package com.autocare.backend.fuel;

import java.util.UUID;

import com.autocare.backend.auth.AuthPrincipal;
import com.autocare.backend.common.web.PageResponse;
import com.autocare.backend.fuel.dto.FuelEntryRequest;
import com.autocare.backend.fuel.dto.FuelEntryResponse;

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
@Tag(name = "Fuel entries")
public class FuelEntryController {

	private final FuelEntryService service;

	public FuelEntryController(FuelEntryService service) {
		this.service = service;
	}

	@PostMapping("/vehicles/{vehicleId}/fuel-entries")
	ResponseEntity<FuelEntryResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId, @Valid @RequestBody FuelEntryRequest request) {
		FuelEntryResponse body = FuelEntryResponse.from(service.create(principal.id(), vehicleId, request));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/vehicles/{vehicleId}/fuel-entries")
	PageResponse<FuelEntryResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(principal.id(), vehicleId, page, size);
	}

	@GetMapping("/fuel-entries/{entryId}")
	FuelEntryResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID entryId) {
		return FuelEntryResponse.from(service.getOwned(principal.id(), entryId));
	}

	@PutMapping("/fuel-entries/{entryId}")
	FuelEntryResponse update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID entryId,
			@Valid @RequestBody FuelEntryRequest request) {
		return FuelEntryResponse.from(service.update(principal.id(), entryId, request));
	}

	@DeleteMapping("/fuel-entries/{entryId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID entryId) {
		service.delete(principal.id(), entryId);
		return ResponseEntity.noContent().build();
	}
}
