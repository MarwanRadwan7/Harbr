package com.harbr.search.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import com.harbr.search.application.ElasticsearchSearchService;
import com.harbr.search.domain.PropertyDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search/es")
@RequiredArgsConstructor
public class ElasticsearchSearchController {

    private final ElasticsearchSearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDocument>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) Integer minGuests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isInstantBook,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PropertyDocument> results = searchService.search(
                query, city, country, propertyType, minBedrooms, minBathrooms,
                minGuests, minPrice, maxPrice, isInstantBook, page, size);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(results)));
    }
}