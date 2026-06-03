package com.easyops.organization.repository;

import com.easyops.organization.entity.OrganizationAppData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationAppDataRepository extends JpaRepository<OrganizationAppData, UUID> {

    List<OrganizationAppData> findByOrganizationIdAndTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(
            UUID organizationId,
            String type
    );
}

