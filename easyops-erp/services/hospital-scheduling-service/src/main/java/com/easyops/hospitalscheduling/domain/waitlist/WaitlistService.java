package com.easyops.hospitalscheduling.domain.waitlist;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.appointment.AppointmentService;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private static final String PENDING = "PENDING";
    private static final String PROMOTED = "PROMOTED";
    private static final String CANCELLED = "CANCELLED";
    private static final String EXPIRED = "EXPIRED";
    private static final String ROUTINE = "ROUTINE";

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final SchedulingResourceRepository resourceRepository;
    private final AppointmentService appointmentService;

    @Transactional
    public WaitlistEntryResponse addEntry(CreateWaitlistEntryRequest request) {
        if (!resourceRepository.existsById(request.getResourceId())) {
            throw new NoSuchElementException("Resource not found: " + request.getResourceId());
        }
        WaitlistEntry entry = new WaitlistEntry();
        entry.setPatientId(request.getPatientId());
        entry.setResourceId(request.getResourceId());
        entry.setPreferredFromDate(request.getPreferredFromDate());
        entry.setPreferredToDate(request.getPreferredToDate());
        entry.setPriority(request.getPriority() != null ? request.getPriority().intValue() : 0);
        entry.setPriorityReason(trimOrNull(request.getPriorityReason()));
        entry.setStatus(PENDING);
        waitlistEntryRepository.save(entry);
        return toResponse(entry);
    }

    public PagedResponse<WaitlistEntryResponse> list(UUID resourceId, UUID patientId, String status, int page, int size) {
        Specification<WaitlistEntry> spec = WaitlistEntrySpecifications.hasResourceId(resourceId)
                .and(WaitlistEntrySpecifications.hasPatientId(patientId))
                .and(WaitlistEntrySpecifications.hasStatus(status));
        Page<WaitlistEntry> p = waitlistEntryRepository.findAll(spec, PageRequest.of(page, size));
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
    public WaitlistEntryResponse updateStatus(UUID id, UpdateWaitlistStatusRequest request) {
        WaitlistEntry entry = waitlistEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Waitlist entry not found: " + id));
        String newStatus = request.getStatus().trim();
        if (!List.of(PROMOTED, CANCELLED, EXPIRED).contains(newStatus)) {
            throw new IllegalArgumentException("status must be PROMOTED, CANCELLED, or EXPIRED");
        }
        entry.setStatus(newStatus);
        waitlistEntryRepository.save(entry);
        return toResponse(entry);
    }

    /**
     * Promote up to maxCandidates (default 1) pending entries for the resource to an appointment in the given slot.
     * Creates reservation+appointment for each, marks entry PROMOTED, returns first appointment and count promoted.
     */
    @Transactional
    public PromoteWaitlistResponse promote(PromoteWaitlistRequest request) {
        if (request.getSlotEnd() == null || request.getSlotStart() == null || !request.getSlotEnd().isAfter(request.getSlotStart())) {
            throw new IllegalArgumentException("slotEnd must be after slotStart");
        }
        if (!resourceRepository.existsById(request.getResourceId())) {
            throw new NoSuchElementException("Resource not found: " + request.getResourceId());
        }
        int maxCandidates = request.getMaxCandidates() != null && request.getMaxCandidates() > 0 ? request.getMaxCandidates() : 1;

        List<WaitlistEntry> pending = waitlistEntryRepository.findByResourceIdAndStatusOrderByPriorityDescCreatedAtAsc(
                request.getResourceId(), PENDING);
        List<WaitlistEntry> toPromote = pending.stream().limit(maxCandidates).toList();

        AppointmentResponse firstAppointment = null;
        int promoted = 0;
        for (WaitlistEntry entry : toPromote) {
            CreateAppointmentRequest appReq = new CreateAppointmentRequest();
            appReq.setPatientId(entry.getPatientId());
            appReq.setResourceId(request.getResourceId());
            appReq.setAppointmentDate(request.getSlotStart() != null ? request.getSlotStart().toLocalDate() : LocalDate.now());
            appReq.setSlotStart(request.getSlotStart());
            appReq.setSlotEnd(request.getSlotEnd());
            appReq.setAppointmentType(ROUTINE);
            try {
                AppointmentResponse created = appointmentService.create(appReq);
                if (firstAppointment == null) {
                    firstAppointment = created;
                }
                entry.setStatus(PROMOTED);
                waitlistEntryRepository.save(entry);
                promoted++;
            } catch (Exception e) {
                break;
            }
        }

        PromoteWaitlistResponse response = new PromoteWaitlistResponse();
        response.setAppointment(firstAppointment);
        response.setCandidatesContacted(promoted);
        return response;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry e) {
        WaitlistEntryResponse r = new WaitlistEntryResponse();
        r.setId(e.getId());
        r.setPatientId(e.getPatientId());
        r.setResourceId(e.getResourceId());
        r.setPreferredFromDate(e.getPreferredFromDate());
        r.setPreferredToDate(e.getPreferredToDate());
        r.setPriority(e.getPriority());
        r.setPriorityReason(e.getPriorityReason());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
