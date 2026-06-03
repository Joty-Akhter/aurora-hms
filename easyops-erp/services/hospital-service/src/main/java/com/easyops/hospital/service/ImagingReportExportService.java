package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.ImagingStudyResponse;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for exporting imaging study reports to PDF format.
 * Uses iText7 html2pdf to convert HTML reports to PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingReportExportService {
    
    private final ImagingReportPrintService printService;
    
    /**
     * Export imaging study report to PDF
     */
    public byte[] exportToPdf(ImagingStudyResponse study) throws IOException {
        log.info("Exporting imaging study report to PDF: {}", study.getStudyId());
        
        try {
            // Generate printable HTML
            String html = printService.generatePrintableHtml(study);
            
            // Convert HTML to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, outputStream);
            
            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully exported PDF for study: {} ({} bytes)", study.getStudyId(), pdfBytes.length);
            
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Failed to export PDF for study: {}", study.getStudyId(), e);
            throw new IOException("Failed to export PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get PDF filename for the study
     */
    public String getPdfFilename(ImagingStudyResponse study) {
        String accessionNumber = study.getAccessionNumber() != null 
            ? study.getAccessionNumber().replaceAll("[^a-zA-Z0-9]", "_")
            : study.getStudyId().toString();
        return "Imaging_Report_" + accessionNumber + ".pdf";
    }
}
