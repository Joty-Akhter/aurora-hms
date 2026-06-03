package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.ManufacturerRequest;
import com.easyops.hospitalpharmacy.dto.response.ManufacturerResponse;
import com.easyops.hospitalpharmacy.entity.Manufacturer;
import com.easyops.hospitalpharmacy.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    @Transactional
    public ManufacturerResponse create(ManufacturerRequest request) {
        Manufacturer manufacturer = Manufacturer.builder()
                .name(request.getName())
                .shortCode(request.getShortCode())
                .country(request.getCountry())
                .contactInfo(request.getContactInfo())
                .active(request.getActive() == null || request.getActive())
                .licenseNo(request.getLicenseNo())
                .vat(request.getVat())
                .commission(request.getCommission())
                .type(request.getType())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse getById(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        return toResponse(manufacturer);
    }

    @Transactional(readOnly = true)
    public List<ManufacturerResponse> search(String name, Boolean activeOnly) {
        List<Manufacturer> manufacturers;
        if (name != null && !name.isBlank()) {
            manufacturers = manufacturerRepository.findByNameContainingIgnoreCase(name);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            manufacturers = manufacturerRepository.findByActiveTrue();
        } else {
            manufacturers = manufacturerRepository.findAll();
        }
        return manufacturers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ManufacturerResponse update(UUID id, ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));

        if (request.getName() != null) {
            manufacturer.setName(request.getName());
        }
        if (request.getShortCode() != null) {
            manufacturer.setShortCode(request.getShortCode());
        }
        if (request.getCountry() != null) {
            manufacturer.setCountry(request.getCountry());
        }
        if (request.getContactInfo() != null) {
            manufacturer.setContactInfo(request.getContactInfo());
        }
        if (request.getActive() != null) {
            manufacturer.setActive(request.getActive());
        }
        if (request.getLicenseNo() != null) {
            manufacturer.setLicenseNo(request.getLicenseNo());
        }
        if (request.getVat() != null) {
            manufacturer.setVat(request.getVat());
        }
        if (request.getCommission() != null) {
            manufacturer.setCommission(request.getCommission());
        }
        if (request.getType() != null) {
            manufacturer.setType(request.getType());
        }
        if (request.getEmail() != null) {
            manufacturer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            manufacturer.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            manufacturer.setAddress(request.getAddress());
        }

        Manufacturer saved = manufacturerRepository.save(manufacturer);
        return toResponse(saved);
    }

    private ManufacturerResponse toResponse(Manufacturer entity) {
        return ManufacturerResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .shortCode(entity.getShortCode())
                .country(entity.getCountry())
                .contactInfo(entity.getContactInfo())
                .active(entity.isActive())
                .licenseNo(entity.getLicenseNo())
                .vat(entity.getVat())
                .commission(entity.getCommission())
                .type(entity.getType())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

