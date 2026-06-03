package com.easyops.hospitalcorporatediscount.domain.contract;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.domain.corporate.CorporateClientRepository;
import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporateContractService {

    private final CorporateContractRepository repository;
    private final CorporateClientRepository corporateClientRepository;
    private final CorporateDiscountEventPublisher eventPublisher;

    @Transactional
    public ContractResponse create(UUID corporateId, CreateContractRequest request) {
        if (!corporateClientRepository.existsById(corporateId)) {
            throw new NoSuchElementException("Corporate not found: " + corporateId);
        }
        if (repository.existsByCorporateClientIdAndContractCode(corporateId, request.getContractCode().trim())) {
            throw new IllegalStateException("Contract with code already exists for this corporate: " + request.getContractCode());
        }
        CorporateContract entity = new CorporateContract();
        entity.setCorporateClientId(corporateId);
        entity.setContractCode(request.getContractCode().trim());
        entity.setContractName(trimOrNull(request.getContractName()));
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setCoverageType(request.getCoverageType().trim());
        entity.setServiceLocations(trimOrNull(request.getServiceLocations()));
        repository.save(entity);
        eventPublisher.publish(EventTypes.CONTRACT_CREATED, payload(entity));
        return toResponse(entity);
    }

    public ContractResponse getById(UUID id) {
        CorporateContract entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contract not found: " + id));
        return toResponse(entity);
    }

    public PagedResponse<ContractResponse> listByCorporate(UUID corporateId, String status, int page, int size) {
        if (!corporateClientRepository.existsById(corporateId)) {
            throw new NoSuchElementException("Corporate not found: " + corporateId);
        }
        Page<CorporateContract> p;
        if (status != null && !status.isBlank()) {
            Specification<CorporateContract> spec = Specification
                    .where(ContractSpecifications.hasCorporateClientId(corporateId))
                    .and(ContractSpecifications.hasEffectiveStatus(status));
            p = repository.findAll(spec, PageRequest.of(page, size));
        } else {
            p = repository.findByCorporateClientId(corporateId, PageRequest.of(page, size));
        }
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
    public ContractResponse update(UUID id, UpdateContractRequest request) {
        CorporateContract entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contract not found: " + id));
        Optional.ofNullable(request.getContractCode()).filter(s -> !s.isBlank()).ifPresent(s -> {
            if (!s.trim().equals(entity.getContractCode()) && repository.existsByCorporateClientIdAndContractCode(entity.getCorporateClientId(), s.trim())) {
                throw new IllegalStateException("Contract with code already exists for this corporate: " + s);
            }
            entity.setContractCode(s.trim());
        });
        Optional.ofNullable(request.getContractName()).ifPresent(s -> entity.setContractName(trimOrNull(s)));
        if (request.getValidFrom() != null) entity.setValidFrom(request.getValidFrom());
        if (request.getValidTo() != null) entity.setValidTo(request.getValidTo());
        Optional.ofNullable(request.getCoverageType()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setCoverageType(s.trim()));
        if (request.getServiceLocations() != null) entity.setServiceLocations(trimOrNull(request.getServiceLocations()));
        repository.save(entity);
        eventPublisher.publish(EventTypes.CONTRACT_UPDATED, payload(entity));
        return toResponse(entity);
    }

    private static Map<String, Object> payload(CorporateContract e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId() != null ? e.getId().toString() : null);
        m.put("corporateClientId", e.getCorporateClientId() != null ? e.getCorporateClientId().toString() : null);
        m.put("contractCode", e.getContractCode());
        m.put("validFrom", e.getValidFrom() != null ? e.getValidFrom().toString() : null);
        m.put("validTo", e.getValidTo() != null ? e.getValidTo().toString() : null);
        return m;
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ContractResponse toResponse(CorporateContract e) {
        ContractResponse r = new ContractResponse();
        r.setId(e.getId());
        r.setCorporateClientId(e.getCorporateClientId());
        r.setContractCode(e.getContractCode());
        r.setContractName(e.getContractName());
        r.setValidFrom(e.getValidFrom());
        r.setValidTo(e.getValidTo());
        r.setCoverageType(e.getCoverageType());
        r.setServiceLocations(e.getServiceLocations());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
