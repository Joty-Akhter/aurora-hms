package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ImagingStudyRequest;
import com.easyops.hospital.dto.response.ImagingStudyResponse;
import com.easyops.hospital.dto.response.ImagingStudyTimelineResponse;
import com.easyops.hospital.dto.response.ImagingStudyTrendResponse;
import com.easyops.hospital.entity.ImagingStudy;
import com.easyops.hospital.service.ImagingReportExportService;
import com.easyops.hospital.service.ImagingReportPrintService;
import com.easyops.hospital.service.ImagingStudyService;
import com.easyops.hospital.service.ImagingStudyTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/imaging-studies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imaging Study Management", description = "APIs for imaging study results and reports management")
public class ImagingStudyController {
    
    private final ImagingStudyService imagingStudyService;
    private final ImagingReportPrintService printService;
    private final ImagingReportExportService exportService;
    private final ImagingStudyTimelineService timelineService;
    
    // ========== Imaging Study CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new imaging study result", description = "Create a new imaging study result and report")
    public ResponseEntity<ImagingStudyResponse> createImagingStudy(
            @Valid @RequestBody ImagingStudyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating imaging study for order: {}", request.getOrderId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyResponse response = imagingStudyService.createImagingStudy(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{studyId}")
    @Operation(summary = "Get imaging study by ID", description = "Retrieve an imaging study by its ID")
    public ResponseEntity<ImagingStudyResponse> getImagingStudyById(@PathVariable UUID studyId) {
        ImagingStudyResponse response = imagingStudyService.getImagingStudyById(studyId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{studyNumber}")
    @Operation(summary = "Get imaging study by study number", description = "Retrieve an imaging study by its study number")
    public ResponseEntity<ImagingStudyResponse> getImagingStudyByNumber(@PathVariable String studyNumber) {
        ImagingStudyResponse response = imagingStudyService.getImagingStudyByNumber(studyNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/accession/{accessionNumber}")
    @Operation(summary = "Get imaging study by accession number", description = "Retrieve an imaging study by its accession number")
    public ResponseEntity<ImagingStudyResponse> getImagingStudyByAccessionNumber(@PathVariable String accessionNumber) {
        ImagingStudyResponse response = imagingStudyService.getImagingStudyByAccessionNumber(accessionNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all imaging studies for a patient", description = "Retrieve all imaging studies for a patient, ordered by date")
    public ResponseEntity<List<ImagingStudyResponse>> getImagingStudiesByPatient(@PathVariable UUID patientId) {
        List<ImagingStudyResponse> responses = imagingStudyService.getImagingStudiesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get imaging studies by order", description = "Retrieve all imaging studies for a specific order")
    public ResponseEntity<List<ImagingStudyResponse>> getImagingStudiesByOrder(@PathVariable UUID orderId) {
        List<ImagingStudyResponse> responses = imagingStudyService.getImagingStudiesByOrder(orderId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/encounters/{encounterId}")
    @Operation(summary = "Get imaging studies by encounter", description = "Retrieve all imaging studies for a specific encounter/visit")
    public ResponseEntity<List<ImagingStudyResponse>> getImagingStudiesByEncounter(@PathVariable UUID encounterId) {
        List<ImagingStudyResponse> responses = imagingStudyService.getImagingStudiesByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/modality/{modality}")
    @Operation(summary = "Get imaging studies by modality", description = "Retrieve imaging studies for a patient filtered by modality")
    public ResponseEntity<List<ImagingStudyResponse>> getImagingStudiesByModality(
            @PathVariable UUID patientId,
            @PathVariable ImagingStudy.StudyModality modality) {
        List<ImagingStudyResponse> responses = imagingStudyService.getImagingStudiesByModality(patientId, modality);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/body-part/{bodyPart}")
    @Operation(summary = "Get imaging studies by body part", description = "Retrieve imaging studies for a patient filtered by body part")
    public ResponseEntity<List<ImagingStudyResponse>> getImagingStudiesByBodyPart(
            @PathVariable UUID patientId,
            @PathVariable String bodyPart) {
        List<ImagingStudyResponse> responses = imagingStudyService.getImagingStudiesByBodyPart(patientId, bodyPart);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/critical/unacknowledged")
    @Operation(summary = "Get unacknowledged critical findings", description = "Retrieve unacknowledged critical findings for a patient")
    public ResponseEntity<List<ImagingStudyResponse>> getUnacknowledgedCriticalFindings(@PathVariable UUID patientId) {
        List<ImagingStudyResponse> responses = imagingStudyService.getUnacknowledgedCriticalFindings(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/critical/unacknowledged")
    @Operation(summary = "Get all unacknowledged critical findings", description = "Retrieve all unacknowledged critical findings")
    public ResponseEntity<List<ImagingStudyResponse>> getAllUnacknowledgedCriticalFindings() {
        List<ImagingStudyResponse> responses = imagingStudyService.getAllUnacknowledgedCriticalFindings();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/unreviewed")
    @Operation(summary = "Get unreviewed studies", description = "Retrieve unreviewed imaging studies for a patient")
    public ResponseEntity<List<ImagingStudyResponse>> getUnreviewedStudies(@PathVariable UUID patientId) {
        List<ImagingStudyResponse> responses = imagingStudyService.getUnreviewedStudies(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{studyId}")
    @Operation(summary = "Update imaging study", description = "Update an imaging study (only allowed for preliminary studies)")
    public ResponseEntity<ImagingStudyResponse> updateImagingStudy(
            @PathVariable UUID studyId,
            @Valid @RequestBody ImagingStudyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating imaging study: {}", studyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyResponse response = imagingStudyService.updateImagingStudy(studyId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{studyId}/review")
    @Operation(summary = "Review imaging study", description = "Mark an imaging study as reviewed")
    public ResponseEntity<ImagingStudyResponse> reviewImagingStudy(
            @PathVariable UUID studyId,
            @RequestParam(required = false) String reviewNotes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Reviewing imaging study: {}", studyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyResponse response = imagingStudyService.reviewImagingStudy(studyId, reviewNotes, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{studyId}/acknowledge-critical")
    @Operation(summary = "Acknowledge critical finding", description = "Acknowledge a critical finding in an imaging study")
    public ResponseEntity<ImagingStudyResponse> acknowledgeCriticalFinding(
            @PathVariable UUID studyId,
            @RequestParam(required = false) String response,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Acknowledging critical finding for study: {}", studyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyResponse imagingResponse = imagingStudyService.acknowledgeCriticalFinding(studyId, response, userId);
        return ResponseEntity.ok(imagingResponse);
    }
    
    @PostMapping("/{studyId}/finalize")
    @Operation(summary = "Finalize study report", description = "Finalize an imaging study report (change from preliminary to final)")
    public ResponseEntity<ImagingStudyResponse> finalizeStudyReport(
            @PathVariable UUID studyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Finalizing study report: {}", studyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyResponse response = imagingStudyService.finalizeStudyReport(studyId, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Report Printing and Export ==========
    
    @GetMapping("/{studyId}/print")
    @Operation(summary = "Get printable report", description = "Get printable HTML version of the imaging study report")
    public ResponseEntity<String> getPrintableReport(@PathVariable UUID studyId) {
        log.info("Generating printable report for study: {}", studyId);
        
        ImagingStudyResponse study = imagingStudyService.getImagingStudyById(studyId);
        String html = printService.generatePrintableHtml(study);
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"report_" + study.getAccessionNumber() + ".html\"")
            .body(html);
    }
    
    @GetMapping("/{studyId}/export/pdf")
    @Operation(summary = "Export report to PDF", description = "Export imaging study report to PDF format")
    public ResponseEntity<byte[]> exportReportToPdf(@PathVariable UUID studyId) {
        log.info("Exporting report to PDF for study: {}", studyId);
        
        try {
            ImagingStudyResponse study = imagingStudyService.getImagingStudyById(studyId);
            byte[] pdfBytes = exportService.exportToPdf(study);
            String filename = exportService.getPdfFilename(study);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length))
                .body(pdfBytes);
                
        } catch (IOException e) {
            log.error("Failed to export PDF for study: {}", studyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== Timeline and Trends ==========
    
    @GetMapping("/patients/{patientId}/timeline")
    @Operation(summary = "Get imaging study timeline", 
               description = "Get chronological timeline of imaging studies with filtering options")
    public ResponseEntity<ImagingStudyTimelineResponse> getTimeline(
            @PathVariable UUID patientId,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String bodyPart) {
        log.info("Getting timeline for patient: {}, modality: {}, bodyPart: {}", patientId, modality, bodyPart);
        
        ImagingStudyTimelineResponse response = timelineService.getTimeline(patientId, modality, bodyPart);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}/trends")
    @Operation(summary = "Get imaging study trends", 
               description = "Get trends analysis including frequency, intervals, and patterns")
    public ResponseEntity<ImagingStudyTrendResponse> getTrends(
            @PathVariable UUID patientId,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String bodyPart) {
        log.info("Getting trends for patient: {}, modality: {}, bodyPart: {}", patientId, modality, bodyPart);
        
        ImagingStudyTrendResponse response = timelineService.getTrends(patientId, modality, bodyPart);
        return ResponseEntity.ok(response);
    }
}
