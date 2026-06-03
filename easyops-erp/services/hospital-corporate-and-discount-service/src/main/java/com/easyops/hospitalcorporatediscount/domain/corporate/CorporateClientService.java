package com.easyops.hospitalcorporatediscount.domain.corporate;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporateClientService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CorporateClientRepository repository;
    private final CorporateDiscountEventPublisher eventPublisher;

    @Transactional
    public CorporateResponse create(CreateCorporateRequest request) {
        if (repository.existsByCode(request.getCode().trim())) {
            throw new IllegalStateException("Corporate with code already exists: " + request.getCode());
        }
        CorporateClient entity = new CorporateClient();
        entity.setName(request.getName().trim());
        entity.setCode(request.getCode().trim().toUpperCase());
        entity.setType(request.getType().trim());
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : DEFAULT_STATUS);
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setPrimaryContactName(trimOrNull(request.getPrimaryContactName()));
        entity.setPrimaryContactPhone(trimOrNull(request.getPrimaryContactPhone()));
        entity.setPrimaryContactEmail(trimOrNull(request.getPrimaryContactEmail()));
        repository.save(entity);
        eventPublisher.publish(EventTypes.CORPORATE_CREATED, payload(entity));
        return toResponse(entity);
    }

    public CorporateResponse getById(UUID id) {
        CorporateClient entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Corporate not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<CorporateResponse> list(String code, String type, String status, int page, int size) {
        Specification<CorporateClient> spec = CorporateSpecifications.hasCode(code)
                .and(CorporateSpecifications.hasType(type))
                .and(CorporateSpecifications.hasStatus(status));
        Page<CorporateClient> p = repository.findAll(spec, PageRequest.of(page, size));
        return new PagedResponse<>(
                p.getContent().stream().map(this::toResponse).toList(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumber(),
                p.getSize(),
                p.isFirst(),
                p.isLast()
        );
    }

    @Transactional
    public CorporateResponse update(UUID id, UpdateCorporateRequest request) {
        CorporateClient entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Corporate not found: " + id));
        Optional.ofNullable(request.getName()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setName(s.trim()));
        Optional.ofNullable(request.getCode()).filter(s -> !s.isBlank()).ifPresent(s -> {
            if (!s.trim().equalsIgnoreCase(entity.getCode()) && repository.existsByCode(s.trim())) {
                throw new IllegalStateException("Corporate with code already exists: " + s);
            }
            entity.setCode(s.trim().toUpperCase());
        });
        Optional.ofNullable(request.getType()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setType(s.trim()));
        Optional.ofNullable(request.getStatus()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setStatus(s.trim()));
        if (request.getValidFrom() != null) entity.setValidFrom(request.getValidFrom());
        if (request.getValidTo() != null) entity.setValidTo(request.getValidTo());
        if (request.getPrimaryContactName() != null) entity.setPrimaryContactName(trimOrNull(request.getPrimaryContactName()));
        if (request.getPrimaryContactPhone() != null) entity.setPrimaryContactPhone(trimOrNull(request.getPrimaryContactPhone()));
        if (request.getPrimaryContactEmail() != null) entity.setPrimaryContactEmail(trimOrNull(request.getPrimaryContactEmail()));
        repository.save(entity);
        eventPublisher.publish(EventTypes.CORPORATE_UPDATED, payload(entity));
        if ("INACTIVE".equalsIgnoreCase(entity.getStatus())) {
            eventPublisher.publish(EventTypes.CORPORATE_DEACTIVATED, payload(entity));
        }
        return toResponse(entity);
    }

    private static Map<String, Object> payload(CorporateClient e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId() != null ? e.getId().toString() : null);
        m.put("code", e.getCode());
        m.put("name", e.getName());
        m.put("status", e.getStatus());
        return m;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private CorporateResponse toResponse(CorporateClient e) {
        CorporateResponse r = new CorporateResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setCode(e.getCode());
        r.setType(e.getType());
        r.setStatus(e.getStatus());
        r.setValidFrom(e.getValidFrom());
        r.setValidTo(e.getValidTo());
        r.setPrimaryContactName(e.getPrimaryContactName());
        r.setPrimaryContactPhone(e.getPrimaryContactPhone());
        r.setPrimaryContactEmail(e.getPrimaryContactEmail());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
