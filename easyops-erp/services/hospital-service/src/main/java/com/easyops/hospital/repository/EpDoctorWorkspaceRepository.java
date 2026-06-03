package com.easyops.hospital.repository;

import com.easyops.hospital.entity.EpDoctorWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpDoctorWorkspaceRepository extends JpaRepository<EpDoctorWorkspace, UUID> {

    Optional<EpDoctorWorkspace> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}
