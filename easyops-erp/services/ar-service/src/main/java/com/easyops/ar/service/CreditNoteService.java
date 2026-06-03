package com.easyops.ar.service;

import com.easyops.ar.dto.CreditNoteRequest;
import com.easyops.ar.entity.ARCreditNote;
import com.easyops.ar.entity.ARCreditNoteLine;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.ARCreditNoteRepository;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditNoteService {
    
    private final ARCreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final ARInvoiceRepository invoiceRepository;
    private final GlJournalPostingService glJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<ARCreditNote> getAllCreditNotes(UUID organizationId) {
        log.info("Getting all credit notes for organization: {}", organizationId);
        return creditNoteRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ARCreditNote> getCreditNotesByStatus(UUID organizationId, String status) {
        log.info("Getting credit notes for organization: {} with status: {}", organizationId, status);
        ARCreditNote.CreditNoteStatus creditNoteStatus = ARCreditNote.CreditNoteStatus.valueOf(status);
        return creditNoteRepository.findByOrganizationIdAndStatus(organizationId, creditNoteStatus);
    }
    
    @Transactional(readOnly = true)
    public ARCreditNote getCreditNoteById(UUID id) {
        log.info("Getting credit note by id: {}", id);
        return creditNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit note not found with id: " + id));
    }
    
    @Transactional
    public ARCreditNote createCreditNote(CreditNoteRequest request) {
        log.info("Creating credit note: {}", request.getCreditNoteNumber());
        
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + request.getCustomerId()));
        
        ARCreditNote creditNote = new ARCreditNote();
        creditNote.setOrganizationId(request.getOrganizationId());
        creditNote.setCreditNoteNumber(request.getCreditNoteNumber());
        creditNote.setCreditNoteDate(request.getCreditNoteDate());
        creditNote.setCustomer(customer);
        creditNote.setReason(request.getReason());
        creditNote.setNotes(request.getNotes());
        creditNote.setStatus(ARCreditNote.CreditNoteStatus.DRAFT);
        
        if (request.getInvoiceId() != null) {
            ARInvoice invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + request.getInvoiceId()));
            creditNote.setInvoice(invoice);
        }
        
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        for (CreditNoteRequest.CreditNoteLineRequest lineReq : request.getLines()) {
            ARCreditNoteLine line = new ARCreditNoteLine();
            line.setCreditNote(creditNote);
            line.setLineNumber(lineReq.getLineNumber());
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setDiscountPercent(lineReq.getDiscountPercent() != null ? lineReq.getDiscountPercent() : BigDecimal.ZERO);
            line.setTaxPercent(lineReq.getTaxPercent() != null ? lineReq.getTaxPercent() : BigDecimal.ZERO);
            line.setAccountId(lineReq.getAccountId());
            
            BigDecimal lineSubtotal = lineReq.getQuantity().multiply(lineReq.getUnitPrice());
            BigDecimal discount = lineSubtotal.multiply(line.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            lineSubtotal = lineSubtotal.subtract(discount);
            
            BigDecimal lineTax = lineSubtotal.multiply(line.getTaxPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);
            
            line.setLineTotal(lineTotal.setScale(2, RoundingMode.HALF_UP));
            
            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
            
            creditNote.getLines().add(line);
        }
        
        creditNote.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        creditNote.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        creditNote.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));
        
        ARCreditNote savedCreditNote = creditNoteRepository.save(creditNote);
        log.info("Credit note created successfully with id: {}", savedCreditNote.getId());
        
        return savedCreditNote;
    }
    
    @Transactional
    public ARCreditNote postCreditNote(UUID id, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to post a credit note");
        log.info("Posting credit note: {}", id);
        
        ARCreditNote creditNote = creditNoteRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Credit note not found with id: " + id));
        
        if (creditNote.getStatus() != ARCreditNote.CreditNoteStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT credit notes can be posted");
        }
        
        UUID journalId = glJournalPostingService.postCreditNoteJournal(creditNote, actorUserId);
        creditNote.setGlJournalId(journalId);
        creditNote.setStatus(ARCreditNote.CreditNoteStatus.POSTED);
        
        Customer customer = creditNote.getCustomer();
        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        customer.setCurrentBalance(currentBalance.subtract(creditNote.getTotalAmount()).max(BigDecimal.ZERO));
        customerRepository.save(customer);
        
        if (creditNote.getInvoice() != null) {
            ARInvoice invoice = creditNote.getInvoice();
            BigDecimal newBalance = invoice.getBalanceDue().subtract(creditNote.getTotalAmount());
            invoice.setBalanceDue(newBalance.max(BigDecimal.ZERO));
            invoiceRepository.save(invoice);
            log.info("Updated invoice {} balance due to: {}", invoice.getInvoiceNumber(), invoice.getBalanceDue());
        }
        
        ARCreditNote postedCreditNote = creditNoteRepository.save(creditNote);
        log.info("Posted credit note {} to GL journal {}", creditNote.getCreditNoteNumber(), journalId);
        
        return postedCreditNote;
    }
    
    @Transactional
    public void deleteCreditNote(UUID id) {
        log.info("Deleting credit note: {}", id);
        
        ARCreditNote creditNote = getCreditNoteById(id);
        
        if (creditNote.getStatus() == ARCreditNote.CreditNoteStatus.POSTED || 
            creditNote.getStatus() == ARCreditNote.CreditNoteStatus.APPLIED) {
            throw new RuntimeException("Cannot delete posted or applied credit notes");
        }
        
        creditNoteRepository.delete(creditNote);
        log.info("Credit note deleted successfully");
    }
}
