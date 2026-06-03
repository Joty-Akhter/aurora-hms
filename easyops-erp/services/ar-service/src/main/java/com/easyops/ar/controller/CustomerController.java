package com.easyops.ar.controller;

import com.easyops.ar.dto.CustomerRequest;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.security.AccountingRbacService;
import com.easyops.ar.security.RbacRequestHeaders;
import com.easyops.ar.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ar/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AR Customers", description = "Customer management for Accounts Receivable")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all customers for an organization")
    public ResponseEntity<List<Customer>> getAllCustomers(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/customers - organizationId: {}, activeOnly: {}", organizationId, activeOnly);
        List<Customer> customers = activeOnly
                ? customerService.getActiveCustomers(organizationId)
                : customerService.getAllCustomers(organizationId);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<Customer> getCustomerById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Customer customer = customerService.getCustomerById(id);
        accountingRbac.requireAccountingView(actor, customer.getOrganizationId());
        log.info("GET /api/ar/customers/{}", id);
        return ResponseEntity.ok(customer);
    }

    @PostMapping
    @Operation(summary = "Create new customer")
    public ResponseEntity<Customer> createCustomer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody CustomerRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        log.info("POST /api/ar/customers - Creating customer: {}", request.getCustomerCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    public ResponseEntity<Customer> updateCustomer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Customer existing = customerService.getCustomerById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("PUT /api/ar/customers/{}", id);
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer")
    public ResponseEntity<Void> deleteCustomer(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Customer existing = customerService.getCustomerById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/ar/customers/{}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
