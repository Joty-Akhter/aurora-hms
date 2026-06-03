package com.easyops.hr.service;

import com.easyops.hr.dto.EmployeeLoanDto;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * RE-02: employee self-service — own loans only (resolved via user → employee link).
 */
@Service
@RequiredArgsConstructor
public class LoanEmployeeSelfService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanService employeeLoanService;

    @Transactional(readOnly = true)
    public List<EmployeeLoanDto> listMyLoans(UUID organizationId, UUID actorUserId) {
        Employee employee = requireEmployeeForUser(organizationId, actorUserId);
        return employeeLoanService.listLoansForEmployeeSelf(organizationId, employee.getEmployeeId());
    }

    @Transactional(readOnly = true)
    public EmployeeLoanDto getMyLoan(UUID organizationId, UUID actorUserId, UUID loanId) {
        Employee employee = requireEmployeeForUser(organizationId, actorUserId);
        EmployeeLoanDto loan = employeeLoanService.getLoan(organizationId, loanId);
        if (!employee.getEmployeeId().equals(loan.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Loan does not belong to your employee profile");
        }
        return loan;
    }

    private Employee requireEmployeeForUser(UUID organizationId, UUID actorUserId) {
        return employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No employee profile linked to your user for this organization"));
    }
}
