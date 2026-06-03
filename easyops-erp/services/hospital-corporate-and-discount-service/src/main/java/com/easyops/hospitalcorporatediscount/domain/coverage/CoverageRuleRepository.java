package com.easyops.hospitalcorporatediscount.domain.coverage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoverageRuleRepository extends JpaRepository<CoverageRule, UUID> {

    List<CoverageRule> findByCorporateContractIdOrderByScopeTypeAscScopeValueAsc(UUID corporateContractId);
}
