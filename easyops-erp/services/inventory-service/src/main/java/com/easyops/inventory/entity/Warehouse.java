package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warehouses", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Warehouse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "code", nullable = false, length = 50)
    private String code;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "warehouse_type", length = 50)
    private String warehouseType = "MAIN";
    
    // Address
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 2)
    private String country;
    
    // Contact
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "manager_id")
    private UUID managerId;
    
    // Capacity
    @Column(name = "total_capacity", precision = 19, scale = 4)
    private BigDecimal totalCapacity;
    
    @Column(name = "capacity_unit", length = 20)
    private String capacityUnit = "M3";
    
    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "status", length = 50)
    private String status = "OPERATIONAL";
    
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

