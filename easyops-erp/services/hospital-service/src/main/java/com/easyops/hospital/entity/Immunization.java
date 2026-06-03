package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "immunizations", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Immunization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "immunization_id")
    private UUID immunizationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "vaccine_name", nullable = false, length = 200)
    private String vaccineName;
    
    @Column(name = "cvx_code", length = 10)
    private String cvxCode;
    
    @Column(name = "administration_date", nullable = false)
    private LocalDate administrationDate;
    
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    
    @Column(name = "manufacturer", length = 200)
    private String manufacturer;
    
    @Column(name = "route", length = 50)
    @Enumerated(EnumType.STRING)
    private Route route;
    
    @Column(name = "site", length = 100)
    private String site;
    
    @Column(name = "dose", length = 100)
    private String dose;
    
    @Column(name = "administered_by")
    private UUID administeredBy;
    
    @Column(name = "administered_location_id")
    private UUID administeredLocationId;
    
    @Column(name = "reaction", columnDefinition = "TEXT")
    private String reaction;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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
    
    public enum Route {
        IM, SC, ID, IN, PO, IV, NASAL, OPHTHALMIC, OTIC, OTHER
    }
}
