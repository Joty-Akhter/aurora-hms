package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientIdentityCardAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface PatientIdentityCardAuditLogRepository extends JpaRepository<PatientIdentityCardAuditLog, UUID> {
    long countByPatientIdAndActionIn(UUID patientId, Collection<String> actions);
}
