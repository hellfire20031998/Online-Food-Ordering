package com.hellfire.email;

/** Provider-agnostic outbound email. {@code htmlBody} is optional; providers fall back to text. */
public record EmailMessage(String to, String subject, String textBody, String htmlBody) {

    public static EmailMessage text(String to, String subject, String textBody) {
        return new EmailMessage(to, subject, textBody, null);
    }
}
