package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "one_on_one_meetings", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OneOnOneMeeting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "meeting_id")
    private UUID meetingId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "manager_id", nullable = false)
    private UUID managerId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "meeting_date", nullable = false)
    private LocalDateTime meetingDate;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "meeting_type", length = 50)
    private String meetingType = "regular";
    
    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;
    
    @Column(name = "discussion_points", columnDefinition = "TEXT")
    private String discussionPoints;
    
    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;
    
    @Column(name = "employee_notes", columnDefinition = "TEXT")
    private String employeeNotes;
    
    @Column(name = "manager_notes", columnDefinition = "TEXT")
    private String managerNotes;
    
    @Column(name = "status", length = 50)
    private String status = "scheduled";
    
    @Column(name = "next_meeting_date")
    private LocalDateTime nextMeetingDate;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

