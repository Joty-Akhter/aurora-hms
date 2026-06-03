package com.easyops.hospitalcorporatediscount.domain.coverage;

import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageItemRequest;
import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageItemResponse;
import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageRequest;
import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoverageEvaluationService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final CoverageRuleLoader coverageRuleLoader;

    public EvaluateCoverageResponse evaluate(EvaluateCoverageRequest request) {
        EvaluateCoverageResponse response = new EvaluateCoverageResponse();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            response.setItems(List.of());
            response.setTotalCovered(BigDecimal.ZERO);
            response.setTotalPatientShare(BigDecimal.ZERO);
            response.setTotalCorporateShare(BigDecimal.ZERO);
            return response;
        }

        List<CoverageRule> rules = coverageRuleLoader.loadForContract(request.getCorporateContractId());
        String visitType = request.getVisitType();

        List<EvaluateCoverageItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalCovered = BigDecimal.ZERO;
        BigDecimal totalPatient = BigDecimal.ZERO;
        BigDecimal totalCorporate = BigDecimal.ZERO;

        int index = 0;
        for (EvaluateCoverageItemRequest item : request.getItems()) {
            BigDecimal lineGross = item.getBasePrice().multiply(item.getQuantity()).setScale(SCALE, ROUNDING);
            Optional<CoverageRule> best = selectBestRule(rules, item, visitType);

            EvaluateCoverageItemResponse line = new EvaluateCoverageItemResponse();
            line.setLineIndex(index);
            line.setServiceCode(item.getServiceCode());

            if (best.isEmpty()) {
                line.setCoveredPercent(BigDecimal.ZERO);
                line.setCoveredAmount(BigDecimal.ZERO);
                line.setCorporateShare(BigDecimal.ZERO);
                line.setPatientShare(lineGross);
                line.setMaxApplicable(null);
                line.setRuleId(null);
            } else {
                CoverageRule rule = best.get();
                BigDecimal deductible = rule.getDeductibleAmount() != null ? rule.getDeductibleAmount() : BigDecimal.ZERO;
                BigDecimal baseForCoverage = lineGross.subtract(deductible).max(BigDecimal.ZERO);
                BigDecimal rawCovered = baseForCoverage.multiply(rule.getCoveragePercent())
                        .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                BigDecimal coveredAmount = rule.getMaxAmount() != null
                        ? rawCovered.min(rule.getMaxAmount())
                        : rawCovered;

                BigDecimal coPay = rule.getCoPayPercent() != null ? rule.getCoPayPercent() : BigDecimal.ZERO;
                BigDecimal corporateShare = coveredAmount.multiply(BigDecimal.valueOf(100).subtract(coPay))
                        .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                BigDecimal patientShare = lineGross.subtract(corporateShare);

                line.setCoveredPercent(rule.getCoveragePercent());
                line.setCoveredAmount(coveredAmount);
                line.setCorporateShare(corporateShare);
                line.setPatientShare(patientShare);
                line.setMaxApplicable(rule.getMaxAmount());
                line.setRuleId(rule.getId());

                totalCovered = totalCovered.add(coveredAmount);
                totalPatient = totalPatient.add(patientShare);
                totalCorporate = totalCorporate.add(corporateShare);
            }

            itemResponses.add(line);
            if (best.isEmpty()) {
                totalPatient = totalPatient.add(lineGross);
            }
            index++;
        }

        response.setItems(itemResponses);
        response.setTotalCovered(totalCovered.setScale(SCALE, ROUNDING));
        response.setTotalPatientShare(totalPatient.setScale(SCALE, ROUNDING));
        response.setTotalCorporateShare(totalCorporate.setScale(SCALE, ROUNDING));
        return response;
    }

    private Optional<CoverageRule> selectBestRule(List<CoverageRule> rules, EvaluateCoverageItemRequest item, String visitType) {
        return rules.stream()
                .filter(r -> matchesVisit(r, visitType))
                .filter(r -> matchesScope(r, item))
                .max(Comparator.comparingInt(this::scopePriority));
    }

    private int scopePriority(CoverageRule r) {
        if (r.getScopeType() == null) {
            return 0;
        }
        return switch (r.getScopeType().trim().toUpperCase()) {
            case "SERVICE" -> 4;
            case "SERVICE_GROUP" -> 3;
            case "DEPARTMENT" -> 2;
            case "ALL" -> 1;
            default -> 0;
        };
    }

    private boolean matchesVisit(CoverageRule r, String visitType) {
        if (r.getApplicableVisitTypes() == null || r.getApplicableVisitTypes().isBlank()) {
            return true;
        }
        if (visitType == null || visitType.isBlank()) {
            return true;
        }
        Set<String> allowed = Arrays.stream(r.getApplicableVisitTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return allowed.contains(visitType.trim().toUpperCase());
    }

    private boolean matchesScope(CoverageRule r, EvaluateCoverageItemRequest item) {
        if (r.getScopeType() == null) {
            return false;
        }
        String st = r.getScopeType().trim().toUpperCase();
        String sv = r.getScopeValue() != null ? r.getScopeValue().trim() : "";
        return switch (st) {
            case "ALL" -> true;
            case "SERVICE" -> item.getServiceCode() != null && item.getServiceCode().trim().equalsIgnoreCase(sv);
            case "SERVICE_GROUP" -> item.getServiceGroupId() != null
                    && item.getServiceGroupId().toString().equalsIgnoreCase(sv);
            case "DEPARTMENT" -> item.getDepartmentId() != null
                    && item.getDepartmentId().toString().equalsIgnoreCase(sv);
            default -> false;
        };
    }
}
