package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.UnitRequest;
import com.easyops.hospitalpharmacy.dto.response.UnitResponse;
import com.easyops.hospitalpharmacy.entity.Unit;
import com.easyops.hospitalpharmacy.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;

    @Transactional
    public UnitResponse create(UnitRequest request) {
        Unit baseUnit = resolveBaseUnit(null, request.getBaseUnitId());
        Unit unit = Unit.builder()
                .name(request.getName())
                .abbreviation(request.getAbbreviation())
                .baseUnit(baseUnit)
                .conversionFactor(request.getConversionFactor())
                .build();
        Unit saved = unitRepository.save(unit);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UnitResponse getById(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id));
        return toResponse(unit);
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> listAll() {
        return unitRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> listBaseUnits() {
        return unitRepository.findByBaseUnitIsNull().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public UnitResponse update(UUID id, UnitRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id));

        if (request.getName() != null) {
            unit.setName(request.getName());
        }
        if (request.getAbbreviation() != null) {
            unit.setAbbreviation(request.getAbbreviation());
        }
        if (request.getBaseUnitId() != null) {
            Unit baseUnit = resolveBaseUnit(id, request.getBaseUnitId());
            unit.setBaseUnit(baseUnit);
        }
        if (request.getConversionFactor() != null) {
            unit.setConversionFactor(request.getConversionFactor());
        }

        Unit saved = unitRepository.save(unit);
        return toResponse(saved);
    }

    private Unit resolveBaseUnit(UUID selfId, UUID baseUnitId) {
        if (baseUnitId == null) {
            return null;
        }
        if (selfId != null && selfId.equals(baseUnitId)) {
            throw new IllegalArgumentException("Unit cannot reference itself");
        }
        return unitRepository.findById(baseUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Base unit not found: " + baseUnitId));
    }

    private UnitResponse toResponse(Unit entity) {
        return UnitResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .abbreviation(entity.getAbbreviation())
                .baseUnitId(entity.getBaseUnit() != null ? entity.getBaseUnit().getId() : null)
                .baseUnitName(entity.getBaseUnit() != null ? entity.getBaseUnit().getName() : null)
                .conversionFactor(entity.getConversionFactor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
