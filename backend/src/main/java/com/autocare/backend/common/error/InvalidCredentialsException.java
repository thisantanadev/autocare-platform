package com.autocare.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Deliberately generic: the same error is returned for unknown email and for
 * wrong password, so login responses cannot be used to enumerate accounts.
 */
public class InvalidCredentialsException extends ApiException {

	public InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
	}
}
