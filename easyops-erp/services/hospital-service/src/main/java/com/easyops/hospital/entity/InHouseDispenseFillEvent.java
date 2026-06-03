package com.easyops.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "in_house_dispense_fill_events", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InHouseDispenseFillEvent {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(name = "dispense_order_id", nullable = false)
    private UUID dispenseOrderId;

    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
