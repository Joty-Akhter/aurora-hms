package com.easyops.hospitalscheduling.domain.waitlist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID>, JpaSpecificationExecutor<WaitlistEntry> {

    @Query("SELECT w FROM WaitlistEntry w WHERE w.resourceId = :resourceId AND w.status = :status ORDER BY w.priority DESC, w.createdAt ASC")
    List<WaitlistEntry> findByResourceIdAndStatusOrderByPriorityDescCreatedAtAsc(
            @Param("resourceId") UUID resourceId,
            @Param("status") String status);

    Page<WaitlistEntry> findByPatientId(UUID patientId, Pageable pageable);
}
