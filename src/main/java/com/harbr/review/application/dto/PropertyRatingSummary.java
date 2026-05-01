package com.harbr.review.application.dto;

public record PropertyRatingSummary(
        Double averageRating,
        Long totalReviews
) {
    public PropertyRatingSummary {
        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (totalReviews == null) {
            totalReviews = 0L;
        }
    }
}