package com.autocare.backend.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autocare")
public record AppProperties(Jwt jwt, Cookie cookie, Cors cors, Analytics analytics) {

	public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
	}

	public record Cookie(boolean secure) {
	}

	public record Cors(List<String> allowedOrigins) {
	}

	public record Analytics(String baseUrl, String internalToken, Duration timeout) {
	}
}
