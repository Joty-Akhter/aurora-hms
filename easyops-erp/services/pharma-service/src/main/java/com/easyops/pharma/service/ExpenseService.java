package com.easyops.pharma.service;

import com.easyops.pharma.entity.Expense;
import com.easyops.pharma.entity.ExpenseCategory;
import com.easyops.pharma.repository.ExpenseRepository;
import com.easyops.pharma.repository.ExpenseCategoryRepository;
import com.easyops.pharma.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final TerritoryRepository territoryRepository;
    
    @Transactional(readOnly = true)
    public List<ExpenseCategory> getAllCategories(UUID organizationId) {
        log.debug("Fetching all expense categories for organization: {}", organizationId);
        return categoryRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public ExpenseCategory getCategoryById(UUID id) {
        log.debug("Fetching expense category by ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense category not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategory createCategory(ExpenseCategory category) {
        log.info("Creating new expense category: {}", category.getName());
        return categoryRepository.save(category);
    }
    
    @Transactional
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategory updateCategory(UUID id, ExpenseCategory category) {
        log.info("Updating expense category: {}", id);
        ExpenseCategory existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setIsActive(category.getIsActive());
        existing.setUpdatedBy(category.getUpdatedBy());
        return categoryRepository.save(existing);
    }
    
    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses(UUID organizationId) {
        log.debug("Fetching all expenses for organization: {}", organizationId);
        return expenseRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByTerritory(UUID territoryId) {
        log.debug("Fetching expenses for territory: {}", territoryId);
        return expenseRepository.findByTerritoryId(territoryId);
    }
    
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByTerritoryAndPeriod(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching expenses for territory: {}, year: {}, month: {}", territoryId, year, month);
        return expenseRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpensesForTerritoryAndMonth(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching total expenses for territory: {}, year: {}, month: {}", territoryId, year, month);
        BigDecimal total = expenseRepository.getTotalExpensesForTerritoryAndMonth(territoryId, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Expense getExpenseById(UUID id) {
        log.debug("Fetching expense by ID: {}", id);
        return expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"expenses", "incentiveCalculations"}, allEntries = true)
    public Expense createExpense(Expense expense) {
        log.info("Creating new expense for territory: {}", expense.getTerritoryId());
        
        // Validate territory exists
        territoryRepository.findById(expense.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + expense.getTerritoryId()));
        
        // Validate category exists
        getCategoryById(expense.getExpenseCategoryId());
        
        // Extract year and month from date
        LocalDate date = expense.getExpenseDate();
        expense.setYear(date.getYear());
        expense.setMonth(date.getMonthValue());
        
        // Set default status
        if (expense.getStatus() == null) {
            expense.setStatus("DRAFT");
        }
        
        return expenseRepository.save(expense);
    }
    
    @Transactional
    @CacheEvict(value = {"expenses", "incentiveCalculations"}, allEntries = true)
    public Expense updateExpense(UUID id, Expense expense) {
        log.info("Updating expense: {}", id);
        Expense existing = getExpenseById(id);
        
        existing.setExpenseCategoryId(expense.getExpenseCategoryId());
        existing.setSourceEmployeeId(expense.getSourceEmployeeId());
        existing.setExpenseAmount(expense.getExpenseAmount());
        existing.setDescription(expense.getDescription());
        existing.setExpenseDate(expense.getExpenseDate());
        existing.setYear(expense.getExpenseDate().getYear());
        existing.setMonth(expense.getExpenseDate().getMonthValue());
        existing.setReceiptUrl(expense.getReceiptUrl());
        existing.setUpdatedBy(expense.getUpdatedBy());
        
        return expenseRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"expenses", "incentiveCalculations"}, allEntries = true)
    public Expense submitExpense(UUID id) {
        log.info("Submitting expense: {}", id);
        Expense expense = getExpenseById(id);
        
        if (!"DRAFT".equals(expense.getStatus())) {
            throw new RuntimeException("Only DRAFT expenses can be submitted");
        }
        
        expense.setStatus("SUBMITTED");
        return expenseRepository.save(expense);
    }
    
    @Transactional
    @CacheEvict(value = {"expenses", "incentiveCalculations"}, allEntries = true)
    public void deleteExpense(UUID id) {
        log.info("Deleting expense: {}", id);
        Expense expense = getExpenseById(id);
        
        if (!"DRAFT".equals(expense.getStatus())) {
            throw new RuntimeException("Only DRAFT expenses can be deleted");
        }
        
        expenseRepository.delete(expense);
    }
}

