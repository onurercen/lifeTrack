package com.lifetrack.common.config;

import com.lifetrack.common.mail.LoggingEmailSender;
import com.lifetrack.common.mail.SmtpEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailConfigTest {

    private final MailConfig config = new MailConfig();

    @Test
    void smtpHost_shouldSendThroughSmtp() {
        assertThat(config.emailSender(provider(new JavaMailSenderImpl()), "smtp.example.com", "from@x", true))
            .isInstanceOf(SmtpEmailSender.class);
    }

    @Test
    void emptyHost_shouldCountAsNoSmtp() {
        // docker compose passes SPRING_MAIL_HOST="" when unset, which still creates a JavaMailSender.
        assertThat(config.emailSender(provider(new JavaMailSenderImpl()), "", "from@x", false))
            .isInstanceOf(LoggingEmailSender.class);
        assertThatThrownBy(() -> config.emailSender(provider(new JavaMailSenderImpl()), " ", "from@x", true))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noSmtp_shouldOnlyBeAllowedWhenNotRequired() {
        assertThat(config.emailSender(provider(null), "", "from@x", false)).isInstanceOf(LoggingEmailSender.class);
        assertThatThrownBy(() -> config.emailSender(provider(null), "", "from@x", true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SPRING_MAIL_HOST");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }
}
