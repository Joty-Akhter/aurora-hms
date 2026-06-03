package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.MedicationRequest;
import com.easyops.hospital.dto.response.MedicationHistoryResponse;
import com.easyops.hospital.dto.response.MedicationResponse;
import com.easyops.hospital.entity.Medication;
import com.easyops.hospital.entity.MedicationHistory;
import com.easyops.hospital.service.MedicationListExportService;
import com.easyops.hospital.service.MedicationListPrintService;
import com.easyops.hospital.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
@Slf4j
public class MedicationController {
    
    private final MedicationService medicationService;
    private final MedicationListPrintService printService;
    private final MedicationListExportService exportService;
    
    /**
     * Create a new medication
     */
    @PostMapping
    public ResponseEntity<MedicationResponse> createMedication(
            @Valid @RequestBody MedicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating medication for patient: {}", request.getPatientId());
        MedicationResponse response = medicationService.createMedication(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Create medication from prescription
     */
    @PostMapping("/from-prescription/{prescriptionId}")
    public ResponseEntity<MedicationResponse> createMedicationFromPrescription(
            @PathVariable UUID prescriptionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating medication from prescription: {}", prescriptionId);
        MedicationResponse response = medicationService.createMedicationFromPrescription(prescriptionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get medication by ID
     */
    @GetMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse> getMedicationById(@PathVariable UUID medicationId) {
        MedicationResponse response = medicationService.getMedicationById(medicationId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all medications for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByPatient(@PathVariable UUID patientId) {
        List<MedicationResponse> medications = medicationService.getMedicationsByPatient(patientId);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Get active medications for a patient
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<MedicationResponse>> getActiveMedicationsByPatient(@PathVariable UUID patientId) {
        List<MedicationResponse> medications = medicationService.getActiveMedicationsByPatient(patientId);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Get medications by status
     */
    @GetMapping("/patient/{patientId}/status/{status}")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByStatus(
            @PathVariable UUID patientId,
            @PathVariable Medication.MedicationStatus status) {
        List<MedicationResponse> medications = medicationService.getMedicationsByPatientAndStatus(patientId, status);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Update a medication
     */
    @PutMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @PathVariable UUID medicationId,
            @Valid @RequestBody MedicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating medication: {}", medicationId);
        MedicationResponse response = medicationService.updateMedication(medicationId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update medication status
     */
    @PatchMapping("/{medicationId}/status")
    public ResponseEntity<MedicationResponse> updateMedicationStatus(
            @PathVariable UUID medicationId,
            @RequestParam Medication.MedicationStatus status,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating medication status: {} to {}", medicationId, status);
        MedicationResponse response = medicationService.updateMedicationStatus(medicationId, status, reason, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a medication (soft delete)
     */
    @DeleteMapping("/{medicationId}")
    public ResponseEntity<Void> deleteMedication(
            @PathVariable UUID medicationId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Deleting medication: {}", medicationId);
        medicationService.deleteMedication(medicationId, reason, userId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Medication History Endpoints ==========
    
    /**
     * Get medication history for a patient
     */
    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<MedicationHistoryResponse>> getMedicationHistoryByPatient(@PathVariable UUID patientId) {
        List<MedicationHistoryResponse> history = medicationService.getMedicationHistoryByPatient(patientId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get medication history for a specific medication
     */
    @GetMapping("/{medicationId}/history")
    public ResponseEntity<List<MedicationHistoryResponse>> getMedicationHistoryByMedication(@PathVariable UUID medicationId) {
        List<MedicationHistoryResponse> history = medicationService.getMedicationHistoryByMedication(medicationId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Reactivate a historical medication
     */
    @PostMapping("/history/{historyId}/reactivate")
    public ResponseEntity<MedicationResponse> reactivateHistoricalMedication(
            @PathVariable UUID historyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Reactivating historical medication: {}", historyId);
        MedicationResponse response = medicationService.reactivateHistoricalMedication(historyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get complete medication history from first prescription to current (chronological)
     */
    @GetMapping("/patient/{patientId}/history/complete")
    public ResponseEntity<List<MedicationHistoryResponse>> getCompleteMedicationHistory(@PathVariable UUID patientId) {
        List<MedicationHistoryResponse> history = medicationService.getCompleteMedicationHistory(patientId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Search medication history by medication name
     */
    @GetMapping("/patient/{patientId}/history/search")
    public ResponseEntity<List<MedicationHistoryResponse>> searchMedicationHistory(
            @PathVariable UUID patientId,
            @RequestParam(required = false) String medicationName,
            @RequestParam(required = false) String genericName,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        List<MedicationHistoryResponse> history = medicationService.searchMedicationHistory(
            patientId, medicationName, genericName, status, startDate, endDate);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get discontinued medications with discontinuation reason
     */
    @GetMapping("/patient/{patientId}/history/discontinued")
    public ResponseEntity<List<MedicationHistoryResponse>> getDiscontinuedMedicationsWithReason(@PathVariable UUID patientId) {
        List<MedicationHistoryResponse> history = medicationService.getDiscontinuedMedicationsWithReason(patientId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Create medication from clinical documentation
     */
    @PostMapping("/from-clinical-note/{noteId}")
    public ResponseEntity<MedicationResponse> createMedicationFromClinicalNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody MedicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating medication from clinical note: {}", noteId);
        MedicationResponse response = medicationService.createMedicationFromClinicalNote(noteId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Bulk import medications from external sources
     */
    @PostMapping("/import/{patientId}")
    public ResponseEntity<List<MedicationResponse>> importMedications(
            @PathVariable UUID patientId,
            @Valid @RequestBody List<MedicationRequest> medications,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Importing {} medications for patient: {}", medications.size(), patientId);
        List<MedicationResponse> imported = medicationService.importMedicationsFromExternalSource(patientId, medications, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(imported);
    }
    
    // ========== Medication List Display and Organization Endpoints ==========
    
    /**
     * Get medications by indication
     */
    @GetMapping("/patient/{patientId}/by-indication")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByIndication(
            @PathVariable UUID patientId,
            @RequestParam String indication) {
        List<MedicationResponse> medications = medicationService.getMedicationsByIndication(patientId, indication);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Get distinct indications for a patient
     */
    @GetMapping("/patient/{patientId}/indications")
    public ResponseEntity<List<String>> getDistinctIndications(@PathVariable UUID patientId) {
        List<String> indications = medicationService.getDistinctIndications(patientId);
        return ResponseEntity.ok(indications);
    }
    
    /**
     * Get medication list summary view
     */
    @GetMapping("/patient/{patientId}/list/summary")
    public ResponseEntity<List<MedicationResponse>> getMedicationListSummary(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MedicationResponse> medications = medicationService.getMedicationListSummary(patientId, status, indication, startDate, endDate);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Get medication list detailed view
     */
    @GetMapping("/patient/{patientId}/list/detailed")
    public ResponseEntity<List<MedicationResponse>> getMedicationListDetailed(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MedicationResponse> medications = medicationService.getMedicationListDetailed(patientId, status, indication, startDate, endDate);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Get medication list timeline view
     */
    @GetMapping("/patient/{patientId}/list/timeline")
    public ResponseEntity<List<MedicationResponse>> getMedicationListTimeline(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MedicationResponse> medications = medicationService.getMedicationListTimeline(patientId, status, indication, startDate, endDate);
        return ResponseEntity.ok(medications);
    }
    
    /**
     * Generate printable HTML for medication list
     */
    @GetMapping("/patient/{patientId}/print")
    public ResponseEntity<String> printMedicationList(
            @PathVariable UUID patientId,
            @RequestParam(required = false, defaultValue = "summary") String viewType,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Generating printable medication list for patient: {}, view: {}", patientId, viewType);
        
        List<MedicationResponse> medications = medicationService.getMedicationListSummary(patientId, status, indication, startDate, endDate);
        String html = printService.generatePrintableHtml(patientId, medications, viewType);
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }
    
    /**
     * Export medication list to PDF
     */
    @GetMapping("/patient/{patientId}/export/pdf")
    public ResponseEntity<ByteArrayResource> exportMedicationListToPdf(
            @PathVariable UUID patientId,
            @RequestParam(required = false, defaultValue = "summary") String viewType,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Exporting medication list to PDF for patient: {}, view: {}", patientId, viewType);
        
        try {
            List<MedicationResponse> medications = medicationService.getMedicationListSummary(patientId, status, indication, startDate, endDate);
            byte[] pdfBytes = exportService.exportToPdf(patientId, medications, viewType);
            String filename = exportService.getPdfFilename(patientId, viewType);
            
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
                
        } catch (IOException e) {
            log.error("Failed to export PDF for patient: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export medication list to CSV
     */
    @GetMapping("/patient/{patientId}/export/csv")
    public ResponseEntity<ByteArrayResource> exportMedicationListToCsv(
            @PathVariable UUID patientId,
            @RequestParam(required = false, defaultValue = "summary") String viewType,
            @RequestParam(required = false) Medication.MedicationStatus status,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Exporting medication list to CSV for patient: {}, view: {}", patientId, viewType);
        
        try {
            List<MedicationResponse> medications = medicationService.getMedicationListSummary(patientId, status, indication, startDate, endDate);
            byte[] csvBytes = exportService.exportToCsv(medications, viewType);
            String filename = exportService.getCsvFilename(patientId, viewType);
            
            ByteArrayResource resource = new ByteArrayResource(csvBytes);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csvBytes.length)
                .body(resource);
                
        } catch (IOException e) {
            log.error("Failed to export CSV for patient: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
