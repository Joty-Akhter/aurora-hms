package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PatientRequest;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.integration.card.PatientIdentityCardIssuanceResult;
import com.easyops.hospital.integration.card.PatientIdentityCardIssuanceService;
import com.easyops.hospital.repository.PatientConsentRepository;
import com.easyops.hospital.repository.PatientEmergencyContactRepository;
import com.easyops.hospital.repository.PatientIdentityCardAuditLogRepository;
import com.easyops.hospital.repository.PatientInsuranceRepository;
import com.easyops.hospital.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientServiceIdentityCardEventTest {

    @Test
    void createPatient_publishesPatientIdentityCardIssuedEventWhenCardIssued() {
        PatientRepository patientRepository = mock(PatientRepository.class);
        PatientEmergencyContactRepository emergencyContactRepository = mock(PatientEmergencyContactRepository.class);
        PatientInsuranceRepository insuranceRepository = mock(PatientInsuranceRepository.class);
        PatientConsentRepository consentRepository = mock(PatientConsentRepository.class);
        PatientIdentityCardAuditLogRepository auditLogRepository = mock(PatientIdentityCardAuditLogRepository.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        PatientIdentityCardIssuanceService issuanceService = mock(PatientIdentityCardIssuanceService.class);

        PatientService patientService = new PatientService(
                patientRepository,
                emergencyContactRepository,
                insuranceRepository,
                consentRepository,
                auditLogRepository,
                domainEventPublisher,
                issuanceService
        );

        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        String mrn = "HOSP-2026-000123";

        Patient savedPatient = Patient.builder()
                .patientId(patientId)
                .mrn(mrn)
                .fullName("Jane Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .patientStatus(Patient.PatientStatus.ACTIVE)
                .build();

        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(emergencyContactRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(insuranceRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(consentRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(issuanceService.issueOrResolveForNewPatient(eq(patientId), eq(userId), eq(mrn), any()))
                .thenReturn(PatientIdentityCardIssuanceResult.builder()
                        .status("ISSUED")
                        .cardId(cardId)
                        .cardNumber(mrn)
                        .build());

        PatientRequest request = new PatientRequest();
        request.setFullName("Jane Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        patientService.createPatient(request, userId, true);

        verify(domainEventPublisher).publish(
                eq("patient.identity_card.issued"),
                argThat(payload ->
                        payload != null
                                && patientId.equals(payload.get("patientId"))
                                && mrn.equals(payload.get("mrn"))
                                && mrn.equals(payload.get("cardIdentifier"))
                                && cardId.equals(payload.get("cardId"))
                                && userId.equals(payload.get("issuedBy")))
        );
    }

    @Test
    void createPatient_survivesCardIssuanceFailureAndReturnsFailedStatus() {
        PatientRepository patientRepository = mock(PatientRepository.class);
        PatientEmergencyContactRepository emergencyContactRepository = mock(PatientEmergencyContactRepository.class);
        PatientInsuranceRepository insuranceRepository = mock(PatientInsuranceRepository.class);
        PatientConsentRepository consentRepository = mock(PatientConsentRepository.class);
        PatientIdentityCardAuditLogRepository auditLogRepository = mock(PatientIdentityCardAuditLogRepository.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        PatientIdentityCardIssuanceService issuanceService = mock(PatientIdentityCardIssuanceService.class);

        PatientService patientService = new PatientService(
                patientRepository,
                emergencyContactRepository,
                insuranceRepository,
                consentRepository,
                auditLogRepository,
                domainEventPublisher,
                issuanceService
        );

        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String mrn = "HOSP-2026-000124";

        Patient savedPatient = Patient.builder()
                .patientId(patientId)
                .mrn(mrn)
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1991, 2, 2))
                .patientStatus(Patient.PatientStatus.ACTIVE)
                .build();

        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(emergencyContactRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(insuranceRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(consentRepository.findByPatientPatientId(eq(patientId))).thenReturn(List.of());
        when(issuanceService.issueOrResolveForNewPatient(eq(patientId), eq(userId), eq(mrn), any()))
                .thenReturn(PatientIdentityCardIssuanceResult.builder()
                        .status("FAILED")
                        .message("Card service unavailable")
                        .build());

        PatientRequest request = new PatientRequest();
        request.setFullName("John Doe");
        request.setDateOfBirth(LocalDate.of(1991, 2, 2));

        var response = patientService.createPatient(request, userId, true);
        assertEquals("FAILED", response.getIdentityCardStatus());
        assertEquals("Card service unavailable", response.getIdentityCardMessage());
        assertEquals(mrn, response.getMrn());
    }
}
