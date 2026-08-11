package com.autocare.backend.auth;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

	private static final String REFRESH_COOKIE = "autocare_refresh";

	@Autowired
	private MockMvc mockMvc;

	private MvcResult register(String email) throws Exception {
		String body = """
				{"name": "Ana Souza", "email": "%s", "password": "s3curePass!"}
				""".formatted(email);
		return mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void registerReturnsTokensAndHttpOnlyRefreshCookie() throws Exception {
		MvcResult result = register("register@example.com");

		String json = result.getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(json, "$.accessToken")).isNotBlank();
		assertThat(JsonPath.<String>read(json, "$.tokenType")).isEqualTo("Bearer");
		assertThat(JsonPath.<String>read(json, "$.user.email")).isEqualTo("register@example.com");

		Cookie refreshCookie = result.getResponse().getCookie(REFRESH_COOKIE);
		assertThat(refreshCookie).isNotNull();
		assertThat(refreshCookie.isHttpOnly()).isTrue();
		assertThat(refreshCookie.getPath()).isEqualTo("/api/v1/auth");
	}

	@Test
	void registerNormalizesEmailAndRejectsDuplicates() throws Exception {
		register("duplicate@example.com");

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Other", "email": "DUPLICATE@example.com", "password": "s3curePass!"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"))
				.andExpect(jsonPath("$.traceId").isNotEmpty());
	}

	@Test
	void loginWithWrongPasswordReturnsGenericError() throws Exception {
		register("wrongpass@example.com");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "wrongpass@example.com", "password": "not-the-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void meReturnsTheAuthenticatedUser() throws Exception {
		MvcResult result = register("me@example.com");
		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("me@example.com"))
				.andExpect(jsonPath("$.name").value("Ana Souza"));
	}

	@Test
	void protectedEndpointsReturnStructured401WithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
	}

	@Test
	void refreshRotatesTokenAndDetectsReuse() throws Exception {
		MvcResult registered = register("rotation@example.com");
		Cookie firstCookie = registered.getResponse().getCookie(REFRESH_COOKIE);

		MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
				.andExpect(status().isOk())
				.andReturn();
		Cookie secondCookie = refreshed.getResponse().getCookie(REFRESH_COOKIE);
		assertThat(secondCookie.getValue()).isNotEqualTo(firstCookie.getValue());

		// Replaying the rotated token must fail and revoke every session.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(secondCookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
		MvcResult registered = register("logout@example.com");
		Cookie refreshCookie = registered.getResponse().getCookie(REFRESH_COOKIE);

		mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registerValidatesInputAndReportsFieldErrors() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "", "email": "not-an-email", "password": "short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors").isArray())
				.andExpect(jsonPath("$.fieldErrors.length()").value(3));
	}
}
