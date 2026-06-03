package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ClinicalChartItemRequest;
import com.easyops.hospital.dto.response.ClinicalChartItemPageResponse;
import com.easyops.hospital.dto.response.ClinicalChartItemResponse;
import com.easyops.hospital.service.ClinicalChartItemService;
import com.easyops.hospital.service.RbacPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinical-chart-items")
@RequiredArgsConstructor
@Tag(name = "Clinical Chart", description = "Legacy clinical chart / charge catalog (seeded master data)")
public class ClinicalChartItemController {

    private final ClinicalChartItemService clinicalChartItemService;
    private final RbacPermissionService rbacPermissionService;

    @GetMapping
    @Operation(summary = "Search clinical chart catalog",
               description = "Paginated browse of active legacy clinical chart rows (status = active). "
                   + "Optional search matches description, P-code, department, sub-department, sub-sub-department, and report group. "
                   + "Set investigationsOnly=true to restrict to SubDeptName Diagnostic, Radiology, or LabTest (same slice as EP autosuggest).")
    public ResponseEntity<ClinicalChartItemPageResponse> search(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "false") boolean investigationsOnly
    ) {
        rbacPermissionService.requireClinicalChartCatalogAccess(userId, organizationId);
        return ResponseEntity.ok(clinicalChartItemService.searchCatalog(searchTerm, page, size, investigationsOnly));
    }

    @GetMapping("/investigations/autocomplete")
    @Operation(summary = "Investigation/test autosuggest",
               description = "Returns distinct descriptions from rows whose SubDeptName is Diagnostic, Radiology, or LabTest (case-insensitive), "
                   + "status active — for EP prescriptions and templates. "
                   + "Search matches description, P-code, department, sub-department name, sub-sub-department, and report group.")
    public ResponseEntity<List<String>> investigationsAutocomplete(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "40") int limit
    ) {
        rbacPermissionService.requireClinicalChartInvestigationsAutocomplete(userId, organizationId);
        return ResponseEntity.ok(clinicalChartItemService.autocompleteInvestigations(q, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinical chart row by ID",
               description = "Returns a single clinical chart row by its UUID.")
    public ResponseEntity<ClinicalChartItemResponse> getById(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @PathVariable("id") UUID clinicalChartItemId
    ) {
        rbacPermissionService.requireClinicalChartCatalogAccess(userId, organizationId);
        return ResponseEntity.ok(clinicalChartItemService.getById(clinicalChartItemId));
    }

    @PostMapping
    @Operation(summary = "Create clinical chart row",
               description = "Creates a new clinical chart master row. If legacyRowId is omitted, the next available legacy row ID is assigned.")
    public ResponseEntity<ClinicalChartItemResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestBody ClinicalChartItemRequest request
    ) {
        rbacPermissionService.requireClinicalChartCatalogAccess(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalChartItemService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update clinical chart row",
               description = "Updates an existing clinical chart master row.")
    public ResponseEntity<ClinicalChartItemResponse> update(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @PathVariable("id") UUID clinicalChartItemId,
            @RequestBody ClinicalChartItemRequest request
    ) {
        rbacPermissionService.requireClinicalChartCatalogAccess(userId, organizationId);
        try {
            return ResponseEntity.ok(clinicalChartItemService.update(clinicalChartItemId, request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
