package com.easyops.hospital.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka-backed implementation of DomainEventPublisher.
 *
 * <p>Publishes JSON-encoded domain events to a shared topic so that downstream services
 * (billing, pharmacy, BFF, etc.) can subscribe. Sends are done asynchronously so a missing
 * broker or topic does not block HTTP requests (Kafka producer {@code max.block.ms} can be ~60s).
 *
 * <p>Disabled by default; set {@code hospital.kafka.events.enabled=true} when Kafka is available.
 */
@Component
@ConditionalOnProperty(name = "hospital.kafka.events.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${hospital.events.topic:hospital-events}")
    private String topic;

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", eventType);
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("payload", payload);

        final String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event for type {}: {}", eventType, e.getMessage(), e);
            meterRegistry.counter("hospital.events.published", Tags.of("type", eventType, "status", "serialization_error"))
                    .increment();
            return;
        }

        CompletableFuture.runAsync(() -> kafkaTemplate.send(topic, eventType, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish domain event type {} to Kafka topic {}: {}", eventType, topic,
                                ex.getMessage(), ex);
                        meterRegistry.counter("hospital.events.published", Tags.of("type", eventType, "status", "publish_error"))
                                .increment();
                    } else {
                        log.debug("Kafka domain event published: topic={} key={} payload={}", topic, eventType, json);
                        meterRegistry.counter("hospital.events.published", Tags.of("type", eventType, "status", "success"))
                                .increment();
                    }
                }));
    }
}

