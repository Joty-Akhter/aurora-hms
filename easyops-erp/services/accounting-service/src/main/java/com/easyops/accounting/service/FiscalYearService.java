package com.easyops.accounting.service;

import com.easyops.accounting.entity.FiscalYear;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.repository.FiscalYearRepository;
import com.easyops.accounting.repository.PeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FiscalYearService {
    
    private final FiscalYearRepository fiscalYearRepository;
    private final PeriodRepository periodRepository;
    
    public FiscalYear getFiscalYearById(UUID fiscalYearId) {
        return fiscalYearRepository.findById(fiscalYearId)
            .orElseThrow(() -> new RuntimeException("Fiscal year not found"));
    }
    
    public List<FiscalYear> getOrganizationFiscalYears(UUID organizationId) {
        return fiscalYearRepository.findByOrganizationIdOrderByStartDateDesc(organizationId);
    }
    
    public List<FiscalYear> getOpenFiscalYears(UUID organizationId) {
        return fiscalYearRepository.findOpenYears(organizationId);
    }
    
    public FiscalYear getFiscalYearForDate(UUID organizationId, LocalDate date) {
        return fiscalYearRepository.findByOrganizationIdAndDate(organizationId, date)
            .orElseThrow(() -> new RuntimeException("No fiscal year found for date: " + date));
    }
    
    @Transactional
    public FiscalYear createFiscalYear(UUID organizationId, String yearCode, LocalDate startDate, LocalDate endDate) {
        // Check if fiscal year already exists
        if (fiscalYearRepository.findByOrganizationIdAndYearCode(organizationId, yearCode).isPresent()) {
            throw new RuntimeException("Fiscal year already exists: " + yearCode);
        }
        
        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.setOrganizationId(organizationId);
        fiscalYear.setYearCode(yearCode);
        fiscalYear.setStartDate(startDate);
        fiscalYear.setEndDate(endDate);
        fiscalYear.setIsClosed(false);
        
        fiscalYear = fiscalYearRepository.save(fiscalYear);
        log.info("Created fiscal year: {} for organization: {}", yearCode, organizationId);
        
        return fiscalYear;
    }
    
    @Transactional
    public List<Period> generateMonthlyPeriods(UUID fiscalYearId, UUID organizationId) {
        FiscalYear fiscalYear = getFiscalYearById(fiscalYearId);
        
        List<Period> periods = new ArrayList<>();
        LocalDate periodStart = fiscalYear.getStartDate();
        int periodNumber = 1;
        
        while (!periodStart.isAfter(fiscalYear.getEndDate())) {
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            if (periodEnd.isAfter(fiscalYear.getEndDate())) {
                periodEnd = fiscalYear.getEndDate();
            }
            
            Period period = new Period();
            period.setOrganizationId(organizationId);
            period.setFiscalYearId(fiscalYearId);
            period.setPeriodNumber(periodNumber);
            period.setPeriodName(periodStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            period.setPeriodType("MONTHLY");
            period.setStartDate(periodStart);
            period.setEndDate(periodEnd);
            period.setStatus("OPEN");
            
            period = periodRepository.save(period);
            periods.add(period);
            
            periodStart = periodEnd.plusDays(1);
            periodNumber++;
        }
        
        log.info("Generated {} monthly periods for fiscal year: {}", periods.size(), fiscalYear.getYearCode());
        return periods;
    }
    
    @Transactional
    public FiscalYear createCurrentFiscalYearWithPeriods(UUID organizationId) {
        // Create fiscal year for current year (Jan 1 - Dec 31)
        int currentYear = LocalDate.now().getYear();
        String yearCode = "FY" + currentYear;
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);
        
        FiscalYear fiscalYear = createFiscalYear(organizationId, yearCode, startDate, endDate);
        generateMonthlyPeriods(fiscalYear.getId(), organizationId);
        
        log.info("Created current fiscal year {} with 12 monthly periods for organization: {}", yearCode, organizationId);
        return fiscalYear;
    }
    
    @Transactional
    public FiscalYear closeFiscalYear(UUID fiscalYearId, UUID closedBy) {
        FiscalYear fiscalYear = getFiscalYearById(fiscalYearId);
        
        if (fiscalYear.getIsClosed()) {
            throw new RuntimeException("Fiscal year is already closed");
        }
        
        // Check if all periods are closed
        List<Period> openPeriods = periodRepository.findByFiscalYearIdOrderByPeriodNumber(fiscalYearId)
            .stream()
            .filter(p -> "OPEN".equals(p.getStatus()))
            .toList();
        
        if (!openPeriods.isEmpty()) {
            throw new RuntimeException("Cannot close fiscal year - there are " + openPeriods.size() + " open periods");
        }
        
        fiscalYear.setIsClosed(true);
        fiscalYear.setClosedAt(LocalDateTime.now());
        fiscalYear.setClosedBy(closedBy);
        
        fiscalYear = fiscalYearRepository.save(fiscalYear);
        log.info("Closed fiscal year: {}", fiscalYear.getYearCode());
        
        return fiscalYear;
    }
}

