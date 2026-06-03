package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfRemittance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfRemittanceRepository extends JpaRepository<EpfRemittance, UUID> {
    List<EpfRemittance> findByOrganizationIdOrderByRemittanceYearDescRemittanceMonthDescCreatedAtDesc(UUID organizationId);

    List<EpfRemittance> findByOrganizationIdAndRemittanceMonthAndRemittanceYear(UUID organizationId, Integer month, Integer year);

    Optional<EpfRemittance> findFirstByOrganizationIdAndRemittanceMonthAndRemittanceYear(UUID organizationId, Integer month, Integer year);
}
