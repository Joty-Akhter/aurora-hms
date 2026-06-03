package com.easyops.hr.service;

import com.easyops.hr.entity.ShiftDefinition;
import com.easyops.hr.repository.ShiftDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShiftDefinitionService {

    private final ShiftDefinitionRepository shiftDefinitionRepository;

    public List<ShiftDefinition> list(UUID organizationId, Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return shiftDefinitionRepository.findByOrganizationIdAndIsActiveOrderByCodeAsc(organizationId, true);
        }
        return shiftDefinitionRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }

    public ShiftDefinition get(UUID id) {
        return shiftDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "shift_definition_not_found"));
    }

    @Transactional
    public ShiftDefinition create(ShiftDefinition def) {
        validateCodes(def.getOrganizationId(), def.getCode(), null);
        def.setCode(def.getCode().trim());
        return shiftDefinitionRepository.save(def);
    }

    @Transactional
    public ShiftDefinition update(UUID id, ShiftDefinition patch) {
        ShiftDefinition existing = get(id);
        if (patch.getCode() == null || patch.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shift_code_required");
        }
        validateCodes(existing.getOrganizationId(), patch.getCode(), id);
        existing.setCode(patch.getCode().trim());
        existing.setName(patch.getName());
        if (patch.getShiftType() != null) {
            existing.setShiftType(patch.getShiftType());
        }
        if (patch.getGraceMinutes() != null) {
            existing.setGraceMinutes(patch.getGraceMinutes());
        }
        if (patch.getExpectedHours() != null) {
            existing.setExpectedHours(patch.getExpectedHours());
        }
        existing.setOvertimeRateMultiplier(patch.getOvertimeRateMultiplier());
        if (patch.getIsActive() != null) {
            existing.setIsActive(patch.getIsActive());
        }
        if (patch.getUpdatedBy() != null) {
            existing.setUpdatedBy(patch.getUpdatedBy());
        }
        return shiftDefinitionRepository.save(existing);
    }

    private void validateCodes(UUID organizationId, String code, UUID excludeId) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shift_code_required");
        }
        shiftDefinitionRepository.findByOrganizationIdAndCodeIgnoreCase(organizationId, code.trim()).ifPresent(found -> {
            if (excludeId == null || !found.getShiftDefinitionId().equals(excludeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "shift_code_already_exists");
            }
        });
    }
}
