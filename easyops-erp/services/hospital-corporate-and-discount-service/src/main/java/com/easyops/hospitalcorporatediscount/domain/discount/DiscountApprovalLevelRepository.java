package com.easyops.hospitalcorporatediscount.domain.discount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountApprovalLevelRepository extends JpaRepository<DiscountApprovalLevel, UUID> {

    List<DiscountApprovalLevel> findByDiscountSchemeIdOrderBySortOrderAsc(UUID discountSchemeId);

    boolean existsByDiscountSchemeIdAndId(UUID discountSchemeId, UUID id);
}
