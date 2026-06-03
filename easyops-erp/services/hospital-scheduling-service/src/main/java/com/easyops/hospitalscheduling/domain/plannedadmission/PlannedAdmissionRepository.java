package com.easyops.hospitalscheduling.domain.plannedadmission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlannedAdmissionRepository extends JpaRepository<PlannedAdmission, UUID>, JpaSpecificationExecutor<PlannedAdmission> {

    List<PlannedAdmission> findByPatientId(UUID patientId);

    List<PlannedAdmission> findByPreferredDateBetween(LocalDate start, LocalDate end);

    List<PlannedAdmission> findByStatus(String status);

    @Query("SELECT p FROM PlannedAdmission p WHERE p.preferredDate BETWEEN :fromDate AND :toDate AND p.status NOT IN ('CANCELLED', 'EXPIRED')")
    List<PlannedAdmission> findByPreferredDateBetweenAndActive(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
