package com.easyops.organization.controller;

import com.easyops.organization.entity.OrganizationAppData;
import com.easyops.organization.security.OrganizationRbacService;
import com.easyops.organization.security.RbacRequestHeaders;
import com.easyops.organization.service.OrganizationAppDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Organization App Data Controller
 * Read-only access to organization-scoped master data (UOM, gender, etc.)
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/app-data")
@RequiredArgsConstructor
@Slf4j
public class OrganizationAppDataController {

    private final OrganizationAppDataService appDataService;
    private final OrganizationRbacService organizationRbac;

    @GetMapping
    public ResponseEntity<List<OrganizationAppData>> getAppData(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam("type") String type,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        log.debug("Request to fetch app data for orgId={}, type={}", organizationId, type);
        List<OrganizationAppData> data = appDataService.getOrganizationAppData(organizationId, type);
        return ResponseEntity.ok(data);
    }
}
