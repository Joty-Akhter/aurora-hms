package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.response.ConsumptionReportItemResponse;
import com.easyops.hospitalpharmacy.dto.response.ControlledSubstanceDispenseRowResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyStockItemResponse;
import com.easyops.hospitalpharmacy.dto.response.SalesSummaryResponse;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentMovementResponse;
import com.easyops.hospitalpharmacy.dto.response.StockOverrideLineReportResponse;
import com.easyops.hospitalpharmacy.dto.response.StockTransferMovementResponse;
import com.easyops.hospitalpharmacy.entity.DispenseLine;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.PharmacyStock;
import com.easyops.hospitalpharmacy.entity.StockMovement;
import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import com.easyops.hospitalpharmacy.repository.DispenseLineRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyLocationRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyStockRepository;
import com.easyops.hospitalpharmacy.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PharmacyReportService {

    private final PharmacyLocationRepository pharmacyLocationRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final DispenseLineRepository dispenseLineRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PharmacyIntegrationProperties integrationProperties;

    @Transactional(readOnly = true)
    public List<PharmacyStockItemResponse> getNearExpiryStock(
            UUID pharmacyLocationId, int days, String productCode, String companyCode) {
        LocalDate now = LocalDate.now();
        LocalDate threshold = now.plusDays(days);

        List<PharmacyStock> allStock;
        if (pharmacyLocationId != null) {
            PharmacyLocation location = getLocation(pharmacyLocationId);
            allStock = pharmacyStockRepository.findByPharmacyLocation(location);
        } else {
            allStock = pharmacyStockRepository.findAll();
        }

        String prodQ = normalizeFilter(productCode);
        String compQ = normalizeFilter(companyCode);

        return allStock.stream()
                .filter(s -> s.getExpiryDate() != null &&
                        !s.getExpiryDate().isBefore(now) &&
                        !s.getExpiryDate().isAfter(threshold))
                .filter(s -> matchesDrugFilter(s.getDrug(), prodQ, compQ))
                .map(s -> PharmacyStockItemResponse.builder()
                        .stockId(s.getId())
                        .drugId(s.getDrug().getId())
                        .genericName(s.getDrug().getGenericName())
                        .brandName(s.getDrug().getBrandName())
                        .strength(s.getDrug().getStrength())
                        .form(s.getDrug().getForm())
                        .route(s.getDrug().getRoute())
                        .batchNumber(s.getBatchNumber())
                        .expiryDate(s.getExpiryDate())
                        .quantityOnHand(s.getQuantityOnHand())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsumptionReportItemResponse> getConsumptionReport(
            UUID pharmacyLocationId, LocalDate from, LocalDate to, String productCode, String companyCode) {
        getLocation(pharmacyLocationId);
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<DispenseLine> lines = dispenseLineRepository.findDispensedLinesForConsumptionReport(
                pharmacyLocationId, fromTs, toTs);

        String prodQ = normalizeFilter(productCode);
        String compQ = normalizeFilter(companyCode);

        Map<UUID, BigDecimal> byDrug = new HashMap<>();
        Map<UUID, Drug> drugSample = new HashMap<>();
        for (DispenseLine dl : lines) {
            Drug d = dl.getDrug();
            if (!matchesDrugFilter(d, prodQ, compQ)) {
                continue;
            }
            UUID drugId = d.getId();
            byDrug.merge(drugId, dl.getQuantityDispensed(), BigDecimal::add);
            drugSample.putIfAbsent(drugId, d);
        }

        return byDrug.entrySet().stream()
                .map(e -> {
                    UUID drugId = e.getKey();
                    BigDecimal total = e.getValue();
                    Drug sample = drugSample.get(drugId);
                    return ConsumptionReportItemResponse.builder()
                            .drugId(drugId)
                            .genericName(sample.getGenericName())
                            .brandName(sample.getBrandName())
                            .strength(sample.getStrength())
                            .form(sample.getForm())
                            .route(sample.getRoute())
                            .totalQuantityIssued(total)
                            .build();
                })
                .sorted(Comparator.comparing(ConsumptionReportItemResponse::getGenericName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(
            UUID pharmacyLocationId, LocalDate from, LocalDate to, String productCode, String companyCode) {
        List<ConsumptionReportItemResponse> byDrug = getConsumptionReport(
                pharmacyLocationId, from, to, productCode, companyCode);
        BigDecimal total = byDrug.stream()
                .map(ConsumptionReportItemResponse::getTotalQuantityIssued)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal defaultPrice = integrationProperties.getBilling().getDefaultUnitPrice();
        boolean estimate = defaultPrice != null && defaultPrice.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal estimatedTotal = null;
        if (estimate) {
            estimatedTotal = BigDecimal.ZERO;
            for (ConsumptionReportItemResponse r : byDrug) {
                BigDecimal qty = r.getTotalQuantityIssued() != null ? r.getTotalQuantityIssued() : BigDecimal.ZERO;
                BigDecimal lineEst = qty.multiply(defaultPrice);
                r.setEstimatedRevenue(lineEst);
                estimatedTotal = estimatedTotal.add(lineEst);
            }
        }

        return SalesSummaryResponse.builder()
                .byDrug(byDrug)
                .totalQuantityIssued(total)
                .distinctDrugCount(byDrug.size())
                .estimatedRevenueTotal(estimatedTotal)
                .revenueEstimateUnitPrice(estimate ? defaultPrice : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ControlledSubstanceDispenseRowResponse> getControlledSubstanceRegister(
            UUID pharmacyLocationId, LocalDate from, LocalDate to, String productCode, String companyCode) {
        getLocation(pharmacyLocationId);
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        String prodQ = normalizeFilter(productCode);
        String compQ = normalizeFilter(companyCode);
        return dispenseLineRepository.findControlledDispenseLinesForRegister(pharmacyLocationId, fromTs, toTs).stream()
                .filter(dl -> matchesDrugFilter(dl.getDrug(), prodQ, compQ))
                .map(this::toControlledRow)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockOverrideLineReportResponse> getStockOverrideLines(
            UUID pharmacyLocationId, LocalDate from, LocalDate to, String productCode, String companyCode) {
        getLocation(pharmacyLocationId);
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        String prodQ = normalizeFilter(productCode);
        String compQ = normalizeFilter(companyCode);
        return dispenseLineRepository.findStockOverrideLinesForReport(pharmacyLocationId, fromTs, toTs).stream()
                .filter(dl -> matchesDrugFilter(dl.getDrug(), prodQ, compQ))
                .map(this::toStockOverrideRow)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentMovementResponse> getStockAdjustmentsReport(
            UUID pharmacyLocationId, LocalDate from, LocalDate to) {
        getLocation(pharmacyLocationId);
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return stockMovementRepository.findAdjustmentsForReport(pharmacyLocationId, fromTs, toTs)
                .stream().map(this::toAdjustmentResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockTransferMovementResponse> getTransferHistory(
            UUID pharmacyLocationId, LocalDate from, LocalDate to) {
        getLocation(pharmacyLocationId);
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return stockMovementRepository.findMovementsFiltered(
                        pharmacyLocationId, null, null, null, fromTs, toTs,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(m -> m.getMovementType().startsWith("transfer_"))
                .map(this::toTransferResponse)
                .collect(Collectors.toList());
    }

    private StockAdjustmentMovementResponse toAdjustmentResponse(StockMovement m) {
        return StockAdjustmentMovementResponse.builder()
                .movementId(m.getId())
                .pharmacyLocationId(m.getPharmacyLocation().getId())
                .pharmacyLocationName(m.getPharmacyLocation().getName())
                .drugId(m.getDrug().getId())
                .genericName(m.getDrug().getGenericName())
                .brandName(m.getDrug().getBrandName())
                .quantityDelta(m.getQuantity())
                .batchNumber(m.getBatchNumber())
                .movementTime(m.getMovementTime())
                .reason(m.getReasonCode() != null ? m.getReasonCode() : m.getNotes())
                .build();
    }

    private StockTransferMovementResponse toTransferResponse(StockMovement m) {
        return StockTransferMovementResponse.builder()
                .movementId(m.getId())
                .pharmacyLocationId(m.getPharmacyLocation().getId())
                .pharmacyLocationName(m.getPharmacyLocation().getName())
                .drugId(m.getDrug().getId())
                .genericName(m.getDrug().getGenericName())
                .brandName(m.getDrug().getBrandName())
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .batchNumber(m.getBatchNumber())
                .movementTime(m.getMovementTime())
                .notes(m.getNotes())
                .build();
    }

    private static String normalizeFilter(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t.toLowerCase(Locale.ROOT);
    }

    private static boolean matchesDrugFilter(Drug d, String productLower, String companyLower) {
        if (productLower != null) {
            if (!containsIgnoreCase(d.getGenericName(), productLower)
                    && !containsIgnoreCase(d.getBrandName(), productLower)) {
                return false;
            }
        }
        if (companyLower != null) {
            String manufacturerName = d.getManufacturer() != null ? d.getManufacturer().getName() : null;
            String manufacturerShortCode = d.getManufacturer() != null ? d.getManufacturer().getShortCode() : null;
            if (!containsIgnoreCase(d.getBrandName(), companyLower)
                    && !containsIgnoreCase(manufacturerName, companyLower)
                    && !containsIgnoreCase(manufacturerShortCode, companyLower)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * WS-L3 — CSV export for consumption (UTF-8). Escapes commas and quotes in text fields.
     */
    public static String formatConsumptionCsv(List<ConsumptionReportItemResponse> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("drugId,genericName,brandName,strength,form,route,totalQuantityIssued\n");
        for (ConsumptionReportItemResponse r : rows) {
            sb.append(csvCell(r.getDrugId())).append(',');
            sb.append(csvCell(r.getGenericName())).append(',');
            sb.append(csvCell(r.getBrandName())).append(',');
            sb.append(csvCell(r.getStrength())).append(',');
            sb.append(csvCell(r.getForm())).append(',');
            sb.append(csvCell(r.getRoute())).append(',');
            sb.append(csvCell(r.getTotalQuantityIssued())).append('\n');
        }
        return sb.toString();
    }

    private static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private ControlledSubstanceDispenseRowResponse toControlledRow(DispenseLine dl) {
        return ControlledSubstanceDispenseRowResponse.builder()
                .dispenseLineId(dl.getId())
                .dispenseOrderId(dl.getDispenseOrder().getId())
                .patientId(dl.getDispenseOrder().getPatientId())
                .pharmacyLocationId(dl.getDispenseOrder().getPharmacyLocation().getId())
                .pharmacyLocationName(dl.getDispenseOrder().getPharmacyLocation().getName())
                .drugId(dl.getDrug().getId())
                .genericName(dl.getDrug().getGenericName())
                .brandName(dl.getDrug().getBrandName())
                .controlledProfileCode(dl.getDrug().getControlledProfileCode())
                .quantityDispensed(dl.getQuantityDispensed())
                .batchNumber(dl.getBatchNumber())
                .lineStatus(dl.getStatus())
                .dispensedAt(dl.getCreatedAt())
                .witnessUserId(dl.getWitnessUserId())
                .build();
    }

    private StockOverrideLineReportResponse toStockOverrideRow(DispenseLine dl) {
        return StockOverrideLineReportResponse.builder()
                .dispenseLineId(dl.getId())
                .dispenseOrderId(dl.getDispenseOrder().getId())
                .pharmacyLocationId(dl.getDispenseOrder().getPharmacyLocation().getId())
                .pharmacyLocationName(dl.getDispenseOrder().getPharmacyLocation().getName())
                .drugId(dl.getDrug().getId())
                .genericName(dl.getDrug().getGenericName())
                .overrideReasonCode(dl.getOverrideReasonCode())
                .quantityDispensed(dl.getQuantityDispensed())
                .dispensedAt(dl.getCreatedAt())
                .build();
    }

    private PharmacyLocation getLocation(UUID id) {
        return pharmacyLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + id));
    }
}

