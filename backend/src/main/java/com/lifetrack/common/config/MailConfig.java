package com.lifetrack.common.config;

import com.lifetrack.common.mail.EmailSender;
import com.lifetrack.common.mail.LoggingEmailSender;
import com.lifetrack.common.mail.SmtpEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class MailConfig {

    /**
     * Spring Boot creates a JavaMailSender when SPRING_MAIL_HOST is set, even to
     * an empty value (as docker compose passes unset variables), so the host is
     * checked too. Without one, e-mails are logged; the prod profile refuses to
     * start instead, since users could neither verify their address nor reset
     * their password.
     */
    @Bean
    public EmailSender emailSender(
        ObjectProvider<JavaMailSender> mailSender,
        @Value("${spring.mail.host:}") String host,
        @Value("${app.mail.from}") String from,
        @Value("${app.mail.required:false}") boolean required
    ) {
        JavaMailSender sender = host.isBlank() ? null : mailSender.getIfAvailable();
        if (sender != null) {
            return new SmtpEmailSender(sender, from);
        }
        if (required) {
            throw new IllegalStateException("SPRING_MAIL_HOST must be set: e-mail is required in this profile");
        }
        return new LoggingEmailSender();
    }
}
