package com.easyops.pharma.repository;

import com.easyops.pharma.entity.SoldProductEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SoldProductEntryRepository extends JpaRepository<SoldProductEntry, UUID> {

    List<SoldProductEntry> findByOrganizationId(UUID organizationId);

    List<SoldProductEntry> findByTerritoryId(UUID territoryId);

    List<SoldProductEntry> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);

    @Query("SELECT COALESCE(SUM(e.totalProductAmount), 0) FROM SoldProductEntry e " +
           "WHERE e.territoryId = :territoryId AND e.year = :year AND e.month = :month AND e.status = 'COMPLETED'")
    BigDecimal getTotalCoveredAmountForTerritoryAndMonth(@Param("territoryId") UUID territoryId,
                                                         @Param("year") Integer year,
                                                         @Param("month") Integer month);

    @Query("SELECT DISTINCT e FROM SoldProductEntry e LEFT JOIN FETCH e.lines WHERE e.id = :id")
    java.util.Optional<SoldProductEntry> findByIdWithLines(@Param("id") UUID id);
}
