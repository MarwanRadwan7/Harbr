package com.harbr.review.application;

import com.harbr.booking.domain.Booking;
import com.harbr.booking.domain.BookingStatus;
import com.harbr.booking.infrastructure.BookingRepository;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.ConflictException;
import com.harbr.common.exception.EntityNotFoundException;
import com.harbr.review.application.dto.CreateReviewRequest;
import com.harbr.review.application.dto.PropertyRatingSummary;
import com.harbr.review.application.dto.ReviewResponse;
import com.harbr.review.domain.Review;
import com.harbr.review.infrastructure.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewResponse create(UUID guestId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking", request.bookingId()));

        if (!booking.getGuest().getId().equals(guestId)) {
            throw new BusinessException("FORBIDDEN", "Only the guest can review their own booking");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Only completed bookings can be reviewed");
        }

        if (reviewRepository.existsByBookingId(request.bookingId())) {
            throw new ConflictException("This booking has already been reviewed");
        }

        Review review = Review.builder()
                .property(booking.getProperty())
                .booking(booking)
                .guest(booking.getGuest())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        review = reviewRepository.save(review);
        log.info("Review created: id={}, property={}, rating={}", review.getId(), booking.getProperty().getId(), request.rating());

        return toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByProperty(UUID propertyId) {
        return reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyRatingSummary getPropertyRating(UUID propertyId) {
        Double avg = reviewRepository.getAverageRatingByPropertyId(propertyId);
        Long count = reviewRepository.countByPropertyId(propertyId);
        return new PropertyRatingSummary(avg, count);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProperty().getId(),
                review.getBooking().getId(),
                review.getGuest().getId(),
                review.getGuest().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}