package com.easyops.manufacturing.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Posts manufacturing cost events to accounting-service via the standard journal integration API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingIntegrationService {

    static final String ACCT_RAW_MATERIALS = "1141";
    static final String ACCT_WIP = "1142";
    static final String ACCT_FINISHED_GOODS = "1143";
    static final String ACCT_ACCRUED_EXPENSES = "2020";
    static final String ACCT_MATERIALS_COST = "5010";

    private final RestTemplate restTemplate;

    @Value("${integration.accounting-service.url:http://ACCOUNTING-SERVICE}")
    private String accountingServiceUrl;

    @Value("${integration.accounting.system-user-id:00000000-0000-0000-0000-000000000001}")
    private String systemUserId;

    public void postWIPEntry(String workOrderNumber, UUID productId, BigDecimal quantity,
                            BigDecimal materialCost, BigDecimal laborCost, BigDecimal overheadCost,
                            UUID organizationId) {
        BigDecimal total = materialCost.add(laborCost).add(overheadCost);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(journalLine(ACCT_WIP, total, BigDecimal.ZERO, "WIP - WO " + workOrderNumber));
        if (materialCost.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(journalLine(ACCT_RAW_MATERIALS, BigDecimal.ZERO, materialCost, "Materials to WIP"));
        }
        BigDecimal laborAndOverhead = laborCost.add(overheadCost);
        if (laborAndOverhead.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(journalLine(ACCT_ACCRUED_EXPENSES, BigDecimal.ZERO, laborAndOverhead, "Labor/overhead to WIP"));
        }
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_WIP",
                "WIP - Work Order " + workOrderNumber, lines);
        log.info("Posted WIP entry for work order: {}", workOrderNumber);
    }

    public void postMaterialIssuance(String workOrderNumber, UUID componentId, BigDecimal quantity,
                                    BigDecimal unitCost, UUID organizationId) {
        BigDecimal totalCost = quantity.multiply(unitCost);
        if (totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Map<String, Object>> lines = List.of(
                journalLine(ACCT_WIP, totalCost, BigDecimal.ZERO, "Material issue - WO " + workOrderNumber),
                journalLine(ACCT_RAW_MATERIALS, BigDecimal.ZERO, totalCost, "Raw materials issued")
        );
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_MATERIAL",
                "Material Issue - WO " + workOrderNumber, lines);
        log.info("Posted material issuance for work order: {}", workOrderNumber);
    }

    public void postLaborCost(String workOrderNumber, BigDecimal laborCost, UUID organizationId) {
        if (laborCost == null || laborCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Map<String, Object>> lines = List.of(
                journalLine(ACCT_WIP, laborCost, BigDecimal.ZERO, "Labor - WO " + workOrderNumber),
                journalLine(ACCT_ACCRUED_EXPENSES, BigDecimal.ZERO, laborCost, "Labor accrual")
        );
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_LABOR",
                "Labor Cost - WO " + workOrderNumber, lines);
        log.info("Posted labor cost for work order: {}", workOrderNumber);
    }

    public void postFinishedGoodsCompletion(String workOrderNumber, UUID productId, BigDecimal quantity,
                                           BigDecimal totalCost, UUID organizationId) {
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Map<String, Object>> lines = List.of(
                journalLine(ACCT_FINISHED_GOODS, totalCost, BigDecimal.ZERO, "Finished goods - WO " + workOrderNumber),
                journalLine(ACCT_WIP, BigDecimal.ZERO, totalCost, "WIP relief on completion")
        );
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_COMPLETION",
                "Finished Goods - WO " + workOrderNumber, lines);
        log.info("Posted finished goods completion for work order: {}", workOrderNumber);
    }

    public void postManufacturingVariance(String workOrderNumber, BigDecimal standardCost,
                                         BigDecimal actualCost, UUID organizationId) {
        BigDecimal variance = actualCost.subtract(standardCost);
        if (variance.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        List<Map<String, Object>> lines;
        if (variance.compareTo(BigDecimal.ZERO) > 0) {
            lines = List.of(
                    journalLine(ACCT_MATERIALS_COST, variance, BigDecimal.ZERO, "Unfavorable variance - WO " + workOrderNumber),
                    journalLine(ACCT_WIP, BigDecimal.ZERO, variance, "WIP variance relief")
            );
        } else {
            BigDecimal amount = variance.abs();
            lines = List.of(
                    journalLine(ACCT_WIP, amount, BigDecimal.ZERO, "Favorable variance - WO " + workOrderNumber),
                    journalLine(ACCT_MATERIALS_COST, BigDecimal.ZERO, amount, "WIP variance relief")
            );
        }
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_VARIANCE",
                "Manufacturing Variance - WO " + workOrderNumber, lines);
        log.info("Posted manufacturing variance for work order: {} - variance: {}", workOrderNumber, variance);
    }

    public void postScrapCost(String workOrderNumber, UUID productId, BigDecimal scrapQuantity,
                             BigDecimal scrapCost, UUID organizationId) {
        if (scrapCost == null || scrapCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Map<String, Object>> lines = List.of(
                journalLine(ACCT_MATERIALS_COST, scrapCost, BigDecimal.ZERO, "Scrap - WO " + workOrderNumber),
                journalLine(ACCT_WIP, BigDecimal.ZERO, scrapCost, "Scrap from WIP")
        );
        postJournal(organizationId, workOrderNumber, "MANUFACTURING_SCRAP",
                "Scrap Cost - WO " + workOrderNumber, lines);
        log.info("Posted scrap cost for work order: {}", workOrderNumber);
    }

    private void postJournal(UUID organizationId, String workOrderNumber, String journalType,
                             String description, List<Map<String, Object>> lines) {
        if (!isBalanced(lines)) {
            log.error("Skipping unbalanced manufacturing journal ({}): debits != credits", journalType);
            return;
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("organizationId", organizationId.toString());
            request.put("entryDate", LocalDate.now().toString());
            request.put("journalType", journalType);
            request.put("referenceId", workOrderNumber);
            request.put("description", description);
            request.put("status", "POSTED");
            request.put("lines", lines);

            String url = accountingServiceUrl + "/api/accounting/journal-entries";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", systemUserId);
            restTemplate.postForEntity(url, new HttpEntity<>(request, headers), Map.class);
        } catch (Exception e) {
            log.error("Failed to post manufacturing journal ({}): {}", journalType, e.getMessage());
        }
    }

    static boolean isBalanced(List<Map<String, Object>> lines) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            Object d = line.get("debitAmount");
            Object c = line.get("creditAmount");
            debits = debits.add(d instanceof BigDecimal bd ? bd : new BigDecimal(d != null ? d.toString() : "0"));
            credits = credits.add(c instanceof BigDecimal bc ? bc : new BigDecimal(c != null ? c.toString() : "0"));
        }
        return debits.compareTo(credits) == 0 && debits.compareTo(BigDecimal.ZERO) > 0;
    }

    private static Map<String, Object> journalLine(String accountCode, BigDecimal debit, BigDecimal credit, String desc) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountCode", accountCode);
        line.put("debitAmount", debit);
        line.put("creditAmount", credit);
        line.put("description", desc);
        return line;
    }
}
