package com.harbr.property.infrastructure;

import com.harbr.property.domain.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
}