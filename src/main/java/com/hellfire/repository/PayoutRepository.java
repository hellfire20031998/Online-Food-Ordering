package com.hellfire.repository;

import com.hellfire.model.Payout;
import com.hellfire.model.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long>, JpaSpecificationExecutor<Payout> {

    List<Payout> findByRestaurantIdOrderByPeriodEndDesc(Long restaurantId);

    long countByStatus(PayoutStatus status);

    @Query("select sum(p.netAmount) from Payout p where p.status = :status")
    BigDecimal sumNetByStatus(@Param("status") PayoutStatus status);

    @Query("select sum(p.netAmount) from Payout p where p.restaurant.id = :restaurantId and p.status = :status")
    BigDecimal sumNetByRestaurantAndStatus(@Param("restaurantId") Long restaurantId, @Param("status") PayoutStatus status);
}
