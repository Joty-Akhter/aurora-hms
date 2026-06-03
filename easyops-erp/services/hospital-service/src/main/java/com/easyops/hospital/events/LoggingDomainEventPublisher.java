package com.easyops.hospital.events;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trivial logging implementation of DomainEventPublisher.
 *
 * <p>Used when {@code hospital.kafka.events.enabled} is false or unset. Kafka is registered
 * when that property is true, so we use explicit properties instead of
 * {@code @ConditionalOnMissingBean(DomainEventPublisher.class)}, which can leave no bean registered.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "hospital.kafka.events.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private final MeterRegistry meterRegistry;

    public LoggingDomainEventPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        log.info("Domain event published: type={} payload={}", eventType, payload);
        meterRegistry.counter("hospital.events.published", Tags.of("type", eventType, "status", "logged"))
                .increment();
    }
}
