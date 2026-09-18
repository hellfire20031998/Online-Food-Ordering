package com.hellfire.team;

import com.hellfire.model.*;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Optional-filter specifications for the paginated team-console lists. */
public final class TeamSpecifications {

    private TeamSpecifications() {
    }

    public static Specification<User> customers(String q, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), UserRole.CUSTOMER));

            if (status == UserStatus.ACTIVE) {
                // Legacy rows have a null status and count as active.
                predicates.add(cb.or(cb.isNull(root.get("status")), cb.equal(root.get("status"), status)));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Restaurant> restaurants(String q, RestaurantStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status == RestaurantStatus.ACTIVE) {
                predicates.add(cb.or(cb.isNull(root.get("status")), cb.equal(root.get("status"), status)));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                var owner = root.join("owner", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("cuisineType")), like),
                        cb.like(cb.lower(owner.get("email")), like)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Order> orders(OrderStatus status, Long restaurantId, Long customerId,
                                              Date from, Date toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }
            if (restaurantId != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
