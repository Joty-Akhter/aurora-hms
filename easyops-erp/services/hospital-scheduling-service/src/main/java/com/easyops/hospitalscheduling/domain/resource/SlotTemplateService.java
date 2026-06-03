package com.easyops.hospitalscheduling.domain.resource;

import com.easyops.hospitalscheduling.api.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotTemplateService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final SlotTemplateRepository repository;

    @Transactional
    public SlotTemplateResponse create(CreateSlotTemplateRequest request) {
        SlotTemplate entity = new SlotTemplate();
        entity.setName(request.getName().trim());
        entity.setResourceType(trimOrNull(request.getResourceType()));
        entity.setBranchId(request.getBranchId());
        entity.setSlotDurationMinutes(request.getSlotDurationMinutes());
        entity.setSlotsPerInterval(request.getSlotsPerInterval() != null ? request.getSlotsPerInterval() : 1);
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setLeadTimeDays(request.getLeadTimeDays() != null ? request.getLeadTimeDays() : 0);
        entity.setMaxAdvanceDays(request.getMaxAdvanceDays());
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : DEFAULT_STATUS);
        repository.save(entity);
        return toResponse(entity);
    }

    public SlotTemplateResponse getById(UUID id) {
        SlotTemplate entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Slot template not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<SlotTemplateResponse> list(String resourceType, UUID branchId, String status, int page, int size) {
        Specification<SlotTemplate> spec = SlotTemplateSpecifications.hasResourceType(resourceType)
                .and(SlotTemplateSpecifications.hasBranchId(branchId))
                .and(SlotTemplateSpecifications.hasStatus(status));
        Page<SlotTemplate> p = repository.findAll(spec, PageRequest.of(page, size));
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
    public SlotTemplateResponse update(UUID id, UpdateSlotTemplateRequest request) {
        SlotTemplate entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Slot template not found: " + id));
        Optional.ofNullable(request.getName()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setName(s.trim()));
        if (request.getResourceType() != null) entity.setResourceType(trimOrNull(request.getResourceType()));
        if (request.getBranchId() != null) entity.setBranchId(request.getBranchId());
        Optional.ofNullable(request.getSlotDurationMinutes()).ifPresent(entity::setSlotDurationMinutes);
        Optional.ofNullable(request.getSlotsPerInterval()).ifPresent(entity::setSlotsPerInterval);
        Optional.ofNullable(request.getStartTime()).ifPresent(entity::setStartTime);
        Optional.ofNullable(request.getEndTime()).ifPresent(entity::setEndTime);
        if (request.getLeadTimeDays() != null) entity.setLeadTimeDays(request.getLeadTimeDays());
        if (request.getMaxAdvanceDays() != null) entity.setMaxAdvanceDays(request.getMaxAdvanceDays());
        Optional.ofNullable(request.getStatus()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setStatus(s.trim()));
        repository.save(entity);
        return toResponse(entity);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private SlotTemplateResponse toResponse(SlotTemplate e) {
        SlotTemplateResponse r = new SlotTemplateResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setResourceType(e.getResourceType());
        r.setBranchId(e.getBranchId());
        r.setSlotDurationMinutes(e.getSlotDurationMinutes());
        r.setSlotsPerInterval(e.getSlotsPerInterval());
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        r.setLeadTimeDays(e.getLeadTimeDays());
        r.setMaxAdvanceDays(e.getMaxAdvanceDays());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
