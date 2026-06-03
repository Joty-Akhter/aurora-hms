package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DispenseOrderRepository extends JpaRepository<DispenseOrder, UUID> {

    boolean existsByPrescriptionId(UUID prescriptionId);

    List<DispenseOrder> findByPatientId(UUID patientId);

    List<DispenseOrder> findByVisitId(UUID visitId);

    List<DispenseOrder> findByPharmacyLocation(PharmacyLocation pharmacyLocation);

    List<DispenseOrder> findByPharmacyLocationAndStatus(PharmacyLocation pharmacyLocation, DispenseOrder.Status status);
}

