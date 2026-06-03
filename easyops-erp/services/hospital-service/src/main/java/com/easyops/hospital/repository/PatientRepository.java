package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    Optional<Patient> findByMrn(String mrn);
    
    Optional<Patient> findByIdNo(String idNo);
    
    List<Patient> findByPrimaryPhone(String phone);
    
    List<Patient> findByPrimaryEmail(String email);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "p.primaryPhone LIKE CONCAT('%', :searchTerm, '%') OR " +
           "p.secondaryPhone LIKE CONCAT('%', :searchTerm, '%')")
    List<Patient> searchPatients(@Param("searchTerm") String searchTerm);

    /**
     * Match phone fields by digits-only substring (e.g. +880 1712-345678 finds 01712345678).
     */
    @Query(
            value = "SELECT * FROM ehr.patients p WHERE p.patient_status = 'ACTIVE' AND ("
                    + "regexp_replace(COALESCE(p.primary_phone, ''), '[^0-9]', '', 'g') LIKE CONCAT('%', :phoneDigits, '%') "
                    + "OR regexp_replace(COALESCE(p.secondary_phone, ''), '[^0-9]', '', 'g') LIKE CONCAT('%', :phoneDigits, '%'))",
            nativeQuery = true)
    List<Patient> searchPatientsByNormalizedPhone(@Param("phoneDigits") String phoneDigits);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.fullName) = LOWER(:fullName) AND " +
           "p.dateOfBirth = :dateOfBirth")
    List<Patient> findPotentialDuplicates(
        @Param("fullName") String fullName,
        @Param("dateOfBirth") LocalDate dateOfBirth);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "p.idNo = :idNo AND p.idNo IS NOT NULL")
    List<Patient> findByIdNoForDuplicateCheck(@Param("idNo") String idNo);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "p.primaryPhone = :phone OR p.secondaryPhone = :phone")
    List<Patient> findByPhoneForDuplicateCheck(@Param("phone") String phone);

    /**
     * Match primary or secondary phone by digits-only form (ignores formatting), active patients only.
     */
    @Query(
            value = "SELECT patient_id FROM ehr.patients WHERE patient_status = 'ACTIVE' AND ("
                    + "regexp_replace(COALESCE(primary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits "
                    + "OR regexp_replace(COALESCE(secondary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits)",
            nativeQuery = true)
    List<UUID> findActivePatientIdsByNormalizedPhone(@Param("phoneDigits") String phoneDigits);

    @Query(
            value = "SELECT patient_id FROM ehr.patients WHERE patient_status = 'ACTIVE' AND ("
                    + "regexp_replace(COALESCE(primary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits "
                    + "OR regexp_replace(COALESCE(secondary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits) "
                    + "AND patient_id <> CAST(:excludePatientId AS uuid)",
            nativeQuery = true)
    List<UUID> findActivePatientIdsByNormalizedPhoneExcluding(
            @Param("phoneDigits") String phoneDigits,
            @Param("excludePatientId") UUID excludePatientId);

    @Query(
            value = "SELECT patient_id FROM ehr.patients WHERE patient_status = 'ACTIVE' "
                    + "AND organization_id = CAST(:organizationId AS uuid) AND ("
                    + "regexp_replace(COALESCE(primary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits "
                    + "OR regexp_replace(COALESCE(secondary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits)",
            nativeQuery = true)
    List<UUID> findActivePatientIdsByNormalizedPhoneForOrganization(
            @Param("phoneDigits") String phoneDigits,
            @Param("organizationId") UUID organizationId);

    @Query(
            value = "SELECT patient_id FROM ehr.patients WHERE patient_status = 'ACTIVE' "
                    + "AND organization_id = CAST(:organizationId AS uuid) AND ("
                    + "regexp_replace(COALESCE(primary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits "
                    + "OR regexp_replace(COALESCE(secondary_phone, ''), '[^0-9]', '', 'g') = :phoneDigits) "
                    + "AND patient_id <> CAST(:excludePatientId AS uuid)",
            nativeQuery = true)
    List<UUID> findActivePatientIdsByNormalizedPhoneForOrganizationExcluding(
            @Param("phoneDigits") String phoneDigits,
            @Param("organizationId") UUID organizationId,
            @Param("excludePatientId") UUID excludePatientId);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "p.primaryEmail = :email OR p.secondaryEmail = :email")
    List<Patient> findByEmailForDuplicateCheck(@Param("email") String email);
    
    List<Patient> findByPatientStatus(Patient.PatientStatus status);
    
    List<Patient> findByOrganizationId(UUID organizationId);
    
    @Query(value = "SELECT MAX(CAST(SUBSTRING(mrn FROM :prefixLength + 1) AS INTEGER)) FROM ehr.patients WHERE mrn LIKE :prefix || '%'", nativeQuery = true)
    Long findMaxMrnSequence(@Param("prefix") String prefix, @Param("prefixLength") int prefixLength);
    
    boolean existsByMrn(String mrn);
}
