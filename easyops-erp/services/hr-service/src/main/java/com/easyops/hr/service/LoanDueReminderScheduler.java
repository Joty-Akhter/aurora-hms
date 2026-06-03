package com.easyops.hr.service;

import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.LoanInstallment;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.LoanInstallmentRepository;
import com.easyops.hr.repository.LoanNotificationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RE-03: daily in-app reminders for installments due within a rolling window (employee must have linked user id).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "hr.loans.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class LoanDueReminderScheduler {

    private final LoanInstallmentRepository loanInstallmentRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanNotificationService loanNotificationService;
    private final LoanNotificationEventRepository loanNotificationEventRepository;

    @Value("${hr.loans.reminder.days-ahead:7}")
    private int daysAhead;

    @Value("${hr.loans.reminder.cooldown-days:7}")
    private int cooldownDays;

    @Scheduled(cron = "${hr.loans.reminder.cron:0 0 7 * * ?}")
    public void sendDueReminders() {
        LocalDate today = LocalDate.now();
        LocalDate to = today.plusDays(daysAhead);
        List<LoanInstallment> rows = loanInstallmentRepository.findUnpaidInstallmentsDueBetween(today, to);
        LocalDateTime since = LocalDateTime.now().minusDays(cooldownDays);
        int sent = 0;
        for (LoanInstallment inst : rows) {
            EmployeeLoan loan = employeeLoanRepository.findById(inst.getLoanId()).orElse(null);
            if (loan == null) {
                continue;
            }
            String dedupeToken = "|seq=" + inst.getSequenceNumber() + "|due=" + inst.getDueDate() + "|";
            if (loanNotificationEventRepository.existsByLoanIdAndEventTypeAndBodyContainingAndCreatedAtAfter(
                    loan.getLoanId(),
                    LoanNotificationService.EVT_PAYMENT_DUE_REMINDER,
                    dedupeToken,
                    since)) {
                continue;
            }
            BigDecimal remaining = inst.getScheduledAmount().subtract(inst.getPaidAmount()).max(BigDecimal.ZERO);
            String body =
                    dedupeToken
                            + "\nInstallment "
                            + inst.getSequenceNumber()
                            + " of "
                            + loan.getTenureMonths()
                            + " is due on "
                            + inst.getDueDate()
                            + ". Remaining: "
                            + remaining.toPlainString()
                            + " "
                            + (loan.getCurrency() != null ? loan.getCurrency() : "")
                            + ".";
            loanNotificationService.notifyLoanEmployee(
                    loan.getOrganizationId(),
                    loan.getEmployeeId(),
                    loan.getLoanId(),
                    loan.getLoanApplicationId(),
                    LoanNotificationService.EVT_PAYMENT_DUE_REMINDER,
                    "Loan payment due soon",
                    body);
            sent++;
        }
        if (sent > 0) {
            log.debug("Loan due reminders sent: {}", sent);
        }
    }
}
