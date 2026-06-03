package com.easyops.communication.repository;

import com.easyops.communication.entity.CommunicationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationDeliveryRepository extends JpaRepository<CommunicationDelivery, UUID> {
    Page<CommunicationDelivery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<CommunicationDelivery> findByIdempotencyKey(String idempotencyKey);

    Page<CommunicationDelivery> findByCorrelationIdOrderByCreatedAtDesc(String correlationId, Pageable pageable);

    Page<CommunicationDelivery> findByEventIdOrderByCreatedAtDesc(String eventId, Pageable pageable);

    Page<CommunicationDelivery> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CommunicationDelivery> findByChannelOrderByCreatedAtDesc(String channel, Pageable pageable);

    Page<CommunicationDelivery> findByStatusAndChannelOrderByCreatedAtDesc(String status, String channel, Pageable pageable);

    List<CommunicationDelivery> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Collection<String> statuses,
            Instant now
    );

    long countByStatusIn(Collection<String> statuses);

    List<CommunicationDelivery> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Collection<String> statuses,
            Instant now,
            Pageable pageable
    );

    long countByCreatedAtAfter(Instant createdAt);

    long countByStatusInAndCreatedAtAfter(Collection<String> statuses, Instant createdAt);

    long countByStatusAndCreatedAtAfter(String status, Instant createdAt);
}
