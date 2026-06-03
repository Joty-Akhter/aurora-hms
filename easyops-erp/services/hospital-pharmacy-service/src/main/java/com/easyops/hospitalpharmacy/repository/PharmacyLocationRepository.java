package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PharmacyLocationRepository extends JpaRepository<PharmacyLocation, UUID> {

    List<PharmacyLocation> findByActiveTrue();
}

