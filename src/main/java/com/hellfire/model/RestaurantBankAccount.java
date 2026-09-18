package com.hellfire.model;

import com.hellfire.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Payout destination for a restaurant, used by the team for manual bank transfers.
 * Account number and UPI id are encrypted at rest; only the owner and TEAM_ADMIN / TEAM_MANAGER
 * ever see them unmasked.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "restaurant_bank_accounts")
public class RestaurantBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @ToString.Exclude
    private Restaurant restaurant;

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
    private String updatedBy;
}
