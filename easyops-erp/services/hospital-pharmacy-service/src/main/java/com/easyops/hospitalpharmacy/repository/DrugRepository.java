package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DrugRepository extends JpaRepository<Drug, UUID> {

    List<Drug> findByGenericNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(String genericName, String brandName);

    List<Drug> findByManufacturerAndActiveIsTrue(Manufacturer manufacturer);

    List<Drug> findByActiveIsTrue();

    @Query("""
        SELECT d FROM Drug d
        WHERE d.active = true
          AND (
            LOWER(d.genericName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(d.brandName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """)
    List<Drug> findActiveByGenericOrBrandNameContaining(@Param("q") String q);
}

