package com.hellfire.payment.service;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.*;
import com.hellfire.notification.NotificationService;
import com.hellfire.payment.PaymentMapper;
import com.hellfire.payment.dto.EarningsSummaryDto;
import com.hellfire.payment.dto.PayoutDto;
import com.hellfire.repository.*;
import com.hellfire.team.dto.PageResponse;
import com.hellfire.team.service.PlatformSettingsService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Restaurant settlements. Payouts are generated per restaurant for a period from settled payments
 * not yet paid out, then marked paid once the team has made the bank transfer.
 */
@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);
    private static final Set<PaymentStatus> SETTLED =
            EnumSet.of(PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    private final PayoutRepository payoutRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBankAccountRepository bankAccountRepository;
    private final PlatformSettingsService platformSettingsService;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------ team

    @Transactional
    public List<PayoutDto> generate(LocalDate from, LocalDate to, String actor, boolean revealBank) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("The period end must not be before its start");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        PlatformSettings settings = platformSettingsService.get();

        List<PayoutDto> created = new ArrayList<>();
        for (Long restaurantId : paymentRepository.findRestaurantIdsWithUnsettledPayments(SETTLED, start, endExclusive)) {
            List<Payment> payments = paymentRepository
                    .findByRestaurantIdAndPayoutIsNullAndStatusInAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                            restaurantId, SETTLED, start, endExclusive);
            if (payments.isEmpty()) {
                continue;
            }
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant " + restaurantId + " not found"));

            Payout payout = new Payout();
            payout.setRestaurant(restaurant);
            payout.setPeriodStart(from);
            payout.setPeriodEnd(to);
            payout.setCurrency(settings.getCurrency());
            payout.setCommissionPercentage(settings.getCommissionPercentage());
            payout.setStatus(PayoutStatus.PENDING);
            payout.setCreatedAt(LocalDateTime.now());
            payout.setCreatedBy(actor);
            fillTotals(payout, payments);

            bankAccountRepository.findByRestaurantId(restaurantId).ifPresentOrElse(bank -> {
                payout.setAccountHolderName(bank.getAccountHolderName());
                payout.setBankName(bank.getBankName());
                payout.setIfsc(bank.getIfsc());
                payout.setAccountNumber(bank.getAccountNumber());
            }, () -> payout.setNotes("No payout account on file for this restaurant at generation time."));

            Payout saved = payoutRepository.save(payout);
            for (Payment p : payments) {
                p.setPayout(saved);
            }
            paymentRepository.saveAll(payments);
            log.info("AUDIT payout generated: payoutId={} restaurantId={} period={}..{} payments={} net={} by {}",
                    saved.getId(), restaurantId, from, to, payments.size(), saved.getNetAmount(), actor);
            created.add(PaymentMapper.toDto(saved, revealBank, null));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public PageResponse<PayoutDto> list(PayoutStatus status, Long restaurantId, int page, int size, boolean revealBank) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(payoutRepository.findAll(spec(status, restaurantId), pageable),
                p -> PaymentMapper.toDto(p, revealBank, null));
    }

    @Transactional(readOnly = true)
    public PayoutDto get(Long id, boolean revealBank) {
        Payout payout = require(id);
        return PaymentMapper.toDto(payout, revealBank, paymentRepository.findByPayoutId(id));
    }

    @Transactional
    public PayoutDto markPaid(Long id, String referenceNumber, String notes, String actor, boolean revealBank) {
        Payout payout = require(id);
        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payouts can be marked as paid");
        }
        if (payout.getNetAmount() == null || payout.getNetAmount().signum() <= 0) {
            throw new IllegalArgumentException("This payout has nothing to transfer; cancel it instead");
        }
        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(LocalDateTime.now());
        payout.setPaidBy(actor);
        payout.setReferenceNumber(referenceNumber.trim());
        if (notes != null && !notes.isBlank()) {
            payout.setNotes(notes.trim());
        }
        payoutRepository.save(payout);
        log.info("AUDIT payout paid: payoutId={} restaurantId={} net={} ref={} by {}",
                id, payout.getRestaurant().getId(), payout.getNetAmount(), payout.getReferenceNumber(), actor);
        notificationService.payoutPaid(payout);
        return PaymentMapper.toDto(payout, revealBank, paymentRepository.findByPayoutId(id));
    }

    /** Cancelling releases the payments so a later generation picks them up again. */
    @Transactional
    public PayoutDto cancel(Long id, String actor, boolean revealBank) {
        Payout payout = require(id);
        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payouts can be cancelled");
        }
        List<Payment> payments = paymentRepository.findByPayoutId(id);
        for (Payment p : payments) {
            p.setPayout(null);
        }
        paymentRepository.saveAll(payments);
        payout.setStatus(PayoutStatus.CANCELLED);
        payoutRepository.save(payout);
        log.info("AUDIT payout cancelled: payoutId={} releasedPayments={} by {}", id, payments.size(), actor);
        return PaymentMapper.toDto(payout, revealBank, List.of());
    }

    // ------------------------------------------------------------------ owner

    @Transactional(readOnly = true)
    public List<PayoutDto> listForOwner(Long restaurantId, User owner) {
        requireOwner(restaurantId, owner);
        return payoutRepository.findByRestaurantIdOrderByPeriodEndDesc(restaurantId).stream()
                .map(p -> PaymentMapper.toDto(p, true, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PayoutDto getForOwner(Long restaurantId, Long payoutId, User owner) {
        requireOwner(restaurantId, owner);
        Payout payout = require(payoutId);
        if (!Objects.equals(payout.getRestaurant().getId(), restaurantId)) {
            throw new ResourceNotFoundException("Payout with ID " + payoutId + " not found");
        }
        return PaymentMapper.toDto(payout, true, paymentRepository.findByPayoutId(payoutId));
    }

    @Transactional(readOnly = true)
    public EarningsSummaryDto earnings(Long restaurantId, User owner) {
        requireOwner(restaurantId, owner);
        List<Payment> unsettled = paymentRepository.findByRestaurantIdAndPayoutIsNullAndStatusIn(restaurantId, SETTLED);
        BigDecimal gross = BigDecimal.ZERO, commission = BigDecimal.ZERO, net = BigDecimal.ZERO;
        for (Payment p : unsettled) {
            gross = gross.add(p.getSettledAmount());
            commission = commission.add(p.getCommissionOnSettled());
            net = net.add(p.getNetToRestaurant());
        }
        PlatformSettings settings = platformSettingsService.get();
        return new EarningsSummaryDto(
                settings.getCurrency(),
                unsettled.size(),
                gross,
                commission,
                net,
                zeroIfNull(payoutRepository.sumNetByRestaurantAndStatus(restaurantId, PayoutStatus.PENDING)),
                zeroIfNull(payoutRepository.sumNetByRestaurantAndStatus(restaurantId, PayoutStatus.PAID)),
                settings.getCommissionPercentage());
    }

    // ------------------------------------------------------------------ helpers

    private static void fillTotals(Payout payout, List<Payment> payments) {
        BigDecimal gross = BigDecimal.ZERO, refunded = BigDecimal.ZERO, commission = BigDecimal.ZERO, net = BigDecimal.ZERO;
        for (Payment p : payments) {
            gross = gross.add(p.getAmount());
            refunded = refunded.add(p.getRefundedAmount() == null ? BigDecimal.ZERO : p.getRefundedAmount());
            commission = commission.add(p.getCommissionOnSettled());
            net = net.add(p.getNetToRestaurant());
        }
        payout.setPaymentCount(payments.size());
        payout.setGrossAmount(gross);
        payout.setRefundedAmount(refunded);
        payout.setCommissionAmount(commission);
        payout.setNetAmount(net);
    }

    private Payout require(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout with ID " + id + " not found"));
    }

    private void requireOwner(Long restaurantId, User user) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found"));
        boolean isOwner = restaurant.getOwner() != null && Objects.equals(restaurant.getOwner().getId(), user.getId());
        if (!isOwner) {
            throw new NotAuthorizedException("Only the restaurant owner can view payouts");
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Specification<Payout> spec(PayoutStatus status, Long restaurantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (restaurantId != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
