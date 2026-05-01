package com.harbr.search.application;

import com.harbr.search.domain.PropertyDocument;
import com.harbr.search.infrastructure.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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

        var boolQuery = co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.of(b -> {
            b.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

            if (query != null && !query.isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields("title", "description", "city", "hostName")
                        .query(query)));
            }
            if (city != null && !city.isBlank()) {
                b.filter(f -> f.term(t -> t.field("city").value(city)));
            }
            if (country != null && !country.isBlank()) {
                b.filter(f -> f.term(t -> t.field("country").value(country)));
            }
            if (propertyType != null && !propertyType.isBlank()) {
                b.filter(f -> f.term(t -> t.field("propertyType").value(propertyType)));
            }
            if (minBedrooms != null) {
                b.filter(f -> f.range(r -> r.field("bedrooms").gte(co.elastic.clients.json.JsonData.of(minBedrooms))));
            }
            if (minBathrooms != null) {
                b.filter(f -> f.range(r -> r.field("bathrooms").gte(co.elastic.clients.json.JsonData.of(minBathrooms))));
            }
            if (minGuests != null) {
                b.filter(f -> f.range(r -> r.field("maxGuests").gte(co.elastic.clients.json.JsonData.of(minGuests))));
            }
            if (minPrice != null) {
                b.filter(f -> f.range(r -> r.field("basePricePerNight").gte(co.elastic.clients.json.JsonData.of(minPrice.doubleValue()))));
            }
            if (maxPrice != null) {
                b.filter(f -> f.range(r -> r.field("basePricePerNight").lte(co.elastic.clients.json.JsonData.of(maxPrice.doubleValue()))));
            }
            if (isInstantBook != null && isInstantBook) {
                b.filter(f -> f.term(t -> t.field("isInstantBook").value(true)));
            }
        });

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