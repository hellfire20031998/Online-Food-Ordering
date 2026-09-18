package com.hellfire.security;

import com.hellfire.model.SensitiveDataAccessLog;
import com.hellfire.repository.SensitiveDataAccessLogRepository;
import com.hellfire.team.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Records who viewed unmasked bank details, and for which record. */
@Service
@RequiredArgsConstructor
public class SensitiveDataAuditService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataAuditService.class);

    public static final String CUSTOMER_BANK_ACCOUNT = "CUSTOMER_BANK_ACCOUNT";
    public static final String RESTAURANT_BANK_ACCOUNT = "RESTAURANT_BANK_ACCOUNT";
    public static final String APPLICATION_BANK_ACCOUNT = "APPLICATION_BANK_ACCOUNT";
    public static final String REFUND_BANK_ACCOUNT = "REFUND_BANK_ACCOUNT";
    public static final String PAYOUT_BANK_ACCOUNT = "PAYOUT_BANK_ACCOUNT";

    private final SensitiveDataAccessLogRepository repository;

    @Transactional
    public void recordReveal(Authentication authentication, String subjectType, Long subjectId) {
        String actor = authentication == null ? "anonymous" : authentication.getName();
        SensitiveDataAccessLog entry = new SensitiveDataAccessLog();
        entry.setActorEmail(actor);
        entry.setSubjectType(subjectType);
        entry.setSubjectId(subjectId);
        entry.setAccessedAt(LocalDateTime.now());
        repository.save(entry);
        log.info("AUDIT sensitive data revealed: type={} id={} by {}", subjectType, subjectId, actor);
    }

    @Transactional(readOnly = true)
    public PageResponse<SensitiveDataAccessLog> recent(int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        return PageResponse.of(repository.findAllByOrderByAccessedAtDesc(PageRequest.of(Math.max(page, 0), safeSize)), e -> e);
    }
}
