package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_application_actions", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "action_id")
    private UUID actionId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private LoanApplicationActionType actionType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "comment_text", length = 2000)
    private String commentText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
