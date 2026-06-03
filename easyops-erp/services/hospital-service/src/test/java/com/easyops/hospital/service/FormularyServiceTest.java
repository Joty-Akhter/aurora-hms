package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.FormularyCheckRequest;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.entity.PatientInsurance;
import com.easyops.hospital.entity.Prescription;
import com.easyops.hospital.repository.FormularyAlternativeRepository;
import com.easyops.hospital.repository.FormularyCheckRepository;
import com.easyops.hospital.repository.PatientInsuranceRepository;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormularyServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientInsuranceRepository patientInsuranceRepository;
    @Mock
    private FormularyCheckRepository formularyCheckRepository;
    @Mock
    private FormularyAlternativeRepository formularyAlternativeRepository;

    @Test
    void checkFormularyCoverage_rejectsPrescriptionInsuranceFromDifferentPatient() {
        FormularyService service = new FormularyService(
                null,
                prescriptionRepository,
                patientRepository,
                patientInsuranceRepository,
                formularyCheckRepository,
                formularyAlternativeRepository
        );

        UUID prescriptionId = UUID.randomUUID();
        UUID requestPatientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();

        Patient requestPatient = Patient.builder().patientId(requestPatientId).build();
        Patient otherPatient = Patient.builder().patientId(otherPatientId).build();

        Prescription prescription = Prescription.builder()
                .prescriptionId(prescriptionId)
                .patient(requestPatient)
                .insuranceId(insuranceId)
                .build();

        PatientInsurance insuranceOwnedByOtherPatient = PatientInsurance.builder()
                .insuranceId(insuranceId)
                .patient(otherPatient)
                .build();

        FormularyCheckRequest request = FormularyCheckRequest.builder()
                .patientId(requestPatientId)
                .prescriptionId(prescriptionId)
                .build();

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(requestPatientId)).thenReturn(Optional.of(requestPatient));
        when(patientInsuranceRepository.findByPatientPatientId(requestPatientId)).thenReturn(List.of());
        when(patientInsuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insuranceOwnedByOtherPatient));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkFormularyCoverage(request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Prescription insurance does not belong to the provided patient", ex.getReason());
    }
}
