package com.easyops.hr.service;

import com.easyops.hr.dto.LoanNotificationEventDto;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.LoanApplication;
import com.easyops.hr.entity.LoanNotificationEvent;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LoanNotificationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RE-03: in-repo loan notifications for employees with a linked user account (self-service feed).
 */
@Service
@RequiredArgsConstructor
public class LoanNotificationService {

    public static final String EVT_APPLICATION_SUBMITTED = "LOAN_APPLICATION_SUBMITTED";
    public static final String EVT_HR_APPROVED_PENDING_FINANCE = "LOAN_HR_APPROVED_PENDING_FINANCE";
    public static final String EVT_APPLICATION_APPROVED = "LOAN_APPLICATION_APPROVED";
    public static final String EVT_APPLICATION_REJECTED = "LOAN_APPLICATION_REJECTED";
    public static final String EVT_LOAN_DISBURSED = "LOAN_DISBURSED";
    public static final String EVT_SETTLEMENT_REQUIRED = "LOAN_SETTLEMENT_REQUIRED";
    public static final String EVT_PAYMENT_DUE_REMINDER = "LOAN_PAYMENT_DUE_REMINDER";

    private final LoanNotificationEventRepository eventRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Loan lifecycle events (disbursement, settlement, due reminders) when the employee record has a linked user.
     */
    @Transactional
    public void notifyLoanEmployee(
            UUID organizationId,
            UUID employeeId,
            UUID loanId,
            UUID loanApplicationId,
            String eventType,
            String title,
            String body) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || employee.getUserId() == null) {
            return;
        }
        LoanNotificationEvent e = new LoanNotificationEvent();
        e.setOrganizationId(organizationId);
        e.setRecipientUserId(employee.getUserId());
        e.setEventType(eventType);
        e.setTitle(title);
        e.setBody(body);
        e.setLoanApplicationId(loanApplicationId);
        e.setLoanId(loanId);
        eventRepository.save(e);
    }

    @Transactional
    public void notifyForApplication(
            LoanApplication app,
            String eventType,
            String title,
            String body,
            UUID loanIdHint) {
        Employee employee = employeeRepository.findById(app.getEmployeeId()).orElse(null);
        if (employee == null || employee.getUserId() == null) {
            return;
        }
        LoanNotificationEvent e = new LoanNotificationEvent();
        e.setOrganizationId(app.getOrganizationId());
        e.setRecipientUserId(employee.getUserId());
        e.setEventType(eventType);
        e.setTitle(title);
        e.setBody(body);
        e.setLoanApplicationId(app.getApplicationId());
        e.setLoanId(loanIdHint);
        eventRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<LoanNotificationEventDto> listForUser(UUID organizationId, UUID recipientUserId, boolean unreadOnly) {
        List<LoanNotificationEvent> rows =
                eventRepository.findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(recipientUserId, organizationId);
        if (unreadOnly) {
            rows = rows.stream().filter(r -> r.getReadAt() == null).toList();
        }
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional
    public void markRead(UUID organizationId, UUID recipientUserId, UUID eventId) {
        LoanNotificationEvent e = eventRepository
                .findByEventIdAndRecipientUserIdAndOrganizationId(eventId, recipientUserId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (e.getReadAt() == null) {
            e.setReadAt(LocalDateTime.now());
            eventRepository.save(e);
        }
    }

    private LoanNotificationEventDto toDto(LoanNotificationEvent e) {
        return LoanNotificationEventDto.builder()
                .eventId(e.getEventId())
                .organizationId(e.getOrganizationId())
                .eventType(e.getEventType())
                .title(e.getTitle())
                .body(e.getBody())
                .loanApplicationId(e.getLoanApplicationId())
                .loanId(e.getLoanId())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
