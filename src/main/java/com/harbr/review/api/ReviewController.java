package com.harbr.review.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.review.application.ReviewService;
import com.harbr.review.application.dto.CreateReviewRequest;
import com.harbr.review.application.dto.PropertyRatingSummary;
import com.harbr.review.application.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        ReviewResponse response = reviewService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> listByProperty(@PathVariable UUID propertyId) {
        List<ReviewResponse> reviews = reviewService.listByProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    @GetMapping("/property/{propertyId}/rating")
    public ResponseEntity<ApiResponse<PropertyRatingSummary>> getPropertyRating(@PathVariable UUID propertyId) {
        PropertyRatingSummary summary = reviewService.getPropertyRating(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}