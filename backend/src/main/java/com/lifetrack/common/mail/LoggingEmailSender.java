package com.lifetrack.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local development without an SMTP server: e-mails (and their codes) go to the log. */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.warn("SMTP is not configured, e-mail not sent.\nTo: {}\nSubject: {}\n\n{}", to, subject, body);
    }
}
