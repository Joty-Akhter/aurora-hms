package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private UUID eventId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "event_type", length = 50)
    private String eventType;
    
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;
    
    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;
    
    @Column(name = "all_day")
    private Boolean allDay = false;
    
    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";
    
    @Column(name = "location", length = 255)
    private String location;
    
    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;
    
    @Column(name = "status", length = 50)
    private String status = "PLANNED";
    
    @Column(name = "organizer_id")
    private UUID organizerId;
    
    @Column(name = "attendees", columnDefinition = "TEXT[]")
    private String[] attendees;
    
    @Column(name = "lead_id")
    private UUID leadId;
    
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "opportunity_id")
    private UUID opportunityId;
    
    @Column(name = "campaign_id")
    private UUID campaignId;
    
    @Column(name = "outcome", length = 50)
    private String outcome;
    
    @Column(name = "outcome_notes", columnDefinition = "TEXT")
    private String outcomeNotes;
    
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes = 15;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;
    
    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern;
    
    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

