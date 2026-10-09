package com.lifetrack.auth.dto;

import com.lifetrack.common.util.Strings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Strings.normalizeEmail(email);
    }
}
