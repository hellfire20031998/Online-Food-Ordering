package com.hellfire.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default provider: no email leaves the machine, the message is logged instead. */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("EMAIL (not sent, provider=log) to={} subject='{}'\n{}", message.to(), message.subject(), message.textBody());
    }

    @Override
    public String providerName() {
        return "log";
    }
}
