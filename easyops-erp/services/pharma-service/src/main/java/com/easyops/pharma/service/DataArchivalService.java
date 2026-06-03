package com.easyops.pharma.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for data archival and optimization
 * Moves old data to archive tables for performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataArchivalService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // Data retention period: 2 years
    private static final int RETENTION_YEARS = 2;
    
    /**
     * Archive deposits older than retention period
     */
    @Async
    @Transactional
    public void archiveOldDeposits(UUID organizationId) {
        LocalDate cutoffDate = LocalDate.now().minusYears(RETENTION_YEARS);
        log.info("Archiving deposits older than {} for organization: {}", cutoffDate, organizationId);
        
        String archiveSql = """
            INSERT INTO pharma.deposits_archive
            SELECT * FROM pharma.deposits
            WHERE organization_id = ? 
            AND deposit_date < ?
            AND id NOT IN (SELECT id FROM pharma.deposits_archive)
            """;
        
        String deleteSql = """
            DELETE FROM pharma.deposits
            WHERE organization_id = ?
            AND deposit_date < ?
            AND id IN (SELECT id FROM pharma.deposits_archive)
            """;
        
        try {
            int archived = jdbcTemplate.update(archiveSql, organizationId, cutoffDate);
            int deleted = jdbcTemplate.update(deleteSql, organizationId, cutoffDate);
            log.info("Archived {} deposits, deleted {} old records", archived, deleted);
        } catch (Exception e) {
            log.error("Failed to archive deposits for organization: {}", organizationId, e);
        }
    }
    
    /**
     * Archive incentive calculations older than retention period
     */
    @Async
    @Transactional
    public void archiveOldIncentiveCalculations(UUID organizationId) {
        LocalDate cutoffDate = LocalDate.now().minusYears(RETENTION_YEARS);
        log.info("Archiving incentive calculations older than {} for organization: {}", cutoffDate, organizationId);
        
        String archiveSql = """
            INSERT INTO pharma.incentive_calculations_archive
            SELECT * FROM pharma.incentive_calculations
            WHERE organization_id = ?
            AND calculation_date < ?
            AND id NOT IN (SELECT id FROM pharma.incentive_calculations_archive)
            """;
        
        String deleteSql = """
            DELETE FROM pharma.incentive_calculations
            WHERE organization_id = ?
            AND calculation_date < ?
            AND id IN (SELECT id FROM pharma.incentive_calculations_archive)
            """;
        
        try {
            int archived = jdbcTemplate.update(archiveSql, organizationId, cutoffDate.toEpochDay());
            int deleted = jdbcTemplate.update(deleteSql, organizationId, cutoffDate.toEpochDay());
            log.info("Archived {} incentive calculations, deleted {} old records", archived, deleted);
        } catch (Exception e) {
            log.error("Failed to archive incentive calculations for organization: {}", organizationId, e);
        }
    }
    
    /**
     * Archive all old data for organization
     */
    @Async
    public void archiveAllOldData(UUID organizationId) {
        log.info("Starting data archival for organization: {}", organizationId);
        archiveOldDeposits(organizationId);
        archiveOldIncentiveCalculations(organizationId);
        log.info("Completed data archival for organization: {}", organizationId);
    }
}
