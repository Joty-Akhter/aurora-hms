package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.StockRequisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockRequisitionRepository extends JpaRepository<StockRequisition, UUID> {

    List<StockRequisition> findByFromLocationIdOrderByCreatedAtDesc(UUID fromLocationId);

    List<StockRequisition> findByToLocationIdOrderByCreatedAtDesc(UUID toLocationId);

    List<StockRequisition> findByStatusOrderByCreatedAtDesc(StockRequisition.StockRequisitionStatus status);
}
