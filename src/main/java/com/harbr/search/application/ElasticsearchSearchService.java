package com.harbr.search.application;

import com.harbr.search.domain.PropertyDocument;
import com.harbr.search.infrastructure.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "harbr.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchSearchService {

    private final PropertySearchRepository propertySearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional(readOnly = true)
    public Page<PropertyDocument> search(String query, String city, String country,
                                          String propertyType, Integer minBedrooms,
                                          Integer minBathrooms, Integer minGuests,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          Boolean isInstantBook,
                                          int page, int size) {

        Criteria criteria = new Criteria("status").is("ACTIVE");

        if (query != null && !query.isBlank()) {
            criteria = criteria.or(new Criteria("title").matches(query))
                    .or(new Criteria("description").matches(query))
                    .or(new Criteria("city").matches(query))
                    .or(new Criteria("hostName").matches(query));
        }
        if (city != null && !city.isBlank()) {
            criteria = criteria.and(new Criteria("city").is(city));
        }
        if (country != null && !country.isBlank()) {
            criteria = criteria.and(new Criteria("country").is(country));
        }
        if (propertyType != null && !propertyType.isBlank()) {
            criteria = criteria.and(new Criteria("propertyType").is(propertyType));
        }
        if (minBedrooms != null) {
            criteria = criteria.and(new Criteria("bedrooms").greaterThanEqual(minBedrooms));
        }
        if (minBathrooms != null) {
            criteria = criteria.and(new Criteria("bathrooms").greaterThanEqual(minBathrooms));
        }
        if (minGuests != null) {
            criteria = criteria.and(new Criteria("maxGuests").greaterThanEqual(minGuests));
        }
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("basePricePerNight").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("basePricePerNight").lessThanEqual(maxPrice));
        }
        if (isInstantBook != null && isInstantBook) {
            criteria = criteria.and(new Criteria("isInstantBook").is(true));
        }

        Query searchQuery = new CriteriaQuery(criteria, PageRequest.of(page, size));
        SearchHits<PropertyDocument> hits = elasticsearchOperations.search(searchQuery, PropertyDocument.class);

        List<PropertyDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, size), hits.getTotalHits());
    }
}