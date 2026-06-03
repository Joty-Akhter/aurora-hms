package com.easyops.ar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reminder_config", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false)
    private Boolean enabled = false;
    
    @Column(name = "days_before_due")
    private Integer daysBeforeDue;  // e.g., -7 means 7 days before due date
    
    @Column(name = "days_after_due_level1")
    private Integer daysAfterDueLevel1 = 1;  // First reminder: 1 day overdue
    
    @Column(name = "days_after_due_level2")
    private Integer daysAfterDueLevel2 = 7;  // Second reminder: 7 days overdue
    
    @Column(name = "days_after_due_level3")
    private Integer daysAfterDueLevel3 = 14;  // Third reminder: 14 days overdue
    
    @Column(name = "email_template_before_due", length = 2000)
    private String emailTemplateBeforeDue;
    
    @Column(name = "email_template_level1", length = 2000)
    private String emailTemplateLevel1;
    
    @Column(name = "email_template_level2", length = 2000)
    private String emailTemplateLevel2;
    
    @Column(name = "email_template_level3", length = 2000)
    private String emailTemplateLevel3;
    
    @Column(name = "send_copy_to_email")
    private String sendCopyToEmail;  // CC email for all reminders
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

