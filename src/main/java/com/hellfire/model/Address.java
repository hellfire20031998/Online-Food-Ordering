package com.hellfire.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "user_id")
    private User user;
             // Name of the recipient
          // Contact number
    private String streetAddress;    // Flat/House number, Street, Area
    private String city;
    private String state;
    private String pincode;       // Pincode
    private String country;
                 // (e.g., Home, Work, Other) - optional
    private Boolean isDefault = false;

}
