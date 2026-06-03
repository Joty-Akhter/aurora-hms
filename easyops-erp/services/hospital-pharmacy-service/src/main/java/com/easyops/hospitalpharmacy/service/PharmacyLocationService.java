package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.PharmacyLocationRequest;
import com.easyops.hospitalpharmacy.dto.response.PharmacyLocationResponse;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.repository.PharmacyLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PharmacyLocationService {

    private final PharmacyLocationRepository pharmacyLocationRepository;

    @Transactional
    public PharmacyLocationResponse create(PharmacyLocationRequest request) {
        PharmacyLocation location = PharmacyLocation.builder()
                .name(request.getName())
                .type(request.getType())
                .workflowType(request.getWorkflowType())
                .is24x7(request.getIs24x7() != null && request.getIs24x7())
                .operationalHours(request.getOperationalHours())
                .active(request.getActive() == null || request.getActive())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        PharmacyLocation saved = pharmacyLocationRepository.save(location);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PharmacyLocationResponse getById(UUID id) {
        PharmacyLocation location = pharmacyLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + id));
        return toResponse(location);
    }

    @Transactional(readOnly = true)
    public List<PharmacyLocationResponse> list(Boolean activeOnly) {
        List<PharmacyLocation> locations = Boolean.TRUE.equals(activeOnly)
                ? pharmacyLocationRepository.findByActiveTrue()
                : pharmacyLocationRepository.findAll();
        return locations.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PharmacyLocationResponse update(UUID id, PharmacyLocationRequest request) {
        PharmacyLocation location = pharmacyLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + id));

        if (request.getName() != null) {
            location.setName(request.getName());
        }
        if (request.getType() != null) {
            location.setType(request.getType());
        }
        if (request.getWorkflowType() != null) {
            location.setWorkflowType(request.getWorkflowType());
        }
        if (request.getIs24x7() != null) {
            location.set24x7(request.getIs24x7());
        }
        if (request.getOperationalHours() != null) {
            location.setOperationalHours(request.getOperationalHours());
        }
        if (request.getActive() != null) {
            location.setActive(request.getActive());
        }
        location.setUpdatedAt(OffsetDateTime.now());
        PharmacyLocation saved = pharmacyLocationRepository.save(location);
        return toResponse(saved);
    }

    private PharmacyLocationResponse toResponse(PharmacyLocation entity) {
        return PharmacyLocationResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .workflowType(entity.getWorkflowType())
                .is24x7(entity.is24x7())
                .operationalHours(entity.getOperationalHours())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

