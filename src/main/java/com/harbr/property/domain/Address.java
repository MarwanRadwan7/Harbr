package com.harbr.property.domain;

import com.harbr.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false)
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;
}