package com.easyops.communication.service;

import com.easyops.communication.dto.InboundCommunicationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "communication.phase3.kafka.enabled", havingValue = "true")
public class CommunicationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommunicationEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final CommunicationDeliveryService deliveryService;

    public CommunicationEventConsumer(ObjectMapper objectMapper, CommunicationDeliveryService deliveryService) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = {
                    "${communication.phase3.kafka.appointment-topic:appointment-lifecycle-events}",
                    "${communication.phase3.kafka.invoice-topic:invoice-lifecycle-events}"
            },
            groupId = "${communication.phase3.kafka.group-id:communication-service-phase3}"
    )
    public void consume(String rawMessage) {
        try {
            InboundCommunicationEvent event = objectMapper.readValue(rawMessage, InboundCommunicationEvent.class);
            deliveryService.ingest(event);
        } catch (Exception ex) {
            log.warn("Rejected malformed communication event: {}", ex.getMessage());
        }
    }
}
