package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.EmergencyPurchaseEntryRequest;
import com.easyops.hospitalpharmacy.dto.response.EmergencyPurchaseEntryResponse;
import com.easyops.hospitalpharmacy.dto.response.EmergencyPurchaseLineResponse;
import com.easyops.hospitalpharmacy.entity.*;
import com.easyops.hospitalpharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmergencyPurchaseService {

    private final EmergencyPurchaseEntryRepository entryRepository;
    private final EmergencyPurchaseLineRepository lineRepository;
    private final PharmacyLocationRepository locationRepository;
    private final DrugRepository drugRepository;
    private final PharmacyStockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public EmergencyPurchaseEntryResponse create(EmergencyPurchaseEntryRequest request, UUID requestedBy) {
        PharmacyLocation toLoc = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + request.getToLocationId()));

        EmergencyPurchaseEntry entry = EmergencyPurchaseEntry.builder()
                .toLocation(toLoc)
                .reason(request.getReason())
                .supplierName(request.getSupplierName())
                .invoiceRef(request.getInvoiceRef())
                .requestedBy(requestedBy)
                .notes(request.getNotes())
                .status(EmergencyPurchaseEntry.EmergencyPurchaseStatus.DRAFT)
                .build();
        EmergencyPurchaseEntry saved = entryRepository.save(entry);

        for (EmergencyPurchaseEntryRequest.Line lineReq : request.getLines()) {
            Drug drug = drugRepository.findById(lineReq.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + lineReq.getDrugId()));
            EmergencyPurchaseLine line = EmergencyPurchaseLine.builder()
                    .purchaseEntry(saved)
                    .drug(drug)
                    .batchNumber(lineReq.getBatchNumber())
                    .expiryDate(lineReq.getExpiryDate())
                    .quantity(lineReq.getQuantity())
                    .unitCost(lineReq.getUnitCost())
                    .notes(lineReq.getNotes())
                    .build();
            lineRepository.save(line);
        }

        return toResponse(saved, lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(saved));
    }

    @Transactional
    public EmergencyPurchaseEntryResponse approve(UUID entryId, UUID approvedBy) {
        EmergencyPurchaseEntry entry = getEntry(entryId);
        if (entry.getStatus() != EmergencyPurchaseEntry.EmergencyPurchaseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT emergency purchases can be approved");
        }
        entry.setStatus(EmergencyPurchaseEntry.EmergencyPurchaseStatus.APPROVED);
        entry.setApprovedBy(approvedBy);
        entry.setApprovedAt(OffsetDateTime.now());
        return toResponse(entryRepository.save(entry), lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(entry));
    }

    @Transactional
    public EmergencyPurchaseEntryResponse receive(UUID entryId) {
        EmergencyPurchaseEntry entry = getEntry(entryId);
        if (entry.getStatus() != EmergencyPurchaseEntry.EmergencyPurchaseStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED emergency purchases can be received");
        }

        List<EmergencyPurchaseLine> lines = lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(entry);
        PharmacyLocation toLoc = entry.getToLocation();

        for (EmergencyPurchaseLine line : lines) {
            Drug drug = line.getDrug();
            BigDecimal qty = line.getQuantity();

            PharmacyStock stock = stockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(toLoc, drug, line.getBatchNumber())
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(toLoc)
                            .drug(drug)
                            .batchNumber(line.getBatchNumber())
                            .expiryDate(line.getExpiryDate())
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());

            stock.setQuantityOnHand(stock.getQuantityOnHand().add(qty));
            if (line.getExpiryDate() != null) {
                stock.setExpiryDate(line.getExpiryDate());
            }
            stockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(toLoc)
                    .drug(drug)
                    .movementType("emergency_purchase")
                    .quantity(qty)
                    .batchNumber(line.getBatchNumber())
                    .referenceType("EMERGENCY_PURCHASE")
                    .referenceId(entry.getId())
                    .requestedBy(entry.getRequestedBy())
                    .approvedBy(entry.getApprovedBy())
                    .notes("Emergency purchase: " + entry.getId() + " reason: " + entry.getReason())
                    .build();
            movementRepository.save(movement);
        }

        entry.setStatus(EmergencyPurchaseEntry.EmergencyPurchaseStatus.RECEIVED);
        entry.setReceivedAt(OffsetDateTime.now());
        return toResponse(entryRepository.save(entry), lines);
    }

    @Transactional(readOnly = true)
    public EmergencyPurchaseEntryResponse getById(UUID id) {
        EmergencyPurchaseEntry entry = getEntry(id);
        return toResponse(entry, lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(entry));
    }

    @Transactional(readOnly = true)
    public List<EmergencyPurchaseEntryResponse> listByLocation(UUID toLocationId) {
        return entryRepository.findByToLocationIdOrderByCreatedAtDesc(toLocationId)
                .stream().map(e -> toResponse(e, lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(e)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmergencyPurchaseEntryResponse> listAll() {
        return entryRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(e -> toResponse(e, lineRepository.findByPurchaseEntryOrderByCreatedAtAsc(e)))
                .collect(Collectors.toList());
    }

    private EmergencyPurchaseEntry getEntry(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Emergency purchase entry not found: " + id));
    }

    private EmergencyPurchaseEntryResponse toResponse(EmergencyPurchaseEntry entry, List<EmergencyPurchaseLine> lines) {
        return EmergencyPurchaseEntryResponse.builder()
                .id(entry.getId())
                .toLocationId(entry.getToLocation().getId())
                .toLocationName(entry.getToLocation().getName())
                .status(entry.getStatus().name())
                .reason(entry.getReason())
                .supplierName(entry.getSupplierName())
                .invoiceRef(entry.getInvoiceRef())
                .requestedBy(entry.getRequestedBy())
                .approvedBy(entry.getApprovedBy())
                .approvedAt(entry.getApprovedAt())
                .receivedAt(entry.getReceivedAt())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .lines(lines.stream().map(this::toLineResponse).collect(Collectors.toList()))
                .build();
    }

    private EmergencyPurchaseLineResponse toLineResponse(EmergencyPurchaseLine line) {
        return EmergencyPurchaseLineResponse.builder()
                .id(line.getId())
                .drugId(line.getDrug().getId())
                .drugName(line.getDrug().getBrandName() != null ? line.getDrug().getBrandName() : line.getDrug().getGenericName())
                .batchNumber(line.getBatchNumber())
                .expiryDate(line.getExpiryDate())
                .quantity(line.getQuantity())
                .unitCost(line.getUnitCost())
                .notes(line.getNotes())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
