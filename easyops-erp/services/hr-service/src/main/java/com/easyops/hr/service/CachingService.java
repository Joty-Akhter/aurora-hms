package com.easyops.hr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caching service for performance optimization
 * Provides caching for frequently accessed data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachingService {
    
    /**
     * Cache key generators for different entity types
     */
    public static String getEpfAccountCacheKey(UUID accountId) {
        return "epf_account:" + accountId;
    }
    
    public static String getEmployeeCacheKey(UUID employeeId) {
        return "employee:" + employeeId;
    }
    
    public static String getOrganizationCacheKey(UUID organizationId) {
        return "organization:" + organizationId;
    }
    
    /**
     * Cache eviction methods
     */
    @CacheEvict(value = "epfAccounts", key = "#accountId")
    public void evictEpfAccountCache(UUID accountId) {
        log.debug("Evicting EPF account cache for: {}", accountId);
    }
    
    @CacheEvict(value = "employees", key = "#employeeId")
    public void evictEmployeeCache(UUID employeeId) {
        log.debug("Evicting employee cache for: {}", employeeId);
    }
    
    /**
     * Clear all caches
     */
    @CacheEvict(value = {"epfAccounts", "employees"}, allEntries = true)
    public void clearAllCaches() {
        log.info("Clearing all caches");
    }
}

