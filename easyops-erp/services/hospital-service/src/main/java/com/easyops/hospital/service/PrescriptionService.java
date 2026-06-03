package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.exception.UnprocessableEntityException;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {
    
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDiagnosisRepository prescriptionDiagnosisRepository;
    private final PrescriptionMedicationRepository prescriptionMedicationRepository;
    private final PrescriptionInteractionRepository prescriptionInteractionRepository;
    private final PrescriptionAllergyCheckRepository prescriptionAllergyCheckRepository;
    private final PrescriptionHistoryRepository prescriptionHistoryRepository;
    private final PrescriptionRefillRepository prescriptionRefillRepository;
    private final PrescriptionRefillRequestRepository prescriptionRefillRequestRepository;
    private final PatientRepository patientRepository;
    private final AllergyRepository allergyRepository;
    private final ComprehensiveInteractionService comprehensiveInteractionService;
    private final DrugInteractionDatabaseService drugInteractionDatabaseService;
    private final ClinicalMedicationSafetyService clinicalMedicationSafetyService;
    private final PatientProblemRepository patientProblemRepository;
    private final LabResultRepository labResultRepository;
    private final PDMPService pdmpService;
    private final EPrescribingService eprescribingService;
    private final AllergyMatchingService allergyMatchingService;
    private final DomainEventPublisher domainEventPublisher;
    private final DeaNumberValidator deaNumberValidator;
    private final NpiValidator npiValidator;
    
    // ========== Prescription CRUD Operations ==========
    
    /**
     * Create a new prescription
     */
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request, UUID userId) {
        log.info("Creating prescription for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Patient not found: " + request.getPatientId()));
        
        List<PrescriptionMedicationRequest> lineRequests = normalizeMedicationLines(request);
        if (lineRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one medication is required");
        }

        String rxNumber = request.getPrescriptionNumber();
        if (rxNumber == null || rxNumber.isBlank()) {
            rxNumber = "RX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }

        Prescription prescription = Prescription.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .epEncounterMode(request.getEpEncounterMode())
            .prescriptionNumber(rxNumber)
            .prescriptionType(request.getPrescriptionType() != null ?
                request.getPrescriptionType() : Prescription.PrescriptionType.ELECTRONIC)
            .pharmacyId(request.getPharmacyId())
            .pharmacyName(request.getPharmacyName())
            .pharmacyNpi(request.getPharmacyNpi())
            .pharmacyAddressLine1(request.getPharmacyAddressLine1())
            .pharmacyAddressLine2(request.getPharmacyAddressLine2())
            .pharmacyCity(request.getPharmacyCity())
            .pharmacyState(request.getPharmacyState())
            .pharmacyZip(request.getPharmacyZip())
            .pharmacyPhone(request.getPharmacyPhone())
            .prescribingProviderId(request.getPrescribingProviderId())
            .prescribingProviderNpi(request.getPrescribingProviderNpi())
            .prescribingProviderName(request.getPrescribingProviderName())
            .prescriptionStatus(Prescription.PrescriptionStatus.DRAFT)
            .notes(request.getNotes())
            .specialInstructions(request.getSpecialInstructions())
            .diagnosisCode(resolvePrimaryDiagnosisCode(request))
            .createdBy(userId)
            .build();

        List<PrescriptionMedication> medEntities = new ArrayList<>();
        for (int i = 0; i < lineRequests.size(); i++) {
            PrescriptionMedicationRequest lr = lineRequests.get(i);
            PrescriptionMedication m = mapLineRequestToEntity(lr);
            m.setLineNumber(i + 1);
            m.setPrescription(prescription);
            medEntities.add(m);
        }
        prescription.setMedications(medEntities);
        prescription.getDiagnoses().addAll(buildDiagnosisEntities(request, prescription));
        syncHeaderFromMedications(prescription);

        Prescription savedPrescription;
        try {
            savedPrescription = prescriptionRepository.save(prescription);
        } catch (DataIntegrityViolationException ex) {
            log.error("Prescription create failed due to invalid data for patient {}", request.getPatientId(), ex);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid prescription data. Please review medication lines, dates, and required fields.",
                ex
            );
        }
        log.info("Created prescription: {}", savedPrescription.getPrescriptionId());
        
        // Perform validation checks
        validatePrescription(savedPrescription);
        
        PrescriptionResponse response = mapToResponse(savedPrescription);

        Map<String, Object> createdEvent = new java.util.HashMap<>();
        createdEvent.put("prescriptionId", response.getPrescriptionId());
        createdEvent.put("patientId", response.getPatientId());
        createdEvent.put("encounterId", response.getEncounterId());
        createdEvent.put("prescribingProviderId", response.getPrescribingProviderId());
        createdEvent.put("medicationCode", response.getMedicationCode());
        createdEvent.put("medicationName", response.getMedicationName());
        createdEvent.put("quantity", PrescriptionDerivedQuantity.deriveUnits(
                response.getFrequency(), response.getDurationDays()));
        createdEvent.put("route", response.getRoute() != null ? response.getRoute().name() : null);
        createdEvent.put("frequency", response.getFrequency());
        createdEvent.put("status", response.getPrescriptionStatus());
        domainEventPublisher.publish("prescription.created", createdEvent);

        return response;
    }
    
    /**
     * Get prescription by ID
     */
    public PrescriptionResponse getPrescriptionById(UUID prescriptionId) {
        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);
        return mapToResponse(prescription);
    }
    
    /**
     * Get prescription by prescription number
     */
    public PrescriptionResponse getPrescriptionByNumber(String prescriptionNumber) {
        Prescription stub = prescriptionRepository.findByPrescriptionNumber(prescriptionNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + prescriptionNumber));
        Prescription prescription = fetchPrescriptionForUpdate(stub.getPrescriptionId());
        return mapToResponse(prescription);
    }
    
    /**
     * Get all prescriptions for a patient
     */
    public List<PrescriptionResponse> getPrescriptionsByPatient(UUID patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientPatientIdOrderByCreatedDateDesc(patientId);
        return prescriptions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get active prescriptions for a patient
     */
    public List<PrescriptionResponse> getActivePrescriptionsByPatient(UUID patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findActivePrescriptionsByPatient(patientId);
        return prescriptions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get draft prescriptions for a patient
     */
    public List<PrescriptionResponse> getDraftPrescriptionsByPatient(UUID patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findDraftPrescriptionsByPatient(patientId);
        return prescriptions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a prescription
     */
    @Transactional
    public PrescriptionResponse updatePrescription(UUID prescriptionId, PrescriptionRequest request, UUID userId) {
        log.info("Updating prescription: {}", prescriptionId);
        
        // Avoid Hibernate MultipleBagFetchException by fetching medications eagerly and
        // allowing diagnoses to initialize lazily within this transaction.
        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);

        // Only allow updates to DRAFT prescriptions
        if (prescription.getPrescriptionStatus() != Prescription.PrescriptionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot update prescription that is not in DRAFT status");
        }

        List<PrescriptionMedicationRequest> lineRequests = normalizeMedicationLines(request);
        if (lineRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one medication is required");
        }

        if (request.getEncounterId() != null) prescription.setEncounterId(request.getEncounterId());

        // EP-2: ep_encounter_mode is immutable after initial set at creation time.
        // Reject early if the caller tries to change an already-set value.
        if (request.getEpEncounterMode() != null
                && prescription.getEpEncounterMode() != null
                && prescription.getEpEncounterMode() != request.getEpEncounterMode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ep_encounter_mode is immutable after initial set (EP-2): "
                    + "prescription " + prescriptionId + " has ep_encounter_mode="
                    + prescription.getEpEncounterMode()
                    + ", cannot change to " + request.getEpEncounterMode());
        }
        // Allow setting the mode only if it was never set at creation (null → value).
        if (request.getEpEncounterMode() != null && prescription.getEpEncounterMode() == null) {
            prescription.setEpEncounterMode(request.getEpEncounterMode());
        }

        if (request.getPrescriptionType() != null) prescription.setPrescriptionType(request.getPrescriptionType());
        if (request.getPharmacyId() != null) prescription.setPharmacyId(request.getPharmacyId());
        if (request.getPharmacyName() != null) prescription.setPharmacyName(request.getPharmacyName());
        if (request.getPharmacyNpi() != null) prescription.setPharmacyNpi(request.getPharmacyNpi());
        if (request.getPharmacyAddressLine1() != null) prescription.setPharmacyAddressLine1(request.getPharmacyAddressLine1());
        if (request.getPharmacyAddressLine2() != null) prescription.setPharmacyAddressLine2(request.getPharmacyAddressLine2());
        if (request.getPharmacyCity() != null) prescription.setPharmacyCity(request.getPharmacyCity());
        if (request.getPharmacyState() != null) prescription.setPharmacyState(request.getPharmacyState());
        if (request.getPharmacyZip() != null) prescription.setPharmacyZip(request.getPharmacyZip());
        if (request.getPharmacyPhone() != null) prescription.setPharmacyPhone(request.getPharmacyPhone());
        if (request.getPrescribingProviderId() != null) prescription.setPrescribingProviderId(request.getPrescribingProviderId());
        if (request.getPrescribingProviderNpi() != null) prescription.setPrescribingProviderNpi(request.getPrescribingProviderNpi());
        if (request.getPrescribingProviderName() != null) prescription.setPrescribingProviderName(request.getPrescribingProviderName());
        if (request.getNotes() != null) prescription.setNotes(request.getNotes());
        if (request.getSpecialInstructions() != null) prescription.setSpecialInstructions(request.getSpecialInstructions());

        // FR-P1.4a: prefer explicit diagnoses list; fall back to legacy single code.
        // Uses the managed-collection pattern (clear + addAll) so JPA orphanRemoval fires correctly.
        if (request.getDiagnoses() != null && !request.getDiagnoses().isEmpty()) {
            prescription.setDiagnosisCode(resolvePrimaryDiagnosisCode(request));
            prescription.getDiagnoses().clear();
            prescription.getDiagnoses().addAll(buildDiagnosisEntities(request, prescription));
        } else if (request.getDiagnosisCode() != null) {
            prescription.setDiagnosisCode(request.getDiagnosisCode());
        }

        prescription.getMedications().clear();
        // Flush orphan removals before re-inserting line_number values.
        // Without this, Hibernate may attempt inserts before deletes and violate
        // uq_prescription_medication_line (prescription_id, line_number).
        prescriptionRepository.saveAndFlush(prescription);
        for (int i = 0; i < lineRequests.size(); i++) {
            PrescriptionMedicationRequest lr = lineRequests.get(i);
            PrescriptionMedication m = mapLineRequestToEntity(lr);
            m.setLineNumber(i + 1);
            m.setPrescription(prescription);
            prescription.getMedications().add(m);
        }
        syncHeaderFromMedications(prescription);

        prescription.setUpdatedBy(userId);
        
        Prescription updatedPrescription;
        try {
            // Flush during request handling so constraint errors are caught here instead of
            // propagating at transaction commit as a generic 500 DataAccessException.
            updatedPrescription = prescriptionRepository.saveAndFlush(prescription);
            validatePrescription(updatedPrescription);
        } catch (DataAccessException ex) {
            log.error("Prescription update failed for {}", prescriptionId, ex);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid prescription update data. Please review medication lines, diagnoses, and required fields.",
                ex
            );
        }

        log.info("Updated prescription: {}", updatedPrescription.getPrescriptionId());

        PrescriptionResponse response = mapToResponse(updatedPrescription);

        Map<String, Object> updatedEvent = new java.util.HashMap<>();
        updatedEvent.put("prescriptionId", response.getPrescriptionId());
        updatedEvent.put("patientId", response.getPatientId());
        updatedEvent.put("encounterId", response.getEncounterId());
        updatedEvent.put("prescribingProviderId", response.getPrescribingProviderId());
        updatedEvent.put("medicationCode", response.getMedicationCode());
        updatedEvent.put("medicationName", response.getMedicationName());
        updatedEvent.put("quantity", PrescriptionDerivedQuantity.deriveUnits(
                response.getFrequency(), response.getDurationDays()));
        updatedEvent.put("route", response.getRoute() != null ? response.getRoute().name() : null);
        updatedEvent.put("frequency", response.getFrequency());
        updatedEvent.put("status", response.getPrescriptionStatus());
        domainEventPublisher.publish("prescription.updated", updatedEvent);

        return response;
    }
    
    /**
     * Delete a prescription
     */
    @Transactional
    public void deletePrescription(UUID prescriptionId) {
        log.info("Deleting prescription: {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + prescriptionId));
        
        // Only allow deletion of DRAFT prescriptions
        if (prescription.getPrescriptionStatus() != Prescription.PrescriptionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot delete prescription that is not in DRAFT status");
        }
        
        try {
            // Explicit cleanup for refill tables keeps delete robust even on older DB instances
            // where FK cascade rules may differ from current schema.
            prescriptionRefillRepository.deleteByPrescriptionPrescriptionId(prescriptionId);
            prescriptionRefillRequestRepository.deleteByPrescriptionPrescriptionId(prescriptionId);

            // Defensive cleanup before deleting the parent row.
            prescriptionInteractionRepository.deleteByPrescriptionPrescriptionId(prescriptionId);
            prescriptionAllergyCheckRepository.deleteByPrescriptionPrescriptionId(prescriptionId);
            prescriptionHistoryRepository.deleteByPrescriptionPrescriptionId(prescriptionId);

            prescriptionRepository.delete(prescription);
            log.info("Deleted prescription: {}", prescriptionId);
        } catch (DataIntegrityViolationException ex) {
            log.error("Cannot delete prescription {} due to related records", prescriptionId, ex);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete prescription because related records exist. Cancel the prescription instead.");
        }
    }
    
    // ========== Drug Interaction Checking ==========

    /**
     * Pre-prescription ad-hoc screening for the first medication being entered.
     * <ul>
     *   <li><b>Drug–drug:</b> {@link DrugInteractionDatabaseService#checkDrugDrugInteractions} — uses
     *       configured external interaction API when {@code drug-interaction.database.enabled=true},
     *       otherwise the service’s local interaction rule set.</li>
     *   <li><b>FR-P1.7:</b> {@link ClinicalMedicationSafetyService#evaluateForAdHocCheck} — drug–disease
     *       heuristics from {@code PatientProblem}, labs, optional request overrides (weight, eGFR,
     *       pregnancy/lactation), age/weight bands, renal/hepatic alerts.</li>
     * </ul>
     * Duplicate same {@code medicationCode} on active meds is still flagged explicitly.
     */
    @Transactional
    public DrugInteractionCheckResponse checkDrugInteractions(DrugInteractionCheckRequest request, UUID patientId) {
        log.info("Checking drug interactions for medication: {}", request.getMedicationCode());

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));

        List<DrugInteractionCheckResponse.InteractionDetail> interactions = new ArrayList<>();

        List<Prescription> activePrescriptions = prescriptionRepository.findActivePrescriptionsByPatient(patientId);
        List<String> existingMedicationCodes = new ArrayList<>();
        for (Prescription p : activePrescriptions) {
            for (PrescriptionMedication m : listMedications(p)) {
                if (m.getMedicationCode() != null && !m.getMedicationCode().isEmpty()) {
                    existingMedicationCodes.add(m.getMedicationCode());
                }
            }
        }
        if (request.getExistingMedicationCodes() != null) {
            existingMedicationCodes.addAll(request.getExistingMedicationCodes());
        }

        if (request.getMedicationCode() != null && existingMedicationCodes.contains(request.getMedicationCode())) {
            interactions.add(DrugInteractionCheckResponse.InteractionDetail.builder()
                    .interactingMedication("Existing medication (same code)")
                    .interactionType("Duplicate medication code")
                    .interactionCategory(PrescriptionInteraction.InteractionCategory.DRUG_DRUG)
                    .severity(PrescriptionInteraction.InteractionSeverity.MODERATE)
                    .clinicalSignificanceLevel(PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE)
                    .description("Patient may already be on this medication (same medication code on file).")
                    .clinicalSignificance("Risk of duplicate therapy or overdose")
                    .actionRequired("Review active medications before prescribing.")
                    .managementGuidance("Confirm intent to duplicate therapy or discontinue prior order.")
                    .build());
        }

        String newCode = request.getMedicationCode();
        String newName = request.getMedicationName() != null ? request.getMedicationName() : "";
        for (Prescription p : activePrescriptions) {
            for (PrescriptionMedication m : listMedications(p)) {
                List<DrugInteractionDatabaseService.DrugInteractionResult> dd =
                        drugInteractionDatabaseService.checkDrugDrugInteractions(
                                newCode, newName,
                                m.getMedicationCode(), m.getMedicationName());
                for (DrugInteractionDatabaseService.DrugInteractionResult r : dd) {
                    interactions.add(mapDrugInteractionResultToDetail(r, m.getMedicationName(), m.getMedicationCode()));
                }
            }
        }

        List<PatientProblem> problems = patientProblemRepository.findCurrentProblemsByPatient(patientId);
        List<LabResult> labs = labResultRepository.findByPatientPatientIdOrderByResultDateDesc(patientId);
        if (labs.size() > 40) {
            labs = new ArrayList<>(labs.subList(0, 40));
        }
        List<DrugInteractionDatabaseService.DrugInteractionResult> clinical =
                clinicalMedicationSafetyService.evaluateForAdHocCheck(
                        patient, newName, newCode, problems, labs, request);
        for (DrugInteractionDatabaseService.DrugInteractionResult r : clinical) {
            interactions.add(mapDrugInteractionResultToDetail(r, r.getInteractingMedication(), null));
        }

        boolean hasInteractions = !interactions.isEmpty();
        String summary = hasInteractions
                ? "Clinical screening found alerts or interactions — review details."
                : "No significant drug-drug or clinical screening alerts for this check.";

        return DrugInteractionCheckResponse.builder()
                .hasInteractions(hasInteractions)
                .interactions(interactions)
                .summary(summary)
                .build();
    }

    private DrugInteractionCheckResponse.InteractionDetail mapDrugInteractionResultToDetail(
            DrugInteractionDatabaseService.DrugInteractionResult r,
            String interactingLabel,
            String interactingCode) {
        return DrugInteractionCheckResponse.InteractionDetail.builder()
                .interactingMedication(interactingLabel != null ? interactingLabel : r.getInteractingMedication())
                .interactingMedicationCode(interactingCode)
                .interactionType(r.getInteractionType())
                .interactionCategory(r.getInteractionCategory())
                .severity(r.getSeverity())
                .clinicalSignificanceLevel(r.getClinicalSignificanceLevel())
                .description(r.getDescription())
                .clinicalSignificance(r.getClinicalSignificance())
                .actionRequired(r.getManagementGuidance())
                .managementGuidance(r.getManagementGuidance())
                .mechanism(r.getMechanism())
                .onsetTime(r.getOnsetTime())
                .evidenceLevel(r.getEvidenceLevel())
                .build();
    }
    
    /**
     * Persisted prescription check: drug–drug, drug–food, drug–lab (via
     * {@link ComprehensiveInteractionService}), plus FR-P1.7 clinical safety per line
     * (disease, dosing, pregnancy/lactation category, renal/hepatic) stored as
     * {@code PrescriptionInteraction} rows.
     */
    @Transactional
    public PrescriptionResponse checkPrescriptionInteractions(UUID prescriptionId) {
        log.info("Checking interactions for prescription: {} (comprehensive check)", prescriptionId);
        
        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);
        
        // Use comprehensive interaction service for enhanced checking
        List<PrescriptionInteraction> interactions = comprehensiveInteractionService
                .checkComprehensiveInteractions(prescriptionId);
        
        // Update prescription status
        prescription.setHasInteractions(!interactions.isEmpty());
        prescriptionRepository.save(prescription);
        
        log.info("Found {} interactions for prescription: {}", interactions.size(), prescriptionId);
        return mapToResponse(prescription);
    }
    
    // ========== Allergy Checking ==========
    
    /**
     * FR-P1.7 / FR-P1.2 — Check for drug allergies using Phase 1 heuristic matching.
     *
     * <p>Matching covers: direct name / code match, brand/generic synonyms,
     * combination-drug component membership, drug-class membership, and
     * documented cross-reactivity between drug classes (returned as CROSS_REACTIVITY
     * entries which are clinical warnings rather than hard blocks).
     */
    @Transactional
    public AllergyCheckResponse checkAllergies(AllergyCheckRequest request) {
        log.info("Checking allergies for medication: {} ({})",
                request.getMedicationName(), request.getMedicationCode());

        List<Allergy> patientAllergies = allergyRepository.findByPatientPatientIdAndStatus(
                request.getPatientId(), Allergy.Status.ACTIVE);

        List<AllergyMatchingService.AllergyMatchResult> matchResults =
                allergyMatchingService.checkMedication(
                        request.getMedicationName(),
                        request.getMedicationCode(),
                        patientAllergies);

        List<AllergyCheckResponse.AllergyDetail> details = matchResults.stream()
                .map(r -> AllergyCheckResponse.AllergyDetail.builder()
                        .allergenName(r.allergy().getAllergenName())
                        .allergenType(r.allergy().getAllergenType() != null
                                ? r.allergy().getAllergenType().toString() : null)
                        .reactionType(r.allergy().getReactionType())
                        .severity(mapAllergySeverity(r.allergy().getSeverity()))
                        .matchType(r.matchType().name())
                        .clinicalNote(r.clinicalNote())
                        .build())
                .collect(Collectors.toList());

        boolean hasAllergies = !details.isEmpty();
        String summary = hasAllergies
                ? buildAllergySummary(details)
                : "No known drug allergy or cross-reactivity concern detected for this medication.";

        return AllergyCheckResponse.builder()
                .hasAllergies(hasAllergies)
                .allergies(details)
                .summary(summary)
                .build();
    }

    private String buildAllergySummary(List<AllergyCheckResponse.AllergyDetail> details) {
        long directCount = details.stream()
                .filter(d -> !"CROSS_REACTIVITY".equals(d.getMatchType()))
                .count();
        long crossCount = details.stream()
                .filter(d -> "CROSS_REACTIVITY".equals(d.getMatchType()))
                .count();
        StringBuilder sb = new StringBuilder();
        if (directCount > 0) {
            sb.append("ALLERGY ALERT: Patient has ").append(directCount)
              .append(" known drug allergy/allergies to this medication or its components. ");
        }
        if (crossCount > 0) {
            sb.append("CROSS-REACTIVITY WARNING: ").append(crossCount)
              .append(" cross-reactivity concern(s) identified — clinical review required.");
        }
        return sb.toString().trim();
    }
    
    /**
     * FR-P1.7 / FR-P1.2 — Check allergies for every medication line on a prescription,
     * using Phase 1 heuristic matching (AllergyMatchingService).
     *
     * <p>Behavioural guarantees:
     * <ul>
     *   <li>Unacknowledged existing check rows are purged and re-generated on each call
     *       (idempotent re-check). Acknowledged rows are preserved for audit compliance.</li>
     *   <li>CROSS_REACTIVITY alerts are stored with matchType=CROSS_REACTIVITY so
     *       the UI can render them differently from confirmed allergy alerts.</li>
     *   <li>One row per allergy-medication match is persisted; de-duplication within
     *       the same medication-allergen pair is applied via the result list.</li>
     * </ul>
     */
    @Transactional
    public PrescriptionResponse checkPrescriptionAllergies(UUID prescriptionId) {
        log.info("Checking allergies for prescription: {}", prescriptionId);

        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);

        List<Allergy> patientAllergies = allergyRepository.findByPatientPatientIdAndStatus(
                prescription.getPatient().getPatientId(), Allergy.Status.ACTIVE);

        // Purge unacknowledged rows to allow clean re-evaluation; keep acknowledged rows for audit.
        List<PrescriptionAllergyCheck> existing =
                prescriptionAllergyCheckRepository.findByPrescriptionPrescriptionId(
                        prescription.getPrescriptionId());
        List<PrescriptionAllergyCheck> unacknowledged = existing.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsAcknowledged()))
                .collect(Collectors.toList());
        if (!unacknowledged.isEmpty()) {
            prescriptionAllergyCheckRepository.deleteAll(unacknowledged);
            prescriptionAllergyCheckRepository.flush();
        }

        List<PrescriptionAllergyCheck> allergyChecks = new ArrayList<>();

        for (PrescriptionMedication med : listMedications(prescription)) {
            if (med.getMedicationName() == null || med.getMedicationName().isBlank()) {
                continue;
            }

            List<AllergyMatchingService.AllergyMatchResult> matches =
                    allergyMatchingService.checkMedication(
                            med.getMedicationName(),
                            med.getMedicationCode(),
                            patientAllergies);

            for (AllergyMatchingService.AllergyMatchResult match : matches) {
                Allergy allergy = match.allergy();
                PrescriptionAllergyCheck check = PrescriptionAllergyCheck.builder()
                        .prescription(prescription)
                        .allergenName(allergy.getAllergenName())
                        .allergenCode(allergy.getAllergenCode())
                        .allergenType(allergy.getAllergenType() != null
                                ? allergy.getAllergenType().toString() : null)
                        .reactionType(allergy.getReactionType())
                        .severity(mapAllergySeverity(allergy.getSeverity()))
                        .matchType(match.matchType().name())
                        .clinicalNote(match.clinicalNote())
                        .isAcknowledged(false)
                        .build();
                allergyChecks.add(check);
            }
        }

        boolean hasAllergyWarnings = !allergyChecks.isEmpty();
        if (hasAllergyWarnings) {
            prescriptionAllergyCheckRepository.saveAll(allergyChecks);
        }
        prescription.setHasAllergyWarnings(hasAllergyWarnings);
        prescriptionRepository.save(prescription);

        return mapToResponse(prescription);
    }
    
    // ========== Prescription Validation ==========
    
    /**
     * Validate a prescription
     */
    @Transactional
    public PrescriptionResponse validatePrescription(UUID prescriptionId) {
        log.info("Validating prescription: {}", prescriptionId);

        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);

        validatePrescription(prescription);

        // Advance DRAFT → PENDING when validation passes (no hard errors).
        // WARNINGS are acceptable — prescriber has been informed via validationNotes.
        if (prescription.getPrescriptionStatus() == Prescription.PrescriptionStatus.DRAFT
                && prescription.getValidationStatus() != Prescription.ValidationStatus.ERRORS) {
            prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.PENDING);
            prescriptionRepository.save(prescription);
            log.info("Prescription {} advanced from DRAFT to PENDING after validation", prescriptionId);
        }

        return mapToResponse(prescription);
    }
    
    private void validatePrescription(Prescription prescription) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // FR-P1.10: extract prescriber last name once for second-letter DEA check.
        // prescriberLastName is null when the name field is absent; the validator skips the
        // second-letter check gracefully in that case rather than producing a false error.
        String prescriberLastName = extractLastName(prescription.getPrescribingProviderName());

        List<PrescriptionMedication> meds = listMedications(prescription);
        if (meds.isEmpty()) {
            errors.add("At least one medication is required");
        }
        for (PrescriptionMedication m : meds) {
            if (m.getMedicationName() == null || m.getMedicationName().isEmpty()) {
                errors.add("Medication name is required on each line");
            }
            if (Boolean.TRUE.equals(m.getIsControlledSubstance())) {
                if (m.getSchedule() == null) {
                    errors.add("Schedule is required for controlled substance: " + m.getMedicationName());
                }
                // FR-P2 / FR-P1.10 / §3.1.7: validate DEA number format, check digit,
                // and second-letter-matches-last-name.
                String deaMsg = deaNumberValidator.validationMessage(m.getDeaNumber(), prescriberLastName);
                if (deaMsg != null) {
                    errors.add(deaMsg + " (controlled substance: " + m.getMedicationName() + ")");
                }
            }
        }
        
        // FR-P3.6: multi-line prescriptions produce one NCPDP SCRIPT <Message>/<NewRx> per line;
        // inform the prescriber so they expect multiple network transactions at transmit time.
        if (meds.size() > 1) {
            warnings.add(String.format(
                    "FR-P3.6: this prescription has %d medication lines — NCPDP e-prescribing will "
                    + "generate and transmit %d separate NewRx message(s) (one per line), each "
                    + "validated against the SCRIPT 2017071 XSD before send.",
                    meds.size(), meds.size()));
        }

        if (prescription.getStartDate() == null) {
            errors.add("Start date is required");
        }
        
        if (prescription.getPrescribingProviderId() == null) {
            errors.add("Prescribing provider is required");
        }

        // FR-P3.x: validate prescriber NPI using Luhn algorithm (format-only check is insufficient
        // — Surescripts rejects messages where the NPI fails the CMS Luhn checksum).
        if (prescription.getPrescribingProviderNpi() != null && !prescription.getPrescribingProviderNpi().isBlank()) {
            String prescriberNpiMsg = npiValidator.validationMessage(prescription.getPrescribingProviderNpi());
            if (prescriberNpiMsg != null) {
                errors.add("Prescribing provider NPI is invalid: " + prescriberNpiMsg);
            }
        }

        // FR-P3.x: validate pharmacy NPI when present (used in NCPDP SCRIPT <To> element).
        if (prescription.getPharmacyNpi() != null && !prescription.getPharmacyNpi().isBlank()) {
            String pharmacyNpiMsg = npiValidator.validationMessage(prescription.getPharmacyNpi());
            if (pharmacyNpiMsg != null) {
                errors.add("Pharmacy NPI is invalid: " + pharmacyNpiMsg);
            }
        }
        
        // Validate controlled substances (header flag synced from lines)
        if (Boolean.TRUE.equals(prescription.getIsControlledSubstance())) {
            // FR-P2 / §3.1.7: validate the header-level DEA number when it is populated.
            // (Individual medication lines carry their own DEA numbers validated above;
            //  the header-level field is used by PDMP queries and e-prescribing routing.)
            if (prescription.getDeaNumber() != null && !prescription.getDeaNumber().isBlank()) {
                String headerDeaMsg = deaNumberValidator.validationMessage(prescription.getDeaNumber(), prescriberLastName);
                if (headerDeaMsg != null) {
                    errors.add("Prescription-level DEA number is invalid: " + headerDeaMsg);
                }
            }

            // Check if PDMP has been queried (warning, not error — some states may not require it)
            if (prescription.getPdmpQueried() == null || !prescription.getPdmpQueried()) {
                warnings.add("PDMP has not been queried for this controlled substance prescription. " +
                           "It is recommended to query PDMP before prescribing controlled substances.");
            }
        }
        
        // Validate dates
        if (prescription.getStartDate() != null && prescription.getEndDate() != null) {
            if (prescription.getEndDate().isBefore(prescription.getStartDate())) {
                errors.add("End date cannot be before start date");
            }
        }
        
        // Check for interactions
        if (prescription.getHasInteractions() != null && prescription.getHasInteractions()) {
            warnings.add("Prescription has drug interactions");
        }
        
        // Check for allergies
        if (prescription.getHasAllergyWarnings() != null && prescription.getHasAllergyWarnings()) {
            warnings.add("Prescription has allergy warnings");
        }
        
        // Set validation status
        if (!errors.isEmpty()) {
            prescription.setValidationStatus(Prescription.ValidationStatus.ERRORS);
            prescription.setValidationNotes(String.join("; ", errors));
        } else if (!warnings.isEmpty()) {
            prescription.setValidationStatus(Prescription.ValidationStatus.WARNINGS);
            prescription.setValidationNotes(String.join("; ", warnings));
        } else {
            prescription.setValidationStatus(Prescription.ValidationStatus.VALID);
            prescription.setValidationNotes("Prescription is valid");
        }
        
        prescriptionRepository.save(prescription);
        
        // Create history record
        PrescriptionHistory history = PrescriptionHistory.builder()
            .prescription(prescription)
            .changeType(PrescriptionHistory.ChangeType.VALIDATED)
            .changedBy(prescription.getUpdatedBy() != null ? prescription.getUpdatedBy() : prescription.getCreatedBy())
            .changedDate(LocalDateTime.now())
            .newValue(prescription.getValidationStatus().toString())
            .fieldName("validation_status")
            .notes(prescription.getValidationNotes())
            .build();
        prescriptionHistoryRepository.save(history);
    }
    
    // ========== Prescription Transmission ==========
    
    /**
     * Transmit prescription (e-prescribing)
     */
    @Transactional
    public PrescriptionResponse transmitPrescription(UUID prescriptionId, PrescriptionTransmitRequest request, UUID userId) {
        log.info("Transmitting prescription: {}", prescriptionId);
        
        Prescription prescription = fetchPrescriptionForUpdate(prescriptionId);
        
        // Validate prescription before transmission
        if (prescription.getValidationStatus() == Prescription.ValidationStatus.ERRORS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot transmit prescription with validation errors");
        }
        
        // Check for unacknowledged interactions
        if (!request.getOverrideInteractions() && prescription.getHasInteractions()) {
            List<PrescriptionInteraction> unacknowledged = 
                prescriptionInteractionRepository.findUnacknowledgedInteractions(prescriptionId);
            if (!unacknowledged.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot transmit prescription with unacknowledged drug interactions");
            }
        }
        
        // Check for unacknowledged allergies
        if (!request.getOverrideAllergies() && prescription.getHasAllergyWarnings()) {
            List<PrescriptionAllergyCheck> unacknowledged = 
                prescriptionAllergyCheckRepository.findUnacknowledgedAllergyChecks(prescriptionId);
            if (!unacknowledged.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot transmit prescription with unacknowledged allergy warnings");
            }
        }
        
        // Check PDMP query for controlled substances
        if (prescription.getIsControlledSubstance() != null && prescription.getIsControlledSubstance()) {
            if (prescription.getPdmpQueried() == null || !prescription.getPdmpQueried()) {
                // For controlled substances, PDMP query is strongly recommended
                // Some states may require it by law
                if (!request.getOverridePdmpCheck()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "PDMP query is required before transmitting controlled substance prescription. "
                            + "Please query PDMP first or override this check if legally permitted.");
                }
            }
        }
        
        // Allow pharmacy to be supplied inline with the transmit request
        UUID requestPharmacyId = null;
        if (request.getPharmacyId() != null && !request.getPharmacyId().isBlank()) {
            try {
                requestPharmacyId = UUID.fromString(request.getPharmacyId());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid pharmacyId format. Expected UUID value.");
            }
        }
        UUID effectivePharmacyId = prescription.getPharmacyId() != null ? prescription.getPharmacyId()
                : requestPharmacyId;
        String effectivePharmacyNpi = prescription.getPharmacyNpi() != null ? prescription.getPharmacyNpi()
                : request.getPharmacyNpi();
        String effectivePharmacyName = prescription.getPharmacyName() != null ? prescription.getPharmacyName()
                : request.getPharmacyName();

        // Verify a pharmacy routing target is present before attempting network transmission
        if (effectivePharmacyId == null && effectivePharmacyNpi == null && effectivePharmacyName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot transmit prescription: no pharmacy is assigned. "
                    + "Please set a pharmacy (pharmacyId or pharmacyNpi) before transmitting.");
        }

        // Use E-Prescribing service to actually transmit the prescription
        com.easyops.hospital.dto.request.PrescriptionTransmissionRequest transmissionRequest =
            com.easyops.hospital.dto.request.PrescriptionTransmissionRequest.builder()
                .prescriptionId(prescriptionId)
                .pharmacyId(effectivePharmacyId)
                .pharmacyNpi(effectivePharmacyNpi)
                .pharmacyName(effectivePharmacyName)
                .pharmacyAddressLine1(prescription.getPharmacyAddressLine1())
                .pharmacyAddressLine2(prescription.getPharmacyAddressLine2())
                .pharmacyCity(prescription.getPharmacyCity())
                .pharmacyState(prescription.getPharmacyState())
                .pharmacyZip(prescription.getPharmacyZip())
                .pharmacyPhone(prescription.getPharmacyPhone())
                .overrideInteractions(request.getOverrideInteractions())
                .overrideAllergies(request.getOverrideAllergies())
                .overridePdmpCheck(request.getOverridePdmpCheck())
                .overrideReason(request.getOverrideReason())
                .build();
        
        // Get provider information for transmission
        String providerNpi = prescription.getPrescribingProviderNpi();
        String providerName = prescription.getPrescribingProviderName();
        
        // Transmit via e-prescribing service
        com.easyops.hospital.dto.response.PrescriptionTransmissionResponse transmissionResponse = 
            eprescribingService.transmitPrescription(transmissionRequest, userId, providerName, providerNpi);
        
        // Update prescription based on transmission result
        if (transmissionResponse.getTransmissionSuccess()) {
            prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.SENT);
            prescription.setSentDate(LocalDateTime.now());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Prescription transmission failed: "
                    + (transmissionResponse.getErrorMessage() != null
                        ? transmissionResponse.getErrorMessage() : "Unknown error"));
        }
        
        prescription.setUpdatedBy(userId);
        Prescription transmittedPrescription = prescriptionRepository.save(prescription);
        
        log.info("Transmitted prescription: {} via e-prescribing network", transmittedPrescription.getPrescriptionId());
        return mapToResponse(transmittedPrescription);
    }
    
    // ========== Prescription Status Management ==========
    
    /**
     * Cancel a prescription
     */
    @Transactional
    public PrescriptionResponse cancelPrescription(UUID prescriptionId, String reason, UUID userId) {
        log.info("Cancelling prescription: {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + prescriptionId));
        
        if (prescription.getPrescriptionStatus() == Prescription.PrescriptionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription is already cancelled");
        }

        // Capture prior status before mutating the entity
        String previousStatus = prescription.getPrescriptionStatus().toString();

        prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.CANCELLED);
        prescription.setCancellationDate(LocalDateTime.now());
        prescription.setCancellationReason(reason);
        prescription.setCancelledBy(userId);
        prescription.setUpdatedBy(userId);
        
        Prescription cancelledPrescription = prescriptionRepository.save(prescription);
        
        // Create history record
        PrescriptionHistory history = PrescriptionHistory.builder()
            .prescription(cancelledPrescription)
            .changeType(PrescriptionHistory.ChangeType.CANCELLED)
            .changedBy(userId)
            .changedDate(LocalDateTime.now())
            .fieldName("prescription_status")
            .previousValue(previousStatus)
            .newValue("CANCELLED")
            .changeReason(reason)
            .build();
        prescriptionHistoryRepository.save(history);
        
        log.info("Cancelled prescription: {}", cancelledPrescription.getPrescriptionId());
        return mapToResponse(cancelledPrescription);
    }
    
    // ========== Interaction & Allergy Acknowledgment ==========

    /**
     * Acknowledge a single drug interaction, persisting the override reason and auditing who
     * reviewed the interaction.  Once all interactions on a prescription are acknowledged the
     * normal transmit flow (without overrideInteractions) will succeed.
     */
    @Transactional
    public PrescriptionInteractionResponse acknowledgeInteraction(
            UUID prescriptionId, UUID interactionId, String overrideReason, UUID userId) {

        log.info("Acknowledging interaction {} for prescription {}", interactionId, prescriptionId);

        PrescriptionInteraction interaction = prescriptionInteractionRepository.findById(interactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Interaction not found: " + interactionId));

        if (!interaction.getPrescription().getPrescriptionId().equals(prescriptionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Interaction does not belong to the specified prescription");
        }

        if (Boolean.TRUE.equals(interaction.getIsAcknowledged())) {
            return mapInteractionToResponse(interaction);
        }

        interaction.setIsAcknowledged(true);
        interaction.setAcknowledgedBy(userId);
        interaction.setAcknowledgedDate(LocalDateTime.now());
        interaction.setOverrideReason(overrideReason);

        PrescriptionInteraction saved = prescriptionInteractionRepository.save(interaction);

        // Audit
        Prescription prescription = saved.getPrescription();
        long unacknowledged = prescriptionInteractionRepository
            .findUnacknowledgedInteractions(prescriptionId).size();
        PrescriptionHistory history = PrescriptionHistory.builder()
            .prescription(prescription)
            .changeType(PrescriptionHistory.ChangeType.UPDATED)
            .changedBy(userId)
            .changedDate(LocalDateTime.now())
            .fieldName("interaction_acknowledged")
            .newValue(interactionId.toString())
            .changeReason(overrideReason)
            .notes("Interaction acknowledged; " + unacknowledged + " unacknowledged remaining")
            .build();
        prescriptionHistoryRepository.save(history);

        log.info("Interaction {} acknowledged by {}", interactionId, userId);
        return mapInteractionToResponse(saved);
    }

    /**
     * Get all interactions for a prescription (for the acknowledgment UI).
     */
    public List<PrescriptionInteractionResponse> getInteractionsByPrescription(UUID prescriptionId) {
        return prescriptionInteractionRepository
            .findByPrescriptionPrescriptionId(prescriptionId)
            .stream()
            .map(this::mapInteractionToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Acknowledge a single allergy check entry.
     */
    @Transactional
    public PrescriptionAllergyCheckResponse acknowledgeAllergyCheck(
            UUID prescriptionId, UUID checkId, String overrideReason, UUID userId) {

        log.info("Acknowledging allergy check {} for prescription {}", checkId, prescriptionId);

        PrescriptionAllergyCheck check = prescriptionAllergyCheckRepository.findById(checkId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Allergy check not found: " + checkId));

        if (!check.getPrescription().getPrescriptionId().equals(prescriptionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Allergy check does not belong to the specified prescription");
        }

        if (Boolean.TRUE.equals(check.getIsAcknowledged())) {
            return mapAllergyCheckToResponse(check);
        }

        check.setIsAcknowledged(true);
        check.setAcknowledgedBy(userId);
        check.setAcknowledgedDate(LocalDateTime.now());
        check.setOverrideReason(overrideReason);
        check.setActionTaken(PrescriptionAllergyCheck.ActionTaken.OVERRIDDEN);
        check.setOverrideBy(userId);
        check.setOverrideDate(LocalDateTime.now());

        PrescriptionAllergyCheck saved = prescriptionAllergyCheckRepository.save(check);

        // Audit
        Prescription prescription = saved.getPrescription();
        long unacknowledged = prescriptionAllergyCheckRepository
            .findUnacknowledgedAllergyChecks(prescriptionId).size();
        PrescriptionHistory history = PrescriptionHistory.builder()
            .prescription(prescription)
            .changeType(PrescriptionHistory.ChangeType.UPDATED)
            .changedBy(userId)
            .changedDate(LocalDateTime.now())
            .fieldName("allergy_check_acknowledged")
            .newValue(checkId.toString())
            .changeReason(overrideReason)
            .notes("Allergy check acknowledged; " + unacknowledged + " unacknowledged remaining")
            .build();
        prescriptionHistoryRepository.save(history);

        log.info("Allergy check {} acknowledged by {}", checkId, userId);
        return mapAllergyCheckToResponse(saved);
    }

    /**
     * Get all allergy checks for a prescription (for the acknowledgment UI).
     */
    public List<PrescriptionAllergyCheckResponse> getAllergyChecksByPrescription(UUID prescriptionId) {
        return prescriptionAllergyCheckRepository
            .findByPrescriptionPrescriptionId(prescriptionId)
            .stream()
            .map(this::mapAllergyCheckToResponse)
            .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========
    
    private PrescriptionAllergyCheck.AllergySeverity mapAllergySeverity(Allergy.Severity severity) {
        if (severity == null) return null;
        switch (severity) {
            case MILD: return PrescriptionAllergyCheck.AllergySeverity.MILD;
            case MODERATE: return PrescriptionAllergyCheck.AllergySeverity.MODERATE;
            case SEVERE: return PrescriptionAllergyCheck.AllergySeverity.SEVERE;
            case LIFE_THREATENING: return PrescriptionAllergyCheck.AllergySeverity.LIFE_THREATENING;
            default: return null;
        }
    }
    
    // ========== Mapping Methods ==========

    private Prescription fetchPrescriptionForUpdate(UUID prescriptionId) {
        // Always use plain findById to avoid any chance of fetching multiple bag collections
        // in a single query (medications + diagnoses), which can trigger MultipleBagFetchException.
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + prescriptionId));
        if (prescription.getMedications() == null) {
            prescription.setMedications(new ArrayList<>());
        }
        if (prescription.getDiagnoses() == null) {
            prescription.setDiagnoses(new ArrayList<>());
        }
        // Initialize collections inside the transaction so downstream update logic can mutate them safely.
        prescription.getMedications().size();
        prescription.getDiagnoses().size();
        return prescription;
    }

    private List<PrescriptionMedicationRequest> normalizeMedicationLines(PrescriptionRequest request) {
        if (request.getMedications() != null && !request.getMedications().isEmpty()) {
            return request.getMedications();
        }
        if (request.getMedicationName() != null && !request.getMedicationName().isBlank()) {
            if (request.getDosageForm() == null || request.getRoute() == null || request.getStartDate() == null) {
                throw new IllegalArgumentException(
                    "Legacy single-medication requests require dosage form, route, and start date");
            }
            return List.of(PrescriptionMedicationRequest.builder()
                .medicationName(request.getMedicationName().trim())
                .medicationCode(request.getMedicationCode())
                .medicationCodeType(request.getMedicationCodeType())
                .dosageStrength(request.getDosageStrength())
                .dosageUnit(request.getDosageUnit())
                .dosageForm(request.getDosageForm())
                .route(request.getRoute())
                .frequency(request.getFrequency())
                .instructions(request.getInstructions())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .durationDays(request.getDurationDays())
                .refillsAuthorized(request.getRefillsAuthorized())
                .refillsRemaining(request.getRefillsRemaining())
                .substitutionAllowed(request.getSubstitutionAllowed())
                .dawCode(request.getDawCode())
                .isControlledSubstance(request.getIsControlledSubstance())
                .schedule(request.getSchedule())
                .deaNumber(request.getDeaNumber())
                .build());
        }
        return List.of();
    }

    private PrescriptionMedication mapLineRequestToEntity(PrescriptionMedicationRequest lr) {
        if (lr.getMedicationName() == null || lr.getMedicationName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medication name is required on each line");
        }
        if (lr.getDosageForm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosage form is required on each line");
        }
        if (lr.getRoute() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route is required on each line");
        }
        if (lr.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required on each line");
        }

        return PrescriptionMedication.builder()
            .medicationName(lr.getMedicationName().trim())
            .medicationCode(lr.getMedicationCode())
            .medicationCodeType(lr.getMedicationCodeType())
            .dosageStrength(lr.getDosageStrength())
            .dosageUnit(lr.getDosageUnit())
            .dosageForm(lr.getDosageForm())
            .route(lr.getRoute())
            .frequency(lr.getFrequency())
            .instructions(lr.getInstructions())
            .startDate(lr.getStartDate())
            .endDate(lr.getEndDate())
            .durationDays(lr.getDurationDays())
            .refillsAuthorized(lr.getRefillsAuthorized() != null ? lr.getRefillsAuthorized() : 0)
            .refillsRemaining(lr.getRefillsRemaining() != null ? lr.getRefillsRemaining() : 0)
            .substitutionAllowed(lr.getSubstitutionAllowed() != null ? lr.getSubstitutionAllowed() : true)
            .dawCode(lr.getDawCode())
            .isControlledSubstance(lr.getIsControlledSubstance() != null ? lr.getIsControlledSubstance() : false)
            .schedule(lr.getSchedule())
            .deaNumber(lr.getDeaNumber())
            .build();
    }

    private void syncHeaderFromMedications(Prescription p) {
        List<PrescriptionMedication> meds = p.getMedications();
        if (meds == null || meds.isEmpty()) {
            return;
        }
        meds.sort(Comparator.comparing(PrescriptionMedication::getLineNumber));
        PrescriptionMedication first = meds.get(0);
        if (meds.size() == 1) {
            p.setMedicationName(first.getMedicationName());
        } else {
            p.setMedicationName(meds.stream()
                .map(PrescriptionMedication::getMedicationName)
                .collect(Collectors.joining("; ")));
        }
        p.setMedicationCode(first.getMedicationCode());
        p.setMedicationCodeType(first.getMedicationCodeType());
        p.setDosageStrength(first.getDosageStrength());
        p.setDosageUnit(first.getDosageUnit());
        p.setDosageForm(first.getDosageForm());
        p.setRoute(first.getRoute());
        p.setFrequency(first.getFrequency());
        p.setInstructions(first.getInstructions());
        p.setStartDate(first.getStartDate());
        p.setEndDate(first.getEndDate());
        p.setDurationDays(first.getDurationDays());
        p.setRefillsAuthorized(first.getRefillsAuthorized());
        p.setRefillsRemaining(first.getRefillsRemaining());
        p.setSubstitutionAllowed(first.getSubstitutionAllowed());
        p.setDawCode(first.getDawCode());
        boolean anyControlled = meds.stream().anyMatch(m -> Boolean.TRUE.equals(m.getIsControlledSubstance()));
        p.setIsControlledSubstance(anyControlled);
        PrescriptionMedication ctrl = meds.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsControlledSubstance()))
            .findFirst()
            .orElse(first);
        p.setSchedule(ctrl.getSchedule());
        p.setDeaNumber(ctrl.getDeaNumber());
    }

    private List<PrescriptionMedication> listMedications(Prescription p) {
        if (p.getMedications() != null && !p.getMedications().isEmpty()) {
            return p.getMedications().stream()
                .sorted(Comparator.comparing(PrescriptionMedication::getLineNumber))
                .collect(Collectors.toList());
        }
        List<PrescriptionMedication> stored =
            prescriptionMedicationRepository.findByPrescriptionPrescriptionIdOrderByLineNumberAsc(p.getPrescriptionId());
        if (!stored.isEmpty()) {
            return stored;
        }
        // Legacy fallback: older rows may only have header medication fields populated.
        if (p.getMedicationName() != null && !p.getMedicationName().isBlank()) {
            PrescriptionMedication headerMedication = PrescriptionMedication.builder()
                .lineNumber(1)
                .medicationName(p.getMedicationName())
                .medicationCode(p.getMedicationCode())
                .medicationCodeType(p.getMedicationCodeType())
                .dosageStrength(p.getDosageStrength())
                .dosageUnit(p.getDosageUnit())
                .dosageForm(p.getDosageForm())
                .route(p.getRoute())
                .frequency(p.getFrequency())
                .instructions(p.getInstructions())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .durationDays(p.getDurationDays())
                .refillsAuthorized(p.getRefillsAuthorized())
                .refillsRemaining(p.getRefillsRemaining())
                .substitutionAllowed(p.getSubstitutionAllowed())
                .dawCode(p.getDawCode())
                .isControlledSubstance(p.getIsControlledSubstance())
                .schedule(p.getSchedule())
                .deaNumber(p.getDeaNumber())
                .build();
            return List.of(headerMedication);
        }
        return List.of();
    }

    private PrescriptionMedicationResponse mapMedicationToResponse(PrescriptionMedication m) {
        return PrescriptionMedicationResponse.builder()
            .prescriptionMedicationId(m.getPrescriptionMedicationId())
            .lineNumber(m.getLineNumber())
            .medicationName(m.getMedicationName())
            .medicationCode(m.getMedicationCode())
            .medicationCodeType(m.getMedicationCodeType())
            .dosageStrength(m.getDosageStrength())
            .dosageUnit(m.getDosageUnit())
            .dosageForm(m.getDosageForm())
            .route(m.getRoute())
            .frequency(m.getFrequency())
            .instructions(m.getInstructions())
            .startDate(m.getStartDate())
            .endDate(m.getEndDate())
            .durationDays(m.getDurationDays())
            .refillsAuthorized(m.getRefillsAuthorized())
            .refillsRemaining(m.getRefillsRemaining())
            .substitutionAllowed(m.getSubstitutionAllowed())
            .dawCode(m.getDawCode())
            .isControlledSubstance(m.getIsControlledSubstance())
            .schedule(m.getSchedule())
            .deaNumber(m.getDeaNumber())
            .build();
    }
    
    private PrescriptionResponse mapToResponse(Prescription prescription) {
        List<PrescriptionInteraction> interactions = prescriptionInteractionRepository
            .findByPrescriptionPrescriptionId(prescription.getPrescriptionId());
        List<PrescriptionAllergyCheck> allergyChecks = prescriptionAllergyCheckRepository
            .findByPrescriptionPrescriptionId(prescription.getPrescriptionId());

        List<PrescriptionMedicationResponse> medicationResponses = listMedications(prescription).stream()
            .map(this::mapMedicationToResponse)
            .collect(Collectors.toList());

        List<PrescriptionDiagnosisResponse> diagnosisResponses =
            prescriptionDiagnosisRepository
                .findByPrescriptionPrescriptionIdOrderBySequenceOrderAsc(prescription.getPrescriptionId())
                .stream()
                .map(this::mapDiagnosisToResponse)
                .collect(Collectors.toList());
        
        return PrescriptionResponse.builder()
            .prescriptionId(prescription.getPrescriptionId())
            .patientId(prescription.getPatient().getPatientId())
            .encounterId(prescription.getEncounterId())
            .epEncounterMode(prescription.getEpEncounterMode())
            .prescriptionNumber(prescription.getPrescriptionNumber())
            .prescriptionType(prescription.getPrescriptionType())
            .medications(medicationResponses)
            .medicationName(prescription.getMedicationName())
            .medicationCode(prescription.getMedicationCode())
            .medicationCodeType(prescription.getMedicationCodeType())
            .dosageStrength(prescription.getDosageStrength())
            .dosageUnit(prescription.getDosageUnit())
            .dosageForm(prescription.getDosageForm())
            .route(prescription.getRoute())
            .frequency(prescription.getFrequency())
            .instructions(prescription.getInstructions())
            .startDate(prescription.getStartDate())
            .endDate(prescription.getEndDate())
            .durationDays(prescription.getDurationDays())
            .refillsAuthorized(prescription.getRefillsAuthorized())
            .refillsRemaining(prescription.getRefillsRemaining())
            .substitutionAllowed(prescription.getSubstitutionAllowed())
            .dawCode(prescription.getDawCode())
            .isControlledSubstance(prescription.getIsControlledSubstance())
            .schedule(prescription.getSchedule())
            .deaNumber(prescription.getDeaNumber())
            .pdmpQueried(prescription.getPdmpQueried())
            .pdmpQueryDate(prescription.getPdmpQueryDate())
            .pharmacyId(prescription.getPharmacyId())
            .pharmacyName(prescription.getPharmacyName())
            .pharmacyNpi(prescription.getPharmacyNpi())
            .pharmacyAddressLine1(prescription.getPharmacyAddressLine1())
            .pharmacyAddressLine2(prescription.getPharmacyAddressLine2())
            .pharmacyCity(prescription.getPharmacyCity())
            .pharmacyState(prescription.getPharmacyState())
            .pharmacyZip(prescription.getPharmacyZip())
            .pharmacyPhone(prescription.getPharmacyPhone())
            .prescribingProviderId(prescription.getPrescribingProviderId())
            .prescribingProviderNpi(prescription.getPrescribingProviderNpi())
            .prescribingProviderName(prescription.getPrescribingProviderName())
            .prescriptionStatus(prescription.getPrescriptionStatus())
            .createdDate(prescription.getCreatedDate())
            .sentDate(prescription.getSentDate())
            .filledDate(prescription.getFilledDate())
            .cancellationDate(prescription.getCancellationDate())
            .expirationDate(prescription.getExpirationDate())
            .cancellationReason(prescription.getCancellationReason())
            .cancelledBy(prescription.getCancelledBy())
            .notes(prescription.getNotes())
            .specialInstructions(prescription.getSpecialInstructions())
            .diagnosisCode(prescription.getDiagnosisCode())
            .diagnoses(diagnosisResponses)
            .hasInteractions(prescription.getHasInteractions())
            .hasAllergyWarnings(prescription.getHasAllergyWarnings())
            .validationStatus(prescription.getValidationStatus())
            .validationNotes(prescription.getValidationNotes())
            .createdAt(prescription.getCreatedAt())
            .updatedAt(prescription.getUpdatedAt())
            .createdBy(prescription.getCreatedBy())
            .updatedBy(prescription.getUpdatedBy())
            .interactions(interactions.stream()
                .map(this::mapInteractionToResponse)
                .collect(Collectors.toList()))
            .allergyChecks(allergyChecks.stream()
                .map(this::mapAllergyCheckToResponse)
                .collect(Collectors.toList()))
            .interactionCount(interactions.size())
            .allergyCheckCount(allergyChecks.size())
            .formularyChecked(prescription.getFormularyChecked())
            .formularyCheckDate(prescription.getFormularyCheckDate())
            .coverageStatus(prescription.getCoverageStatus())
            .formularyTier(prescription.getFormularyTier())
            .requiresPriorAuthorization(prescription.getRequiresPriorAuthorization())
            .priorAuthorizationObtained(prescription.getPriorAuthorizationObtained())
            .priorAuthorizationNumber(prescription.getPriorAuthorizationNumber())
            .patientCostEstimate(prescription.getPatientCostEstimate())
            .copayAmount(prescription.getCopayAmount())
            .insuranceId(prescription.getInsuranceId())
            .pbmName(prescription.getPbmName())
            .build();
    }
    
    private PrescriptionInteractionResponse mapInteractionToResponse(PrescriptionInteraction interaction) {
        return PrescriptionInteractionResponse.builder()
            .interactionId(interaction.getInteractionId())
            .prescriptionId(interaction.getPrescription().getPrescriptionId())
            .interactingMedication(interaction.getInteractingMedication())
            .interactingMedicationCode(interaction.getInteractingMedicationCode())
            .interactionType(interaction.getInteractionType())
            .interactionCategory(interaction.getInteractionCategory())
            .severity(interaction.getSeverity())
            .clinicalSignificanceLevel(interaction.getClinicalSignificanceLevel())
            .description(interaction.getDescription())
            .clinicalSignificance(interaction.getClinicalSignificance())
            .actionRequired(interaction.getActionRequired())
            .managementGuidance(interaction.getManagementGuidance())
            .mechanism(interaction.getMechanism())
            .onsetTime(interaction.getOnsetTime())
            .evidenceLevel(interaction.getEvidenceLevel())
            .interactingSubstance(interaction.getInteractingSubstance())
            .interactingSubstanceType(interaction.getInteractingSubstanceType())
            .documentationReferences(interaction.getDocumentationReferences())
            .isAcknowledged(interaction.getIsAcknowledged())
            .acknowledgedBy(interaction.getAcknowledgedBy())
            .acknowledgedDate(interaction.getAcknowledgedDate())
            .overrideReason(interaction.getOverrideReason())
            .createdAt(interaction.getCreatedAt())
            .build();
    }
    
    private PrescriptionAllergyCheckResponse mapAllergyCheckToResponse(PrescriptionAllergyCheck check) {
        return PrescriptionAllergyCheckResponse.builder()
            .checkId(check.getCheckId())
            .prescriptionId(check.getPrescription().getPrescriptionId())
            .allergenName(check.getAllergenName())
            .allergenCode(check.getAllergenCode())
            .allergenType(check.getAllergenType())
            .reactionType(check.getReactionType())
            .severity(check.getSeverity())
            .actionTaken(check.getActionTaken())
            .overrideReason(check.getOverrideReason())
            .overrideBy(check.getOverrideBy())
            .overrideDate(check.getOverrideDate())
            .isAcknowledged(check.getIsAcknowledged())
            .acknowledgedBy(check.getAcknowledgedBy())
            .acknowledgedDate(check.getAcknowledgedDate())
            .matchType(check.getMatchType())
            .clinicalNote(check.getClinicalNote())
            .createdAt(check.getCreatedAt())
            .build();
    }

    // ========== FR-P1.4a: Diagnosis helpers ==========

    /**
     * Derive the primary ICD-10 code from the request.
     * If {@code diagnoses} is provided, returns the code of the first entry flagged as primary
     * after {@link #orderedDiagnosisRequests(PrescriptionRequest) ordering}; if none are flagged
     * primary, the first entry in that order is treated as primary (implicit primary).
     * Falls back to legacy {@code diagnosisCode} when the diagnoses list is absent.
     */
    private String resolvePrimaryDiagnosisCode(PrescriptionRequest request) {
        if (request.getDiagnoses() != null && !request.getDiagnoses().isEmpty()) {
            List<PrescriptionDiagnosisRequest> ordered = orderedDiagnosisRequests(request);
            return ordered.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()))
                .findFirst()
                .map(PrescriptionDiagnosisRequest::getDiagnosisCode)
                .orElse(ordered.get(0).getDiagnosisCode());
        }
        return request.getDiagnosisCode();
    }

    /**
     * Copies the diagnoses list and applies optional {@code sequenceOrder} sorting (FR-P1.4a).
     * When every {@code sequenceOrder} is null, original list order is kept.
     */
    private static List<PrescriptionDiagnosisRequest> orderedDiagnosisRequests(PrescriptionRequest request) {
        List<PrescriptionDiagnosisRequest> copy = new ArrayList<>(request.getDiagnoses());
        boolean anySequence = copy.stream().anyMatch(d -> d.getSequenceOrder() != null);
        if (anySequence) {
            copy.sort(Comparator.comparing(
                    d -> d.getSequenceOrder() != null ? d.getSequenceOrder() : Integer.MAX_VALUE));
        }
        return copy;
    }

    /**
     * Build {@link PrescriptionDiagnosis} entities from the request, enforcing FR-P1.4a rules:
     * <ul>
     *   <li>Maximum 12 diagnoses per prescription.</li>
     *   <li>No duplicate ICD-10 codes within a single prescription.</li>
     *   <li>Exactly one entry may be flagged as primary; if none is flagged, the first
     *       entry in sequence is promoted automatically.</li>
     * </ul>
     * All hard rule violations throw {@link UnprocessableEntityException} with HTTP 422 and a
     * machine-readable {@code code} before the prescription is persisted (Req-J1).
     */
    private List<PrescriptionDiagnosis> buildDiagnosisEntities(PrescriptionRequest request, Prescription prescription) {
        List<PrescriptionDiagnosis> entities = new ArrayList<>();

        if (request.getDiagnoses() != null && !request.getDiagnoses().isEmpty()) {
            List<PrescriptionDiagnosisRequest> diagnoses = orderedDiagnosisRequests(request);

            // FR-P1.4a / Req-J1: maximum 12 diagnoses per prescription
            if (diagnoses.size() > 12) {
                throw new UnprocessableEntityException(
                        "A prescription may contain at most 12 ICD-10 diagnoses; " + diagnoses.size() + " were submitted.",
                        "DIAGNOSIS_LIMIT_EXCEEDED");
            }

            // FR-P1.4a / Req-J1: duplicate ICD-10 codes are not permitted within a prescription
            Set<String> seen = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            for (PrescriptionDiagnosisRequest dr : diagnoses) {
                String code = dr.getDiagnosisCode();
                if (code != null && !seen.add(code.trim().toUpperCase())) {
                    duplicates.add(code);
                }
            }
            if (!duplicates.isEmpty()) {
                throw new UnprocessableEntityException(
                        "Duplicate ICD-10 diagnosis code(s) within a single prescription are not permitted: "
                                + duplicates,
                        "DUPLICATE_DIAGNOSIS_CODE");
            }

            // FR-P1.4a / Req-J1: at most one explicit primary; if none, first in ordered list is implicit primary
            long primaryCount = diagnoses.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()))
                    .count();
            if (primaryCount > 1) {
                throw new UnprocessableEntityException(
                        "Exactly one diagnosis may be marked as primary; " + primaryCount
                                + " were submitted with isPrimary=true. Remove the flag from all but one entry.",
                        "MULTIPLE_PRIMARY_DIAGNOSES");
            }
            boolean anyPrimary = primaryCount == 1;

            for (int i = 0; i < diagnoses.size(); i++) {
                PrescriptionDiagnosisRequest dr = diagnoses.get(i);
                boolean isPrimary = Boolean.TRUE.equals(dr.getIsPrimary()) || (!anyPrimary && i == 0);
                entities.add(PrescriptionDiagnosis.builder()
                    .prescription(prescription)
                    .diagnosisCode(dr.getDiagnosisCode())
                    .diagnosisDescription(dr.getDiagnosisDescription())
                    .isPrimary(isPrimary)
                    .sequenceOrder(i)
                    .build());
            }
        } else if (request.getDiagnosisCode() != null && !request.getDiagnosisCode().isBlank()) {
            entities.add(PrescriptionDiagnosis.builder()
                .prescription(prescription)
                .diagnosisCode(request.getDiagnosisCode())
                .isPrimary(true)
                .sequenceOrder(0)
                .build());
        }

        return entities;
    }

    private PrescriptionDiagnosisResponse mapDiagnosisToResponse(PrescriptionDiagnosis d) {
        return PrescriptionDiagnosisResponse.builder()
            .id(d.getId())
            .diagnosisCode(d.getDiagnosisCode())
            .diagnosisDescription(d.getDiagnosisDescription())
            .isPrimary(d.getIsPrimary())
            .sequenceOrder(d.getSequenceOrder())
            .createdAt(d.getCreatedAt())
            .build();
    }

    /**
     * Extracts the last name from a full prescriber name string for FR-P1.10 DEA second-letter validation.
     *
     * <p>Handles two common formats:
     * <ul>
     *   <li>"Last, First [MI]" — last name is everything before the first comma</li>
     *   <li>"First [MI] Last"  — last name is the final whitespace-delimited token</li>
     * </ul>
     * Returns {@code null} when the input is null/blank so callers can safely skip the check.
     */
    static String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String trimmed = fullName.trim();
        int commaIdx = trimmed.indexOf(',');
        if (commaIdx > 0) {
            return trimmed.substring(0, commaIdx).trim();
        }
        String[] parts = trimmed.split("\\s+");
        return parts[parts.length - 1];
    }
}
