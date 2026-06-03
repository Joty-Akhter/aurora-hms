package com.easyops.pharma.repository;

import com.easyops.pharma.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    
    List<ExpenseCategory> findByOrganizationId(UUID organizationId);
    
    List<ExpenseCategory> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
}

