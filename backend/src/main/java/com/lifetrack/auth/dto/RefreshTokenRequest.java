package com.lifetrack.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "Yenileme anahtarı zorunludur") String refreshToken) {
}
