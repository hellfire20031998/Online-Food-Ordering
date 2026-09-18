package com.hellfire.repository;

import com.hellfire.model.SensitiveDataAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensitiveDataAccessLogRepository extends JpaRepository<SensitiveDataAccessLog, Long> {

    Page<SensitiveDataAccessLog> findAllByOrderByAccessedAtDesc(Pageable pageable);

    long countBySubjectTypeAndSubjectId(String subjectType, Long subjectId);
}
