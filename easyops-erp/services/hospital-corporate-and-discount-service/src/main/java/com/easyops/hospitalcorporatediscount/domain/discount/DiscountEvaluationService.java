package com.easyops.hospitalcorporatediscount.domain.discount;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountEvaluationService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final DiscountSchemeService discountSchemeService;
    private final DiscountApprovalLevelRepository levelRepository;

    public EvaluateDiscountsResponse evaluate(EvaluateDiscountsRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            EvaluateDiscountsResponse empty = new EvaluateDiscountsResponse();
            empty.setApplicableSchemes(List.of());
            empty.setRecommendedTotalDiscount(BigDecimal.ZERO);
            empty.setRequiresApproval(false);
            empty.setMessage("No items to evaluate");
            return empty;
        }

        BigDecimal totalBill = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(i.getQuantity()).setScale(SCALE, ROUNDING))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalBill.compareTo(BigDecimal.ZERO) <= 0) {
            EvaluateDiscountsResponse empty = new EvaluateDiscountsResponse();
            empty.setApplicableSchemes(List.of());
            empty.setRecommendedTotalDiscount(BigDecimal.ZERO);
            empty.setRequiresApproval(false);
            return empty;
        }

        Set<String> itemServiceCodes = request.getItems().stream()
                .map(i -> i.getServiceCode() != null ? i.getServiceCode().trim() : "")
                .collect(Collectors.toSet());

        List<DiscountScheme> schemes = discountSchemeService.getActiveSchemesForEvaluation(
                request.getCorporateClientId(), request.getVisitType(), request.getDepartmentId());

        List<DiscountScheme> applicable = new ArrayList<>();
        for (DiscountScheme scheme : schemes) {
            if (scheme.getServiceCode() != null && !scheme.getServiceCode().isBlank()) {
                if (!itemServiceCodes.contains(scheme.getServiceCode().trim())) {
                    continue;
                }
            }
            if (scheme.getPatientCategory() != null && !scheme.getPatientCategory().isBlank()) {
                continue;
            }
            if (request.getRequestedSchemeId() != null && !request.getRequestedSchemeId().equals(scheme.getId())) {
                continue;
            }
            applicable.add(scheme);
        }

        List<ApplicableSchemeDto> dtos = new ArrayList<>();
        BigDecimal recommendedTotal = BigDecimal.ZERO;
        boolean anyRequiresApproval = false;

        for (DiscountScheme scheme : applicable) {
            BigDecimal raw;
            if ("PERCENT".equalsIgnoreCase(scheme.getDiscountType())) {
                raw = totalBill.multiply(scheme.getDiscountValue()).divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
            } else {
                raw = scheme.getDiscountValue().min(totalBill);
            }
            BigDecimal capped = raw;
            if (scheme.getMaxDiscountPercent() != null && scheme.getMaxDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxByPercent = totalBill.multiply(scheme.getMaxDiscountPercent()).divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                capped = capped.min(maxByPercent);
            }
            if (scheme.getMaxDiscountAmount() != null && scheme.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                capped = capped.min(scheme.getMaxDiscountAmount());
            }

            ApplicableSchemeDto dto = new ApplicableSchemeDto();
            dto.setSchemeId(scheme.getId());
            dto.setSchemeCode(scheme.getCode());
            dto.setRecommendedPercent("PERCENT".equalsIgnoreCase(scheme.getDiscountType()) ? scheme.getDiscountValue() : null);
            dto.setRecommendedAmount(raw);
            dto.setCappedAmount(capped);
            dto.setRequiresApproval(Boolean.TRUE.equals(scheme.getRequiresApproval()));

            if (Boolean.TRUE.equals(scheme.getRequiresApproval())) {
                levelRepository.findByDiscountSchemeIdOrderBySortOrderAsc(scheme.getId()).stream()
                        .findFirst()
                        .ifPresent(level -> dto.setRequiredApprovalLevel(level.getRoleOrGroupId()));
                anyRequiresApproval = true;
            }

            dtos.add(dto);
            if (capped.compareTo(recommendedTotal) > 0) {
                recommendedTotal = capped;
            }
        }

        if (request.getRequestedSchemeId() != null && dtos.isEmpty()) {
            EvaluateDiscountsResponse resp = new EvaluateDiscountsResponse();
            resp.setApplicableSchemes(List.of());
            resp.setRecommendedTotalDiscount(BigDecimal.ZERO);
            resp.setRequiresApproval(false);
            resp.setMessage("Requested scheme not found or not applicable");
            return resp;
        }

        EvaluateDiscountsResponse response = new EvaluateDiscountsResponse();
        response.setApplicableSchemes(dtos);
        response.setRecommendedTotalDiscount(recommendedTotal);
        response.setRequiresApproval(anyRequiresApproval);
        return response;
    }
}
