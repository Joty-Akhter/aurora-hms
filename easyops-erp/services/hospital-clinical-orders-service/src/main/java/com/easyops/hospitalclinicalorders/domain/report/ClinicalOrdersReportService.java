package com.easyops.hospitalclinicalorders.domain.report;

import com.easyops.hospitalclinicalorders.api.dto.TatReportItem;
import com.easyops.hospitalclinicalorders.api.dto.VolumeReportItem;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClinicalOrdersReportService {

    private final ClinicalOrderRepository clinicalOrderRepository;

    public ClinicalOrdersReportService(ClinicalOrderRepository clinicalOrderRepository) {
        this.clinicalOrderRepository = clinicalOrderRepository;
    }

    /**
     * Turnaround time report: order created to result_available_at or performed_at (when COMPLETED), by order type.
     * from/to are required; orderType filter is optional.
     */
    @Transactional(readOnly = true)
    public List<TatReportItem> getTatReport(OffsetDateTime from, OffsetDateTime to, String orderType) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        List<Object[]> rows = clinicalOrderRepository.findTatAggregates(from, to, orderType);
        return rows.stream()
                .map(row -> new TatReportItem(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? ((Number) row[2]).doubleValue() : null))
                .collect(Collectors.toList());
    }

    /**
     * Volume report: count of orders in period, grouped by orderType or department.
     * from/to are required; groupBy must be "orderType" or "department".
     */
    @Transactional(readOnly = true)
    public List<VolumeReportItem> getVolumeReport(OffsetDateTime from, OffsetDateTime to, String groupBy) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        List<Object[]> rows = "department".equalsIgnoreCase(groupBy)
                ? clinicalOrderRepository.findVolumesByDepartment(from, to)
                : clinicalOrderRepository.findVolumesByOrderType(from, to);
        return rows.stream()
                .map(row -> new VolumeReportItem(
                        row[0] != null ? row[0].toString() : null,
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }
}
