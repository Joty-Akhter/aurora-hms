package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PharmacyDirectory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmacyDirectoryRepository extends JpaRepository<PharmacyDirectory, UUID> {

    Optional<PharmacyDirectory> findByNpi(String npi);

    Optional<PharmacyDirectory> findByNcpdpId(String ncpdpId);

    /** Full-text name/city search with optional e-prescribing filter — used by the pharmacy picker. */
    @Query("""
        SELECT p FROM PharmacyDirectory p
        WHERE p.isActive = TRUE
          AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(p.city) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR p.npi LIKE CONCAT('%', :q, '%'))
          AND (:state IS NULL OR LOWER(p.state) = LOWER(:state))
          AND (:eprescribingOnly = FALSE OR p.isEprescribingCapable = TRUE)
        ORDER BY p.name ASC
        """)
    List<PharmacyDirectory> search(
            @Param("q") String q,
            @Param("state") String state,
            @Param("eprescribingOnly") boolean eprescribingOnly);

    /** FR-P3.5 staleness alert: entries not verified within the given threshold. */
    @Query("""
        SELECT p FROM PharmacyDirectory p
        WHERE p.isActive = TRUE
          AND (p.lastVerifiedAt IS NULL OR p.lastVerifiedAt < :threshold)
        ORDER BY p.lastVerifiedAt ASC NULLS FIRST
        """)
    List<PharmacyDirectory> findStale(@Param("threshold") OffsetDateTime threshold);

    List<PharmacyDirectory> findByIsActiveTrueOrderByNameAsc();
}
