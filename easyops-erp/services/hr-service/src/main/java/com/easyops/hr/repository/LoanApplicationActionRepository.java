package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanApplicationAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanApplicationActionRepository extends JpaRepository<LoanApplicationAction, UUID> {

    List<LoanApplicationAction> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
