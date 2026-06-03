package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardResponse;
import com.easyops.hospitalcard.api.dto.IssueCardRequest;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.api.dto.ReplaceCardRequest;
import com.easyops.hospitalcard.domain.card.CardService;
import com.easyops.hospitalcard.security.InternalServiceAuth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service-to-service card APIs for hospital-service patient identity issuance (bypasses user RBAC).
 */
@RestController
@RequestMapping("/api/hospital-card-management/internal/cards")
@RequiredArgsConstructor
public class CardInternalController {

    private final CardService cardService;
    private final InternalServiceAuth internalServiceAuth;

    @PostMapping
    public ResponseEntity<CardResponse> issue(
            @Valid @RequestBody IssueCardRequest request,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        internalServiceAuth.assertServiceKey(serviceKey);
        CardResponse created = cardService.issue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        internalServiceAuth.assertServiceKey(serviceKey);
        OffsetDateTime from = parseOffsetDateTime(issuedAtFrom);
        OffsetDateTime to = parseOffsetDateTime(issuedAtTo);
        return cardService.search(
                cardNumber, ownerReferenceId, ownerType, corporateId, cardProductId, status, from, to, page, size);
    }

    @PostMapping("/{id}/replace")
    public CardResponse replace(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ReplaceCardRequest request,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        internalServiceAuth.assertServiceKey(serviceKey);
        return cardService.replace(id, request != null ? request : new ReplaceCardRequest());
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid date format: " + value + ". Use ISO-8601 (e.g. 2024-01-01T00:00:00Z)");
        }
    }
}
