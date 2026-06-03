package com.easyops.hr.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomReportBuilderService {

    private static final Logger log = LoggerFactory.getLogger(CustomReportBuilderService.class);
    
    private final ProvidentFundReportingService providentFundReportingService;
    
    /**
     * Build custom report based on parameters
     */
    public Map<String, Object> buildCustomReport(Map<String, Object> reportConfig) {
        log.info("Building custom report with config: {}", reportConfig);
        
        String reportType = (String) reportConfig.get("reportType");
        UUID organizationId = UUID.fromString(reportConfig.get("organizationId").toString());
        Map<String, Object> filters = (Map<String, Object>) reportConfig.getOrDefault("filters", new HashMap<>());
        List<String> fields = (List<String>) reportConfig.getOrDefault("fields", List.of());
        
        Map<String, Object> report = new HashMap<>();
        report.put("reportType", reportType);
        report.put("organizationId", organizationId);
        report.put("generatedAt", LocalDate.now());
        
        switch (reportType) {
            case "provident_fund_executive":
                report.put("data", providentFundReportingService.getExecutiveDashboard(organizationId));
                break;
            case "provident_fund_compliance":
                LocalDate startDate = filters.containsKey("startDate") 
                        ? LocalDate.parse(filters.get("startDate").toString())
                        : LocalDate.now().minusMonths(6);
                LocalDate endDate = filters.containsKey("endDate")
                        ? LocalDate.parse(filters.get("endDate").toString())
                        : LocalDate.now();
                report.put("data", providentFundReportingService.getComplianceReport(
                        organizationId, startDate, endDate));
                break;
            case "provident_fund_cost":
                Integer year = filters.containsKey("year")
                        ? Integer.parseInt(filters.get("year").toString())
                        : LocalDate.now().getYear();
                report.put("data", providentFundReportingService.getCostAnalysisReport(organizationId, year));
                break;
            default:
                report.put("error", "Unknown report type: " + reportType);
        }
        
        // Apply field filtering if specified
        if (!fields.isEmpty() && report.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) report.get("data");
            Map<String, Object> filteredData = new HashMap<>();
            fields.forEach(field -> {
                if (data.containsKey(field)) {
                    filteredData.put(field, data.get(field));
                }
            });
            report.put("data", filteredData);
        }
        
        return report;
    }
    
    /**
     * Get available report types
     */
    public List<Map<String, Object>> getAvailableReportTypes() {
        return List.of(
                Map.of("type", "provident_fund_executive", "name", "Provident Fund Executive Dashboard",
                        "category", "provident_fund", "description", "Executive-level Provident Fund overview"),
                Map.of("type", "provident_fund_compliance", "name", "Provident Fund Compliance Report",
                        "category", "provident_fund", "description", "Compliance status and records"),
                Map.of("type", "provident_fund_cost", "name", "Provident Fund Cost Analysis",
                        "category", "provident_fund", "description", "Cost analysis and breakdown")
        );
    }
}

