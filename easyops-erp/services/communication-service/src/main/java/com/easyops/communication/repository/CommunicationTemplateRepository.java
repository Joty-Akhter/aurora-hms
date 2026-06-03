package com.easyops.communication.repository;

import com.easyops.communication.entity.CommunicationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CommunicationTemplateRepository extends JpaRepository<CommunicationTemplate, UUID> {
    Page<CommunicationTemplate> findByTemplateKeyContainingIgnoreCase(String templateKey, Pageable pageable);

    Optional<CommunicationTemplate> findByTemplateKeyAndChannelAndLocaleAndVersion(
            String templateKey,
            String channel,
            String locale,
            Integer version
    );

    Optional<CommunicationTemplate> findByTemplateKeyAndChannelAndLocaleAndStatus(
            String templateKey,
            String channel,
            String locale,
            String status
    );

    List<CommunicationTemplate> findByTemplateKeyAndChannelAndStatus(String templateKey, String channel, String status);

    @Modifying
    @Query("""
            update CommunicationTemplate ct
               set ct.status = 'ARCHIVED'
             where ct.templateKey = :templateKey
               and ct.channel = :channel
               and ct.locale = :locale
               and ct.status = 'ACTIVE'
               and ct.id <> :excludeId
            """)
    int archiveOtherActiveVersions(String templateKey, String channel, String locale, UUID excludeId);
}
