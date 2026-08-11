package com.autocare.backend.common.error;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {

	public InvalidRefreshTokenException() {
		super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is missing, expired or revoked");
	}
}
