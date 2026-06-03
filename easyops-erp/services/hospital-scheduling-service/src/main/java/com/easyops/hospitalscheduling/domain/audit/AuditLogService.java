package com.easyops.hospitalscheduling.domain.audit;

import com.easyops.hospitalscheduling.api.dto.AuditLogResponse;
import com.easyops.hospitalscheduling.api.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String entityType, UUID entityId, String action,
                       UUID actorId, String actorRole, String bookingChannel,
                       String reason, String correlationId,
                       String beforeState, String afterState) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setActorRole(actorRole);
        log.setBookingChannel(bookingChannel);
        log.setReason(reason);
        log.setCorrelationId(correlationId);
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        repository.save(log);
    }

    public PagedResponse<AuditLogResponse> search(String entityType, UUID entityId, UUID actorId,
                                                   String action, OffsetDateTime fromDate, OffsetDateTime toDate,
                                                   String correlationId, int page, int size) {
        Specification<AuditLog> spec = Specification.where(null);
        if (entityType != null && !entityType.isBlank())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("entityType"), entityType));
        if (entityId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("entityId"), entityId));
        if (actorId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("actorId"), actorId));
        if (action != null && !action.isBlank())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), action));
        if (fromDate != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), fromDate));
        if (toDate != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("createdAt"), toDate));
        if (correlationId != null && !correlationId.isBlank())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("correlationId"), correlationId));

        Page<AuditLog> p = repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        PagedResponse<AuditLogResponse> resp = new PagedResponse<>();
        resp.setContent(p.getContent().stream().map(this::toResponse).toList());
        resp.setTotalElements(p.getTotalElements());
        resp.setTotalPages(p.getTotalPages());
        resp.setNumber(p.getNumber());
        resp.setSize(p.getSize());
        resp.setFirst(p.isFirst());
        resp.setLast(p.isLast());
        return resp;
    }

    public AuditLogResponse getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Audit log entry not found: " + id));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(log.getId());
        r.setEntityType(log.getEntityType());
        r.setEntityId(log.getEntityId());
        r.setAction(log.getAction());
        r.setActorId(log.getActorId());
        r.setActorRole(log.getActorRole());
        r.setBookingChannel(log.getBookingChannel());
        r.setReason(log.getReason());
        r.setCorrelationId(log.getCorrelationId());
        r.setBeforeState(log.getBeforeState());
        r.setAfterState(log.getAfterState());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }
}
