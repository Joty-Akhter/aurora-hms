package com.easyops.hr.service;

import com.easyops.hr.dto.*;
import com.easyops.hr.entity.*;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LoanApplicationActionRepository;
import com.easyops.hr.repository.LoanApplicationRepository;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.util.LoanJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private static final List<LoanApplicationStatus> BLOCKING_STATUSES = List.of(
            LoanApplicationStatus.SUBMITTED,
            LoanApplicationStatus.PENDING_FINANCE_APPROVAL,
            LoanApplicationStatus.AWAITING_CLARIFICATION,
            LoanApplicationStatus.APPROVED);

    /** ST-01, EL-05: block new applications while a loan account is open or exit settlement is pending. */
    private static final Set<EmployeeLoanStatus> BLOCKING_LOAN_STATUSES = EnumSet.of(
            EmployeeLoanStatus.PENDING_DISBURSEMENT,
            EmployeeLoanStatus.ACTIVE,
            EmployeeLoanStatus.SETTLEMENT_PENDING);

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationActionRepository loanApplicationActionRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanCategoryRepository loanCategoryRepository;
    private final LoanOrgSettingsProvider loanOrgSettingsProvider;
    private final ApprovedLoanCreator approvedLoanCreator;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanNotificationService loanNotificationService;

    @Transactional
    public LoanApplicationDto create(UUID organizationId, LoanApplicationCreateRequest request, UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .filter(e -> organizationId.equals(e.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        LoanCategory category = loanCategoryRepository.findByCategoryIdAndOrganizationId(request.getCategoryId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan category is inactive");
        }

        LoanApplication app = new LoanApplication();
        app.setOrganizationId(organizationId);
        app.setEmployeeId(employee.getEmployeeId());
        app.setCategoryId(category.getCategoryId());
        app.setRequestedAmount(request.getRequestedAmount());
        app.setRequestedTenureMonths(request.getRequestedTenureMonths());
        app.setPurposeNotes(trimToNull(request.getPurposeNotes()));
        app.setAttachmentReferencesJson(LoanJsonUtil.toJsonStringList(request.getAttachmentReferences()));
        app.setLimitOverrideReason(trimToNull(request.getLimitOverrideReason()));
        app.setFacilityOverrideReason(trimToNull(request.getFacilityOverrideReason()));
        app.setStatus(LoanApplicationStatus.DRAFT);
        app.setApplicationDate(LocalDate.now());

        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.CREATED, actorUserId, null);
        return toDto(saved, category.getCategoryType());
    }

    @Transactional
    public LoanApplicationDto update(UUID organizationId, UUID applicationId, LoanApplicationUpdateRequest request, UUID actorUserId) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.DRAFT
                && app.getStatus() != LoanApplicationStatus.AWAITING_CLARIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only DRAFT or AWAITING_CLARIFICATION applications can be updated");
        }

        LoanCategory category = loanCategoryRepository.findByCategoryIdAndOrganizationId(request.getCategoryId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan category is inactive");
        }

        app.setCategoryId(category.getCategoryId());
        app.setRequestedAmount(request.getRequestedAmount());
        app.setRequestedTenureMonths(request.getRequestedTenureMonths());
        app.setPurposeNotes(trimToNull(request.getPurposeNotes()));
        if (request.getAttachmentReferences() != null) {
            app.setAttachmentReferencesJson(LoanJsonUtil.toJsonStringList(request.getAttachmentReferences()));
        }
        if (request.getLimitOverrideReason() != null) {
            app.setLimitOverrideReason(trimToNull(request.getLimitOverrideReason()));
        }
        if (request.getFacilityOverrideReason() != null) {
            app.setFacilityOverrideReason(trimToNull(request.getFacilityOverrideReason()));
        }

        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.UPDATED, actorUserId, null);
        return toDto(saved, category.getCategoryType());
    }

    @Transactional
    public LoanApplicationDto submit(UUID organizationId, UUID applicationId, UUID actorUserId) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.DRAFT
                && app.getStatus() != LoanApplicationStatus.AWAITING_CLARIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only DRAFT or AWAITING_CLARIFICATION applications can be submitted");
        }

        LoanOrganizationSettingsDto settingsDto = loanOrgSettingsProvider.getSettings(organizationId);
        LoanCategory category = loanCategoryRepository.findByCategoryIdAndOrganizationId(app.getCategoryId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));

        assertEligibleForSubmit(app, category, settingsDto);

        boolean wasClarification = app.getStatus() == LoanApplicationStatus.AWAITING_CLARIFICATION;
        app.setStatus(LoanApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        app.setClarificationMessage(null);
        app.setClarificationRequestedByUserId(null);
        LoanApplication saved = loanApplicationRepository.save(app);
        if (wasClarification) {
            recordAction(saved.getApplicationId(), LoanApplicationActionType.RESUBMITTED_AFTER_CLARIFICATION, actorUserId, null);
        }
        recordAction(saved.getApplicationId(), LoanApplicationActionType.SUBMITTED, actorUserId, null);
        loanNotificationService.notifyForApplication(
                saved,
                LoanNotificationService.EVT_APPLICATION_SUBMITTED,
                "Loan application submitted",
                "Your loan application was submitted and is awaiting review.",
                null);
        return toDto(saved, category.getCategoryType());
    }

    /**
     * AL-03: one endpoint — HR step when SUBMITTED; Finance step when PENDING_FINANCE_APPROVAL;
     * or single-step when org {@code skipFinanceApproval} or BR-08 salary advance path applies.
     */
    @Transactional
    public LoanApplicationDto approveOrAdvance(
            UUID organizationId, UUID applicationId, UUID actorUserId, LoanApplicationDecisionRequest decision) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(organizationId);
        if (app.getStatus() == LoanApplicationStatus.SUBMITTED) {
            return hrApprove(organizationId, app, actorUserId, settings, decision);
        }
        if (app.getStatus() == LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            String comment = decision != null ? decision.getComment() : null;
            return financeApprove(organizationId, app, actorUserId, comment);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Application is not awaiting approval at this stage (SUBMITTED or PENDING_FINANCE_APPROVAL required)");
    }

    private LoanApplicationDto hrApprove(
            UUID organizationId,
            LoanApplication app,
            UUID actorUserId,
            LoanOrganizationSettingsDto settings,
            LoanApplicationDecisionRequest decision) {
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED applications can receive HR approval");
        }

        LoanCategory category = loanCategoryRepository.findByCategoryIdAndOrganizationId(app.getCategoryId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));

        applyHrApprovalOverrides(app, actorUserId, category, settings, decision);

        String comment = decision != null ? decision.getComment() : null;

        boolean salaryAdvanceQuickPath = Boolean.TRUE.equals(settings.getSalaryAdvanceSkipFinanceApproval())
                && category.getCategoryType() == LoanCategoryType.SALARY_ADVANCE;
        if (salaryAdvanceQuickPath || Boolean.TRUE.equals(settings.getSkipFinanceApproval())) {
            return finalizeApproval(organizationId, app, actorUserId, comment, LoanApplicationActionType.APPROVED);
        }

        app.setStatus(LoanApplicationStatus.PENDING_FINANCE_APPROVAL);
        app.setDecidedAt(null);
        app.setDecidedByUserId(null);
        app.setRejectionReason(null);
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.HR_APPROVED, actorUserId, trimToNull(comment));
        loanNotificationService.notifyForApplication(
                saved,
                LoanNotificationService.EVT_HR_APPROVED_PENDING_FINANCE,
                "HR approval received",
                "Your loan application passed HR review and is awaiting Finance approval.",
                null);
        return toDto(saved);
    }

    /** AD-02 / LC-05: record approver and optional expiry when HR approves an application that used overrides. */
    private void applyHrApprovalOverrides(
            LoanApplication app,
            UUID actorUserId,
            LoanCategory category,
            LoanOrganizationSettingsDto settings,
            LoanApplicationDecisionRequest decision) {
        BigDecimal effectiveMax = effectiveMaxPrincipal(category, settings);
        if (app.getRequestedAmount().compareTo(effectiveMax) > 0) {
            if (trimToNull(app.getLimitOverrideReason()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Limit override reason must be present before HR approval when amount exceeds policy cap (AD-02)");
            }
            app.setLimitOverrideApprovedByUserId(actorUserId);
            if (decision != null && decision.getLimitOverrideExpiresAt() != null) {
                app.setLimitOverrideExpiresAt(decision.getLimitOverrideExpiresAt());
            }
        }
        if (trimToNull(app.getFacilityOverrideReason()) != null) {
            app.setFacilityOverrideApprovedByUserId(actorUserId);
            if (decision != null && decision.getFacilityOverrideExpiresAt() != null) {
                app.setFacilityOverrideExpiresAt(decision.getFacilityOverrideExpiresAt());
            }
        }
    }

    private static BigDecimal effectiveMaxPrincipal(LoanCategory category, LoanOrganizationSettingsDto settings) {
        BigDecimal effectiveMax = settings.getMaxPrincipalAmount();
        if (category.getMaxPrincipalAmount() != null) {
            effectiveMax = effectiveMax.min(category.getMaxPrincipalAmount());
        }
        return effectiveMax;
    }

    private void assertValidOverridesForFinalization(LoanApplication app, UUID organizationId) {
        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(organizationId);
        LoanCategory category = loanCategoryRepository.findByCategoryIdAndOrganizationId(app.getCategoryId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));
        BigDecimal effectiveMax = effectiveMaxPrincipal(category, settings);
        if (app.getRequestedAmount().compareTo(effectiveMax) > 0) {
            if (app.getLimitOverrideApprovedByUserId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Limit override requires HR approval before finalization (AD-02)");
            }
            if (app.getLimitOverrideExpiresAt() != null && app.getLimitOverrideExpiresAt().isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Limit override has expired");
            }
        }
        if (trimToNull(app.getFacilityOverrideReason()) != null) {
            if (app.getFacilityOverrideApprovedByUserId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Facility override requires HR approval before finalization (LC-05)");
            }
            if (app.getFacilityOverrideExpiresAt() != null && app.getFacilityOverrideExpiresAt().isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Facility override has expired");
            }
        }
    }

    @Transactional
    public LoanApplicationDto financeApprove(
            UUID organizationId,
            UUID applicationId,
            UUID actorUserId,
            String comment) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        return financeApprove(organizationId, app, actorUserId, comment);
    }

    private LoanApplicationDto financeApprove(
            UUID organizationId,
            LoanApplication app,
            UUID actorUserId,
            String comment) {
        if (app.getStatus() != LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING_FINANCE_APPROVAL applications can receive Finance approval");
        }
        return finalizeApproval(organizationId, app, actorUserId, comment, LoanApplicationActionType.FINANCE_APPROVED);
    }

    private LoanApplicationDto finalizeApproval(
            UUID organizationId,
            LoanApplication app,
            UUID actorUserId,
            String comment,
            LoanApplicationActionType actionType) {
        assertValidOverridesForFinalization(app, organizationId);
        app.setStatus(LoanApplicationStatus.APPROVED);
        app.setDecidedAt(LocalDateTime.now());
        app.setDecidedByUserId(actorUserId);
        app.setRejectionReason(null);
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), actionType, actorUserId, trimToNull(comment));
        approvedLoanCreator.createLoanFromApprovedApplication(saved);
        UUID loanId = employeeLoanRepository.findByLoanApplicationId(saved.getApplicationId())
                .map(l -> l.getLoanId())
                .orElse(null);
        loanNotificationService.notifyForApplication(
                saved,
                LoanNotificationService.EVT_APPLICATION_APPROVED,
                "Loan application approved",
                "Your loan application was approved.",
                loanId);
        return toDto(saved);
    }

    @Transactional
    public LoanApplicationDto reject(UUID organizationId, UUID applicationId, UUID actorUserId, String reason) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED
                && app.getStatus() != LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only SUBMITTED or PENDING_FINANCE_APPROVAL applications can be rejected");
        }

        app.setStatus(LoanApplicationStatus.REJECTED);
        app.setDecidedAt(LocalDateTime.now());
        app.setDecidedByUserId(actorUserId);
        app.setRejectionReason(reason.trim());
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.REJECTED, actorUserId, reason.trim());
        loanNotificationService.notifyForApplication(
                saved,
                LoanNotificationService.EVT_APPLICATION_REJECTED,
                "Loan application rejected",
                "Your loan application was rejected: " + reason.trim(),
                null);
        return toDto(saved);
    }

    @Transactional
    public LoanApplicationDto cancel(UUID organizationId, UUID applicationId, UUID actorUserId) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.DRAFT
                && app.getStatus() != LoanApplicationStatus.SUBMITTED
                && app.getStatus() != LoanApplicationStatus.PENDING_FINANCE_APPROVAL
                && app.getStatus() != LoanApplicationStatus.AWAITING_CLARIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Application cannot be cancelled in current status");
        }

        app.setStatus(LoanApplicationStatus.CANCELLED);
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.CANCELLED, actorUserId, null);
        return toDto(saved);
    }

    /** AL-03: request more information from applicant. */
    @Transactional
    public LoanApplicationDto requestClarification(
            UUID organizationId, UUID applicationId, UUID actorUserId, String message) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED
                && app.getStatus() != LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Clarification can only be requested while SUBMITTED or PENDING_FINANCE_APPROVAL");
        }
        String m = message != null ? message.trim() : "";
        if (m.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }
        app.setStatus(LoanApplicationStatus.AWAITING_CLARIFICATION);
        app.setClarificationMessage(m);
        app.setClarificationRequestedByUserId(actorUserId);
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.CLARIFICATION_REQUESTED, actorUserId, m);
        return toDto(saved);
    }

    /** AL-03: delegate HR approval to another user (SUBMITTED only). */
    @Transactional
    public LoanApplicationDto delegateHrApproval(
            UUID organizationId, UUID applicationId, UUID actorUserId, UUID delegateToUserId) {
        LoanApplication app = loadApplication(organizationId, applicationId);
        if (app.getStatus() != LoanApplicationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delegation only applies to SUBMITTED applications");
        }
        app.setDelegatedToUserId(delegateToUserId);
        LoanApplication saved = loanApplicationRepository.save(app);
        recordAction(saved.getApplicationId(), LoanApplicationActionType.DELEGATED, actorUserId,
                "delegateTo=" + delegateToUserId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public LoanApplicationDto get(UUID organizationId, UUID applicationId) {
        return toDto(loadApplication(organizationId, applicationId));
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationDto> list(
            UUID organizationId, UUID employeeId, LoanApplicationStatus status, LoanCategoryType categoryType) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<LoanApplication> apps =
                loanApplicationRepository.search(organizationId, employeeId, status, categoryType);
        Map<UUID, LoanCategoryType> typesByCategoryId = categoryTypesByCategoryId(organizationId, apps);
        return apps.stream().map(a -> toDto(a, typesByCategoryId.get(a.getCategoryId()))).toList();
    }

    private Map<UUID, LoanCategoryType> categoryTypesByCategoryId(UUID organizationId, List<LoanApplication> apps) {
        Set<UUID> ids = apps.stream().map(LoanApplication::getCategoryId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return loanCategoryRepository.findByOrganizationIdAndCategoryIdIn(organizationId, ids).stream()
                .collect(Collectors.toMap(LoanCategory::getCategoryId, LoanCategory::getCategoryType));
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationActionDto> listActions(UUID organizationId, UUID applicationId) {
        loadApplication(organizationId, applicationId);
        return loanApplicationActionRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(this::toActionDto)
                .toList();
    }

    private LoanApplication loadApplication(UUID organizationId, UUID applicationId) {
        return loanApplicationRepository.findByApplicationIdAndOrganizationId(applicationId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));
    }

    private void assertEligibleForSubmit(LoanApplication app, LoanCategory category, LoanOrganizationSettingsDto settings) {
        Employee emp = employeeRepository.findById(app.getEmployeeId())
                .filter(e -> app.getOrganizationId().equals(e.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        LocalDate hire = emp.getHireDate();
        if (hire == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee hire date is required for loan eligibility");
        }
        LocalDate eligibleOnOrAfter = hire.plusMonths(settings.getMinTenureMonths());
        if (eligibleOnOrAfter.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee must complete at least " + settings.getMinTenureMonths() + " months of service before applying (BR-01, EL-01)");
        }

        if (!Boolean.TRUE.equals(emp.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee must be active to apply for a loan (EL-02)");
        }

        assertEmploymentStatusEligible(emp, settings);

        BigDecimal effectiveMax = effectiveMaxPrincipal(category, settings);
        if (app.getRequestedAmount().compareTo(effectiveMax) > 0) {
            if (trimToNull(app.getLimitOverrideReason()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Requested amount exceeds maximum (" + effectiveMax + " " + settings.getCurrency()
                                + "); record limitOverrideReason for an authorized exception (AD-02, BR-03)");
            }
        }

        if (category.getMaxTenureMonths() != null && app.getRequestedTenureMonths() > category.getMaxTenureMonths()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requested tenure exceeds category maximum (" + category.getMaxTenureMonths() + " months)");
        }

        if (hasConflictingFacility(settings, category, app.getOrganizationId(), app.getEmployeeId(), app.getApplicationId())) {
            if (trimToNull(app.getFacilityOverrideReason()) == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Another facility conflicts with policy; record facilityOverrideReason for an authorized exception (LC-05, BR-02)");
            }
        }

        if (employeeLoanRepository.existsByOrganizationIdAndEmployeeIdAndStatusIn(
                app.getOrganizationId(), app.getEmployeeId(), BLOCKING_LOAN_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Employee has an open loan or pending exit settlement; resolve before a new application (ST-01, EL-05)");
        }
    }

    private void assertEmploymentStatusEligible(Employee emp, LoanOrganizationSettingsDto settings) {
        List<String> disqualifying = settings.getDisqualifyingEmploymentStatuses();
        String es = emp.getEmploymentStatus() == null ? "" : emp.getEmploymentStatus().trim();
        if (disqualifying == null || disqualifying.isEmpty()) {
            if (es.isEmpty() || !"ACTIVE".equalsIgnoreCase(es)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employee employment status must be ACTIVE (EL-02)");
            }
            return;
        }
        for (String d : disqualifying) {
            if (d != null && es.equalsIgnoreCase(d.trim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employment status '" + es + "' is not eligible for loans (EL-02)");
            }
        }
        if (es.isEmpty() || !"ACTIVE".equalsIgnoreCase(es)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee employment status must be ACTIVE (EL-02)");
        }
    }

    private boolean hasConflictingFacility(LoanOrganizationSettingsDto settings, LoanCategory newCategory,
                                           UUID organizationId, UUID employeeId, UUID excludeApplicationId) {
        List<LoanApplication> pending = loanApplicationRepository.findBlockingApplications(
                organizationId, employeeId, BLOCKING_STATUSES, excludeApplicationId);
        if (pending.isEmpty()) {
            return false;
        }

        if (Boolean.TRUE.equals(settings.getEnforceSingleActiveLoan())
                || !Boolean.TRUE.equals(settings.getAllowSalaryAdvanceWithActiveTermLoan())) {
            return true;
        }

        for (LoanApplication other : pending) {
            LoanCategory cat = loanCategoryRepository.findByCategoryIdAndOrganizationId(other.getCategoryId(), organizationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Loan category missing for application"));
            if (newCategory.getCategoryType() == LoanCategoryType.TERM_LOAN && cat.getCategoryType() == LoanCategoryType.TERM_LOAN) {
                return true;
            }
            if (newCategory.getCategoryType() == LoanCategoryType.SALARY_ADVANCE && cat.getCategoryType() == LoanCategoryType.SALARY_ADVANCE) {
                return true;
            }
        }
        return false;
    }

    private void recordAction(UUID applicationId, LoanApplicationActionType type, UUID actorUserId, String comment) {
        LoanApplicationAction a = new LoanApplicationAction();
        a.setApplicationId(applicationId);
        a.setActionType(type);
        a.setActorUserId(actorUserId);
        a.setCommentText(trimToNull(comment));
        loanApplicationActionRepository.save(a);
    }

    private LoanCategoryType resolveCategoryType(UUID organizationId, UUID categoryId) {
        return loanCategoryRepository.findByCategoryIdAndOrganizationId(categoryId, organizationId)
                .map(LoanCategory::getCategoryType)
                .orElse(null);
    }

    private LoanApplicationDto toDto(LoanApplication a) {
        return toDto(a, resolveCategoryType(a.getOrganizationId(), a.getCategoryId()));
    }

    private LoanApplicationDto toDto(LoanApplication a, LoanCategoryType categoryType) {
        LoanApplicationDto.LoanApplicationDtoBuilder b = LoanApplicationDto.builder()
                .applicationId(a.getApplicationId())
                .organizationId(a.getOrganizationId())
                .employeeId(a.getEmployeeId())
                .categoryId(a.getCategoryId())
                .categoryType(categoryType)
                .requestedAmount(a.getRequestedAmount())
                .requestedTenureMonths(a.getRequestedTenureMonths())
                .purposeNotes(a.getPurposeNotes())
                .attachmentReferences(LoanJsonUtil.parseStringList(a.getAttachmentReferencesJson()))
                .delegatedToUserId(a.getDelegatedToUserId())
                .clarificationMessage(a.getClarificationMessage())
                .clarificationRequestedByUserId(a.getClarificationRequestedByUserId())
                .limitOverrideReason(a.getLimitOverrideReason())
                .limitOverrideApprovedByUserId(a.getLimitOverrideApprovedByUserId())
                .limitOverrideExpiresAt(a.getLimitOverrideExpiresAt())
                .facilityOverrideReason(a.getFacilityOverrideReason())
                .facilityOverrideApprovedByUserId(a.getFacilityOverrideApprovedByUserId())
                .facilityOverrideExpiresAt(a.getFacilityOverrideExpiresAt())
                .status(a.getStatus())
                .applicationDate(a.getApplicationDate())
                .submittedAt(a.getSubmittedAt())
                .decidedAt(a.getDecidedAt())
                .decidedByUserId(a.getDecidedByUserId())
                .rejectionReason(a.getRejectionReason())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt());
        applyInstallmentPreview(b, a);
        return b.build();
    }

    /** AL-02: equal-principal monthly amount and total recovery (zero-interest v1). */
    private static void applyInstallmentPreview(LoanApplicationDto.LoanApplicationDtoBuilder b, LoanApplication a) {
        if (a.getRequestedAmount() == null || a.getRequestedTenureMonths() == null || a.getRequestedTenureMonths() < 1) {
            return;
        }
        BigDecimal monthly = a.getRequestedAmount()
                .divide(BigDecimal.valueOf(a.getRequestedTenureMonths()), 2, RoundingMode.HALF_UP);
        b.recommendedInstallmentAmount(monthly);
        b.totalScheduledRecovery(a.getRequestedAmount());
        b.installmentPreviewNote(
                "Equal principal per month; zero interest (v1). Total scheduled recovery equals requested principal.");
    }

    private LoanApplicationActionDto toActionDto(LoanApplicationAction a) {
        return LoanApplicationActionDto.builder()
                .actionId(a.getActionId())
                .applicationId(a.getApplicationId())
                .actionType(a.getActionType())
                .actorUserId(a.getActorUserId())
                .commentText(a.getCommentText())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
