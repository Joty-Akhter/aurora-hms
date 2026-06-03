package com.easyops.hospital.service;

import com.easyops.hospital.entity.Prescription;
import com.easyops.hospital.entity.PrescriptionMedication;
import com.easyops.hospital.entity.PrescriptionTransmission;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.entity.PharmacyNetwork;
import com.easyops.hospital.util.PatientNameInterop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-P3.6 — NCPDP SCRIPT 2017071 message builder.
 *
 * <p>Builds a valid {@code <NewRx>} XML message conforming to the NCPDP SCRIPT 2017071
 * standard and validates it against a bundled XSD before returning the XML string.
 *
 * <h3>Standard references</h3>
 * <ul>
 *   <li>NCPDP SCRIPT Standard, Implementation Guide, Version 2017071 (licensed document).</li>
 *   <li>Message type: {@code NEWRX} — New Prescription.</li>
 *   <li>Namespace: {@code http://www.ncpdp.org/schema/SCRIPT}</li>
 *   <li>Attributes: {@code DatatypesVersion="20170701" TransactionDomain="SCRIPT"
 *       TransactionVersion="20170701" StructuresVersion="20170701" ECLVersion="20170701"}</li>
 * </ul>
 *
 * <h3>Phase 1 scope (FR-P3.6)</h3>
 * <ul>
 *   <li>Builds a single {@code <NewRx>} using the prescription-level primary medication fields
 *       ({@code medicationName}, {@code medicationCode}, {@code quantity}, etc.).</li>
 *   <li>Validates the generated XML against the bundled simplified XSD before returning.</li>
 *   <li><strong>Multi-line prescriptions:</strong> when {@link Prescription#getMedications()} is
 *       non-empty, one full {@code Message} (each containing a {@code NewRx}) is built and
 *       XSD-validated <em>per line item</em>, in {@code lineNumber} order.  The returned string
 *       from {@link #buildAndValidateNewRx} joins those documents with
 *       {@link #NCPDP_MULTI_MESSAGE_SEPARATOR}.  {@link com.easyops.hospital.service.EPrescribingService}
 *       sends each document to the network in separate HTTP requests when not in simulation mode.</li>
 *   <li>Legacy prescriptions with no medication rows still use denormalised fields on
 *       {@link Prescription} for a single {@code NewRx}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NcpdpScriptMessageBuilder {

    private final NpiValidator npiValidator;

    // ---------------------------------------------------------------------------
    // NCPDP SCRIPT 2017071 constants
    // ---------------------------------------------------------------------------

    public static final String NCPDP_NS = "http://www.ncpdp.org/schema/SCRIPT";
    private static final String SCRIPT_VERSION = "20170701";

    /** NCPDP Qualifier: NPI for prescriber (sender). */
    private static final String QUALIFIER_NPI_SENDER = "G";
    /** NCPDP Qualifier: NCPDP Provider ID (pharmacy). */
    private static final String QUALIFIER_NCPDP_ID = "P";
    /** NCPDP product code qualifier: NDC. */
    private static final String QUALIFIER_NDC = "ND";
    /** NCPDP product code qualifier: RxNorm. */
    private static final String QUALIFIER_RXNORM = "RXN";
    /** NCPDP product code qualifier: Drug name. */
    private static final String QUALIFIER_DRUG_NAME = "DN";
    /** NCPDP diagnosis code qualifier: ICD-10-CM. */
    private static final String QUALIFIER_ICD10 = "ABK";

    private static final DateTimeFormatter NCPDP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter NCPDP_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String XSD_RESOURCE = "/ncpdp/ncpdp-script-20170701.xsd";

    /**
     * Joins multiple full NCPDP {@code Message} XML documents when a prescription has several
     * medication lines.  Not part of the NCPDP wire format — only used for storage and for the
     * e-prescribing service to split before posting each message.
     */
    public static final String NCPDP_MULTI_MESSAGE_SEPARATOR =
            "\n<!-- EASYOPS_NCPDP_MESSAGE_SEPARATOR -->\n";

    // ---------------------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------------------

    @Value("${ncpdp.sender.software-developer:EasyOps}")
    private String softwareDeveloper;

    @Value("${ncpdp.sender.software-product:EasyOps EHR}")
    private String softwareProduct;

    @Value("${ncpdp.sender.software-version:1.0}")
    private String softwareVersion;

    @Value("${ncpdp.test-mode:true}")
    private boolean testMode;

    /** Cached compiled schema — loaded once, thread-safe after init. */
    private volatile Schema compiledSchema;

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Build and XSD-validate one or more full {@code Message} XML documents (one {@code NewRx}
     * per {@link PrescriptionMedication} line when present; otherwise a single legacy
     * {@code NewRx} from denormalised {@link Prescription} fields).
     *
     * @return non-empty list of validated XML strings, one per medication line (or one legacy doc)
     */
    public List<String> buildAndValidateAllNewRxDocuments(
            Prescription prescription,
            PrescriptionTransmission transmission,
            PharmacyNetwork network) {

        List<PrescriptionMedication> lines = orderedMedicationLines(prescription);
        List<String> results = new ArrayList<>();

        try {
            if (lines.isEmpty()) {
                runNpiPreflightChecks(prescription, transmission);
                runPhase1MedicationPreflightCheck(prescription, null);
                Document doc = buildDocument(prescription, transmission, network, null);
                String xml = serializeToXml(doc);
                validateAgainstXsd(xml);
                results.add(xml);
                log.debug("NCPDP SCRIPT NewRx built and validated (legacy single) for prescription {} / tx {}",
                        prescription.getPrescriptionId(), transmission.getTransmissionId());
                return results;
            }

            log.info("FR-P3.6: building {} NCPDP SCRIPT NewRx message(s) for prescription {} / tx {}",
                    lines.size(), prescription.getPrescriptionId(), transmission.getTransmissionId());

            runNpiPreflightChecks(prescription, transmission);
            for (PrescriptionMedication line : lines) {
                runPhase1MedicationPreflightCheck(prescription, line);
                Document doc = buildDocument(prescription, transmission, network, line);
                String xml = serializeToXml(doc);
                validateAgainstXsd(xml);
                results.add(xml);
            }
            return results;
        } catch (NcpdpBuildException | NcpdpValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new NcpdpBuildException("Failed to build NCPDP SCRIPT NewRx message: " + e.getMessage(), e);
        }
    }

    /**
     * Build and XSD-validate NewRx NCPDP SCRIPT 2017071 XML.
     * Multiple medication lines produce multiple full {@code Message} documents joined by
     * {@link #NCPDP_MULTI_MESSAGE_SEPARATOR}.
     */
    public String buildAndValidateNewRx(
            Prescription prescription,
            PrescriptionTransmission transmission,
            PharmacyNetwork network) {

        List<String> docs = buildAndValidateAllNewRxDocuments(prescription, transmission, network);
        if (docs.size() == 1) {
            return docs.get(0);
        }
        return String.join(NCPDP_MULTI_MESSAGE_SEPARATOR, docs);
    }

    private static List<PrescriptionMedication> orderedMedicationLines(Prescription prescription) {
        if (prescription.getMedications() == null || prescription.getMedications().isEmpty()) {
            return List.of();
        }
        return prescription.getMedications().stream()
                .sorted(Comparator.comparing(PrescriptionMedication::getLineNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------------
    // NPI pre-flight validation
    // ---------------------------------------------------------------------------

    /**
     * Validates all NPIs that will appear in the outbound NCPDP SCRIPT message
     * using the CMS Luhn checksum.  Throws {@link NcpdpBuildException} on the
     * first failure so the transmission is aborted before any XML is constructed.
     *
     * <p>Checks performed:
     * <ul>
     *   <li>Prescriber NPI — required; placed in {@code <From>} header and
     *       {@code <Prescriber>/<Identification>/<NPI>}.</li>
     *   <li>Pharmacy NPI — validated when present (falls back gracefully in routing
     *       if absent, but an <em>invalid</em> value must not be transmitted).</li>
     * </ul>
     */
    private void runNpiPreflightChecks(Prescription prescription, PrescriptionTransmission transmission) {
        String prescriberNpi = coalesce(prescription.getPrescribingProviderNpi(), "");
        if (hasText(prescriberNpi)) {
            String msg = npiValidator.validationMessage(prescriberNpi);
            if (msg != null) {
                throw new NcpdpBuildException(
                        "NCPDP pre-flight: prescriber NPI failed Luhn validation — " + msg);
            }
        }

        String pharmacyNpi = coalesce(transmission.getPharmacyNpi(), prescription.getPharmacyNpi(), "");
        if (hasText(pharmacyNpi)) {
            String msg = npiValidator.validationMessage(pharmacyNpi);
            if (msg != null) {
                throw new NcpdpBuildException(
                        "NCPDP pre-flight: pharmacy NPI failed Luhn validation — " + msg);
            }
        }
    }

    /**
     * FR-P3.6 medication pre-flight: drug name must be present for the segment being encoded.
     *
     * @param line {@code null} for legacy denormalised prescriptions; otherwise the line being built
     */
    private void runPhase1MedicationPreflightCheck(Prescription prescription, PrescriptionMedication line) {
        if (line != null) {
            if (!hasText(line.getMedicationName())) {
                throw new NcpdpBuildException(
                        "NCPDP pre-flight (FR-P3.6): medication line " + line.getLineNumber()
                                + " has no medicationName — cannot build NCPDP DrugDescription.");
            }
            return;
        }
        if (!hasText(prescription.getMedicationName())) {
            throw new NcpdpBuildException(
                    "NCPDP pre-flight (FR-P3.6): prescription-level medicationName is required "
                            + "when no medication line items exist.");
        }
    }

    // ---------------------------------------------------------------------------
    // Exceptions
    // ---------------------------------------------------------------------------

    public static class NcpdpBuildException extends RuntimeException {
        public NcpdpBuildException(String msg, Throwable cause) { super(msg, cause); }
        public NcpdpBuildException(String msg) { super(msg); }
    }

    public static class NcpdpValidationException extends RuntimeException {
        private final String xmlFragment;
        public NcpdpValidationException(String msg, String xmlFragment, Throwable cause) {
            super(msg, cause);
            this.xmlFragment = xmlFragment;
        }
        public String getXmlFragment() { return xmlFragment; }
    }

    // ---------------------------------------------------------------------------
    // Document construction
    // ---------------------------------------------------------------------------

    private Document buildDocument(
            Prescription prescription,
            PrescriptionTransmission transmission,
            PharmacyNetwork network,
            PrescriptionMedication line) throws ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Disable external entity processing (XXE prevention)
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        // ------------------------------------------------------------------ Message (root)
        Element message = createEl(doc, "Message");
        message.setAttribute("DatatypesVersion",   SCRIPT_VERSION);
        message.setAttribute("TransactionDomain",  "SCRIPT");
        message.setAttribute("TransactionVersion", SCRIPT_VERSION);
        message.setAttribute("StructuresVersion",  SCRIPT_VERSION);
        message.setAttribute("ECLVersion",         SCRIPT_VERSION);
        doc.appendChild(message);

        // ------------------------------------------------------------------ Header
        message.appendChild(buildHeader(doc, prescription, transmission, network));

        // ------------------------------------------------------------------ Body
        Element body = createEl(doc, "Body");
        body.appendChild(buildNewRx(doc, prescription, line));
        message.appendChild(body);

        return doc;
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private Element buildHeader(
            Document doc,
            Prescription prescription,
            PrescriptionTransmission transmission,
            PharmacyNetwork network) {

        Element header = createEl(doc, "Header");

        // To: pharmacy identified by NPI (Qualifier NP) or NCPDP ID (Qualifier P)
        String pharmacyNpi = coalesce(
                transmission.getPharmacyNpi(),
                prescription.getPharmacyNpi(),
                "UNKNOWN");
        header.appendChild(qualifiedEl(doc, "To", QUALIFIER_NCPDP_ID, pharmacyNpi));

        // From: prescriber NPI
        String prescriberNpi = coalesce(
                prescription.getPrescribingProviderNpi(),
                "UNKNOWN");
        header.appendChild(qualifiedEl(doc, "From", QUALIFIER_NPI_SENDER, prescriberNpi));

        // MessageID: unique per message
        String messageId = "EASYOPS-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        header.appendChild(textEl(doc, "MessageID", messageId));

        // SentTime
        header.appendChild(textEl(doc, "SentTime",
                LocalDateTime.now().format(NCPDP_DATETIME)));

        // Security block (credentials from network config)
        if (hasText(network.getUsername()) && hasText(network.getPassword())) {
            header.appendChild(buildSecurityBlock(doc, network.getUsername(), network.getPassword()));
        }

        // Sender software identification
        header.appendChild(buildSenderSoftware(doc));

        // TestMessage flag — always "Y" until test-mode=false in config
        header.appendChild(textEl(doc, "TestMessage", testMode ? "Y" : "N"));

        return header;
    }

    private Element buildSecurityBlock(Document doc, String username, String password) {
        Element security = createEl(doc, "Security");
        Element ut = createEl(doc, "UsernameToken");
        ut.appendChild(textEl(doc, "Username", username));
        ut.appendChild(textEl(doc, "Password", password));
        security.appendChild(ut);
        return security;
    }

    private Element buildSenderSoftware(Document doc) {
        Element ss = createEl(doc, "SenderSoftware");
        ss.appendChild(textEl(doc, "SenderSoftwareDeveloper", softwareDeveloper));
        ss.appendChild(textEl(doc, "SenderSoftwareProduct", softwareProduct));
        ss.appendChild(textEl(doc, "SenderSoftwareVersionRelease", softwareVersion));
        return ss;
    }

    // -------------------------------------------------------------------------
    // NewRx (inside Body)
    // -------------------------------------------------------------------------

    private Element buildNewRx(Document doc, Prescription prescription, PrescriptionMedication line) {
        Element newRx = createEl(doc, "NewRx");

        LocalDate written = prescription.getStartDate() != null ? prescription.getStartDate() : LocalDate.now();
        if (line != null && line.getStartDate() != null) {
            written = line.getStartDate();
        }
        newRx.appendChild(dateEl(doc, "WrittenDate", written));

        // Patient
        if (prescription.getPatient() != null) {
            newRx.appendChild(buildPatient(doc, prescription.getPatient()));
        }

        // Prescriber
        newRx.appendChild(buildPrescriber(doc, prescription, line));

        // Pharmacy
        newRx.appendChild(buildPharmacy(doc, prescription));

        // Medication
        newRx.appendChild(buildMedicationPrescribed(doc, prescription, line));

        return newRx;
    }

    // -------------------------------------------------------------------------
    // Patient
    // -------------------------------------------------------------------------

    private Element buildPatient(Document doc, Patient patient) {
        Element patientEl = createEl(doc, "Patient");
        Element humanPatient = createEl(doc, "HumanPatient");
        patientEl.appendChild(humanPatient);

        // Name (required)
        String[] nameParts = PatientNameInterop.splitFullName(patient.getFullName());
        String firstName = nameParts[0];
        String middleName = nameParts[1];
        String lastName = nameParts[2];

        Element name = createEl(doc, "Name");
        name.appendChild(textEl(doc, "LastName", lastName));
        name.appendChild(textEl(doc, "FirstName", firstName));
        if (hasText(middleName)) {
            name.appendChild(textEl(doc, "MiddleName", middleName));
        }
        humanPatient.appendChild(name);

        // Gender (M / F / U)
        humanPatient.appendChild(textEl(doc, "Gender", mapGender(patient.getGender())));

        // Date of birth
        if (patient.getDateOfBirth() != null) {
            humanPatient.appendChild(dateEl(doc, "DateOfBirth", patient.getDateOfBirth()));
        }

        // Address
        if (hasText(patient.getPrimaryAddressLine1())) {
            humanPatient.appendChild(buildAddress(doc,
                    patient.getPrimaryAddressLine1(),
                    patient.getPrimaryCity(),
                    patient.getPrimaryState(),
                    patient.getPrimaryZip(),
                    patient.getPrimaryCountry()));
        }

        // Identification — patient MRN (optional; useful for HL7 reconciliation)
        if (hasText(patient.getMrn())) {
            Element identification = createEl(doc, "Identification");
            Element patientId = createEl(doc, "MedicalRecordIdentificationNumberEHR");
            patientId.setTextContent(patient.getMrn());
            identification.appendChild(patientId);
            humanPatient.appendChild(identification);
        }

        return patientEl;
    }

    // -------------------------------------------------------------------------
    // Prescriber
    // -------------------------------------------------------------------------

    private Element buildPrescriber(Document doc, Prescription prescription, PrescriptionMedication line) {
        Element prescriber = createEl(doc, "Prescriber");
        Element nonVet = createEl(doc, "NonVeterinarian");
        prescriber.appendChild(nonVet);

        // Name (required)
        String providerName = coalesce(prescription.getPrescribingProviderName(), "");
        String[] nameParts = PatientNameInterop.splitFullName(providerName);
        Element name = createEl(doc, "Name");
        name.appendChild(textEl(doc, "LastName", nameParts[2]));
        name.appendChild(textEl(doc, "FirstName", nameParts[0]));
        if (hasText(nameParts[1])) {
            name.appendChild(textEl(doc, "MiddleName", nameParts[1]));
        }
        nonVet.appendChild(name);

        // Identification (required — at minimum NPI)
        Element identification = createEl(doc, "Identification");

        String npi = coalesce(prescription.getPrescribingProviderNpi(), "");
        if (hasText(npi)) {
            identification.appendChild(textEl(doc, "NPI", npi));
        }
        String deaForLine = line != null && hasText(line.getDeaNumber())
                ? line.getDeaNumber()
                : prescription.getDeaNumber();
        if (hasText(deaForLine)) {
            identification.appendChild(textEl(doc, "DEANumber", deaForLine));
        }
        nonVet.appendChild(identification);

        return prescriber;
    }

    // -------------------------------------------------------------------------
    // Pharmacy
    // -------------------------------------------------------------------------

    private Element buildPharmacy(Document doc, Prescription prescription) {
        Element pharmacy = createEl(doc, "Pharmacy");

        // Identification
        Element identification = createEl(doc, "Identification");
        String npi = coalesce(prescription.getPharmacyNpi(), "");
        if (hasText(npi)) {
            identification.appendChild(textEl(doc, "NPI", npi));
        }
        pharmacy.appendChild(identification);

        // Store name
        if (hasText(prescription.getPharmacyName())) {
            pharmacy.appendChild(textEl(doc, "StoreName", prescription.getPharmacyName()));
        }

        // Address
        if (hasText(prescription.getPharmacyAddressLine1())) {
            pharmacy.appendChild(buildAddress(doc,
                    prescription.getPharmacyAddressLine1(),
                    prescription.getPharmacyCity(),
                    prescription.getPharmacyState(),
                    prescription.getPharmacyZip(),
                    null));
        }

        // Phone
        if (hasText(prescription.getPharmacyPhone())) {
            pharmacy.appendChild(buildCommunicationNumbers(doc, prescription.getPharmacyPhone()));
        }

        return pharmacy;
    }

    // -------------------------------------------------------------------------
    // MedicationPrescribed
    // -------------------------------------------------------------------------

    private Element buildMedicationPrescribed(Document doc, Prescription prescription, PrescriptionMedication line) {
        if (line != null) {
            return buildMedicationPrescribedFromLine(doc, prescription, line);
        }
        Element med = createEl(doc, "MedicationPrescribed");

        med.appendChild(textEl(doc, "DrugDescription",
                coalesce(prescription.getMedicationName(), "UNKNOWN")));

        if (hasText(prescription.getMedicationCode())) {
            Element drugCoded = createEl(doc, "DrugCoded");
            String qualifier = resolveCodeQualifier(prescription);
            Element productCode = createEl(doc, "ProductCode");
            productCode.setAttribute("Qualifier", qualifier);
            productCode.setTextContent(prescription.getMedicationCode());
            drugCoded.appendChild(productCode);
            med.appendChild(drugCoded);
        }

        {
            String qv = PrescriptionDerivedQuantity.deriveUnits(
                    prescription.getFrequency(), prescription.getDurationDays()).toPlainString();
            Element quantity = createEl(doc, "Quantity");
            quantity.appendChild(textEl(doc, "Value", qv));
            quantity.appendChild(textEl(doc, "CodeListQualifier", "EA"));
            med.appendChild(quantity);
        }

        if (prescription.getDurationDays() != null) {
            med.appendChild(textEl(doc, "DaysSupply", String.valueOf(prescription.getDurationDays())));
        }

        String sigText = coalesce(prescription.getInstructions(), prescription.getFrequency(), "");
        if (hasText(sigText)) {
            Element sig = createEl(doc, "Sig");
            sig.appendChild(textEl(doc, "SigText", sigText));
            med.appendChild(sig);
        }

        int refills = prescription.getRefillsAuthorized() != null ? prescription.getRefillsAuthorized() : 0;
        Element refillsEl = createEl(doc, "Refills");
        refillsEl.appendChild(textEl(doc, "Qualifier", "DF"));
        refillsEl.appendChild(textEl(doc, "Value", String.valueOf(refills)));
        med.appendChild(refillsEl);

        String substitutionCode = Boolean.FALSE.equals(prescription.getSubstitutionAllowed()) ? "1" : "0";
        if (hasText(prescription.getDawCode())) {
            substitutionCode = prescription.getDawCode();
        }
        med.appendChild(textEl(doc, "Substitutions", substitutionCode));

        LocalDate written = prescription.getStartDate() != null ? prescription.getStartDate() : LocalDate.now();
        med.appendChild(dateEl(doc, "WrittenDate", written));

        if (prescription.getExpirationDate() != null) {
            med.appendChild(dateEl(doc, "LastFillDate", prescription.getExpirationDate()));
        }

        appendDiagnosisAndNote(med, doc, prescription);

        return med;
    }

    private Element buildMedicationPrescribedFromLine(Document doc, Prescription prescription, PrescriptionMedication line) {
        Element med = createEl(doc, "MedicationPrescribed");

        med.appendChild(textEl(doc, "DrugDescription", line.getMedicationName()));

        if (hasText(line.getMedicationCode())) {
            Element drugCoded = createEl(doc, "DrugCoded");
            String qualifier = resolveCodeQualifier(line);
            Element productCode = createEl(doc, "ProductCode");
            productCode.setAttribute("Qualifier", qualifier);
            productCode.setTextContent(line.getMedicationCode());
            drugCoded.appendChild(productCode);
            med.appendChild(drugCoded);
        }

        {
            String qv = PrescriptionDerivedQuantity.deriveUnits(
                    line.getFrequency(), line.getDurationDays()).toPlainString();
            Element quantity = createEl(doc, "Quantity");
            quantity.appendChild(textEl(doc, "Value", qv));
            quantity.appendChild(textEl(doc, "CodeListQualifier", "EA"));
            med.appendChild(quantity);
        }

        if (line.getDurationDays() != null) {
            med.appendChild(textEl(doc, "DaysSupply", String.valueOf(line.getDurationDays())));
        }

        String sigText = coalesce(line.getInstructions(), line.getFrequency(), "");
        if (hasText(sigText)) {
            Element sig = createEl(doc, "Sig");
            sig.appendChild(textEl(doc, "SigText", sigText));
            med.appendChild(sig);
        }

        int refills = line.getRefillsAuthorized() != null ? line.getRefillsAuthorized() : 0;
        Element refillsEl = createEl(doc, "Refills");
        refillsEl.appendChild(textEl(doc, "Qualifier", "DF"));
        refillsEl.appendChild(textEl(doc, "Value", String.valueOf(refills)));
        med.appendChild(refillsEl);

        String substitutionCode = Boolean.FALSE.equals(line.getSubstitutionAllowed()) ? "1" : "0";
        if (hasText(line.getDawCode())) {
            substitutionCode = line.getDawCode();
        }
        med.appendChild(textEl(doc, "Substitutions", substitutionCode));

        LocalDate written = line.getStartDate() != null ? line.getStartDate()
                : (prescription.getStartDate() != null ? prescription.getStartDate() : LocalDate.now());
        med.appendChild(dateEl(doc, "WrittenDate", written));

        if (prescription.getExpirationDate() != null) {
            med.appendChild(dateEl(doc, "LastFillDate", prescription.getExpirationDate()));
        }

        appendDiagnosisAndNote(med, doc, prescription);

        return med;
    }

    private void appendDiagnosisAndNote(Element med, Document doc, Prescription prescription) {
        String diagCode = primaryDiagnosisCode(prescription);
        if (hasText(diagCode)) {
            Element diagnosis = createEl(doc, "Diagnosis");
            Element principal = createEl(doc, "Principal");
            Element diagEl = createEl(doc, "DiagnosisCode");
            diagEl.setAttribute("Qualifier", QUALIFIER_ICD10);
            diagEl.setTextContent(diagCode);
            principal.appendChild(diagEl);
            diagnosis.appendChild(principal);
            med.appendChild(diagnosis);
        }

        String note = coalesce(prescription.getSpecialInstructions(), prescription.getNotes(), "");
        if (hasText(note)) {
            med.appendChild(textEl(doc, "Note", note));
        }
    }

    private static String resolveCodeQualifier(PrescriptionMedication line) {
        if (line.getMedicationCodeType() == null) {
            return QUALIFIER_DRUG_NAME;
        }
        return switch (line.getMedicationCodeType()) {
            case NDC -> QUALIFIER_NDC;
            case RXNORM -> QUALIFIER_RXNORM;
            default -> QUALIFIER_DRUG_NAME;
        };
    }

    // -------------------------------------------------------------------------
    // Shared element builders
    // -------------------------------------------------------------------------

    private Element buildAddress(Document doc,
                                  String line1, String city,
                                  String state, String zip,
                                  String country) {
        Element address = createEl(doc, "Address");
        if (hasText(line1))    address.appendChild(textEl(doc, "AddressLine1", line1));
        if (hasText(city))     address.appendChild(textEl(doc, "City",         city));
        if (hasText(state))    address.appendChild(textEl(doc, "State",        state));
        if (hasText(zip))      address.appendChild(textEl(doc, "ZipCode",      zip));
        if (hasText(country))  address.appendChild(textEl(doc, "CountryCode",  country));
        return address;
    }

    private Element buildCommunicationNumbers(Document doc, String phone) {
        Element comm = createEl(doc, "CommunicationNumbers");
        Element tel  = createEl(doc, "PrimaryTelephone");
        tel.appendChild(textEl(doc, "Number", digitsOnly(phone)));
        comm.appendChild(tel);
        return comm;
    }

    /** Creates an element in the NCPDP namespace. */
    private static Element createEl(Document doc, String localName) {
        return doc.createElementNS(NCPDP_NS, localName);
    }

    /** Creates a text element in the NCPDP namespace. */
    private static Element textEl(Document doc, String localName, String text) {
        Element el = createEl(doc, localName);
        el.setTextContent(text != null ? text : "");
        return el;
    }

    /** Creates a `<Name[@Qualifier]>text</Name>` element. */
    private static Element qualifiedEl(Document doc, String localName, String qualifier, String text) {
        Element el = createEl(doc, localName);
        el.setAttribute("Qualifier", qualifier);
        el.setTextContent(text);
        return el;
    }

    /** Creates a `<ParentEl><Date>YYYYMMDD</Date></ParentEl>` block. */
    private static Element dateEl(Document doc, String parentName, LocalDate date) {
        Element parent = createEl(doc, parentName);
        parent.appendChild(textEl(doc, "Date", date.format(NCPDP_DATE)));
        return parent;
    }

    // -------------------------------------------------------------------------
    // Serialization
    // -------------------------------------------------------------------------

    private String serializeToXml(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new NcpdpBuildException("Failed to serialize NCPDP SCRIPT XML: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // XSD validation
    // -------------------------------------------------------------------------

    /**
     * Validates the XML string against the bundled NCPDP SCRIPT 2017071 XSD.
     *
     * @throws NcpdpValidationException if the XML fails validation
     */
    public void validateAgainstXsd(String xml) {
        try {
            Schema schema = getOrLoadSchema();
            Validator validator = schema.newValidator();
            // Prevent XXE in validator
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            String fragment = xml.length() > 500 ? xml.substring(0, 500) + "..." : xml;
            log.error("NCPDP SCRIPT XSD validation failed: {}\nXML fragment:\n{}", e.getMessage(), fragment);
            throw new NcpdpValidationException(
                    "NCPDP SCRIPT 2017071 XSD validation failed: " + e.getMessage(), fragment, e);
        } catch (IOException e) {
            throw new NcpdpBuildException("IO error during XSD validation: " + e.getMessage(), e);
        }
    }

    private Schema getOrLoadSchema() {
        if (compiledSchema != null) return compiledSchema;
        synchronized (this) {
            if (compiledSchema == null) {
                try {
                    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    var resource = getClass().getResourceAsStream(XSD_RESOURCE);
                    if (resource == null) {
                        throw new NcpdpBuildException("NCPDP SCRIPT XSD not found on classpath: " + XSD_RESOURCE);
                    }
                    compiledSchema = sf.newSchema(new StreamSource(resource));
                    log.info("NCPDP SCRIPT 2017071 XSD loaded from {}", XSD_RESOURCE);
                } catch (SAXException e) {
                    throw new NcpdpBuildException("Failed to compile NCPDP SCRIPT XSD: " + e.getMessage(), e);
                }
            }
        }
        return compiledSchema;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String mapGender(Patient.Gender gender) {
        if (gender == null) return "U";
        return switch (gender) {
            case Male   -> "M";
            case Female -> "F";
            default     -> "U";
        };
    }

    private static String resolveCodeQualifier(Prescription prescription) {
        if (prescription.getMedicationCodeType() == null) return QUALIFIER_DRUG_NAME;
        return switch (prescription.getMedicationCodeType()) {
            case NDC    -> QUALIFIER_NDC;
            case RXNORM -> QUALIFIER_RXNORM;
            default     -> QUALIFIER_DRUG_NAME;
        };
    }

    private static String primaryDiagnosisCode(Prescription prescription) {
        // Prefer normalised diagnoses list (FR-P1.4a)
        if (prescription.getDiagnoses() != null && !prescription.getDiagnoses().isEmpty()) {
            return prescription.getDiagnoses().stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()))
                    .findFirst()
                    .map(d -> d.getDiagnosisCode())
                    .orElse(prescription.getDiagnoses().get(0).getDiagnosisCode());
        }
        return prescription.getDiagnosisCode();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    /** Strip non-digit characters from a phone number for NCPDP CommunicationNumbers. */
    private static String digitsOnly(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
}
