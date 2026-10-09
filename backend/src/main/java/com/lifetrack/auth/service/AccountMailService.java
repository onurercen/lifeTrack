package com.lifetrack.auth.service;

import com.lifetrack.common.mail.EmailSender;
import com.lifetrack.user.entity.User;
import org.springframework.stereotype.Service;

/** The e-mails LifeTrack sends to account owners. */
@Service
public class AccountMailService {

    private final EmailSender emailSender;

    public AccountMailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendVerificationCode(User user, String code) {
        emailSender.send(
            user.getEmail(),
            "LifeTrack doğrulama kodun: " + code,
            greeting(user)
                + "LifeTrack hesabını doğrulamak için bu kodu uygulamaya gir:\n\n"
                + "    " + code + "\n\n"
                + validityNote()
                + "Bu hesabı sen oluşturmadıysan bu e-postayı yok sayabilirsin."
        );
    }

    public void sendPasswordResetCode(User user, String code) {
        emailSender.send(
            user.getEmail(),
            "LifeTrack şifre sıfırlama kodun: " + code,
            greeting(user)
                + "Şifreni sıfırlamak için bu kodu uygulamaya gir:\n\n"
                + "    " + code + "\n\n"
                + validityNote()
                + "Şifre sıfırlamayı sen istemediysen bu e-postayı yok sayabilirsin; şifren değişmez."
        );
    }

    private static String greeting(User user) {
        return "Merhaba " + user.getName() + ",\n\n";
    }

    private static String validityNote() {
        return "Kod " + EmailCodeService.VALIDITY.toMinutes() + " dakika geçerlidir.\n\n";
    }
}
