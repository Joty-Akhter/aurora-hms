package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.BomVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BomVersionRepository extends JpaRepository<BomVersion, UUID> {

    @Query("SELECT bv FROM BomVersion bv WHERE bv.bomHeader.bomId = :bomId ORDER BY bv.versionNumber DESC")
    List<BomVersion> findByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT bv FROM BomVersion bv WHERE bv.bomHeader.bomId = :bomId AND bv.versionNumber = :versionNumber")
    Optional<BomVersion> findByBomIdAndVersionNumber(@Param("bomId") UUID bomId, @Param("versionNumber") Integer versionNumber);

    @Query("SELECT bv FROM BomVersion bv WHERE bv.bomHeader.bomId = :bomId ORDER BY bv.versionNumber DESC LIMIT 1")
    Optional<BomVersion> findLatestVersionByBomId(@Param("bomId") UUID bomId);

    @Query("SELECT bv FROM BomVersion bv WHERE bv.bomHeader.bomId = :bomId AND bv.status = :status " +
           "ORDER BY bv.versionNumber DESC")
    List<BomVersion> findByBomIdAndStatus(@Param("bomId") UUID bomId, @Param("status") String status);

    @Query("SELECT COUNT(bv) FROM BomVersion bv WHERE bv.bomHeader.bomId = :bomId")
    long countVersionsByBomId(@Param("bomId") UUID bomId);
}

