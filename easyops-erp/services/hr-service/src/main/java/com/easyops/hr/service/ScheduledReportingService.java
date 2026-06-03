package com.easyops.hr.service;

import com.easyops.hr.entity.ScheduledReport;
import com.easyops.hr.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduledReportingService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportingService.class);
    
    private final ScheduledReportRepository scheduledReportRepository;
    private final CustomReportBuilderService customReportBuilderService;
    
    /**
     * Create scheduled report
     */
    public ScheduledReport createScheduledReport(ScheduledReport scheduledReport) {
        log.info("Creating scheduled report: {}", scheduledReport.getReportName());
        
        // Calculate next run date
        scheduledReport.setNextRunDate(calculateNextRunDate(
                scheduledReport.getScheduleFrequency(),
                scheduledReport.getScheduleDay()));
        
        return scheduledReportRepository.save(scheduledReport);
    }
    
    /**
     * Execute scheduled reports (called by scheduler)
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void executeScheduledReports() {
        log.info("Executing scheduled reports");
        
        List<ScheduledReport> dueReports = scheduledReportRepository
                .findReportsDueForExecution(LocalDate.now());
        
        for (ScheduledReport report : dueReports) {
            try {
                executeReport(report);
            } catch (Exception e) {
                log.error("Error executing scheduled report: {}", report.getScheduledReportId(), e);
                report.setLastRunStatus("failed");
                report.setLastError(e.getMessage());
                scheduledReportRepository.save(report);
            }
        }
    }
    
    /**
     * Execute a single report
     */
    public void executeReport(ScheduledReport report) {
        log.info("Executing scheduled report: {}", report.getScheduledReportId());
        
        try {
            // Parse report config and build report
            // This would typically parse the JSON config and call customReportBuilderService
            // For now, just update the report status
            
            report.setLastRunDate(LocalDateTime.now());
            report.setRunCount(report.getRunCount() + 1);
            report.setLastRunStatus("success");
            report.setNextRunDate(calculateNextRunDate(
                    report.getScheduleFrequency(),
                    report.getScheduleDay()));
            
            scheduledReportRepository.save(report);
            
            // Send report to recipients (would integrate with email service)
            sendReportToRecipients(report);
            
        } catch (Exception e) {
            report.setLastRunStatus("failed");
            report.setLastError(e.getMessage());
            scheduledReportRepository.save(report);
            throw e;
        }
    }
    
    /**
     * Get scheduled report by ID
     */
    public ScheduledReport getScheduledReportById(UUID reportId) {
        log.debug("Fetching scheduled report by ID: {}", reportId);
        return scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Scheduled report not found: " + reportId));
    }
    
    /**
     * Get scheduled reports for organization
     */
    public List<ScheduledReport> getScheduledReports(UUID organizationId) {
        log.debug("Fetching scheduled reports for organization: {}", organizationId);
        return scheduledReportRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Update scheduled report
     */
    public ScheduledReport updateScheduledReport(UUID reportId, ScheduledReport reportData) {
        log.info("Updating scheduled report: {}", reportId);
        
        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Scheduled report not found: " + reportId));
        
        report.setReportName(reportData.getReportName());
        report.setReportConfig(reportData.getReportConfig());
        report.setScheduleFrequency(reportData.getScheduleFrequency());
        report.setScheduleDay(reportData.getScheduleDay());
        report.setScheduleTime(reportData.getScheduleTime());
        report.setRecipients(reportData.getRecipients());
        report.setFormat(reportData.getFormat());
        report.setIsActive(reportData.getIsActive());
        report.setNextRunDate(calculateNextRunDate(
                report.getScheduleFrequency(),
                report.getScheduleDay()));
        
        return scheduledReportRepository.save(report);
    }
    
    private LocalDate calculateNextRunDate(String frequency, Integer scheduleDay) {
        LocalDate today = LocalDate.now();
        
        switch (frequency.toLowerCase()) {
            case "daily":
                return today.plusDays(1);
            case "weekly":
                int daysUntilNext = (scheduleDay != null ? scheduleDay : 1) - today.getDayOfWeek().getValue();
                if (daysUntilNext <= 0) {
                    daysUntilNext += 7;
                }
                return today.plusDays(daysUntilNext);
            case "monthly":
                LocalDate nextMonth = today.plusMonths(1);
                if (scheduleDay != null) {
                    int dayOfMonth = Math.min(scheduleDay, nextMonth.lengthOfMonth());
                    return nextMonth.withDayOfMonth(dayOfMonth);
                }
                return nextMonth.withDayOfMonth(1);
            case "quarterly":
                return today.plusMonths(3);
            case "yearly":
                return today.plusYears(1);
            default:
                return today.plusDays(1);
        }
    }
    
    private void sendReportToRecipients(ScheduledReport report) {
        log.debug("Sending report to recipients for scheduled report: {}", report.getScheduledReportId());
        // This would integrate with email service to send reports
        // For now, just log
    }
}

