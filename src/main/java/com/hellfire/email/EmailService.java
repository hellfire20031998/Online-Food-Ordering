package com.hellfire.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget facade over the active {@link EmailSender}. Sending happens on a background
 * thread so an unreachable mail server never fails a user request; failures are logged.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender sender;

    public EmailService(EmailSender sender) {
        this.sender = sender;
        log.info("Email provider: {}", sender.providerName());
    }

    @Async
    public void sendAsync(EmailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            log.warn("Skipping email with no recipient: {}", message);
            return;
        }
        try {
            sender.send(message);
            log.info("Email sent via {} to {} ('{}')", sender.providerName(), message.to(), message.subject());
        } catch (Exception e) {
            log.error("Email to {} ('{}') failed via {}: {}", message.to(), message.subject(),
                    sender.providerName(), e.getMessage());
        }
    }
}
