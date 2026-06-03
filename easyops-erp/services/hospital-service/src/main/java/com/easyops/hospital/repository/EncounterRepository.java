package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Encounter;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByPatientPatientIdOrderByStartDateDescStartTimeDesc(UUID patientId);
    
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByPatientPatientIdAndStatus(UUID patientId, Encounter.EncounterStatus status);
    
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdOrderByStartDateDescStartTimeDesc(UUID organizationId);

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdAndStatus(UUID organizationId, Encounter.EncounterStatus status);

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdAndEncounterType(UUID organizationId, Encounter.EncounterType encounterType);
    
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByPatientPatientIdAndStartDateBetween(
        UUID patientId, LocalDate startDate, LocalDate endDate);
    
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdAndStartDateBetween(
        UUID organizationId, LocalDate startDate, LocalDate endDate);
    
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByAttendingPhysicianId(UUID attendingPhysicianId);

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByLocationId(UUID locationId);

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByDepartmentId(UUID departmentId);

    @EntityGraph(attributePaths = {"patient"})
    Optional<Encounter> findByEncounterNumber(String encounterNumber);

    /** Single-encounter fetch with patient (replaces plain {@link #findById} for read/mapping paths). */
    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT e FROM Encounter e WHERE e.encounterId = :encounterId")
    Optional<Encounter> findWithPatientById(@Param("encounterId") UUID encounterId);
    
    boolean existsByEncounterNumber(String encounterNumber);
    
    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT e FROM Encounter e WHERE e.patient.patientId = :patientId " +
           "AND e.status IN ('IN_PROGRESS', 'ARRIVED', 'ADMITTED') " +
           "ORDER BY e.startDate DESC, e.startTime DESC")
    List<Encounter> findActiveEncountersByPatient(@Param("patientId") UUID patientId);

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT e FROM Encounter e WHERE e.organizationId = :organizationId " +
           "AND e.status IN ('IN_PROGRESS', 'ARRIVED', 'ADMITTED') " +
           "ORDER BY e.startDate DESC, e.startTime DESC")
    List<Encounter> findActiveEncountersByOrganization(@Param("organizationId") UUID organizationId);

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT e FROM Encounter e WHERE e.patient.patientId = :patientId " +
           "AND e.encounterType = 'HOSPITAL_ADMISSION' " +
           "AND e.status = 'ADMITTED' " +
           "ORDER BY e.admissionDate DESC, e.admissionTime DESC")
    List<Encounter> findActiveAdmissionsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT COUNT(e) FROM Encounter e WHERE e.organizationId = :organizationId " +
           "AND e.startDate = :date")
    Long countEncountersByDate(@Param("organizationId") UUID organizationId, 
                               @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(e) FROM Encounter e WHERE e.organizationId = :organizationId " +
           "AND e.status IN ('IN_PROGRESS', 'ARRIVED', 'ADMITTED')")
    Long countActiveEncounters(@Param("organizationId") UUID organizationId);

    /** Active inpatient-style encounters for IPD prescribing list (EP-1 / EP-11). */
    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdAndEncounterTypeInAndStatusInOrderByStartDateDescStartTimeDesc(
            UUID organizationId,
            Collection<Encounter.EncounterType> encounterTypes,
            Collection<Encounter.EncounterStatus> statuses);

    @EntityGraph(attributePaths = {"patient"})
    List<Encounter> findByOrganizationIdAndEncounterTypeInAndStatusInAndAttendingPhysicianIdOrderByStartDateDescStartTimeDesc(
            UUID organizationId,
            Collection<Encounter.EncounterType> encounterTypes,
            Collection<Encounter.EncounterStatus> statuses,
            UUID attendingPhysicianId);
}
