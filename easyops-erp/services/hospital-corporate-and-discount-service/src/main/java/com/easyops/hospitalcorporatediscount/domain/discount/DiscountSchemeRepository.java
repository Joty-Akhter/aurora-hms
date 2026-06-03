package com.easyops.hospitalcorporatediscount.domain.discount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface DiscountSchemeRepository extends JpaRepository<DiscountScheme, UUID>, JpaSpecificationExecutor<DiscountScheme> {

    Optional<DiscountScheme> findByCode(String code);

    boolean existsByCode(String code);
}
