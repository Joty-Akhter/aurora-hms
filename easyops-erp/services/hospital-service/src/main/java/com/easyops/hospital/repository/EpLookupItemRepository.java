package com.easyops.hospital.repository;

import com.easyops.hospital.entity.EpLookupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpLookupItemRepository extends JpaRepository<EpLookupItem, UUID> {

    List<EpLookupItem> findByCategoryAndActiveTrueOrderByDisplayOrderAsc(String category);

    List<EpLookupItem> findByActiveTrueOrderByCategoryAscDisplayOrderAsc();

    /**
     * Resolve ADVICE by normalized text. Prefer active rows when legacy duplicates exist (same norm, mixed active flags).
     */
    @Query(value = """
            SELECT * FROM ehr.ep_lookup_items e
            WHERE e.category = 'ADVICE'
              AND regexp_replace(lower(trim(e.value)), '\\s+', ' ', 'g') = :norm
            ORDER BY e.is_active DESC, e.display_order ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<EpLookupItem> findAdviceByNormalizedPreferActive(@Param("norm") String normalizedLowerSpaces);

    /** Active rows only so deactivated ADVICE lines do not inflate display_order for new inserts. */
    @Query("SELECT COALESCE(MAX(e.displayOrder), 0) FROM EpLookupItem e WHERE e.category = :category AND e.active = true")
    Integer maxDisplayOrderForCategory(@Param("category") String category);
}
