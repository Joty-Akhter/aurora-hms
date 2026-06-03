package com.easyops.ar.service;

import com.easyops.ar.dto.AgingReportResponse;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.repository.ARInvoiceRepository;
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
    
    private final ARInvoiceRepository invoiceRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = "agingReports", key = "#organizationId + '_' + #asOfDate")
    public List<AgingReportResponse> generateAgingReport(UUID organizationId, LocalDate asOfDate) {
        log.info("Generating AR aging report for organization: {} as of: {}", organizationId, asOfDate);
        
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        List<ARInvoice> outstandingInvoices = invoiceRepository.findOutstandingInvoices(organizationId);
        List<AgingReportResponse> reportItems = new ArrayList<>();
        
        for (ARInvoice invoice : outstandingInvoices) {
            AgingReportResponse item = new AgingReportResponse();
            item.setInvoiceId(invoice.getId());
            item.setInvoiceNumber(invoice.getInvoiceNumber());
            item.setInvoiceDate(invoice.getInvoiceDate());
            item.setDueDate(invoice.getDueDate());
            item.setCustomerId(invoice.getCustomer().getId());
            item.setCustomerName(invoice.getCustomer().getCustomerName());
            item.setTotalAmount(invoice.getTotalAmount());
            item.setBalanceDue(invoice.getBalanceDue());
            
            // Calculate days overdue
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate);
            item.setDaysOverdue((int) Math.max(0, daysOverdue));
            
            // Categorize into aging buckets
            BigDecimal balanceDue = invoice.getBalanceDue();
            
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
        
        log.info("Generated aging report with {} items", reportItems.size());
        return reportItems;
    }
}

