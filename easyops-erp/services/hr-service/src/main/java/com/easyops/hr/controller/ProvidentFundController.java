package com.easyops.hr.controller;

import com.easyops.hr.entity.*;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeService;
import com.easyops.hr.service.ProvidentFundFilingService;
import com.easyops.hr.service.ProvidentFundReconciliationService;
import com.easyops.hr.service.ProvidentFundRemittanceService;
import com.easyops.hr.service.ProvidentFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/provident-fund")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProvidentFundController {

    private final ProvidentFundService providentFundService;
    private final ProvidentFundFilingService providentFundFilingService;
    private final ProvidentFundRemittanceService providentFundRemittanceService;
    private final ProvidentFundReconciliationService providentFundReconciliationService;
    private final EmployeeService employeeService;
    private final HrRbacService hrRbac;

    // EPF Account endpoints
    @PostMapping("/accounts")
    public ResponseEntity<EpfAccount> createEpfAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfAccount epfAccount) {
        log.info("POST /provident-fund/accounts - Creating EPF account");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, epfAccount.getOrganizationId());
        EpfAccount created = providentFundService.createEpfAccount(epfAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<EpfAccount> getEpfAccountById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /provident-fund/accounts/{}", id);
        EpfAccount account = providentFundService.getEpfAccountById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts/employee/{employeeId}")
    public ResponseEntity<List<EpfAccount>> getEpfAccountsByEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID employeeId) {
        log.info("GET /provident-fund/accounts/employee/{}", employeeId);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, org);
        List<EpfAccount> accounts = providentFundService.getEpfAccountsByEmployee(employeeId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/organization/{organizationId}")
    public ResponseEntity<List<EpfAccount>> getEpfAccountsByOrganization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        log.info("GET /provident-fund/accounts/organization/{}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfAccount> accounts = providentFundService.getEpfAccountsByOrganization(organizationId);
        return ResponseEntity.ok(accounts);
    }

    // Alias method for test compatibility
    @GetMapping("/accounts")
    public ResponseEntity<List<EpfAccount>> getEpfAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/accounts?organizationId={}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfAccount> accounts = providentFundService.getEpfAccountsByOrganization(organizationId);
        return ResponseEntity.ok(accounts);
    }

    // EPF Contribution endpoints
    @PostMapping("/contributions")
    public ResponseEntity<EpfContribution> createContribution(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfContribution contribution) {
        log.info("POST /provident-fund/contributions - Creating contribution");
        EpfAccount account = providentFundService.getEpfAccountById(contribution.getEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, account.getOrganizationId());
        EpfContribution created = providentFundService.createContribution(contribution);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/contributions/account/{epfAccountId}")
    public ResponseEntity<List<EpfContribution>> getContributionsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId) {
        log.info("GET /provident-fund/contributions/account/{}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        List<EpfContribution> contributions = providentFundService.getContributionsByAccount(epfAccountId);
        return ResponseEntity.ok(contributions);
    }

    @GetMapping("/contributions/period")
    public ResponseEntity<List<EpfContribution>> getContributionsByPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/contributions/period - organizationId: {}, month: {}, year: {}",
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfContribution> contributions = providentFundService.getContributionsByPeriod(
                organizationId, month, year);
        return ResponseEntity.ok(contributions);
    }

    // EPF Interest Calculation endpoints
    @PostMapping("/interest/calculate")
    public ResponseEntity<EpfInterestCalculation> calculateInterest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID epfAccountId,
            @RequestParam Integer financialYear,
            @RequestParam BigDecimal interestRate) {
        log.info("POST /provident-fund/interest/calculate - account: {}, year: {}, rate: {}",
                epfAccountId, financialYear, interestRate);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, account.getOrganizationId());
        EpfInterestCalculation calculation = providentFundService.calculateInterestForFinancialYear(
                epfAccountId, financialYear, interestRate);
        return ResponseEntity.status(HttpStatus.CREATED).body(calculation);
    }

    @GetMapping("/interest/account/{epfAccountId}")
    public ResponseEntity<List<EpfInterestCalculation>> getInterestCalculationsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId) {
        log.info("GET /provident-fund/interest/account/{}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        List<EpfInterestCalculation> calculations = providentFundService.getInterestCalculationsByAccount(epfAccountId);
        return ResponseEntity.ok(calculations);
    }

    // EPF Withdrawal endpoints
    @PostMapping("/withdrawals")
    public ResponseEntity<EpfWithdrawal> createWithdrawalRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfWithdrawal withdrawal) {
        log.info("POST /provident-fund/withdrawals - Creating withdrawal request");
        EpfAccount account = providentFundService.getEpfAccountById(withdrawal.getEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, account.getOrganizationId());
        if (withdrawal.getCreatedBy() == null || withdrawal.getCreatedBy().isBlank()) {
            withdrawal.setCreatedBy(actor.toString());
        }
        withdrawal.setUpdatedBy(actor.toString());
        EpfWithdrawal created = providentFundService.createWithdrawalRequest(withdrawal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/withdrawals/{id}/approve")
    public ResponseEntity<EpfWithdrawal> approveWithdrawal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID approvedBy,
            @RequestParam(required = false) BigDecimal approvedAmount) {
        log.info("PUT /provident-fund/withdrawals/{}/approve", id);
        EpfWithdrawal withdrawal = providentFundService.getWithdrawalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfApprovalManage(actor, withdrawal.getOrganizationId());
        if (!actor.equals(approvedBy)) {
            throw new RuntimeException("approvedBy must match authenticated user");
        }
        EpfWithdrawal result = providentFundService.approveWithdrawal(id, actor, approvedAmount);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/withdrawals/{id}/review")
    public ResponseEntity<EpfWithdrawal> reviewWithdrawal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID reviewedBy,
            @RequestParam(defaultValue = "true") boolean approveForFinalApproval,
            @RequestParam(required = false) String comments) {
        EpfWithdrawal withdrawal = providentFundService.getWithdrawalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfApprovalManage(actor, withdrawal.getOrganizationId());
        if (!actor.equals(reviewedBy)) {
            throw new RuntimeException("reviewedBy must match authenticated user");
        }
        EpfWithdrawal result = providentFundService.reviewWithdrawal(id, actor, approveForFinalApproval, comments);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/withdrawals/{id}/reject")
    public ResponseEntity<EpfWithdrawal> rejectWithdrawal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID rejectedBy,
            @RequestParam(required = false) String reason) {
        EpfWithdrawal withdrawal = providentFundService.getWithdrawalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfApprovalManage(actor, withdrawal.getOrganizationId());
        if (!actor.equals(rejectedBy)) {
            throw new RuntimeException("rejectedBy must match authenticated user");
        }
        EpfWithdrawal result = providentFundService.rejectWithdrawal(id, actor, reason);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/withdrawals/{id}/process")
    public ResponseEntity<EpfWithdrawal> processWithdrawal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam String paymentReference) {
        log.info("PUT /provident-fund/withdrawals/{}/process", id);
        EpfWithdrawal withdrawal = providentFundService.getWithdrawalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfApprovalManage(actor, withdrawal.getOrganizationId());
        EpfWithdrawal result = providentFundService.processWithdrawal(id, paymentReference);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/withdrawals/account/{epfAccountId}")
    public ResponseEntity<List<EpfWithdrawal>> getWithdrawalsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId) {
        log.info("GET /provident-fund/withdrawals/account/{}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        List<EpfWithdrawal> withdrawals = providentFundService.getWithdrawalsByAccount(epfAccountId);
        return ResponseEntity.ok(withdrawals);
    }

    // EPF Transfer endpoints
    @PostMapping("/transfers")
    public ResponseEntity<EpfTransfer> createTransferRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfTransfer transfer) {
        log.info("POST /provident-fund/transfers - Creating transfer request");
        EpfAccount source = providentFundService.getEpfAccountById(transfer.getSourceEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, source.getOrganizationId());
        EpfTransfer created = providentFundService.createTransferRequest(transfer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/transfers/{id}/process")
    public ResponseEntity<EpfTransfer> processTransfer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("PUT /provident-fund/transfers/{}/process", id);
        EpfTransfer transfer = providentFundService.getTransferById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, transfer.getOrganizationId());
        EpfTransfer result = providentFundService.processTransfer(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/transfers/account/{epfAccountId}")
    public ResponseEntity<List<EpfTransfer>> getTransfersByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId) {
        log.info("GET /provident-fund/transfers/account/{}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        List<EpfTransfer> transfers = providentFundService.getTransfersByAccount(epfAccountId);
        return ResponseEntity.ok(transfers);
    }

    // EPF Nomination endpoints
    @PostMapping("/nominations")
    public ResponseEntity<EpfNomination> createNomination(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfNomination nomination) {
        log.info("POST /provident-fund/nominations - Creating nomination");
        EpfAccount account = providentFundService.getEpfAccountById(nomination.getEpfAccountId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, account.getOrganizationId());
        EpfNomination created = providentFundService.createNomination(nomination);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/nominations/account/{epfAccountId}")
    public ResponseEntity<List<EpfNomination>> getNominationsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId) {
        log.info("GET /provident-fund/nominations/account/{}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        List<EpfNomination> nominations = providentFundService.getNominationsByAccount(epfAccountId);
        return ResponseEntity.ok(nominations);
    }

    @PutMapping("/nominations/{id}")
    public ResponseEntity<EpfNomination> updateNomination(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody EpfNomination nomination) {
        log.info("PUT /provident-fund/nominations/{}", id);
        EpfNomination existing = providentFundService.getNominationById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        EpfNomination updated = providentFundService.updateNomination(id, nomination);
        return ResponseEntity.ok(updated);
    }

    // EPF Statement endpoints
    @GetMapping("/statements/account/{epfAccountId}")
    public ResponseEntity<Map<String, Object>> generateAccountStatement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID epfAccountId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        log.info("GET /provident-fund/statements/account/{} - from {} to {}", epfAccountId, fromDate, toDate);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, account.getOrganizationId());
        Map<String, Object> statement = providentFundService.generateAccountStatement(epfAccountId, fromDate, toDate);
        return ResponseEntity.ok(statement);
    }

    // EPF Compliance endpoints
    @PostMapping("/compliance")
    public ResponseEntity<EpfComplianceRecord> createComplianceRecord(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfComplianceRecord record) {
        log.info("POST /provident-fund/compliance - Creating compliance record");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfComplianceManage(actor, record.getOrganizationId());
        EpfComplianceRecord created = providentFundService.createComplianceRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/compliance/{id}")
    public ResponseEntity<EpfComplianceRecord> updateComplianceRecord(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody EpfComplianceRecord record) {
        log.info("PUT /provident-fund/compliance/{}", id);
        EpfComplianceRecord existing = providentFundService.getComplianceRecordById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfComplianceManage(actor, existing.getOrganizationId());
        EpfComplianceRecord updated = providentFundService.updateComplianceRecord(id, record);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/compliance/organization/{organizationId}")
    public ResponseEntity<List<EpfComplianceRecord>> getComplianceRecordsByOrganization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        log.info("GET /provident-fund/compliance/organization/{}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfComplianceRecord> records = providentFundService.getComplianceRecordsByOrganization(organizationId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/compliance/organization/{organizationId}/overdue")
    public ResponseEntity<List<EpfComplianceRecord>> getOverdueComplianceRecords(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        log.info("GET /provident-fund/compliance/organization/{}/overdue", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfComplianceRecord> records = providentFundService.getOverdueComplianceRecords(organizationId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/compliance/statutory-contribution")
    public ResponseEntity<BigDecimal> calculateStatutoryContribution(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/compliance/statutory-contribution - organizationId: {}, period: {}/{}",
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        BigDecimal amount = providentFundService.calculateStatutoryContribution(organizationId, month, year);
        return ResponseEntity.ok(amount);
    }

    // EPF ECR / Challan filing workflow endpoints
    @PostMapping("/filings/ecr/generate")
    public ResponseEntity<EpfFiling> generateEcr(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /provident-fund/filings/ecr/generate - organizationId: {}, period: {}/{}", organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfFilingManage(actor, organizationId);
        EpfFiling filing = providentFundFilingService.generateEcr(organizationId, month, year, actor.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }

    @PostMapping("/filings/challan/generate")
    public ResponseEntity<EpfFiling> generateChallan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /provident-fund/filings/challan/generate - organizationId: {}, period: {}/{}", organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfFilingManage(actor, organizationId);
        EpfFiling filing = providentFundFilingService.generateChallan(organizationId, month, year, actor.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }

    @PutMapping("/filings/{filingId}/submit")
    public ResponseEntity<EpfFiling> submitFiling(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID filingId,
            @RequestParam String submissionReference,
            @RequestParam(required = false) LocalDate submissionDate) {
        EpfFiling existing = providentFundFilingService.getFilingById(filingId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfFilingManage(actor, existing.getOrganizationId());
        EpfFiling filing = providentFundFilingService.submitFiling(
                filingId, submissionReference, submissionDate, actor.toString());
        return ResponseEntity.ok(filing);
    }

    @PutMapping("/filings/{filingId}/verify")
    public ResponseEntity<EpfFiling> verifyFiling(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID filingId) {
        EpfFiling existing = providentFundFilingService.getFilingById(filingId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfFilingManage(actor, existing.getOrganizationId());
        EpfFiling filing = providentFundFilingService.verifyFiling(filingId, actor.toString());
        return ResponseEntity.ok(filing);
    }

    @GetMapping("/filings/organization/{organizationId}")
    public ResponseEntity<List<EpfFiling>> getFilingsByOrganization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EpfFiling> filings = providentFundFilingService.getFilingsByOrganization(organizationId, month, year);
        return ResponseEntity.ok(filings);
    }

    @GetMapping("/filings/{filingId}/artifact")
    public ResponseEntity<Map<String, Object>> getFilingArtifact(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID filingId) {
        EpfFiling filing = providentFundFilingService.getFilingById(filingId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, filing.getOrganizationId());
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("filingId", filing.getFilingId());
        payload.put("filingType", filing.getFilingType());
        payload.put("artifactFormat", filing.getArtifactFormat());
        payload.put("artifactChecksum", filing.getArtifactChecksum());
        payload.put("artifactContent", filing.getArtifactContent());
        return ResponseEntity.ok(payload);
    }

    // EPF remittance/payment tracking endpoints
    @GetMapping("/remittances/organization/{organizationId}")
    public ResponseEntity<List<EpfRemittance>> getRemittances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(providentFundRemittanceService.list(organizationId, month, year));
    }

    @GetMapping("/remittances/summary")
    public ResponseEntity<Map<String, Object>> getRemittanceSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(providentFundRemittanceService.getPeriodRemittanceSummary(organizationId, month, year));
    }

    @PutMapping("/remittances/{remittanceId}/mark-paid")
    public ResponseEntity<EpfRemittance> markRemittancePaid(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID remittanceId,
            @RequestBody Map<String, Object> request) {
        EpfRemittance existing = providentFundRemittanceService.getById(remittanceId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfRemittanceManage(actor, existing.getOrganizationId());

        BigDecimal amountPaid = request != null && request.get("amountPaid") != null
                ? new BigDecimal(request.get("amountPaid").toString())
                : existing.getLiabilityAmount();
        LocalDate paymentDate = request != null && request.get("paymentDate") != null
                ? LocalDate.parse(request.get("paymentDate").toString())
                : LocalDate.now();
        String paymentReference = request != null ? (String) request.get("paymentReference") : null;
        String paymentChannel = request != null ? (String) request.get("paymentChannel") : null;
        String notes = request != null ? (String) request.get("notes") : null;

        EpfRemittance remittance = providentFundRemittanceService.markRemittancePaid(
                remittanceId, amountPaid, paymentDate, paymentReference, paymentChannel, notes, actor.toString());
        return ResponseEntity.ok(remittance);
    }

    @PutMapping("/remittances/{remittanceId}/mark-failed")
    public ResponseEntity<EpfRemittance> markRemittanceFailed(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID remittanceId,
            @RequestBody(required = false) Map<String, Object> request) {
        EpfRemittance existing = providentFundRemittanceService.getById(remittanceId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfRemittanceManage(actor, existing.getOrganizationId());
        String reason = request != null && request.get("reason") != null ? request.get("reason").toString() : "payment_failed";
        return ResponseEntity.ok(providentFundRemittanceService.markRemittanceFailed(remittanceId, reason, actor.toString()));
    }

    @GetMapping("/reconciliation/period")
    public ResponseEntity<Map<String, Object>> reconcilePeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) BigDecimal tolerance) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> result = tolerance != null
                ? providentFundReconciliationService.reconcilePeriod(organizationId, month, year, tolerance)
                : providentFundReconciliationService.reconcilePeriod(organizationId, month, year);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/corrections/contributions/{contributionId}/reverse")
    public ResponseEntity<EpfContribution> reverseContribution(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID contributionId,
            @RequestBody Map<String, Object> request) {
        EpfContribution contribution = providentFundService.getContributionById(contributionId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfCorrectionManage(actor, contribution.getOrganizationId());
        String reason = request != null && request.get("reason") != null ? request.get("reason").toString() : null;
        EpfContribution reversal = providentFundService.reverseContribution(contributionId, reason, actor.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(reversal);
    }

    @PostMapping("/corrections/withdrawals/{withdrawalId}/reverse")
    public ResponseEntity<EpfWithdrawal> reverseWithdrawal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID withdrawalId,
            @RequestBody Map<String, Object> request) {
        EpfWithdrawal withdrawal = providentFundService.getWithdrawalById(withdrawalId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfCorrectionManage(actor, withdrawal.getOrganizationId());
        String reason = request != null && request.get("reason") != null ? request.get("reason").toString() : null;
        EpfWithdrawal reversed = providentFundService.reverseProcessedWithdrawal(withdrawalId, reason, actor.toString());
        return ResponseEntity.ok(reversed);
    }

    @PostMapping("/corrections/transfers/{transferId}/reverse")
    public ResponseEntity<EpfTransfer> reverseTransfer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID transferId,
            @RequestBody Map<String, Object> request) {
        EpfTransfer transfer = providentFundService.getTransferById(transferId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfCorrectionManage(actor, transfer.getOrganizationId());
        String reason = request != null && request.get("reason") != null ? request.get("reason").toString() : null;
        EpfTransfer reversed = providentFundService.reverseProcessedTransfer(transferId, reason, actor.toString());
        return ResponseEntity.ok(reversed);
    }

    @GetMapping("/corrections/organization/{organizationId}")
    public ResponseEntity<List<EpfCorrectionLog>> getCorrectionLogs(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(providentFundService.getCorrectionLogs(organizationId));
    }

    @GetMapping("/audit/organization/{organizationId}")
    public ResponseEntity<List<EpfAuditEvent>> getAuditEvents(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(providentFundService.getAuditEvents(organizationId));
    }

    @PostMapping("/exit-cases/initiate")
    public ResponseEntity<EpfExitCase> initiateExitCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID employeeId,
            @RequestParam String exitType,
            @RequestParam(required = false) UUID targetEpfAccountId,
            @RequestParam(required = false) String notes) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        EpfExitCase result = providentFundService.initiateExitCase(
                organizationId, employeeId, exitType, targetEpfAccountId, notes, actor.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/exit-cases/{exitCaseId}/complete")
    public ResponseEntity<EpfExitCase> completeExitCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID exitCaseId,
            @RequestParam(required = false) String completionReference) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        EpfExitCase existing = providentFundService.getExitCaseById(exitCaseId);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        EpfExitCase result = providentFundService.completeExitCase(exitCaseId, completionReference, actor.toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/exit-cases/organization/{organizationId}")
    public ResponseEntity<List<EpfExitCase>> listExitCases(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(providentFundService.getExitCasesByOrganization(organizationId));
    }
}
