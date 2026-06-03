package com.easyops.pharma.repository;

import com.easyops.pharma.entity.AdjustmentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdjustmentLineRepository extends JpaRepository<AdjustmentLine, UUID> {
    
    List<AdjustmentLine> findByAdjustmentId(UUID adjustmentId);
    
    List<AdjustmentLine> findByProductId(UUID productId);
}

