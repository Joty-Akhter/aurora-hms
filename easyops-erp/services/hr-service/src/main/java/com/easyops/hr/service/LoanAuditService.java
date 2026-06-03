package com.easyops.hr.service;

import com.easyops.hr.entity.LoanAuditLog;
import com.easyops.hr.repository.LoanAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RE-04: Centralized persistence for loan audit entries.
 */
@Service
@RequiredArgsConstructor
public class LoanAuditService {

    private final LoanAuditLogRepository loanAuditLogRepository;

    /** Convenience: all loan lifecycle events tied to the loan account id (RE-04). */
    @Transactional
    public void logLoan(
            UUID organizationId,
            UUID loanId,
            String action,
            String oldValues,
            String newValues,
            UUID actorUserId) {
        log(organizationId, LoanAuditLog.ENTITY_LOAN, loanId, action, oldValues, newValues, actorUserId);
    }

    @Transactional
    public void log(
            UUID organizationId,
            String entityType,
            UUID entityId,
            String action,
            String oldValues,
            String newValues,
            UUID actorUserId) {
        LoanAuditLog row = new LoanAuditLog();
        row.setOrganizationId(organizationId);
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setAction(action);
        row.setOldValues(oldValues);
        row.setNewValues(newValues);
        row.setPerformedAt(LocalDateTime.now());
        row.setPerformedBy(actorUserId != null ? actorUserId.toString() : null);
        loanAuditLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<LoanAuditLog> listForLoan(UUID organizationId, UUID loanId) {
        return loanAuditLogRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
                organizationId, LoanAuditLog.ENTITY_LOAN, loanId);
    }

    /**
     * Org-level loan config audit: COA mapping replaces and AD-03 bulk recalc summaries (entity id = organization id).
     */
    @Transactional(readOnly = true)
    public List<LoanAuditLog> listOrgLoanConfigAudit(UUID organizationId) {
        return loanAuditLogRepository.findByOrganizationIdAndEntityTypeInOrderByPerformedAtDesc(
                organizationId,
                List.of(LoanAuditLog.ENTITY_LOAN_ACCOUNTING_COA, LoanAuditLog.ENTITY_LOAN_ORG));
    }
}
