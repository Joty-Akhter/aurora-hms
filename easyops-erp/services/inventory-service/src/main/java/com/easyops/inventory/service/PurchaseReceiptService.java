package com.easyops.inventory.service;

import com.easyops.inventory.dto.PurchaseReceiptRequest;
import com.easyops.inventory.entity.BatchLot;
import com.easyops.inventory.entity.SerialNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseReceiptService {
    
    private final StockService stockService;
    private final BatchLotService batchLotService;
    private final SerialNumberService serialNumberService;
    
    /**
     * Process goods receipt from purchase order
     */
    @Transactional
    public Map<String, Object> processPurchaseReceipt(PurchaseReceiptRequest request) {
        log.info("Processing purchase receipt for PO: {}, Warehouse: {}", 
                 request.getPurchaseOrderId(), request.getWarehouseId());
        
        List<Map<String, Object>> processedLines = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (PurchaseReceiptRequest.ReceiptLine line : request.getLines()) {
            Map<String, Object> lineResult = new HashMap<>();
            
            // Handle batch tracking if batch number provided
            if (line.getBatchNumber() != null && !line.getBatchNumber().isEmpty()) {
                BatchLot batch = createOrUpdateBatch(
                    request.getOrganizationId(),
                    line.getProductId(),
                    line.getBatchNumber(),
                    line.getLotNumber(),
                    line.getQuantity(),
                    line.getManufactureDate(),
                    line.getExpiryDate(),
                    request.getSupplierId(),
                    request.getPoNumber(),
                    request.getReceiptDate()
                );
                lineResult.put("batchId", batch.getId());
            }
            
            // Handle serial number tracking if provided
            if (line.getSerialNumber() != null && !line.getSerialNumber().isEmpty()) {
                SerialNumber serial = registerSerialNumber(
                    request.getOrganizationId(),
                    line.getProductId(),
                    line.getSerialNumber(),
                    request.getWarehouseId(),
                    line.getBatchNumber(),
                    request.getReceiptDate()
                );
                lineResult.put("serialId", serial.getId());
            }
            
            // Receive stock
            stockService.receiveStock(
                request.getOrganizationId(),
                line.getProductId(),
                request.getWarehouseId(),
                line.getQuantity(),
                line.getUnitCost(),
                "PURCHASE_ORDER",
                request.getPurchaseOrderId(),
                request.getCreatedBy()
            );
            
            BigDecimal lineValue = line.getQuantity().multiply(line.getUnitCost());
            totalValue = totalValue.add(lineValue);
            
            lineResult.put("productId", line.getProductId());
            lineResult.put("quantity", line.getQuantity());
            lineResult.put("unitCost", line.getUnitCost());
            lineResult.put("totalCost", lineValue);
            
            processedLines.add(lineResult);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("purchaseOrderId", request.getPurchaseOrderId());
        result.put("warehouseId", request.getWarehouseId());
        result.put("receiptDate", request.getReceiptDate());
        result.put("lines", processedLines);
        result.put("totalValue", totalValue);
        result.put("status", "COMPLETED");
        
        log.info("Successfully processed purchase receipt. Total value: {}", totalValue);
        
        return result;
    }
    
    private BatchLot createOrUpdateBatch(UUID organizationId, UUID productId, String batchNumber,
                                        String lotNumber, BigDecimal quantity, 
                                        java.time.LocalDate manufactureDate, java.time.LocalDate expiryDate,
                                        UUID supplierId, String poNumber, java.time.LocalDate receiptDate) {
        
        BatchLot batch = new BatchLot();
        batch.setOrganizationId(organizationId);
        batch.setProductId(productId);
        batch.setBatchNumber(batchNumber);
        batch.setLotNumber(lotNumber);
        batch.setManufactureDate(manufactureDate);
        batch.setExpiryDate(expiryDate);
        batch.setSupplierId(supplierId);
        batch.setPoNumber(poNumber);
        batch.setReceiptDate(receiptDate);
        batch.setInitialQuantity(quantity);
        
        return batchLotService.createBatch(batch);
    }
    
    private SerialNumber registerSerialNumber(UUID organizationId, UUID productId, String serialNumber,
                                              UUID warehouseId, String batchNumber, java.time.LocalDate purchaseDate) {
        
        SerialNumber serial = new SerialNumber();
        serial.setOrganizationId(organizationId);
        serial.setProductId(productId);
        serial.setSerialNumber(serialNumber);
        serial.setWarehouseId(warehouseId);
        serial.setBatchNumber(batchNumber);
        serial.setPurchaseDate(purchaseDate);
        
        return serialNumberService.registerSerial(serial);
    }
}

