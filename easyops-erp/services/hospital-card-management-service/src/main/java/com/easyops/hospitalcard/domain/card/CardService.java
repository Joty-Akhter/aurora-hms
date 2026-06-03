package com.easyops.hospitalcard.domain.card;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.account.CardAccount;
import com.easyops.hospitalcard.domain.account.CardAccountRepository;
import com.easyops.hospitalcard.domain.account.CardTransaction;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.events.CardClosedEvent;
import com.easyops.hospitalcard.domain.events.CardReplacedEvent;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String SOURCE_LIFECYCLE = "CARD_LIFECYCLE";
    private static final String TX_TYPE_TRANSFER = "TRANSFER";
    private static final String STATUS_COMMITTED = "COMMITTED";

    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardProductRepository cardProductRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final String DEFAULT_ACCOUNT_TYPE = "PREPAID";
    private static final String DEFAULT_CURRENCY = "INR";
    private static final String OWNER_TYPE_STAFF = "STAFF";
    private static final String STAFF_IDENTITY_PRODUCT_CODE = "STAFF_IDENTITY";

    @Transactional
    public CardResponse issue(IssueCardRequest request) {
        CardProduct product = cardProductRepository.findById(request.getCardProductId())
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + request.getCardProductId()));

        String cardNumber = request.getCardNumber();
        if (cardNumber == null || cardNumber.isBlank()) {
            cardNumber = generateCardNumber();
        }
        if (cardRepository.findByCardNumber(cardNumber).isPresent()) {
            throw new IllegalArgumentException("Card number already exists: " + cardNumber);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setCardNumber(cardNumber);
        card.setPhysicalSerial(request.getPhysicalSerial());
        card.setCardProductId(product.getId());
        card.setLimitProfileId(request.getLimitProfileId());
        card.setOwnerType(request.getOwnerType());
        card.setOwnerReferenceId(request.getOwnerReferenceId());
        card.setCorporateId(request.getCorporateId());
        card.setStatus("ISSUED");
        card.setIssuedAt(now);
        card.setCreatedAt(now);
        card.setUpdatedAt(now);
        card.setCreatedBy(null);
        cardRepository.save(card);

        if (!isIdentityOnlyProduct(product)) {
            CardAccount account = new CardAccount();
            account.setId(UUID.randomUUID());
            account.setCardId(card.getId());
            account.setAccountType(DEFAULT_ACCOUNT_TYPE);
            account.setCurrency(DEFAULT_CURRENCY);
            account.setCurrentBalance(BigDecimal.ZERO);
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
            cardAccountRepository.save(account);
        }

        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));
        CardDetailResponse response = new CardDetailResponse();
        copyCardToResponse(card, response);
        cardAccountRepository.findByCardId(card.getId()).ifPresent(acc -> {
            AccountSummary summary = new AccountSummary();
            summary.setAccountId(acc.getId());
            summary.setAccountType(acc.getAccountType());
            summary.setCurrentBalance(acc.getCurrentBalance());
            summary.setCurrency(acc.getCurrency());
            summary.setCreditLimit(acc.getCreditLimit());
            response.setAccountSummary(summary);
        });
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CardResponse> search(String cardNumber, String ownerReferenceId, String ownerType,
                                               UUID corporateId, UUID cardProductId, String status,
                                               OffsetDateTime issuedAtFrom, OffsetDateTime issuedAtTo,
                                               int page, int size) {
        Specification<Card> spec = Specification
                .where(CardSpecifications.hasCardNumber(cardNumber))
                .and(CardSpecifications.hasOwnerReferenceId(ownerReferenceId))
                .and(CardSpecifications.hasOwnerType(ownerType))
                .and(CardSpecifications.hasCorporateId(corporateId))
                .and(CardSpecifications.hasCardProductId(cardProductId))
                .and(CardSpecifications.hasStatus(status))
                .and(CardSpecifications.issuedAtBetween(issuedAtFrom, issuedAtTo));
        Page<Card> result = cardRepository.findAll(spec, PageRequest.of(page, size));
        PagedResponse<CardResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(this::toResponse).toList());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setNumber(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    /**
     * List cards for a portal user (owner). Used by GET /me/cards.
     */
    @Transactional(readOnly = true)
    public java.util.List<CardResponse> listCardsForOwner(String ownerReferenceId, String ownerType) {
        PagedResponse<CardResponse> paged = search(null, ownerReferenceId, ownerType, null, null, null, null, null, 0, 500);
        return paged.getContent();
    }

    /**
     * Throws if the card does not belong to the given owner (for /me/cards/{id}/statement).
     */
    @Transactional(readOnly = true)
    public void assertOwner(UUID cardId, String ownerReferenceId, String ownerType) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        if (!card.getOwnerReferenceId().equals(ownerReferenceId)) {
            throw new IllegalArgumentException("ACCESS_DENIED: Card does not belong to current user");
        }
        if (ownerType != null && !ownerType.isBlank() && !ownerType.equals(card.getOwnerType())) {
            throw new IllegalArgumentException("ACCESS_DENIED: Card does not belong to current user");
        }
    }

    @Transactional
    public CardResponse updateStatus(UUID id, UpdateCardStatusRequest request) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));
        String newStatus = normalizeRequired(request.getStatus(), "status").toUpperCase();
        if (!isSupportedStatus(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Use ISSUED, ACTIVE, BLOCKED, SUSPENDED, REVOKED, REPLACED, EXPIRED, or CLOSED.");
        }

        if (isTerminalCloseStatus(newStatus)) {
            CardAccount account = cardAccountRepository.findByCardId(id)
                    .orElse(null);
            BigDecimal balance = account != null ? account.getCurrentBalance() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                String reason = request.getReason();
                if (reason == null || reason.isBlank()) {
                    throw new IllegalArgumentException(
                            "Balance must be zero to close, or provide a reason to force close. Current balance: " + balance);
                }
            }
            String currency = account != null ? account.getCurrency() : null;
            card.setStatus(newStatus);
            OffsetDateTime now = OffsetDateTime.now();
            card.setClosedAt(now);
            card.setUpdatedAt(now);
            card.setStatusChangeReason(request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim() : null);
            cardRepository.save(card);
            eventPublisher.publishEvent(new CardClosedEvent(card.getId(), balance, currency));
            return toResponse(card);
        }

        card.setStatus(newStatus);
        OffsetDateTime now = OffsetDateTime.now();
        card.setUpdatedAt(now);
        if ("ACTIVE".equals(newStatus)) {
            card.setActivatedAt(now);
            card.setStatusChangeReason(null);
        } else if ("BLOCKED".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            card.setBlockedAt(now);
            card.setStatusChangeReason(request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim() : null);
        }
        cardRepository.save(card);
        return toResponse(card);
    }

    @Transactional
    public CardResponse replace(UUID id, ReplaceCardRequest request) {
        Card oldCard = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));

        Optional<CardAccount> oldAccountOpt = cardAccountRepository.findByCardId(oldCard.getId());
        CardAccount oldAccount = oldAccountOpt.orElse(null);
        BigDecimal oldBalance = oldAccount != null ? oldAccount.getCurrentBalance() : BigDecimal.ZERO;
        String currency = oldAccount != null ? oldAccount.getCurrency() : DEFAULT_CURRENCY;
        String accountType = oldAccount != null ? oldAccount.getAccountType() : DEFAULT_ACCOUNT_TYPE;

        String newCardNumber = generateCardNumber();
        OffsetDateTime now = OffsetDateTime.now();

        Card newCard = new Card();
        newCard.setId(UUID.randomUUID());
        newCard.setCardNumber(newCardNumber);
        newCard.setPhysicalSerial(null);
        newCard.setCardProductId(oldCard.getCardProductId());
        newCard.setLimitProfileId(oldCard.getLimitProfileId());
        newCard.setOwnerType(oldCard.getOwnerType());
        newCard.setOwnerReferenceId(oldCard.getOwnerReferenceId());
        newCard.setCorporateId(oldCard.getCorporateId());
        newCard.setStatus("ISSUED");
        newCard.setIssuedAt(now);
        newCard.setCreatedAt(now);
        newCard.setUpdatedAt(now);
        newCard.setCreatedBy(null);
        cardRepository.save(newCard);

        CardAccount newAccount = null;
        CardProduct product = cardProductRepository.findById(newCard.getCardProductId())
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + newCard.getCardProductId()));
        if (!isIdentityOnlyProduct(product)) {
            newAccount = new CardAccount();
            newAccount.setId(UUID.randomUUID());
            newAccount.setCardId(newCard.getId());
            newAccount.setAccountType(accountType);
            newAccount.setCurrency(currency);
            newAccount.setCurrentBalance(BigDecimal.ZERO);
            newAccount.setCreditLimit(oldAccount != null ? oldAccount.getCreditLimit() : null);
            newAccount.setCreatedAt(now);
            newAccount.setUpdatedAt(now);
            cardAccountRepository.save(newAccount);
        }

        if (oldBalance.compareTo(BigDecimal.ZERO) > 0 && oldAccount != null && newAccount != null) {
            String ref = "replace:" + oldCard.getId() + "->" + newCard.getId();
            OffsetDateTime posted = now;
            CardTransaction debitTx = new CardTransaction();
            debitTx.setId(UUID.randomUUID());
            debitTx.setCardAccountId(oldAccount.getId());
            debitTx.setTransactionType(TX_TYPE_TRANSFER);
            debitTx.setSourceSystem(SOURCE_LIFECYCLE);
            debitTx.setExternalReferenceId(ref);
            debitTx.setAmount(oldBalance.negate());
            debitTx.setCurrency(currency);
            debitTx.setStatus(STATUS_COMMITTED);
            debitTx.setPostedAt(posted);
            debitTx.setCreatedAt(now);
            cardTransactionRepository.save(debitTx);

            CardTransaction creditTx = new CardTransaction();
            creditTx.setId(UUID.randomUUID());
            creditTx.setCardAccountId(newAccount.getId());
            creditTx.setTransactionType(TX_TYPE_TRANSFER);
            creditTx.setSourceSystem(SOURCE_LIFECYCLE);
            creditTx.setExternalReferenceId(ref);
            creditTx.setAmount(oldBalance);
            creditTx.setCurrency(currency);
            creditTx.setStatus(STATUS_COMMITTED);
            creditTx.setPostedAt(posted);
            creditTx.setCreatedAt(now);
            cardTransactionRepository.save(creditTx);

            oldAccount.setCurrentBalance(BigDecimal.ZERO);
            oldAccount.setUpdatedAt(now);
            cardAccountRepository.save(oldAccount);
            newAccount.setCurrentBalance(oldBalance);
            newAccount.setUpdatedAt(now);
            cardAccountRepository.save(newAccount);
        }

        oldCard.setReplacedByCardId(newCard.getId());
        oldCard.setStatus("REPLACED");
        oldCard.setClosedAt(now);
        oldCard.setUpdatedAt(now);
        oldCard.setStatusChangeReason(request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim() : null);
        cardRepository.save(oldCard);

        eventPublisher.publishEvent(new CardReplacedEvent(oldCard.getId(), newCard.getId(), oldBalance));

        return toResponse(newCard);
    }

    @Transactional
    public CardResponse issueStaffIdentity(IssueStaffCardRequest request) {
        String employeeId = normalizeRequired(request.getEmployeeId(), "employeeId");
        String employmentStatus = normalizeRequired(request.getEmploymentStatus(), "employmentStatus").toUpperCase();
        boolean adminOverride = Boolean.TRUE.equals(request.getAdminOverride());
        if (!"ACTIVE".equals(employmentStatus)) {
            if (!adminOverride) {
                throw new IllegalArgumentException("Cannot issue staff card for non-active employment status: " + employmentStatus);
            }
            normalizeRequired(request.getOverrideReason(), "overrideReason");
        }
        CardProduct product = cardProductRepository.findByCode(STAFF_IDENTITY_PRODUCT_CODE)
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + STAFF_IDENTITY_PRODUCT_CODE));

        ensureNoActiveStaffCard(product.getId(), employeeId);

        IssueCardRequest issueCardRequest = new IssueCardRequest();
        issueCardRequest.setCardProductId(product.getId());
        issueCardRequest.setOwnerType(OWNER_TYPE_STAFF);
        issueCardRequest.setOwnerReferenceId(employeeId);
        issueCardRequest.setCardNumber(request.getCardNumber());
        issueCardRequest.setPhysicalSerial(request.getPhysicalSerial());
        CardResponse created = issue(issueCardRequest);

        UpdateCardStatusRequest activate = new UpdateCardStatusRequest();
        activate.setStatus("ACTIVE");
        activate.setReason(adminOverride
                ? "Staff onboarding activation (admin override): " + request.getOverrideReason().trim()
                : "Staff onboarding activation");
        return updateStatus(created.getId(), activate);
    }

    @Transactional
    public CardResponse replaceStaffIdentity(UUID id, ReplaceStaffCardRequest request) {
        Card oldCard = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));
        ensureStaffIdentityCard(oldCard);
        if (isTerminalCloseStatus(oldCard.getStatus())) {
            throw new IllegalArgumentException("Cannot replace card with terminal status: " + oldCard.getStatus());
        }
        ReplaceCardRequest replaceRequest = new ReplaceCardRequest();
        replaceRequest.setReason(normalizeRequired(request.getReason(), "reason"));
        return replace(id, replaceRequest);
    }

    @Transactional
    public CardResponse reprintStaffIdentity(UUID id, String reason) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));
        ensureStaffIdentityCard(card);
        String normalizedReason = normalizeRequired(reason, "reason");
        if (isTerminalCloseStatus(card.getStatus())) {
            throw new IllegalArgumentException("Cannot reprint card with terminal status: " + card.getStatus());
        }
        card.setUpdatedAt(OffsetDateTime.now());
        card.setStatusChangeReason("REPRINT: " + normalizedReason);
        cardRepository.save(card);
        return toResponse(card);
    }

    @Transactional
    public CardResponse suspendStaffIdentity(UUID id, String reason) {
        return updateStaffIdentityStatus(id, "SUSPENDED", reason);
    }

    @Transactional
    public CardResponse revokeStaffIdentity(UUID id, String reason) {
        return updateStaffIdentityStatus(id, "REVOKED", reason);
    }

    @Transactional(readOnly = true)
    public StaffCardVerificationResponse verifyStaffCard(String cardNumber, String employeeId) {
        boolean hasCardNumber = cardNumber != null && !cardNumber.isBlank();
        boolean hasEmployeeId = employeeId != null && !employeeId.isBlank();
        if (!hasCardNumber && !hasEmployeeId) {
            throw new IllegalArgumentException("Either cardNumber or employeeId is required");
        }
        if (hasCardNumber && hasEmployeeId) {
            throw new IllegalArgumentException("Provide only one lookup parameter: cardNumber or employeeId");
        }

        Card card;
        if (hasCardNumber) {
            card = cardRepository.findByCardNumber(cardNumber.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardNumber));
        } else {
            String normalizedEmployeeId = normalizeRequired(employeeId, "employeeId");
            CardProduct product = cardProductRepository.findByCode(STAFF_IDENTITY_PRODUCT_CODE)
                    .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + STAFF_IDENTITY_PRODUCT_CODE));
            card = cardRepository.findByOwnerTypeAndOwnerReferenceIdAndCardProductId(OWNER_TYPE_STAFF, normalizedEmployeeId, product.getId())
                    .stream()
                    .sorted((a, b) -> {
                        if ("ACTIVE".equals(a.getStatus()) && !"ACTIVE".equals(b.getStatus())) return -1;
                        if (!"ACTIVE".equals(a.getStatus()) && "ACTIVE".equals(b.getStatus())) return 1;
                        OffsetDateTime aIssued = a.getIssuedAt() != null ? a.getIssuedAt() : OffsetDateTime.MIN;
                        OffsetDateTime bIssued = b.getIssuedAt() != null ? b.getIssuedAt() : OffsetDateTime.MIN;
                        return bIssued.compareTo(aIssued);
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Staff card not found for employeeId: " + normalizedEmployeeId));
        }

        ensureStaffIdentityCard(card);
        StaffCardVerificationResponse response = new StaffCardVerificationResponse();
        response.setCardId(card.getId());
        response.setCardNumber(card.getCardNumber());
        response.setEmployeeId(card.getOwnerReferenceId());
        response.setOwnerType(card.getOwnerType());
        response.setStatus(card.getStatus());
        response.setActive("ACTIVE".equals(card.getStatus()));
        return response;
    }

    @Transactional
    public java.util.List<CardResponse> syncStaffEmploymentStatus(StaffEmploymentStatusSyncRequest request) {
        String employeeId = normalizeRequired(request.getEmployeeId(), "employeeId");
        String employmentStatus = normalizeRequired(request.getEmploymentStatus(), "employmentStatus");
        CardProduct product = cardProductRepository.findByCode(STAFF_IDENTITY_PRODUCT_CODE)
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + STAFF_IDENTITY_PRODUCT_CODE));
        java.util.List<Card> cards = cardRepository.findByOwnerTypeAndOwnerReferenceIdAndCardProductId(
                OWNER_TYPE_STAFF, employeeId, product.getId());
        if (cards.isEmpty()) {
            return java.util.List.of();
        }
        String targetCardStatus = mapEmploymentToCardStatus(employmentStatus);
        UpdateCardStatusRequest statusRequest = new UpdateCardStatusRequest();
        statusRequest.setStatus(targetCardStatus);
        statusRequest.setReason(request.getReason());
        return cards.stream()
                .filter(card -> !targetCardStatus.equals(card.getStatus()))
                .filter(card -> !isTerminalCloseStatus(card.getStatus()))
                .map(card -> updateStatus(card.getId(), statusRequest))
                .toList();
    }

    private String generateCardNumber() {
        return "CARD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private CardResponse toResponse(Card card) {
        CardResponse dto = new CardResponse();
        copyCardToResponse(card, dto);
        return dto;
    }

    private void ensureNoActiveStaffCard(UUID staffProductId, String employeeId) {
        boolean hasActive = cardRepository.findByOwnerTypeAndOwnerReferenceIdAndCardProductId(
                        OWNER_TYPE_STAFF, employeeId, staffProductId)
                .stream()
                .anyMatch(card -> "ACTIVE".equals(card.getStatus()));
        if (hasActive) {
            throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: Active staff identity card already exists for employeeId: " + employeeId);
        }
    }

    private void ensureStaffIdentityCard(Card card) {
        if (!OWNER_TYPE_STAFF.equals(card.getOwnerType())) {
            throw new IllegalArgumentException("Card is not a staff identity card");
        }
        CardProduct product = cardProductRepository.findById(card.getCardProductId())
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + card.getCardProductId()));
        if (!STAFF_IDENTITY_PRODUCT_CODE.equals(product.getCode())) {
            throw new IllegalArgumentException("Card is not a staff identity card");
        }
    }

    private CardResponse updateStaffIdentityStatus(UUID id, String status, String reason) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));
        ensureStaffIdentityCard(card);
        UpdateCardStatusRequest statusRequest = new UpdateCardStatusRequest();
        statusRequest.setStatus(status);
        statusRequest.setReason(normalizeRequired(reason, "reason"));
        return updateStatus(id, statusRequest);
    }

    private static String mapEmploymentToCardStatus(String employmentStatus) {
        String normalized = employmentStatus.trim().toUpperCase();
        return switch (normalized) {
            case "ACTIVE" -> "ACTIVE";
            case "SUSPENDED", "ON_LEAVE", "INVESTIGATION" -> "SUSPENDED";
            case "TERMINATED", "RESIGNED", "INACTIVE" -> "REVOKED";
            default -> throw new IllegalArgumentException("Unsupported employmentStatus: " + employmentStatus);
        };
    }

    private static boolean isSupportedStatus(String status) {
        return "ISSUED".equals(status)
                || "ACTIVE".equals(status)
                || "BLOCKED".equals(status)
                || "SUSPENDED".equals(status)
                || "REVOKED".equals(status)
                || "REPLACED".equals(status)
                || "EXPIRED".equals(status)
                || "CLOSED".equals(status);
    }

    private static boolean isTerminalCloseStatus(String status) {
        return "CLOSED".equals(status)
                || "REVOKED".equals(status)
                || "REPLACED".equals(status)
                || "EXPIRED".equals(status);
    }

    private static boolean isIdentityOnlyProduct(CardProduct product) {
        if (product == null || product.getCode() == null) {
            return false;
        }
        String code = product.getCode().trim().toUpperCase();
        return code.endsWith("_IDENTITY") || code.startsWith("CORPORATE_BENEFIT");
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private void copyCardToResponse(Card card, CardResponse dto) {
        dto.setId(card.getId());
        dto.setCardNumber(card.getCardNumber());
        dto.setPhysicalSerial(card.getPhysicalSerial());
        dto.setCardProductId(card.getCardProductId());
        dto.setLimitProfileId(card.getLimitProfileId());
        dto.setOwnerType(card.getOwnerType());
        dto.setOwnerReferenceId(card.getOwnerReferenceId());
        dto.setCorporateId(card.getCorporateId());
        dto.setStatus(card.getStatus());
        dto.setReplacedByCardId(card.getReplacedByCardId());
        dto.setIssuedAt(card.getIssuedAt());
        dto.setActivatedAt(card.getActivatedAt());
        dto.setBlockedAt(card.getBlockedAt());
        dto.setClosedAt(card.getClosedAt());
        dto.setCreatedAt(card.getCreatedAt());
        dto.setCreatedBy(card.getCreatedBy());
        dto.setStatusChangeReason(card.getStatusChangeReason());
    }
}
