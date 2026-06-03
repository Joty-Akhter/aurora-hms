package com.easyops.hospitalscheduling.integration;

import com.easyops.hospitalscheduling.api.dto.CreateAppointmentRequest;
import com.easyops.hospitalscheduling.domain.appointment.Appointment;
import com.easyops.hospitalscheduling.integration.dto.PatientContactForSms;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Loads patient contact from hospital-service for appointment SMS (waitlist promote, reschedule gaps).
 */
@Slf4j
@Service
public class PatientSmsEnrichmentService {

    private final HospitalPatientClient hospitalPatientClient;
    private final boolean enabled;

    public PatientSmsEnrichmentService(
            HospitalPatientClient hospitalPatientClient,
            @Value("${hospital.integration.patient-lookup.enabled:true}") boolean enabled
    ) {
        this.hospitalPatientClient = hospitalPatientClient;
        this.enabled = enabled;
    }

    public void enrichCreateRequest(CreateAppointmentRequest appReq, UUID patientId) {
        if (!enabled || patientId == null) {
            return;
        }
        try {
            PatientContactForSms p = hospitalPatientClient.getPatient(patientId);
            if (p == null) {
                return;
            }
            if (blank(appReq.getPatientSmsPhone()) && smsConsentAllowsRegistryPhone(p)) {
                String phone = firstNonBlankPhone(p.getPrimaryPhone(), p.getSecondaryPhone());
                if (phone != null) {
                    appReq.setPatientSmsPhone(phone);
                }
            }
            if (blank(appReq.getPatientSmsDisplayName()) && p.getFullName() != null && !p.getFullName().isBlank()) {
                appReq.setPatientSmsDisplayName(p.getFullName().trim());
            }
        } catch (Exception ex) {
            log.warn("Could not load patient {} from hospital-service for SMS fields: {}", patientId, ex.getMessage());
        }
    }

    /**
     * Fills blank {@link Appointment#getNotificationPatientPhone()} / {@link Appointment#getNotificationPatientName()}
     * from hospital registry when lookup is enabled.
     */
    public void enrichAppointmentNotificationGaps(Appointment appointment) {
        if (!enabled || appointment == null || appointment.getPatientId() == null) {
            return;
        }
        boolean needsPhone = blank(appointment.getNotificationPatientPhone());
        boolean needsName = blank(appointment.getNotificationPatientName());
        if (!needsPhone && !needsName) {
            return;
        }
        try {
            PatientContactForSms p = hospitalPatientClient.getPatient(appointment.getPatientId());
            if (p == null) {
                return;
            }
            if (needsPhone && smsConsentAllowsRegistryPhone(p)) {
                String phone = firstNonBlankPhone(p.getPrimaryPhone(), p.getSecondaryPhone());
                if (phone != null) {
                    appointment.setNotificationPatientPhone(phone);
                }
            }
            if (needsName && p.getFullName() != null && !p.getFullName().isBlank()) {
                appointment.setNotificationPatientName(p.getFullName().trim());
            }
        } catch (Exception ex) {
            log.warn("Could not load patient {} from hospital-service for appointment SMS fields: {}",
                    appointment.getPatientId(), ex.getMessage());
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** null/true → allow; explicit false → patient opted out of text messaging in registry. */
    private static boolean smsConsentAllowsRegistryPhone(PatientContactForSms p) {
        return !Boolean.FALSE.equals(p.getConsentTextMessaging());
    }

    private static String firstNonBlankPhone(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return null;
    }
}
