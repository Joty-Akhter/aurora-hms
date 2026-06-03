package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.StockAdjustmentApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockAdjustmentApprovalRepository extends JpaRepository<StockAdjustmentApproval, UUID> {

    List<StockAdjustmentApproval> findByStatusOrderByCreatedAtDesc(
            StockAdjustmentApproval.AdjustmentApprovalStatus status);

    List<StockAdjustmentApproval> findByPharmacyLocationAndStatusOrderByCreatedAtDesc(
            PharmacyLocation location, StockAdjustmentApproval.AdjustmentApprovalStatus status);
}
