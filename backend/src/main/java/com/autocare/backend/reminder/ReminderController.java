package com.autocare.backend.reminder;

import java.util.List;
import java.util.UUID;

import com.autocare.backend.auth.AuthPrincipal;
import com.autocare.backend.reminder.dto.ReminderRequest;
import com.autocare.backend.reminder.dto.ReminderResponse;

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
@RequestMapping("/api/v1")
@Tag(name = "Reminders")
public class ReminderController {

	private final ReminderService service;

	public ReminderController(ReminderService service) {
		this.service = service;
	}

	@PostMapping("/vehicles/{vehicleId}/reminders")
	ResponseEntity<ReminderResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable UUID vehicleId, @Valid @RequestBody ReminderRequest request) {
		ReminderResponse body = service.create(principal.id(), vehicleId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/vehicles/{vehicleId}/reminders")
	List<ReminderResponse> list(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID vehicleId) {
		return service.list(principal.id(), vehicleId);
	}

	@GetMapping("/reminders/{reminderId}")
	ReminderResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID reminderId) {
		return service.get(principal.id(), reminderId);
	}

	@PutMapping("/reminders/{reminderId}")
	ReminderResponse update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID reminderId,
			@Valid @RequestBody ReminderRequest request) {
		return service.update(principal.id(), reminderId, request);
	}

	@PostMapping("/reminders/{reminderId}/complete")
	ReminderResponse complete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID reminderId) {
		return service.complete(principal.id(), reminderId);
	}

	@PostMapping("/reminders/{reminderId}/reopen")
	ReminderResponse reopen(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID reminderId) {
		return service.reopen(principal.id(), reminderId);
	}

	@DeleteMapping("/reminders/{reminderId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID reminderId) {
		service.delete(principal.id(), reminderId);
		return ResponseEntity.noContent().build();
	}
}
