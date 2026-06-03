package com.easyops.ar.repository;

import com.easyops.ar.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    List<Customer> findByOrganizationId(UUID organizationId);
    
    List<Customer> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<Customer> findByOrganizationIdAndCustomerCode(UUID organizationId, String customerCode);
    
    boolean existsByOrganizationIdAndCustomerCode(UUID organizationId, String customerCode);
}

