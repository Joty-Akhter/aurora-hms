package com.easyops.ap.service;

import com.easyops.ap.dto.PaymentRequest;
import com.easyops.ap.dto.PaymentAllocationRequest;
import com.easyops.ap.entity.APPayment;
import com.easyops.ap.entity.APPaymentAllocation;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.entity.Vendor;
import com.easyops.ap.repository.APPaymentRepository;
import com.easyops.ap.repository.APPaymentAllocationRepository;
import com.easyops.ap.repository.APBillRepository;
import com.easyops.ap.repository.VendorRepository;
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
public class PaymentService {
    private final APPaymentRepository paymentRepository;
    private final APPaymentAllocationRepository allocationRepository;
    private final APBillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final BillService billService;
    private final GlJournalPostingService glJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<APPayment> getAllPayments(UUID organizationId) {
        return paymentRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<APPayment> getPaymentsByStatus(UUID organizationId, String status) {
        return paymentRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public APPayment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
    }
    
    @Transactional
    public APPayment createPayment(PaymentRequest request) {
        if (paymentRepository.existsByPaymentNumber(request.getPaymentNumber())) {
            throw new RuntimeException("Payment number already exists: " + request.getPaymentNumber());
        }
        
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        APPayment payment = new APPayment();
        payment.setOrganizationId(request.getOrganizationId());
        payment.setPaymentNumber(request.getPaymentNumber());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setVendor(vendor);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setCheckNumber(request.getCheckNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setBankAccountId(request.getBankAccountId());
        payment.setNotes(request.getNotes());
        payment.setCreatedBy(request.getCreatedBy());
        payment.setStatus("DRAFT");
        
        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (PaymentAllocationRequest allocationRequest : request.getAllocations()) {
            APBill bill = billRepository.findById(allocationRequest.getBillId())
                    .orElseThrow(() -> new RuntimeException("Bill not found"));
            
            APPaymentAllocation allocation = new APPaymentAllocation();
            allocation.setBill(bill);
            allocation.setAllocatedAmount(allocationRequest.getAllocatedAmount());
            
            payment.addAllocation(allocation);
            totalAllocated = totalAllocated.add(allocationRequest.getAllocatedAmount());
        }
        
        if (totalAllocated.compareTo(request.getAmount()) > 0) {
            throw new RuntimeException("Total allocation cannot exceed payment amount");
        }
        
        APPayment savedPayment = paymentRepository.save(payment);
        
        for (APPaymentAllocation allocation : savedPayment.getAllocations()) {
            BigDecimal totalPaid = allocationRepository.sumAllocatedByBillId(allocation.getBill().getId());
            if (totalPaid == null) totalPaid = BigDecimal.ZERO;
            billService.updateBillPaymentStatus(allocation.getBill().getId(), totalPaid);
        }
        
        return savedPayment;
    }
    
    @Transactional
    public APPayment postPayment(UUID id, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to post a payment");
        APPayment payment = getPaymentById(id);
        if (!"DRAFT".equals(payment.getStatus())) {
            throw new RuntimeException("Only draft payments can be posted");
        }
        if (payment.getGlJournalId() != null) {
            throw new RuntimeException("Payment already has a GL journal entry");
        }

        UUID journalId = glJournalPostingService.postPaymentJournal(payment, actorUserId);
        payment.setGlJournalId(journalId);
        payment.setStatus("POSTED");
        log.info("Posted payment {} to GL journal {}", payment.getPaymentNumber(), journalId);
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public void deletePayment(UUID id) {
        APPayment payment = getPaymentById(id);
        if ("POSTED".equals(payment.getStatus())) {
            throw new RuntimeException("Cannot delete posted payment");
        }
        paymentRepository.deleteById(id);
    }
}

