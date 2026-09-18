package com.hellfire.team.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private long totalRestaurants;
    private long activeRestaurants;
    private long suspendedRestaurants;
    private long openRestaurants;
    private long pendingApplications;
    private long openRefunds;
    private long pendingPayouts;
    private BigDecimal pendingPayoutsAmount;

    private long totalCustomers;
    private long blockedCustomers;
    private long newCustomersLast7Days;

    private long totalOrders;
    private long ordersToday;
    private BigDecimal revenueToday;
    private long ordersLast7Days;
    private BigDecimal revenueLast7Days;

    /** Order status name to count. */
    private Map<String, Long> ordersByStatus;

    private List<TopRestaurantDto> topRestaurantsLast30Days;
    private List<TeamCustomerDto> recentCustomers;

    private String currency;
    private BigDecimal commissionPercentage;
}
