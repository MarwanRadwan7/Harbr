package com.harbr.search.application;

import com.harbr.property.domain.Property;
import com.harbr.property.domain.PropertyStatus;
import com.harbr.search.domain.PropertyDocument;
import com.harbr.search.infrastructure.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "harbr.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchSyncService {

    private final PropertySearchRepository propertySearchRepository;

    public void indexProperty(Property property) {
        if (property.getDeletedAt() != null || property.getStatus() != PropertyStatus.ACTIVE) {
            propertySearchRepository.deleteById(property.getId());
            return;
        }

        PropertyDocument doc = toDocument(property);
        propertySearchRepository.save(doc);
        log.info("Indexed property in Elasticsearch: id={}", property.getId());
    }

    public void removeProperty(UUID propertyId) {
        propertySearchRepository.deleteById(propertyId);
        log.info("Removed property from Elasticsearch: id={}", propertyId);
    }

    private PropertyDocument toDocument(Property property) {
        return new PropertyDocument(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getPropertyType().name(),
                property.getMaxGuests(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getBasePricePerNight(),
                property.getStatus().name(),
                property.getIsInstantBook(),
                property.getAddress().getCity(),
                property.getAddress().getCountry(),
                property.getAddress().getLat(),
                property.getAddress().getLng(),
                property.getHost().getFullName()
        );
    }
}