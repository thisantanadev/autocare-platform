package com.autocare.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Also raised when a resource exists but belongs to another user, so an
 * attacker cannot distinguish "not mine" from "does not exist".
 */
public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String resourceName) {
		super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resourceName + " not found");
	}
}
