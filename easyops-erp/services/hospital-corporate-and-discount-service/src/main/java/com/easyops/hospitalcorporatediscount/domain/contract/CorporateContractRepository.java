package com.easyops.hospitalcorporatediscount.domain.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CorporateContractRepository extends JpaRepository<CorporateContract, UUID>, JpaSpecificationExecutor<CorporateContract> {

    Page<CorporateContract> findByCorporateClientId(UUID corporateClientId, Pageable pageable);

    Optional<CorporateContract> findByCorporateClientIdAndContractCode(UUID corporateClientId, String contractCode);

    boolean existsByCorporateClientIdAndContractCode(UUID corporateClientId, String contractCode);
}
