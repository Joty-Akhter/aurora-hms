package com.easyops.hospitalcorporatediscount.domain.discount;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.config.CacheConfig;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClientRepository;
import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountSchemeService {

    private final DiscountSchemeRepository schemeRepository;
    private final DiscountApprovalLevelRepository levelRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final CorporateDiscountEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional
    public DiscountSchemeResponse create(CreateDiscountSchemeRequest request) {
        String code = request.getCode().trim();
        if (schemeRepository.existsByCode(code)) {
            throw new IllegalStateException("Discount scheme with code already exists: " + code);
        }
        if (request.getCorporateClientId() != null && !corporateClientRepository.existsById(request.getCorporateClientId())) {
            throw new NoSuchElementException("Corporate not found: " + request.getCorporateClientId());
        }
        DiscountScheme entity = new DiscountScheme();
        entity.setCode(code);
        entity.setName(request.getName().trim());
        entity.setCorporateClientId(request.getCorporateClientId());
        entity.setVisitType(trimOrNull(request.getVisitType()));
        entity.setDepartmentId(request.getDepartmentId());
        entity.setServiceCode(trimOrNull(request.getServiceCode()));
        entity.setPatientCategory(trimOrNull(request.getPatientCategory()));
        entity.setDiscountType(request.getDiscountType().trim());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setMaxDiscountPercent(request.getMaxDiscountPercent());
        entity.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false);
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : "ACTIVE");
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        schemeRepository.save(entity);
        evictActiveSchemesCache();
        eventPublisher.publish(EventTypes.DISCOUNT_SCHEME_CREATED, payload(entity));
        return toResponse(entity);
    }

    @Cacheable(value = CacheConfig.CACHE_DISCOUNT_SCHEME, key = "#id")
    public DiscountSchemeDetailResponse getById(UUID id) {
        DiscountScheme entity = schemeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Discount scheme not found: " + id));
        DiscountSchemeDetailResponse detail = new DiscountSchemeDetailResponse();
        copyToResponse(entity, detail);
        List<DiscountApprovalLevelResponse> levels = levelRepository.findByDiscountSchemeIdOrderBySortOrderAsc(id)
                .stream()
                .map(this::toLevelResponse)
                .toList();
        detail.setApprovalLevels(levels);
        return detail;
    }

    @Cacheable(value = CacheConfig.CACHE_ACTIVE_DISCOUNT_SCHEMES,
            key = "#corporateClientId + '|' + (#visitType != null ? #visitType : '') + '|' + (#departmentId != null ? #departmentId.toString() : '')")
    public List<DiscountScheme> getActiveSchemesForEvaluation(UUID corporateClientId, String visitType, UUID departmentId) {
        Specification<DiscountScheme> spec = Specification
                .where(DiscountSchemeSpecifications.activeAndValidNow())
                .and(DiscountSchemeSpecifications.corporateOrGeneral(corporateClientId))
                .and(DiscountSchemeSpecifications.visitTypeMatches(visitType))
                .and(DiscountSchemeSpecifications.departmentMatches(departmentId));
        return schemeRepository.findAll(spec);
    }

    public PagedResponse<DiscountSchemeResponse> list(String code, UUID corporateClientId, String status, int page, int size) {
        Specification<DiscountScheme> spec = Specification
                .where(DiscountSchemeSpecifications.hasCode(code))
                .and(DiscountSchemeSpecifications.hasCorporateClientId(corporateClientId))
                .and(DiscountSchemeSpecifications.hasStatus(status));
        Page<DiscountScheme> p = schemeRepository.findAll(spec, PageRequest.of(page, size));
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
    public DiscountSchemeResponse update(UUID id, UpdateDiscountSchemeRequest request) {
        DiscountScheme entity = schemeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Discount scheme not found: " + id));
        if (request.getName() != null) entity.setName(request.getName().trim());
        if (request.getCorporateClientId() != null) {
            if (!corporateClientRepository.existsById(request.getCorporateClientId())) {
                throw new NoSuchElementException("Corporate not found: " + request.getCorporateClientId());
            }
            entity.setCorporateClientId(request.getCorporateClientId());
        }
        if (request.getVisitType() != null) entity.setVisitType(trimOrNull(request.getVisitType()));
        if (request.getDepartmentId() != null) entity.setDepartmentId(request.getDepartmentId());
        if (request.getServiceCode() != null) entity.setServiceCode(trimOrNull(request.getServiceCode()));
        if (request.getPatientCategory() != null) entity.setPatientCategory(trimOrNull(request.getPatientCategory()));
        if (request.getDiscountType() != null && !request.getDiscountType().isBlank()) entity.setDiscountType(request.getDiscountType().trim());
        if (request.getDiscountValue() != null) entity.setDiscountValue(request.getDiscountValue());
        if (request.getMaxDiscountAmount() != null) entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getMaxDiscountPercent() != null) entity.setMaxDiscountPercent(request.getMaxDiscountPercent());
        if (request.getRequiresApproval() != null) entity.setRequiresApproval(request.getRequiresApproval());
        if (request.getStatus() != null && !request.getStatus().isBlank()) entity.setStatus(request.getStatus().trim());
        if (request.getValidFrom() != null) entity.setValidFrom(request.getValidFrom());
        if (request.getValidTo() != null) entity.setValidTo(request.getValidTo());
        schemeRepository.save(entity);
        evictSchemeAndActiveSchemesCache(entity.getId());
        eventPublisher.publish(EventTypes.DISCOUNT_SCHEME_UPDATED, payload(entity));
        if ("INACTIVE".equalsIgnoreCase(entity.getStatus())) {
            eventPublisher.publish(EventTypes.DISCOUNT_SCHEME_DEACTIVATED, payload(entity));
        }
        return toResponse(entity);
    }

    private void evictActiveSchemesCache() {
        var cache = cacheManager.getCache(CacheConfig.CACHE_ACTIVE_DISCOUNT_SCHEMES);
        if (cache != null) cache.clear();
    }

    private void evictSchemeAndActiveSchemesCache(UUID schemeId) {
        var schemeCache = cacheManager.getCache(CacheConfig.CACHE_DISCOUNT_SCHEME);
        if (schemeCache != null && schemeId != null) schemeCache.evict(schemeId);
        evictActiveSchemesCache();
    }

    private static Map<String, Object> payload(DiscountScheme e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId() != null ? e.getId().toString() : null);
        m.put("code", e.getCode());
        m.put("name", e.getName());
        m.put("corporateClientId", e.getCorporateClientId() != null ? e.getCorporateClientId().toString() : null);
        m.put("status", e.getStatus());
        return m;
    }

    @Transactional
    public DiscountApprovalLevelResponse addApprovalLevel(UUID schemeId, CreateApprovalLevelRequest request) {
        if (!schemeRepository.existsById(schemeId)) {
            throw new NoSuchElementException("Discount scheme not found: " + schemeId);
        }
        DiscountApprovalLevel level = new DiscountApprovalLevel();
        level.setDiscountSchemeId(schemeId);
        level.setRoleOrGroupId(request.getRoleOrGroupId().trim());
        level.setMaxDiscountPercent(request.getMaxDiscountPercent());
        level.setMaxDiscountAmount(request.getMaxDiscountAmount());
        level.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        levelRepository.save(level);
        evictSchemeAndActiveSchemesCache(schemeId);
        return toLevelResponse(level);
    }

    public List<DiscountApprovalLevelResponse> getApprovalLevels(UUID schemeId) {
        if (!schemeRepository.existsById(schemeId)) {
            throw new NoSuchElementException("Discount scheme not found: " + schemeId);
        }
        return levelRepository.findByDiscountSchemeIdOrderBySortOrderAsc(schemeId)
                .stream()
                .map(this::toLevelResponse)
                .toList();
    }

    @Transactional
    public void deleteApprovalLevel(UUID schemeId, UUID levelId) {
        if (!levelRepository.existsByDiscountSchemeIdAndId(schemeId, levelId)) {
            throw new NoSuchElementException("Approval level not found: " + levelId + " for scheme: " + schemeId);
        }
        levelRepository.deleteById(levelId);
        evictSchemeAndActiveSchemesCache(schemeId);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private DiscountSchemeResponse toResponse(DiscountScheme e) {
        DiscountSchemeResponse r = new DiscountSchemeResponse();
        copyToResponse(e, r);
        return r;
    }

    private void copyToResponse(DiscountScheme e, DiscountSchemeResponse r) {
        r.setId(e.getId());
        r.setCode(e.getCode());
        r.setName(e.getName());
        r.setCorporateClientId(e.getCorporateClientId());
        r.setVisitType(e.getVisitType());
        r.setDepartmentId(e.getDepartmentId());
        r.setServiceCode(e.getServiceCode());
        r.setPatientCategory(e.getPatientCategory());
        r.setDiscountType(e.getDiscountType());
        r.setDiscountValue(e.getDiscountValue());
        r.setMaxDiscountAmount(e.getMaxDiscountAmount());
        r.setMaxDiscountPercent(e.getMaxDiscountPercent());
        r.setRequiresApproval(e.getRequiresApproval());
        r.setStatus(e.getStatus());
        r.setValidFrom(e.getValidFrom());
        r.setValidTo(e.getValidTo());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
    }

    private DiscountApprovalLevelResponse toLevelResponse(DiscountApprovalLevel e) {
        DiscountApprovalLevelResponse r = new DiscountApprovalLevelResponse();
        r.setId(e.getId());
        r.setDiscountSchemeId(e.getDiscountSchemeId());
        r.setRoleOrGroupId(e.getRoleOrGroupId());
        r.setMaxDiscountPercent(e.getMaxDiscountPercent());
        r.setMaxDiscountAmount(e.getMaxDiscountAmount());
        r.setSortOrder(e.getSortOrder());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
