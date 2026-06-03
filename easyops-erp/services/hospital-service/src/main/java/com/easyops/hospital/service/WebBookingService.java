package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PatientRequest;
import com.easyops.hospital.dto.request.WebBookingAppointmentRequest;
import com.easyops.hospital.dto.response.PatientResponse;
import com.easyops.hospital.dto.response.WebBookableDoctorResponse;
import com.easyops.hospital.dto.response.WebBookingAppointmentResponse;
import com.easyops.hospital.dto.response.WebBookingAvailabilityResponse;
import com.easyops.hospital.dto.response.WebBookingSlotResponse;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.integration.scheduling.SchedulingServiceClient;
import com.easyops.hospital.repository.DoctorRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebBookingService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6");
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final int DEFAULT_BOOKING_WINDOW_DAYS = 30;

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final SchedulingServiceClient schedulingServiceClient;
    private final JdbcTemplate jdbcTemplate;

    @Value("${hospital.web-booking.organization-id:a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6}")
    private String organizationIdConfig;

    /** scheduling.api or other service account UUID with APPOINTMENT_VIEW/BOOK for ASHK. */
    @Value("${hospital.web-booking.scheduling-user-id:}")
    private String schedulingUserIdConfig;

    private String resolvedSchedulingUserId;

    @PostConstruct
    void resolveSchedulingServiceAccount() {
        if (schedulingUserIdConfig != null && !schedulingUserIdConfig.isBlank()) {
            resolvedSchedulingUserId = schedulingUserIdConfig.trim();
            return;
        }
        try {
            UUID schedulingApiUserId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users.users WHERE username = 'scheduling.api' AND is_active = true LIMIT 1",
                    UUID.class);
            resolvedSchedulingUserId = schedulingApiUserId.toString();
            log.info("Web booking using scheduling.api user {}", resolvedSchedulingUserId);
        } catch (EmptyResultDataAccessException ex) {
            resolvedSchedulingUserId = SYSTEM_ACTOR_ID.toString();
            log.warn("scheduling.api user not found; web booking falls back to {}", resolvedSchedulingUserId);
        }
    }

    private UUID organizationId() {
        try {
            return UUID.fromString(organizationIdConfig);
        } catch (IllegalArgumentException ex) {
            return DEFAULT_ORG_ID;
        }
    }

    private String schedulingUserId() {
        return resolvedSchedulingUserId != null ? resolvedSchedulingUserId : SYSTEM_ACTOR_ID.toString();
    }

    private UUID actorUserId() {
        try {
            return UUID.fromString(schedulingUserId());
        } catch (IllegalArgumentException ex) {
            return SYSTEM_ACTOR_ID;
        }
    }

    public List<WebBookableDoctorResponse> listWebBookableDoctors() {
        return doctorRepository.findWebBookableDoctors().stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    public List<WebBookingAvailabilityResponse> getDoctorAvailability(UUID doctorId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_date_range");
        }
        Doctor doctor = requireWebBookableDoctor(doctorId);
        validateDateWithinBookingWindow(doctor, fromDate);
        validateDateWithinBookingWindow(doctor, toDate);

        String resourceId = requireSchedulingResourceId(doctor);
        List<Map<String, Object>> raw;
        try {
            raw = schedulingServiceClient.getAvailabilityRequired(
                    resourceId, fromDate, toDate, schedulingUserId(), organizationId());
        } catch (RestClientException ex) {
            log.warn("Availability lookup failed for doctor {}: {}", doctorId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "scheduling_unavailable");
        }
        return mapAvailability(raw);
    }

    @Transactional
    public WebBookingAppointmentResponse bookAppointment(WebBookingAppointmentRequest request) {
        Doctor doctor = requireWebBookableDoctor(request.getDoctorId());
        OffsetDateTime slotStart = parseSlotTimestamp(request.getSlotStart(), "invalid_slot_start");
        OffsetDateTime slotEnd = parseSlotTimestamp(request.getSlotEnd(), "invalid_slot_end");
        if (!slotEnd.isAfter(slotStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_slot_range");
        }

        LocalDate appointmentDate = slotStart.toLocalDate();
        if (request.getAppointmentDate() != null && !request.getAppointmentDate().isBlank()) {
            LocalDate requestedDate = parseAppointmentDate(request.getAppointmentDate());
            if (!requestedDate.equals(appointmentDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slot_date_mismatch");
            }
        }
        validateDateWithinBookingWindow(doctor, appointmentDate);

        if (!slotStart.isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slot_in_past");
        }

        String resourceId = requireSchedulingResourceId(doctor);
        validateSlotStillAvailable(resourceId, request.getSlotStart(), request.getSlotEnd());

        PatientRequest patientRequest = PatientRequest.builder()
                .fullName(request.getFullName().trim())
                .primaryPhone(request.getPrimaryPhone().trim())
                .primaryEmail(request.getPrimaryEmail())
                .ageYears(request.getAgeYears() != null ? request.getAgeYears() : 30)
                .organizationId(organizationId())
                .consentTextMessaging(true)
                .patientStatus(Patient.PatientStatus.ACTIVE)
                .build();

        PatientResponse patient = patientService.createPatient(patientRequest, actorUserId(), true);

        Map<String, Object> appointmentBody = new LinkedHashMap<>();
        appointmentBody.put("patientId", patient.getPatientId().toString());
        appointmentBody.put("resourceId", resourceId);
        appointmentBody.put("appointmentDate", appointmentDate.toString());
        appointmentBody.put("slotStart", request.getSlotStart());
        appointmentBody.put("slotEnd", request.getSlotEnd());
        appointmentBody.put("appointmentType", "NEW");
        appointmentBody.put("bookingChannel", "WEB");
        appointmentBody.put("patientSmsDisplayName", request.getFullName().trim());
        appointmentBody.put("patientSmsPhone", request.getPrimaryPhone().trim());
        if (doctor.getSerialStartFrom() != null) {
            appointmentBody.put("serialStartFrom", doctor.getSerialStartFrom());
        }

        try {
            Map<String, Object> created = schedulingServiceClient.createAppointment(
                    appointmentBody, schedulingUserId(), organizationId());
            UUID appointmentId = created != null && created.get("id") != null
                    ? UUID.fromString(String.valueOf(created.get("id")))
                    : null;
            return WebBookingAppointmentResponse.builder()
                    .appointmentId(appointmentId)
                    .patientId(patient.getPatientId())
                    .mrn(patient.getMrn())
                    .message("Appointment request submitted successfully.")
                    .build();
        } catch (RestClientException ex) {
            log.warn("Web booking failed for doctor {}: {}", doctor.getDoctorId(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "appointment_booking_failed");
        }
    }

    private Doctor requireWebBookableDoctor(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "doctor_not_found"));
        if (!Boolean.TRUE.equals(doctor.getIsActive()) || !Boolean.TRUE.equals(doctor.getAppointmentsFromWeb())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctor_not_available_for_web_booking");
        }
        return doctor;
    }

    private String requireSchedulingResourceId(Doctor doctor) {
        String resourceId = doctorService.findOrCreateSchedulingResource(
                doctor.getDoctorId(), schedulingUserId(), organizationId());
        if (resourceId == null || resourceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "scheduling_unavailable");
        }
        return resourceId;
    }

    private LocalDate parseAppointmentDate(String appointmentDate) {
        try {
            return LocalDate.parse(appointmentDate);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_appointment_date");
        }
    }

    private void validateDateWithinBookingWindow(Doctor doctor, LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointment_date_in_past");
        }
        int windowDays = doctor.getNumberOfDaysCanAppointment() != null && doctor.getNumberOfDaysCanAppointment() > 0
                ? doctor.getNumberOfDaysCanAppointment()
                : DEFAULT_BOOKING_WINDOW_DAYS;
        LocalDate maxDate = today.plusDays(windowDays);
        if (date.isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointment_date_outside_booking_window");
        }
    }

    private void validateSlotStillAvailable(String resourceId, String slotStart, String slotEnd) {
        LocalDate date = parseSlotTimestamp(slotStart, "invalid_slot_start").toLocalDate();
        List<Map<String, Object>> raw;
        try {
            raw = schedulingServiceClient.getAvailabilityRequired(
                    resourceId, date, date, schedulingUserId(), organizationId());
        } catch (RestClientException ex) {
            log.warn("Slot re-validation failed for resource {}: {}", resourceId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "scheduling_unavailable");
        }

        for (WebBookingAvailabilityResponse day : mapAvailability(raw)) {
            if (day.getSlots() == null) {
                continue;
            }
            for (WebBookingSlotResponse slot : day.getSlots()) {
                if (slotTimestampsMatch(slotStart, slot.getStart())
                        && slotTimestampsMatch(slotEnd, slot.getEnd())
                        && slot.getAvailableCount() > 0) {
                    return;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "slot_no_longer_available");
    }

    private OffsetDateTime parseSlotTimestamp(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
        }
    }

    private boolean slotTimestampsMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        try {
            return OffsetDateTime.parse(a.trim()).isEqual(OffsetDateTime.parse(b.trim()));
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private List<WebBookingAvailabilityResponse> mapAvailability(List<Map<String, Object>> raw) {
        List<WebBookingAvailabilityResponse> responses = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Map<String, Object> day : raw) {
            String date = day.get("date") != null ? String.valueOf(day.get("date")) : null;
            boolean blackedOut = Boolean.TRUE.equals(day.get("blackedOut"));
            List<WebBookingSlotResponse> slots = new ArrayList<>();
            Object slotsObj = day.get("slots");
            if (slotsObj instanceof List<?> slotList) {
                for (Object slotItem : slotList) {
                    if (!(slotItem instanceof Map<?, ?> slotMap)) {
                        continue;
                    }
                    int availableCount = slotMap.get("availableCount") instanceof Number n ? n.intValue() : 0;
                    if (availableCount <= 0) {
                        continue;
                    }
                    String start = slotMap.get("start") != null ? String.valueOf(slotMap.get("start")) : null;
                    String end = slotMap.get("end") != null ? String.valueOf(slotMap.get("end")) : null;
                    if (start != null) {
                        try {
                            if (OffsetDateTime.parse(start).isBefore(now)) {
                                continue;
                            }
                        } catch (DateTimeParseException ignored) {
                            // keep slot if parsing fails
                        }
                    }
                    slots.add(WebBookingSlotResponse.builder()
                            .start(start)
                            .end(end)
                            .availableCount(availableCount)
                            .build());
                }
            }
            responses.add(WebBookingAvailabilityResponse.builder()
                    .date(date)
                    .blackedOut(blackedOut)
                    .slots(slots)
                    .build());
        }
        return responses;
    }

    private WebBookableDoctorResponse toDoctorResponse(Doctor doctor) {
        String departmentName = doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentName() : null;
        return WebBookableDoctorResponse.builder()
                .doctorId(doctor.getDoctorId())
                .doctorName(doctor.getDoctorName())
                .doctorCode(doctor.getDoctorCode())
                .speciality(doctor.getSpeciality())
                .departmentName(departmentName)
                .numberOfDaysCanAppointment(doctor.getNumberOfDaysCanAppointment())
                .build();
    }
}
