package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanCategoryRepository extends JpaRepository<LoanCategory, UUID> {

    long countByOrganizationId(UUID organizationId);

    /** Sort by {@code sortOrder}, then {@code code} (Spring Data: two properties, not "AndCode"). */
    List<LoanCategory> findByOrganizationIdOrderBySortOrderAscCodeAsc(UUID organizationId);

    List<LoanCategory> findByOrganizationIdAndCategoryIdIn(UUID organizationId, Collection<UUID> categoryIds);

    Optional<LoanCategory> findByCategoryIdAndOrganizationId(UUID categoryId, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
