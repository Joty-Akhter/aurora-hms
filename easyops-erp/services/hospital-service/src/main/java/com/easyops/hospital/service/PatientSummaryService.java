package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.PatientSummaryResponse;
import com.easyops.hospital.dto.response.PatientTimelineResponse;
import com.easyops.hospital.dto.response.PatientTimelineResponse.TimelineEvent;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientSummaryService {
    
    private final PatientRepository patientRepository;
    private final PatientProblemRepository problemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AllergyRepository allergyRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final ImmunizationRepository immunizationRepository;
    private final MedicationRepository medicationRepository;
    
    public PatientSummaryResponse getPatientSummary(UUID patientId) {
        log.info("Generating patient summary for patient: {}", patientId);
        
        // Get patient
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientId));
        
        // Get active problems
        List<PatientProblem> activeProblems = problemRepository.findCurrentProblemsByPatient(patientId);
        
        // Get active prescriptions
        List<Prescription> activePrescriptions = prescriptionRepository.findActivePrescriptionsByPatient(patientId);
        
        // Get active allergies
        List<Allergy> activeAllergies = allergyRepository.findActiveAllergiesByPatient(patientId);
        
        // Get latest vital signs
        VitalSigns latestVitalSigns = vitalSignsRepository.findLatestVitalSignsByPatient(patientId);
        
        // Get recent clinical notes (last 5)
        List<ClinicalNote> allNotes = clinicalNoteRepository.findCurrentVersionsByPatient(patientId);
        List<ClinicalNote> recentNotes = allNotes.stream()
            .limit(5)
            .collect(Collectors.toList());
        
        // Get recent immunizations (last 5)
        List<Immunization> allImmunizations = immunizationRepository.findByPatientPatientIdOrderByAdministrationDateDesc(patientId);
        List<Immunization> recentImmunizations = allImmunizations.stream()
            .limit(5)
            .collect(Collectors.toList());
        
        // Get active medications
        List<Medication> activeMedications = medicationRepository.findActiveMedicationsByPatient(patientId);
        
        // Build summary
        return PatientSummaryResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .dateOfBirth(patient.getDateOfBirth())
            .age(calculateAge(patient.getDateOfBirth()))
            .gender(patient.getGender())
            .activeProblemsCount(activeProblems.size())
            .activeProblems(activeProblems.stream()
                .map(this::mapToProblemSummary)
                .collect(Collectors.toList()))
            .activePrescriptionsCount(activePrescriptions.size())
            .activePrescriptions(activePrescriptions.stream()
                .map(this::mapToPrescriptionSummary)
                .collect(Collectors.toList()))
            .activeAllergiesCount(activeAllergies.size())
            .activeAllergies(activeAllergies.stream()
                .map(this::mapToAllergySummary)
                .collect(Collectors.toList()))
            .latestVitalSigns(mapToVitalSignsSummary(latestVitalSigns))
            .recentNotesCount(recentNotes.size())
            .recentNotes(recentNotes.stream()
                .map(this::mapToNoteSummary)
                .collect(Collectors.toList()))
            .recentImmunizationsCount(recentImmunizations.size())
            .recentImmunizations(recentImmunizations.stream()
                .map(this::mapToImmunizationSummary)
                .collect(Collectors.toList()))
            .activeMedicationsCount(activeMedications.size())
            .activeMedications(activeMedications.stream()
                .map(this::mapToMedicationSummary)
                .collect(Collectors.toList()))
            .lastUpdated(LocalDateTime.now())
            .build();
    }
    
    public PatientTimelineResponse getPatientTimeline(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating patient timeline for patient: {} from {} to {}", patientId, startDate, endDate);
        
        List<TimelineEvent> events = new ArrayList<>();
        
        // Get vital signs in date range
        vitalSignsRepository.findByPatientPatientIdAndMeasurementDateBetween(patientId, startDate, endDate)
            .forEach(vs -> events.add(TimelineEvent.builder()
                .eventDate(vs.getMeasurementDate())
                .eventTime(vs.getMeasurementTime())
                .eventType("VITAL_SIGNS")
                .title("Vital Signs Recorded")
                .description(String.format("BP: %s/%s, HR: %s, Temp: %s", 
                    vs.getSystolicBp(), vs.getDiastolicBp(), vs.getHeartRate(), vs.getTemperature()))
                .build()));
        
        // Get clinical notes in date range
        clinicalNoteRepository.findByPatientPatientIdAndNoteDateBetween(patientId, startDate, endDate)
            .forEach(note -> events.add(TimelineEvent.builder()
                .eventDate(note.getNoteDate())
                .eventTime(note.getNoteTime())
                .eventType("CLINICAL_NOTE")
                .title(note.getNoteType() + " Note")
                .description(note.getChiefComplaint() != null ? note.getChiefComplaint() : "Clinical note")
                .build()));
        
        // Get prescriptions in date range
        List<Prescription> allPrescriptions = prescriptionRepository.findByPatientPatientId(patientId);
        allPrescriptions.stream()
            .filter(p -> p.getStartDate() != null && 
                        !p.getStartDate().isBefore(startDate) && 
                        !p.getStartDate().isAfter(endDate))
            .forEach(prescription -> {
                String description = prescription.getInstructions();
                if (description == null || description.isEmpty()) {
                    description = prescription.getFrequency() != null ? prescription.getFrequency() : "";
                }
                events.add(TimelineEvent.builder()
                    .eventDate(prescription.getStartDate())
                    .eventType("PRESCRIPTION")
                    .title("Prescription: " + prescription.getMedicationName())
                    .description(description)
                    .build());
            });
        
        // Get problems documented in date range
        List<PatientProblem> allProblems = problemRepository.findByPatientPatientId(patientId);
        allProblems.stream()
            .filter(p -> p.getDocumentedDate() != null && 
                        !p.getDocumentedDate().isBefore(startDate) && 
                        !p.getDocumentedDate().isAfter(endDate))
            .forEach(problem -> events.add(TimelineEvent.builder()
                .eventDate(problem.getDocumentedDate())
                .eventType("PROBLEM")
                .title("Problem: " + problem.getProblemName())
                .description(problem.getProblemType() != null ? problem.getProblemType().toString() : "")
                .build()));
        
        // Sort events by date and time
        events.sort((e1, e2) -> {
            int dateCompare = e1.getEventDate().compareTo(e2.getEventDate());
            if (dateCompare != 0) return dateCompare;
            if (e1.getEventTime() != null && e2.getEventTime() != null) {
                return e1.getEventTime().compareTo(e2.getEventTime());
            }
            return 0;
        });
        
        return PatientTimelineResponse.builder()
            .patientId(patientId)
            .startDate(startDate)
            .endDate(endDate)
            .events(events)
            .totalEvents(events.size())
            .build();
    }
    
    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return LocalDate.now().getYear() - dateOfBirth.getYear() - 
            (LocalDate.now().getDayOfYear() < dateOfBirth.getDayOfYear() ? 1 : 0);
    }
    
    private PatientSummaryResponse.ProblemSummary mapToProblemSummary(PatientProblem problem) {
        return PatientSummaryResponse.ProblemSummary.builder()
            .problemId(problem.getProblemId())
            .problemName(problem.getProblemName())
            .problemType(problem.getProblemType().toString())
            .status(problem.getStatus().toString())
            .icd10Code(problem.getIcd10Code())
            .onsetDate(problem.getOnsetDate())
            .build();
    }
    
    private PatientSummaryResponse.PrescriptionSummary mapToPrescriptionSummary(Prescription prescription) {
        String dosage = "";
        if (prescription.getDosageStrength() != null && prescription.getDosageUnit() != null) {
            dosage = prescription.getDosageStrength() + " " + prescription.getDosageUnit();
        } else if (prescription.getDosageStrength() != null) {
            dosage = prescription.getDosageStrength().toString();
        }
        
        return PatientSummaryResponse.PrescriptionSummary.builder()
            .prescriptionId(prescription.getPrescriptionId())
            .medicationName(prescription.getMedicationName())
            .dosage(dosage)
            .route(prescription.getRoute() != null ? prescription.getRoute().toString() : "")
            .frequency(prescription.getFrequency())
            .startDate(prescription.getStartDate())
            .refillsRemaining(prescription.getRefillsRemaining())
            .build();
    }
    
    private PatientSummaryResponse.AllergySummary mapToAllergySummary(Allergy allergy) {
        return PatientSummaryResponse.AllergySummary.builder()
            .allergyId(allergy.getAllergyId())
            .allergenName(allergy.getAllergenName())
            .allergenType(allergy.getAllergenType().toString())
            .severity(allergy.getSeverity().toString())
            .reactionType(allergy.getReactionType())
            .build();
    }
    
    private PatientSummaryResponse.VitalSignsSummary mapToVitalSignsSummary(VitalSigns vitalSigns) {
        if (vitalSigns == null) return null;
        return PatientSummaryResponse.VitalSignsSummary.builder()
            .measurementDate(vitalSigns.getMeasurementDate())
            .measurementTime(vitalSigns.getMeasurementTime())
            .systolicBp(vitalSigns.getSystolicBp())
            .diastolicBp(vitalSigns.getDiastolicBp())
            .heartRate(vitalSigns.getHeartRate())
            .temperature(vitalSigns.getTemperature())
            .temperatureUnit(vitalSigns.getTemperatureUnit() != null ? vitalSigns.getTemperatureUnit().name() : null)
            .oxygenSaturation(vitalSigns.getOxygenSaturation())
            .weight(vitalSigns.getWeight())
            .weightUnit(vitalSigns.getWeightUnit() != null ? vitalSigns.getWeightUnit().name() : null)
            .bmi(vitalSigns.getBmi())
            .build();
    }
    
    private PatientSummaryResponse.NoteSummary mapToNoteSummary(ClinicalNote note) {
        return PatientSummaryResponse.NoteSummary.builder()
            .noteId(note.getNoteId())
            .noteType(note.getNoteType().toString())
            .noteDate(note.getNoteDate())
            .noteTime(note.getNoteTime())
            .chiefComplaint(note.getChiefComplaint())
            .noteStatus(note.getNoteStatus().toString())
            .build();
    }
    
    private PatientSummaryResponse.ImmunizationSummary mapToImmunizationSummary(Immunization immunization) {
        return PatientSummaryResponse.ImmunizationSummary.builder()
            .immunizationId(immunization.getImmunizationId())
            .vaccineName(immunization.getVaccineName())
            .administrationDate(immunization.getAdministrationDate())
            .build();
    }
    
    private PatientSummaryResponse.MedicationSummary mapToMedicationSummary(Medication medication) {
        String dosage = "";
        if (medication.getDosageStrength() != null && medication.getDosageUnit() != null) {
            dosage = medication.getDosageStrength() + " " + medication.getDosageUnit();
        } else if (medication.getDosageStrength() != null) {
            dosage = medication.getDosageStrength().toString();
        }
        
        return PatientSummaryResponse.MedicationSummary.builder()
            .medicationId(medication.getMedicationId())
            .medicationName(medication.getMedicationName())
            .genericName(medication.getGenericName())
            .dosage(dosage)
            .route(medication.getRoute() != null ? medication.getRoute().toString() : "")
            .frequency(medication.getFrequency())
            .startDate(medication.getStartDate())
            .indication(medication.getIndication())
            .medicationStatus(medication.getMedicationStatus() != null ? medication.getMedicationStatus().toString() : "")
            .build();
    }
}
