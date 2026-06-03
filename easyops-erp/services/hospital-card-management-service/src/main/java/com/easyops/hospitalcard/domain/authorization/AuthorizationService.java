package com.easyops.hospitalcard.domain.authorization;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.account.CardAccount;
import com.easyops.hospitalcard.domain.account.CardAccountRepository;
import com.easyops.hospitalcard.domain.account.CardTransaction;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.events.CardBalanceChangedEvent;
import com.easyops.hospitalcard.domain.events.CardTransactionCommittedEvent;
import com.easyops.hospitalcard.domain.limit.LimitCheckResult;
import com.easyops.hospitalcard.domain.limit.LimitEnforcementService;
import com.easyops.hospitalcard.domain.metrics.CardMetrics;
import com.easyops.hospitalcard.domain.product.CardProduct;
import com.easyops.hospitalcard.domain.product.CardProductRepository;
import com.easyops.hospitalcard.domain.product.LimitProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMMITTED = "COMMITTED";
    private static final String TYPE_AUTH = "AUTH";
    private static final String TYPE_CAPTURE = "CAPTURE";
    private static final String REASON_CARD_NOT_FOUND = "CARD_NOT_FOUND";
    private static final String REASON_CARD_BLOCKED = "CARD_BLOCKED";
    private static final String REASON_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LimitEnforcementService limitEnforcementService;
    private final CardMetrics cardMetrics;
    private final CardProductRepository cardProductRepository;

    @Transactional(readOnly = true)
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        String lookup = request.getEffectiveCardNumber();
        if (lookup == null || lookup.isBlank()) {
            return declined(REASON_CARD_NOT_FOUND, null);
        }

        Card card = cardRepository.findByCardNumber(lookup).orElse(null);
        if (card == null) {
            cardMetrics.recordAuthorization(false);
            return declined(REASON_CARD_NOT_FOUND, null);
        }

        if (!STATUS_ACTIVE.equals(card.getStatus())) {
            cardMetrics.recordAuthorization(false);
            CardAccount account = cardAccountRepository.findByCardId(card.getId()).orElse(null);
            BigDecimal balance = account != null ? account.getCurrentBalance() : null;
            return declined(REASON_CARD_BLOCKED, balance);
        }
        ensureWalletAllowed(card);

        CardAccount account = cardAccountRepository.findByCardId(card.getId())
                .orElseThrow(() -> new IllegalStateException("Card account not found for card: " + card.getId()));

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = cardTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if (TYPE_AUTH.equals(existing.get().getTransactionType())) {
                    cardMetrics.recordAuthorization(true);
                    AuthorizationResponse response = new AuthorizationResponse();
                    response.setApproved(true);
                    response.setAuthorizationId(existing.get().getId());
                    response.setRemainingBalance(account.getCurrentBalance());
                    return response;
                }
                throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: key already used for a different operation");
            }
        }

        if (request.getExternalReferenceId() != null && request.getSourceSystem() != null) {
            var existing = cardTransactionRepository.findByExternalReferenceIdAndSourceSystem(
                    request.getExternalReferenceId(), request.getSourceSystem());
            if (existing.isPresent() && TYPE_AUTH.equals(existing.get().getTransactionType())) {
                AuthorizationResponse response = new AuthorizationResponse();
                response.setApproved(true);
                response.setAuthorizationId(existing.get().getId());
                response.setRemainingBalance(account.getCurrentBalance());
                return response;
            }
        }

        if (account.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            cardMetrics.recordAuthorization(false);
            return declined(REASON_INSUFFICIENT_BALANCE, account.getCurrentBalance());
        }

        LimitCheckResult limitCheck = limitEnforcementService.checkAuthorization(
                card.getId(),
                request.getAmount(),
                request.getMealCount(),
                null);
        if (!limitCheck.isAllowed()) {
            cardMetrics.recordAuthorization(false);
            AuthorizationResponse response = new AuthorizationResponse();
            response.setApproved(false);
            response.setReasonCode(limitCheck.getReasonCode());
            response.setRemainingBalance(account.getCurrentBalance());
            response.setRemainingLimits(limitCheck.getRemainingLimits());
            return response;
        }

        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency() : account.getCurrency();
        OffsetDateTime now = OffsetDateTime.now();

        CardTransaction authTx = new CardTransaction();
        authTx.setId(UUID.randomUUID());
        authTx.setCardAccountId(account.getId());
        authTx.setTransactionType(TYPE_AUTH);
        authTx.setSourceSystem(request.getSourceSystem());
        authTx.setExternalReferenceId(request.getExternalReferenceId());
        authTx.setAmount(request.getAmount().negate());
        authTx.setCurrency(currency);
        authTx.setStatus(STATUS_PENDING);
        authTx.setCreatedAt(now);
        authTx.setIdempotencyKey(request.getIdempotencyKey());
        if (request.getMealCount() != null) {
            authTx.setMealCountDelta(-request.getMealCount());
        }
        cardTransactionRepository.save(authTx);
        cardMetrics.recordAuthorization(true);
        cardMetrics.recordTransaction(TYPE_AUTH);

        AuthorizationResponse response = new AuthorizationResponse();
        response.setApproved(true);
        response.setAuthorizationId(authTx.getId());
        response.setRemainingBalance(account.getCurrentBalance());
        return response;
    }

    @Transactional
    public CardTransactionResponse capture(UUID authId, CaptureRequest request) {
        CardTransaction authTx = cardTransactionRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("Authorization not found: " + authId));

        if (!TYPE_AUTH.equals(authTx.getTransactionType())) {
            throw new IllegalArgumentException("Transaction is not an authorization: " + authId);
        }
        if (!STATUS_PENDING.equals(authTx.getStatus())) {
            throw new IllegalStateException("Authorization already captured or reversed: " + authId);
        }

        BigDecimal authAmount = authTx.getAmount().abs();
        if (request.getAmount().compareTo(authAmount) > 0) {
            throw new IllegalArgumentException("Capture amount cannot exceed authorized amount");
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = cardTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if (TYPE_CAPTURE.equals(existing.get().getTransactionType())
                        && authId.equals(existing.get().getAuthorizationId())) {
                    return toTransactionResponse(existing.get());
                }
                throw new IllegalArgumentException("IDEMPOTENCY_CONFLICT: key already used for a different operation");
            }
        }

        CardAccount account = cardAccountRepository.findById(authTx.getCardAccountId())
                .orElseThrow(() -> new IllegalStateException("Card account not found: " + authTx.getCardAccountId()));
        Card card = cardRepository.findById(account.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + account.getCardId()));
        ensureWalletAllowed(card);

        OffsetDateTime now = OffsetDateTime.now();

        CardTransaction captureTx = new CardTransaction();
        captureTx.setId(UUID.randomUUID());
        captureTx.setCardAccountId(account.getId());
        captureTx.setTransactionType(TYPE_CAPTURE);
        captureTx.setSourceSystem(authTx.getSourceSystem());
        captureTx.setExternalReferenceId(authTx.getExternalReferenceId());
        captureTx.setAuthorizationId(authId);
        captureTx.setAmount(request.getAmount().negate());
        captureTx.setCurrency(authTx.getCurrency());
        captureTx.setStatus(STATUS_COMMITTED);
        captureTx.setPostedAt(now);
        captureTx.setCreatedAt(now);
        captureTx.setIdempotencyKey(request.getIdempotencyKey());
        cardTransactionRepository.save(captureTx);

        account.setCurrentBalance(account.getCurrentBalance().subtract(request.getAmount()));
        account.setUpdatedAt(now);
        cardAccountRepository.save(account);

        authTx.setStatus(STATUS_COMMITTED);
        authTx.setPostedAt(now);
        cardTransactionRepository.save(authTx);

        LimitProfile limitProfile = limitEnforcementService.resolveLimitProfile(account.getCardId());
        if (limitProfile != null) {
            int mealDelta = authTx.getMealCountDelta() != null ? -authTx.getMealCountDelta() : 0;
            limitEnforcementService.recordConsumed(
                    account.getCardId(),
                    limitProfile.getId(),
                    request.getAmount(),
                    mealDelta,
                    0);
        }

        eventPublisher.publishEvent(new CardTransactionCommittedEvent(
                account.getCardId(),
                captureTx.getId(),
                request.getAmount(),
                captureTx.getSourceSystem(),
                captureTx.getExternalReferenceId()));
        eventPublisher.publishEvent(new CardBalanceChangedEvent(
                account.getCardId(),
                account.getId(),
                account.getCurrentBalance(),
                account.getCurrency()));

        cardMetrics.recordTransaction(TYPE_CAPTURE);
        return toTransactionResponse(captureTx);
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

    private AuthorizationResponse declined(String reasonCode, BigDecimal remainingBalance) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setApproved(false);
        response.setReasonCode(reasonCode);
        response.setRemainingBalance(remainingBalance);
        return response;
    }

    private static CardTransactionResponse toTransactionResponse(CardTransaction tx) {
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
