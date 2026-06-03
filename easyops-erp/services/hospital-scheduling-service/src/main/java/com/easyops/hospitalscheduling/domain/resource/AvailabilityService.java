package com.easyops.hospitalscheduling.domain.resource;

import com.easyops.hospitalscheduling.api.dto.AvailabilityResponse;
import com.easyops.hospitalscheduling.api.dto.SlotAvailabilityDto;
import com.easyops.hospitalscheduling.config.SchedulingTimeZoneResolver;
import com.easyops.hospitalscheduling.domain.reservation.ReservationRepository;
import com.easyops.hospitalscheduling.domain.roster.RosterBlock;
import com.easyops.hospitalscheduling.domain.roster.RosterBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int DEFAULT_SLOT_DURATION_MINUTES = 30;
    private static final int DEFAULT_SLOTS_PER_INTERVAL = 1;
    // When no slot template is selected, honor full working-hours ranges configured per doctor/resource.
    private static final LocalTime DEFAULT_WINDOW_START = LocalTime.MIN;
    private static final LocalTime DEFAULT_WINDOW_END = LocalTime.MAX;

    private final SchedulingResourceRepository resourceRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final BlackoutRepository blackoutRepository;
    private final SlotTemplateRepository slotTemplateRepository;
    private final ReservationRepository reservationRepository;
    private final RosterBlockRepository rosterBlockRepository;
    private final SchedulingTimeZoneResolver schedulingTimeZoneResolver;

    /**
     * Returns availability per day in the range [fromDate, toDate]. For each day: working hours (by day-of-week),
     * blackouts, and slot template (or defaults) are applied; slots are generated and availableCount is
     * capacity minus overlapping non-cancelled reservations.
     */
    public List<AvailabilityResponse> getAvailability(UUID resourceId, LocalDate fromDate, LocalDate toDate, UUID slotTemplateId) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate and toDate must be set and fromDate <= toDate");
        }
        var resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + resourceId));

        int slotDurationMinutes = DEFAULT_SLOT_DURATION_MINUTES;
        int slotsPerInterval = DEFAULT_SLOTS_PER_INTERVAL;
        LocalTime windowStart = DEFAULT_WINDOW_START;
        LocalTime windowEnd = DEFAULT_WINDOW_END;

        if (slotTemplateId != null) {
            Optional<SlotTemplate> template = slotTemplateRepository.findById(slotTemplateId);
            if (template.isPresent()) {
                slotDurationMinutes = template.get().getSlotDurationMinutes();
                slotsPerInterval = template.get().getSlotsPerInterval() != null ? template.get().getSlotsPerInterval() : 1;
                windowStart = template.get().getStartTime();
                windowEnd = template.get().getEndTime();
            }
        } else {
            Optional<SlotTemplate> def = resolveDefaultSlotTemplate(resource);
            if (def.isPresent()) {
                SlotTemplate t = def.get();
                slotDurationMinutes = t.getSlotDurationMinutes() != null ? t.getSlotDurationMinutes() : DEFAULT_SLOT_DURATION_MINUTES;
                slotsPerInterval = t.getSlotsPerInterval() != null ? t.getSlotsPerInterval() : DEFAULT_SLOTS_PER_INTERVAL;
                windowStart = t.getStartTime() != null ? t.getStartTime() : DEFAULT_WINDOW_START;
                windowEnd = t.getEndTime() != null ? t.getEndTime() : DEFAULT_WINDOW_END;
            }
        }

        final int templateDurationMins = slotDurationMinutes;
        final int templateCapacity = slotsPerInterval;
        final LocalTime wStart = windowStart;
        final LocalTime wEnd = windowEnd;
        final ZoneId hospitalZone = schedulingTimeZoneResolver.getDefaultZone();

        List<AvailabilityResponse> result = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            boolean blackedOut = !blackoutRepository.findByResourceIdAndBlackoutDateBetween(resourceId, date, date).isEmpty()
                    || (resource.getBranchId() != null && !blackoutRepository.findByBranchIdAndBlackoutDateBetween(resource.getBranchId(), date, date).isEmpty());

            List<SlotAvailabilityDto> slots = new ArrayList<>();
            if (!blackedOut) {
                int dayOfWeek = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : date.getDayOfWeek().getValue();
                List<WorkingHours> hours = workingHoursRepository.findByResourceIdOrderByDayOfWeekAscStartTimeAsc(resourceId).stream()
                        .filter(wh -> wh.getDayOfWeek() != null && wh.getDayOfWeek().intValue() == dayOfWeek)
                        .toList();

                for (WorkingHours wh : hours) {
                    LocalTime segmentStart = wh.getStartTime().isBefore(wStart) ? wStart : wh.getStartTime();
                    LocalTime segmentEnd = wh.getEndTime().isAfter(wEnd) ? wEnd : wh.getEndTime();
                    if (segmentStart.equals(segmentEnd)) continue;

                    int durationMins = templateDurationMins;
                    if (wh.getSlotDurationMinutes() != null && wh.getSlotDurationMinutes() > 0) {
                        durationMins = wh.getSlotDurationMinutes();
                    }
                    int capacity = templateCapacity;
                    if (wh.getSlotsPerInterval() != null && wh.getSlotsPerInterval() > 0) {
                        capacity = wh.getSlotsPerInterval();
                    }
                    generateSlotsForSegment(
                            slots,
                            resourceId,
                            date,
                            hospitalZone,
                            segmentStart,
                            segmentEnd,
                            durationMins,
                            capacity,
                            wh.getMaxSlotsPerSegment());
                }
                slots = dedupeSlotsByInstant(slots);
            }

            result.add(new AvailabilityResponse(resourceId, date, slots, blackedOut));
        }
        return result;
    }

    /**
     * Returns the total available slot units for a resource on a given date (for utilization reporting).
     * Uses working hours, blackouts, and slot template (or defaults). Blacked-out days return 0.
     * Slot units = (working segment minutes / slot duration) * slots per interval, summed over segments.
     */
    public long countAvailableSlotUnits(UUID resourceId, LocalDate date) {
        var resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) return 0;
        boolean blackedOut = !blackoutRepository.findByResourceIdAndBlackoutDateBetween(resourceId, date, date).isEmpty()
                || (resource.getBranchId() != null && !blackoutRepository.findByBranchIdAndBlackoutDateBetween(resource.getBranchId(), date, date).isEmpty());
        if (blackedOut) return 0;

        int slotDurationMinutes = DEFAULT_SLOT_DURATION_MINUTES;
        int slotsPerInterval = DEFAULT_SLOTS_PER_INTERVAL;
        LocalTime windowStart = DEFAULT_WINDOW_START;
        LocalTime windowEnd = DEFAULT_WINDOW_END;
        Optional<SlotTemplate> def = resolveDefaultSlotTemplate(resource);
        if (def.isPresent()) {
            SlotTemplate t = def.get();
            slotDurationMinutes = t.getSlotDurationMinutes() != null ? t.getSlotDurationMinutes() : DEFAULT_SLOT_DURATION_MINUTES;
            slotsPerInterval = t.getSlotsPerInterval() != null ? t.getSlotsPerInterval() : DEFAULT_SLOTS_PER_INTERVAL;
            windowStart = t.getStartTime() != null ? t.getStartTime() : DEFAULT_WINDOW_START;
            windowEnd = t.getEndTime() != null ? t.getEndTime() : DEFAULT_WINDOW_END;
        }

        int dayOfWeek = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : date.getDayOfWeek().getValue();
        List<WorkingHours> hours = workingHoursRepository.findByResourceIdOrderByDayOfWeekAscStartTimeAsc(resourceId).stream()
                .filter(wh -> wh.getDayOfWeek() != null && wh.getDayOfWeek().intValue() == dayOfWeek)
                .toList();
        if (hours.isEmpty()) return 0;

        final int templateDurationMins = slotDurationMinutes;
        final int templateCapacity = slotsPerInterval;
        final LocalTime wStart = windowStart;
        final LocalTime wEnd = windowEnd;
        long total = 0;
        for (WorkingHours wh : hours) {
            LocalTime segmentStart = wh.getStartTime().isBefore(wStart) ? wStart : wh.getStartTime();
            LocalTime segmentEnd = wh.getEndTime().isAfter(wEnd) ? wEnd : wh.getEndTime();
            if (segmentStart.equals(segmentEnd)) continue;
            int durationMins = templateDurationMins;
            if (wh.getSlotDurationMinutes() != null && wh.getSlotDurationMinutes() > 0) {
                durationMins = wh.getSlotDurationMinutes();
            }
            int capacity = templateCapacity;
            if (wh.getSlotsPerInterval() != null && wh.getSlotsPerInterval() > 0) {
                capacity = wh.getSlotsPerInterval();
            }
            if (wh.getMaxSlotsPerSegment() != null && wh.getMaxSlotsPerSegment() > 0) {
                total += (long) wh.getMaxSlotsPerSegment() * capacity;
            } else {
                long segmentMinutes = segmentDurationMinutes(segmentStart, segmentEnd);
                int slotCount = durationMins > 0 ? (int) (segmentMinutes / durationMins) : 0;
                total += (long) slotCount * capacity;
            }
        }
        return total;
    }

    /**
     * When {@code maxSlotsCap} is set (doctor schedule max patients), divide the segment evenly into exactly that
     * many bookable slots. Otherwise use fixed-duration stepping.
     */
    private void generateSlotsForSegment(
            List<SlotAvailabilityDto> slots,
            UUID resourceId,
            LocalDate date,
            ZoneId hospitalZone,
            LocalTime segmentStart,
            LocalTime segmentEnd,
            int durationMins,
            int capacity,
            Integer maxSlotsCap) {
        long segmentMinutes = segmentDurationMinutes(segmentStart, segmentEnd);
        if (segmentMinutes <= 0) {
            return;
        }

        if (maxSlotsCap != null && maxSlotsCap > 0) {
            for (int i = 0; i < maxSlotsCap; i++) {
                long startMin = (segmentMinutes * i) / maxSlotsCap;
                long endMin = (segmentMinutes * (i + 1)) / maxSlotsCap;
                if (endMin <= startMin) {
                    continue;
                }
                LocalTime slotStart = segmentStart.plusMinutes(startMin);
                LocalTime slotEndTime = segmentStart.plusMinutes(endMin);
                appendSlotIfBookable(slots, resourceId, date, hospitalZone, slotStart, slotEndTime, capacity);
            }
            return;
        }

        // Minute-based stepping (supports overnight segments where LocalTime end < start on the clock).
        for (long elapsed = 0; elapsed + durationMins <= segmentMinutes; elapsed += durationMins) {
            LocalTime slotStart = segmentStart.plusMinutes(elapsed);
            LocalTime slotEndTime = segmentStart.plusMinutes(elapsed + durationMins);
            appendSlotIfBookable(slots, resourceId, date, hospitalZone, slotStart, slotEndTime, capacity);
        }
    }

    /**
     * Collapse duplicate slot instants from overlapping working-hour rows (same start/end).
     * Keeps the entry with the highest available capacity.
     */
    private List<SlotAvailabilityDto> dedupeSlotsByInstant(List<SlotAvailabilityDto> slots) {
        Map<String, SlotAvailabilityDto> byKey = new LinkedHashMap<>();
        for (SlotAvailabilityDto slot : slots) {
            if (slot.getStart() == null || slot.getEnd() == null) {
                continue;
            }
            String key = slot.getStart().toString() + "|" + slot.getEnd().toString();
            SlotAvailabilityDto existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, slot);
            } else {
                existing.setAvailableCount(Math.max(existing.getAvailableCount(), slot.getAvailableCount()));
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private void appendSlotIfBookable(
            List<SlotAvailabilityDto> slots,
            UUID resourceId,
            LocalDate date,
            ZoneId hospitalZone,
            LocalTime slotStart,
            LocalTime slotEndTime,
            int capacity) {
        OffsetDateTime slotStartDt = date.atTime(slotStart).atZone(hospitalZone).toOffsetDateTime();
        OffsetDateTime slotEndDt = date.atTime(slotEndTime).atZone(hospitalZone).toOffsetDateTime();

        List<RosterBlock> overlappingBlocks =
                rosterBlockRepository.findOverlappingUnavailableOrSubstitute(resourceId, slotStartDt, slotEndDt);
        if (!overlappingBlocks.isEmpty()) {
            UUID substituteId = null;
            for (RosterBlock b : overlappingBlocks) {
                if ("SUBSTITUTE".equals(b.getType()) && b.getSubstituteResourceId() != null
                        && resourceRepository.existsById(b.getSubstituteResourceId())) {
                    substituteId = b.getSubstituteResourceId();
                    break;
                }
            }
            if (substituteId != null) {
                int overlapping = reservationRepository.findOverlapping(substituteId, slotStartDt, slotEndDt, null).size();
                int available = Math.max(0, capacity - overlapping);
                SlotAvailabilityDto dto = new SlotAvailabilityDto(slotStartDt, slotEndDt, available);
                dto.setSubstituteResourceId(substituteId);
                slots.add(dto);
            }
            return;
        }
        int overlapping = reservationRepository.findOverlapping(resourceId, slotStartDt, slotEndDt, null).size();
        int available = Math.max(0, capacity - overlapping);
        slots.add(new SlotAvailabilityDto(slotStartDt, slotEndDt, available));
    }

    /** Segment length in minutes; supports overnight windows (e.g. 22:00–01:00). */
    private static long segmentDurationMinutes(LocalTime segmentStart, LocalTime segmentEnd) {
        long minutes = segmentStart.until(segmentEnd, ChronoUnit.MINUTES);
        if (minutes <= 0) {
            minutes += 24 * 60L;
        }
        return minutes;
    }

    /**
     * Active slot template for a resource when the caller did not specify one: same rules as slot-unit counts.
     */
    private Optional<SlotTemplate> resolveDefaultSlotTemplate(SchedulingResource resource) {
        String resourceType = resource.getResourceType();
        if (resourceType == null || resourceType.isBlank()) {
            return Optional.empty();
        }
        Specification<SlotTemplate> base = SlotTemplateSpecifications.hasResourceType(resourceType)
                .and(SlotTemplateSpecifications.hasStatus("ACTIVE"));
        if (resource.getBranchId() != null) {
            Page<SlotTemplate> withBranch = slotTemplateRepository.findAll(
                    base.and(SlotTemplateSpecifications.hasBranchId(resource.getBranchId())),
                    PageRequest.of(0, 1));
            if (!withBranch.isEmpty()) {
                return Optional.of(withBranch.getContent().get(0));
            }
        }
        Page<SlotTemplate> anyBranch = slotTemplateRepository.findAll(base, PageRequest.of(0, 1));
        return anyBranch.isEmpty() ? Optional.empty() : Optional.of(anyBranch.getContent().get(0));
    }
}
