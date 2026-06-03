package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.exception.DuplicatePatientConflictException;
import com.easyops.hospital.exception.UnprocessableEntityException;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.integration.card.PatientIdentityCardIssuanceResult;
import com.easyops.hospital.integration.card.PatientIdentityCardIssuanceService;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.EmergencyContactRequestValidation;
import com.easyops.hospital.util.InsuranceRequestValidation;
import com.easyops.hospital.util.PatientPhoneNormalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    /**
     * When GET patient is called without {@code X-User-Id} (internal clients), use this user for card-service
     * resolution so {@link PatientResponse} identity fields can still populate. Empty = skip enrichment in that case.
     */
    @Value("${hospital.patient-identity-card.get-patient-enrichment-user-id:}")
    private String patientIdentityCardGetPatientEnrichmentUserId;
    
    private final PatientRepository patientRepository;
    private final PatientEmergencyContactRepository emergencyContactRepository;
    private final PatientInsuranceRepository insuranceRepository;
    private final PatientConsentRepository consentRepository;
    private final PatientIdentityCardAuditLogRepository patientIdentityCardAuditLogRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PatientIdentityCardIssuanceService patientIdentityCardIssuanceService;
    
    /**
     * Generate unique Medical Record Number (MRN)
     * Format: ORGCODE-YYSERIAL (e.g., ORG123-26000000001)
     * Serial is 9-digit zero-padded numeric sequence per org/year.
     */
    public String generateMrn(UUID organizationId) {
        String year = String.format("%02d", LocalDateTime.now().getYear() % 100);
        String orgCode = deriveOrgCode(organizationId);
        String prefix = orgCode + "-" + year;
        
        Long maxSequence = patientRepository.findMaxMrnSequence(prefix, prefix.length());
        if (maxSequence == null) {
            maxSequence = 0L;
        }
        
        long nextSequence = maxSequence + 1;
        String sequence = String.format("%09d", nextSequence);
        
        String mrn = prefix + sequence;
        
        // Ensure uniqueness
        int attempts = 0;
        while (patientRepository.existsByMrn(mrn) && attempts < 10) {
            nextSequence++;
            sequence = String.format("%09d", nextSequence);
            mrn = prefix + sequence;
            attempts++;
        }
        
        if (attempts >= 10) {
            throw new RuntimeException("Unable to generate unique MRN after multiple attempts");
        }
        
        return mrn;
    }

    private String deriveOrgCode(UUID organizationId) {
        if (organizationId == null) {
            return "ORG";
        }
        // Use a stable short code from organization UUID when explicit org code is unavailable.
        String compact = organizationId.toString().replace("-", "").toUpperCase();
        String code = compact.substring(0, Math.min(6, compact.length()));
        return code.isBlank() ? "ORG" : code;
    }
    
    /**
     * Check for duplicate patients
     */
    public DuplicatePatientResponse checkForDuplicates(PatientRequest request) {
        normalizePatientRegistrationRequest(request);
        List<DuplicatePatientResponse.DuplicateMatch> matches = new ArrayList<>();
        UUID excludePatientId = request.getExcludePatientId();

        // Check exact name and DOB match (skip when caller sends phone-only partial check)
        String fullName = normalizeFullName(request.getFullName());
        if (!fullName.isBlank() && request.getDateOfBirth() != null) {
            List<Patient> nameDobMatches = patientRepository.findPotentialDuplicates(
                fullName,
                request.getDateOfBirth()
            );

            for (Patient patient : nameDobMatches) {
                if (isExcludedPatient(patient, excludePatientId)) {
                    continue;
                }
                matches.add(createDuplicateMatch(patient, "Exact match on name and date of birth"));
            }
        }

        // Check ID number match
        if (request.getIdNo() != null && !request.getIdNo().trim().isEmpty()) {
            List<Patient> idNoMatches = patientRepository.findByIdNoForDuplicateCheck(request.getIdNo());
            for (Patient patient : idNoMatches) {
                if (isExcludedPatient(patient, excludePatientId)) {
                    continue;
                }
                if (!matches.stream().anyMatch(m -> m.getPatientId().equals(patient.getPatientId()))) {
                    matches.add(createDuplicateMatch(patient, "Exact match on photo ID"));
                }
            }
        }
        
        // Check phone match (normalized digits, active patients)
        String phoneDigits = PatientPhoneNormalization.normalizeDigits(request.getPrimaryPhone());
        if (PatientPhoneNormalization.isEligibleForUniquenessCheck(request.getPrimaryPhone())) {
            for (UUID patientId : findConflictingPatientIds(phoneDigits, request.getOrganizationId(), request.getExcludePatientId())) {
                if (matches.stream().anyMatch(m -> m.getPatientId().equals(patientId))) {
                    continue;
                }
                patientRepository.findById(patientId).ifPresent(patient ->
                        matches.add(createDuplicateMatch(patient, "Another patient uses this mobile number")));
            }
        }
        
        // Check email match
        if (request.getPrimaryEmail() != null && !request.getPrimaryEmail().trim().isEmpty()) {
            List<Patient> emailMatches = patientRepository.findByEmailForDuplicateCheck(request.getPrimaryEmail());
            for (Patient patient : emailMatches) {
                if (isExcludedPatient(patient, excludePatientId)) {
                    continue;
                }
                if (!matches.stream().anyMatch(m -> m.getPatientId().equals(patient.getPatientId()))) {
                    matches.add(createDuplicateMatch(patient, "Match on email address"));
                }
            }
        }
        
        return DuplicatePatientResponse.builder()
            .hasDuplicates(!matches.isEmpty())
            .phoneDuplicateBlocked(false)
            .matchReason(null)
            .matches(matches)
            .build();
    }

    private boolean hasBlockingDuplicates(DuplicatePatientResponse duplicateResponse) {
        if (!Boolean.TRUE.equals(duplicateResponse.getHasDuplicates())) {
            return false;
        }
        if (duplicateResponse.getMatches() == null || duplicateResponse.getMatches().isEmpty()) {
            return false;
        }
        return duplicateResponse.getMatches().stream().anyMatch(m -> !isPhoneRelatedMatch(m.getMatchReason()));
    }

    private static boolean isPhoneRelatedMatch(String matchReason) {
        if (matchReason == null) {
            return false;
        }
        String lower = matchReason.toLowerCase(Locale.ROOT);
        return lower.contains("mobile") || lower.contains("phone");
    }
    
    private DuplicatePatientResponse.DuplicateMatch createDuplicateMatch(Patient patient, String reason) {
        return DuplicatePatientResponse.DuplicateMatch.builder()
            .patientId(patient.getPatientId())
            .mrn(patient.getMrn())
            .fullName(patient.getFullName())
            .dateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null)
            .idNo(patient.getIdNo())
            .phone(patient.getPrimaryPhone())
            .email(patient.getPrimaryEmail())
            .matchScore(1.0)
            .matchReason(reason)
            .build();
    }
    
    private String normalizeFullName(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean isExcludedPatient(Patient patient, UUID excludePatientId) {
        return excludePatientId != null && excludePatientId.equals(patient.getPatientId());
    }

    private List<UUID> findConflictingPatientIds(String phoneDigits, UUID organizationId, UUID excludePatientId) {
        if (organizationId != null) {
            if (excludePatientId != null) {
                return patientRepository.findActivePatientIdsByNormalizedPhoneForOrganizationExcluding(
                        phoneDigits, organizationId, excludePatientId);
            }
            return patientRepository.findActivePatientIdsByNormalizedPhoneForOrganization(phoneDigits, organizationId);
        }
        if (excludePatientId != null) {
            return patientRepository.findActivePatientIdsByNormalizedPhoneExcluding(phoneDigits, excludePatientId);
        }
        return patientRepository.findActivePatientIdsByNormalizedPhone(phoneDigits);
    }

    private String normalizeStoredPhone(String phone) {
        String digits = PatientPhoneNormalization.normalizeForStorage(phone);
        return digits.isEmpty() ? null : digits;
    }

    private void validatePhoneFields(PatientRequest request) {
        validateOptionalPhoneLength(request.getPrimaryPhone(), "Primary phone");
        validateOptionalPhoneLength(request.getSecondaryPhone(), "Secondary phone");
    }

    private void validateOptionalPhoneLength(String phone, String label) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        String digits = PatientPhoneNormalization.normalizeDigits(phone);
        if (!digits.isEmpty() && digits.length() < PatientPhoneNormalization.MIN_DIGITS_FOR_UNIQUENESS) {
            throw new UnprocessableEntityException(
                    label + " must be at least " + PatientPhoneNormalization.MIN_DIGITS_FOR_UNIQUENESS
                            + " digits, or be empty.",
                    "INVALID_PHONE");
        }
    }

    private String resolveStoredFullName(PatientRequest request) {
        return normalizeFullName(request.getFullName());
    }

    private static final java.util.regex.Pattern ID_NO_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9\\-/\\\\]*$");

    /** When only age is supplied (e.g. appointment quick registration), derive an approximate DOB. */
    private void normalizePatientRegistrationRequest(PatientRequest request) {
        if (request.getFullName() != null) {
            request.setFullName(request.getFullName().trim());
        }
        if (request.getIdNo() != null) {
            String trimmed = request.getIdNo().trim();
            request.setIdNo(trimmed.isEmpty() ? null : trimmed);
        }
        validateIdNo(request.getIdNo());
        resolveDateOfBirthFromAge(request);
    }

    private void validateIdNo(String idNo) {
        if (idNo == null || idNo.isBlank()) {
            return;
        }
        if (!ID_NO_PATTERN.matcher(idNo).matches()) {
            throw new IllegalArgumentException(
                    "ID number may only contain letters, digits, hyphen (-), forward slash (/), and backslash (\\)");
        }
    }

    private void resolveDateOfBirthFromAge(PatientRequest request) {
        if (request.getDateOfBirth() != null) {
            return;
        }
        if (request.getAgeYears() == null) {
            return;
        }
        request.setDateOfBirth(LocalDate.now().minusYears(request.getAgeYears()));
    }
    
    /**
     * Create a new patient
     *
     * @param acknowledgeDuplicate when true, skip duplicate check (caller already acknowledged possible duplicates)
     */
    @Transactional
    public PatientResponse createPatient(PatientRequest request, UUID userId, boolean acknowledgeDuplicate) {
        log.info("Creating new patient: {}", request.getFullName());

        normalizePatientRegistrationRequest(request);

        validatePhoneFields(request);

        if (!acknowledgeDuplicate) {
            DuplicatePatientResponse dup = checkForDuplicates(request);
            if (hasBlockingDuplicates(dup)) {
                throw new DuplicatePatientConflictException(dup);
            }
        }
        
        // Generate MRN
        String mrn = generateMrn(request.getOrganizationId());
        
        // Build patient entity
        Patient patient = Patient.builder()
            .mrn(mrn)
            .fullName(resolveStoredFullName(request))
            .preferredName(request.getPreferredName())
            .dateOfBirth(request.getDateOfBirth())
            .gender(request.getGender())
            .sexAtBirth(request.getSexAtBirth())
            .idNo(request.getIdNo())
            .idType(request.getIdType())
            .race(request.getRace())
            .ethnicity(request.getEthnicity())
            .maritalStatus(request.getMaritalStatus())
            .patientType(request.getPatientType())
            .fatherName(request.getFatherName())
            .motherName(request.getMotherName())
            .spouseName(request.getSpouseName())
            .bloodGroup(request.getBloodGroup())
            .religion(request.getReligion())
            .occupation(request.getOccupation())
            .introducedBy(request.getIntroducedBy())
            .primaryAddressLine1(request.getPrimaryAddressLine1())
            .primaryAddressLine2(request.getPrimaryAddressLine2())
            .primaryCity(request.getPrimaryCity())
            .primaryState(request.getPrimaryState())
            .primaryZip(request.getPrimaryZip())
            .primaryCountry(request.getPrimaryCountry() != null ? request.getPrimaryCountry() : "Bangladesh")
            .mailingAddressLine1(request.getMailingAddressLine1())
            .mailingAddressLine2(request.getMailingAddressLine2())
            .mailingCity(request.getMailingCity())
            .mailingState(request.getMailingState())
            .mailingZip(request.getMailingZip())
            .mailingCountry(request.getMailingCountry())
            .primaryPhone(normalizeStoredPhone(request.getPrimaryPhone()))
            .primaryPhoneType(request.getPrimaryPhoneType())
            .secondaryPhone(normalizeStoredPhone(request.getSecondaryPhone()))
            .secondaryPhoneType(request.getSecondaryPhoneType())
            .primaryEmail(request.getPrimaryEmail())
            .secondaryEmail(request.getSecondaryEmail())
            .preferredContactMethod(request.getPreferredContactMethod())
            .consentTextMessaging(request.getConsentTextMessaging() != null ? request.getConsentTextMessaging() : true)
            .consentEmailCommunication(request.getConsentEmailCommunication() != null ? request.getConsentEmailCommunication() : false)
            .primaryCareProviderId(request.getPrimaryCareProviderId())
            .primaryCareLocationId(request.getPrimaryCareLocationId())
            .referringPhysicianId(request.getReferringPhysicianId())
            .patientStatus(request.getPatientStatus() != null ? request.getPatientStatus() : Patient.PatientStatus.ACTIVE)
            .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English")
            .interpreterNeeded(request.getInterpreterNeeded() != null ? request.getInterpreterNeeded() : false)
            .specialNeeds(request.getSpecialNeeds())
            .registeredBy(userId)
            .registrationLocationId(request.getRegistrationLocationId())
            .organizationId(request.getOrganizationId())
            .createdBy(userId)
            .build();
        
        patient = patientRepository.save(patient);
        final Patient savedPatient = patient;
        
        // Save emergency contacts
        if (request.getEmergencyContacts() != null && !request.getEmergencyContacts().isEmpty()) {
            List<PatientEmergencyContact> contacts = request.getEmergencyContacts().stream()
                .map(ec -> createEmergencyContact(savedPatient, ec, userId))
                .collect(Collectors.toList());
            emergencyContactRepository.saveAll(contacts);
        }
        
        // Save insurance
        if (request.getInsuranceList() != null && !request.getInsuranceList().isEmpty()) {
            List<PatientInsurance> insuranceList = request.getInsuranceList().stream()
                .map(ins -> createInsurance(savedPatient, ins, userId))
                .collect(Collectors.toList());
            insuranceRepository.saveAll(insuranceList);
        }
        
        // Save consents
        if (request.getConsents() != null && !request.getConsents().isEmpty()) {
            List<PatientConsent> consents = request.getConsents().stream()
                .map(cons -> createConsent(savedPatient, cons, userId))
                .collect(Collectors.toList());
            consentRepository.saveAll(consents);
        }
        
        PatientResponse response = mapToResponse(patient);

        // Emit domain event (Map.of cannot contain null values; organizationId is often unset at registration)
        domainEventPublisher.publish("patient.created", buildPatientEventPayload(
            response.getPatientId(), response.getMrn(), response.getOrganizationId()));

        PatientIdentityCardIssuanceResult cardResult = patientIdentityCardIssuanceService.issueOrResolveForNewPatient(
                response.getPatientId(), userId, response.getMrn(), response.getOrganizationId());
        response.setIdentityCardStatus(cardResult.getStatus());
        response.setIdentityCardId(cardResult.getCardId());
        response.setIdentityCardNumber(cardResult.getCardNumber());
        response.setIdentityCardMessage(cardResult.getMessage());
        if ("ISSUED".equals(cardResult.getStatus()) && cardResult.getCardId() != null) {
            Map<String, Object> issuedPayload = new HashMap<>();
            issuedPayload.put("patientId", response.getPatientId());
            issuedPayload.put("mrn", response.getMrn());
            issuedPayload.put("cardIdentifier", cardResult.getCardNumber());
            issuedPayload.put("cardId", cardResult.getCardId());
            issuedPayload.put("issuedBy", userId);
            issuedPayload.put("issuedAt", OffsetDateTime.now().toString());
            if (response.getOrganizationId() != null) {
                issuedPayload.put("organizationId", response.getOrganizationId());
            }
            domainEventPublisher.publish("patient.identity_card.issued", issuedPayload);
        }

        return response;
    }

    private Map<String, Object> buildPatientEventPayload(UUID patientId, String mrn, UUID organizationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientId", patientId);
        payload.put("mrn", mrn);
        if (organizationId != null) {
            payload.put("organizationId", organizationId);
        }
        return payload;
    }
    
    private PatientEmergencyContact createEmergencyContact(Patient patient, EmergencyContactRequest request, UUID userId) {
        EmergencyContactRequestValidation.assertValid(request);
        return PatientEmergencyContact.builder()
            .patient(patient)
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
            .createdBy(userId)
            .build();
    }
    
    private PatientInsurance createInsurance(Patient patient, InsuranceRequest request, UUID userId) {
        InsuranceRequestValidation.assertEffectiveBeforeExpiration(request);
        return PatientInsurance.builder()
            .patient(patient)
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
            .verificationStatus(request.getVerificationStatus() != null ? request.getVerificationStatus() : PatientInsurance.VerificationStatus.Not_Verified)
            .verifiedDate(request.getVerifiedDate())
            .insurancePhone(request.getInsurancePhone())
            .createdBy(userId)
            .build();
    }
    
    private PatientConsent createConsent(Patient patient, ConsentRequest request, UUID userId) {
        return PatientConsent.builder()
            .patient(patient)
            .consentType(request.getConsentType())
            .consentStatus(request.getConsentStatus())
            .consentDate(request.getConsentDate())
            .signature(request.getSignature())
            .expiresDate(request.getExpiresDate())
            .notes(request.getNotes())
            .createdBy(userId)
            .build();
    }
    
    /**
     * Get patient by ID
     */
    public PatientResponse getPatientById(UUID patientId, UUID actorUserId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: " + patientId));
        PatientResponse response = mapToResponse(patient);
        enrichPatientResponseWithIdentityCard(response, actorUserId);
        return response;
    }
    
    /**
     * Get patient by MRN
     */
    public PatientResponse getPatientByMrn(String mrn, UUID actorUserId) {
        Patient patient = patientRepository.findByMrn(mrn)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with MRN: " + mrn));
        PatientResponse response = mapToResponse(patient);
        enrichPatientResponseWithIdentityCard(response, actorUserId);
        return response;
    }
    
    /**
     * Get all patients
     */
    public List<PatientResponse> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search patients
     */
    public List<PatientResponse> searchPatients(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            return List.of();
        }
        String term = searchTerm.trim();
        List<Patient> patients = new ArrayList<>(patientRepository.searchPatients(term));
        String phoneDigits = term.replaceAll("\\D", "");
        if (phoneDigits.length() >= 2) {
            java.util.Set<UUID> seen = patients.stream()
                    .map(Patient::getPatientId)
                    .collect(Collectors.toSet());
            for (Patient byPhone : patientRepository.searchPatientsByNormalizedPhone(phoneDigits)) {
                if (seen.add(byPhone.getPatientId())) {
                    patients.add(byPhone);
                }
            }
        }
        return patients.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update patient
     */
    @Transactional
    public PatientResponse updatePatient(UUID patientId, PatientRequest request, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: " + patientId));

        validatePhoneFields(request);
        if (request.getIdNo() != null) {
            String trimmed = request.getIdNo().trim();
            request.setIdNo(trimmed.isEmpty() ? null : trimmed);
        }
        validateIdNo(request.getIdNo());

        // Update patient fields
        patient.setFullName(resolveStoredFullName(request));
        patient.setPreferredName(request.getPreferredName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setSexAtBirth(request.getSexAtBirth());
        patient.setIdNo(request.getIdNo());
        patient.setIdType(request.getIdType());
        patient.setRace(request.getRace());
        patient.setEthnicity(request.getEthnicity());
        patient.setMaritalStatus(request.getMaritalStatus());
        patient.setPatientType(request.getPatientType());
        patient.setFatherName(request.getFatherName());
        patient.setMotherName(request.getMotherName());
        patient.setSpouseName(request.getSpouseName());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setReligion(request.getReligion());
        patient.setOccupation(request.getOccupation());
        patient.setIntroducedBy(request.getIntroducedBy());
        patient.setPrimaryAddressLine1(request.getPrimaryAddressLine1());
        patient.setPrimaryAddressLine2(request.getPrimaryAddressLine2());
        patient.setPrimaryCity(request.getPrimaryCity());
        patient.setPrimaryState(request.getPrimaryState());
        patient.setPrimaryZip(request.getPrimaryZip());
        patient.setPrimaryCountry(request.getPrimaryCountry());
        patient.setMailingAddressLine1(request.getMailingAddressLine1());
        patient.setMailingAddressLine2(request.getMailingAddressLine2());
        patient.setMailingCity(request.getMailingCity());
        patient.setMailingState(request.getMailingState());
        patient.setMailingZip(request.getMailingZip());
        patient.setMailingCountry(request.getMailingCountry());
        patient.setPrimaryPhone(normalizeStoredPhone(request.getPrimaryPhone()));
        patient.setPrimaryPhoneType(request.getPrimaryPhoneType());
        patient.setSecondaryPhone(normalizeStoredPhone(request.getSecondaryPhone()));
        patient.setSecondaryPhoneType(request.getSecondaryPhoneType());
        patient.setPrimaryEmail(request.getPrimaryEmail());
        patient.setSecondaryEmail(request.getSecondaryEmail());
        patient.setPreferredContactMethod(request.getPreferredContactMethod());
        patient.setConsentTextMessaging(request.getConsentTextMessaging());
        patient.setConsentEmailCommunication(request.getConsentEmailCommunication());
        patient.setPrimaryCareProviderId(request.getPrimaryCareProviderId());
        patient.setPrimaryCareLocationId(request.getPrimaryCareLocationId());
        patient.setReferringPhysicianId(request.getReferringPhysicianId());
        if (request.getPatientStatus() != null) {
            patient.setPatientStatus(request.getPatientStatus());
        }
        patient.setPreferredLanguage(request.getPreferredLanguage());
        patient.setInterpreterNeeded(request.getInterpreterNeeded());
        patient.setSpecialNeeds(request.getSpecialNeeds());
        patient.setUpdatedBy(userId);
        
        patient = patientRepository.save(patient);
        final Patient savedPatient = patient;
        
        // Update related entities if provided
        if (request.getEmergencyContacts() != null) {
            emergencyContactRepository.deleteByPatientPatientId(patientId);
            if (!request.getEmergencyContacts().isEmpty()) {
                List<PatientEmergencyContact> contacts = request.getEmergencyContacts().stream()
                    .map(ec -> createEmergencyContact(savedPatient, ec, userId))
                    .collect(Collectors.toList());
                emergencyContactRepository.saveAll(contacts);
            }
        }
        
        if (request.getInsuranceList() != null) {
            insuranceRepository.deleteByPatientPatientId(patientId);
            if (!request.getInsuranceList().isEmpty()) {
                List<PatientInsurance> insuranceList = request.getInsuranceList().stream()
                    .map(ins -> createInsurance(savedPatient, ins, userId))
                    .collect(Collectors.toList());
                insuranceRepository.saveAll(insuranceList);
            }
        }
        
        if (request.getConsents() != null) {
            consentRepository.deleteByPatientPatientId(patientId);
            if (!request.getConsents().isEmpty()) {
                List<PatientConsent> consents = request.getConsents().stream()
                    .map(cons -> createConsent(savedPatient, cons, userId))
                    .collect(Collectors.toList());
                consentRepository.saveAll(consents);
            }
        }

        PatientResponse response = mapToResponse(patient);
        enrichPatientResponseWithIdentityCard(response, userId);

        domainEventPublisher.publish("patient.updated", buildPatientEventPayload(
            response.getPatientId(), response.getMrn(), response.getOrganizationId()));

        return response;
    }

    /**
     * Fills identity card fields from Hospital Card Service so GET/PUT patient match createPatient behaviour.
     */
    private void enrichPatientResponseWithIdentityCard(PatientResponse response, UUID actorUserId) {
        if (response == null || response.getPatientId() == null) {
            return;
        }
        UUID effectiveActor = actorUserId;
        if (effectiveActor == null
                && patientIdentityCardGetPatientEnrichmentUserId != null
                && !patientIdentityCardGetPatientEnrichmentUserId.isBlank()) {
            try {
                effectiveActor = UUID.fromString(patientIdentityCardGetPatientEnrichmentUserId.trim());
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Invalid hospital.patient-identity-card.get-patient-enrichment-user-id: {}",
                        patientIdentityCardGetPatientEnrichmentUserId);
                return;
            }
        }
        if (effectiveActor == null) {
            return;
        }
        PatientIdentityCardIssuanceResult r = patientIdentityCardIssuanceService.resolveExistingForPatient(
                response.getPatientId(), effectiveActor, response.getOrganizationId());
        response.setIdentityCardStatus(r.getStatus());
        response.setIdentityCardId(r.getCardId());
        response.setIdentityCardNumber(r.getCardNumber());
        response.setIdentityCardMessage(r.getMessage());
    }
    
    /**
     * Delete patient (soft delete by setting status to ARCHIVED)
     */
    @Transactional
    public void deletePatient(UUID patientId, UUID userId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: " + patientId));
        patient.setPatientStatus(Patient.PatientStatus.ARCHIVED);
        patient.setUpdatedBy(userId);
        patientRepository.save(patient);

        Map<String, Object> deletePayload = buildPatientEventPayload(
            patient.getPatientId(), patient.getMrn(), patient.getOrganizationId());
        deletePayload.put("status", patient.getPatientStatus().name());
        domainEventPublisher.publish("patient.updated", deletePayload);
    }

    public PatientIdentityCardPrintResponse getIdentityCardPrintPreview(UUID patientId, UUID userId) {
        return buildIdentityCardPrintResponse(patientId, userId, false);
    }

    @Transactional
    public PatientIdentityCardPrintResponse reprintIdentityCard(UUID patientId, UUID userId) {
        return buildIdentityCardPrintResponse(patientId, userId, true);
    }

    @Transactional
    public PatientIdentityCardActionResponse replaceIdentityCard(UUID patientId, UUID userId, String reason) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: " + patientId));
        PatientIdentityCardIssuanceResult result =
                patientIdentityCardIssuanceService.replaceForPatient(patientId, userId, reason, patient.getOrganizationId());
        if (!"REPLACED".equals(result.getStatus())) {
            throw new RuntimeException("Failed to replace patient identity card: " + result.getMessage());
        }

        OffsetDateTime now = OffsetDateTime.now();
        patientIdentityCardAuditLogRepository.save(PatientIdentityCardAuditLog.builder()
                .patientId(patientId)
                .cardId(result.getCardId())
                .cardNumber(result.getCardNumber())
                .action("REPLACE")
                .reason(reason)
                .printedBy(userId)
                .printedAt(now)
                .createdAt(now)
                .build());

        return PatientIdentityCardActionResponse.builder()
                .patientId(patient.getPatientId())
                .cardId(result.getCardId())
                .cardNumber(result.getCardNumber())
                .status(result.getStatus())
                .message(result.getMessage())
                .action("REPLACE")
                .reason(reason)
                .performedBy(userId)
                .performedAt(now)
                .build();
    }

    private PatientIdentityCardPrintResponse buildIdentityCardPrintResponse(UUID patientId, UUID userId, boolean audit) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: " + patientId));
        ensurePatientActiveForCardActions(patient);
        PatientIdentityCardIssuanceResult result =
                patientIdentityCardIssuanceService.resolveExistingForPatient(patientId, userId, patient.getOrganizationId());
        if (!"ISSUED".equals(result.getStatus()) || result.getCardId() == null) {
            throw new RuntimeException("Patient identity card not available: " + result.getMessage());
        }

        String action = isFirstPrint(patientId) ? "PRINT" : "REPRINT";
        OffsetDateTime now = OffsetDateTime.now();
        if (audit) {
            patientIdentityCardAuditLogRepository.save(PatientIdentityCardAuditLog.builder()
                    .patientId(patientId)
                    .cardId(result.getCardId())
                    .cardNumber(result.getCardNumber())
                    .action(action)
                    .printedBy(userId)
                    .printedAt(now)
                    .createdAt(now)
                    .build());
        }

        return PatientIdentityCardPrintResponse.builder()
                .patientId(patient.getPatientId())
                .mrn(patient.getMrn())
                .cardId(result.getCardId())
                .cardNumber(result.getCardNumber())
                .title("Patient identity card")
                .html(buildIdentityCardHtml(patient, result.getCardNumber()))
                .action(action)
                .printedBy(audit ? userId : null)
                .printedAt(audit ? now : null)
                .build();
    }

    private boolean isFirstPrint(UUID patientId) {
        long count = patientIdentityCardAuditLogRepository.countByPatientIdAndActionIn(
                patientId, List.of("PRINT", "REPRINT"));
        return count == 0;
    }

    private String buildIdentityCardHtml(Patient patient, String cardNumber) {
        String name = escapeHtml(patient.getFullName());
        String mrn = escapeHtml(patient.getMrn());
        String phoneRaw = patient.getPrimaryPhone();
        String phoneDisplay = escapeHtml(
                phoneRaw != null && !phoneRaw.isBlank() ? phoneRaw.trim() : "—");

        String rawMrn = patient.getMrn() != null ? patient.getMrn() : "";
        String rawCard = cardNumber != null ? cardNumber : rawMrn;
        String qrPayload = "AURORA|PATIENT|MRN:" + rawMrn + "|CARD:" + rawCard;
        String qrImg = buildPatientIdentityCardQrImgElement(qrPayload);

        String tpl = """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8"/>
                <title>Patient Identity Card — Aurora</title>
                <link rel="preconnect" href="https://fonts.googleapis.com"/>
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@500;600;800&family=Noto+Sans+Bengali:wght@400;600&display=swap" rel="stylesheet"/>
                <style>
                  * { box-sizing: border-box; }
                  body {
                    margin: 0;
                    font-family: Inter, system-ui, -apple-system, sans-serif;
                    background: #e8eef5;
                    padding: 20px;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                  }
                  .id-card {
                    width: 400px;
                    max-width: 100%;
                    margin: 0 auto;
                    background: #fff;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 12px 40px rgba(15, 23, 42, 0.12);
                    border: 1px solid rgba(148, 163, 184, 0.35);
                  }
                  .card-header {
                    position: relative;
                    padding: 18px 20px 16px;
                    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 45%, #f1f5f9 100%);
                    min-height: 112px;
                  }
                  .header-top {
                    display: flex;
                    align-items: flex-start;
                    gap: 14px;
                    position: relative;
                    z-index: 2;
                  }
                  .logo-wrap { flex-shrink: 0; }
                  .title-block h1 {
                    margin: 0;
                    font-size: 1.75rem;
                    font-weight: 800;
                    letter-spacing: 0.06em;
                    color: #1e1b4b;
                    line-height: 1.1;
                  }
                  .title-block h2 {
                    margin: 4px 0 0;
                    font-size: 0.7rem;
                    font-weight: 600;
                    letter-spacing: 0.22em;
                    color: #6d28d9;
                    text-transform: uppercase;
                  }
                  .title-block .tagline-bn {
                    margin: 8px 0 0;
                    font-family: "Noto Sans Bengali", Inter, sans-serif;
                    font-size: 0.95rem;
                    font-weight: 600;
                    color: #475569;
                    line-height: 1.35;
                  }
                  .header-watermark {
                    position: absolute;
                    right: -8px;
                    bottom: -12px;
                    width: 200px;
                    height: 180px;
                    opacity: 0.14;
                    z-index: 1;
                    pointer-events: none;
                  }
                  .card-body {
                    display: flex;
                    gap: 18px;
                    align-items: stretch;
                    padding: 18px 20px 10px;
                    background: #fff;
                  }
                  .info-col {
                    flex: 1;
                    min-width: 0;
                  }
                  .info-row {
                    display: flex;
                    align-items: flex-start;
                    gap: 10px;
                    margin-bottom: 14px;
                  }
                  .info-row:last-child { margin-bottom: 0; }
                  .info-row svg { flex-shrink: 0; margin-top: 2px; }
                  .info-text .lbl {
                    display: block;
                    font-size: 0.65rem;
                    font-weight: 600;
                    letter-spacing: 0.14em;
                    text-transform: uppercase;
                    color: #64748b;
                    margin-bottom: 2px;
                  }
                  .info-text .val {
                    font-size: 0.88rem;
                    font-weight: 600;
                    color: #0f172a;
                    word-break: break-word;
                    line-height: 1.35;
                  }
                  .qr-col {
                    flex-shrink: 0;
                    width: 132px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                  }
                  .qr-col img {
                    width: 132px;
                    height: 132px;
                    display: block;
                    border-radius: 8px;
                    border: 1px solid #e2e8f0;
                  }
                  .qr-col span {
                    margin-top: 6px;
                    font-size: 0.62rem;
                    color: #94a3b8;
                    letter-spacing: 0.06em;
                    text-transform: uppercase;
                  }
                  .wave-wrap { line-height: 0; margin-top: 4px; }
                  .wave-wrap svg { display: block; width: 100%; height: 28px; }
                  .footer-band {
                    background: linear-gradient(90deg, #5b21b6 0%, #5b21b6 48%, #0f766e 52%, #0f766e 100%);
                  }
                  .card-footer {
                    position: relative;
                    padding: 12px 18px 18px;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                  }
                  .footer-icon { flex-shrink: 0; display: flex; align-items: center; }
                  .card-footer .footer-text {
                    font-size: 0.68rem;
                    font-weight: 700;
                    letter-spacing: 0.2em;
                    color: #fff;
                    text-transform: uppercase;
                    line-height: 1.35;
                  }
                  @media print {
                    body { background: #fff; padding: 0; }
                    .id-card { box-shadow: none; border: 1px solid #ccc; }
                  }
                </style>
                </head>
                <body>
                <div class="id-card">
                  <header class="card-header">
                    <div class="header-watermark" aria-hidden="true">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 180" width="200" height="180">
                        <g fill="#475569">
                          <rect x="20" y="80" width="160" height="90" rx="4"/>
                          <rect x="60" y="40" width="80" height="50" rx="4"/>
                          <rect x="85" y="20" width="30" height="30" rx="2"/>
                          <rect x="35" y="100" width="25" height="40"/>
                          <rect x="90" y="100" width="25" height="40"/>
                          <rect x="140" y="100" width="25" height="40"/>
                        </g>
                      </svg>
                    </div>
                    <div class="header-top">
                      <div class="logo-wrap" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
                          <defs>
                            <linearGradient id="alogo" x1="0" y1="0" x2="1" y2="1">
                              <stop offset="0%" stop-color="#6d28d9"/>
                              <stop offset="100%" stop-color="#0d9488"/>
                            </linearGradient>
                          </defs>
                          <circle cx="24" cy="24" r="22" fill="url(#alogo)"/>
                          <path fill="#fff" d="M22 12h4v24h-4zM12 22h24v4H12z"/>
                        </svg>
                      </div>
                      <div class="title-block">
                        <h1>AURORA</h1>
                        <h2>Specialized Hospital</h2>
                        <p class="tagline-bn">আপনার স্বাস্থ্য, আমাদের প্রতিশ্রুতি</p>
                      </div>
                    </div>
                  </header>
                  <div class="card-body">
                    <div class="info-col">
                      <div class="info-row">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6d28d9" stroke-width="2" aria-hidden="true">
                          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                        </svg>
                        <div class="info-text">
                          <span class="lbl">Patient Name</span>
                          <span class="val">__PATIENT_NAME__</span>
                        </div>
                      </div>
                      <div class="info-row">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6d28d9" stroke-width="2" aria-hidden="true">
                          <rect x="4" y="5" width="16" height="14" rx="2"/><path d="M8 9h8M8 13h5"/>
                        </svg>
                        <div class="info-text">
                          <span class="lbl">Patient ID</span>
                          <span class="val">__MRN__</span>
                        </div>
                      </div>
                      <div class="info-row">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6d28d9" stroke-width="2" aria-hidden="true">
                          <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                        </svg>
                        <div class="info-text">
                          <span class="lbl">Mobile</span>
                          <span class="val">__PHONE__</span>
                        </div>
                      </div>
                    </div>
                    <div class="qr-col">
                      __QR_IMG__
                      <span>Scan to verify</span>
                    </div>
                  </div>
                  <div class="footer-band">
                    <div class="wave-wrap" aria-hidden="true">
                      <svg viewBox="0 0 400 28" preserveAspectRatio="none">
                        <path d="M0,28 L0,12 C80,2 160,22 200,12 C240,2 320,24 400,8 L400,28 Z" fill="#5b21b6"/>
                        <path d="M0,28 L0,18 C100,8 200,22 300,14 C330,11 370,20 400,16 L400,28 Z" fill="#0f766e"/>
                      </svg>
                    </div>
                    <footer class="card-footer">
                      <div class="footer-icon" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none">
                          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="#fff" stroke-width="2" fill="rgba(255,255,255,0.15)"/>
                          <path d="M12 9v6M9 12h6" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                      </div>
                      <div class="footer-text">Your health, our commitment</div>
                    </footer>
                  </div>
                </div>
                </body>
                </html>
                """;

        return tpl.replace("__QR_IMG__", qrImg)
                .replace("__PATIENT_NAME__", name)
                .replace("__MRN__", mrn)
                .replace("__PHONE__", phoneDisplay);
    }

    /**
     * Inline QR for print HTML — avoids third-party image hosts (works offline once HTML is generated).
     */
    private String buildPatientIdentityCardQrImgElement(String payload) {
        String dataUri = encodeQrPayloadAsPngDataUri(payload);
        if (dataUri == null || dataUri.isEmpty()) {
            return "<div class=\"qr-fallback\" style=\"width:132px;height:132px;border:1px dashed #cbd5e1;border-radius:8px;"
                    + "display:flex;align-items:center;justify-content:center;text-align:center;font-size:10px;color:#64748b;padding:6px;\">"
                    + "QR unavailable</div>";
        }
        return "<img src=\""
                + dataUri
                + "\" width=\"132\" height=\"132\" alt=\"Patient verification QR\"/>";
    }

    private String encodeQrPayloadAsPngDataUri(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 0);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, 132, 132, hints);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException | IOException e) {
            log.warn("QR generation failed for patient identity card: {}", e.getMessage());
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void ensurePatientActiveForCardActions(Patient patient) {
        if (patient.getPatientStatus() != null && patient.getPatientStatus() != Patient.PatientStatus.ACTIVE) {
            throw new IllegalArgumentException("Identity card actions are allowed only for ACTIVE patients");
        }
    }
    
    /**
     * Map Patient entity to PatientResponse
     */
    private PatientResponse mapToResponse(Patient patient) {
        PatientResponse response = PatientResponse.builder()
            .patientId(patient.getPatientId())
            .mrn(patient.getMrn())
            .organizationId(patient.getOrganizationId())
            .fullName(patient.getFullName())
            .preferredName(patient.getPreferredName())
            .dateOfBirth(patient.getDateOfBirth())
            .gender(patient.getGender())
            .sexAtBirth(patient.getSexAtBirth())
            .idNo(patient.getIdNo())
            .idType(patient.getIdType())
            .race(patient.getRace())
            .ethnicity(patient.getEthnicity())
            .maritalStatus(patient.getMaritalStatus())
            .patientType(patient.getPatientType())
            .fatherName(patient.getFatherName())
            .motherName(patient.getMotherName())
            .spouseName(patient.getSpouseName())
            .bloodGroup(patient.getBloodGroup())
            .religion(patient.getReligion())
            .occupation(patient.getOccupation())
            .introducedBy(patient.getIntroducedBy())
            .primaryAddressLine1(patient.getPrimaryAddressLine1())
            .primaryAddressLine2(patient.getPrimaryAddressLine2())
            .primaryCity(patient.getPrimaryCity())
            .primaryState(patient.getPrimaryState())
            .primaryZip(patient.getPrimaryZip())
            .primaryCountry(patient.getPrimaryCountry())
            .mailingAddressLine1(patient.getMailingAddressLine1())
            .mailingAddressLine2(patient.getMailingAddressLine2())
            .mailingCity(patient.getMailingCity())
            .mailingState(patient.getMailingState())
            .mailingZip(patient.getMailingZip())
            .mailingCountry(patient.getMailingCountry())
            .primaryPhone(patient.getPrimaryPhone())
            .primaryPhoneType(patient.getPrimaryPhoneType())
            .secondaryPhone(patient.getSecondaryPhone())
            .secondaryPhoneType(patient.getSecondaryPhoneType())
            .primaryEmail(patient.getPrimaryEmail())
            .secondaryEmail(patient.getSecondaryEmail())
            .preferredContactMethod(patient.getPreferredContactMethod())
            .consentTextMessaging(patient.getConsentTextMessaging())
            .consentEmailCommunication(patient.getConsentEmailCommunication())
            .primaryCareProviderId(patient.getPrimaryCareProviderId())
            .primaryCareLocationId(patient.getPrimaryCareLocationId())
            .referringPhysicianId(patient.getReferringPhysicianId())
            .patientStatus(patient.getPatientStatus())
            .preferredLanguage(patient.getPreferredLanguage())
            .interpreterNeeded(patient.getInterpreterNeeded())
            .specialNeeds(patient.getSpecialNeeds())
            .registrationDate(patient.getRegistrationDate())
            .registeredBy(patient.getRegisteredBy())
            .registrationLocationId(patient.getRegistrationLocationId())
            .createdAt(patient.getCreatedAt())
            .updatedAt(patient.getUpdatedAt())
            .createdBy(patient.getCreatedBy())
            .updatedBy(patient.getUpdatedBy())
            .build();
        
        // Load and map related entities
        if (patient.getEmergencyContacts() != null) {
            response.setEmergencyContacts(patient.getEmergencyContacts().stream()
                .map(this::mapEmergencyContactToResponse)
                .collect(Collectors.toList()));
        } else {
            List<PatientEmergencyContact> contacts = emergencyContactRepository.findByPatientPatientId(patient.getPatientId());
            response.setEmergencyContacts(contacts.stream()
                .map(this::mapEmergencyContactToResponse)
                .collect(Collectors.toList()));
        }
        
        if (patient.getInsuranceList() != null) {
            response.setInsuranceList(patient.getInsuranceList().stream()
                .map(this::mapInsuranceToResponse)
                .collect(Collectors.toList()));
        } else {
            List<PatientInsurance> insuranceList = insuranceRepository.findByPatientPatientId(patient.getPatientId());
            response.setInsuranceList(insuranceList.stream()
                .map(this::mapInsuranceToResponse)
                .collect(Collectors.toList()));
        }
        
        if (patient.getConsents() != null) {
            response.setConsents(patient.getConsents().stream()
                .map(this::mapConsentToResponse)
                .collect(Collectors.toList()));
        } else {
            List<PatientConsent> consents = consentRepository.findByPatientPatientId(patient.getPatientId());
            response.setConsents(consents.stream()
                .map(this::mapConsentToResponse)
                .collect(Collectors.toList()));
        }
        
        return response;
    }
    
    private EmergencyContactResponse mapEmergencyContactToResponse(PatientEmergencyContact contact) {
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
    
    private InsuranceResponse mapInsuranceToResponse(PatientInsurance insurance) {
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
    
    private ConsentResponse mapConsentToResponse(PatientConsent consent) {
        return ConsentResponse.builder()
            .consentId(consent.getConsentId())
            .patientId(consent.getPatient().getPatientId())
            .consentType(consent.getConsentType())
            .consentStatus(consent.getConsentStatus())
            .consentDate(consent.getConsentDate())
            .signature(consent.getSignature())
            .expiresDate(consent.getExpiresDate())
            .notes(consent.getNotes())
            .createdAt(consent.getCreatedAt())
            .updatedAt(consent.getUpdatedAt())
            .build();
    }
}
