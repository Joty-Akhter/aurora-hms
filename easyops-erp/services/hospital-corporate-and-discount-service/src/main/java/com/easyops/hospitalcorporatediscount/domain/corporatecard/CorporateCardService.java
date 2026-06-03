package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateCardResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CorporateCardValidationResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCorporateCardRequest;
import com.easyops.hospitalcorporatediscount.api.dto.PagedResponse;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContract;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractRepository;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClient;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CorporateCardService {
    private final CorporateCardRepository corporateCardRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final CorporateContractRepository corporateContractRepository;
    private final HospitalCardRegistryClient hospitalCardRegistryClient;

    public CorporateCardService(
            CorporateCardRepository corporateCardRepository,
            CorporateClientRepository corporateClientRepository,
            CorporateContractRepository corporateContractRepository,
            HospitalCardRegistryClient hospitalCardRegistryClient) {
        this.corporateCardRepository = corporateCardRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.corporateContractRepository = corporateContractRepository;
        this.hospitalCardRegistryClient = hospitalCardRegistryClient;
    }

    @Transactional
    public CorporateCardResponse issue(CreateCorporateCardRequest request, UUID actorUserId, UUID organizationId) {
        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new NoSuchElementException("Corporate client not found: " + request.getCorporateClientId()));
        if (!"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new IllegalStateException("Cannot issue card for non-active corporate client");
        }
        validateClientWindow(client);
        validateContract(request.getCorporateClientId(), request.getContractId());
        ensureNoDuplicateActiveHolderCard(request.getCorporateClientId(), request.getHolderIdentifier(), request.getCardType());

        HospitalCardRegistryClient.RegistryCardResult issued = hospitalCardRegistryClient.issue(
                actorUserId,
                organizationId,
                request.getCardProductId(),
                request.getCorporateClientId(),
                request.getHolderIdentifier().trim(),
                request.getCardNumber()
        );

        CorporateCard card = new CorporateCard();
        card.setCorporateClientId(request.getCorporateClientId());
        card.setContractId(request.getContractId());
        card.setHolderName(request.getHolderName().trim());
        card.setHolderIdentifier(request.getHolderIdentifier().trim());
        card.setCardType(request.getCardType().trim().toUpperCase());
        card.setCardProductId(request.getCardProductId());
        card.setCardId(issued.cardId());
        card.setCardNumber(issued.cardNumber());
        card.setStatus(toCorporateStatus(issued.status()));
        LocalDate validFrom = deriveValidFrom(client, request.getContractId());
        LocalDate validTo = deriveValidTo(client, request.getContractId());
        card.setValidFrom(validFrom);
        card.setValidTo(validTo);
        card.setCreatedBy(actorUserId);
        CorporateCard saved = corporateCardRepository.save(card);
        return toResponse(saved, "ISSUE");
    }

    @Transactional
    public CorporateCardResponse reissue(UUID corporateCardId, String reason, UUID actorUserId, UUID organizationId) {
        CorporateCard existing = corporateCardRepository.findById(corporateCardId)
                .orElseThrow(() -> new NoSuchElementException("Corporate card not found: " + corporateCardId));
        if (!isActive(existing.getStatus())) {
            throw new IllegalStateException("Only ACTIVE corporate cards can be reissued");
        }
        ensureNotExpired(existing);

        HospitalCardRegistryClient.RegistryCardResult replaced = hospitalCardRegistryClient.replace(
                actorUserId,
                organizationId,
                existing.getCardId(),
                reason
        );

        existing.setStatus("BLOCKED");

        CorporateCard replacement = new CorporateCard();
        replacement.setCorporateClientId(existing.getCorporateClientId());
        replacement.setContractId(existing.getContractId());
        replacement.setHolderName(existing.getHolderName());
        replacement.setHolderIdentifier(existing.getHolderIdentifier());
        replacement.setCardType(existing.getCardType());
        replacement.setCardProductId(existing.getCardProductId());
        replacement.setCardId(replaced.cardId());
        replacement.setCardNumber(replaced.cardNumber());
        replacement.setStatus(toCorporateStatus(replaced.status()));
        replacement.setValidFrom(existing.getValidFrom());
        replacement.setValidTo(existing.getValidTo());
        replacement.setCreatedBy(actorUserId);

        CorporateCard savedReplacement = corporateCardRepository.save(replacement);
        existing.setReplacedByCorporateCardId(savedReplacement.getId());
        corporateCardRepository.save(existing);
        return toResponse(savedReplacement, "REISSUE");
    }

    @Transactional(readOnly = true)
    public CorporateCardResponse reprint(UUID corporateCardId) {
        CorporateCard card = corporateCardRepository.findById(corporateCardId)
                .orElseThrow(() -> new NoSuchElementException("Corporate card not found: " + corporateCardId));
        if (!isActive(card.getStatus())) {
            throw new IllegalStateException("Reprint is allowed only for ACTIVE corporate cards");
        }
        ensureNotExpired(card);
        return toResponse(card, "REPRINT");
    }

    @Transactional(readOnly = true)
    public CorporateCardResponse getById(UUID corporateCardId) {
        CorporateCard card = corporateCardRepository.findById(corporateCardId)
                .orElseThrow(() -> new NoSuchElementException("Corporate card not found: " + corporateCardId));
        expireIfPastDue(card, null, null);
        return toResponse(card, "VIEW");
    }

    @Transactional(readOnly = true)
    public PagedResponse<CorporateCardResponse> list(UUID corporateClientId, String holderIdentifier, String status, int page, int size) {
        Specification<CorporateCard> spec = Specification.where(CorporateCardSpecifications.hasCorporateClientId(corporateClientId))
                .and(CorporateCardSpecifications.hasHolderIdentifier(holderIdentifier))
                .and(CorporateCardSpecifications.hasStatus(status));
        Page<CorporateCard> result = corporateCardRepository.findAll(spec, PageRequest.of(page, Math.max(1, size)));
        result.getContent().forEach(card -> expireIfPastDue(card, null, null));
        return new PagedResponse<>(
                result.map(c -> toResponse(c, "VIEW")).getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CorporateCardValidationResponse validateForBilling(String cardNumber, UUID actorUserId, UUID organizationId) {
        CorporateCardValidationResponse response = new CorporateCardValidationResponse();
        CorporateCard card = corporateCardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new NoSuchElementException("Corporate card not found for number: " + cardNumber));
        expireIfPastDue(card, actorUserId, organizationId);
        boolean policyActive = isPolicyActiveForCard(card);
        HospitalCardRegistryClient.RegistryCardResult registry = hospitalCardRegistryClient.findByCardNumber(actorUserId, organizationId, cardNumber);
        if (registry == null) {
            response.setValid(false);
            response.setMessage("Card is missing in hospital-card-management-service");
        } else if (!card.getCardId().equals(registry.cardId())) {
            response.setValid(false);
            response.setMessage("Card id mismatch between corporate and card registry");
        } else if (!canonicalComparableStatus(card.getStatus()).equals(canonicalComparableStatus(registry.status()))) {
            response.setValid(false);
            response.setMessage("Card status mismatch between corporate and card registry");
        } else if (!policyActive) {
            response.setValid(false);
            response.setMessage("Corporate policy is not active for this card");
        } else if (!isActive(card.getStatus())) {
            response.setValid(false);
            response.setMessage("Card is not active");
        } else {
            response.setValid(true);
            response.setMessage("Card is valid for billing");
        }
        response.setCorporateCardId(card.getId());
        response.setCardId(card.getCardId());
        response.setCardNumber(card.getCardNumber());
        response.setCorporateStatus(card.getStatus());
        response.setRegistryStatus(registry != null ? normalizeStatus(registry.status()) : null);
        response.setCorporateClientId(card.getCorporateClientId());
        response.setContractId(card.getContractId());
        response.setHolderIdentifier(card.getHolderIdentifier());
        response.setCardType(card.getCardType());
        return response;
    }

    private void validateContract(UUID corporateClientId, UUID contractId) {
        if (contractId == null) {
            return;
        }
        CorporateContract contract = corporateContractRepository.findById(contractId)
                .orElseThrow(() -> new NoSuchElementException("Corporate contract not found: " + contractId));
        if (!corporateClientId.equals(contract.getCorporateClientId())) {
            throw new IllegalArgumentException("Contract does not belong to provided corporate client");
        }
        LocalDate today = LocalDate.now();
        if (contract.getValidFrom() != null && contract.getValidFrom().isAfter(today)) {
            throw new IllegalStateException("Contract validity window has not started");
        }
        if (contract.getValidTo() != null && contract.getValidTo().isBefore(today)) {
            throw new IllegalStateException("Contract validity window has expired");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ISSUED";
        }
        return status.trim().toUpperCase();
    }

    private String toCorporateStatus(String sourceStatus) {
        String normalized = normalizeStatus(sourceStatus);
        if ("ISSUED".equals(normalized)) {
            return "ACTIVE";
        }
        if ("REPLACED".equals(normalized)) {
            return "BLOCKED";
        }
        return normalized;
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    private void ensureNoDuplicateActiveHolderCard(UUID corporateClientId, String holderIdentifier, String cardType) {
        boolean duplicateExists = corporateCardRepository.existsByCorporateClientIdAndHolderIdentifierIgnoreCaseAndCardTypeAndStatusIn(
                corporateClientId,
                holderIdentifier.trim(),
                cardType.trim().toUpperCase(),
                List.of("ACTIVE")
        );
        if (duplicateExists) {
            throw new IllegalStateException("Active corporate card already exists for holder under same client/card type");
        }
    }

    private CorporateCardResponse toResponse(CorporateCard card, String action) {
        CorporateCardResponse response = new CorporateCardResponse();
        response.setId(card.getId());
        response.setCorporateClientId(card.getCorporateClientId());
        response.setContractId(card.getContractId());
        response.setHolderName(card.getHolderName());
        response.setHolderIdentifier(card.getHolderIdentifier());
        response.setCardType(card.getCardType());
        response.setCardProductId(card.getCardProductId());
        response.setCardId(card.getCardId());
        response.setCardNumber(card.getCardNumber());
        response.setStatus(card.getStatus());
        response.setReplacedByCorporateCardId(card.getReplacedByCorporateCardId());
        response.setValidFrom(card.getValidFrom());
        response.setValidTo(card.getValidTo());
        response.setCreatedAt(card.getCreatedAt());
        response.setUpdatedAt(card.getUpdatedAt());
        response.setAction(action);
        response.setTitle("Corporate benefit card");
        response.setHtml(buildPrintHtml(card, action));
        return response;
    }

    private String buildPrintHtml(CorporateCard card, String action) {
        String generatedAt = OffsetDateTime.now().toString();
        return """
                <html><head><title>Corporate Benefit Card</title></head><body style="font-family: Arial, sans-serif; padding: 20px;">
                <div style="border: 1px solid #333; border-radius: 8px; max-width: 520px; padding: 18px;">
                <h2 style="margin-top: 0;">Corporate Benefit Card</h2>
                <p><strong>Action:</strong> %s</p>
                <p><strong>Card Number:</strong> %s</p>
                <p><strong>Status:</strong> %s</p>
                <p><strong>Holder:</strong> %s</p>
                <p><strong>Holder ID:</strong> %s</p>
                <p><strong>Card Type:</strong> %s</p>
                <p><strong>Valid From:</strong> %s</p>
                <p><strong>Valid To:</strong> %s</p>
                <p><strong>Generated:</strong> %s</p>
                </div></body></html>
                """.formatted(action, card.getCardNumber(), card.getStatus(), card.getHolderName(),
                card.getHolderIdentifier(), card.getCardType(),
                card.getValidFrom() != null ? card.getValidFrom() : "-",
                card.getValidTo() != null ? card.getValidTo() : "-",
                generatedAt);
    }

    @Transactional
    public CorporateCardResponse block(UUID corporateCardId, String reason, UUID actorUserId, UUID organizationId) {
        CorporateCard card = corporateCardRepository.findById(corporateCardId)
                .orElseThrow(() -> new NoSuchElementException("Corporate card not found: " + corporateCardId));
        if ("BLOCKED".equalsIgnoreCase(card.getStatus())) {
            return toResponse(card, "BLOCK");
        }
        HospitalCardRegistryClient.RegistryCardResult updated = hospitalCardRegistryClient.updateStatus(
                actorUserId,
                organizationId,
                card.getCardId(),
                "BLOCKED",
                reason
        );
        card.setStatus(toCorporateStatus(updated.status()));
        CorporateCard saved = corporateCardRepository.save(card);
        return toResponse(saved, "BLOCK");
    }

    private void validateClientWindow(CorporateClient client) {
        LocalDate today = LocalDate.now();
        if (client.getValidFrom() != null && client.getValidFrom().isAfter(today)) {
            throw new IllegalStateException("Corporate client validity window has not started");
        }
        if (client.getValidTo() != null && client.getValidTo().isBefore(today)) {
            throw new IllegalStateException("Corporate client validity window has expired");
        }
    }

    private LocalDate deriveValidFrom(CorporateClient client, UUID contractId) {
        if (contractId == null) {
            return client.getValidFrom();
        }
        CorporateContract contract = corporateContractRepository.findById(contractId)
                .orElseThrow(() -> new NoSuchElementException("Corporate contract not found: " + contractId));
        return contract.getValidFrom();
    }

    private LocalDate deriveValidTo(CorporateClient client, UUID contractId) {
        if (contractId == null) {
            return client.getValidTo();
        }
        CorporateContract contract = corporateContractRepository.findById(contractId)
                .orElseThrow(() -> new NoSuchElementException("Corporate contract not found: " + contractId));
        return contract.getValidTo();
    }

    private void ensureNotExpired(CorporateCard card) {
        if (card.getValidTo() != null && card.getValidTo().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Corporate card is expired");
        }
    }

    private String canonicalComparableStatus(String status) {
        String normalized = normalizeStatus(status);
        if ("ISSUED".equals(normalized)) {
            return "ACTIVE";
        }
        if ("REPLACED".equals(normalized)) {
            return "BLOCKED";
        }
        return normalized;
    }

    private void expireIfPastDue(CorporateCard card, UUID actorUserId, UUID organizationId) {
        if (!isActive(card.getStatus()) || card.getValidTo() == null || !card.getValidTo().isBefore(LocalDate.now())) {
            return;
        }
        try {
            if (actorUserId != null) {
                hospitalCardRegistryClient.updateStatus(actorUserId, organizationId, card.getCardId(), "EXPIRED", "Auto-expired by validity date");
            }
        } catch (Exception ignored) {
            // Keep local status aligned with validity even if registry update fails.
        }
        card.setStatus("EXPIRED");
        corporateCardRepository.save(card);
    }

    private boolean isPolicyActiveForCard(CorporateCard card) {
        LocalDate today = LocalDate.now();
        CorporateClient client = corporateClientRepository.findById(card.getCorporateClientId()).orElse(null);
        if (client == null || !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            return false;
        }
        if (client.getValidFrom() != null && client.getValidFrom().isAfter(today)) {
            return false;
        }
        if (client.getValidTo() != null && client.getValidTo().isBefore(today)) {
            return false;
        }
        if (card.getContractId() == null) {
            return true;
        }
        CorporateContract contract = corporateContractRepository.findById(card.getContractId()).orElse(null);
        if (contract == null) {
            return false;
        }
        if (contract.getValidFrom() != null && contract.getValidFrom().isAfter(today)) {
            return false;
        }
        return contract.getValidTo() == null || !contract.getValidTo().isBefore(today);
    }
}
