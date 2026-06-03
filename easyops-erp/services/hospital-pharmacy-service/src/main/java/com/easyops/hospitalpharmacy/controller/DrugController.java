package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.DrugRequest;
import com.easyops.hospitalpharmacy.dto.request.FormularyRuleRequest;
import com.easyops.hospitalpharmacy.dto.response.DrugResponse;
import com.easyops.hospitalpharmacy.dto.response.FormularyRuleResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.DrugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/drugs")
@RequiredArgsConstructor
@Slf4j
public class DrugController {

    private final DrugService drugService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<DrugResponse> create(
            @Valid @RequestBody DrugRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Creating drug: {}", request.getGenericName());
        DrugResponse response = drugService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/formulary-alternatives")
    public ResponseEntity<List<DrugResponse>> listFormularyAlternatives(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(drugService.listFormularyAlternativeDrugs(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DrugResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(drugService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DrugResponse>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
            @RequestParam(value = "manufacturerId", required = false) UUID manufacturerId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(drugService.search(name, activeOnly, manufacturerId));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DrugResponse>> searchForPrescription(
            @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        Page<DrugResponse> result = drugService.searchForPrescription(query, page, size);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DrugResponse> update(
            @PathVariable UUID id,
            @RequestBody DrugRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Updating drug: {}", id);
        return ResponseEntity.ok(drugService.update(id, request));
    }

    @PostMapping("/{id}/formulary-rules")
    public ResponseEntity<List<FormularyRuleResponse>> upsertFormularyRules(
            @PathVariable UUID id,
            @RequestBody List<FormularyRuleRequest> requests,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Upserting formulary rules for drug: {}", id);
        List<FormularyRuleResponse> responses = drugService.upsertFormularyRules(id, requests);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/formulary-rules")
    public ResponseEntity<List<FormularyRuleResponse>> getFormularyRules(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(drugService.getFormularyRules(id));
    }
}
