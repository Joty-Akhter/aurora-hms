package com.easyops.notification.service;

import com.easyops.notification.dto.WebhookRequest;
import com.easyops.notification.dto.WebhookResponse;
import com.easyops.notification.entity.Webhook;
import com.easyops.notification.entity.WebhookDelivery;
import com.easyops.notification.repository.WebhookDeliveryRepository;
import com.easyops.notification.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {
    
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebClient.Builder webClientBuilder;
    
    @Transactional
    public WebhookResponse createWebhook(WebhookRequest request, UUID createdBy) {
        Webhook webhook = new Webhook();
        webhook.setOrganizationId(request.getOrganizationId());
        webhook.setName(request.getName());
        webhook.setUrl(request.getUrl());
        webhook.setSecret(request.getSecret());
        webhook.setEvents(request.getEvents());
        webhook.setIsActive(request.getIsActive());
        webhook.setRetryCount(request.getRetryCount());
        webhook.setTimeoutSeconds(request.getTimeoutSeconds());
        webhook.setCreatedBy(createdBy);
        
        webhook = webhookRepository.save(webhook);
        log.info("Created webhook: {} for organization: {}", webhook.getName(), webhook.getOrganizationId());
        
        return mapToResponse(webhook);
    }
    
    public List<WebhookResponse> getOrganizationWebhooks(UUID organizationId) {
        return webhookRepository.findByOrganizationId(organizationId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public WebhookResponse getWebhook(UUID webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new RuntimeException("Webhook not found"));
        return mapToResponse(webhook);
    }
    
    @Transactional
    public WebhookResponse updateWebhook(UUID webhookId, WebhookRequest request) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new RuntimeException("Webhook not found"));
        
        webhook.setName(request.getName());
        webhook.setUrl(request.getUrl());
        webhook.setSecret(request.getSecret());
        webhook.setEvents(request.getEvents());
        webhook.setIsActive(request.getIsActive());
        webhook.setRetryCount(request.getRetryCount());
        webhook.setTimeoutSeconds(request.getTimeoutSeconds());
        
        webhook = webhookRepository.save(webhook);
        log.info("Updated webhook: {}", webhookId);
        
        return mapToResponse(webhook);
    }
    
    @Transactional
    public void deleteWebhook(UUID webhookId) {
        webhookRepository.deleteById(webhookId);
        log.info("Deleted webhook: {}", webhookId);
    }
    
    @Async
    @Transactional
    public void triggerWebhooks(UUID organizationId, String eventType, Map<String, Object> payload) {
        List<Webhook> webhooks = webhookRepository.findActiveWebhooksForEvent(organizationId, eventType);
        
        for (Webhook webhook : webhooks) {
            deliverWebhook(webhook, eventType, payload, 0);
        }
    }
    
    private void deliverWebhook(Webhook webhook, String eventType, Map<String, Object> payload, int retryAttempt) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setWebhookId(webhook.getId());
        delivery.setEventType(eventType);
        delivery.setPayload(payload);
        delivery.setRetryAttempt(retryAttempt);
        
        try {
            String signature = generateSignature(payload.toString(), webhook.getSecret());
            
            WebClient webClient = webClientBuilder.build();
            
            String responseBody = webClient.post()
                .uri(webhook.getUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-Webhook-Signature", signature)
                .header("X-Event-Type", eventType)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(webhook.getTimeoutSeconds()))
                .block();
            
            delivery.setSuccess(true);
            delivery.setResponseStatus(200);
            delivery.setResponseBody(responseBody);
            delivery.setDeliveredAt(LocalDateTime.now());
            
            log.info("Webhook delivered successfully: {} to {}", webhook.getName(), webhook.getUrl());
            
        } catch (Exception e) {
            delivery.setSuccess(false);
            delivery.setErrorMessage(e.getMessage());
            
            log.error("Failed to deliver webhook: {}", webhook.getName(), e);
            
            // Retry if not exceeded max attempts
            if (retryAttempt < webhook.getRetryCount()) {
                log.info("Retrying webhook delivery: attempt {}", retryAttempt + 1);
                deliverWebhook(webhook, eventType, payload, retryAttempt + 1);
            }
        }
        
        deliveryRepository.save(delivery);
    }
    
    public Page<WebhookDelivery> getDeliveryHistory(UUID webhookId, Pageable pageable) {
        return deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId, pageable);
    }
    
    private String generateSignature(String payload, String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to generate webhook signature", e);
            return "";
        }
    }
    
    private WebhookResponse mapToResponse(Webhook webhook) {
        return new WebhookResponse(
            webhook.getId(),
            webhook.getOrganizationId(),
            webhook.getName(),
            webhook.getUrl(),
            webhook.getEvents(),
            webhook.getIsActive(),
            webhook.getRetryCount(),
            webhook.getTimeoutSeconds(),
            webhook.getCreatedAt(),
            webhook.getUpdatedAt()
        );
    }
}

