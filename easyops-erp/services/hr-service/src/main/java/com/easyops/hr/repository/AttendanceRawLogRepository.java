package com.easyops.hr.repository;

import com.easyops.hr.entity.AttendanceRawLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRawLogRepository extends JpaRepository<AttendanceRawLog, UUID> {

    List<AttendanceRawLog> findByOrganizationIdOrderByPunchTimeAsc(UUID organizationId);

    @Query("SELECT r FROM AttendanceRawLog r WHERE r.organizationId = :orgId " +
           "AND r.punchTime >= :from AND r.punchTime < :to " +
           "ORDER BY r.employeeId ASC, r.punchTime ASC")
    List<AttendanceRawLog> findByOrgAndPunchTimeRange(
            @Param("orgId") UUID organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Unprocessed punches in a time window — used by the processing pipeline. */
    @Query("SELECT r FROM AttendanceRawLog r WHERE r.organizationId = :orgId " +
           "AND r.processed = FALSE " +
           "AND r.punchTime >= :from AND r.punchTime < :to " +
           "ORDER BY r.employeeId ASC, r.punchTime ASC")
    List<AttendanceRawLog> findUnprocessedInRange(
            @Param("orgId") UUID organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<AttendanceRawLog> findByOrganizationIdAndPunchTimeBetweenOrderByPunchTimeAsc(
            UUID organizationId, LocalDateTime from, LocalDateTime to);
}
