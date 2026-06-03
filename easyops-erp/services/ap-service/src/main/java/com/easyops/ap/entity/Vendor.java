package com.easyops.ap.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vendor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "vendor_code", nullable = false, length = 50)
    private String vendorCode;
    
    @Column(name = "vendor_name", nullable = false)
    private String vendorName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "payment_terms")
    private Integer paymentTerms = 30;
    
    @Column(name = "tax_id", length = 100)
    private String taxId;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

