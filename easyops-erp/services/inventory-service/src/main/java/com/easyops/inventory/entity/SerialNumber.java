package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "serial_numbers", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SerialNumber implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;
    
    @Column(name = "warehouse_id")
    private UUID warehouseId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    
    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;
    
    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;
    
    @Column(name = "warranty_period_months")
    private Integer warrantyPeriodMonths;
    
    @Column(name = "status", length = 50)
    private String status = "IN_STOCK"; // IN_STOCK, ALLOCATED, SOLD, IN_SERVICE, RETURNED, SCRAPPED
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(name = "sale_date")
    private LocalDate saleDate;
    
    @Column(name = "sale_invoice_number", length = 50)
    private String saleInvoiceNumber;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

