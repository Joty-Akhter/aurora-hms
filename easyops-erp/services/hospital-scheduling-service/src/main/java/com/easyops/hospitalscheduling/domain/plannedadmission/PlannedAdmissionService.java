package com.easyops.hospitalscheduling.domain.plannedadmission;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import com.easyops.hospitalscheduling.domain.reservation.Reservation;
import com.easyops.hospitalscheduling.domain.reservation.ReservationRepository;
import com.easyops.hospitalscheduling.domain.reservation.ReservationService;
import com.easyops.hospitalscheduling.events.PlannedAdmissionEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannedAdmissionService {

    private static final String REQUESTED = "REQUESTED";
    private static final String RESERVED = "RESERVED";
    private static final String CONVERTED = "CONVERTED";
    private static final String EXPIRED = "EXPIRED";
    private static final String CANCELLED = "CANCELLED";
    private static final String REFERENCE_TYPE = "PLANNED_ADMISSION";
    private static final int DEFAULT_EXPIRY_HOURS = 24;

    private final PlannedAdmissionRepository plannedAdmissionRepository;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final SchedulingResourceRepository resourceRepository;
    private final PlannedAdmissionEventPublisher plannedAdmissionEventPublisher;

    @Transactional
    public PlannedAdmissionResponse create(CreatePlannedAdmissionRequest request) {
        PlannedAdmission entity = new PlannedAdmission();
        entity.setPatientId(request.getPatientId());
        entity.setPreferredDate(request.getPreferredDate());
        entity.setPreferredWardOrBedClass(trimOrNull(request.getPreferredWardOrBedClass()));
        entity.setStatus(REQUESTED);
        plannedAdmissionRepository.save(entity);
        return toResponse(entity);
    }

    public PlannedAdmissionResponse getById(UUID id) {
        PlannedAdmission entity = plannedAdmissionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planned admission not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<PlannedAdmissionResponse> list(UUID patientId, LocalDate preferredDateFrom, LocalDate preferredDateTo,
                                                        String status, int page, int size) {
        Specification<PlannedAdmission> spec = PlannedAdmissionSpecifications.hasPatientId(patientId)
                .and(PlannedAdmissionSpecifications.preferredDateFrom(preferredDateFrom))
                .and(PlannedAdmissionSpecifications.preferredDateTo(preferredDateTo))
                .and(PlannedAdmissionSpecifications.hasStatus(status));
        Page<PlannedAdmission> p = plannedAdmissionRepository.findAll(spec, PageRequest.of(page, size));
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
    public PlannedAdmissionResponse updateStatus(UUID id, UpdatePlannedAdmissionStatusRequest request) {
        PlannedAdmission entity = plannedAdmissionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planned admission not found: " + id));
        String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : null;
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        switch (newStatus) {
            case RESERVED -> setReserved(entity, request);
            case CONVERTED -> setConverted(entity);
            case EXPIRED, CANCELLED -> {
                entity.setStatus(newStatus);
                plannedAdmissionRepository.save(entity);
            }
            default -> throw new IllegalArgumentException("Invalid status: " + newStatus);
        }
        return toResponse(entity);
    }

    private void setReserved(PlannedAdmission entity, UpdatePlannedAdmissionStatusRequest request) {
        if (request.getBedGroupResourceId() == null) {
            throw new IllegalArgumentException("bedGroupResourceId is required when status is RESERVED");
        }
        if (!resourceRepository.existsById(request.getBedGroupResourceId())) {
            throw new NoSuchElementException("Resource not found: " + request.getBedGroupResourceId());
        }
        OffsetDateTime slotStart = entity.getPreferredDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime slotEnd = entity.getPreferredDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        CreateReservationRequest createRes = new CreateReservationRequest();
        createRes.setResourceId(request.getBedGroupResourceId());
        createRes.setSlotStart(slotStart);
        createRes.setSlotEnd(slotEnd);
        createRes.setStatus("CONFIRMED");
        createRes.setReferenceType(REFERENCE_TYPE);
        createRes.setReferenceId(entity.getId().toString());
        createRes.setPatientId(entity.getPatientId());
        ReservationResponse resResponse = reservationService.create(createRes);
        Reservation reservation = reservationRepository.findById(resResponse.getId()).orElseThrow();

        entity.setStatus(RESERVED);
        entity.setBedGroupResourceId(request.getBedGroupResourceId());
        entity.setReservationId(reservation.getId());
        // expires_at: from request, or default 24h (booking rules could be extended for planned-admission expiry)
        entity.setExpiresAt(request.getExpiresAt() != null
                ? request.getExpiresAt()
                : OffsetDateTime.now(ZoneOffset.UTC).plusHours(DEFAULT_EXPIRY_HOURS));
        plannedAdmissionRepository.save(entity);
        plannedAdmissionEventPublisher.publishReserved(entity);
    }

    /** When CONVERTED: optionally close the linked reservation so the bed is no longer held. */
    private void setConverted(PlannedAdmission entity) {
        if (entity.getReservationId() != null) {
            reservationRepository.findById(entity.getReservationId()).ifPresent(r -> {
                r.setStatus("COMPLETED");
                reservationRepository.save(r);
            });
        }
        entity.setStatus(CONVERTED);
        plannedAdmissionRepository.save(entity);
        plannedAdmissionEventPublisher.publishConverted(entity);
    }

    public ExpectedAdmissionsResponse getExpectedAdmissions(LocalDate fromDate, LocalDate toDate, String wardOrBedClass) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        List<PlannedAdmission> list = plannedAdmissionRepository.findByPreferredDateBetweenAndActive(fromDate, toDate);
        if (wardOrBedClass != null && !wardOrBedClass.isBlank()) {
            String w = wardOrBedClass.trim();
            list = list.stream().filter(p -> w.equalsIgnoreCase(p.getPreferredWardOrBedClass())).toList();
        }
        Map<LocalDate, Long> byDate = list.stream()
                .collect(Collectors.groupingBy(PlannedAdmission::getPreferredDate, Collectors.counting()));
        List<ExpectedAdmissionsResponse.ExpectedAdmissionsItem> items = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    ExpectedAdmissionsResponse.ExpectedAdmissionsItem item = new ExpectedAdmissionsResponse.ExpectedAdmissionsItem();
                    item.setDate(e.getKey());
                    item.setCount(e.getValue());
                    item.setWardOrBedClass(wardOrBedClass);
                    return item;
                })
                .toList();
        ExpectedAdmissionsResponse response = new ExpectedAdmissionsResponse();
        response.setItems(items);
        return response;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private PlannedAdmissionResponse toResponse(PlannedAdmission e) {
        PlannedAdmissionResponse r = new PlannedAdmissionResponse();
        r.setId(e.getId());
        r.setPatientId(e.getPatientId());
        r.setPreferredDate(e.getPreferredDate());
        r.setPreferredWardOrBedClass(e.getPreferredWardOrBedClass());
        r.setStatus(e.getStatus());
        r.setBedGroupResourceId(e.getBedGroupResourceId());
        r.setReservationId(e.getReservationId());
        r.setExpiresAt(e.getExpiresAt());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
