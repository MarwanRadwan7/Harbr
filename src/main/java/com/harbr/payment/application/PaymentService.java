package com.harbr.payment.application;

import com.harbr.booking.domain.*;
import com.harbr.booking.infrastructure.BookingRepository;
import com.harbr.booking.infrastructure.PaymentTransactionRepository;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.EntityNotFoundException;
import com.harbr.notification.application.NotificationService;
import com.harbr.notification.domain.NotificationChannel;
import com.harbr.payment.application.dto.CreatePaymentIntentRequest;
import com.harbr.payment.application.dto.PaymentIntentResponse;
import com.harbr.payment.infrastructure.PaymentGateway;
import com.harbr.payment.infrastructure.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentProperties paymentProperties;
    private final NotificationService notificationService;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(UUID userId, CreatePaymentIntentRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking", request.bookingId()));

        if (!booking.getGuest().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "Only the guest can pay for their booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Booking must be PENDING or CONFIRMED to process payment");
        }

        if (request.amount().compareTo(booking.getTotalPrice()) != 0) {
            throw new BusinessException("AMOUNT_MISMATCH", "Payment amount must match booking total of " + booking.getTotalPrice());
        }

        PaymentGateway.CreatePaymentIntentResult result = paymentGateway.createPaymentIntent(
                request.amount(),
                request.currency(),
                booking.getId().toString()
        );

        PaymentTransaction transaction = PaymentTransaction.builder()
                .booking(booking)
                .provider(PaymentTransaction.Provider.valueOf(paymentProperties.getProvider().toUpperCase()))
                .providerTxId(result.providerTxId())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.PENDING)
                .type(PaymentType.CHARGE)
                .build();

        try {
            paymentTransactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate payment transaction for providerTxId={}, ignoring", result.providerTxId());
        }

        return new PaymentIntentResponse(result.clientSecret(), result.providerTxId(), paymentProperties.getProvider());
    }

    @Transactional
    public void processWebhookEvent(PaymentGateway.WebhookResult result) {
        if (result == null) return;

        PaymentTransaction transaction = paymentTransactionRepository.findByProviderTxId(result.providerTxId())
                .orElseGet(() -> createMissingTransaction(result));

        if (result.success()) {
            transaction.setStatus(PaymentStatus.SUCCEEDED);
            transaction.setProcessedAt(Instant.now());

            Booking booking = transaction.getBooking();
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                log.info("Booking confirmed via payment: bookingId={}", booking.getId());

                notificationService.createNotification(
                        booking.getGuest().getId(), "Payment Successful",
                        "Your payment for " + booking.getProperty().getTitle() + " was successful. Booking confirmed!",
                        NotificationChannel.IN_APP, "PAYMENT_SUCCEEDED", "payment",
                        transaction.getId().toString(), booking.getId(), "booking"
                );
            }
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason(result.failureReason());

            Booking booking = transaction.getBooking();
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.REJECTED);
                bookingRepository.save(booking);
                log.info("Booking rejected via payment failure: bookingId={}", booking.getId());

                notificationService.createNotification(
                        booking.getGuest().getId(), "Payment Failed",
                        "Your payment for " + booking.getProperty().getTitle() + " failed. Booking rejected.",
                        NotificationChannel.IN_APP, "PAYMENT_FAILED", "payment",
                        transaction.getId().toString(), booking.getId(), "booking"
                );
            }
        }

        paymentTransactionRepository.save(transaction);
    }

    private PaymentTransaction createMissingTransaction(PaymentGateway.WebhookResult result) {
        log.warn("No existing transaction found for providerTxId={}, creating from webhook", result.providerTxId());

        Booking booking = findBookingFromMetadata(result);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .booking(booking)
                .provider(PaymentTransaction.Provider.valueOf(paymentProperties.getProvider().toUpperCase()))
                .providerTxId(result.providerTxId())
                .amount(result.amount())
                .currency(result.currency())
                .status(PaymentStatus.PENDING)
                .type(PaymentType.CHARGE)
                .build();

        return paymentTransactionRepository.save(transaction);
    }

    private Booking findBookingFromMetadata(PaymentGateway.WebhookResult result) {
        return bookingRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("NO_BOOKING", "No booking found for webhook event"));
    }
}