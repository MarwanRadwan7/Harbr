package com.harbr.booking.api;

import com.harbr.auth.domain.Role;
import com.harbr.booking.application.BookingService;
import com.harbr.booking.application.dto.*;
import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> listMyBookings(
            Authentication authentication,
            @RequestParam(defaultValue = "GUEST") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        PagedResponse<BookingResponse> response = bookingService.listMyBookings(userId, role, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody CancelBookingRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.cancel(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.confirm(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> reject(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.reject(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> complete(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        BookingResponse response = bookingService.complete(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}