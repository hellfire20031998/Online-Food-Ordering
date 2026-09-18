package com.hellfire.repository;

import com.hellfire.model.Restaurant;
import com.hellfire.model.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {

    Restaurant findByOwnerId(long id);

    /** Customer-facing listing: everything that is not suspended (legacy null status counts as active). */
    @Query("SELECT r FROM Restaurant r WHERE r.status IS NULL OR r.status <> :suspended")
    List<Restaurant> findAllPublic(@Param("suspended") RestaurantStatus suspended);

    @Query("SELECT r FROM Restaurant r WHERE (r.status IS NULL OR r.status <> :suspended) AND (" +
            "lower(r.name) LIKE lower(concat('%', :query, '%')) " +
            "OR lower(r.cuisineType) LIKE lower(concat('%', :query, '%')))")
    List<Restaurant> searchPublic(@Param("query") String query, @Param("suspended") RestaurantStatus suspended);

    long countByStatus(RestaurantStatus status);

    long countByOpenTrue();

    @Modifying
    @Query("update Restaurant r set r.status = :status where r.status is null")
    int backfillMissingStatus(@Param("status") RestaurantStatus status);
}
