package com.easyops.hospitalpharmacy.events;

import com.easyops.hospitalpharmacy.repository.DispenseOrderRepository;
import com.easyops.hospitalpharmacy.service.DispenseOrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listens to prescription events from hospital-service and creates
 * corresponding dispense orders in the pharmacy domain.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionEventListener {

    private final DispenseOrderService dispenseOrderService;
    private final DispenseOrderRepository dispenseOrderRepository;
    private final ObjectMapper objectMapper;

    @Value("${hospital.events.topic:hospital-events}")
    private String topic;

    @KafkaListener(topics = "${hospital.events.topic:hospital-events}", groupId = "hospital-pharmacy-service")
    public void handlePrescriptionEvents(ConsumerRecord<String, String> record) {
        String key = record.key();
        String value = record.value();
        try {
            JsonNode root = objectMapper.readTree(value);
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");

            if (!type.startsWith("prescription.")) {
                return;
            }

            if ("prescription.created".equals(type)) {
                handlePrescriptionCreated(payload);
            } else if ("prescription.updated".equals(type)) {
                // For now, no-op; could be used later to sync status.
            }
        } catch (Exception ex) {
            log.error("Failed to process prescription event from topic {} key {}: {}", topic, key, ex.getMessage(), ex);
        }
    }

    private void handlePrescriptionCreated(JsonNode payload) {
        try {
            UUID prescriptionId = UUID.fromString(payload.path("prescriptionId").asText());
            if (dispenseOrderRepository.existsByPrescriptionId(prescriptionId)) {
                log.info("Skipping duplicate dispense order creation for prescriptionId={} (Kafka idempotency)", prescriptionId);
                return;
            }
            UUID patientId = UUID.fromString(payload.path("patientId").asText());
            String encounterIdText = payload.path("encounterId").asText(null);
            UUID encounterId = encounterIdText != null && !encounterIdText.isBlank()
                ? UUID.fromString(encounterIdText)
                : null;

            dispenseOrderService.createOrderFromPrescriptionEvent(prescriptionId, patientId, encounterId);
            log.info("Created dispense order from prescription event for prescriptionId={}", prescriptionId);
        } catch (Exception ex) {
            log.error("Failed to create dispense order from prescription event payload={}: {}", payload, ex.getMessage(), ex);
        }
    }
}

