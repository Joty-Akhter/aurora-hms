package com.easyops.manufacturing.service;

import com.easyops.manufacturing.repository.BomHeaderRepository;
import com.easyops.manufacturing.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturingAnalyticsService {

    private final WorkOrderRepository workOrderRepository;
    private final BomHeaderRepository bomHeaderRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get comprehensive manufacturing dashboard with all KPIs
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getManufacturingDashboard(UUID organizationId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Work Order Metrics
        dashboard.put("workOrders", getWorkOrderMetrics(organizationId));
        
        // Production Metrics
        dashboard.put("production", getProductionMetrics(organizationId));
        
        // Quality Metrics
        dashboard.put("quality", getQualityMetrics(organizationId));
        
        // Cost Metrics
        dashboard.put("costs", getCostMetrics(organizationId));
        
        // Performance Metrics
        dashboard.put("performance", getPerformanceMetrics(organizationId));
        
        log.info("Generated manufacturing dashboard for organization: {}", organizationId);
        return dashboard;
    }

    /**
     * Calculate OEE (Overall Equipment Effectiveness)
     * OEE = Availability × Performance × Quality
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateOEE(UUID organizationId, String workCenterCode, LocalDate date) {
        String sql = """
            SELECT 
                work_center_code,
                work_center_name,
                production_date,
                availability_percentage,
                performance_percentage,
                quality_percentage,
                oee_percentage,
                total_planned_time,
                total_actual_time,
                total_good_quantity,
                total_rejected_quantity,
                total_operations
            FROM manufacturing.v_oee_metrics
            WHERE organization_id = ?
            AND (? IS NULL OR work_center_code = ?)
            AND (? IS NULL OR production_date = ?)
            ORDER BY production_date DESC, work_center_code
            LIMIT 100
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            sql, 
            organizationId, 
            workCenterCode, workCenterCode,
            date, date
        );
        
        Map<String, Object> oeeData = new HashMap<>();
        oeeData.put("metrics", results);
        oeeData.put("count", results.size());
        
        // Calculate averages
        if (!results.isEmpty()) {
            double avgAvailability = results.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("availability_percentage", 0)).doubleValue())
                .average().orElse(0);
            double avgPerformance = results.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("performance_percentage", 0)).doubleValue())
                .average().orElse(0);
            double avgQuality = results.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("quality_percentage", 0)).doubleValue())
                .average().orElse(0);
            double avgOEE = results.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("oee_percentage", 0)).doubleValue())
                .average().orElse(0);
            
            oeeData.put("averages", Map.of(
                "availability", BigDecimal.valueOf(avgAvailability).setScale(2, RoundingMode.HALF_UP),
                "performance", BigDecimal.valueOf(avgPerformance).setScale(2, RoundingMode.HALF_UP),
                "quality", BigDecimal.valueOf(avgQuality).setScale(2, RoundingMode.HALF_UP),
                "oee", BigDecimal.valueOf(avgOEE).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        
        return oeeData;
    }

    /**
     * Get production trends over time
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductionTrends(UUID organizationId, String period, int limit) {
        String groupBy = switch (period.toUpperCase()) {
            case "DAILY" -> "production_date";
            case "WEEKLY" -> "production_week";
            case "MONTHLY" -> "production_month";
            default -> "production_date";
        };
        
        String sql = String.format("""
            SELECT 
                %s AS period,
                work_orders_completed,
                total_quantity_produced,
                total_quantity_scrapped,
                daily_yield_percentage,
                total_production_cost,
                cost_per_unit,
                on_time_count,
                late_count,
                on_time_percentage
            FROM manufacturing.v_production_trends
            WHERE organization_id = ?
            ORDER BY %s DESC
            LIMIT ?
        """, groupBy, groupBy);
        
        List<Map<String, Object>> trends = jdbcTemplate.queryForList(sql, organizationId, limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("trends", trends);
        result.put("count", trends.size());
        
        return result;
    }

    /**
     * Get cost analysis by product
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCostAnalysisByProduct(UUID organizationId) {
        String sql = """
            SELECT 
                product_id,
                product_code,
                product_name,
                total_work_orders,
                total_quantity_produced,
                total_material_cost,
                total_labor_cost,
                total_overhead_cost,
                total_cost,
                material_cost_per_unit,
                labor_cost_per_unit,
                overhead_cost_per_unit,
                total_cost_per_unit,
                material_cost_percentage,
                labor_cost_percentage,
                overhead_cost_percentage
            FROM manufacturing.v_cost_analysis_by_product
            WHERE organization_id = ?
            ORDER BY total_cost DESC
        """;
        
        return jdbcTemplate.queryForList(sql, organizationId);
    }

    /**
     * Get quality metrics summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQualityMetricsSummary(UUID organizationId) {
        String sql = """
            SELECT 
                product_id,
                product_code,
                product_name,
                total_inspections,
                passed_inspections,
                failed_inspections,
                total_quantity_inspected,
                total_quantity_passed,
                total_quantity_failed,
                total_defects,
                total_critical_defects,
                total_major_defects,
                total_minor_defects,
                avg_pass_rate,
                avg_defect_rate,
                first_pass_yield
            FROM manufacturing.v_quality_metrics_by_product
            WHERE organization_id = ?
            ORDER BY first_pass_yield ASC
        """;
        
        List<Map<String, Object>> productMetrics = jdbcTemplate.queryForList(sql, organizationId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("productMetrics", productMetrics);
        summary.put("productCount", productMetrics.size());
        
        // Overall aggregates
        if (!productMetrics.isEmpty()) {
            long totalInspections = productMetrics.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("total_inspections", 0L)).longValue())
                .sum();
            long passedInspections = productMetrics.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("passed_inspections", 0L)).longValue())
                .sum();
            
            summary.put("totalInspections", totalInspections);
            summary.put("passedInspections", passedInspections);
            summary.put("failedInspections", totalInspections - passedInspections);
            summary.put("overallPassRate", 
                totalInspections > 0 ? (passedInspections * 100.0 / totalInspections) : 0);
        }
        
        return summary;
    }

    // ==================== Private Helper Methods ====================

    private Map<String, Object> getWorkOrderMetrics(UUID organizationId) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("total", workOrderRepository.countByOrganization(organizationId));
        metrics.put("created", workOrderRepository.countByOrganizationAndStatus(organizationId, "CREATED"));
        metrics.put("released", workOrderRepository.countByOrganizationAndStatus(organizationId, "RELEASED"));
        metrics.put("inProgress", workOrderRepository.countByOrganizationAndStatus(organizationId, "IN_PROGRESS"));
        metrics.put("completed", workOrderRepository.countByOrganizationAndStatus(organizationId, "COMPLETED"));
        metrics.put("overdue", workOrderRepository.findOverdueWorkOrders(organizationId, LocalDateTime.now()).size());
        
        return metrics;
    }

    private Map<String, Object> getProductionMetrics(UUID organizationId) {
        String sql = """
            SELECT 
                COUNT(*) AS total_completed,
                SUM(quantity_planned) AS total_planned,
                SUM(quantity_completed) AS total_completed_qty,
                SUM(quantity_scrapped) AS total_scrapped,
                AVG(completion_percentage) AS avg_completion,
                CASE 
                    WHEN SUM(quantity_planned) > 0 
                    THEN (SUM(quantity_completed) / SUM(quantity_planned) * 100)
                    ELSE 0
                END AS overall_yield
            FROM manufacturing.work_orders
            WHERE organization_id = ?
            AND status IN ('COMPLETED', 'CLOSED')
            AND actual_end_date >= CURRENT_DATE - INTERVAL '30 days'
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, organizationId);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }

    private Map<String, Object> getQualityMetrics(UUID organizationId) {
        String sql = """
            SELECT 
                COUNT(*) AS total_inspections,
                COUNT(CASE WHEN overall_result = 'PASS' THEN 1 END) AS passed,
                COUNT(CASE WHEN overall_result = 'FAIL' THEN 1 END) AS failed,
                SUM(defects_found) AS total_defects,
                AVG(pass_rate) AS avg_pass_rate
            FROM manufacturing.quality_inspections
            WHERE organization_id = ?
            AND inspection_date >= CURRENT_DATE - INTERVAL '30 days'
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, organizationId);
        
        Map<String, Object> qualityMetrics = results.isEmpty() ? new HashMap<>() : results.get(0);
        
        // Add NC count
        String ncSql = """
            SELECT COUNT(*) AS open_ncs
            FROM manufacturing.non_conformances
            WHERE organization_id = ?
            AND status IN ('OPEN', 'IN_REVIEW')
        """;
        
        Long openNCs = jdbcTemplate.queryForObject(ncSql, Long.class, organizationId);
        qualityMetrics.put("openNonConformances", openNCs);
        
        return qualityMetrics;
    }

    private Map<String, Object> getCostMetrics(UUID organizationId) {
        String sql = """
            SELECT 
                SUM(material_cost) AS total_material_cost,
                SUM(labor_cost) AS total_labor_cost,
                SUM(overhead_cost) AS total_overhead_cost,
                SUM(total_cost) AS total_manufacturing_cost,
                AVG(total_cost) AS avg_cost_per_wo,
                CASE 
                    WHEN SUM(quantity_completed) > 0 
                    THEN SUM(total_cost) / SUM(quantity_completed)
                    ELSE 0
                END AS cost_per_unit
            FROM manufacturing.work_orders
            WHERE organization_id = ?
            AND status IN ('COMPLETED', 'CLOSED')
            AND actual_end_date >= CURRENT_DATE - INTERVAL '30 days'
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, organizationId);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }

    private Map<String, Object> getPerformanceMetrics(UUID organizationId) {
        String sql = """
            SELECT 
                completed_last_30_days,
                units_produced_last_30_days,
                avg_units_per_work_order,
                avg_cycle_time_hours,
                avg_lead_time_days,
                first_pass_yield_30_days,
                on_time_delivery_30_days,
                capacity_utilization_30_days
            FROM manufacturing.v_manufacturing_performance
            WHERE organization_id = ?
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, organizationId);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }

    /**
     * Get production summary report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductionSummaryReport(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                work_order_id,
                work_order_number,
                product_code,
                product_name,
                status,
                quantity_planned,
                quantity_completed,
                quantity_scrapped,
                completion_percentage,
                planned_start_date,
                planned_end_date,
                actual_start_date,
                actual_end_date,
                material_cost,
                labor_cost,
                overhead_cost,
                total_cost,
                days_since_created,
                days_until_due,
                completed_on_time
            FROM manufacturing.v_work_order_summary
            WHERE organization_id = ?
            AND (? IS NULL OR DATE(created_at) >= ?)
            AND (? IS NULL OR DATE(created_at) <= ?)
            ORDER BY created_at DESC
        """;
        
        List<Map<String, Object>> workOrders = jdbcTemplate.queryForList(
            sql, 
            organizationId,
            startDate, startDate,
            endDate, endDate
        );
        
        Map<String, Object> report = new HashMap<>();
        report.put("workOrders", workOrders);
        report.put("count", workOrders.size());
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        // Summary statistics
        if (!workOrders.isEmpty()) {
            BigDecimal totalPlanned = workOrders.stream()
                .map(wo -> (BigDecimal) wo.get("quantity_planned"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCompleted = workOrders.stream()
                .map(wo -> (BigDecimal) wo.get("quantity_completed"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCost = workOrders.stream()
                .map(wo -> (BigDecimal) wo.get("total_cost"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            report.put("summary", Map.of(
                "totalPlanned", totalPlanned,
                "totalCompleted", totalCompleted,
                "totalCost", totalCost,
                "averageCost", totalCost.divide(BigDecimal.valueOf(workOrders.size()), 2, RoundingMode.HALF_UP)
            ));
        }
        
        return report;
    }

    /**
     * Get work center performance report
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWorkCenterPerformance(UUID organizationId) {
        String sql = """
            SELECT 
                work_center_id,
                work_center_code,
                work_center_name,
                work_center_type,
                status,
                capacity_per_hour,
                efficiency_percentage,
                utilization_percentage,
                cost_per_hour,
                active_operations,
                operations_last_30_days,
                actual_efficiency_30_days,
                maintenance_status,
                last_maintenance_date,
                next_maintenance_date
            FROM manufacturing.v_work_center_utilization
            WHERE organization_id = ?
            ORDER BY work_center_code
        """;
        
        return jdbcTemplate.queryForList(sql, organizationId);
    }

    /**
     * Get non-conformance analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNonConformanceAnalytics(UUID organizationId) {
        String sql = """
            SELECT 
                nc_type,
                severity,
                category,
                root_cause,
                nc_count,
                total_quantity_affected,
                total_cost_impact,
                avg_resolution_days,
                open_count,
                closed_count
            FROM manufacturing.v_non_conformance_analytics
            WHERE organization_id = ?
            ORDER BY nc_count DESC
        """;
        
        List<Map<String, Object>> analytics = jdbcTemplate.queryForList(sql, organizationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("analytics", analytics);
        result.put("count", analytics.size());
        
        // Summary
        if (!analytics.isEmpty()) {
            long totalNCs = analytics.stream()
                .mapToLong(a -> ((Number) a.getOrDefault("nc_count", 0L)).longValue())
                .sum();
            long totalOpen = analytics.stream()
                .mapToLong(a -> ((Number) a.getOrDefault("open_count", 0L)).longValue())
                .sum();
            
            result.put("summary", Map.of(
                "totalNonConformances", totalNCs,
                "totalOpen", totalOpen,
                "totalClosed", totalNCs - totalOpen
            ));
        }
        
        return result;
    }

    /**
     * Get manufacturing performance summary
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPerformanceSummary(UUID organizationId) {
        String sql = """
            SELECT *
            FROM manufacturing.v_manufacturing_performance
            WHERE organization_id = ?
        """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, organizationId);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }

    /**
     * Get shop floor dashboard data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getShopFloorDashboard(UUID organizationId) {
        String sql = """
            SELECT 
                work_order_id,
                work_order_number,
                product_code,
                product_name,
                work_order_status,
                priority,
                quantity_planned,
                quantity_completed,
                completion_percentage,
                planned_end_date,
                current_operation,
                current_work_center,
                materials_pending,
                days_until_due,
                alert_status,
                created_at,
                actual_start_date
            FROM manufacturing.v_shop_floor_dashboard
            WHERE organization_id = ?
            ORDER BY priority DESC, planned_end_date ASC
            LIMIT 50
        """;
        
        return jdbcTemplate.queryForList(sql, organizationId);
    }
}

