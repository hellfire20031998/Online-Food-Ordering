package com.hellfire.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hellfire.Dto.RestaurantDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private  String fullName;
    private  String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private  String password;


    private USER_ROLE role;

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "customer")
    @JsonIgnore
    private List<Order> orders=new ArrayList<>();

    @ElementCollection
    private List<RestaurantDto> favorites = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RestaurantRole> restaurantRoles = new ArrayList<>();



}
