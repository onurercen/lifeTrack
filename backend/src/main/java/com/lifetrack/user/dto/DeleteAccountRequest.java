package com.lifetrack.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank(message = "Şifre zorunludur") String password) {
}
