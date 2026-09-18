package com.hellfire.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit trail of every time a platform team member viewed unmasked bank account details.
 * Required by the RBI expectation of access logging for sensitive customer financial data.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "sensitive_data_access_log", indexes = @Index(name = "idx_sda_accessed_at", columnList = "accessedAt"))
public class SensitiveDataAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorEmail;

    /** e.g. CUSTOMER_BANK_ACCOUNT, RESTAURANT_BANK_ACCOUNT, APPLICATION_BANK_ACCOUNT, REFUND_BANK_ACCOUNT, PAYOUT_BANK_ACCOUNT */
    private String subjectType;

    private Long subjectId;

    private LocalDateTime accessedAt;
}
