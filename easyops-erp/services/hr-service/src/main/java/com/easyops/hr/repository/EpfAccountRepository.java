package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfAccountRepository extends JpaRepository<EpfAccount, UUID> {
    
    Optional<EpfAccount> findByOrganizationIdAndEpfAccountNumber(UUID organizationId, String epfAccountNumber);
    
    List<EpfAccount> findByEmployeeId(UUID employeeId);
    
    List<EpfAccount> findByOrganizationId(UUID organizationId);
    
    List<EpfAccount> findByOrganizationIdAndAccountStatus(UUID organizationId, String accountStatus);
    
    List<EpfAccount> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
}

