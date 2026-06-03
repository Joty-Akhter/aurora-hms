package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "prescription_transmissions", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionTransmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transmission_id")
    private UUID transmissionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    // Transmission Information
    @Column(name = "transmission_date", nullable = false)
    private LocalDateTime transmissionDate;
    
    @Column(name = "transmission_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransmissionStatus transmissionStatus = TransmissionStatus.PENDING;
    
    @Column(name = "transmission_method", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransmissionMethod transmissionMethod = TransmissionMethod.E_PRESCRIBING;
    
    // Network Information
    @Column(name = "network_name", length = 100)
    private String networkName; // Surescripts, RelayHealth, etc.
    
    @Column(name = "network_id", length = 100)
    private String networkId; // Network-specific ID
    
    @Column(name = "network_transaction_id", length = 200)
    private String networkTransactionId; // Transaction ID from network
    
    // Pharmacy Information
    @Column(name = "pharmacy_id")
    private UUID pharmacyId;
    
    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;
    
    @Column(name = "pharmacy_npi", length = 20)
    private String pharmacyNpi;
    
    @Column(name = "pharmacy_dea", length = 20)
    private String pharmacyDea;
    
    @Column(name = "pharmacy_address_line1", length = 255)
    private String pharmacyAddressLine1;
    
    @Column(name = "pharmacy_address_line2", length = 255)
    private String pharmacyAddressLine2;
    
    @Column(name = "pharmacy_city", length = 100)
    private String pharmacyCity;
    
    @Column(name = "pharmacy_state", length = 50)
    private String pharmacyState;
    
    @Column(name = "pharmacy_zip", length = 20)
    private String pharmacyZip;
    
    @Column(name = "pharmacy_phone", length = 50)
    private String pharmacyPhone;
    
    @Column(name = "pharmacy_fax", length = 50)
    private String pharmacyFax;
    
    // Transmission Results
    @Column(name = "transmission_success")
    @Builder.Default
    private Boolean transmissionSuccess = false;
    
    @Column(name = "confirmation_received")
    @Builder.Default
    private Boolean confirmationReceived = false;
    
    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate;
    
    @Column(name = "confirmation_message", columnDefinition = "TEXT")
    private String confirmationMessage;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "last_retry_date")
    private LocalDateTime lastRetryDate;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    // Network Response Data (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "network_response", columnDefinition = "jsonb")
    private Map<String, Object> networkResponse;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transmission_payload", columnDefinition = "jsonb")
    private Map<String, Object> transmissionPayload; // What was sent to network
    
    // FR-P3.6: NCPDP SCRIPT 2017071 XML audit storage
    /** Full validated NCPDP SCRIPT NewRx XML string sent (or built for simulation). */
    @Column(name = "ncpdp_xml_payload", columnDefinition = "TEXT")
    private String ncpdpXmlPayload;

    /** MessageID from the NCPDP Header — used for cross-reference with the e-prescribing network. */
    @Column(name = "ncpdp_message_id", length = 100)
    private String ncpdpMessageId;

    // Fill Status Information
    @Column(name = "fill_status", length = 50)
    @Enumerated(EnumType.STRING)
    private FillStatus fillStatus;
    
    @Column(name = "fill_status_date")
    private LocalDateTime fillStatusDate;
    
    @Column(name = "fill_status_message", columnDefinition = "TEXT")
    private String fillStatusMessage;

    /** {@code NETWORK_WEBHOOK} vs {@code IN_HOUSE_PHARMACY} — avoids double-applying fill updates. */
    @Column(name = "fill_status_source", length = 32)
    @Enumerated(EnumType.STRING)
    private FillStatusSource fillStatusSource;
    
    @Column(name = "filled_date")
    private LocalDateTime filledDate;
    
    @Column(name = "picked_up_date")
    private LocalDateTime pickedUpDate;
    
    @Column(name = "cancelled_by_pharmacy")
    @Builder.Default
    private Boolean cancelledByPharmacy = false;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    // Provider Information
    @Column(name = "transmitted_by", nullable = false)
    private UUID transmittedBy;
    
    @Column(name = "transmitted_by_name", length = 200)
    private String transmittedByName;
    
    @Column(name = "transmitted_by_npi", length = 20)
    private String transmittedByNpi;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum TransmissionStatus {
        PENDING,           // Transmission queued
        IN_PROGRESS,       // Transmission in progress
        SENT,              // Successfully sent to network
        CONFIRMED,         // Confirmed received by pharmacy
        FAILED,            // Transmission failed
        CANCELLED,         // Transmission cancelled
        EXPIRED            // Transmission expired
    }
    
    public enum TransmissionMethod {
        E_PRESCRIBING,     // Electronic prescribing network
        FAX,               // Fax transmission
        PHONE,             // Phone transmission
        PAPER,             // Paper prescription
        DIRECT_MESSAGE,   // Direct messaging
        API                // Direct API integration
    }
    
    public enum FillStatus {
        PENDING,           // Prescription sent, awaiting fill
        IN_PROGRESS,       // Pharmacy is processing
        FILLED,            // Prescription filled
        PARTIALLY_FILLED,  // Partially filled
        PICKED_UP,         // Patient picked up
        CANCELLED,         // Cancelled by pharmacy
        ON_HOLD,           // On hold at pharmacy
        OUT_OF_STOCK,      // Out of stock
        REJECTED,          // Rejected by pharmacy
        EXPIRED            // Prescription expired
    }
}
