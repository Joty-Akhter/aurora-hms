package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardResponse;
import com.easyops.hospitalcard.api.dto.CardTransactionResponse;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.domain.account.CardAccountService;
import com.easyops.hospitalcard.domain.card.CardService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Portal / self-service endpoints. Require identity from BFF via headers.
 * BFF resolves authenticated user and sets X-Owner-Reference-Id (and optionally X-Owner-Type).
 */
@RestController
@RequestMapping("/api/hospital-card-management/me")
@RequiredArgsConstructor
public class MeCardController {

    public static final String HEADER_OWNER_REFERENCE_ID = "X-Owner-Reference-Id";
    public static final String HEADER_OWNER_TYPE = "X-Owner-Type";

    private final CardService cardService;
    private final CardAccountService cardAccountService;
    private final HospitalCardRbacService hospitalCardRbac;

    @GetMapping("/cards")
    public List<CardResponse> getMyCards(
            @RequestHeader(value = HEADER_OWNER_REFERENCE_ID, required = false) String ownerReferenceId,
            @RequestHeader(value = HEADER_OWNER_TYPE, required = false) String ownerType,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        if (ownerReferenceId == null || ownerReferenceId.isBlank()) {
            throw new IllegalArgumentException("UNAUTHORIZED: " + HEADER_OWNER_REFERENCE_ID + " required");
        }
        return cardService.listCardsForOwner(ownerReferenceId.trim(), trimOrNull(ownerType));
    }

    @GetMapping("/cards/{id}/statement")
    public PagedResponse<CardTransactionResponse> getStatement(
            @PathVariable("id") UUID cardId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader(value = HEADER_OWNER_REFERENCE_ID, required = false) String ownerReferenceId,
            @RequestHeader(value = HEADER_OWNER_TYPE, required = false) String ownerType,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        if (ownerReferenceId == null || ownerReferenceId.isBlank()) {
            throw new IllegalArgumentException("UNAUTHORIZED: " + HEADER_OWNER_REFERENCE_ID + " required");
        }
        cardService.assertOwner(cardId, ownerReferenceId.trim(), trimOrNull(ownerType));
        OffsetDateTime fromDt = parseOffsetDateTime(from);
        OffsetDateTime toDt = parseOffsetDateTime(to);
        return cardAccountService.listTransactions(cardId, fromDt, toDt, null, null, page, size);
    }

    private static String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + value + ". Use ISO-8601 (e.g. 2024-01-01T00:00:00Z)");
        }
    }
}
