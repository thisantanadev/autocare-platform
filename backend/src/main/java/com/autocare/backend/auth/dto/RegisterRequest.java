package com.autocare.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Email @Size(max = 255) String email,
		// BCrypt only uses the first 72 bytes of input, so longer passwords are rejected.
		@NotBlank @Size(min = 8, max = 72) String password) {
}
