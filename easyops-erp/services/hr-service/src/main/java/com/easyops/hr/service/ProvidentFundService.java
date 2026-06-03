package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProvidentFundService {

    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfInterestCalculationRepository epfInterestCalculationRepository;
    private final EpfWithdrawalRepository epfWithdrawalRepository;
    private final EpfTransferRepository epfTransferRepository;
    private final EpfNominationRepository epfNominationRepository;
    private final EpfComplianceRecordRepository epfComplianceRecordRepository;
    private final EpfCorrectionLogRepository epfCorrectionLogRepository;
    private final EpfAuditEventRepository epfAuditEventRepository;
    private final EpfExitCaseRepository epfExitCaseRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    // EPF Account Management
    public EpfAccount createEpfAccount(EpfAccount epfAccount) {
        log.info("Creating EPF account for employee: {}", epfAccount.getEmployeeId());

        epfAccountRepository.findByOrganizationIdAndEpfAccountNumber(
                epfAccount.getOrganizationId(), epfAccount.getEpfAccountNumber())
                .ifPresent(account -> {
                    throw new RuntimeException("EPF account number already exists: " +
                            epfAccount.getEpfAccountNumber());
                });

        return epfAccountRepository.save(epfAccount);
    }

    public EpfAccount getEpfAccountById(UUID epfAccountId) {
        log.debug("Fetching EPF account by ID: {}", epfAccountId);
        return epfAccountRepository.findById(epfAccountId)
                .orElseThrow(() -> new RuntimeException("EPF account not found with ID: " + epfAccountId));
    }

    public EpfWithdrawal getWithdrawalById(UUID withdrawalId) {
        return epfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found: " + withdrawalId));
    }

    public EpfTransfer getTransferById(UUID transferId) {
        return epfTransferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
    }

    public EpfNomination getNominationById(UUID nominationId) {
        return epfNominationRepository.findById(nominationId)
                .orElseThrow(() -> new RuntimeException("Nomination not found: " + nominationId));
    }

    public List<EpfAccount> getEpfAccountsByEmployee(UUID employeeId) {
        log.debug("Fetching EPF accounts for employee: {}", employeeId);
        return epfAccountRepository.findByEmployeeId(employeeId);
    }

    public List<EpfAccount> getEpfAccountsByOrganization(UUID organizationId) {
        log.debug("Fetching EPF accounts for organization: {}", organizationId);
        return epfAccountRepository.findByOrganizationId(organizationId);
    }

    // EPF Contribution Management
    public EpfContribution createContribution(EpfContribution contribution) {
        log.info("Creating EPF contribution for account: {}", contribution.getEpfAccountId());

        epfContributionRepository.findByEpfAccountIdAndContributionMonthAndContributionYear(
                contribution.getEpfAccountId(),
                contribution.getContributionMonth(),
                contribution.getContributionYear())
                .ifPresent(c -> {
                    throw new RuntimeException("Contribution already exists for period: " +
                            contribution.getContributionMonth() + "/" + contribution.getContributionYear());
                });

        if (contribution.getEmployeeContributionAmount() == null) {
            BigDecimal employeeAmount = contribution.getEmployeeBasicSalary()
                    .multiply(contribution.getEmployeeContributionRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            contribution.setEmployeeContributionAmount(employeeAmount);
        }

        if (contribution.getEmployerContributionAmount() == null) {
            BigDecimal employerAmount = contribution.getEmployeeBasicSalary()
                    .multiply(contribution.getEmployerContributionRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            contribution.setEmployerContributionAmount(employerAmount);
        }

        contribution.setTotalContribution(
                contribution.getEmployeeContributionAmount()
                        .add(contribution.getEmployerContributionAmount()));

        EpfContribution savedContribution = epfContributionRepository.save(contribution);

        updateEpfAccountBalance(contribution.getEpfAccountId());
        appendAuditEvent(
                contribution.getOrganizationId(),
                contribution.getEpfAccountId(),
                contribution.getEmployeeId(),
                "contribution",
                savedContribution.getContributionId(),
                "contribution_created",
                contribution.getCreatedBy(),
                Map.of(
                        "contributionMonth", contribution.getContributionMonth(),
                        "contributionYear", contribution.getContributionYear(),
                        "totalContribution", nvl(contribution.getTotalContribution())
                )
        );

        return savedContribution;
    }

    public List<EpfContribution> getContributionsByAccount(UUID epfAccountId) {
        log.debug("Fetching contributions for EPF account: {}", epfAccountId);
        return epfContributionRepository.findByEpfAccountId(epfAccountId);
    }

    public List<EpfContribution> getContributionsByPeriod(UUID organizationId, Integer month, Integer year) {
        log.debug("Fetching contributions for period: {}/{}", month, year);
        return epfContributionRepository.findByOrganizationAndPeriod(organizationId, month, year);
    }

    public EpfContribution getContributionById(UUID contributionId) {
        return epfContributionRepository.findById(contributionId)
                .orElseThrow(() -> new RuntimeException("Contribution not found: " + contributionId));
    }

    /** Recalculate and update EPF account balance from contributions. Called after create/update contribution. */
    public void updateEpfAccountBalanceFromContributions(UUID epfAccountId) {
        updateEpfAccountBalance(epfAccountId);
    }

    private void updateEpfAccountBalance(UUID epfAccountId) {
        EpfAccount account = getEpfAccountById(epfAccountId);

        BigDecimal employeeTotal = epfContributionRepository.findByEpfAccountId(epfAccountId).stream()
                .map(EpfContribution::getEmployeeContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal employerTotal = epfContributionRepository.findByEpfAccountId(epfAccountId).stream()
                .map(EpfContribution::getEmployerContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        account.setEmployeeContributionBalance(employeeTotal);
        account.setEmployerContributionBalance(employerTotal);
        account.setCurrentBalance(employeeTotal.add(employerTotal));
        account.setLastContributionDate(LocalDate.now());

        epfAccountRepository.save(account);
    }

    // =====================================================
    // INTEREST CALCULATION ENGINE
    // =====================================================

    public EpfInterestCalculation calculateInterestForFinancialYear(UUID epfAccountId, Integer financialYear,
                                                                    BigDecimal interestRate) {
        log.info("Calculating EPF interest for account: {}, financial year: {}", epfAccountId, financialYear);

        EpfAccount account = getEpfAccountById(epfAccountId);

        epfInterestCalculationRepository.findByEpfAccountIdAndFinancialYear(epfAccountId, financialYear)
                .ifPresent(calc -> {
                    throw new RuntimeException("Interest already calculated for financial year: " + financialYear);
                });

        BigDecimal openingBalance = getOpeningBalanceForYear(epfAccountId, financialYear);

        LocalDate fyStart = LocalDate.of(financialYear, 4, 1);
        LocalDate fyEnd = LocalDate.of(financialYear + 1, 3, 31);

        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(epfAccountId).stream()
                .filter(c -> {
                    LocalDate contribDate = LocalDate.of(c.getContributionYear(), c.getContributionMonth(), 1);
                    return !contribDate.isBefore(fyStart) && !contribDate.isAfter(fyEnd);
                })
                .toList();

        BigDecimal totalContributions = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestAmount = calculateMonthlyInterest(openingBalance, contributions, interestRate);

        BigDecimal closingBalance = openingBalance.add(totalContributions).add(interestAmount);

        EpfInterestCalculation interestCalc = EpfInterestCalculation.builder()
                .epfAccountId(epfAccountId)
                .organizationId(account.getOrganizationId())
                .financialYear(financialYear)
                .interestRate(interestRate)
                .openingBalance(openingBalance)
                .totalContributions(totalContributions)
                .interestAmount(interestAmount)
                .closingBalance(closingBalance)
                .calculationDate(LocalDate.now())
                .status("calculated")
                .build();

        EpfInterestCalculation saved = epfInterestCalculationRepository.save(interestCalc);

        account.setInterestBalance(account.getInterestBalance().add(interestAmount));
        account.setCurrentBalance(account.getCurrentBalance().add(interestAmount));
        account.setLastInterestCalculationDate(LocalDate.now());
        epfAccountRepository.save(account);

        return saved;
    }

    private BigDecimal getOpeningBalanceForYear(UUID epfAccountId, Integer financialYear) {
        return epfInterestCalculationRepository.findByEpfAccountIdAndFinancialYear(epfAccountId, financialYear - 1)
                .map(EpfInterestCalculation::getClosingBalance)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateMonthlyInterest(BigDecimal openingBalance, List<EpfContribution> contributions,
                                                BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);

        BigDecimal runningBalance = openingBalance;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (EpfContribution contrib : contributions) {
            BigDecimal monthlyInterest = runningBalance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            totalInterest = totalInterest.add(monthlyInterest);
            runningBalance = runningBalance.add(contrib.getTotalContribution());
        }

        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    public List<EpfInterestCalculation> getInterestCalculationsByAccount(UUID epfAccountId) {
        log.debug("Fetching interest calculations for EPF account: {}", epfAccountId);
        return epfInterestCalculationRepository.findByEpfAccountId(epfAccountId);
    }

    // Overload method that accepts String financial year (e.g., "2024-2025")
    public EpfInterestCalculation calculateInterest(UUID epfAccountId, String financialYear, BigDecimal interestRate) {
        Integer year = Integer.parseInt(financialYear.split("-")[0]);
        return calculateInterestForFinancialYear(epfAccountId, year, interestRate);
    }

    // =====================================================
    // WITHDRAWAL REQUEST MANAGEMENT
    // =====================================================

    public EpfWithdrawal createWithdrawalRequest(EpfWithdrawal withdrawal) {
        log.info("Creating EPF withdrawal request for account: {}", withdrawal.getEpfAccountId());

        EpfAccount account = getEpfAccountById(withdrawal.getEpfAccountId());

        if (withdrawal.getRequestedAmount().compareTo(account.getCurrentBalance()) > 0) {
            throw new RuntimeException("Withdrawal amount exceeds available balance");
        }

        validateWithdrawalRules(account, withdrawal);

        withdrawal.setStatus("submitted");
        if (withdrawal.getRequestDate() == null) {
            withdrawal.setRequestDate(LocalDate.now());
        }
        withdrawal.setApprovalWorkflow(buildApprovalWorkflowJson("submitted", null, null, null));
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getEpfAccountId(),
                saved.getEmployeeId(),
                "withdrawal",
                saved.getWithdrawalId(),
                "withdrawal_submitted",
                saved.getCreatedBy(),
                Map.of("requestedAmount", nvl(saved.getRequestedAmount()), "withdrawalType", saved.getWithdrawalType() != null ? saved.getWithdrawalType() : "")
        );
        return saved;
    }

    private void validateWithdrawalRules(EpfAccount account, EpfWithdrawal withdrawal) {
        if ("full".equals(withdrawal.getWithdrawalType())) {
            // Full withdrawal only allowed if employee is terminated
            // This would need employee status check
        }
    }

    public EpfWithdrawal approveWithdrawal(UUID withdrawalId, UUID approvedBy, BigDecimal approvedAmount) {
        log.info("Approving EPF withdrawal: {}", withdrawalId);

        EpfWithdrawal withdrawal = epfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found: " + withdrawalId));

        if (!"under_finance_approval".equals(withdrawal.getStatus())) {
            throw new RuntimeException("Withdrawal cannot be approved. Current status: " + withdrawal.getStatus());
        }
        if (withdrawal.getCreatedBy() != null && withdrawal.getCreatedBy().equalsIgnoreCase(approvedBy.toString())) {
            throw new RuntimeException("Maker-checker violation: creator cannot perform final approval");
        }
        String reviewedBy = extractWorkflowActor(withdrawal.getApprovalWorkflow(), "reviewedBy");
        if (reviewedBy != null && reviewedBy.equalsIgnoreCase(approvedBy.toString())) {
            throw new RuntimeException("Maker-checker violation: reviewer cannot perform final approval");
        }

        withdrawal.setStatus("approved");
        withdrawal.setApprovedAmount(approvedAmount != null ? approvedAmount : withdrawal.getRequestedAmount());
        withdrawal.setUpdatedBy(approvedBy.toString());
        withdrawal.setApprovalWorkflow(buildApprovalWorkflowJson("approved", null, approvedBy, null));
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getEpfAccountId(),
                saved.getEmployeeId(),
                "withdrawal",
                saved.getWithdrawalId(),
                "withdrawal_approved",
                approvedBy != null ? approvedBy.toString() : null,
                Map.of("approvedAmount", nvl(saved.getApprovedAmount()), "status", saved.getStatus())
        );
        return saved;
    }

    public EpfWithdrawal reviewWithdrawal(UUID withdrawalId, UUID reviewedBy, boolean approvedForFinalApproval, String comments) {
        EpfWithdrawal withdrawal = epfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found: " + withdrawalId));
        if (!"submitted".equals(withdrawal.getStatus())) {
            throw new RuntimeException("Only submitted withdrawals can be reviewed. Current status: " + withdrawal.getStatus());
        }
        if (withdrawal.getCreatedBy() != null && withdrawal.getCreatedBy().equalsIgnoreCase(reviewedBy.toString())) {
            throw new RuntimeException("Maker-checker violation: creator cannot perform review");
        }
        if (approvedForFinalApproval) {
            withdrawal.setStatus("under_finance_approval");
            withdrawal.setApprovalWorkflow(buildApprovalWorkflowJson("under_finance_approval", reviewedBy, null, comments));
        } else {
            withdrawal.setStatus("rejected");
            withdrawal.setApprovalWorkflow(buildApprovalWorkflowJson("rejected", reviewedBy, null, comments));
        }
        withdrawal.setUpdatedBy(reviewedBy.toString());
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getEpfAccountId(),
                saved.getEmployeeId(),
                "withdrawal",
                saved.getWithdrawalId(),
                approvedForFinalApproval ? "withdrawal_reviewed_forwarded" : "withdrawal_reviewed_rejected",
                reviewedBy != null ? reviewedBy.toString() : null,
                Map.of("comments", comments != null ? comments : "", "status", saved.getStatus())
        );
        return saved;
    }

    public EpfWithdrawal rejectWithdrawal(UUID withdrawalId, UUID rejectedBy, String reason) {
        EpfWithdrawal withdrawal = epfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found: " + withdrawalId));
        if (!"submitted".equals(withdrawal.getStatus()) && !"under_finance_approval".equals(withdrawal.getStatus())
                && !"approved".equals(withdrawal.getStatus())) {
            throw new RuntimeException("Withdrawal cannot be rejected in current status: " + withdrawal.getStatus());
        }
        if (withdrawal.getCreatedBy() != null && withdrawal.getCreatedBy().equalsIgnoreCase(rejectedBy.toString())) {
            throw new RuntimeException("Maker-checker violation: creator cannot reject own request");
        }
        withdrawal.setStatus("rejected");
        withdrawal.setUpdatedBy(rejectedBy.toString());
        withdrawal.setNotes(appendNote(withdrawal.getNotes(), reason != null ? reason : "rejected"));
        withdrawal.setApprovalWorkflow(buildApprovalWorkflowJson("rejected", rejectedBy, null, reason));
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getEpfAccountId(),
                saved.getEmployeeId(),
                "withdrawal",
                saved.getWithdrawalId(),
                "withdrawal_rejected",
                rejectedBy != null ? rejectedBy.toString() : null,
                Map.of("reason", reason != null ? reason : "", "status", saved.getStatus() != null ? saved.getStatus() : "")
        );
        return saved;
    }

    // Overload method that only requires withdrawalId and approvedBy
    public EpfWithdrawal approveWithdrawalRequest(UUID withdrawalId, UUID approvedBy) {
        return approveWithdrawal(withdrawalId, approvedBy, null);
    }

    public EpfWithdrawal processWithdrawal(UUID withdrawalId, String paymentReference) {
        log.info("Processing EPF withdrawal: {}", withdrawalId);

        EpfWithdrawal withdrawal = epfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found: " + withdrawalId));

        if (!"approved".equals(withdrawal.getStatus())) {
            throw new RuntimeException("Withdrawal must be approved before processing");
        }

        EpfAccount account = getEpfAccountById(withdrawal.getEpfAccountId());
        BigDecimal withdrawalAmount = withdrawal.getApprovedAmount();

        account.setCurrentBalance(account.getCurrentBalance().subtract(withdrawalAmount));
        epfAccountRepository.save(account);

        withdrawal.setStatus("processed");
        withdrawal.setProcessedDate(LocalDate.now());
        withdrawal.setPaymentReference(paymentReference);
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getEpfAccountId(),
                saved.getEmployeeId(),
                "withdrawal",
                saved.getWithdrawalId(),
                "withdrawal_processed",
                saved.getUpdatedBy(),
                Map.of("approvedAmount", nvl(saved.getApprovedAmount()), "paymentReference", paymentReference != null ? paymentReference : "")
        );
        return saved;
    }

    public List<EpfWithdrawal> getWithdrawalsByAccount(UUID epfAccountId) {
        log.debug("Fetching withdrawals for EPF account: {}", epfAccountId);
        return epfWithdrawalRepository.findByEpfAccountId(epfAccountId);
    }

    public List<EpfWithdrawal> getWithdrawalsByEmployee(UUID employeeId) {
        log.debug("Fetching withdrawals for employee: {}", employeeId);
        return epfWithdrawalRepository.findByEmployeeId(employeeId);
    }

    // =====================================================
    // TRANSFER PROCESSING
    // =====================================================

    public EpfTransfer createTransferRequest(EpfTransfer transfer) {
        log.info("Creating EPF transfer request from account: {}", transfer.getSourceEpfAccountId());

        EpfAccount sourceAccount = getEpfAccountById(transfer.getSourceEpfAccountId());

        if (transfer.getTransferAmount().compareTo(sourceAccount.getCurrentBalance()) > 0) {
            throw new RuntimeException("Transfer amount exceeds available balance");
        }

        transfer.setStatus("pending");
        EpfTransfer saved = epfTransferRepository.save(transfer);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getSourceEpfAccountId(),
                saved.getEmployeeId(),
                "transfer",
                saved.getTransferId(),
                "transfer_requested",
                saved.getCreatedBy(),
                Map.of("transferAmount", nvl(saved.getTransferAmount()), "transferType", saved.getTransferType())
        );
        return saved;
    }

    public EpfTransfer processTransfer(UUID transferId) {
        log.info("Processing EPF transfer: {}", transferId);

        EpfTransfer transfer = epfTransferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        if (!"pending".equals(transfer.getStatus())) {
            throw new RuntimeException("Transfer cannot be processed. Current status: " + transfer.getStatus());
        }

        EpfAccount sourceAccount = getEpfAccountById(transfer.getSourceEpfAccountId());

        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(transfer.getTransferAmount()));
        epfAccountRepository.save(sourceAccount);

        if (transfer.getTargetEpfAccountId() != null) {
            EpfAccount targetAccount = getEpfAccountById(transfer.getTargetEpfAccountId());
            targetAccount.setCurrentBalance(targetAccount.getCurrentBalance().add(transfer.getTransferAmount()));
            epfAccountRepository.save(targetAccount);
        }

        transfer.setStatus("processed");
        transfer.setTransferDate(LocalDate.now());
        EpfTransfer saved = epfTransferRepository.save(transfer);
        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getSourceEpfAccountId(),
                saved.getEmployeeId(),
                "transfer",
                saved.getTransferId(),
                "transfer_processed",
                saved.getUpdatedBy(),
                Map.of("transferAmount", nvl(saved.getTransferAmount()), "targetEpfAccountId", saved.getTargetEpfAccountId() != null ? saved.getTargetEpfAccountId().toString() : "")
        );
        return saved;
    }

    public List<EpfTransfer> getTransfersByAccount(UUID epfAccountId) {
        log.debug("Fetching transfers for EPF account: {}", epfAccountId);
        return epfTransferRepository.findBySourceEpfAccountId(epfAccountId);
    }

    // =====================================================
    // NOMINATION MANAGEMENT
    // =====================================================

    public EpfNomination createNomination(EpfNomination nomination) {
        log.info("Creating EPF nomination for account: {}", nomination.getEpfAccountId());

        List<EpfNomination> existingNominations = epfNominationRepository
                .findByEpfAccountIdAndIsActive(nomination.getEpfAccountId(), true);

        BigDecimal totalShare = existingNominations.stream()
                .map(EpfNomination::getSharePercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalShare.add(nomination.getSharePercentage()).compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException("Total nomination share percentage cannot exceed 100%");
        }

        if (nomination.getIsPrimary()) {
            epfNominationRepository.findByEpfAccountIdAndIsPrimary(nomination.getEpfAccountId(), true)
                    .ifPresent(primary -> {
                        primary.setIsPrimary(false);
                        epfNominationRepository.save(primary);
                    });
        }

        return epfNominationRepository.save(nomination);
    }

    public List<EpfNomination> getNominationsByAccount(UUID epfAccountId) {
        log.debug("Fetching nominations for EPF account: {}", epfAccountId);
        return epfNominationRepository.findByEpfAccountId(epfAccountId);
    }

    public EpfNomination updateNomination(UUID nominationId, EpfNomination nominationData) {
        log.info("Updating EPF nomination: {}", nominationId);

        EpfNomination nomination = epfNominationRepository.findById(nominationId)
                .orElseThrow(() -> new RuntimeException("Nomination not found: " + nominationId));

        nomination.setNomineeName(nominationData.getNomineeName());
        nomination.setNomineeRelationship(nominationData.getNomineeRelationship());
        nomination.setNomineeDateOfBirth(nominationData.getNomineeDateOfBirth());
        nomination.setNomineeAddress(nominationData.getNomineeAddress());
        nomination.setNomineePhone(nominationData.getNomineePhone());
        nomination.setNomineeEmail(nominationData.getNomineeEmail());
        nomination.setSharePercentage(nominationData.getSharePercentage());
        nomination.setIsPrimary(nominationData.getIsPrimary());

        return epfNominationRepository.save(nomination);
    }

    // =====================================================
    // STATEMENT GENERATION
    // =====================================================

    public Map<String, Object> generateAccountStatement(UUID epfAccountId, LocalDate fromDate, LocalDate toDate) {
        log.info("Generating EPF account statement for account: {} from {} to {}",
                epfAccountId, fromDate, toDate);

        EpfAccount account = getEpfAccountById(epfAccountId);

        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(epfAccountId).stream()
                .filter(c -> {
                    LocalDate contribDate = LocalDate.of(c.getContributionYear(), c.getContributionMonth(), 1);
                    return !contribDate.isBefore(fromDate) && !contribDate.isAfter(toDate);
                })
                .toList();

        List<EpfWithdrawal> withdrawals = epfWithdrawalRepository.findByEpfAccountId(epfAccountId).stream()
                .filter(w -> w.getWithdrawalDate() != null &&
                        !w.getWithdrawalDate().isBefore(fromDate) && !w.getWithdrawalDate().isAfter(toDate))
                .toList();

        List<EpfTransfer> transfers = epfTransferRepository.findBySourceEpfAccountId(epfAccountId).stream()
                .filter(t -> t.getTransferDate() != null &&
                        !t.getTransferDate().isBefore(fromDate) && !t.getTransferDate().isAfter(toDate))
                .toList();

        BigDecimal totalContributions = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = withdrawals.stream()
                .filter(w -> "processed".equals(w.getStatus()))
                .map(EpfWithdrawal::getApprovedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> statement = new HashMap<>();
        statement.put("account", account);
        statement.put("fromDate", fromDate);
        statement.put("toDate", toDate);
        statement.put("openingBalance", account.getCurrentBalance().subtract(totalContributions).add(totalWithdrawals));
        statement.put("contributions", contributions);
        statement.put("withdrawals", withdrawals);
        statement.put("transfers", transfers);
        statement.put("totalContributions", totalContributions);
        statement.put("totalWithdrawals", totalWithdrawals);
        statement.put("closingBalance", account.getCurrentBalance());

        return statement;
    }

    // =====================================================
    // COMPLIANCE MANAGEMENT
    // =====================================================

    public EpfComplianceRecord createComplianceRecord(EpfComplianceRecord record) {
        log.info("Creating EPF compliance record for organization: {}, type: {}",
                record.getOrganizationId(), record.getComplianceType());
        return epfComplianceRecordRepository.save(record);
    }

    public EpfComplianceRecord updateComplianceRecord(UUID recordId, EpfComplianceRecord recordData) {
        log.info("Updating EPF compliance record: {}", recordId);

        EpfComplianceRecord record = epfComplianceRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Compliance record not found: " + recordId));

        record.setSubmissionDate(recordData.getSubmissionDate());
        record.setStatus(recordData.getStatus());
        record.setFileReference(recordData.getFileReference());
        record.setAmount(recordData.getAmount());
        record.setPenaltyAmount(recordData.getPenaltyAmount());
        record.setNotes(recordData.getNotes());

        return epfComplianceRecordRepository.save(record);
    }

    public EpfComplianceRecord getComplianceRecordById(UUID recordId) {
        return epfComplianceRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Compliance record not found: " + recordId));
    }

    public List<EpfComplianceRecord> getComplianceRecordsByOrganization(UUID organizationId) {
        log.debug("Fetching compliance records for organization: {}", organizationId);
        return epfComplianceRecordRepository.findByOrganizationId(organizationId);
    }

    public List<EpfComplianceRecord> getOverdueComplianceRecords(UUID organizationId) {
        log.debug("Fetching overdue compliance records for organization: {}", organizationId);
        return epfComplianceRecordRepository.findOverdueComplianceRecords(organizationId, LocalDate.now());
    }

    public BigDecimal calculateStatutoryContribution(UUID organizationId, Integer month, Integer year) {
        log.info("Calculating statutory EPF contribution for organization: {}, period: {}/{}",
                organizationId, month, year);

        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationAndPeriod(organizationId, month, year);

        return contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =====================================================
    // REVERSALS / ADJUSTMENTS
    // =====================================================

    public EpfContribution reverseContribution(UUID contributionId, String reason, String actorUserId) {
        EpfContribution original = epfContributionRepository.findById(contributionId)
                .orElseThrow(() -> new RuntimeException("Contribution not found: " + contributionId));
        if ("reversed".equalsIgnoreCase(original.getStatus()) || "reversal".equalsIgnoreCase(original.getStatus())) {
            throw new RuntimeException("Contribution is already reversed: " + contributionId);
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Reversal reason is required");
        }

        EpfContribution reversal = EpfContribution.builder()
                .epfAccountId(original.getEpfAccountId())
                .employeeId(original.getEmployeeId())
                .organizationId(original.getOrganizationId())
                .contributionPeriodStart(original.getContributionPeriodStart())
                .contributionPeriodEnd(original.getContributionPeriodEnd())
                .contributionMonth(original.getContributionMonth())
                .contributionYear(original.getContributionYear())
                .employeeBasicSalary(original.getEmployeeBasicSalary())
                .pfWageBase(original.getPfWageBase())
                .employeeContributionRate(original.getEmployeeContributionRate())
                .employeeContributionAmount(nvl(original.getEmployeeContributionAmount()).negate())
                .employerContributionRate(original.getEmployerContributionRate())
                .employerContributionAmount(nvl(original.getEmployerContributionAmount()).negate())
                .employerEpfAmount(nvl(original.getEmployerEpfAmount()).negate())
                .employerPensionAmount(nvl(original.getEmployerPensionAmount()).negate())
                .employerEdliAmount(nvl(original.getEmployerEdliAmount()).negate())
                .employerAdminChargeAmount(nvl(original.getEmployerAdminChargeAmount()).negate())
                .totalContribution(nvl(original.getTotalContribution()).negate())
                .status("reversal")
                .processedDate(LocalDate.now())
                .payrollRunId(original.getPayrollRunId())
                .createdBy(actorUserId)
                .updatedBy(actorUserId)
                .build();

        EpfContribution saved = epfContributionRepository.save(reversal);
        original.setStatus("reversed");
        original.setUpdatedBy(actorUserId);
        epfContributionRepository.save(original);

        updateEpfAccountBalanceFromContributions(original.getEpfAccountId());
        logCorrection(original.getOrganizationId(), original.getEpfAccountId(), "contribution",
                original.getContributionId(), "reversal", reversal.getTotalContribution(), reason, actorUserId);
        appendAuditEvent(
                original.getOrganizationId(),
                original.getEpfAccountId(),
                original.getEmployeeId(),
                "contribution",
                original.getContributionId(),
                "contribution_reversed",
                actorUserId,
                Map.of("reason", reason != null ? reason : "", "reversalContributionId", saved.getContributionId().toString())
        );
        return saved;
    }

    public EpfWithdrawal reverseProcessedWithdrawal(UUID withdrawalId, String reason, String actorUserId) {
        EpfWithdrawal withdrawal = getWithdrawalById(withdrawalId);
        if (!"processed".equalsIgnoreCase(withdrawal.getStatus())) {
            throw new RuntimeException("Only processed withdrawals can be reversed");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Reversal reason is required");
        }
        BigDecimal amount = withdrawal.getApprovedAmount() != null ? withdrawal.getApprovedAmount() : withdrawal.getRequestedAmount();
        EpfAccount account = getEpfAccountById(withdrawal.getEpfAccountId());
        account.setCurrentBalance(nvl(account.getCurrentBalance()).add(nvl(amount)));
        epfAccountRepository.save(account);

        withdrawal.setStatus("reversed");
        withdrawal.setNotes(appendNote(withdrawal.getNotes(), "Reversed: " + reason));
        withdrawal.setUpdatedBy(actorUserId);
        EpfWithdrawal saved = epfWithdrawalRepository.save(withdrawal);

        logCorrection(withdrawal.getOrganizationId(), withdrawal.getEpfAccountId(), "withdrawal",
                withdrawal.getWithdrawalId(), "reversal", nvl(amount), reason, actorUserId);
        appendAuditEvent(
                withdrawal.getOrganizationId(),
                withdrawal.getEpfAccountId(),
                withdrawal.getEmployeeId(),
                "withdrawal",
                withdrawal.getWithdrawalId(),
                "withdrawal_reversed",
                actorUserId,
                Map.of("reason", reason != null ? reason : "", "amount", nvl(amount))
        );
        return saved;
    }

    public EpfTransfer reverseProcessedTransfer(UUID transferId, String reason, String actorUserId) {
        EpfTransfer transfer = getTransferById(transferId);
        if (!"processed".equalsIgnoreCase(transfer.getStatus())) {
            throw new RuntimeException("Only processed transfers can be reversed");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Reversal reason is required");
        }
        BigDecimal amount = nvl(transfer.getTransferAmount());

        EpfAccount source = getEpfAccountById(transfer.getSourceEpfAccountId());
        source.setCurrentBalance(nvl(source.getCurrentBalance()).add(amount));
        epfAccountRepository.save(source);

        if (transfer.getTargetEpfAccountId() != null) {
            EpfAccount target = getEpfAccountById(transfer.getTargetEpfAccountId());
            target.setCurrentBalance(nvl(target.getCurrentBalance()).subtract(amount));
            epfAccountRepository.save(target);
        }

        transfer.setStatus("reversed");
        transfer.setNotes(appendNote(transfer.getNotes(), "Reversed: " + reason));
        transfer.setUpdatedBy(actorUserId);
        EpfTransfer saved = epfTransferRepository.save(transfer);

        logCorrection(transfer.getOrganizationId(), transfer.getSourceEpfAccountId(), "transfer",
                transfer.getTransferId(), "reversal", amount, reason, actorUserId);
        appendAuditEvent(
                transfer.getOrganizationId(),
                transfer.getSourceEpfAccountId(),
                transfer.getEmployeeId(),
                "transfer",
                transfer.getTransferId(),
                "transfer_reversed",
                actorUserId,
                Map.of("reason", reason, "amount", amount)
        );
        return saved;
    }

    public List<EpfCorrectionLog> getCorrectionLogs(UUID organizationId) {
        return epfCorrectionLogRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    public List<EpfAuditEvent> getAuditEvents(UUID organizationId) {
        return epfAuditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    public EpfExitCase initiateExitCase(UUID organizationId, UUID employeeId, String exitType, UUID targetEpfAccountId,
                                        String notes, String actorUserId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        if (!organizationId.equals(employee.getOrganizationId())) {
            throw new RuntimeException("Employee does not belong to organization: " + organizationId);
        }

        EpfAccount sourceAccount = epfAccountRepository.findByEmployeeId(employeeId).stream()
                .filter(a -> organizationId.equals(a.getOrganizationId()))
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active EPF account found for employee: " + employeeId));

        EpfExitCase exitCase = EpfExitCase.builder()
                .organizationId(organizationId)
                .employeeId(employeeId)
                .sourceEpfAccountId(sourceAccount.getEpfAccountId())
                .targetEpfAccountId(targetEpfAccountId)
                .exitType(exitType != null ? exitType.toLowerCase() : "close_only")
                .terminationDate(employee.getTerminationDate())
                .status("initiated")
                .notes(notes)
                .createdBy(actorUserId)
                .updatedBy(actorUserId)
                .build();

        String normalized = exitCase.getExitType();
        if ("transfer".equals(normalized)) {
            if (targetEpfAccountId == null) {
                throw new RuntimeException("targetEpfAccountId is required for transfer exit type");
            }
            EpfTransfer transfer = createTransferRequest(EpfTransfer.builder()
                    .sourceEpfAccountId(sourceAccount.getEpfAccountId())
                    .targetEpfAccountId(targetEpfAccountId)
                    .employeeId(employeeId)
                    .organizationId(organizationId)
                    .transferType("inter_organization")
                    .transferAmount(nvl(sourceAccount.getCurrentBalance()))
                    .notes("Auto-created for EPF exit case")
                    .build());
            exitCase.setTransferId(transfer.getTransferId());
            exitCase.setStatus("transfer_pending");
        } else if ("settlement".equals(normalized)) {
            EpfWithdrawal withdrawal = createWithdrawalRequest(EpfWithdrawal.builder()
                    .epfAccountId(sourceAccount.getEpfAccountId())
                    .employeeId(employeeId)
                    .organizationId(organizationId)
                    .withdrawalType("full")
                    .requestedAmount(nvl(sourceAccount.getCurrentBalance()))
                    .withdrawalReason("Exit settlement")
                    .notes("Auto-created for EPF exit case")
                    .build());
            exitCase.setWithdrawalId(withdrawal.getWithdrawalId());
            exitCase.setStatus("settlement_pending");
        }

        sourceAccount.setAccountStatus("exit_in_progress");
        sourceAccount.setUpdatedBy(actorUserId);
        epfAccountRepository.save(sourceAccount);
        EpfExitCase saved = epfExitCaseRepository.save(exitCase);

        appendAuditEvent(
                organizationId,
                sourceAccount.getEpfAccountId(),
                employeeId,
                "exit_case",
                saved.getExitCaseId(),
                "exit_case_initiated",
                actorUserId,
                Map.of(
                        "exitType", saved.getExitType(),
                        "status", saved.getStatus(),
                        "transferId", saved.getTransferId() != null ? saved.getTransferId().toString() : "",
                        "withdrawalId", saved.getWithdrawalId() != null ? saved.getWithdrawalId().toString() : ""
                )
        );
        return saved;
    }

    public EpfExitCase completeExitCase(UUID exitCaseId, String completionReference, String actorUserId) {
        EpfExitCase exitCase = epfExitCaseRepository.findById(exitCaseId)
                .orElseThrow(() -> new RuntimeException("EPF exit case not found: " + exitCaseId));

        if (exitCase.getTransferId() != null) {
            EpfTransfer transfer = getTransferById(exitCase.getTransferId());
            if (!"processed".equalsIgnoreCase(transfer.getStatus())) {
                throw new RuntimeException("Exit case transfer is not processed yet");
            }
        }
        if (exitCase.getWithdrawalId() != null) {
            EpfWithdrawal withdrawal = getWithdrawalById(exitCase.getWithdrawalId());
            if (!"processed".equalsIgnoreCase(withdrawal.getStatus())) {
                throw new RuntimeException("Exit case withdrawal is not processed yet");
            }
        }

        EpfAccount source = getEpfAccountById(exitCase.getSourceEpfAccountId());
        source.setAccountStatus("closed");
        source.setIsActive(false);
        source.setClosingDate(LocalDate.now());
        source.setUpdatedBy(actorUserId);
        epfAccountRepository.save(source);

        exitCase.setStatus("completed");
        exitCase.setCompletionReference(completionReference);
        exitCase.setUpdatedBy(actorUserId);
        EpfExitCase saved = epfExitCaseRepository.save(exitCase);

        appendAuditEvent(
                saved.getOrganizationId(),
                saved.getSourceEpfAccountId(),
                saved.getEmployeeId(),
                "exit_case",
                saved.getExitCaseId(),
                "exit_case_completed",
                actorUserId,
                Map.of("completionReference", completionReference != null ? completionReference : "")
        );
        return saved;
    }

    public List<EpfExitCase> getExitCasesByOrganization(UUID organizationId) {
        return epfExitCaseRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    public EpfExitCase getExitCaseById(UUID exitCaseId) {
        return epfExitCaseRepository.findById(exitCaseId)
                .orElseThrow(() -> new RuntimeException("EPF exit case not found: " + exitCaseId));
    }

    private void logCorrection(UUID organizationId, UUID epfAccountId, String entityType, UUID entityId,
                               String actionType, BigDecimal amountImpact, String reason, String actorUserId) {
        EpfCorrectionLog logEntry = EpfCorrectionLog.builder()
                .organizationId(organizationId)
                .epfAccountId(epfAccountId)
                .entityType(entityType)
                .entityId(entityId)
                .actionType(actionType)
                .amountImpact(amountImpact)
                .reason(reason)
                .reversedBy(actorUserId)
                .build();
        epfCorrectionLogRepository.save(logEntry);
    }

    private static String appendNote(String existing, String extra) {
        if (existing == null || existing.isBlank()) {
            return extra;
        }
        return existing + " | " + extra;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildApprovalWorkflowJson(String stage, UUID reviewerId, UUID approverId, String comments) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("stage", stage);
        payload.put("reviewedBy", reviewerId != null ? reviewerId.toString() : null);
        payload.put("approvedBy", approverId != null ? approverId.toString() : null);
        payload.put("comments", comments);
        payload.put("updatedAt", LocalDate.now().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize approval workflow", e);
        }
    }

    private String extractWorkflowActor(String approvalWorkflow, String fieldName) {
        if (approvalWorkflow == null || approvalWorkflow.isBlank() || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(approvalWorkflow);
            JsonNode field = root.get(fieldName);
            if (field == null || field.isNull()) {
                return null;
            }
            String value = field.asText();
            return value == null || value.isBlank() ? null : value;
        } catch (JsonProcessingException ex) {
            log.warn("Ignoring malformed approval workflow JSON while reading {}: {}", fieldName, ex.getMessage());
            return null;
        }
    }

    private void appendAuditEvent(UUID organizationId, UUID epfAccountId, UUID employeeId, String entityType, UUID entityId,
                                  String eventType, String actorUserId, Map<String, Object> eventData) {
        String data;
        try {
            data = objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            data = "{\"serializationError\":\"" + e.getMessage() + "\"}";
        }
        EpfAuditEvent event = EpfAuditEvent.builder()
                .organizationId(organizationId)
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .entityType(entityType)
                .entityId(entityId)
                .eventType(eventType)
                .actorUserId(actorUserId)
                .eventData(data)
                .build();
        epfAuditEventRepository.save(event);
    }
}
