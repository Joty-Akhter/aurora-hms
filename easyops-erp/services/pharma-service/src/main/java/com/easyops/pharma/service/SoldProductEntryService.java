package com.easyops.pharma.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easyops.pharma.entity.ProductDisbursement;
import com.easyops.pharma.entity.ProductDisbursementLine;
import com.easyops.pharma.entity.SoldProductEntry;
import com.easyops.pharma.entity.SoldProductEntryLine;
import com.easyops.pharma.repository.TerritoryRepository;
import com.easyops.pharma.repository.ProductDisbursementLineRepository;
import com.easyops.pharma.repository.ProductDisbursementRepository;
import com.easyops.pharma.repository.SoldProductEntryLineRepository;
import com.easyops.pharma.repository.SoldProductEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoldProductEntryService {

    private final SoldProductEntryRepository soldProductEntryRepository;
    private final SoldProductEntryLineRepository soldProductEntryLineRepository;
    private final TerritoryRepository territoryRepository;
    private final TargetService targetService;
    private final ProductDisbursementRepository disbursementRepository;
    private final ProductDisbursementLineRepository disbursementLineRepository;

    @Transactional(readOnly = true)
    public List<SoldProductEntry> getAll(UUID organizationId) {
        log.info("Fetching all sold product entries for organization: {}", organizationId);
        List<SoldProductEntry> entries = soldProductEntryRepository.findByOrganizationId(organizationId);
        log.info("Found {} sold product entries for organization {}", entries.size(), organizationId);
        if (!entries.isEmpty()) {
            log.info("First entry: id={}, territoryId={}, entryDate={}, status={}", 
                entries.get(0).getId(), entries.get(0).getTerritoryId(), 
                entries.get(0).getEntryDate(), entries.get(0).getStatus());
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public List<SoldProductEntry> getByTerritory(UUID territoryId) {
        return soldProductEntryRepository.findByTerritoryId(territoryId);
    }

    @Transactional(readOnly = true)
    public List<SoldProductEntry> getByTerritoryAndPeriod(UUID territoryId, Integer year, Integer month) {
        return soldProductEntryRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }

    @Transactional(readOnly = true)
    public SoldProductEntry getById(UUID id) {
        return soldProductEntryRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Sold product entry not found: " + id));
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage", "soldProductEntries"}, allEntries = true)
    public SoldProductEntry create(SoldProductEntry entry) {
        log.info("Creating sold product entry for territory: {}, organizationId: {}", entry.getTerritoryId(), entry.getOrganizationId());

        territoryRepository.findById(entry.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found: " + entry.getTerritoryId()));

        LocalDate d = entry.getEntryDate();
        entry.setYear(d.getYear());
        entry.setMonth(d.getMonthValue());

        if (entry.getLines() != null && !entry.getLines().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (SoldProductEntryLine line : entry.getLines()) {
                if (line.getQuantitySold() != null && line.getTpWithVat() != null) {
                    line.setProductAmount(line.getQuantitySold().multiply(line.getTpWithVat()));
                    total = total.add(line.getProductAmount());
                }
            }
            entry.setTotalProductAmount(total);
        }

        if (entry.getStatus() == null) {
            entry.setStatus("DRAFT");
        }

        List<SoldProductEntryLine> linesToSave = entry.getLines();
        entry.setLines(null);

        SoldProductEntry saved = soldProductEntryRepository.save(entry);
        log.info("Saved sold product entry with ID: {}, organizationId: {}", saved.getId(), saved.getOrganizationId());

        if (linesToSave != null && !linesToSave.isEmpty()) {
            for (SoldProductEntryLine line : linesToSave) {
                line.setSoldProductEntryId(saved.getId());
                soldProductEntryLineRepository.save(line);
            }
            log.info("Saved {} lines for entry {}", linesToSave.size(), saved.getId());
        }

        saved.setLines(linesToSave);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage", "soldProductEntries"}, allEntries = true)
    public SoldProductEntry update(UUID id, SoldProductEntry entry) {
        log.info("Updating sold product entry: {}", id);
        SoldProductEntry existing = getById(id);

        if ("SUBMITTED".equals(existing.getStatus()) || "COMPLETED".equals(existing.getStatus())) {
            throw new RuntimeException("Cannot update entry with status: " + existing.getStatus());
        }

        existing.setEntryDate(entry.getEntryDate());
        existing.setYear(entry.getEntryDate().getYear());
        existing.setMonth(entry.getEntryDate().getMonthValue());
        existing.setEmployeeId(entry.getEmployeeId());
        existing.setNotes(entry.getNotes());
        existing.setUpdatedBy(entry.getUpdatedBy());

        if (entry.getLines() != null) {
            // Clear existing lines by modifying the collection, not replacing it
            if (existing.getLines() != null) {
                existing.getLines().clear();
            } else {
                existing.setLines(new ArrayList<>());
            }

            BigDecimal total = BigDecimal.ZERO;
            for (SoldProductEntryLine line : entry.getLines()) {
                line.setSoldProductEntryId(id);
                if (line.getQuantitySold() != null && line.getTpWithVat() != null) {
                    line.setProductAmount(line.getQuantitySold().multiply(line.getTpWithVat()));
                    total = total.add(line.getProductAmount());
                }
                existing.getLines().add(line);
            }
            existing.setTotalProductAmount(total);
        }

        return soldProductEntryRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage", "soldProductEntries"}, allEntries = true)
    public SoldProductEntry submit(UUID id) {
        log.info("Submitting sold product entry: {}", id);
        SoldProductEntry entry = getById(id);

        if (!"DRAFT".equals(entry.getStatus())) {
            throw new RuntimeException("Only DRAFT entries can be submitted");
        }
        if (entry.getLines() == null || entry.getLines().isEmpty()) {
            throw new RuntimeException("Cannot submit entry without lines");
        }

        entry.setStatus("SUBMITTED");
        SoldProductEntry saved = soldProductEntryRepository.save(entry);
        targetService.calculateCoverageSafely(entry.getTerritoryId(), entry.getYear(), entry.getMonth());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage", "soldProductEntries"}, allEntries = true)
    public SoldProductEntry complete(UUID id) {
        log.info("Completing sold product entry: {}", id);
        SoldProductEntry entry = getById(id);

        if (!"SUBMITTED".equals(entry.getStatus())) {
            throw new RuntimeException("Only SUBMITTED entries can be completed");
        }

        entry.setStatus("COMPLETED");
        SoldProductEntry saved = soldProductEntryRepository.save(entry);
        targetService.calculateCoverageSafely(entry.getTerritoryId(), entry.getYear(), entry.getMonth());
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"deposits", "targetCoverage", "soldProductEntries"}, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting sold product entry: {}", id);
        SoldProductEntry entry = getById(id);
        if (!"DRAFT".equals(entry.getStatus())) {
            throw new RuntimeException("Only DRAFT entries can be deleted");
        }
        soldProductEntryLineRepository.findBySoldProductEntryId(id)
                .forEach(soldProductEntryLineRepository::delete);
        soldProductEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCoveredAmount(UUID territoryId, Integer year, Integer month) {
        BigDecimal t = soldProductEntryRepository.getTotalCoveredAmountForTerritoryAndMonth(territoryId, year, month);
        return t != null ? t : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingQuantity(UUID territoryId, UUID productId) {
        BigDecimal totalDisbursed = BigDecimal.ZERO;
        for (ProductDisbursement d : disbursementRepository.findByTerritoryId(territoryId)) {
            for (ProductDisbursementLine line : disbursementLineRepository.findByProductDisbursementId(d.getId())) {
                if (line.getProductId().equals(productId)) {
                    totalDisbursed = totalDisbursed.add(line.getCurrentMonthQuantity());
                }
            }
        }
        BigDecimal totalSold = soldProductEntryLineRepository.sumQuantitySoldByTerritoryAndProduct(territoryId, productId);
        if (totalSold == null) totalSold = BigDecimal.ZERO;
        return totalDisbursed.subtract(totalSold);
    }
}
