package com.easyops.pharma.repository;

import com.easyops.pharma.entity.ProductReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReceiptRepository extends JpaRepository<ProductReceipt, UUID> {
    
    List<ProductReceipt> findByOrganizationId(UUID organizationId);
    
    List<ProductReceipt> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<ProductReceipt> findByOrganizationIdAndReceiptDateBetween(UUID organizationId, LocalDate startDate, LocalDate endDate);
    
    Optional<ProductReceipt> findByReceiptNumber(String receiptNumber);
    
    @Query("SELECT pr FROM ProductReceipt pr WHERE pr.organizationId = :orgId ORDER BY pr.receiptDate DESC, pr.createdAt DESC")
    List<ProductReceipt> findAllByOrganizationOrderByDate(@Param("orgId") UUID organizationId);
}

