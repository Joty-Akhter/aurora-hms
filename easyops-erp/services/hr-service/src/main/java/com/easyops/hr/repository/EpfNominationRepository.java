package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfNomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfNominationRepository extends JpaRepository<EpfNomination, UUID> {
    
    List<EpfNomination> findByEpfAccountId(UUID epfAccountId);
    
    List<EpfNomination> findByEmployeeId(UUID employeeId);
    
    List<EpfNomination> findByEpfAccountIdAndIsActive(UUID epfAccountId, Boolean isActive);
    
    Optional<EpfNomination> findByEpfAccountIdAndIsPrimary(UUID epfAccountId, Boolean isPrimary);
    
    List<EpfNomination> findByOrganizationId(UUID organizationId);
}

