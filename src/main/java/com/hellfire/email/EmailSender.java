package com.hellfire.email;

/**
 * Transport abstraction. Exactly one implementation is active, chosen by {@code app.email.provider}:
 * <ul>
 *   <li>{@code log} (default): writes the message to the application log; for local dev and tests.</li>
 *   <li>{@code smtp}: any SMTP relay via Spring Mail. Gmail (App Password) and Amazon SES's SMTP
 *       interface are both configured this way, see application.properties.</li>
 *   <li>{@code resend}: Resend's HTTP API.</li>
 * </ul>
 * Implementations are synchronous and may throw; {@link EmailService} handles retries/async.
 */
public interface EmailSender {

    void send(EmailMessage message);

    String providerName();
}
