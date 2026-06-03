package com.easyops.ap.service;

import com.easyops.ap.dto.AgingReportResponse;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.repository.APBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgingReportService {
    
    private final APBillRepository billRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = "apAgingReports", key = "#organizationId + '_' + #asOfDate")
    public List<AgingReportResponse> generateAgingReport(UUID organizationId, LocalDate asOfDate) {
        log.info("Generating AP aging report for organization: {} as of: {}", organizationId, asOfDate);
        
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        List<APBill> outstandingBills = billRepository.findOutstandingBills(organizationId);
        List<AgingReportResponse> reportItems = new ArrayList<>();
        
        for (APBill bill : outstandingBills) {
            AgingReportResponse item = new AgingReportResponse();
            item.setBillId(bill.getId());
            item.setBillNumber(bill.getBillNumber());
            item.setBillDate(bill.getBillDate());
            item.setDueDate(bill.getDueDate());
            item.setVendorId(bill.getVendor().getId());
            item.setVendorName(bill.getVendor().getVendorName());
            item.setTotalAmount(bill.getTotalAmount());
            item.setBalanceDue(bill.getBalanceDue());
            
            // Calculate days overdue
            long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), asOfDate);
            item.setDaysOverdue((int) Math.max(0, daysOverdue));
            
            // Categorize into aging buckets
            BigDecimal balanceDue = bill.getBalanceDue();
            
            if (daysOverdue <= 0) {
                // Current (not yet due)
                item.setCurrent(balanceDue);
                item.setDays1To30(BigDecimal.ZERO);
                item.setDays31To60(BigDecimal.ZERO);
                item.setDays61To90(BigDecimal.ZERO);
                item.setDays90Plus(BigDecimal.ZERO);
            } else if (daysOverdue <= 30) {
                // 1-30 days overdue
                item.setCurrent(BigDecimal.ZERO);
                item.setDays1To30(balanceDue);
                item.setDays31To60(BigDecimal.ZERO);
                item.setDays61To90(BigDecimal.ZERO);
                item.setDays90Plus(BigDecimal.ZERO);
            } else if (daysOverdue <= 60) {
                // 31-60 days overdue
                item.setCurrent(BigDecimal.ZERO);
                item.setDays1To30(BigDecimal.ZERO);
                item.setDays31To60(balanceDue);
                item.setDays61To90(BigDecimal.ZERO);
                item.setDays90Plus(BigDecimal.ZERO);
            } else if (daysOverdue <= 90) {
                // 61-90 days overdue
                item.setCurrent(BigDecimal.ZERO);
                item.setDays1To30(BigDecimal.ZERO);
                item.setDays31To60(BigDecimal.ZERO);
                item.setDays61To90(balanceDue);
                item.setDays90Plus(BigDecimal.ZERO);
            } else {
                // 90+ days overdue
                item.setCurrent(BigDecimal.ZERO);
                item.setDays1To30(BigDecimal.ZERO);
                item.setDays31To60(BigDecimal.ZERO);
                item.setDays61To90(BigDecimal.ZERO);
                item.setDays90Plus(balanceDue);
            }
            
            reportItems.add(item);
        }
        
        log.info("Generated AP aging report with {} items", reportItems.size());
        return reportItems;
    }
}

