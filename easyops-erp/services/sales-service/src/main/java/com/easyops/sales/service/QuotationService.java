package com.easyops.sales.service;

import com.easyops.sales.dto.QuotationRequest;
import com.easyops.sales.entity.Quotation;
import com.easyops.sales.entity.QuotationLine;
import com.easyops.sales.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    
    private final QuotationRepository quotationRepository;
    
    @Value("${sales.quotation.auto-number-prefix:QT}")
    private String quotationPrefix;
    
    @Value("${sales.quotation.validity-days:30}")
    private int validityDays;
    
    @Transactional(readOnly = true)
    public List<Quotation> getAllQuotations(UUID organizationId) {
        return quotationRepository.findByOrganizationIdOrderByQuotationDateDesc(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByStatus(UUID organizationId, String status) {
        return quotationRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByCustomer(UUID organizationId, UUID customerId) {
        return quotationRepository.findByOrganizationIdAndCustomerId(organizationId, customerId);
    }
    
    @Transactional(readOnly = true)
    public Quotation getQuotationById(UUID id) {
        return quotationRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found with id: " + id));
    }
    
    @Transactional
    public Quotation createQuotation(QuotationRequest request) {
        log.info("Creating quotation for organization: {}", request.getOrganizationId());
        
        // Customer details should be provided in the request
        // Customer master data is managed in CRM service, not in Sales service
        if (request.getCustomerId() == null) {
            throw new RuntimeException("Customer ID is required");
        }
        
        // Create quotation
        Quotation quotation = new Quotation();
        quotation.setOrganizationId(request.getOrganizationId());
        quotation.setQuotationNumber(generateQuotationNumber(request.getOrganizationId()));
        quotation.setQuotationDate(request.getQuotationDate() != null ? request.getQuotationDate() : LocalDate.now());
        quotation.setValidUntil(request.getValidUntil() != null ? request.getValidUntil() : LocalDate.now().plusDays(validityDays));
        
        quotation.setCustomerId(request.getCustomerId());
        quotation.setCustomerName(request.getCustomerName()); // From request
        quotation.setCustomerEmail(request.getCustomerEmail()); // From request
        quotation.setContactPerson(request.getContactPerson());
        quotation.setBillingAddress(request.getBillingAddress());
        quotation.setShippingAddress(request.getShippingAddress());
        
        quotation.setDiscountPercent(request.getDiscountPercent());
        quotation.setDiscountAmount(request.getDiscountAmount());
        quotation.setNotes(request.getNotes());
        quotation.setTermsAndConditions(request.getTermsAndConditions());
        quotation.setSalesPersonId(request.getSalesPersonId());
        quotation.setSalesPersonName(request.getSalesPersonName());
        quotation.setStatus("DRAFT");
        
        // Add lines
        int lineNumber = 1;
        for (QuotationRequest.QuotationLineRequest lineReq : request.getLines()) {
            QuotationLine line = new QuotationLine();
            line.setLineNumber(lineNumber++);
            line.setProductId(lineReq.getProductId());
            line.setProductCode(lineReq.getProductCode());
            line.setProductName(lineReq.getProductName());
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitOfMeasure(lineReq.getUnitOfMeasure());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setDiscountPercent(lineReq.getDiscountPercent());
            line.setTaxPercent(lineReq.getTaxPercent());
            
            line.calculateLineTotal();
            quotation.addLine(line);
        }
        
        // Calculate totals
        quotation.calculateTotals();
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public Quotation updateQuotation(UUID id, QuotationRequest request) {
        log.info("Updating quotation: {}", id);
        
        Quotation quotation = getQuotationById(id);
        
        // Only allow updates if quotation is in DRAFT status
        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new RuntimeException("Cannot update quotation in status: " + quotation.getStatus());
        }
        
        // Update basic fields
        quotation.setQuotationDate(request.getQuotationDate());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setContactPerson(request.getContactPerson());
        quotation.setBillingAddress(request.getBillingAddress());
        quotation.setShippingAddress(request.getShippingAddress());
        quotation.setDiscountPercent(request.getDiscountPercent());
        quotation.setDiscountAmount(request.getDiscountAmount());
        quotation.setNotes(request.getNotes());
        quotation.setTermsAndConditions(request.getTermsAndConditions());
        quotation.setSalesPersonId(request.getSalesPersonId());
        quotation.setSalesPersonName(request.getSalesPersonName());
        
        // Update lines - clear and re-add
        quotation.getLines().clear();
        
        int lineNumber = 1;
        for (QuotationRequest.QuotationLineRequest lineReq : request.getLines()) {
            QuotationLine line = new QuotationLine();
            line.setLineNumber(lineNumber++);
            line.setProductId(lineReq.getProductId());
            line.setProductCode(lineReq.getProductCode());
            line.setProductName(lineReq.getProductName());
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitOfMeasure(lineReq.getUnitOfMeasure());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setDiscountPercent(lineReq.getDiscountPercent());
            line.setTaxPercent(lineReq.getTaxPercent());
            
            line.calculateLineTotal();
            quotation.addLine(line);
        }
        
        // Recalculate totals
        quotation.calculateTotals();
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public Quotation sendQuotation(UUID id) {
        log.info("Sending quotation: {}", id);
        
        Quotation quotation = getQuotationById(id);
        
        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new RuntimeException("Can only send quotations in DRAFT status");
        }
        
        quotation.setStatus("SENT");
        
        // TODO: Send email notification
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public Quotation acceptQuotation(UUID id) {
        log.info("Accepting quotation: {}", id);
        
        Quotation quotation = getQuotationById(id);
        
        if (!"SENT".equals(quotation.getStatus())) {
            throw new RuntimeException("Can only accept quotations in SENT status");
        }
        
        quotation.setStatus("ACCEPTED");
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public Quotation rejectQuotation(UUID id) {
        log.info("Rejecting quotation: {}", id);
        
        Quotation quotation = getQuotationById(id);
        
        if (!"SENT".equals(quotation.getStatus())) {
            throw new RuntimeException("Can only reject quotations in SENT status");
        }
        
        quotation.setStatus("REJECTED");
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public Quotation markAsConverted(UUID id, UUID salesOrderId) {
        log.info("Marking quotation {} as converted to order {}", id, salesOrderId);
        
        Quotation quotation = getQuotationById(id);
        quotation.setStatus("CONVERTED");
        quotation.setConvertedToOrderId(salesOrderId);
        quotation.setConvertedDate(LocalDateTime.now());
        
        return quotationRepository.save(quotation);
    }
    
    @Transactional
    public void deleteQuotation(UUID id) {
        log.info("Deleting quotation: {}", id);
        
        Quotation quotation = getQuotationById(id);
        
        // Only allow deletion of DRAFT or REJECTED quotations
        if (!"DRAFT".equals(quotation.getStatus()) && !"REJECTED".equals(quotation.getStatus())) {
            throw new RuntimeException("Cannot delete quotation in status: " + quotation.getStatus());
        }
        
        quotationRepository.delete(quotation);
    }
    
    @Transactional
    public void expireOldQuotations(UUID organizationId) {
        log.info("Expiring old quotations for organization: {}", organizationId);
        
        List<Quotation> expiredQuotations = quotationRepository.findExpiredQuotations(organizationId, LocalDate.now());
        
        for (Quotation quotation : expiredQuotations) {
            quotation.setStatus("EXPIRED");
        }
        
        if (!expiredQuotations.isEmpty()) {
            quotationRepository.saveAll(expiredQuotations);
            log.info("Expired {} quotations", expiredQuotations.size());
        }
    }
    
    private String generateQuotationNumber(UUID organizationId) {
        Integer maxNumber = quotationRepository.findMaxQuotationNumber(organizationId, quotationPrefix);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("%s%06d", quotationPrefix, nextNumber);
    }
}

