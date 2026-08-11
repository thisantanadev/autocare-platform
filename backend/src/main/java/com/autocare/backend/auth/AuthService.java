package com.autocare.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import com.autocare.backend.auth.dto.LoginRequest;
import com.autocare.backend.auth.dto.RegisterRequest;
import com.autocare.backend.common.error.EmailAlreadyUsedException;
import com.autocare.backend.common.error.InvalidCredentialsException;
import com.autocare.backend.common.error.InvalidRefreshTokenException;
import com.autocare.backend.common.error.ResourceNotFoundException;
import com.autocare.backend.config.AppProperties;
import com.autocare.backend.user.User;
import com.autocare.backend.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AppProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties properties) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.properties = properties;
	}

	public AuthResult register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException();
		}
		User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()));
		userRepository.save(user);
		return issueTokens(user);
	}

	public AuthResult login(LoginRequest request) {
		User user = userRepository.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return issueTokens(user);
	}

	// noRollbackFor keeps the containment update (revoke-all) committed even
	// though the request itself fails with InvalidRefreshTokenException.
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public AuthResult refresh(String rawRefreshToken) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
				.orElseThrow(InvalidRefreshTokenException::new);
		Instant now = Instant.now();
		if (!stored.isActive(now)) {
			// A rotated or revoked token being presented again suggests it was stolen:
			// revoke every active session for this user as a containment measure.
			refreshTokenRepository.revokeAllActiveForUser(stored.getUser().getId(), now);
			throw new InvalidRefreshTokenException();
		}
		stored.revoke(now);
		return issueTokens(stored.getUser());
	}

	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
				.ifPresent(token -> token.revoke(Instant.now()));
	}

	@Transactional(readOnly = true)
	public User getUser(UUID userId) {
		return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private AuthResult issueTokens(User user) {
		String rawRefreshToken = generateRefreshTokenValue();
		Instant expiresAt = Instant.now().plus(properties.jwt().refreshTokenTtl());
		refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), expiresAt));
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return new AuthResult(accessToken, jwtService.accessTokenTtlSeconds(), rawRefreshToken, user);
	}

	private String generateRefreshTokenValue() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required but not available", ex);
		}
	}

	public record AuthResult(String accessToken, long expiresInSeconds, String refreshToken, User user) {
	}
}
