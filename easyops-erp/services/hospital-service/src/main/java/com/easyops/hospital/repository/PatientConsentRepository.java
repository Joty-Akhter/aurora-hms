package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {
    
    List<PatientConsent> findByPatientPatientId(UUID patientId);
    
    Optional<PatientConsent> findByPatientPatientIdAndConsentType(
        UUID patientId, PatientConsent.ConsentType consentType);
    
    List<PatientConsent> findByPatientPatientIdAndConsentStatus(
        UUID patientId, PatientConsent.ConsentStatus consentStatus);
    
    void deleteByPatientPatientId(UUID patientId);
}
