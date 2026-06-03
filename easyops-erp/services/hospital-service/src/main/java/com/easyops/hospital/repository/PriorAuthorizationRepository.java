package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PriorAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriorAuthorizationRepository extends JpaRepository<PriorAuthorization, UUID> {
    List<PriorAuthorization> findByPrescriptionPrescriptionId(UUID prescriptionId);
    Optional<PriorAuthorization> findByPriorAuthNumber(String priorAuthNumber);
    List<PriorAuthorization> findByStatus(PriorAuthorization.PriorAuthStatus status);
    List<PriorAuthorization> findByPrescriptionPrescriptionIdAndStatus(UUID prescriptionId, PriorAuthorization.PriorAuthStatus status);
}
