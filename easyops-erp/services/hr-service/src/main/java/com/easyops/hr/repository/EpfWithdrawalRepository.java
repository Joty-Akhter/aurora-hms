package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpfWithdrawalRepository extends JpaRepository<EpfWithdrawal, UUID> {
    
    List<EpfWithdrawal> findByEpfAccountId(UUID epfAccountId);
    
    List<EpfWithdrawal> findByEmployeeId(UUID employeeId);
    
    List<EpfWithdrawal> findByOrganizationId(UUID organizationId);
    
    List<EpfWithdrawal> findByStatus(String status);
    
    List<EpfWithdrawal> findByOrganizationIdAndStatus(UUID organizationId, String status);
}

