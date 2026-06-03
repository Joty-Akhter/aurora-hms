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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_receipts", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductReceipt implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;
    
    @Column(name = "receipt_number", length = 100, unique = true)
    private String receiptNumber; // Auto-generated receipt number
    
    @Column(name = "total_value", precision = 19, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, COMPLETED
    
    @Column(name = "user_name", length = 200)
    private String userName; // Name of user creating the entry
    
    @Column(name = "user_designation", length = 100)
    private String userDesignation; // Designation of user creating the entry
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "productReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductReceiptLine> receiptLines;
    
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

