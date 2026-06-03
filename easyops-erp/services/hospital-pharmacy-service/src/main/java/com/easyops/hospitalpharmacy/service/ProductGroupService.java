package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.ProductGroupRequest;
import com.easyops.hospitalpharmacy.dto.response.ProductGroupResponse;
import com.easyops.hospitalpharmacy.entity.ProductGroup;
import com.easyops.hospitalpharmacy.repository.ProductGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductGroupService {

    private final ProductGroupRepository productGroupRepository;

    @Transactional
    public ProductGroupResponse create(ProductGroupRequest request) {
        ProductGroup productGroup = ProductGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() == null || request.getActive())
                .build();
        ProductGroup saved = productGroupRepository.save(productGroup);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductGroupResponse getById(UUID id) {
        ProductGroup productGroup = productGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductGroup not found: " + id));
        return toResponse(productGroup);
    }

    @Transactional(readOnly = true)
    public List<ProductGroupResponse> list(boolean activeOnly) {
        List<ProductGroup> groups;
        if (activeOnly) {
            groups = productGroupRepository.findByActiveTrue();
        } else {
            groups = productGroupRepository.findAll();
        }
        return groups.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ProductGroupResponse update(UUID id, ProductGroupRequest request) {
        ProductGroup productGroup = productGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductGroup not found: " + id));

        if (request.getName() != null) {
            productGroup.setName(request.getName());
        }
        if (request.getDescription() != null) {
            productGroup.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            productGroup.setActive(request.getActive());
        }

        ProductGroup saved = productGroupRepository.save(productGroup);
        return toResponse(saved);
    }

    private ProductGroupResponse toResponse(ProductGroup entity) {
        return ProductGroupResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
