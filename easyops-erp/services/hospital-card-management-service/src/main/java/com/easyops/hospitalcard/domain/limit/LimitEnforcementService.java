package com.easyops.hospitalcard.domain.limit;

import com.easyops.hospitalcard.api.dto.LimitUsageSummary;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import com.easyops.hospitalcard.domain.product.LimitProfile;
import com.easyops.hospitalcard.domain.product.LimitProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LimitEnforcementService {

    private static final String RESET_DAILY = "DAILY";
    private static final String RESET_MONTHLY = "MONTHLY";
    private static final String REASON_LIMIT_EXCEEDED = "LIMIT_EXCEEDED";

    private final CardRepository cardRepository;
    private final CardProductRepository cardProductRepository;
    private final LimitProfileRepository limitProfileRepository;
    private final CardLimitUsageRepository cardLimitUsageRepository;

    /**
     * Resolve the limit profile for a card: card override or product default.
     */
    @Transactional(readOnly = true)
    public LimitProfile resolveLimitProfile(UUID cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null) return null;
        if (card.getLimitProfileId() != null) {
            return limitProfileRepository.findById(card.getLimitProfileId()).orElse(null);
        }
        CardProduct product = cardProductRepository.findById(card.getCardProductId()).orElse(null);
        if (product == null || product.getDefaultLimitProfileId() == null) return null;
        return limitProfileRepository.findById(product.getDefaultLimitProfileId()).orElse(null);
    }

    /**
     * Compute current period start/end in UTC from profile's reset_policy.
     */
    public static PeriodBounds computePeriod(LimitProfile profile, OffsetDateTime now) {
        OffsetDateTime utc = now.withOffsetSameInstant(ZoneOffset.UTC);
        String policy = profile.getResetPolicy() != null ? profile.getResetPolicy().toUpperCase() : RESET_DAILY;

        if (RESET_MONTHLY.equals(policy)) {
            OffsetDateTime periodStart = utc.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            OffsetDateTime periodEnd = periodStart.plusMonths(1).minusNanos(1);
            return new PeriodBounds(periodStart, periodEnd);
        }
        // DAILY or default
        OffsetDateTime periodStart = utc.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime periodEnd = periodStart.plusDays(1).minusNanos(1);
        return new PeriodBounds(periodStart, periodEnd);
    }

    /**
     * Get limit usage for balance response (read-only). Returns null if card has no limit profile.
     */
    @Transactional(readOnly = true)
    public LimitUsageSummary getLimitUsageForBalance(UUID cardId) {
        LimitProfile profile = resolveLimitProfile(cardId);
        if (profile == null) return null;

        PeriodBounds period = computePeriod(profile, OffsetDateTime.now());
        CardLimitUsage usage = cardLimitUsageRepository
                .findByCardIdAndLimitProfileIdAndPeriodStartAndPeriodEnd(
                        cardId, profile.getId(), period.start, period.end)
                .orElse(null);

        LimitUsageSummary summary = new LimitUsageSummary();
        summary.setPeriodStart(period.start);
        summary.setPeriodEnd(period.end);
        summary.setResetPolicy(profile.getResetPolicy());
        summary.setAmountConsumed(usage != null ? usage.getAmountConsumed() : BigDecimal.ZERO);
        summary.setMealCountConsumed(usage != null ? usage.getMealCountConsumed() : 0);
        summary.setVisitCountConsumed(usage != null ? usage.getVisitCountConsumed() : 0);
        summary.setDailyAmountLimit(profile.getDailyAmountLimit());
        summary.setMonthlyAmountLimit(profile.getMonthlyAmountLimit());
        summary.setDailyMealLimit(profile.getDailyMealLimit());
        summary.setDailyVisitLimit(profile.getDailyVisitLimit());
        return summary;
    }

    /**
     * Get or create usage row for (card, profile, period). Creates with zero consumed if missing.
     */
    @Transactional
    public CardLimitUsage getOrCreateUsage(UUID cardId, UUID limitProfileId, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return cardLimitUsageRepository
                .findByCardIdAndLimitProfileIdAndPeriodStartAndPeriodEnd(cardId, limitProfileId, periodStart, periodEnd)
                .orElseGet(() -> {
                    CardLimitUsage usage = new CardLimitUsage();
                    usage.setId(UUID.randomUUID());
                    usage.setCardId(cardId);
                    usage.setLimitProfileId(limitProfileId);
                    usage.setPeriodStart(periodStart);
                    usage.setPeriodEnd(periodEnd);
                    usage.setAmountConsumed(BigDecimal.ZERO);
                    usage.setMealCountConsumed(0);
                    usage.setVisitCountConsumed(0);
                    OffsetDateTime t = OffsetDateTime.now();
                    usage.setCreatedAt(t);
                    usage.setUpdatedAt(t);
                    return cardLimitUsageRepository.save(usage);
                });
    }

    /**
     * Check if the requested amount/meal/visit is within limits. Returns allowed or LIMIT_EXCEEDED.
     */
    @Transactional
    public LimitCheckResult checkAuthorization(UUID cardId, BigDecimal amount, Integer mealCount, Integer visitCount) {
        LimitProfile profile = resolveLimitProfile(cardId);
        if (profile == null) {
            return LimitCheckResult.allowed();
        }

        PeriodBounds period = computePeriod(profile, OffsetDateTime.now());
        CardLimitUsage usage = getOrCreateUsage(cardId, profile.getId(), period.start, period.end);

        BigDecimal amountRequested = amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
        int mealRequested = mealCount != null && mealCount > 0 ? mealCount : 0;
        int visitRequested = visitCount != null && visitCount > 0 ? visitCount : 0;

        BigDecimal newAmountConsumed = usage.getAmountConsumed().add(amountRequested);
        int newMealConsumed = usage.getMealCountConsumed() + mealRequested;
        int newVisitConsumed = usage.getVisitCountConsumed() + visitRequested;

        String policy = profile.getResetPolicy() != null ? profile.getResetPolicy().toUpperCase() : RESET_DAILY;
        Map<String, Object> remaining = new HashMap<>();

        if (RESET_MONTHLY.equals(policy)) {
            if (profile.getMonthlyAmountLimit() != null && newAmountConsumed.compareTo(profile.getMonthlyAmountLimit()) > 0) {
                remaining.put("monthlyAmountLimit", profile.getMonthlyAmountLimit());
                remaining.put("monthlyAmountConsumed", usage.getAmountConsumed());
                remaining.put("monthlyAmountRemaining", profile.getMonthlyAmountLimit().subtract(usage.getAmountConsumed()));
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
            // Enforce daily meal/visit limits even when amount reset is MONTHLY
            if (profile.getDailyMealLimit() != null && newMealConsumed > profile.getDailyMealLimit()) {
                remaining.put("dailyMealLimit", profile.getDailyMealLimit());
                remaining.put("dailyMealConsumed", usage.getMealCountConsumed());
                remaining.put("dailyMealRemaining", profile.getDailyMealLimit() - usage.getMealCountConsumed());
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
            if (profile.getDailyVisitLimit() != null && newVisitConsumed > profile.getDailyVisitLimit()) {
                remaining.put("dailyVisitLimit", profile.getDailyVisitLimit());
                remaining.put("dailyVisitConsumed", usage.getVisitCountConsumed());
                remaining.put("dailyVisitRemaining", profile.getDailyVisitLimit() - usage.getVisitCountConsumed());
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
        } else {
            if (profile.getDailyAmountLimit() != null && newAmountConsumed.compareTo(profile.getDailyAmountLimit()) > 0) {
                remaining.put("dailyAmountLimit", profile.getDailyAmountLimit());
                remaining.put("dailyAmountConsumed", usage.getAmountConsumed());
                remaining.put("dailyAmountRemaining", profile.getDailyAmountLimit().subtract(usage.getAmountConsumed()));
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
            if (profile.getDailyMealLimit() != null && newMealConsumed > profile.getDailyMealLimit()) {
                remaining.put("dailyMealLimit", profile.getDailyMealLimit());
                remaining.put("dailyMealConsumed", usage.getMealCountConsumed());
                remaining.put("dailyMealRemaining", profile.getDailyMealLimit() - usage.getMealCountConsumed());
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
            if (profile.getDailyVisitLimit() != null && newVisitConsumed > profile.getDailyVisitLimit()) {
                remaining.put("dailyVisitLimit", profile.getDailyVisitLimit());
                remaining.put("dailyVisitConsumed", usage.getVisitCountConsumed());
                remaining.put("dailyVisitRemaining", profile.getDailyVisitLimit() - usage.getVisitCountConsumed());
                return LimitCheckResult.limitExceeded(REASON_LIMIT_EXCEEDED, remaining);
            }
        }

        return LimitCheckResult.allowed();
    }

    /**
     * Increment consumed amounts after a committed transaction (e.g. capture). Call from AuthorizationService on capture.
     */
    @Transactional
    public void recordConsumed(UUID cardId, UUID limitProfileId, BigDecimal amount, int mealDelta, int visitDelta) {
        LimitProfile profile = limitProfileRepository.findById(limitProfileId).orElse(null);
        if (profile == null) return;

        PeriodBounds period = computePeriod(profile, OffsetDateTime.now());
        CardLimitUsage usage = getOrCreateUsage(cardId, limitProfileId, period.start, period.end);

        usage.setAmountConsumed(usage.getAmountConsumed().add(amount != null ? amount.abs() : BigDecimal.ZERO));
        usage.setMealCountConsumed(usage.getMealCountConsumed() + Math.max(0, mealDelta));
        usage.setVisitCountConsumed(usage.getVisitCountConsumed() + Math.max(0, visitDelta));
        usage.setUpdatedAt(OffsetDateTime.now());
        cardLimitUsageRepository.save(usage);
    }

    public static final class PeriodBounds {
        public final OffsetDateTime start;
        public final OffsetDateTime end;

        public PeriodBounds(OffsetDateTime start, OffsetDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
