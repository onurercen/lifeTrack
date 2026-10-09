package com.lifetrack.support;

import com.lifetrack.common.mail.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces SMTP in tests: keeps sent e-mails so tests can read the codes. */
@Component
@Primary
public class TestMailbox implements EmailSender {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    public record Mail(String to, String subject, String body) {
    }

    private final List<Mail> mails = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        mails.add(new Mail(to, subject, body));
    }

    public List<Mail> mailsTo(String to) {
        return mails.stream().filter(mail -> mail.to().equals(to)).toList();
    }

    /** The code in the latest e-mail to [to]. */
    public String lastCode(String to) {
        List<Mail> received = mailsTo(to);
        if (received.isEmpty()) {
            throw new AssertionError("No e-mail sent to " + to);
        }
        Matcher matcher = CODE.matcher(received.get(received.size() - 1).body());
        if (!matcher.find()) {
            throw new AssertionError("No code in the last e-mail to " + to);
        }
        return matcher.group(1);
    }
}
