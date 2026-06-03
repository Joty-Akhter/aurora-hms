package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.PharmacyNearExpiryRule;
import com.easyops.hospitalpharmacy.repository.PharmacyNearExpiryRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Phase P3 WS-J — evaluates {@code pharmacy_near_expiry_rules} against batch expiry dates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NearExpiryEvaluationService {

    private final PharmacyNearExpiryRuleRepository pharmacyNearExpiryRuleRepository;

    public void assertDispenseAllowedForExpiry(Drug drug, LocalDate expiryDate) {
        if (expiryDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            throw new IllegalArgumentException("Cannot dispense expired stock for drug " + drug.getId());
        }
        long daysUntil = ChronoUnit.DAYS.between(today, expiryDate);
        List<PharmacyNearExpiryRule> rules = pharmacyNearExpiryRuleRepository.findEffectiveOnOrBefore(today);
        PharmacyNearExpiryRule.Action strictest = null;
        for (PharmacyNearExpiryRule r : rules) {
            if (r.getTherapeuticClassId() != null
                    && !Objects.equals(r.getTherapeuticClassId(), drug.getTherapeuticClassId())) {
                continue;
            }
            if (daysUntil > r.getDaysBeforeExpiry()) {
                continue;
            }
            strictest = stricter(strictest, r.getAction());
        }
        if (strictest == null) {
            return;
        }
        switch (strictest) {
            case BLOCK -> throw new IllegalArgumentException(
                    "Near-expiry policy blocks dispensing drug " + drug.getId() + " (batch expires " + expiryDate + ")");
            case ALLOW_WITH_APPROVAL -> log.warn(
                    "Near-expiry policy requires approval for drug {} (batch expires {}); proceeding — approval UI not yet enforced",
                    drug.getId(), expiryDate);
            case WARN -> log.warn("Near-expiry warning: drug {} batch expires {} (within configured window)", drug.getId(), expiryDate);
            case ALLOW -> {
                // no-op
            }
        }
    }

    private static PharmacyNearExpiryRule.Action stricter(
            PharmacyNearExpiryRule.Action current,
            PharmacyNearExpiryRule.Action candidate) {
        if (current == null) {
            return candidate;
        }
        return priority(candidate) > priority(current) ? candidate : current;
    }

    private static int priority(PharmacyNearExpiryRule.Action a) {
        return switch (a) {
            case BLOCK -> 3;
            case ALLOW_WITH_APPROVAL -> 2;
            case WARN -> 2;
            case ALLOW -> 1;
        };
    }
}
