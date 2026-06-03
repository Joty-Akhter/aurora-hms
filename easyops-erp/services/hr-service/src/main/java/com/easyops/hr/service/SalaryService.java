package com.easyops.hr.service;

import com.easyops.hr.dto.ComponentDependencyDto;
import com.easyops.hr.dto.ComponentUsageDto;
import com.easyops.hr.dto.ComponentWiseCostDto;
import com.easyops.hr.dto.EmployeeSelfSalarySummaryDto;
import com.easyops.hr.dto.GradeHeadcountDto;
import com.easyops.hr.dto.PositionSalaryDefaultsDto;
import com.easyops.hr.dto.SalaryBandSummaryDto;
import com.easyops.hr.dto.SalaryRevisionHistoryItemDto;
import com.easyops.hr.dto.SalaryGradeSummaryDto;
import com.easyops.hr.dto.ProrationDto;
import com.easyops.hr.dto.SalaryStructureSummaryDto;
import com.easyops.hr.entity.AssignmentSource;
import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.ComponentValueType;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.EmployeeSalaryAssignment;
import com.easyops.hr.entity.EmployeeSalaryDetail;
import com.easyops.hr.entity.SalaryAuditLog;
import com.easyops.hr.entity.SalaryBand;
import com.easyops.hr.entity.SalaryBulkRevision;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryComponentCategory;
import com.easyops.hr.entity.SalaryGrade;
import com.easyops.hr.entity.ProrationRule;
import com.easyops.hr.entity.SalaryStructure;
import com.easyops.hr.exception.ResourceConflictException;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.EmployeeSalaryAssignmentRepository;
import com.easyops.hr.repository.EmployeeSalaryDetailRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.SalaryAuditLogRepository;
import com.easyops.hr.repository.SalaryBandRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import com.easyops.hr.repository.SalaryGradeRepository;
import com.easyops.hr.repository.SalaryBulkRevisionRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import com.easyops.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SalaryService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final SalaryGradeRepository salaryGradeRepository;
    private final SalaryBandRepository salaryBandRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryAuditLogRepository salaryAuditLogRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    private final SalaryBulkRevisionRepository salaryBulkRevisionRepository;

    // Salary Structure Methods
    public List<SalaryStructure> getAllSalaryStructures(UUID organizationId) {
        return salaryStructureRepository.findByOrganizationId(organizationId);
    }

    /** SS-41: List structures with optional effective date and includeInactive filter. */
    public List<SalaryStructure> getAllSalaryStructures(UUID organizationId, LocalDate effectiveDate, boolean includeInactive) {
        return salaryStructureRepository.findByOrganizationIdWithFilters(organizationId, effectiveDate, includeInactive);
    }

    public Optional<SalaryStructure> getSalaryStructureById(UUID structureId) {
        return salaryStructureRepository.findById(structureId);
    }

    /** GET by id with optional effective date: returns empty if structure is not effective on the given date. */
    public Optional<SalaryStructure> getSalaryStructureByIdAndEffectiveDate(UUID structureId, LocalDate effectiveDate) {
        Optional<SalaryStructure> opt = salaryStructureRepository.findById(structureId);
        if (opt.isEmpty() || effectiveDate == null) return opt;
        SalaryStructure s = opt.get();
        if (s.getEffectiveFrom() != null && s.getEffectiveFrom().isAfter(effectiveDate)) return Optional.empty();
        if (s.getEffectiveTo() != null && s.getEffectiveTo().isBefore(effectiveDate)) return Optional.empty();
        return Optional.of(s);
    }

    /** SS-42: Get structure by id with nested grades and bands. effectiveDate filters grades/bands when set. */
    public Optional<SalaryStructureSummaryDto> getStructureWithGradesAndBands(UUID structureId, LocalDate effectiveDate) {
        return salaryStructureRepository.findById(structureId)
                .map(s -> buildStructureSummary(s, effectiveDate));
    }

    /** SS-45, SS-47: List structures with nested grades and bands (for payroll or structure summary report). */
    public List<SalaryStructureSummaryDto> getStructureSummaries(UUID organizationId, LocalDate effectiveDate, boolean includeInactive) {
        List<SalaryStructure> structures = salaryStructureRepository.findByOrganizationIdWithFilters(
                organizationId, effectiveDate, includeInactive);
        return structures.stream()
                .map(s -> buildStructureSummary(s, effectiveDate))
                .toList();
    }

    private SalaryStructureSummaryDto buildStructureSummary(SalaryStructure s, LocalDate effectiveDate) {
        List<SalaryGrade> grades = effectiveDate == null
                ? salaryGradeRepository.findBySalaryStructureIdOrderByDisplayOrderAsc(s.getSalaryStructureId())
                : salaryGradeRepository.findBySalaryStructureIdAndEffectiveDate(s.getSalaryStructureId(), effectiveDate);
        List<SalaryGradeSummaryDto> gradeDtos = grades.stream()
                .map(g -> toGradeSummary(g, effectiveDate))
                .toList();
        return SalaryStructureSummaryDto.builder()
                .salaryStructureId(s.getSalaryStructureId())
                .organizationId(s.getOrganizationId())
                .code(s.getCode())
                .structureName(s.getStructureName())
                .description(s.getDescription())
                .currency(s.getCurrency())
                .payFrequency(s.getPayFrequency())
                .isDefault(s.getIsDefault())
                .effectiveFrom(s.getEffectiveFrom())
                .effectiveTo(s.getEffectiveTo())
                .isActive(s.getIsActive())
                .grades(gradeDtos)
                .build();
    }

    private SalaryGradeSummaryDto toGradeSummary(SalaryGrade g, LocalDate effectiveDate) {
        List<SalaryBand> bands = effectiveDate == null
                ? salaryBandRepository.findBySalaryGradeIdOrderByDisplayOrderAsc(g.getSalaryGradeId())
                : salaryBandRepository.findBySalaryGradeIdAndEffectiveDate(g.getSalaryGradeId(), effectiveDate);
        List<SalaryBandSummaryDto> bandDtos = bands.stream().map(this::toBandSummary).toList();
        return SalaryGradeSummaryDto.builder()
                .salaryGradeId(g.getSalaryGradeId())
                .salaryStructureId(g.getSalaryStructureId())
                .code(g.getCode())
                .name(g.getName())
                .displayOrder(g.getDisplayOrder())
                .description(g.getDescription())
                .effectiveFrom(g.getEffectiveFrom())
                .effectiveTo(g.getEffectiveTo())
                .bands(bandDtos)
                .build();
    }

    private SalaryBandSummaryDto toBandSummary(SalaryBand b) {
        return SalaryBandSummaryDto.builder()
                .salaryBandId(b.getSalaryBandId())
                .salaryGradeId(b.getSalaryGradeId())
                .code(b.getCode())
                .name(b.getName())
                .displayOrder(b.getDisplayOrder())
                .minimumAmount(b.getMinimumAmount())
                .maximumAmount(b.getMaximumAmount())
                .midPoint(b.getMidPoint())
                .currency(b.getCurrency())
                .effectiveFrom(b.getEffectiveFrom())
                .effectiveTo(b.getEffectiveTo())
                .build();
    }

    /** SS-48: Grade-wise headcount for org as of date. Structure headcount from salary details; grade/band from position default. */
    public List<GradeHeadcountDto> getGradeWiseHeadcount(UUID organizationId, LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        List<SalaryStructure> structures = salaryStructureRepository.findByOrganizationIdWithFilters(
                organizationId, asOfDate, true);
        List<GradeHeadcountDto> result = new java.util.ArrayList<>();
        for (SalaryStructure s : structures) {
            long structureCount = employeeSalaryDetailRepository.countDistinctEmployeesByStructureAndDate(
                    organizationId, s.getSalaryStructureId(), asOfDate);
            result.add(GradeHeadcountDto.builder()
                    .structureId(s.getSalaryStructureId())
                    .structureCode(s.getCode())
                    .structureName(s.getStructureName())
                    .gradeId(null)
                    .gradeCode(null)
                    .gradeName(null)
                    .bandId(null)
                    .bandCode(null)
                    .bandName(null)
                    .headcount(structureCount)
                    .build());
            List<SalaryGrade> grades = salaryGradeRepository.findBySalaryStructureIdAndEffectiveDate(
                    s.getSalaryStructureId(), asOfDate);
            for (SalaryGrade g : grades) {
                long gradeCount = employeeRepository.countByOrganizationAndDefaultGradeAsOfDate(
                        organizationId, g.getSalaryGradeId(), asOfDate);
                result.add(GradeHeadcountDto.builder()
                        .structureId(s.getSalaryStructureId())
                        .structureCode(s.getCode())
                        .structureName(s.getStructureName())
                        .gradeId(g.getSalaryGradeId())
                        .gradeCode(g.getCode())
                        .gradeName(g.getName())
                        .bandId(null)
                        .bandCode(null)
                        .bandName(null)
                        .headcount(gradeCount)
                        .build());
                List<SalaryBand> bands = salaryBandRepository.findBySalaryGradeIdAndEffectiveDate(
                        g.getSalaryGradeId(), asOfDate);
                for (SalaryBand b : bands) {
                    long bandCount = employeeRepository.countByOrganizationAndDefaultBandAsOfDate(
                            organizationId, b.getSalaryBandId(), asOfDate);
                    result.add(GradeHeadcountDto.builder()
                            .structureId(s.getSalaryStructureId())
                            .structureCode(s.getCode())
                            .structureName(s.getStructureName())
                            .gradeId(g.getSalaryGradeId())
                            .gradeCode(g.getCode())
                            .gradeName(g.getName())
                            .bandId(b.getSalaryBandId())
                            .bandCode(b.getCode())
                            .bandName(b.getName())
                            .headcount(bandCount)
                            .build());
                }
            }
        }
        return result;
    }

    /** ES-53: Employee self-service – view own current salary (structure name, grade name, component names). Amounts may be masked. */
    public Optional<EmployeeSelfSalarySummaryDto> getSelfSalarySummary(UUID employeeId, UUID organizationId, LocalDate asOfDate, boolean maskAmounts) {
        if (asOfDate == null) asOfDate = LocalDate.now();
        Optional<EmployeeSalaryAssignment> assignmentOpt = getAssignment(employeeId, organizationId, asOfDate);
        if (assignmentOpt.isEmpty()) return Optional.empty();
        EmployeeSalaryAssignment a = assignmentOpt.get();
        SalaryStructure structure = salaryStructureRepository.findById(a.getSalaryStructureId()).orElse(null);
        SalaryGrade grade = a.getSalaryGradeId() != null ? salaryGradeRepository.findById(a.getSalaryGradeId()).orElse(null) : null;
        SalaryBand band = a.getSalaryBandId() != null ? salaryBandRepository.findById(a.getSalaryBandId()).orElse(null) : null;

        List<EmployeeSalaryDetail> details = getEmployeeSalaryDetails(employeeId, organizationId, asOfDate);
        List<EmployeeSelfSalarySummaryDto.ComponentLineDto> lines = new ArrayList<>();
        for (EmployeeSalaryDetail d : details) {
            SalaryComponent c = salaryComponentRepository.findById(d.getComponentId()).orElse(null);
            if (c == null) continue;
            BigDecimal amount = null;
            if (!maskAmounts) {
                if (d.getValueType() == ComponentValueType.AMOUNT && d.getAmount() != null) {
                    amount = d.getAmount();
                } else if (d.getValueType() == ComponentValueType.USE_MASTER_DEFAULT && c.getDefaultAmount() != null) {
                    amount = c.getDefaultAmount();
                }
            }
            lines.add(EmployeeSelfSalarySummaryDto.ComponentLineDto.builder()
                    .componentCode(c.getCode())
                    .componentName(c.getShortName() != null ? c.getShortName() : c.getComponentName())
                    .componentType(c.getComponentType())
                    .amount(amount)
                    .valueType(d.getValueType() != null ? d.getValueType().name() : null)
                    .build());
        }
        EmployeeSelfSalarySummaryDto summary = EmployeeSelfSalarySummaryDto.builder()
                .structureCode(structure != null ? structure.getCode() : null)
                .structureName(structure != null ? structure.getStructureName() : null)
                .gradeCode(grade != null ? grade.getCode() : null)
                .gradeName(grade != null ? grade.getName() : null)
                .bandCode(band != null ? band.getCode() : null)
                .bandName(band != null ? band.getName() : null)
                .currency(structure != null ? structure.getCurrency() : null)
                .payFrequency(structure != null ? structure.getPayFrequency() : null)
                .components(lines)
                .build();
        return Optional.of(summary);
    }

    /** RPT-05: Grade-wise headcount and cost from assignments. One row per structure, per grade, per band with headcount and totalCost. */
    public List<GradeHeadcountDto> getGradeWiseHeadcountAndCost(UUID organizationId, LocalDate asOfDate) {
        final LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<EmployeeSalaryAssignment> allActive = employeeSalaryAssignmentRepository.findActiveByOrganizationAndDate(organizationId, date);
        // One assignment per employee (take first when multiple)
        Map<UUID, EmployeeSalaryAssignment> onePerEmployee = allActive.stream()
                .collect(Collectors.toMap(EmployeeSalaryAssignment::getEmployeeId, a -> a, (a, b) -> a));

        // Group by structureId, (structureId+gradeId), (structureId+gradeId+bandId) for headcount and cost
        Map<UUID, List<UUID>> byStructure = new java.util.HashMap<>();
        Map<UUID, List<UUID>> byGrade = new java.util.HashMap<>();
        Map<UUID, List<UUID>> byBand = new java.util.HashMap<>();
        for (EmployeeSalaryAssignment a : onePerEmployee.values()) {
            UUID empId = a.getEmployeeId();
            byStructure.computeIfAbsent(a.getSalaryStructureId(), k -> new ArrayList<>()).add(empId);
            if (a.getSalaryGradeId() != null) {
                byGrade.computeIfAbsent(a.getSalaryGradeId(), k -> new ArrayList<>()).add(empId);
            }
            if (a.getSalaryBandId() != null) {
                byBand.computeIfAbsent(a.getSalaryBandId(), k -> new ArrayList<>()).add(empId);
            }
        }

        List<GradeHeadcountDto> result = new ArrayList<>();
        List<SalaryStructure> structures = salaryStructureRepository.findByOrganizationIdWithFilters(organizationId, date, true);
        for (SalaryStructure s : structures) {
            List<UUID> structureEmployees = byStructure.getOrDefault(s.getSalaryStructureId(), List.of());
            BigDecimal structureCost = structureEmployees.stream()
                    .map(empId -> computeEmployeeMonthlyCost(empId, organizationId, date))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(GradeHeadcountDto.builder()
                    .structureId(s.getSalaryStructureId())
                    .structureCode(s.getCode())
                    .structureName(s.getStructureName())
                    .gradeId(null)
                    .gradeCode(null)
                    .gradeName(null)
                    .bandId(null)
                    .bandCode(null)
                    .bandName(null)
                    .headcount(structureEmployees.size())
                    .totalCost(structureCost)
                    .currency(s.getCurrency())
                    .build());
            List<SalaryGrade> grades = salaryGradeRepository.findBySalaryStructureIdAndEffectiveDate(s.getSalaryStructureId(), date);
            for (SalaryGrade g : grades) {
                List<UUID> gradeEmployees = byGrade.getOrDefault(g.getSalaryGradeId(), List.of());
                BigDecimal gradeCost = gradeEmployees.stream()
                        .map(empId -> computeEmployeeMonthlyCost(empId, organizationId, date))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                result.add(GradeHeadcountDto.builder()
                        .structureId(s.getSalaryStructureId())
                        .structureCode(s.getCode())
                        .structureName(s.getStructureName())
                        .gradeId(g.getSalaryGradeId())
                        .gradeCode(g.getCode())
                        .gradeName(g.getName())
                        .bandId(null)
                        .bandCode(null)
                        .bandName(null)
                        .headcount(gradeEmployees.size())
                        .totalCost(gradeCost)
                        .currency(s.getCurrency())
                        .build());
                List<SalaryBand> bands = salaryBandRepository.findBySalaryGradeIdAndEffectiveDate(g.getSalaryGradeId(), date);
                for (SalaryBand b : bands) {
                    List<UUID> bandEmployees = byBand.getOrDefault(b.getSalaryBandId(), List.of());
                    BigDecimal bandCost = bandEmployees.stream()
                            .map(empId -> computeEmployeeMonthlyCost(empId, organizationId, date))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(GradeHeadcountDto.builder()
                            .structureId(s.getSalaryStructureId())
                            .structureCode(s.getCode())
                            .structureName(s.getStructureName())
                            .gradeId(g.getSalaryGradeId())
                            .gradeCode(g.getCode())
                            .gradeName(g.getName())
                            .bandId(b.getSalaryBandId())
                            .bandCode(b.getCode())
                            .bandName(b.getName())
                            .headcount(bandEmployees.size())
                            .totalCost(bandCost)
                            .currency(b.getCurrency() != null ? b.getCurrency() : s.getCurrency())
                            .build());
                }
            }
        }
        return result;
    }

    /** RPT-05: Sum of EARNING component values for an employee as-of date (amount or USE_MASTER_DEFAULT from component). */
    public BigDecimal computeEmployeeMonthlyCost(UUID employeeId, UUID organizationId, LocalDate asOfDate) {
        List<EmployeeSalaryDetail> details = employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(employeeId, organizationId, asOfDate);
        List<SalaryComponent> components = getSalaryComponentsEffectiveOn(organizationId, asOfDate);
        Map<UUID, SalaryComponent> componentMap = components.stream().collect(Collectors.toMap(SalaryComponent::getComponentId, c -> c, (a, b) -> a));
        BigDecimal total = BigDecimal.ZERO;
        for (EmployeeSalaryDetail d : details) {
            SalaryComponent c = componentMap.get(d.getComponentId());
            if (c == null || !"EARNING".equalsIgnoreCase(c.getComponentType())) continue;
            BigDecimal contrib = null;
            if (d.getValueType() == ComponentValueType.AMOUNT && d.getAmount() != null) {
                contrib = d.getAmount();
            } else if (d.getValueType() == ComponentValueType.USE_MASTER_DEFAULT && c.getDefaultAmount() != null) {
                contrib = c.getDefaultAmount();
            }
            if (contrib != null) {
                total = total.add(contrib);
            }
        }
        return total;
    }

    /** ES-58: Component-wise cost – total amount per component across all employees as of date. */
    public List<ComponentWiseCostDto> getComponentWiseCost(UUID organizationId, LocalDate asOfDate) {
        if (asOfDate == null) asOfDate = LocalDate.now();
        List<EmployeeSalaryAssignment> allActive = employeeSalaryAssignmentRepository.findActiveByOrganizationAndDate(organizationId, asOfDate);
        Map<UUID, EmployeeSalaryAssignment> onePerEmployee = allActive.stream()
                .collect(Collectors.toMap(EmployeeSalaryAssignment::getEmployeeId, a -> a, (a, b) -> a));
        List<SalaryComponent> components = getSalaryComponentsEffectiveOn(organizationId, asOfDate);
        Map<UUID, SalaryComponent> componentMap = components.stream().collect(Collectors.toMap(SalaryComponent::getComponentId, c -> c, (a, b) -> a));

        Map<UUID, BigDecimal> totalByComponent = new java.util.HashMap<>();
        Map<UUID, Set<UUID>> employeesByComponent = new java.util.HashMap<>();
        for (UUID employeeId : onePerEmployee.keySet()) {
            List<EmployeeSalaryDetail> details = employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(employeeId, organizationId, asOfDate);
            for (EmployeeSalaryDetail d : details) {
                SalaryComponent c = componentMap.get(d.getComponentId());
                if (c == null) continue;
                BigDecimal amount = null;
                if (d.getValueType() == ComponentValueType.AMOUNT && d.getAmount() != null) {
                    amount = d.getAmount();
                } else if (d.getValueType() == ComponentValueType.USE_MASTER_DEFAULT && c.getDefaultAmount() != null) {
                    amount = c.getDefaultAmount();
                }
                if (amount != null) {
                    totalByComponent.merge(d.getComponentId(), amount, BigDecimal::add);
                    employeesByComponent.computeIfAbsent(d.getComponentId(), k -> new HashSet<>()).add(employeeId);
                }
            }
        }
        List<ComponentWiseCostDto> result = new ArrayList<>();
        for (SalaryComponent c : components) {
            BigDecimal total = totalByComponent.getOrDefault(c.getComponentId(), BigDecimal.ZERO);
            int count = employeesByComponent.getOrDefault(c.getComponentId(), Set.of()).size();
            result.add(ComponentWiseCostDto.builder()
                    .componentId(c.getComponentId())
                    .componentCode(c.getCode())
                    .componentName(c.getComponentName())
                    .componentType(c.getComponentType())
                    .category(c.getCategory() != null ? c.getCategory().name() : null)
                    .totalAmount(total)
                    .employeeCount(count)
                    .currency(c.getCurrency() != null ? c.getCurrency() : null)
                    .build());
        }
        return result;
    }

    /**
     * Create a salary structure. Validates: code uniqueness (per org), effective dates (effectiveTo >= effectiveFrom),
     * and optional payroll-closed period check (SS-30: rejects if effective period overlaps a closed payroll run).
     */
    public SalaryStructure createSalaryStructure(SalaryStructure structure) {
        String code = structure.getCode();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Salary structure code is required");
        }
        UUID orgId = structure.getOrganizationId();
        if (orgId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        structure.setCode(code.trim());
        LocalDate from = structure.getEffectiveFrom();
        LocalDate to = structure.getEffectiveTo();
        if (from == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        validateEffectiveDates(from, to);
        ensureNoOverlappingStructure(orgId, structure.getCode(), from, to, null);
        if (structure.getCurrency() == null || structure.getCurrency().isBlank()) {
            structure.setCurrency("BDT");
        }
        if (Boolean.TRUE.equals(structure.getIsDefault())) {
            clearOtherDefaultsForOrg(structure.getOrganizationId(), null);
        }
        if (structure.getIsDefault() == null) {
            structure.setIsDefault(false);
        }
        ensureNotInClosedPayrollPeriod(orgId, from, to);
        log.info("Creating salary structure: {} (code: {})", structure.getStructureName(), structure.getCode());
        SalaryStructure created = salaryStructureRepository.save(structure);
        auditLog(created.getOrganizationId(), SalaryAuditLog.ENTITY_STRUCTURE, created.getSalaryStructureId(),
            SalaryAuditLog.ACTION_CREATE, null, structureSummary(created), created.getCreatedBy());
        return created;
    }

    /** Clears isDefault for all structures in the org except the one with excludeStructureId (null = clear all). */
    private void clearOtherDefaultsForOrg(UUID organizationId, UUID excludeStructureId) {
        List<SalaryStructure> others = salaryStructureRepository.findByOrganizationId(organizationId).stream()
                .filter(s -> !s.getSalaryStructureId().equals(excludeStructureId))
                .filter(s -> Boolean.TRUE.equals(s.getIsDefault()))
                .toList();
        for (SalaryStructure s : others) {
            s.setIsDefault(false);
            salaryStructureRepository.save(s);
        }
    }

    /**
     * Update a salary structure. Code is immutable. Validates effective dates (effectiveTo >= effectiveFrom)
     * and optional payroll-closed period check (SS-30).
     */
    public SalaryStructure updateSalaryStructure(UUID structureId, SalaryStructure structure) {
        SalaryStructure existing = salaryStructureRepository.findById(structureId)
                .orElseThrow(() -> new RuntimeException("Salary structure not found"));
        String oldSummary = structureSummary(existing);
        LocalDate from = structure.getEffectiveFrom() != null ? structure.getEffectiveFrom() : existing.getEffectiveFrom();
        LocalDate to = structure.getEffectiveTo() != null ? structure.getEffectiveTo() : existing.getEffectiveTo();
        if (structure.getEffectiveFrom() != null || structure.getEffectiveTo() != null) {
            validateEffectiveDates(from, to);
            ensureNotInClosedPayrollPeriod(existing.getOrganizationId(), from, to);
        } else {
            ensureNotInClosedPayrollPeriod(existing.getOrganizationId(), existing.getEffectiveFrom(), existing.getEffectiveTo());
        }
        // Code is immutable: never update from request
        if (structure.getStructureName() != null) existing.setStructureName(structure.getStructureName());
        if (structure.getDescription() != null) existing.setDescription(structure.getDescription());
        if (structure.getCurrency() != null) existing.setCurrency(structure.getCurrency());
        if (structure.getPayFrequency() != null) existing.setPayFrequency(structure.getPayFrequency());
        if (structure.getIsActive() != null) existing.setIsActive(structure.getIsActive());
        if (structure.getEffectiveFrom() != null || structure.getEffectiveTo() != null) {
            if (existing.getCode() != null) {
                ensureNoOverlappingStructure(
                    existing.getOrganizationId(),
                    existing.getCode(),
                    from,
                    to,
                    structureId
                );
            }
            if (structure.getEffectiveFrom() != null) existing.setEffectiveFrom(structure.getEffectiveFrom());
            if (structure.getEffectiveTo() != null) existing.setEffectiveTo(structure.getEffectiveTo());
        }
        if (structure.getIsDefault() != null) {
            existing.setIsDefault(structure.getIsDefault());
            if (Boolean.TRUE.equals(structure.getIsDefault())) {
                clearOtherDefaultsForOrg(existing.getOrganizationId(), structureId);
            }
        }
        SalaryStructure updated = salaryStructureRepository.save(existing);
        auditLog(updated.getOrganizationId(), SalaryAuditLog.ENTITY_STRUCTURE, updated.getSalaryStructureId(),
            SalaryAuditLog.ACTION_UPDATE, oldSummary, structureSummary(updated), updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy());
        return updated;
    }

    /** SS-23: effectiveTo must be >= effectiveFrom; effectiveTo may be null (open-ended). */
    private static void validateEffectiveDates(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveTo != null && effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be greater than or equal to effectiveFrom");
        }
    }

    /**
     * SS-02: Only one active structure per code per effective date within scope (organization).
     * Throws ResourceConflictException if another structure with the same org+code has an overlapping effective period.
     */
    private void ensureNoOverlappingStructure(UUID organizationId, String code,
                                             LocalDate effectiveFrom, LocalDate effectiveTo,
                                             UUID excludeStructureId) {
        List<SalaryStructure> overlapping = salaryStructureRepository.findOverlappingByOrganizationAndCode(
            organizationId, code, effectiveFrom, effectiveTo, excludeStructureId
        );
        if (!overlapping.isEmpty()) {
            throw new ResourceConflictException(
                "Another salary structure with code '" + code + "' already has an overlapping effective period within this organization. "
                    + "Only one active structure per code is allowed for any given date (SS-02)."
            );
        }
    }

    /** SS-30: Reject changes if structure effective period overlaps a closed (processed/approved) payroll period. */
    private void ensureNotInClosedPayrollPeriod(UUID organizationId, LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null) return;
        LocalDate end = periodEnd != null ? periodEnd : LocalDate.of(9999, 12, 31);
        List<?> closed = payrollRunRepository.findClosedRunsOverlapping(organizationId, periodStart, end);
        if (!closed.isEmpty()) {
            throw new IllegalArgumentException(
                "Structure effective period overlaps a closed payroll period. Edits affecting closed periods are not allowed (SS-30)."
            );
        }
    }

    /** SS-28: Write audit log entry for structure/grade/band change. */
    private void auditLog(UUID organizationId, String entityType, UUID entityId, String action,
                         String oldValues, String newValues, String performedBy) {
        SalaryAuditLog logEntry = new SalaryAuditLog();
        logEntry.setOrganizationId(organizationId);
        logEntry.setEntityType(entityType);
        logEntry.setEntityId(entityId);
        logEntry.setAction(action);
        logEntry.setPerformedBy(performedBy);
        logEntry.setPerformedAt(LocalDateTime.now());
        logEntry.setOldValues(oldValues);
        logEntry.setNewValues(newValues);
        salaryAuditLogRepository.save(logEntry);
    }

    private static String structureSummary(SalaryStructure s) {
        return "code=" + s.getCode() + ",name=" + s.getStructureName() + ",effectiveFrom=" + s.getEffectiveFrom()
            + ",effectiveTo=" + s.getEffectiveTo() + ",isActive=" + s.getIsActive();
    }

    /** SC-30: Summary string for component audit log (code, type, effective dates, isActive). */
    private static String componentSummary(SalaryComponent c) {
        if (c == null) return "";
        return "code=" + c.getCode() + ",type=" + c.getComponentType() + ",effectiveFrom=" + c.getEffectiveFrom()
            + ",effectiveTo=" + c.getEffectiveTo() + ",isActive=" + c.getIsActive()
            + ",expenseGl=" + c.getExpenseAccountCode() + ",liabilityGl=" + c.getLiabilityAccountCode();
    }

    /** SC-23: Statutory tags where only one component per org is recommended (warn if multiple). */
    private static final Set<String> SINGLE_USE_STATUTORY_TAGS = Set.of(
        "PF_EMPLOYEE", "PF_EMPLOYER", "INCOME_TAX", "ESI_EMPLOYEE", "ESI_EMPLOYER");

    /** SC-23: Log warning if multiple active components in the org have the same single-use statutory tag. */
    private void warnDuplicateStatutoryTags(SalaryComponent component) {
        if (component.getStatutoryTags() == null || component.getStatutoryTags().isEmpty()) return;
        UUID orgId = component.getOrganizationId();
        UUID componentId = component.getComponentId();
        for (String tag : component.getStatutoryTags()) {
            if (tag == null || !SINGLE_USE_STATUTORY_TAGS.contains(tag.trim().toUpperCase())) continue;
            List<SalaryComponent> withTag = salaryComponentRepository.findByOrganizationIdAndIsActiveTrueAndStatutoryTag(orgId, tag.trim());
            long others = withTag.stream().filter(c -> !c.getComponentId().equals(componentId)).count();
            if (others > 0) {
                log.warn("SC-23: Multiple components have statutory tag '{}' in organization {}. Only one is recommended per organization to avoid double application. Component: {} ({})",
                    tag, orgId, component.getCode(), component.getComponentName());
            }
        }
    }

    /** SC-22: Components with the given statutory tag (e.g. PF_WAGE for PF wage base = sum of their amounts). */
    public List<SalaryComponent> getSalaryComponentsWithStatutoryTag(UUID organizationId, String tag) {
        if (organizationId == null || tag == null || tag.isBlank()) return List.of();
        return salaryComponentRepository.findByOrganizationIdAndIsActiveTrueAndStatutoryTag(organizationId, tag.trim());
    }

    /** SS-29: Revision history for a structure (audit log entries). */
    public List<SalaryAuditLog> getStructureRevisionHistory(UUID organizationId, UUID structureId) {
        return salaryAuditLogRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
            organizationId, SalaryAuditLog.ENTITY_STRUCTURE, structureId);
    }

    // Salary Component Methods (SC-01, SC-06: code unique per org, immutable after creation; SC-04, SC-05: effective dates, display order)
    public List<SalaryComponent> getAllSalaryComponents(UUID organizationId) {
        return salaryComponentRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
    }

    /** SC-04: Components effective on the given date (for payroll/assignment). Ordered by displayOrder. Active only. */
    public List<SalaryComponent> getSalaryComponentsEffectiveOn(UUID organizationId, LocalDate effectiveDate) {
        return getSalaryComponentsEffectiveOn(organizationId, effectiveDate, false);
    }

    /** SC-46: Components effective on the given date, optionally including inactive. Ordered by displayOrder. */
    public List<SalaryComponent> getSalaryComponentsEffectiveOn(UUID organizationId, LocalDate effectiveDate, boolean includeInactive) {
        if (effectiveDate == null) {
            List<SalaryComponent> list = salaryComponentRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
            if (!includeInactive) {
                list = list.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();
            }
            return list;
        }
        if (includeInactive) {
            return salaryComponentRepository.findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAscIncludeInactive(organizationId, effectiveDate);
        }
        return salaryComponentRepository.findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAsc(organizationId, effectiveDate);
    }

    /** SC-47: List for payroll: components effective on date, active only, ordered by displayOrder. */
    public List<SalaryComponent> getSalaryComponentsForPayroll(UUID organizationId, LocalDate effectiveDate) {
        if (organizationId == null) return List.of();
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
        return salaryComponentRepository.findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAsc(organizationId, effectiveDate);
    }

    /** ES-22: Components in dependency order for payroll (base/formula refs before dependents; displayOrder as secondary). */
    public List<SalaryComponent> getComponentsInDependencyOrder(UUID organizationId, LocalDate effectiveDate) {
        List<SalaryComponent> list = getSalaryComponentsForPayroll(organizationId, effectiveDate);
        if (list.isEmpty()) return list;
        Map<String, SalaryComponent> byCode = list.stream().filter(c -> c.getCode() != null).collect(Collectors.toMap(SalaryComponent::getCode, c -> c, (a, b) -> a));
        Set<String> done = new HashSet<>();
        List<SalaryComponent> result = new ArrayList<>();
        int maxIterations = list.size() * 2;
        while (result.size() < list.size() && maxIterations-- > 0) {
            boolean added = false;
            for (SalaryComponent c : list) {
                if (result.contains(c)) continue;
                String code = c.getCode();
                if (code == null || code.isBlank()) {
                    result.add(c);
                    added = true;
                    continue;
                }
                Set<String> refs = new HashSet<>();
                if (c.getBaseComponentCode() != null && !c.getBaseComponentCode().isBlank()) refs.add(c.getBaseComponentCode().trim());
                if (c.getFormulaExpression() != null && !c.getFormulaExpression().isBlank()) refs.addAll(extractComponentCodesFromFormula(c.getFormulaExpression()));
                boolean depsDone = refs.stream().allMatch(ref -> byCode.containsKey(ref) ? done.contains(ref) : true);
                if (depsDone) {
                    result.add(c);
                    done.add(code);
                    added = true;
                }
            }
            if (!added) break;
        }
        if (result.size() < list.size()) {
            for (SalaryComponent c : list) {
                if (!result.contains(c)) result.add(c);
            }
        }
        return result;
    }

    /** SC-37, SC-45: List components with filters (type, category, effectiveDate, includeInactive). */
    public List<SalaryComponent> getSalaryComponentsWithFilters(UUID organizationId, String type,
            SalaryComponentCategory category, LocalDate effectiveDate, boolean includeInactive) {
        List<SalaryComponent> list = salaryComponentRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
        if (type != null && !type.isBlank()) {
            String t = type.trim();
            list = list.stream().filter(c -> t.equalsIgnoreCase(c.getComponentType())).toList();
        }
        if (category != null) {
            list = list.stream().filter(c -> category.equals(c.getCategory())).toList();
        }
        if (effectiveDate != null) {
            LocalDate d = effectiveDate;
            list = list.stream().filter(c ->
                (c.getEffectiveFrom() == null || !c.getEffectiveFrom().isAfter(d))
                && (c.getEffectiveTo() == null || !c.getEffectiveTo().isBefore(d))).toList();
        }
        if (!includeInactive) {
            list = list.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();
        }
        return list;
    }

    public Optional<SalaryComponent> getSalaryComponentById(UUID componentId) {
        return salaryComponentRepository.findById(componentId);
    }

    public Optional<SalaryComponent> getSalaryComponentByOrganizationIdAndCode(UUID organizationId, String code) {
        return salaryComponentRepository.findByOrganizationIdAndCode(organizationId, code != null ? code.trim() : null);
    }

    /** SC-09: Normalize calculation type string to CalculationBasis enum; default FIXED. */
    private static CalculationBasis normalizeCalculationBasis(String calculationType) {
        if (calculationType == null || calculationType.isBlank()) return CalculationBasis.FIXED;
        String u = calculationType.trim().toUpperCase();
        try {
            return CalculationBasis.valueOf(u);
        } catch (IllegalArgumentException e) {
            if ("PERCENTAGE".equals(u) || "PERCENTAGEOFBASIC".equals(u.replace("_", ""))) return CalculationBasis.PERCENTAGE_OF_BASIC;
            if ("PERCENTAGEOFGROSS".equals(u.replace("_", ""))) return CalculationBasis.PERCENTAGE_OF_GROSS;
            return CalculationBasis.FIXED;
        }
    }

    /** SC-15, SC-27, SC-28: Validate calculation fields, base component exists and is earning, no circular ref, ceiling >= floor. */
    private void validateComponentCalculationFields(SalaryComponent component, UUID organizationId, UUID excludeComponentId) {
        CalculationBasis basis = component.getCalculationBasis() != null
            ? normalizeCalculationBasis(component.getCalculationBasis().name())
            : CalculationBasis.FIXED;
        component.setCalculationBasis(basis);

        // SC-11, SC-26: Percentage non-negative
        if (component.getPercentageValue() != null && component.getPercentageValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage must be non-negative (SC-26).");
        }

        // SC-27: Ceiling >= floor when both set
        if (component.getCeilingAmount() != null && component.getFloorAmount() != null
                && component.getCeilingAmount().compareTo(component.getFloorAmount()) < 0) {
            throw new IllegalArgumentException("Ceiling must be greater than or equal to floor (SC-27).");
        }

        if (basis == CalculationBasis.PERCENTAGE_OF_BASIC || basis == CalculationBasis.PERCENTAGE_OF_GROSS) {
            if (component.getBaseComponentCode() == null || component.getBaseComponentCode().isBlank()) {
                throw new IllegalArgumentException("Base component code is required for " + basis + " (SC-11).");
            }
            String baseCode = component.getBaseComponentCode().trim();
            SalaryComponent base = salaryComponentRepository.findByOrganizationIdAndCode(organizationId, baseCode)
                    .orElseThrow(() -> new IllegalArgumentException("Base component '" + baseCode + "' does not exist in this organization (SC-15)."));
            if (excludeComponentId != null && base.getComponentId().equals(excludeComponentId)) {
                throw new IllegalArgumentException("Base component cannot be the same as this component (circular reference).");
            }
            if (!"EARNING".equalsIgnoreCase(base.getComponentType())) {
                throw new IllegalArgumentException("Base component must be an earning component (SC-11).");
            }
            component.setBaseComponentCode(baseCode);
        }

        if (basis == CalculationBasis.FORMULA) {
            String formula = component.getFormulaExpression();
            if (formula == null || formula.isBlank()) {
                throw new IllegalArgumentException("Formula expression is required for FORMULA calculation basis (SC-12).");
            }
            Set<String> refCodes = extractComponentCodesFromFormula(formula);
            List<SalaryComponent> allInOrg = salaryComponentRepository.findByOrganizationId(organizationId);
            Set<String> orgCodes = new HashSet<>();
            for (SalaryComponent c : allInOrg) {
                if (c.getCode() != null) orgCodes.add(c.getCode());
            }
            String currentCode = component.getCode();
            for (String ref : refCodes) {
                if (!orgCodes.contains(ref)) {
                    throw new IllegalArgumentException("Formula references component '" + ref + "' which does not exist in this organization (SC-12).");
                }
                if (ref.equals(currentCode)) {
                    throw new IllegalArgumentException("Formula cannot reference the same component (circular reference, SC-28).");
                }
            }
            // Full circular check: any ref (direct or transitive) pointing back to current
            if (currentCode != null && hasCircularDependency(organizationId, currentCode, refCodes, excludeComponentId)) {
                throw new IllegalArgumentException("Formula introduces a circular dependency (SC-28).");
            }
        }

        if (basis == CalculationBasis.STATUTORY) {
            if (component.getStatutoryType() == null || component.getStatutoryType().isBlank()) {
                throw new IllegalArgumentException("Statutory type is required for STATUTORY calculation basis (SC-13).");
            }
        }
    }

    /** Extract component code identifiers from formula (allowed: letters, digits, underscore). */
    private static Set<String> extractComponentCodesFromFormula(String formula) {
        Set<String> codes = new HashSet<>();
        if (formula == null) return codes;
        String[] tokens = formula.split("[+\\-*/()\\s]+");
        Pattern number = Pattern.compile("^\\d+\\.?\\d*$");
        for (String t : tokens) {
            String s = t.trim();
            if (s.isEmpty()) continue;
            if (number.matcher(s).matches()) continue;
            if (s.matches("^[A-Za-z_][A-Za-z0-9_]*$")) codes.add(s);
        }
        return codes;
    }

    /** Check if adding dependency from currentCode to refCodes would create a cycle (refCodes -> ... -> currentCode). */
    private boolean hasCircularDependency(UUID organizationId, String currentCode, Set<String> refCodes, UUID excludeComponentId) {
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>(refCodes);
        while (!queue.isEmpty()) {
            String code = queue.remove(0);
            if (code.equals(currentCode)) return true;
            if (!visited.add(code)) continue;
            SalaryComponent c = salaryComponentRepository.findByOrganizationIdAndCode(organizationId, code).orElse(null);
            if (c == null || (excludeComponentId != null && c.getComponentId().equals(excludeComponentId))) continue;
            if (c.getBaseComponentCode() != null && !c.getBaseComponentCode().isBlank()) {
                queue.add(c.getBaseComponentCode().trim());
            }
            if (c.getFormulaExpression() != null && !c.getFormulaExpression().isBlank()) {
                queue.addAll(extractComponentCodesFromFormula(c.getFormulaExpression()));
            }
        }
        return false;
    }

    /** SC-19, SC-29: Ensure org has at least one active Basic earning. When deactivating or changing away from Basic, block if this is the only one and others depend on it. */
    private void ensureAtLeastOneBasicEarningWhenRemoving(UUID organizationId, SalaryComponent component, boolean deactivatingOrRemovingBasic) {
        if (!deactivatingOrRemovingBasic) return;
        long basicCount = salaryComponentRepository.countByOrganizationIdAndComponentTypeAndCategoryAndIsActive(
                organizationId, "EARNING", SalaryComponentCategory.BASIC, true);
        if (basicCount <= 1) {
            // Check if any other component references this one (base or formula)
            List<SalaryComponent> refsByBase = salaryComponentRepository.findByOrganizationIdAndBaseComponentCode(organizationId, component.getCode());
            List<SalaryComponent> all = salaryComponentRepository.findByOrganizationId(organizationId);
            long refsByFormula = all.stream()
                    .filter(c -> c.getFormulaExpression() != null && !c.getFormulaExpression().isBlank()
                            && extractComponentCodesFromFormula(c.getFormulaExpression()).contains(component.getCode()))
                    .count();
            if (!refsByBase.isEmpty() || refsByFormula > 0) {
                throw new ResourceConflictException(
                        "Cannot deactivate or change this component: it is the only Basic earning and other components depend on it (SC-29). Define another Basic first.");
            }
        }
    }

    public SalaryComponent createSalaryComponent(SalaryComponent component) {
        String code = component.getCode();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Salary component code is required");
        }
        UUID orgId = component.getOrganizationId();
        if (orgId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        code = code.trim();
        if (salaryComponentRepository.existsByOrganizationIdAndCode(orgId, code)) {
            throw new ResourceConflictException(
                "Salary component with code '" + code + "' already exists in this organization. Code must be unique per organization (SC-06).");
        }
        component.setCode(code);
        if (component.getComponentName() == null || component.getComponentName().isBlank()) {
            throw new IllegalArgumentException("Component name is required");
        }
        // SC-04: default effectiveFrom to today if null; validate effectiveTo >= effectiveFrom
        if (component.getEffectiveFrom() == null) {
            component.setEffectiveFrom(LocalDate.now());
        }
        if (component.getDisplayOrder() == null) {
            component.setDisplayOrder(0);
        }
        validateEffectiveDates(component.getEffectiveFrom(), component.getEffectiveTo());
        validateComponentCalculationFields(component, orgId, null);
        if (component.getStatutoryTags() == null) component.setStatutoryTags(new ArrayList<>());
        if (component.getTaxability() != null) component.setTaxability(component.getTaxability());
        log.info("Creating salary component: {} (code: {})", component.getComponentName(), code);
        SalaryComponent created = salaryComponentRepository.save(component);
        warnDuplicateStatutoryTags(created);
        auditLog(created.getOrganizationId(), SalaryAuditLog.ENTITY_COMPONENT, created.getComponentId(),
            SalaryAuditLog.ACTION_CREATE, null, componentSummary(created),
            created.getCreatedBy() != null ? created.getCreatedBy() : "system");
        return created;
    }

    /**
     * Ensures payroll can store line items for overtime and LOP by creating {@code OT_PAY} (earning) and
     * {@code LOP_DED} (deduction) salary components when missing. Idempotent per organization.
     */
    @Transactional
    public void ensureAttendancePayrollLineComponents(UUID organizationId) {
        ensureAttendanceLineComponent(organizationId, "OT_PAY", "Overtime Pay", "EARNING", SalaryComponentCategory.OTHER_ALLOWANCE);
        ensureAttendanceLineComponent(organizationId, "LOP_DED", "Loss of Pay", "DEDUCTION", SalaryComponentCategory.OTHER_DEDUCTION);
        ensureAttendanceLineComponent(organizationId, "BONUS_PAY", "Bonus Payment", "EARNING", SalaryComponentCategory.OTHER_ALLOWANCE);
    }

    private void ensureAttendanceLineComponent(
            UUID organizationId,
            String code,
            String name,
            String componentType,
            SalaryComponentCategory category) {
        if (salaryComponentRepository.existsByOrganizationIdAndCode(organizationId, code)) {
            return;
        }
        SalaryComponent c = new SalaryComponent();
        c.setOrganizationId(organizationId);
        c.setCode(code);
        c.setComponentName(name);
        c.setDescription("Auto-created for payroll time and attendance line items.");
        c.setComponentType(componentType);
        c.setCategory(category);
        c.setCalculationBasis(CalculationBasis.FIXED);
        c.setDefaultAmount(BigDecimal.ZERO);
        c.setIsActive(true);
        c.setDisplayOrder(9999);
        c.setProrationRule(ProrationRule.NO_PRORATION);
        if ("EARNING".equalsIgnoreCase(componentType)) {
            c.setIsTaxable(true);
        }
        createSalaryComponent(c);
    }

    public SalaryComponent updateSalaryComponent(UUID componentId, SalaryComponent component) {
        SalaryComponent existing = salaryComponentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Salary component not found"));
        UUID orgId = existing.getOrganizationId();
        String oldSummary = componentSummary(existing);
        boolean wasOpenEnded = existing.getEffectiveTo() == null;
        // SC-29: Before changing isActive or category, ensure we don't remove the only Basic when others depend on it
        boolean wasBasicEarning = SalaryComponentCategory.BASIC.equals(existing.getCategory()) && "EARNING".equalsIgnoreCase(existing.getComponentType());
        boolean deactivating = component.getIsActive() != null && !component.getIsActive();
        boolean removingBasic = component.getCategory() != null && !SalaryComponentCategory.BASIC.equals(component.getCategory()) && wasBasicEarning;
        if (wasBasicEarning && (deactivating || removingBasic)) {
            ensureAtLeastOneBasicEarningWhenRemoving(orgId, existing, true);
        }
        // SC-33: Code and component type are immutable after creation — never update from request
        if (component.getComponentName() != null) existing.setComponentName(component.getComponentName());
        if (component.getDescription() != null) existing.setDescription(component.getDescription());
        // Do not set componentType from request (SC-33)
        if (component.getCalculationBasis() != null) {
            existing.setCalculationBasis(normalizeCalculationBasis(component.getCalculationBasis().name()));
        }
        if (component.getIsTaxable() != null) existing.setIsTaxable(component.getIsTaxable());
        if (component.getIsStatutory() != null) existing.setIsStatutory(component.getIsStatutory());
        if (component.getIsActive() != null) existing.setIsActive(component.getIsActive());
        if (component.getCategory() != null) existing.setCategory(component.getCategory());
        // SC-04, SC-05: effective dates and display order
        if (component.getEffectiveFrom() != null) existing.setEffectiveFrom(component.getEffectiveFrom());
        if (component.getEffectiveTo() != null) existing.setEffectiveTo(component.getEffectiveTo());
        if (component.getDisplayOrder() != null) existing.setDisplayOrder(component.getDisplayOrder());
        // SC-09–SC-18: calculation basis fields
        if (component.getDefaultAmount() != null) existing.setDefaultAmount(component.getDefaultAmount());
        if (component.getPercentageValue() != null) existing.setPercentageValue(component.getPercentageValue());
        if (component.getBaseComponentCode() != null) existing.setBaseComponentCode(component.getBaseComponentCode().trim().isEmpty() ? null : component.getBaseComponentCode().trim());
        if (component.getFormulaExpression() != null) existing.setFormulaExpression(component.getFormulaExpression().trim().isEmpty() ? null : component.getFormulaExpression().trim());
        if (component.getStatutoryType() != null) existing.setStatutoryType(component.getStatutoryType().trim().isEmpty() ? null : component.getStatutoryType().trim());
        if (component.getCeilingAmount() != null) existing.setCeilingAmount(component.getCeilingAmount());
        if (component.getFloorAmount() != null) existing.setFloorAmount(component.getFloorAmount());
        if (component.getRoundingRule() != null) existing.setRoundingRule(component.getRoundingRule().trim().isEmpty() ? null : component.getRoundingRule().trim());
        if (component.getApplicabilityRule() != null) existing.setApplicabilityRule(component.getApplicabilityRule().trim().isEmpty() ? null : component.getApplicabilityRule().trim());
        validateEffectiveDates(existing.getEffectiveFrom(), existing.getEffectiveTo());
        validateComponentCalculationFields(existing, orgId, componentId);
        // SC-07: short name / payslip label (optional)
        if (component.getShortName() != null) existing.setShortName(component.getShortName().trim().isEmpty() ? null : component.getShortName().trim());
        // SC-08: optional currency
        if (component.getCurrency() != null) existing.setCurrency(component.getCurrency().trim().isEmpty() ? null : component.getCurrency().trim());
        // ES-29: proration rule per component
        if (component.getProrationRule() != null) existing.setProrationRule(component.getProrationRule());
        // SC-20, SC-21: statutory tags and taxability
        if (component.getTaxability() != null) existing.setTaxability(component.getTaxability());
        if (component.getStatutoryTags() != null) {
            existing.getStatutoryTags().clear();
            existing.getStatutoryTags().addAll(component.getStatutoryTags());
        }
        if (component.getExpenseAccountCode() != null) {
            String eg = component.getExpenseAccountCode().trim();
            existing.setExpenseAccountCode(eg.isEmpty() ? null : eg);
        }
        if (component.getLiabilityAccountCode() != null) {
            String lg = component.getLiabilityAccountCode().trim();
            existing.setLiabilityAccountCode(lg.isEmpty() ? null : lg);
        }

        SalaryComponent updated = salaryComponentRepository.save(existing);
        warnDuplicateStatutoryTags(updated);
        String newSummary = componentSummary(updated);
        String performedBy = updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy();
        if (performedBy == null) performedBy = "system";
        auditLog(orgId, SalaryAuditLog.ENTITY_COMPONENT, updated.getComponentId(), SalaryAuditLog.ACTION_UPDATE, oldSummary, newSummary, performedBy);
        if (deactivating || (component.getEffectiveTo() != null && wasOpenEnded)) {
            auditLog(orgId, SalaryAuditLog.ENTITY_COMPONENT, updated.getComponentId(), SalaryAuditLog.ACTION_DEACTIVATE, oldSummary, newSummary, performedBy);
        }
        return updated;
    }

    /** SC-41, SC-32, SC-34: Usage of a component — employee count, formula refs, payroll result count. For warn on deactivate and prevent delete. */
    public ComponentUsageDto getComponentUsage(UUID componentId) {
        SalaryComponent component = salaryComponentRepository.findById(componentId).orElse(null);
        if (component == null) {
            return ComponentUsageDto.builder().employeeCount(0).referencedInFormulasCount(0).payrollResultCount(0).build();
        }
        long employeeCount = employeeSalaryDetailRepository.countDistinctEmployeesByComponentId(componentId);
        long payrollResultCount = payrollComponentRepository.countByComponentId(componentId);
        UUID orgId = component.getOrganizationId();
        String code = component.getCode();
        if (orgId == null || code == null || code.isBlank()) {
            return ComponentUsageDto.builder().employeeCount(employeeCount).referencedInFormulasCount(0).payrollResultCount(payrollResultCount).build();
        }
        List<SalaryComponent> refsByBase = salaryComponentRepository.findByOrganizationIdAndBaseComponentCode(orgId, code);
        List<SalaryComponent> all = salaryComponentRepository.findByOrganizationId(orgId);
        long refsByFormula = all.stream()
                .filter(c -> !c.getComponentId().equals(componentId))
                .filter(c -> c.getFormulaExpression() != null && !c.getFormulaExpression().isBlank()
                        && extractComponentCodesFromFormula(c.getFormulaExpression()).contains(code))
                .count();
        return ComponentUsageDto.builder()
                .employeeCount(employeeCount)
                .referencedInFormulasCount(refsByBase.size() + refsByFormula)
                .payrollResultCount(payrollResultCount)
                .build();
    }

    /** SC-32: Throws if component is used in any employee salary or past payroll. Use before any physical delete; we do not expose delete, only deactivate (SC-31). */
    public void preventDeleteIfUsed(UUID componentId) {
        long empCount = employeeSalaryDetailRepository.countByComponentId(componentId);
        long payrollCount = payrollComponentRepository.countByComponentId(componentId);
        if (empCount > 0 || payrollCount > 0) {
            throw new ResourceConflictException(
                "Cannot delete this salary component: it is referenced in " + empCount + " employee salary record(s) and " + payrollCount + " payroll result line(s). Deactivate instead (SC-32).");
        }
    }

    /** SC-30: Revision history for a component (audit log entries). */
    public List<SalaryAuditLog> getComponentRevisionHistory(UUID organizationId, UUID componentId) {
        return salaryAuditLogRepository.findByOrganizationIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
            organizationId, SalaryAuditLog.ENTITY_COMPONENT, componentId);
    }

    /** ES-28–ES-30: Proration for pay period – days worked from join/relieving, optional prorated amount by component rule. */
    public ProrationDto getProrationForPeriod(UUID employeeId, LocalDate periodStart, LocalDate periodEnd,
            LocalDate joinOverride, LocalDate relievingOverride, UUID componentId, BigDecimal fullAmount) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        LocalDate hireDate = employee.getHireDate();
        LocalDate terminationDate = employee.getTerminationDate();

        int daysWorked = ProrationService.daysWorkedInPeriod(
                periodStart, periodEnd, hireDate, terminationDate, joinOverride, relievingOverride);
        int totalDays = ProrationService.totalDaysInPeriod(periodStart, periodEnd);

        LocalDate joinDateUsed = joinOverride != null ? joinOverride : hireDate;
        LocalDate relievingDateUsed = relievingOverride != null ? relievingOverride
                : (terminationDate != null ? terminationDate : periodEnd);

        ProrationRule rule = ProrationRule.BY_DAYS;
        BigDecimal proratedAmount = null;
        if (fullAmount != null && fullAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (componentId != null) {
                Optional<SalaryComponent> comp = salaryComponentRepository.findById(componentId);
                if (comp.isPresent() && comp.get().getProrationRule() != null) {
                    rule = comp.get().getProrationRule();
                }
            }
            proratedAmount = ProrationService.prorate(fullAmount, rule, daysWorked, totalDays, null, null);
        }

        return ProrationDto.builder()
                .daysWorked(daysWorked)
                .totalDaysInPeriod(totalDays)
                .joinDateUsed(joinDateUsed)
                .relievingDateUsed(relievingDateUsed)
                .proratedAmount(proratedAmount)
                .fullAmount(fullAmount)
                .prorationRule(rule != null ? rule.name() : null)
                .build();
    }

    /** SC-49: Component dependency report – components that reference other components (base or formula). */
    public List<ComponentDependencyDto> getComponentDependencies(UUID organizationId) {
        if (organizationId == null) return List.of();
        List<SalaryComponent> all = salaryComponentRepository.findByOrganizationIdOrderByDisplayOrderAsc(organizationId);
        Map<String, String> codeToName = all.stream()
                .filter(c -> c.getCode() != null)
                .collect(Collectors.toMap(SalaryComponent::getCode, c -> c.getComponentName() != null ? c.getComponentName() : c.getCode(), (a, b) -> a));
        List<ComponentDependencyDto> result = new ArrayList<>();
        for (SalaryComponent c : all) {
            if (c.getBaseComponentCode() != null && !c.getBaseComponentCode().isBlank()) {
                result.add(ComponentDependencyDto.builder()
                        .componentCode(c.getCode())
                        .componentName(c.getComponentName())
                        .dependencyType("BASE_COMPONENT")
                        .referencedCode(c.getBaseComponentCode().trim())
                        .referencedName(codeToName.get(c.getBaseComponentCode().trim()))
                        .build());
            }
            if (c.getFormulaExpression() != null && !c.getFormulaExpression().isBlank()) {
                Set<String> refs = extractComponentCodesFromFormula(c.getFormulaExpression());
                for (String ref : refs) {
                    result.add(ComponentDependencyDto.builder()
                            .componentCode(c.getCode())
                            .componentName(c.getComponentName())
                            .dependencyType("FORMULA_REF")
                            .referencedCode(ref)
                            .referencedName(codeToName.get(ref))
                            .build());
                }
            }
        }
        return result;
    }

    // ---------- Employee Salary Detail (ES-07–ES-15) ----------

    public List<EmployeeSalaryDetail> getAllEmployeeSalaryDetails(UUID organizationId) {
        return employeeSalaryDetailRepository.findByOrganizationId(organizationId);
    }

    /** All details for employee (all history). */
    public List<EmployeeSalaryDetail> getEmployeeSalaryDetails(UUID employeeId, UUID organizationId) {
        return employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }

    /** ES-46: Details effective on asOfDate (one active per employee per component per date). */
    public List<EmployeeSalaryDetail> getEmployeeSalaryDetails(UUID employeeId, UUID organizationId, LocalDate asOfDate) {
        if (asOfDate == null) {
            return getEmployeeSalaryDetails(employeeId, organizationId);
        }
        return employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(
                employeeId, organizationId, asOfDate);
    }

    /** ES-07–ES-15: Create employee salary component. Validates valueType/amount/percentage, no overlap, component exists and active, SC-24 statutory no manual amount, ES-15 Basic required. */
    public EmployeeSalaryDetail createEmployeeSalaryDetail(EmployeeSalaryDetail detail) {
        if (detail.getEmployeeId() == null || detail.getOrganizationId() == null || detail.getComponentId() == null) {
            throw new IllegalArgumentException("employeeId, organizationId, and componentId are required");
        }
        if (detail.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        validateEffectiveDates(detail.getEffectiveFrom(), detail.getEffectiveTo());
        // ES-31: Salary assignment only for employees active on effective date (join date <= effective, no relieving or relieving > effective)
        ensureEmployeeActiveOnDate(detail.getEmployeeId(), detail.getEffectiveFrom());

        ComponentValueType valueType = detail.getValueType() != null ? detail.getValueType() : ComponentValueType.AMOUNT;
        detail.setValueType(valueType);

        // ES-34: Component must exist, be active, and be effective on the assignment's effective date (same organization)
        SalaryComponent component = salaryComponentRepository.findById(detail.getComponentId())
                .orElseThrow(() -> new IllegalArgumentException("Salary component not found. ES-34: component must exist in same organization."));
        if (!component.getOrganizationId().equals(detail.getOrganizationId())) {
            throw new IllegalArgumentException("Component must belong to the same organization as the employee salary. ES-34.");
        }
        if (!Boolean.TRUE.equals(component.getIsActive())) {
            throw new IllegalArgumentException("Component is not active; cannot assign to employee. ES-34.");
        }
        LocalDate effectiveDate = detail.getEffectiveFrom();
        if (component.getEffectiveFrom() != null && component.getEffectiveFrom().isAfter(effectiveDate)) {
            throw new IllegalArgumentException("Component is not yet effective on effectiveFrom date. ES-34.");
        }
        if (component.getEffectiveTo() != null && component.getEffectiveTo().isBefore(effectiveDate)) {
            throw new IllegalArgumentException("Component is no longer effective on effectiveFrom date. ES-34.");
        }

        // SC-24: Statutory component shall not allow manual override of amount
        if (component.getCalculationBasis() == CalculationBasis.STATUTORY) {
            if (detail.getAmount() != null && detail.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Statutory component amount cannot be set manually; use valueType USE_MASTER_DEFAULT. SC-24.");
            }
            detail.setAmount(null);
            detail.setPercentage(null);
            detail.setValueType(ComponentValueType.USE_MASTER_DEFAULT);
        } else {
            switch (valueType) {
                case AMOUNT -> {
                    if (detail.getAmount() == null) {
                        throw new IllegalArgumentException("Amount is required when valueType is AMOUNT");
                    }
                    if (detail.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Amount must be non-negative");
                    }
                    detail.setPercentage(null);
                    validateComponentCeilingFloor(component, detail.getAmount(), null);
                }
                case PERCENTAGE -> {
                    if (detail.getPercentage() == null) {
                        throw new IllegalArgumentException("Percentage is required when valueType is PERCENTAGE");
                    }
                    if (detail.getPercentage().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Percentage must be non-negative");
                    }
                    detail.setAmount(null);
                    validateComponentCeilingFloor(component, null, detail.getPercentage());
                }
                case USE_MASTER_DEFAULT -> {
                    detail.setAmount(null);
                    detail.setPercentage(null);
                }
            }
        }

        ensureNotInClosedPayrollPeriod(detail.getOrganizationId(), detail.getEffectiveFrom(), detail.getEffectiveTo());
        // ES-11, ES-33, NF-05: Duplicate effective periods rejected – one active per employee+component per date.
        boolean hasOverlap = !employeeSalaryDetailRepository.findOverlappingEmployeeComponent(
                detail.getEmployeeId(),
                detail.getComponentId(),
                detail.getEffectiveFrom(),
                detail.getEffectiveTo()
        ).isEmpty();
        if (hasOverlap) {
            throw new ResourceConflictException(
                    "Another record for this employee and component already exists overlapping the effective period. ES-11/ES-33: one active per employee per component per date.");
        }

        if (detail.getIsActive() == null) {
            detail.setIsActive(true);
        }

        log.info("Creating employee salary detail for employee: {} component: {} valueType: {}", detail.getEmployeeId(), detail.getComponentId(), detail.getValueType());
        EmployeeSalaryDetail created = employeeSalaryDetailRepository.save(detail);

        // ES-15: After add, ensure at least one Basic earning exists for this employee on this period (Basic required)
        ensureBasicEarningPresentForEmployeePeriod(created.getEmployeeId(), created.getOrganizationId(),
                created.getEffectiveFrom(), created.getEffectiveTo(), null);

        auditLog(created.getOrganizationId(), SalaryAuditLog.ENTITY_EMPLOYEE_SALARY_DETAIL, created.getSalaryDetailId(),
                SalaryAuditLog.ACTION_CREATE, null, employeeDetailSummary(created), created.getCreatedBy());
        return created;
    }

    /** ES-48: Update employee salary component (valueType, amount, percentage, effectiveTo). ES-11: no overlap. */
    public EmployeeSalaryDetail updateEmployeeSalaryDetail(UUID detailId, EmployeeSalaryDetail updates) {
        EmployeeSalaryDetail existing = employeeSalaryDetailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Employee salary detail not found"));
        String oldSummary = employeeDetailSummary(existing);
        SalaryComponent component = salaryComponentRepository.findById(existing.getComponentId())
                .orElseThrow(() -> new IllegalArgumentException("Salary component not found"));

        if (updates.getValueType() != null) {
            existing.setValueType(updates.getValueType());
            switch (updates.getValueType()) {
                case AMOUNT -> {
                    existing.setAmount(updates.getAmount() != null ? updates.getAmount() : BigDecimal.ZERO);
                    existing.setPercentage(null);
                    if (component.getCalculationBasis() != CalculationBasis.STATUTORY) {
                        validateComponentCeilingFloor(component, existing.getAmount(), null);
                    }
                }
                case PERCENTAGE -> {
                    existing.setPercentage(updates.getPercentage());
                    existing.setAmount(null);
                    validateComponentCeilingFloor(component, null, existing.getPercentage());
                }
                case USE_MASTER_DEFAULT -> {
                    existing.setAmount(null);
                    existing.setPercentage(null);
                }
            }
        }
        if (updates.getAmount() != null && ComponentValueType.AMOUNT.equals(existing.getValueType())) {
            if (component.getCalculationBasis() == CalculationBasis.STATUTORY) {
                existing.setAmount(null);
                existing.setValueType(ComponentValueType.USE_MASTER_DEFAULT);
            } else {
                existing.setAmount(updates.getAmount());
                validateComponentCeilingFloor(component, existing.getAmount(), null);
            }
        }
        if (updates.getPercentage() != null && ComponentValueType.PERCENTAGE.equals(existing.getValueType())) {
            existing.setPercentage(updates.getPercentage());
            validateComponentCeilingFloor(component, null, existing.getPercentage());
        }
        if (updates.getEffectiveTo() != null) {
            validateEffectiveDates(existing.getEffectiveFrom(), updates.getEffectiveTo());
            existing.setEffectiveTo(updates.getEffectiveTo());
        }

        EmployeeSalaryDetail updated = employeeSalaryDetailRepository.save(existing);
        String performedBy = updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy();
        if (performedBy == null) performedBy = "system";
        auditLog(updated.getOrganizationId(), SalaryAuditLog.ENTITY_EMPLOYEE_SALARY_DETAIL, updated.getSalaryDetailId(),
                SalaryAuditLog.ACTION_UPDATE, oldSummary, employeeDetailSummary(updated), performedBy);
        return updated;
    }

    /** ES-12: Remove component with effective dating – set effectiveTo (and optionally isActive=false). */
    public EmployeeSalaryDetail endEmployeeSalaryDetail(UUID detailId, LocalDate effectiveTo) {
        EmployeeSalaryDetail existing = employeeSalaryDetailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Employee salary detail not found"));
        String oldSummary = employeeDetailSummary(existing);
        if (effectiveTo == null) {
            throw new IllegalArgumentException("effectiveTo is required to end a component assignment");
        }
        validateEffectiveDates(existing.getEffectiveFrom(), effectiveTo);
        ensureNotInClosedPayrollPeriod(existing.getOrganizationId(), existing.getEffectiveFrom(), effectiveTo);
        existing.setEffectiveTo(effectiveTo);
        existing.setIsActive(false);
        EmployeeSalaryDetail ended = employeeSalaryDetailRepository.save(existing);
        String performedBy = ended.getUpdatedBy() != null ? ended.getUpdatedBy() : ended.getCreatedBy();
        if (performedBy == null) performedBy = "system";
        auditLog(ended.getOrganizationId(), SalaryAuditLog.ENTITY_EMPLOYEE_SALARY_DETAIL, ended.getSalaryDetailId(),
                SalaryAuditLog.ACTION_DEACTIVATE, oldSummary, employeeDetailSummary(ended), performedBy);
        return ended;
    }

    public Optional<EmployeeSalaryDetail> getEmployeeSalaryDetailById(UUID detailId) {
        return employeeSalaryDetailRepository.findById(detailId);
    }

    /** ES-31: Employee must be active on the given date (hireDate <= date and no termination or terminationDate > date). Rejects assignment for inactive. */
    private void ensureEmployeeActiveOnDate(UUID employeeId, LocalDate effectiveDate) {
        com.easyops.hr.entity.Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (emp.getHireDate() != null && emp.getHireDate().isAfter(effectiveDate)) {
            throw new IllegalArgumentException("Employee is not yet active on " + effectiveDate + " (hire date: " + emp.getHireDate() + "). ES-31: salary assignment only for active employment.");
        }
        if (emp.getTerminationDate() != null && !emp.getTerminationDate().isAfter(effectiveDate)) {
            throw new IllegalArgumentException("Employee is no longer active on " + effectiveDate + " (termination date: " + emp.getTerminationDate() + "). ES-31: salary assignment only for active employment.");
        }
    }

    /** ES-32: Validate component value (amount or percentage) against master ceiling/floor when defined. */
    private void validateComponentCeilingFloor(SalaryComponent component, BigDecimal amount, BigDecimal percentage) {
        if (amount != null && component.getCeilingAmount() != null && amount.compareTo(component.getCeilingAmount()) > 0) {
            throw new IllegalArgumentException("Amount exceeds component ceiling " + component.getCeilingAmount() + ". ES-32.");
        }
        if (amount != null && component.getFloorAmount() != null && amount.compareTo(component.getFloorAmount()) < 0) {
            throw new IllegalArgumentException("Amount is below component floor " + component.getFloorAmount() + ". ES-32.");
        }
        // ES-32: Percentage validated against non-negative above; percentage-specific ceiling/floor can be added to component master when defined
        if (percentage != null && percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage must not exceed 100. ES-32.");
        }
    }

    /** ES-15: Ensure at least one Basic earning exists for employee in the given period; throw if not. */
    private void ensureBasicEarningPresentForEmployeePeriod(UUID employeeId, UUID organizationId,
                                                           LocalDate effectiveFrom, LocalDate effectiveTo, UUID excludeDetailId) {
        List<EmployeeSalaryDetail> onPeriod = employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdOverlappingPeriod(
                employeeId, organizationId, effectiveFrom, effectiveTo);
        if (onPeriod.isEmpty()) return;
        for (EmployeeSalaryDetail d : onPeriod) {
            if (excludeDetailId != null && d.getSalaryDetailId().equals(excludeDetailId)) continue;
            SalaryComponent c = salaryComponentRepository.findById(d.getComponentId()).orElse(null);
            if (c == null || !"EARNING".equalsIgnoreCase(c.getComponentType()) || c.getCategory() != SalaryComponentCategory.BASIC) {
                continue;
            }
            if (ComponentValueType.AMOUNT.equals(d.getValueType()) && d.getAmount() != null && d.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                return;
            }
            if (ComponentValueType.USE_MASTER_DEFAULT.equals(d.getValueType())) {
                return;
            }
        }
        throw new IllegalArgumentException("ES-15: At least one Basic earning component with amount or Use Master Default is required for this employee in the effective period.");
    }

    private static String employeeDetailSummary(EmployeeSalaryDetail d) {
        return "employeeId=" + d.getEmployeeId() + ",componentId=" + d.getComponentId()
                + ",valueType=" + d.getValueType() + ",amount=" + d.getAmount() + ",percentage=" + d.getPercentage()
                + ",effectiveFrom=" + d.getEffectiveFrom() + ",effectiveTo=" + d.getEffectiveTo();
    }

    // ---------- Employee Salary Assignment (ES-01–ES-06) ----------

    /** ES-01: Get the active assignment for an employee on a given date (one active per employee per date). */
    public Optional<EmployeeSalaryAssignment> getAssignment(UUID employeeId, UUID organizationId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<EmployeeSalaryAssignment> list = employeeSalaryAssignmentRepository.findActiveByEmployeeIdAndDate(employeeId, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** All assignment history for an employee (newest first). */
    public List<EmployeeSalaryAssignment> getAssignmentHistory(UUID employeeId) {
        return employeeSalaryAssignmentRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId);
    }

    /** ES-18: Component detail history for an employee (newest first). */
    public List<EmployeeSalaryDetail> getDetailHistory(UUID employeeId, UUID organizationId) {
        return employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdOrderByEffectiveFromDesc(employeeId, organizationId);
    }

    /** ES-18: Unified revision history (assignments + component details) for an employee, sorted by effectiveFrom descending. */
    public List<SalaryRevisionHistoryItemDto> getRevisionHistory(UUID employeeId, UUID organizationId) {
        List<SalaryRevisionHistoryItemDto> list = new ArrayList<>();
        for (EmployeeSalaryAssignment a : employeeSalaryAssignmentRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)) {
            if (!a.getOrganizationId().equals(organizationId)) continue;
            list.add(SalaryRevisionHistoryItemDto.builder()
                    .kind(SalaryRevisionHistoryItemDto.KIND_ASSIGNMENT)
                    .id(a.getAssignmentId())
                    .employeeId(a.getEmployeeId())
                    .effectiveFrom(a.getEffectiveFrom())
                    .effectiveTo(a.getEffectiveTo())
                    .revisionType(a.getRevisionType())
                    .revisionReason(a.getRevisionReason())
                    .summary("Structure " + a.getSalaryStructureId() + " Grade " + a.getSalaryGradeId() + (a.getSalaryBandId() != null ? " Band " + a.getSalaryBandId() : ""))
                    .createdBy(a.getCreatedBy())
                    .build());
        }
        for (EmployeeSalaryDetail d : employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdOrderByEffectiveFromDesc(employeeId, organizationId)) {
            list.add(SalaryRevisionHistoryItemDto.builder()
                    .kind(SalaryRevisionHistoryItemDto.KIND_COMPONENT)
                    .id(d.getSalaryDetailId())
                    .employeeId(d.getEmployeeId())
                    .effectiveFrom(d.getEffectiveFrom())
                    .effectiveTo(d.getEffectiveTo())
                    .revisionType(d.getRevisionType())
                    .revisionReason(d.getRevisionReason())
                    .summary("Component " + d.getComponentId() + " " + d.getValueType() + " " + (d.getAmount() != null ? d.getAmount() : d.getPercentage()))
                    .createdBy(d.getCreatedBy())
                    .build());
        }
        list.sort((a, b) -> {
            int c = (b.getEffectiveFrom() != null ? b.getEffectiveFrom() : LocalDate.MIN)
                    .compareTo(a.getEffectiveFrom() != null ? a.getEffectiveFrom() : LocalDate.MIN);
            if (c != 0) return c;
            return a.getKind().compareTo(b.getKind());
        });
        return list;
    }

    /** ES-01–ES-06: Create assignment; validates same org, structure/grade/band hierarchy, no overlapping period. */
    public EmployeeSalaryAssignment createAssignment(EmployeeSalaryAssignment assignment) {
        if (assignment.getEmployeeId() == null) {
            throw new IllegalArgumentException("employeeId is required");
        }
        if (assignment.getSalaryStructureId() == null) {
            throw new IllegalArgumentException("salaryStructureId is required");
        }
        if (assignment.getSalaryGradeId() == null) {
            throw new IllegalArgumentException("salaryGradeId is required");
        }
        if (assignment.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (assignment.getSource() == null) {
            assignment.setSource(AssignmentSource.OVERRIDE);
        }
        Employee employee = employeeRepository.findById(assignment.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        assignment.setOrganizationId(employee.getOrganizationId());
        validateAssignmentStructureGradeBand(employee.getOrganizationId(), assignment.getSalaryStructureId(),
                assignment.getSalaryGradeId(), assignment.getSalaryBandId());
        validateEffectiveDates(assignment.getEffectiveFrom(), assignment.getEffectiveTo());
        ensureNotInClosedPayrollPeriod(assignment.getOrganizationId(), assignment.getEffectiveFrom(), assignment.getEffectiveTo());
        if (employeeSalaryAssignmentRepository.countOverlappingPeriod(
                assignment.getEmployeeId(), assignment.getEffectiveFrom(), assignment.getEffectiveTo(), null) > 0) {
            throw new ResourceConflictException(
                    "Another salary assignment already exists for this employee overlapping the period " +
                            assignment.getEffectiveFrom() + " to " + assignment.getEffectiveTo() + ". ES-01: one active per employee per date.");
        }
        log.info("Creating salary assignment for employee: {} structure: {} grade: {} from {}", assignment.getEmployeeId(),
                assignment.getSalaryStructureId(), assignment.getSalaryGradeId(), assignment.getEffectiveFrom());
        EmployeeSalaryAssignment created = employeeSalaryAssignmentRepository.save(assignment);
        String performedBy = created.getCreatedBy() != null ? created.getCreatedBy() : "system";
        auditLog(created.getOrganizationId(), SalaryAuditLog.ENTITY_EMPLOYEE_SALARY_ASSIGNMENT, created.getAssignmentId(),
                SalaryAuditLog.ACTION_CREATE, null, assignmentSummary(created), performedBy);
        return created;
    }

    /** ES-01–ES-06: Update assignment (e.g. set effectiveTo to end it, or change grade/band). Validates same rules. */
    public EmployeeSalaryAssignment updateAssignment(UUID assignmentId, EmployeeSalaryAssignment updates) {
        EmployeeSalaryAssignment existing = employeeSalaryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Salary assignment not found"));
        UUID orgId = existing.getOrganizationId();
        String oldSummary = assignmentSummary(existing);
        if (updates.getSalaryStructureId() != null) {
            validateAssignmentStructureGradeBand(existing.getOrganizationId(), updates.getSalaryStructureId(),
                    updates.getSalaryGradeId() != null ? updates.getSalaryGradeId() : existing.getSalaryGradeId(),
                    updates.getSalaryBandId() != null ? updates.getSalaryBandId() : existing.getSalaryBandId());
            existing.setSalaryStructureId(updates.getSalaryStructureId());
        }
        if (updates.getSalaryGradeId() != null) {
            UUID newGradeId = updates.getSalaryGradeId();
            UUID bandIdToValidate = updates.getSalaryBandId() != null ? updates.getSalaryBandId() : existing.getSalaryBandId();
            validateAssignmentStructureGradeBand(existing.getOrganizationId(), existing.getSalaryStructureId(), newGradeId, bandIdToValidate);
            existing.setSalaryGradeId(newGradeId);
            if (existing.getSalaryBandId() != null) {
                SalaryBand currentBand = salaryBandRepository.findById(existing.getSalaryBandId()).orElse(null);
                if (currentBand == null || !currentBand.getSalaryGradeId().equals(newGradeId)) {
                    existing.setSalaryBandId(null);
                }
            }
        }
        if (updates.getSalaryBandId() != null) {
            SalaryBand band = salaryBandRepository.findById(updates.getSalaryBandId())
                    .orElseThrow(() -> new IllegalArgumentException("Salary band not found"));
            if (!band.getSalaryGradeId().equals(existing.getSalaryGradeId())) {
                throw new IllegalArgumentException("Salary band must belong to the assignment's salary grade. ES-01.");
            }
            existing.setSalaryBandId(updates.getSalaryBandId());
        }
        if (updates.getEffectiveFrom() != null) {
            existing.setEffectiveFrom(updates.getEffectiveFrom());
            ensureEmployeeActiveOnDate(existing.getEmployeeId(), updates.getEffectiveFrom());
        }
        if (updates.getEffectiveTo() != null) existing.setEffectiveTo(updates.getEffectiveTo());
        if (updates.getSource() != null) existing.setSource(updates.getSource());
        if (updates.getRevisionReason() != null) existing.setRevisionReason(updates.getRevisionReason());
        if (updates.getRevisionType() != null) existing.setRevisionType(updates.getRevisionType());
        validateEffectiveDates(existing.getEffectiveFrom(), existing.getEffectiveTo());
        ensureNotInClosedPayrollPeriod(existing.getOrganizationId(), existing.getEffectiveFrom(), existing.getEffectiveTo());
        if (employeeSalaryAssignmentRepository.countOverlappingPeriod(
                existing.getEmployeeId(), existing.getEffectiveFrom(), existing.getEffectiveTo(), existing.getAssignmentId()) > 0) {
            throw new ResourceConflictException("Updated period would overlap another assignment for this employee. ES-01.");
        }
        EmployeeSalaryAssignment updated = employeeSalaryAssignmentRepository.save(existing);
        String performedBy = updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy();
        if (performedBy == null) performedBy = "system";
        auditLog(orgId, SalaryAuditLog.ENTITY_EMPLOYEE_SALARY_ASSIGNMENT, updated.getAssignmentId(),
                SalaryAuditLog.ACTION_UPDATE, oldSummary, assignmentSummary(updated), performedBy);
        return updated;
    }

    /** ES-06: Get default structure/grade/band from position for suggesting or auto-filling assignment. */
    public Optional<PositionSalaryDefaultsDto> getPositionSalaryDefaults(UUID positionId) {
        return positionRepository.findById(positionId)
                .filter(p -> p.getDefaultSalaryStructureId() != null && p.getDefaultSalaryGradeId() != null)
                .map(p -> PositionSalaryDefaultsDto.builder()
                        .positionId(p.getPositionId())
                        .organizationId(p.getOrganizationId())
                        .defaultSalaryStructureId(p.getDefaultSalaryStructureId())
                        .defaultSalaryGradeId(p.getDefaultSalaryGradeId())
                        .defaultSalaryBandId(p.getDefaultSalaryBandId())
                        .build());
    }

    /** List assignments by org with optional structure/grade and as-of date filter. */
    public List<EmployeeSalaryAssignment> listAssignments(UUID organizationId, UUID structureId, UUID gradeId, LocalDate asOfDate) {
        return employeeSalaryAssignmentRepository.findByOrganizationIdAndFilters(organizationId, structureId, gradeId, asOfDate);
    }

    public Optional<EmployeeSalaryAssignment> getAssignmentById(UUID assignmentId) {
        return employeeSalaryAssignmentRepository.findById(assignmentId);
    }

    /** Validate structure/grade/band belong to org and grade in structure, band in grade (ES-01). */
    private void validateAssignmentStructureGradeBand(UUID organizationId, UUID structureId, UUID gradeId, UUID bandId) {
        SalaryStructure structure = salaryStructureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Salary structure not found"));
        if (!structure.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Salary structure must belong to the same organization as the employee. ES-01.");
        }
        SalaryGrade grade = salaryGradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Salary grade not found"));
        if (!grade.getSalaryStructureId().equals(structureId)) {
            throw new IllegalArgumentException("Salary grade must belong to the selected salary structure. ES-01.");
        }
        if (bandId != null) {
            SalaryBand band = salaryBandRepository.findById(bandId)
                    .orElseThrow(() -> new IllegalArgumentException("Salary band not found"));
            if (!band.getSalaryGradeId().equals(gradeId)) {
                throw new IllegalArgumentException("Salary band must belong to the selected salary grade. ES-01.");
            }
        }
    }

    private static String assignmentSummary(EmployeeSalaryAssignment a) {
        return "employeeId=" + a.getEmployeeId()
                + ",structureId=" + a.getSalaryStructureId()
                + ",gradeId=" + a.getSalaryGradeId()
                + ",bandId=" + a.getSalaryBandId()
                + ",effectiveFrom=" + a.getEffectiveFrom()
                + ",effectiveTo=" + a.getEffectiveTo()
                + ",source=" + a.getSource()
                + ",revisionType=" + a.getRevisionType()
                + ",revisionReason=" + a.getRevisionReason();
    }

    // ---------- Bulk Revision (ES-21) ----------

    public SalaryBulkRevision createBulkRevision(SalaryBulkRevision revision) {
        if (revision.getOrganizationId() == null) throw new IllegalArgumentException("organizationId is required");
        if (revision.getTargetType() == null || (!"BY_GRADE".equals(revision.getTargetType()) && !"BY_STRUCTURE".equals(revision.getTargetType()))) {
            throw new IllegalArgumentException("targetType must be BY_GRADE or BY_STRUCTURE");
        }
        if ("BY_GRADE".equals(revision.getTargetType()) && revision.getTargetGradeId() == null) {
            throw new IllegalArgumentException("targetGradeId is required when targetType is BY_GRADE");
        }
        if ("BY_STRUCTURE".equals(revision.getTargetType()) && revision.getTargetStructureId() == null) {
            throw new IllegalArgumentException("targetStructureId is required when targetType is BY_STRUCTURE");
        }
        if (revision.getComponentCode() == null || revision.getComponentCode().isBlank()) {
            throw new IllegalArgumentException("componentCode is required");
        }
        if (revision.getPercentageValue() == null || revision.getPercentageValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("percentageValue is required and must be non-negative");
        }
        if (revision.getEffectiveFrom() == null) throw new IllegalArgumentException("effectiveFrom is required");
        ensureNotInClosedPayrollPeriod(revision.getOrganizationId(), revision.getEffectiveFrom(), null);
        revision.setStatus("PENDING");
        revision.setRequestedAt(LocalDateTime.now());
        return salaryBulkRevisionRepository.save(revision);
    }

    public SalaryBulkRevision approveBulkRevision(UUID bulkRevisionId, String approvedBy) {
        SalaryBulkRevision revision = salaryBulkRevisionRepository.findById(bulkRevisionId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk revision not found"));
        if (!"PENDING".equals(revision.getStatus())) {
            throw new IllegalArgumentException("Only PENDING bulk revisions can be approved. Current status: " + revision.getStatus());
        }
        revision.setStatus("APPROVED");
        revision.setApprovedBy(approvedBy);
        revision.setApprovedAt(LocalDateTime.now());
        revision.setRejectionReason(null);
        salaryBulkRevisionRepository.save(revision);
        int applied = applyBulkRevision(revision);
        revision.setRowsApplied(applied);
        revision.setStatus("APPLIED");
        return salaryBulkRevisionRepository.save(revision);
    }

    public SalaryBulkRevision rejectBulkRevision(UUID bulkRevisionId, String rejectionReason) {
        SalaryBulkRevision revision = salaryBulkRevisionRepository.findById(bulkRevisionId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk revision not found"));
        if (!"PENDING".equals(revision.getStatus())) {
            throw new IllegalArgumentException("Only PENDING bulk revisions can be rejected");
        }
        revision.setStatus("REJECTED");
        revision.setRejectionReason(rejectionReason);
        return salaryBulkRevisionRepository.save(revision);
    }

    public List<SalaryBulkRevision> listBulkRevisions(UUID organizationId, String status) {
        if (status != null && !status.isBlank()) {
            return salaryBulkRevisionRepository.findByOrganizationIdAndStatusOrderByRequestedAtDesc(organizationId, status);
        }
        return salaryBulkRevisionRepository.findByOrganizationIdOrderByRequestedAtDesc(organizationId);
    }

    public Optional<SalaryBulkRevision> getBulkRevisionById(UUID bulkRevisionId) {
        return salaryBulkRevisionRepository.findById(bulkRevisionId);
    }

    /** Apply approved bulk revision: create new effective-dated detail rows (X% increase to component) for employees in target grade/structure. ES-16: revision by new rows only. */
    private int applyBulkRevision(SalaryBulkRevision revision) {
        UUID orgId = revision.getOrganizationId();
        LocalDate effectiveFrom = revision.getEffectiveFrom();
        ensureNotInClosedPayrollPeriod(orgId, effectiveFrom, null);
        SalaryComponent component = salaryComponentRepository.findByOrganizationIdAndCode(orgId, revision.getComponentCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Component not found with code: " + revision.getComponentCode()));
        BigDecimal pct = revision.getPercentageValue();
        BigDecimal factor = BigDecimal.ONE.add(pct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        List<EmployeeSalaryAssignment> assignments;
        if ("BY_GRADE".equals(revision.getTargetType())) {
            assignments = employeeSalaryAssignmentRepository.findByOrganizationIdAndFilters(orgId, null, revision.getTargetGradeId(), effectiveFrom);
        } else {
            assignments = employeeSalaryAssignmentRepository.findByOrganizationIdAndFilters(orgId, revision.getTargetStructureId(), null, effectiveFrom);
        }
        Set<UUID> employeeIds = new HashSet<>();
        for (EmployeeSalaryAssignment a : assignments) {
            employeeIds.add(a.getEmployeeId());
        }
        String revisionReason = revision.getComment() != null ? revision.getComment() : "Bulk " + pct + "% revision (bulk_revision_id=" + revision.getBulkRevisionId() + ")";
        int count = 0;
        for (UUID employeeId : employeeIds) {
            List<EmployeeSalaryDetail> currentDetails = employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(employeeId, orgId, effectiveFrom);
            EmployeeSalaryDetail currentDetail = null;
            for (EmployeeSalaryDetail d : currentDetails) {
                if (d.getComponentId().equals(component.getComponentId())) {
                    currentDetail = d;
                    break;
                }
            }
            if (currentDetail == null) continue;
            if (!ComponentValueType.AMOUNT.equals(currentDetail.getValueType()) || currentDetail.getAmount() == null || currentDetail.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal newAmount = currentDetail.getAmount().multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
            LocalDate dayBefore = effectiveFrom.minusDays(1);
            if (currentDetail.getEffectiveFrom().isBefore(effectiveFrom)) {
                currentDetail.setEffectiveTo(dayBefore);
                employeeSalaryDetailRepository.save(currentDetail);
            }
            EmployeeSalaryDetail newDetail = new EmployeeSalaryDetail();
            newDetail.setEmployeeId(employeeId);
            newDetail.setOrganizationId(orgId);
            newDetail.setSalaryStructureId(currentDetail.getSalaryStructureId());
            newDetail.setComponentId(component.getComponentId());
            newDetail.setValueType(ComponentValueType.AMOUNT);
            newDetail.setAmount(newAmount);
            newDetail.setEffectiveFrom(effectiveFrom);
            newDetail.setEffectiveTo(null);
            newDetail.setIsActive(true);
            newDetail.setRevisionType("BULK_PERCENTAGE");
            newDetail.setRevisionReason(revisionReason);
            employeeSalaryDetailRepository.save(newDetail);
            count++;
        }
        log.info("Applied bulk revision {}: {} rows created for {} employees", revision.getBulkRevisionId(), count, employeeIds.size());
        return count;
    }

    // ---------- Salary Grades (SS-08–SS-12) ----------
    public List<SalaryGrade> getGradesByStructureId(UUID structureId) {
        return salaryGradeRepository.findBySalaryStructureIdOrderByDisplayOrderAsc(structureId);
    }

    public Optional<SalaryGrade> getGradeById(UUID gradeId) {
        return salaryGradeRepository.findById(gradeId);
    }

    public SalaryGrade createGrade(SalaryGrade grade) {
        UUID structureId = grade.getSalaryStructureId();
        if (structureId == null) {
            throw new IllegalArgumentException("salaryStructureId is required");
        }
        salaryStructureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Salary structure not found"));
        String code = grade.getCode();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Grade code is required");
        }
        code = code.trim();
        if (salaryGradeRepository.existsBySalaryStructureIdAndCode(structureId, code)) {
            throw new ResourceConflictException(
                    "Grade with code '" + code + "' already exists in this structure");
        }
        grade.setCode(code);
        if (grade.getDisplayOrder() == null) {
            grade.setDisplayOrder(0);
        }
        validateEffectiveDates(grade.getEffectiveFrom(), grade.getEffectiveTo());
        log.info("Creating salary grade: {} (code: {}) for structure {}", grade.getName(), code, structureId);
        SalaryGrade created = salaryGradeRepository.save(grade);
        SalaryStructure struct = salaryStructureRepository.findById(structureId).orElse(null);
        if (struct != null) {
            auditLog(struct.getOrganizationId(), SalaryAuditLog.ENTITY_GRADE, created.getSalaryGradeId(), SalaryAuditLog.ACTION_CREATE,
                null, gradeSummary(created), created.getCreatedBy());
        }
        return created;
    }

    public SalaryGrade updateGrade(UUID gradeId, SalaryGrade grade) {
        SalaryGrade existing = salaryGradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Salary grade not found"));
        // Code is immutable
        if (grade.getName() != null) existing.setName(grade.getName());
        if (grade.getDisplayOrder() != null) existing.setDisplayOrder(grade.getDisplayOrder());
        if (grade.getDescription() != null) existing.setDescription(grade.getDescription());
        if (grade.getEffectiveFrom() != null || grade.getEffectiveTo() != null) {
            LocalDate from = grade.getEffectiveFrom() != null ? grade.getEffectiveFrom() : existing.getEffectiveFrom();
            LocalDate to = grade.getEffectiveTo() != null ? grade.getEffectiveTo() : existing.getEffectiveTo();
            validateEffectiveDates(from, to);
            if (grade.getEffectiveFrom() != null) existing.setEffectiveFrom(grade.getEffectiveFrom());
            if (grade.getEffectiveTo() != null) existing.setEffectiveTo(grade.getEffectiveTo());
        }
        String oldSummary = gradeSummary(existing);
        SalaryGrade updated = salaryGradeRepository.save(existing);
        SalaryStructure struct = salaryStructureRepository.findById(updated.getSalaryStructureId()).orElse(null);
        if (struct != null) {
            auditLog(struct.getOrganizationId(), SalaryAuditLog.ENTITY_GRADE, updated.getSalaryGradeId(), SalaryAuditLog.ACTION_UPDATE,
                oldSummary, gradeSummary(updated), updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy());
        }
        return updated;
    }

    private static String gradeSummary(SalaryGrade g) {
        return "code=" + g.getCode() + ",name=" + g.getName() + ",displayOrder=" + g.getDisplayOrder()
            + ",effectiveFrom=" + g.getEffectiveFrom() + ",effectiveTo=" + g.getEffectiveTo();
    }

    // ---------- Salary Bands (SS-13–SS-19) ----------
    public List<SalaryBand> getBandsByGradeId(UUID gradeId) {
        return salaryBandRepository.findBySalaryGradeIdOrderByDisplayOrderAsc(gradeId);
    }

    public Optional<SalaryBand> getBandById(UUID bandId) {
        return salaryBandRepository.findById(bandId);
    }

    public SalaryBand createBand(SalaryBand band) {
        UUID gradeId = band.getSalaryGradeId();
        if (gradeId == null) {
            throw new IllegalArgumentException("salaryGradeId is required");
        }
        SalaryGrade grade = salaryGradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Salary grade not found"));
        validateBandAmounts(band.getMinimumAmount(), band.getMaximumAmount(), band.getMidPoint());
        if (band.getDisplayOrder() == null) {
            band.setDisplayOrder(0);
        }
        if (band.getCurrency() == null || band.getCurrency().isBlank()) {
            SalaryStructure structure = salaryStructureRepository.findById(grade.getSalaryStructureId()).orElse(null);
            band.setCurrency(structure != null && structure.getCurrency() != null ? structure.getCurrency() : "BDT");
        }
        validateEffectiveDates(band.getEffectiveFrom(), band.getEffectiveTo());
        ensureNoOverlappingBandsInGrade(gradeId, band.getMinimumAmount(), band.getMaximumAmount(), null);
        log.info("Creating salary band for grade {}: min={}, max={}", gradeId, band.getMinimumAmount(), band.getMaximumAmount());
        SalaryBand created = salaryBandRepository.save(band);
        SalaryStructure struct = salaryStructureRepository.findById(grade.getSalaryStructureId()).orElse(null);
        if (struct != null) {
            auditLog(struct.getOrganizationId(), SalaryAuditLog.ENTITY_BAND, created.getSalaryBandId(), SalaryAuditLog.ACTION_CREATE,
                null, bandSummary(created), created.getCreatedBy());
        }
        return created;
    }

    public SalaryBand updateBand(UUID bandId, SalaryBand band) {
        SalaryBand existing = salaryBandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("Salary band not found"));
        if (band.getMinimumAmount() != null) existing.setMinimumAmount(band.getMinimumAmount());
        if (band.getMaximumAmount() != null) existing.setMaximumAmount(band.getMaximumAmount());
        if (band.getMidPoint() != null) existing.setMidPoint(band.getMidPoint());
        validateBandAmounts(existing.getMinimumAmount(), existing.getMaximumAmount(), existing.getMidPoint());
        if (band.getCurrency() != null) existing.setCurrency(band.getCurrency());
        if (band.getName() != null) existing.setName(band.getName());
        if (band.getCode() != null) existing.setCode(band.getCode());
        if (band.getDisplayOrder() != null) existing.setDisplayOrder(band.getDisplayOrder());
        if (band.getEffectiveFrom() != null) existing.setEffectiveFrom(band.getEffectiveFrom());
        if (band.getEffectiveTo() != null) existing.setEffectiveTo(band.getEffectiveTo());
        if (band.getEffectiveFrom() != null || band.getEffectiveTo() != null) {
            LocalDate from = existing.getEffectiveFrom();
            LocalDate to = existing.getEffectiveTo();
            validateEffectiveDates(from, to);
        }
        ensureNoOverlappingBandsInGrade(existing.getSalaryGradeId(), existing.getMinimumAmount(), existing.getMaximumAmount(), bandId);
        String oldSummary = bandSummary(existing);
        SalaryBand updated = salaryBandRepository.save(existing);
        SalaryGrade g = salaryGradeRepository.findById(updated.getSalaryGradeId()).orElse(null);
        if (g != null) {
            SalaryStructure struct = salaryStructureRepository.findById(g.getSalaryStructureId()).orElse(null);
            if (struct != null) {
                auditLog(struct.getOrganizationId(), SalaryAuditLog.ENTITY_BAND, updated.getSalaryBandId(), SalaryAuditLog.ACTION_UPDATE,
                    oldSummary, bandSummary(updated), updated.getUpdatedBy() != null ? updated.getUpdatedBy() : updated.getCreatedBy());
            }
        }
        return updated;
    }

    private static String bandSummary(SalaryBand b) {
        return "min=" + b.getMinimumAmount() + ",max=" + b.getMaximumAmount() + ",mid=" + b.getMidPoint()
            + ",effectiveFrom=" + b.getEffectiveFrom() + ",effectiveTo=" + b.getEffectiveTo();
    }

    /** SS-15: minimum ≤ mid ≤ maximum (when mid present), minimum < maximum. */
    private static void validateBandAmounts(BigDecimal min, BigDecimal max, BigDecimal mid) {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Band minimum and maximum amounts are required");
        }
        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Band minimum must be less than maximum");
        }
        if (mid != null) {
            if (mid.compareTo(min) < 0 || mid.compareTo(max) > 0) {
                throw new IllegalArgumentException("Band mid-point must be between minimum and maximum (inclusive)");
            }
        }
    }

    /** SS-20: Prevent overlapping pay ranges between bands within the same grade. */
    private void ensureNoOverlappingBandsInGrade(UUID gradeId, BigDecimal min, BigDecimal max, UUID excludeBandId) {
        List<SalaryBand> others = salaryBandRepository.findBySalaryGradeIdOrderByDisplayOrderAsc(gradeId).stream()
                .filter(b -> !b.getSalaryBandId().equals(excludeBandId))
                .toList();
        for (SalaryBand other : others) {
            if (other.getMinimumAmount() != null && other.getMaximumAmount() != null
                    && other.getMinimumAmount().compareTo(max) <= 0 && other.getMaximumAmount().compareTo(min) >= 0) {
                throw new ResourceConflictException(
                        "Band range [" + min + "–" + max + "] overlaps with existing band [" + other.getMinimumAmount() + "–" + other.getMaximumAmount() + "] in this grade. SS-20.");
            }
        }
    }

    /** SS-22: Delete grade only if no position uses it as default. */
    public void deleteGrade(UUID gradeId) {
        SalaryGrade grade = salaryGradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Salary grade not found"));
        if (positionRepository.existsByDefaultSalaryGradeId(gradeId)) {
            throw new ResourceConflictException(
                    "Cannot delete grade: at least one position uses it as default. SS-22.");
        }
        salaryGradeRepository.delete(grade);
    }

    /** SS-22: Delete band only if no position uses it as default. */
    public void deleteBand(UUID bandId) {
        SalaryBand band = salaryBandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("Salary band not found"));
        if (positionRepository.existsByDefaultSalaryBandId(bandId)) {
            throw new ResourceConflictException(
                    "Cannot delete band: at least one position uses it as default. SS-22.");
        }
        salaryBandRepository.delete(band);
    }
}

