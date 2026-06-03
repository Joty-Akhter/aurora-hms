package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.DispenseIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DispenseIdempotencyRepository extends JpaRepository<DispenseIdempotencyRecord, UUID> {

    Optional<DispenseIdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
