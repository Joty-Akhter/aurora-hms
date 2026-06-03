package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "formulary_alternatives", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FormularyAlternative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formulary_rule_id", nullable = false)
    private FormularyRule formularyRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alternative_drug_id", nullable = false)
    private Drug alternativeDrug;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(name = "equivalence_class", length = 100)
    private String equivalenceClass;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
