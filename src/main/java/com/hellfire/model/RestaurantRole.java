package com.hellfire.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Restaurant restaurant;

    @Getter
    @Enumerated(EnumType.STRING)
    private USER_ROLE role;




}
