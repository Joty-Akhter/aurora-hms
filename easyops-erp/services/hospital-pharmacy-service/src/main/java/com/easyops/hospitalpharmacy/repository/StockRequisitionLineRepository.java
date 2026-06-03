package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.StockRequisition;
import com.easyops.hospitalpharmacy.entity.StockRequisitionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockRequisitionLineRepository extends JpaRepository<StockRequisitionLine, UUID> {

    List<StockRequisitionLine> findByRequisitionOrderByCreatedAtAsc(StockRequisition requisition);

    void deleteByRequisition(StockRequisition requisition);
}
