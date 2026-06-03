package com.easyops.ar.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ar_credit_note_lines", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ARCreditNoteLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    @JsonBackReference
    private ARCreditNote creditNote;
    
    @Column(nullable = false)
    private Integer lineNumber;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    
    @Column
    private UUID accountId;  // Revenue account to credit
}

