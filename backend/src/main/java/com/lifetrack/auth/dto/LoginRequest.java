package com.lifetrack.auth.dto;

import com.lifetrack.common.util.Strings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    private String email;

    @NotBlank(message = "Şifre zorunludur")
    private String password;

    public String getEmail() {
        return email;
    }

    // Normalised on input so "Ayse@Test.com" and "ayse@test.com" are the same account.
    public void setEmail(String email) {
        this.email = Strings.normalizeEmail(email);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
