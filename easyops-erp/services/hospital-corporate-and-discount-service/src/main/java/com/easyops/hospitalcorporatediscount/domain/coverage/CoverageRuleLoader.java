package com.easyops.hospitalcorporatediscount.domain.coverage;

import com.easyops.hospitalcorporatediscount.config.CacheConfig;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Loads coverage rules with {@link Cacheable}; kept separate from {@link CoverageRuleService}
 * so list and evaluation paths both use the cache (avoids self-invocation skipping the proxy).
 */
@Service
@RequiredArgsConstructor
public class CoverageRuleLoader {

    private final CoverageRuleRepository repository;
    private final CorporateContractRepository corporateContractRepository;

    @Cacheable(value = CacheConfig.CACHE_COVERAGE_RULES, key = "#contractId")
    public List<CoverageRule> loadForContract(UUID contractId) {
        if (!corporateContractRepository.existsById(contractId)) {
            throw new NoSuchElementException("Contract not found: " + contractId);
        }
        return repository.findByCorporateContractIdOrderByScopeTypeAscScopeValueAsc(contractId);
    }
}
