package com.easyops.hospitalcard.domain.account;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.limit.LimitEnforcementService;
import com.easyops.hospitalcard.domain.metrics.CardMetrics;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardAccountService {

    private static final String SOURCE_CARD_ADMIN = "CARD_ADMIN";

    private final CardAccountRepository cardAccountRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardRepository cardRepository;
    private final CardProductRepository cardProductRepository;
    private final LimitEnforcementService limitEnforcementService;
    private final CardMetrics cardMetrics;

    @Transactional(readOnly = true)
    public PagedResponse<CardTransactionResponse> listTransactions(
            UUID cardId,
            OffsetDateTime from,
            OffsetDateTime to,
            String type,
            String status,
            int page,
            int size) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        ensureWalletAllowed(card);
        CardAccount account = cardAccountRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card account not found for card: " + cardId));

        Specification<CardTransaction> spec = Specification
                .where(CardTransactionSpecifications.hasCardAccountId(account.getId()))
                .and(CardTransactionSpecifications.createdFrom(from))
                .and(CardTransactionSpecifications.createdTo(to))
                .and(CardTransactionSpecifications.hasTransactionType(type))
                .and(CardTransactionSpecifications.hasStatus(status));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = cardTransactionRepository.findAll(spec, pageable);

        List<CardTransactionResponse> content = result.getContent().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        PagedResponse<CardTransactionResponse> response = new PagedResponse<>();
        response.setContent(content);
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setSize(result.getSize());
        response.setNumber(result.getNumber());
        return response;
    }

    @Transactional(readOnly = true)
    public CardBalanceResponse getBalance(UUID cardId) {
        cardMetrics.recordBalanceCheck();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        ensureWalletAllowed(card);
        CardAccount account = cardAccountRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card account not found for card: " + cardId));

        CardBalanceResponse response = new CardBalanceResponse();
        response.setCardId(cardId);
        response.setAccountId(account.getId());
        response.setAccountType(account.getAccountType());
        response.setCurrentBalance(account.getCurrentBalance());
        response.setCurrency(account.getCurrency());
        response.setCreditLimit(account.getCreditLimit());
        response.setLimitProfileId(card.getLimitProfileId());
        response.setLimitUsage(limitEnforcementService.getLimitUsageForBalance(cardId));
        return response;
    }

    @Transactional
    public CardTransactionResponse topup(UUID cardId, TopupRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        ensureWalletAllowed(card);
        CardAccount account = cardAccountRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card account not found for card: " + cardId));

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = cardTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if ("TOPUP".equals(existing.get().getTransactionType())) {
                    return toTransactionResponse(existing.get());
                }
                throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: key already used for a different operation");
            }
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive");
        }

        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency() : account.getCurrency();
        OffsetDateTime now = OffsetDateTime.now();

        CardTransaction tx = new CardTransaction();
        tx.setId(UUID.randomUUID());
        tx.setCardAccountId(account.getId());
        tx.setTransactionType("TOPUP");
        tx.setSourceSystem(SOURCE_CARD_ADMIN);
        tx.setExternalReferenceId(request.getReference());
        tx.setAmount(request.getAmount());
        tx.setCurrency(currency);
        tx.setStatus("COMMITTED");
        tx.setPostedAt(now);
        tx.setCreatedAt(now);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        cardTransactionRepository.save(tx);
        cardMetrics.recordTransaction("TOPUP");

        account.setCurrentBalance(account.getCurrentBalance().add(request.getAmount()));
        account.setUpdatedAt(now);
        cardAccountRepository.save(account);

        return toTransactionResponse(tx);
    }

    @Transactional
    public CardTransactionResponse createAdjustment(UUID cardId, CreateAdjustmentRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        ensureWalletAllowed(card);
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Adjustment reason is required");
        }

        CardAccount account = cardAccountRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card account not found for card: " + cardId));

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = cardTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if ("ADJUSTMENT".equals(existing.get().getTransactionType())) {
                    return toTransactionResponse(existing.get());
                }
                throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: key already used for a different operation");
            }
        }

        BigDecimal amount = request.getAmount();
        OffsetDateTime now = OffsetDateTime.now();

        CardTransaction tx = new CardTransaction();
        tx.setId(UUID.randomUUID());
        tx.setCardAccountId(account.getId());
        tx.setTransactionType("ADJUSTMENT");
        tx.setSourceSystem(SOURCE_CARD_ADMIN);
        tx.setExternalReferenceId(request.getReason());
        tx.setAmount(amount);
        tx.setCurrency(account.getCurrency());
        tx.setStatus("COMMITTED");
        tx.setPostedAt(now);
        tx.setCreatedAt(now);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        cardTransactionRepository.save(tx);
        cardMetrics.recordTransaction("ADJUSTMENT");

        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        account.setUpdatedAt(now);
        cardAccountRepository.save(account);

        return toTransactionResponse(tx);
    }

    @Transactional
    public CardTransactionResponse refundTransaction(UUID transactionId, RefundRequest request) {
        CardTransaction original = cardTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!"COMMITTED".equals(original.getStatus())) {
            throw new IllegalArgumentException("Only committed transactions can be refunded");
        }
        String type = original.getTransactionType();
        if (!"CAPTURE".equals(type) && !"TOPUP".equals(type)) {
            throw new IllegalArgumentException("Only CAPTURE or TOPUP transactions can be refunded");
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = cardTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if ("REFUND".equals(existing.get().getTransactionType())) {
                    return toTransactionResponse(existing.get());
                }
                throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: key already used for a different operation");
            }
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        BigDecimal maxRefund = original.getAmount().abs();
        if (request.getAmount().compareTo(maxRefund) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original transaction amount");
        }

        CardAccount account = cardAccountRepository.findById(original.getCardAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Card account not found for transaction"));
        Card card = cardRepository.findById(account.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + account.getCardId()));
        ensureWalletAllowed(card);

        OffsetDateTime now = OffsetDateTime.now();

        CardTransaction refundTx = new CardTransaction();
        refundTx.setId(UUID.randomUUID());
        refundTx.setCardAccountId(account.getId());
        refundTx.setTransactionType("REFUND");
        refundTx.setSourceSystem(original.getSourceSystem() != null ? original.getSourceSystem() : SOURCE_CARD_ADMIN);
        refundTx.setExternalReferenceId(request.getReason() != null ? request.getReason() : "refund-of:" + transactionId);
        refundTx.setAuthorizationId(transactionId);
        refundTx.setAmount(request.getAmount());
        refundTx.setCurrency(original.getCurrency());
        refundTx.setStatus("COMMITTED");
        refundTx.setPostedAt(now);
        refundTx.setCreatedAt(now);
        refundTx.setIdempotencyKey(request.getIdempotencyKey());
        cardTransactionRepository.save(refundTx);
        cardMetrics.recordTransaction("REFUND");

        account.setCurrentBalance(account.getCurrentBalance().add(request.getAmount()));
        account.setUpdatedAt(now);
        cardAccountRepository.save(account);

        return toTransactionResponse(refundTx);
    }

    private void ensureWalletAllowed(Card card) {
        CardProduct product = cardProductRepository.findById(card.getCardProductId())
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + card.getCardProductId()));
        if (isIdentityOnlyProduct(product)) {
            throw new IllegalArgumentException("Wallet operations are not allowed for identity-only card product: " + product.getCode());
        }
    }

    private static boolean isIdentityOnlyProduct(CardProduct product) {
        if (product == null || product.getCode() == null) {
            return false;
        }
        String code = product.getCode().trim().toUpperCase();
        return code.endsWith("_IDENTITY") || code.startsWith("CORPORATE_BENEFIT");
    }

    private CardTransactionResponse toTransactionResponse(CardTransaction tx) {
        CardTransactionResponse dto = new CardTransactionResponse();
        dto.setId(tx.getId());
        dto.setCardAccountId(tx.getCardAccountId());
        dto.setTransactionType(tx.getTransactionType());
        dto.setSourceSystem(tx.getSourceSystem());
        dto.setExternalReferenceId(tx.getExternalReferenceId());
        dto.setAuthorizationId(tx.getAuthorizationId());
        dto.setAmount(tx.getAmount());
        dto.setCurrency(tx.getCurrency());
        dto.setMealCountDelta(tx.getMealCountDelta());
        dto.setStatus(tx.getStatus());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setPostedAt(tx.getPostedAt());
        dto.setCreatedBy(tx.getCreatedBy());
        return dto;
    }
}
