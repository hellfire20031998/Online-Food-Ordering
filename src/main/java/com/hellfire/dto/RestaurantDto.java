package com.hellfire.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class RestaurantDto {

    private String title;

    @Column(length = 1000)
    private String images;

    private String description;

    private Long id;
}
