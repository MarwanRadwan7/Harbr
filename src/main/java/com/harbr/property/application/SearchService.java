package com.harbr.property.application;

import com.harbr.common.web.PagedResponse;
import com.harbr.property.application.dto.PropertyResponse;
import com.harbr.property.application.dto.PropertySearchRequest;
import com.harbr.property.domain.Property;
import com.harbr.property.infrastructure.PropertyRepository;
import com.harbr.property.infrastructure.PropertySpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;

    @Transactional(readOnly = true)
    @Cacheable(value = "property-search", key = "#request.toString()")
    public PagedResponse<PropertyResponse> search(PropertySearchRequest request) {
        if (request.lat() != null && request.lng() != null && request.radiusKm() != null) {
            return searchWithGeo(request);
        }

        Specification<Property> spec = PropertySpecifications.withSearch(request);
        Page<Property> page = propertyRepository.findAll(spec,
                PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "createdAt")));

        return PagedResponse.from(page.map(propertyService::toPropertyResponsePublic));
    }

    private PagedResponse<PropertyResponse> searchWithGeo(PropertySearchRequest request) {
        double radiusMeters = request.radiusKm() * 1000.0;

        List<Property> geoResults = propertyRepository.findWithinRadius(
                request.lat(), request.lng(), radiusMeters
        );

        Set<UUID> geoIds = geoResults.stream()
                .map(Property::getId)
                .collect(Collectors.toSet());

        if (geoIds.isEmpty()) {
            return new PagedResponse<>(List.of(), request.page(), request.size(), 0, 0);
        }

        Specification<Property> spec = PropertySpecifications.withSearch(request)
                .and((root, query, cb) -> root.get("id").in(geoIds));

        Page<Property> page = propertyRepository.findAll(spec,
                PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "createdAt")));

        return PagedResponse.from(page.map(propertyService::toPropertyResponsePublic));
    }
}