package com.easyops.hospitalcard.domain.report;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.account.CardAccount;
import com.easyops.hospitalcard.domain.account.CardAccountRepository;
import com.easyops.hospitalcard.domain.account.CardTransactionRepository;
import com.easyops.hospitalcard.domain.account.UsageByDomainProjection;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardReportService {

    private final CardAccountRepository cardAccountRepository;
    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;

    /**
     * Prepaid liabilities: cards/accounts with balance > 0.
     * When asOf is null: uses current_balance from account.
     * When asOf is set: computes point-in-time balance from committed transactions with posted_at (or created_at) <= asOf.
     * Optional filters: cardProductId, ownerType.
     */
    public List<LiabilityReportItem> getLiabilities(OffsetDateTime asOf, UUID cardProductId, String ownerType) {
        String ownerTypeTrimmed = trimOrNull(ownerType);
        if (asOf != null) {
            return getLiabilitiesAsAt(asOf, cardProductId, ownerTypeTrimmed);
        }
        List<CardAccount> accounts = cardAccountRepository.findLiabilities(cardProductId, ownerTypeTrimmed);
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<UUID> cardIds = accounts.stream().map(CardAccount::getCardId).distinct().toList();
        Map<UUID, Card> cardsById = cardRepository.findAllById(cardIds).stream().collect(Collectors.toMap(Card::getId, c -> c));
        List<LiabilityReportItem> items = new ArrayList<>();
        for (CardAccount a : accounts) {
            Card card = cardsById.get(a.getCardId());
            if (card == null) continue;
            LiabilityReportItem item = new LiabilityReportItem();
            item.setCardId(card.getId());
            item.setCardNumber(card.getCardNumber());
            item.setOwnerType(card.getOwnerType());
            item.setOwnerReferenceId(card.getOwnerReferenceId());
            item.setCurrentBalance(a.getCurrentBalance());
            item.setCurrency(a.getCurrency());
            items.add(item);
        }
        return items;
    }

    private List<LiabilityReportItem> getLiabilitiesAsAt(OffsetDateTime asOf, UUID cardProductId, String ownerType) {
        List<Object[]> rows = cardTransactionRepository.findBalanceAsAtByAccount(asOf);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> balanceByAccountId = new java.util.HashMap<>();
        List<UUID> accountIds = new ArrayList<>();
        for (Object[] row : rows) {
            UUID accountId = (UUID) row[0];
            BigDecimal balance = (BigDecimal) row[1];
            balanceByAccountId.put(accountId, balance);
            accountIds.add(accountId);
        }
        List<CardAccount> accounts = cardAccountRepository.findAllById(accountIds);
        List<UUID> cardIds = accounts.stream().map(CardAccount::getCardId).distinct().toList();
        List<Card> cards = cardRepository.findAllById(cardIds);
        Map<UUID, Card> cardsById = cards.stream().collect(Collectors.toMap(Card::getId, c -> c));
        List<LiabilityReportItem> items = new ArrayList<>();
        for (CardAccount a : accounts) {
            Card card = cardsById.get(a.getCardId());
            if (card == null) continue;
            if (cardProductId != null && !cardProductId.equals(card.getCardProductId())) continue;
            if (ownerType != null && !ownerType.equals(card.getOwnerType())) continue;
            BigDecimal balance = balanceByAccountId.get(a.getId());
            if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) continue;
            LiabilityReportItem item = new LiabilityReportItem();
            item.setCardId(card.getId());
            item.setCardNumber(card.getCardNumber());
            item.setOwnerType(card.getOwnerType());
            item.setOwnerReferenceId(card.getOwnerReferenceId());
            item.setCurrentBalance(balance);
            item.setCurrency(a.getCurrency());
            items.add(item);
        }
        return items;
    }

    /**
     * Usage aggregated by source_system (e.g. CANTEEN, HOSPITAL_BILLING) in date range.
     */
    public List<UsageByDomainItem> getUsageByDomain(OffsetDateTime from, OffsetDateTime to, String sourceSystem) {
        List<UsageByDomainProjection> rows = cardTransactionRepository.usageByDomain(from, to, trimOrNull(sourceSystem));
        return rows.stream()
            .map(r -> new UsageByDomainItem(
                r.getSourceSystem(),
                r.getTotalAmount(),
                r.getTransactionCount() != null ? r.getTransactionCount() : 0L))
            .toList();
    }

    /**
     * Corporate exposure: cards linked to corporate_id with balance and credit_limit; summary total.
     */
    public CorporateExposureResponse getCorporateExposure(UUID corporateId, OffsetDateTime asOf) {
        if (corporateId == null) {
            CorporateExposureResponse empty = new CorporateExposureResponse();
            empty.setItems(List.of());
            empty.setTotalBalance(BigDecimal.ZERO);
            return empty;
        }
        List<Card> cards = cardRepository.findByCorporateId(corporateId, Pageable.unpaged()).getContent();
        List<CorporateExposureItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Card card : cards) {
            CardAccount account = cardAccountRepository.findByCardId(card.getId()).orElse(null);
            if (account == null) continue;
            CorporateExposureItem item = new CorporateExposureItem();
            item.setCardId(card.getId());
            item.setCardNumber(card.getCardNumber());
            item.setOwnerType(card.getOwnerType());
            item.setOwnerReferenceId(card.getOwnerReferenceId());
            item.setCurrentBalance(account.getCurrentBalance());
            item.setCreditLimit(account.getCreditLimit());
            item.setCurrency(account.getCurrency());
            items.add(item);
            if (account.getCurrentBalance() != null) {
                total = total.add(account.getCurrentBalance());
            }
        }
        CorporateExposureResponse response = new CorporateExposureResponse();
        response.setItems(items);
        response.setTotalBalance(total);
        return response;
    }

    private static String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
