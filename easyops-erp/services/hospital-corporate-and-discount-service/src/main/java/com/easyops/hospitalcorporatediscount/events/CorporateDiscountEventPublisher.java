package com.easyops.hospitalcorporatediscount.events;

import java.util.Map;

/**
 * Optional event publisher for corporate-discount domain events.
 * <p>
 * When Kafka/RabbitMQ is in use, a Kafka- or Rabbit-backed implementation can be wired
 * in; otherwise a no-op or logging implementation is used. Publishing is intended to be
 * async and non-blocking so that request latency is not affected.
 * </p>
 * <p>
 * Consumer contracts (payload shapes and event types) are documented in
 * {@code src/main/resources/docs/EVENTS.md} and in {@link EventTypes}.
 * </p>
 */
public interface CorporateDiscountEventPublisher {

    /**
     * Publish a domain event. Payload must be JSON-serializable (e.g. String, Number, UUID, Map, List).
     *
     * @param eventType one of {@link EventTypes}
     * @param payload   minimal payload (id, corporateClientId, etc.); consumers may fetch full details via API
     */
    void publish(String eventType, Map<String, Object> payload);
}
