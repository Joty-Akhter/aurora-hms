package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SchedulingResourceRepository extends JpaRepository<SchedulingResource, UUID>, JpaSpecificationExecutor<SchedulingResource> {

    Optional<SchedulingResource> findByResourceTypeAndExternalReferenceId(String resourceType, String externalReferenceId);

    Page<SchedulingResource> findByBranchId(UUID branchId, Pageable pageable);

    Page<SchedulingResource> findByResourceTypeAndStatus(String resourceType, String status, Pageable pageable);
}
