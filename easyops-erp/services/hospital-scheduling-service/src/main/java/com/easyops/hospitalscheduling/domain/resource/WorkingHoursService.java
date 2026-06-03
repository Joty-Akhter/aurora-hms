package com.easyops.hospitalscheduling.domain.resource;

import com.easyops.hospitalscheduling.api.dto.SetWorkingHoursRequest;
import com.easyops.hospitalscheduling.api.dto.WorkingHoursEntryDto;
import com.easyops.hospitalscheduling.api.dto.WorkingHoursResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkingHoursService {

    private final WorkingHoursRepository workingHoursRepository;
    private final SchedulingResourceRepository resourceRepository;

    /**
     * Set working hours for a resource (replaces any existing). Creates one row per entry.
     */
    @Transactional
    public List<WorkingHoursResponse> setWorkingHours(UUID resourceId, SetWorkingHoursRequest request) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new NoSuchElementException("Resource not found: " + resourceId);
        }
        workingHoursRepository.deleteByResourceId(resourceId);
        List<WorkingHoursEntryDto> entries = dedupeEntries(request.getEntries());
        for (WorkingHoursEntryDto entry : entries) {
            WorkingHours wh = new WorkingHours();
            wh.setResourceId(resourceId);
            wh.setDayOfWeek(entry.getDayOfWeek().shortValue());
            wh.setStartTime(entry.getStartTime());
            wh.setEndTime(entry.getEndTime());
            wh.setEffectiveFrom(entry.getEffectiveFrom());
            wh.setEffectiveTo(entry.getEffectiveTo());
            wh.setSlotDurationMinutes(entry.getSlotDurationMinutes());
            wh.setSlotsPerInterval(entry.getSlotsPerInterval());
            wh.setMaxSlotsPerSegment(entry.getMaxSlotsPerSegment());
            workingHoursRepository.save(wh);
        }
        return getWorkingHours(resourceId);
    }

    public List<WorkingHoursResponse> getWorkingHours(UUID resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new NoSuchElementException("Resource not found: " + resourceId);
        }
        return workingHoursRepository.findByResourceIdOrderByDayOfWeekAscStartTimeAsc(resourceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Merge duplicate day + time-window rows (keeps highest maxSlotsPerSegment when present).
     */
    private List<WorkingHoursEntryDto> dedupeEntries(List<WorkingHoursEntryDto> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        Map<String, WorkingHoursEntryDto> byKey = new LinkedHashMap<>();
        for (WorkingHoursEntryDto entry : entries) {
            if (entry.getDayOfWeek() == null || entry.getStartTime() == null || entry.getEndTime() == null) {
                continue;
            }
            String key = entry.getDayOfWeek() + "|" + entry.getStartTime() + "|" + entry.getEndTime();
            WorkingHoursEntryDto existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, entry);
                continue;
            }
            Integer existingMax = existing.getMaxSlotsPerSegment();
            Integer entryMax = entry.getMaxSlotsPerSegment();
            if (entryMax != null && (existingMax == null || entryMax > existingMax)) {
                existing.setMaxSlotsPerSegment(entryMax);
            }
            if (entry.getSlotDurationMinutes() != null
                    && (existing.getSlotDurationMinutes() == null
                    || entry.getSlotDurationMinutes() < existing.getSlotDurationMinutes())) {
                existing.setSlotDurationMinutes(entry.getSlotDurationMinutes());
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private WorkingHoursResponse toResponse(WorkingHours e) {
        WorkingHoursResponse r = new WorkingHoursResponse();
        r.setId(e.getId());
        r.setResourceId(e.getResourceId());
        r.setDayOfWeek(e.getDayOfWeek() != null ? e.getDayOfWeek().intValue() : null);
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        r.setEffectiveFrom(e.getEffectiveFrom());
        r.setEffectiveTo(e.getEffectiveTo());
        r.setSlotDurationMinutes(e.getSlotDurationMinutes());
        r.setSlotsPerInterval(e.getSlotsPerInterval());
        r.setMaxSlotsPerSegment(e.getMaxSlotsPerSegment());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
