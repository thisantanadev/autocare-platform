package com.autocare.backend.common.error;

import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends ApiException {

	public EmailAlreadyUsedException() {
		super(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE", "This email is already registered");
	}
}
