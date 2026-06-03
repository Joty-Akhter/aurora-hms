package com.easyops.hospitalcard.domain.product;

import com.easyops.hospitalcard.api.dto.CardWithLimitUsageResponse;
import com.easyops.hospitalcard.api.dto.CreateLimitProfileRequest;
import com.easyops.hospitalcard.api.dto.LimitProfileResponse;
import com.easyops.hospitalcard.api.dto.LimitUsageSummary;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.domain.card.Card;
import com.easyops.hospitalcard.domain.card.CardRepository;
import com.easyops.hospitalcard.domain.limit.LimitEnforcementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LimitProfileService {

    private final LimitProfileRepository limitProfileRepository;
    private final CardRepository cardRepository;
    private final CardProductRepository cardProductRepository;
    private final LimitEnforcementService limitEnforcementService;

    @Transactional
    public LimitProfileResponse create(CreateLimitProfileRequest request) {
        LimitProfile entity = new LimitProfile();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDailyAmountLimit(request.getDailyAmountLimit());
        entity.setMonthlyAmountLimit(request.getMonthlyAmountLimit());
        entity.setDailyMealLimit(request.getDailyMealLimit());
        entity.setDailyVisitLimit(request.getDailyVisitLimit());
        entity.setResetPolicy(request.getResetPolicy());
        entity.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency() : "INR");
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        limitProfileRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public LimitProfileResponse getById(UUID id) {
        LimitProfile entity = limitProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Limit profile not found: " + id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LimitProfileResponse> list(String name, int page, int size) {
        Page<LimitProfile> result;
        if (name != null && !name.isBlank()) {
            result = limitProfileRepository.findByNameContainingIgnoreCase(name, PageRequest.of(page, size));
        } else {
            result = limitProfileRepository.findAll(PageRequest.of(page, size));
        }
        PagedResponse<LimitProfileResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(this::toResponse).toList());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setNumber(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    /**
     * Returns cards that use this limit profile (direct assignment or product default), with current period limit usage.
     */
    @Transactional(readOnly = true)
    public List<CardWithLimitUsageResponse> getCardsWithUsage(UUID profileId) {
        limitProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Limit profile not found: " + profileId));

        Set<UUID> seenIds = new LinkedHashSet<>();
        List<Card> cards = new ArrayList<>();
        List<Card> direct = cardRepository.findByLimitProfileId(profileId);
        for (Card c : direct) {
            if (seenIds.add(c.getId())) {
                cards.add(c);
            }
        }
        List<CardProduct> productsWithDefault = cardProductRepository.findByDefaultLimitProfileId(profileId);
        for (CardProduct p : productsWithDefault) {
            for (Card c : cardRepository.findByCardProductIdAndLimitProfileIdIsNull(p.getId())) {
                if (seenIds.add(c.getId())) {
                    cards.add(c);
                }
            }
        }

        List<CardWithLimitUsageResponse> result = new ArrayList<>(cards.size());
        for (Card card : cards) {
            CardWithLimitUsageResponse row = new CardWithLimitUsageResponse();
            row.setCardId(card.getId());
            row.setCardNumber(card.getCardNumber());
            row.setOwnerType(card.getOwnerType());
            row.setOwnerReferenceId(card.getOwnerReferenceId());
            row.setStatus(card.getStatus());
            LimitUsageSummary usage = limitEnforcementService.getLimitUsageForBalance(card.getId());
            row.setLimitUsage(usage);
            result.add(row);
        }
        return result;
    }

    private LimitProfileResponse toResponse(LimitProfile entity) {
        LimitProfileResponse dto = new LimitProfileResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setDailyAmountLimit(entity.getDailyAmountLimit());
        dto.setMonthlyAmountLimit(entity.getMonthlyAmountLimit());
        dto.setDailyMealLimit(entity.getDailyMealLimit());
        dto.setDailyVisitLimit(entity.getDailyVisitLimit());
        dto.setResetPolicy(entity.getResetPolicy());
        dto.setCurrency(entity.getCurrency());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
