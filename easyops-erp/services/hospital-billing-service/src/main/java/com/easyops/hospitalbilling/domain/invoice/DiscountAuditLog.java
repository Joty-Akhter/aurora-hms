package com.easyops.hospitalbilling.domain.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit record for discount application (who, which card/corporate, what discount, which invoice/line).
 * Filled when invoice is created from charge lines with discounts, or when invoice-level discount is added.
 */
@Entity
@Table(name = "discount_audit_log")
public class DiscountAuditLog {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "charge_line_id")
    private UUID chargeLineId;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "corporate_contract_id")
    private UUID corporateContractId;

    @Column(name = "card_reference", length = 100)
    private String cardReference;

    @Column(name = "applied_by_user_id")
    private UUID appliedByUserId;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getChargeLineId() {
        return chargeLineId;
    }

    public void setChargeLineId(UUID chargeLineId) {
        this.chargeLineId = chargeLineId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public UUID getCorporateContractId() {
        return corporateContractId;
    }

    public void setCorporateContractId(UUID corporateContractId) {
        this.corporateContractId = corporateContractId;
    }

    public String getCardReference() {
        return cardReference;
    }

    public void setCardReference(String cardReference) {
        this.cardReference = cardReference;
    }

    public UUID getAppliedByUserId() {
        return appliedByUserId;
    }

    public void setAppliedByUserId(UUID appliedByUserId) {
        this.appliedByUserId = appliedByUserId;
    }

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(OffsetDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
