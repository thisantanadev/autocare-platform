package com.autocare.backend.auth.dto;

import com.autocare.backend.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
