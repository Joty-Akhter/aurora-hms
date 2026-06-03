package com.easyops.hr.controller;

import com.easyops.hr.entity.*;
import com.easyops.hr.security.HrEmployeeSelfAccessService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeService;
import com.easyops.hr.service.ProvidentFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/provident-fund/employee")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProvidentFundEmployeeController {
    
    private final ProvidentFundService providentFundService;
    private final EmployeeService employeeService;
    private final HrEmployeeSelfAccessService hrEmployeeSelfAccessService;
    
    /**
     * Employee Self-Service: Get own EPF account
     */
    @GetMapping("/account")
    public ResponseEntity<List<EpfAccount>> getMyEpfAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId) {
        log.info("GET /provident-fund/employee/account - employeeId: {}", employeeId);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, org, employeeId);
        List<EpfAccount> accounts = providentFundService.getEpfAccountsByEmployee(employeeId);
        return ResponseEntity.ok(accounts);
    }
    
    /**
     * Employee Self-Service: View contribution history
     */
    @GetMapping("/contributions")
    public ResponseEntity<List<EpfContribution>> getMyContributions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam(required = false) UUID epfAccountId) {
        log.info("GET /provident-fund/employee/contributions - employeeId: {}", employeeId);
        
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        List<EpfContribution> contributions;
        if (epfAccountId != null) {
            EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
            hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());
            contributions = providentFundService.getContributionsByAccount(epfAccountId);
        } else {
            UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
            hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, org, employeeId);
            List<EpfAccount> accounts = providentFundService.getEpfAccountsByEmployee(employeeId);
            if (accounts.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            contributions = providentFundService.getContributionsByAccount(accounts.get(0).getEpfAccountId());
        }
        
        return ResponseEntity.ok(contributions);
    }
    
    /**
     * Employee Self-Service: Submit withdrawal request
     */
    @PostMapping("/withdrawals")
    public ResponseEntity<EpfWithdrawal> submitWithdrawalRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfWithdrawal withdrawal) {
        log.info("POST /provident-fund/employee/withdrawals - Submitting withdrawal request");
        EpfAccount account = providentFundService.getEpfAccountById(withdrawal.getEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!withdrawal.getEmployeeId().equals(account.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "withdrawal_employee_mismatch");
        }
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrManage(actor, account.getOrganizationId(), withdrawal.getEmployeeId());
        EpfWithdrawal created = providentFundService.createWithdrawalRequest(withdrawal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Employee Self-Service: Get withdrawal requests
     */
    @GetMapping("/withdrawals")
    public ResponseEntity<List<EpfWithdrawal>> getMyWithdrawals(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId) {
        log.info("GET /provident-fund/employee/withdrawals - employeeId: {}", employeeId);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, org, employeeId);
        List<EpfWithdrawal> withdrawals = providentFundService.getWithdrawalsByEmployee(employeeId);
        return ResponseEntity.ok(withdrawals);
    }
    
    /**
     * Employee Self-Service: Download account statement
     */
    @GetMapping("/statements")
    public ResponseEntity<Map<String, Object>> downloadStatement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID epfAccountId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("GET /provident-fund/employee/statements - employeeId: {}, accountId: {}", 
                employeeId, epfAccountId);
        
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!employeeId.equals(account.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statement_employee_mismatch");
        }
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());

        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<String, Object> statement = providentFundService.generateAccountStatement(epfAccountId, startDate, endDate);
        
        return ResponseEntity.ok(statement);
    }
    
    /**
     * Employee Self-Service: View nominations
     */
    @GetMapping("/nominations")
    public ResponseEntity<List<EpfNomination>> getMyNominations(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID epfAccountId) {
        log.info("GET /provident-fund/employee/nominations - employeeId: {}, accountId: {}", 
                employeeId, epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());
        List<EpfNomination> nominations = providentFundService.getNominationsByAccount(epfAccountId);
        return ResponseEntity.ok(nominations);
    }
    
    /**
     * Employee Self-Service: Create/Update nomination
     */
    @PostMapping("/nominations")
    public ResponseEntity<EpfNomination> createNomination(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfNomination nomination) {
        log.info("POST /provident-fund/employee/nominations - Creating nomination");
        EpfAccount account = providentFundService.getEpfAccountById(nomination.getEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!nomination.getEmployeeId().equals(account.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nomination_employee_mismatch");
        }
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrManage(actor, account.getOrganizationId(), nomination.getEmployeeId());
        EpfNomination created = providentFundService.createNomination(nomination);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/nominations/{id}")
    public ResponseEntity<EpfNomination> updateNomination(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody EpfNomination nomination) {
        log.info("PUT /provident-fund/employee/nominations/{}", id);
        EpfNomination existing = providentFundService.getNominationById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrManage(actor, existing.getOrganizationId(), existing.getEmployeeId());
        EpfNomination updated = providentFundService.updateNomination(id, nomination);
        return ResponseEntity.ok(updated);
    }
}
