package com.easyops.communication.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "communication.phase3.worker.enabled", havingValue = "true", matchIfMissing = true)
public class CommunicationDeliveryWorker {

    private final CommunicationDeliveryService deliveryService;

    public CommunicationDeliveryWorker(CommunicationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${communication.phase3.worker.poll-interval-ms:10000}")
    public void process() {
        deliveryService.processReadyDeliveries();
    }
}
