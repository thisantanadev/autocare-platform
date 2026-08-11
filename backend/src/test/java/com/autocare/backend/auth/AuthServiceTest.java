package com.autocare.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autocare.backend.auth.dto.LoginRequest;
import com.autocare.backend.auth.dto.RegisterRequest;
import com.autocare.backend.common.error.EmailAlreadyUsedException;
import com.autocare.backend.common.error.InvalidCredentialsException;
import com.autocare.backend.common.error.InvalidRefreshTokenException;
import com.autocare.backend.config.AppProperties;
import com.autocare.backend.user.User;
import com.autocare.backend.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtService jwtService;

	// Low strength keeps the test fast while still exercising real hashing.
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("secret", Duration.ofMinutes(15), Duration.ofDays(14)),
				new AppProperties.Cookie(false),
				new AppProperties.Cors(List.of()),
				new AppProperties.Analytics("http://localhost:8000", "token", Duration.ofSeconds(2)));
		authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService,
				properties);
	}

	@Test
	void registerNormalizesEmailAndHashesPassword() {
		when(userRepository.existsByEmail("driver@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(jwtService.createAccessToken(any(), any())).thenReturn("access-token");

		AuthService.AuthResult result = authService
				.register(new RegisterRequest("Ana", "  Driver@Example.COM ", "s3cretPass"));

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertThat(savedUser.getValue().getEmail()).isEqualTo("driver@example.com");
		assertThat(savedUser.getValue().getPasswordHash()).isNotEqualTo("s3cretPass");
		assertThat(passwordEncoder.matches("s3cretPass", savedUser.getValue().getPasswordHash())).isTrue();
		assertThat(result.refreshToken()).isNotBlank();
	}

	@Test
	void registerStoresOnlyTheHashOfTheRefreshToken() {
		when(userRepository.existsByEmail(any())).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(jwtService.createAccessToken(any(), any())).thenReturn("access-token");

		AuthService.AuthResult result = authService
				.register(new RegisterRequest("Ana", "driver@example.com", "s3cretPass"));

		ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(savedToken.capture());
		assertThat(savedToken.getValue().isActive(Instant.now())).isTrue();
		// The raw value handed to the client is 32 random bytes, base64url-encoded.
		assertThat(result.refreshToken()).hasSize(43).matches("[A-Za-z0-9_-]+");
		// The stored lookup key is the SHA-256 hex of that raw value.
		assertThat(refreshTokenRepository.findByTokenHash(sha256(result.refreshToken()))).isNotNull();
	}

	@Test
	void registerRejectsDuplicateEmail() {
		when(userRepository.existsByEmail("driver@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService
				.register(new RegisterRequest("Ana", "driver@example.com", "s3cretPass")))
				.isInstanceOf(EmailAlreadyUsedException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void loginRejectsUnknownEmailWithGenericError() {
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void loginRejectsWrongPasswordWithGenericError() {
		User user = new User("Ana", "driver@example.com", passwordEncoder.encode("correct-pass"));
		when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("driver@example.com", "wrong-pass")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void refreshRotatesTheToken() {
		User user = new User("Ana", "driver@example.com", "hash");
		String rawToken = "raw-refresh-token";
		RefreshToken stored = new RefreshToken(user, sha256(rawToken), Instant.now().plusSeconds(3600));
		when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));
		when(jwtService.createAccessToken(any(), any())).thenReturn("new-access-token");

		AuthService.AuthResult result = authService.refresh(rawToken);

		assertThat(stored.getRevokedAt()).isNotNull();
		verify(refreshTokenRepository).save(any(RefreshToken.class));
		assertThat(result.refreshToken()).isNotEqualTo(rawToken);
	}

	@Test
	void refreshWithRevokedTokenRevokesAllUserSessions() {
		User user = new User("Ana", "driver@example.com", "hash");
		String rawToken = "stolen-token";
		RefreshToken stored = new RefreshToken(user, sha256(rawToken), Instant.now().plusSeconds(3600));
		stored.revoke(Instant.now());
		when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh(rawToken))
				.isInstanceOf(InvalidRefreshTokenException.class);
		verify(refreshTokenRepository).revokeAllActiveForUser(eq(user.getId()), any(Instant.class));
	}

	@Test
	void logoutRevokesThePresentedToken() {
		User user = new User("Ana", "driver@example.com", "hash");
		String rawToken = "active-token";
		RefreshToken stored = new RefreshToken(user, sha256(rawToken), Instant.now().plusSeconds(3600));
		when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));

		authService.logout(rawToken);

		assertThat(stored.getRevokedAt()).isNotNull();
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
