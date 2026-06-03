package com.easyops.hospitalscheduling.domain.doctormapping;

import com.easyops.hospitalscheduling.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class DoctorResourceMappingService {

    private final DoctorResourceMappingRepository repository;

    public DoctorResourceMappingService(DoctorResourceMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DoctorResourceMappingResponse create(CreateDoctorResourceMappingRequest req, UUID createdBy) {
        DoctorResourceMapping m = new DoctorResourceMapping();
        m.setDoctorUserId(req.getDoctorUserId());
        m.setResourceId(req.getResourceId());
        m.setBranchId(req.getBranchId());
        m.setPrimary(req.getIsPrimary() != null ? req.getIsPrimary() : true);
        m.setEffectiveFrom(req.getEffectiveFrom());
        m.setEffectiveTo(req.getEffectiveTo());
        m.setStatus("ACTIVE");
        m.setCreatedBy(createdBy);
        return toResponse(repository.save(m));
    }

    public PagedResponse<DoctorResourceMappingResponse> list(UUID doctorUserId, UUID resourceId, UUID branchId, String status, int page, int size) {
        Specification<DoctorResourceMapping> spec = Specification.where(null);
        if (doctorUserId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("doctorUserId"), doctorUserId));
        if (resourceId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("resourceId"), resourceId));
        if (branchId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("branchId"), branchId));
        if (status != null && !status.isBlank()) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        Page<DoctorResourceMapping> p = repository.findAll(spec, PageRequest.of(page, size));
        PagedResponse<DoctorResourceMappingResponse> resp = new PagedResponse<>();
        resp.setContent(p.getContent().stream().map(this::toResponse).toList());
        resp.setTotalElements(p.getTotalElements());
        resp.setTotalPages(p.getTotalPages());
        resp.setNumber(p.getNumber());
        resp.setSize(p.getSize());
        resp.setFirst(p.isFirst());
        resp.setLast(p.isLast());
        return resp;
    }

    public DoctorResourceMappingResponse resolve(UUID doctorUserId, UUID branchId) {
        // Fallback order: exact branch → null-branch → throw
        if (branchId != null) {
            List<DoctorResourceMapping> exact = repository.findByDoctorUserIdAndBranchIdAndStatus(doctorUserId, branchId, "ACTIVE");
            if (!exact.isEmpty()) return toResponse(exact.get(0));
        }
        // null-branch fallback
        return repository.findByDoctorUserIdAndBranchIdIsNullAndStatus(doctorUserId, "ACTIVE")
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("DOCTOR_RESOURCE_MAPPING_NOT_FOUND: no active mapping for doctorUserId=" + doctorUserId));
    }

    @Transactional
    public DoctorResourceMappingResponse update(UUID id, UpdateDoctorResourceMappingRequest req) {
        DoctorResourceMapping m = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Doctor resource mapping not found: " + id));
        if (req.getBranchId() != null) m.setBranchId(req.getBranchId());
        if (req.getIsPrimary() != null) m.setPrimary(req.getIsPrimary());
        if (req.getEffectiveFrom() != null) m.setEffectiveFrom(req.getEffectiveFrom());
        if (req.getEffectiveTo() != null) m.setEffectiveTo(req.getEffectiveTo());
        if (req.getStatus() != null && !req.getStatus().isBlank()) m.setStatus(req.getStatus());
        return toResponse(repository.save(m));
    }

    private DoctorResourceMappingResponse toResponse(DoctorResourceMapping m) {
        DoctorResourceMappingResponse r = new DoctorResourceMappingResponse();
        r.setId(m.getId());
        r.setDoctorUserId(m.getDoctorUserId());
        r.setResourceId(m.getResourceId());
        r.setBranchId(m.getBranchId());
        r.setPrimary(m.isPrimary());
        r.setEffectiveFrom(m.getEffectiveFrom());
        r.setEffectiveTo(m.getEffectiveTo());
        r.setStatus(m.getStatus());
        r.setCreatedAt(m.getCreatedAt());
        r.setUpdatedAt(m.getUpdatedAt());
        return r;
    }
}
