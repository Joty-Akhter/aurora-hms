package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfFilingRepository extends JpaRepository<EpfFiling, UUID> {
    List<EpfFiling> findByOrganizationIdOrderByFilingYearDescFilingMonthDescCreatedAtDesc(UUID organizationId);

    List<EpfFiling> findByOrganizationIdAndFilingMonthAndFilingYear(UUID organizationId, Integer filingMonth, Integer filingYear);

    Optional<EpfFiling> findByOrganizationIdAndFilingMonthAndFilingYearAndFilingType(
            UUID organizationId, Integer filingMonth, Integer filingYear, String filingType);
}
