package com.easyops.hr.repository;

import com.easyops.hr.entity.DepartmentLeaveApprover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentLeaveApproverRepository extends JpaRepository<DepartmentLeaveApprover, UUID> {

    List<DepartmentLeaveApprover> findByDepartmentIdOrderByStepOrderAsc(UUID departmentId);

    void deleteByDepartmentId(UUID departmentId);
}
