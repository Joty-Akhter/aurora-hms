package com.easyops.hospitalcorporatediscount.domain.discount;

import com.easyops.hospitalcorporatediscount.api.dto.CreateDiscountDecisionRequest;
import com.easyops.hospitalcorporatediscount.api.dto.DiscountDecisionResponse;
import com.easyops.hospitalcorporatediscount.api.dto.PagedResponse;
import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountDecisionService {

    private final DiscountDecisionRepository repository;
    private final CorporateDiscountEventPublisher eventPublisher;

    public PagedResponse<DiscountDecisionResponse> list(int page, int size) {
        Page<DiscountDecision> p = repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<DiscountDecisionResponse> content = p.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(
                content,
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumber(),
                p.getSize(),
                p.isFirst(),
                p.isLast());
    }

    @Transactional
    public DiscountDecisionResponse create(CreateDiscountDecisionRequest request) {
        DiscountDecision entity = new DiscountDecision();
        entity.setBillContextId(trimOrNull(request.getBillContextId()));
        entity.setPatientId(request.getPatientId());
        entity.setCorporateClientId(request.getCorporateClientId());
        entity.setDiscountSchemeId(request.getDiscountSchemeId());
        entity.setDiscountAmount(request.getDiscountAmount());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setDecidedByUserId(request.getDecidedByUserId());
        entity.setApprovedByUserId(request.getApprovedByUserId());
        repository.save(entity);
        eventPublisher.publish(EventTypes.DISCOUNT_DECISION_CREATED, payload(entity));
        return toResponse(entity);
    }

    private static Map<String, Object> payload(DiscountDecision e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId() != null ? e.getId().toString() : null);
        m.put("billContextId", e.getBillContextId());
        m.put("patientId", e.getPatientId() != null ? e.getPatientId().toString() : null);
        m.put("discountSchemeId", e.getDiscountSchemeId() != null ? e.getDiscountSchemeId().toString() : null);
        m.put("discountAmount", e.getDiscountAmount() != null ? e.getDiscountAmount().doubleValue() : null);
        return m;
    }

    public DiscountDecisionResponse getById(UUID id) {
        DiscountDecision entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Discount decision not found: " + id));
        return toResponse(entity);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private DiscountDecisionResponse toResponse(DiscountDecision e) {
        DiscountDecisionResponse r = new DiscountDecisionResponse();
        r.setId(e.getId());
        r.setBillContextId(e.getBillContextId());
        r.setPatientId(e.getPatientId());
        r.setCorporateClientId(e.getCorporateClientId());
        r.setDiscountSchemeId(e.getDiscountSchemeId());
        r.setDiscountAmount(e.getDiscountAmount());
        r.setDiscountPercent(e.getDiscountPercent());
        r.setDecidedByUserId(e.getDecidedByUserId());
        r.setApprovedByUserId(e.getApprovedByUserId());
        r.setCreatedAt(e.getCreatedAt());
        r.setApprovedAt(e.getApprovedAt());
        return r;
    }
}
