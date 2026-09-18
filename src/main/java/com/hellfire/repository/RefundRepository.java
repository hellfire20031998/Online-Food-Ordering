package com.hellfire.repository;

import com.hellfire.model.Refund;
import com.hellfire.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {

    List<Refund> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    Optional<Refund> findByProviderRefundId(String providerRefundId);

    long countByStatus(RefundStatus status);
}
