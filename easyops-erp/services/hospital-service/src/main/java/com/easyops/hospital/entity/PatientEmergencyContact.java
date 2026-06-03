package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_emergency_contacts", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientEmergencyContact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "contact_id")
    private UUID contactId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "contact_name", nullable = false, length = 200)
    private String contactName;
    
    @Column(name = "relationship", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Relationship relationship;
    
    @Column(name = "primary_phone", nullable = false, length = 50)
    private String primaryPhone;
    
    @Column(name = "secondary_phone", length = 50)
    private String secondaryPhone;
    
    @Column(name = "address_line1", length = 255)
    private String addressLine1;
    
    @Column(name = "address_line2", length = 255)
    private String addressLine2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 50)
    private String state;
    
    @Column(name = "zip", length = 20)
    private String zip;
    
    @Column(name = "country", length = 50)
    private String country;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
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
    
    public enum Relationship {
        Spouse, Parent, Child, Sibling, Friend, Other
    }
}
