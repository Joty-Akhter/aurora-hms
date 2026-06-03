package com.easyops.ap.service;

import com.easyops.accountingperiod.AccountingPeriodResolver;
import com.easyops.ap.dto.BillRequest;
import com.easyops.ap.dto.BillLineRequest;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.entity.APBillLine;
import com.easyops.ap.entity.Vendor;
import com.easyops.ap.repository.APBillRepository;
import com.easyops.ap.repository.VendorRepository;
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
public class BillService {
    private final APBillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final AccountingPeriodResolver accountingPeriodResolver;
    private final GlJournalPostingService glJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<APBill> getAllBills(UUID organizationId) {
        return billRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<APBill> getBillsByStatus(UUID organizationId, String status) {
        return billRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<APBill> getOutstandingBills(UUID organizationId) {
        return billRepository.findOutstandingBills(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<APBill> getOverdueBills(UUID organizationId) {
        return billRepository.findOverdueBills(organizationId, LocalDate.now());
    }
    
    @Transactional(readOnly = true)
    public APBill getBillById(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with ID: " + id));
    }
    
    @Transactional
    public APBill createBill(BillRequest request, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to create a bill");

        if (billRepository.existsByBillNumber(request.getBillNumber())) {
            throw new RuntimeException("Bill number already exists: " + request.getBillNumber());
        }

        UUID periodId = resolvePeriodId(request, actorUserId);
        
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        APBill bill = new APBill();
        bill.setOrganizationId(request.getOrganizationId());
        bill.setBillNumber(request.getBillNumber());
        bill.setBillDate(request.getBillDate());
        bill.setDueDate(request.getDueDate());
        bill.setVendor(vendor);
        bill.setPeriodId(periodId);
        bill.setCurrency(request.getCurrency());
        bill.setPurchaseOrderRef(request.getPurchaseOrderRef());
        bill.setNotes(request.getNotes());
        bill.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : actorUserId);
        bill.setStatus("DRAFT");
        bill.setPaymentStatus("UNPAID");
        
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        int lineNumber = 1;
        for (BillLineRequest lineRequest : request.getLines()) {
            APBillLine line = new APBillLine();
            line.setLineNumber(lineNumber++);
            line.setDescription(lineRequest.getDescription());
            line.setQuantity(lineRequest.getQuantity());
            line.setUnitPrice(lineRequest.getUnitPrice());
            line.setDiscountPercent(lineRequest.getDiscountPercent());
            line.setTaxPercent(lineRequest.getTaxPercent());
            line.setAccountId(lineRequest.getAccountId());
            
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
            
            bill.addLine(line);
        }
        
        bill.setSubtotal(subtotal);
        bill.setTaxAmount(totalTax);
        bill.setTotalAmount(subtotal.add(totalTax));
        bill.setBalanceDue(subtotal.add(totalTax));
        
        return billRepository.save(bill);
    }

    private UUID resolvePeriodId(BillRequest request, UUID actorUserId) {
        UUID canonical = accountingPeriodResolver.resolvePeriodId(
                request.getOrganizationId(),
                request.getBillDate(),
                actorUserId);
        UUID requested = request.getPeriodId();
        if (requested != null && !requested.equals(canonical)) {
            throw new RuntimeException(
                    "Selected period does not contain bill date " + request.getBillDate()
                            + ". Omit periodId to resolve automatically.");
        }
        if (requested == null) {
            log.info("Resolved period {} for bill date {}", canonical, request.getBillDate());
        }
        return canonical;
    }
    
    @Transactional
    public APBill postBill(UUID id, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "Authenticated user is required to post a bill");
        APBill bill = billRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with ID: " + id));
        if (bill.getPeriodId() == null) {
            throw new RuntimeException("Cannot post bill without an accounting period");
        }
        if (!"DRAFT".equals(bill.getStatus())) {
            throw new RuntimeException("Only draft bills can be posted");
        }
        if (bill.getGlJournalId() != null) {
            throw new RuntimeException("Bill already has a GL journal entry");
        }

        UUID journalId = glJournalPostingService.postBillJournal(bill, actorUserId);
        bill.setGlJournalId(journalId);
        bill.setStatus("POSTED");
        log.info("Posted bill {} to GL journal {}", bill.getBillNumber(), journalId);
        return billRepository.save(bill);
    }
    
    @Transactional
    public APBill updateBillPaymentStatus(UUID id, BigDecimal paidAmount) {
        APBill bill = getBillById(id);
        bill.setPaidAmount(paidAmount);
        bill.setBalanceDue(bill.getTotalAmount().subtract(paidAmount));
        
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            bill.setPaymentStatus("UNPAID");
        } else if (paidAmount.compareTo(bill.getTotalAmount()) >= 0) {
            bill.setPaymentStatus("PAID");
        } else {
            bill.setPaymentStatus("PARTIAL");
        }
        
        if (LocalDate.now().isAfter(bill.getDueDate()) && 
            bill.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            bill.setPaymentStatus("OVERDUE");
        }
        
        return billRepository.save(bill);
    }
    
    @Transactional
    public void deleteBill(UUID id) {
        APBill bill = getBillById(id);
        if ("POSTED".equals(bill.getStatus())) {
            throw new RuntimeException("Cannot delete posted bill");
        }
        billRepository.deleteById(id);
    }
}

