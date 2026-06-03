package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.PharmacyCreditAccountRequest;
import com.easyops.hospitalpharmacy.dto.request.PharmacyCreditChargeRequest;
import com.easyops.hospitalpharmacy.dto.request.PharmacyPaymentRequest;
import com.easyops.hospitalpharmacy.dto.response.PharmacyCreditAccountResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyPaymentResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.PharmacyCreditAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/credit-accounts")
@RequiredArgsConstructor
@Slf4j
public class PharmacyCreditAccountController {

    private final PharmacyCreditAccountService creditAccountService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<PharmacyCreditAccountResponse> create(
            @Valid @RequestBody PharmacyCreditAccountRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creditAccountService.createAccount(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacyCreditAccountResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.ok(creditAccountService.getById(id));
    }

    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<PharmacyCreditAccountResponse> getByPatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.ok(creditAccountService.getByPatient(patientId));
    }

    @GetMapping
    public ResponseEntity<List<PharmacyCreditAccountResponse>> listActive(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.ok(creditAccountService.listActive());
    }

    @PostMapping("/{id}/charges")
    public ResponseEntity<PharmacyCreditAccountResponse> chargeAccount(
            @PathVariable UUID id,
            @Valid @RequestBody PharmacyCreditChargeRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.ok(creditAccountService.chargeAccount(id, request.getAmount()));
    }

    @PostMapping("/payments")
    public ResponseEntity<PharmacyPaymentResponse> recordPayment(
            @Valid @RequestBody PharmacyPaymentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creditAccountService.recordPayment(request, actor));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PharmacyPaymentResponse>> listPayments(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireCreditManage(actor, organizationId);
        return ResponseEntity.ok(creditAccountService.listPayments(id));
    }
}
