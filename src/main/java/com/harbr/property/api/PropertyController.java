package com.harbr.property.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import com.harbr.property.application.PropertyService;
import com.harbr.property.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreatePropertyRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        PropertyResponse response = propertyService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>> list(
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
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (hasAnyFilter(city, country, propertyType, minGuests, minBedrooms, minBathrooms, minPrice, maxPrice, isInstantBook, amenityIds, query)) {
            PropertySearchRequest searchRequest = new PropertySearchRequest(
                    city, country, propertyType, minGuests, minBedrooms, minBathrooms,
                    minPrice, maxPrice, isInstantBook, amenityIds, null, null,
                    null, null, null, query, page, size
            );
            PagedResponse<PropertyResponse> response = propertyService.search(searchRequest);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }

        PagedResponse<PropertyResponse> response = propertyService.list(page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private boolean hasAnyFilter(String city, String country, String propertyType, Integer minGuests,
                                 Integer minBedrooms, Integer minBathrooms, BigDecimal minPrice,
                                 BigDecimal maxPrice, Boolean isInstantBook,
                                 List<UUID> amenityIds, String query) {
        return (city != null && !city.isBlank())
                || (country != null && !country.isBlank())
                || (propertyType != null && !propertyType.isBlank())
                || minGuests != null
                || minBedrooms != null
                || minBathrooms != null
                || minPrice != null
                || maxPrice != null
                || (isInstantBook != null && isInstantBook)
                || (amenityIds != null && !amenityIds.isEmpty())
                || (query != null && !query.isBlank());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(@PathVariable UUID id) {
        PropertyResponse response = propertyService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody UpdatePropertyRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        PropertyResponse response = propertyService.update(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        propertyService.softDelete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<PropertyImageResponse>> uploadImage(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isCover", defaultValue = "false") boolean isCover) {
        UUID userId = (UUID) authentication.getPrincipal();
        PropertyImageResponse response = propertyService.addImage(id, userId, file, isCover);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        propertyService.deleteImage(imageId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityRuleDto>>> getAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getAvailabilityRules(id)));
    }

    @PostMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<AvailabilityRuleDto>> addAvailabilityRule(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilityRuleRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AvailabilityRuleDto response = propertyService.addAvailabilityRule(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}