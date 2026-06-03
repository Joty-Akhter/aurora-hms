package com.easyops.accounting.service;

import com.easyops.accounting.config.AccountingValidationProperties;
import com.easyops.accounting.dto.JournalEntryRequest;
import com.easyops.accounting.dto.JournalLineRequest;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.repository.JournalEntryRepository;
import com.easyops.accounting.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JournalPostingService {
    
    private final JournalEntryRepository journalRepository;
    private final JournalLineRepository journalLineRepository;
    private final PeriodService periodService;
    private final AccountingValidationProperties validationProperties;
    
    @Transactional
    public JournalEntry createJournalEntry(JournalEntryRequest request, UUID createdBy) {
        validateJournalDate(request.getJournalDate(), request);

        // Validate period is open
        Period period = periodService.getPeriodForDate(request.getOrganizationId(), request.getJournalDate());
        if (!"OPEN".equals(period.getStatus())) {
            throw new RuntimeException("Cannot post to a closed or locked period");
        }
        
        // Validate double-entry (debits = credits)
        BigDecimal totalDebit = request.getLines().stream()
            .map(JournalLineRequest::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = request.getLines().stream()
            .map(JournalLineRequest::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("Journal entry is not balanced. Debits: " + totalDebit + ", Credits: " + totalCredit);
        }
        
        // Generate journal number
        String journalNumber = generateJournalNumber();
        
        // Create journal entry
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setOrganizationId(request.getOrganizationId());
        journalEntry.setJournalNumber(journalNumber);
        journalEntry.setJournalDate(request.getJournalDate());
        journalEntry.setPeriodId(period.getId());
        journalEntry.setJournalType(request.getJournalType());
        journalEntry.setReferenceNumber(request.getReferenceNumber());
        journalEntry.setSourceModule(request.getSourceModule());
        journalEntry.setSourceDocumentId(request.getSourceDocumentId());
        journalEntry.setDescription(request.getDescription());
        journalEntry.setStatus("DRAFT");
        journalEntry.setTotalDebit(totalDebit);
        journalEntry.setTotalCredit(totalCredit);
        journalEntry.setCreatedBy(createdBy);

        int lineNumber = 1;
        for (JournalLineRequest lineReq : request.getLines()) {
            JournalLine line = new JournalLine();
            line.setJournalEntry(journalEntry);
            line.setLineNumber(lineNumber++);
            line.setAccountId(lineReq.getAccountId());
            line.setDebitAmount(lineReq.getDebitAmount());
            line.setCreditAmount(lineReq.getCreditAmount());
            line.setDescription(lineReq.getDescription());
            line.setDepartmentId(lineReq.getDepartmentId());
            line.setCostCenterId(lineReq.getCostCenterId());
            line.setTags(lineReq.getTags());
            journalEntry.getLines().add(line);
        }

        journalEntry = journalRepository.save(journalEntry);

        log.info("Created journal entry: {} with {} lines", journalNumber, journalEntry.getLines().size());
        return journalEntry;
    }
    
    @Transactional
    public JournalEntry postJournalEntry(UUID journalEntryId, UUID postedBy) {
        JournalEntry journalEntry = journalRepository.findById(journalEntryId)
            .orElseThrow(() -> new RuntimeException("Journal entry not found"));
        
        if (!"DRAFT".equals(journalEntry.getStatus())) {
            throw new RuntimeException("Only draft journal entries can be posted");
        }
        
        // Re-validate period is still open
        Period period = periodService.getPeriodById(journalEntry.getPeriodId());
        if (!"OPEN".equals(period.getStatus())) {
            throw new RuntimeException("Cannot post to a closed or locked period");
        }
        
        // Post the entry
        journalEntry.setStatus("POSTED");
        journalEntry.setPostedAt(LocalDateTime.now());
        journalEntry.setPostedBy(postedBy);
        
        journalEntry = journalRepository.save(journalEntry);
        log.info("Posted journal entry: {}", journalEntry.getJournalNumber());
        
        // Note: Account balances will be updated by database trigger
        
        return journalEntry;
    }
    
    public Page<JournalEntry> getJournalEntries(UUID organizationId, Pageable pageable) {
        return journalRepository.findByOrganizationIdOrderByJournalDateDesc(organizationId, pageable);
    }
    
    public JournalEntry getJournalEntry(UUID journalEntryId) {
        return journalRepository.findById(journalEntryId)
            .orElseThrow(() -> new RuntimeException("Journal entry not found"));
    }
    
    public List<JournalLine> getJournalLines(UUID journalEntryId) {
        return journalLineRepository.findByJournalEntry_IdOrderByLineNumber(journalEntryId);
    }
    
    @Transactional
    public JournalEntry reverseJournalEntry(UUID journalEntryId, UUID reversedBy) {
        JournalEntry original = getJournalEntry(journalEntryId);
        
        if (!"POSTED".equals(original.getStatus())) {
            throw new RuntimeException("Only posted journal entries can be reversed");
        }
        
        // Mark original as reversed
        original.setStatus("REVERSED");
        original.setReversedAt(LocalDateTime.now());
        original.setReversedBy(reversedBy);
        
        // Create reversal entry
        String reversalNumber = generateJournalNumber();
        JournalEntry reversal = new JournalEntry();
        reversal.setOrganizationId(original.getOrganizationId());
        reversal.setJournalNumber(reversalNumber);
        reversal.setJournalDate(original.getJournalDate());
        reversal.setPeriodId(original.getPeriodId());
        reversal.setJournalType("ADJUSTMENT");
        reversal.setReferenceNumber(original.getJournalNumber());
        reversal.setDescription("Reversal of " + original.getJournalNumber() + " - " + original.getDescription());
        reversal.setStatus("DRAFT");
        reversal.setTotalDebit(original.getTotalCredit());
        reversal.setTotalCredit(original.getTotalDebit());
        reversal.setCreatedBy(reversedBy);
        
        List<JournalLine> originalLines = getJournalLines(journalEntryId);
        int lineNumber = 1;
        for (JournalLine origLine : originalLines) {
            JournalLine line = new JournalLine();
            line.setJournalEntry(reversal);
            line.setLineNumber(lineNumber++);
            line.setAccountId(origLine.getAccountId());
            line.setDebitAmount(origLine.getCreditAmount());
            line.setCreditAmount(origLine.getDebitAmount());
            line.setDescription("Reversal: " + origLine.getDescription());
            line.setDepartmentId(origLine.getDepartmentId());
            reversal.getLines().add(line);
        }

        reversal = journalRepository.save(reversal);
        
        original.setReversalJournalId(reversal.getId());
        journalRepository.save(original);

        reversal = postJournalEntry(reversal.getId(), reversedBy);
        
        log.info("Reversed journal entry: {} with reversal: {}", original.getJournalNumber(), reversalNumber);
        return reversal;
    }

    private void validateJournalDate(LocalDate journalDate, JournalEntryRequest request) {
        if (journalDate == null) {
            throw new RuntimeException("Journal date is required");
        }
        if (validationProperties.isAllowBackdated()) {
            return;
        }
        if (request.getSourceModule() != null && !request.getSourceModule().isBlank()) {
            return;
        }
        String journalType = request.getJournalType();
        if (journalType != null && !"MANUAL".equalsIgnoreCase(journalType)) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (journalDate.isBefore(today)) {
            throw new RuntimeException(
                    "Backdated journal entries are not allowed. Journal date: " + journalDate + ", today: " + today);
        }
    }

    private String generateJournalNumber() {
        String prefix = "JV";
        Integer maxNumber = journalRepository.findMaxJournalNumber(prefix);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("%s%06d", prefix, nextNumber);
    }
}

