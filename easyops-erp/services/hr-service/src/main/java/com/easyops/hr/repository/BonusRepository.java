package com.easyops.hr.repository;

import com.easyops.hr.entity.Bonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BonusRepository extends JpaRepository<Bonus, UUID> {

    List<Bonus> findByOrganizationId(UUID organizationId);

    List<Bonus> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);

    List<Bonus> findByOrganizationIdAndStatus(UUID organizationId, String status);

    /** HR-PY-03: Approved bonuses not yet linked to a payroll run, with paymentDate in [from, to]. */
    List<Bonus> findByOrganizationIdAndStatusAndPayrollRunIdIsNullAndPaymentDateBetween(
            UUID organizationId, String status, LocalDate from, LocalDate to);

    /** HR-PY-03: All bonuses linked to a specific payroll run. */
    List<Bonus> findByPayrollRunId(UUID payrollRunId);
}
