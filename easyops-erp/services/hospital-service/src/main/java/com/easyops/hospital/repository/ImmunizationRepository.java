package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImmunizationRepository extends JpaRepository<Immunization, UUID> {
    
    List<Immunization> findByPatientPatientId(UUID patientId);
    
    List<Immunization> findByPatientPatientIdOrderByAdministrationDateDesc(UUID patientId);
    
    @Query("SELECT i FROM Immunization i WHERE i.patient.patientId = :patientId " +
           "AND i.vaccineName = :vaccineName ORDER BY i.administrationDate DESC")
    List<Immunization> findByPatientAndVaccineName(
        @Param("patientId") UUID patientId, 
        @Param("vaccineName") String vaccineName);
    
    @Query("SELECT i FROM Immunization i WHERE i.patient.patientId = :patientId " +
           "AND i.cvxCode = :cvxCode ORDER BY i.administrationDate DESC")
    List<Immunization> findByPatientAndCvxCode(
        @Param("patientId") UUID patientId, 
        @Param("cvxCode") String cvxCode);
    
    Optional<Immunization> findFirstByPatientPatientIdAndVaccineNameOrderByAdministrationDateDesc(
        UUID patientId, String vaccineName);
    
    void deleteByPatientPatientId(UUID patientId);
}
