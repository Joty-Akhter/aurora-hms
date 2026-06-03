package com.easyops.hospitalcard.domain.limit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CardLimitUsageRepository extends JpaRepository<CardLimitUsage, UUID> {

    Optional<CardLimitUsage> findByCardIdAndLimitProfileIdAndPeriodStartAndPeriodEnd(
            UUID cardId,
            UUID limitProfileId,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd
    );
}
