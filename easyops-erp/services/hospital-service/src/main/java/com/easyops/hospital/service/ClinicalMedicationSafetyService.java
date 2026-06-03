package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DrugInteractionCheckRequest;
import com.easyops.hospital.entity.LabResult;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.entity.PatientProblem;
import com.easyops.hospital.entity.PrescriptionInteraction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * FR-P1.7 clinical medication safety: drug–disease rules, age/weight heuristics,
 * renal/hepatic adjustment alerts, and pregnancy/lactation category display.
 * <p>
 * Rules are evidence-inspired heuristics for screening — not a substitute for
 * a licensed drug database (Micromedex, DrugBank API, etc.) when integrated.
 */
@Service
@RequiredArgsConstructor
public class ClinicalMedicationSafetyService {

    private final DrugInteractionDatabaseService drugInteractionDatabaseService;

    public List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateForAdHocCheck(
            Patient patient,
            String medicationName,
            String medicationCode,
            List<PatientProblem> problems,
            List<LabResult> recentLabs,
            DrugInteractionCheckRequest request) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        if (medicationName == null || medicationName.isBlank()) {
            return out;
        }
        String name = medicationName;
        List<PatientProblem> safeProblems = problems != null ? problems : List.of();

        out.addAll(evaluateDrugDisease(name, safeProblems));
        out.addAll(evaluatePregnancyLactation(name, safeProblems, request));
        out.addAll(evaluatePediatricGeriatricDosing(name, patient));
        out.addAll(evaluateWeightBasedDosing(name, request));
        out.addAll(evaluateRenalHepatic(name, safeProblems, recentLabs, request));
        return out;
    }

    /**
     * Per-medication evaluation for comprehensive prescription screening.
     */
    public List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateForPrescriptionLine(
            Patient patient,
            String medicationName,
            BigDecimal doseStrength,
            String doseUnit,
            List<PatientProblem> problems,
            List<LabResult> recentLabs) {

        DrugInteractionCheckRequest empty = DrugInteractionCheckRequest.builder().build();
        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        if (medicationName == null || medicationName.isBlank()) {
            return out;
        }
        out.addAll(evaluateDrugDisease(medicationName, problems != null ? problems : List.of()));
        out.addAll(evaluatePregnancyLactation(medicationName,
                problems != null ? problems : List.of(), empty));
        out.addAll(evaluatePediatricGeriatricDosing(medicationName, patient));
        if (doseStrength != null && doseStrength.compareTo(BigDecimal.ZERO) > 0) {
            out.addAll(evaluateWeightBasedDosingWithDose(medicationName, doseStrength, doseUnit, null));
        }
        out.addAll(evaluateRenalHepatic(medicationName, problems != null ? problems : List.of(),
                recentLabs != null ? recentLabs : List.of(), empty));
        return out;
    }

    // --- Drug–disease ---

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateDrugDisease(
            String medicationName, List<PatientProblem> problems) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        if (problems.isEmpty()) {
            return out;
        }
        String lower = medicationName.toLowerCase(Locale.ROOT);
        boolean ckd = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesCkd);
        boolean liver = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesLiverDisease);
        boolean asthma = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesAsthma);

        // NSAIDs + CKD
        if (ckd && (containsNsaids(lower))) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    DrugInteractionDatabaseService.summarizeProblems(problems),
                    "NSAID use with chronic kidney disease",
                    PrescriptionInteraction.InteractionCategory.DRUG_DISEASE,
                    PrescriptionInteraction.InteractionSeverity.MAJOR,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "NSAIDs can reduce renal perfusion and worsen kidney function in CKD.",
                    "Acute kidney injury, fluid retention, hypertension",
                    "Prefer acetaminophen for analgesia when appropriate; if NSAID unavoidable, use lowest dose/shortest duration and monitor renal function.",
                    "Hemodynamic effect on afferent arterioles; prostaglandin inhibition",
                    "Days to weeks",
                    "Established — screening heuristic"));
        }

        // Metformin + significant renal impairment (problem-based)
        if (ckd && lower.contains("metformin")) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    DrugInteractionDatabaseService.summarizeProblems(problems),
                    "Metformin in renal impairment",
                    PrescriptionInteraction.InteractionCategory.DRUG_DISEASE,
                    PrescriptionInteraction.InteractionSeverity.MAJOR,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "Metformin is contraindicated or should be avoided when eGFR is severely reduced (institutional thresholds apply).",
                    "Lactic acidosis risk",
                    "Verify eGFR/creatinine; hold metformin around iodinated contrast per protocol.",
                    "Reduced renal clearance",
                    "Ongoing",
                    "Established — screening heuristic"));
        }

        // Beta-blockers non-selective + asthma/COPD reactive airway (simplified)
        if (asthma && (lower.contains("propranolol") || lower.contains("nadolol"))) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    DrugInteractionDatabaseService.summarizeProblems(problems),
                    "Non-selective beta-blocker with reactive airway disease",
                    PrescriptionInteraction.InteractionCategory.DRUG_DISEASE,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                    "Non-selective beta-blockers may provoke bronchospasm.",
                    "Bronchospasm, respiratory compromise",
                    "Consider cardioselective beta-blocker if appropriate; monitor symptoms.",
                    "Beta-2 blockade in bronchial smooth muscle",
                    "Hours to days",
                    "Established — screening heuristic"));
        }

        // High alcohol / cirrhosis + acetaminophen high-dose concern (light touch)
        if (liver && (lower.contains("acetaminophen") || lower.contains("paracetamol"))) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    DrugInteractionDatabaseService.summarizeProblems(problems),
                    "Acetaminophen in hepatic disease",
                    PrescriptionInteraction.InteractionCategory.DRUG_DISEASE,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "Reduced hepatic reserve increases risk of hepatotoxicity at high cumulative doses.",
                    "Hepatotoxicity",
                    "Limit total daily dose per hepatic guidance; avoid alcohol co-use.",
                    "Reduced glucuronidation / glutathione stores",
                    "Cumulative",
                    "Established — screening heuristic"));
        }

        return out;
    }

    private static boolean containsNsaids(String lowerName) {
        return lowerName.contains("ibuprofen") || lowerName.contains("naproxen")
                || lowerName.contains("diclofenac") || lowerName.contains("indomethacin")
                || lowerName.contains("ketorolac") || lowerName.contains("celecoxib");
    }

    private static boolean problemIndicatesCkd(PatientProblem p) {
        String code = Optional.ofNullable(p.getIcd10Code()).orElse("");
        String n = Optional.ofNullable(p.getProblemName()).orElse("").toLowerCase(Locale.ROOT);
        return code.startsWith("N18") || code.startsWith("N19")
                || n.contains("chronic kidney") || n.contains("ckd") || n.contains("renal failure")
                || n.contains("dialysis") || n.contains("kidney disease");
    }

    private static boolean problemIndicatesLiverDisease(PatientProblem p) {
        String code = Optional.ofNullable(p.getIcd10Code()).orElse("");
        String n = Optional.ofNullable(p.getProblemName()).orElse("").toLowerCase(Locale.ROOT);
        return code.startsWith("K70") || code.startsWith("K74") || code.startsWith("K75")
                || n.contains("cirrhosis") || n.contains("liver disease") || n.contains("hepatitis");
    }

    private static boolean problemIndicatesAsthma(PatientProblem p) {
        String code = Optional.ofNullable(p.getIcd10Code()).orElse("");
        String n = Optional.ofNullable(p.getProblemName()).orElse("").toLowerCase(Locale.ROOT);
        return code.startsWith("J45") || n.contains("asthma") || n.contains("reactive airway");
    }

    private static boolean problemIndicatesPregnancy(PatientProblem p) {
        String code = Optional.ofNullable(p.getIcd10Code()).orElse("");
        String n = Optional.ofNullable(p.getProblemName()).orElse("").toLowerCase(Locale.ROOT);
        if (code.startsWith("O") && code.length() >= 3 && code.charAt(1) >= '0' && code.charAt(1) <= '9') {
            return true;
        }
        return n.contains("pregnant") || n.contains("pregnancy") || n.contains("gravida");
    }

    // --- Pregnancy / lactation (simplified category display) ---

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluatePregnancyLactation(
            String medicationName,
            List<PatientProblem> problems,
            DrugInteractionCheckRequest request) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        String lower = medicationName.toLowerCase(Locale.ROOT);

        boolean pregnant = request != null && request.getPregnancyStatus() == DrugInteractionCheckRequest.PregnancyStatus.PREGNANT;
        boolean possible = request != null && request.getPregnancyStatus() == DrugInteractionCheckRequest.PregnancyStatus.POSSIBLE;
        boolean fromProblem = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesPregnancy);
        boolean lact = request != null && Boolean.TRUE.equals(request.getLactating());

        if (!pregnant && !possible && !fromProblem && !lact) {
            return out;
        }

        String category = inferPregnancyCategory(lower);
        String lactNote = lact
                ? " Lactation: verify infant exposure; many drugs are compatible with breastfeeding at standard doses."
                : "";

        out.add(drugInteractionDatabaseService.buildScreeningAlert(
                "Pregnancy/lactation (simplified FDA-style category)",
                "Category " + category + " (heuristic)",
                PrescriptionInteraction.InteractionCategory.PREGNANCY_LACTATION,
                "X".equals(category) ? PrescriptionInteraction.InteractionSeverity.MAJOR : PrescriptionInteraction.InteractionSeverity.MINOR,
                "X".equals(category) ? PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT : PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                "Displayed category is a local screening heuristic, not a legal label. Confirm with authoritative drug references.",
                "Teratogenicity / fetal risk varies by trimester and agent.",
                "Use specialist guidelines; consider alternative when category X or contraindicated." + lactNote,
                "Varies by agent",
                "Per trimester",
                "Heuristic — verify in licensed DB"));

        return out;
    }

    /** Very small rule set for demo / screening — replace with licensed data when available. */
    private static String inferPregnancyCategory(String lowerMedicationName) {
        if (lowerMedicationName.contains("warfarin")) {
            return "X";
        }
        if (lowerMedicationName.contains("lisinopril") || lowerMedicationName.contains("enalapril")
                || lowerMedicationName.contains("losartan")) {
            return "D";
        }
        if (lowerMedicationName.contains("metformin")) {
            return "B";
        }
        if (lowerMedicationName.contains("paracetamol") || lowerMedicationName.contains("acetaminophen")) {
            return "B";
        }
        if (lowerMedicationName.contains("ibuprofen")) {
            return "C";
        }
        return "C";
    }

    // --- Age ---

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluatePediatricGeriatricDosing(
            String medicationName, Patient patient) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        int age = DrugInteractionDatabaseService.ageYears(patient);
        if (age < 0) {
            return out;
        }
        String lower = medicationName.toLowerCase(Locale.ROOT);

        if (age < 18 && (lower.contains("ciprofloxacin") && (lower.contains("suspension") || lower.contains("tablet")))) {
            // Fluoroquinolone pediatric use restricted — flag as review
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    "Pediatric patient (" + age + " y)",
                    "Fluoroquinolone in pediatric patient — review indication",
                    PrescriptionInteraction.InteractionCategory.PEDIATRIC_GERIATRIC_DOSING,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                    "Fluoroquinolones have pediatric restrictions and musculoskeletal concerns.",
                    "Tendon / cartilage effects in growing patients",
                    "Use only when no alternative per guideline; document informed consent if used.",
                    "Cartilage toxicity in juvenile animals",
                    "Duration of therapy",
                    "Guideline — screening"));
        }

        if (age >= 65 && (lower.contains("ibuprofen") || lower.contains("naproxen"))) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    "Geriatric patient (" + age + " y)",
                    "NSAID in older adult — Beers-criteria style caution",
                    PrescriptionInteraction.InteractionCategory.PEDIATRIC_GERIATRIC_DOSING,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "NSAIDs increase GI bleed and renal risk in older adults.",
                    "GI bleeding, AKI",
                    "Prefer topical or non-NSAID analgesia when possible; PPI cover if NSAID required.",
                    "COX inhibition / perfusion",
                    "Days",
                    "Beers-inspired heuristic"));
        }

        return out;
    }

    // --- Weight ---

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateWeightBasedDosing(
            String medicationName, DrugInteractionCheckRequest request) {
        if (request == null || request.getWeightKg() == null || request.getDoseStrengthMg() == null) {
            return List.of();
        }
        return evaluateWeightBasedDosingWithDose(medicationName, request.getDoseStrengthMg(),
                request.getDoseUnit(), request.getWeightKg());
    }

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateWeightBasedDosingWithDose(
            String medicationName, BigDecimal doseMg, String doseUnit, BigDecimal weightKg) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0 || doseMg == null) {
            return out;
        }
        String lower = medicationName.toLowerCase(Locale.ROOT);
        if (!lower.contains("amoxicillin")) {
            return out;
        }
        // Rough daily mg/kg check assuming BID common teaching example (not therapeutic endorsement)
        BigDecimal mgPerKg = doseMg.divide(weightKg, 2, RoundingMode.HALF_UP);
        if (mgPerKg.compareTo(new BigDecimal("90")) > 0) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    weightKg + " kg body weight",
                    "High mg/kg amoxicillin dose — verify calculation",
                    PrescriptionInteraction.InteractionCategory.WEIGHT_BASED_DOSING,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.MODERATE,
                    "Computed mg/kg exceeds common pediatric upper screening threshold for amoxicillin (heuristic).",
                    "Dose-related toxicity unlikely for amoxicillin but verify indication and frequency.",
                    "Double-check total daily dose and frequency (" + Optional.ofNullable(doseUnit).orElse("unit") + ").",
                    "Linear scaling by weight",
                    "Ongoing",
                    "Heuristic"));
        } else if (mgPerKg.compareTo(new BigDecimal("20")) < 0) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    weightKg + " kg body weight",
                    "Low mg/kg amoxicillin dose — verify adequacy",
                    PrescriptionInteraction.InteractionCategory.WEIGHT_BASED_DOSING,
                    PrescriptionInteraction.InteractionSeverity.MINOR,
                    PrescriptionInteraction.ClinicalSignificanceLevel.MINOR,
                    "Dose per kg appears low for typical acute otitis media style regimens (heuristic).",
                    "Under-treatment risk",
                    "Confirm indication and duration.",
                    "Exposure-dependent efficacy",
                    "Ongoing",
                    "Heuristic"));
        }
        return out;
    }

    // --- Renal / hepatic ---

    private List<DrugInteractionDatabaseService.DrugInteractionResult> evaluateRenalHepatic(
            String medicationName,
            List<PatientProblem> problems,
            List<LabResult> recentLabs,
            DrugInteractionCheckRequest request) {

        List<DrugInteractionDatabaseService.DrugInteractionResult> out = new ArrayList<>();
        String lower = medicationName.toLowerCase(Locale.ROOT);

        BigDecimal egfr = request != null ? request.getEgfrMlMin() : null;
        BigDecimal creat = request != null ? request.getSerumCreatinineMgDl() : null;
        if (egfr == null || creat == null) {
            Optional<BigDecimal[]> fromLabs = extractCreatinineEgfr(recentLabs);
            if (fromLabs.isPresent()) {
                BigDecimal[] pair = fromLabs.get();
                if (creat == null) {
                    creat = pair[0];
                }
                if (egfr == null) {
                    egfr = pair[1];
                }
            }
        }

        boolean ckd = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesCkd);
        boolean liver = problems.stream().anyMatch(ClinicalMedicationSafetyService::problemIndicatesLiverDisease);

        if (egfr != null && egfr.compareTo(new BigDecimal("30")) < 0 && lower.contains("metformin")) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    egfr + " mL/min/1.73m² (estimated)",
                    "Severe renal impairment — metformin",
                    PrescriptionInteraction.InteractionCategory.RENAL_HEPATIC_ALERT,
                    PrescriptionInteraction.InteractionSeverity.MAJOR,
                    PrescriptionInteraction.ClinicalSignificanceLevel.CRITICAL,
                    "Metformin is generally contraindicated when eGFR falls below institutional threshold (often below 30 mL/min/1.73m²).",
                    "Lactic acidosis",
                    "Hold or dose-adjust per renal protocol; confirm labs.",
                    "Accumulation when GFR low",
                    "Immediate",
                    "Guideline — screening"));
        }

        if ((ckd || (egfr != null && egfr.compareTo(new BigDecimal("60")) < 0)) && containsNsaids(lower)) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    egfr != null ? "eGFR " + egfr : "CKD documented",
                    "Renal risk — NSAID",
                    PrescriptionInteraction.InteractionCategory.RENAL_HEPATIC_ALERT,
                    PrescriptionInteraction.InteractionSeverity.MAJOR,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "NSAIDs may worsen renal function when eGFR is reduced.",
                    "AKI, fluid overload",
                    "Avoid or use lowest effective dose with monitoring.",
                    "Hemodynamic / prostaglandins",
                    "Days",
                    "Established — screening"));
        }

        if (liver && (lower.contains("simvastatin") || lower.contains("atorvastatin"))) {
            out.add(drugInteractionDatabaseService.buildScreeningAlert(
                    DrugInteractionDatabaseService.summarizeProblems(problems),
                    "Hepatic impairment — statin",
                    PrescriptionInteraction.InteractionCategory.RENAL_HEPATIC_ALERT,
                    PrescriptionInteraction.InteractionSeverity.MODERATE,
                    PrescriptionInteraction.ClinicalSignificanceLevel.SIGNIFICANT,
                    "Statins require caution and possible dose reduction in significant liver disease.",
                    "Hepatotoxicity",
                    "Monitor LFTs; consider lower intensity statin.",
                    "Hepatic metabolism",
                    "Weeks",
                    "Guideline — screening"));
        }

        return out;
    }

    private Optional<BigDecimal[]> extractCreatinineEgfr(List<LabResult> labs) {
        if (labs == null || labs.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal creat = null;
        BigDecimal egfr = null;
        for (LabResult r : labs) {
            String tn = r.getTestName() != null ? r.getTestName().toLowerCase(Locale.ROOT) : "";
            if (creat == null && tn.contains("creatinine") && r.getResultValueNumeric() != null) {
                creat = r.getResultValueNumeric();
            }
            if (egfr == null && (tn.contains("egfr") || tn.contains("gfr")) && r.getResultValueNumeric() != null) {
                egfr = r.getResultValueNumeric();
            }
        }
        if (creat == null && egfr == null) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal[]{creat, egfr});
    }
}
