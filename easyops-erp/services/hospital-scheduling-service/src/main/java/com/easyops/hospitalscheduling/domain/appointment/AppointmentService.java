package com.easyops.hospitalscheduling.domain.appointment;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.events.AppointmentEventPublisher;
import com.easyops.hospitalscheduling.events.CommunicationAppointmentEventPublisher;
import com.easyops.hospitalscheduling.domain.audit.AuditLogService;
import com.easyops.hospitalscheduling.domain.resource.BookingRule;
import com.easyops.hospitalscheduling.domain.resource.BookingRuleRepository;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResource;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import com.easyops.hospitalscheduling.domain.resource.SlotTemplate;
import com.easyops.hospitalscheduling.domain.resource.SlotTemplateRepository;
import com.easyops.hospitalscheduling.domain.resource.SlotTemplateSpecifications;
import com.easyops.hospitalscheduling.domain.reservation.Reservation;
import com.easyops.hospitalscheduling.domain.reservation.ReservationRepository;
import com.easyops.hospitalscheduling.config.SchedulingMetrics;
import com.easyops.hospitalscheduling.domain.reservation.ReservationService;
import com.easyops.hospitalscheduling.integration.PatientSmsEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String APPOINTMENT_REFERENCE_TYPE = "APPOINTMENT";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String NO_SHOW = "NO_SHOW";
    private static final String COMPLETED = "COMPLETED";

    private final AppointmentRepository appointmentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final SchedulingResourceRepository resourceRepository;
    private final BookingRuleRepository bookingRuleRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final SchedulingMetrics schedulingMetrics;
    private final AuditLogService auditLogService;
    private final QueueEventBroadcaster queueEventBroadcaster;
    private final PatientSmsEnrichmentService patientSmsEnrichmentService;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.getSlotStart() != null && request.getSlotEnd() != null && !request.getSlotEnd().isAfter(request.getSlotStart())) {
            throw new IllegalArgumentException("slotEnd must be after slotStart");
        }
        String idemKey = trimOrNull(request.getIdempotencyKey());
        if (idemKey != null) {
            Optional<Appointment> existing = appointmentRepository.findByIdempotencyKey(idemKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        patientSmsEnrichmentService.enrichCreateRequest(request, request.getPatientId());
        assertNoDuplicatePatientSlot(request);

        List<UUID> additionalIds = request.getAdditionalResourceIds() != null ? request.getAdditionalResourceIds().stream().filter(id -> id != null && !id.equals(request.getResourceId())).distinct().toList() : List.of();
        if (!additionalIds.isEmpty()) {
            return createWithMultiResource(request, idemKey, additionalIds);
        }

        SchedulingResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + request.getResourceId()));
        CheckConflictsRequest checkReq = new CheckConflictsRequest();
        checkReq.setResourceId(request.getResourceId());
        checkReq.setSlotStart(request.getSlotStart());
        checkReq.setSlotEnd(request.getSlotEnd());
        checkReq.setExcludeReservationId(null);
        ConflictCheckResponse conflict = reservationService.checkConflicts(checkReq);
        if (conflict.isHasConflict()) {
            throw new IllegalStateException("Slot conflicts with existing reservation(s)");
        }
        Integer maxPerSlot = effectiveMaxPerSlot(resource);
        int overlappingCount = conflict.getConflictingReservations() != null ? conflict.getConflictingReservations().size() : 0;
        if (maxPerSlot != null && (overlappingCount + 1) > maxPerSlot) {
            String overrideReason = trimOrNull(request.getOverbookingOverrideReason());
            if (overrideReason == null) {
                throw new IllegalStateException("Slot is at capacity (max " + maxPerSlot + " per slot). Provide overbookingOverrideReason to allow overbooking.");
            }
        }

        CreateReservationRequest createRes = new CreateReservationRequest();
        createRes.setResourceId(request.getResourceId());
        createRes.setSlotStart(request.getSlotStart());
        createRes.setSlotEnd(request.getSlotEnd());
        createRes.setStatus(CONFIRMED);
        createRes.setReferenceType(APPOINTMENT_REFERENCE_TYPE);
        createRes.setPatientId(request.getPatientId());
        ReservationResponse resResponse = reservationService.create(createRes);
        Reservation reservation = reservationRepository.findById(resResponse.getId()).orElseThrow();

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setReservationId(reservation.getId());
        appointment.setPatientId(request.getPatientId());
        appointment.setResourceId(request.getResourceId());
        appointment.setClinicOrLocationId(request.getClinicOrLocationId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setSlotStart(request.getSlotStart());
        appointment.setSlotEnd(request.getSlotEnd());
        appointment.setAppointmentType(trimOrNull(request.getAppointmentType()) != null ? request.getAppointmentType().trim() : "ROUTINE");
        appointment.setStatus(CONFIRMED);
        appointment.setIdempotencyKey(idemKey);
        appointment.setBookingChannel(request.getBookingChannel() != null ? request.getBookingChannel() : "INTERNAL");
        appointment.setBookedBy(request.getBookedBy());
        appointment.setSlotTemplateId(request.getSlotTemplateId());
        appointment.setSessionShift(request.getSessionShift());
        appointment.setSessionLabel(request.getSessionLabel());
        appointment.setNotificationPatientName(trimOrNull(request.getPatientSmsDisplayName()));
        appointment.setNotificationPatientPhone(trimOrNull(request.getPatientSmsPhone()));

        appointment.setTokenNumber(computeNextTokenForSlot(request, null));

        patientSmsEnrichmentService.enrichAppointmentNotificationGaps(appointment);
        normalizeAppointmentNotificationPhone(appointment);
        appointmentRepository.save(appointment);
        reservation.setReferenceId(appointment.getId().toString());
        reservationRepository.save(reservation);

        appointmentEventPublisher.publishCreated(appointment);
        schedulingMetrics.incrementAppointmentsCreated();
        auditLogService.record("APPOINTMENT", appointment.getId(), "CREATED",
                appointment.getBookedBy(), null, appointment.getBookingChannel(), null, idemKey, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "CREATED"));
        return toResponse(appointment);
    }

    /** Phase 4: create appointment with multi-resource reservations (e.g. doctor + room). */
    private AppointmentResponse createWithMultiResource(CreateAppointmentRequest request, String idemKey, List<UUID> additionalIds) {
        assertNoDuplicatePatientSlot(request);
        List<UUID> resourceIds = new ArrayList<>();
        resourceIds.add(request.getResourceId());
        resourceIds.addAll(additionalIds);
        resourceIds = new ArrayList<>(new LinkedHashSet<>(resourceIds));
        SchedulingResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + request.getResourceId()));
        CheckConflictsRequest checkReq = new CheckConflictsRequest();
        checkReq.setResourceId(request.getResourceId());
        checkReq.setSlotStart(request.getSlotStart());
        checkReq.setSlotEnd(request.getSlotEnd());
        checkReq.setExcludeReservationId(null);
        ConflictCheckResponse conflict = reservationService.checkConflicts(checkReq);
        if (conflict.isHasConflict()) {
            throw new IllegalStateException("Slot conflicts with existing reservation(s)");
        }
        Integer maxPerSlot = effectiveMaxPerSlot(resource);
        int overlappingCount = conflict.getConflictingReservations() != null ? conflict.getConflictingReservations().size() : 0;
        if (maxPerSlot != null && (overlappingCount + 1) > maxPerSlot) {
            String overrideReason = trimOrNull(request.getOverbookingOverrideReason());
            if (overrideReason == null) {
                throw new IllegalStateException("Slot is at capacity (max " + maxPerSlot + " per slot). Provide overbookingOverrideReason to allow overbooking.");
            }
        }
        CreateMultiResourceReservationRequest multiReq = new CreateMultiResourceReservationRequest();
        multiReq.setResourceIds(resourceIds);
        multiReq.setSlotStart(request.getSlotStart());
        multiReq.setSlotEnd(request.getSlotEnd());
        multiReq.setReferenceType(APPOINTMENT_REFERENCE_TYPE);
        multiReq.setReferenceId(null);
        multiReq.setPatientId(request.getPatientId());
        multiReq.setIdempotencyKey(idemKey);
        List<ReservationResponse> created = reservationService.createMulti(multiReq);
        ReservationResponse primaryRes = created.stream().filter(r -> request.getResourceId().equals(r.getResourceId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Primary reservation not in createMulti result"));
        Optional<Appointment> existingApp = appointmentRepository.findByReservationId(primaryRes.getId());
        if (existingApp.isPresent()) {
            return toResponse(existingApp.get());
        }
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setReservationId(primaryRes.getId());
        appointment.setPatientId(request.getPatientId());
        appointment.setResourceId(request.getResourceId());
        appointment.setClinicOrLocationId(request.getClinicOrLocationId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setSlotStart(request.getSlotStart());
        appointment.setSlotEnd(request.getSlotEnd());
        appointment.setAppointmentType(trimOrNull(request.getAppointmentType()) != null ? request.getAppointmentType().trim() : "ROUTINE");
        appointment.setStatus(CONFIRMED);
        appointment.setIdempotencyKey(idemKey);
        appointment.setBookingChannel(request.getBookingChannel() != null ? request.getBookingChannel() : "INTERNAL");
        appointment.setBookedBy(request.getBookedBy());
        appointment.setSlotTemplateId(request.getSlotTemplateId());
        appointment.setSessionShift(request.getSessionShift());
        appointment.setSessionLabel(request.getSessionLabel());
        appointment.setNotificationPatientName(trimOrNull(request.getPatientSmsDisplayName()));
        appointment.setNotificationPatientPhone(trimOrNull(request.getPatientSmsPhone()));
        appointment.setTokenNumber(computeNextTokenForSlot(request, null));
        patientSmsEnrichmentService.enrichAppointmentNotificationGaps(appointment);
        normalizeAppointmentNotificationPhone(appointment);
        appointmentRepository.save(appointment);
        for (ReservationResponse rr : created) {
            Reservation r = reservationRepository.findById(rr.getId()).orElseThrow();
            r.setReferenceId(appointment.getId().toString());
            reservationRepository.save(r);
        }
        appointmentEventPublisher.publishCreated(appointment);
        schedulingMetrics.incrementAppointmentsCreated();
        auditLogService.record("APPOINTMENT", appointment.getId(), "CREATED",
                appointment.getBookedBy(), null, appointment.getBookingChannel(), null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "CREATED"));
        return toResponse(appointment);
    }

    public AppointmentDetailResponse getById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));
        AppointmentDetailResponse detail = new AppointmentDetailResponse();
        copyToResponse(appointment, detail);
        detail.setReservation(reservationService.getById(reservation.getId()));
        return detail;
    }

    public PagedResponse<AppointmentResponse> list(UUID patientId, UUID resourceId, UUID clinicId,
                                                   LocalDate fromDate, LocalDate toDate, String status,
                                                   String appointmentType,
                                                   UUID slotTemplateId, String sessionShift, String bookingChannel,
                                                   int page, int size) {
        Specification<Appointment> spec = AppointmentSpecifications.hasPatientId(patientId)
                .and(AppointmentSpecifications.hasResourceId(resourceId))
                .and(AppointmentSpecifications.hasClinicOrLocationId(clinicId))
                .and(AppointmentSpecifications.appointmentDateFrom(fromDate))
                .and(AppointmentSpecifications.appointmentDateTo(toDate))
                .and(AppointmentSpecifications.hasStatus(status))
                .and(AppointmentSpecifications.hasAppointmentType(appointmentType))
                .and(AppointmentSpecifications.hasSlotTemplateId(slotTemplateId))
                .and(AppointmentSpecifications.hasSessionShift(sessionShift))
                .and(AppointmentSpecifications.hasBookingChannel(bookingChannel));
        Page<Appointment> p = appointmentRepository.findAll(spec, PageRequest.of(page, size));
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
    public AppointmentResponse reschedule(UUID id, RescheduleAppointmentRequest request) {
        if (request.getNewSlotEnd() == null || request.getNewSlotStart() == null || !request.getNewSlotEnd().isAfter(request.getNewSlotStart())) {
            throw new IllegalArgumentException("newSlotEnd must be after newSlotStart");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        String rescheduleIdemKey = trimOrNull(request.getIdempotencyKey());
        if (rescheduleIdemKey != null && rescheduleIdemKey.equals(appointment.getRescheduleIdempotencyKey())) {
            return toResponse(appointment);
        }
        if (!CONFIRMED.equals(appointment.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED appointments can be rescheduled");
        }
        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));

        CheckConflictsRequest checkReq = new CheckConflictsRequest();
        checkReq.setResourceId(appointment.getResourceId());
        checkReq.setSlotStart(request.getNewSlotStart());
        checkReq.setSlotEnd(request.getNewSlotEnd());
        checkReq.setExcludeReservationId(reservation.getId());
        ConflictCheckResponse conflict = reservationService.checkConflicts(checkReq);
        if (conflict.isHasConflict()) {
            throw new IllegalStateException("New slot conflicts with existing reservation(s)");
        }

        LocalDate previousDate = appointment.getAppointmentDate();
        OffsetDateTime previousSlotStart = appointment.getSlotStart();
        OffsetDateTime previousSlotEnd = appointment.getSlotEnd();
        LocalDate newDate = request.getNewSlotStart().toLocalDate();
        assertNoDuplicatePatientSlot(
                appointment.getPatientId(),
                appointment.getResourceId(),
                newDate,
                request.getNewSlotStart(),
                request.getNewSlotEnd(),
                appointment.getId());

        reservation.setSlotStart(request.getNewSlotStart());
        reservation.setSlotEnd(request.getNewSlotEnd());
        reservationRepository.save(reservation);

        appointment.setAppointmentDate(newDate);
        appointment.setSlotStart(request.getNewSlotStart());
        appointment.setSlotEnd(request.getNewSlotEnd());
        boolean slotOrDateChanged = previousDate == null
                || !previousDate.equals(newDate)
                || !slotsEqual(previousSlotStart, request.getNewSlotStart())
                || !slotsEqual(previousSlotEnd, request.getNewSlotEnd());
        if (slotOrDateChanged) {
            CreateAppointmentRequest tokenReq = new CreateAppointmentRequest();
            tokenReq.setResourceId(appointment.getResourceId());
            tokenReq.setAppointmentDate(newDate);
            tokenReq.setSlotStart(request.getNewSlotStart());
            tokenReq.setSlotEnd(request.getNewSlotEnd());
            tokenReq.setSerialStartFrom(request.getSerialStartFrom());
            appointment.setTokenNumber(computeNextTokenForSlot(tokenReq, appointment.getId()));
        }
        appointmentRepository.save(appointment);
        if (rescheduleIdemKey != null) {
            appointment.setRescheduleIdempotencyKey(rescheduleIdemKey);
            appointmentRepository.save(appointment);
        }

        String reqSmsName = trimOrNull(request.getPatientSmsDisplayName());
        String reqSmsPhone = trimOrNull(request.getPatientSmsPhone());
        if (reqSmsName != null) {
            appointment.setNotificationPatientName(reqSmsName);
        }
        if (reqSmsPhone != null) {
            appointment.setNotificationPatientPhone(reqSmsPhone);
        }
        patientSmsEnrichmentService.enrichAppointmentNotificationGaps(appointment);
        normalizeAppointmentNotificationPhone(appointment);
        appointmentRepository.save(appointment);

        appointmentEventPublisher.publishRescheduled(appointment);
        auditLogService.record("APPOINTMENT", appointment.getId(), "RESCHEDULED",
                null, null, appointment.getBookingChannel(), null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "RESCHEDULED"));
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        String cancelIdemKey = request != null ? trimOrNull(request.getIdempotencyKey()) : null;
        if (cancelIdemKey != null && CANCELLED.equals(appointment.getStatus()) && cancelIdemKey.equals(appointment.getCancelIdempotencyKey())) {
            return toResponse(appointment);
        }
        SchedulingResource resource = resourceRepository.findById(appointment.getResourceId())
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + appointment.getResourceId()));
        Integer cutoffHours = effectiveCancellationCutoffHours(resource);
        if (cutoffHours != null) {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime cutoffTime = appointment.getSlotStart().minusHours(cutoffHours);
            if (!now.isBefore(cutoffTime)) {
                throw new IllegalStateException("Cancellation not allowed within " + cutoffHours + " hours of appointment start");
            }
        }
        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));
        reservation.setStatus(CANCELLED);
        reservationRepository.save(reservation);
        appointment.setStatus(CANCELLED);
        appointmentRepository.save(appointment);
        if (cancelIdemKey != null) {
            appointment.setCancelIdempotencyKey(cancelIdemKey);
            appointmentRepository.save(appointment);
        }
        appointmentEventPublisher.publishCancelled(appointment);
        auditLogService.record("APPOINTMENT", appointment.getId(), "CANCELLED",
                null, null, appointment.getBookingChannel(),
                request != null ? trimOrNull(request.getReason()) : null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "CANCELLED"));
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse checkIn(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        if (CANCELLED.equals(appointment.getStatus())) {
            throw new IllegalStateException("Cannot check in cancelled appointment");
        }
        if (COMPLETED.equals(appointment.getStatus())) {
            throw new IllegalStateException("Cannot check in completed appointment");
        }
        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));
        reservation.setStatus(CHECKED_IN);
        reservationRepository.save(reservation);
        appointment.setStatus(CHECKED_IN);
        appointmentRepository.save(appointment);
        appointmentEventPublisher.publishCheckedIn(appointment);
        auditLogService.record("APPOINTMENT", appointment.getId(), "CHECKED_IN",
                null, null, appointment.getBookingChannel(), null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "CHECKED_IN"));
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse noShow(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        if (!CONFIRMED.equals(appointment.getStatus())) {
            throw new IllegalStateException("No-show is only allowed from CONFIRMED status");
        }
        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));
        reservation.setStatus(NO_SHOW);
        reservationRepository.save(reservation);
        appointment.setStatus(NO_SHOW);
        appointmentRepository.save(appointment);
        appointmentEventPublisher.publishNoShow(appointment);
        auditLogService.record("APPOINTMENT", appointment.getId(), "NO_SHOW",
                null, null, appointment.getBookingChannel(), null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "NO_SHOW"));
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        if (COMPLETED.equals(appointment.getStatus())) {
            return toResponse(appointment);
        }
        if (CANCELLED.equals(appointment.getStatus())) {
            throw new IllegalStateException("Cannot complete cancelled appointment");
        }
        if (!CONFIRMED.equals(appointment.getStatus()) && !CHECKED_IN.equals(appointment.getStatus())) {
            throw new IllegalStateException("Cannot complete appointment in status: " + appointment.getStatus());
        }

        Reservation reservation = reservationRepository.findById(appointment.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + appointment.getReservationId()));
        reservation.setStatus(COMPLETED);
        reservationRepository.save(reservation);
        appointment.setStatus(COMPLETED);
        appointmentRepository.save(appointment);

        appointmentEventPublisher.publishCompleted(appointment);
        auditLogService.record("APPOINTMENT", appointment.getId(), "COMPLETED",
                null, null, appointment.getBookingChannel(), null, null, null, null);
        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "COMPLETED"));
        return toResponse(appointment);
    }

    @Transactional
    public void delete(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found: " + id));
        UUID primaryReservationId = appointment.getReservationId();

        // Delete appointment first to release the FK reference to the reservation,
        // then delete all associated reservations to fully free the slot(s).
        appointmentRepository.delete(appointment);
        appointmentRepository.flush();

        // Delete additional reservations linked via referenceId (multi-resource appointments).
        List<Reservation> linked = reservationRepository.findByReferenceTypeAndReferenceId(
                APPOINTMENT_REFERENCE_TYPE, id.toString());
        if (!linked.isEmpty()) {
            reservationRepository.deleteAll(linked);
            reservationRepository.flush();
        }

        // Delete the primary reservation if not already removed above.
        if (primaryReservationId != null) {
            reservationRepository.findById(primaryReservationId)
                    .ifPresent(r -> reservationRepository.delete(r));
        }

        queueEventBroadcaster.broadcast(appointment.getResourceId(), appointment.getAppointmentDate(),
                buildQueueEvent(appointment, "DELETED"));
    }

    public QueueResponse getQueue(UUID resourceId, LocalDate date) {
        List<Appointment> list = appointmentRepository.findByResourceIdAndAppointmentDate(resourceId, date);
        List<Appointment> active = list.stream()
                .filter(a -> {
                    String st = a.getStatus();
                    return CHECKED_IN.equals(st) || CONFIRMED.equals(st);
                })
                .sorted(Comparator
                        .comparing((Appointment a) -> CHECKED_IN.equals(a.getStatus()) ? 0 : 1)
                        .thenComparing(Appointment::getSlotStart)
                        .thenComparing(a -> a.getTokenNumber() != null ? a.getTokenNumber() : 0))
                .toList();
        QueueResponse response = new QueueResponse();
        response.setResourceId(resourceId);
        response.setDate(date);
        response.setAppointments(active.stream().map(this::toResponse).toList());
        return response;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Resolve effective max reservations per slot for overbooking policy.
     * Order: scheduling_booking_rules (RESOURCE > BRANCH > GLOBAL), then slot template slots_per_interval for resource type/branch, default 1.
     * Configurable per resource via booking rules; otherwise falls back to slot template capacity.
     */
    private Integer effectiveMaxPerSlot(SchedulingResource resource) {
        List<BookingRule> resourceRules = bookingRuleRepository.findByScopeTypeAndScopeId("RESOURCE", resource.getId());
        for (BookingRule r : resourceRules) {
            if (r.getMaxPerSlot() != null) return r.getMaxPerSlot();
        }
        if (resource.getBranchId() != null) {
            List<BookingRule> branchRules = bookingRuleRepository.findByScopeTypeAndScopeId("BRANCH", resource.getBranchId());
            for (BookingRule r : branchRules) {
                if (r.getMaxPerSlot() != null) return r.getMaxPerSlot();
            }
        }
        List<BookingRule> globalRules = bookingRuleRepository.findByScopeTypeAndScopeIdIsNull("GLOBAL");
        for (BookingRule r : globalRules) {
            if (r.getMaxPerSlot() != null) return r.getMaxPerSlot();
        }
        Integer fromTemplate = maxPerSlotFromSlotTemplate(resource);
        return fromTemplate != null ? fromTemplate : 1;
    }

    /** Fallback: slot template slots_per_interval for resource type (and branch if set). */
    private Integer maxPerSlotFromSlotTemplate(SchedulingResource resource) {
        String resourceType = resource.getResourceType();
        if (resourceType == null || resourceType.isBlank()) return null;
        Specification<SlotTemplate> base = SlotTemplateSpecifications.hasResourceType(resourceType)
                .and(SlotTemplateSpecifications.hasStatus("ACTIVE"));
        if (resource.getBranchId() != null) {
            Page<SlotTemplate> withBranch = slotTemplateRepository.findAll(
                    base.and(SlotTemplateSpecifications.hasBranchId(resource.getBranchId())),
                    PageRequest.of(0, 1));
            if (!withBranch.isEmpty()) return withBranch.getContent().get(0).getSlotsPerInterval();
        }
        Page<SlotTemplate> anyBranch = slotTemplateRepository.findAll(base, PageRequest.of(0, 1));
        return anyBranch.isEmpty() ? null : anyBranch.getContent().get(0).getSlotsPerInterval();
    }

    /** Resolve effective cancellation_cutoff_hours from booking rules: RESOURCE > BRANCH > GLOBAL. */
    private Integer effectiveCancellationCutoffHours(SchedulingResource resource) {
        List<BookingRule> resourceRules = bookingRuleRepository.findByScopeTypeAndScopeId("RESOURCE", resource.getId());
        for (BookingRule r : resourceRules) {
            if (r.getCancellationCutoffHours() != null) return r.getCancellationCutoffHours();
        }
        if (resource.getBranchId() != null) {
            List<BookingRule> branchRules = bookingRuleRepository.findByScopeTypeAndScopeId("BRANCH", resource.getBranchId());
            for (BookingRule r : branchRules) {
                if (r.getCancellationCutoffHours() != null) return r.getCancellationCutoffHours();
            }
        }
        List<BookingRule> globalRules = bookingRuleRepository.findByScopeTypeAndScopeIdIsNull("GLOBAL");
        for (BookingRule r : globalRules) {
            if (r.getCancellationCutoffHours() != null) return r.getCancellationCutoffHours();
        }
        return null;
    }

    private AppointmentResponse toResponse(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        copyToResponse(a, r);
        return r;
    }

    private void copyToResponse(Appointment a, AppointmentResponse r) {
        r.setId(a.getId());
        r.setReservationId(a.getReservationId());
        r.setPatientId(a.getPatientId());
        r.setResourceId(a.getResourceId());
        r.setClinicOrLocationId(a.getClinicOrLocationId());
        r.setAppointmentDate(a.getAppointmentDate());
        r.setSlotStart(a.getSlotStart());
        r.setSlotEnd(a.getSlotEnd());
        r.setAppointmentType(a.getAppointmentType());
        r.setStatus(a.getStatus());
        r.setVisitId(a.getVisitId());
        r.setTokenNumber(a.getTokenNumber());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setCreatedBy(a.getCreatedBy());
        r.setBookingChannel(a.getBookingChannel());
        r.setBookedBy(a.getBookedBy());
        r.setSlotTemplateId(a.getSlotTemplateId());
        r.setSessionShift(a.getSessionShift());
        r.setSessionLabel(a.getSessionLabel());
        r.setNotificationPatientPhone(a.getNotificationPatientPhone());
        r.setNotificationPatientName(a.getNotificationPatientName());
        r.setVersion(a.getVersion());
    }

    /**
     * Serial (SL) per doctor-day: increments within the same slot; otherwise ordered by earlier slot times
     * (morning appointments receive lower numbers than afternoon).
     */
    private static void normalizeAppointmentNotificationPhone(Appointment appointment) {
        String normalized = CommunicationAppointmentEventPublisher.normalizeRecipientPhone(
                appointment.getNotificationPatientPhone());
        if (normalized != null && !normalized.isBlank()) {
            appointment.setNotificationPatientPhone(normalized);
        }
    }

    private int computeNextTokenForSlot(CreateAppointmentRequest request, UUID excludeAppointmentId) {
        List<Appointment> sameDay = appointmentRepository.findByResourceIdAndAppointmentDateAndStatusNotIn(
                request.getResourceId(),
                request.getAppointmentDate(),
                List.of(CANCELLED, NO_SHOW));
        List<Appointment> activeDay = sameDay.stream()
                .filter(a -> excludeAppointmentId == null || !excludeAppointmentId.equals(a.getId()))
                .toList();

        List<Appointment> sameInstant = activeDay.stream()
                .filter(a -> slotsEqual(a.getSlotStart(), request.getSlotStart())
                        && slotsEqual(a.getSlotEnd(), request.getSlotEnd()))
                .toList();
        if (!sameInstant.isEmpty()) {
            int maxToken = sameInstant.stream()
                    .mapToInt(a -> a.getTokenNumber() != null ? a.getTokenNumber() : 0)
                    .max()
                    .orElse(0);
            if (maxToken > 0) {
                return maxToken + 1;
            }
        }

        int base = request.getSerialStartFrom() != null && request.getSerialStartFrom() > 0
                ? request.getSerialStartFrom()
                : 1;
        if (request.getSlotStart() == null) {
            int maxDay = activeDay.stream()
                    .mapToInt(a -> a.getTokenNumber() != null ? a.getTokenNumber() : 0)
                    .max()
                    .orElse(0);
            return maxDay > 0 ? maxDay + 1 : base;
        }
        long earlierSlots = activeDay.stream()
                .filter(a -> a.getSlotStart() != null
                        && request.getSlotStart() != null
                        && a.getSlotStart().toInstant().isBefore(request.getSlotStart().toInstant()))
                .count();
        return base + (int) earlierSlots;
    }

    /** Same wall-clock instant even when persisted/API offsets differ (e.g. Z vs +06:00). */
    private static boolean slotsEqual(OffsetDateTime a, OffsetDateTime b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.toInstant().equals(b.toInstant());
    }

    private void assertNoDuplicatePatientSlot(CreateAppointmentRequest request) {
        if (request.getPatientId() == null
                || request.getResourceId() == null
                || request.getAppointmentDate() == null
                || request.getSlotStart() == null
                || request.getSlotEnd() == null) {
            return;
        }
        assertNoDuplicatePatientSlot(
                request.getPatientId(),
                request.getResourceId(),
                request.getAppointmentDate(),
                request.getSlotStart(),
                request.getSlotEnd(),
                null);
    }

    private void assertNoDuplicatePatientSlot(
            UUID patientId,
            UUID resourceId,
            LocalDate appointmentDate,
            OffsetDateTime slotStart,
            OffsetDateTime slotEnd,
            UUID excludeAppointmentId) {
        List<Appointment> sameDay = appointmentRepository.findByPatientIdAndResourceIdAndAppointmentDateAndStatusNotIn(
                patientId, resourceId, appointmentDate, List.of(CANCELLED, NO_SHOW));
        boolean duplicateDay = sameDay.stream()
                .anyMatch(a -> excludeAppointmentId == null || !excludeAppointmentId.equals(a.getId()));
        if (duplicateDay) {
            throw new IllegalStateException(
                    "This patient already has an appointment with this doctor on this date.");
        }
        List<Appointment> resourceDay = appointmentRepository.findByResourceIdAndAppointmentDateAndStatusNotIn(
                resourceId, appointmentDate, List.of(CANCELLED, NO_SHOW));
        boolean duplicateExact = resourceDay.stream()
                .filter(a -> excludeAppointmentId == null || !excludeAppointmentId.equals(a.getId()))
                .filter(a -> patientId.equals(a.getPatientId()))
                .anyMatch(a -> slotsEqual(a.getSlotStart(), slotStart) && slotsEqual(a.getSlotEnd(), slotEnd));
        if (duplicateExact) {
            throw new IllegalStateException(
                    "This patient already has an appointment with this doctor in the selected slot on this date.");
        }
        boolean duplicateStart = resourceDay.stream()
                .filter(a -> excludeAppointmentId == null || !excludeAppointmentId.equals(a.getId()))
                .filter(a -> patientId.equals(a.getPatientId()))
                .anyMatch(a -> slotsEqual(a.getSlotStart(), slotStart));
        if (duplicateStart) {
            throw new IllegalStateException(
                    "This patient already has an appointment with this doctor at this time on this date.");
        }
    }

    private QueueUpdateEvent buildQueueEvent(Appointment a, String eventType) {
        QueueUpdateEvent event = new QueueUpdateEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setAppointmentId(a.getId());
        event.setPatientId(a.getPatientId());
        event.setTokenNumber(a.getTokenNumber());
        event.setStatus(a.getStatus());
        event.setSlotStart(a.getSlotStart());
        event.setTimestamp(OffsetDateTime.now());
        return event;
    }
}
