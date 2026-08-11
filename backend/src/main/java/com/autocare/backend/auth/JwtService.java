package com.autocare.backend.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.autocare.backend.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final int MIN_SECRET_BYTES = 32;

	private final SecretKey signingKey;
	private final Duration accessTokenTtl;

	public JwtService(AppProperties properties) {
		String secret = properties.jwt().secret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			// Fail fast at startup: running with a missing or weak signing key would
			// silently produce forgeable tokens.
			throw new IllegalStateException(
					"JWT_SECRET must be configured with at least " + MIN_SECRET_BYTES + " bytes");
		}
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenTtl = properties.jwt().accessTokenTtl();
	}

	public String createAccessToken(UUID userId, String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(accessTokenTtl)))
				.signWith(signingKey)
				.compact();
	}

	public Optional<AuthPrincipal> parseAccessToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(new AuthPrincipal(
					UUID.fromString(claims.getSubject()),
					claims.get("email", String.class)));
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public long accessTokenTtlSeconds() {
		return accessTokenTtl.toSeconds();
	}
}
