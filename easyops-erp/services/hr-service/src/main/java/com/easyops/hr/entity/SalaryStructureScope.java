package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Optional structure assignment by organization with overrides by department or location (SS-25).
 * When both departmentId and locationId are null, the assignment is org-wide.
 */
@Entity
@Table(name = "salary_structure_scope", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructureScope {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "salary_structure_id", nullable = false)
    private UUID salaryStructureId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
