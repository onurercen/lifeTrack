package com.lifetrack.common.mail;

/** Sends plain-text e-mails. Implementations must not throw: a failed send is logged. */
public interface EmailSender {

    void send(String to, String subject, String body);
}
