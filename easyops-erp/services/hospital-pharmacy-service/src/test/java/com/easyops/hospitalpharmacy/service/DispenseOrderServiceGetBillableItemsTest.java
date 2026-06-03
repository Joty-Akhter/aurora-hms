package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.config.PharmacyDispenseProperties;
import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import com.easyops.hospitalpharmacy.dto.response.BillableDispenseItemResponse;
import com.easyops.hospitalpharmacy.entity.DispenseLine;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.Manufacturer;
import com.easyops.hospitalpharmacy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Phase P1 — A1 {@code getBillableItems}: qty filter + override reason on billable DTO (lightweight service test).
 */
@ExtendWith(MockitoExtension.class)
class DispenseOrderServiceGetBillableItemsTest {

    @Mock
    private DispenseOrderRepository dispenseOrderRepository;
    @Mock
    private DispenseLineRepository dispenseLineRepository;
    @Mock
    private PharmacyLocationRepository pharmacyLocationRepository;
    @Mock
    private DrugRepository drugRepository;
    @Mock
    private PharmacyStockRepository pharmacyStockRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;

    private final PharmacyDispenseProperties pharmacyDispenseProperties = new PharmacyDispenseProperties();

    private DispenseOrderService dispenseOrderService;

    @BeforeEach
    void setUp() {
        dispenseOrderService = new DispenseOrderService(
                dispenseOrderRepository,
                dispenseLineRepository,
                pharmacyLocationRepository,
                drugRepository,
                pharmacyStockRepository,
                stockMovementRepository,
                pharmacyDispenseProperties,
                new PharmacyIntegrationProperties(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void getBillableItems_excludesZeroQty_includesOverrideReason() {
        UUID orderId = UUID.randomUUID();
        UUID drugId = UUID.randomUUID();

        Manufacturer manufacturer = Manufacturer.builder()
                .id(UUID.randomUUID())
                .name("Test Manufacturer")
                .build();
        Drug drug = Drug.builder()
                .id(drugId)
                .genericName("Acetaminophen")
                .brandName("Tylenol")
                .manufacturer(manufacturer)
                .build();

        DispenseOrder order = DispenseOrder.builder()
                .id(orderId)
                .status(DispenseOrder.Status.IN_PROGRESS)
                .build();

        DispenseLine skipped = DispenseLine.builder()
                .id(UUID.randomUUID())
                .dispenseOrder(order)
                .drug(drug)
                .quantityDispensed(BigDecimal.ZERO)
                .quantityReturned(BigDecimal.ZERO)
                .status(DispenseLine.Status.DISPENSED)
                .build();

        DispenseLine billable = DispenseLine.builder()
                .id(UUID.randomUUID())
                .dispenseOrder(order)
                .drug(drug)
                .quantityDispensed(new BigDecimal("10"))
                .quantityReturned(BigDecimal.ZERO)
                .status(DispenseLine.Status.FILLED_WITH_STOCK_OVERRIDE)
                .overrideReasonCode("Shelf count disagreed with ledger")
                .build();

        when(dispenseOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(dispenseLineRepository.findByDispenseOrder(any(DispenseOrder.class))).thenReturn(List.of(skipped, billable));

        List<BillableDispenseItemResponse> items = dispenseOrderService.getBillableItems(orderId);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getDispenseLineId()).isEqualTo(billable.getId());
        assertThat(items.get(0).getQuantityDispensed()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(items.get(0).getLineStatus()).isEqualTo(DispenseLine.Status.FILLED_WITH_STOCK_OVERRIDE);
        assertThat(items.get(0).getOverrideReasonCode()).isEqualTo("Shelf count disagreed with ledger");
    }

    @Test
    void getBillableItems_orderMissing_throws() {
        UUID orderId = UUID.randomUUID();
        when(dispenseOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispenseOrderService.getBillableItems(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispense order not found");
    }
}
