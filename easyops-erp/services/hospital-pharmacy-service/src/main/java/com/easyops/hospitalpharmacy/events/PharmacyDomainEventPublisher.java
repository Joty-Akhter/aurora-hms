package com.easyops.hospitalpharmacy.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outgoing pharmacy events (Phase P2 — WS-K): {@code pharmacy.sale.completed}, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PharmacyDomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PharmacyIntegrationProperties integrationProperties;

    public void publish(String type, Map<String, Object> payload) {
        if (!integrationProperties.getEvents().isPublishEnabled()) {
            return;
        }
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("type", type);
            envelope.put("payload", payload);
            String json = objectMapper.writeValueAsString(envelope);
            String key = payload.getOrDefault("dispenseOrderId", payload.getOrDefault("organizationId", UUID.randomUUID())).toString();
            kafkaTemplate.send(integrationProperties.getEvents().getTopic(), key, json);
        } catch (Exception e) {
            log.warn("Failed to publish pharmacy event {}: {}", type, e.getMessage());
        }
    }

    public void publishSaleCompleted(
            UUID organizationId,
            UUID dispenseOrderId,
            UUID patientId,
            UUID prescriptionId,
            String billingPosted) {
        Map<String, Object> p = new HashMap<>();
        p.put("organizationId", organizationId != null ? organizationId.toString() : null);
        p.put("dispenseOrderId", dispenseOrderId.toString());
        p.put("patientId", patientId != null ? patientId.toString() : null);
        p.put("prescriptionId", prescriptionId != null ? prescriptionId.toString() : null);
        p.put("billingPosted", billingPosted);
        publish("pharmacy.sale.completed", p);
    }

    /** Domain lifecycle event (WS-K1); complements {@code pharmacy.sale.completed} for order-state consumers. */
    public void publishDispenseOrderCompleted(
            UUID organizationId,
            UUID dispenseOrderId,
            UUID patientId,
            UUID prescriptionId) {
        Map<String, Object> p = new HashMap<>();
        p.put("organizationId", organizationId != null ? organizationId.toString() : null);
        p.put("dispenseOrderId", dispenseOrderId.toString());
        p.put("patientId", patientId != null ? patientId.toString() : null);
        p.put("prescriptionId", prescriptionId != null ? prescriptionId.toString() : null);
        publish("dispense-order.completed", p);
    }

    /**
     * Emitted when on-hand stock changes due to dispense issue or return (WS-K1).
     * {@code quantityDelta} matches stock movement quantity (negative for issue, positive for return).
     */
    public void publishStockChanged(
            UUID organizationId,
            UUID pharmacyLocationId,
            UUID drugId,
            String batchNumber,
            BigDecimal quantityDelta,
            String movementType,
            UUID dispenseOrderId) {
        Map<String, Object> p = new HashMap<>();
        p.put("organizationId", organizationId != null ? organizationId.toString() : null);
        p.put("pharmacyLocationId", pharmacyLocationId.toString());
        p.put("drugId", drugId.toString());
        p.put("batchNumber", batchNumber);
        p.put("quantityDelta", quantityDelta != null ? quantityDelta.toPlainString() : null);
        p.put("movementType", movementType);
        p.put("dispenseOrderId", dispenseOrderId.toString());
        publish("pharmacy.stock.changed", p);
    }

    public void publishSaleCancelled(UUID organizationId, UUID dispenseOrderId) {
        Map<String, Object> p = new HashMap<>();
        p.put("organizationId", organizationId != null ? organizationId.toString() : null);
        p.put("dispenseOrderId", dispenseOrderId.toString());
        publish("pharmacy.sale.cancelled", p);
    }

    public void publishDispenseLineReturned(UUID organizationId, UUID dispenseOrderId, UUID dispenseLineId, String quantityReturned) {
        Map<String, Object> p = new HashMap<>();
        p.put("organizationId", organizationId != null ? organizationId.toString() : null);
        p.put("dispenseOrderId", dispenseOrderId.toString());
        p.put("dispenseLineId", dispenseLineId.toString());
        p.put("quantityReturned", quantityReturned);
        publish("dispense-line.returned", p);
    }
}
