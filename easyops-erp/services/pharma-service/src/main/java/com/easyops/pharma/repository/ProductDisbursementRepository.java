package com.easyops.pharma.repository;

import com.easyops.pharma.entity.ProductDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductDisbursementRepository extends JpaRepository<ProductDisbursement, UUID> {
    
    List<ProductDisbursement> findByOrganizationId(UUID organizationId);
    
    List<ProductDisbursement> findByTerritoryId(UUID territoryId);
    
    List<ProductDisbursement> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    @Query("SELECT pd FROM ProductDisbursement pd WHERE pd.territoryId = :territoryId AND pd.year = :year AND pd.month = :month ORDER BY pd.disbursementDate DESC")
    List<ProductDisbursement> findByTerritoryAndPeriod(@Param("territoryId") UUID territoryId, @Param("year") Integer year, @Param("month") Integer month);
    
    List<ProductDisbursement> findByOrganizationIdAndDisbursementDateBetween(UUID organizationId, LocalDate startDate, LocalDate endDate);
}

