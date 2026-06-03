package com.easyops.hospital.repository;

import com.easyops.hospital.entity.WebhookIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookIntegrationRepository extends JpaRepository<WebhookIntegration, UUID> {

    /** Look up an active integration by its primary key (provided in X-Webhook-Integration-Id header). */
    Optional<WebhookIntegration> findByIntegrationIdAndIsActiveTrue(UUID integrationId);

    /** Look up the active default integration by its well-known name. */
    Optional<WebhookIntegration> findByIntegrationNameAndIsActiveTrue(String integrationName);
}
