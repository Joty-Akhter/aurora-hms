package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ImagingStudyLinkRequest;
import com.easyops.hospital.dto.response.ImagingStudyLinkResponse;
import com.easyops.hospital.service.ImagingStudyIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for imaging study integration with other clinical data
 */
@RestController
@RequestMapping("/api/imaging-studies/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imaging Study Integration", description = "APIs for linking imaging studies to encounters, problems, notes, and medications")
@CrossOrigin(origins = "*")
public class ImagingStudyIntegrationController {
    
    private final ImagingStudyIntegrationService integrationService;
    
    // ========== Clinical Notes Integration ==========
    
    @PostMapping("/clinical-notes")
    @Operation(summary = "Link study to clinical note", description = "Link an imaging study to a clinical note")
    public ResponseEntity<ImagingStudyLinkResponse> linkToClinicalNote(
            @Valid @RequestBody ImagingStudyLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking study {} to clinical note {}", request.getStudyId(), request.getTargetId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyLinkResponse response = integrationService.linkToClinicalNote(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/clinical-notes/{linkId}")
    @Operation(summary = "Unlink study from clinical note", description = "Remove the link between an imaging study and clinical note")
    public ResponseEntity<Void> unlinkFromClinicalNote(@PathVariable UUID linkId) {
        log.info("Unlinking study from clinical note: {}", linkId);
        integrationService.unlinkFromClinicalNote(linkId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/studies/{studyId}/clinical-notes")
    @Operation(summary = "Get clinical notes for study", description = "Get all clinical notes linked to an imaging study")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getClinicalNotesByStudy(@PathVariable UUID studyId) {
        List<ImagingStudyLinkResponse> links = integrationService.getClinicalNotesByStudy(studyId);
        return ResponseEntity.ok(links);
    }
    
    @GetMapping("/clinical-notes/{noteId}/studies")
    @Operation(summary = "Get studies for clinical note", description = "Get all imaging studies linked to a clinical note")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getStudiesByClinicalNote(@PathVariable UUID noteId) {
        List<ImagingStudyLinkResponse> links = integrationService.getStudiesByClinicalNote(noteId);
        return ResponseEntity.ok(links);
    }
    
    // ========== Problems Integration ==========
    
    @PostMapping("/problems")
    @Operation(summary = "Link study to problem", description = "Link an imaging study to a problem/diagnosis")
    public ResponseEntity<ImagingStudyLinkResponse> linkToProblem(
            @Valid @RequestBody ImagingStudyLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking study {} to problem {}", request.getStudyId(), request.getTargetId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyLinkResponse response = integrationService.linkToProblem(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/problems/{linkId}")
    @Operation(summary = "Unlink study from problem", description = "Remove the link between an imaging study and problem")
    public ResponseEntity<Void> unlinkFromProblem(@PathVariable UUID linkId) {
        log.info("Unlinking study from problem: {}", linkId);
        integrationService.unlinkFromProblem(linkId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/studies/{studyId}/problems")
    @Operation(summary = "Get problems for study", description = "Get all problems linked to an imaging study")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getProblemsByStudy(@PathVariable UUID studyId) {
        List<ImagingStudyLinkResponse> links = integrationService.getProblemsByStudy(studyId);
        return ResponseEntity.ok(links);
    }
    
    @GetMapping("/problems/{problemId}/studies")
    @Operation(summary = "Get studies for problem", description = "Get all imaging studies linked to a problem")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getStudiesByProblem(@PathVariable UUID problemId) {
        List<ImagingStudyLinkResponse> links = integrationService.getStudiesByProblem(problemId);
        return ResponseEntity.ok(links);
    }
    
    // ========== Medications Integration ==========
    
    @PostMapping("/medications")
    @Operation(summary = "Link study to medication", description = "Link an imaging study to a medication (e.g., contrast agent)")
    public ResponseEntity<ImagingStudyLinkResponse> linkToMedication(
            @Valid @RequestBody ImagingStudyLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking study {} to medication {}", request.getStudyId(), request.getTargetId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingStudyLinkResponse response = integrationService.linkToMedication(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/medications/{linkId}")
    @Operation(summary = "Unlink study from medication", description = "Remove the link between an imaging study and medication")
    public ResponseEntity<Void> unlinkFromMedication(@PathVariable UUID linkId) {
        log.info("Unlinking study from medication: {}", linkId);
        integrationService.unlinkFromMedication(linkId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/studies/{studyId}/medications")
    @Operation(summary = "Get medications for study", description = "Get all medications linked to an imaging study")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getMedicationsByStudy(@PathVariable UUID studyId) {
        List<ImagingStudyLinkResponse> links = integrationService.getMedicationsByStudy(studyId);
        return ResponseEntity.ok(links);
    }
    
    @GetMapping("/medications/{prescriptionId}/studies")
    @Operation(summary = "Get studies for medication", description = "Get all imaging studies linked to a medication")
    public ResponseEntity<List<ImagingStudyLinkResponse>> getStudiesByMedication(@PathVariable UUID prescriptionId) {
        List<ImagingStudyLinkResponse> links = integrationService.getStudiesByMedication(prescriptionId);
        return ResponseEntity.ok(links);
    }
    
    // ========== Encounter Integration ==========
    
    @PutMapping("/studies/{studyId}/encounter")
    @Operation(summary = "Link study to encounter", description = "Link an imaging study to an encounter/visit")
    public ResponseEntity<Void> linkToEncounter(
            @PathVariable UUID studyId,
            @RequestParam UUID encounterId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking study {} to encounter {}", studyId, encounterId);
        if (userId == null) userId = UUID.randomUUID();
        integrationService.linkToEncounter(studyId, encounterId, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/encounters/{encounterId}/studies")
    @Operation(summary = "Get studies for encounter", description = "Get all imaging studies linked to an encounter")
    public ResponseEntity<List<com.easyops.hospital.entity.ImagingStudy>> getStudiesByEncounter(@PathVariable UUID encounterId) {
        List<com.easyops.hospital.entity.ImagingStudy> studies = integrationService.getStudiesByEncounter(encounterId);
        return ResponseEntity.ok(studies);
    }
}
