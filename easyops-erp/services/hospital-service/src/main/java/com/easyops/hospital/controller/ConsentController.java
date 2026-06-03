package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ConsentRequest;
import com.easyops.hospital.dto.response.ConsentResponse;
import com.easyops.hospital.entity.PatientConsent;
import com.easyops.hospital.repository.PatientConsentRepository;
import com.easyops.hospital.repository.PatientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients/{patientId}/consents")
@RequiredArgsConstructor
@Tag(name = "Consent Management", description = "APIs for managing patient consents")
public class ConsentController {
    
    private final PatientConsentRepository repository;
    private final PatientRepository patientRepository;
    
    @GetMapping
    @Operation(summary = "Get all consents for a patient")
    public ResponseEntity<List<ConsentResponse>> getConsents(@PathVariable UUID patientId) {
        List<PatientConsent> consents = repository.findByPatientPatientId(patientId);
        List<ConsentResponse> responses = consents.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Add consent record to patient")
    public ResponseEntity<ConsentResponse> addConsent(
            @PathVariable UUID patientId,
            @Valid @RequestBody ConsentRequest request) {
        PatientConsent consent = mapToEntity(patientId, request);
        consent = repository.save(consent);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(consent));
    }
    
    @PutMapping("/{consentId}")
    @Operation(summary = "Update consent record")
    public ResponseEntity<ConsentResponse> updateConsent(
            @PathVariable UUID patientId,
            @PathVariable UUID consentId,
            @Valid @RequestBody ConsentRequest request) {
        PatientConsent consent = repository.findById(consentId)
            .orElseThrow(() -> new RuntimeException("Consent record not found"));
        updateEntity(consent, request);
        consent = repository.save(consent);
        return ResponseEntity.ok(mapToResponse(consent));
    }
    
    @DeleteMapping("/{consentId}")
    @Operation(summary = "Delete consent record")
    public ResponseEntity<Void> deleteConsent(@PathVariable UUID consentId) {
        repository.deleteById(consentId);
        return ResponseEntity.noContent().build();
    }
    
    private PatientConsent mapToEntity(UUID patientId, ConsentRequest request) {
        return PatientConsent.builder()
            .patient(patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found")))
            .consentType(request.getConsentType())
            .consentStatus(request.getConsentStatus())
            .consentDate(request.getConsentDate())
            .signature(request.getSignature())
            .expiresDate(request.getExpiresDate())
            .notes(request.getNotes())
            .build();
    }
    
    private void updateEntity(PatientConsent consent, ConsentRequest request) {
        consent.setConsentType(request.getConsentType());
        consent.setConsentStatus(request.getConsentStatus());
        consent.setConsentDate(request.getConsentDate());
        consent.setSignature(request.getSignature());
        consent.setExpiresDate(request.getExpiresDate());
        consent.setNotes(request.getNotes());
    }
    
    private ConsentResponse mapToResponse(PatientConsent consent) {
        return ConsentResponse.builder()
            .consentId(consent.getConsentId())
            .patientId(consent.getPatient().getPatientId())
            .consentType(consent.getConsentType())
            .consentStatus(consent.getConsentStatus())
            .consentDate(consent.getConsentDate())
            .signature(consent.getSignature())
            .expiresDate(consent.getExpiresDate())
            .notes(consent.getNotes())
            .createdAt(consent.getCreatedAt())
            .updatedAt(consent.getUpdatedAt())
            .build();
    }
}
