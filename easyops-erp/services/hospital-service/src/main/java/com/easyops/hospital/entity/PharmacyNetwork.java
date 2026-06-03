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
@Table(name = "pharmacy_networks", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PharmacyNetwork {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "network_id")
    private UUID networkId;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Network Identification
    @Column(name = "network_name", nullable = false, length = 100)
    private String networkName; // Surescripts, RelayHealth, etc.
    
    @Column(name = "network_code", length = 50)
    private String networkCode; // Internal code for the network
    
    @Column(name = "network_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NetworkType networkType = NetworkType.E_PRESCRIBING;
    
    // Network Configuration
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;
    
    @Column(name = "api_key", length = 500)
    private String apiKey; // Encrypted API key
    
    @Column(name = "username", length = 200)
    private String username;
    
    @Column(name = "password", length = 500)
    private String password; // Encrypted password
    
    @Column(name = "certificate_path", length = 500)
    private String certificatePath; // Path to SSL certificate
    
    @Column(name = "environment", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Environment environment = Environment.PRODUCTION;
    
    // Network Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "last_connection_test")
    private LocalDateTime lastConnectionTest;
    
    @Column(name = "connection_status", length = 50)
    @Enumerated(EnumType.STRING)
    private ConnectionStatus connectionStatus;
    
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
    
    // Network Capabilities
    @Column(name = "supports_prescription_transmission")
    @Builder.Default
    private Boolean supportsPrescriptionTransmission = true;
    
    @Column(name = "supports_fill_status_updates")
    @Builder.Default
    private Boolean supportsFillStatusUpdates = true;
    
    @Column(name = "supports_medication_history")
    @Builder.Default
    private Boolean supportsMedicationHistory = false;
    
    @Column(name = "supports_benefits_information")
    @Builder.Default
    private Boolean supportsBenefitsInformation = false;
    
    @Column(name = "supports_prior_authorization")
    @Builder.Default
    private Boolean supportsPriorAuthorization = false;
    
    // Network Settings (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "network_settings", columnDefinition = "jsonb")
    private Map<String, Object> networkSettings;
    
    // Statistics
    @Column(name = "total_transmissions")
    @Builder.Default
    private Long totalTransmissions = 0L;
    
    @Column(name = "successful_transmissions")
    @Builder.Default
    private Long successfulTransmissions = 0L;
    
    @Column(name = "failed_transmissions")
    @Builder.Default
    private Long failedTransmissions = 0L;
    
    @Column(name = "last_transmission_date")
    private LocalDateTime lastTransmissionDate;
    
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
    
    public enum NetworkType {
        E_PRESCRIBING,      // E-prescribing network (Surescripts, etc.)
        DIRECT_API,         // Direct API integration
        HL7,               // HL7 messaging
        FHIR,              // FHIR API
        FAX,               // Fax network
        OTHER              // Other network type
    }
    
    public enum Environment {
        PRODUCTION,        // Production environment
        STAGING,           // Staging environment
        TEST,              // Test environment
        DEVELOPMENT        // Development environment
    }
    
    public enum ConnectionStatus {
        CONNECTED,         // Connected and operational
        DISCONNECTED,      // Disconnected
        ERROR,             // Connection error
        TESTING,           // Testing connection
        UNKNOWN            // Unknown status
    }
}
