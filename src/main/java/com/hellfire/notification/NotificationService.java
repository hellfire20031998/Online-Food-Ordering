package com.hellfire.notification;

import com.hellfire.email.EmailMessage;
import com.hellfire.email.EmailService;
import com.hellfire.model.Payout;
import com.hellfire.model.Refund;
import com.hellfire.model.RefundMethod;
import com.hellfire.model.RestaurantApplication;
import com.hellfire.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds the platform's transactional emails. Messages are assembled synchronously (while the
 * entities are still attached) and handed to {@link EmailService} for asynchronous delivery.
 */
@Service
public class NotificationService {

    private final EmailService emailService;
    private final String frontendUrl;
    private final String platformName;

    public NotificationService(EmailService emailService,
                               @Value("${app.frontend-url}") String frontendUrl,
                               @Value("${app.name:Foodiyapa}") String platformName) {
        this.emailService = emailService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.platformName = platformName;
    }

    public void applicationReceived(RestaurantApplication app) {
        String name = app.getApplicant().getFullName();
        emailService.sendAsync(build(app.getApplicant().getEmail(),
                "We received your application for " + app.getRestaurantName(),
                List.of(
                        "Hi " + name + ",",
                        "Thanks for applying to list " + app.getRestaurantName() + " on " + platformName + ".",
                        "Our team reviews applications within a few business days. We will email you as soon as a decision is made.",
                        "You can check the status any time at " + frontendUrl + "/partner")));
    }

    public void applicationApproved(RestaurantApplication app) {
        String name = app.getApplicant().getFullName();
        emailService.sendAsync(build(app.getApplicant().getEmail(),
                app.getRestaurantName() + " has been approved",
                List.of(
                        "Hi " + name + ",",
                        "Good news: " + app.getRestaurantName() + " is now live on " + platformName + " and your account has restaurant owner access.",
                        "Please sign out and sign in again, then open your dashboard at " + frontendUrl + "/admin/restaurant to add your menu and open for orders.",
                        "Payouts will be made to the bank account you provided; you can update it from the restaurant details page.")));
    }

    public void applicationRejected(RestaurantApplication app) {
        String name = app.getApplicant().getFullName();
        emailService.sendAsync(build(app.getApplicant().getEmail(),
                "Update on your application for " + app.getRestaurantName(),
                List.of(
                        "Hi " + name + ",",
                        "We reviewed your application for " + app.getRestaurantName() + " and could not approve it at this time.",
                        "Reason: " + app.getRejectionReason(),
                        "You are welcome to address the points above and apply again at " + frontendUrl + "/partner")));
    }

    public void teamMemberCreated(User member, String createdBy) {
        emailService.sendAsync(build(member.getEmail(),
                "Your " + platformName + " team account",
                List.of(
                        "Hi " + member.getFullName() + ",",
                        createdBy + " created a " + platformName + " team account for you with the role " + member.getRole() + ".",
                        "Sign in at " + frontendUrl + "/account/login with the temporary password they shared with you, then open the console at " + frontendUrl + "/team.")));
    }

    public void refundRequested(Refund refund) {
        User customer = refund.getPayment().getCustomer();
        emailService.sendAsync(build(customer.getEmail(),
                "Refund request received for order #" + refund.getOrder().getId(),
                List.of(
                        "Hi " + customer.getFullName() + ",",
                        "We received your request to refund " + money(refund.getAmount(), refund.getPayment().getCurrency())
                                + " for order #" + refund.getOrder().getId() + ".",
                        "Our team reviews refunds within a few business days and will email you once it is processed.")));
    }

    public void refundCompleted(Refund refund) {
        User customer = refund.getPayment().getCustomer();
        String how = refund.getMethod() == RefundMethod.BANK_TRANSFER
                ? "It was transferred to the bank account you provided" + (refund.getReferenceNumber() != null
                        ? " (reference " + refund.getReferenceNumber() + ")" : "") + "."
                : "It was returned to your original payment method and should appear within 5 to 10 business days.";
        emailService.sendAsync(build(customer.getEmail(),
                "Refund of " + money(refund.getAmount(), refund.getPayment().getCurrency()) + " for order #" + refund.getOrder().getId(),
                List.of(
                        "Hi " + customer.getFullName() + ",",
                        "Your refund of " + money(refund.getAmount(), refund.getPayment().getCurrency())
                                + " for order #" + refund.getOrder().getId() + " has been processed.",
                        how)));
    }

    public void refundRejected(Refund refund) {
        User customer = refund.getPayment().getCustomer();
        emailService.sendAsync(build(customer.getEmail(),
                "Update on your refund request for order #" + refund.getOrder().getId(),
                List.of(
                        "Hi " + customer.getFullName() + ",",
                        "We reviewed your refund request for order #" + refund.getOrder().getId() + " and could not approve it.",
                        "Reason: " + refund.getRejectionReason(),
                        "Reply to this email if you believe this is a mistake.")));
    }

    public void payoutPaid(Payout payout) {
        User owner = payout.getRestaurant().getOwner();
        if (owner == null) {
            return;
        }
        emailService.sendAsync(build(owner.getEmail(),
                "Payout of " + money(payout.getNetAmount(), payout.getCurrency()) + " for " + payout.getRestaurant().getName(),
                List.of(
                        "Hi " + owner.getFullName() + ",",
                        "We transferred " + money(payout.getNetAmount(), payout.getCurrency()) + " for orders between "
                                + payout.getPeriodStart() + " and " + payout.getPeriodEnd()
                                + " (" + payout.getPaymentCount() + " payments, " + payout.getCommissionPercentage()
                                + "% platform commission).",
                        "Bank reference: " + payout.getReferenceNumber(),
                        "Details are in your dashboard at " + frontendUrl + "/admin/restaurant/payouts")));
    }

    private static String money(java.math.BigDecimal amount, String currency) {
        return (currency == null ? "" : currency + " ") + (amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private EmailMessage build(String to, String subject, List<String> paragraphs) {
        String text = String.join("\n\n", paragraphs) + "\n\n— " + platformName + " team";
        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif;font-size:15px;line-height:1.5;color:#222\">");
        for (String p : paragraphs) {
            html.append("<p>").append(escape(p)).append("</p>");
        }
        html.append("<p>— ").append(escape(platformName)).append(" team</p></div>");
        return new EmailMessage(to, subject, text, html.toString());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
