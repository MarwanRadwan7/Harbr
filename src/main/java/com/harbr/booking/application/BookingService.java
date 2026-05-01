package com.harbr.booking.application;

import com.harbr.auth.domain.Role;
import com.harbr.auth.domain.User;
import com.harbr.auth.infrastructure.UserRepository;
import com.harbr.booking.application.dto.*;
import com.harbr.booking.domain.*;
import com.harbr.booking.infrastructure.BookingRepository;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.ConflictException;
import com.harbr.common.exception.EntityNotFoundException;
import com.harbr.common.exception.ForbiddenException;
import com.harbr.common.web.PagedResponse;
import com.harbr.property.domain.Property;
import com.harbr.property.domain.PropertyStatus;
import com.harbr.property.infrastructure.PropertyRepository;
import com.harbr.notification.application.NotificationService;
import com.harbr.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");
    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING, BookingStatus.CONFIRMED
    );

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    @Transactional
    public BookingResponse create(UUID guestId, CreateBookingRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Property", request.propertyId()));

        if (property.getStatus() != PropertyStatus.ACTIVE) {
            throw new BusinessException("PROPERTY_UNAVAILABLE", "Property is not available for booking");
        }

        if (request.guestCount() > property.getMaxGuests()) {
            throw new BusinessException("GUEST_LIMIT_EXCEEDED",
                    "Guest count " + request.guestCount() + " exceeds maximum of " + property.getMaxGuests());
        }

        validateDates(request.checkIn(), request.checkOut());

        List<Booking> overlapping = bookingRepository.findOverlappingBookingsForUpdate(
                property.getId(),
                request.checkIn(),
                request.checkOut(),
                List.of(BookingStatus.CANCELLED, BookingStatus.REJECTED)
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Property is already booked for the selected dates");
        }

        PricingBreakdown pricing = pricingService.calculatePricing(property, request.checkIn(), request.checkOut());

        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new EntityNotFoundException("User", guestId));

        Booking booking = Booking.builder()
                .property(property)
                .guest(guest)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .guestCount(request.guestCount())
                .baseAmount(pricing.baseAmount())
                .cleaningFee(pricing.cleaningFee())
                .serviceFee(pricing.serviceFee())
                .totalPrice(pricing.totalPrice())
                .build();

        if (property.getIsInstantBook()) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }

        booking = bookingRepository.save(booking);
        log.info("Booking created: id={}, property={}, guest={}, status={}",
                booking.getId(), property.getId(), guestId, booking.getStatus());

        return toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> listMyBookings(UUID userId, String role, int page, int size) {
        Page<Booking> bookings;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if ("HOST".equalsIgnoreCase(role)) {
            bookings = bookingRepository.findByPropertyHostId(userId, pageable);
        } else {
            bookings = bookingRepository.findByGuestId(userId, pageable);
        }

        return PagedResponse.from(bookings.map(this::toBookingResponse));
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID bookingId, UUID userId) {
        Booking booking = findBookingOrThrow(bookingId);
        verifyAccess(booking, userId);
        return toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, UUID userId, CancelBookingRequest request) {
        Booking booking = findBookingOrThrow(bookingId);
        verifyAccess(booking, userId);

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.reason());
        booking.setCancelledBy(determineCancelledBy(booking, userId));
        booking.setCancelledAt(Instant.now());

        booking = bookingRepository.save(booking);
        log.info("Booking cancelled: id={}, cancelledBy={}", booking.getId(), booking.getCancelledBy());

        UUID notifyUserId = "HOST".equals(booking.getCancelledBy())
                ? booking.getGuest().getId()
                : booking.getProperty().getHost().getId();
        notificationService.createNotification(
                notifyUserId, "Booking Cancelled",
                "A booking for " + booking.getProperty().getTitle() + " has been cancelled",
                NotificationChannel.IN_APP, "BOOKING_CANCELLED", "booking",
                booking.getId().toString(), booking.getId(), "booking"
        );

        return toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse confirm(UUID bookingId, UUID userId) {
        Booking booking = findBookingOrThrow(bookingId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!booking.getProperty().getHost().getId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the host can confirm a booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only PENDING bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        log.info("Booking confirmed: id={}", booking.getId());

        notificationService.createNotification(
                booking.getGuest().getId(), "Booking Confirmed",
                "Your booking for " + booking.getProperty().getTitle() + " has been confirmed",
                NotificationChannel.IN_APP, "BOOKING_CONFIRMED", "booking",
                booking.getId().toString(), booking.getId(), "booking"
        );

        return toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse reject(UUID bookingId, UUID userId) {
        Booking booking = findBookingOrThrow(bookingId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!booking.getProperty().getHost().getId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the host can reject a booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);
        log.info("Booking rejected: id={}", booking.getId());

        notificationService.createNotification(
                booking.getGuest().getId(), "Booking Rejected",
                "Your booking for " + booking.getProperty().getTitle() + " has been rejected",
                NotificationChannel.IN_APP, "BOOKING_REJECTED", "booking",
                booking.getId().toString(), booking.getId(), "booking"
        );

        return toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse complete(UUID bookingId, UUID userId) {
        Booking booking = findBookingOrThrow(bookingId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!booking.getProperty().getHost().getId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the host can complete a booking");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("INVALID_STATUS", "Only CONFIRMED bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);
        log.info("Booking completed: id={}", booking.getId());

        notificationService.createNotification(
                booking.getGuest().getId(), "Booking Completed",
                "Your stay at " + booking.getProperty().getTitle() + " has been marked as completed. You can now leave a review!",
                NotificationChannel.IN_APP, "BOOKING_COMPLETED", "booking",
                booking.getId().toString(), booking.getId(), "booking"
        );

        return toBookingResponse(booking);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException("INVALID_DATES", "Check-in date cannot be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("INVALID_DATES", "Check-out date must be after check-in date");
        }
    }

    private Booking findBookingOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));
    }

    private void verifyAccess(Booking booking, UUID userId) {
        if (!booking.getGuest().getId().equals(userId)
                && !booking.getProperty().getHost().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));
            if (user.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You do not have access to this booking");
            }
        }
    }

    private String determineCancelledBy(Booking booking, UUID userId) {
        if (booking.getProperty().getHost().getId().equals(userId)) {
            return "HOST";
        }
        return "GUEST";
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getProperty().getId(),
                booking.getProperty().getTitle(),
                booking.getGuest().getId(),
                booking.getGuest().getFullName(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getGuestCount(),
                booking.getBaseAmount(),
                booking.getCleaningFee(),
                booking.getServiceFee(),
                booking.getTotalPrice(),
                booking.getStatus().name(),
                booking.getCancellationReason(),
                booking.getCancelledBy(),
                booking.getCancelledAt(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}