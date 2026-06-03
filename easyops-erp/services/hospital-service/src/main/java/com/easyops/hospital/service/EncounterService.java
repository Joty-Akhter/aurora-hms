package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.EncounterRequest;
import com.easyops.hospital.dto.response.EncounterResponse;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.Encounter;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.repository.DoctorRepository;
import com.easyops.hospital.repository.EncounterRepository;
import com.easyops.hospital.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.easyops.hospital.entity.Encounter.EncounterStatus;
import static com.easyops.hospital.entity.Encounter.EncounterType;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EncounterService {
    
    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * Generate unique encounter number
     */
    private String generateEncounterNumber(UUID organizationId) {
        String prefix = "ENC";
        String orgPrefix = organizationId.toString().substring(0, 8).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return String.format("%s-%s-%s", prefix, orgPrefix, timestamp);
    }
    
    /**
     * Calculate length of stay in days
     */
    private Integer calculateLengthOfStay(LocalDate admissionDate, LocalDate dischargeDate) {
        if (admissionDate == null || dischargeDate == null) {
            return null;
        }
        if (dischargeDate.isBefore(admissionDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(admissionDate, dischargeDate);
    }
    
    /**
     * Create a new encounter
     */
    @Transactional
    public EncounterResponse createEncounter(UUID organizationId, EncounterRequest request, UUID userId) {
        log.info("Creating encounter for patient: {}", request.getPatientId());
        
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        assertPatientBelongsToOrganization(organizationId, patient);

        // Generate encounter number if not provided
        String encounterNumber = request.getEncounterNumber();
        if (encounterNumber == null || encounterNumber.isEmpty()) {
            encounterNumber = generateEncounterNumber(organizationId);
            // Ensure uniqueness
            while (encounterRepository.existsByEncounterNumber(encounterNumber)) {
                encounterNumber = generateEncounterNumber(organizationId);
            }
        } else {
            if (encounterRepository.existsByEncounterNumber(encounterNumber)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Encounter number already exists: " + encounterNumber);
            }
        }
        
        Encounter encounter = Encounter.builder()
            .patient(patient)
            .organizationId(organizationId)
            .encounterNumber(encounterNumber)
            .encounterType(request.getEncounterType())
            .status(request.getStatus() != null ? request.getStatus() : Encounter.EncounterStatus.PLANNED)
            .startDate(request.getStartDate())
            .startTime(request.getStartTime())
            .endDate(request.getEndDate())
            .endTime(request.getEndTime())
            .admissionDate(request.getAdmissionDate())
            .admissionTime(request.getAdmissionTime())
            .dischargeDate(request.getDischargeDate())
            .dischargeTime(request.getDischargeTime())
            .locationId(request.getLocationId())
            .departmentId(request.getDepartmentId())
            .roomNumber(request.getRoomNumber())
            .bedNumber(request.getBedNumber())
            .attendingPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getAttendingPhysicianId()))
            .admittingPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getAdmittingPhysicianId()))
            .primaryCareProviderId(resolvePortalUserOrDoctorIdToDoctorId(request.getPrimaryCareProviderId()))
            .referringPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getReferringPhysicianId()))
            .chiefComplaint(request.getChiefComplaint())
            .admissionDiagnosis(request.getAdmissionDiagnosis())
            .primaryDiagnosis(request.getPrimaryDiagnosis())
            .secondaryDiagnoses(request.getSecondaryDiagnoses())
            .dischargeDiagnosis(request.getDischargeDiagnosis())
            .dischargeDisposition(request.getDischargeDisposition())
            .dischargeInstructions(request.getDischargeInstructions())
            .visitReason(request.getVisitReason())
            .visitType(request.getVisitType())
            .serviceType(request.getServiceType())
            .insuranceProviderId(request.getInsuranceProviderId())
            .insurancePolicyNumber(request.getInsurancePolicyNumber())
            .authorizationNumber(request.getAuthorizationNumber())
            .billingStatus(request.getBillingStatus())
            .notes(request.getNotes())
            .specialInstructions(request.getSpecialInstructions())
            .isEmergency(request.getIsEmergency() != null ? request.getIsEmergency() : false)
            .isReadmission(request.getIsReadmission() != null ? request.getIsReadmission() : false)
            .readmissionReason(request.getReadmissionReason())
            .createdBy(userId)
            .build();
        
        // Calculate length of stay if admission and discharge dates are present
        if (encounter.getAdmissionDate() != null && encounter.getDischargeDate() != null) {
            encounter.setLengthOfStayDays(calculateLengthOfStay(
                encounter.getAdmissionDate(), encounter.getDischargeDate()));
        }
        
        encounter = encounterRepository.save(encounter);
        EncounterResponse response = mapEncounterToResponse(encounter);

        // Emit domain event
        domainEventPublisher.publish("encounter.created", Map.of(
            "encounterId", response.getEncounterId(),
            "encounterNumber", response.getEncounterNumber(),
            "patientId", response.getPatientId(),
            "organizationId", response.getOrganizationId(),
            "status", response.getStatus()
        ));

        return response;
    }
    
    /**
     * Get encounter by ID
     */
    @Transactional(readOnly = true)
    public EncounterResponse getEncounterById(UUID encounterId) {
        Encounter encounter = encounterRepository.findWithPatientById(encounterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        return mapEncounterToResponse(encounter);
    }
    
    /**
     * Get encounter by encounter number
     */
    @Transactional(readOnly = true)
    public EncounterResponse getEncounterByNumber(String encounterNumber) {
        Encounter encounter = encounterRepository.findByEncounterNumber(encounterNumber)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Encounter not found with number: " + encounterNumber));
        return mapEncounterToResponse(encounter);
    }
    
    /**
     * Get all encounters for a patient
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByPatient(UUID patientId) {
        List<Encounter> encounters = encounterRepository
            .findByPatientPatientIdOrderByStartDateDescStartTimeDesc(patientId);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get active encounters for a patient
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getActiveEncountersByPatient(UUID patientId) {
        List<Encounter> encounters = encounterRepository.findActiveEncountersByPatient(patientId);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get encounters by organization
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByOrganization(UUID organizationId) {
        List<Encounter> encounters = encounterRepository
            .findByOrganizationIdOrderByStartDateDescStartTimeDesc(organizationId);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get encounters by status
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByStatus(UUID organizationId, Encounter.EncounterStatus status) {
        List<Encounter> encounters = encounterRepository.findByOrganizationIdAndStatus(organizationId, status);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get encounters by type
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByType(UUID organizationId, Encounter.EncounterType encounterType) {
        List<Encounter> encounters = encounterRepository.findByOrganizationIdAndEncounterType(organizationId, encounterType);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get encounters by date range
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByDateRange(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        List<Encounter> encounters = encounterRepository
            .findByOrganizationIdAndStartDateBetween(organizationId, startDate, endDate);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Get active encounters for organization
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getActiveEncountersByOrganization(UUID organizationId) {
        List<Encounter> encounters = encounterRepository.findActiveEncountersByOrganization(organizationId);
        return mapEncountersToResponses(encounters);
    }

    /**
     * Active inpatient / hospital-admission encounters for IPD prescribing dashboard (EP-1 / EP-11).
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getActiveInpatientEncountersByOrganization(UUID organizationId, UUID attendingPhysicianId) {
        List<EncounterType> types = List.of(EncounterType.INPATIENT, EncounterType.HOSPITAL_ADMISSION);
        List<EncounterStatus> statuses = List.of(
                EncounterStatus.IN_PROGRESS,
                EncounterStatus.ARRIVED,
                EncounterStatus.ADMITTED
        );
        UUID attendingDoctorId = resolvePortalUserOrDoctorIdToDoctorId(attendingPhysicianId);
        List<Encounter> encounters;
        if (attendingDoctorId != null) {
            encounters = encounterRepository
                    .findByOrganizationIdAndEncounterTypeInAndStatusInAndAttendingPhysicianIdOrderByStartDateDescStartTimeDesc(
                            organizationId, types, statuses, attendingDoctorId);
        } else {
            encounters = encounterRepository
                    .findByOrganizationIdAndEncounterTypeInAndStatusInOrderByStartDateDescStartTimeDesc(
                            organizationId, types, statuses);
        }
        return mapEncountersToResponses(encounters);
    }

    /** When the patient row is org-scoped, the encounter must be created in that same tenant. */
    private void assertPatientBelongsToOrganization(UUID organizationId, Patient patient) {
        UUID patientOrg = patient.getOrganizationId();
        if (patientOrg != null && !patientOrg.equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Patient belongs to a different organization than the encounter being created");
        }
    }

    /**
     * Portal {@code users.id} and {@code hospital.doctors.doctor_id} are both UUIDs. When the value matches
     * {@code hospital.doctors.linked_user_id}, return that row's {@code doctor_id}; otherwise return the input
     * (doctor id or any other UUID, e.g. external referring provider with no linked row).
     */
    private UUID resolvePortalUserOrDoctorIdToDoctorId(UUID userOrDoctorId) {
        if (userOrDoctorId == null) {
            return null;
        }
        return doctorRepository.findByLinkedUserId(userOrDoctorId)
                .map(Doctor::getDoctorId)
                .orElse(userOrDoctorId);
    }

    /**
     * Get active admissions for a patient
     */
    @Transactional(readOnly = true)
    public List<EncounterResponse> getActiveAdmissionsByPatient(UUID patientId) {
        List<Encounter> encounters = encounterRepository.findActiveAdmissionsByPatient(patientId);
        return mapEncountersToResponses(encounters);
    }
    
    /**
     * Update encounter
     */
    @Transactional
    public EncounterResponse updateEncounter(UUID encounterId, EncounterRequest request, UUID userId) {
        log.info("Updating encounter: {}", encounterId);
        
        Encounter encounter = encounterRepository.findWithPatientById(encounterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        
        // Update fields
        if (request.getEncounterType() != null) {
            encounter.setEncounterType(request.getEncounterType());
        }
        if (request.getStatus() != null) {
            encounter.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            encounter.setStartDate(request.getStartDate());
        }
        if (request.getStartTime() != null) {
            encounter.setStartTime(request.getStartTime());
        }
        if (request.getEndDate() != null) {
            encounter.setEndDate(request.getEndDate());
        }
        if (request.getEndTime() != null) {
            encounter.setEndTime(request.getEndTime());
        }
        if (request.getAdmissionDate() != null) {
            encounter.setAdmissionDate(request.getAdmissionDate());
        }
        if (request.getAdmissionTime() != null) {
            encounter.setAdmissionTime(request.getAdmissionTime());
        }
        if (request.getDischargeDate() != null) {
            encounter.setDischargeDate(request.getDischargeDate());
        }
        if (request.getDischargeTime() != null) {
            encounter.setDischargeTime(request.getDischargeTime());
        }
        
        encounter.setLocationId(request.getLocationId());
        encounter.setDepartmentId(request.getDepartmentId());
        encounter.setRoomNumber(request.getRoomNumber());
        encounter.setBedNumber(request.getBedNumber());
        encounter.setAttendingPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getAttendingPhysicianId()));
        encounter.setAdmittingPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getAdmittingPhysicianId()));
        encounter.setPrimaryCareProviderId(resolvePortalUserOrDoctorIdToDoctorId(request.getPrimaryCareProviderId()));
        encounter.setReferringPhysicianId(resolvePortalUserOrDoctorIdToDoctorId(request.getReferringPhysicianId()));
        encounter.setChiefComplaint(request.getChiefComplaint());
        encounter.setAdmissionDiagnosis(request.getAdmissionDiagnosis());
        encounter.setPrimaryDiagnosis(request.getPrimaryDiagnosis());
        encounter.setSecondaryDiagnoses(request.getSecondaryDiagnoses());
        encounter.setDischargeDiagnosis(request.getDischargeDiagnosis());
        encounter.setDischargeDisposition(request.getDischargeDisposition());
        encounter.setDischargeInstructions(request.getDischargeInstructions());
        encounter.setVisitReason(request.getVisitReason());
        encounter.setVisitType(request.getVisitType());
        encounter.setServiceType(request.getServiceType());
        encounter.setInsuranceProviderId(request.getInsuranceProviderId());
        encounter.setInsurancePolicyNumber(request.getInsurancePolicyNumber());
        encounter.setAuthorizationNumber(request.getAuthorizationNumber());
        encounter.setBillingStatus(request.getBillingStatus());
        encounter.setNotes(request.getNotes());
        encounter.setSpecialInstructions(request.getSpecialInstructions());
        if (request.getIsEmergency() != null) {
            encounter.setIsEmergency(request.getIsEmergency());
        }
        if (request.getIsReadmission() != null) {
            encounter.setIsReadmission(request.getIsReadmission());
        }
        encounter.setReadmissionReason(request.getReadmissionReason());
        encounter.setUpdatedBy(userId);
        
        // Recalculate length of stay if dates changed
        if (encounter.getAdmissionDate() != null && encounter.getDischargeDate() != null) {
            encounter.setLengthOfStayDays(calculateLengthOfStay(
                encounter.getAdmissionDate(), encounter.getDischargeDate()));
        } else {
            encounter.setLengthOfStayDays(null);
        }
        
        encounter = encounterRepository.save(encounter);
        EncounterResponse response = mapEncounterToResponse(encounter);

        domainEventPublisher.publish("encounter.updated", Map.of(
            "encounterId", response.getEncounterId(),
            "encounterNumber", response.getEncounterNumber(),
            "patientId", response.getPatientId(),
            "organizationId", response.getOrganizationId(),
            "status", response.getStatus()
        ));

        return response;
    }
    
    /**
     * Update encounter status
     */
    @Transactional
    public EncounterResponse updateEncounterStatus(UUID encounterId, Encounter.EncounterStatus status, UUID userId) {
        Encounter encounter = encounterRepository.findWithPatientById(encounterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));

        encounter.setStatus(status);
        encounter.setUpdatedBy(userId);
        
        // Auto-set dates based on status
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        if (status == Encounter.EncounterStatus.ARRIVED || status == Encounter.EncounterStatus.IN_PROGRESS) {
            if (encounter.getStartDate() == null) {
                encounter.setStartDate(today);
            }
            if (encounter.getStartTime() == null) {
                encounter.setStartTime(now);
            }
        } else if (status == Encounter.EncounterStatus.ADMITTED) {
            if (encounter.getAdmissionDate() == null) {
                encounter.setAdmissionDate(today);
            }
            if (encounter.getAdmissionTime() == null) {
                encounter.setAdmissionTime(now);
            }
        } else if (status == Encounter.EncounterStatus.DISCHARGED || status == Encounter.EncounterStatus.COMPLETED) {
            if (encounter.getDischargeDate() == null) {
                encounter.setDischargeDate(today);
            }
            if (encounter.getDischargeTime() == null) {
                encounter.setDischargeTime(now);
            }
            if (encounter.getEndDate() == null) {
                encounter.setEndDate(today);
            }
            if (encounter.getEndTime() == null) {
                encounter.setEndTime(now);
            }
        }
        
        encounter = encounterRepository.save(encounter);
        EncounterResponse response = mapEncounterToResponse(encounter);

        String eventType = (status == Encounter.EncounterStatus.DISCHARGED ||
                            status == Encounter.EncounterStatus.COMPLETED)
            ? "encounter.discharged"
            : "encounter.updated";

        domainEventPublisher.publish(eventType, Map.of(
            "encounterId", response.getEncounterId(),
            "encounterNumber", response.getEncounterNumber(),
            "patientId", response.getPatientId(),
            "organizationId", response.getOrganizationId(),
            "status", response.getStatus()
        ));

        return response;
    }
    
    /**
     * Delete encounter
     */
    @Transactional
    public void deleteEncounter(UUID encounterId) {
        if (!encounterRepository.existsById(encounterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found");
        }
        encounterRepository.deleteById(encounterId);
    }
    
    private List<EncounterResponse> mapEncountersToResponses(List<Encounter> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return List.of();
        }
        List<EncounterResponse> responses = encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        attachPhysicianNames(responses);
        return responses;
    }

    private EncounterResponse mapEncounterToResponse(Encounter encounter) {
        EncounterResponse response = mapToResponse(encounter);
        attachPhysicianNames(List.of(response));
        return response;
    }

    /** Batch-load doctor display names for attending / admitting UUIDs on encounter rows. */
    private void attachPhysicianNames(List<EncounterResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Set<UUID> ids = new HashSet<>();
        for (EncounterResponse r : responses) {
            if (r.getAttendingPhysicianId() != null) {
                ids.add(r.getAttendingPhysicianId());
            }
            if (r.getAdmittingPhysicianId() != null) {
                ids.add(r.getAdmittingPhysicianId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<UUID, String> namesById = doctorRepository.findAllByDoctorIdIn(ids).stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, Doctor::getDoctorName, (a, b) -> a));
        for (EncounterResponse r : responses) {
            if (r.getAttendingPhysicianId() != null) {
                r.setAttendingPhysicianName(namesById.get(r.getAttendingPhysicianId()));
            }
            if (r.getAdmittingPhysicianId() != null) {
                r.setAdmittingPhysicianName(namesById.get(r.getAdmittingPhysicianId()));
            }
        }
    }

    /**
     * Map entity to response DTO
     */
    private EncounterResponse mapToResponse(Encounter encounter) {
        Patient patient = encounter.getPatient();
        String patientName = patient != null && patient.getFullName() != null
            ? patient.getFullName().trim() : null;
        String mrn = patient != null ? patient.getMrn() : null;
        UUID patientId = patient != null ? patient.getPatientId() : null;

        return EncounterResponse.builder()
            .encounterId(encounter.getEncounterId())
            .patientId(patientId)
            .patientName(patientName)
            .mrn(mrn)
            .organizationId(encounter.getOrganizationId())
            .encounterNumber(encounter.getEncounterNumber())
            .encounterType(encounter.getEncounterType())
            .status(encounter.getStatus())
            .startDate(encounter.getStartDate())
            .startTime(encounter.getStartTime())
            .endDate(encounter.getEndDate())
            .endTime(encounter.getEndTime())
            .admissionDate(encounter.getAdmissionDate())
            .admissionTime(encounter.getAdmissionTime())
            .dischargeDate(encounter.getDischargeDate())
            .dischargeTime(encounter.getDischargeTime())
            .locationId(encounter.getLocationId())
            .departmentId(encounter.getDepartmentId())
            .roomNumber(encounter.getRoomNumber())
            .bedNumber(encounter.getBedNumber())
            .attendingPhysicianId(encounter.getAttendingPhysicianId())
            .admittingPhysicianId(encounter.getAdmittingPhysicianId())
            .primaryCareProviderId(encounter.getPrimaryCareProviderId())
            .referringPhysicianId(encounter.getReferringPhysicianId())
            .chiefComplaint(encounter.getChiefComplaint())
            .admissionDiagnosis(encounter.getAdmissionDiagnosis())
            .primaryDiagnosis(encounter.getPrimaryDiagnosis())
            .secondaryDiagnoses(encounter.getSecondaryDiagnoses())
            .dischargeDiagnosis(encounter.getDischargeDiagnosis())
            .dischargeDisposition(encounter.getDischargeDisposition())
            .dischargeInstructions(encounter.getDischargeInstructions())
            .visitReason(encounter.getVisitReason())
            .visitType(encounter.getVisitType())
            .serviceType(encounter.getServiceType())
            .insuranceProviderId(encounter.getInsuranceProviderId())
            .insurancePolicyNumber(encounter.getInsurancePolicyNumber())
            .authorizationNumber(encounter.getAuthorizationNumber())
            .billingStatus(encounter.getBillingStatus())
            .notes(encounter.getNotes())
            .specialInstructions(encounter.getSpecialInstructions())
            .isEmergency(encounter.getIsEmergency())
            .isReadmission(encounter.getIsReadmission())
            .readmissionReason(encounter.getReadmissionReason())
            .lengthOfStayDays(encounter.getLengthOfStayDays())
            .createdAt(encounter.getCreatedAt())
            .updatedAt(encounter.getUpdatedAt())
            .createdBy(encounter.getCreatedBy())
            .updatedBy(encounter.getUpdatedBy())
            .build();
    }
}
