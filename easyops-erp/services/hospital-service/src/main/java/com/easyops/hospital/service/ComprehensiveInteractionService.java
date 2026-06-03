package com.easyops.hospital.service;

import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive drug interaction checking service
 * Handles drug-drug, drug-food, drug-lab, and other interaction types
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveInteractionService {
    
    private final DrugInteractionDatabaseService drugInteractionDatabaseService;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedicationRepository prescriptionMedicationRepository;
    private final PrescriptionInteractionRepository prescriptionInteractionRepository;
    private final LabResultRepository labResultRepository;
    private final PatientProblemRepository patientProblemRepository;
    private final ClinicalMedicationSafetyService clinicalMedicationSafetyService;
    
    /**
     * Comprehensive interaction check for a prescription
     */
    @Transactional
    public List<PrescriptionInteraction> checkComprehensiveInteractions(UUID prescriptionId) {
        log.info("Performing comprehensive interaction check for prescription: {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + prescriptionId));
        
        // Delete existing (non-acknowledged) interaction rows to avoid duplicates on re-check.
        // Acknowledged rows are preserved for audit compliance — only unacknowledged ones are refreshed.
        List<PrescriptionInteraction> existing =
                prescriptionInteractionRepository.findByPrescriptionPrescriptionId(prescriptionId);
        List<PrescriptionInteraction> unacknowledged = existing.stream()
                .filter(ix -> !Boolean.TRUE.equals(ix.getIsAcknowledged()))
                .collect(Collectors.toList());
        if (!unacknowledged.isEmpty()) {
            prescriptionInteractionRepository.deleteAll(unacknowledged);
            prescriptionInteractionRepository.flush();
        }

        List<PrescriptionInteraction> allInteractions = new ArrayList<>();
        
        // 1. Drug-Drug Interactions
        List<PrescriptionInteraction> drugDrugInteractions = checkDrugDrugInteractions(prescription);
        allInteractions.addAll(drugDrugInteractions);
        
        // 2. Drug-Food Interactions
        List<PrescriptionInteraction> drugFoodInteractions = checkDrugFoodInteractions(prescription);
        allInteractions.addAll(drugFoodInteractions);
        
        // 3. Drug-Lab Interactions
        List<PrescriptionInteraction> drugLabInteractions = checkDrugLabInteractions(prescription);
        allInteractions.addAll(drugLabInteractions);
        
        // 4. FR-P1.7 — drug–disease, pregnancy/lactation display, age/weight heuristics, renal/hepatic alerts
        List<PrescriptionInteraction> frP17Interactions = checkFrP17ClinicalSafety(prescription);
        allInteractions.addAll(frP17Interactions);
        
        // Save all interactions
        if (!allInteractions.isEmpty()) {
            prescriptionInteractionRepository.saveAll(allInteractions);
            prescription.setHasInteractions(true);
        } else {
            prescription.setHasInteractions(false);
        }
        
        prescriptionRepository.save(prescription);
        
        log.info("Found {} interactions for prescription: {}", allInteractions.size(), prescriptionId);
        return allInteractions;
    }
    
    /**
     * Check drug-drug interactions
     */
    private List<PrescriptionInteraction> checkDrugDrugInteractions(Prescription prescription) {
        List<PrescriptionInteraction> interactions = new ArrayList<>();
        
        // Get active prescriptions for the patient
        List<Prescription> activePrescriptions = prescriptionRepository.findActivePrescriptionsByPatient(
                prescription.getPatient().getPatientId());
        
        // Remove current prescription from list
        activePrescriptions = activePrescriptions.stream()
                .filter(p -> !p.getPrescriptionId().equals(prescription.getPrescriptionId()))
                .collect(Collectors.toList());
        
        List<PrescriptionMedication> newMeds = loadMeds(prescription);
        for (Prescription activePrescription : activePrescriptions) {
            List<PrescriptionMedication> activeMeds = loadMeds(activePrescription);
            for (PrescriptionMedication nm : newMeds) {
                for (PrescriptionMedication am : activeMeds) {
                    List<DrugInteractionDatabaseService.DrugInteractionResult> results =
                            drugInteractionDatabaseService.checkDrugDrugInteractions(
                                    nm.getMedicationCode(),
                                    nm.getMedicationName(),
                                    am.getMedicationCode(),
                                    am.getMedicationName()
                            );

                    for (DrugInteractionDatabaseService.DrugInteractionResult result : results) {
                        PrescriptionInteraction interaction = mapToPrescriptionInteraction(
                                prescription, result, am.getMedicationName(),
                                am.getMedicationCode());
                        interactions.add(interaction);
                    }
                }
            }
        }
        
        return interactions;
    }

    private List<PrescriptionMedication> loadMeds(Prescription p) {
        if (p.getMedications() != null && !p.getMedications().isEmpty()) {
            return p.getMedications().stream()
                    .sorted(Comparator.comparing(PrescriptionMedication::getLineNumber))
                    .collect(Collectors.toList());
        }
        return prescriptionMedicationRepository.findByPrescriptionPrescriptionIdOrderByLineNumberAsc(p.getPrescriptionId());
    }
    
    /**
     * Check drug-food interactions for every line item on the prescription.
     * Previously this only used the denormalized header fields (medicationCode / medicationName)
     * which meant line items 2+ were silently skipped for food-interaction checks.
     */
    private List<PrescriptionInteraction> checkDrugFoodInteractions(Prescription prescription) {
        List<PrescriptionInteraction> interactions = new ArrayList<>();
        
        // Common foods to check (in production this would come from patient dietary preferences)
        List<String> commonFoods = Arrays.asList(
                "Grapefruit", "Grapefruit Juice", "Alcohol", "Dairy Products",
                "High-fat meal", "Caffeine", "Green leafy vegetables", "Vitamin K rich foods"
        );
        
        for (PrescriptionMedication med : loadMeds(prescription)) {
            List<DrugInteractionDatabaseService.DrugInteractionResult> results =
                    drugInteractionDatabaseService.checkDrugFoodInteractions(
                            med.getMedicationCode(),
                            med.getMedicationName(),
                            commonFoods
                    );

            for (DrugInteractionDatabaseService.DrugInteractionResult result : results) {
                PrescriptionInteraction interaction = mapToPrescriptionInteraction(
                        prescription, result, result.getInteractingMedication(), null);
                interaction.setInteractingSubstance(result.getInteractingMedication());
                interaction.setInteractingSubstanceType("FOOD");
                interactions.add(interaction);
            }
        }
        
        return interactions;
    }
    
    /**
     * Check drug-lab test interactions
     */
    private List<PrescriptionInteraction> checkDrugLabInteractions(Prescription prescription) {
        List<PrescriptionInteraction> interactions = new ArrayList<>();
        
        // Get recent lab tests for the patient
        List<LabResult> allLabResults = labResultRepository
                .findByPatientPatientIdOrderByResultDateDesc(prescription.getPatient().getPatientId());
        
        List<LabResult> recentLabResults = allLabResults.stream()
                .limit(20) // Check last 20 lab results
                .collect(Collectors.toList());
        
        List<String> labTests = recentLabResults.stream()
                .map(LabResult::getTestName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // Add common lab tests if none found
        if (labTests.isEmpty()) {
            labTests = Arrays.asList("INR", "PT", "Creatinine", "Liver Function Tests", 
                    "Glucose", "Cholesterol", "Complete Blood Count");
        }
        
        for (PrescriptionMedication nm : loadMeds(prescription)) {
            List<DrugInteractionDatabaseService.DrugInteractionResult> results =
                    drugInteractionDatabaseService.checkDrugLabInteractions(
                            nm.getMedicationCode(),
                            nm.getMedicationName(),
                            labTests
                    );

            for (DrugInteractionDatabaseService.DrugInteractionResult result : results) {
                PrescriptionInteraction interaction = mapToPrescriptionInteraction(
                        prescription, result, result.getInteractingMedication(), null);
                interaction.setInteractingSubstance(result.getInteractingMedication());
                interaction.setInteractingSubstanceType("LAB_TEST");
                interactions.add(interaction);
            }
        }
        
        return interactions;
    }
    
    /**
     * FR-P1.7 clinical safety using patient problem list, labs, and structured heuristics.
     */
    private List<PrescriptionInteraction> checkFrP17ClinicalSafety(Prescription prescription) {
        List<PrescriptionInteraction> interactions = new ArrayList<>();
        Patient patient = prescription.getPatient();
        List<PatientProblem> problems = patientProblemRepository.findCurrentProblemsByPatient(patient.getPatientId());
        List<LabResult> labs = labResultRepository.findByPatientPatientIdOrderByResultDateDesc(patient.getPatientId());
        if (labs.size() > 40) {
            labs = new ArrayList<>(labs.subList(0, 40));
        }

        for (PrescriptionMedication nm : loadMeds(prescription)) {
            List<DrugInteractionDatabaseService.DrugInteractionResult> results =
                    clinicalMedicationSafetyService.evaluateForPrescriptionLine(
                            patient,
                            nm.getMedicationName(),
                            nm.getDosageStrength(),
                            nm.getDosageUnit(),
                            problems,
                            labs);
            for (DrugInteractionDatabaseService.DrugInteractionResult result : results) {
                interactions.add(mapToPrescriptionInteraction(
                        prescription, result, result.getInteractingMedication(), null));
            }
        }
        return interactions;
    }
    
    /**
     * Map DrugInteractionResult to PrescriptionInteraction entity
     */
    private PrescriptionInteraction mapToPrescriptionInteraction(
            Prescription prescription,
            DrugInteractionDatabaseService.DrugInteractionResult result,
            String interactingMedication,
            String interactingMedicationCode) {
        
        return PrescriptionInteraction.builder()
                .prescription(prescription)
                .interactingMedication(interactingMedication)
                .interactingMedicationCode(interactingMedicationCode)
                .interactionType(result.getInteractionType())
                .interactionCategory(result.getInteractionCategory())
                .severity(result.getSeverity())
                .clinicalSignificanceLevel(result.getClinicalSignificanceLevel())
                .description(result.getDescription())
                .clinicalSignificance(result.getClinicalSignificance())
                .actionRequired(generateActionRequired(result))
                .managementGuidance(result.getManagementGuidance())
                .mechanism(result.getMechanism())
                .onsetTime(result.getOnsetTime())
                .evidenceLevel(result.getEvidenceLevel())
                .isAcknowledged(false)
                .build();
    }
    
    /**
     * Generate action required based on severity and clinical significance
     */
    private String generateActionRequired(DrugInteractionDatabaseService.DrugInteractionResult result) {
        StringBuilder action = new StringBuilder();
        
        switch (result.getSeverity()) {
            case CONTRAINDICATED:
                action.append("DO NOT PRESCRIBE. Use alternative medication. ");
                break;
            case MAJOR:
                action.append("REQUIRES CLOSE MONITORING. Consider alternative or adjust therapy. ");
                break;
            case MODERATE:
                action.append("MONITOR PATIENT. Consider dose adjustment or monitoring. ");
                break;
            case MINOR:
                action.append("MONITOR AS NEEDED. Usually manageable with appropriate monitoring. ");
                break;
            default:
                action.append("REVIEW INTERACTION. ");
        }
        
        if (result.getManagementGuidance() != null && !result.getManagementGuidance().isEmpty()) {
            action.append(result.getManagementGuidance());
        }
        
        return action.toString();
    }
}
