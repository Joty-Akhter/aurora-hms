package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.EmergencyPurchaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyPurchaseEntryRepository extends JpaRepository<EmergencyPurchaseEntry, UUID> {

    List<EmergencyPurchaseEntry> findByToLocationIdOrderByCreatedAtDesc(UUID toLocationId);

    List<EmergencyPurchaseEntry> findByStatusOrderByCreatedAtDesc(EmergencyPurchaseEntry.EmergencyPurchaseStatus status);
}
