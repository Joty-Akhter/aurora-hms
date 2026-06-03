package com.easyops.pharma.service;

import com.easyops.pharma.entity.Territory;
import com.easyops.pharma.entity.Deposit;
import com.easyops.pharma.repository.TerritoryRepository;
import com.easyops.pharma.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositService {

    private final DepositRepository depositRepository;
    private final TerritoryRepository territoryRepository;
    private final TargetService targetService;
    private final AccountingIntegrationService accountingIntegrationService;

    @Transactional(readOnly = true)
    public List<Deposit> getAllDeposits(UUID organizationId) {
        return depositRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public List<Deposit> getDepositsByTerritory(UUID territoryId) {
        return depositRepository.findByTerritoryId(territoryId);
    }

    @Transactional(readOnly = true)
    public List<Deposit> getDepositsByTerritoryAndPeriod(UUID territoryId, Integer year, Integer month) {
        return depositRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }

    @Transactional(readOnly = true)
    public Deposit getDepositById(UUID id) {
        return depositRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found with ID: " + id));
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage"}, allEntries = true)
    public Deposit createDeposit(Deposit deposit) {
        log.info("Creating deposit amount entry for territory: {}", deposit.getTerritoryId());

        territoryRepository.findById(deposit.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + deposit.getTerritoryId()));

        LocalDate date = deposit.getDepositDate();
        deposit.setYear(date.getYear());
        deposit.setMonth(date.getMonthValue());

        if (deposit.getStatus() == null) {
            deposit.setStatus("DRAFT");
        }

        return depositRepository.save(deposit);
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage"}, allEntries = true)
    public Deposit updateDeposit(UUID id, Deposit deposit) {
        log.info("Updating deposit: {}", id);
        Deposit existing = getDepositById(id);

        if ("SUBMITTED".equals(existing.getStatus()) || "COMPLETED".equals(existing.getStatus())) {
            throw new RuntimeException("Cannot update deposit with status: " + existing.getStatus());
        }

        existing.setDepositDate(deposit.getDepositDate());
        existing.setYear(deposit.getDepositDate().getYear());
        existing.setMonth(deposit.getDepositDate().getMonthValue());
        existing.setDepositAmount(deposit.getDepositAmount());
        existing.setBankAccountId(deposit.getBankAccountId());
        existing.setBankName(deposit.getBankName());
        existing.setBankAccountNumber(deposit.getBankAccountNumber());
        existing.setEmployeeId(deposit.getEmployeeId());
        existing.setNotes(deposit.getNotes());
        existing.setUpdatedBy(deposit.getUpdatedBy());

        return depositRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage"}, allEntries = true)
    public Deposit submitDeposit(UUID id) {
        log.info("Submitting deposit: {}", id);
        Deposit deposit = getDepositById(id);

        if (!"DRAFT".equals(deposit.getStatus())) {
            throw new RuntimeException("Only DRAFT deposits can be submitted");
        }

        deposit.setStatus("SUBMITTED");
        Deposit saved = depositRepository.save(deposit);
        targetService.calculateCoverageSafely(deposit.getTerritoryId(), deposit.getYear(), deposit.getMonth());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage"}, allEntries = true)
    public Deposit completeDeposit(UUID id) {
        log.info("Completing deposit: {}", id);
        Deposit deposit = getDepositById(id);

        if (!"SUBMITTED".equals(deposit.getStatus())) {
            throw new RuntimeException("Only SUBMITTED deposits can be completed");
        }

        deposit.setStatus("COMPLETED");
        Deposit saved = depositRepository.save(deposit);
        targetService.calculateCoverageSafely(deposit.getTerritoryId(), deposit.getYear(), deposit.getMonth());
        try {
            accountingIntegrationService.postDepositJournalEntry(saved);
        } catch (Exception e) {
            log.warn("Failed to post deposit journal entry: {}", e.getMessage());
        }
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage"}, allEntries = true)
    public void deleteDeposit(UUID id) {
        log.info("Deleting deposit: {}", id);
        Deposit deposit = getDepositById(id);

        if (!"DRAFT".equals(deposit.getStatus())) {
            throw new RuntimeException("Only DRAFT deposits can be deleted");
        }

        depositRepository.delete(deposit);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalCoveredAmount(UUID territoryId, Integer year, Integer month) {
        BigDecimal total = depositRepository.getTotalCoveredAmountForTerritoryAndMonth(territoryId, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

}
