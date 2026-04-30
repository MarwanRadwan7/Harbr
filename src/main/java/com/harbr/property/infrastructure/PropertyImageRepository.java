package com.harbr.property.infrastructure;

import com.harbr.property.domain.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    List<PropertyImage> findByPropertyIdOrderByDisplayOrderAsc(UUID propertyId);

    void deleteAllByPropertyId(UUID propertyId);
}