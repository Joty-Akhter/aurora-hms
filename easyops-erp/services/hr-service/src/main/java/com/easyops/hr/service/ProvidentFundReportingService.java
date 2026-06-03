package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProvidentFundReportingService {

    private static final Logger log = LoggerFactory.getLogger(ProvidentFundReportingService.class);
    
    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfInterestCalculationRepository epfInterestCalculationRepository;
    private final EpfComplianceRecordRepository epfComplianceRecordRepository;
    private final EpfWithdrawalRepository epfWithdrawalRepository;
    private final EmployeeRepository employeeRepository;
    
    /**
     * Executive dashboard for Provident Fund
     */
    public Map<String, Object> getExecutiveDashboard(UUID organizationId) {
        log.info("Generating executive dashboard for organization: {}", organizationId);
        
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId);
        List<EpfComplianceRecord> complianceRecords = epfComplianceRecordRepository
                .findByOrganizationId(organizationId);
        
        BigDecimal totalBalance = accounts.stream()
                .map(EpfAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalContributions = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEmployeeContributions = contributions.stream()
                .map(EpfContribution::getEmployeeContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEmployerContributions = contributions.stream()
                .map(EpfContribution::getEmployerContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long activeAccounts = accounts.stream().filter(EpfAccount::getIsActive).count();
        long totalEmployees = employeeRepository.findByOrganizationId(organizationId).size();
        BigDecimal participationRate = new BigDecimal(activeAccounts)
                .divide(new BigDecimal(totalEmployees > 0 ? totalEmployees : 1), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        
        long pendingCompliance = complianceRecords.stream()
                .filter(r -> "pending".equals(r.getStatus()))
                .count();
        
        long overdueCompliance = complianceRecords.stream()
                .filter(r -> "overdue".equals(r.getStatus()))
                .count();
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("organizationId", organizationId);
        dashboard.put("totalBalance", totalBalance);
        dashboard.put("totalContributions", totalContributions);
        dashboard.put("totalEmployeeContributions", totalEmployeeContributions);
        dashboard.put("totalEmployerContributions", totalEmployerContributions);
        dashboard.put("activeAccounts", activeAccounts);
        dashboard.put("totalEmployees", totalEmployees);
        dashboard.put("participationRate", participationRate);
        dashboard.put("pendingCompliance", pendingCompliance);
        dashboard.put("overdueCompliance", overdueCompliance);
        dashboard.put("complianceStatus", overdueCompliance > 0 ? "At Risk" : "Compliant");
        
        return dashboard;
    }
    
    /**
     * Manager reports for team Provident Fund
     */
    public Map<String, Object> getManagerTeamReport(UUID managerId, UUID departmentId, UUID organizationId) {
        log.info("Generating manager team report for manager: {}, department: {}", managerId, departmentId);
        
        List<Employee> teamMembers = employeeRepository
                .findByOrganizationIdAndDepartmentId(organizationId, departmentId);
        
        List<EpfAccount> teamAccounts = epfAccountRepository.findByOrganizationId(organizationId).stream()
                .filter(account -> teamMembers.stream()
                        .anyMatch(emp -> emp.getEmployeeId().equals(account.getEmployeeId())))
                .collect(Collectors.toList());
        
        BigDecimal teamTotalBalance = teamAccounts.stream()
                .map(EpfAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<EpfContribution> teamContributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(contrib -> teamMembers.stream()
                        .anyMatch(emp -> emp.getEmployeeId().equals(contrib.getEmployeeId())))
                .collect(Collectors.toList());
        
        BigDecimal teamTotalContributions = teamContributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> report = new HashMap<>();
        report.put("managerId", managerId);
        report.put("departmentId", departmentId);
        report.put("teamSize", teamMembers.size());
        report.put("enrolledMembers", teamAccounts.size());
        report.put("teamTotalBalance", teamTotalBalance);
        report.put("teamTotalContributions", teamTotalContributions);
        report.put("averageBalance", teamTotalBalance.divide(
                new BigDecimal(teamAccounts.size() > 0 ? teamAccounts.size() : 1), 
                2, RoundingMode.HALF_UP));
        report.put("teamMembers", teamMembers.stream().map(emp -> {
            Map<String, Object> member = new HashMap<>();
            member.put("employeeId", emp.getEmployeeId());
            member.put("name", emp.getName());
            EpfAccount account = teamAccounts.stream()
                    .filter(a -> a.getEmployeeId().equals(emp.getEmployeeId()))
                    .findFirst()
                    .orElse(null);
            member.put("hasAccount", account != null);
            member.put("balance", account != null ? account.getCurrentBalance() : BigDecimal.ZERO);
            return member;
        }).collect(Collectors.toList()));
        
        return report;
    }
    
    /**
     * Individual employee Provident Fund statement
     */
    public Map<String, Object> getEmployeeStatement(UUID employeeId, UUID epfAccountId, 
                                                    LocalDate startDate, LocalDate endDate) {
        log.info("Generating employee statement for employee: {}, account: {}", employeeId, epfAccountId);
        
        EpfAccount account = epfAccountRepository.findById(epfAccountId)
                .orElseThrow(() -> new RuntimeException("EPF account not found"));
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByEpfAccountId(epfAccountId).stream()
                .filter(c -> {
                    LocalDate contribDate = LocalDate.of(c.getContributionYear(), c.getContributionMonth(), 1);
                    return !contribDate.isBefore(startDate) && !contribDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        List<EpfWithdrawal> withdrawals = epfWithdrawalRepository
                .findByEpfAccountId(epfAccountId).stream()
                .filter(w -> w.getWithdrawalDate() != null &&
                        !w.getWithdrawalDate().isBefore(startDate) &&
                        !w.getWithdrawalDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        BigDecimal totalContributions = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate opening balance (current balance minus contributions in period)
        BigDecimal openingBalance = account.getCurrentBalance().subtract(totalContributions);
        
        Map<String, Object> statement = new HashMap<>();
        statement.put("employeeId", employeeId);
        statement.put("accountId", epfAccountId);
        statement.put("accountNumber", account.getEpfAccountNumber());
        statement.put("period", startDate + " to " + endDate);
        statement.put("openingBalance", openingBalance);
        statement.put("currentBalance", account.getCurrentBalance());
        statement.put("totalContributions", totalContributions);
        statement.put("contributions", contributions);
        statement.put("withdrawals", withdrawals);
        statement.put("closingBalance", account.getCurrentBalance());
        
        return statement;
    }
    
    /**
     * Compliance report
     */
    public Map<String, Object> getComplianceReport(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating compliance report for organization: {} from {} to {}", 
                organizationId, startDate, endDate);
        
        List<EpfComplianceRecord> records = epfComplianceRecordRepository
                .findByOrganizationId(organizationId).stream()
                .filter(r -> !r.getCompliancePeriodStart().isBefore(startDate) &&
                        !r.getCompliancePeriodStart().isAfter(endDate))
                .collect(Collectors.toList());
        
        long totalRecords = records.size();
        long filedRecords = records.stream().filter(r -> "filed".equals(r.getStatus())).count();
        long pendingRecords = records.stream().filter(r -> "pending".equals(r.getStatus())).count();
        long overdueRecords = records.stream().filter(r -> "overdue".equals(r.getStatus())).count();
        
        BigDecimal totalPenalties = records.stream()
                .map(EpfComplianceRecord::getPenaltyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> report = new HashMap<>();
        report.put("organizationId", organizationId);
        report.put("period", startDate + " to " + endDate);
        report.put("totalRecords", totalRecords);
        report.put("filedRecords", filedRecords);
        report.put("pendingRecords", pendingRecords);
        report.put("overdueRecords", overdueRecords);
        report.put("complianceRate", new BigDecimal(filedRecords)
                .divide(new BigDecimal(totalRecords > 0 ? totalRecords : 1), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP));
        report.put("totalPenalties", totalPenalties);
        report.put("records", records);
        
        return report;
    }
    
    /**
     * Cost analysis report
     */
    public Map<String, Object> getCostAnalysisReport(UUID organizationId, Integer year) {
        log.info("Generating cost analysis report for organization: {}, year: {}", organizationId, year);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(c -> c.getContributionYear().equals(year))
                .collect(Collectors.toList());
        
        BigDecimal totalEmployerCost = contributions.stream()
                .map(EpfContribution::getEmployerContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEmployeeCost = contributions.stream()
                .map(EpfContribution::getEmployeeContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = totalEmployerCost.add(totalEmployeeCost);
        
        long uniqueEmployees = contributions.stream()
                .map(EpfContribution::getEmployeeId)
                .distinct()
                .count();
        
        BigDecimal avgCostPerEmployee = totalCost
                .divide(new BigDecimal(uniqueEmployees > 0 ? uniqueEmployees : 1), 
                        2, RoundingMode.HALF_UP);
        
        // Monthly breakdown
        Map<Integer, BigDecimal> monthlyCosts = contributions.stream()
                .collect(Collectors.groupingBy(
                        EpfContribution::getContributionMonth,
                        Collectors.reducing(BigDecimal.ZERO,
                                EpfContribution::getEmployerContributionAmount,
                                BigDecimal::add)));
        
        Map<String, Object> report = new HashMap<>();
        report.put("organizationId", organizationId);
        report.put("year", year);
        report.put("totalEmployerCost", totalEmployerCost);
        report.put("totalEmployeeCost", totalEmployeeCost);
        report.put("totalCost", totalCost);
        report.put("averageCostPerEmployee", avgCostPerEmployee);
        report.put("totalEmployees", uniqueEmployees);
        report.put("monthlyBreakdown", monthlyCosts);
        
        return report;
    }
    
    /**
     * Trend analysis report
     */
    public Map<String, Object> getTrendAnalysisReport(UUID organizationId, Integer months) {
        log.info("Generating trend analysis report for organization: {}, months: {}", organizationId, months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(c -> {
                    LocalDate contribDate = LocalDate.of(c.getContributionYear(), c.getContributionMonth(), 1);
                    return !contribDate.isBefore(startDate) && !contribDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        // Group by month
        Map<String, BigDecimal> monthlyTrends = contributions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getContributionMonth() + "/" + c.getContributionYear(),
                        Collectors.reducing(BigDecimal.ZERO,
                                EpfContribution::getTotalContribution,
                                BigDecimal::add)));
        
        // Calculate growth rate
        List<BigDecimal> amounts = monthlyTrends.values().stream()
                .sorted()
                .collect(Collectors.toList());
        
        BigDecimal growthRate = BigDecimal.ZERO;
        if (amounts.size() >= 2) {
            BigDecimal first = amounts.get(0);
            BigDecimal last = amounts.get(amounts.size() - 1);
            if (first.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = last.subtract(first)
                        .divide(first, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("organizationId", organizationId);
        report.put("period", months + " months");
        report.put("monthlyTrends", monthlyTrends);
        report.put("growthRate", growthRate.setScale(2, RoundingMode.HALF_UP));
        report.put("trendDirection", growthRate.compareTo(BigDecimal.ZERO) > 0 ? "Increasing" : "Decreasing");
        
        return report;
    }
}

