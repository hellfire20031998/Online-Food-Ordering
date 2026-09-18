package com.hellfire.model;

import com.hellfire.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A customer's saved destination for refunds of cash-on-delivery orders. Account number and UPI id
 * are encrypted at rest and excluded from toString so they never reach the logs.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "customer_bank_accounts")
public class CustomerBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @ToString.Exclude
    private User user;

    private String accountHolderName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String accountNumber;

    private String ifsc;
    private String bankName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String upiId;

    private LocalDateTime updatedAt;
}
