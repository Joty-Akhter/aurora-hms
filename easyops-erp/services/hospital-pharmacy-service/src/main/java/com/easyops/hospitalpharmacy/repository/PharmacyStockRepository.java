package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.PharmacyStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, UUID> {

    List<PharmacyStock> findByPharmacyLocation(PharmacyLocation pharmacyLocation);

    Optional<PharmacyStock> findByPharmacyLocationAndDrugAndBatchNumber(
            PharmacyLocation pharmacyLocation,
            Drug drug,
            String batchNumber
    );
}

