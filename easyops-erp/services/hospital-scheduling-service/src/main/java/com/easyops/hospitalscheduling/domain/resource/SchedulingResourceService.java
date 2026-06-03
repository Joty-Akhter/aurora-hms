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
public class SchedulingResourceService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final SchedulingResourceRepository repository;

    @Transactional
    public ResourceResponse create(CreateResourceRequest request) {
        SchedulingResource entity = new SchedulingResource();
        entity.setResourceType(request.getResourceType().trim());
        entity.setExternalReferenceId(request.getExternalReferenceId().trim());
        entity.setName(request.getName().trim());
        entity.setBranchId(request.getBranchId());
        entity.setDepartmentId(request.getDepartmentId());
        entity.setMetadata(trimOrNull(request.getMetadata()));
        entity.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : DEFAULT_STATUS);
        repository.save(entity);
        return toResponse(entity);
    }

    public ResourceResponse getById(UUID id) {
        SchedulingResource entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<ResourceResponse> list(UUID branchId, UUID departmentId, String resourceType, String status, int page, int size) {
        Specification<SchedulingResource> spec = ResourceSpecifications.hasBranchId(branchId)
                .and(ResourceSpecifications.hasDepartmentId(departmentId))
                .and(ResourceSpecifications.hasResourceType(resourceType))
                .and(ResourceSpecifications.hasStatus(status));
        Page<SchedulingResource> p = repository.findAll(spec, PageRequest.of(page, size));
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
    public ResourceResponse update(UUID id, UpdateResourceRequest request) {
        SchedulingResource entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
        Optional.ofNullable(request.getResourceType()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setResourceType(s.trim()));
        Optional.ofNullable(request.getExternalReferenceId()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setExternalReferenceId(s.trim()));
        Optional.ofNullable(request.getName()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setName(s.trim()));
        if (request.getBranchId() != null) entity.setBranchId(request.getBranchId());
        if (request.getDepartmentId() != null) entity.setDepartmentId(request.getDepartmentId());
        if (request.getMetadata() != null) entity.setMetadata(trimOrNull(request.getMetadata()));
        Optional.ofNullable(request.getStatus()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setStatus(s.trim()));
        repository.save(entity);
        return toResponse(entity);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ResourceResponse toResponse(SchedulingResource e) {
        ResourceResponse r = new ResourceResponse();
        r.setId(e.getId());
        r.setResourceType(e.getResourceType());
        r.setExternalReferenceId(e.getExternalReferenceId());
        r.setName(e.getName());
        r.setBranchId(e.getBranchId());
        r.setDepartmentId(e.getDepartmentId());
        r.setMetadata(e.getMetadata());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
