package com.easyops.hospitalscheduling.domain.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    @Query("""
        SELECT r FROM Reservation r WHERE r.resourceId = :resourceId
        AND r.slotStart < :slotEnd AND r.slotEnd > :slotStart
        AND r.status NOT IN ('CANCELLED', 'NO_SHOW')
        AND (:excludeReservationId IS NULL OR r.id <> :excludeReservationId)
        """)
    List<Reservation> findOverlapping(@Param("resourceId") UUID resourceId,
                                      @Param("slotStart") OffsetDateTime slotStart,
                                      @Param("slotEnd") OffsetDateTime slotEnd,
                                      @Param("excludeReservationId") UUID excludeReservationId);

    Page<Reservation> findByPatientId(UUID patientId, Pageable pageable);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    List<Reservation> findByReferenceTypeAndReferenceIdAndSlotStartAndSlotEnd(
            String referenceType, String referenceId, OffsetDateTime slotStart, OffsetDateTime slotEnd);

    List<Reservation> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    @Query(value = """
        SELECT resource_id, CAST((slot_start AT TIME ZONE 'UTC') AS DATE) as d, COUNT(*) as cnt
        FROM hospital_scheduling.scheduling_reservations
        WHERE slot_start >= :from AND slot_start < :to
        AND status NOT IN ('CANCELLED', 'NO_SHOW')
        AND (:resourceId IS NULL OR resource_id = :resourceId)
        GROUP BY resource_id, CAST((slot_start AT TIME ZONE 'UTC') AS DATE)
        ORDER BY resource_id, d
        """, nativeQuery = true)
    List<Object[]> countSlotUsedByResourceAndDate(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("resourceId") UUID resourceId);
}
