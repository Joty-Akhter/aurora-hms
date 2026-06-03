package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.MedicationRequest;
import com.easyops.hospital.dto.response.MedicationHistoryResponse;
import com.easyops.hospital.dto.response.MedicationResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.ClinicalNoteRepository;
import com.easyops.hospital.repository.MedicationHistoryRepository;
import com.easyops.hospital.repository.MedicationRepository;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.repository.PrescriptionMedicationRepository;
import com.easyops.hospital.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationService {
    
    private final MedicationRepository medicationRepository;
    private final MedicationHistoryRepository medicationHistoryRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedicationRepository prescriptionMedicationRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    
    // ========== Medication CRUD Operations ==========
    
    /**
     * Create a new medication entry
     */
    @Transactional
    public MedicationResponse createMedication(MedicationRequest request, UUID userId) {
        log.info("Creating medication for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));
        
        // Build medication entity
        Medication medication = Medication.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .medicationName(request.getMedicationName())
            .genericName(request.getGenericName())
            .medicationCode(request.getMedicationCode())
            .medicationCodeType(request.getMedicationCodeType())
            .ndcCode(request.getNdcCode())
            .rxnormCode(request.getRxnormCode())
            .dosageStrength(request.getDosageStrength())
            .dosageUnit(request.getDosageUnit())
            .dosageForm(request.getDosageForm())
            .quantity(request.getQuantity())
            .quantityUnit(request.getQuantityUnit())
            .route(request.getRoute())
            .frequency(request.getFrequency())
            .timing(request.getTiming())
            .instructions(request.getInstructions())
            .prescriptionId(request.getPrescriptionId())
            .prescribingProviderId(request.getPrescribingProviderId())
            .prescribingProviderName(request.getPrescribingProviderName())
            .prescribingProviderNpi(request.getPrescribingProviderNpi())
            .prescriptionDate(request.getPrescriptionDate())
            .pharmacyId(request.getPharmacyId())
            .pharmacyName(request.getPharmacyName())
            .refillsAuthorized(request.getRefillsAuthorized() != null ? request.getRefillsAuthorized() : 0)
            .refillsRemaining(request.getRefillsRemaining() != null ? request.getRefillsRemaining() : 0)
            .medicationStatus(request.getMedicationStatus() != null ? 
                request.getMedicationStatus() : Medication.MedicationStatus.ACTIVE)
            .statusDate(request.getStatusDate() != null ? request.getStatusDate() : LocalDate.now())
            .indication(request.getIndication())
            .diagnosisCode(request.getDiagnosisCode())
            .medicationSource(request.getMedicationSource())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .lastFilledDate(request.getLastFilledDate())
            .notes(request.getNotes())
            .specialInstructions(request.getSpecialInstructions())
            .createdBy(userId)
            .build();
        
        Medication savedMedication = medicationRepository.save(medication);
        log.info("Created medication: {}", savedMedication.getMedicationId());
        
        return mapToResponse(savedMedication);
    }
    
    /**
     * Create medication from prescription (automatic addition)
     */
    @Transactional
    public MedicationResponse createMedicationFromPrescription(UUID prescriptionId, UUID userId) {
        log.info("Creating medication(s) from prescription: {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findByIdWithMedications(prescriptionId)
            .orElseThrow(() -> new RuntimeException("Prescription not found: " + prescriptionId));
        
        // Only create medication if prescription is SENT or FILLED
        if (prescription.getPrescriptionStatus() != Prescription.PrescriptionStatus.SENT &&
            prescription.getPrescriptionStatus() != Prescription.PrescriptionStatus.FILLED) {
            throw new RuntimeException("Cannot create medication from prescription that is not SENT or FILLED");
        }

        List<PrescriptionMedication> lines = prescription.getMedications();
        if (lines == null || lines.isEmpty()) {
            lines = prescriptionMedicationRepository.findByPrescriptionPrescriptionIdOrderByLineNumberAsc(prescriptionId);
        }
        if (lines.isEmpty()) {
            throw new RuntimeException("Prescription has no medication lines");
        }

        List<Medication> existing = medicationRepository.findByPrescriptionId(prescriptionId);
        MedicationResponse lastCreated = null;
        for (PrescriptionMedication line : lines) {
            boolean alreadyExists = existing.stream()
                .anyMatch(m -> m.getMedicationName() != null
                    && m.getMedicationName().equalsIgnoreCase(line.getMedicationName()));
            if (alreadyExists) {
                continue;
            }
            MedicationRequest request = MedicationRequest.builder()
                .patientId(prescription.getPatient().getPatientId())
                .encounterId(prescription.getEncounterId())
                .medicationName(line.getMedicationName())
                .medicationCode(line.getMedicationCode())
                .medicationCodeType(line.getMedicationCodeType() != null ?
                    Medication.MedicationCodeType.valueOf(line.getMedicationCodeType().name()) : null)
                .dosageStrength(line.getDosageStrength())
                .dosageUnit(line.getDosageUnit())
                .dosageForm(line.getDosageForm() != null ?
                    Medication.DosageForm.valueOf(line.getDosageForm().name()) : null)
                .quantity(PrescriptionDerivedQuantity.deriveUnits(line.getFrequency(), line.getDurationDays()))
                .quantityUnit("EA")
                .route(line.getRoute() != null ?
                    Medication.Route.valueOf(line.getRoute().name()) : null)
                .frequency(line.getFrequency())
                .instructions(line.getInstructions())
                .prescriptionId(prescription.getPrescriptionId())
                .prescribingProviderId(prescription.getPrescribingProviderId())
                .prescribingProviderName(prescription.getPrescribingProviderName())
                .prescribingProviderNpi(prescription.getPrescribingProviderNpi())
                .prescriptionDate(line.getStartDate())
                .pharmacyId(prescription.getPharmacyId())
                .pharmacyName(prescription.getPharmacyName())
                .refillsAuthorized(line.getRefillsAuthorized())
                .refillsRemaining(line.getRefillsRemaining())
                .medicationStatus(Medication.MedicationStatus.ACTIVE)
                .statusDate(LocalDate.now())
                .diagnosisCode(prescription.getDiagnosisCode())
                .medicationSource(Medication.MedicationSource.PRESCRIPTION)
                .startDate(line.getStartDate())
                .endDate(line.getEndDate())
                .notes(prescription.getNotes())
                .specialInstructions(prescription.getSpecialInstructions())
                .build();
            lastCreated = createMedication(request, userId);
        }
        if (lastCreated != null) {
            return lastCreated;
        }
        if (!existing.isEmpty()) {
            return mapToResponse(existing.get(0));
        }
        throw new RuntimeException("No new medications were created from prescription");
    }
    
    /**
     * Get medication by ID
     */
    public MedicationResponse getMedicationById(UUID medicationId) {
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        return mapToResponse(medication);
    }
    
    /**
     * Get all medications for a patient
     */
    public List<MedicationResponse> getMedicationsByPatient(UUID patientId) {
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get active medications for a patient
     */
    public List<MedicationResponse> getActiveMedicationsByPatient(UUID patientId) {
        List<Medication> medications = medicationRepository.findActiveMedicationsByPatient(patientId);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get medications by status
     */
    public List<MedicationResponse> getMedicationsByPatientAndStatus(UUID patientId, Medication.MedicationStatus status) {
        List<Medication> medications = medicationRepository.findByPatientIdAndStatus(patientId, status);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a medication
     */
    @Transactional
    public MedicationResponse updateMedication(UUID medicationId, MedicationRequest request, UUID userId) {
        log.info("Updating medication: {}", medicationId);
        
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        
        // Create history record before updating
        createHistoryRecord(medication, userId);
        
        // Update fields
        if (request.getMedicationName() != null) medication.setMedicationName(request.getMedicationName());
        if (request.getGenericName() != null) medication.setGenericName(request.getGenericName());
        if (request.getMedicationCode() != null) medication.setMedicationCode(request.getMedicationCode());
        if (request.getMedicationCodeType() != null) medication.setMedicationCodeType(request.getMedicationCodeType());
        if (request.getNdcCode() != null) medication.setNdcCode(request.getNdcCode());
        if (request.getRxnormCode() != null) medication.setRxnormCode(request.getRxnormCode());
        if (request.getDosageStrength() != null) medication.setDosageStrength(request.getDosageStrength());
        if (request.getDosageUnit() != null) medication.setDosageUnit(request.getDosageUnit());
        if (request.getDosageForm() != null) medication.setDosageForm(request.getDosageForm());
        if (request.getQuantity() != null) medication.setQuantity(request.getQuantity());
        if (request.getQuantityUnit() != null) medication.setQuantityUnit(request.getQuantityUnit());
        if (request.getRoute() != null) medication.setRoute(request.getRoute());
        if (request.getFrequency() != null) medication.setFrequency(request.getFrequency());
        if (request.getTiming() != null) medication.setTiming(request.getTiming());
        if (request.getInstructions() != null) medication.setInstructions(request.getInstructions());
        if (request.getIndication() != null) medication.setIndication(request.getIndication());
        if (request.getDiagnosisCode() != null) medication.setDiagnosisCode(request.getDiagnosisCode());
        if (request.getStartDate() != null) medication.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) medication.setEndDate(request.getEndDate());
        if (request.getNotes() != null) medication.setNotes(request.getNotes());
        if (request.getSpecialInstructions() != null) medication.setSpecialInstructions(request.getSpecialInstructions());
        
        medication.setUpdatedBy(userId);
        
        Medication updatedMedication = medicationRepository.save(medication);
        log.info("Updated medication: {}", updatedMedication.getMedicationId());
        
        return mapToResponse(updatedMedication);
    }
    
    /**
     * Update medication status
     */
    @Transactional
    public MedicationResponse updateMedicationStatus(UUID medicationId, Medication.MedicationStatus status, 
                                                     String reason, UUID userId) {
        log.info("Updating medication status: {} to {}", medicationId, status);
        
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        
        // Create history record before status change
        createHistoryRecord(medication, userId);
        
        Medication.MedicationStatus oldStatus = medication.getMedicationStatus();
        medication.setMedicationStatus(status);
        medication.setStatusDate(LocalDate.now());
        medication.setStatusChangedBy(userId);
        medication.setUpdatedBy(userId);
        
        // If discontinuing, set end date
        if (status == Medication.MedicationStatus.DISCONTINUED) {
            medication.setEndDate(LocalDate.now());
            if (reason != null && !reason.isEmpty()) {
                medication.setNotes((medication.getNotes() != null ? medication.getNotes() + "\n" : "") + 
                    "Discontinued: " + reason);
            }
        }
        
        Medication updatedMedication = medicationRepository.save(medication);
        
        // Create history record for status change
        MedicationHistory history = MedicationHistory.builder()
            .medication(updatedMedication)
            .patient(updatedMedication.getPatient())
            .medicationName(updatedMedication.getMedicationName())
            .genericName(updatedMedication.getGenericName())
            .medicationCode(updatedMedication.getMedicationCode())
            .medicationCodeType(updatedMedication.getMedicationCodeType())
            .dosageStrength(updatedMedication.getDosageStrength())
            .dosageUnit(updatedMedication.getDosageUnit())
            .dosageForm(updatedMedication.getDosageForm())
            .route(updatedMedication.getRoute())
            .frequency(updatedMedication.getFrequency())
            .instructions(updatedMedication.getInstructions())
            .startDate(updatedMedication.getStartDate())
            .endDate(updatedMedication.getEndDate())
            .medicationStatus(status)
            .statusDate(LocalDate.now())
            .discontinuationReason(status == Medication.MedicationStatus.DISCONTINUED ? reason : null)
            .medicationSource(updatedMedication.getMedicationSource())
            .prescriptionId(updatedMedication.getPrescriptionId())
            .prescribingProviderName(updatedMedication.getPrescribingProviderName())
            .indication(updatedMedication.getIndication())
            .diagnosisCode(updatedMedication.getDiagnosisCode())
            .notes(reason != null ? reason : updatedMedication.getNotes())
            .createdBy(userId)
            .build();
        
        medicationHistoryRepository.save(history);
        
        log.info("Updated medication status: {} from {} to {}", medicationId, oldStatus, status);
        return mapToResponse(updatedMedication);
    }
    
    /**
     * Delete a medication (soft delete by setting status to DISCONTINUED)
     */
    @Transactional
    public void deleteMedication(UUID medicationId, String reason, UUID userId) {
        log.info("Deleting medication: {}", medicationId);
        updateMedicationStatus(medicationId, Medication.MedicationStatus.DISCONTINUED, 
            reason != null ? reason : "Deleted by user", userId);
    }
    
    // ========== Medication History Operations ==========
    
    /**
     * Get medication history for a patient
     */
    public List<MedicationHistoryResponse> getMedicationHistoryByPatient(UUID patientId) {
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get medication history for a specific medication
     */
    public List<MedicationHistoryResponse> getMedicationHistoryByMedication(UUID medicationId) {
        List<MedicationHistory> history = medicationHistoryRepository.findByMedicationMedicationIdOrderByStatusDateDesc(medicationId);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get complete medication history from first prescription to current (chronological)
     */
    public List<MedicationHistoryResponse> getCompleteMedicationHistory(UUID patientId) {
        List<MedicationHistory> history = medicationHistoryRepository.findCompleteHistoryByPatientOrderByStartDateAsc(patientId);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search medication history by medication name
     */
    public List<MedicationHistoryResponse> searchMedicationHistoryByName(UUID patientId, String searchTerm) {
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientIdAndMedicationNameContaining(patientId, searchTerm);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search medication history by generic name
     */
    public List<MedicationHistoryResponse> searchMedicationHistoryByGenericName(UUID patientId, String searchTerm) {
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientIdAndGenericNameContaining(patientId, searchTerm);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search medication history by date range
     */
    public List<MedicationHistoryResponse> searchMedicationHistoryByDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientIdAndDateRange(patientId, startDate, endDate);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search medication history by status and date range
     */
    public List<MedicationHistoryResponse> searchMedicationHistoryByStatusAndDateRange(
            UUID patientId, Medication.MedicationStatus status, LocalDate startDate, LocalDate endDate) {
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientIdAndStatusAndDateRange(patientId, status, startDate, endDate);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get discontinued medications with discontinuation reason
     */
    public List<MedicationHistoryResponse> getDiscontinuedMedicationsWithReason(UUID patientId) {
        List<MedicationHistory> history = medicationHistoryRepository.findDiscontinuedMedicationsWithReason(patientId);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Comprehensive search for medication history
     */
    public List<MedicationHistoryResponse> searchMedicationHistory(
            UUID patientId, String medicationName, String genericName, 
            Medication.MedicationStatus status, LocalDate startDate, LocalDate endDate) {
        
        List<MedicationHistory> results;
        
        // If all search criteria are provided, use the most specific query
        if (status != null && startDate != null && endDate != null) {
            results = medicationHistoryRepository.findByPatientIdAndStatusAndDateRange(patientId, status, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            results = medicationHistoryRepository.findByPatientIdAndDateRange(patientId, startDate, endDate);
        } else if (status != null) {
            results = medicationHistoryRepository.findByPatientIdAndStatus(patientId, status);
        } else {
            // Default to all history
            results = medicationHistoryRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        }
        
        // Apply name filters in memory if provided
        if (medicationName != null && !medicationName.trim().isEmpty()) {
            String lowerName = medicationName.toLowerCase();
            results = results.stream()
                .filter(mh -> mh.getMedicationName() != null && 
                             mh.getMedicationName().toLowerCase().contains(lowerName))
                .collect(Collectors.toList());
        }
        
        if (genericName != null && !genericName.trim().isEmpty()) {
            String lowerGeneric = genericName.toLowerCase();
            results = results.stream()
                .filter(mh -> mh.getGenericName() != null && 
                             mh.getGenericName().toLowerCase().contains(lowerGeneric))
                .collect(Collectors.toList());
        }
        
        return results.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Reactivate a historical medication
     */
    @Transactional
    public MedicationResponse reactivateHistoricalMedication(UUID historyId, UUID userId) {
        log.info("Reactivating historical medication: {}", historyId);
        
        MedicationHistory history = medicationHistoryRepository.findById(historyId)
            .orElseThrow(() -> new RuntimeException("Medication history not found: " + historyId));
        
        MedicationRequest request = MedicationRequest.builder()
            .patientId(history.getPatient().getPatientId())
            .medicationName(history.getMedicationName())
            .genericName(history.getGenericName())
            .medicationCode(history.getMedicationCode())
            .medicationCodeType(history.getMedicationCodeType())
            .dosageStrength(history.getDosageStrength())
            .dosageUnit(history.getDosageUnit())
            .dosageForm(history.getDosageForm())
            .route(history.getRoute())
            .frequency(history.getFrequency())
            .instructions(history.getInstructions())
            .prescriptionId(history.getPrescriptionId())
            .prescribingProviderName(history.getPrescribingProviderName())
            .medicationStatus(Medication.MedicationStatus.ACTIVE)
            .statusDate(LocalDate.now())
            .indication(history.getIndication())
            .diagnosisCode(history.getDiagnosisCode())
            .medicationSource(history.getMedicationSource())
            .startDate(LocalDate.now())
            .notes("Reactivated from medication history")
            .build();
        
        return createMedication(request, userId);
    }
    
    /**
     * Create medication from clinical documentation (clinical note)
     * This extracts medication information from the plan or treatment plan section of a clinical note
     */
    @Transactional
    public MedicationResponse createMedicationFromClinicalNote(UUID noteId, MedicationRequest baseRequest, UUID userId) {
        log.info("Creating medication from clinical note: {}", noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Use base request but override with note context
        MedicationRequest request = MedicationRequest.builder()
            .patientId(note.getPatient().getPatientId())
            .encounterId(note.getEncounterId())
            .medicationName(baseRequest.getMedicationName())
            .genericName(baseRequest.getGenericName())
            .medicationCode(baseRequest.getMedicationCode())
            .medicationCodeType(baseRequest.getMedicationCodeType())
            .ndcCode(baseRequest.getNdcCode())
            .rxnormCode(baseRequest.getRxnormCode())
            .dosageStrength(baseRequest.getDosageStrength())
            .dosageUnit(baseRequest.getDosageUnit())
            .dosageForm(baseRequest.getDosageForm())
            .quantity(baseRequest.getQuantity())
            .quantityUnit(baseRequest.getQuantityUnit())
            .route(baseRequest.getRoute())
            .frequency(baseRequest.getFrequency())
            .timing(baseRequest.getTiming())
            .instructions(baseRequest.getInstructions() != null ? baseRequest.getInstructions() : 
                (note.getTreatmentPlan() != null ? note.getTreatmentPlan() : note.getPlan()))
            .prescribingProviderId(note.getAuthoringProviderId())
            .prescribingProviderName(note.getAuthoringProviderName())
            .medicationStatus(Medication.MedicationStatus.ACTIVE)
            .statusDate(LocalDate.now())
            .indication(baseRequest.getIndication() != null ? baseRequest.getIndication() : note.getClinicalImpression())
            .diagnosisCode(baseRequest.getDiagnosisCode())
            .medicationSource(Medication.MedicationSource.CLINICAL_DOCUMENTATION)
            .startDate(baseRequest.getStartDate() != null ? baseRequest.getStartDate() : note.getNoteDate())
            .endDate(baseRequest.getEndDate())
            .notes("Extracted from clinical note: " + note.getNoteId() + 
                (baseRequest.getNotes() != null ? "\n" + baseRequest.getNotes() : ""))
            .specialInstructions(baseRequest.getSpecialInstructions())
            .build();
        
        return createMedication(request, userId);
    }
    
    /**
     * Bulk import medications from external sources
     */
    @Transactional
    public List<MedicationResponse> importMedicationsFromExternalSource(
            UUID patientId, List<MedicationRequest> medications, UUID userId) {
        log.info("Importing {} medications for patient: {}", medications.size(), patientId);
        
        List<MedicationResponse> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < medications.size(); i++) {
            MedicationRequest request = medications.get(i);
            try {
                // Ensure patient ID matches
                request.setPatientId(patientId);
                // Set source if not set
                if (request.getMedicationSource() == null) {
                    request.setMedicationSource(Medication.MedicationSource.EXTERNAL_IMPORT);
                }
                // Set start date if not provided
                if (request.getStartDate() == null) {
                    request.setStartDate(LocalDate.now());
                }
                // Set status if not provided
                if (request.getMedicationStatus() == null) {
                    request.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
                }
                
                MedicationResponse response = createMedication(request, userId);
                imported.add(response);
            } catch (Exception e) {
                log.error("Failed to import medication {}: {}", i + 1, e.getMessage());
                errors.add(String.format("Medication %d (%s): %s", i + 1, 
                    request.getMedicationName() != null ? request.getMedicationName() : "Unknown", 
                    e.getMessage()));
            }
        }
        
        if (!errors.isEmpty()) {
            log.warn("Import completed with {} errors out of {} medications", errors.size(), medications.size());
            // Could throw an exception or return partial results - returning partial for now
        }
        
        log.info("Successfully imported {} out of {} medications", imported.size(), medications.size());
        return imported;
    }
    
    // ========== Medication List Display and Organization ==========
    
    /**
     * Get medications by indication
     */
    public List<MedicationResponse> getMedicationsByIndication(UUID patientId, String indication) {
        List<Medication> medications = medicationRepository.findByPatientIdAndIndicationContaining(patientId, indication);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get medications by date range
     */
    public List<MedicationResponse> getMedicationsByDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        List<Medication> medications = medicationRepository.findByPatientIdAndDateRange(patientId, startDate, endDate);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get medications by status and date range
     */
    public List<MedicationResponse> getMedicationsByStatusAndDateRange(
            UUID patientId, Medication.MedicationStatus status, LocalDate startDate, LocalDate endDate) {
        List<Medication> medications = medicationRepository.findByPatientIdAndStatusAndDateRange(patientId, status, startDate, endDate);
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get distinct indications for a patient
     */
    public List<String> getDistinctIndications(UUID patientId) {
        return medicationRepository.findDistinctIndicationsByPatientId(patientId);
    }
    
    /**
     * Get medication list summary view (simplified information)
     */
    public List<MedicationResponse> getMedicationListSummary(UUID patientId, 
            Medication.MedicationStatus status, String indication, LocalDate startDate, LocalDate endDate) {
        List<Medication> medications;
        
        if (status != null && startDate != null && endDate != null) {
            medications = medicationRepository.findByPatientIdAndStatusAndDateRange(patientId, status, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            medications = medicationRepository.findByPatientIdAndDateRange(patientId, startDate, endDate);
        } else if (status != null) {
            medications = medicationRepository.findByPatientIdAndStatus(patientId, status);
        } else if (indication != null && !indication.trim().isEmpty()) {
            medications = medicationRepository.findByPatientIdAndIndicationContaining(patientId, indication);
        } else {
            medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        }
        
        return medications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get medication list detailed view (all information)
     */
    public List<MedicationResponse> getMedicationListDetailed(UUID patientId,
            Medication.MedicationStatus status, String indication, LocalDate startDate, LocalDate endDate) {
        // Same as summary but with all fields - could be enhanced with additional details
        return getMedicationListSummary(patientId, status, indication, startDate, endDate);
    }
    
    /**
     * Get medication list timeline view (chronological with status changes)
     */
    public List<MedicationResponse> getMedicationListTimeline(UUID patientId,
            Medication.MedicationStatus status, String indication, LocalDate startDate, LocalDate endDate) {
        // Get current medications
        List<MedicationResponse> currentMedications = getMedicationListSummary(patientId, status, indication, startDate, endDate);
        
        // Get historical medications
        List<MedicationHistoryResponse> history = getMedicationHistoryByPatient(patientId);
        
        // Combine and sort chronologically by start date
        List<MedicationResponse> timeline = new ArrayList<>(currentMedications);
        
        // Convert history to medication responses for timeline view
        for (MedicationHistoryResponse h : history) {
            MedicationResponse timelineItem = MedicationResponse.builder()
                .medicationId(h.getMedicationId())
                .patientId(h.getPatientId())
                .medicationName(h.getMedicationName())
                .genericName(h.getGenericName())
                .medicationCode(h.getMedicationCode())
                .medicationCodeType(h.getMedicationCodeType())
                .dosageStrength(h.getDosageStrength())
                .dosageUnit(h.getDosageUnit())
                .dosageForm(h.getDosageForm())
                .route(h.getRoute())
                .frequency(h.getFrequency())
                .instructions(h.getInstructions())
                .indication(h.getIndication())
                .diagnosisCode(h.getDiagnosisCode())
                .medicationStatus(h.getMedicationStatus())
                .statusDate(h.getStatusDate())
                .startDate(h.getStartDate())
                .endDate(h.getEndDate())
                .prescribingProviderName(h.getPrescribingProviderName())
                .medicationSource(h.getMedicationSource())
                .notes(h.getNotes())
                .build();
            timeline.add(timelineItem);
        }
        
        // Sort by start date descending (most recent first)
        timeline.sort((a, b) -> {
            if (a.getStartDate() == null && b.getStartDate() == null) return 0;
            if (a.getStartDate() == null) return 1;
            if (b.getStartDate() == null) return -1;
            return b.getStartDate().compareTo(a.getStartDate());
        });
        
        return timeline;
    }
    
    // ========== Helper Methods ==========
    
    private void createHistoryRecord(Medication medication, UUID userId) {
        MedicationHistory history = MedicationHistory.builder()
            .medication(medication)
            .patient(medication.getPatient())
            .medicationName(medication.getMedicationName())
            .genericName(medication.getGenericName())
            .medicationCode(medication.getMedicationCode())
            .medicationCodeType(medication.getMedicationCodeType())
            .dosageStrength(medication.getDosageStrength())
            .dosageUnit(medication.getDosageUnit())
            .dosageForm(medication.getDosageForm())
            .route(medication.getRoute())
            .frequency(medication.getFrequency())
            .instructions(medication.getInstructions())
            .startDate(medication.getStartDate())
            .endDate(medication.getEndDate())
            .medicationStatus(medication.getMedicationStatus())
            .statusDate(medication.getStatusDate() != null ? medication.getStatusDate() : LocalDate.now())
            .medicationSource(medication.getMedicationSource())
            .prescriptionId(medication.getPrescriptionId())
            .prescribingProviderName(medication.getPrescribingProviderName())
            .indication(medication.getIndication())
            .diagnosisCode(medication.getDiagnosisCode())
            .notes(medication.getNotes())
            .createdBy(userId)
            .build();
        
        medicationHistoryRepository.save(history);
    }
    
    private MedicationResponse mapToResponse(Medication medication) {
        return MedicationResponse.builder()
            .medicationId(medication.getMedicationId())
            .patientId(medication.getPatient().getPatientId())
            .encounterId(medication.getEncounterId())
            .medicationName(medication.getMedicationName())
            .genericName(medication.getGenericName())
            .medicationCode(medication.getMedicationCode())
            .medicationCodeType(medication.getMedicationCodeType())
            .ndcCode(medication.getNdcCode())
            .rxnormCode(medication.getRxnormCode())
            .dosageStrength(medication.getDosageStrength())
            .dosageUnit(medication.getDosageUnit())
            .dosageForm(medication.getDosageForm())
            .quantity(medication.getQuantity())
            .quantityUnit(medication.getQuantityUnit())
            .route(medication.getRoute())
            .frequency(medication.getFrequency())
            .timing(medication.getTiming())
            .instructions(medication.getInstructions())
            .prescriptionId(medication.getPrescriptionId())
            .prescribingProviderId(medication.getPrescribingProviderId())
            .prescribingProviderName(medication.getPrescribingProviderName())
            .prescribingProviderNpi(medication.getPrescribingProviderNpi())
            .prescriptionDate(medication.getPrescriptionDate())
            .pharmacyId(medication.getPharmacyId())
            .pharmacyName(medication.getPharmacyName())
            .refillsAuthorized(medication.getRefillsAuthorized())
            .refillsRemaining(medication.getRefillsRemaining())
            .medicationStatus(medication.getMedicationStatus())
            .statusDate(medication.getStatusDate())
            .statusChangedBy(medication.getStatusChangedBy())
            .indication(medication.getIndication())
            .diagnosisCode(medication.getDiagnosisCode())
            .medicationSource(medication.getMedicationSource())
            .startDate(medication.getStartDate())
            .endDate(medication.getEndDate())
            .lastFilledDate(medication.getLastFilledDate())
            .notes(medication.getNotes())
            .specialInstructions(medication.getSpecialInstructions())
            .createdAt(medication.getCreatedAt())
            .updatedAt(medication.getUpdatedAt())
            .createdBy(medication.getCreatedBy())
            .updatedBy(medication.getUpdatedBy())
            .build();
    }
    
    private MedicationHistoryResponse mapHistoryToResponse(MedicationHistory history) {
        return MedicationHistoryResponse.builder()
            .historyId(history.getHistoryId())
            .medicationId(history.getMedication().getMedicationId())
            .patientId(history.getPatient().getPatientId())
            .medicationName(history.getMedicationName())
            .genericName(history.getGenericName())
            .medicationCode(history.getMedicationCode())
            .medicationCodeType(history.getMedicationCodeType())
            .dosageStrength(history.getDosageStrength())
            .dosageUnit(history.getDosageUnit())
            .dosageForm(history.getDosageForm())
            .route(history.getRoute())
            .frequency(history.getFrequency())
            .instructions(history.getInstructions())
            .startDate(history.getStartDate())
            .endDate(history.getEndDate())
            .medicationStatus(history.getMedicationStatus())
            .statusDate(history.getStatusDate())
            .discontinuationReason(history.getDiscontinuationReason())
            .medicationSource(history.getMedicationSource())
            .prescriptionId(history.getPrescriptionId())
            .prescribingProviderName(history.getPrescribingProviderName())
            .indication(history.getIndication())
            .diagnosisCode(history.getDiagnosisCode())
            .notes(history.getNotes())
            .createdAt(history.getCreatedAt())
            .createdBy(history.getCreatedBy())
            .build();
    }
}
