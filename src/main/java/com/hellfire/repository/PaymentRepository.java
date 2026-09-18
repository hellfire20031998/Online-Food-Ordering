package com.hellfire.repository;

import com.hellfire.model.Payment;
import com.hellfire.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    List<Payment> findByPayoutId(Long payoutId);

    /** Settled payments of one restaurant not yet included in any payout, paid inside [from, to). */
    List<Payment> findByRestaurantIdAndPayoutIsNullAndStatusInAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            Long restaurantId, Collection<PaymentStatus> statuses, LocalDateTime from, LocalDateTime to);

    /** Everything settled and not yet paid out for one restaurant (any period). */
    List<Payment> findByRestaurantIdAndPayoutIsNullAndStatusIn(Long restaurantId, Collection<PaymentStatus> statuses);

    @Query("select distinct p.restaurant.id from Payment p where p.payout is null and p.status in :statuses " +
            "and p.paidAt >= :from and p.paidAt < :to")
    List<Long> findRestaurantIdsWithUnsettledPayments(@Param("statuses") Collection<PaymentStatus> statuses,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);
}
