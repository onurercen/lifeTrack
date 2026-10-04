package com.lifetrack.auth.dto;

import com.lifetrack.common.util.Strings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Ad soyad zorunludur")
    private String name;

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    private String email;

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
