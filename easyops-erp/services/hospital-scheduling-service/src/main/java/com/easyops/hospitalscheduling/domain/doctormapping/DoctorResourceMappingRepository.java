package com.easyops.hospitalscheduling.domain.doctormapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorResourceMappingRepository extends JpaRepository<DoctorResourceMapping, UUID>, JpaSpecificationExecutor<DoctorResourceMapping> {
    List<DoctorResourceMapping> findByDoctorUserIdAndStatus(UUID doctorUserId, String status);
    List<DoctorResourceMapping> findByDoctorUserIdAndBranchIdAndStatus(UUID doctorUserId, UUID branchId, String status);
    Optional<DoctorResourceMapping> findByDoctorUserIdAndBranchIdIsNullAndStatus(UUID doctorUserId, String status);
    List<DoctorResourceMapping> findByResourceId(UUID resourceId);
}
