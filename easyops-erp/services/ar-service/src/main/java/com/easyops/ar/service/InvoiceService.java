package com.easyops.ar.service;

import com.easyops.accountingperiod.AccountingPeriodResolver;
import com.easyops.ar.dto.InvoiceRequest;
import com.easyops.ar.dto.InvoiceLineRequest;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.ARInvoiceLine;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {
    
    private final ARInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final AccountingPeriodResolver accountingPeriodResolver;
    private final GlJournalPostingService glJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<ARInvoice> getAllInvoices(UUID organizationId) {
        log.debug("Fetching all invoices for organization: {}", organizationId);
        return invoiceRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ARInvoice> getInvoicesByStatus(UUID organizationId, String status) {
        log.debug("Fetching invoices by status: {} for organization: {}", status, organizationId);
        return invoiceRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<ARInvoice> getOutstandingInvoices(UUID organizationId) {
        log.debug("Fetching outstanding invoices for organization: {}", organizationId);
        return invoiceRepository.findOutstandingInvoices(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ARInvoice> getOverdueInvoices(UUID organizationId) {
        log.debug("Fetching overdue invoices for organization: {}", organizationId);
        return invoiceRepository.findOverdueInvoices(organizationId, LocalDate.now());
    }
    
    @Transactional(readOnly = true)
    public ARInvoice getInvoiceById(UUID id) {
        log.debug("Fetching invoice by ID: {}", id);
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
    }
    
    @Transactional
    public ARInvoice createInvoice(InvoiceRequest request, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to create an invoice");

        // Auto-generate invoice number if not provided
        String invoiceNumber = request.getInvoiceNumber();
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            invoiceNumber = generateInvoiceNumber(request.getOrganizationId());
            log.info("Auto-generated invoice number: {}", invoiceNumber);
        } else {
            log.info("Creating new invoice: {}", invoiceNumber);
        }
        
        // Check if invoice number already exists
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw new RuntimeException("Invoice number already exists: " + invoiceNumber);
        }

        UUID periodId = resolvePeriodId(request, actorUserId);
        
        // Fetch customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + request.getCustomerId()));
        
        // Calculate invoice total first for credit limit check
        BigDecimal invoiceTotal = calculateInvoiceTotal(request.getLines());
        
        // Check credit limit
        if (customer.getCreditLimit() != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
            BigDecimal newBalance = currentBalance.add(invoiceTotal);
            
            if (newBalance.compareTo(customer.getCreditLimit()) > 0) {
                log.warn("Credit limit exceeded for customer {}: Current={}, New Invoice={}, Credit Limit={}", 
                    customer.getCustomerName(), currentBalance, invoiceTotal, customer.getCreditLimit());
                
                throw new RuntimeException(String.format(
                    "Credit limit exceeded for customer '%s'. Current balance: %.2f, New invoice: %.2f, Credit limit: %.2f, New balance would be: %.2f",
                    customer.getCustomerName(), currentBalance, invoiceTotal, customer.getCreditLimit(), newBalance));
            }
        }
        
        // Create invoice
        ARInvoice invoice = new ARInvoice();
        invoice.setOrganizationId(request.getOrganizationId());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setCustomer(customer);
        invoice.setPeriodId(periodId);
        invoice.setCurrency(request.getCurrency());
        invoice.setNotes(request.getNotes());
        invoice.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : actorUserId);
        invoice.setStatus("DRAFT");
        invoice.setPaymentStatus("UNPAID");
        
        // Add lines and calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        int lineNumber = 1;
        for (InvoiceLineRequest lineRequest : request.getLines()) {
            ARInvoiceLine line = new ARInvoiceLine();
            line.setLineNumber(lineNumber++);
            line.setDescription(lineRequest.getDescription());
            line.setQuantity(lineRequest.getQuantity());
            line.setUnitPrice(lineRequest.getUnitPrice());
            line.setDiscountPercent(lineRequest.getDiscountPercent());
            line.setTaxPercent(lineRequest.getTaxPercent());
            line.setAccountId(lineRequest.getAccountId());
            
            // Calculate line total
            BigDecimal lineSubtotal = lineRequest.getQuantity()
                    .multiply(lineRequest.getUnitPrice())
                    .setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal discount = lineSubtotal
                    .multiply(lineRequest.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            lineSubtotal = lineSubtotal.subtract(discount);
            
            BigDecimal lineTax = lineSubtotal
                    .multiply(lineRequest.getTaxPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            BigDecimal lineTotal = lineSubtotal.add(lineTax);
            line.setLineTotal(lineTotal);
            
            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
            
            invoice.addLine(line);
        }
        
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(totalTax);
        invoice.setTotalAmount(subtotal.add(totalTax));
        invoice.setBalanceDue(subtotal.add(totalTax));
        
        return invoiceRepository.save(invoice);
    }
    
    @Transactional
    public ARInvoice postInvoice(UUID id, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to post an invoice");
        log.info("Posting invoice: {}", id);
        
        ARInvoice invoice = invoiceRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
        if (invoice.getPeriodId() == null) {
            throw new RuntimeException("Cannot post invoice without an accounting period");
        }
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new RuntimeException("Only draft invoices can be posted");
        }
        if (invoice.getGlJournalId() != null) {
            throw new RuntimeException("Invoice already has a GL journal entry");
        }
        
        UUID journalId = glJournalPostingService.postInvoiceJournal(invoice, actorUserId);
        invoice.setGlJournalId(journalId);
        invoice.setStatus("POSTED");
        invoice.setPaymentStatus("UNPAID");
        
        // Update customer balance
        Customer customer = invoice.getCustomer();
        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        customer.setCurrentBalance(currentBalance.add(invoice.getTotalAmount()));
        
        customerRepository.save(customer);
        log.info("Posted invoice {} to GL journal {} and updated customer {} balance to: {}",
                invoice.getInvoiceNumber(), journalId, customer.getCustomerName(), customer.getCurrentBalance());
        
        return invoiceRepository.save(invoice);
    }
    
    @Transactional
    public ARInvoice updateInvoicePaymentStatus(UUID id, BigDecimal paidAmount) {
        log.info("Updating payment status for invoice: {}", id);
        
        ARInvoice invoice = getInvoiceById(id);
        invoice.setPaidAmount(paidAmount);
        invoice.setBalanceDue(invoice.getTotalAmount().subtract(paidAmount));
        
        // Update payment status
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setPaymentStatus("UNPAID");
        } else if (paidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus("PAID");
        } else {
            invoice.setPaymentStatus("PARTIAL");
        }
        
        // Check if overdue
        if (LocalDate.now().isAfter(invoice.getDueDate()) && 
            invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("OVERDUE");
        }
        
        return invoiceRepository.save(invoice);
    }
    
    @Transactional
    public void deleteInvoice(UUID id) {
        log.info("Deleting invoice: {}", id);
        
        ARInvoice invoice = getInvoiceById(id);
        if ("POSTED".equals(invoice.getStatus())) {
            throw new RuntimeException("Cannot delete posted invoice");
        }
        
        invoiceRepository.deleteById(id);
    }
    
    private UUID resolvePeriodId(InvoiceRequest request, UUID actorUserId) {
        UUID canonical = accountingPeriodResolver.resolvePeriodId(
                request.getOrganizationId(),
                request.getInvoiceDate(),
                actorUserId);
        UUID requested = request.getPeriodId();
        if (requested != null && !requested.equals(canonical)) {
            throw new RuntimeException(
                    "Selected period does not contain invoice date " + request.getInvoiceDate()
                            + ". Omit periodId to resolve automatically.");
        }
        if (requested == null) {
            log.info("Resolved period {} for invoice date {}", canonical, request.getInvoiceDate());
        }
        return canonical;
    }

    /**
     * Helper method to calculate invoice total from line items
     */
    private BigDecimal calculateInvoiceTotal(List<InvoiceLineRequest> lines) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (InvoiceLineRequest lineRequest : lines) {
            BigDecimal lineSubtotal = lineRequest.getQuantity()
                    .multiply(lineRequest.getUnitPrice())
                    .setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal discount = lineSubtotal
                    .multiply(lineRequest.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            lineSubtotal = lineSubtotal.subtract(discount);
            
            BigDecimal lineTax = lineSubtotal
                    .multiply(lineRequest.getTaxPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            
            total = total.add(lineSubtotal).add(lineTax);
        }
        
        return total.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Helper method to generate invoice number
     */
    private String generateInvoiceNumber(UUID organizationId) {
        // Find the highest invoice number for this organization
        String prefix = "INV-";
        
        // Get count of invoices for this organization
        long count = invoiceRepository.findByOrganizationId(organizationId).size();
        
        // Generate next number (count + 1, padded to 6 digits)
        return String.format("%s%06d", prefix, count + 1);
    }
}

