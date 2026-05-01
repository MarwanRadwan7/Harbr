package com.harbr.property.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import com.harbr.property.application.PropertyService;
import com.harbr.property.application.SearchService;
import com.harbr.property.application.dto.PropertyResponse;
import com.harbr.property.application.dto.PropertySearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer minGuests,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isInstantBook,
            @RequestParam(required = false) List<UUID> amenityIds,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PropertySearchRequest request = new PropertySearchRequest(
                city, country, propertyType, minGuests, minBedrooms, minBathrooms,
                minPrice, maxPrice, isInstantBook, amenityIds, checkIn, checkOut,
                lat, lng, radiusKm, query, page, size
        );

        PagedResponse<PropertyResponse> result = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}