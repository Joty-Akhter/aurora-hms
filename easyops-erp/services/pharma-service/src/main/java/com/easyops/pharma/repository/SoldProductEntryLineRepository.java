package com.easyops.pharma.repository;

import com.easyops.pharma.entity.SoldProductEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SoldProductEntryLineRepository extends JpaRepository<SoldProductEntryLine, UUID> {

    List<SoldProductEntryLine> findBySoldProductEntryId(UUID soldProductEntryId);

    List<SoldProductEntryLine> findByProductId(UUID productId);

    @Query("SELECT COALESCE(SUM(l.quantitySold), 0) FROM SoldProductEntryLine l JOIN l.soldProductEntry e WHERE e.territoryId = :territoryId AND l.productId = :productId")
    BigDecimal sumQuantitySoldByTerritoryAndProduct(@Param("territoryId") UUID territoryId, @Param("productId") UUID productId);
}
