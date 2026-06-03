package com.easyops.ar.service;

import com.easyops.ar.dto.ReceiptRequest;
import com.easyops.ar.dto.ReceiptAllocationRequest;
import com.easyops.ar.entity.ARReceipt;
import com.easyops.ar.entity.ARReceiptAllocation;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.ARReceiptRepository;
import com.easyops.ar.repository.ARReceiptAllocationRepository;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {
    
    private final ARReceiptRepository receiptRepository;
    private final ARReceiptAllocationRepository allocationRepository;
    private final ARInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceService invoiceService;
    private final GlJournalPostingService glJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<ARReceipt> getAllReceipts(UUID organizationId) {
        log.debug("Fetching all receipts for organization: {}", organizationId);
        return receiptRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ARReceipt> getReceiptsByStatus(UUID organizationId, String status) {
        log.debug("Fetching receipts by status: {} for organization: {}", status, organizationId);
        return receiptRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public ARReceipt getReceiptById(UUID id) {
        log.debug("Fetching receipt by ID: {}", id);
        return receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receipt not found with ID: " + id));
    }
    
    @Transactional
    public ARReceipt createReceipt(ReceiptRequest request) {
        log.info("Creating new receipt: {}", request.getReceiptNumber());
        
        // Check if receipt number already exists
        if (receiptRepository.existsByReceiptNumber(request.getReceiptNumber())) {
            throw new RuntimeException("Receipt number already exists: " + request.getReceiptNumber());
        }
        
        // Fetch customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + request.getCustomerId()));
        
        // Create receipt
        ARReceipt receipt = new ARReceipt();
        receipt.setOrganizationId(request.getOrganizationId());
        receipt.setReceiptNumber(request.getReceiptNumber());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setCustomer(customer);
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setReferenceNumber(request.getReferenceNumber());
        receipt.setAmount(request.getAmount());
        receipt.setCurrency(request.getCurrency());
        receipt.setBankAccountId(request.getBankAccountId());
        receipt.setNotes(request.getNotes());
        receipt.setCreatedBy(request.getCreatedBy());
        receipt.setStatus("DRAFT");
        
        // Add allocations
        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (ReceiptAllocationRequest allocationRequest : request.getAllocations()) {
            ARInvoice invoice = invoiceRepository.findById(allocationRequest.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + allocationRequest.getInvoiceId()));
            
            ARReceiptAllocation allocation = new ARReceiptAllocation();
            allocation.setInvoice(invoice);
            allocation.setAllocatedAmount(allocationRequest.getAllocatedAmount());
            
            receipt.addAllocation(allocation);
            totalAllocated = totalAllocated.add(allocationRequest.getAllocatedAmount());
        }
        
        // Validate total allocation
        if (totalAllocated.compareTo(request.getAmount()) > 0) {
            throw new RuntimeException("Total allocation cannot exceed receipt amount");
        }
        
        ARReceipt savedReceipt = receiptRepository.save(receipt);
        
        // Update invoice payment status
        for (ARReceiptAllocation allocation : savedReceipt.getAllocations()) {
            BigDecimal totalPaid = allocationRepository.sumAllocatedByInvoiceId(allocation.getInvoice().getId());
            if (totalPaid == null) totalPaid = BigDecimal.ZERO;
            invoiceService.updateInvoicePaymentStatus(allocation.getInvoice().getId(), totalPaid);
        }
        
        return savedReceipt;
    }
    
    @Transactional
    public ARReceipt postReceipt(UUID id, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to post a receipt");
        log.info("Posting receipt: {}", id);
        
        ARReceipt receipt = getReceiptById(id);
        
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new RuntimeException("Only draft receipts can be posted");
        }
        if (receipt.getGlJournalId() != null) {
            throw new RuntimeException("Receipt already has a GL journal entry");
        }
        
        UUID journalId = glJournalPostingService.postReceiptJournal(receipt, actorUserId);
        receipt.setGlJournalId(journalId);
        receipt.setStatus("POSTED");
        
        // Update customer balance - reduce by receipt amount
        Customer customer = receipt.getCustomer();
        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        customer.setCurrentBalance(currentBalance.subtract(receipt.getAmount()));
        
        customerRepository.save(customer);
        log.info("Posted receipt {} to GL journal {} and updated customer {} balance to: {}",
                receipt.getReceiptNumber(), journalId, customer.getCustomerName(), customer.getCurrentBalance());
        
        return receiptRepository.save(receipt);
    }
    
    @Transactional
    public void deleteReceipt(UUID id) {
        log.info("Deleting receipt: {}", id);
        
        ARReceipt receipt = getReceiptById(id);
        if ("POSTED".equals(receipt.getStatus())) {
            throw new RuntimeException("Cannot delete posted receipt");
        }
        
        receiptRepository.deleteById(id);
    }
}

