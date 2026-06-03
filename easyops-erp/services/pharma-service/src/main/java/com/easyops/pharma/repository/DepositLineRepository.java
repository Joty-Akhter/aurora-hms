package com.easyops.pharma.repository;

import com.easyops.pharma.entity.DepositLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositLineRepository extends JpaRepository<DepositLine, UUID> {
    
    List<DepositLine> findByDepositId(UUID depositId);
    
    List<DepositLine> findByProductId(UUID productId);
}

