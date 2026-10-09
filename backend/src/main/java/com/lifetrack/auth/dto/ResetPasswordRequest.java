package com.lifetrack.auth.dto;

import com.lifetrack.common.util.Strings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçerli bir e-posta giriniz")
    private String email;

    @NotBlank(message = "Kod zorunludur")
    @Pattern(regexp = "\\d{6}", message = "Kod 6 haneli olmalıdır")
    private String code;

    @NotBlank(message = "Yeni şifre zorunludur")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Strings.normalizeEmail(email);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
