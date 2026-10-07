package com.lifetrack.auth.dto;

/**
 * [token] is the short-lived access token (JWT) for the Authorization header;
 * [refreshToken] is exchanged for a new pair at /api/auth/refresh.
 */
public record AuthResponse(String token, String refreshToken, UserResponse user) {
}
