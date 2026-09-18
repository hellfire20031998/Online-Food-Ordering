package com.hellfire.payment.service;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.*;
import com.hellfire.payment.PaymentMapper;
import com.hellfire.payment.dto.PaymentConfigDto;
import com.hellfire.payment.dto.PaymentDto;
import com.hellfire.payment.gateway.GatewayIntent;
import com.hellfire.payment.gateway.GatewayIntentStatus;
import com.hellfire.payment.gateway.GatewayWebhookEvent;
import com.hellfire.payment.gateway.PaymentGateway;
import com.hellfire.repository.CartRepository;
import com.hellfire.repository.OrderRepository;
import com.hellfire.repository.PaymentRepository;
import com.hellfire.repository.RefundRepository;
import com.hellfire.team.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lifecycle of the {@link Payment} attached to every order. Provider-independent: all gateway
 * traffic goes through {@link PaymentGateway}.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final RefundRepository refundRepository;
    private final PaymentGateway gateway;
    private final PlatformSettingsService platformSettingsService;

    public PaymentConfigDto config() {
        PlatformSettings settings = platformSettingsService.get();
        return new PaymentConfigDto(gateway.enabled(), gateway.provider(), gateway.publishableKey(), settings.getCurrency());
    }

    // ------------------------------------------------------------------ order creation

    /** Creates the payment row for a freshly saved order, starting a gateway attempt for online methods. */
    @Transactional
    public Payment createForOrder(Order order, PaymentMethods method) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setRestaurant(order.getRestaurant());
        payment.setCustomer(order.getCustomer());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(platformSettingsService.get().getCurrency());
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        if (method == PaymentMethods.CASH_ON_DELIVERY) {
            payment.setProvider(PaymentProvider.CASH_ON_DELIVERY);
            return paymentRepository.save(payment);
        }

        if (!gateway.enabled()) {
            throw new IllegalArgumentException("Online payments are not available right now. Please choose cash on delivery.");
        }
        payment.setProvider(gateway.provider());
        payment = paymentRepository.save(payment); // id needed for gateway metadata
        GatewayIntent intent = gateway.createIntent(payment);
        payment.setProviderPaymentId(intent.providerPaymentId());
        payment.setProviderClientSecret(intent.clientSecret());
        return paymentRepository.save(payment);
    }

    // ------------------------------------------------------------------ customer-facing reads / confirm

    @Transactional(readOnly = true)
    public PaymentDto getForOrder(Long orderId, User user) {
        Payment payment = requireByOrder(orderId);
        requireCustomer(payment, user);
        return PaymentMapper.toDto(payment, true);
    }

    /** Client-side confirmation after checkout: asks the gateway for the truth and applies it. */
    @Transactional
    public PaymentDto confirm(Long paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));
        requireCustomer(payment, user);
        if (payment.getProvider() == null || !payment.getProvider().isGateway()) {
            throw new IllegalArgumentException("This order is paid on delivery");
        }
        if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.FAILED) {
            GatewayIntentStatus status = gateway.fetchIntent(payment.getProviderPaymentId());
            applyGatewayState(payment, status.state(), status.failureReason());
        }
        return PaymentMapper.toDto(payment, true);
    }

    // ------------------------------------------------------------------ webhook

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        GatewayWebhookEvent event = gateway.parseWebhook(payload, signatureHeader);
        switch (event.type()) {
            case PAYMENT_SUCCEEDED -> byProviderId(event.providerPaymentId())
                    .ifPresent(p -> applyGatewayState(p, GatewayIntentStatus.State.PAID, null));
            case PAYMENT_FAILED -> byProviderId(event.providerPaymentId())
                    .ifPresent(p -> applyGatewayState(p, GatewayIntentStatus.State.FAILED, event.failureReason()));
            case PAYMENT_CANCELLED -> byProviderId(event.providerPaymentId())
                    .ifPresent(p -> applyGatewayState(p, GatewayIntentStatus.State.CANCELLED, null));
            case REFUND_COMPLETED -> refundRepository.findByProviderRefundId(event.providerRefundId())
                    .ifPresent(r -> completeGatewayRefund(r, true, null));
            case REFUND_FAILED -> refundRepository.findByProviderRefundId(event.providerRefundId())
                    .ifPresent(r -> completeGatewayRefund(r, false, event.failureReason()));
            case IGNORED -> log.debug("Ignored gateway webhook");
        }
    }

    // ------------------------------------------------------------------ order lifecycle hooks

    /** Cash on delivery: the restaurant collected the money when the order was delivered. */
    @Transactional
    public void onOrderFulfilled(Order order) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
            if (p.getProvider() == PaymentProvider.CASH_ON_DELIVERY && p.getStatus() == PaymentStatus.PENDING) {
                markPaid(p);
            }
        });
    }

    /**
     * On cancellation: an unpaid online attempt is cancelled at the gateway; a paid order gets an
     * automatic full refund request for the team to process.
     */
    @Transactional
    public void onOrderCancelled(Order order, String actorEmail, boolean byCustomer) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.FAILED) {
                if (p.getProvider() != null && p.getProvider().isGateway() && p.getProviderPaymentId() != null) {
                    gateway.cancelIntent(p.getProviderPaymentId());
                }
                p.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(p);
                return;
            }
            if (p.getStatus().isSettled() && p.getRefundableAmount().signum() > 0 && !p.hasOpenRefund()) {
                Refund refund = new Refund();
                refund.setPayment(p);
                refund.setOrder(order);
                refund.setAmount(p.getRefundableAmount());
                refund.setReason("Order cancelled");
                refund.setStatus(RefundStatus.REQUESTED);
                refund.setMethod(p.getProvider() != null && p.getProvider().isGateway()
                        ? RefundMethod.GATEWAY : RefundMethod.BANK_TRANSFER);
                refund.setRequestedBy(actorEmail);
                refund.setRequestedByCustomer(byCustomer);
                refund.setRequestedAt(LocalDateTime.now());
                refundRepository.save(refund);
                log.info("AUDIT refund auto-requested on cancellation: orderId={} amount={} by {}",
                        order.getId(), refund.getAmount(), actorEmail);
            }
        });
    }

    // ------------------------------------------------------------------ state transitions (shared with RefundService)

    @Transactional
    public void markPaid(Payment payment) {
        if (payment.getStatus().isSettled()) {
            return;
        }
        PlatformSettings settings = platformSettingsService.get();
        BigDecimal pct = settings.getCommissionPercentage() == null ? BigDecimal.ZERO : settings.getCommissionPercentage();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        payment.setCommissionPercentage(pct);
        payment.setCommissionAmount(payment.getAmount().multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order.getOrderStatus() == null || order.getOrderStatus().isPaymentState()) {
            order.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order);
        }
        if (payment.getProvider() != null && payment.getProvider().isGateway()) {
            clearCart(payment.getCustomer());
        }
        log.info("AUDIT payment paid: paymentId={} orderId={} amount={} provider={}",
                payment.getId(), order.getId(), payment.getAmount(), payment.getProvider());
    }

    void applyGatewayState(Payment payment, GatewayIntentStatus.State state, String failureReason) {
        switch (state) {
            case PAID -> markPaid(payment);
            case FAILED -> {
                if (!payment.getStatus().isSettled()) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(failureReason);
                    paymentRepository.save(payment);
                    Order order = payment.getOrder();
                    order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                    orderRepository.save(order);
                }
            }
            case CANCELLED -> {
                if (!payment.getStatus().isSettled()) {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                    Order order = payment.getOrder();
                    if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                        order.setOrderStatus(OrderStatus.CANCELLED);
                        orderRepository.save(order);
                    }
                }
            }
            case PENDING -> { /* nothing to do yet */ }
        }
    }

    /** Applies a finished refund to the payment's running totals. */
    @Transactional
    public void applyRefund(Refund refund) {
        Payment payment = refund.getPayment();
        BigDecimal refunded = (payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount())
                .add(refund.getAmount());
        payment.setRefundedAmount(refunded.min(payment.getAmount()));
        payment.setStatus(payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);
    }

    private void completeGatewayRefund(Refund refund, boolean succeeded, String failureReason) {
        if (refund.getStatus() != RefundStatus.PROCESSING) {
            return;
        }
        refund.setProcessedAt(LocalDateTime.now());
        if (succeeded) {
            refund.setStatus(RefundStatus.COMPLETED);
            applyRefund(refund);
        } else {
            refund.setStatus(RefundStatus.FAILED);
            refund.setNotes((refund.getNotes() == null ? "" : refund.getNotes() + " ") + "Gateway: " + failureReason);
        }
        refundRepository.save(refund);
        log.info("AUDIT gateway refund {}: refundId={} providerRefundId={}",
                succeeded ? "completed" : "failed", refund.getId(), refund.getProviderRefundId());
    }

    // ------------------------------------------------------------------ helpers

    private java.util.Optional<Payment> byProviderId(String providerPaymentId) {
        if (providerPaymentId == null) {
            return java.util.Optional.empty();
        }
        return paymentRepository.findByProviderPaymentId(providerPaymentId);
    }

    private Payment requireByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order " + orderId));
    }

    private static void requireCustomer(Payment payment, User user) {
        if (payment.getCustomer() == null || !Objects.equals(payment.getCustomer().getId(), user.getId())) {
            throw new NotAuthorizedException("This payment does not belong to you");
        }
    }

    private void clearCart(User customer) {
        Cart cart = cartRepository.findByCustomerId(customer.getId());
        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            cart.getItems().clear();
            cart.setTotal(BigDecimal.ZERO);
            cartRepository.save(cart);
        }
    }
}
