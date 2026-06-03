package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.EmergencyPurchaseEntry;
import com.easyops.hospitalpharmacy.entity.EmergencyPurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyPurchaseLineRepository extends JpaRepository<EmergencyPurchaseLine, UUID> {

    List<EmergencyPurchaseLine> findByPurchaseEntryOrderByCreatedAtAsc(EmergencyPurchaseEntry purchaseEntry);
}
