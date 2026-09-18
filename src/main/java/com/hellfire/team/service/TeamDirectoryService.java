package com.hellfire.team.service;

import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.*;
import com.hellfire.repository.OrderRepository;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.team.TeamSpecifications;
import com.hellfire.team.dto.PageResponse;
import com.hellfire.team.dto.TeamCustomerDto;
import com.hellfire.team.dto.TeamOrderDto;
import com.hellfire.team.dto.TeamRestaurantDto;
import com.hellfire.team.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Cross-tenant views and status controls over restaurants, customers and orders for the platform team.
 * Every status change is logged with the acting team member for auditability.
 */
@Service
@RequiredArgsConstructor
public class TeamDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(TeamDirectoryService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    // ---------------------------------------------------------------- restaurants

    @Transactional(readOnly = true)
    public PageResponse<TeamRestaurantDto> listRestaurants(String q, RestaurantStatus status, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));
        return PageResponse.of(
                restaurantRepository.findAll(TeamSpecifications.restaurants(q, status), pageable),
                r -> TeamMapper.toRestaurantDto(r, orderRepository.countByRestaurantId(r.getId())));
    }

    @Transactional(readOnly = true)
    public TeamRestaurantDto getRestaurant(Long id) {
        Restaurant r = requireRestaurant(id);
        return TeamMapper.toRestaurantDto(r, orderRepository.countByRestaurantId(id));
    }

    @Transactional
    public TeamRestaurantDto suspendRestaurant(Long id, String actor) {
        return changeRestaurantStatus(id, RestaurantStatus.SUSPENDED, actor);
    }

    @Transactional
    public TeamRestaurantDto reactivateRestaurant(Long id, String actor) {
        return changeRestaurantStatus(id, RestaurantStatus.ACTIVE, actor);
    }

    private TeamRestaurantDto changeRestaurantStatus(Long id, RestaurantStatus status, String actor) {
        Restaurant r = requireRestaurant(id);
        RestaurantStatus before = TeamMapper.effectiveStatus(r);
        r.setStatus(status);
        restaurantRepository.save(r);
        log.info("AUDIT restaurant status: id={} name='{}' {} -> {} by {}", id, r.getName(), before, status, actor);
        return TeamMapper.toRestaurantDto(r, orderRepository.countByRestaurantId(id));
    }

    private Restaurant requireRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant with ID " + id + " not found"));
    }

    // ---------------------------------------------------------------- customers

    @Transactional(readOnly = true)
    public PageResponse<TeamCustomerDto> listCustomers(String q, UserStatus status, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(
                userRepository.findAll(TeamSpecifications.customers(q, status), pageable),
                u -> TeamMapper.toCustomerDto(u, orderRepository.countByCustomerId(u.getId())));
    }

    @Transactional(readOnly = true)
    public TeamCustomerDto getCustomer(Long id) {
        User u = requireCustomer(id);
        return TeamMapper.toCustomerDto(u, orderRepository.countByCustomerId(id));
    }

    @Transactional
    public TeamCustomerDto blockCustomer(Long id, String actor) {
        return changeCustomerStatus(id, UserStatus.BLOCKED, actor);
    }

    @Transactional
    public TeamCustomerDto unblockCustomer(Long id, String actor) {
        return changeCustomerStatus(id, UserStatus.ACTIVE, actor);
    }

    private TeamCustomerDto changeCustomerStatus(Long id, UserStatus status, String actor) {
        User u = requireCustomer(id);
        UserStatus before = TeamMapper.effectiveStatus(u);
        u.setStatus(status);
        userRepository.save(u);
        log.info("AUDIT customer status: id={} email={} {} -> {} by {}", id, u.getEmail(), before, status, actor);
        return TeamMapper.toCustomerDto(u, orderRepository.countByCustomerId(id));
    }

    private User requireCustomer(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + id + " not found"));
        if (u.getRole() != UserRole.CUSTOMER) {
            throw new ResourceNotFoundException("Customer with ID " + id + " not found");
        }
        return u;
    }

    // ---------------------------------------------------------------- orders

    @Transactional(readOnly = true)
    public PageResponse<TeamOrderDto> listOrders(OrderStatus status, Long restaurantId, Long customerId,
                                                 Date from, Date toExclusive, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(
                orderRepository.findAll(
                        TeamSpecifications.orders(status, restaurantId, customerId, from, toExclusive), pageable),
                TeamMapper::toOrderDto);
    }

    @Transactional(readOnly = true)
    public TeamOrderDto getOrder(Long id) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));
        return TeamMapper.toOrderDto(o);
    }

    // ---------------------------------------------------------------- helpers

    static Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
