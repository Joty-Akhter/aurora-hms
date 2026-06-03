package com.easyops.hospital.service;

import com.easyops.hospital.entity.Allergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * FR-P1.7 / FR-P1.2 — Phase 1 drug-allergy matching heuristics.
 *
 * <p><b>What this service does</b> (Phase 1 — implemented):
 * <ul>
 *   <li><b>DIRECT</b>: normalised name contains / matches the allergen name (word-level, ≥4 chars).</li>
 *   <li><b>SYNONYM</b>: brand↔generic equivalence (e.g. "Tylenol" = "acetaminophen").</li>
 *   <li><b>DRUG_COMPONENT</b>: medication is a combination drug that contains the allergen
 *       as a component (e.g. "Amox-Clav" contains "amoxicillin").</li>
 *   <li><b>DRUG_CLASS</b>: allergen is a drug-class name (e.g. "penicillin") and the
 *       medication being prescribed is a known member of that class.</li>
 *   <li><b>CROSS_REACTIVITY</b>: allergen belongs to class A and the medication belongs to
 *       class B which has documented partial cross-reactivity with A (e.g. penicillin → cephalosporins).
 *       Rendered as a clinical warning, not a hard block.</li>
 * </ul>
 *
 * <p><b>Phase 2 — required before unsupervised clinical go-live</b>:
 * Integration with a licensed allergen cross-reference database (e.g. First Databank NDDF,
 * Multum, FDB, or RxNorm allergen hierarchy API) to provide complete and up-to-date coverage.
 * Phase 1 heuristics cover the most clinically significant patterns but are NOT exhaustive.
 * A drug-allergy pair not flagged here is not guaranteed to be safe.
 */
@Service
@Slf4j
public class AllergyMatchingService {

    // =========================================================================
    // Public API types
    // =========================================================================

    public enum MatchType {
        DIRECT,          // Same drug (name or code match)
        SYNONYM,         // Brand / generic synonym equivalence
        DRUG_COMPONENT,  // Medication is a combination containing the allergen
        DRUG_CLASS,      // Allergen is a drug class; medication is a member
        CROSS_REACTIVITY // Related class with documented partial cross-reactivity (warning)
    }

    public record AllergyMatchResult(
            Allergy allergy,
            MatchType matchType,
            String matchedOn,     // What triggered the match (for UI display)
            String clinicalNote   // Prescriber-facing explanation
    ) {}

    // =========================================================================
    // Static knowledge base
    // =========================================================================

    /**
     * Drug class keyword → set of member drug name fragments (all lowercase, no hyphens).
     * A medication "belongs" to a class if its normalised name contains any member fragment.
     * Fragments must be ≥5 chars to reduce false positives.
     */
    private static final Map<String, Set<String>> DRUG_CLASS_MEMBERS;

    /**
     * All known class keywords, including synonyms for the same class
     * (e.g. "penicillin", "penicillins", "beta-lactam").
     * Maps each keyword → canonical class name used in DRUG_CLASS_MEMBERS.
     */
    private static final Map<String, String> CLASS_KEYWORD_TO_CANONICAL;

    /**
     * Generic name → all known synonyms / brand names (lower-cased).
     * Built bidirectionally: brand names also map back to generic.
     */
    private static final Map<String, Set<String>> SYNONYM_GROUPS;
    /** Flat reverse map: any surface form → canonical generic name. */
    private static final Map<String, String> SURFACE_TO_GENERIC;

    /**
     * Combination drug name fragments → set of component name fragments.
     * A medication "decomposes" if its normalised name contains a combination key.
     */
    private static final Map<String, Set<String>> COMBINATION_COMPONENTS;

    /** Cross-reactivity: allergen canonical class → { medication class, severity, note }. */
    private static final List<CrossReactRule> CROSS_REACT_RULES;

    private record CrossReactRule(
            String allergenClass,      // canonical class of the allergen
            String medicationClass,    // canonical class of the medication being prescribed
            String severity,           // "MODERATE" or "LOW"
            String clinicalNote
    ) {}

    // =========================================================================
    // Data initialisation
    // =========================================================================

    static {
        DRUG_CLASS_MEMBERS = buildClassMembers();
        CLASS_KEYWORD_TO_CANONICAL = buildClassKeywords();
        SYNONYM_GROUPS = buildSynonymGroups();
        SURFACE_TO_GENERIC = buildSurfaceToGeneric(SYNONYM_GROUPS);
        COMBINATION_COMPONENTS = buildCombinationComponents();
        CROSS_REACT_RULES = buildCrossReactRules();
    }

    private static Map<String, Set<String>> buildClassMembers() {
        Map<String, Set<String>> m = new LinkedHashMap<>();

        m.put("penicillin", Set.of(
                "ampicillin", "amoxicillin", "piperacillin", "oxacillin", "cloxacillin",
                "dicloxacillin", "nafcillin", "ticarcillin", "flucloxacillin",
                "phenoxymethylpenicillin", "benzylpenicillin", "procaine penicillin",
                "benzathine penicillin"));

        m.put("cephalosporin", Set.of(
                "cephalexin", "cefalexin", "cefazolin", "cefadroxil", "cefaclor",
                "cefuroxime", "cefprozil", "cefdinir", "cefpodoxime", "cefixime",
                "ceftriaxone", "cefotaxime", "ceftazidime", "cefoperazone",
                "cefepime", "ceftaroline", "ceftolozane", "ceftazidime-avibactam",
                "cefoxitin", "cefotetan"));

        m.put("carbapenem", Set.of(
                "meropenem", "imipenem", "ertapenem", "doripenem"));

        m.put("sulfonamide", Set.of(
                "sulfamethoxazole", "sulfadiazine", "sulfisoxazole",
                "sulfasalazine", "sulfacetamide", "sulfaclozine",
                "trimethoprim-sulfamethoxazole", "co-trimoxazole", "bactrim", "septra"));

        m.put("fluoroquinolone", Set.of(
                "ciprofloxacin", "levofloxacin", "moxifloxacin", "ofloxacin",
                "norfloxacin", "gemifloxacin", "delafloxacin", "enrofloxacin",
                "nalidixic"));

        m.put("macrolide", Set.of(
                "azithromycin", "clarithromycin", "erythromycin",
                "roxithromycin", "fidaxomicin"));

        m.put("aminoglycoside", Set.of(
                "gentamicin", "amikacin", "tobramycin", "streptomycin",
                "neomycin", "kanamycin", "netilmicin", "paromomycin"));

        m.put("tetracycline", Set.of(
                "tetracycline", "doxycycline", "minocycline", "tigecycline",
                "demeclocycline", "oxytetracycline"));

        m.put("nsaid", Set.of(
                "ibuprofen", "naproxen", "diclofenac", "indomethacin", "ketorolac",
                "celecoxib", "meloxicam", "piroxicam", "ketoprofen", "flurbiprofen",
                "etodolac", "sulindac", "mefenamic", "meclofenamate", "fenoprofen",
                "diflunisal", "tolmetin"));

        m.put("opioid", Set.of(
                "morphine", "codeine", "oxycodone", "hydrocodone", "fentanyl",
                "tramadol", "hydromorphone", "buprenorphine", "methadone",
                "meperidine", "pethidine", "oxymorphone", "tapentadol",
                "alfentanil", "sufentanil", "remifentanil", "nalbuphine",
                "butorphanol", "pentazocine"));

        m.put("statin", Set.of(
                "atorvastatin", "rosuvastatin", "simvastatin", "pravastatin",
                "lovastatin", "fluvastatin", "pitavastatin"));

        m.put("ace inhibitor", Set.of(
                "lisinopril", "enalapril", "ramipril", "captopril", "benazepril",
                "fosinopril", "quinapril", "perindopril", "trandolapril", "moexipril"));

        m.put("arb", Set.of(
                "losartan", "valsartan", "irbesartan", "candesartan",
                "olmesartan", "telmisartan", "azilsartan", "eprosartan"));

        m.put("benzodiazepine", Set.of(
                "diazepam", "lorazepam", "alprazolam", "clonazepam",
                "temazepam", "midazolam", "oxazepam", "triazolam",
                "chlordiazepoxide", "nitrazepam", "flurazepam"));

        m.put("ssri", Set.of(
                "fluoxetine", "sertraline", "escitalopram", "citalopram",
                "paroxetine", "fluvoxamine"));

        m.put("beta blocker", Set.of(
                "metoprolol", "atenolol", "carvedilol", "propranolol",
                "bisoprolol", "nadolol", "timolol", "nebivolol", "labetalol",
                "acebutolol", "betaxolol", "pindolol", "sotalol"));

        m.put("calcium channel blocker", Set.of(
                "amlodipine", "nifedipine", "diltiazem", "verapamil",
                "felodipine", "nicardipine", "nisoldipine", "isradipine",
                "clevidipine", "nitrendipine"));

        m.put("azole antifungal", Set.of(
                "fluconazole", "itraconazole", "voriconazole", "posaconazole",
                "isavuconazole", "ketoconazole", "clotrimazole", "miconazole",
                "econazole", "tioconazole"));

        return Collections.unmodifiableMap(m);
    }

    private static Map<String, String> buildClassKeywords() {
        Map<String, String> m = new LinkedHashMap<>();
        // penicillin class variants
        for (String k : List.of("penicillin", "penicillins", "beta lactam", "beta-lactam",
                "aminopenicillin", "antistaphylococcal penicillin")) {
            m.put(k, "penicillin");
        }
        // cephalosporin
        for (String k : List.of("cephalosporin", "cephalosporins", "cephalo")) {
            m.put(k, "cephalosporin");
        }
        // carbapenem
        for (String k : List.of("carbapenem", "carbapenems")) {
            m.put(k, "carbapenem");
        }
        // sulfonamide
        for (String k : List.of("sulfonamide", "sulfonamides", "sulphonamide", "sulphonamides",
                "sulfa", "sulpha", "sulfa drug")) {
            m.put(k, "sulfonamide");
        }
        // fluoroquinolone
        for (String k : List.of("fluoroquinolone", "fluoroquinolones", "quinolone", "quinolones")) {
            m.put(k, "fluoroquinolone");
        }
        // macrolide
        for (String k : List.of("macrolide", "macrolides")) {
            m.put(k, "macrolide");
        }
        // aminoglycoside
        for (String k : List.of("aminoglycoside", "aminoglycosides")) {
            m.put(k, "aminoglycoside");
        }
        // tetracycline
        for (String k : List.of("tetracycline", "tetracyclines")) {
            m.put(k, "tetracycline");
        }
        // nsaid
        for (String k : List.of("nsaid", "nsaids", "non-steroidal", "nonsteroidal",
                "anti-inflammatory", "antiinflammatory")) {
            m.put(k, "nsaid");
        }
        // opioid
        for (String k : List.of("opioid", "opioids", "opiate", "opiates", "narcotic", "narcotics")) {
            m.put(k, "opioid");
        }
        // statin
        for (String k : List.of("statin", "statins", "hmg coa", "hmg-coa")) {
            m.put(k, "statin");
        }
        // ACE inhibitor
        for (String k : List.of("ace inhibitor", "ace inhibitors", "acei", "angiotensin converting enzyme")) {
            m.put(k, "ace inhibitor");
        }
        // ARB
        for (String k : List.of("arb", "arbs", "sartan", "sartans",
                "angiotensin receptor blocker", "angiotensin receptor")) {
            m.put(k, "arb");
        }
        // benzodiazepine
        for (String k : List.of("benzodiazepine", "benzodiazepines", "benzo", "benzos")) {
            m.put(k, "benzodiazepine");
        }
        // SSRI
        for (String k : List.of("ssri", "ssris", "selective serotonin")) {
            m.put(k, "ssri");
        }
        // beta blocker
        for (String k : List.of("beta blocker", "beta-blocker", "beta blockers",
                "beta-blockers", "beta adrenergic")) {
            m.put(k, "beta blocker");
        }
        // calcium channel blocker
        for (String k : List.of("calcium channel blocker", "ccb", "calcium antagonist")) {
            m.put(k, "calcium channel blocker");
        }
        // azole antifungal
        for (String k : List.of("azole", "azole antifungal", "azole antifungals")) {
            m.put(k, "azole antifungal");
        }
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Set<String>> buildSynonymGroups() {
        // Each entry: generic name → all equivalent surface forms (including itself)
        // The map is later flattened bidirectionally in buildSurfaceToGeneric().
        Map<String, Set<String>> m = new LinkedHashMap<>();
        m.put("acetaminophen", Set.of("paracetamol", "tylenol", "panadol", "calpol", "datril", "tempra"));
        m.put("ibuprofen",     Set.of("advil", "motrin", "nurofen", "brufen", "caldolor", "ibuprin"));
        m.put("naproxen",      Set.of("aleve", "naprosyn", "anaprox", "midol", "flanax"));
        m.put("aspirin",       Set.of("acetylsalicylic acid", "asa", "bayer aspirin", "aspro", "ecotrin"));
        m.put("omeprazole",    Set.of("prilosec", "losec", "omesec"));
        m.put("pantoprazole",  Set.of("protonix", "pantoloc", "pantozol", "pantosec"));
        m.put("esomeprazole",  Set.of("nexium"));
        m.put("lansoprazole",  Set.of("prevacid", "zoton"));
        m.put("metformin",     Set.of("glucophage", "fortamet", "glumetza", "riomet", "glycomet"));
        m.put("atorvastatin",  Set.of("lipitor", "atorva", "torvast"));
        m.put("simvastatin",   Set.of("zocor", "simva"));
        m.put("rosuvastatin",  Set.of("crestor", "rosuvas"));
        m.put("pravastatin",   Set.of("pravachol", "selektine"));
        m.put("lisinopril",    Set.of("zestril", "prinivil", "lisodur"));
        m.put("enalapril",     Set.of("vasotec", "renitec", "renivace"));
        m.put("ramipril",      Set.of("altace", "tritace", "ramace"));
        m.put("amlodipine",    Set.of("norvasc", "istin", "amlostin"));
        m.put("amoxicillin",   Set.of("amoxil", "trimox", "polymox", "amoxicilina"));
        m.put("azithromycin",  Set.of("zithromax", "z-pak", "zmax", "azithrocin", "azithrox"));
        m.put("ciprofloxacin", Set.of("cipro", "ciprobay", "ciflox", "baycip"));
        m.put("metronidazole", Set.of("flagyl", "metrogyl", "metrocream", "noritate", "metonidazole"));
        m.put("doxycycline",   Set.of("vibramycin", "doryx", "oracea", "monodox", "adoxa"));
        m.put("salbutamol",    Set.of("albuterol", "ventolin", "proventil", "proair", "airet"));
        m.put("cetirizine",    Set.of("zyrtec", "reactine", "cirrus", "cetrin"));
        m.put("loratadine",    Set.of("claritin", "clarityn", "alavert", "lorano"));
        m.put("fexofenadine",  Set.of("allegra", "telfast", "fexidine"));
        m.put("prednisone",    Set.of("deltasone", "orasone", "prednicot", "sterapred"));
        m.put("prednisolone",  Set.of("prelone", "pediapred", "millipred", "orapred"));
        m.put("warfarin",      Set.of("coumadin", "jantoven", "warfant", "warf"));
        m.put("digoxin",       Set.of("lanoxin", "lanoxicaps", "digox"));
        m.put("furosemide",    Set.of("lasix", "frusemide", "salix"));
        m.put("spironolactone", Set.of("aldactone", "spiractin"));
        m.put("metoprolol",    Set.of("lopressor", "toprol", "metolar", "betaloc"));
        m.put("atenolol",      Set.of("tenormin", "aten"));
        m.put("carvedilol",    Set.of("coreg", "dilatrend"));
        m.put("losartan",      Set.of("cozaar", "hyzaar"));
        m.put("valsartan",     Set.of("diovan", "valzaar"));
        m.put("irbesartan",    Set.of("avapro", "aprovel"));
        m.put("fluconazole",   Set.of("diflucan", "trican", "fluzole"));
        m.put("clindamycin",   Set.of("cleocin", "clindagel", "clindasol"));
        m.put("clarithromycin", Set.of("biaxin", "klacid", "klaricid"));
        m.put("erythromycin",  Set.of("eryc", "erythrocin", "ilosone", "e-mycin"));
        m.put("vancomycin",    Set.of("vancocin", "vancoled"));
        m.put("ceftriaxone",   Set.of("rocephin", "ceftriaxona"));
        m.put("cephalexin",    Set.of("keflex", "ceporex", "keftab"));
        m.put("diazepam",      Set.of("valium", "diastat"));
        m.put("lorazepam",     Set.of("ativan", "temesta"));
        m.put("alprazolam",    Set.of("xanax", "niravam"));
        m.put("gabapentin",    Set.of("neurontin", "gralise", "horizant"));
        m.put("tramadol",      Set.of("ultram", "ultracet", "tramal", "contramal"));
        return Collections.unmodifiableMap(m);
    }

    /** Build a flat surface → generic map for O(1) canonical lookup. */
    private static Map<String, String> buildSurfaceToGeneric(Map<String, Set<String>> groups) {
        Map<String, String> flat = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : groups.entrySet()) {
            String generic = e.getKey();
            flat.put(generic, generic);
            for (String synonym : e.getValue()) {
                flat.put(synonym, generic);
            }
        }
        return Collections.unmodifiableMap(flat);
    }

    /** Normalised combination-drug fragment → set of component name fragments. */
    private static Map<String, Set<String>> buildCombinationComponents() {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        // Penicillin combinations
        m.put("amox clav",         Set.of("amoxicillin", "clavulanate"));
        m.put("co amoxiclav",      Set.of("amoxicillin", "clavulanate"));
        m.put("augmentin",         Set.of("amoxicillin", "clavulanate"));
        m.put("amoxicillin clavulanate", Set.of("amoxicillin", "clavulanate"));
        m.put("pip tazo",          Set.of("piperacillin", "tazobactam"));
        m.put("tazocin",           Set.of("piperacillin", "tazobactam"));
        m.put("zosyn",             Set.of("piperacillin", "tazobactam"));
        m.put("piperacillin tazobactam", Set.of("piperacillin", "tazobactam"));
        m.put("ampicillin sulbactam", Set.of("ampicillin", "sulbactam"));
        m.put("unasyn",            Set.of("ampicillin", "sulbactam"));
        // Sulfonamide combinations
        m.put("trimethoprim sulfamethoxazole", Set.of("trimethoprim", "sulfamethoxazole"));
        m.put("co trimoxazole",    Set.of("trimethoprim", "sulfamethoxazole"));
        m.put("tmp smx",           Set.of("trimethoprim", "sulfamethoxazole"));
        m.put("bactrim",           Set.of("trimethoprim", "sulfamethoxazole"));
        m.put("septra",            Set.of("trimethoprim", "sulfamethoxazole"));
        // Cephalosporin combinations
        m.put("ceftazidime avibactam", Set.of("ceftazidime", "avibactam"));
        m.put("ceftolozane tazobactam", Set.of("ceftolozane", "tazobactam"));
        // Carbapenem combinations
        m.put("imipenem cilastatin", Set.of("imipenem", "cilastatin"));
        m.put("meropenem vaborbactam", Set.of("meropenem", "vaborbactam"));
        // Other common combinations
        m.put("amoxicillin metronidazole", Set.of("amoxicillin", "metronidazole"));
        return Collections.unmodifiableMap(m);
    }

    private static List<CrossReactRule> buildCrossReactRules() {
        return List.of(
            new CrossReactRule(
                "penicillin", "cephalosporin",
                "MODERATE",
                "Patient has a documented penicillin allergy. Cephalosporins share a beta-lactam "
                + "ring structure; cross-reactivity is estimated at 1–5% (lower with 3rd/4th-generation "
                + "agents). Clinical risk assessment required before prescribing. Consider skin testing "
                + "if benefit outweighs risk."),
            new CrossReactRule(
                "penicillin", "carbapenem",
                "LOW",
                "Patient has a documented penicillin allergy. Cross-reactivity with carbapenems "
                + "is rare (<1%) but documented. Use with caution and have emergency treatment available."),
            new CrossReactRule(
                "cephalosporin", "carbapenem",
                "LOW",
                "Patient has a documented cephalosporin allergy. Very rare cross-reactivity with "
                + "carbapenems. Use clinical judgement; the shared beta-lactam ring is structurally "
                + "different in carbapenems."),
            new CrossReactRule(
                "sulfonamide", "sulfonamide",
                "MODERATE",
                "Patient has a documented sulfonamide allergy. This medication contains a sulfonamide "
                + "moiety. Review the nature of the prior reaction (immune-mediated vs non-immune) before prescribing."),
            new CrossReactRule(
                "opioid", "opioid",
                "MODERATE",
                "Patient has a documented opioid/opiate allergy or intolerance. Assess the nature of "
                + "the reaction (true allergy vs intolerance vs side effect) before prescribing another opioid. "
                + "Different opioid chemical classes (morphinans, phenylpiperidines, diphenylheptanes) have "
                + "low cross-reactivity, but individual variation exists.")
        );
    }

    // =========================================================================
    // Public matching method
    // =========================================================================

    /**
     * Checks a single medication against a list of active allergies and returns all match results.
     *
     * <p>Multiple results can be returned for a single allergy (e.g. both DRUG_CLASS and
     * CROSS_REACTIVITY). Callers should retain the highest-priority result per allergy if
     * they need a single representative record.
     *
     * @param medicationName normalised medication name from the prescription line item
     * @param medicationCode NDC or RxNorm code (may be null)
     * @param activeAllergies patient's active drug allergies
     * @return list of match results (empty if no allergy concerns found)
     */
    public List<AllergyMatchResult> checkMedication(
            String medicationName,
            String medicationCode,
            List<Allergy> activeAllergies) {

        if (medicationName == null || medicationName.isBlank() || activeAllergies.isEmpty()) {
            return List.of();
        }

        String normMed = normalize(medicationName);
        String canonMed = toCanonicalGeneric(normMed);

        List<AllergyMatchResult> results = new ArrayList<>();

        for (Allergy allergy : activeAllergies) {
            if (allergy.getAllergenName() == null) continue;

            // Only apply drug-class and cross-reactivity logic for drug allergies.
            // For FOOD/ENVIRONMENTAL/LATEX, fall through to direct name match only.
            boolean isDrugAllergy = allergy.getAllergenType() == Allergy.AllergenType.DRUG
                    || allergy.getAllergenType() == null;

            String normAllergen  = normalize(allergy.getAllergenName());
            String canonAllergen = toCanonicalGeneric(normAllergen);

            // --- 1. Allergen code match (highest confidence) ------------------
            if (medicationCode != null && allergy.getAllergenCode() != null
                    && !medicationCode.isBlank() && !allergy.getAllergenCode().isBlank()
                    && medicationCode.equalsIgnoreCase(allergy.getAllergenCode())) {
                results.add(new AllergyMatchResult(allergy, MatchType.DIRECT,
                        "allergen code " + allergy.getAllergenCode(),
                        "Medication code matches a recorded allergen code directly."));
                continue; // highest confidence; skip further checks for this allergy
            }

            // --- 2. Direct name match -----------------------------------------
            if (isDirectNameMatch(normMed, normAllergen)
                    || (canonMed != null && canonAllergen != null
                        && canonMed.equals(canonAllergen))) {
                results.add(new AllergyMatchResult(allergy, MatchType.DIRECT,
                        allergy.getAllergenName(),
                        "Medication name matches recorded allergen '" + allergy.getAllergenName() + "' directly."));
                continue;
            }

            // --- 3. Synonym match ---------------------------------------------
            if (isSynonymMatch(normMed, canonMed, normAllergen, canonAllergen)) {
                results.add(new AllergyMatchResult(allergy, MatchType.SYNONYM,
                        allergy.getAllergenName(),
                        "'" + medicationName + "' is a known brand/generic equivalent of '"
                        + allergy.getAllergenName() + "'."));
                continue;
            }

            // --- 4. Drug component match (combination drugs) ------------------
            if (isDrugAllergy) {
                AllergyMatchResult componentMatch =
                        checkCombinationComponents(medicationName, normMed, allergy, normAllergen, canonAllergen);
                if (componentMatch != null) {
                    results.add(componentMatch);
                    continue;
                }

                // --- 5. Drug class membership match ---------------------------
                AllergyMatchResult classMatch = checkDrugClassMembership(
                        medicationName, normMed, allergy, normAllergen);
                if (classMatch != null) {
                    results.add(classMatch);
                    continue;
                }

                // --- 6. Cross-reactivity (runs independently; can stack on top) --
                AllergyMatchResult crossMatch =
                        checkCrossReactivity(medicationName, normMed, allergy, normAllergen);
                if (crossMatch != null) {
                    results.add(crossMatch);
                }
            }
        }

        return results;
    }

    // =========================================================================
    // Match sub-checks
    // =========================================================================

    /**
     * Direct name match: one normalised name contains the other as a meaningful token sequence.
     * A minimum overlap length of 5 characters is required to prevent false positives.
     */
    private static boolean isDirectNameMatch(String normMed, String normAllergen) {
        if (normMed.isEmpty() || normAllergen.isEmpty()) return false;

        // Simple containment (one is a substring of the other) with length guard
        int minLen = Math.min(normMed.length(), normAllergen.length());
        if (minLen >= 5 && (normMed.contains(normAllergen) || normAllergen.contains(normMed))) {
            return true;
        }

        // Word-token intersection: any token ≥5 chars shared between both
        Set<String> medTokens = tokenize(normMed);
        Set<String> allergenTokens = tokenize(normAllergen);
        for (String t : allergenTokens) {
            if (t.length() >= 5 && medTokens.contains(t)) return true;
        }
        return false;
    }

    /** Synonym match: either name resolves to the same canonical generic. */
    private static boolean isSynonymMatch(
            String normMed, String canonMed, String normAllergen, String canonAllergen) {
        if (canonMed != null && canonAllergen != null && canonMed.equals(canonAllergen)) {
            // Same canonical but not caught by direct match → must be a brand↔generic case
            return !normMed.equals(normAllergen);
        }
        return false;
    }

    /** Checks whether the medication is a combination drug containing the allergen as a component. */
    private static AllergyMatchResult checkCombinationComponents(
            String medName, String normMed, Allergy allergy, String normAllergen, String canonAllergen) {

        for (Map.Entry<String, Set<String>> e : COMBINATION_COMPONENTS.entrySet()) {
            String comboKey = e.getKey();
            if (!normMed.contains(comboKey)) continue;

            Set<String> components = e.getValue();
            for (String component : components) {
                boolean componentMatchesAllergen =
                        normAllergen.contains(component) || component.contains(normAllergen)
                        || (canonAllergen != null && (canonAllergen.contains(component)
                            || component.contains(canonAllergen)));
                if (componentMatchesAllergen && component.length() >= 5) {
                    return new AllergyMatchResult(allergy, MatchType.DRUG_COMPONENT,
                            component + " (in " + medName + ")",
                            "'" + medName + "' is a combination drug containing '"
                            + component + "', to which the patient has a recorded allergy ("
                            + allergy.getAllergenName() + ").");
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the allergen name is a drug-class keyword and the medication
     * is a known member of that class.
     */
    private static AllergyMatchResult checkDrugClassMembership(
            String medName, String normMed, Allergy allergy, String normAllergen) {

        // Determine if allergen name identifies a drug class
        String canonClass = resolveAllergenToClass(normAllergen);
        if (canonClass == null) return null;

        // Check if the medication belongs to that class
        Set<String> classMembers = DRUG_CLASS_MEMBERS.getOrDefault(canonClass, Set.of());
        for (String member : classMembers) {
            if (normMed.contains(member)) {
                return new AllergyMatchResult(allergy, MatchType.DRUG_CLASS,
                        canonClass + " class → " + member,
                        "'" + medName + "' belongs to the " + canonClass + " drug class. "
                        + "Patient has a documented allergy to " + allergy.getAllergenName() + ".");
            }
        }
        return null;
    }

    /**
     * Checks cross-reactivity: determines the drug class of both the allergen and the medication,
     * then looks for a cross-reactivity rule between them.
     */
    private static AllergyMatchResult checkCrossReactivity(
            String medName, String normMed, Allergy allergy, String normAllergen) {

        // Determine what drug class the ALLERGEN belongs to
        String allergenClass = determineDrugClass(normAllergen);
        if (allergenClass == null) return null;

        // Determine what drug class the MEDICATION belongs to
        String medClass = determineDrugClass(normMed);
        if (medClass == null) return null;

        // Same class is handled by DRUG_CLASS match above, not cross-reactivity
        if (allergenClass.equals(medClass)) return null;

        for (CrossReactRule rule : CROSS_REACT_RULES) {
            if (rule.allergenClass().equals(allergenClass)
                    && rule.medicationClass().equals(medClass)) {
                return new AllergyMatchResult(allergy, MatchType.CROSS_REACTIVITY,
                        allergenClass + " → " + medClass + " (" + rule.severity() + ")",
                        rule.clinicalNote());
            }
        }
        return null;
    }

    // =========================================================================
    // Helper utilities
    // =========================================================================

    /**
     * Normalises a drug name: lowercase, replace hyphens/slashes with space,
     * strip parenthetical suffixes (e.g. "(as base)"), collapse whitespace.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)", " ")  // remove parenthetical content
                .replace("-", " ")
                .replace("/", " ")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Splits a normalised string into word tokens (each ≥2 chars). */
    private static Set<String> tokenize(String normalised) {
        Set<String> tokens = new HashSet<>();
        for (String t : normalised.split(" ")) {
            if (t.length() >= 2) tokens.add(t);
        }
        return tokens;
    }

    /** Returns the canonical generic name for a surface form, or null if not in the synonym map. */
    private String toCanonicalGeneric(String normalised) {
        // Try exact lookup first
        if (SURFACE_TO_GENERIC.containsKey(normalised)) {
            return SURFACE_TO_GENERIC.get(normalised);
        }
        // Try token-level lookup (e.g. "acetaminophen 500mg" → "acetaminophen")
        for (String token : tokenize(normalised)) {
            if (SURFACE_TO_GENERIC.containsKey(token)) {
                return SURFACE_TO_GENERIC.get(token);
            }
        }
        return null;
    }

    /**
     * If a normalised allergen name contains a known class keyword, returns the canonical class.
     * Used to check whether an allergen entry like "penicillin allergy" identifies a class.
     */
    private static String resolveAllergenToClass(String normAllergen) {
        for (Map.Entry<String, String> e : CLASS_KEYWORD_TO_CANONICAL.entrySet()) {
            if (normAllergen.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Determines which canonical drug class a medication (or allergen) belongs to,
     * by checking its normalised name against class member lists.
     * Also checks class keywords so that an allergen named "penicillin" resolves correctly.
     */
    private static String determineDrugClass(String normName) {
        // 1. Check if the name IS a class keyword (for allergens like "penicillin")
        String asClass = resolveAllergenToClass(normName);
        if (asClass != null) return asClass;

        // 2. Check if the name contains a known class member (for medications like "amoxicillin")
        for (Map.Entry<String, Set<String>> classEntry : DRUG_CLASS_MEMBERS.entrySet()) {
            for (String member : classEntry.getValue()) {
                if (normName.contains(member)) {
                    return classEntry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Convenience: returns the single highest-priority match from a list, for callers
     * that need only one representative result per allergy-medication pair.
     * Priority: CODE_MATCH/DIRECT > SYNONYM > DRUG_COMPONENT > DRUG_CLASS > CROSS_REACTIVITY.
     */
    public static MatchType highestPriority(List<AllergyMatchResult> results) {
        if (results.isEmpty()) return null;
        int best = Integer.MAX_VALUE;
        for (AllergyMatchResult r : results) {
            int ord = matchTypeOrdinal(r.matchType());
            if (ord < best) best = ord;
        }
        final int bestFinal = best;
        return results.stream()
                .filter(r -> matchTypeOrdinal(r.matchType()) == bestFinal)
                .findFirst()
                .map(AllergyMatchResult::matchType)
                .orElse(null);
    }

    private static int matchTypeOrdinal(MatchType t) {
        return switch (t) {
            case DIRECT           -> 0;
            case SYNONYM          -> 1;
            case DRUG_COMPONENT   -> 2;
            case DRUG_CLASS       -> 3;
            case CROSS_REACTIVITY -> 4;
        };
    }
}
