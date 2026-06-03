package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.StockRequisitionApprovalRequest;
import com.easyops.hospitalpharmacy.dto.request.StockRequisitionRequest;
import com.easyops.hospitalpharmacy.dto.response.StockRequisitionLineResponse;
import com.easyops.hospitalpharmacy.dto.response.StockRequisitionResponse;
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
public class StockRequisitionService {

    private final StockRequisitionRepository requisitionRepository;
    private final StockRequisitionLineRepository requisitionLineRepository;
    private final PharmacyLocationRepository locationRepository;
    private final DrugRepository drugRepository;
    private final PharmacyStockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public StockRequisitionResponse create(StockRequisitionRequest request, UUID requestedBy) {
        PharmacyLocation from = getLocation(request.getFromLocationId());
        PharmacyLocation to = getLocation(request.getToLocationId());

        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Source and destination pharmacy locations must differ");
        }

        StockRequisition requisition = StockRequisition.builder()
                .fromLocation(from)
                .toLocation(to)
                .requestedBy(requestedBy)
                .notes(request.getNotes())
                .status(StockRequisition.StockRequisitionStatus.DRAFT)
                .build();
        StockRequisition saved = requisitionRepository.save(requisition);

        for (StockRequisitionRequest.Line lineReq : request.getLines()) {
            Drug drug = getDrug(lineReq.getDrugId());
            StockRequisitionLine line = StockRequisitionLine.builder()
                    .requisition(saved)
                    .drug(drug)
                    .requestedQuantity(lineReq.getRequestedQuantity())
                    .batchNumber(lineReq.getBatchNumber())
                    .notes(lineReq.getNotes())
                    .build();
            requisitionLineRepository.save(line);
        }

        return toResponse(saved, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(saved));
    }

    @Transactional
    public StockRequisitionResponse submit(UUID requisitionId) {
        StockRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != StockRequisition.StockRequisitionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT requisitions can be submitted");
        }
        req.setStatus(StockRequisition.StockRequisitionStatus.SUBMITTED);
        req.setSubmittedAt(OffsetDateTime.now());
        return toResponse(requisitionRepository.save(req),
                requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(req));
    }

    @Transactional
    public StockRequisitionResponse approve(UUID requisitionId, StockRequisitionApprovalRequest request, UUID approvedBy) {
        StockRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != StockRequisition.StockRequisitionStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED requisitions can be approved");
        }

        List<StockRequisitionLine> lines = requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(req);

        boolean anyApproved = false;
        boolean allApproved = true;
        for (StockRequisitionLine line : lines) {
            StockRequisitionApprovalRequest.LineApproval lineApproval = request.getLineApprovals() == null ? null :
                    request.getLineApprovals().stream()
                    .filter(la -> la.getLineId().equals(line.getId()))
                    .findFirst().orElse(null);
            if (lineApproval != null && lineApproval.getApprovedQuantity() != null) {
                line.setApprovedQuantity(lineApproval.getApprovedQuantity());
                requisitionLineRepository.save(line);
                if (lineApproval.getApprovedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    anyApproved = true;
                }
                if (lineApproval.getApprovedQuantity().compareTo(line.getRequestedQuantity()) < 0) {
                    allApproved = false;
                }
            } else {
                allApproved = false;
            }
        }

        req.setApprovedBy(approvedBy);
        req.setApprovedAt(OffsetDateTime.now());
        req.setApprovalNotes(request.getApprovalNotes());

        if (!anyApproved) {
            req.setStatus(StockRequisition.StockRequisitionStatus.REJECTED);
        } else if (allApproved) {
            req.setStatus(StockRequisition.StockRequisitionStatus.APPROVED);
        } else {
            req.setStatus(StockRequisition.StockRequisitionStatus.PARTIALLY_APPROVED);
        }

        return toResponse(requisitionRepository.save(req), lines);
    }

    @Transactional
    public StockRequisitionResponse reject(UUID requisitionId, String rejectionNotes, UUID rejectedBy) {
        StockRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != StockRequisition.StockRequisitionStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED requisitions can be rejected");
        }
        req.setStatus(StockRequisition.StockRequisitionStatus.REJECTED);
        req.setApprovedBy(rejectedBy);
        req.setApprovedAt(OffsetDateTime.now());
        req.setApprovalNotes(rejectionNotes);
        return toResponse(requisitionRepository.save(req),
                requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(req));
    }

    @Transactional
    public StockRequisitionResponse receive(UUID requisitionId, UUID receivedBy) {
        StockRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != StockRequisition.StockRequisitionStatus.APPROVED
                && req.getStatus() != StockRequisition.StockRequisitionStatus.PARTIALLY_APPROVED) {
            throw new IllegalStateException("Only APPROVED/PARTIALLY_APPROVED requisitions can be received");
        }

        List<StockRequisitionLine> lines = requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(req);
        PharmacyLocation fromLoc = req.getFromLocation();
        PharmacyLocation toLoc = req.getToLocation();

        for (StockRequisitionLine line : lines) {
            BigDecimal qty = line.getApprovedQuantity() != null ? line.getApprovedQuantity() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Drug drug = line.getDrug();
            String batch = line.getBatchNumber();

            // Deduct from source
            PharmacyStock sourceStock = stockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(fromLoc, drug, batch)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Insufficient stock at source for drug " + drug.getId()));
            BigDecimal newSourceQty = sourceStock.getQuantityOnHand().subtract(qty);
            if (newSourceQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Transfer would result in negative stock for drug " + drug.getId());
            }
            sourceStock.setQuantityOnHand(newSourceQty);
            stockRepository.save(sourceStock);

            StockMovement outMovement = StockMovement.builder()
                    .pharmacyLocation(fromLoc)
                    .drug(drug)
                    .movementType("transfer_out")
                    .quantity(qty.negate())
                    .batchNumber(batch)
                    .referenceType("STOCK_REQUISITION")
                    .referenceId(req.getId())
                    .requestedBy(req.getRequestedBy())
                    .approvedBy(req.getApprovedBy())
                    .notes("Requisition: " + req.getId())
                    .build();
            movementRepository.save(outMovement);

            // Add to destination
            PharmacyStock destStock = stockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(toLoc, drug, batch)
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(toLoc)
                            .drug(drug)
                            .batchNumber(batch)
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());
            destStock.setQuantityOnHand(destStock.getQuantityOnHand().add(qty));
            stockRepository.save(destStock);

            StockMovement inMovement = StockMovement.builder()
                    .pharmacyLocation(toLoc)
                    .drug(drug)
                    .movementType("transfer_in")
                    .quantity(qty)
                    .batchNumber(batch)
                    .referenceType("STOCK_REQUISITION")
                    .referenceId(req.getId())
                    .requestedBy(req.getRequestedBy())
                    .approvedBy(req.getApprovedBy())
                    .notes("Requisition: " + req.getId())
                    .build();
            movementRepository.save(inMovement);

            line.setReceivedQuantity(qty);
            requisitionLineRepository.save(line);
        }

        req.setStatus(StockRequisition.StockRequisitionStatus.RECEIVED);
        return toResponse(requisitionRepository.save(req), lines);
    }

    @Transactional(readOnly = true)
    public StockRequisitionResponse getById(UUID id) {
        StockRequisition req = getRequisition(id);
        return toResponse(req, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(req));
    }

    @Transactional(readOnly = true)
    public List<StockRequisitionResponse> listByFromLocation(UUID fromLocationId) {
        return requisitionRepository.findByFromLocationIdOrderByCreatedAtDesc(fromLocationId)
                .stream().map(r -> toResponse(r, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockRequisitionResponse> listByToLocation(UUID toLocationId) {
        return requisitionRepository.findByToLocationIdOrderByCreatedAtDesc(toLocationId)
                .stream().map(r -> toResponse(r, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockRequisitionResponse> listByStatus(String status) {
        StockRequisition.StockRequisitionStatus statusEnum =
                StockRequisition.StockRequisitionStatus.valueOf(status.toUpperCase());
        return requisitionRepository.findByStatusOrderByCreatedAtDesc(statusEnum)
                .stream().map(r -> toResponse(r, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(r)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockRequisitionResponse> listAll() {
        return requisitionRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(r -> toResponse(r, requisitionLineRepository.findByRequisitionOrderByCreatedAtAsc(r)))
                .collect(Collectors.toList());
    }

    private StockRequisition getRequisition(UUID id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock requisition not found: " + id));
    }

    private PharmacyLocation getLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + id));
    }

    private Drug getDrug(UUID id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + id));
    }

    private StockRequisitionResponse toResponse(StockRequisition req, List<StockRequisitionLine> lines) {
        return StockRequisitionResponse.builder()
                .id(req.getId())
                .fromLocationId(req.getFromLocation().getId())
                .fromLocationName(req.getFromLocation().getName())
                .toLocationId(req.getToLocation().getId())
                .toLocationName(req.getToLocation().getName())
                .status(req.getStatus().name())
                .requestedBy(req.getRequestedBy())
                .submittedAt(req.getSubmittedAt())
                .approvedBy(req.getApprovedBy())
                .approvalNotes(req.getApprovalNotes())
                .approvedAt(req.getApprovedAt())
                .notes(req.getNotes())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .lines(lines.stream().map(this::toLineResponse).collect(Collectors.toList()))
                .build();
    }

    private StockRequisitionLineResponse toLineResponse(StockRequisitionLine line) {
        return StockRequisitionLineResponse.builder()
                .id(line.getId())
                .drugId(line.getDrug().getId())
                .genericName(line.getDrug().getGenericName())
                .brandName(line.getDrug().getBrandName())
                .requestedQuantity(line.getRequestedQuantity())
                .approvedQuantity(line.getApprovedQuantity())
                .receivedQuantity(line.getReceivedQuantity())
                .batchNumber(line.getBatchNumber())
                .notes(line.getNotes())
                .build();
    }
}
