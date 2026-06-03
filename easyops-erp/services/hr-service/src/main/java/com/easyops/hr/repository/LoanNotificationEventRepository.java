package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanNotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanNotificationEventRepository extends JpaRepository<LoanNotificationEvent, UUID> {

    List<LoanNotificationEvent> findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID recipientUserId, UUID organizationId);

    Optional<LoanNotificationEvent> findByEventIdAndRecipientUserIdAndOrganizationId(
            UUID eventId, UUID recipientUserId, UUID organizationId);

    /** Dedupe: same installment reminder at most once per rolling window. */
    boolean existsByLoanIdAndEventTypeAndBodyContainingAndCreatedAtAfter(
            UUID loanId, String eventType, String bodySubstring, LocalDateTime createdAtAfter);
}
