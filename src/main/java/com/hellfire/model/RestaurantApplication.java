package com.hellfire.model;

import com.hellfire.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A request from an existing user to become a restaurant owner. On approval the platform team
 * turns it into a Restaurant (plus its bank account) and promotes the applicant to ADMIN.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "restaurant_applications")
public class RestaurantApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private User applicant;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String applicantPhone;

    // ---- restaurant details
    private String restaurantName;
    @Column(length = 1000)
    private String description;
    private String cuisineType;
    private String openingHours;

    private String streetAddress;
    private String city;
    private String state;
    private String pincode;
    private String country;

    @Embedded
    private ContactInformation contactInformation;

    @ElementCollection
    @CollectionTable(name = "restaurant_application_images", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();

    // ---- payout account (copied to RestaurantBankAccount on approval)
    private String bankAccountHolderName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String bankAccountNumber;

    private String bankIfsc;
    private String bankName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    @ToString.Exclude
    private String upiId;

    // ---- review
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    @Column(length = 1000)
    private String rejectionReason;

    @OneToOne
    private Restaurant restaurant;
}
