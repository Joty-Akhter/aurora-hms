package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "icd11_codes", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Icd11Code {
    
    @Id
    @Column(name = "code", length = 20)
    private String code;
    
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "chapter", length = 100)
    private String chapter;
    
    @Column(name = "is_valid")
    @Builder.Default
    private Boolean isValid = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
