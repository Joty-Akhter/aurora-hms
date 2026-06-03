package com.easyops.pharma.repository;

import com.easyops.pharma.entity.ProductDisbursementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductDisbursementLineRepository extends JpaRepository<ProductDisbursementLine, UUID> {
    
    List<ProductDisbursementLine> findByProductDisbursementId(UUID productDisbursementId);
    
    List<ProductDisbursementLine> findByProductId(UUID productId);
}

