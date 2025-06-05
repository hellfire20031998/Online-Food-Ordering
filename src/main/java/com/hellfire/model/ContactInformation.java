package com.hellfire.model;


import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.sql.exec.spi.StandardEntityInstanceResolver;

//@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInformation {

    private String email;
    private  String mobile;

    private String twitter;
    private String instagram;
}
