package com.easyops.ar.service;

import com.easyops.ar.dto.CustomerRequest;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(UUID organizationId) {
        log.debug("Fetching all customers for organization: {}", organizationId);
        return customerRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Customer> getActiveCustomers(UUID organizationId) {
        log.debug("Fetching active customers for organization: {}", organizationId);
        return customerRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID id) {
        log.debug("Fetching customer by ID: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
    }
    
    @Transactional
    public Customer createCustomer(CustomerRequest request) {
        log.info("Creating new customer: {}", request.getCustomerCode());
        
        // Check if customer code already exists
        if (customerRepository.existsByOrganizationIdAndCustomerCode(
                request.getOrganizationId(), request.getCustomerCode())) {
            throw new RuntimeException("Customer code already exists: " + request.getCustomerCode());
        }
        
        Customer customer = new Customer();
        
        // If ID is provided (for syncing from other services), use it
        if (request.getId() != null) {
            customer.setId(request.getId());
            log.info("Using provided customer ID: {}", request.getId());
        }
        
        customer.setOrganizationId(request.getOrganizationId());
        customer.setCustomerCode(request.getCustomerCode());
        customer.setCustomerName(request.getCustomerName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setPaymentTerms(request.getPaymentTerms());
        customer.setIsActive(request.getIsActive());
        
        return customerRepository.save(customer);
    }
    
    @Transactional
    public Customer updateCustomer(UUID id, CustomerRequest request) {
        log.info("Updating customer: {}", id);
        
        Customer customer = getCustomerById(id);
        customer.setCustomerName(request.getCustomerName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setPaymentTerms(request.getPaymentTerms());
        customer.setIsActive(request.getIsActive());
        
        return customerRepository.save(customer);
    }
    
    @Transactional
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer: {}", id);
        customerRepository.deleteById(id);
    }
}

