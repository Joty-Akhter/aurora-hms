package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "epf_exit_cases", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfExitCase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "exit_case_id")
    private UUID exitCaseId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "source_epf_account_id", nullable = false)
    private UUID sourceEpfAccountId;

    @Column(name = "target_epf_account_id")
    private UUID targetEpfAccountId;

    @Column(name = "exit_type", nullable = false, length = 40)
    private String exitType; // transfer, settlement, close_only

    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "initiated"; // initiated, transfer_pending, settlement_pending, completed, cancelled

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "withdrawal_id")
    private UUID withdrawalId;

    @Column(name = "completion_reference", length = 120)
    private String completionReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
