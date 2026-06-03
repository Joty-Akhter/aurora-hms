package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.InsuranceRequest;
import com.easyops.hospital.dto.response.InsuranceResponse;
import com.easyops.hospital.entity.PatientInsurance;
import com.easyops.hospital.repository.PatientInsuranceRepository;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.util.InsuranceRequestValidation;
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
@RequestMapping("/api/patients/{patientId}/insurance")
@RequiredArgsConstructor
@Tag(name = "Insurance Management", description = "APIs for managing patient insurance information")
public class InsuranceController {
    
    private final PatientInsuranceRepository repository;
    private final PatientRepository patientRepository;
    
    @GetMapping
    @Operation(summary = "Get all insurance records for a patient")
    public ResponseEntity<List<InsuranceResponse>> getInsuranceRecords(@PathVariable UUID patientId) {
        List<PatientInsurance> insuranceList = repository.findByPatientPatientId(patientId);
        List<InsuranceResponse> responses = insuranceList.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Add insurance record to patient")
    public ResponseEntity<InsuranceResponse> addInsurance(
            @PathVariable UUID patientId,
            @Valid @RequestBody InsuranceRequest request) {
        InsuranceRequestValidation.assertEffectiveBeforeExpiration(request);
        PatientInsurance insurance = mapToEntity(patientId, request);
        insurance = repository.save(insurance);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(insurance));
    }
    
    @PutMapping("/{insuranceId}")
    @Operation(summary = "Update insurance record")
    public ResponseEntity<InsuranceResponse> updateInsurance(
            @PathVariable UUID patientId,
            @PathVariable UUID insuranceId,
            @Valid @RequestBody InsuranceRequest request) {
        PatientInsurance insurance = repository.findById(insuranceId)
                .orElseThrow(() -> new RuntimeException("Insurance record not found"));
        InsuranceRequestValidation.assertEffectiveBeforeExpiration(request);
        updateEntity(insurance, request);
        insurance = repository.save(insurance);
        return ResponseEntity.ok(mapToResponse(insurance));
    }
    
    @DeleteMapping("/{insuranceId}")
    @Operation(summary = "Delete insurance record")
    public ResponseEntity<Void> deleteInsurance(@PathVariable UUID insuranceId) {
        repository.deleteById(insuranceId);
        return ResponseEntity.noContent().build();
    }
    
    private PatientInsurance mapToEntity(UUID patientId, InsuranceRequest request) {
        return PatientInsurance.builder()
            .patient(patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found")))
            .insuranceType(request.getInsuranceType())
            .insuranceCompanyName(request.getInsuranceCompanyName())
            .policyNumber(request.getPolicyNumber())
            .groupNumber(request.getGroupNumber())
            .subscriberName(request.getSubscriberName())
            .subscriberDob(request.getSubscriberDob())
            .subscriberRelationship(request.getSubscriberRelationship())
            .effectiveDate(request.getEffectiveDate())
            .expirationDate(request.getExpirationDate())
            .copayAmount(request.getCopayAmount())
            .verificationStatus(request.getVerificationStatus() != null ? 
                request.getVerificationStatus() : PatientInsurance.VerificationStatus.Not_Verified)
            .verifiedDate(request.getVerifiedDate())
            .insurancePhone(request.getInsurancePhone())
            .build();
    }
    
    private void updateEntity(PatientInsurance insurance, InsuranceRequest request) {
        insurance.setInsuranceType(request.getInsuranceType());
        insurance.setInsuranceCompanyName(request.getInsuranceCompanyName());
        insurance.setPolicyNumber(request.getPolicyNumber());
        insurance.setGroupNumber(request.getGroupNumber());
        insurance.setSubscriberName(request.getSubscriberName());
        insurance.setSubscriberDob(request.getSubscriberDob());
        insurance.setSubscriberRelationship(request.getSubscriberRelationship());
        insurance.setEffectiveDate(request.getEffectiveDate());
        insurance.setExpirationDate(request.getExpirationDate());
        insurance.setCopayAmount(request.getCopayAmount());
        if (request.getVerificationStatus() != null) {
            insurance.setVerificationStatus(request.getVerificationStatus());
        }
        insurance.setVerifiedDate(request.getVerifiedDate());
        insurance.setInsurancePhone(request.getInsurancePhone());
    }
    
    private InsuranceResponse mapToResponse(PatientInsurance insurance) {
        return InsuranceResponse.builder()
            .insuranceId(insurance.getInsuranceId())
            .patientId(insurance.getPatient().getPatientId())
            .insuranceType(insurance.getInsuranceType())
            .insuranceCompanyName(insurance.getInsuranceCompanyName())
            .policyNumber(insurance.getPolicyNumber())
            .groupNumber(insurance.getGroupNumber())
            .subscriberName(insurance.getSubscriberName())
            .subscriberDob(insurance.getSubscriberDob())
            .subscriberRelationship(insurance.getSubscriberRelationship())
            .effectiveDate(insurance.getEffectiveDate())
            .expirationDate(insurance.getExpirationDate())
            .copayAmount(insurance.getCopayAmount())
            .verificationStatus(insurance.getVerificationStatus())
            .verifiedDate(insurance.getVerifiedDate())
            .insurancePhone(insurance.getInsurancePhone())
            .createdAt(insurance.getCreatedAt())
            .updatedAt(insurance.getUpdatedAt())
            .build();
    }
}
