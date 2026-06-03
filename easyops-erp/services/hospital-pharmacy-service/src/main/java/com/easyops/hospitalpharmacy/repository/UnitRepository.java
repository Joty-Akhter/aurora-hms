package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

    Optional<Unit> findByAbbreviationIgnoreCase(String abbreviation);

    List<Unit> findByBaseUnitIsNull();
}
