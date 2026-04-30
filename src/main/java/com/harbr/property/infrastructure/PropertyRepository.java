package com.harbr.property.infrastructure;

import com.harbr.property.domain.Property;
import com.harbr.property.domain.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Page<Property> findByHostIdAndDeletedAtIsNull(UUID hostId, Pageable pageable);

    Page<Property> findByStatusAndDeletedAtIsNull(PropertyStatus status, Pageable pageable);

    Page<Property> findByDeletedAtIsNull(Pageable pageable);
}