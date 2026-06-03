package com.easyops.hr.repository;

import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayrollTimeAttendancePolicyRepository extends JpaRepository<PayrollTimeAttendancePolicy, UUID> {
}
