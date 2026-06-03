package com.easyops.ar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reminder_history", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private ARInvoice invoice;
    
    @Column(nullable = false)
    private LocalDate sentDate;
    
    @Column(nullable = false)
    private Integer reminderLevel;  // 0=before due, 1,2,3=overdue levels
    
    @Column(nullable = false)
    private String recipientEmail;
    
    @Column(nullable = false)
    private Boolean emailSent = false;
    
    @Column(length = 500)
    private String errorMessage;  // If email failed
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

