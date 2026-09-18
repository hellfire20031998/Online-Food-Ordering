package com.hellfire.payment.service;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.*;
import com.hellfire.notification.NotificationService;
import com.hellfire.onboarding.BankDetails;
import com.hellfire.payment.PaymentMapper;
import com.hellfire.payment.dto.*;
import com.hellfire.payment.gateway.GatewayRefundResult;
import com.hellfire.payment.gateway.PaymentGateway;
import com.hellfire.repository.OrderRepository;
import com.hellfire.repository.PaymentRepository;
import com.hellfire.repository.RefundRepository;
import com.hellfire.team.dto.PageResponse;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Refund ledger. Customers raise requests; TEAM_ADMIN / TEAM_MANAGER decide. Gateway payments are
 * reversed through the gateway, cash-on-delivery money is returned by manual bank transfer.
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway gateway;
    private final PaymentService paymentService;
    private final CustomerBankAccountService customerBankAccountService;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------ customer

    @Transactional
    public RefundDto request(Long orderId, User customer, RefundRequest req) {
        Payment payment = requirePaymentForOrder(orderId);
        if (payment.getCustomer() == null || !Objects.equals(payment.getCustomer().getId(), customer.getId())) {
            throw new NotAuthorizedException("This order is not yours");
        }
        CustomerBankDetails bank = req.getBankAccount();
        boolean cash = payment.getProvider() == null || !payment.getProvider().isGateway();
        if (cash && bank == null) {
            // Fall back to the customer's saved refund account.
            bank = customerBankAccountService.savedDetails(customer).orElse(null);
        }
        Refund refund = newRefund(payment, req.getAmount(), req.getReason(), bank, customer.getEmail(), true);
        refundRepository.save(refund);
        if (cash && req.isSaveBankAccount() && req.getBankAccount() != null) {
            customerBankAccountService.saveFromRefund(customer, req.getBankAccount());
        }
        log.info("AUDIT refund requested: refundId={} orderId={} amount={} by customer {}",
                refund.getId(), orderId, refund.getAmount(), customer.getEmail());
        notificationService.refundRequested(refund);
        return PaymentMapper.toDto(refund, true);
    }

    @Transactional(readOnly = true)
    public List<RefundDto> forOrder(Long orderId, User customer) {
        Payment payment = requirePaymentForOrder(orderId);
        if (payment.getCustomer() == null || !Objects.equals(payment.getCustomer().getId(), customer.getId())) {
            throw new NotAuthorizedException("This order is not yours");
        }
        return refundRepository.findByOrderIdOrderByRequestedAtDesc(orderId).stream()
                .map(r -> PaymentMapper.toDto(r, true))
                .toList();
    }

    // ------------------------------------------------------------------ team

    @Transactional(readOnly = true)
    public PageResponse<RefundDto> list(RefundStatus status, String q, int page, int size, boolean revealBank) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "requestedAt"));
        return PageResponse.of(refundRepository.findAll(spec(status, q), pageable), r -> PaymentMapper.toDto(r, revealBank));
    }

    @Transactional(readOnly = true)
    public RefundDto get(Long id, boolean revealBank) {
        return PaymentMapper.toDto(require(id), revealBank);
    }

    /** Team-initiated refund: created and approved in one step. */
    @Transactional
    public RefundDto create(TeamRefundRequest req, String actor, boolean revealBank) {
        Payment payment = requirePaymentForOrder(req.getOrderId());
        Refund refund = newRefund(payment, req.getAmount(), req.getReason(), req.getBankAccount(), actor, false);
        refund.setNotes(req.getNotes());
        refundRepository.save(refund);
        log.info("AUDIT refund created by team: refundId={} orderId={} amount={} by {}",
                refund.getId(), req.getOrderId(), refund.getAmount(), actor);
        process(refund, actor);
        return PaymentMapper.toDto(refund, revealBank);
    }

    @Transactional
    public RefundDto approve(Long id, RefundDecisionRequest req, String actor, boolean revealBank) {
        Refund refund = require(id);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new IllegalArgumentException("Only requested refunds can be approved");
        }
        if (req != null && req.getBankAccount() != null) {
            applyBankDetails(refund, req.getBankAccount());
        }
        if (req != null && req.getNotes() != null) {
            refund.setNotes(req.getNotes());
        }
        process(refund, actor);
        return PaymentMapper.toDto(refund, revealBank);
    }

    /** Manual refunds: the team has made the bank transfer and records its reference. */
    @Transactional
    public RefundDto complete(Long id, RefundDecisionRequest req, String actor, boolean revealBank) {
        Refund refund = require(id);
        if (refund.getStatus() != RefundStatus.APPROVED || refund.getMethod() != RefundMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Only approved bank-transfer refunds can be completed");
        }
        if (req == null || req.getReferenceNumber() == null || req.getReferenceNumber().isBlank()) {
            throw new IllegalArgumentException("The bank transfer reference is required");
        }
        refund.setReferenceNumber(req.getReferenceNumber().trim());
        if (req.getNotes() != null) {
            refund.setNotes(req.getNotes());
        }
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProcessedBy(actor);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);
        paymentService.applyRefund(refund);
        log.info("AUDIT refund completed (bank transfer): refundId={} ref={} by {}", id, refund.getReferenceNumber(), actor);
        notificationService.refundCompleted(refund);
        return PaymentMapper.toDto(refund, revealBank);
    }

    @Transactional
    public RefundDto reject(Long id, RefundDecisionRequest req, String actor, boolean revealBank) {
        Refund refund = require(id);
        if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.APPROVED) {
            throw new IllegalArgumentException("Only open refunds can be rejected");
        }
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(req.getReason().trim());
        refund.setProcessedBy(actor);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);
        log.info("AUDIT refund rejected: refundId={} by {} reason='{}'", id, actor, refund.getRejectionReason());
        notificationService.refundRejected(refund);
        return PaymentMapper.toDto(refund, revealBank);
    }

    // ------------------------------------------------------------------ internals

    /** Executes an approved refund: gateway call, or hand-off to a manual transfer. */
    private void process(Refund refund, String actor) {
        Payment payment = refund.getPayment();
        if (refund.getAmount().compareTo(payment.getRefundableAmount()) > 0) {
            throw new IllegalArgumentException("Refund exceeds the refundable amount of " + payment.getRefundableAmount());
        }
        refund.setProcessedBy(actor);
        refund.setProcessedAt(LocalDateTime.now());

        if (refund.getMethod() == RefundMethod.GATEWAY) {
            GatewayRefundResult result = gateway.refund(payment.getProviderPaymentId(), refund.getAmount(),
                    refund.getReason(), "refund-" + refund.getId());
            refund.setProviderRefundId(result.providerRefundId());
            if (result.completed()) {
                refund.setStatus(RefundStatus.COMPLETED);
                refundRepository.save(refund);
                paymentService.applyRefund(refund);
                notificationService.refundCompleted(refund);
            } else {
                refund.setStatus(RefundStatus.PROCESSING);
                refundRepository.save(refund);
            }
            log.info("AUDIT refund sent to gateway: refundId={} providerRefundId={} status={} by {}",
                    refund.getId(), refund.getProviderRefundId(), refund.getStatus(), actor);
        } else {
            if (!refund.hasBankDetails()) {
                throw new IllegalArgumentException("Bank details are required for a bank-transfer refund");
            }
            refund.setStatus(RefundStatus.APPROVED);
            refundRepository.save(refund);
            log.info("AUDIT refund approved for bank transfer: refundId={} by {}", refund.getId(), actor);
        }
    }

    private Refund newRefund(Payment payment, BigDecimal requestedAmount, String reason,
                             CustomerBankDetails bank, String requestedBy, boolean byCustomer) {
        if (!payment.getStatus().isSettled()) {
            throw new IllegalArgumentException("This order has not been paid, so there is nothing to refund");
        }
        BigDecimal refundable = payment.getRefundableAmount();
        if (refundable.signum() <= 0) {
            throw new IllegalArgumentException("This order has already been fully refunded");
        }
        if (payment.hasOpenRefund()) {
            throw new IllegalArgumentException("A refund for this order is already in progress");
        }
        BigDecimal amount = requestedAmount == null ? refundable : requestedAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.signum() <= 0 || amount.compareTo(refundable) > 0) {
            throw new IllegalArgumentException("Refund amount must be between 0.01 and " + refundable);
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setOrder(payment.getOrder());
        refund.setAmount(amount);
        refund.setReason(reason == null ? null : reason.trim());
        refund.setStatus(RefundStatus.REQUESTED);
        refund.setMethod(payment.getProvider() != null && payment.getProvider().isGateway()
                ? RefundMethod.GATEWAY : RefundMethod.BANK_TRANSFER);
        refund.setRequestedBy(requestedBy);
        refund.setRequestedByCustomer(byCustomer);
        refund.setRequestedAt(LocalDateTime.now());
        if (bank != null) {
            applyBankDetails(refund, bank);
        }
        if (refund.getMethod() == RefundMethod.BANK_TRANSFER && byCustomer && !refund.hasBankDetails()) {
            throw new IllegalArgumentException(
                    "Please provide the bank account or UPI id to refund to, or save one in your profile");
        }
        return refund;
    }

    private static void applyBankDetails(Refund refund, CustomerBankDetails bank) {
        String name = bank.getBeneficiaryName() == null ? null : bank.getBeneficiaryName().trim();
        String number = bank.getAccountNumber() == null || bank.getAccountNumber().isBlank() ? null : bank.getAccountNumber().trim();
        String ifsc = BankDetails.normalizeIfsc(bank.getIfsc() == null || bank.getIfsc().isBlank() ? null : bank.getIfsc());
        String upi = BankDetails.normalizeUpi(bank.getUpiId());
        if (number != null && !number.matches("\\d{9,18}")) {
            throw new IllegalArgumentException("accountNumber must be 9 to 18 digits");
        }
        if (ifsc != null && !ifsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw new IllegalArgumentException("ifsc must be a valid IFSC code");
        }
        if (number != null && ifsc == null) {
            throw new IllegalArgumentException("ifsc is required with an account number");
        }
        refund.setBeneficiaryName(name);
        refund.setAccountNumber(number);
        refund.setIfsc(ifsc);
        refund.setUpiId(upi);
    }

    private Payment requirePaymentForOrder(Long orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found"));
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order " + orderId));
    }

    private Refund require(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund with ID " + id + " not found"));
    }

    private static Specification<Refund> spec(RefundStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                var payment = root.join("payment", JoinType.LEFT);
                var customer = payment.join("customer", JoinType.LEFT);
                var restaurant = payment.join("restaurant", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(customer.get("email")), like),
                        cb.like(cb.lower(customer.get("fullName")), like),
                        cb.like(cb.lower(restaurant.get("name")), like)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
