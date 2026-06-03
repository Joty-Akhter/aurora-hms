package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanRepaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepaymentAllocationRepository extends JpaRepository<LoanRepaymentAllocation, UUID> {

    List<LoanRepaymentAllocation> findByTransactionIdOrderByAllocationId(UUID transactionId);
}
