package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {

    List<WorkingHours> findByResourceIdOrderByDayOfWeekAscStartTimeAsc(UUID resourceId);

    Optional<WorkingHours> findByResourceIdAndDayOfWeek(UUID resourceId, Short dayOfWeek);

    @Modifying
    @Query("DELETE FROM WorkingHours w WHERE w.resourceId = :resourceId")
    void deleteByResourceId(@Param("resourceId") UUID resourceId);
}
