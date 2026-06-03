package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryService {
    
    private final PatientRepository patientRepository;
    private final PatientMedicalHistoryRepository medicalHistoryRepository;
    private final FamilyHistoryRepository familyHistoryRepository;
    private final SocialHistoryRepository socialHistoryRepository;
    private final ImmunizationRepository immunizationRepository;
    private final AllergyRepository allergyRepository;
    private final DomainEventPublisher domainEventPublisher;
    
    // ========== Medical History Operations ==========
    
    @Transactional
    public MedicalHistoryResponse createMedicalHistory(UUID patientId, MedicalHistoryRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));
        
        PatientMedicalHistory history = PatientMedicalHistory.builder()
            .patient(patient)
            .historyType(request.getHistoryType())
            .conditionName(request.getConditionName() != null ? request.getConditionName().trim() : null)
            .icd10Code(normalizeCode(request.getIcd10Code(), 20, "ICD-10"))
            .icd11Code(normalizeCode(request.getIcd11Code(), 20, "ICD-11"))
            .snomedCode(normalizeCode(request.getSnomedCode(), 50, "SNOMED"))
            .onsetDate(request.getOnsetDate())
            .resolutionDate(request.getResolutionDate())
            .status(request.getStatus() != null ? request.getStatus() : PatientMedicalHistory.Status.ACTIVE)
            .severity(normalizeBlank(request.getSeverity()))
            .notes(normalizeBlank(request.getNotes()))
            .documentedBy(userId)
            .documentedDate(request.getDocumentedDate() != null ? request.getDocumentedDate() : java.time.LocalDate.now())
            .createdBy(userId)
            .build();
        
        try {
            history = medicalHistoryRepository.save(history);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid medical history data. Please review required fields and code formats.",
                ex
            );
        }
        return mapMedicalHistoryToResponse(history);
    }
    
    public List<MedicalHistoryResponse> getMedicalHistoryByPatient(UUID patientId) {
        List<PatientMedicalHistory> histories = medicalHistoryRepository.findByPatientPatientId(patientId);
        return histories.stream()
            .map(this::mapMedicalHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    public List<MedicalHistoryResponse> getPastMedicalHistoryByPatient(UUID patientId) {
        List<PatientMedicalHistory> histories = medicalHistoryRepository.findPastMedicalHistoryByPatient(patientId);
        return histories.stream()
            .map(this::mapMedicalHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public MedicalHistoryResponse updateMedicalHistory(UUID historyId, MedicalHistoryRequest request, UUID userId) {
        PatientMedicalHistory history = medicalHistoryRepository.findById(historyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical history not found: " + historyId));
        
        history.setHistoryType(request.getHistoryType());
        history.setConditionName(request.getConditionName() != null ? request.getConditionName().trim() : null);
        history.setIcd10Code(normalizeCode(request.getIcd10Code(), 20, "ICD-10"));
        history.setIcd11Code(normalizeCode(request.getIcd11Code(), 20, "ICD-11"));
        history.setSnomedCode(normalizeCode(request.getSnomedCode(), 50, "SNOMED"));
        history.setOnsetDate(request.getOnsetDate());
        history.setResolutionDate(request.getResolutionDate());
        if (request.getStatus() != null) {
            history.setStatus(request.getStatus());
        }
        history.setSeverity(normalizeBlank(request.getSeverity()));
        history.setNotes(normalizeBlank(request.getNotes()));
        history.setUpdatedBy(userId);
        
        try {
            history = medicalHistoryRepository.save(history);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid medical history data. Please review required fields and code formats.",
                ex
            );
        }
        return mapMedicalHistoryToResponse(history);
    }
    
    @Transactional
    public void deleteMedicalHistory(UUID historyId) {
        medicalHistoryRepository.deleteById(historyId);
    }
    
    // ========== Family History Operations ==========
    
    @Transactional
    public FamilyHistoryResponse createFamilyHistory(UUID patientId, FamilyHistoryRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));
        
        FamilyHistory familyHistory = FamilyHistory.builder()
            .patient(patient)
            .familyMemberRelationship(request.getFamilyMemberRelationship())
            .conditionName(request.getConditionName() != null ? request.getConditionName().trim() : null)
            .icd10Code(normalizeCode(request.getIcd10Code(), 20, "ICD-10"))
            .icd11Code(normalizeCode(request.getIcd11Code(), 20, "ICD-11"))
            .snomedCode(normalizeCode(request.getSnomedCode(), 50, "SNOMED"))
            .ageAtOnset(request.getAgeAtOnset())
            .ageAtDeath(request.getAgeAtDeath())
            .notes(normalizeBlank(request.getNotes()))
            .documentedDate(request.getDocumentedDate() != null ? request.getDocumentedDate() : java.time.LocalDate.now())
            .documentedBy(userId)
            .createdBy(userId)
            .build();
        
        try {
            familyHistory = familyHistoryRepository.save(familyHistory);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid family history data. Please review required fields and code formats.",
                ex
            );
        }
        return mapFamilyHistoryToResponse(familyHistory);
    }
    
    public List<FamilyHistoryResponse> getFamilyHistoryByPatient(UUID patientId) {
        List<FamilyHistory> histories = familyHistoryRepository.findByPatientPatientId(patientId);
        return histories.stream()
            .map(this::mapFamilyHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public FamilyHistoryResponse updateFamilyHistory(UUID familyHistoryId, FamilyHistoryRequest request, UUID userId) {
        FamilyHistory familyHistory = familyHistoryRepository.findById(familyHistoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family history not found: " + familyHistoryId));
        
        familyHistory.setFamilyMemberRelationship(request.getFamilyMemberRelationship());
        familyHistory.setConditionName(request.getConditionName() != null ? request.getConditionName().trim() : null);
        familyHistory.setIcd10Code(normalizeCode(request.getIcd10Code(), 20, "ICD-10"));
        familyHistory.setIcd11Code(normalizeCode(request.getIcd11Code(), 20, "ICD-11"));
        familyHistory.setSnomedCode(normalizeCode(request.getSnomedCode(), 50, "SNOMED"));
        familyHistory.setAgeAtOnset(request.getAgeAtOnset());
        familyHistory.setAgeAtDeath(request.getAgeAtDeath());
        familyHistory.setNotes(normalizeBlank(request.getNotes()));
        familyHistory.setUpdatedBy(userId);
        
        try {
            familyHistory = familyHistoryRepository.save(familyHistory);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid family history data. Please review required fields and code formats.",
                ex
            );
        }
        return mapFamilyHistoryToResponse(familyHistory);
    }
    
    @Transactional
    public void deleteFamilyHistory(UUID familyHistoryId) {
        familyHistoryRepository.deleteById(familyHistoryId);
    }
    
    // ========== Social History Operations ==========
    
    @Transactional
    public SocialHistoryResponse createSocialHistory(UUID patientId, SocialHistoryRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        SocialHistory socialHistory = SocialHistory.builder()
            .patient(patient)
            .category(request.getCategory())
            .status(request.getStatus())
            .frequency(request.getFrequency())
            .quantity(request.getQuantity())
            .durationYears(request.getDurationYears())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .notes(request.getNotes())
            .documentedDate(request.getDocumentedDate() != null ? request.getDocumentedDate() : java.time.LocalDate.now())
            .documentedBy(userId)
            .createdBy(userId)
            .build();
        
        socialHistory = socialHistoryRepository.save(socialHistory);
        return mapSocialHistoryToResponse(socialHistory);
    }
    
    public List<SocialHistoryResponse> getSocialHistoryByPatient(UUID patientId) {
        List<SocialHistory> histories = socialHistoryRepository.findByPatientPatientId(patientId);
        return histories.stream()
            .map(this::mapSocialHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public SocialHistoryResponse updateSocialHistory(UUID socialHistoryId, SocialHistoryRequest request, UUID userId) {
        SocialHistory socialHistory = socialHistoryRepository.findById(socialHistoryId)
            .orElseThrow(() -> new RuntimeException("Social history not found"));
        
        socialHistory.setCategory(request.getCategory());
        socialHistory.setStatus(request.getStatus());
        socialHistory.setFrequency(request.getFrequency());
        socialHistory.setQuantity(request.getQuantity());
        socialHistory.setDurationYears(request.getDurationYears());
        socialHistory.setStartDate(request.getStartDate());
        socialHistory.setEndDate(request.getEndDate());
        socialHistory.setNotes(request.getNotes());
        socialHistory.setUpdatedBy(userId);
        
        socialHistory = socialHistoryRepository.save(socialHistory);
        return mapSocialHistoryToResponse(socialHistory);
    }
    
    @Transactional
    public void deleteSocialHistory(UUID socialHistoryId) {
        socialHistoryRepository.deleteById(socialHistoryId);
    }
    
    // ========== Immunization Operations ==========
    
    @Transactional
    public ImmunizationResponse createImmunization(UUID patientId, ImmunizationRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        Immunization immunization = Immunization.builder()
            .patient(patient)
            .vaccineName(request.getVaccineName())
            .cvxCode(request.getCvxCode())
            .administrationDate(request.getAdministrationDate())
            .lotNumber(request.getLotNumber())
            .manufacturer(request.getManufacturer())
            .route(request.getRoute())
            .site(request.getSite())
            .dose(request.getDose())
            .administeredBy(request.getAdministeredBy())
            .administeredLocationId(request.getAdministeredLocationId())
            .reaction(request.getReaction())
            .notes(request.getNotes())
            .createdBy(userId)
            .build();
        
        immunization = immunizationRepository.save(immunization);
        return mapImmunizationToResponse(immunization);
    }
    
    public List<ImmunizationResponse> getImmunizationsByPatient(UUID patientId) {
        List<Immunization> immunizations = immunizationRepository.findByPatientPatientIdOrderByAdministrationDateDesc(patientId);
        return immunizations.stream()
            .map(this::mapImmunizationToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ImmunizationResponse updateImmunization(UUID immunizationId, ImmunizationRequest request, UUID userId) {
        Immunization immunization = immunizationRepository.findById(immunizationId)
            .orElseThrow(() -> new RuntimeException("Immunization not found"));
        
        immunization.setVaccineName(request.getVaccineName());
        immunization.setCvxCode(request.getCvxCode());
        immunization.setAdministrationDate(request.getAdministrationDate());
        immunization.setLotNumber(request.getLotNumber());
        immunization.setManufacturer(request.getManufacturer());
        immunization.setRoute(request.getRoute());
        immunization.setSite(request.getSite());
        immunization.setDose(request.getDose());
        immunization.setAdministeredBy(request.getAdministeredBy());
        immunization.setAdministeredLocationId(request.getAdministeredLocationId());
        immunization.setReaction(request.getReaction());
        immunization.setNotes(request.getNotes());
        immunization.setUpdatedBy(userId);
        
        immunization = immunizationRepository.save(immunization);
        return mapImmunizationToResponse(immunization);
    }
    
    @Transactional
    public void deleteImmunization(UUID immunizationId) {
        immunizationRepository.deleteById(immunizationId);
    }
    
    // ========== Allergy Operations ==========
    
    @Transactional
    public AllergyResponse createAllergy(UUID patientId, AllergyRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        // Check for duplicate allergy
        allergyRepository.findByPatientPatientIdAndAllergenNameIgnoreCase(patientId, request.getAllergenName())
            .ifPresent(existing -> {
                throw new RuntimeException("Allergy already exists for this patient: " + request.getAllergenName());
            });
        
        Allergy allergy = Allergy.builder()
            .patient(patient)
            .allergenName(request.getAllergenName())
            .allergenType(request.getAllergenType())
            .allergenCode(request.getAllergenCode())
            .reactionType(request.getReactionType())
            .severity(request.getSeverity())
            .onsetDate(request.getOnsetDate())
            .status(request.getStatus() != null ? request.getStatus() : Allergy.Status.ACTIVE)
            .verificationStatus(request.getVerificationStatus() != null ? 
                request.getVerificationStatus() : Allergy.VerificationStatus.UNCONFIRMED)
            .documentedBy(userId)
            .documentedDate(request.getDocumentedDate() != null ? request.getDocumentedDate() : java.time.LocalDate.now())
            .notes(request.getNotes())
            .createdBy(userId)
            .build();
        
        allergy = allergyRepository.save(allergy);
        AllergyResponse response = mapAllergyToResponse(allergy);

        domainEventPublisher.publish("allergy.added", java.util.Map.of(
            "allergyId", response.getAllergyId(),
            "patientId", response.getPatientId(),
            "allergenName", response.getAllergenName(),
            "severity", response.getSeverity(),
            "status", response.getStatus()
        ));

        return response;
    }
    
    public List<AllergyResponse> getAllergiesByPatient(UUID patientId) {
        List<Allergy> allergies = allergyRepository.findByPatientPatientId(patientId);
        return allergies.stream()
            .map(this::mapAllergyToResponse)
            .collect(Collectors.toList());
    }
    
    public List<AllergyResponse> getActiveAllergiesByPatient(UUID patientId) {
        List<Allergy> allergies = allergyRepository.findActiveAllergiesByPatient(patientId);
        return allergies.stream()
            .map(this::mapAllergyToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public AllergyResponse updateAllergy(UUID allergyId, AllergyRequest request, UUID userId) {
        Allergy allergy = allergyRepository.findById(allergyId)
            .orElseThrow(() -> new RuntimeException("Allergy not found"));
        
        allergy.setAllergenName(request.getAllergenName());
        allergy.setAllergenType(request.getAllergenType());
        allergy.setAllergenCode(request.getAllergenCode());
        allergy.setReactionType(request.getReactionType());
        allergy.setSeverity(request.getSeverity());
        allergy.setOnsetDate(request.getOnsetDate());
        if (request.getStatus() != null) {
            allergy.setStatus(request.getStatus());
        }
        if (request.getVerificationStatus() != null) {
            allergy.setVerificationStatus(request.getVerificationStatus());
        }
        allergy.setNotes(request.getNotes());
        allergy.setUpdatedBy(userId);
        
        allergy = allergyRepository.save(allergy);
        AllergyResponse response = mapAllergyToResponse(allergy);

        domainEventPublisher.publish("allergy.updated", java.util.Map.of(
            "allergyId", response.getAllergyId(),
            "patientId", response.getPatientId(),
            "allergenName", response.getAllergenName(),
            "severity", response.getSeverity(),
            "status", response.getStatus()
        ));

        return response;
    }
    
    @Transactional
    public void deleteAllergy(UUID allergyId) {
        Allergy allergy = allergyRepository.findById(allergyId)
            .orElseThrow(() -> new RuntimeException("Allergy not found"));

        allergyRepository.deleteById(allergyId);

        domainEventPublisher.publish("allergy.updated", java.util.Map.of(
            "allergyId", allergy.getAllergyId(),
            "patientId", allergy.getPatient().getPatientId(),
            "allergenName", allergy.getAllergenName(),
            "severity", allergy.getSeverity(),
            "status", "DELETED"
        ));
    }
    
    // ========== Mapping Methods ==========
    
    private MedicalHistoryResponse mapMedicalHistoryToResponse(PatientMedicalHistory history) {
        return MedicalHistoryResponse.builder()
            .historyId(history.getHistoryId())
            .patientId(history.getPatient().getPatientId())
            .historyType(history.getHistoryType())
            .conditionName(history.getConditionName())
            .icd10Code(history.getIcd10Code())
            .icd11Code(history.getIcd11Code())
            .snomedCode(history.getSnomedCode())
            .onsetDate(history.getOnsetDate())
            .resolutionDate(history.getResolutionDate())
            .status(history.getStatus())
            .severity(history.getSeverity())
            .notes(history.getNotes())
            .documentedBy(history.getDocumentedBy())
            .documentedDate(history.getDocumentedDate())
            .createdAt(history.getCreatedAt())
            .updatedAt(history.getUpdatedAt())
            .build();
    }

    private static String normalizeBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeCode(String value, int maxLength, String fieldName) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(" - ");
        if (separator > 0) {
            normalized = normalized.substring(0, separator).trim();
        }
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " code must be at most " + maxLength + " characters"
            );
        }
        return normalized;
    }
    
    private FamilyHistoryResponse mapFamilyHistoryToResponse(FamilyHistory familyHistory) {
        return FamilyHistoryResponse.builder()
            .familyHistoryId(familyHistory.getFamilyHistoryId())
            .patientId(familyHistory.getPatient().getPatientId())
            .familyMemberRelationship(familyHistory.getFamilyMemberRelationship())
            .conditionName(familyHistory.getConditionName())
            .icd10Code(familyHistory.getIcd10Code())
            .icd11Code(familyHistory.getIcd11Code())
            .snomedCode(familyHistory.getSnomedCode())
            .ageAtOnset(familyHistory.getAgeAtOnset())
            .ageAtDeath(familyHistory.getAgeAtDeath())
            .notes(familyHistory.getNotes())
            .documentedDate(familyHistory.getDocumentedDate())
            .documentedBy(familyHistory.getDocumentedBy())
            .createdAt(familyHistory.getCreatedAt())
            .updatedAt(familyHistory.getUpdatedAt())
            .build();
    }
    
    private SocialHistoryResponse mapSocialHistoryToResponse(SocialHistory socialHistory) {
        return SocialHistoryResponse.builder()
            .socialHistoryId(socialHistory.getSocialHistoryId())
            .patientId(socialHistory.getPatient().getPatientId())
            .category(socialHistory.getCategory())
            .status(socialHistory.getStatus())
            .frequency(socialHistory.getFrequency())
            .quantity(socialHistory.getQuantity())
            .durationYears(socialHistory.getDurationYears())
            .startDate(socialHistory.getStartDate())
            .endDate(socialHistory.getEndDate())
            .notes(socialHistory.getNotes())
            .documentedDate(socialHistory.getDocumentedDate())
            .documentedBy(socialHistory.getDocumentedBy())
            .createdAt(socialHistory.getCreatedAt())
            .updatedAt(socialHistory.getUpdatedAt())
            .build();
    }
    
    private ImmunizationResponse mapImmunizationToResponse(Immunization immunization) {
        return ImmunizationResponse.builder()
            .immunizationId(immunization.getImmunizationId())
            .patientId(immunization.getPatient().getPatientId())
            .vaccineName(immunization.getVaccineName())
            .cvxCode(immunization.getCvxCode())
            .administrationDate(immunization.getAdministrationDate())
            .lotNumber(immunization.getLotNumber())
            .manufacturer(immunization.getManufacturer())
            .route(immunization.getRoute())
            .site(immunization.getSite())
            .dose(immunization.getDose())
            .administeredBy(immunization.getAdministeredBy())
            .administeredLocationId(immunization.getAdministeredLocationId())
            .reaction(immunization.getReaction())
            .notes(immunization.getNotes())
            .createdAt(immunization.getCreatedAt())
            .updatedAt(immunization.getUpdatedAt())
            .build();
    }
    
    private AllergyResponse mapAllergyToResponse(Allergy allergy) {
        return AllergyResponse.builder()
            .allergyId(allergy.getAllergyId())
            .patientId(allergy.getPatient().getPatientId())
            .allergenName(allergy.getAllergenName())
            .allergenType(allergy.getAllergenType())
            .allergenCode(allergy.getAllergenCode())
            .reactionType(allergy.getReactionType())
            .severity(allergy.getSeverity())
            .onsetDate(allergy.getOnsetDate())
            .status(allergy.getStatus())
            .verificationStatus(allergy.getVerificationStatus())
            .documentedBy(allergy.getDocumentedBy())
            .documentedDate(allergy.getDocumentedDate())
            .notes(allergy.getNotes())
            .createdAt(allergy.getCreatedAt())
            .updatedAt(allergy.getUpdatedAt())
            .build();
    }
}
