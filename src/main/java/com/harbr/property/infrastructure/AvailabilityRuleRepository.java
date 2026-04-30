package com.harbr.property.infrastructure;

import com.harbr.property.domain.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {

    List<AvailabilityRule> findByPropertyIdOrderByStartDateAsc(UUID propertyId);

    void deleteAllByPropertyId(UUID propertyId);
}