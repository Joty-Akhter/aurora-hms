package com.easyops.hr.repository;

import com.easyops.hr.entity.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, UUID> {
    
    List<PayrollComponent> findByPayrollDetailId(UUID payrollDetailId);

    /** ES-26: Payslip component order. */
    List<PayrollComponent> findByPayrollDetailIdOrderByDisplayOrderAsc(UUID payrollDetailId);

    /** SC-32: Count payroll result lines referencing this component (prevent delete if used in past payroll). */
    long countByComponentId(UUID componentId);

    void deleteByPayrollDetailId(UUID payrollDetailId);
}

