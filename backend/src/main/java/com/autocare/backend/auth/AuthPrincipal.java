package com.autocare.backend.auth;

import java.util.UUID;

/**
 * Authenticated identity extracted from a verified access token. Stored as the
 * security context principal so controllers never need to re-parse the JWT.
 */
public record AuthPrincipal(UUID id, String email) {
}
