package com.easyops.inventory.service;

import com.easyops.inventory.entity.SerialNumber;
import com.easyops.inventory.repository.SerialNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SerialNumberService {
    
    private final SerialNumberRepository serialNumberRepository;
    
    @Transactional(readOnly = true)
    public List<SerialNumber> getAllSerials(UUID organizationId) {
        log.debug("Fetching all serial numbers for organization: {}", organizationId);
        return serialNumberRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<SerialNumber> getSerialsByProduct(UUID organizationId, UUID productId) {
        log.debug("Fetching serial numbers for product: {}", productId);
        return serialNumberRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }
    
    @Transactional(readOnly = true)
    public List<SerialNumber> getSerialsByStatus(UUID organizationId, String status) {
        log.debug("Fetching serial numbers with status: {}", status);
        return serialNumberRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<SerialNumber> getAvailableSerials(UUID organizationId, UUID warehouseId) {
        log.debug("Fetching available serial numbers in warehouse: {}", warehouseId);
        return serialNumberRepository.findAvailableSerials(organizationId, warehouseId);
    }
    
    @Transactional(readOnly = true)
    public List<SerialNumber> getSerialsByCustomer(UUID organizationId, UUID customerId) {
        log.debug("Fetching serial numbers sold to customer: {}", customerId);
        return serialNumberRepository.findByCustomer(organizationId, customerId);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "serial", key = "#id")
    public SerialNumber getSerialById(UUID id) {
        log.debug("Fetching serial number by ID: {}", id);
        return serialNumberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serial number not found with ID: " + id));
    }
    
    @Transactional(readOnly = true)
    public SerialNumber getSerialByNumber(UUID organizationId, UUID productId, String serialNumber) {
        log.debug("Fetching serial number: {} for product: {}", serialNumber, productId);
        return serialNumberRepository.findByOrganizationIdAndProductIdAndSerialNumber(
                organizationId, productId, serialNumber)
                .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumber));
    }
    
    @Transactional
    @CacheEvict(value = {"serial", "serials"}, allEntries = true)
    public SerialNumber registerSerial(SerialNumber serial) {
        log.info("Registering serial number: {} for product: {}", serial.getSerialNumber(), serial.getProductId());
        
        if (serialNumberRepository.existsByOrganizationIdAndProductIdAndSerialNumber(
                serial.getOrganizationId(), serial.getProductId(), serial.getSerialNumber())) {
            throw new RuntimeException("Serial number already exists: " + serial.getSerialNumber());
        }
        
        serial.setStatus("IN_STOCK");
        
        // Calculate warranty end date if not provided
        if (serial.getWarrantyStartDate() != null && serial.getWarrantyPeriodMonths() != null && serial.getWarrantyEndDate() == null) {
            serial.setWarrantyEndDate(serial.getWarrantyStartDate().plusMonths(serial.getWarrantyPeriodMonths()));
        }
        
        return serialNumberRepository.save(serial);
    }
    
    @Transactional
    @CacheEvict(value = {"serial", "serials"}, allEntries = true)
    public SerialNumber allocateSerial(UUID id, UUID salesOrderId) {
        log.info("Allocating serial number: {} for sales order: {}", id, salesOrderId);
        
        SerialNumber serial = getSerialById(id);
        
        if (!"IN_STOCK".equals(serial.getStatus())) {
            throw new RuntimeException("Serial number not available for allocation. Status: " + serial.getStatus());
        }
        
        serial.setStatus("ALLOCATED");
        
        return serialNumberRepository.save(serial);
    }
    
    @Transactional
    @CacheEvict(value = {"serial", "serials"}, allEntries = true)
    public SerialNumber sellSerial(UUID id, UUID customerId, String invoiceNumber) {
        log.info("Selling serial number: {} to customer: {}", id, customerId);
        
        SerialNumber serial = getSerialById(id);
        
        if (!"ALLOCATED".equals(serial.getStatus()) && !"IN_STOCK".equals(serial.getStatus())) {
            throw new RuntimeException("Serial number cannot be sold. Status: " + serial.getStatus());
        }
        
        serial.setStatus("SOLD");
        serial.setCustomerId(customerId);
        serial.setSaleDate(LocalDate.now());
        serial.setSaleInvoiceNumber(invoiceNumber);
        
        return serialNumberRepository.save(serial);
    }
    
    @Transactional
    @CacheEvict(value = {"serial", "serials"}, allEntries = true)
    public SerialNumber returnSerial(UUID id) {
        log.info("Processing return for serial number: {}", id);
        
        SerialNumber serial = getSerialById(id);
        
        serial.setStatus("RETURNED");
        serial.setWarehouseId(null); // Will need to be reassigned
        
        return serialNumberRepository.save(serial);
    }
}

