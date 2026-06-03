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
@Table(name = "products", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    // Basic Information
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;
    
    @Column(name = "barcode", length = 100)
    private String barcode;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "short_description", length = 500)
    private String shortDescription;
    
    @Column(name = "category_id")
    private UUID categoryId;
    
    @Column(name = "brand", length = 100)
    private String brand;
    
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;
    
    @Column(name = "model_number", length = 100)
    private String modelNumber;
    
    // Product Type
    @Column(name = "product_type", length = 50)
    private String productType = "GOODS"; // GOODS, SERVICE, BUNDLE
    
    @Column(name = "item_type", length = 50)
    private String itemType = "STOCKABLE"; // STOCKABLE, NON_STOCKABLE, SERVICE
    
    // Pricing
    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice = BigDecimal.ZERO;
    
    @Column(name = "selling_price", precision = 19, scale = 4)
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    
    @Column(name = "wholesale_price", precision = 19, scale = 4)
    private BigDecimal wholesalePrice;
    
    @Column(name = "retail_price", precision = 19, scale = 4)
    private BigDecimal retailPrice; // MRP - Maximum Retail Price
    
    @Column(name = "currency", length = 3)
    private String currency = "BDT"; // Bangladeshi Taka
    
    // Alien Pharma specific fields
    // TP = Trade Price = wholesalePrice (already exists above)
    // MRP = Maximum Retail Price = retailPrice (already exists above)
    
    @Column(name = "pack_size", precision = 10, scale = 2)
    private BigDecimal packSize = BigDecimal.ONE; // Number of units per pack
    
    // Stock Information
    @Column(name = "uom", length = 50)
    private String uom = "PCS";
    
    @Column(name = "reorder_level", precision = 19, scale = 4)
    private BigDecimal reorderLevel = BigDecimal.ZERO;
    
    @Column(name = "min_stock_level", precision = 19, scale = 4)
    private BigDecimal minStockLevel = BigDecimal.ZERO;
    
    @Column(name = "max_stock_level", precision = 19, scale = 4)
    private BigDecimal maxStockLevel;
    
    @Column(name = "safety_stock", precision = 19, scale = 4)
    private BigDecimal safetyStock = BigDecimal.ZERO;
    
    // Physical Attributes
    @Column(name = "weight", precision = 10, scale = 4)
    private BigDecimal weight;
    
    @Column(name = "weight_unit", length = 10)
    private String weightUnit = "KG";
    
    @Column(name = "length", precision = 10, scale = 4)
    private BigDecimal length;
    
    @Column(name = "width", precision = 10, scale = 4)
    private BigDecimal width;
    
    @Column(name = "height", precision = 10, scale = 4)
    private BigDecimal height;
    
    @Column(name = "dimension_unit", length = 10)
    private String dimensionUnit = "CM";
    
    // Supplier
    @Column(name = "primary_supplier_id")
    private UUID primarySupplierId;
    
    @Column(name = "supplier_sku", length = 100)
    private String supplierSku;
    
    @Column(name = "lead_time_days")
    private Integer leadTimeDays = 0;
    
    // Accounting
    @Column(name = "inventory_gl_account_id")
    private UUID inventoryGlAccountId;
    
    @Column(name = "cogs_gl_account_id")
    private UUID cogsGlAccountId;
    
    @Column(name = "revenue_gl_account_id")
    private UUID revenueGlAccountId;
    
    // Tax
    @Column(name = "tax_category", length = 50)
    private String taxCategory;
    
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    @Column(name = "hs_code", length = 50)
    private String hsCode;
    
    @Column(name = "country_of_origin", length = 2)
    private String countryOfOrigin;
    
    // Tracking
    @Column(name = "track_inventory")
    private Boolean trackInventory = true;
    
    @Column(name = "track_batch")
    private Boolean trackBatch = false;
    
    @Column(name = "track_serial")
    private Boolean trackSerial = false;
    
    @Column(name = "has_expiry")
    private Boolean hasExpiry = false;
    
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;
    
    // Status
    @Column(name = "status", length = 50)
    private String status = "ACTIVE";
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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

