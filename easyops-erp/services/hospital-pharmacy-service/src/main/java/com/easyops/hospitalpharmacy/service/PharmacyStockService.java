package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.StockAdjustmentRequest;
import com.easyops.hospitalpharmacy.dto.request.StockReceiptRequest;
import com.easyops.hospitalpharmacy.dto.request.StockTransferRequest;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentMovementResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyStockItemResponse;
import com.easyops.hospitalpharmacy.dto.response.StockTransferMovementResponse;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.PharmacyStock;
import com.easyops.hospitalpharmacy.entity.StockMovement;
import com.easyops.hospitalpharmacy.repository.DrugRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyLocationRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyStockRepository;
import com.easyops.hospitalpharmacy.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PharmacyStockService {

    private final PharmacyLocationRepository pharmacyLocationRepository;
    private final DrugRepository drugRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public List<PharmacyStockItemResponse> getStockForLocation(UUID pharmacyLocationId) {
        PharmacyLocation location = getLocation(pharmacyLocationId);
        List<PharmacyStock> stock = pharmacyStockRepository.findByPharmacyLocation(location);
        return stock.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockTransferMovementResponse> getTransfersForLocation(UUID pharmacyLocationId) {
        PharmacyLocation location = getLocation(pharmacyLocationId);
        return stockMovementRepository.findByPharmacyLocationOrderByMovementTimeDesc(location).stream()
                .filter(movement ->
                        "transfer_in".equalsIgnoreCase(movement.getMovementType())
                                || "transfer_out".equalsIgnoreCase(movement.getMovementType()))
                .map(this::toTransferResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentMovementResponse> getAdjustmentsForLocation(UUID pharmacyLocationId) {
        PharmacyLocation location = getLocation(pharmacyLocationId);
        return stockMovementRepository.findByPharmacyLocationOrderByMovementTimeDesc(location).stream()
                .filter(movement -> "adjustment".equalsIgnoreCase(movement.getMovementType()))
                .map(this::toAdjustmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void receiveStock(UUID pharmacyLocationId, StockReceiptRequest request) {
        PharmacyLocation location = getLocation(pharmacyLocationId);

        for (StockReceiptRequest.Line line : request.getLines()) {
            Drug drug = getDrug(line.getDrugId());
            BigDecimal qty = line.getQuantity();

            if (!drug.getManufacturer().isActive()) {
                throw new IllegalArgumentException(
                        "Cannot receive stock from inactive manufacturer '" + drug.getManufacturer().getName()
                                + "' for drug " + drug.getId());
            }
            if (line.getExpiryDate() != null && line.getExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Cannot receive expired stock for drug " + drug.getId());
            }

            PharmacyStock stock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(location, drug, line.getBatchNumber())
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(location)
                            .drug(drug)
                            .batchNumber(line.getBatchNumber())
                            .expiryDate(line.getExpiryDate())
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());

            stock.setQuantityOnHand(stock.getQuantityOnHand().add(qty));
            if (line.getExpiryDate() != null) {
                stock.setExpiryDate(line.getExpiryDate());
            }
            pharmacyStockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(location)
                    .drug(drug)
                    .movementType("receipt")
                    .quantity(qty)
                    .batchNumber(line.getBatchNumber())
                    .referenceType(line.getReferenceType())
                    .referenceId(line.getReferenceId())
                    .notes(line.getNotes())
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    @Transactional
    public void adjustStock(UUID pharmacyLocationId, StockAdjustmentRequest request, UUID requestedByUserId) {
        PharmacyLocation location = getLocation(pharmacyLocationId);
        UUID approvedBy = request.getApprovedByUserId();
        if (approvedBy == null) {
            approvedBy = requestedByUserId;
            request.setApprovedByUserId(approvedBy);
        }

        for (StockAdjustmentRequest.Line line : request.getLines()) {
            Drug drug = getDrug(line.getDrugId());
            BigDecimal delta = line.getQuantityDelta();

            PharmacyStock stock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(location, drug, line.getBatchNumber())
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(location)
                            .drug(drug)
                            .batchNumber(line.getBatchNumber())
                            .expiryDate(line.getExpiryDate())
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());

            BigDecimal newQty = stock.getQuantityOnHand().add(delta);
            if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Adjustment would result in negative stock for drug " + drug.getId());
            }
            stock.setQuantityOnHand(newQty);
            if (line.getExpiryDate() != null) {
                stock.setExpiryDate(line.getExpiryDate());
            }
            pharmacyStockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(location)
                    .drug(drug)
                    .movementType("adjustment")
                    .quantity(delta)
                    .batchNumber(line.getBatchNumber())
                    .reasonCode(line.getReason())
                    .requestedBy(requestedByUserId)
                    .approvedBy(approvedBy)
                    .notes(appendApprovalTag(line.getReason(), approvedBy, requestedByUserId))
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    @Transactional
    public void transferStock(UUID sourcePharmacyLocationId, StockTransferRequest request, UUID requestedByUserId) {
        PharmacyLocation source = getLocation(sourcePharmacyLocationId);
        PharmacyLocation destination = getLocation(request.getDestinationPharmacyLocationId());
        UUID approvedBy = request.getApprovedByUserId();
        if (approvedBy == null) {
            approvedBy = requestedByUserId;
            request.setApprovedByUserId(approvedBy);
        }

        for (StockTransferRequest.Line line : request.getLines()) {
            Drug drug = getDrug(line.getDrugId());
            BigDecimal qty = line.getQuantity();

            // Deduct from source
            PharmacyStock sourceStock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(source, drug, line.getBatchNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Insufficient stock at source for drug " + drug.getId()));

            BigDecimal newSourceQty = sourceStock.getQuantityOnHand().subtract(qty);
            if (newSourceQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Transfer would result in negative stock at source for drug " + drug.getId());
            }
            sourceStock.setQuantityOnHand(newSourceQty);
            pharmacyStockRepository.save(sourceStock);

            StockMovement sourceMovement = StockMovement.builder()
                    .pharmacyLocation(source)
                    .drug(drug)
                    .movementType("transfer_out")
                    .quantity(qty.negate())
                    .batchNumber(line.getBatchNumber())
                    .requestedBy(requestedByUserId)
                    .approvedBy(approvedBy)
                    .notes(appendApprovalTag(line.getNotes(), approvedBy, requestedByUserId))
                    .build();
            stockMovementRepository.save(sourceMovement);

            // Add to destination
            PharmacyStock destStock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(destination, drug, line.getBatchNumber())
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(destination)
                            .drug(drug)
                            .batchNumber(line.getBatchNumber())
                            .expiryDate(line.getExpiryDate())
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());

            destStock.setQuantityOnHand(destStock.getQuantityOnHand().add(qty));
            if (line.getExpiryDate() != null) {
                destStock.setExpiryDate(line.getExpiryDate());
            }
            pharmacyStockRepository.save(destStock);

            StockMovement destMovement = StockMovement.builder()
                    .pharmacyLocation(destination)
                    .drug(drug)
                    .movementType("transfer_in")
                    .quantity(qty)
                    .batchNumber(line.getBatchNumber())
                    .requestedBy(requestedByUserId)
                    .approvedBy(approvedBy)
                    .notes(appendApprovalTag(line.getNotes(), approvedBy, requestedByUserId))
                    .build();
            stockMovementRepository.save(destMovement);
        }
    }

    private static String appendApprovalTag(String base, UUID approvedBy, UUID requestedBy) {
        String tag = "[requestedBy=" + requestedBy + " approvedBy=" + approvedBy + "]";
        if (base == null || base.isBlank()) {
            return tag;
        }
        return base.trim() + " " + tag;
    }

    private PharmacyLocation getLocation(UUID id) {
        return pharmacyLocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + id));
    }

    private Drug getDrug(UUID id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + id));
    }

    private PharmacyStockItemResponse toResponse(PharmacyStock stock) {
        return PharmacyStockItemResponse.builder()
                .stockId(stock.getId())
                .drugId(stock.getDrug().getId())
                .genericName(stock.getDrug().getGenericName())
                .brandName(stock.getDrug().getBrandName())
                .strength(stock.getDrug().getStrength())
                .form(stock.getDrug().getForm())
                .route(stock.getDrug().getRoute())
                .batchNumber(stock.getBatchNumber())
                .expiryDate(stock.getExpiryDate())
                .quantityOnHand(stock.getQuantityOnHand())
                .build();
    }

    private StockTransferMovementResponse toTransferResponse(StockMovement movement) {
        return StockTransferMovementResponse.builder()
                .movementId(movement.getId())
                .pharmacyLocationId(movement.getPharmacyLocation().getId())
                .pharmacyLocationName(movement.getPharmacyLocation().getName())
                .drugId(movement.getDrug().getId())
                .genericName(movement.getDrug().getGenericName())
                .brandName(movement.getDrug().getBrandName())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .batchNumber(movement.getBatchNumber())
                .movementTime(movement.getMovementTime())
                .notes(movement.getNotes())
                .build();
    }

    private StockAdjustmentMovementResponse toAdjustmentResponse(StockMovement movement) {
        return StockAdjustmentMovementResponse.builder()
                .movementId(movement.getId())
                .pharmacyLocationId(movement.getPharmacyLocation().getId())
                .pharmacyLocationName(movement.getPharmacyLocation().getName())
                .drugId(movement.getDrug().getId())
                .genericName(movement.getDrug().getGenericName())
                .brandName(movement.getDrug().getBrandName())
                .quantityDelta(movement.getQuantity())
                .batchNumber(movement.getBatchNumber())
                .movementTime(movement.getMovementTime())
                .reason(movement.getReasonCode() != null ? movement.getReasonCode() : movement.getNotes())
                .build();
    }
}

