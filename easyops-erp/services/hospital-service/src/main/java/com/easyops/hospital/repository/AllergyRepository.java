package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, UUID> {
    
    List<Allergy> findByPatientPatientId(UUID patientId);
    
    List<Allergy> findByPatientPatientIdAndStatus(UUID patientId, Allergy.Status status);
    
    List<Allergy> findByPatientPatientIdAndAllergenType(
        UUID patientId, Allergy.AllergenType allergenType);
    
    List<Allergy> findByPatientPatientIdAndSeverity(
        UUID patientId, Allergy.Severity severity);
    
    @Query("SELECT a FROM Allergy a WHERE a.patient.patientId = :patientId " +
           "AND a.status = 'ACTIVE'")
    List<Allergy> findActiveAllergiesByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT a FROM Allergy a WHERE a.patient.patientId = :patientId " +
           "AND LOWER(a.allergenName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Allergy> searchAllergiesByPatientAndName(
        @Param("patientId") UUID patientId, 
        @Param("searchTerm") String searchTerm);
    
    Optional<Allergy> findByPatientPatientIdAndAllergenNameIgnoreCase(
        UUID patientId, String allergenName);
    
    void deleteByPatientPatientId(UUID patientId);
}
