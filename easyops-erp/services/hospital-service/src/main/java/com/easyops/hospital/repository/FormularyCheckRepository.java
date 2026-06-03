package com.easyops.hospital.repository;

import com.easyops.hospital.entity.FormularyCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormularyCheckRepository extends JpaRepository<FormularyCheck, UUID> {
    List<FormularyCheck> findByPrescriptionPrescriptionId(UUID prescriptionId);
    List<FormularyCheck> findByPrescriptionPrescriptionIdOrderByCheckDateDesc(UUID prescriptionId);
    FormularyCheck findFirstByPrescriptionPrescriptionIdOrderByCheckDateDesc(UUID prescriptionId);
}
