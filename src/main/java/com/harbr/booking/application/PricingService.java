package com.harbr.booking.application;

import com.harbr.booking.application.dto.PricingBreakdown;
import com.harbr.property.domain.AvailabilityRule;
import com.harbr.property.domain.Property;
import com.harbr.property.domain.RuleType;
import com.harbr.property.infrastructure.AvailabilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");
    private final AvailabilityRuleRepository availabilityRuleRepository;

    @Transactional(readOnly = true)
    public PricingBreakdown calculatePricing(Property property, LocalDate checkIn, LocalDate checkOut) {
        int nights = checkIn.until(checkOut).getDays();
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        List<AvailabilityRule> rules = availabilityRuleRepository
                .findByPropertyIdOrderByStartDateAsc(property.getId());

        BigDecimal nightlyRate = property.getBasePricePerNight();
        BigDecimal baseAmount = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightPrice = nightlyRate;

            for (AvailabilityRule rule : rules) {
                if (!night.isBefore(rule.getStartDate()) && !night.isAfter(rule.getEndDate())) {
                    if (rule.getRuleType() == RuleType.PRICE_OVERRIDE && rule.getPriceOverride() != null) {
                        nightPrice = rule.getPriceOverride();
                    }
                    if (rule.getRuleType() == RuleType.BLOCKED) {
                        throw new IllegalStateException("Property is blocked on " + night);
                    }
                }
            }

            baseAmount = baseAmount.add(nightPrice);
        }

        BigDecimal cleaningFee = property.getCleaningFee() != null ? property.getCleaningFee() : BigDecimal.ZERO;
        BigDecimal serviceFee = baseAmount.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = baseAmount.add(cleaningFee).add(serviceFee);

        return new PricingBreakdown(
                baseAmount.setScale(2, RoundingMode.HALF_UP),
                cleaningFee,
                serviceFee,
                totalPrice.setScale(2, RoundingMode.HALF_UP),
                nights,
                nightlyRate
        );
    }
}