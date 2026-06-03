package com.easyops.pharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Expense implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "territory_id", nullable = false)
    private UUID territoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", insertable = false, updatable = false)
    private Territory territory;
    
    @Column(name = "expense_category_id", nullable = false)
    private UUID expenseCategoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_id", insertable = false, updatable = false)
    private ExpenseCategory expenseCategory;
    
    @Column(name = "source_employee_id")
    private UUID sourceEmployeeId; // Optional - employee who provided expense info
    
    @Column(name = "expense_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal expenseAmount;
    
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month; // 1-12
    
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl; // Optional receipt upload
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}

