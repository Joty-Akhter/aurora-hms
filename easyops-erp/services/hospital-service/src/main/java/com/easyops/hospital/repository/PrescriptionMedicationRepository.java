package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionMedicationRepository extends JpaRepository<PrescriptionMedication, UUID> {

    List<PrescriptionMedication> findByPrescriptionPrescriptionIdOrderByLineNumberAsc(UUID prescriptionId);
}
