package com.easyops.hospitalscheduling.domain.roster;

import com.easyops.hospitalscheduling.api.dto.CreateRosterBlockRequest;
import com.easyops.hospitalscheduling.api.dto.RosterBlockResponse;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RosterBlockService {

    private final RosterBlockRepository rosterBlockRepository;
    private final SchedulingResourceRepository resourceRepository;

    @Transactional
    public RosterBlockResponse create(UUID resourceId, CreateRosterBlockRequest request) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new NoSuchElementException("Resource not found: " + resourceId);
        }
        if (request.getEndTime() == null || request.getStartTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if ("SUBSTITUTE".equals(request.getType()) && request.getSubstituteResourceId() == null) {
            throw new IllegalArgumentException("substituteResourceId is required when type is SUBSTITUTE");
        }
        if (request.getSubstituteResourceId() != null && !resourceRepository.existsById(request.getSubstituteResourceId())) {
            throw new NoSuchElementException("Substitute resource not found: " + request.getSubstituteResourceId());
        }

        RosterBlock entity = new RosterBlock();
        entity.setResourceId(resourceId);
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setType(request.getType().trim());
        entity.setSubstituteResourceId(request.getSubstituteResourceId());
        rosterBlockRepository.save(entity);
        return toResponse(entity);
    }

    public List<RosterBlockResponse> listByResourceId(UUID resourceId, OffsetDateTime from, OffsetDateTime to) {
        List<RosterBlock> blocks;
        if (from != null && to != null) {
            blocks = rosterBlockRepository.findByResourceIdAndTimeOverlap(resourceId, from, to);
        } else {
            blocks = rosterBlockRepository.findByResourceIdOrderByStartTimeAsc(resourceId);
        }
        return blocks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteById(UUID id) {
        if (!rosterBlockRepository.existsById(id)) {
            throw new NoSuchElementException("Roster block not found: " + id);
        }
        rosterBlockRepository.deleteById(id);
    }

    private RosterBlockResponse toResponse(RosterBlock e) {
        RosterBlockResponse r = new RosterBlockResponse();
        r.setId(e.getId());
        r.setResourceId(e.getResourceId());
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        r.setType(e.getType());
        r.setSubstituteResourceId(e.getSubstituteResourceId());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
