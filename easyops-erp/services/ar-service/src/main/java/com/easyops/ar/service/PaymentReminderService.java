package com.easyops.ar.service;

import com.easyops.ar.client.EmailClient;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.entity.ReminderConfig;
import com.easyops.ar.entity.ReminderHistory;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.ReminderConfigRepository;
import com.easyops.ar.repository.ReminderHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderService {
    
    private final ARInvoiceRepository invoiceRepository;
    private final ReminderConfigRepository reminderConfigRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;
    private final EmailClient emailClient;
    
    /**
     * Scheduled job to send payment reminders
     * Runs daily at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendScheduledReminders() {
        log.info("Starting scheduled payment reminder job");
        
        // Get all organizations with reminder configs
        List<ReminderConfig> configs = reminderConfigRepository.findAll();
        
        for (ReminderConfig config : configs) {
            if (config.getEnabled()) {
                processRemindersForOrganization(config);
            }
        }
        
        log.info("Completed scheduled payment reminder job");
    }
    
    /**
     * Process reminders for a specific organization
     */
    @Transactional
    public void processRemindersForOrganization(ReminderConfig config) {
        log.info("Processing reminders for organization: {}", config.getOrganizationId());
        
        LocalDate today = LocalDate.now();
        
        // Get all outstanding invoices
        List<ARInvoice> outstandingInvoices = invoiceRepository.findOutstandingInvoices(config.getOrganizationId());
        
        for (ARInvoice invoice : outstandingInvoices) {
            processInvoiceReminder(invoice, config, today);
        }
    }
    
    /**
     * Process reminder for a single invoice
     */
    private void processInvoiceReminder(ARInvoice invoice, ReminderConfig config, LocalDate today) {
        long daysToDue = ChronoUnit.DAYS.between(today, invoice.getDueDate());
        long daysOverdue = daysToDue < 0 ? Math.abs(daysToDue) : 0;
        
        Integer reminderLevel = null;
        String emailTemplate = null;
        
        // Determine which reminder to send
        if (daysToDue > 0 && config.getDaysBeforeDue() != null) {
            // Before due date reminder
            if (daysToDue == Math.abs(config.getDaysBeforeDue())) {
                reminderLevel = 0;
                emailTemplate = config.getEmailTemplateBeforeDue();
            }
        } else if (daysOverdue > 0) {
            // Overdue reminders
            if (daysOverdue == config.getDaysAfterDueLevel1()) {
                reminderLevel = 1;
                emailTemplate = config.getEmailTemplateLevel1();
            } else if (daysOverdue == config.getDaysAfterDueLevel2()) {
                reminderLevel = 2;
                emailTemplate = config.getEmailTemplateLevel2();
            } else if (daysOverdue == config.getDaysAfterDueLevel3()) {
                reminderLevel = 3;
                emailTemplate = config.getEmailTemplateLevel3();
            }
        }
        
        // Send reminder if applicable and not already sent
        if (reminderLevel != null && emailTemplate != null) {
            Optional<ReminderHistory> existingReminder = reminderHistoryRepository.findByInvoiceIdAndLevel(
                    invoice.getId(), reminderLevel);
            
            if (existingReminder.isEmpty()) {
                sendReminder(invoice, config, reminderLevel, emailTemplate);
            } else {
                log.debug("Reminder already sent for invoice {} at level {}", invoice.getInvoiceNumber(), reminderLevel);
            }
        }
    }
    
    /**
     * Send reminder email
     */
    private void sendReminder(ARInvoice invoice, ReminderConfig config, Integer level, String emailTemplate) {
        Customer customer = invoice.getCustomer();
        
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            log.warn("Cannot send reminder for invoice {}: Customer has no email", invoice.getInvoiceNumber());
            return;
        }
        
        try {
            // Create reminder history record
            ReminderHistory history = new ReminderHistory();
            history.setOrganizationId(config.getOrganizationId());
            history.setInvoice(invoice);
            history.setSentDate(LocalDate.now());
            history.setReminderLevel(level);
            history.setRecipientEmail(customer.getEmail());
            
            // Format email content
            String emailContent = formatEmailTemplate(emailTemplate, invoice, customer);
            
            // Send email via notification service
            String emailSubject = String.format("Payment Reminder - Invoice %s", invoice.getInvoiceNumber());
            
            log.info("Sending reminder level {} for invoice {} to {}", 
                    level, invoice.getInvoiceNumber(), customer.getEmail());
            
            try {
                emailClient.sendEmail(customer.getEmail(), emailSubject, emailContent, config.getOrganizationId());
                history.setEmailSent(true);
                log.info("Reminder email sent successfully");
            } catch (Exception emailError) {
                log.error("Failed to send email: {}", emailError.getMessage());
                history.setEmailSent(false);
                history.setErrorMessage(emailError.getMessage());
            }
            
            reminderHistoryRepository.save(history);
            
            log.info("Reminder sent successfully for invoice: {}", invoice.getInvoiceNumber());
            
        } catch (Exception e) {
            log.error("Failed to send reminder for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            
            // Record failure
            ReminderHistory history = new ReminderHistory();
            history.setOrganizationId(config.getOrganizationId());
            history.setInvoice(invoice);
            history.setSentDate(LocalDate.now());
            history.setReminderLevel(level);
            history.setRecipientEmail(customer.getEmail());
            history.setEmailSent(false);
            history.setErrorMessage(e.getMessage());
            reminderHistoryRepository.save(history);
        }
    }
    
    /**
     * Format email template with invoice and customer details
     */
    private String formatEmailTemplate(String template, ARInvoice invoice, Customer customer) {
        return template
                .replace("{customerName}", customer.getCustomerName())
                .replace("{invoiceNumber}", invoice.getInvoiceNumber())
                .replace("{invoiceDate}", invoice.getInvoiceDate().toString())
                .replace("{dueDate}", invoice.getDueDate().toString())
                .replace("{totalAmount}", invoice.getTotalAmount().toString())
                .replace("{balanceDue}", invoice.getBalanceDue().toString());
    }
    
    /**
     * Manual trigger for sending reminders (for testing or manual run)
     */
    @Transactional
    public void sendRemindersNow(UUID organizationId) {
        log.info("Manually triggering reminders for organization: {}", organizationId);
        
        Optional<ReminderConfig> configOpt = reminderConfigRepository.findByOrganizationId(organizationId);
        
        if (configOpt.isEmpty()) {
            throw new RuntimeException("No reminder configuration found for organization");
        }
        
        ReminderConfig config = configOpt.get();
        if (!config.getEnabled()) {
            throw new RuntimeException("Reminders are not enabled for this organization");
        }
        
        processRemindersForOrganization(config);
        log.info("Manual reminder trigger completed");
    }
    
    /**
     * Get reminder history for an organization
     */
    @Transactional(readOnly = true)
    public List<ReminderHistory> getReminderHistory(UUID organizationId, Integer days) {
        LocalDate startDate = LocalDate.now().minusDays(days != null ? days : 30);
        return reminderHistoryRepository.findRecentReminders(startDate);
    }
}

