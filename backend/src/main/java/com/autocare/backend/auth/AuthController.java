package com.autocare.backend.auth;

import java.time.Duration;

import com.autocare.backend.auth.dto.AuthResponse;
import com.autocare.backend.auth.dto.LoginRequest;
import com.autocare.backend.auth.dto.RegisterRequest;
import com.autocare.backend.common.error.InvalidRefreshTokenException;
import com.autocare.backend.config.AppProperties;
import com.autocare.backend.user.UserResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

	static final String REFRESH_COOKIE = "autocare_refresh";

	private final AuthService authService;
	private final AppProperties properties;

	public AuthController(AuthService authService, AppProperties properties) {
		this.authService = authService;
		this.properties = properties;
	}

	@PostMapping("/register")
	ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return respondWithTokens(authService.register(request), HttpStatus.CREATED);
	}

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return respondWithTokens(authService.login(request), HttpStatus.OK);
	}

	@PostMapping("/refresh")
	ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		return respondWithTokens(authService.refresh(refreshToken), HttpStatus.OK);
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		authService.logout(refreshToken);
		ResponseCookie expiredCookie = refreshCookie("", Duration.ZERO);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
		return UserResponse.from(authService.getUser(principal.id()));
	}

	private ResponseEntity<AuthResponse> respondWithTokens(AuthService.AuthResult result, HttpStatus status) {
		ResponseCookie cookie = refreshCookie(result.refreshToken(), properties.jwt().refreshTokenTtl());
		AuthResponse body = new AuthResponse(result.accessToken(), "Bearer", result.expiresInSeconds(),
				UserResponse.from(result.user()));
		return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
	}

	private ResponseCookie refreshCookie(String value, Duration maxAge) {
		// HttpOnly + SameSite=Strict + a path scoped to the auth endpoints keeps the
		// refresh token out of reach of scripts and of cross-site requests.
		return ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(properties.cookie().secure())
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(maxAge)
				.build();
	}
}
