package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "department_leave_approvers", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentLeaveApprover {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "approver_employee_id", nullable = false)
    private UUID approverEmployeeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
