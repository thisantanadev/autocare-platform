package com.autocare.backend.vehicle;

import java.util.List;
import java.util.UUID;

import com.autocare.backend.auth.AuthPrincipal;
import com.autocare.backend.vehicle.dto.VehicleRequest;
import com.autocare.backend.vehicle.dto.VehicleResponse;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles")
public class VehicleController {

	private final VehicleService vehicleService;

	public VehicleController(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	@GetMapping
	List<VehicleResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
		return vehicleService.list(principal.id()).stream().map(VehicleResponse::from).toList();
	}

	@PostMapping
	ResponseEntity<VehicleResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody VehicleRequest request) {
		VehicleResponse body = VehicleResponse.from(vehicleService.create(principal.id(), request));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{vehicleId}")
	VehicleResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID vehicleId) {
		return VehicleResponse.from(vehicleService.getOwned(principal.id(), vehicleId));
	}

	@PutMapping("/{vehicleId}")
	VehicleResponse update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID vehicleId,
			@Valid @RequestBody VehicleRequest request) {
		return VehicleResponse.from(vehicleService.update(principal.id(), vehicleId, request));
	}

	@DeleteMapping("/{vehicleId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID vehicleId) {
		vehicleService.delete(principal.id(), vehicleId);
		return ResponseEntity.noContent().build();
	}
}
