package com.easyops.hospitalscheduling.domain.resource;

import com.easyops.hospitalscheduling.api.dto.BlackoutResponse;
import com.easyops.hospitalscheduling.api.dto.CreateBlackoutRequest;
import com.easyops.hospitalscheduling.api.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlackoutService {

    private final BlackoutRepository blackoutRepository;
    private final SchedulingResourceRepository resourceRepository;

    @Transactional
    public BlackoutResponse create(CreateBlackoutRequest request) {
        if (request.getResourceId() == null && request.getBranchId() == null) {
            throw new IllegalArgumentException("At least one of resourceId or branchId must be set");
        }
        if (request.getResourceId() != null && !resourceRepository.existsById(request.getResourceId())) {
            throw new NoSuchElementException("Resource not found: " + request.getResourceId());
        }
        Blackout entity = new Blackout();
        entity.setResourceId(request.getResourceId());
        entity.setBranchId(request.getBranchId());
        entity.setBlackoutDate(request.getBlackoutDate());
        entity.setReason(trimOrNull(request.getReason()));
        blackoutRepository.save(entity);
        return toResponse(entity);
    }

    public PagedResponse<BlackoutResponse> list(UUID resourceId, UUID branchId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Specification<Blackout> spec = BlackoutSpecifications.hasResourceId(resourceId)
                .and(BlackoutSpecifications.hasBranchId(branchId))
                .and(BlackoutSpecifications.blackoutDateFrom(fromDate))
                .and(BlackoutSpecifications.blackoutDateTo(toDate));
        Page<Blackout> p = blackoutRepository.findAll(spec, PageRequest.of(page, size));
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
    public void deleteById(UUID id) {
        if (!blackoutRepository.existsById(id)) {
            throw new NoSuchElementException("Blackout not found: " + id);
        }
        blackoutRepository.deleteById(id);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private BlackoutResponse toResponse(Blackout e) {
        BlackoutResponse r = new BlackoutResponse();
        r.setId(e.getId());
        r.setResourceId(e.getResourceId());
        r.setBranchId(e.getBranchId());
        r.setBlackoutDate(e.getBlackoutDate());
        r.setReason(e.getReason());
        r.setCreatedAt(e.getCreatedAt());
        r.setCreatedBy(e.getCreatedBy());
        return r;
    }
}
