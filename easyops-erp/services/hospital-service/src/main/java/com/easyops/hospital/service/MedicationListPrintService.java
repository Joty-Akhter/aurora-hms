package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.MedicationResponse;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating printable HTML reports for medication lists.
 * Creates print-friendly HTML that can be printed directly from the browser.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationListPrintService {
    
    private final PatientRepository patientRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    /**
     * Generate printable HTML for a medication list
     */
    public String generatePrintableHtml(UUID patientId, List<MedicationResponse> medications, String viewType) {
        log.info("Generating printable HTML for medication list: patient={}, count={}, view={}", 
            patientId, medications.size(), viewType);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        StringBuilder html = new StringBuilder();
        
        // Print styles
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Medication List - ").append(getPatientName(patient)).append("</title>\n");
        html.append("<style>\n");
        html.append(getPrintStyles());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // Report header
        html.append(generateReportHeader(patient, viewType));
        
        // Patient information
        html.append(generatePatientSection(patient));
        
        // Medication list
        html.append(generateMedicationList(medications, viewType));
        
        // Footer
        html.append(generateFooter());
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    /**
     * Generate report header
     */
    private String generateReportHeader(Patient patient, String viewType) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"report-header\">\n");
        html.append("<h1>MEDICATION LIST</h1>\n");
        html.append("<div class=\"header-info\">\n");
        html.append("<div><strong>View Type:</strong> ").append(viewType != null ? viewType : "All Medications").append("</div>\n");
        html.append("<div><strong>Generated:</strong> ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))).append("</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate patient information section
     */
    private String generatePatientSection(Patient patient) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Patient Information</h2>\n");
        html.append("<table class=\"info-table\">\n");
        html.append("<tr><td><strong>Patient Name:</strong></td><td>").append(getPatientName(patient)).append("</td></tr>\n");
        if (patient.getMrn() != null) {
            html.append("<tr><td><strong>MRN:</strong></td><td>").append(patient.getMrn()).append("</td></tr>\n");
        }
        html.append("<tr><td><strong>Patient ID:</strong></td><td>").append(patient.getPatientId()).append("</td></tr>\n");
        if (patient.getDateOfBirth() != null) {
            html.append("<tr><td><strong>Date of Birth:</strong></td><td>").append(patient.getDateOfBirth().format(DATE_FORMATTER)).append("</td></tr>\n");
        }
        html.append("</table>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate medication list section
     */
    private String generateMedicationList(List<MedicationResponse> medications, String viewType) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Medications (").append(medications.size()).append(")</h2>\n");
        
        if (medications.isEmpty()) {
            html.append("<p class=\"no-data\">No medications found.</p>\n");
        } else {
            html.append("<table class=\"medication-table\">\n");
            html.append("<thead>\n");
            html.append("<tr>\n");
            html.append("<th>Medication Name</th>\n");
            html.append("<th>Dosage</th>\n");
            html.append("<th>Route</th>\n");
            html.append("<th>Frequency</th>\n");
            html.append("<th>Status</th>\n");
            html.append("<th>Start Date</th>\n");
            if ("detailed".equals(viewType) || "timeline".equals(viewType)) {
                html.append("<th>Indication</th>\n");
                html.append("<th>Provider</th>\n");
            }
            html.append("</tr>\n");
            html.append("</thead>\n");
            html.append("<tbody>\n");
            
            for (MedicationResponse med : medications) {
                html.append("<tr>\n");
                html.append("<td><strong>").append(escapeHtml(med.getMedicationName())).append("</strong><br/>");
                if (med.getGenericName() != null) {
                    html.append("<small>").append(escapeHtml(med.getGenericName())).append("</small>");
                }
                html.append("</td>\n");
                
                html.append("<td>");
                if (med.getDosageStrength() != null) {
                    html.append(med.getDosageStrength());
                }
                if (med.getDosageUnit() != null) {
                    html.append(" ").append(med.getDosageUnit());
                }
                if (med.getDosageForm() != null) {
                    html.append(" ").append(med.getDosageForm());
                }
                html.append("</td>\n");
                
                html.append("<td>").append(med.getRoute() != null ? med.getRoute() : "N/A").append("</td>\n");
                html.append("<td>").append(med.getFrequency() != null ? escapeHtml(med.getFrequency()) : "N/A").append("</td>\n");
                html.append("<td><span class=\"status-badge status-").append(med.getMedicationStatus() != null ? med.getMedicationStatus().name().toLowerCase() : "unknown").append("\">")
                    .append(med.getMedicationStatus() != null ? med.getMedicationStatus() : "N/A").append("</span></td>\n");
                html.append("<td>").append(med.getStartDate() != null ? med.getStartDate().format(DATE_FORMATTER) : "N/A").append("</td>\n");
                
                if ("detailed".equals(viewType) || "timeline".equals(viewType)) {
                    html.append("<td>").append(med.getIndication() != null ? escapeHtml(med.getIndication()) : "N/A").append("</td>\n");
                    html.append("<td>").append(med.getPrescribingProviderName() != null ? escapeHtml(med.getPrescribingProviderName()) : "N/A").append("</td>\n");
                }
                
                html.append("</tr>\n");
                
                // Add instructions if available in detailed view
                if (("detailed".equals(viewType) || "timeline".equals(viewType)) && med.getInstructions() != null && !med.getInstructions().trim().isEmpty()) {
                    html.append("<tr class=\"instruction-row\">\n");
                    html.append("<td colspan=\"").append("detailed".equals(viewType) || "timeline".equals(viewType) ? "8" : "6").append("\">");
                    html.append("<strong>Instructions:</strong> ").append(escapeHtml(med.getInstructions()));
                    html.append("</td>\n");
                    html.append("</tr>\n");
                }
            }
            
            html.append("</tbody>\n");
            html.append("</table>\n");
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate footer
     */
    private String generateFooter() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"footer\">\n");
        html.append("<hr>\n");
        html.append("<p class=\"footer-text\">\n");
        html.append("This is an electronic medication list. Please verify all information with your healthcare provider.\n");
        html.append("</p>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Get print styles
     */
    private String getPrintStyles() {
        return """
            @media print {
                @page {
                    margin: 0.5in;
                }
                body {
                    font-family: Arial, sans-serif;
                    font-size: 10pt;
                    line-height: 1.4;
                    color: #000;
                }
                .no-print {
                    display: none !important;
                }
            }
            body {
                font-family: Arial, sans-serif;
                font-size: 10pt;
                line-height: 1.4;
                color: #000;
                max-width: 8.5in;
                margin: 0 auto;
                padding: 20px;
            }
            .report-header {
                text-align: center;
                border-bottom: 3px solid #000;
                padding-bottom: 15px;
                margin-bottom: 20px;
            }
            .report-header h1 {
                margin: 0 0 10px 0;
                font-size: 18pt;
                font-weight: bold;
            }
            .header-info {
                display: flex;
                justify-content: space-around;
                font-size: 9pt;
            }
            .section {
                margin-bottom: 25px;
                page-break-inside: avoid;
            }
            .section h2 {
                font-size: 14pt;
                font-weight: bold;
                border-bottom: 2px solid #000;
                padding-bottom: 5px;
                margin-bottom: 10px;
            }
            .info-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
            }
            .info-table td {
                padding: 5px 10px;
                border-bottom: 1px solid #ddd;
            }
            .info-table td:first-child {
                width: 200px;
                font-weight: bold;
            }
            .medication-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 10px;
            }
            .medication-table th {
                background-color: #f5f5f5;
                border: 1px solid #000;
                padding: 8px;
                text-align: left;
                font-weight: bold;
                font-size: 9pt;
            }
            .medication-table td {
                border: 1px solid #ddd;
                padding: 8px;
                font-size: 9pt;
            }
            .medication-table tr:nth-child(even) {
                background-color: #f9f9f9;
            }
            .instruction-row {
                background-color: #f0f0f0;
            }
            .instruction-row td {
                padding: 5px 8px;
                font-size: 8pt;
                font-style: italic;
            }
            .status-badge {
                padding: 2px 6px;
                border-radius: 3px;
                font-size: 8pt;
                font-weight: bold;
            }
            .status-active {
                background-color: #4caf50;
                color: white;
            }
            .status-discontinued {
                background-color: #f44336;
                color: white;
            }
            .status-on_hold {
                background-color: #ff9800;
                color: white;
            }
            .status-completed {
                background-color: #2196f3;
                color: white;
            }
            .no-data {
                text-align: center;
                padding: 20px;
                color: #666;
                font-style: italic;
            }
            .footer {
                margin-top: 40px;
                padding-top: 20px;
                border-top: 2px solid #000;
            }
            .footer-text {
                font-size: 8pt;
                text-align: center;
                margin: 5px 0;
                color: #666;
            }
            """;
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    /**
     * Get patient name
     */
    private String getPatientName(Patient patient) {
        String n = com.easyops.hospital.util.PatientDisplayName.of(patient);
        return n.isEmpty() ? "Unknown Patient" : n;
    }
}
