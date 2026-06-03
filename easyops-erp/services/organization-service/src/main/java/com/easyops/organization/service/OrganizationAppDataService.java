package com.easyops.organization.service;

import com.easyops.organization.entity.OrganizationAppData;
import com.easyops.organization.repository.OrganizationAppDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationAppDataService {

    private final OrganizationAppDataRepository appDataRepository;

    public List<OrganizationAppData> getOrganizationAppData(UUID organizationId, String type) {
        log.debug("Fetching app data for organization: {}, type: {}", organizationId, type);
        return appDataRepository
                .findByOrganizationIdAndTypeAndIsActiveTrueOrderByDisplayOrderAscNameAsc(organizationId, type);
    }
}

