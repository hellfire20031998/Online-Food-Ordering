package com.hellfire.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"foods"})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO )
    private  Long id;

    private String name;

    @ManyToOne
    @JsonIgnore
    private Restaurant restaurant;

}
