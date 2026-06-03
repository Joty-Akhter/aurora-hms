package com.easyops.hr.service;

import com.easyops.hr.entity.Timesheet;
import com.easyops.hr.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimesheetService {
    
    private final TimesheetRepository timesheetRepository;
    
    public List<Timesheet> getAllTimesheets(UUID organizationId) {
        return timesheetRepository.findByOrganizationId(organizationId);
    }
    
    public Timesheet getTimesheetById(UUID timesheetId) {
        return timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new RuntimeException("Timesheet not found"));
    }
    
    public List<Timesheet> getEmployeeTimesheets(UUID employeeId, UUID organizationId) {
        return timesheetRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public List<Timesheet> getPendingTimesheets(UUID organizationId) {
        return timesheetRepository.findByOrganizationIdAndStatus(organizationId, "pending");
    }
    
    public Timesheet createTimesheet(Timesheet timesheet) {
        log.info("Creating timesheet for employee: {}", timesheet.getEmployeeId());
        timesheet.setStatus("draft");
        return timesheetRepository.save(timesheet);
    }
    
    public Timesheet updateTimesheet(UUID timesheetId, Timesheet timesheet) {
        Timesheet existing = getTimesheetById(timesheetId);
        
        if (timesheet.getTotalHours() != null) existing.setTotalHours(timesheet.getTotalHours());
        if (timesheet.getRegularHours() != null) existing.setRegularHours(timesheet.getRegularHours());
        if (timesheet.getOvertimeHours() != null) existing.setOvertimeHours(timesheet.getOvertimeHours());
        if (timesheet.getStatus() != null) existing.setStatus(timesheet.getStatus());
        
        return timesheetRepository.save(existing);
    }
    
    public Timesheet submitTimesheet(UUID timesheetId) {
        Timesheet timesheet = getTimesheetById(timesheetId);
        timesheet.setStatus("pending");
        timesheet.setSubmittedAt(LocalDateTime.now());
        return timesheetRepository.save(timesheet);
    }
    
    public Timesheet approveTimesheet(UUID timesheetId, UUID approvedBy) {
        Timesheet timesheet = getTimesheetById(timesheetId);
        timesheet.setStatus("approved");
        timesheet.setApprovedBy(approvedBy);
        timesheet.setApprovedAt(LocalDateTime.now());
        return timesheetRepository.save(timesheet);
    }
    
    public Timesheet rejectTimesheet(UUID timesheetId, UUID rejectedBy, String rejectionReason) {
        Timesheet timesheet = getTimesheetById(timesheetId);
        timesheet.setStatus("rejected");
        timesheet.setApprovedBy(rejectedBy);
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setRejectionReason(rejectionReason);
        return timesheetRepository.save(timesheet);
    }
    
    public void deleteTimesheet(UUID timesheetId) {
        timesheetRepository.deleteById(timesheetId);
    }
}

