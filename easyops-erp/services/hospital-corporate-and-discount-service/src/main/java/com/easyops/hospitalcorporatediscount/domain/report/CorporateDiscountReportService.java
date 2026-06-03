package com.easyops.hospitalcorporatediscount.domain.report;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateUtilizationResponse;
import com.easyops.hospitalcorporatediscount.api.dto.DiscountSummaryResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CorporateUtilizationResponse.CorporateUtilizationItem;
import com.easyops.hospitalcorporatediscount.api.dto.DiscountSummaryResponse.DiscountSummaryItem;
import com.easyops.hospitalcorporatediscount.domain.discount.DiscountDecisionRepository;
import com.easyops.hospitalcorporatediscount.domain.discount.DiscountSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporateDiscountReportService {

    private final DiscountDecisionRepository decisionRepository;
    private final DiscountSchemeRepository schemeRepository;

    /**
     * Corporate utilization: count of discount decisions per corporate in period.
     * If corporateId is present, returns single utilization; otherwise returns one item per corporate.
     */
    public CorporateUtilizationResponse getCorporateUtilization(
            Optional<UUID> corporateId,
            LocalDate from,
            LocalDate to
    ) {
        OffsetDateTime fromInclusive = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toEndOfDay = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        CorporateUtilizationResponse response = new CorporateUtilizationResponse();
        response.setFrom(from.toString());
        response.setTo(to.toString());

        if (corporateId.isPresent()) {
            long count = decisionRepository.countByCorporateClientIdAndCreatedAtBetween(
                    corporateId.get(), fromInclusive, toEndOfDay);
            CorporateUtilizationItem item = new CorporateUtilizationItem();
            item.setCorporateId(corporateId.get());
            item.setDecisionCount(count);
            response.setSingle(item);
        } else {
            List<Object[]> rows = decisionRepository.countByCorporateAndCreatedAtBetween(fromInclusive, toEndOfDay);
            List<CorporateUtilizationItem> items = new ArrayList<>();
            for (Object[] row : rows) {
                CorporateUtilizationItem item = new CorporateUtilizationItem();
                item.setCorporateId((UUID) row[0]);
                item.setDecisionCount(((Number) row[1]).longValue());
                items.add(item);
            }
            response.setByCorporate(items);
        }
        return response;
    }

    /**
     * Discount summary: total discount amount (and count) by scheme in period.
     * If schemeId is present, returns single summary; otherwise returns one item per scheme.
     */
    public DiscountSummaryResponse getDiscountSummary(
            LocalDate from,
            LocalDate to,
            Optional<UUID> schemeId
    ) {
        OffsetDateTime fromInclusive = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toEndOfDay = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        DiscountSummaryResponse response = new DiscountSummaryResponse();
        response.setFrom(from.toString());
        response.setTo(to.toString());

        if (schemeId.isPresent()) {
            BigDecimal total = decisionRepository.sumDiscountAmountByDiscountSchemeIdAndCreatedAtBetween(
                    schemeId.get(), fromInclusive, toEndOfDay);
            long count = decisionRepository.countByDiscountSchemeIdAndCreatedAtBetween(
                    schemeId.get(), fromInclusive, toEndOfDay);
            DiscountSummaryItem item = new DiscountSummaryItem();
            item.setSchemeId(schemeId.get());
            item.setSchemeCode(schemeRepository.findById(schemeId.get()).map(s -> s.getCode()).orElse(null));
            item.setTotalAmount(total != null ? total : BigDecimal.ZERO);
            item.setDecisionCount(count);
            response.setSingle(item);
        } else {
            List<Object[]> rows = decisionRepository.sumAmountAndCountBySchemeAndCreatedAtBetween(fromInclusive, toEndOfDay);
            List<DiscountSummaryItem> items = new ArrayList<>();
            for (Object[] row : rows) {
                DiscountSummaryItem item = new DiscountSummaryItem();
                item.setSchemeId((UUID) row[0]);
                item.setSchemeCode(schemeRepository.findById((UUID) row[0]).map(s -> s.getCode()).orElse(null));
                item.setTotalAmount(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                item.setDecisionCount(row[2] != null ? ((Number) row[2]).longValue() : 0L);
                items.add(item);
            }
            response.setByScheme(items);
        }
        return response;
    }
}
