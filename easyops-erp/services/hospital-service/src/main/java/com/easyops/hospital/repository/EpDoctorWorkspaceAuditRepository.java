package com.easyops.hospital.repository;

import com.easyops.hospital.entity.EpDoctorWorkspaceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EpDoctorWorkspaceAuditRepository extends JpaRepository<EpDoctorWorkspaceAudit, UUID> {
}
