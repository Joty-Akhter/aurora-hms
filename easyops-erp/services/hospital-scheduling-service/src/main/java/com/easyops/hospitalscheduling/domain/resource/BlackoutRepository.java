package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BlackoutRepository extends JpaRepository<Blackout, UUID>, JpaSpecificationExecutor<Blackout> {

    List<Blackout> findByResourceIdAndBlackoutDateBetween(UUID resourceId, LocalDate from, LocalDate to);

    List<Blackout> findByBranchIdAndBlackoutDateBetween(UUID branchId, LocalDate from, LocalDate to);
}
