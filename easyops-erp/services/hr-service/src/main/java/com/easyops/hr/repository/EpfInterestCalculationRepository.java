package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfInterestCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfInterestCalculationRepository extends JpaRepository<EpfInterestCalculation, UUID> {
    
    List<EpfInterestCalculation> findByEpfAccountId(UUID epfAccountId);
    
    List<EpfInterestCalculation> findByOrganizationId(UUID organizationId);
    
    List<EpfInterestCalculation> findByFinancialYear(Integer financialYear);
    
    Optional<EpfInterestCalculation> findByEpfAccountIdAndFinancialYear(UUID epfAccountId, Integer financialYear);
    
    List<EpfInterestCalculation> findByStatus(String status);
}

