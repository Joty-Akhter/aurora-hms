package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks how often a portal user selects each EP advice line, for ranked autocomplete on prescriptions/templates.
 */
@Entity
@Table(name = "ep_advice_user_usage", schema = "ehr",
        uniqueConstraints = @UniqueConstraint(name = "uq_ep_advice_usage_user_lookup",
                columnNames = {"user_id", "advice_lookup_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpAdviceUserUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "advice_lookup_id", nullable = false)
    private EpLookupItem adviceLookupItem;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Long useCount = 1L;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;
}
