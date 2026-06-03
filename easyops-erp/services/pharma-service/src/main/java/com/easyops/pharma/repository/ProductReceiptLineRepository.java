package com.easyops.pharma.repository;

import com.easyops.pharma.entity.ProductReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductReceiptLineRepository extends JpaRepository<ProductReceiptLine, UUID> {
    
    List<ProductReceiptLine> findByProductReceiptId(UUID productReceiptId);
    
    List<ProductReceiptLine> findByProductId(UUID productId);
}

