package com.easyops.pharma.service;

import com.easyops.pharma.dto.*;
import com.easyops.pharma.entity.*;
import com.easyops.pharma.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {
    
    private final AreaRepository areaRepository;
    private final DivisionRepository divisionRepository;
    private final RegionRepository regionRepository;
    private final TerritoryRepository territoryRepository;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    private final ProductDisbursementRepository disbursementRepository;
    private final ProductDisbursementLineRepository disbursementLineRepository;
    private final ProductReceiptRepository receiptRepository;
    private final ProductReceiptLineRepository receiptLineRepository;
    private final DepositRepository depositRepository;
    private final DepositLineRepository depositLineRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final AdjustmentLineRepository adjustmentLineRepository;
    private final TargetRepository targetRepository;
    private final TargetCoverageRepository targetCoverageRepository;
    private final ExpenseRepository expenseRepository;
    private final IncentiveCalculationRepository incentiveCalculationRepository;
    private final IncentiveDistributionRepository incentiveDistributionRepository;
    
    // Monthly Closing Report - territory-based; for areaId, get territories in area and aggregate
    @Transactional(readOnly = true)
    public MonthlyClosingReportDTO generateMonthlyClosingReport(
            UUID organizationId, UUID areaId, UUID employeeId, Integer year, Integer month) {
        return generateMonthlyClosingReportByArea(organizationId, areaId, employeeId, year, month);
    }
    
    @Transactional(readOnly = true)
    public MonthlyClosingReportDTO generateMonthlyClosingReportByTerritory(
            UUID organizationId, UUID territoryId, UUID employeeId, Integer year, Integer month) {
        log.info("Generating monthly closing report for territory: {}, employee: {}, year: {}, month: {}", 
                territoryId, employeeId, year, month);
        
        Territory territory = territoryRepository.findById(territoryId)
                .orElseThrow(() -> new RuntimeException("Territory not found: " + territoryId));
        
        return buildMonthlyClosingReport(organizationId, List.of(territoryId), territory, employeeId, year, month);
    }
    
    @Transactional(readOnly = true)
    public MonthlyClosingReportDTO generateMonthlyClosingReportByArea(
            UUID organizationId, UUID areaId, UUID employeeId, Integer year, Integer month) {
        log.info("Generating monthly closing report for area: {}, employee: {}, year: {}, month: {}", 
                areaId, employeeId, year, month);
        
        List<Territory> territories = territoryRepository.findByAreaId(areaId);
        if (territories.isEmpty()) {
            throw new RuntimeException("No territories found in area: " + areaId);
        }
        List<UUID> territoryIds = territories.stream().map(Territory::getId).collect(Collectors.toList());
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Area not found: " + areaId));
        
        Territory firstTerritory = territories.get(0);
        MonthlyClosingReportDTO report = buildMonthlyClosingReport(organizationId, territoryIds, firstTerritory, employeeId, year, month);
        report.setAreaName(area.getName());
        return report;
    }
    
    private MonthlyClosingReportDTO buildMonthlyClosingReport(
            UUID organizationId, List<UUID> territoryIds, Territory singleTerritory, UUID employeeId, Integer year, Integer month) {
        MonthlyClosingReportDTO report = new MonthlyClosingReportDTO();
        report.setYear(year);
        report.setMonth(month);
        
        Territory territoryForHierarchy = singleTerritory != null ? singleTerritory : 
                territoryRepository.findById(territoryIds.get(0)).orElse(null);
        if (territoryForHierarchy != null) {
            report.setTerritoryName(territoryForHierarchy.getName());
            if (territoryForHierarchy.getAreaId() != null) {
                areaRepository.findById(territoryForHierarchy.getAreaId())
                        .ifPresent(a -> {
                            report.setAreaName(a.getName());
                            if (a.getDivisionId() != null) {
                                divisionRepository.findById(a.getDivisionId())
                                        .ifPresent(d -> report.setDivisionName(d.getName()));
                            }
                            if (a.getRegionId() != null) {
                                regionRepository.findById(a.getRegionId())
                                        .ifPresent(r -> report.setRegionName(r.getName()));
                            }
                        });
            }
        }
        
        if (employeeId != null) {
            for (UUID tid : territoryIds) {
                assignmentRepository.findByTerritoryIdAndEmployeeId(tid, employeeId)
                        .ifPresent(assignment -> {
                            report.setEmployeeId(assignment.getEmployeeId().toString());
                            report.setDesignation(assignment.getRoleInTerritory());
                        });
                if (report.getEmployeeId() != null) break;
            }
        }
        
        List<MonthlyClosingReportDTO.ProductWiseDetail> productDetails = new ArrayList<>();
        List<ProductDisbursement> disbursements = new ArrayList<>();
        List<Deposit> deposits = new ArrayList<>();
        List<Adjustment> adjustments = new ArrayList<>();
        
        for (UUID tid : territoryIds) {
            disbursements.addAll(disbursementRepository.findByTerritoryIdAndYearAndMonth(tid, year, month));
            deposits.addAll(depositRepository.findByTerritoryIdAndYearAndMonth(tid, year, month));
            adjustments.addAll(adjustmentRepository.findByTerritoryIdAndYearAndMonth(tid, year, month));
        }
        
        // Group by product
        Map<UUID, ProductSummary> productMap = new HashMap<>();
        
        // Process disbursements (receipts)
        for (ProductDisbursement disbursement : disbursements) {
            List<ProductDisbursementLine> lines = disbursementLineRepository
                    .findByProductDisbursementId(disbursement.getId());
            for (ProductDisbursementLine line : lines) {
                ProductSummary summary = productMap.computeIfAbsent(line.getProductId(), 
                        k -> new ProductSummary(line.getProductId(), line.getProductName()));
                summary.addReceived(line.getCurrentMonthQuantity(), 
                        disbursement.getDisbursementDate(), line.getTpWithVat());
            }
        }
        
        // Process deposits (sales)
        for (Deposit deposit : deposits) {
            List<DepositLine> lines = depositLineRepository.findByDepositId(deposit.getId());
            for (DepositLine line : lines) {
                ProductSummary summary = productMap.computeIfAbsent(line.getProductId(),
                        k -> new ProductSummary(line.getProductId(), line.getProductName()));
                summary.addSold(line.getQuantitySold(), deposit.getDepositDate());
            }
        }
        
        // Process adjustments
        for (Adjustment adjustment : adjustments) {
            List<AdjustmentLine> lines = adjustmentLineRepository
                    .findByAdjustmentId(adjustment.getId());
    for (AdjustmentLine line : lines) {
        ProductSummary summary = productMap.computeIfAbsent(line.getProductId(),
                k -> new ProductSummary(line.getProductId(), line.getProductName()));
        summary.addAdjusted(line.getAdjustmentQuantity(), adjustment.getAdjustmentDate(), 
                adjustment.getAdjustmentType());
    }
        }
        
        // Convert to DTOs
        for (ProductSummary summary : productMap.values()) {
            MonthlyClosingReportDTO.ProductWiseDetail detail = new MonthlyClosingReportDTO.ProductWiseDetail();
            detail.setProductId(summary.productId);
            detail.setProductName(summary.productName);
            detail.setOpeningBalance(summary.openingBalance);
            detail.setQuantityReceived(summary.totalReceived);
            detail.setQuantitySold(summary.totalSold);
            detail.setQuantityAdjusted(summary.totalAdjusted);
            detail.setClosingBalance(summary.getClosingBalance());
            detail.setTradePricePerUnit(summary.tpWithVat);
            detail.setTotalValue(summary.getTotalValue());
            detail.setTransactions(summary.transactions);
            productDetails.add(detail);
        }
        
        report.setProductDetails(productDetails);
        
        // Calculate financial summary
        BigDecimal totalSupplied = productDetails.stream()
                .map(d -> {
                    BigDecimal qty = d.getQuantityReceived() != null ? d.getQuantityReceived() : BigDecimal.ZERO;
                    BigDecimal tp = d.getTradePricePerUnit() != null ? d.getTradePricePerUnit() : BigDecimal.ZERO;
                    return qty.multiply(tp);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalProductsSuppliedValue(totalSupplied);
        
        BigDecimal totalDeposits = deposits.stream()
                .map(Deposit::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalDepositsReceived(totalDeposits);
        report.setDueAmount(totalSupplied.subtract(totalDeposits));
        
        // Get target (aggregate across territories - use first territory with target, or sum)
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCovered = BigDecimal.ZERO;
        for (UUID tid : territoryIds) {
            Optional<Target> targetOpt = targetRepository.findActiveTargetForTerritoryAndMonth(tid, year, month);
            if (targetOpt.isPresent()) {
                Target target = targetOpt.get();
                totalTarget = totalTarget.add(target.getTargetAmount());
                BigDecimal covered = targetCoverageRepository.findByTerritoryIdAndYearAndMonth(tid, year, month)
                        .map(cov -> cov.getCoveredAmount() != null ? cov.getCoveredAmount() : BigDecimal.ZERO)
                        .orElse(BigDecimal.ZERO);
                totalCovered = totalCovered.add(covered);
            }
        }
        report.setTargetAmount(totalTarget);
        report.setCoveredAmount(totalCovered);
        if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            report.setTargetCoveragePercentage(totalCovered.divide(totalTarget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        }
        
        // Performance metrics
        report.setTargetAchieved(report.getTargetCoveragePercentage() != null && 
                report.getTargetCoveragePercentage().compareTo(BigDecimal.valueOf(100)) >= 0);
        if (totalSupplied.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal efficiency = totalDeposits
                    .divide(totalSupplied, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            report.setCollectionEfficiency(efficiency);
        }
        
        return report;
    }
    
    // Helper class for product summary
    private static class ProductSummary {
        UUID productId;
        String productName;
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalSold = BigDecimal.ZERO;
        BigDecimal totalAdjusted = BigDecimal.ZERO;
        BigDecimal tpWithVat = BigDecimal.ZERO;
        List<MonthlyClosingReportDTO.TransactionDetail> transactions = new ArrayList<>();
        
        ProductSummary(UUID productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
        
        void addReceived(BigDecimal qty, LocalDate date, BigDecimal tp) {
            this.totalReceived = this.totalReceived.add(qty);
            this.tpWithVat = tp;
            transactions.add(new MonthlyClosingReportDTO.TransactionDetail(
                    date, "ALLOCATION", qty, null));
        }
        
        void addSold(BigDecimal qty, LocalDate date) {
            this.totalSold = this.totalSold.add(qty);
            transactions.add(new MonthlyClosingReportDTO.TransactionDetail(
                    date, "SALE", qty, null));
        }
        
        void addAdjusted(BigDecimal qty, LocalDate date, String type) {
            this.totalAdjusted = this.totalAdjusted.add(qty);
            transactions.add(new MonthlyClosingReportDTO.TransactionDetail(
                    date, "ADJUSTMENT", qty, type));
        }
        
        BigDecimal getClosingBalance() {
            return openingBalance.add(totalReceived).subtract(totalSold).subtract(totalAdjusted);
        }
        
        BigDecimal getTotalValue() {
            return getClosingBalance().multiply(tpWithVat);
        }
    }
    
    // Area Performance Report - aggregate from territories in area
    @Transactional(readOnly = true)
    public AreaPerformanceReportDTO generateAreaPerformanceReport(
            UUID organizationId, UUID areaId, Integer year, Integer month) {
        log.info("Generating area performance report for area: {}, year: {}, month: {}", 
                areaId, year, month);
        
        List<UUID> territoryIds = territoryRepository.findByAreaId(areaId).stream()
                .map(Territory::getId).collect(Collectors.toList());
        if (territoryIds.isEmpty()) {
            throw new RuntimeException("No territories found in area: " + areaId);
        }
        
        AreaPerformanceReportDTO report = new AreaPerformanceReportDTO();
        report.setAreaId(areaId);
        
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Area not found: " + areaId));
        report.setAreaName(area.getName());
        
        if (area.getDivisionId() != null) {
            divisionRepository.findById(area.getDivisionId())
                    .ifPresent(d -> report.setDivisionName(d.getName()));
        }
        if (area.getRegionId() != null) {
            regionRepository.findById(area.getRegionId())
                    .ifPresent(r -> report.setRegionName(r.getName()));
        }
        
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCovered = BigDecimal.ZERO;
        for (UUID tid : territoryIds) {
            Optional<Target> targetOpt = targetRepository.findActiveTargetForTerritoryAndMonth(tid, year, month);
            if (targetOpt.isPresent()) {
                totalTarget = totalTarget.add(targetOpt.get().getTargetAmount());
            }
            BigDecimal covered = targetCoverageRepository.findByTerritoryIdAndYearAndMonth(tid, year, month)
                    .map(c -> c.getCoveredAmount() != null ? c.getCoveredAmount() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            totalCovered = totalCovered.add(covered);
        }
        report.setTargetAmount(totalTarget);
        report.setCoveredAmount(totalCovered);
        if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            report.setTargetCoveragePercentage(totalCovered.divide(totalTarget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        }
        
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (UUID tid : territoryIds) {
            totalExpenses = totalExpenses.add(expenseRepository.findByTerritoryIdAndYearAndMonth(tid, year, month).stream()
                    .map(Expense::getExpenseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        report.setTotalExpenses(totalExpenses);
        
        if (report.getTargetAmount() != null && report.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expensePercentage = totalExpenses
                    .divide(report.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            report.setExpensePercentage(expensePercentage);
            report.setExpenseWithinLimit(expensePercentage.compareTo(BigDecimal.valueOf(30)) <= 0);
        }
        
        BigDecimal totalIncentiveBase = BigDecimal.ZERO;
        boolean anyEligible = false;
        for (UUID tid : territoryIds) {
            Optional<IncentiveCalculation> incentiveOpt = incentiveCalculationRepository.findByTerritoryIdAndYearAndMonth(tid, year, month);
            if (incentiveOpt.isPresent()) {
                IncentiveCalculation calc = incentiveOpt.get();
                if (Boolean.TRUE.equals(calc.getTerritoryEligible())) anyEligible = true;
                if (calc.getIncentiveBaseAmount() != null) totalIncentiveBase = totalIncentiveBase.add(calc.getIncentiveBaseAmount());
            }
        }
        report.setIncentiveEligible(anyEligible);
        report.setIncentiveBaseAmount(totalIncentiveBase);
        
        List<AreaPerformanceReportDTO.EmployeePerformance> employeePerformances = new ArrayList<>();
        Set<UUID> seenEmployees = new HashSet<>();
        for (UUID tid : territoryIds) {
            for (EmployeeTerritoryAssignment assignment : assignmentRepository.findByTerritoryId(tid)) {
                if (seenEmployees.add(assignment.getEmployeeId())) {
                    BigDecimal incentiveAmount = BigDecimal.ZERO;
                    for (UUID calcTid : territoryIds) {
                        Optional<IncentiveCalculation> calcOpt = incentiveCalculationRepository.findByTerritoryIdAndYearAndMonth(calcTid, year, month);
                        if (calcOpt.isPresent()) {
                            incentiveAmount = incentiveAmount.add(
                                    incentiveDistributionRepository.findByIncentiveCalculationIdAndEmployeeId(calcOpt.get().getId(), assignment.getEmployeeId()).stream()
                                            .map(IncentiveDistribution::getIncentiveAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                        }
                    }
                    AreaPerformanceReportDTO.EmployeePerformance perf = new AreaPerformanceReportDTO.EmployeePerformance();
                    perf.setEmployeeId(assignment.getEmployeeId());
                    perf.setRole(assignment.getRoleInTerritory());
                    perf.setIncentiveAmount(incentiveAmount);
                    employeePerformances.add(perf);
                }
            }
        }
        report.setEmployeePerformances(employeePerformances);
        
        return report;
    }
    
    // Inventory Reports - In-Stock Total Amount
    @Transactional(readOnly = true)
    public InventoryReportDTO.InStockTotalAmount generateInStockTotalAmountReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating in-stock total amount report from {} to {}", startDate, endDate);
        
        // Get all receipts up to endDate
        List<ProductReceipt> receipts = receiptRepository
                .findByOrganizationId(organizationId);
        
        // Calculate total inventory value
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<UUID, BigDecimal> productQuantities = new HashMap<>();
        
        for (ProductReceipt receipt : receipts) {
            if (receipt.getReceiptDate().isAfter(endDate)) continue;
            
            List<ProductReceiptLine> lines = receiptLineRepository
                    .findByProductReceiptId(receipt.getId());
            for (ProductReceiptLine line : lines) {
                BigDecimal qty = productQuantities.getOrDefault(line.getProductId(), BigDecimal.ZERO);
                productQuantities.put(line.getProductId(), qty.add(line.getQuantity()));
            }
        }
        
        // Subtract disbursements
        List<ProductDisbursement> disbursements = disbursementRepository
                .findByOrganizationIdAndDisbursementDateBetween(organizationId, startDate, endDate);
        
        for (ProductDisbursement disbursement : disbursements) {
            List<ProductDisbursementLine> lines = disbursementLineRepository
                    .findByProductDisbursementId(disbursement.getId());
            for (ProductDisbursementLine line : lines) {
                BigDecimal qty = productQuantities.getOrDefault(line.getProductId(), BigDecimal.ZERO);
                productQuantities.put(line.getProductId(), qty.subtract(line.getCurrentMonthQuantity()));
            }
        }
        
        // Calculate total value
        for (Map.Entry<UUID, BigDecimal> entry : productQuantities.entrySet()) {
            // Get TP from latest receipt or disbursement
            BigDecimal tp = getProductTp(entry.getKey());
            if (tp != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = totalAmount.add(entry.getValue().multiply(tp));
            }
        }
        
        InventoryReportDTO.InStockTotalAmount report = new InventoryReportDTO.InStockTotalAmount();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAsOfDate(endDate);
        report.setLocation("Central Depot");
        report.setTotalInStockAmount(totalAmount);
        
        return report;
    }
    
    // Inventory Reports - In-Stock Product-Wise
    @Transactional(readOnly = true)
    public InventoryReportDTO.InStockProductWise generateInStockProductWiseReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating in-stock product-wise report from {} to {}", startDate, endDate);
        
        // Get all receipts up to endDate
        List<ProductReceipt> receipts = receiptRepository
                .findByOrganizationId(organizationId);
        
        // Calculate product quantities and details
        Map<UUID, ProductStockInfo> productMap = new HashMap<>();
        
        for (ProductReceipt receipt : receipts) {
            if (receipt.getReceiptDate().isAfter(endDate)) continue;
            
            List<ProductReceiptLine> lines = receiptLineRepository
                    .findByProductReceiptId(receipt.getId());
            for (ProductReceiptLine line : lines) {
                ProductStockInfo info = productMap.computeIfAbsent(line.getProductId(),
                        k -> new ProductStockInfo(line.getProductId(), line.getProductName(), 
                                line.getPackSize(), line.getTpWithVat()));
                info.addQuantity(line.getQuantity());
            }
        }
        
        // Subtract disbursements
        List<ProductDisbursement> disbursements = disbursementRepository
                .findByOrganizationIdAndDisbursementDateBetween(organizationId, startDate, endDate);
        
        for (ProductDisbursement disbursement : disbursements) {
            List<ProductDisbursementLine> lines = disbursementLineRepository
                    .findByProductDisbursementId(disbursement.getId());
            for (ProductDisbursementLine line : lines) {
                ProductStockInfo info = productMap.get(line.getProductId());
                if (info != null) {
                    info.subtractQuantity(line.getCurrentMonthQuantity());
                }
            }
        }
        
        // Convert to DTOs
        List<InventoryReportDTO.ProductStockDetail> products = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (ProductStockInfo info : productMap.values()) {
            if (info.quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amount = info.quantity.multiply(info.tpWithVat);
                totalAmount = totalAmount.add(amount);
                
                InventoryReportDTO.ProductStockDetail detail = 
                        new InventoryReportDTO.ProductStockDetail();
                detail.setProductId(info.productId);
                detail.setProductName(info.productName);
                detail.setPackSize(info.packSize);
                detail.setTpWithVat(info.tpWithVat);
                detail.setQuantity(info.quantity);
                detail.setAmount(amount);
                products.add(detail);
            }
        }
        
        InventoryReportDTO.InStockProductWise report = new InventoryReportDTO.InStockProductWise();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAsOfDate(endDate);
        report.setLocation("Central Depot");
        report.setProducts(products);
        report.setTotalInStockAmount(totalAmount);
        
        return report;
    }
    
    private static class ProductStockInfo {
        UUID productId;
        String productName;
        BigDecimal packSize;
        BigDecimal tpWithVat;
        BigDecimal quantity = BigDecimal.ZERO;
        
        ProductStockInfo(UUID productId, String productName, BigDecimal packSize, BigDecimal tpWithVat) {
            this.productId = productId;
            this.productName = productName;
            this.packSize = packSize != null ? packSize : BigDecimal.ZERO;
            this.tpWithVat = tpWithVat != null ? tpWithVat : BigDecimal.ZERO;
        }
        
        void addQuantity(BigDecimal qty) {
            this.quantity = this.quantity.add(qty != null ? qty : BigDecimal.ZERO);
        }
        
        void subtractQuantity(BigDecimal qty) {
            this.quantity = this.quantity.subtract(qty != null ? qty : BigDecimal.ZERO);
        }
    }
    
    // Helper to get product TP
    private BigDecimal getProductTp(UUID productId) {
        // Try to get from latest receipt line
        List<ProductReceiptLine> receiptLines = receiptLineRepository.findByProductId(productId);
        if (!receiptLines.isEmpty()) {
            return receiptLines.get(0).getTpWithVat();
        }
        
        // Try to get from latest disbursement line
        List<ProductDisbursementLine> disbursementLines = disbursementLineRepository.findByProductId(productId);
        if (!disbursementLines.isEmpty()) {
            return disbursementLines.get(0).getTpWithVat();
        }
        
        return BigDecimal.ZERO;
    }
    
    // Area-Wise Allocation Report - uses territoryId; when areaId filter, get territories in area
    @Transactional(readOnly = true)
    public InventoryReportDTO.AreaWiseAllocation generateAreaWiseAllocationReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate, UUID areaId) {
        log.info("Generating area-wise allocation report from {} to {}", startDate, endDate);
        
        List<ProductDisbursement> disbursements;
        if (areaId != null) {
            List<UUID> territoryIds = territoryRepository.findByAreaId(areaId).stream().map(Territory::getId).collect(Collectors.toList());
            disbursements = new ArrayList<>();
            for (UUID tid : territoryIds) {
                disbursements.addAll(disbursementRepository.findByTerritoryId(tid).stream()
                        .filter(d -> !d.getDisbursementDate().isBefore(startDate) &&  !d.getDisbursementDate().isAfter(endDate))
                        .collect(Collectors.toList()));
            }
        } else {
            disbursements = disbursementRepository
                    .findByOrganizationIdAndDisbursementDateBetween(organizationId, startDate, endDate);
        }
        
        Map<UUID, AreaAllocationSummary> areaMap = new HashMap<>();
        
        for (ProductDisbursement disbursement : disbursements) {
            UUID key = disbursement.getTerritoryId();
            AreaAllocationSummary summary = areaMap.computeIfAbsent(key, k -> new AreaAllocationSummary(key));
            
            summary.addDisbursement(disbursement);
            
            List<ProductDisbursementLine> lines = disbursementLineRepository
                    .findByProductDisbursementId(disbursement.getId());
            for (ProductDisbursementLine line : lines) {
                summary.addProduct(line.getProductId(), line.getProductName(), 
                        line.getCurrentMonthQuantity(), line.getTpWithVat());
            }
        }
        
        List<InventoryReportDTO.AreaAllocationDetail> areaDetails = new ArrayList<>();
        for (AreaAllocationSummary summary : areaMap.values()) {
            Territory territory = territoryRepository.findById(summary.territoryId).orElse(null);
            if (territory == null) continue;
            
            InventoryReportDTO.AreaAllocationDetail detail = 
                    new InventoryReportDTO.AreaAllocationDetail();
            detail.setAreaId(summary.territoryId);
            detail.setAreaName(territory.getName());
            detail.setTotalAllocations(summary.disbursementDates.size());
            detail.setAllocationDates(new ArrayList<>(summary.disbursementDates));
            
            List<InventoryReportDTO.ProductAllocationDetail> productDetails = new ArrayList<>();
            for (Map.Entry<UUID, ProductAllocSummary> entry : summary.products.entrySet()) {
                ProductAllocSummary prodSummary = entry.getValue();
                InventoryReportDTO.ProductAllocationDetail prodDetail = 
                        new InventoryReportDTO.ProductAllocationDetail();
                prodDetail.setProductId(entry.getKey());
                prodDetail.setProductName(prodSummary.productName);
                prodDetail.setTotalQuantityAllocated(prodSummary.totalQuantity);
                prodDetail.setTotalAmount(prodSummary.totalAmount);
                productDetails.add(prodDetail);
            }
            detail.setProductDetails(productDetails);
            
            BigDecimal totalAmount = productDetails.stream()
                    .map(InventoryReportDTO.ProductAllocationDetail::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            detail.setTotalAllocationAmount(totalAmount);
            
            areaDetails.add(detail);
        }
        
        InventoryReportDTO.AreaWiseAllocation report = new InventoryReportDTO.AreaWiseAllocation();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAreas(areaDetails);
        
        return report;
    }
    
    private static class AreaAllocationSummary {
        UUID territoryId;
        Set<LocalDate> disbursementDates = new HashSet<>();
        Map<UUID, ProductAllocSummary> products = new HashMap<>();
        
        AreaAllocationSummary(UUID territoryId) {
            this.territoryId = territoryId;
        }
        
        void addDisbursement(ProductDisbursement disbursement) {
            disbursementDates.add(disbursement.getDisbursementDate());
        }
        
        void addProduct(UUID productId, String productName, BigDecimal qty, BigDecimal tp) {
            ProductAllocSummary summary = products.computeIfAbsent(productId,
                    k -> new ProductAllocSummary(productId, productName));
            summary.add(qty, tp);
        }
    }
    
    private static class ProductAllocSummary {
        UUID productId;
        String productName;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        ProductAllocSummary(UUID productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
        
        void add(BigDecimal qty, BigDecimal tp) {
            this.totalQuantity = this.totalQuantity.add(qty);
            this.totalAmount = this.totalAmount.add(qty.multiply(tp));
        }
    }
    
    // Month-Wise Allocation Report
    @Transactional(readOnly = true)
    public InventoryReportDTO.MonthWiseAllocation generateMonthWiseAllocationReport(
            UUID organizationId, Integer year, UUID areaId) {
        log.info("Generating month-wise allocation report for year: {}", year);
        
        List<ProductDisbursement> disbursements;
        if (areaId != null) {
            List<UUID> territoryIds = territoryRepository.findByAreaId(areaId).stream().map(Territory::getId).collect(Collectors.toList());
            disbursements = new ArrayList<>();
            for (UUID tid : territoryIds) {
                disbursements.addAll(disbursementRepository.findByTerritoryId(tid).stream()
                        .filter(d -> d.getYear() != null && d.getYear().equals(year))
                        .collect(Collectors.toList()));
            }
        } else {
            disbursements = disbursementRepository.findByOrganizationId(organizationId).stream()
                    .filter(d -> d.getYear() != null && d.getYear().equals(year))
                    .collect(Collectors.toList());
        }
        
        Map<Integer, MonthAllocationSummary> monthMap = new HashMap<>();
        Set<UUID> allTerritoryIds = new HashSet<>();
        
        for (ProductDisbursement disbursement : disbursements) {
            Integer month = disbursement.getMonth();
            if (month == null) continue;
            
            MonthAllocationSummary summary = monthMap.computeIfAbsent(month,
                    k -> new MonthAllocationSummary(year, month));
            
            summary.addDisbursement(disbursement);
            allTerritoryIds.add(disbursement.getTerritoryId());
            
            List<ProductDisbursementLine> lines = disbursementLineRepository
                    .findByProductDisbursementId(disbursement.getId());
            for (ProductDisbursementLine line : lines) {
                summary.addQuantity(line.getCurrentMonthQuantity());
                summary.addAmount(line.getCurrentMonthQuantity().multiply(line.getTpWithVat()));
            }
        }
        
        List<InventoryReportDTO.MonthlyAllocationDetail> monthlyDetails = new ArrayList<>();
        BigDecimal grandTotalQuantity = BigDecimal.ZERO;
        BigDecimal grandTotalAmount = BigDecimal.ZERO;
        int grandTotalAllocations = 0;
        
        for (int month = 1; month <= 12; month++) {
            MonthAllocationSummary summary = monthMap.get(month);
            if (summary != null) {
                InventoryReportDTO.MonthlyAllocationDetail detail = 
                        new InventoryReportDTO.MonthlyAllocationDetail();
                detail.setYear(year);
                detail.setMonth(month);
                detail.setTotalQuantity(summary.totalQuantity);
                detail.setTotalAmount(summary.totalAmount);
                detail.setNumberOfAllocations(summary.disbursementCount);
                detail.setNumberOfAreas(allTerritoryIds.size());
                detail.setTopProducts(new ArrayList<>());
                
                monthlyDetails.add(detail);
                grandTotalQuantity = grandTotalQuantity.add(summary.totalQuantity);
                grandTotalAmount = grandTotalAmount.add(summary.totalAmount);
                grandTotalAllocations += summary.disbursementCount;
            }
        }
        
        InventoryReportDTO.MonthWiseAllocation report = new InventoryReportDTO.MonthWiseAllocation();
        report.setYear(year);
        report.setMonthlyDetails(monthlyDetails);
        report.setGrandTotalQuantity(grandTotalQuantity);
        report.setGrandTotalAmount(grandTotalAmount);
        report.setGrandTotalAllocations(grandTotalAllocations);
        
        return report;
    }
    
    // Annual Allocation Report
    @Transactional(readOnly = true)
    public InventoryReportDTO.AnnualAllocation generateAnnualAllocationReport(
            UUID organizationId, Integer year, UUID areaId) {
        log.info("Generating annual allocation report for year: {}", year);
        
        InventoryReportDTO.MonthWiseAllocation monthWise = generateMonthWiseAllocationReport(
                organizationId, year, areaId);
        
        BigDecimal totalAmount = monthWise.getGrandTotalAmount();
        BigDecimal averageMonthly = totalAmount.compareTo(BigDecimal.ZERO) > 0 
                ? totalAmount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        String peakMonth = null;
        BigDecimal peakAmount = BigDecimal.ZERO;
        for (InventoryReportDTO.MonthlyAllocationDetail detail : monthWise.getMonthlyDetails()) {
            if (detail.getTotalAmount().compareTo(peakAmount) > 0) {
                peakAmount = detail.getTotalAmount();
                peakMonth = new java.text.SimpleDateFormat("MMMM yyyy")
                        .format(java.sql.Date.valueOf(LocalDate.of(year, detail.getMonth(), 1)));
            }
        }
        
        InventoryReportDTO.AnnualAllocation report = new InventoryReportDTO.AnnualAllocation();
        report.setYear(year);
        report.setTotalAllocations(monthWise.getGrandTotalAllocations());
        report.setTotalQuantity(monthWise.getGrandTotalQuantity());
        report.setTotalAmount(totalAmount);
        report.setAverageMonthlyAllocation(averageMonthly);
        report.setPeakMonth(peakMonth);
        report.setMonthlyBreakdown(monthWise.getMonthlyDetails());
        
        return report;
    }
    
    private static class MonthAllocationSummary {
        Integer year;
        Integer month;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int disbursementCount = 0;
        
        MonthAllocationSummary(Integer year, Integer month) {
            this.year = year;
            this.month = month;
        }
        
        void addDisbursement(ProductDisbursement disbursement) {
            disbursementCount++;
        }
        
        void addQuantity(BigDecimal qty) {
            this.totalQuantity = this.totalQuantity.add(qty != null ? qty : BigDecimal.ZERO);
        }
        
        void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount != null ? amount : BigDecimal.ZERO);
        }
    }
    
    // Collection Reports
    @Transactional(readOnly = true)
    public CollectionReportDTO.AreaWiseCollection generateAreaWiseCollectionReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate, UUID areaId) {
        log.info("Generating area-wise collection report from {} to {}", startDate, endDate);
        
        List<Deposit> deposits;
        if (areaId != null) {
            List<UUID> territoryIds = territoryRepository.findByAreaId(areaId).stream().map(Territory::getId).collect(Collectors.toList());
            deposits = new ArrayList<>();
            for (UUID tid : territoryIds) {
                deposits.addAll(depositRepository.findByTerritoryId(tid).stream()
                        .filter(d -> !d.getDepositDate().isBefore(startDate) && !d.getDepositDate().isAfter(endDate))
                        .collect(Collectors.toList()));
            }
        } else {
            deposits = depositRepository
                    .findByOrganizationIdAndDepositDateBetween(organizationId, startDate, endDate);
        }
        
        Map<UUID, AreaCollectionSummary> areaMap = new HashMap<>();
        
        for (Deposit deposit : deposits) {
            AreaCollectionSummary summary = areaMap.computeIfAbsent(deposit.getTerritoryId(),
                    k -> new AreaCollectionSummary(deposit.getTerritoryId()));
            summary.addDeposit(deposit);
        }
        
        List<CollectionReportDTO.AreaCollectionDetail> areaDetails = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        
        for (AreaCollectionSummary summary : areaMap.values()) {
            Territory territory = territoryRepository.findById(summary.territoryId).orElse(null);
            if (territory == null) continue;
            
            CollectionReportDTO.AreaCollectionDetail detail = 
                    new CollectionReportDTO.AreaCollectionDetail();
            detail.setAreaId(summary.territoryId);
            detail.setAreaName(territory.getName());
            detail.setNumberOfDeposits(summary.deposits.size());
            detail.setTotalCollectionAmount(summary.totalAmount);
            
        // Get target for the period
        YearMonth yearMonth = YearMonth.from(startDate);
        Optional<Target> targetOpt = targetRepository
                .findActiveTargetForTerritoryAndMonth(summary.territoryId, yearMonth.getYear(), yearMonth.getMonthValue());
        if (targetOpt.isPresent()) {
                detail.setTargetAmount(targetOpt.get().getTargetAmount());
                if (targetOpt.get().getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = summary.totalAmount
                            .divide(targetOpt.get().getTargetAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    detail.setCoveragePercentage(percentage);
                }
            }
            
            List<CollectionReportDTO.DepositDetail> depositDetails = summary.deposits.stream()
                    .map(d -> {
                        CollectionReportDTO.DepositDetail dd = new CollectionReportDTO.DepositDetail();
                        dd.setDepositId(d.getId());
                        dd.setDepositDate(d.getDepositDate());
                    dd.setDepositAmount(d.getDepositAmount());
                    dd.setStatus(d.getStatus());
                    dd.setCollectedBy(d.getEmployeeId() != null ? d.getEmployeeId().toString() : "N/A");
                    return dd;
                    })
                    .collect(Collectors.toList());
            detail.setDeposits(depositDetails);
            
            areaDetails.add(detail);
            grandTotal = grandTotal.add(summary.totalAmount);
        }
        
        CollectionReportDTO.AreaWiseCollection report = new CollectionReportDTO.AreaWiseCollection();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAreas(areaDetails);
        report.setGrandTotalCollection(grandTotal);
        
        return report;
    }
    
    private static class AreaCollectionSummary {
        UUID territoryId;
        List<Deposit> deposits = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        AreaCollectionSummary(UUID territoryId) {
            this.territoryId = territoryId;
        }
        
        void addDeposit(Deposit deposit) {
            deposits.add(deposit);
            totalAmount = totalAmount.add(deposit.getDepositAmount());
        }
    }
    
    // Collection Reports - Employee-Wise Collection
    @Transactional(readOnly = true)
    public CollectionReportDTO.EmployeeWiseCollection generateEmployeeWiseCollectionReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate, UUID employeeId) {
        log.info("Generating employee-wise collection report from {} to {}, employee: {}", 
                startDate, endDate, employeeId);
        
        List<Deposit> deposits;
        if (employeeId != null) {
            deposits = depositRepository.findByOrganizationIdAndEmployeeIdAndDepositDateBetween(
                    organizationId, employeeId, startDate, endDate);
        } else {
            // Get all employees who have deposits in the organization
            deposits = depositRepository.findByOrganizationIdAndDepositDateBetween(
                    organizationId, startDate, endDate);
        }
        
        // Group deposits by employee
        Map<UUID, EmployeeCollectionSummary> employeeMap = new HashMap<>();
        
        for (Deposit deposit : deposits) {
            if (deposit.getEmployeeId() == null) continue;
            
            EmployeeCollectionSummary summary = employeeMap.computeIfAbsent(
                    deposit.getEmployeeId(), k -> new EmployeeCollectionSummary(deposit.getEmployeeId()));
            summary.addDeposit(deposit);
        }
        
        List<CollectionReportDTO.EmployeeCollectionDetail> employeeDetails = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        
        for (EmployeeCollectionSummary summary : employeeMap.values()) {
            CollectionReportDTO.EmployeeCollectionDetail detail = 
                    new CollectionReportDTO.EmployeeCollectionDetail();
            detail.setEmployeeId(summary.employeeId);
            detail.setNumberOfDeposits(summary.deposits.size());
            detail.setTotalCollectionAmount(summary.totalAmount);
            
            // Get employee assignments to get name, code, role, and assigned territories
            List<EmployeeTerritoryAssignment> assignments = assignmentRepository
                    .findByEmployeeId(summary.employeeId);
            
            if (!assignments.isEmpty()) {
                EmployeeTerritoryAssignment firstAssignment = assignments.get(0);
                detail.setEmployeeName(firstAssignment.getEmployeeId().toString());
                detail.setEmployeeIdCode(firstAssignment.getEmployeeId().toString());
                
                Set<String> roles = assignments.stream()
                        .map(EmployeeTerritoryAssignment::getRoleInTerritory)
                        .collect(Collectors.toSet());
                detail.setRole(String.join(", ", roles));
                
                List<UUID> territoryIds = assignments.stream()
                        .map(EmployeeTerritoryAssignment::getTerritoryId)
                        .distinct()
                        .collect(Collectors.toList());
                detail.setAssignedAreaIds(territoryIds);
                
                List<String> territoryNames = territoryIds.stream()
                        .map(id -> territoryRepository.findById(id)
                                .map(Territory::getName)
                                .orElse("Unknown"))
                        .collect(Collectors.toList());
                detail.setAssignedAreaNames(territoryNames);
            }
            
            employeeDetails.add(detail);
            grandTotal = grandTotal.add(summary.totalAmount);
        }
        
        // Sort by total collection amount descending
        employeeDetails.sort((a, b) -> b.getTotalCollectionAmount()
                .compareTo(a.getTotalCollectionAmount()));
        
        CollectionReportDTO.EmployeeWiseCollection report = 
                new CollectionReportDTO.EmployeeWiseCollection();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setEmployees(employeeDetails);
        report.setGrandTotalCollection(grandTotal);
        
        return report;
    }
    
    private static class EmployeeCollectionSummary {
        UUID employeeId;
        List<Deposit> deposits = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        EmployeeCollectionSummary(UUID employeeId) {
            this.employeeId = employeeId;
        }
        
        void addDeposit(Deposit deposit) {
            deposits.add(deposit);
            totalAmount = totalAmount.add(deposit.getDepositAmount());
        }
    }
    
    // Financial Reports - Accounts Balance - iterate over territories
    @Transactional(readOnly = true)
    public FinancialReportDTO.AccountsBalance generateAccountsBalanceReport(
            UUID organizationId, LocalDate asOfDate) {
        log.info("Generating accounts balance report as of {}", asOfDate);
        
        List<Territory> territories = territoryRepository.findByOrganizationId(organizationId);
        List<FinancialReportDTO.AreaBalance> areaBalances = new ArrayList<>();
        BigDecimal totalDue = BigDecimal.ZERO;
        
        for (Territory territory : territories) {
            List<ProductDisbursement> disbursements = disbursementRepository
                    .findByTerritoryId(territory.getId()).stream()
                    .filter(d -> !d.getDisbursementDate().isAfter(asOfDate))
                    .collect(Collectors.toList());
            
            BigDecimal totalSupplied = BigDecimal.ZERO;
            for (ProductDisbursement d : disbursements) {
                List<ProductDisbursementLine> lines = disbursementLineRepository
                        .findByProductDisbursementId(d.getId());
                for (ProductDisbursementLine line : lines) {
                    totalSupplied = totalSupplied.add(
                            line.getCurrentMonthQuantity().multiply(line.getTpWithVat()));
                }
            }
            
            List<Deposit> deposits = depositRepository.findByTerritoryId(territory.getId()).stream()
                    .filter(d -> !d.getDepositDate().isAfter(asOfDate))
                    .collect(Collectors.toList());
            
            BigDecimal totalDeposits = deposits.stream()
                    .map(Deposit::getDepositAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dueAmount = totalSupplied.subtract(totalDeposits);
            
            FinancialReportDTO.AreaBalance balance = new FinancialReportDTO.AreaBalance();
            balance.setAreaId(territory.getId());
            balance.setAreaName(territory.getName());
            balance.setTotalProductsSupplied(totalSupplied);
            balance.setTotalDepositsReceived(totalDeposits);
            balance.setDueAmount(dueAmount);
            balance.setOverdueAmount(dueAmount); // Simplified - could add date-based logic
            
            areaBalances.add(balance);
            totalDue = totalDue.add(dueAmount);
        }
        
        FinancialReportDTO.AccountsBalance report = new FinancialReportDTO.AccountsBalance();
        report.setAsOfDate(asOfDate);
        report.setAreaBalances(areaBalances);
        report.setTotalDueAmount(totalDue);
        
        return report;
    }
    
    // Financial Reports - Income and Expense
    @Transactional(readOnly = true)
    public FinancialReportDTO.IncomeExpense generateIncomeExpenseReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating income expense report from {} to {}", startDate, endDate);
        
        List<Territory> territories = territoryRepository.findByOrganizationId(organizationId);
        List<FinancialReportDTO.AreaIncomeExpense> areaDetails = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (Territory territory : territories) {
            List<Deposit> deposits = depositRepository
                    .findByOrganizationIdAndDepositDateBetween(organizationId, startDate, endDate)
                    .stream()
                    .filter(d -> d.getTerritoryId().equals(territory.getId()))
                    .collect(Collectors.toList());
            
            BigDecimal income = deposits.stream()
                    .map(Deposit::getDepositAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<Expense> expenses = expenseRepository
                    .findByTerritoryId(territory.getId()).stream()
                    .filter(e -> !e.getExpenseDate().isBefore(startDate) && 
                                !e.getExpenseDate().isAfter(endDate))
                    .collect(Collectors.toList());
            
            BigDecimal expense = expenses.stream()
                    .map(Expense::getExpenseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            FinancialReportDTO.AreaIncomeExpense detail = new FinancialReportDTO.AreaIncomeExpense();
            detail.setAreaId(territory.getId());
            detail.setAreaName(territory.getName());
            detail.setIncome(income);
            detail.setExpenses(expense);
            detail.setNetIncome(income.subtract(expense));
            
            areaDetails.add(detail);
            totalIncome = totalIncome.add(income);
            totalExpenses = totalExpenses.add(expense);
        }
        
        FinancialReportDTO.IncomeExpense report = new FinancialReportDTO.IncomeExpense();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTotalIncome(totalIncome);
        report.setTotalExpenses(totalExpenses);
        report.setNetIncome(totalIncome.subtract(totalExpenses));
        report.setAreaDetails(areaDetails);
        
        return report;
    }
    
    // Financial Reports - Incentive Report
    @Transactional(readOnly = true)
    public FinancialReportDTO.IncentiveReport generateIncentiveReport(
            UUID organizationId, Integer year, Integer month) {
        log.info("Generating incentive report for year: {}, month: {}", year, month);
        
        List<Territory> territories = territoryRepository.findByOrganizationId(organizationId);
        List<FinancialReportDTO.AreaIncentiveDetail> areaIncentives = new ArrayList<>();
        BigDecimal totalIncentive = BigDecimal.ZERO;
        int eligibleAreas = 0;
        
        for (Territory territory : territories) {
            Optional<IncentiveCalculation> calcOpt = incentiveCalculationRepository
                    .findByTerritoryIdAndYearAndMonth(territory.getId(), year, month);
            
            if (calcOpt.isPresent()) {
                IncentiveCalculation calc = calcOpt.get();
                
                FinancialReportDTO.AreaIncentiveDetail detail = 
                        new FinancialReportDTO.AreaIncentiveDetail();
                detail.setAreaId(territory.getId());
                detail.setAreaName(territory.getName());
                detail.setTargetAmount(calc.getTargetAmount());
                detail.setCoveredAmount(calc.getCoveredAmount());
                
                if (calc.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = calc.getCoveredAmount()
                            .divide(calc.getTargetAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    detail.setTargetCoveragePercentage(percentage);
                }
                
                detail.setTargetAchieved(calc.getTargetAchieved());
                detail.setExpenseWithinLimit(calc.getExpenseWithinLimit());
                detail.setEligible(calc.getTerritoryEligible());
                detail.setIncentiveBaseAmount(calc.getIncentiveBaseAmount());
                
                // Get employee incentives
                List<IncentiveDistribution> distributions = incentiveDistributionRepository
                        .findByIncentiveCalculationId(calc.getId());
                
                List<FinancialReportDTO.EmployeeIncentiveDetail> employeeIncentives = 
                        distributions.stream()
                        .map(d -> {
                            FinancialReportDTO.EmployeeIncentiveDetail ed = 
                                    new FinancialReportDTO.EmployeeIncentiveDetail();
                            ed.setEmployeeId(d.getEmployeeId());
                            ed.setRole(d.getRoleInArea());
                            ed.setIncentiveAmount(d.getIncentiveAmount());
                            return ed;
                        })
                        .collect(Collectors.toList());
                
                detail.setEmployeeIncentives(employeeIncentives);
                areaIncentives.add(detail);
                
                if (Boolean.TRUE.equals(calc.getTerritoryEligible())) {
                    totalIncentive = totalIncentive.add(calc.getIncentiveBaseAmount());
                    eligibleAreas++;
                }
            }
        }
        
        FinancialReportDTO.IncentiveReport report = new FinancialReportDTO.IncentiveReport();
        report.setYear(year);
        report.setMonth(month);
        report.setAreaIncentives(areaIncentives);
        report.setTotalIncentiveAmount(totalIncentive);
        report.setEligibleAreas(eligibleAreas);
        report.setTotalAreas(territories.size());
        
        return report;
    }
}

