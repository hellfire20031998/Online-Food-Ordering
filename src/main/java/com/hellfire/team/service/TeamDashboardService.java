package com.hellfire.team.service;

import com.hellfire.model.*;
import com.hellfire.repository.*;
import com.hellfire.team.dto.DashboardSummaryDto;
import com.hellfire.team.dto.TopRestaurantDto;
import com.hellfire.team.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeamDashboardService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RestaurantApplicationRepository applicationRepository;
    private final RefundRepository refundRepository;
    private final PayoutRepository payoutRepository;
    private final PlatformSettingsService platformSettingsService;

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime sevenDaysAgo = startOfToday.minusDays(6);
        LocalDateTime thirtyDaysAgo = startOfToday.minusDays(29);

        Date today = toDate(startOfToday);
        Date week = toDate(sevenDaysAgo);
        Date month = toDate(thirtyDaysAgo);

        long totalRestaurants = restaurantRepository.count();
        long suspended = restaurantRepository.countByStatus(RestaurantStatus.SUSPENDED);

        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long blockedCustomers = userRepository.countByRoleAndStatus(UserRole.CUSTOMER, UserStatus.BLOCKED);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            if (row[0] != null) {
                byStatus.put(((OrderStatus) row[0]).name(), (Long) row[1]);
            }
        }

        List<TopRestaurantDto> top = orderRepository
                .topRestaurantsSince(month, OrderStatus.CANCELLED, PageRequest.of(0, 5))
                .stream()
                .map(row -> new TopRestaurantDto(
                        (Long) row[0],
                        (String) row[1],
                        (Long) row[2],
                        row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3]))
                .toList();

        PlatformSettings settings = platformSettingsService.get();

        return DashboardSummaryDto.builder()
                .totalRestaurants(totalRestaurants)
                .activeRestaurants(totalRestaurants - suspended)
                .suspendedRestaurants(suspended)
                .openRestaurants(restaurantRepository.countByOpenTrue())
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                .openRefunds(refundRepository.countByStatus(RefundStatus.REQUESTED)
                        + refundRepository.countByStatus(RefundStatus.APPROVED))
                .pendingPayouts(payoutRepository.countByStatus(PayoutStatus.PENDING))
                .pendingPayoutsAmount(zeroIfNull(payoutRepository.sumNetByStatus(PayoutStatus.PENDING)))
                .totalCustomers(totalCustomers)
                .blockedCustomers(blockedCustomers)
                .newCustomersLast7Days(userRepository.countByRoleAndCreatedAtAfter(UserRole.CUSTOMER, sevenDaysAgo))
                .totalOrders(orderRepository.count())
                .ordersToday(orderRepository.countByCreatedAtGreaterThanEqual(today))
                .revenueToday(zeroIfNull(orderRepository.sumRevenueSince(today, OrderStatus.CANCELLED)))
                .ordersLast7Days(orderRepository.countByCreatedAtGreaterThanEqual(week))
                .revenueLast7Days(zeroIfNull(orderRepository.sumRevenueSince(week, OrderStatus.CANCELLED)))
                .ordersByStatus(byStatus)
                .topRestaurantsLast30Days(top)
                .recentCustomers(userRepository.findTop5ByRoleOrderByCreatedAtDesc(UserRole.CUSTOMER).stream()
                        .map(u -> TeamMapper.toCustomerDto(u, orderRepository.countByCustomerId(u.getId())))
                        .toList())
                .currency(settings.getCurrency())
                .commissionPercentage(settings.getCommissionPercentage())
                .build();
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
