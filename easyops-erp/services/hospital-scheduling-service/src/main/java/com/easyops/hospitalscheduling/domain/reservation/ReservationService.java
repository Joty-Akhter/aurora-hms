package com.easyops.hospitalscheduling.domain.reservation;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.config.SchedulingMetrics;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String DEFAULT_STATUS = "TENTATIVE";

    private final ReservationRepository reservationRepository;
    private final SchedulingResourceRepository resourceRepository;
    private final SchedulingMetrics schedulingMetrics;

    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        if (request.getSlotStart() != null && request.getSlotEnd() != null && !request.getSlotEnd().isAfter(request.getSlotStart())) {
            throw new IllegalArgumentException("slotEnd must be after slotStart");
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        if (!resourceRepository.existsById(request.getResourceId())) {
            throw new NoSuchElementException("Resource not found: " + request.getResourceId());
        }
        List<Reservation> overlapping = reservationRepository.findOverlapping(
                request.getResourceId(),
                request.getSlotStart(),
                request.getSlotEnd(),
                null
        );
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Slot conflicts with existing reservation(s): " + overlapping.stream().map(r -> r.getId().toString()).reduce((a, b) -> a + ", " + b).orElse(""));
        }
        Reservation entity = new Reservation();
        entity.setId(UUID.randomUUID());
        entity.setResourceId(request.getResourceId());
        entity.setSlotStart(request.getSlotStart());
        entity.setSlotEnd(request.getSlotEnd());
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : DEFAULT_STATUS);
        entity.setReferenceType(trimOrNull(request.getReferenceType()));
        entity.setReferenceId(trimOrNull(request.getReferenceId()));
        entity.setPatientId(request.getPatientId());
        entity.setIdempotencyKey(trimOrNull(request.getIdempotencyKey()));
        reservationRepository.save(entity);
        return toResponse(entity);
    }

    public ReservationResponse getById(UUID id) {
        Reservation entity = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<ReservationResponse> list(UUID resourceId, UUID patientId, String status,
                                                    OffsetDateTime from, OffsetDateTime to, int page, int size) {
        Specification<Reservation> spec = ReservationSpecifications.hasResourceId(resourceId)
                .and(ReservationSpecifications.hasPatientId(patientId))
                .and(ReservationSpecifications.hasStatus(status))
                .and(ReservationSpecifications.slotStartFrom(from))
                .and(ReservationSpecifications.slotEndTo(to));
        Page<Reservation> p = reservationRepository.findAll(spec, PageRequest.of(page, size));
        return new PagedResponse<>(
                p.getContent().stream().map(this::toResponse).toList(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumber(),
                p.getSize(),
                p.isFirst(),
                p.isLast()
        );
    }

    @Transactional
    public ReservationResponse updateStatus(UUID id, UpdateReservationStatusRequest request) {
        Reservation entity = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + id));
        entity.setStatus(request.getStatus().trim());
        reservationRepository.save(entity);
        return toResponse(entity);
    }

    /**
     * Create one reservation per resource for the same slot and reference (e.g. doctor + room).
     * If any resource has a conflict, the whole operation fails; otherwise all are created
     * with the same referenceType/referenceId. Idempotency: store key on first reservation only;
     * on retry, return all reservations for that reference and slot.
     */
    @Transactional
    public List<ReservationResponse> createMulti(CreateMultiResourceReservationRequest request) {
        if (request.getSlotEnd() == null || request.getSlotStart() == null || !request.getSlotEnd().isAfter(request.getSlotStart())) {
            throw new IllegalArgumentException("slotEnd must be after slotStart");
        }
        List<UUID> resourceIds = request.getResourceIds().stream().distinct().toList();
        if (resourceIds.isEmpty()) {
            throw new IllegalArgumentException("at least one resourceId required");
        }
        String idempotencyKey = trimOrNull(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Reservation first = existing.get();
                List<Reservation> batch = reservationRepository.findByReferenceTypeAndReferenceIdAndSlotStartAndSlotEnd(
                        first.getReferenceType(), first.getReferenceId(), first.getSlotStart(), first.getSlotEnd());
                return batch.stream().sorted((a, b) -> a.getResourceId().compareTo(b.getResourceId())).map(this::toResponse).toList();
            }
        }
        for (UUID rid : resourceIds) {
            if (!resourceRepository.existsById(rid)) {
                throw new NoSuchElementException("Resource not found: " + rid);
            }
            List<Reservation> overlapping = reservationRepository.findOverlapping(rid, request.getSlotStart(), request.getSlotEnd(), null);
            if (!overlapping.isEmpty()) {
                throw new IllegalStateException("Slot conflicts with existing reservation(s) for resource " + rid + ": " +
                        overlapping.stream().map(r -> r.getId().toString()).collect(Collectors.joining(", ")));
            }
        }
        String refType = trimOrNull(request.getReferenceType());
        String refId = trimOrNull(request.getReferenceId());
        List<Reservation> created = new ArrayList<>();
        for (int i = 0; i < resourceIds.size(); i++) {
            Reservation entity = new Reservation();
            entity.setId(UUID.randomUUID());
            entity.setResourceId(resourceIds.get(i));
            entity.setSlotStart(request.getSlotStart());
            entity.setSlotEnd(request.getSlotEnd());
            entity.setStatus(DEFAULT_STATUS);
            entity.setReferenceType(refType);
            entity.setReferenceId(refId);
            entity.setPatientId(request.getPatientId());
            entity.setIdempotencyKey(i == 0 ? idempotencyKey : null);
            reservationRepository.save(entity);
            created.add(entity);
        }
        return created.stream().map(this::toResponse).toList();
    }

    public ConflictCheckResponse checkConflicts(CheckConflictsRequest request) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(
                request.getResourceId(),
                request.getSlotStart(),
                request.getSlotEnd(),
                request.getExcludeReservationId()
        );
        if (!overlapping.isEmpty()) {
            schedulingMetrics.incrementReservationsConflicts();
        }
        List<ReservationResponse> list = overlapping.stream().map(this::toResponse).toList();
        return new ConflictCheckResponse(!list.isEmpty(), list);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ReservationResponse toResponse(Reservation e) {
        ReservationResponse r = new ReservationResponse();
        r.setId(e.getId());
        r.setResourceId(e.getResourceId());
        r.setSlotStart(e.getSlotStart());
        r.setSlotEnd(e.getSlotEnd());
        r.setStatus(e.getStatus());
        r.setReferenceType(e.getReferenceType());
        r.setReferenceId(e.getReferenceId());
        r.setPatientId(e.getPatientId());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
