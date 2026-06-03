package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientInsuranceRepository extends JpaRepository<PatientInsurance, UUID> {
    
    List<PatientInsurance> findByPatientPatientId(UUID patientId);
    
    Optional<PatientInsurance> findByPatientPatientIdAndInsuranceType(
        UUID patientId, PatientInsurance.InsuranceType insuranceType);
    
    List<PatientInsurance> findByPatientPatientIdOrderByInsuranceType(UUID patientId);
    
    void deleteByPatientPatientId(UUID patientId);
}
