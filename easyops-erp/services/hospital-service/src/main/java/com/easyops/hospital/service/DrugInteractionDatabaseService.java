package com.easyops.hospital.service;

import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.entity.PatientProblem;
import com.easyops.hospital.entity.PrescriptionInteraction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * Service for integrating with external drug interaction databases
 * Supports integration with DrugBank, Micromedex, and other drug interaction APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrugInteractionDatabaseService {
    
    private final RestTemplate restTemplate;
    
    @Value("${drug-interaction.database.enabled:false}")
    private boolean databaseIntegrationEnabled;
    
    @Value("${drug-interaction.database.provider:DRUGBANK}")
    private String databaseProvider; // DRUGBANK, MICROMEDEX, CUSTOM
    
    @Value("${drug-interaction.database.endpoint:}")
    private String databaseEndpoint;
    
    @Value("${drug-interaction.database.api-key:}")
    private String databaseApiKey;
    
    @Value("${drug-interaction.database.timeout:30000}")
    private int databaseTimeout;
    
    @Value("${drug-interaction.database.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${drug-interaction.database.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${drug-interaction.database.retry.delay:5000}")
    private long retryDelay;
    
    /**
     * Check for drug-drug interactions
     */
    public List<DrugInteractionResult> checkDrugDrugInteractions(
            String medicationCode1, String medicationName1,
            String medicationCode2, String medicationName2) {
        
        log.info("Checking drug-drug interaction: {} vs {}", medicationName1, medicationName2);
        
        if (!databaseIntegrationEnabled || databaseEndpoint == null || databaseEndpoint.isEmpty()) {
            log.warn("Drug interaction database integration is disabled. Using enhanced local checking.");
            return checkDrugDrugInteractionsLocal(medicationCode1, medicationName1, medicationCode2, medicationName2);
        }
        
        try {
            // Build request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("medication1", Map.of(
                    "code", medicationCode1 != null ? medicationCode1 : "",
                    "name", medicationName1
            ));
            requestPayload.put("medication2", Map.of(
                    "code", medicationCode2 != null ? medicationCode2 : "",
                    "name", medicationName2
            ));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (databaseApiKey != null && !databaseApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + databaseApiKey);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            // Make API call with retry logic
            ResponseEntity<Map> response = null;
            int attempts = 0;
            
            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            databaseEndpoint + "/interactions/drug-drug",
                            HttpMethod.POST,
                            request,
                            Map.class
                    );
                    break; // Success
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("Drug interaction API call failed, retrying (attempt {}/{})", attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Drug interaction API call interrupted", ie);
                        }
                    } else {
                        log.error("Drug interaction API call failed after {} attempts", attempts, e);
                        // Fall back to local checking
                        return checkDrugDrugInteractionsLocal(medicationCode1, medicationName1, medicationCode2, medicationName2);
                    }
                }
            }
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseDrugInteractionResponse(response.getBody(), PrescriptionInteraction.InteractionCategory.DRUG_DRUG);
            }
            
        } catch (Exception e) {
            log.error("Error checking drug-drug interactions via database", e);
        }
        
        // Fall back to local checking
        return checkDrugDrugInteractionsLocal(medicationCode1, medicationName1, medicationCode2, medicationName2);
    }
    
    /**
     * Check for drug-food interactions
     */
    public List<DrugInteractionResult> checkDrugFoodInteractions(
            String medicationCode, String medicationName, List<String> foods) {
        
        log.info("Checking drug-food interactions for medication: {}", medicationName);
        
        if (!databaseIntegrationEnabled || databaseEndpoint == null || databaseEndpoint.isEmpty()) {
            log.warn("Drug interaction database integration is disabled. Using enhanced local checking.");
            return checkDrugFoodInteractionsLocal(medicationCode, medicationName, foods);
        }
        
        try {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("medication", Map.of(
                    "code", medicationCode != null ? medicationCode : "",
                    "name", medicationName
            ));
            requestPayload.put("foods", foods != null ? foods : Collections.emptyList());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (databaseApiKey != null && !databaseApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + databaseApiKey);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    databaseEndpoint + "/interactions/drug-food",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseDrugInteractionResponse(response.getBody(), PrescriptionInteraction.InteractionCategory.DRUG_FOOD);
            }
            
        } catch (Exception e) {
            log.error("Error checking drug-food interactions via database", e);
        }
        
        return checkDrugFoodInteractionsLocal(medicationCode, medicationName, foods);
    }
    
    /**
     * Check for drug-lab test interactions
     */
    public List<DrugInteractionResult> checkDrugLabInteractions(
            String medicationCode, String medicationName, List<String> labTests) {
        
        log.info("Checking drug-lab interactions for medication: {}", medicationName);
        
        if (!databaseIntegrationEnabled || databaseEndpoint == null || databaseEndpoint.isEmpty()) {
            log.warn("Drug interaction database integration is disabled. Using enhanced local checking.");
            return checkDrugLabInteractionsLocal(medicationCode, medicationName, labTests);
        }
        
        try {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("medication", Map.of(
                    "code", medicationCode != null ? medicationCode : "",
                    "name", medicationName
            ));
            requestPayload.put("labTests", labTests != null ? labTests : Collections.emptyList());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (databaseApiKey != null && !databaseApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + databaseApiKey);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    databaseEndpoint + "/interactions/drug-lab",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseDrugInteractionResponse(response.getBody(), PrescriptionInteraction.InteractionCategory.DRUG_LAB);
            }
            
        } catch (Exception e) {
            log.error("Error checking drug-lab interactions via database", e);
        }
        
        return checkDrugLabInteractionsLocal(medicationCode, medicationName, labTests);
    }

    /**
     * Factory for FR-P1.7 clinical screening alerts (disease, dosing, pregnancy, organ function).
     */
    public DrugInteractionResult buildScreeningAlert(
            String interactingContext,
            String interactionType,
            PrescriptionInteraction.InteractionCategory category,
            PrescriptionInteraction.InteractionSeverity severity,
            PrescriptionInteraction.ClinicalSignificanceLevel clinicalSignificanceLevel,
            String description,
            String clinicalSignificance,
            String managementGuidance,
            String mechanism,
            String onsetTime,
            String evidenceLevel) {
        return DrugInteractionResult.builder()
                .interactingMedication(interactingContext)
                .interactionType(interactionType)
                .interactionCategory(category)
                .severity(severity)
                .clinicalSignificanceLevel(clinicalSignificanceLevel)
                .description(description)
                .clinicalSignificance(clinicalSignificance)
                .managementGuidance(managementGuidance)
                .mechanism(mechanism)
                .onsetTime(onsetTime)
                .evidenceLevel(evidenceLevel)
                .build();
    }

    /**
     * Age in full years from date of birth, or -1 if unknown.
     */
    public static int ageYears(Patient patient) {
        if (patient == null || patient.getDateOfBirth() == null) {
            return -1;
        }
        return Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
    }

    /**
     * Summarize active problem list for interaction context strings.
     */
    public static String summarizeProblems(List<PatientProblem> problems) {
        if (problems == null || problems.isEmpty()) {
            return "";
        }
        return problems.stream()
                .map(p -> p.getProblemName() != null ? p.getProblemName() : "")
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(8)
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }
    
    // ========== Local/Enhanced Checking Methods ==========
    
    /**
     * Enhanced local drug-drug interaction checking
     * Uses comprehensive knowledge base of known interactions
     */
    private List<DrugInteractionResult> checkDrugDrugInteractionsLocal(
            String medicationCode1, String medicationName1,
            String medicationCode2, String medicationName2) {
        
        List<DrugInteractionResult> results = new ArrayList<>();
        
        // Enhanced interaction database (expanded from basic implementation)
        // This would typically be loaded from a database or configuration file
        
        // Example: Warfarin interactions
        if (containsWarfarin(medicationName1) || containsWarfarin(medicationName2)) {
            String otherMed = containsWarfarin(medicationName1) ? medicationName2 : medicationName1;
            
            if (containsAspirin(otherMed) || containsNSAID(otherMed)) {
                results.add(createInteractionResult(
                        medicationName1, medicationName2,
                        "Increased bleeding risk",
                        PrescriptionInteraction.InteractionSeverity.MAJOR,
                        PrescriptionInteraction.ClinicalSignificanceLevel.CRITICAL,
                        "Warfarin combined with aspirin or NSAIDs significantly increases bleeding risk",
                        "Monitor INR closely. Consider alternative pain management. Patient education on bleeding signs.",
                        "Increased risk of gastrointestinal bleeding and other hemorrhagic complications",
                        "Pharmacodynamic - additive anticoagulant effects",
                        "Within days",
                        "Established",
                        PrescriptionInteraction.InteractionCategory.DRUG_DRUG
                ));
            }
        }
        
        // Example: MAOI interactions
        if (containsMAOI(medicationName1) || containsMAOI(medicationName2)) {
            String otherMed = containsMAOI(medicationName1) ? medicationName2 : medicationName1;
            
            if (containsSSRI(otherMed) || containsTriptan(otherMed)) {
                results.add(createInteractionResult(
                        medicationName1, medicationName2,
                        "Serotonin syndrome risk",
                        PrescriptionInteraction.InteractionSeverity.CONTRAINDICATED,
                        PrescriptionInteraction.ClinicalSignificanceLevel.CRITICAL,
                        "MAOIs with SSRIs or triptans can cause life-threatening serotonin syndrome",
                        "DO NOT COMBINE. Use alternative medications. Wait appropriate washout period.",
                        "Life-threatening condition with hyperthermia, autonomic instability, mental status changes",
                        "Pharmacodynamic - excessive serotonin activity",
                        "Within hours to days",
                        "Established",
                        PrescriptionInteraction.InteractionCategory.DRUG_DRUG
                ));
            }
        }
        
        // Example: Digoxin interactions
        if (containsDigoxin(medicationName1) || containsDigoxin(medicationName2)) {
            String otherMed = containsDigoxin(medicationName1) ? medicationName2 : medicationName1;
            
            if (containsAmiodarone(otherMed) || containsVerapamil(otherMed)) {
                results.add(createInteractionResult(
                        medicationName1, medicationName2,
                        "Increased digoxin levels",
                        PrescriptionInteraction.InteractionSeverity.MAJOR,
                        PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                        "These medications increase digoxin levels, increasing risk of toxicity",
                        "Monitor digoxin levels. Reduce digoxin dose if necessary. Watch for signs of toxicity.",
                        "Nausea, vomiting, arrhythmias, visual disturbances",
                        "Pharmacokinetic - reduced digoxin clearance",
                        "Within days to weeks",
                        "Established",
                        PrescriptionInteraction.InteractionCategory.DRUG_DRUG
                ));
            }
        }
        
        return results;
    }
    
    /**
     * Enhanced local drug-food interaction checking
     */
    private List<DrugInteractionResult> checkDrugFoodInteractionsLocal(
            String medicationCode, String medicationName, List<String> foods) {
        
        List<DrugInteractionResult> results = new ArrayList<>();
        
        if (foods == null || foods.isEmpty()) {
            // Check common food interactions
            foods = Arrays.asList("Grapefruit", "Alcohol", "Dairy", "High-fat meal", "Caffeine");
        }
        
        // Warfarin and Vitamin K foods
        if (containsWarfarin(medicationName)) {
            for (String food : foods) {
                if (containsVitaminK(food)) {
                    results.add(createInteractionResult(
                            medicationName, food,
                            "Reduced warfarin effectiveness",
                            PrescriptionInteraction.InteractionSeverity.MODERATE,
                            PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                            "Foods high in vitamin K can reduce warfarin's anticoagulant effect",
                            "Maintain consistent vitamin K intake. Monitor INR. Patient education on dietary consistency.",
                            "Reduced anticoagulation, increased risk of thrombosis",
                            "Pharmacodynamic - vitamin K antagonizes warfarin",
                            "Within days",
                            "Established",
                            PrescriptionInteraction.InteractionCategory.DRUG_FOOD
                    ));
                }
            }
        }
        
        // Grapefruit interactions
        for (String food : foods) {
            if (food.toLowerCase().contains("grapefruit")) {
                if (containsStatins(medicationName) || containsCalciumChannelBlockers(medicationName)) {
                    results.add(createInteractionResult(
                            medicationName, food,
                            "Increased drug levels",
                            PrescriptionInteraction.InteractionSeverity.MAJOR,
                            PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                            "Grapefruit inhibits CYP3A4, increasing drug levels and risk of toxicity",
                            "Avoid grapefruit and grapefruit juice. Use alternative fruits if needed.",
                            "Increased risk of side effects and toxicity",
                            "Pharmacokinetic - CYP3A4 inhibition",
                            "Within hours",
                            "Established",
                            PrescriptionInteraction.InteractionCategory.DRUG_FOOD
                    ));
                }
            }
        }
        
        // Alcohol interactions
        for (String food : foods) {
            if (food.toLowerCase().contains("alcohol")) {
                if (containsBenzodiazepines(medicationName) || containsOpioids(medicationName) || containsAntihistamines(medicationName)) {
                    results.add(createInteractionResult(
                            medicationName, food,
                            "Enhanced CNS depression",
                            PrescriptionInteraction.InteractionSeverity.MAJOR,
                            PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                            "Alcohol enhances CNS depressant effects, increasing risk of respiratory depression",
                            "Avoid alcohol consumption. Patient education on risks. Monitor for excessive sedation.",
                            "Drowsiness, dizziness, impaired coordination, respiratory depression",
                            "Pharmacodynamic - additive CNS depression",
                            "Within hours",
                            "Established",
                            PrescriptionInteraction.InteractionCategory.DRUG_FOOD
                    ));
                }
            }
        }
        
        return results;
    }
    
    /**
     * Enhanced local drug-lab interaction checking
     */
    private List<DrugInteractionResult> checkDrugLabInteractionsLocal(
            String medicationCode, String medicationName, List<String> labTests) {
        
        List<DrugInteractionResult> results = new ArrayList<>();
        
        if (labTests == null || labTests.isEmpty()) {
            // Check common lab test interactions
            labTests = Arrays.asList("INR", "PT", "Creatinine", "Liver Function Tests", "Glucose", "Cholesterol");
        }
        
        // Warfarin and INR/PT
        if (containsWarfarin(medicationName)) {
            for (String labTest : labTests) {
                if (labTest.equalsIgnoreCase("INR") || labTest.equalsIgnoreCase("PT")) {
                    results.add(createInteractionResult(
                            medicationName, labTest,
                            "Warfarin affects coagulation tests",
                            PrescriptionInteraction.InteractionSeverity.MINOR,
                            PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                            "Warfarin is expected to increase INR and PT - this is therapeutic, not an interaction",
                            "Monitor INR regularly. Adjust warfarin dose to maintain therapeutic range.",
                            "Expected therapeutic effect on coagulation",
                            "Pharmacodynamic - intended anticoagulant effect",
                            "Ongoing",
                        "Established",
                        PrescriptionInteraction.InteractionCategory.DRUG_LAB
                    ));
                }
            }
        }
        
        // ACE inhibitors and creatinine
        if (containsACEInhibitor(medicationName)) {
            for (String labTest : labTests) {
                if (labTest.equalsIgnoreCase("Creatinine")) {
                    results.add(createInteractionResult(
                            medicationName, labTest,
                            "ACE inhibitors may increase creatinine",
                            PrescriptionInteraction.InteractionSeverity.MINOR,
                            PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                            "ACE inhibitors can cause mild, reversible increases in serum creatinine",
                            "Monitor creatinine levels. Small increases are expected and usually not concerning.",
                            "Mild elevation in serum creatinine",
                            "Pharmacodynamic - reduced glomerular filtration",
                            "Within days to weeks",
                            "Established",
                            PrescriptionInteraction.InteractionCategory.DRUG_LAB
                    ));
                }
            }
        }
        
        return results;
    }
    
    // ========== Helper Methods ==========
    
    private DrugInteractionResult createInteractionResult(
            String medication1, String medication2,
            String interactionType,
            PrescriptionInteraction.InteractionSeverity severity,
            PrescriptionInteraction.ClinicalSignificanceLevel clinicalSignificance,
            String description,
            String managementGuidance,
            String clinicalSignificanceText,
            String mechanism,
            String onsetTime,
            String evidenceLevel,
            PrescriptionInteraction.InteractionCategory category) {
        
        return DrugInteractionResult.builder()
                .interactingMedication(medication2)
                .interactionType(interactionType)
                .interactionCategory(category)
                .severity(severity)
                .clinicalSignificanceLevel(clinicalSignificance)
                .description(description)
                .clinicalSignificance(clinicalSignificanceText)
                .managementGuidance(managementGuidance)
                .mechanism(mechanism)
                .onsetTime(onsetTime)
                .evidenceLevel(evidenceLevel)
                .build();
    }
    
    private List<DrugInteractionResult> parseDrugInteractionResponse(
            Map<String, Object> responseBody, PrescriptionInteraction.InteractionCategory category) {
        
        List<DrugInteractionResult> results = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> interactions = (List<Map<String, Object>>) responseBody.get("interactions");
        
        if (interactions != null) {
            for (Map<String, Object> interaction : interactions) {
                DrugInteractionResult result = DrugInteractionResult.builder()
                        .interactingMedication((String) interaction.get("interactingMedication"))
                        .interactionType((String) interaction.get("interactionType"))
                        .interactionCategory(category)
                        .severity(parseSeverity((String) interaction.get("severity")))
                        .clinicalSignificanceLevel(parseClinicalSignificance((String) interaction.get("clinicalSignificanceLevel")))
                        .description((String) interaction.get("description"))
                        .clinicalSignificance((String) interaction.get("clinicalSignificance"))
                        .managementGuidance((String) interaction.get("managementGuidance"))
                        .mechanism((String) interaction.get("mechanism"))
                        .onsetTime((String) interaction.get("onsetTime"))
                        .evidenceLevel((String) interaction.get("evidenceLevel"))
                        .build();
                results.add(result);
            }
        }
        
        return results;
    }
    
    private PrescriptionInteraction.InteractionSeverity parseSeverity(String severityStr) {
        if (severityStr == null) return PrescriptionInteraction.InteractionSeverity.UNKNOWN;
        try {
            return PrescriptionInteraction.InteractionSeverity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PrescriptionInteraction.InteractionSeverity.UNKNOWN;
        }
    }
    
    private PrescriptionInteraction.ClinicalSignificanceLevel parseClinicalSignificance(String levelStr) {
        if (levelStr == null) return PrescriptionInteraction.ClinicalSignificanceLevel.UNKNOWN;
        try {
            return PrescriptionInteraction.ClinicalSignificanceLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PrescriptionInteraction.ClinicalSignificanceLevel.UNKNOWN;
        }
    }
    
    // Medication detection helpers
    private boolean containsWarfarin(String medicationName) {
        return medicationName != null && medicationName.toLowerCase().contains("warfarin");
    }
    
    private boolean containsAspirin(String medicationName) {
        return medicationName != null && medicationName.toLowerCase().contains("aspirin");
    }
    
    private boolean containsNSAID(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("ibuprofen") || lower.contains("naproxen") || 
               lower.contains("diclofenac") || lower.contains("indomethacin");
    }
    
    private boolean containsMAOI(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("maoi") || lower.contains("phenelzine") || 
               lower.contains("tranylcypromine") || lower.contains("isocarboxazid");
    }
    
    private boolean containsSSRI(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("fluoxetine") || lower.contains("sertraline") || 
               lower.contains("paroxetine") || lower.contains("citalopram") || 
               lower.contains("escitalopram");
    }
    
    private boolean containsTriptan(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("sumatriptan") || lower.contains("rizatriptan") || 
               lower.contains("eletriptan") || lower.contains("zolmitriptan");
    }
    
    private boolean containsDigoxin(String medicationName) {
        return medicationName != null && medicationName.toLowerCase().contains("digoxin");
    }
    
    private boolean containsAmiodarone(String medicationName) {
        return medicationName != null && medicationName.toLowerCase().contains("amiodarone");
    }
    
    private boolean containsVerapamil(String medicationName) {
        return medicationName != null && medicationName.toLowerCase().contains("verapamil");
    }
    
    private boolean containsStatins(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("atorvastatin") || lower.contains("simvastatin") || 
               lower.contains("lovastatin") || lower.contains("rosuvastatin");
    }
    
    private boolean containsCalciumChannelBlockers(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("nifedipine") || lower.contains("felodipine") || 
               lower.contains("amlodipine") || lower.contains("diltiazem");
    }
    
    private boolean containsBenzodiazepines(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("diazepam") || lower.contains("lorazepam") || 
               lower.contains("alprazolam") || lower.contains("clonazepam");
    }
    
    private boolean containsOpioids(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("morphine") || lower.contains("oxycodone") || 
               lower.contains("hydrocodone") || lower.contains("codeine") || 
               lower.contains("fentanyl");
    }
    
    private boolean containsAntihistamines(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("diphenhydramine") || lower.contains("doxylamine") || 
               lower.contains("promethazine");
    }
    
    private boolean containsVitaminK(String food) {
        if (food == null) return false;
        String lower = food.toLowerCase();
        return lower.contains("kale") || lower.contains("spinach") || 
               lower.contains("broccoli") || lower.contains("brussels") || 
               lower.contains("cabbage") || lower.contains("lettuce");
    }
    
    private boolean containsACEInhibitor(String medicationName) {
        if (medicationName == null) return false;
        String lower = medicationName.toLowerCase();
        return lower.contains("lisinopril") || lower.contains("enalapril") || 
               lower.contains("ramipril") || lower.contains("captopril");
    }
    
    /**
     * Result class for drug interaction checking
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DrugInteractionResult {
        private String interactingMedication;
        private String interactionType;
        private PrescriptionInteraction.InteractionCategory interactionCategory;
        private PrescriptionInteraction.InteractionSeverity severity;
        private PrescriptionInteraction.ClinicalSignificanceLevel clinicalSignificanceLevel;
        private String description;
        private String clinicalSignificance;
        private String managementGuidance;
        private String mechanism;
        private String onsetTime;
        private String evidenceLevel;
    }
}
