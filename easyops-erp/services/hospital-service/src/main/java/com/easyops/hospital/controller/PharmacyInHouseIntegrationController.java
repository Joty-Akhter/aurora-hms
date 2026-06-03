package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.AllergyCheckRequest;
import com.easyops.hospital.dto.request.DispenseClinicalScreenRequest;
import com.easyops.hospital.dto.request.DrugInteractionCheckRequest;
import com.easyops.hospital.dto.request.InHouseDispenseFillRequest;
import com.easyops.hospital.dto.response.DispenseClinicalScreenResponse;
import com.easyops.hospital.dto.response.InHouseDispenseFillResponse;
import com.easyops.hospital.service.EPrescribingService;
import com.easyops.hospital.service.PrescriptionService;
import com.easyops.hospital.service.RbacPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal integration from {@code hospital-pharmacy-service} when a linked prescription is dispensed in-house.
 */
@RestController
@RequestMapping("/api/integrations/pharmacy")
@RequiredArgsConstructor
@Tag(name = "Pharmacy integration (in-house)", description = "EHR sync when internal pharmacy completes dispensing")
public class PharmacyInHouseIntegrationController {

    private final EPrescribingService eprescribingService;
    private final PrescriptionService prescriptionService;
    private final RbacPermissionService rbacPermissionService;

    @PostMapping("/in-house-dispense-fill")
    @Operation(summary = "Apply fill status from in-house pharmacy dispensing (WS-B)")
    public ResponseEntity<InHouseDispenseFillResponse> applyInHouseDispenseFill(
            @Valid @RequestBody InHouseDispenseFillRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePharmacyDispenseOrHospitalManage(userId, organizationId);
        return ResponseEntity.ok(eprescribingService.applyInHouseDispenseFill(request));
    }

    /**
     * Phase P4 — WS-I: combined interaction + allergy screening for a SKU about to be dispensed.
     * Pharmacy service applies org policy (severity tiers, cross-reactivity handling) after receiving this payload.
     */
    @PostMapping("/dispense-clinical-screen")
    @Operation(summary = "Dispense-time clinical screening (interactions + allergies)")
    public ResponseEntity<DispenseClinicalScreenResponse> dispenseClinicalScreen(
            @Valid @RequestBody DispenseClinicalScreenRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePharmacyDispenseOrHospitalManage(userId, organizationId);
        DrugInteractionCheckRequest di = DrugInteractionCheckRequest.builder()
                .medicationCode(request.getMedicationCode())
                .medicationName(request.getMedicationName())
                .build();
        var interactions = prescriptionService.checkDrugInteractions(di, request.getPatientId());
        var allergies = prescriptionService.checkAllergies(AllergyCheckRequest.builder()
                .patientId(request.getPatientId())
                .medicationCode(request.getMedicationCode())
                .medicationName(request.getMedicationName())
                .build());
        return ResponseEntity.ok(DispenseClinicalScreenResponse.builder()
                .interactions(interactions)
                .allergies(allergies)
                .build());
    }
}
