package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.ImagingStudyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating printable HTML reports for imaging studies.
 * Creates print-friendly HTML that can be printed directly from the browser.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingReportPrintService {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    /**
     * Generate printable HTML for an imaging study report
     */
    public String generatePrintableHtml(ImagingStudyResponse study) {
        log.info("Generating printable HTML for study: {}", study.getStudyId());
        
        StringBuilder html = new StringBuilder();
        
        // Print styles
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Imaging Study Report - ").append(study.getAccessionNumber()).append("</title>\n");
        html.append("<style>\n");
        html.append(getPrintStyles());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // Report header
        html.append(generateReportHeader(study));
        
        // Patient information
        html.append(generatePatientSection(study));
        
        // Study information
        html.append(generateStudySection(study));
        
        // Report content
        html.append(generateReportContent(study));
        
        // Radiologist information
        html.append(generateRadiologistSection(study));
        
        // Footer
        html.append(generateFooter(study));
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    /**
     * Generate report header
     */
    private String generateReportHeader(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"report-header\">\n");
        html.append("<h1>IMAGING STUDY REPORT</h1>\n");
        html.append("<div class=\"header-info\">\n");
        html.append("<div><strong>Accession Number:</strong> ").append(study.getAccessionNumber()).append("</div>\n");
        html.append("<div><strong>Study Number:</strong> ").append(study.getStudyNumber()).append("</div>\n");
        if (study.getReportDate() != null) {
            html.append("<div><strong>Report Date:</strong> ").append(study.getReportDate().format(DATE_TIME_FORMATTER)).append("</div>\n");
        }
        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate patient information section
     */
    private String generatePatientSection(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Patient Information</h2>\n");
        html.append("<table class=\"info-table\">\n");
        html.append("<tr><td><strong>Patient Name:</strong></td><td>").append(study.getPatientName()).append("</td></tr>\n");
        if (study.getMrn() != null) {
            html.append("<tr><td><strong>MRN:</strong></td><td>").append(study.getMrn()).append("</td></tr>\n");
        }
        html.append("<tr><td><strong>Patient ID:</strong></td><td>").append(study.getPatientId()).append("</td></tr>\n");
        html.append("</table>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate study information section
     */
    private String generateStudySection(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Study Information</h2>\n");
        html.append("<table class=\"info-table\">\n");
        html.append("<tr><td><strong>Study Name:</strong></td><td>").append(study.getStudyName()).append("</td></tr>\n");
        html.append("<tr><td><strong>Modality:</strong></td><td>").append(study.getStudyModality()).append("</td></tr>\n");
        html.append("<tr><td><strong>CPT Code:</strong></td><td>").append(study.getCptCode()).append("</td></tr>\n");
        html.append("<tr><td><strong>Body Part:</strong></td><td>")
            .append(study.getBodyPartExamined())
            .append(study.getLaterality() != null ? " (" + study.getLaterality() + ")" : "")
            .append("</td></tr>\n");
        html.append("<tr><td><strong>Study Date:</strong></td><td>")
            .append(study.getStudyDate().format(DATE_TIME_FORMATTER))
            .append("</td></tr>\n");
        html.append("<tr><td><strong>Completion Date:</strong></td><td>")
            .append(study.getStudyCompletionDate().format(DATE_TIME_FORMATTER))
            .append("</td></tr>\n");
        html.append("<tr><td><strong>Status:</strong></td><td>").append(study.getStudyStatus()).append("</td></tr>\n");
        
        if (study.getContrastUsed() != null && study.getContrastUsed()) {
            html.append("<tr><td><strong>Contrast:</strong></td><td>")
                .append(study.getContrastType() != null ? study.getContrastType() : "Yes")
                .append("</td></tr>\n");
        }
        
        if (study.getNumberOfImages() != null) {
            html.append("<tr><td><strong>Number of Images:</strong></td><td>").append(study.getNumberOfImages()).append("</td></tr>\n");
        }
        
        if (study.getNumberOfSeries() != null) {
            html.append("<tr><td><strong>Number of Series:</strong></td><td>").append(study.getNumberOfSeries()).append("</td></tr>\n");
        }
        
        html.append("</table>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate report content section
     */
    private String generateReportContent(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Report Content</h2>\n");
        
        if (study.getClinicalHistory() != null && !study.getClinicalHistory().isEmpty()) {
            html.append("<div class=\"content-block\">\n");
            html.append("<h3>Clinical History</h3>\n");
            html.append("<p class=\"content-text\">").append(escapeHtml(study.getClinicalHistory())).append("</p>\n");
            html.append("</div>\n");
        }
        
        if (study.getTechniqueDescription() != null && !study.getTechniqueDescription().isEmpty()) {
            html.append("<div class=\"content-block\">\n");
            html.append("<h3>Technique</h3>\n");
            html.append("<p class=\"content-text\">").append(escapeHtml(study.getTechniqueDescription())).append("</p>\n");
            html.append("</div>\n");
        }
        
        if (study.getFindings() != null && !study.getFindings().isEmpty()) {
            html.append("<div class=\"content-block\">\n");
            html.append("<h3>Findings</h3>\n");
            html.append("<div class=\"findings-box\">\n");
            html.append("<p class=\"content-text\">").append(escapeHtml(study.getFindings())).append("</p>\n");
            html.append("</div>\n");
            html.append("</div>\n");
        }
        
        if (study.getImpressionConclusion() != null && !study.getImpressionConclusion().isEmpty()) {
            html.append("<div class=\"content-block\">\n");
            html.append("<h3>Impression/Conclusion</h3>\n");
            html.append("<div class=\"impression-box\">\n");
            html.append("<p class=\"content-text impression-text\">").append(escapeHtml(study.getImpressionConclusion())).append("</p>\n");
            html.append("</div>\n");
            html.append("</div>\n");
        }
        
        if (study.getRecommendations() != null && !study.getRecommendations().isEmpty()) {
            html.append("<div class=\"content-block\">\n");
            html.append("<h3>Recommendations</h3>\n");
            html.append("<div class=\"recommendations-box\">\n");
            html.append("<p class=\"content-text\">").append(escapeHtml(study.getRecommendations())).append("</p>\n");
            html.append("</div>\n");
            html.append("</div>\n");
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate radiologist information section
     */
    private String generateRadiologistSection(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">\n");
        html.append("<h2>Radiologist Information</h2>\n");
        html.append("<table class=\"info-table\">\n");
        
        if (study.getInterpretingRadiologistName() != null) {
            html.append("<tr><td><strong>Interpreting Radiologist:</strong></td><td>")
                .append(study.getInterpretingRadiologistName())
                .append("</td></tr>\n");
            
            if (study.getInterpretingRadiologistNpi() != null) {
                html.append("<tr><td><strong>NPI:</strong></td><td>")
                    .append(study.getInterpretingRadiologistNpi())
                    .append("</td></tr>\n");
            }
            
            if (study.getInterpretingRadiologistSpecialty() != null) {
                html.append("<tr><td><strong>Specialty:</strong></td><td>")
                    .append(study.getInterpretingRadiologistSpecialty())
                    .append("</td></tr>\n");
            }
        }
        
        if (study.getPreliminaryReadingBy() != null) {
            html.append("<tr><td><strong>Preliminary Reading By:</strong></td><td>")
                .append(study.getPreliminaryReadingBy())
                .append("</td></tr>\n");
        }
        
        if (study.getReviewingRadiologist() != null) {
            html.append("<tr><td><strong>Reviewing Radiologist:</strong></td><td>")
                .append(study.getReviewingRadiologist())
                .append("</td></tr>\n");
        }
        
        if (study.getReportFinalizedDate() != null) {
            html.append("<tr><td><strong>Finalized Date:</strong></td><td>")
                .append(study.getReportFinalizedDate().format(DATE_TIME_FORMATTER))
                .append("</td></tr>\n");
        }
        
        html.append("</table>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Generate footer
     */
    private String generateFooter(ImagingStudyResponse study) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"footer\">\n");
        html.append("<hr>\n");
        html.append("<p class=\"footer-text\">\n");
        html.append("This is an electronic report. Signature on file.\n");
        html.append("</p>\n");
        if (study.getIsFinal() != null && study.getIsFinal()) {
            html.append("<p class=\"footer-text final-indicator\">\n");
            html.append("<strong>FINAL REPORT</strong>\n");
            html.append("</p>\n");
        } else if (study.getIsPreliminary() != null && study.getIsPreliminary()) {
            html.append("<p class=\"footer-text preliminary-indicator\">\n");
            html.append("<strong>PRELIMINARY REPORT</strong>\n");
            html.append("</p>\n");
        }
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
                    margin: 1in;
                }
                body {
                    font-family: Arial, sans-serif;
                    font-size: 11pt;
                    line-height: 1.4;
                    color: #000;
                }
                .no-print {
                    display: none !important;
                }
            }
            body {
                font-family: Arial, sans-serif;
                font-size: 11pt;
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
                font-size: 10pt;
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
            .section h3 {
                font-size: 12pt;
                font-weight: bold;
                margin-top: 15px;
                margin-bottom: 8px;
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
            .content-block {
                margin-bottom: 20px;
            }
            .content-text {
                white-space: pre-wrap;
                margin: 0;
                line-height: 1.6;
            }
            .findings-box {
                background-color: #f5f5f5;
                border: 1px solid #ddd;
                padding: 15px;
                margin-top: 10px;
            }
            .impression-box {
                background-color: #e3f2fd;
                border: 2px solid #2196f3;
                padding: 15px;
                margin-top: 10px;
            }
            .impression-text {
                font-weight: bold;
            }
            .recommendations-box {
                background-color: #fff3e0;
                border: 1px solid #ff9800;
                padding: 15px;
                margin-top: 10px;
            }
            .footer {
                margin-top: 40px;
                padding-top: 20px;
                border-top: 2px solid #000;
            }
            .footer-text {
                font-size: 9pt;
                text-align: center;
                margin: 5px 0;
            }
            .final-indicator {
                color: #2e7d32;
                font-weight: bold;
            }
            .preliminary-indicator {
                color: #f57c00;
                font-weight: bold;
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
}
