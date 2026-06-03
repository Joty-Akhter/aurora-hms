package com.easyops.hr.controller;

import com.easyops.hr.dto.BulkImportResultDto;
import com.easyops.hr.dto.ComponentDependencyDto;
import com.easyops.hr.dto.ComponentUsageDto;
import com.easyops.hr.dto.ComponentWiseCostDto;
import com.easyops.hr.dto.EmployeeSelfSalarySummaryDto;
import com.easyops.hr.dto.GradeHeadcountDto;
import com.easyops.hr.dto.ProrationDto;
import com.easyops.hr.dto.SalaryStructureSummaryDto;
import com.easyops.hr.dto.PositionSalaryDefaultsDto;
import com.easyops.hr.dto.SalaryRevisionHistoryItemDto;
import com.easyops.hr.entity.EmployeeSalaryAssignment;
import com.easyops.hr.entity.EmployeeSalaryDetail;
import com.easyops.hr.entity.SalaryComponentCategory;
import com.easyops.hr.entity.SalaryAuditLog;
import com.easyops.hr.entity.SalaryBand;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryGrade;
import com.easyops.hr.entity.SalaryBulkRevision;
import com.easyops.hr.entity.SalaryStructure;
import com.easyops.hr.security.HrEmployeeSelfAccessService;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeService;
import com.easyops.hr.service.SalaryBulkImportService;
import com.easyops.hr.service.SalaryReportExportService;
import com.easyops.hr.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/salary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SalaryController {

    private final SalaryService salaryService;
    private final SalaryReportExportService salaryReportExportService;
    private final SalaryBulkImportService salaryBulkImportService;
    private final HrRbacService hrRbac;
    private final EmployeeService employeeService;
    private final HrEmployeeSelfAccessService hrEmployeeSelfAccessService;

    // Salary Structures
    /**
     * SS-41: List structures by organization.
     * Query params: organizationId (required), effectiveDate (optional), includeInactive (optional).
     * When effectiveDate is set, only structures effective on that date are returned.
     * When includeInactive is false or unset, only active structures are returned.
     */
    @GetMapping("/structures")
    public ResponseEntity<List<SalaryStructure>> getAllSalaryStructures(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(required = false) Boolean includeInactive) {
        requireView(userIdHeader, organizationId);
        log.info("GET /salary/structures - organizationId: {}, effectiveDate: {}, includeInactive: {}", organizationId, effectiveDate, includeInactive);
        List<SalaryStructure> structures = (effectiveDate != null || Boolean.FALSE.equals(includeInactive))
                ? salaryService.getAllSalaryStructures(organizationId, effectiveDate, Boolean.TRUE.equals(includeInactive))
                : salaryService.getAllSalaryStructures(organizationId);
        return ResponseEntity.ok(structures);
    }

    /** SS-45, SS-47: List structures with nested grades and bands (for payroll or report). */
    @GetMapping("/structures/summaries")
    public ResponseEntity<List<SalaryStructureSummaryDto>> getStructureSummaries(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(required = false) Boolean includeInactive) {
        boolean includeInactiveFlag = includeInactive != null && includeInactive;
        List<SalaryStructureSummaryDto> summaries = salaryService.getStructureSummaries(organizationId, effectiveDate, includeInactiveFlag);
        return ResponseEntity.ok(summaries);
    }

    /** SS-47: Export structure summary to Excel or PDF. */
    @GetMapping("/structures/summaries/export")
    public ResponseEntity<byte[]> exportStructureSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(required = false) Boolean includeInactive) {
        requireView(userIdHeader, organizationId);
        boolean includeInactiveFlag = includeInactive != null && includeInactive;
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportToPdf(organizationId, effectiveDate, includeInactiveFlag)
                : salaryReportExportService.exportToExcel(organizationId, effectiveDate, includeInactiveFlag);
        String contentType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF_VALUE : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "structure-summary." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** SS-48 / RPT-05: Grade-wise headcount and cost (structure, grade, band) as of date. */
    @GetMapping("/reports/grade-headcount")
    public ResponseEntity<List<GradeHeadcountDto>> getGradeHeadcount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate) {
        requireView(userIdHeader, organizationId);
        List<GradeHeadcountDto> headcount = salaryService.getGradeWiseHeadcountAndCost(organizationId, asOfDate);
        return ResponseEntity.ok(headcount);
    }

    /** RPT-05: Export grade-wise headcount and cost to Excel or PDF. */
    @GetMapping("/reports/grade-headcount/export")
    public ResponseEntity<byte[]> exportGradeHeadcountCost(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "excel") String format) {
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportGradeHeadcountCostToPdf(organizationId, asOfDate)
                : salaryReportExportService.exportGradeHeadcountCostToExcel(organizationId, asOfDate);
        String contentType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF_VALUE : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "grade-headcount-cost." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** ES-58: Component-wise cost report – total amount per component across employees (as-of date). */
    @GetMapping("/reports/component-cost")
    public ResponseEntity<List<ComponentWiseCostDto>> getComponentWiseCost(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate) {
        requireView(userIdHeader, organizationId);
        return ResponseEntity.ok(salaryService.getComponentWiseCost(organizationId, asOfDate));
    }

    /** ES-58: Export component-wise cost to Excel or PDF. */
    @GetMapping("/reports/component-cost/export")
    public ResponseEntity<byte[]> exportComponentWiseCost(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(defaultValue = "excel") String format) {
        requireView(userIdHeader, organizationId);
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportComponentWiseCostToPdf(organizationId, asOfDate)
                : salaryReportExportService.exportComponentWiseCostToExcel(organizationId, asOfDate);
        String contentType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF_VALUE : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "component-cost." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** ES-53: Employee self-service – view own salary (structure, grade, components; amounts maskable). */
    @GetMapping("/self/summary")
    public ResponseEntity<EmployeeSelfSalarySummaryDto> getSelfSalarySummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(required = false) Boolean maskAmounts) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, organizationId, employeeId);
        return salaryService.getSelfSalarySummary(employeeId, organizationId, asOfDate, Boolean.TRUE.equals(maskAmounts))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** RPT-03: Employee salary report export – employees with structure, grade, component-wise value (current or as-of). */
    @GetMapping("/reports/employee-salary/export")
    public ResponseEntity<byte[]> exportEmployeeSalaryReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID bandId,
            @RequestParam(defaultValue = "excel") String format) {
        requireView(userIdHeader, organizationId);
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportEmployeeSalaryReportToPdf(organizationId, asOfDate, structureId, gradeId, bandId)
                : salaryReportExportService.exportEmployeeSalaryReportToExcel(organizationId, asOfDate, structureId, gradeId, bandId);
        String contentType = "pdf".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "employee-salary-report." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** ES-59: Bulk export – employee salary (assignment + components) for selected employees or by grade/structure/band. */
    @GetMapping("/reports/employee-salary/bulk-export")
    public ResponseEntity<byte[]> bulkExportEmployeeSalary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate,
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID bandId,
            @RequestParam(required = false) List<UUID> employeeIds,
            @RequestParam(defaultValue = "excel") String format) {
        requireView(userIdHeader, organizationId);
        java.util.Set<UUID> idSet = (employeeIds != null && !employeeIds.isEmpty()) ? new java.util.HashSet<>(employeeIds) : null;
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportEmployeeSalaryReportToPdf(organizationId, asOfDate, structureId, gradeId, bandId, idSet)
                : salaryReportExportService.exportEmployeeSalaryReportToExcel(organizationId, asOfDate, structureId, gradeId, bandId, idSet);
        String contentType = "pdf".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "employee-salary-bulk." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** SS-49: Bulk import structures, grades, bands from XLSX (sheets: Structures, Grades, Bands). */
    @PostMapping(value = "/structures/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResultDto> bulkImport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam("file") MultipartFile file) {
        requireManage(userIdHeader, organizationId);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(BulkImportResultDto.builder()
                    .createdStructures(0).createdGrades(0).createdBands(0)
                    .errors(List.of(new BulkImportResultDto.BulkImportErrorDto(0, "File", "File is empty")))
                    .build());
        }
        BulkImportResultDto result = salaryBulkImportService.importFromExcel(file, organizationId);
        return ResponseEntity.ok(result);
    }

    /** SS-42: Get structure by id. Optional effectiveDate: returns 404 if structure is not effective on that date. */
    @GetMapping("/structures/{id}")
    public ResponseEntity<SalaryStructure> getSalaryStructure(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate effectiveDate) {
        Optional<SalaryStructure> opt = effectiveDate != null
                ? salaryService.getSalaryStructureByIdAndEffectiveDate(id, effectiveDate)
                : salaryService.getSalaryStructureById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        requireView(userIdHeader, opt.get().getOrganizationId());
        return ResponseEntity.ok(opt.get());
    }

    /** SS-42: Get structure by id with nested grades and bands. Optional effectiveDate filters grades/bands. */
    @GetMapping("/structures/{id}/summary")
    public ResponseEntity<SalaryStructureSummaryDto> getStructureSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate effectiveDate) {
        return salaryService.getStructureWithGradesAndBands(id, effectiveDate)
                .map(dto -> {
                    requireView(userIdHeader, dto.getOrganizationId());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/structures")
    public ResponseEntity<SalaryStructure> createSalaryStructure(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SalaryStructure structure) {
        requireManage(userIdHeader, structure.getOrganizationId());
        log.info("POST /salary/structures - Creating structure: {}", structure.getStructureName());
        SalaryStructure created = salaryService.createSalaryStructure(structure);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/structures/{id}")
    public ResponseEntity<SalaryStructure> updateSalaryStructure(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody SalaryStructure structure) {
        requireManage(userIdHeader, orgFromStructureId(id));
        log.info("PUT /salary/structures/{}", id);
        SalaryStructure updated = salaryService.updateSalaryStructure(id, structure);
        return ResponseEntity.ok(updated);
    }

    /** SS-29: Revision history (audit log) for a salary structure. */
    @GetMapping("/structures/{id}/revision-history")
    public ResponseEntity<List<SalaryAuditLog>> getStructureRevisionHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID id) {
        requireView(userIdHeader, organizationId);
        List<SalaryAuditLog> history = salaryService.getStructureRevisionHistory(organizationId, id);
        return ResponseEntity.ok(history);
    }

    // Salary Grades (SS-08–SS-12)
    @GetMapping("/structures/{structureId}/grades")
    public ResponseEntity<List<SalaryGrade>> getGradesByStructure(@PathVariable UUID structureId) {
        List<SalaryGrade> grades = salaryService.getGradesByStructureId(structureId);
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/grades/{id}")
    public ResponseEntity<SalaryGrade> getGradeById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        requireView(userIdHeader, orgFromGradeId(id));
        return salaryService.getGradeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/structures/{structureId}/grades")
    public ResponseEntity<SalaryGrade> createGrade(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID structureId,
            @RequestBody SalaryGrade grade) {
        requireManage(userIdHeader, orgFromStructureId(structureId));
        grade.setSalaryStructureId(structureId);
        SalaryGrade created = salaryService.createGrade(grade);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/grades/{id}")
    public ResponseEntity<SalaryGrade> updateGrade(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody SalaryGrade grade) {
        requireManage(userIdHeader, orgFromGradeId(id));
        SalaryGrade updated = salaryService.updateGrade(id, grade);
        return ResponseEntity.ok(updated);
    }

    // Salary Bands (SS-13–SS-19)
    @GetMapping("/grades/{gradeId}/bands")
    public ResponseEntity<List<SalaryBand>> getBandsByGrade(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID gradeId) {
        requireView(userIdHeader, orgFromGradeId(gradeId));
        List<SalaryBand> bands = salaryService.getBandsByGradeId(gradeId);
        return ResponseEntity.ok(bands);
    }

    @GetMapping("/bands/{id}")
    public ResponseEntity<SalaryBand> getBandById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        requireView(userIdHeader, orgFromBandId(id));
        return salaryService.getBandById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/grades/{gradeId}/bands")
    public ResponseEntity<SalaryBand> createBand(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID gradeId,
            @RequestBody SalaryBand band) {
        requireManage(userIdHeader, orgFromGradeId(gradeId));
        band.setSalaryGradeId(gradeId);
        SalaryBand created = salaryService.createBand(band);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/bands/{id}")
    public ResponseEntity<SalaryBand> updateBand(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody SalaryBand band) {
        requireManage(userIdHeader, orgFromBandId(id));
        SalaryBand updated = salaryService.updateBand(id, band);
        return ResponseEntity.ok(updated);
    }

    // Salary Components – SC-42–SC-47: create/update full payload; get by id/code; list by org, type, category, effectiveDate, includeInactive; list for payroll
    /** SC-45, SC-46: List by org with optional type, category, effectiveDate, includeInactive. Always ordered by displayOrder. */
    @GetMapping("/components")
    public ResponseEntity<List<SalaryComponent>> getAllSalaryComponents(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean includeInactive) {
        requireView(userIdHeader, organizationId);
        log.info("GET /salary/components - organizationId: {}, effectiveDate: {}, type: {}, category: {}, includeInactive: {}",
                organizationId, effectiveDate, type, category, includeInactive);
        List<SalaryComponent> components;
        if (type != null && !type.isBlank() || category != null && !category.isBlank()) {
            SalaryComponentCategory cat = null;
            if (category != null && !category.isBlank()) {
                try {
                    cat = SalaryComponentCategory.valueOf(category.trim().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException ignored) { }
            }
            components = salaryService.getSalaryComponentsWithFilters(
                    organizationId, type, cat, effectiveDate, Boolean.TRUE.equals(includeInactive));
        } else {
            components = salaryService.getSalaryComponentsEffectiveOn(
                    organizationId, effectiveDate, Boolean.TRUE.equals(includeInactive));
        }
        return ResponseEntity.ok(components);
    }

    /** SC-47: List for payroll – components effective on the given date, active only, ordered by displayOrder. effectiveDate defaults to today. */
    @GetMapping("/components/for-payroll")
    public ResponseEntity<List<SalaryComponent>> getSalaryComponentsForPayroll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate effectiveDate) {
        requireView(userIdHeader, organizationId);
        List<SalaryComponent> components = salaryService.getSalaryComponentsForPayroll(organizationId, effectiveDate);
        return ResponseEntity.ok(components);
    }

    /** SC-43: Get component by id. */
    @GetMapping("/components/{id}")
    public ResponseEntity<SalaryComponent> getSalaryComponentById(@PathVariable UUID id) {
        return salaryService.getSalaryComponentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/components/{id}/usage")
    public ResponseEntity<ComponentUsageDto> getComponentUsage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        requireView(userIdHeader, orgFromComponentId(id));
        return ResponseEntity.ok(salaryService.getComponentUsage(id));
    }

    /** SC-30: Audit log / revision history for this component. */
    @GetMapping("/components/{id}/revision-history")
    public ResponseEntity<List<SalaryAuditLog>> getComponentRevisionHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID id) {
        requireView(userIdHeader, organizationId);
        return ResponseEntity.ok(salaryService.getComponentRevisionHistory(organizationId, id));
    }

    /** ES-28–ES-30: Proration for pay period – days worked from join/relieving, optional prorated amount by component rule. */
    @GetMapping("/proration")
    public ResponseEntity<ProrationDto> getProrationForPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) LocalDate joinOverride,
            @RequestParam(required = false) LocalDate relievingOverride,
            @RequestParam(required = false) UUID componentId,
            @RequestParam(required = false) BigDecimal fullAmount) {
        requireView(userIdHeader, orgFromEmployeeId(employeeId));
        ProrationDto dto = salaryService.getProrationForPeriod(
                employeeId, periodStart, periodEnd, joinOverride, relievingOverride, componentId, fullAmount);
        return ResponseEntity.ok(dto);
    }

    /** SC-43: Get component by organization and code. */
    @GetMapping("/components/by-code")
    public ResponseEntity<SalaryComponent> getSalaryComponentByCode(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam String code) {
        requireView(userIdHeader, organizationId);
        return salaryService.getSalaryComponentByOrganizationIdAndCode(organizationId, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** SC-22: List components with the given statutory tag (e.g. PF_WAGE for PF wage base). */
    @GetMapping("/components/by-statutory-tag")
    public ResponseEntity<List<SalaryComponent>> getSalaryComponentsByStatutoryTag(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam String tag) {
        requireView(userIdHeader, organizationId);
        List<SalaryComponent> list = salaryService.getSalaryComponentsWithStatutoryTag(organizationId, tag);
        return ResponseEntity.ok(list);
    }
    
    /** SC-42: Create component with full payload. Returns 409 Conflict if code already exists in organization. */
    @PostMapping("/components")
    public ResponseEntity<SalaryComponent> createSalaryComponent(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SalaryComponent component) {
        requireManage(userIdHeader, component.getOrganizationId());
        log.info("POST /salary/components - Creating component: {} (code: {})", component.getComponentName(), component.getCode());
        SalaryComponent created = salaryService.createSalaryComponent(component);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** SC-42: Update component with full payload. Code and componentType are immutable. */
    @PutMapping("/components/{id}")
    public ResponseEntity<SalaryComponent> updateSalaryComponent(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody SalaryComponent component) {
        requireManage(userIdHeader, orgFromComponentId(id));
        log.info("PUT /salary/components/{}", id);
        SalaryComponent updated = salaryService.updateSalaryComponent(id, component);
        return ResponseEntity.ok(updated);
    }

    /** SC-48: Component master report export (Excel or PDF). Filter by type, category, effectiveDate, includeInactive. */
    @GetMapping("/components/export")
    public ResponseEntity<byte[]> exportComponentMaster(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(required = false) Boolean includeInactive,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        requireView(userIdHeader, organizationId);
        boolean includeInactiveFlag = Boolean.TRUE.equals(includeInactive);
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportComponentMasterToPdf(organizationId, effectiveDate, includeInactiveFlag, type, category)
                : salaryReportExportService.exportComponentMasterToExcel(organizationId, effectiveDate, includeInactiveFlag, type, category);
        String contentType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF_VALUE : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String filename = "component-master." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** SC-49: Component dependency report – components that reference other components (base or formula). */
    @GetMapping("/reports/component-dependencies")
    public ResponseEntity<List<ComponentDependencyDto>> getComponentDependencies(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        requireView(userIdHeader, organizationId);
        List<ComponentDependencyDto> list = salaryService.getComponentDependencies(organizationId);
        return ResponseEntity.ok(list);
    }

    /** SC-50: Bulk import salary components from XLSX (sheet "Components"). */
    @PostMapping(value = "/components/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResultDto> bulkImportComponents(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam("file") MultipartFile file) {
        requireManage(userIdHeader, organizationId);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(BulkImportResultDto.builder()
                    .createdComponents(0)
                    .errors(List.of(new BulkImportResultDto.BulkImportErrorDto(0, "File", "File is empty")))
                    .build());
        }
        BulkImportResultDto result = salaryBulkImportService.importComponentsFromExcel(file, organizationId);
        return ResponseEntity.ok(result);
    }

    // ---------- Employee Salary Details (ES-07–ES-15, ES-46–ES-48) ----------

    @GetMapping("/details")
    public ResponseEntity<List<EmployeeSalaryDetail>> getAllEmployeeSalaryDetails(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        requireView(userIdHeader, organizationId);
        log.info("GET /salary/details - organizationId: {}, employeeId: {}, asOfDate: {}", organizationId, employeeId, asOfDate);
        
        List<EmployeeSalaryDetail> details;
        if (employeeId != null) {
            details = asOfDate != null
                    ? salaryService.getEmployeeSalaryDetails(employeeId, organizationId, asOfDate)
                    : salaryService.getEmployeeSalaryDetails(employeeId, organizationId);
        } else {
            details = salaryService.getAllEmployeeSalaryDetails(organizationId);
        }
        
        return ResponseEntity.ok(details);
    }

    /** ES-46: Get employee salary details; optional asOfDate for one active per component per date. */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeSalaryDetail>> getEmployeeSalaryDetails(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        requireView(userIdHeader, organizationId);
        log.info("GET /salary/employee/{} asOfDate={}", employeeId, asOfDate);
        List<EmployeeSalaryDetail> details = asOfDate != null
                ? salaryService.getEmployeeSalaryDetails(employeeId, organizationId, asOfDate)
                : salaryService.getEmployeeSalaryDetails(employeeId, organizationId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/employee/details")
    public ResponseEntity<EmployeeSalaryDetail> createEmployeeSalaryDetail(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EmployeeSalaryDetail detail) {
        UUID orgId = detail.getOrganizationId() != null
                ? detail.getOrganizationId()
                : orgFromEmployeeId(detail.getEmployeeId());
        requireManage(userIdHeader, orgId);
        log.info("POST /salary/employee/details - Creating detail for employee: {}", detail.getEmployeeId());
        EmployeeSalaryDetail created = salaryService.createEmployeeSalaryDetail(detail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** ES-48: Update employee salary component (valueType, amount, percentage, effectiveTo). */
    @PutMapping("/employee/details/{detailId}")
    public ResponseEntity<EmployeeSalaryDetail> updateEmployeeSalaryDetail(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID detailId,
            @RequestBody EmployeeSalaryDetail updates) {
        requireManage(userIdHeader, orgFromDetailId(detailId));
        log.info("PUT /salary/employee/details/{}", detailId);
        EmployeeSalaryDetail updated = salaryService.updateEmployeeSalaryDetail(detailId, updates);
        return ResponseEntity.ok(updated);
    }

    /** ES-12: End component assignment with effective dating (set effectiveTo). */
    @DeleteMapping("/employee/details/{detailId}")
    public ResponseEntity<EmployeeSalaryDetail> endEmployeeSalaryDetail(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID detailId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate effectiveTo) {
        requireManage(userIdHeader, orgFromDetailId(detailId));
        log.info("DELETE /salary/employee/details/{} effectiveTo={}", detailId, effectiveTo);
        EmployeeSalaryDetail ended = salaryService.endEmployeeSalaryDetail(detailId, effectiveTo);
        return ResponseEntity.ok(ended);
    }

    // ---------- Employee Salary Assignment (ES-01–ES-06) ----------

    /** ES-01: Get active assignment for employee; optional asOfDate (default today). */
    @GetMapping("/assignments/employee/{employeeId}")
    public ResponseEntity<EmployeeSalaryAssignment> getEmployeeAssignment(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) LocalDate asOfDate) {
        Optional<EmployeeSalaryAssignment> assignment = salaryService.getAssignment(employeeId, organizationId, asOfDate);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Assignment history for an employee (newest first). */
    @GetMapping("/assignments/employee/{employeeId}/history")
    public ResponseEntity<List<EmployeeSalaryAssignment>> getEmployeeAssignmentHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID employeeId) {
        requireView(userIdHeader, orgFromEmployeeId(employeeId));
        return ResponseEntity.ok(salaryService.getAssignmentHistory(employeeId));
    }

    /** ES-18: Component detail history for an employee (newest first). */
    @GetMapping("/employee/{employeeId}/detail-history")
    public ResponseEntity<List<EmployeeSalaryDetail>> getEmployeeDetailHistory(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId) {
        return ResponseEntity.ok(salaryService.getDetailHistory(employeeId, organizationId));
    }

    /** ES-18: Unified revision history (assignments + component details) for an employee, sorted by effectiveFrom descending. */
    @GetMapping("/employee/{employeeId}/revision-history")
    public ResponseEntity<List<SalaryRevisionHistoryItemDto>> getEmployeeRevisionHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId) {
        requireView(userIdHeader, organizationId);
        return ResponseEntity.ok(salaryService.getRevisionHistory(employeeId, organizationId));
    }

    /** RPT-04: Salary revision history report export (per employee). */
    @GetMapping("/employee/{employeeId}/revision-history/export")
    public ResponseEntity<byte[]> exportEmployeeRevisionHistory(
            @PathVariable UUID employeeId,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "excel") String format) {
        byte[] bytes = "pdf".equalsIgnoreCase(format)
                ? salaryReportExportService.exportEmployeeRevisionHistoryToPdf(employeeId, organizationId)
                : salaryReportExportService.exportEmployeeRevisionHistoryToExcel(employeeId, organizationId);
        String contentType = "pdf".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument-spreadsheetml.sheet";
        String filename = "salary-revision-history." + ("pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /** ES-01–ES-06: Create assignment (structure, grade, optional band, effectiveFrom/To, source). */
    @PostMapping("/assignments")
    public ResponseEntity<EmployeeSalaryAssignment> createAssignment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EmployeeSalaryAssignment assignment) {
        requireManage(userIdHeader, assignment.getOrganizationId());
        EmployeeSalaryAssignment created = salaryService.createAssignment(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Update assignment (e.g. set effectiveTo, or change grade/band). */
    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<EmployeeSalaryAssignment> updateAssignment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID assignmentId,
            @RequestBody EmployeeSalaryAssignment updates) {
        requireManage(userIdHeader, orgFromAssignmentId(assignmentId));
        EmployeeSalaryAssignment updated = salaryService.updateAssignment(assignmentId, updates);
        return ResponseEntity.ok(updated);
    }

    /** Get assignment by id. */
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<EmployeeSalaryAssignment> getAssignmentById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID assignmentId) {
        requireView(userIdHeader, orgFromAssignmentId(assignmentId));
        return salaryService.getAssignmentById(assignmentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** List assignments by org; optional filters structureId, gradeId, asOfDate. */
    @GetMapping("/assignments")
    public ResponseEntity<List<EmployeeSalaryAssignment>> listAssignments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) LocalDate asOfDate) {
        requireView(userIdHeader, organizationId);
        return ResponseEntity.ok(salaryService.listAssignments(organizationId, structureId, gradeId, asOfDate));
    }

    /** ES-06: Default structure/grade/band from position (for suggesting or auto-fill in assignment UI). */
    @GetMapping("/positions/{positionId}/salary-defaults")
    public ResponseEntity<PositionSalaryDefaultsDto> getPositionSalaryDefaults(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID positionId) {
        return salaryService.getPositionSalaryDefaults(positionId)
                .map(dto -> {
                    requireView(userIdHeader, dto.getOrganizationId());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------- Bulk Revision (ES-21) ----------

    /** Create bulk revision request (status PENDING). Optional approval: call POST .../approve to apply. */
    @PostMapping("/bulk-revisions")
    public ResponseEntity<SalaryBulkRevision> createBulkRevision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SalaryBulkRevision revision) {
        requireManage(userIdHeader, revision.getOrganizationId());
        SalaryBulkRevision created = salaryService.createBulkRevision(revision);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Approve and apply bulk revision (creates new effective-dated detail rows). */
    @PostMapping("/bulk-revisions/{bulkRevisionId}/approve")
    public ResponseEntity<SalaryBulkRevision> approveBulkRevision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bulkRevisionId,
            @RequestParam(required = false) String approvedBy) {
        requireManage(userIdHeader, orgFromBulkRevisionId(bulkRevisionId));
        SalaryBulkRevision updated = salaryService.approveBulkRevision(bulkRevisionId, approvedBy != null ? approvedBy : "system");
        return ResponseEntity.ok(updated);
    }

    /** Reject pending bulk revision. */
    @PostMapping("/bulk-revisions/{bulkRevisionId}/reject")
    public ResponseEntity<SalaryBulkRevision> rejectBulkRevision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bulkRevisionId,
            @RequestParam(required = false) String reason) {
        requireManage(userIdHeader, orgFromBulkRevisionId(bulkRevisionId));
        SalaryBulkRevision updated = salaryService.rejectBulkRevision(bulkRevisionId, reason != null ? reason : "Rejected");
        return ResponseEntity.ok(updated);
    }

    /** List bulk revisions by org; optional status filter (PENDING, APPROVED, REJECTED, APPLIED). */
    @GetMapping("/bulk-revisions")
    public ResponseEntity<List<SalaryBulkRevision>> listBulkRevisions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        requireView(userIdHeader, organizationId);
        return ResponseEntity.ok(salaryService.listBulkRevisions(organizationId, status));
    }

    @GetMapping("/bulk-revisions/{bulkRevisionId}")
    public ResponseEntity<SalaryBulkRevision> getBulkRevisionById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bulkRevisionId) {
        return salaryService.getBulkRevisionById(bulkRevisionId)
                .map(br -> {
                    requireView(userIdHeader, br.getOrganizationId());
                    return ResponseEntity.ok(br);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void requireView(String userIdHeader, UUID organizationId) {
        hrRbac.requireHrView(RbacRequestHeaders.requireUserId(userIdHeader), organizationId);
    }

    private void requireManage(String userIdHeader, UUID organizationId) {
        hrRbac.requireHrManage(RbacRequestHeaders.requireUserId(userIdHeader), organizationId);
    }

    private UUID orgFromEmployeeId(UUID employeeId) {
        return employeeService.getEmployeeById(employeeId).getOrganizationId();
    }

    private UUID orgFromStructureId(UUID structureId) {
        return salaryService.getSalaryStructureById(structureId)
                .map(SalaryStructure::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary structure not found"));
    }

    private UUID orgFromGradeId(UUID gradeId) {
        return salaryService.getGradeById(gradeId)
                .map(g -> orgFromStructureId(g.getSalaryStructureId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary grade not found"));
    }

    private UUID orgFromBandId(UUID bandId) {
        return salaryService.getBandById(bandId)
                .map(b -> orgFromGradeId(b.getSalaryGradeId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary band not found"));
    }

    private UUID orgFromComponentId(UUID componentId) {
        return salaryService.getSalaryComponentById(componentId)
                .map(SalaryComponent::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary component not found"));
    }

    private UUID orgFromAssignmentId(UUID assignmentId) {
        return salaryService.getAssignmentById(assignmentId)
                .map(EmployeeSalaryAssignment::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private UUID orgFromDetailId(UUID detailId) {
        return salaryService.getEmployeeSalaryDetailById(detailId)
                .map(EmployeeSalaryDetail::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary detail not found"));
    }

    private UUID orgFromBulkRevisionId(UUID bulkRevisionId) {
        return salaryService.getBulkRevisionById(bulkRevisionId)
                .map(SalaryBulkRevision::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bulk revision not found"));
    }

}

