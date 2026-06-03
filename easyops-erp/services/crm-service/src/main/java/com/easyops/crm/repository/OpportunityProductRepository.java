package com.easyops.crm.repository;

import com.easyops.crm.entity.OpportunityProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface OpportunityProductRepository extends JpaRepository<OpportunityProduct, UUID> {
    
    List<OpportunityProduct> findByOpportunityIdOrderByLineOrder(UUID opportunityId);
    
    List<OpportunityProduct> findByOrganizationId(UUID organizationId);
    
    void deleteByOpportunityId(UUID opportunityId);
    
    @Query("SELECT SUM(op.lineTotal) FROM OpportunityProduct op WHERE op.opportunityId = :oppId")
    BigDecimal sumLineTotal(@Param("oppId") UUID opportunityId);
    
    @Query("SELECT COUNT(op) FROM OpportunityProduct op WHERE op.opportunityId = :oppId")
    long countByOpportunityId(@Param("oppId") UUID opportunityId);
}

