package com.easyops.hospitalcorporatediscount.domain.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CorporateTariffRepository extends JpaRepository<CorporateTariff, UUID> {

    List<CorporateTariff> findByCorporateContractIdOrderByScopeTypeAscScopeValueAsc(UUID corporateContractId);
}
