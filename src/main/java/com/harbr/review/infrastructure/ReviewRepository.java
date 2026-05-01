package com.harbr.review.infrastructure;

import com.harbr.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBookingId(UUID bookingId);

    List<Review> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId);

    boolean existsByBookingId(UUID bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.property.id = :propertyId")
    Double getAverageRatingByPropertyId(UUID propertyId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.property.id = :propertyId")
    Long countByPropertyId(UUID propertyId);
}