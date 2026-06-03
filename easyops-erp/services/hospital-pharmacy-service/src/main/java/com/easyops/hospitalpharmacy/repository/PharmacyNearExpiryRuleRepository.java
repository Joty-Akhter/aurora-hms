package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyNearExpiryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PharmacyNearExpiryRuleRepository extends JpaRepository<PharmacyNearExpiryRule, UUID> {

    @Query("SELECT r FROM PharmacyNearExpiryRule r WHERE r.effectiveFrom <= :onDate AND (r.effectiveTo IS NULL OR r.effectiveTo >= :onDate) ORDER BY r.effectiveFrom DESC")
    List<PharmacyNearExpiryRule> findEffectiveOnOrBefore(@Param("onDate") LocalDate onDate);
}
