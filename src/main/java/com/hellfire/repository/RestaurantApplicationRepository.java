package com.hellfire.repository;

import com.hellfire.model.ApplicationStatus;
import com.hellfire.model.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantApplicationRepository
        extends JpaRepository<RestaurantApplication, Long>, JpaSpecificationExecutor<RestaurantApplication> {

    boolean existsByApplicantIdAndStatus(Long applicantId, ApplicationStatus status);

    List<RestaurantApplication> findByApplicantIdOrderBySubmittedAtDesc(Long applicantId);

    long countByStatus(ApplicationStatus status);
}
