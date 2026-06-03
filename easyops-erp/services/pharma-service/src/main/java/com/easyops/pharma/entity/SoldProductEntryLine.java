package com.easyops.pharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "sold_product_entry_lines", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SoldProductEntryLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "sold_product_entry_id", nullable = false)
    private UUID soldProductEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_product_entry_id", insertable = false, updatable = false)
    @JsonIgnore
    private SoldProductEntry soldProductEntry;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "tp_with_vat", precision = 19, scale = 4)
    private BigDecimal tpWithVat;

    @Column(name = "quantity_sold", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantitySold;

    @Column(name = "current_outstanding_quantity", precision = 19, scale = 4)
    private BigDecimal currentOutstandingQuantity;

    @Column(name = "product_amount", precision = 19, scale = 2)
    private BigDecimal productAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
