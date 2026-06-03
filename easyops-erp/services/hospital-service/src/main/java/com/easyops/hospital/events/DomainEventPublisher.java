package com.easyops.hospital.events;

import java.util.Map;

/**
 * Simple domain event publisher abstraction for hospital-service.
 *
 * For now this is implemented as a logging publisher; it can later be
 * backed by Kafka or another event bus without changing callers.
 */
public interface DomainEventPublisher {

    void publish(String eventType, Map<String, Object> payload);
}

