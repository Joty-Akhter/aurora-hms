package com.easyops.hr.service;

import com.easyops.hr.entity.Position;
import com.easyops.hr.entity.SalaryBand;
import com.easyops.hr.entity.SalaryGrade;
import com.easyops.hr.entity.SalaryStructure;
import com.easyops.hr.repository.PositionRepository;
import com.easyops.hr.repository.SalaryBandRepository;
import com.easyops.hr.repository.SalaryGradeRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PositionService {

    private final PositionRepository positionRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final SalaryGradeRepository salaryGradeRepository;
    private final SalaryBandRepository salaryBandRepository;
    
    public List<Position> getAllPositions(UUID organizationId) {
        log.debug("Fetching all positions for organization: {}", organizationId);
        return positionRepository.findByOrganizationId(organizationId);
    }
    
    public List<Position> getActivePositions(UUID organizationId) {
        log.debug("Fetching active positions for organization: {}", organizationId);
        return positionRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    public List<Position> getPositionsByDepartment(UUID organizationId, UUID departmentId) {
        log.debug("Fetching positions for department: {}", departmentId);
        return positionRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId);
    }
    
    public Position getPositionById(UUID positionId) {
        log.debug("Fetching position by ID: {}", positionId);
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found with ID: " + positionId));
    }
    
    public Position createPosition(Position position) {
        log.info("Creating new position: {} for organization: {}",
                position.getTitle(), position.getOrganizationId());

        // Check for duplicate title
        positionRepository.findByOrganizationIdAndTitle(
                position.getOrganizationId(), position.getTitle())
                .ifPresent(p -> {
                    throw new RuntimeException("Position title already exists: " + position.getTitle());
                });

        // SS-27: Validate default structure/grade/band if provided
        if (position.getDefaultSalaryStructureId() != null) {
            validatePositionDefaultStructure(position.getOrganizationId(), position.getDefaultSalaryStructureId());
            if (position.getDefaultSalaryGradeId() != null) {
                validatePositionDefaultGrade(position.getDefaultSalaryStructureId(), position.getDefaultSalaryGradeId());
                if (position.getDefaultSalaryBandId() != null) {
                    validatePositionDefaultBand(position.getDefaultSalaryGradeId(), position.getDefaultSalaryBandId());
                }
            } else {
                position.setDefaultSalaryBandId(null);
            }
        } else {
            position.setDefaultSalaryGradeId(null);
            position.setDefaultSalaryBandId(null);
        }

        return positionRepository.save(position);
    }
    
    public Position updatePosition(UUID positionId, Position positionData) {
        log.info("Updating position: {}", positionId);

        Position existingPosition = getPositionById(positionId);

        // Update fields
        existingPosition.setTitle(positionData.getTitle());
        existingPosition.setDescription(positionData.getDescription());
        existingPosition.setDepartmentId(positionData.getDepartmentId());
        existingPosition.setLevel(positionData.getLevel());
        existingPosition.setHierarchyRank(positionData.getHierarchyRank());
        existingPosition.setSalaryRangeMin(positionData.getSalaryRangeMin());
        existingPosition.setSalaryRangeMax(positionData.getSalaryRangeMax());
        existingPosition.setCurrency(positionData.getCurrency());
        existingPosition.setIsActive(positionData.getIsActive());
        existingPosition.setUpdatedBy(positionData.getUpdatedBy());

        // SS-27: Validate and set default structure/grade/band (same org; grade in structure; band in grade)
        if (positionData.getDefaultSalaryStructureId() != null) {
            validatePositionDefaultStructure(existingPosition.getOrganizationId(), positionData.getDefaultSalaryStructureId());
            existingPosition.setDefaultSalaryStructureId(positionData.getDefaultSalaryStructureId());
        } else {
            existingPosition.setDefaultSalaryStructureId(null);
            existingPosition.setDefaultSalaryGradeId(null);
            existingPosition.setDefaultSalaryBandId(null);
        }
        if (existingPosition.getDefaultSalaryStructureId() != null && positionData.getDefaultSalaryGradeId() != null) {
            validatePositionDefaultGrade(existingPosition.getDefaultSalaryStructureId(), positionData.getDefaultSalaryGradeId());
            existingPosition.setDefaultSalaryGradeId(positionData.getDefaultSalaryGradeId());
        } else {
            existingPosition.setDefaultSalaryGradeId(null);
            existingPosition.setDefaultSalaryBandId(null);
        }
        if (existingPosition.getDefaultSalaryGradeId() != null && positionData.getDefaultSalaryBandId() != null) {
            validatePositionDefaultBand(existingPosition.getDefaultSalaryGradeId(), positionData.getDefaultSalaryBandId());
            existingPosition.setDefaultSalaryBandId(positionData.getDefaultSalaryBandId());
        } else {
            existingPosition.setDefaultSalaryBandId(null);
        }

        return positionRepository.save(existingPosition);
    }

    /** SS-27: Structure must belong to the position's organization. */
    private void validatePositionDefaultStructure(UUID positionOrgId, UUID structureId) {
        SalaryStructure structure = salaryStructureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Salary structure not found"));
        if (!structure.getOrganizationId().equals(positionOrgId)) {
            throw new IllegalArgumentException("Salary structure must belong to the same organization as the position. SS-27.");
        }
    }

    /** SS-27: Grade must belong to the given structure. */
    private void validatePositionDefaultGrade(UUID structureId, UUID gradeId) {
        SalaryGrade grade = salaryGradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Salary grade not found"));
        if (!grade.getSalaryStructureId().equals(structureId)) {
            throw new IllegalArgumentException("Salary grade must belong to the selected salary structure. SS-27.");
        }
    }

    /** SS-27: Band must belong to the given grade. */
    private void validatePositionDefaultBand(UUID gradeId, UUID bandId) {
        SalaryBand band = salaryBandRepository.findById(bandId)
                .orElseThrow(() -> new IllegalArgumentException("Salary band not found"));
        if (!band.getSalaryGradeId().equals(gradeId)) {
            throw new IllegalArgumentException("Salary band must belong to the selected salary grade. SS-27.");
        }
    }
    
    public void deletePosition(UUID positionId) {
        log.info("Deactivating position: {}", positionId);
        Position position = getPositionById(positionId);
        position.setIsActive(false);
        positionRepository.save(position);
    }
}

