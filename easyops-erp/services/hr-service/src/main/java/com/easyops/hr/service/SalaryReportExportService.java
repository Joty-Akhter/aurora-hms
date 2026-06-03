package com.easyops.hr.service;

import com.easyops.hr.dto.ComponentWiseCostDto;
import com.easyops.hr.dto.GradeHeadcountDto;
import com.easyops.hr.dto.SalaryBandSummaryDto;
import com.easyops.hr.dto.SalaryGradeSummaryDto;
import com.easyops.hr.dto.SalaryRevisionHistoryItemDto;
import com.easyops.hr.dto.SalaryStructureSummaryDto;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.EmployeeSalaryAssignment;
import com.easyops.hr.entity.EmployeeSalaryDetail;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryComponentCategory;
import com.easyops.hr.entity.SalaryGrade;
import com.easyops.hr.entity.SalaryStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.EmployeeSalaryAssignmentRepository;
import com.easyops.hr.repository.EmployeeSalaryDetailRepository;
import com.easyops.hr.repository.SalaryBandRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import com.easyops.hr.repository.SalaryGradeRepository;
import com.easyops.hr.repository.SalaryStructureRepository;

/**
 * SS-47: Export structure summary to Excel and PDF.
 * SC-48: Export component master report to Excel and PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryReportExportService {

    private final SalaryService salaryService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final SalaryGradeRepository salaryGradeRepository;
    private final SalaryBandRepository salaryBandRepository;
    private final SalaryComponentRepository salaryComponentRepository;

    public byte[] exportToExcel(UUID organizationId, LocalDate effectiveDate, boolean includeInactive) {
        List<SalaryStructureSummaryDto> summaries = salaryService.getStructureSummaries(
                organizationId, effectiveDate, includeInactive);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Structure Summary");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            int rowNum = 0;
            for (SalaryStructureSummaryDto s : summaries) {
                rowNum = writeStructureExcel(sheet, rowNum, s, headerStyle);
            }
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private int writeStructureExcel(Sheet sheet, int startRow, SalaryStructureSummaryDto s, CellStyle headerStyle) {
        int row = startRow;
        Row titleRow = sheet.createRow(row++);
        titleRow.createCell(0).setCellValue("Structure: " + (s.getCode() != null ? s.getCode() : "") + " - " + s.getStructureName());
        titleRow.getCell(0).setCellStyle(headerStyle);
        row++;
        Row headerRow = sheet.createRow(row++);
        String[] headers = { "Level", "Code", "Name", "Min", "Mid", "Max", "Currency", "Effective" };
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        for (SalaryGradeSummaryDto g : s.getGrades()) {
            Row gradeRow = sheet.createRow(row++);
            gradeRow.createCell(0).setCellValue("Grade");
            gradeRow.createCell(1).setCellValue(g.getCode() != null ? g.getCode() : "");
            gradeRow.createCell(2).setCellValue(g.getName() != null ? g.getName() : "");
            gradeRow.createCell(6).setCellValue(s.getCurrency() != null ? s.getCurrency() : "");
            gradeRow.createCell(7).setCellValue(formatEffective(g.getEffectiveFrom(), g.getEffectiveTo()));
            for (SalaryBandSummaryDto b : g.getBands()) {
                Row bandRow = sheet.createRow(row++);
                bandRow.createCell(0).setCellValue("Band");
                bandRow.createCell(1).setCellValue(b.getCode() != null ? b.getCode() : "");
                bandRow.createCell(2).setCellValue(b.getName() != null ? b.getName() : "");
                setDecimal(bandRow, 3, b.getMinimumAmount());
                setDecimal(bandRow, 4, b.getMidPoint());
                setDecimal(bandRow, 5, b.getMaximumAmount());
                bandRow.createCell(6).setCellValue(b.getCurrency() != null ? b.getCurrency() : "");
                bandRow.createCell(7).setCellValue(formatEffective(b.getEffectiveFrom(), b.getEffectiveTo()));
            }
        }
        row++;
        return row;
    }

    private void setDecimal(Row row, int col, BigDecimal value) {
        if (value != null) {
            row.createCell(col).setCellValue(value.doubleValue());
        }
    }

    private String formatEffective(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "";
        if (from == null) return "– to " + to;
        if (to == null) return from + " –";
        return from + " to " + to;
    }

    public byte[] exportToPdf(UUID organizationId, LocalDate effectiveDate, boolean includeInactive) {
        List<SalaryStructureSummaryDto> summaries = salaryService.getStructureSummaries(
                organizationId, effectiveDate, includeInactive);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            com.lowagie.text.Font pdfTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (SalaryStructureSummaryDto s : summaries) {
                document.add(new Paragraph("Structure: " + (s.getCode() != null ? s.getCode() : "") + " - " + s.getStructureName(), pdfTitleFont));
                document.add(Chunk.NEWLINE);
                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1f, 1.5f, 2.5f, 2f, 2f, 2f, 1f, 2.5f});
                table.addCell(headerCell("Level", pdfHeaderFont));
                table.addCell(headerCell("Code", pdfHeaderFont));
                table.addCell(headerCell("Name", pdfHeaderFont));
                table.addCell(headerCell("Min", pdfHeaderFont));
                table.addCell(headerCell("Mid", pdfHeaderFont));
                table.addCell(headerCell("Max", pdfHeaderFont));
                table.addCell(headerCell("Currency", pdfHeaderFont));
                table.addCell(headerCell("Effective", pdfHeaderFont));
                for (SalaryGradeSummaryDto g : s.getGrades()) {
                    table.addCell(cell("Grade"));
                    table.addCell(cell(g.getCode() != null ? g.getCode() : ""));
                    table.addCell(cell(g.getName() != null ? g.getName() : ""));
                    table.addCell(cell(""));
                    table.addCell(cell(""));
                    table.addCell(cell(""));
                    table.addCell(cell(s.getCurrency() != null ? s.getCurrency() : ""));
                    table.addCell(cell(formatEffective(g.getEffectiveFrom(), g.getEffectiveTo())));
                    for (SalaryBandSummaryDto b : g.getBands()) {
                        table.addCell(cell("Band"));
                        table.addCell(cell(b.getCode() != null ? b.getCode() : ""));
                        table.addCell(cell(b.getName() != null ? b.getName() : ""));
                        table.addCell(cell(b.getMinimumAmount() != null ? b.getMinimumAmount().toPlainString() : ""));
                        table.addCell(cell(b.getMidPoint() != null ? b.getMidPoint().toPlainString() : ""));
                        table.addCell(cell(b.getMaximumAmount() != null ? b.getMaximumAmount().toPlainString() : ""));
                        table.addCell(cell(b.getCurrency() != null ? b.getCurrency() : ""));
                        table.addCell(cell(formatEffective(b.getEffectiveFrom(), b.getEffectiveTo())));
                    }
                }
                document.add(table);
                document.add(Chunk.NEWLINE);
            }
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private PdfPCell headerCell(String text, com.lowagie.text.Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setGrayFill(0.9f);
        return c;
    }

    private PdfPCell cell(String text) {
        return new PdfPCell(new Phrase(text != null ? text : ""));
    }

    // ---------- SC-48: Component master report export ----------
    /** SC-48: Export component master to Excel. Filter by type, category, effectiveDate, includeInactive. */
    public byte[] exportComponentMasterToExcel(UUID organizationId, LocalDate effectiveDate, boolean includeInactive,
                                               String type, String category) {
        List<SalaryComponent> components = getComponentsForReport(organizationId, effectiveDate, includeInactive, type, category);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Component Master");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Code", "Name", "Type", "Category", "Calculation Basis", "Base/%/Formula", "Statutory Tags", "Taxability", "Effective From", "Effective To", "Display Order", "Payslip Label", "Currency" };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            for (SalaryComponent c : components) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(c.getCode() != null ? c.getCode() : "");
                row.createCell(1).setCellValue(c.getComponentName() != null ? c.getComponentName() : "");
                row.createCell(2).setCellValue(c.getComponentType() != null ? c.getComponentType() : "");
                row.createCell(3).setCellValue(c.getCategory() != null ? c.getCategory().name() : "");
                row.createCell(4).setCellValue(c.getCalculationBasis() != null ? c.getCalculationBasis().name() : "");
                row.createCell(5).setCellValue(formatCalculationDetail(c));
                row.createCell(6).setCellValue(c.getStatutoryTags() != null ? String.join(", ", c.getStatutoryTags()) : "");
                row.createCell(7).setCellValue(c.getTaxability() != null ? c.getTaxability().name() : "");
                row.createCell(8).setCellValue(c.getEffectiveFrom() != null ? c.getEffectiveFrom().toString() : "");
                row.createCell(9).setCellValue(c.getEffectiveTo() != null ? c.getEffectiveTo().toString() : "");
                row.createCell(10).setCellValue(c.getDisplayOrder() != null ? c.getDisplayOrder().intValue() : 0);
                row.createCell(11).setCellValue(c.getShortName() != null ? c.getShortName() : "");
                row.createCell(12).setCellValue(c.getCurrency() != null ? c.getCurrency() : "");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate component master Excel", e);
        }
    }

    /** SC-48: Export component master to PDF. */
    public byte[] exportComponentMasterToPdf(UUID organizationId, LocalDate effectiveDate, boolean includeInactive,
                                              String type, String category) {
        List<SalaryComponent> components = getComponentsForReport(organizationId, effectiveDate, includeInactive, type, category);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            String[] headers = { "Code", "Name", "Type", "Category", "Calculation", "Base/%/Formula", "Taxability", "Effective", "Order" };
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2f, 1f, 1.5f, 1.5f, 2f, 1f, 1.5f, 0.6f});
            for (String h : headers) {
                table.addCell(headerCell(h, pdfHeaderFont));
            }
            for (SalaryComponent c : components) {
                table.addCell(cell(c.getCode()));
                table.addCell(cell(c.getComponentName()));
                table.addCell(cell(c.getComponentType()));
                table.addCell(cell(c.getCategory() != null ? c.getCategory().name() : ""));
                table.addCell(cell(c.getCalculationBasis() != null ? c.getCalculationBasis().name() : ""));
                table.addCell(cell(formatCalculationDetail(c)));
                table.addCell(cell(c.getTaxability() != null ? c.getTaxability().name() : ""));
                table.addCell(cell(formatEffective(c.getEffectiveFrom(), c.getEffectiveTo())));
                table.addCell(cell(c.getDisplayOrder() != null ? String.valueOf(c.getDisplayOrder()) : ""));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate component master PDF", e);
        }
    }

    private List<SalaryComponent> getComponentsForReport(UUID organizationId, LocalDate effectiveDate, boolean includeInactive,
                                                        String type, String category) {
        SalaryComponentCategory cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = SalaryComponentCategory.valueOf(category.trim().toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException ignored) { }
        }
        if ((type != null && !type.isBlank()) || cat != null) {
            return salaryService.getSalaryComponentsWithFilters(organizationId, type, cat, effectiveDate, includeInactive);
        }
        return salaryService.getSalaryComponentsEffectiveOn(organizationId, effectiveDate, includeInactive);
    }

    private static String formatCalculationDetail(SalaryComponent c) {
        if (c == null) return "";
        var basis = c.getCalculationBasis();
        if (basis == null) return "";
        if (basis == com.easyops.hr.entity.CalculationBasis.PERCENTAGE_OF_BASIC || basis == com.easyops.hr.entity.CalculationBasis.PERCENTAGE_OF_GROSS) {
            String pct = c.getPercentageValue() != null ? c.getPercentageValue().toPlainString() + "%" : "";
            String base = c.getBaseComponentCode() != null ? " of " + c.getBaseComponentCode() : "";
            return pct + base;
        }
        if (basis == com.easyops.hr.entity.CalculationBasis.FORMULA && c.getFormulaExpression() != null) {
            return c.getFormulaExpression();
        }
        if (basis == com.easyops.hr.entity.CalculationBasis.FIXED || basis == com.easyops.hr.entity.CalculationBasis.MANUAL) {
            return c.getDefaultAmount() != null ? c.getDefaultAmount().toPlainString() : "";
        }
        if (basis == com.easyops.hr.entity.CalculationBasis.STATUTORY && c.getStatutoryType() != null) {
            return c.getStatutoryType();
        }
        return "";
    }

    // ---------- RPT-03: Employee salary report (structure, grade, component-wise value as-of date) ----------

    public byte[] exportEmployeeSalaryReportToExcel(UUID organizationId,
                                                    LocalDate asOfDate,
                                                    UUID structureId,
                                                    UUID gradeId,
                                                    UUID bandId) {
        return exportEmployeeSalaryReportToExcel(organizationId, asOfDate, structureId, gradeId, bandId, null);
    }

    /** ES-59: Bulk export – same as above with optional filter by employee IDs (selected employees). */
    public byte[] exportEmployeeSalaryReportToExcel(UUID organizationId,
                                                    LocalDate asOfDate,
                                                    UUID structureId,
                                                    UUID gradeId,
                                                    UUID bandId,
                                                    Set<UUID> employeeIds) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<Employee> employees = employeeRepository.findByOrganizationIdAndEmploymentStatus(organizationId, "ACTIVE");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            employees = employees.stream().filter(e -> employeeIds.contains(e.getEmployeeId())).toList();
        }

        Map<UUID, SalaryStructure> structureCache = new HashMap<>();
        Map<UUID, SalaryGrade> gradeCache = new HashMap<>();
        Map<UUID, com.easyops.hr.entity.SalaryBand> bandCache = new HashMap<>();
        Map<UUID, SalaryComponent> componentCache = new HashMap<>();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Employee Salary");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                    "Employee Number", "Employee Name", "Employee ID",
                    "Structure Code", "Structure Name",
                    "Grade Code", "Grade Name",
                    "Band Code", "Band Name",
                    "Component Code", "Component Name", "Component Type", "Category",
                    "Value Type", "Amount", "Percentage", "Currency",
                    "Detail Effective From", "Detail Effective To"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (Employee employee : employees) {
                if (employee.getHireDate() != null && employee.getHireDate().isAfter(date)) {
                    continue;
                }
                if (employee.getTerminationDate() != null && employee.getTerminationDate().isBefore(date)) {
                    continue;
                }

                List<EmployeeSalaryAssignment> assignments =
                        employeeSalaryAssignmentRepository.findActiveByEmployeeIdAndDate(employee.getEmployeeId(), date);
                if (assignments.isEmpty()) {
                    continue;
                }
                EmployeeSalaryAssignment assignment = assignments.get(0);
                if (!organizationId.equals(assignment.getOrganizationId())) {
                    continue;
                }
                if (structureId != null && !structureId.equals(assignment.getSalaryStructureId())) {
                    continue;
                }
                if (gradeId != null && !gradeId.equals(assignment.getSalaryGradeId())) {
                    continue;
                }
                if (bandId != null && (assignment.getSalaryBandId() == null || !bandId.equals(assignment.getSalaryBandId()))) {
                    continue;
                }

                SalaryStructure structure = structureCache.computeIfAbsent(
                        assignment.getSalaryStructureId(),
                        id -> salaryStructureRepository.findById(id).orElse(null)
                );
                SalaryGrade grade = gradeCache.computeIfAbsent(
                        assignment.getSalaryGradeId(),
                        id -> salaryGradeRepository.findById(id).orElse(null)
                );
                com.easyops.hr.entity.SalaryBand band = null;
                if (assignment.getSalaryBandId() != null) {
                    band = bandCache.computeIfAbsent(
                            assignment.getSalaryBandId(),
                            id -> salaryBandRepository.findById(id).orElse(null)
                    );
                }

                List<EmployeeSalaryDetail> details =
                        employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(
                                employee.getEmployeeId(), organizationId, date);
                if (details.isEmpty()) {
                    continue;
                }

                for (EmployeeSalaryDetail detail : details) {
                    SalaryComponent component = componentCache.computeIfAbsent(
                            detail.getComponentId(),
                            id -> salaryComponentRepository.findById(id).orElse(null)
                    );

                    Row row = sheet.createRow(rowNum++);
                    int col = 0;
                    row.createCell(col++).setCellValue(employee.getEmployeeNumber());
                    row.createCell(col++).setCellValue(employee.getFullName());
                    row.createCell(col++).setCellValue(employee.getEmployeeId().toString());

                    row.createCell(col++).setCellValue(structure != null && structure.getCode() != null ? structure.getCode() : "");
                    row.createCell(col++).setCellValue(structure != null && structure.getStructureName() != null ? structure.getStructureName() : "");

                    row.createCell(col++).setCellValue(grade != null && grade.getCode() != null ? grade.getCode() : "");
                    row.createCell(col++).setCellValue(grade != null && grade.getName() != null ? grade.getName() : "");

                    row.createCell(col++).setCellValue(band != null && band.getCode() != null ? band.getCode() : "");
                    row.createCell(col++).setCellValue(band != null && band.getName() != null ? band.getName() : "");

                    row.createCell(col++).setCellValue(component != null && component.getCode() != null ? component.getCode() : "");
                    row.createCell(col++).setCellValue(component != null && component.getComponentName() != null ? component.getComponentName() : "");
                    row.createCell(col++).setCellValue(component != null && component.getComponentType() != null ? component.getComponentType() : "");
                    row.createCell(col++).setCellValue(component != null && component.getCategory() != null ? component.getCategory().name() : "");

                    row.createCell(col++).setCellValue(detail.getValueType() != null ? detail.getValueType().name() : "");
                    if (detail.getAmount() != null) {
                        row.createCell(col++).setCellValue(detail.getAmount().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    if (detail.getPercentage() != null) {
                        row.createCell(col++).setCellValue(detail.getPercentage().doubleValue());
                    } else {
                        row.createCell(col++).setCellValue("");
                    }
                    String currency = null;
                    if (component != null && component.getCurrency() != null) {
                        currency = component.getCurrency();
                    } else if (structure != null) {
                        currency = structure.getCurrency();
                    }
                    row.createCell(col++).setCellValue(currency != null ? currency : "");

                    row.createCell(col++).setCellValue(detail.getEffectiveFrom() != null ? detail.getEffectiveFrom().toString() : "");
                    row.createCell(col++).setCellValue(detail.getEffectiveTo() != null ? detail.getEffectiveTo().toString() : "");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate employee salary Excel", e);
        }
    }

    public byte[] exportEmployeeSalaryReportToPdf(UUID organizationId,
                                                  LocalDate asOfDate,
                                                  UUID structureId,
                                                  UUID gradeId,
                                                  UUID bandId) {
        return exportEmployeeSalaryReportToPdf(organizationId, asOfDate, structureId, gradeId, bandId, null);
    }

    /** ES-59: Bulk export – same with optional filter by employee IDs. */
    public byte[] exportEmployeeSalaryReportToPdf(UUID organizationId,
                                                  LocalDate asOfDate,
                                                  UUID structureId,
                                                  UUID gradeId,
                                                  UUID bandId,
                                                  Set<UUID> employeeIds) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<Employee> employees = employeeRepository.findByOrganizationIdAndEmploymentStatus(organizationId, "ACTIVE");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            employees = employees.stream().filter(e -> employeeIds.contains(e.getEmployeeId())).toList();
        }

        Map<UUID, SalaryStructure> structureCache = new HashMap<>();
        Map<UUID, SalaryGrade> gradeCache = new HashMap<>();
        Map<UUID, com.easyops.hr.entity.SalaryBand> bandCache = new HashMap<>();
        Map<UUID, SalaryComponent> componentCache = new HashMap<>();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            String[] headers = {
                    "Emp No", "Name",
                    "Struct", "Grade", "Band",
                    "Comp Code", "Comp Name",
                    "Type", "Cat",
                    "ValueType", "Amount", "%", "Curr",
                    "From", "To"
            };

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            for (String h : headers) {
                table.addCell(headerCell(h, pdfHeaderFont));
            }

            for (Employee employee : employees) {
                if (employee.getHireDate() != null && employee.getHireDate().isAfter(date)) {
                    continue;
                }
                if (employee.getTerminationDate() != null && employee.getTerminationDate().isBefore(date)) {
                    continue;
                }

                List<EmployeeSalaryAssignment> assignments =
                        employeeSalaryAssignmentRepository.findActiveByEmployeeIdAndDate(employee.getEmployeeId(), date);
                if (assignments.isEmpty()) {
                    continue;
                }
                EmployeeSalaryAssignment assignment = assignments.get(0);
                if (!organizationId.equals(assignment.getOrganizationId())) {
                    continue;
                }
                if (structureId != null && !structureId.equals(assignment.getSalaryStructureId())) {
                    continue;
                }
                if (gradeId != null && !gradeId.equals(assignment.getSalaryGradeId())) {
                    continue;
                }
                if (bandId != null && (assignment.getSalaryBandId() == null || !bandId.equals(assignment.getSalaryBandId()))) {
                    continue;
                }

                SalaryStructure structure = structureCache.computeIfAbsent(
                        assignment.getSalaryStructureId(),
                        id -> salaryStructureRepository.findById(id).orElse(null)
                );
                SalaryGrade grade = gradeCache.computeIfAbsent(
                        assignment.getSalaryGradeId(),
                        id -> salaryGradeRepository.findById(id).orElse(null)
                );
                com.easyops.hr.entity.SalaryBand band = null;
                if (assignment.getSalaryBandId() != null) {
                    band = bandCache.computeIfAbsent(
                            assignment.getSalaryBandId(),
                            id -> salaryBandRepository.findById(id).orElse(null)
                    );
                }

                List<EmployeeSalaryDetail> details =
                        employeeSalaryDetailRepository.findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(
                                employee.getEmployeeId(), organizationId, date);
                if (details.isEmpty()) {
                    continue;
                }

                for (EmployeeSalaryDetail detail : details) {
                    SalaryComponent component = componentCache.computeIfAbsent(
                            detail.getComponentId(),
                            id -> salaryComponentRepository.findById(id).orElse(null)
                    );

                    table.addCell(cell(employee.getEmployeeNumber()));
                    table.addCell(cell(employee.getFullName()));

                    table.addCell(cell(structure != null ? structure.getCode() : ""));
                    table.addCell(cell(grade != null ? grade.getCode() : ""));
                    table.addCell(cell(band != null ? band.getCode() : ""));

                    table.addCell(cell(component != null ? component.getCode() : ""));
                    table.addCell(cell(component != null ? component.getComponentName() : ""));
                    table.addCell(cell(component != null ? component.getComponentType() : ""));
                    table.addCell(cell(component != null && component.getCategory() != null ? component.getCategory().name() : ""));

                    table.addCell(cell(detail.getValueType() != null ? detail.getValueType().name() : ""));
                    table.addCell(cell(detail.getAmount() != null ? detail.getAmount().toPlainString() : ""));
                    table.addCell(cell(detail.getPercentage() != null ? detail.getPercentage().toPlainString() : ""));

                    String currency = null;
                    if (component != null && component.getCurrency() != null) {
                        currency = component.getCurrency();
                    } else if (structure != null) {
                        currency = structure.getCurrency();
                    }
                    table.addCell(cell(currency != null ? currency : ""));

                    table.addCell(cell(detail.getEffectiveFrom() != null ? detail.getEffectiveFrom().toString() : ""));
                    table.addCell(cell(detail.getEffectiveTo() != null ? detail.getEffectiveTo().toString() : ""));
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate employee salary PDF", e);
        }
    }

    // ---------- RPT-04: Salary revision history report (per employee) ----------

    public byte[] exportEmployeeRevisionHistoryToExcel(UUID employeeId, UUID organizationId) {
        List<SalaryRevisionHistoryItemDto> history = salaryService.getRevisionHistory(employeeId, organizationId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Revision History");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                    "Kind", "Effective From", "Effective To",
                    "Revision Type", "Revision Reason",
                    "Summary", "Created By"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (SalaryRevisionHistoryItemDto item : history) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(item.getKind());
                row.createCell(col++).setCellValue(item.getEffectiveFrom() != null ? item.getEffectiveFrom().toString() : "");
                row.createCell(col++).setCellValue(item.getEffectiveTo() != null ? item.getEffectiveTo().toString() : "");
                row.createCell(col++).setCellValue(item.getRevisionType() != null ? item.getRevisionType() : "");
                row.createCell(col++).setCellValue(item.getRevisionReason() != null ? item.getRevisionReason() : "");
                row.createCell(col++).setCellValue(item.getSummary() != null ? item.getSummary() : "");
                row.createCell(col++).setCellValue(item.getCreatedBy() != null ? item.getCreatedBy() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate revision history Excel", e);
        }
    }

    public byte[] exportEmployeeRevisionHistoryToPdf(UUID employeeId, UUID organizationId) {
        List<SalaryRevisionHistoryItemDto> history = salaryService.getRevisionHistory(employeeId, organizationId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            String[] headers = {
                    "Kind", "Effective From", "Effective To",
                    "Revision Type", "Revision Reason",
                    "Summary", "Created By"
            };

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            for (String h : headers) {
                table.addCell(headerCell(h, pdfHeaderFont));
            }

            for (SalaryRevisionHistoryItemDto item : history) {
                table.addCell(cell(item.getKind()));
                table.addCell(cell(item.getEffectiveFrom() != null ? item.getEffectiveFrom().toString() : ""));
                table.addCell(cell(item.getEffectiveTo() != null ? item.getEffectiveTo().toString() : ""));
                table.addCell(cell(item.getRevisionType() != null ? item.getRevisionType() : ""));
                table.addCell(cell(item.getRevisionReason() != null ? item.getRevisionReason() : ""));
                table.addCell(cell(item.getSummary() != null ? item.getSummary() : ""));
                table.addCell(cell(item.getCreatedBy() != null ? item.getCreatedBy() : ""));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate revision history PDF", e);
        }
    }

    // ---------- RPT-05: Grade-wise headcount and cost export ----------

    /** RPT-05: Export grade-wise headcount and cost to Excel. */
    public byte[] exportGradeHeadcountCostToExcel(UUID organizationId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<GradeHeadcountDto> rows = salaryService.getGradeWiseHeadcountAndCost(organizationId, date);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Grade Headcount & Cost");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Level", "Structure Code", "Structure Name", "Grade Code", "Grade Name", "Band Code", "Band Name", "Headcount", "Total Cost", "Currency" };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            for (GradeHeadcountDto r : rows) {
                Row row = sheet.createRow(rowNum++);
                String level = r.getBandId() != null ? "Band" : (r.getGradeId() != null ? "Grade" : "Structure");
                row.createCell(0).setCellValue(level);
                row.createCell(1).setCellValue(r.getStructureCode() != null ? r.getStructureCode() : "");
                row.createCell(2).setCellValue(r.getStructureName() != null ? r.getStructureName() : "");
                row.createCell(3).setCellValue(r.getGradeCode() != null ? r.getGradeCode() : "");
                row.createCell(4).setCellValue(r.getGradeName() != null ? r.getGradeName() : "");
                row.createCell(5).setCellValue(r.getBandCode() != null ? r.getBandCode() : "");
                row.createCell(6).setCellValue(r.getBandName() != null ? r.getBandName() : "");
                row.createCell(7).setCellValue(r.getHeadcount());
                if (r.getTotalCost() != null) {
                    row.createCell(8).setCellValue(r.getTotalCost().doubleValue());
                } else {
                    row.createCell(8).setCellValue("");
                }
                row.createCell(9).setCellValue(r.getCurrency() != null ? r.getCurrency() : "");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate grade headcount/cost Excel", e);
        }
    }

    /** RPT-05: Export grade-wise headcount and cost to PDF. */
    public byte[] exportGradeHeadcountCostToPdf(UUID organizationId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<GradeHeadcountDto> rows = salaryService.getGradeWiseHeadcountAndCost(organizationId, date);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            String[] headers = { "Level", "Structure", "Grade", "Band", "Headcount", "Total Cost", "Currency" };
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String h : headers) {
                table.addCell(headerCell(h, pdfHeaderFont));
            }
            for (GradeHeadcountDto r : rows) {
                String level = r.getBandId() != null ? "Band" : (r.getGradeId() != null ? "Grade" : "Structure");
                table.addCell(cell(level));
                table.addCell(cell(r.getStructureCode() != null ? r.getStructureCode() : ""));
                table.addCell(cell(r.getGradeCode() != null ? r.getGradeCode() : ""));
                table.addCell(cell(r.getBandCode() != null ? r.getBandCode() : ""));
                table.addCell(cell(String.valueOf(r.getHeadcount())));
                table.addCell(cell(r.getTotalCost() != null ? r.getTotalCost().toPlainString() : ""));
                table.addCell(cell(r.getCurrency() != null ? r.getCurrency() : ""));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate grade headcount/cost PDF", e);
        }
    }

    // ---------- ES-58: Component-wise cost report export ----------

    /** ES-58: Export component-wise cost to Excel. */
    public byte[] exportComponentWiseCostToExcel(UUID organizationId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<ComponentWiseCostDto> rows = salaryService.getComponentWiseCost(organizationId, date);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Component Cost");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Component Code", "Component Name", "Type", "Category", "Total Amount", "Employee Count", "Currency" };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            for (ComponentWiseCostDto r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getComponentCode() != null ? r.getComponentCode() : "");
                row.createCell(1).setCellValue(r.getComponentName() != null ? r.getComponentName() : "");
                row.createCell(2).setCellValue(r.getComponentType() != null ? r.getComponentType() : "");
                row.createCell(3).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                if (r.getTotalAmount() != null) {
                    row.createCell(4).setCellValue(r.getTotalAmount().doubleValue());
                } else {
                    row.createCell(4).setCellValue("");
                }
                row.createCell(5).setCellValue(r.getEmployeeCount());
                row.createCell(6).setCellValue(r.getCurrency() != null ? r.getCurrency() : "");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate component cost Excel", e);
        }
    }

    /** ES-58: Export component-wise cost to PDF. */
    public byte[] exportComponentWiseCostToPdf(UUID organizationId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        List<ComponentWiseCostDto> rows = salaryService.getComponentWiseCost(organizationId, date);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            com.lowagie.text.Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            String[] headers = { "Code", "Name", "Type", "Category", "Total Amount", "Employees", "Currency" };
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String h : headers) {
                table.addCell(headerCell(h, pdfHeaderFont));
            }
            for (ComponentWiseCostDto r : rows) {
                table.addCell(cell(r.getComponentCode()));
                table.addCell(cell(r.getComponentName()));
                table.addCell(cell(r.getComponentType()));
                table.addCell(cell(r.getCategory()));
                table.addCell(cell(r.getTotalAmount() != null ? r.getTotalAmount().toPlainString() : ""));
                table.addCell(cell(String.valueOf(r.getEmployeeCount())));
                table.addCell(cell(r.getCurrency()));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate component cost PDF", e);
        }
    }
}
