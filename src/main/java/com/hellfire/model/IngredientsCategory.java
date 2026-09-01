package com.hellfire.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientsCategory {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private  String name;

    @ManyToOne
    @JsonIgnore
    private Restaurant restaurant;

    @JsonIgnore
    @OneToMany(mappedBy = "category",cascade = CascadeType.ALL)
    private List<IngredientsItem> ingredients=new ArrayList<>();
}
