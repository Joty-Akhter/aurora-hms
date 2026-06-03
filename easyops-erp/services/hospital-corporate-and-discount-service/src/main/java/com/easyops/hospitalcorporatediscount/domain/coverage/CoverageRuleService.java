package com.easyops.hospitalcorporatediscount.domain.coverage;

import com.easyops.hospitalcorporatediscount.api.dto.CoverageRuleResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCoverageRuleRequest;
import com.easyops.hospitalcorporatediscount.config.CacheConfig;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractRepository;
import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoverageRuleService {

    private final CoverageRuleRepository repository;
    private final CorporateContractRepository corporateContractRepository;
    private final CoverageRuleLoader coverageRuleLoader;
    private final CorporateDiscountEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional
    public CoverageRuleResponse create(UUID contractId, CreateCoverageRuleRequest request) {
        if (!corporateContractRepository.existsById(contractId)) {
            throw new NoSuchElementException("Contract not found: " + contractId);
        }
        CoverageRule entity = new CoverageRule();
        entity.setCorporateContractId(contractId);
        entity.setScopeType(request.getScopeType().trim());
        entity.setScopeValue(request.getScopeValue().trim());
        entity.setCoveragePercent(request.getCoveragePercent());
        entity.setMaxAmount(request.getMaxAmount());
        entity.setCoPayPercent(request.getCoPayPercent());
        entity.setDeductibleAmount(request.getDeductibleAmount());
        entity.setApplicableVisitTypes(trimOrNull(request.getApplicableVisitTypes()));
        repository.save(entity);
        evictCoverageRulesCache(contractId);
        eventPublisher.publish(EventTypes.COVERAGE_RULE_CREATED, payload(entity));
        return toResponse(entity);
    }

    public List<CoverageRuleResponse> listByContractId(UUID contractId) {
        return coverageRuleLoader.loadForContract(contractId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID id) {
        CoverageRule entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coverage rule not found: " + id));
        UUID contractId = entity.getCorporateContractId();
        repository.deleteById(id);
        evictCoverageRulesCache(contractId);
        eventPublisher.publish(EventTypes.COVERAGE_RULE_DELETED, payload(entity));
    }

    private void evictCoverageRulesCache(UUID contractId) {
        var cache = cacheManager.getCache(CacheConfig.CACHE_COVERAGE_RULES);
        if (cache != null && contractId != null) {
            cache.evict(contractId);
        }
    }

    private static Map<String, Object> payload(CoverageRule e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId() != null ? e.getId().toString() : null);
        m.put("corporateContractId", e.getCorporateContractId() != null ? e.getCorporateContractId().toString() : null);
        m.put("scopeType", e.getScopeType());
        m.put("scopeValue", e.getScopeValue());
        return m;
    }

    private CoverageRuleResponse toResponse(CoverageRule e) {
        CoverageRuleResponse r = new CoverageRuleResponse();
        r.setId(e.getId());
        r.setCorporateContractId(e.getCorporateContractId());
        r.setScopeType(e.getScopeType());
        r.setScopeValue(e.getScopeValue());
        r.setCoveragePercent(e.getCoveragePercent());
        r.setMaxAmount(e.getMaxAmount());
        r.setCoPayPercent(e.getCoPayPercent());
        r.setDeductibleAmount(e.getDeductibleAmount());
        r.setApplicableVisitTypes(e.getApplicableVisitTypes());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
