package com.easyops.hospitalcard.domain.product;

import com.easyops.hospitalcard.api.dto.CardProductResponse;
import com.easyops.hospitalcard.api.dto.CreateCardProductRequest;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.api.dto.UpdateCardProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardProductService {

    private final CardProductRepository cardProductRepository;

    @Transactional
    public CardProductResponse create(CreateCardProductRequest request) {
        if (cardProductRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Card product with code already exists: " + request.getCode());
        }
        CardProduct entity = new CardProduct();
        entity.setId(UUID.randomUUID());
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setMediumType(request.getMediumType());
        entity.setUsageDomains(request.getUsageDomains());
        entity.setDefaultLimitProfileId(request.getDefaultLimitProfileId());
        entity.setValidityStartDate(request.getValidityStartDate());
        entity.setValidityEndDate(request.getValidityEndDate());
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus() : "ACTIVE");
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(null); // can be set from security context when available
        cardProductRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public CardProductResponse getById(UUID id) {
        CardProduct entity = cardProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CardProductResponse> list(String code, String status, int page, int size) {
        Specification<CardProduct> spec = Specification
                .where(CardProductSpecifications.hasCode(code))
                .and(CardProductSpecifications.hasStatus(status));
        Page<CardProduct> result = cardProductRepository.findAll(spec, PageRequest.of(page, size));
        PagedResponse<CardProductResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(this::toResponse).toList());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setNumber(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    @Transactional
    public CardProductResponse update(UUID id, UpdateCardProductRequest request) {
        CardProduct entity = cardProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card product not found: " + id));
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getMediumType() != null) {
            entity.setMediumType(request.getMediumType());
        }
        if (request.getUsageDomains() != null) {
            entity.setUsageDomains(request.getUsageDomains());
        }
        if (request.getDefaultLimitProfileId() != null) {
            entity.setDefaultLimitProfileId(request.getDefaultLimitProfileId());
        }
        if (request.getValidityStartDate() != null) {
            entity.setValidityStartDate(request.getValidityStartDate());
        }
        if (request.getValidityEndDate() != null) {
            entity.setValidityEndDate(request.getValidityEndDate());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(OffsetDateTime.now());
        cardProductRepository.save(entity);
        return toResponse(entity);
    }

    private CardProductResponse toResponse(CardProduct entity) {
        CardProductResponse dto = new CardProductResponse();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setMediumType(entity.getMediumType());
        dto.setUsageDomains(entity.getUsageDomains());
        dto.setDefaultLimitProfileId(entity.getDefaultLimitProfileId());
        dto.setValidityStartDate(entity.getValidityStartDate());
        dto.setValidityEndDate(entity.getValidityEndDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
