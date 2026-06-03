package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.DoctorDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    
    Optional<Doctor> findByDoctorCode(String doctorCode);
    
    List<Doctor> findByDepartment(DoctorDepartment department);
    
    List<Doctor> findByDepartment_DepartmentId(UUID departmentId);
    
    List<Doctor> findByIsActiveAndAvailabilityStatus(Boolean isActive, Doctor.AvailabilityStatus availabilityStatus);
    
    List<Doctor> findByDoctorType(Doctor.DoctorType doctorType);
    
    List<Doctor> findByIndoorOutdoorStatus(Doctor.IndoorOutdoorStatus indoorOutdoorStatus);
    
    Optional<Doctor> findByEmail(String email);
    
    Optional<Doctor> findByBmdcRegistrationNumber(String bmdcRegistrationNumber);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.doctorCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.speciality) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Doctor> searchDoctors(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "d.isActive = true AND " +
           "d.availabilityStatus = 'AVAILABLE' AND " +
           "d.prescriptionStatus = 'ACTIVE'")
    List<Doctor> findActiveDoctorsForPrescription();
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "d.department.departmentId = :departmentId AND " +
           "d.isActive = true AND " +
           "d.availabilityStatus = 'AVAILABLE'")
    List<Doctor> findActiveDoctorsByDepartment(@Param("departmentId") UUID departmentId);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.speciality) LIKE LOWER(CONCAT('%', :speciality, '%')) AND " +
           "d.isActive = true")
    List<Doctor> findBySpeciality(@Param("speciality") String speciality);
    
    @Query("SELECT d FROM Doctor d WHERE d.isActive = true")
    List<Doctor> findAllActiveDoctors();

    @Query(value = "SELECT MAX(CAST(SUBSTRING(doctor_code FROM :prefixLength + 1) AS INTEGER)) " +
           "FROM hospital.doctors WHERE doctor_code LIKE :prefix || '%'", nativeQuery = true)
    Long findMaxDoctorCodeSequence(@Param("prefix") String prefix, @Param("prefixLength") int prefixLength);
    
    boolean existsByDoctorCode(String doctorCode);
    
    boolean existsByEmail(String email);
    
    boolean existsByBmdcRegistrationNumber(String bmdcRegistrationNumber);

    /** Portal user ↔ doctor row (082-doctor-linked-user-id). */
    Optional<Doctor> findByLinkedUserId(UUID linkedUserId);

    List<Doctor> findAllByDoctorIdIn(Collection<UUID> doctorIds);

    @Query("SELECT d FROM Doctor d WHERE d.isActive = true AND d.appointmentsFromWeb = true ORDER BY d.doctorName")
    List<Doctor> findWebBookableDoctors();
}
