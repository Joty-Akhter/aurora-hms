package com.easyops.hospitalcorporatediscount.events;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Logging implementation of {@link CorporateDiscountEventPublisher}.
 * Used when no Kafka/RabbitMQ implementation is configured. Events are logged at INFO
 * for audit/debug. Registered via {@link com.easyops.hospitalcorporatediscount.config.EventsConfig};
 * provide your own bean to replace with Kafka/RabbitMQ.
 */
@Slf4j
public class LoggingCorporateDiscountEventPublisher implements CorporateDiscountEventPublisher {

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        log.info("Corporate-discount event: type={} payload={}", eventType, payload);
    }
}
