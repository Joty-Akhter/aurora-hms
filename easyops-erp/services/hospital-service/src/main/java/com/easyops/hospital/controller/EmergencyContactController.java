package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.EmergencyContactRequest;
import com.easyops.hospital.dto.response.EmergencyContactResponse;
import com.easyops.hospital.entity.PatientEmergencyContact;
import com.easyops.hospital.repository.PatientEmergencyContactRepository;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.util.EmergencyContactRequestValidation;
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
@RequestMapping("/api/patients/{patientId}/emergency-contacts")
@RequiredArgsConstructor
@Tag(name = "Emergency Contact Management", description = "APIs for managing patient emergency contacts")
public class EmergencyContactController {
    
    private final PatientEmergencyContactRepository repository;
    private final PatientRepository patientRepository;
    
    @GetMapping
    @Operation(summary = "Get all emergency contacts for a patient")
    public ResponseEntity<List<EmergencyContactResponse>> getEmergencyContacts(@PathVariable UUID patientId) {
        List<PatientEmergencyContact> contacts = repository.findByPatientPatientId(patientId);
        List<EmergencyContactResponse> responses = contacts.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Add emergency contact to patient")
    public ResponseEntity<EmergencyContactResponse> addEmergencyContact(
            @PathVariable UUID patientId,
            @Valid @RequestBody EmergencyContactRequest request) {
        EmergencyContactRequestValidation.assertValid(request);
        PatientEmergencyContact contact = mapToEntity(patientId, request);
        contact = repository.save(contact);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(contact));
    }
    
    @PutMapping("/{contactId}")
    @Operation(summary = "Update emergency contact")
    public ResponseEntity<EmergencyContactResponse> updateEmergencyContact(
            @PathVariable UUID patientId,
            @PathVariable UUID contactId,
            @Valid @RequestBody EmergencyContactRequest request) {
        PatientEmergencyContact contact = repository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Emergency contact not found"));
        EmergencyContactRequestValidation.assertValid(request);
        updateEntity(contact, request);
        contact = repository.save(contact);
        return ResponseEntity.ok(mapToResponse(contact));
    }
    
    @DeleteMapping("/{contactId}")
    @Operation(summary = "Delete emergency contact")
    public ResponseEntity<Void> deleteEmergencyContact(@PathVariable UUID contactId) {
        repository.deleteById(contactId);
        return ResponseEntity.noContent().build();
    }
    
    private PatientEmergencyContact mapToEntity(UUID patientId, EmergencyContactRequest request) {
        return PatientEmergencyContact.builder()
            .patient(patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found")))
            .contactName(request.getContactName())
            .relationship(request.getRelationship())
            .primaryPhone(request.getPrimaryPhone())
            .secondaryPhone(request.getSecondaryPhone())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .zip(request.getZip())
            .country(request.getCountry())
            .email(request.getEmail())
            .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
            .build();
    }
    
    private void updateEntity(PatientEmergencyContact contact, EmergencyContactRequest request) {
        contact.setContactName(request.getContactName());
        contact.setRelationship(request.getRelationship());
        contact.setPrimaryPhone(request.getPrimaryPhone());
        contact.setSecondaryPhone(request.getSecondaryPhone());
        contact.setAddressLine1(request.getAddressLine1());
        contact.setAddressLine2(request.getAddressLine2());
        contact.setCity(request.getCity());
        contact.setState(request.getState());
        contact.setZip(request.getZip());
        contact.setCountry(request.getCountry());
        contact.setEmail(request.getEmail());
        if (request.getIsPrimary() != null) {
            contact.setIsPrimary(request.getIsPrimary());
        }
    }
    
    private EmergencyContactResponse mapToResponse(PatientEmergencyContact contact) {
        return EmergencyContactResponse.builder()
            .contactId(contact.getContactId())
            .patientId(contact.getPatient().getPatientId())
            .contactName(contact.getContactName())
            .relationship(contact.getRelationship())
            .primaryPhone(contact.getPrimaryPhone())
            .secondaryPhone(contact.getSecondaryPhone())
            .addressLine1(contact.getAddressLine1())
            .addressLine2(contact.getAddressLine2())
            .city(contact.getCity())
            .state(contact.getState())
            .zip(contact.getZip())
            .country(contact.getCountry())
            .email(contact.getEmail())
            .isPrimary(contact.getIsPrimary())
            .createdAt(contact.getCreatedAt())
            .updatedAt(contact.getUpdatedAt())
            .build();
    }
}
