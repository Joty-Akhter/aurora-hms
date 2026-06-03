package com.easyops.ar.service;

import com.easyops.ar.client.EmailClient;
import com.easyops.ar.dto.CustomerStatementResponse;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.ARReceipt;
import com.easyops.ar.entity.ARCreditNote;
import com.easyops.ar.entity.Customer;
import com.easyops.ar.repository.ARInvoiceRepository;
import com.easyops.ar.repository.ARReceiptRepository;
import com.easyops.ar.repository.ARCreditNoteRepository;
import com.easyops.ar.repository.CustomerRepository;
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
    
    private final CustomerRepository customerRepository;
    private final ARInvoiceRepository invoiceRepository;
    private final ARReceiptRepository receiptRepository;
    private final ARCreditNoteRepository creditNoteRepository;
    private final EmailClient emailClient;
    
    @Transactional(readOnly = true)
    public CustomerStatementResponse generateCustomerStatement(UUID customerId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating statement for customer: {} from {} to {}", customerId, startDate, endDate);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));
        
        CustomerStatementResponse statement = new CustomerStatementResponse();
        statement.setCustomerId(customerId);
        statement.setCustomerName(customer.getCustomerName());
        statement.setCustomerCode(customer.getCustomerCode());
        statement.setStatementDate(LocalDate.now());
        statement.setPeriodStart(startDate);
        statement.setPeriodEnd(endDate);
        
        List<CustomerStatementResponse.StatementTransaction> transactions = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        
        // Calculate opening balance (all transactions before start date)
        BigDecimal openingBalance = calculateBalanceAsOf(customerId, startDate.minusDays(1));
        statement.setOpeningBalance(openingBalance);
        runningBalance = openingBalance;
        
        // Get all invoices in period
        List<ARInvoice> invoices = invoiceRepository.findByCustomerIdAndInvoiceDateBetween(
                customerId, startDate, endDate);
        
        for (ARInvoice invoice : invoices) {
            if ("POSTED".equals(invoice.getStatus())) {
                runningBalance = runningBalance.add(invoice.getTotalAmount());
                
                transactions.add(new CustomerStatementResponse.StatementTransaction(
                        invoice.getInvoiceDate(),
                        "INVOICE",
                        invoice.getInvoiceNumber(),
                        invoice.getNotes() != null ? invoice.getNotes() : "Customer Invoice",
                        invoice.getTotalAmount(),
                        BigDecimal.ZERO,
                        runningBalance
                ));
            }
        }
        
        // Get all credit notes in period
        List<ARCreditNote> creditNotes = creditNoteRepository.findByCustomerIdAndCreditNoteDateBetween(
                customerId, startDate, endDate);
        
        for (ARCreditNote creditNote : creditNotes) {
            if (creditNote.getStatus() == ARCreditNote.CreditNoteStatus.POSTED) {
                runningBalance = runningBalance.subtract(creditNote.getTotalAmount());
                
                transactions.add(new CustomerStatementResponse.StatementTransaction(
                        creditNote.getCreditNoteDate(),
                        "CREDIT_NOTE",
                        creditNote.getCreditNoteNumber(),
                        creditNote.getNotes() != null ? creditNote.getNotes() : "Credit Note",
                        BigDecimal.ZERO,
                        creditNote.getTotalAmount(),
                        runningBalance
                ));
            }
        }
        
        // Get all receipts in period
        List<ARReceipt> receipts = receiptRepository.findByCustomerIdAndReceiptDateBetween(
                customerId, startDate, endDate);
        
        for (ARReceipt receipt : receipts) {
            if ("POSTED".equals(receipt.getStatus())) {
                runningBalance = runningBalance.subtract(receipt.getAmount());
                
                transactions.add(new CustomerStatementResponse.StatementTransaction(
                        receipt.getReceiptDate(),
                        "PAYMENT",
                        receipt.getReceiptNumber(),
                        "Payment - " + receipt.getPaymentMethod(),
                        BigDecimal.ZERO,
                        receipt.getAmount(),
                        runningBalance
                ));
            }
        }
        
        // Sort transactions by date
        transactions.sort(Comparator.comparing(CustomerStatementResponse.StatementTransaction::getDate));
        
        statement.setTransactions(transactions);
        statement.setClosingBalance(runningBalance);
        
        log.info("Statement generated with {} transactions", transactions.size());
        return statement;
    }
    
    private BigDecimal calculateBalanceAsOf(UUID customerId, LocalDate asOfDate) {
        BigDecimal balance = BigDecimal.ZERO;
        
        // Sum all posted invoices up to asOfDate
        List<ARInvoice> invoices = invoiceRepository.findByCustomerIdAndInvoiceDateBefore(customerId, asOfDate.plusDays(1));
        for (ARInvoice invoice : invoices) {
            if ("POSTED".equals(invoice.getStatus())) {
                balance = balance.add(invoice.getTotalAmount());
            }
        }
        
        // Subtract all posted credit notes
        List<ARCreditNote> creditNotes = creditNoteRepository.findByCustomerIdAndCreditNoteDateBefore(customerId, asOfDate.plusDays(1));
        for (ARCreditNote cn : creditNotes) {
            if (cn.getStatus() == ARCreditNote.CreditNoteStatus.POSTED) {
                balance = balance.subtract(cn.getTotalAmount());
            }
        }
        
        // Subtract all posted receipts
        List<ARReceipt> receipts = receiptRepository.findByCustomerIdAndReceiptDateBefore(customerId, asOfDate.plusDays(1));
        for (ARReceipt receipt : receipts) {
            if ("POSTED".equals(receipt.getStatus())) {
                balance = balance.subtract(receipt.getAmount());
            }
        }
        
        return balance;
    }
    
    /**
     * Email statement to customer
     */
    @Transactional
    public void emailStatement(UUID customerId, LocalDate startDate, LocalDate endDate) {
        log.info("Emailing statement to customer: {}", customerId);
        
        CustomerStatementResponse statement = generateCustomerStatement(customerId, startDate, endDate);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            throw new RuntimeException("Customer has no email address on file");
        }
        
        // Build HTML email content
        String htmlContent = buildStatementHtml(statement);
        String subject = String.format("Account Statement - %s to %s", startDate, endDate);
        
        emailClient.sendHtmlEmail(customer.getEmail(), subject, htmlContent, statement.getCustomerId());
        
        log.info("Statement emailed successfully to: {}", customer.getEmail());
    }
    
    /**
     * Build HTML email content for statement
     */
    private String buildStatementHtml(CustomerStatementResponse statement) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>Customer Statement</h2>");
        html.append("<p><strong>Customer:</strong> ").append(statement.getCustomerName()).append("</p>");
        html.append("<p><strong>Period:</strong> ").append(statement.getPeriodStart())
            .append(" to ").append(statement.getPeriodEnd()).append("</p>");
        html.append("<p><strong>Opening Balance:</strong> $").append(statement.getOpeningBalance()).append("</p>");
        
        html.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse; width: 100%; margin-top: 20px;'>");
        html.append("<thead><tr style='background-color: #f5f5f5;'>");
        html.append("<th>Date</th><th>Type</th><th>Reference</th><th>Description</th>");
        html.append("<th align='right'>Debit</th><th align='right'>Credit</th><th align='right'>Balance</th>");
        html.append("</tr></thead><tbody>");
        
        for (CustomerStatementResponse.StatementTransaction txn : statement.getTransactions()) {
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

