package com.lifetrack.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mevcut şifre zorunludur")
    String currentPassword,

    @NotBlank(message = "Yeni şifre zorunludur")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    String newPassword
) {
}
