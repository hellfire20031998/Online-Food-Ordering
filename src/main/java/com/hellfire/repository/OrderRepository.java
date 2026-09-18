package com.hellfire.repository;

import com.hellfire.model.Order;
import com.hellfire.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);

    long countByRestaurantId(Long restaurantId);

    long countByCustomerId(Long customerId);

    long countByCreatedAtGreaterThanEqual(Date from);

    /** Sum of order totals since {@code from}, excluding orders in {@code excluded} status. Null when no rows. */
    @Query("select sum(o.totalAmount) from Order o where o.createdAt >= :from and o.orderStatus <> :excluded")
    BigDecimal sumRevenueSince(@Param("from") Date from, @Param("excluded") OrderStatus excluded);

    /** Rows of [OrderStatus, Long count]. */
    @Query("select o.orderStatus, count(o) from Order o group by o.orderStatus")
    List<Object[]> countGroupedByStatus();

    /** Rows of [restaurantId, restaurantName, Long orderCount, BigDecimal revenue], busiest first. */
    @Query("select r.id, r.name, count(o), sum(o.totalAmount) from Order o join o.restaurant r " +
            "where o.createdAt >= :from and o.orderStatus <> :excluded " +
            "group by r.id, r.name order by count(o) desc")
    List<Object[]> topRestaurantsSince(@Param("from") Date from,
                                       @Param("excluded") OrderStatus excluded,
                                       Pageable pageable);
}
