package com.autocare.backend.common.error;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(ex.getStatus(), ex.getCode(), ex.getMessage(),
				request.getRequestURI());
		if (ex.getStatus().is5xxServerError()) {
			log.error("[{}] {} {}", body.traceId(), ex.getCode(), ex.getMessage());
		}
		return ResponseEntity.status(ex.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ApiErrorResponse.FieldValidationError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
				"Validation failed for one or more fields", request.getRequestURI(), fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
				"Request body is missing or malformed", request.getRequestURI());
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
				"Parameter '" + ex.getName() + "' has an invalid value", request.getRequestURI());
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
				"HTTP method not allowed for this endpoint", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
				"Resource not found", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
				"Operation violates a data constraint", request.getRequestURI());
		log.warn("[{}] Data integrity violation on {}", body.traceId(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		ApiErrorResponse body = ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
				"An unexpected error occurred", request.getRequestURI());
		log.error("[{}] Unexpected error on {}", body.traceId(), request.getRequestURI(), ex);
		return ResponseEntity.internalServerError().body(body);
	}
}
