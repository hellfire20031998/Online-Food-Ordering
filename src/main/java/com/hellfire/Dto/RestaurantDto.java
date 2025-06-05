package com.hellfire.Dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import org.hibernate.sql.exec.spi.StandardEntityInstanceResolver;

import java.util.List;

@Data
@Embeddable
public class RestaurantDto {

    private String title;

    @Column(length = 1000)
    private String images;

//    @Column(length = 1000)
    private  String description;

    private Long id;


}
