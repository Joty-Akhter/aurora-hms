package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.SupplierReturnOrderRequest;
import com.easyops.hospitalpharmacy.dto.response.SupplierReturnOrderLineResponse;
import com.easyops.hospitalpharmacy.dto.response.SupplierReturnOrderResponse;
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
public class SupplierReturnOrderService {

    private final SupplierReturnOrderRepository returnOrderRepository;
    private final SupplierReturnOrderLineRepository returnOrderLineRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final PharmacyLocationRepository locationRepository;
    private final DrugRepository drugRepository;
    private final PharmacyStockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public SupplierReturnOrderResponse create(SupplierReturnOrderRequest request, UUID requestedBy) {
        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + request.getManufacturerId()));
        PharmacyLocation fromLoc = locationRepository.findById(request.getFromLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + request.getFromLocationId()));

        SupplierReturnOrder order = SupplierReturnOrder.builder()
                .manufacturer(manufacturer)
                .fromLocation(fromLoc)
                .requestedBy(requestedBy)
                .returnReference(request.getReturnReference())
                .notes(request.getNotes())
                .status(SupplierReturnOrder.SupplierReturnStatus.DRAFT)
                .build();
        SupplierReturnOrder saved = returnOrderRepository.save(order);

        for (SupplierReturnOrderRequest.Line lineReq : request.getLines()) {
            Drug drug = drugRepository.findById(lineReq.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + lineReq.getDrugId()));
            SupplierReturnOrderLine line = SupplierReturnOrderLine.builder()
                    .returnOrder(saved)
                    .drug(drug)
                    .batchNumber(lineReq.getBatchNumber())
                    .expiryDate(lineReq.getExpiryDate())
                    .quantity(lineReq.getQuantity())
                    .returnReason(SupplierReturnOrderLine.ReturnReason.valueOf(lineReq.getReturnReason()))
                    .notes(lineReq.getNotes())
                    .build();
            returnOrderLineRepository.save(line);
        }

        return toResponse(saved, returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(saved));
    }

    @Transactional
    public SupplierReturnOrderResponse submit(UUID orderId) {
        SupplierReturnOrder order = getOrder(orderId);
        if (order.getStatus() != SupplierReturnOrder.SupplierReturnStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT return orders can be submitted");
        }
        order.setStatus(SupplierReturnOrder.SupplierReturnStatus.SUBMITTED);
        return toResponse(returnOrderRepository.save(order),
                returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(order));
    }

    @Transactional
    public SupplierReturnOrderResponse approve(UUID orderId, UUID approvedBy) {
        SupplierReturnOrder order = getOrder(orderId);
        if (order.getStatus() != SupplierReturnOrder.SupplierReturnStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED return orders can be approved");
        }
        order.setStatus(SupplierReturnOrder.SupplierReturnStatus.APPROVED);
        order.setApprovedBy(approvedBy);
        order.setApprovedAt(OffsetDateTime.now());
        return toResponse(returnOrderRepository.save(order),
                returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(order));
    }

    @Transactional
    public SupplierReturnOrderResponse dispatch(UUID orderId, UUID dispatchedBy) {
        SupplierReturnOrder order = getOrder(orderId);
        if (order.getStatus() != SupplierReturnOrder.SupplierReturnStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED return orders can be dispatched");
        }

        List<SupplierReturnOrderLine> lines = returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(order);
        PharmacyLocation fromLoc = order.getFromLocation();

        for (SupplierReturnOrderLine line : lines) {
            Drug drug = line.getDrug();
            BigDecimal qty = line.getQuantity();

            PharmacyStock stock = stockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(fromLoc, drug, line.getBatchNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stock not found for drug " + drug.getId() + " batch " + line.getBatchNumber()));

            BigDecimal newQty = stock.getQuantityOnHand().subtract(qty);
            if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Return would result in negative stock for drug " + drug.getId());
            }
            stock.setQuantityOnHand(newQty);
            stockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(fromLoc)
                    .drug(drug)
                    .movementType("supplier_return")
                    .quantity(qty.negate())
                    .batchNumber(line.getBatchNumber())
                    .referenceType("SUPPLIER_RETURN_ORDER")
                    .referenceId(order.getId())
                    .requestedBy(order.getRequestedBy())
                    .approvedBy(order.getApprovedBy())
                    .reasonCode(line.getReturnReason().name())
                    .notes("Supplier return: " + order.getId())
                    .build();
            movementRepository.save(movement);
        }

        order.setStatus(SupplierReturnOrder.SupplierReturnStatus.DISPATCHED);
        order.setDispatchedAt(OffsetDateTime.now());
        return toResponse(returnOrderRepository.save(order), lines);
    }

    @Transactional(readOnly = true)
    public SupplierReturnOrderResponse getById(UUID id) {
        SupplierReturnOrder order = getOrder(id);
        return toResponse(order, returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(order));
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnOrderResponse> listByFromLocation(UUID fromLocationId) {
        return returnOrderRepository.findByFromLocationIdOrderByCreatedAtDesc(fromLocationId)
                .stream().map(o -> toResponse(o, returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(o)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnOrderResponse> listByManufacturer(UUID manufacturerId) {
        return returnOrderRepository.findByManufacturerIdOrderByCreatedAtDesc(manufacturerId)
                .stream().map(o -> toResponse(o, returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(o)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnOrderResponse> listAll() {
        return returnOrderRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(o -> toResponse(o, returnOrderLineRepository.findByReturnOrderOrderByCreatedAtAsc(o)))
                .collect(Collectors.toList());
    }

    private SupplierReturnOrder getOrder(UUID id) {
        return returnOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier return order not found: " + id));
    }

    private SupplierReturnOrderResponse toResponse(SupplierReturnOrder order, List<SupplierReturnOrderLine> lines) {
        return SupplierReturnOrderResponse.builder()
                .id(order.getId())
                .manufacturerId(order.getManufacturer().getId())
                .manufacturerName(order.getManufacturer().getName())
                .fromLocationId(order.getFromLocation().getId())
                .fromLocationName(order.getFromLocation().getName())
                .status(order.getStatus().name())
                .returnReference(order.getReturnReference())
                .requestedBy(order.getRequestedBy())
                .approvedBy(order.getApprovedBy())
                .approvedAt(order.getApprovedAt())
                .dispatchedAt(order.getDispatchedAt())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .lines(lines.stream().map(this::toLineResponse).collect(Collectors.toList()))
                .build();
    }

    private SupplierReturnOrderLineResponse toLineResponse(SupplierReturnOrderLine line) {
        return SupplierReturnOrderLineResponse.builder()
                .id(line.getId())
                .drugId(line.getDrug().getId())
                .drugName(line.getDrug().getBrandName() != null ? line.getDrug().getBrandName() : line.getDrug().getGenericName())
                .batchNumber(line.getBatchNumber())
                .expiryDate(line.getExpiryDate())
                .quantity(line.getQuantity())
                .returnReason(line.getReturnReason().name())
                .notes(line.getNotes())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
