package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.NonConformance;
import com.easyops.manufacturing.repository.NonConformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NonConformanceService {

    private final NonConformanceRepository nonConformanceRepository;

    @Transactional(readOnly = true)
    public List<NonConformance> getAllNonConformances(UUID organizationId) {
        return nonConformanceRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<NonConformance> getNonConformanceById(UUID ncId) {
        return nonConformanceRepository.findById(ncId);
    }

    @Transactional(readOnly = true)
    public List<NonConformance> getNonConformancesByStatus(UUID organizationId, String status) {
        return nonConformanceRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Transactional(readOnly = true)
    public List<NonConformance> getOpenNonConformances(UUID organizationId) {
        return nonConformanceRepository.findOpenNonConformances(organizationId);
    }

    @Transactional(readOnly = true)
    public List<NonConformance> getCriticalOpenNCs(UUID organizationId) {
        return nonConformanceRepository.findCriticalOpenNCs(organizationId);
    }

    @Transactional(readOnly = true)
    public List<NonConformance> getNonConformancesByWorkOrder(UUID workOrderId) {
        return nonConformanceRepository.findByWorkOrderId(workOrderId);
    }

    @Transactional
    public NonConformance createNonConformance(NonConformance nc) {
        if (nc.getNcNumber() == null || nc.getNcNumber().isEmpty()) {
            nc.setNcNumber(generateNCNumber(nc.getOrganizationId()));
        }
        
        if (nc.getStatus() == null) {
            nc.setStatus("OPEN");
        }
        
        if (nc.getSeverity() == null) {
            nc.setSeverity("MINOR");
        }
        
        if (nc.getReportedDate() == null) {
            nc.setReportedDate(LocalDateTime.now());
        }
        
        NonConformance saved = nonConformanceRepository.save(nc);
        log.info("Created non-conformance: {} with severity: {}", saved.getNcNumber(), saved.getSeverity());
        return saved;
    }

    @Transactional
    public NonConformance updateNonConformance(UUID ncId, NonConformance nc) {
        NonConformance existing = nonConformanceRepository.findById(ncId)
                .orElseThrow(() -> new RuntimeException("Non-conformance not found: " + ncId));
        
        existing.setNcType(nc.getNcType());
        existing.setSeverity(nc.getSeverity());
        existing.setCategory(nc.getCategory());
        existing.setStatus(nc.getStatus());
        existing.setDescription(nc.getDescription());
        existing.setLocation(nc.getLocation());
        existing.setQuantityAffected(nc.getQuantityAffected());
        existing.setQuantityReworked(nc.getQuantityReworked());
        existing.setQuantityScrapped(nc.getQuantityScrapped());
        existing.setRootCause(nc.getRootCause());
        existing.setRootCauseDescription(nc.getRootCauseDescription());
        existing.setImmediateAction(nc.getImmediateAction());
        existing.setCorrectiveAction(nc.getCorrectiveAction());
        existing.setPreventiveAction(nc.getPreventiveAction());
        existing.setDisposition(nc.getDisposition());
        existing.setResolutionNotes(nc.getResolutionNotes());
        existing.setCostImpact(nc.getCostImpact());
        existing.setUpdatedBy(nc.getUpdatedBy());
        
        NonConformance updated = nonConformanceRepository.save(existing);
        log.info("Updated non-conformance: {}", updated.getNcNumber());
        return updated;
    }

    @Transactional
    public void deleteNonConformance(UUID ncId) {
        nonConformanceRepository.deleteById(ncId);
        log.info("Deleted non-conformance: {}", ncId);
    }

    @Transactional
    public NonConformance assignNonConformance(UUID ncId, UUID assignedTo) {
        NonConformance nc = nonConformanceRepository.findById(ncId)
                .orElseThrow(() -> new RuntimeException("Non-conformance not found: " + ncId));
        
        nc.setAssignedTo(assignedTo);
        nc.setStatus("IN_REVIEW");
        
        NonConformance assigned = nonConformanceRepository.save(nc);
        log.info("Assigned non-conformance: {} to user: {}", assigned.getNcNumber(), assignedTo);
        return assigned;
    }

    @Transactional
    public NonConformance resolveNonConformance(UUID ncId, String disposition, String resolutionNotes, UUID resolvedBy) {
        NonConformance nc = nonConformanceRepository.findById(ncId)
                .orElseThrow(() -> new RuntimeException("Non-conformance not found: " + ncId));
        
        nc.setDisposition(disposition);
        nc.setResolutionNotes(resolutionNotes);
        nc.setResolvedBy(resolvedBy);
        nc.setResolvedDate(LocalDateTime.now());
        nc.setStatus("CLOSED");
        
        NonConformance resolved = nonConformanceRepository.save(nc);
        log.info("Resolved non-conformance: {} with disposition: {}", resolved.getNcNumber(), disposition);
        return resolved;
    }

    private String generateNCNumber(UUID organizationId) {
        long count = nonConformanceRepository.countByOrganization(organizationId);
        return String.format("NC-%06d", count + 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNonConformanceDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNCs", nonConformanceRepository.countByOrganization(organizationId));
        stats.put("openNCs", nonConformanceRepository.countOpenByOrganization(organizationId));
        stats.put("criticalNCs", nonConformanceRepository.countBySeverity(organizationId, "CRITICAL"));
        stats.put("majorNCs", nonConformanceRepository.countBySeverity(organizationId, "MAJOR"));
        stats.put("minorNCs", nonConformanceRepository.countBySeverity(organizationId, "MINOR"));
        
        return stats;
    }
}

