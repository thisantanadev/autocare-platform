package com.autocare.backend.common.error;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldValidationError> fieldErrors,
		String traceId) {

	public record FieldValidationError(String field, String message) {
	}

	public static ApiErrorResponse of(HttpStatus status, String code, String message, String path) {
		return of(status, code, message, path, List.of());
	}

	public static ApiErrorResponse of(HttpStatus status, String code, String message, String path,
			List<FieldValidationError> fieldErrors) {
		String traceId = UUID.randomUUID().toString().substring(0, 8);
		return new ApiErrorResponse(Instant.now(), status.value(), code, message, path, fieldErrors, traceId);
	}
}
