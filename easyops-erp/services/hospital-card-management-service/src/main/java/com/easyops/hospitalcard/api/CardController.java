package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.account.CardAccountService;
import com.easyops.hospitalcard.domain.card.CardService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-card-management/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardAccountService cardAccountService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping
    public ResponseEntity<CardResponse> issue(
            @Valid @RequestBody IssueCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardResponse created = cardService.issue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public CardDetailResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return cardService.getById(id);
    }

    @GetMapping("/search")
    public PagedResponse<CardResponse> search(
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "ownerReferenceId", required = false) String ownerReferenceId,
            @RequestParam(value = "ownerType", required = false) String ownerType,
            @RequestParam(value = "corporateId", required = false) UUID corporateId,
            @RequestParam(value = "cardProductId", required = false) UUID cardProductId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "issuedAtFrom", required = false) String issuedAtFrom,
            @RequestParam(value = "issuedAtTo", required = false) String issuedAtTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime from = parseOffsetDateTime(issuedAtFrom);
        OffsetDateTime to = parseOffsetDateTime(issuedAtTo);
        return cardService.search(cardNumber, ownerReferenceId, ownerType, corporateId, cardProductId, status, from, to, page, size);
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + value + ". Use ISO-8601 (e.g. 2024-01-01T00:00:00Z)");
        }
    }

    @PatchMapping("/{id}/status")
    public CardResponse updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCardStatusRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.updateStatus(id, request);
    }

    @PostMapping("/{id}/replace")
    public CardResponse replace(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ReplaceCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.replace(id, request != null ? request : new ReplaceCardRequest());
    }

    @GetMapping("/{id}/balance")
    public CardBalanceResponse getBalance(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return cardAccountService.getBalance(id);
    }

    @GetMapping("/{id}/transactions")
    public PagedResponse<CardTransactionResponse> getTransactions(
            @PathVariable("id") UUID id,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime fromDt = parseOffsetDateTime(from);
        OffsetDateTime toDt = parseOffsetDateTime(to);
        return cardAccountService.listTransactions(id, fromDt, toDt, type, status, page, size);
    }

    @PostMapping("/{id}/topup")
    public ResponseEntity<CardTransactionResponse> topup(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TopupRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardTransactionResponse result = cardAccountService.topup(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<CardTransactionResponse> createAdjustment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateAdjustmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardTransactionResponse result = cardAccountService.createAdjustment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
