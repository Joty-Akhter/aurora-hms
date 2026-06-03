package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.MedicationResponse;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for exporting medication lists to PDF and CSV formats.
 * Uses iText7 html2pdf for PDF generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationListExportService {
    
    private final MedicationListPrintService printService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    /**
     * Export medication list to PDF
     */
    public byte[] exportToPdf(UUID patientId, List<MedicationResponse> medications, String viewType) throws IOException {
        log.info("Exporting medication list to PDF: patient={}, count={}, view={}", 
            patientId, medications.size(), viewType);
        
        try {
            // Generate printable HTML
            String html = printService.generatePrintableHtml(patientId, medications, viewType);
            
            // Convert HTML to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, outputStream);
            
            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully exported PDF for patient: {} ({} bytes)", patientId, pdfBytes.length);
            
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Failed to export PDF for patient: {}", patientId, e);
            throw new IOException("Failed to export PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Export medication list to CSV
     */
    public byte[] exportToCsv(List<MedicationResponse> medications, String viewType) throws IOException {
        log.info("Exporting medication list to CSV: count={}, view={}", medications.size(), viewType);
        
        try {
            StringBuilder csv = new StringBuilder();
            
            // CSV Header
            csv.append("Medication Name,Generic Name,Dosage,Route,Frequency,Status,Start Date,End Date");
            if ("detailed".equals(viewType) || "timeline".equals(viewType)) {
                csv.append(",Indication,Provider,Instructions");
            }
            csv.append("\n");
            
            // CSV Data
            for (MedicationResponse med : medications) {
                csv.append(escapeCsv(med.getMedicationName())).append(",");
                csv.append(escapeCsv(med.getGenericName())).append(",");
                
                // Dosage
                StringBuilder dosage = new StringBuilder();
                if (med.getDosageStrength() != null) {
                    dosage.append(med.getDosageStrength());
                }
                if (med.getDosageUnit() != null) {
                    dosage.append(" ").append(med.getDosageUnit());
                }
                if (med.getDosageForm() != null) {
                    dosage.append(" ").append(med.getDosageForm());
                }
                csv.append(escapeCsv(dosage.toString())).append(",");
                
                csv.append(escapeCsv(med.getRoute() != null ? med.getRoute().name() : "")).append(",");
                csv.append(escapeCsv(med.getFrequency())).append(",");
                csv.append(escapeCsv(med.getMedicationStatus() != null ? med.getMedicationStatus().name() : "")).append(",");
                csv.append(med.getStartDate() != null ? med.getStartDate().format(DATE_FORMATTER) : "").append(",");
                csv.append(med.getEndDate() != null ? med.getEndDate().format(DATE_FORMATTER) : "");
                
                if ("detailed".equals(viewType) || "timeline".equals(viewType)) {
                    csv.append(",").append(escapeCsv(med.getIndication()));
                    csv.append(",").append(escapeCsv(med.getPrescribingProviderName()));
                    csv.append(",").append(escapeCsv(med.getInstructions()));
                }
                
                csv.append("\n");
            }
            
            byte[] csvBytes = csv.toString().getBytes("UTF-8");
            log.info("Successfully exported CSV ({} bytes)", csvBytes.length);
            
            return csvBytes;
            
        } catch (Exception e) {
            log.error("Failed to export CSV", e);
            throw new IOException("Failed to export CSV: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get PDF filename
     */
    public String getPdfFilename(UUID patientId, String viewType) {
        String view = viewType != null ? viewType.replaceAll("[^a-zA-Z0-9]", "_") : "all";
        return "Medication_List_" + patientId.toString().substring(0, 8) + "_" + view + ".pdf";
    }
    
    /**
     * Get CSV filename
     */
    public String getCsvFilename(UUID patientId, String viewType) {
        String view = viewType != null ? viewType.replaceAll("[^a-zA-Z0-9]", "_") : "all";
        return "Medication_List_" + patientId.toString().substring(0, 8) + "_" + view + ".csv";
    }
    
    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
