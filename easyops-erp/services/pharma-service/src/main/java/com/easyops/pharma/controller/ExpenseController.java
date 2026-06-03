package com.easyops.pharma.controller;

import com.easyops.pharma.entity.Expense;
import com.easyops.pharma.entity.ExpenseCategory;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.ExpenseService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expense Management", description = "Territory-based expense tracking and management APIs")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    // Expense Category endpoints
    @GetMapping("/categories")
    @Operation(summary = "Get all expense categories")
    public ResponseEntity<List<ExpenseCategory>> getAllCategories(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/expenses/categories - organizationId: {}", organizationId);
        List<ExpenseCategory> categories = expenseService.getAllCategories(organizationId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get expense category by ID")
    public ResponseEntity<ExpenseCategory> getCategoryById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ExpenseCategory category = expenseService.getCategoryById(id);
        pharmaRbac.requirePharmaView(actor, category.getOrganizationId());
        log.info("GET /api/pharma/expenses/categories/{}", id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/categories")
    @Operation(summary = "Create new expense category")
    public ResponseEntity<ExpenseCategory> createCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ExpenseCategory category) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, category.getOrganizationId());
        log.info("POST /api/pharma/expenses/categories");
        ExpenseCategory created = expenseService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update expense category")
    public ResponseEntity<ExpenseCategory> updateCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ExpenseCategory category) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, category.getOrganizationId());
        log.info("PUT /api/pharma/expenses/categories/{}", id);
        ExpenseCategory updated = expenseService.updateCategory(id, category);
        return ResponseEntity.ok(updated);
    }

    // Expense endpoints
    @GetMapping
    @Operation(summary = "Get all expenses")
    public ResponseEntity<List<Expense>> getAllExpenses(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/expenses - organizationId: {}", organizationId);
        List<Expense> expenses = expenseService.getAllExpenses(organizationId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get expenses by territory")
    public ResponseEntity<List<Expense>> getExpensesByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/expenses/territory/{}", territoryId);
        List<Expense> expenses = expenseService.getExpensesByTerritory(territoryId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/territory/{territoryId}/period")
    @Operation(summary = "Get expenses by territory and period")
    public ResponseEntity<List<Expense>> getExpensesByTerritoryAndPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/expenses/territory/{}/period - year: {}, month: {}", territoryId, year, month);
        List<Expense> expenses = expenseService.getExpensesByTerritoryAndPeriod(territoryId, year, month);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/territory/{territoryId}/total")
    @Operation(summary = "Get total expenses for territory and month")
    public ResponseEntity<BigDecimal> getTotalExpenses(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/expenses/territory/{}/total - year: {}, month: {}", territoryId, year, month);
        BigDecimal total = expenseService.getTotalExpensesForTerritoryAndMonth(territoryId, year, month);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<Expense> getExpenseById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Expense expense = expenseService.getExpenseById(id);
        pharmaRbac.requirePharmaView(actor, expense.getOrganizationId());
        log.info("GET /api/pharma/expenses/{}", id);
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    @Operation(summary = "Create new expense")
    public ResponseEntity<Expense> createExpense(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Expense expense) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, expense.getOrganizationId());
        log.info("POST /api/pharma/expenses");
        Expense created = expenseService.createExpense(expense);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense")
    public ResponseEntity<Expense> updateExpense(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Expense expense) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, expense.getOrganizationId());
        log.info("PUT /api/pharma/expenses/{}", id);
        Expense updated = expenseService.updateExpense(id, expense);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit expense")
    public ResponseEntity<Expense> submitExpense(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Expense existing = expenseService.getExpenseById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/expenses/{}/submit", id);
        Expense expense = expenseService.submitExpense(id);
        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense")
    public ResponseEntity<Void> deleteExpense(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Expense existing = expenseService.getExpenseById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/expenses/{}", id);
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
