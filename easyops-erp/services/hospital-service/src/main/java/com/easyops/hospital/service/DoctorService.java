package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DoctorRequest;
import com.easyops.hospital.dto.response.DoctorResponse;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.DoctorDepartment;
import com.easyops.hospital.integration.rbac.RbacAuthorizationClient;
import com.easyops.hospital.integration.rbac.RbacRoleResponse;
import com.easyops.hospital.integration.scheduling.SchedulingServiceClient;
import com.easyops.hospital.integration.usermanagement.UserManagementClient;
import com.easyops.hospital.integration.usermanagement.UserManagementUserResponse;
import com.easyops.hospital.repository.DoctorDepartmentRepository;
import com.easyops.hospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorDepartmentRepository doctorDepartmentRepository;
    private final SchedulingServiceClient schedulingServiceClient;
    private final UserManagementClient userManagementClient;
    private final RbacAuthorizationClient rbacAuthorizationClient;
    private final RbacPermissionService rbacPermissionService;

    @Value("${hospital.doctor.default-portal-user-password}")
    private String defaultPortalUserPassword;

    @Value("${hospital.doctor.prescribing-authority-role-code:PRESCRIBING_AUTHORITY}")
    private String prescribingAuthorityRoleCode;

    @Value("${hospital.doctor.base-user-role-code:USER}")
    private String baseUserRoleCode;

    private String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequiredText(String value, String fieldLabel) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " is required");
        }
        return trimmed;
    }

    /** Prefer trimmed {@code preferred}; when blank, keep existing {@code fallback} (may be null). */
    private static String coalesceTrimmedOrFallback(String preferred, String fallback) {
        if (preferred != null) {
            String trimmed = preferred.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return fallback;
    }
    
    /**
     * Generate doctor code: [Dept Initial][Name Initial][Serial]
     * Example: CARDR01 (Cardiology + Dr. Rahman + 01)
     */
    private String generateDoctorCode(String departmentName, String doctorName) {
        // Get department initials (first 3-4 letters, uppercase)
        String deptInitial = departmentName.length() >= 4 
            ? departmentName.substring(0, 4).toUpperCase().replaceAll("[^A-Z]", "")
            : departmentName.toUpperCase().replaceAll("[^A-Z]", "");
        
        // Get doctor name initials (first letter of each word, uppercase)
        String[] nameParts = doctorName.trim().split("\\s+");
        StringBuilder nameInitial = new StringBuilder();
        for (String part : nameParts) {
            if (!part.isEmpty()) {
                nameInitial.append(part.substring(0, 1).toUpperCase());
            }
        }
        // Take first 3-4 letters of name initial
        String nameInitialStr = nameInitial.length() >= 4 
            ? nameInitial.substring(0, 4)
            : nameInitial.toString();
        
        // Generate prefix
        String prefix = deptInitial + nameInitialStr;
        
        // Find max sequence for this prefix
        Long maxSequence = doctorRepository.findMaxDoctorCodeSequence(prefix, prefix.length());
        if (maxSequence == null) {
            maxSequence = 0L;
        }
        
        // Generate next sequence
        long nextSequence = maxSequence + 1;
        String sequence = String.format("%02d", nextSequence);
        
        String doctorCode = prefix + sequence;
        
        // Ensure uniqueness
        int attempts = 0;
        while (doctorRepository.existsByDoctorCode(doctorCode) && attempts < 10) {
            nextSequence++;
            sequence = String.format("%02d", nextSequence);
            doctorCode = prefix + sequence;
            attempts++;
        }
        
        if (attempts >= 10) {
            throw new RuntimeException("Unable to generate unique doctor code after multiple attempts");
        }
        
        return doctorCode;
    }
    
    /**
     * Get all doctors. When {@code includeInactive} is false, returns only active (non–soft-deleted) doctors.
     */
    public List<DoctorResponse> getAllDoctors(boolean includeInactive) {
        List<Doctor> doctors = includeInactive
                ? doctorRepository.findAll()
                : doctorRepository.findAllActiveDoctors();
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active doctors for prescription dropdown
     */
    public List<DoctorResponse> getActiveDoctorsForPrescription() {
        List<Doctor> doctors = doctorRepository.findActiveDoctorsForPrescription();
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get doctor by ID
     */
    public DoctorResponse getDoctorById(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        return mapToResponse(doctor);
    }

    /**
     * Find (or create) the scheduling-service resource for a doctor and return its ID.
     * This proxies the findOrCreate call through the backend so the frontend never has
     * to call the scheduling service directly (avoiding RBAC/org-context issues).
     *
     * @return scheduling resourceId, or null if the scheduling service is unavailable
     */
    public String findOrCreateSchedulingResource(UUID doctorId, String userId, UUID organizationId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        UUID departmentId = doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentId() : null;
        String resourceId = schedulingServiceClient.findOrCreateResource(
                doctorId.toString(),
                doctor.getDoctorName(),
                departmentId,
                userId,
                organizationId);
        // Also sync working hours and blackouts so the booking calendar shows up-to-date
        // availability even when the doctor was saved before the scheduling service was reachable.
        // Best-effort: SchedulingServiceClient logs warnings internally and never throws.
        if (resourceId != null) {
            if (doctor.getAppointmentSlots() != null && !doctor.getAppointmentSlots().isEmpty()) {
                schedulingServiceClient.syncWorkingHours(resourceId, doctor.getAppointmentSlots(), userId, organizationId);
            }
            schedulingServiceClient.syncBlackouts(resourceId, doctor.getOffDays(), userId, organizationId);
        }
        return resourceId;
    }
    
    /**
     * Get doctor by code
     */
    public DoctorResponse getDoctorByCode(String doctorCode) {
        Doctor doctor = doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new RuntimeException("Doctor not found with code: " + doctorCode));
        return mapToResponse(doctor);
    }
    
    /**
     * Search doctors by name, code, department, or speciality.
     * When {@code includeInactive} is false, inactive (soft-deleted) doctors are omitted.
     */
    public List<DoctorResponse> searchDoctors(String searchTerm, boolean includeInactive) {
        List<Doctor> doctors = doctorRepository.searchDoctors(searchTerm);
        if (!includeInactive) {
            doctors = doctors.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                    .collect(Collectors.toList());
        }
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get doctors by department
     */
    public List<DoctorResponse> getDoctorsByDepartment(UUID departmentId) {
        List<Doctor> doctors = doctorRepository.findByDepartment_DepartmentId(departmentId);
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active doctors by department
     */
    public List<DoctorResponse> getActiveDoctorsByDepartment(UUID departmentId) {
        List<Doctor> doctors = doctorRepository.findActiveDoctorsByDepartment(departmentId);
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get doctors by speciality
     */
    public List<DoctorResponse> getDoctorsBySpeciality(String speciality) {
        List<Doctor> doctors = doctorRepository.findBySpeciality(speciality);
        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new doctor
     */
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request, String userId, UUID organizationId) {
        String normalizedDegree = normalizeRequiredText(request.getDegree(), "Degree");
        String normalizedEmail = normalizeToNull(request.getEmail());
        String normalizedBmdcRegistrationNumber = normalizeRequiredText(
                request.getBmdcRegistrationNumber(), "BMDC registration number");

        // Validate department exists
        DoctorDepartment department = doctorDepartmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + request.getDepartmentId()));
        
        // Check if department is active
        if (department.getStatus() != DoctorDepartment.DepartmentStatus.ACTIVE) {
            throw new RuntimeException("Cannot create doctor for inactive department");
        }
        
        // Check email uniqueness if provided
        if (normalizedEmail != null) {
            if (doctorRepository.existsByEmail(normalizedEmail)) {
                throw new RuntimeException("Doctor with email '" + normalizedEmail + "' already exists");
            }
        }
        
        // Check BMDC registration number uniqueness if provided
        if (normalizedBmdcRegistrationNumber != null) {
            if (doctorRepository.existsByBmdcRegistrationNumber(normalizedBmdcRegistrationNumber)) {
                throw new RuntimeException("Doctor with BMDC registration number '" + normalizedBmdcRegistrationNumber + "' already exists");
            }
        }
        
        // Generate doctor code
        String doctorCode = generateDoctorCode(department.getDepartmentName(), request.getDoctorName());
        
        // Build doctor entity
        Doctor doctor = Doctor.builder()
                .doctorCode(doctorCode)
                .doctorName(request.getDoctorName())
                .department(department)
                .doctorType(request.getDoctorType())
                .indoorOutdoorStatus(request.getIndoorOutdoorStatus())
                .degree(normalizedDegree)
                .speciality(request.getSpeciality())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .registrationDate(request.getRegistrationDate())
                .bmdcRegistrationNumber(normalizedBmdcRegistrationNumber)
                .phoneNumber(request.getPhoneNumber())
                .email(normalizedEmail)
                .presentAddress(request.getPresentAddress())
                .district(request.getDistrict())
                .thana(request.getThana())
                .area(request.getArea())
                .chamberRoom(request.getChamberRoom())
                .visitFeeNew(request.getVisitFeeNew())
                .visitFeeOld(request.getVisitFeeOld())
                .takeCommission(request.getTakeCommission() != null ? request.getTakeCommission() : false)
                .patientsPerDay(request.getPatientsPerDay())
                .serialStartFrom(request.getSerialStartFrom() != null ? request.getSerialStartFrom() : 1)
                .numberOfDaysCanAppointment(request.getNumberOfDaysCanAppointment())
                .numberOfAppointmentsFromWeb(request.getNumberOfAppointmentsFromWeb())
                .numberOfAppointmentsFromMobile(request.getNumberOfAppointmentsFromMobile())
                .appointmentsFromWeb(request.getAppointmentsFromWeb() != null ? request.getAppointmentsFromWeb() : false)
                .appointmentsFromMobile(request.getAppointmentsFromMobile() != null ? request.getAppointmentsFromMobile() : false)
                .slotsPerDay(request.getSlotsPerDay() != null ? request.getSlotsPerDay() : 1)
                .weeklySchedule(request.getWeeklySchedule())
                .appointmentSlots(request.getAppointmentSlots())
                .offDays(request.getOffDays())
                .smsEnabled(request.getSmsEnabled() != null ? request.getSmsEnabled() : false)
                .prescriptionStatus(request.getPrescriptionStatus())
                .availabilityStatus(request.getAvailabilityStatus() != null ? request.getAvailabilityStatus() : Doctor.AvailabilityStatus.AVAILABLE)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(userId)
                .build();
        
        doctor = doctorRepository.save(doctor);
        log.info("Created doctor: {} with code: {} and ID: {}", doctor.getDoctorName(), doctor.getDoctorCode(), doctor.getDoctorId());

        boolean wantPortalUser = !Boolean.FALSE.equals(request.getCreateLinkedUser());
        if (wantPortalUser && Boolean.TRUE.equals(doctor.getIsActive())) {
            provisionPortalUserAndPrescribingRole(
                    doctor, normalizedEmail, request.getDoctorName(), request.getPhoneNumber(), userId, organizationId);
        }

        syncSchedulingAsync(doctor, userId, organizationId);

        return mapToResponse(doctor);
    }

    /**
     * Update doctor. Actors with only {@code hospital.doctor_schedule}/{@code manage} may update
     * appointment slots / weekly template / off-days only (see {@link #updateDoctorScheduleFieldsOnly}).
     */
    @Transactional
    public DoctorResponse updateDoctor(UUID doctorId, DoctorRequest request, String userId, UUID organizationId) {
        Optional<UUID> actorOpt = tryParseActorUuid(userId);
        if (actorOpt.isPresent()) {
            UUID actor = actorOpt.get();
            boolean hospitalManage = rbacPermissionService.hasHospitalManage(actor, organizationId);
            boolean scheduleManage = rbacPermissionService.hasDoctorScheduleManage(actor, organizationId);
            if (!hospitalManage && !scheduleManage) {
                rbacPermissionService.requireHospitalManage(actor, organizationId);
            }
            if (!hospitalManage && scheduleManage) {
                return updateDoctorScheduleFieldsOnly(doctorId, request, userId, organizationId);
            }
        }
        return updateDoctorFull(doctorId, request, userId, organizationId);
    }

    /**
     * Applies only schedule-related fields for narrow roles (doctor attendants).
     */
    private DoctorResponse updateDoctorScheduleFieldsOnly(UUID doctorId, DoctorRequest request, String userId, UUID organizationId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        if (request.getSlotsPerDay() != null) {
            doctor.setSlotsPerDay(request.getSlotsPerDay());
        }
        if (request.getWeeklySchedule() != null) {
            doctor.setWeeklySchedule(request.getWeeklySchedule());
        }
        if (request.getAppointmentSlots() != null) {
            doctor.setAppointmentSlots(request.getAppointmentSlots());
        }
        if (request.getOffDays() != null) {
            doctor.setOffDays(request.getOffDays());
        }
        doctor.setUpdatedBy(truncateUpdatedBy(userId));
        doctor = doctorRepository.save(doctor);
        log.info("Schedule-only doctor update: {} ({})", doctor.getDoctorId(), doctor.getDoctorName());
        syncSchedulingAsync(doctor, userId, organizationId);
        return mapToResponse(doctor);
    }

    private static Optional<UUID> tryParseActorUuid(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        String t = userId.trim();
        if ("system".equalsIgnoreCase(t)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(t));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private DoctorResponse updateDoctorFull(UUID doctorId, DoctorRequest request, String userId, UUID organizationId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        boolean wasActive = Boolean.TRUE.equals(doctor.getIsActive());
        boolean deactivating = wasActive && Boolean.FALSE.equals(request.getIsActive());
        String normalizedDegree = deactivating
                ? coalesceTrimmedOrFallback(request.getDegree(), doctor.getDegree())
                : normalizeRequiredText(request.getDegree(), "Degree");
        String normalizedEmail = normalizeToNull(request.getEmail());
        String normalizedBmdcRegistrationNumber = deactivating
                ? coalesceTrimmedOrFallback(request.getBmdcRegistrationNumber(), doctor.getBmdcRegistrationNumber())
                : normalizeRequiredText(request.getBmdcRegistrationNumber(), "BMDC registration number");
        
        // Validate department if changed
        if (!doctor.getDepartment().getDepartmentId().equals(request.getDepartmentId())) {
            DoctorDepartment department = doctorDepartmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + request.getDepartmentId()));
            
            if (department.getStatus() != DoctorDepartment.DepartmentStatus.ACTIVE) {
                throw new RuntimeException("Cannot assign doctor to inactive department");
            }
            doctor.setDepartment(department);
            
            // Regenerate doctor code if department or name changed
            if (!doctor.getDoctorName().equals(request.getDoctorName()) || 
                !doctor.getDepartment().getDepartmentId().equals(request.getDepartmentId())) {
                String newCode = generateDoctorCode(department.getDepartmentName(), request.getDoctorName());
                doctor.setDoctorCode(newCode);
            }
        }
        
        // Check email uniqueness if changed
        if (normalizedEmail != null) {
            if (!normalizedEmail.equals(doctor.getEmail()) &&
                doctorRepository.existsByEmail(normalizedEmail)) {
                throw new RuntimeException("Doctor with email '" + normalizedEmail + "' already exists");
            }
        }
        
        // Check BMDC registration number uniqueness if changed
        if (normalizedBmdcRegistrationNumber != null) {
            if (!normalizedBmdcRegistrationNumber.equals(doctor.getBmdcRegistrationNumber()) &&
                doctorRepository.existsByBmdcRegistrationNumber(normalizedBmdcRegistrationNumber)) {
                throw new RuntimeException("Doctor with BMDC registration number '" + normalizedBmdcRegistrationNumber + "' already exists");
            }
        }
        
        // Update fields
        doctor.setDoctorName(request.getDoctorName());
        doctor.setDoctorType(request.getDoctorType());
        doctor.setIndoorOutdoorStatus(request.getIndoorOutdoorStatus());
        doctor.setDegree(normalizedDegree);
        doctor.setSpeciality(request.getSpeciality());
        doctor.setGender(request.getGender());
        doctor.setBirthDate(request.getBirthDate());
        doctor.setRegistrationDate(request.getRegistrationDate());
        doctor.setBmdcRegistrationNumber(normalizedBmdcRegistrationNumber);
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setEmail(normalizedEmail);
        doctor.setPresentAddress(request.getPresentAddress());
        doctor.setDistrict(request.getDistrict());
        doctor.setThana(request.getThana());
        doctor.setArea(request.getArea());
        doctor.setChamberRoom(request.getChamberRoom());
        doctor.setVisitFeeNew(request.getVisitFeeNew());
        doctor.setVisitFeeOld(request.getVisitFeeOld());
        if (request.getTakeCommission() != null) {
            doctor.setTakeCommission(request.getTakeCommission());
        }
        doctor.setPatientsPerDay(request.getPatientsPerDay());
        if (request.getSerialStartFrom() != null) {
            doctor.setSerialStartFrom(request.getSerialStartFrom());
        }
        doctor.setNumberOfDaysCanAppointment(request.getNumberOfDaysCanAppointment());
        doctor.setNumberOfAppointmentsFromWeb(request.getNumberOfAppointmentsFromWeb());
        doctor.setNumberOfAppointmentsFromMobile(request.getNumberOfAppointmentsFromMobile());
        if (request.getAppointmentsFromWeb() != null) {
            doctor.setAppointmentsFromWeb(request.getAppointmentsFromWeb());
        }
        if (request.getAppointmentsFromMobile() != null) {
            doctor.setAppointmentsFromMobile(request.getAppointmentsFromMobile());
        }
        if (request.getSlotsPerDay() != null) {
            doctor.setSlotsPerDay(request.getSlotsPerDay());
        }
        if (request.getWeeklySchedule() != null) {
            doctor.setWeeklySchedule(request.getWeeklySchedule());
        }
        if (request.getAppointmentSlots() != null) {
            doctor.setAppointmentSlots(request.getAppointmentSlots());
        }
        if (request.getOffDays() != null) {
            doctor.setOffDays(request.getOffDays());
        }
        if (request.getSmsEnabled() != null) {
            doctor.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getPrescriptionStatus() != null) {
            doctor.setPrescriptionStatus(request.getPrescriptionStatus());
        }
        if (request.getAvailabilityStatus() != null) {
            doctor.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        if (request.getIsActive() != null) {
            doctor.setIsActive(request.getIsActive());
        }
        doctor.setUpdatedBy(truncateUpdatedBy(userId));
        
        doctor = doctorRepository.save(doctor);
        log.info("Updated doctor: {} with code: {} and ID: {}", doctor.getDoctorName(), doctor.getDoctorCode(), doctor.getDoctorId());

        boolean nowActive = Boolean.TRUE.equals(doctor.getIsActive());
        if (Boolean.TRUE.equals(request.getCreateLinkedUser())
                && doctor.getLinkedUserId() == null
                && nowActive) {
            provisionPortalUserAndPrescribingRole(
                    doctor, normalizedEmail, request.getDoctorName(), request.getPhoneNumber(), userId, organizationId);
        }

        if (wasActive && !nowActive) {
            deactivateLinkedPortalUserBestEffort(doctor, userId, organizationId);
        }
        if (!wasActive && nowActive) {
            activateLinkedPortalUserIfPresent(doctor, userId, organizationId);
        }
        if (doctor.getLinkedUserId() != null && nowActive) {
            ensureLinkedPortalUserHasRequiredRoles(doctor, userId, organizationId);
        }

        syncSchedulingAsync(doctor, userId, organizationId);

        return mapToResponse(doctor);
    }

    /**
     * Delete doctor (soft delete by setting isActive to false).
     * When {@link Doctor#getLinkedUserId()} is set, the linked {@code users.users} row is soft-deactivated via user-management.
     */
    @Transactional
    public void deleteDoctor(UUID doctorId, String userId, UUID organizationId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found with ID: " + doctorId));

        boolean wasActive = Boolean.TRUE.equals(doctor.getIsActive());
        deactivateLinkedPortalUserBestEffort(doctor, userId, organizationId);

        doctor.setIsActive(false);
        doctor.setAvailabilityStatus(Doctor.AvailabilityStatus.NOT_AVAILABLE);
        doctor.setAppointmentsFromWeb(false);
        doctor.setAppointmentsFromMobile(false);
        doctor.setUpdatedBy(truncateUpdatedBy(userId));
        doctorRepository.save(doctor);

        if (wasActive) {
            syncSchedulingAsync(doctor, userId, organizationId);
        }

        log.info("Deleted (deactivated) doctor: {} with code: {} and ID: {} (wasActive={})",
                doctor.getDoctorName(), doctor.getDoctorCode(), doctor.getDoctorId(), wasActive);
    }
    
    /**
     * Best-effort sync of doctor schedule to hospital-scheduling-service.
     * Errors are logged as warnings and do not affect the doctor save transaction.
     */
    private void syncSchedulingAsync(Doctor doctor, String userId, UUID organizationId) {
        try {
            UUID departmentId = doctor.getDepartment() != null
                    ? doctor.getDepartment().getDepartmentId() : null;
            String resourceId = schedulingServiceClient.findOrCreateResource(
                    doctor.getDoctorId().toString(),
                    doctor.getDoctorName(),
                    departmentId,
                    userId,
                    organizationId);

            if (resourceId == null) {
                log.warn("Scheduling sync skipped for doctor {} — could not resolve resource", doctor.getDoctorId());
                return;
            }

            if (doctor.getAppointmentSlots() != null && !doctor.getAppointmentSlots().isEmpty()) {
                schedulingServiceClient.syncWorkingHours(resourceId, doctor.getAppointmentSlots(), userId, organizationId);
            }

            schedulingServiceClient.syncBlackouts(resourceId, doctor.getOffDays(), userId, organizationId);

        } catch (Exception e) {
            log.warn("Scheduling sync failed for doctor {}: {}", doctor.getDoctorId(), e.getMessage());
        }
    }

    /**
     * Map entity to response DTO
     */
    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .doctorId(doctor.getDoctorId())
                .doctorCode(doctor.getDoctorCode())
                .doctorName(doctor.getDoctorName())
                .departmentId(doctor.getDepartment().getDepartmentId())
                .departmentName(doctor.getDepartment().getDepartmentName())
                .doctorType(doctor.getDoctorType())
                .indoorOutdoorStatus(doctor.getIndoorOutdoorStatus())
                .degree(doctor.getDegree())
                .speciality(doctor.getSpeciality())
                .gender(doctor.getGender())
                .birthDate(doctor.getBirthDate())
                .registrationDate(doctor.getRegistrationDate())
                .bmdcRegistrationNumber(doctor.getBmdcRegistrationNumber())
                .phoneNumber(doctor.getPhoneNumber())
                .email(doctor.getEmail())
                .presentAddress(doctor.getPresentAddress())
                .district(doctor.getDistrict())
                .thana(doctor.getThana())
                .area(doctor.getArea())
                .chamberRoom(doctor.getChamberRoom())
                .visitFeeNew(doctor.getVisitFeeNew())
                .visitFeeOld(doctor.getVisitFeeOld())
                .takeCommission(doctor.getTakeCommission())
                .patientsPerDay(doctor.getPatientsPerDay())
                .serialStartFrom(doctor.getSerialStartFrom())
                .numberOfDaysCanAppointment(doctor.getNumberOfDaysCanAppointment())
                .numberOfAppointmentsFromWeb(doctor.getNumberOfAppointmentsFromWeb())
                .numberOfAppointmentsFromMobile(doctor.getNumberOfAppointmentsFromMobile())
                .appointmentsFromWeb(doctor.getAppointmentsFromWeb())
                .appointmentsFromMobile(doctor.getAppointmentsFromMobile())
                .slotsPerDay(doctor.getSlotsPerDay())
                .weeklySchedule(doctor.getWeeklySchedule())
                .appointmentSlots(doctor.getAppointmentSlots())
                .offDays(doctor.getOffDays())
                .smsEnabled(doctor.getSmsEnabled())
                .prescriptionStatus(doctor.getPrescriptionStatus())
                .availabilityStatus(doctor.getAvailabilityStatus())
                .isActive(doctor.getIsActive())
                .linkedUserId(doctor.getLinkedUserId())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .createdBy(doctor.getCreatedBy())
                .updatedBy(doctor.getUpdatedBy())
                .build();
    }

    /**
     * Creates {@code users.users} + assigns {@link #prescribingAuthorityRoleCode} in rbac for this doctor's portal login.
     */
    private void provisionPortalUserAndPrescribingRole(
            Doctor doctor,
            String normalizedEmail,
            String doctorDisplayName,
            String phoneNumber,
            String userId,
            UUID organizationId) {
        UUID actorUserId = requireActorUserUuid(userId,
                "Cannot create a portal user for this doctor without a valid X-User-Id header (UUID of the acting administrator).");
        String[] nameParts = splitDoctorDisplayName(doctorDisplayName);
        UserManagementUserResponse created = userManagementClient.createUser(
                doctor.getDoctorCode(),
                normalizedEmail,
                defaultPortalUserPassword,
                nameParts[0],
                nameParts[1],
                phoneNumber,
                actorUserId,
                organizationId);
        if (created == null || created.getId() == null) {
            throw new RuntimeException("user-management did not return a user id for the new portal account");
        }
        doctor.setLinkedUserId(created.getId());
        doctorRepository.save(doctor);
        log.info("Created portal user {} for doctor {}", created.getId(), doctor.getDoctorId());

        RbacRoleResponse prescribingRole = rbacAuthorizationClient.getRoleByCode(
                prescribingAuthorityRoleCode, actorUserId, organizationId);
        if (prescribingRole == null || prescribingRole.getId() == null) {
            throw new RuntimeException("rbac did not return role id for code: " + prescribingAuthorityRoleCode);
        }
        RbacRoleResponse baseUserRole = rbacAuthorizationClient.getRoleByCode(
                baseUserRoleCode, actorUserId, organizationId);
        if (baseUserRole == null || baseUserRole.getId() == null) {
            throw new RuntimeException(
                    "rbac did not return role id for code: " + baseUserRoleCode
                            + " — portal users require " + baseUserRoleCode + " and " + prescribingAuthorityRoleCode);
        }
        assignRequiredPortalRolesPreservingExisting(
                created.getId(), prescribingRole, baseUserRole, organizationId, actorUserId);
    }

    /**
     * RBAC assign replaces org-scoped roles; merge existing assignments so manually granted roles are kept.
     */
    private void assignRequiredPortalRolesPreservingExisting(
            UUID portalUserId,
            RbacRoleResponse prescribingRole,
            RbacRoleResponse baseUserRole,
            UUID organizationId,
            UUID actorUserId) {
        Set<UUID> roleIds = new LinkedHashSet<>();
        List<RbacRoleResponse> existing =
                rbacAuthorizationClient.getUserRoles(portalUserId, organizationId, actorUserId);
        for (RbacRoleResponse role : existing) {
            if (role.getId() != null) {
                roleIds.add(role.getId());
            }
        }
        roleIds.add(prescribingRole.getId());
        roleIds.add(baseUserRole.getId());
        rbacAuthorizationClient.assignRolesToUser(
                portalUserId, roleIds, organizationId, actorUserId, organizationId);
        log.info("Assigned portal roles {} to user {}", roleIds, portalUserId);
    }

    private void ensureLinkedPortalUserHasRequiredRoles(Doctor doctor, String userId, UUID organizationId) {
        if (doctor.getLinkedUserId() == null) {
            return;
        }
        UUID actorUserId = requireActorUserUuid(userId,
                "Cannot update portal roles for this doctor without a valid X-User-Id header (UUID of the acting administrator).");
        RbacRoleResponse prescribingRole = rbacAuthorizationClient.getRoleByCode(
                prescribingAuthorityRoleCode, actorUserId, organizationId);
        if (prescribingRole == null || prescribingRole.getId() == null) {
            log.warn("Skipping portal role repair for doctor {} — {} role not found",
                    doctor.getDoctorId(), prescribingAuthorityRoleCode);
            return;
        }
        RbacRoleResponse baseUserRole = rbacAuthorizationClient.getRoleByCode(
                baseUserRoleCode, actorUserId, organizationId);
        if (baseUserRole == null || baseUserRole.getId() == null) {
            log.warn("Skipping portal role repair for doctor {} — {} role not found",
                    doctor.getDoctorId(), baseUserRoleCode);
            return;
        }
        assignRequiredPortalRolesPreservingExisting(
                doctor.getLinkedUserId(), prescribingRole, baseUserRole, organizationId, actorUserId);
    }

    /**
     * Soft-deactivate the linked portal user when a doctor is retired. Failures are logged and ignored
     * so doctor soft-delete always completes (portal login can be fixed manually if needed).
     */
    private void deactivateLinkedPortalUserBestEffort(Doctor doctor, String userId, UUID organizationId) {
        if (doctor.getLinkedUserId() == null) {
            return;
        }
        Optional<UUID> actorUserId = tryParseActorUuid(userId);
        if (actorUserId.isEmpty()) {
            log.warn("Skipping linked portal user deactivation for doctor {} — missing or invalid X-User-Id",
                    doctor.getDoctorId());
            return;
        }
        try {
            userManagementClient.deactivateUser(doctor.getLinkedUserId(), actorUserId.get(), organizationId);
            log.info("Deactivated portal user {} for doctor {}", doctor.getLinkedUserId(), doctor.getDoctorId());
        } catch (Exception e) {
            log.warn("Could not deactivate linked portal user {} for doctor {}: {}",
                    doctor.getLinkedUserId(), doctor.getDoctorId(), e.getMessage());
        }
    }

    private static String truncateUpdatedBy(String userId) {
        if (userId == null) {
            return "system";
        }
        String trimmed = userId.trim();
        if (trimmed.isEmpty()) {
            return "system";
        }
        return trimmed.length() <= 100 ? trimmed : trimmed.substring(0, 100);
    }

    private void activateLinkedPortalUserIfPresent(Doctor doctor, String userId, UUID organizationId) {
        if (doctor.getLinkedUserId() == null) {
            return;
        }
        UUID actorUserId = requireActorUserUuid(userId,
                "Cannot activate the linked portal user without a valid X-User-Id header (UUID of the acting administrator).");
        userManagementClient.activateUser(doctor.getLinkedUserId(), actorUserId, organizationId);
        log.info("Activated portal user {} for doctor {}", doctor.getLinkedUserId(), doctor.getDoctorId());
        ensureLinkedPortalUserHasRequiredRoles(doctor, userId, organizationId);
    }

    private static UUID requireActorUserUuid(String userId, String message) {
        return tryParseActorUuid(userId)
                .orElseThrow(() -> new RuntimeException(message));
    }

    /**
     * First token → first name, remainder → last name (user-management allows blank last name).
     */
    private static String[] splitDoctorDisplayName(String doctorName) {
        String t = doctorName == null ? "" : doctorName.trim();
        if (t.isEmpty()) {
            return new String[] { "Doctor", "" };
        }
        String[] parts = t.split("\\s+");
        if (parts.length == 1) {
            return new String[] { parts[0], "" };
        }
        String first = parts[0];
        StringBuilder rest = new StringBuilder(parts[1]);
        for (int i = 2; i < parts.length; i++) {
            rest.append(' ').append(parts[i]);
        }
        return new String[] { first, rest.toString() };
    }
}
