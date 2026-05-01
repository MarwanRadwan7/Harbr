package com.harbr.property.infrastructure;

import com.harbr.property.application.dto.PropertySearchRequest;
import com.harbr.property.domain.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PropertySpecifications {

    public static Specification<Property> withSearch(PropertySearchRequest request) {
        return (Root<Property> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.equal(root.get("status"), PropertyStatus.ACTIVE));

            if (request.city() != null && !request.city().isBlank()) {
                Join<Property, Address> address = root.join("address", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(address.get("city")), request.city().toLowerCase()));
            }

            if (request.country() != null && !request.country().isBlank()) {
                Join<Property, Address> address = root.join("address", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(address.get("country")), request.country().toLowerCase()));
            }

            if (request.propertyType() != null && !request.propertyType().isBlank()) {
                predicates.add(cb.equal(root.get("propertyType"), PropertyType.valueOf(request.propertyType())));
            }

            if (request.minGuests() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxGuests"), request.minGuests()));
            }

            if (request.minBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), request.minBedrooms()));
            }

            if (request.minBathrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), request.minBathrooms()));
            }

            if (request.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePricePerNight"), request.minPrice()));
            }

            if (request.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePricePerNight"), request.maxPrice()));
            }

            if (request.isInstantBook() != null && request.isInstantBook()) {
                predicates.add(cb.isTrue(root.get("isInstantBook")));
            }

            if (request.query() != null && !request.query().isBlank()) {
                String pattern = "%" + request.query().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (request.amenityIds() != null && !request.amenityIds().isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<PropertyAmenity> pa = subquery.from(PropertyAmenity.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(pa.get("property").get("id"), root.get("id")),
                        pa.get("amenity").get("id").in(request.amenityIds())
                );
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}