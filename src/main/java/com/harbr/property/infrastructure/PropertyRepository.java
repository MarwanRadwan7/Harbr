package com.harbr.property.infrastructure;

import com.harbr.property.domain.Property;
import com.harbr.property.domain.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    Page<Property> findByHostIdAndDeletedAtIsNull(UUID hostId, Pageable pageable);

    Page<Property> findByStatusAndDeletedAtIsNull(PropertyStatus status, Pageable pageable);

    Page<Property> findByDeletedAtIsNull(Pageable pageable);

    @Query(value = """
            SELECT p.* FROM properties p
            JOIN addresses a ON p.address_id = a.id
            WHERE p.deleted_at IS NULL
              AND p.status = 'ACTIVE'
              AND ST_DWithin(
                  ST_SetSRID(ST_MakePoint(a.lng, a.lat), 4326)::geography,
                  ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY p.created_at DESC
            """,
            nativeQuery = true)
    List<Property> findWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );
}