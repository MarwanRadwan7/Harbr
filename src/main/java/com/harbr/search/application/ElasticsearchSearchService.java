package com.harbr.search.application;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.harbr.search.domain.PropertyDocument;
import com.harbr.search.infrastructure.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

        List<Query> must = new ArrayList<>();
        List<Query> filters = new ArrayList<>();

        filters.add(Query.of(f -> f.term(t -> t.field("status").value("ACTIVE"))));

        if (query != null && !query.isBlank()) {
            must.add(Query.of(f -> f.multiMatch(mm -> mm
                    .fields("title", "description", "city", "hostName")
                    .query(query))));
        }
        if (city != null && !city.isBlank()) {
            filters.add(Query.of(f -> f.term(t -> t.field("city").value(city))));
        }
        if (country != null && !country.isBlank()) {
            filters.add(Query.of(f -> f.term(t -> t.field("country").value(country))));
        }
        if (propertyType != null && !propertyType.isBlank()) {
            filters.add(Query.of(f -> f.term(t -> t.field("propertyType").value(propertyType))));
        }
        if (minBedrooms != null) {
            filters.add(Query.of(f -> f.range(r -> r.field("bedrooms").gte(JsonData.of(minBedrooms)))));
        }
        if (minBathrooms != null) {
            filters.add(Query.of(f -> f.range(r -> r.field("bathrooms").gte(JsonData.of(minBathrooms)))));
        }
        if (minGuests != null) {
            filters.add(Query.of(f -> f.range(r -> r.field("maxGuests").gte(JsonData.of(minGuests)))));
        }
        if (minPrice != null) {
            filters.add(Query.of(f -> f.range(r -> r.field("basePricePerNight").gte(JsonData.of(minPrice.doubleValue())))));
        }
        if (maxPrice != null) {
            filters.add(Query.of(f -> f.range(r -> r.field("basePricePerNight").lte(JsonData.of(maxPrice.doubleValue())))));
        }
        if (isInstantBook != null && isInstantBook) {
            filters.add(Query.of(f -> f.term(t -> t.field("isInstantBook").value(true))));
        }

        BoolQuery boolQuery = BoolQuery.of(b -> b
                .must(must)
                .filter(filters)
        );

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<PropertyDocument> hits = elasticsearchOperations.search(searchQuery, PropertyDocument.class);

        List<PropertyDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, size), hits.getTotalHits());
    }
}