package com.lifetrack.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Ad soyad zorunludur")
    @Size(max = 255, message = "Ad soyad en fazla 255 karakter olabilir")
    String name
) {
}
