package com.easyops.hospitalscheduling.integration;

import com.easyops.hospitalscheduling.api.dto.CreateAppointmentRequest;
import com.easyops.hospitalscheduling.domain.appointment.Appointment;
import com.easyops.hospitalscheduling.integration.dto.PatientContactForSms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientSmsEnrichmentServiceTest {

    @Mock
    private HospitalPatientClient hospitalPatientClient;

    @Test
    void enrichCreateRequest_skipsPhoneWhenConsentTextMessagingIsFalse() {
        UUID pid = UUID.randomUUID();
        PatientContactForSms p = new PatientContactForSms();
        p.setPrimaryPhone("+8801999999999");
        p.setConsentTextMessaging(false);
        when(hospitalPatientClient.getPatient(pid)).thenReturn(p);

        PatientSmsEnrichmentService svc = new PatientSmsEnrichmentService(hospitalPatientClient, true);
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(pid);

        svc.enrichCreateRequest(req, pid);

        assertThat(req.getPatientSmsPhone()).isNull();
    }

    @Test
    void enrichCreateRequest_fillsPhoneWhenConsentNull() {
        UUID pid = UUID.randomUUID();
        PatientContactForSms p = new PatientContactForSms();
        p.setPrimaryPhone("+8801888888888");
        p.setConsentTextMessaging(null);
        when(hospitalPatientClient.getPatient(pid)).thenReturn(p);

        PatientSmsEnrichmentService svc = new PatientSmsEnrichmentService(hospitalPatientClient, true);
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(pid);

        svc.enrichCreateRequest(req, pid);

        assertThat(req.getPatientSmsPhone()).isEqualTo("+8801888888888");
    }

    @Test
    void enrichCreateRequest_fillsPhoneWhenRequestHasBlankString() {
        UUID pid = UUID.randomUUID();
        PatientContactForSms p = new PatientContactForSms();
        p.setPrimaryPhone("+8801777777777");
        when(hospitalPatientClient.getPatient(any())).thenReturn(p);

        PatientSmsEnrichmentService svc = new PatientSmsEnrichmentService(hospitalPatientClient, true);
        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setPatientId(pid);
        req.setPatientSmsPhone("   ");

        svc.enrichCreateRequest(req, pid);

        assertThat(req.getPatientSmsPhone()).isEqualTo("+8801777777777");
    }

    @Test
    void enrichAppointment_skipsPhoneWhenConsentFalse() {
        UUID pid = UUID.randomUUID();
        PatientContactForSms p = new PatientContactForSms();
        p.setPrimaryPhone("+8801666666666");
        p.setConsentTextMessaging(false);
        when(hospitalPatientClient.getPatient(pid)).thenReturn(p);

        PatientSmsEnrichmentService svc = new PatientSmsEnrichmentService(hospitalPatientClient, true);
        Appointment a = new Appointment();
        a.setPatientId(pid);
        a.setNotificationPatientPhone(null);

        svc.enrichAppointmentNotificationGaps(a);

        assertThat(a.getNotificationPatientPhone()).isNull();
    }
}
