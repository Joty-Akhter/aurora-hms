package com.easyops.hospitalclinicalorders.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ClinicalOrdersEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ClinicalOrdersEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishOrderSetCreated(UUID orderSetId, UUID patientId, UUID visitId, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new OrderSetCreatedEvent(orderSetId, patientId, visitId, timestamp));
    }

    public void publishOrderCreated(UUID orderSetId, UUID orderId, UUID patientId, UUID visitId, String status, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderSetId, orderId, patientId, visitId, status, timestamp));
    }

    public void publishOrderStatusChanged(UUID orderSetId, UUID orderId, UUID patientId, UUID visitId, String status, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new OrderStatusChangedEvent(orderSetId, orderId, patientId, visitId, status, timestamp));
    }

    public void publishWorklistStatusChanged(UUID worklistItemId, UUID orderId, String status, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new WorklistStatusChangedEvent(worklistItemId, orderId, status, timestamp));
    }

    public void publishOrderCancelled(UUID orderSetId, UUID orderId, UUID patientId, UUID visitId, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new OrderCancelledEvent(orderSetId, orderId, patientId, visitId, timestamp));
    }

    public void publishResultAvailable(UUID orderId, UUID orderSetId, UUID patientId, UUID visitId,
                                       UUID resultLinkId, String resultStatus, String source, OffsetDateTime timestamp) {
        applicationEventPublisher.publishEvent(new ResultAvailableEvent(orderId, orderSetId, patientId, visitId,
                resultLinkId, resultStatus, source, timestamp));
    }
}
