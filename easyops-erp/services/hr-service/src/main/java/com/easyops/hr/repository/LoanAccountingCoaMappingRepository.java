package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanAccountingCoaMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanAccountingCoaMappingRepository extends JpaRepository<LoanAccountingCoaMapping, UUID> {

    List<LoanAccountingCoaMapping> findByOrganizationIdOrderByMappingKeyAsc(UUID organizationId);

    Optional<LoanAccountingCoaMapping> findByOrganizationIdAndMappingKey(UUID organizationId, String mappingKey);

    void deleteByOrganizationId(UUID organizationId);
}
