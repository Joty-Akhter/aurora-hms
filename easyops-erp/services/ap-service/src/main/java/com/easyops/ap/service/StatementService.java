package com.easyops.ap.service;

import com.easyops.ap.client.EmailClient;
import com.easyops.ap.dto.VendorStatementResponse;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.entity.APPayment;
import com.easyops.ap.entity.Vendor;
import com.easyops.ap.repository.APBillRepository;
import com.easyops.ap.repository.APPaymentRepository;
import com.easyops.ap.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {
    
    private final VendorRepository vendorRepository;
    private final APBillRepository billRepository;
    private final APPaymentRepository paymentRepository;
    private final EmailClient emailClient;
    
    @Transactional(readOnly = true)
    public VendorStatementResponse generateVendorStatement(UUID vendorId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating statement for vendor: {} from {} to {}", vendorId, startDate, endDate);
        
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + vendorId));
        
        VendorStatementResponse statement = new VendorStatementResponse();
        statement.setVendorId(vendorId);
        statement.setVendorName(vendor.getVendorName());
        statement.setVendorCode(vendor.getVendorCode());
        statement.setStatementDate(LocalDate.now());
        statement.setPeriodStart(startDate);
        statement.setPeriodEnd(endDate);
        
        List<VendorStatementResponse.StatementTransaction> transactions = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        
        // Calculate opening balance
        BigDecimal openingBalance = calculateBalanceAsOf(vendorId, startDate.minusDays(1));
        statement.setOpeningBalance(openingBalance);
        runningBalance = openingBalance;
        
        // Get all bills in period
        List<APBill> bills = billRepository.findByVendorIdAndBillDateBetween(vendorId, startDate, endDate);
        
        for (APBill bill : bills) {
            if ("POSTED".equals(bill.getStatus())) {
                runningBalance = runningBalance.add(bill.getTotalAmount());
                
                transactions.add(new VendorStatementResponse.StatementTransaction(
                        bill.getBillDate(),
                        "BILL",
                        bill.getBillNumber(),
                        bill.getNotes() != null ? bill.getNotes() : "Vendor Bill",
                        bill.getTotalAmount(),
                        BigDecimal.ZERO,
                        runningBalance
                ));
            }
        }
        
        // Get all payments in period
        List<APPayment> payments = paymentRepository.findByVendorIdAndPaymentDateBetween(vendorId, startDate, endDate);
        
        for (APPayment payment : payments) {
            if ("POSTED".equals(payment.getStatus())) {
                runningBalance = runningBalance.subtract(payment.getAmount());
                
                transactions.add(new VendorStatementResponse.StatementTransaction(
                        payment.getPaymentDate(),
                        "PAYMENT",
                        payment.getPaymentNumber(),
                        "Payment - " + payment.getPaymentMethod(),
                        BigDecimal.ZERO,
                        payment.getAmount(),
                        runningBalance
                ));
            }
        }
        
        // Sort by date
        transactions.sort(Comparator.comparing(VendorStatementResponse.StatementTransaction::getDate));
        
        statement.setTransactions(transactions);
        statement.setClosingBalance(runningBalance);
        
        log.info("Statement generated with {} transactions", transactions.size());
        return statement;
    }
    
    private BigDecimal calculateBalanceAsOf(UUID vendorId, LocalDate asOfDate) {
        BigDecimal balance = BigDecimal.ZERO;
        
        // Sum all posted bills
        List<APBill> bills = billRepository.findByVendorIdAndBillDateBefore(vendorId, asOfDate.plusDays(1));
        for (APBill bill : bills) {
            if ("POSTED".equals(bill.getStatus())) {
                balance = balance.add(bill.getTotalAmount());
            }
        }
        
        // Subtract all posted payments
        List<APPayment> payments = paymentRepository.findByVendorIdAndPaymentDateBefore(vendorId, asOfDate.plusDays(1));
        for (APPayment payment : payments) {
            if ("POSTED".equals(payment.getStatus())) {
                balance = balance.subtract(payment.getAmount());
            }
        }
        
        return balance;
    }
    
    /**
     * Email statement to vendor
     */
    @Transactional
    public void emailStatement(UUID vendorId, LocalDate startDate, LocalDate endDate) {
        log.info("Emailing statement to vendor: {}", vendorId);
        
        VendorStatementResponse statement = generateVendorStatement(vendorId, startDate, endDate);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        if (vendor.getEmail() == null || vendor.getEmail().isEmpty()) {
            throw new RuntimeException("Vendor has no email address on file");
        }
        
        // Build HTML email content
        String htmlContent = buildStatementHtml(statement);
        String subject = String.format("Vendor Statement - %s to %s", startDate, endDate);
        
        emailClient.sendHtmlEmail(vendor.getEmail(), subject, htmlContent, statement.getVendorId());
        
        log.info("Statement emailed successfully to: {}", vendor.getEmail());
    }
    
    /**
     * Build HTML email content for vendor statement
     */
    private String buildStatementHtml(VendorStatementResponse statement) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>Vendor Statement</h2>");
        html.append("<p><strong>Vendor:</strong> ").append(statement.getVendorName()).append("</p>");
        html.append("<p><strong>Period:</strong> ").append(statement.getPeriodStart())
            .append(" to ").append(statement.getPeriodEnd()).append("</p>");
        html.append("<p><strong>Opening Balance:</strong> $").append(statement.getOpeningBalance()).append("</p>");
        
        html.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse; width: 100%; margin-top: 20px;'>");
        html.append("<thead><tr style='background-color: #f5f5f5;'>");
        html.append("<th>Date</th><th>Type</th><th>Reference</th><th>Description</th>");
        html.append("<th align='right'>Debit</th><th align='right'>Credit</th><th align='right'>Balance</th>");
        html.append("</tr></thead><tbody>");
        
        for (VendorStatementResponse.StatementTransaction txn : statement.getTransactions()) {
            html.append("<tr>");
            html.append("<td>").append(txn.getDate()).append("</td>");
            html.append("<td>").append(txn.getType()).append("</td>");
            html.append("<td>").append(txn.getReference()).append("</td>");
            html.append("<td>").append(txn.getDescription()).append("</td>");
            html.append("<td align='right'>").append(txn.getDebit().compareTo(BigDecimal.ZERO) > 0 ? "$" + txn.getDebit() : "-").append("</td>");
            html.append("<td align='right'>").append(txn.getCredit().compareTo(BigDecimal.ZERO) > 0 ? "$" + txn.getCredit() : "-").append("</td>");
            html.append("<td align='right'><strong>$").append(txn.getBalance()).append("</strong></td>");
            html.append("</tr>");
        }
        
        html.append("</tbody></table>");
        html.append("<p style='margin-top: 20px;'><strong>Closing Balance: $").append(statement.getClosingBalance()).append("</strong></p>");
        html.append("<p style='color: #666; font-size: 12px; margin-top: 30px;'>This is an automatically generated statement. Please contact us if you have any questions.</p>");
        html.append("</body></html>");
        
        return html.toString();
    }
}

