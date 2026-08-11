package com.autocare.backend.auth;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autocare.backend.config.AppProperties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

	private static final String STRONG_SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";

	private AppProperties properties(String secret, Duration accessTtl) {
		return new AppProperties(
				new AppProperties.Jwt(secret, accessTtl, Duration.ofDays(14)),
				new AppProperties.Cookie(false),
				new AppProperties.Cors(List.of("http://localhost:5173")),
				new AppProperties.Analytics("http://localhost:8000", "token", Duration.ofSeconds(2)));
	}

	@Test
	void roundTripsUserIdAndEmail() {
		JwtService service = new JwtService(properties(STRONG_SECRET, Duration.ofMinutes(15)));
		UUID userId = UUID.randomUUID();

		String token = service.createAccessToken(userId, "driver@example.com");
		Optional<AuthPrincipal> principal = service.parseAccessToken(token);

		assertThat(principal).isPresent();
		assertThat(principal.get().id()).isEqualTo(userId);
		assertThat(principal.get().email()).isEqualTo("driver@example.com");
	}

	@Test
	void refusesToStartWithWeakSecret() {
		assertThatThrownBy(() -> new JwtService(properties("too-short", Duration.ofMinutes(15))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void rejectsMalformedToken() {
		JwtService service = new JwtService(properties(STRONG_SECRET, Duration.ofMinutes(15)));

		assertThat(service.parseAccessToken("not-a-jwt")).isEmpty();
	}

	@Test
	void rejectsTokenSignedWithDifferentKey() {
		JwtService issuer = new JwtService(
				properties("another-secret-0123456789abcdef0123456789abcdef", Duration.ofMinutes(15)));
		JwtService verifier = new JwtService(properties(STRONG_SECRET, Duration.ofMinutes(15)));

		String token = issuer.createAccessToken(UUID.randomUUID(), "driver@example.com");

		assertThat(verifier.parseAccessToken(token)).isEmpty();
	}

	@Test
	void rejectsExpiredToken() {
		JwtService service = new JwtService(properties(STRONG_SECRET, Duration.ofMinutes(-5)));

		String expired = service.createAccessToken(UUID.randomUUID(), "driver@example.com");

		assertThat(service.parseAccessToken(expired)).isEmpty();
	}
}
