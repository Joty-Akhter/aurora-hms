package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SlotTemplateRepository extends JpaRepository<SlotTemplate, UUID>, JpaSpecificationExecutor<SlotTemplate> {

    Page<SlotTemplate> findByResourceType(String resourceType, Pageable pageable);

    Page<SlotTemplate> findByBranchId(UUID branchId, Pageable pageable);

    Page<SlotTemplate> findByStatus(String status, Pageable pageable);
}
