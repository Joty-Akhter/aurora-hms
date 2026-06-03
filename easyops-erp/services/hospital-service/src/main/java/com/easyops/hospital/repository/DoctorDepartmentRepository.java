package com.easyops.hospital.repository;

import com.easyops.hospital.entity.DoctorDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorDepartmentRepository extends JpaRepository<DoctorDepartment, UUID> {
    
    Optional<DoctorDepartment> findByDepartmentName(String departmentName);
    
    List<DoctorDepartment> findByStatus(DoctorDepartment.DepartmentStatus status);
    
    @Query("SELECT d FROM DoctorDepartment d WHERE " +
           "LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND d.status = :status")
    List<DoctorDepartment> searchDepartments(
            @Param("searchTerm") String searchTerm,
            @Param("status") DoctorDepartment.DepartmentStatus status);
    
    boolean existsByDepartmentName(String departmentName);
}
