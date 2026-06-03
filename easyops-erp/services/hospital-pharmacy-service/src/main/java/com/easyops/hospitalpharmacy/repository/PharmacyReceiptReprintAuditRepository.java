package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyReceiptReprintAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PharmacyReceiptReprintAuditRepository extends JpaRepository<PharmacyReceiptReprintAudit, UUID> {

    boolean existsByDispenseOrder_IdAndUserIdAndPrintedAtAfter(
            UUID dispenseOrderId,
            UUID userId,
            OffsetDateTime printedAfter);
}
