package com.harbr.property.infrastructure;

import com.harbr.property.domain.PropertyAmenity;
import com.harbr.property.domain.PropertyAmenityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, PropertyAmenityId> {

    List<PropertyAmenity> findByPropertyId(UUID propertyId);

    void deleteAllByPropertyId(UUID propertyId);
}