package com.easyops.ap.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ap_bill_lines", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APBillLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @JsonBackReference
    private APBill bill;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;
    
    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;
    
    @Column(name = "line_total", precision = 19, scale = 4)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

