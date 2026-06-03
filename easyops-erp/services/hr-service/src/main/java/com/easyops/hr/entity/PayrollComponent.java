package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_components", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PayrollComponent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "payroll_component_id")
    private UUID payrollComponentId;
    
    @Column(name = "payroll_detail_id", nullable = false)
    private UUID payrollDetailId;
    
    @Column(name = "component_id", nullable = false)
    private UUID componentId;
    
    @Column(name = "component_type", nullable = false, length = 50)
    private String componentType;

    /** ES-26: Order for payslip display; from salary_component.displayOrder at calculation time. */
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "is_taxable")
    private Boolean isTaxable = true;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

