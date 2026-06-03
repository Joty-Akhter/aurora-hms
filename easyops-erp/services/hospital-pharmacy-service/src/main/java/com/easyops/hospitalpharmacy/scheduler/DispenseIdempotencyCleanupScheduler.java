package com.easyops.hospitalpharmacy.scheduler;

import com.easyops.hospitalpharmacy.config.PharmacyDispenseProperties;
import com.easyops.hospitalpharmacy.repository.DispenseIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * TTL for stored idempotency replay payloads (Phase P1 — plan K2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DispenseIdempotencyCleanupScheduler {

    private final DispenseIdempotencyRepository dispenseIdempotencyRepository;
    private final PharmacyDispenseProperties pharmacyDispenseProperties;

    @Scheduled(cron = "${hospital.pharmacy.dispense.idempotency-cleanup-cron:0 0 3 * * *}")
    public void purgeExpiredIdempotencyRecords() {
        int days = pharmacyDispenseProperties.getIdempotencyRetentionDays();
        if (days <= 0) {
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minus(days, ChronoUnit.DAYS);
        long removed = dispenseIdempotencyRepository.deleteByCreatedAtBefore(cutoff);
        if (removed > 0) {
            log.info("Removed {} dispense idempotency record(s) older than {} day(s)", removed, days);
        }
    }
}
