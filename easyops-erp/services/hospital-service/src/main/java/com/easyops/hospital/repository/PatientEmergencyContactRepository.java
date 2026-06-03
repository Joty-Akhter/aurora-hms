package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientEmergencyContactRepository extends JpaRepository<PatientEmergencyContact, UUID> {
    
    List<PatientEmergencyContact> findByPatientPatientId(UUID patientId);
    
    Optional<PatientEmergencyContact> findByPatientPatientIdAndIsPrimary(UUID patientId, Boolean isPrimary);
    
    void deleteByPatientPatientId(UUID patientId);
}
