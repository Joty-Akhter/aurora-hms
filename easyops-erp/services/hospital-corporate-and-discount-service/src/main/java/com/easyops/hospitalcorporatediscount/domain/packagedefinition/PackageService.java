package com.easyops.hospitalcorporatediscount.domain.packagedefinition;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PackageService {

    private static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ONE;

    private final PackageRepository packageRepository;
    private final PackageItemRepository packageItemRepository;

    @Transactional
    public PackageResponse create(CreatePackageRequest request) {
        if (packageRepository.existsByCode(request.getCode().trim())) {
            throw new IllegalStateException("Package with code already exists: " + request.getCode());
        }
        PackageDefinition entity = new PackageDefinition();
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName().trim());
        entity.setDescription(trimOrNull(request.getDescription()));
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setIsCorporateOnly(request.getIsCorporateOnly() != null ? request.getIsCorporateOnly() : false);
        entity.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        packageRepository.save(entity);
        return toResponse(entity);
    }

    public PackageDetailResponse getById(UUID id) {
        PackageDefinition entity = packageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Package not found: " + id));
        List<PackageItem> items = packageItemRepository.findByPackageIdOrderByItemTypeAscItemCodeAsc(id);
        return toDetailResponse(entity, items);
    }

    public PagedResponse<PackageResponse> list(String code, Boolean isPublic, int page, int size) {
        Specification<PackageDefinition> spec = PackageSpecifications.hasCode(code)
                .and(PackageSpecifications.isPublic(isPublic));
        Page<PackageDefinition> p = packageRepository.findAll(spec, PageRequest.of(page, size));
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
    public PackageResponse update(UUID id, UpdatePackageRequest request) {
        PackageDefinition entity = packageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Package not found: " + id));
        Optional.ofNullable(request.getCode()).filter(s -> !s.isBlank()).ifPresent(s -> {
            if (!s.trim().equalsIgnoreCase(entity.getCode()) && packageRepository.existsByCode(s.trim())) {
                throw new IllegalStateException("Package with code already exists: " + s);
            }
            entity.setCode(s.trim());
        });
        Optional.ofNullable(request.getName()).filter(s -> !s.isBlank()).ifPresent(s -> entity.setName(s.trim()));
        if (request.getDescription() != null) entity.setDescription(trimOrNull(request.getDescription()));
        if (request.getDefaultPrice() != null) entity.setDefaultPrice(request.getDefaultPrice());
        if (request.getIsCorporateOnly() != null) entity.setIsCorporateOnly(request.getIsCorporateOnly());
        if (request.getIsPublic() != null) entity.setIsPublic(request.getIsPublic());
        packageRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public PackageItemResponse addItem(UUID packageId, CreatePackageItemRequest request) {
        PackageDefinition pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found: " + packageId));
        PackageItem item = new PackageItem();
        item.setPackageId(pkg.getId());
        item.setItemType(request.getItemType().trim());
        item.setItemCode(request.getItemCode().trim());
        item.setQuantityIncluded(request.getQuantityIncluded() != null ? request.getQuantityIncluded() : DEFAULT_QUANTITY);
        packageItemRepository.save(item);
        return toItemResponse(item);
    }

    public List<PackageItemResponse> listItems(UUID packageId) {
        if (!packageRepository.existsById(packageId)) {
            throw new NoSuchElementException("Package not found: " + packageId);
        }
        return packageItemRepository.findByPackageIdOrderByItemTypeAscItemCodeAsc(packageId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional
    public void deleteItem(UUID packageId, UUID itemId) {
        if (!packageItemRepository.existsByPackageIdAndId(packageId, itemId)) {
            throw new NoSuchElementException("Package item not found: " + itemId + " in package: " + packageId);
        }
        packageItemRepository.deleteById(itemId);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private PackageResponse toResponse(PackageDefinition e) {
        PackageResponse r = new PackageResponse();
        r.setId(e.getId());
        r.setCode(e.getCode());
        r.setName(e.getName());
        r.setDescription(e.getDescription());
        r.setDefaultPrice(e.getDefaultPrice());
        r.setIsCorporateOnly(e.getIsCorporateOnly());
        r.setIsPublic(e.getIsPublic());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }

    private PackageDetailResponse toDetailResponse(PackageDefinition e, List<PackageItem> items) {
        PackageDetailResponse r = new PackageDetailResponse();
        r.setId(e.getId());
        r.setCode(e.getCode());
        r.setName(e.getName());
        r.setDescription(e.getDescription());
        r.setDefaultPrice(e.getDefaultPrice());
        r.setIsCorporateOnly(e.getIsCorporateOnly());
        r.setIsPublic(e.getIsPublic());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setItems(items.stream().map(this::toItemResponse).toList());
        return r;
    }

    private PackageItemResponse toItemResponse(PackageItem e) {
        PackageItemResponse r = new PackageItemResponse();
        r.setId(e.getId());
        r.setPackageId(e.getPackageId());
        r.setItemType(e.getItemType());
        r.setItemCode(e.getItemCode());
        r.setQuantityIncluded(e.getQuantityIncluded());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
