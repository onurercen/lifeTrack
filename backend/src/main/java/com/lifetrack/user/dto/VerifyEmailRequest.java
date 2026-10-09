package com.lifetrack.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
    @NotBlank(message = "Kod zorunludur")
    @Pattern(regexp = "\\s*\\d{6}\\s*", message = "Kod 6 haneli olmalıdır")
    String code
) {
}
