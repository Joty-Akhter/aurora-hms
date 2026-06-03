package com.easyops.hospitalscheduling.domain.roster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RosterBlockRepository extends JpaRepository<RosterBlock, UUID> {

    List<RosterBlock> findByResourceIdOrderByStartTimeAsc(UUID resourceId);

    @Query("""
        SELECT b FROM RosterBlock b WHERE b.resourceId = :resourceId
        AND b.startTime < :slotEnd AND b.endTime > :slotStart
        AND b.type IN ('UNAVAILABLE', 'SUBSTITUTE')
        """)
    List<RosterBlock> findOverlappingUnavailableOrSubstitute(
            @Param("resourceId") UUID resourceId,
            @Param("slotStart") OffsetDateTime slotStart,
            @Param("slotEnd") OffsetDateTime slotEnd);

    @Query("""
        SELECT b FROM RosterBlock b WHERE b.resourceId = :resourceId
        AND b.startTime < :toInclusive AND b.endTime > :fromInclusive
        ORDER BY b.startTime ASC
        """)
    List<RosterBlock> findByResourceIdAndTimeOverlap(
            @Param("resourceId") UUID resourceId,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toInclusive") OffsetDateTime toInclusive);
}
