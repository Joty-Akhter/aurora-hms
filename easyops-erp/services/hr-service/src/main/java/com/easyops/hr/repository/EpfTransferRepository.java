package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpfTransferRepository extends JpaRepository<EpfTransfer, UUID> {
    
    List<EpfTransfer> findBySourceEpfAccountId(UUID sourceEpfAccountId);
    
    List<EpfTransfer> findByTargetEpfAccountId(UUID targetEpfAccountId);
    
    List<EpfTransfer> findByEmployeeId(UUID employeeId);
    
    List<EpfTransfer> findByOrganizationId(UUID organizationId);
    
    List<EpfTransfer> findByStatus(String status);
}

