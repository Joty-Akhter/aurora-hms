package com.easyops.hr.service;

import com.easyops.hr.dto.BulkImportResultDto;
import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.SalaryBand;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryComponentCategory;
import com.easyops.hr.entity.SalaryGrade;
import com.easyops.hr.entity.SalaryStructure;
import com.easyops.hr.entity.Taxability;
import com.easyops.hr.repository.SalaryGradeRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SS-49: Bulk import structures, grades, and bands from Excel (XLSX). Expects sheets: Structures, Grades, Bands.
 * SC-50: Bulk import components from sheet "Components".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryBulkImportService {

    private final SalaryService salaryService;
    private final SalaryStructureRepository salaryStructureRepository;
    private final SalaryGradeRepository salaryGradeRepository;

    @Transactional
    public BulkImportResultDto importFromExcel(MultipartFile file, UUID organizationId) {
        List<BulkImportResultDto.BulkImportErrorDto> errors = new ArrayList<>();
        int[] counts = { 0, 0, 0 }; // structures, grades, bands
        if (organizationId == null) {
            errors.add(new BulkImportResultDto.BulkImportErrorDto(0, "Request", "organizationId is required"));
            return BulkImportResultDto.builder().createdStructures(0).createdGrades(0).createdBands(0).errors(errors).build();
        }
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            importStructuresSheet(wb, organizationId, counts, errors);
            importGradesSheet(wb, organizationId, counts, errors);
            importBandsSheet(wb, organizationId, counts, errors);
        } catch (Exception e) {
            log.warn("Bulk import failed", e);
            errors.add(new BulkImportResultDto.BulkImportErrorDto(0, "File", e.getMessage() != null ? e.getMessage() : "Failed to parse file"));
        }
        return BulkImportResultDto.builder()
                .createdStructures(counts[0])
                .createdGrades(counts[1])
                .createdBands(counts[2])
                .errors(errors)
                .build();
    }

    /** SC-50: Bulk import salary components from Excel. Expects sheet "Components". */
    @Transactional
    public BulkImportResultDto importComponentsFromExcel(MultipartFile file, UUID organizationId) {
        List<BulkImportResultDto.BulkImportErrorDto> errors = new ArrayList<>();
        int createdComponents = 0;
        if (organizationId == null) {
            errors.add(new BulkImportResultDto.BulkImportErrorDto(0, "Request", "organizationId is required"));
            return BulkImportResultDto.builder().createdComponents(0).errors(errors).build();
        }
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("Components");
            if (sheet == null) {
                errors.add(new BulkImportResultDto.BulkImportErrorDto(0, "File", "Sheet 'Components' not found"));
                return BulkImportResultDto.builder().createdComponents(0).errors(errors).build();
            }
            int rowNum = 0;
            for (Row row : sheet) {
                rowNum++;
                if (rowNum == 1) continue; // header
                String code = getCellString(row, 0);
                if (code == null || code.isBlank()) continue;
                try {
                    SalaryComponent c = new SalaryComponent();
                    c.setOrganizationId(organizationId);
                    c.setCode(code.trim());
                    c.setComponentName(nonBlank(getCellString(row, 1), code.trim()));
                    c.setDescription(getCellString(row, 2));
                    c.setComponentType(nonBlank(getCellString(row, 3), "EARNING"));
                    String catStr = getCellString(row, 4);
                    if (catStr != null && !catStr.isBlank()) {
                        try {
                            c.setCategory(SalaryComponentCategory.valueOf(catStr.trim().toUpperCase().replace(" ", "_")));
                        } catch (IllegalArgumentException ignored) { }
                    }
                    c.setCalculationBasis(parseCalculationBasis(nonBlank(getCellString(row, 5), "FIXED")));
                    String amt = getCellString(row, 6);
                    if (amt != null && !amt.isBlank()) c.setDefaultAmount(new BigDecimal(amt.trim()));
                    String pct = getCellString(row, 7);
                    if (pct != null && !pct.isBlank()) c.setPercentageValue(new BigDecimal(pct.trim()));
                    c.setBaseComponentCode(blankToNull(getCellString(row, 8)));
                    c.setFormulaExpression(blankToNull(getCellString(row, 9)));
                    c.setStatutoryType(blankToNull(getCellString(row, 10)));
                    String ceil = getCellString(row, 11);
                    if (ceil != null && !ceil.isBlank()) c.setCeilingAmount(new BigDecimal(ceil.trim()));
                    String floor = getCellString(row, 12);
                    if (floor != null && !floor.isBlank()) c.setFloorAmount(new BigDecimal(floor.trim()));
                    String effFrom = getCellString(row, 13);
                    c.setEffectiveFrom(effFrom != null && !effFrom.isBlank() ? LocalDate.parse(effFrom.trim()) : LocalDate.now());
                    String effTo = getCellString(row, 14);
                    if (effTo != null && !effTo.isBlank()) c.setEffectiveTo(LocalDate.parse(effTo.trim()));
                    c.setDisplayOrder(getCellInt(row, 15, 0));
                    c.setShortName(blankToNull(getCellString(row, 16)));
                    c.setCurrency(blankToNull(getCellString(row, 17)));
                    String taxStr = getCellString(row, 18);
                    if (taxStr != null && !taxStr.isBlank()) {
                        try {
                            c.setTaxability(Taxability.valueOf(taxStr.trim().toUpperCase().replace(" ", "_")));
                        } catch (IllegalArgumentException ignored) { }
                        c.setIsTaxable("TAXABLE".equalsIgnoreCase(taxStr.trim()));
                    }
                    String tagsStr = getCellString(row, 19);
                    if (tagsStr != null && !tagsStr.isBlank()) {
                        c.setStatutoryTags(Arrays.stream(tagsStr.split("[,;]")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
                    }
                    c.setIsActive(!Boolean.FALSE.toString().equalsIgnoreCase(nonBlank(getCellString(row, 20), "true")));
                    salaryService.createSalaryComponent(c);
                    createdComponents++;
                } catch (Exception e) {
                    errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Component", e.getMessage() != null ? e.getMessage() : "Validation failed"));
                }
            }
        } catch (Exception e) {
            log.warn("Component bulk import failed", e);
            errors.add(new BulkImportResultDto.BulkImportErrorDto(0, "File", e.getMessage() != null ? e.getMessage() : "Failed to parse file"));
        }
        return BulkImportResultDto.builder()
                .createdComponents(createdComponents)
                .errors(errors)
                .build();
    }

    private static String blankToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private static CalculationBasis parseCalculationBasis(String value) {
        if (value == null || value.isBlank()) return CalculationBasis.FIXED;
        String u = value.trim().toUpperCase();
        try {
            return CalculationBasis.valueOf(u);
        } catch (IllegalArgumentException e) {
            if ("PERCENTAGE".equals(u) || "PERCENTAGEOFBASIC".equals(u.replace("_", ""))) return CalculationBasis.PERCENTAGE_OF_BASIC;
            if ("PERCENTAGEOFGROSS".equals(u.replace("_", ""))) return CalculationBasis.PERCENTAGE_OF_GROSS;
            return CalculationBasis.FIXED;
        }
    }

    private void importStructuresSheet(Workbook wb, UUID organizationId, int[] counts, List<BulkImportResultDto.BulkImportErrorDto> errors) {
        Sheet sheet = wb.getSheet("Structures");
        if (sheet == null) return;
        int rowNum = 0;
        for (Row row : sheet) {
            rowNum++;
            if (rowNum == 1) continue; // skip header
            String code = getCellString(row, 0);
            String name = getCellString(row, 1);
            if (code == null || code.isBlank()) continue;
            try {
                SalaryStructure s = new SalaryStructure();
                s.setOrganizationId(organizationId);
                s.setCode(code.trim());
                s.setStructureName(name != null && !name.isBlank() ? name.trim() : code);
                s.setPayFrequency(nonBlank(getCellString(row, 2), "monthly"));
                s.setCurrency(nonBlank(getCellString(row, 3), "BDT"));
                String effFrom = getCellString(row, 4);
                s.setEffectiveFrom(effFrom != null && !effFrom.isBlank() ? LocalDate.parse(effFrom.trim()) : LocalDate.now());
                s.setIsActive(true);
                s.setIsDefault(false);
                salaryService.createSalaryStructure(s);
                counts[0]++;
            } catch (Exception e) {
                errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Structure", e.getMessage() != null ? e.getMessage() : "Validation failed"));
            }
        }
    }

    private void importGradesSheet(Workbook wb, UUID organizationId, int[] counts, List<BulkImportResultDto.BulkImportErrorDto> errors) {
        Sheet sheet = wb.getSheet("Grades");
        if (sheet == null) return;
        int rowNum = 0;
        for (Row row : sheet) {
            rowNum++;
            if (rowNum == 1) continue;
            String structureCode = getCellString(row, 0);
            String code = getCellString(row, 1);
            String name = getCellString(row, 2);
            if (structureCode == null || structureCode.isBlank() || code == null || code.isBlank()) continue;
            try {
                SalaryStructure structure = salaryStructureRepository.findByOrganizationIdAndCode(organizationId, structureCode.trim()).orElse(null);
                if (structure == null) {
                    errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Grade", "Structure not found: " + structureCode));
                    continue;
                }
                SalaryGrade g = new SalaryGrade();
                g.setSalaryStructureId(structure.getSalaryStructureId());
                g.setCode(code.trim());
                g.setName(name != null && !name.isBlank() ? name.trim() : code);
                g.setDisplayOrder(getCellInt(row, 3, 0));
                g.setDescription(getCellString(row, 4));
                String effFrom = getCellString(row, 5);
                String effTo = getCellString(row, 6);
                if (effFrom != null && !effFrom.isBlank()) g.setEffectiveFrom(LocalDate.parse(effFrom.trim()));
                if (effTo != null && !effTo.isBlank()) g.setEffectiveTo(LocalDate.parse(effTo.trim()));
                salaryService.createGrade(g);
                counts[1]++;
            } catch (Exception e) {
                errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Grade", e.getMessage() != null ? e.getMessage() : "Validation failed"));
            }
        }
    }

    private void importBandsSheet(Workbook wb, UUID organizationId, int[] counts, List<BulkImportResultDto.BulkImportErrorDto> errors) {
        Sheet sheet = wb.getSheet("Bands");
        if (sheet == null) return;
        int rowNum = 0;
        for (Row row : sheet) {
            rowNum++;
            if (rowNum == 1) continue;
            String structureCode = getCellString(row, 0);
            String gradeCode = getCellString(row, 1);
            String minS = getCellString(row, 2);
            String maxS = getCellString(row, 3);
            if (structureCode == null || structureCode.isBlank() || gradeCode == null || gradeCode.isBlank() || minS == null || maxS == null) continue;
            try {
                SalaryStructure structure = salaryStructureRepository.findByOrganizationIdAndCode(organizationId, structureCode.trim()).orElse(null);
                if (structure == null) {
                    errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Band", "Structure not found: " + structureCode));
                    continue;
                }
                SalaryGrade grade = salaryGradeRepository.findBySalaryStructureIdAndCode(structure.getSalaryStructureId(), gradeCode.trim()).orElse(null);
                if (grade == null) {
                    errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Band", "Grade not found: " + gradeCode));
                    continue;
                }
                BigDecimal min = new BigDecimal(minS.trim());
                BigDecimal max = new BigDecimal(maxS.trim());
                SalaryBand b = new SalaryBand();
                b.setSalaryGradeId(grade.getSalaryGradeId());
                b.setMinimumAmount(min);
                b.setMaximumAmount(max);
                String midS = getCellString(row, 4);
                if (midS != null && !midS.isBlank()) b.setMidPoint(new BigDecimal(midS.trim()));
                b.setCurrency(nonBlank(getCellString(row, 5), structure.getCurrency()));
                b.setName(getCellString(row, 6));
                b.setCode(getCellString(row, 7));
                b.setDisplayOrder(getCellInt(row, 8, 0));
                String effFrom = getCellString(row, 9);
                String effTo = getCellString(row, 10);
                if (effFrom != null && !effFrom.isBlank()) b.setEffectiveFrom(LocalDate.parse(effFrom.trim()));
                if (effTo != null && !effTo.isBlank()) b.setEffectiveTo(LocalDate.parse(effTo.trim()));
                salaryService.createBand(b);
                counts[2]++;
            } catch (Exception e) {
                errors.add(new BulkImportResultDto.BulkImportErrorDto(rowNum + 1, "Band", e.getMessage() != null ? e.getMessage() : "Validation failed"));
            }
        }
    }

    private static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.STRING) return cell.getStringCellValue();
        if (type == CellType.NUMERIC)
            return DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
        if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        if (type == CellType.FORMULA) return cell.getCellFormula();
        return null;
    }

    private static int getCellInt(Row row, int col, int defaultValue) {
        String s = getCellString(row, col);
        if (s == null || s.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String nonBlank(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }
}
