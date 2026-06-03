package com.easyops.ar.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;
    
    @Column(name = "customer_name", nullable = false)
    @JsonProperty("customerName")
    private String customerName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    @Column(name = "current_balance", precision = 19, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "payment_terms")
    private Integer paymentTerms = 30;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Computed property - not stored in database
    @Transient
    public Boolean isCreditLimitExceeded() {
        if (creditLimit == null || currentBalance == null) {
            return false;
        }
        return currentBalance.compareTo(creditLimit) > 0;
    }
}

