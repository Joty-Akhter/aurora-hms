package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.BomLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BomLineRepository extends JpaRepository<BomLine, UUID> {

    @Query("SELECT bl FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId ORDER BY bl.sequenceNumber")
    List<BomLine> findByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT bl FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId AND bl.parentLine IS NULL " +
           "ORDER BY bl.sequenceNumber")
    List<BomLine> findTopLevelComponentsByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT bl FROM BomLine bl WHERE bl.parentLine.bomLineId = :parentLineId ORDER BY bl.sequenceNumber")
    List<BomLine> findByParentLineId(@Param("parentLineId") UUID parentLineId);

    @Query("SELECT bl FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId AND bl.componentId = :componentId")
    List<BomLine> findByBomIdAndComponentId(@Param("bomId") UUID bomId, @Param("componentId") UUID componentId);

    @Query("SELECT bl FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId AND bl.componentType = :componentType " +
           "ORDER BY bl.sequenceNumber")
    List<BomLine> findByBomIdAndComponentType(@Param("bomId") UUID bomId, @Param("componentType") String componentType);

    @Query("SELECT bl FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId AND bl.levelNumber = :levelNumber " +
           "ORDER BY bl.sequenceNumber")
    List<BomLine> findByBomIdAndLevel(@Param("bomId") UUID bomId, @Param("levelNumber") Integer levelNumber);

    @Query("SELECT COUNT(bl) FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId")
    long countByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT SUM(bl.unitCost * bl.quantityPerUnit) FROM BomLine bl WHERE bl.bomHeader.bomId = :bomId " +
           "AND bl.parentLine IS NULL")
    Double calculateDirectMaterialCost(@Param("bomId") UUID bomId);
}

