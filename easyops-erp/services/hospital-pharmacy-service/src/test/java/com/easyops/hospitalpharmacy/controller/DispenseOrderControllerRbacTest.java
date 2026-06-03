package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.CreateDispenseOrderRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseReturnRequest;
import com.easyops.hospitalpharmacy.dto.response.BillableDispenseItemResponse;
import com.easyops.hospitalpharmacy.dto.response.DispenseOrderResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.service.DispenseIdempotencyService;
import com.easyops.hospitalpharmacy.service.DispenseOrderService;
import com.easyops.rbac.client.RbacPermissionClient;
import com.easyops.rbac.client.dto.RbacPermissionDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 — dispense API RBAC (no Mockito; JDK 25 compatible with hospital-service pattern).
 */
class DispenseOrderControllerRbacTest {

    private static final UUID USER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String USER_HEADER = USER.toString();
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static RbacPermissionDto perm(String resource, String action) {
        RbacPermissionDto d = new RbacPermissionDto();
        d.setResource(resource);
        d.setAction(action);
        d.setIsActive(true);
        return d;
    }

    private static final List<RbacPermissionDto> PHARMACIST_DISPENSER_LIKE = List.of(
            perm("hospital.pharmacy", "dispense"),
            perm("hospital.prescription", "view"),
            perm("hospital", "view")
    );

    private static class FixedPermissionsRbacClient extends RbacPermissionClient {
        private final List<RbacPermissionDto> permissions;

        FixedPermissionsRbacClient(List<RbacPermissionDto> permissions) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-pharmacy-test");
            this.permissions = permissions;
        }

        @Override
        public List<RbacPermissionDto> fetchUserPermissions(UUID userId, UUID organizationId) {
            return permissions;
        }
    }

    /**
     * Avoids DB/JPA in tests; delegates to the supplier (same outcome as optional idempotency key absent).
     */
    private static class PassThroughIdempotencyService extends DispenseIdempotencyService {
        PassThroughIdempotencyService() {
            super(null, null);
        }

        @Override
        public DispenseOrderResponse executePostDispenseLines(UUID orderId, String idempotencyKey, Supplier<DispenseOrderResponse> action) {
            return action.get();
        }

        @Override
        public DispenseOrderResponse executePostReturns(UUID orderId, String idempotencyKey, Supplier<DispenseOrderResponse> action) {
            return action.get();
        }
    }

    private static class AlwaysDenyRbacClient extends RbacPermissionClient {
        AlwaysDenyRbacClient() {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-pharmacy-test");
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
        }
    }

    /**
     * Minimal stub: RBAC runs before service; overrides avoid touching repositories.
     */
    private static class StubDispenseOrderService extends DispenseOrderService {
        StubDispenseOrderService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public List<DispenseOrderResponse> search(String patientId, String visitId, UUID pharmacyLocationId, String status) {
            return List.of();
        }

        @Override
        public DispenseOrderResponse getById(UUID id) {
            return null;
        }

        @Override
        public DispenseOrderResponse createOrder(CreateDispenseOrderRequest request) {
            return null;
        }

        @Override
        public DispenseOrderResponse updateStatus(UUID orderId, String status, UUID actorUserId, UUID organizationId) {
            return null;
        }

        @Override
        public List<BillableDispenseItemResponse> getBillableItems(UUID orderId) {
            return Collections.emptyList();
        }

        @Override
        public DispenseOrderResponse addDispenseLines(
                UUID orderId, List<DispenseLineRequest> requests, UUID actorUserId, UUID organizationId) {
            return null;
        }

        @Override
        public DispenseOrderResponse recordReturns(UUID orderId, DispenseReturnRequest request, UUID actorUserId, UUID organizationId) {
            return null;
        }
    }

    @Test
    void searchDispenseOrders_forbiddenWhenRbacDenies() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.searchDispenseOrders(null, null, null, null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void searchDispenseOrders_okWhenPharmacistPermissions() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.searchDispenseOrders(null, null, null, null, USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void ePrescribingTransmitter_cannotReadDispenseQueue_withoutDispenseOrManage() {
        List<RbacPermissionDto> transmitterOnly = List.of(
                perm("hospital", "view"),
                perm("hospital.prescription", "view"),
                perm("hospital.prescription", "transmit")
        );
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(transmitterOnly)));

        assertThatThrownBy(() -> controller.searchDispenseOrders(null, null, null, null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void getBillableItems_okWhenPharmacistPermissions() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.getBillableItems(UUID.randomUUID(), USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void getBillableItems_forbiddenWhenRbacDenies() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.getBillableItems(UUID.randomUUID(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void getDispenseOrder_okWhenPharmacistPermissions() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.getDispenseOrder(UUID.randomUUID(), USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void getDispenseOrder_forbiddenWhenRbacDenies() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.getDispenseOrder(UUID.randomUUID(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void updateStatus_okWhenPharmacistPermissions() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.updateStatus(UUID.randomUUID(), "COMPLETED", USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void updateStatus_forbiddenWhenRbacDenies() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.updateStatus(UUID.randomUUID(), "COMPLETED", USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    private static DispenseReturnRequest sampleReturnRequest() {
        DispenseReturnRequest req = new DispenseReturnRequest();
        DispenseReturnRequest.Line line = new DispenseReturnRequest.Line();
        line.setDispenseLineId(UUID.randomUUID());
        line.setQuantityReturned(BigDecimal.ONE);
        req.setLines(List.of(line));
        return req;
    }

    @Test
    void addDispenseLines_okWhenPharmacistPermissionsWithoutStockOverride() {
        DispenseLineRequest line = new DispenseLineRequest();
        line.setDrugId(UUID.randomUUID());
        line.setQuantityDispensed(BigDecimal.ONE);

        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                new PassThroughIdempotencyService(),
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.addDispenseLines(UUID.randomUUID(), List.of(line), null, USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void recordReturns_okWhenPharmacistPermissions() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                new PassThroughIdempotencyService(),
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.recordReturns(UUID.randomUUID(), sampleReturnRequest(), null, USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void recordReturns_forbiddenWhenRbacDenies() {
        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.recordReturns(UUID.randomUUID(), sampleReturnRequest(), null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void addDispenseLines_stockOverrideReason_forbiddenWhenMissingStockOverridePermission() {
        DispenseLineRequest line = new DispenseLineRequest();
        line.setDrugId(UUID.randomUUID());
        line.setQuantityDispensed(BigDecimal.ONE);
        line.setStockOverrideReason("No matching stock row");

        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatThrownBy(() -> controller.addDispenseLines(UUID.randomUUID(), List.of(line), null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void addDispenseLines_stockOverrideReason_okWhenStockOverridePermission() {
        DispenseLineRequest line = new DispenseLineRequest();
        line.setDrugId(UUID.randomUUID());
        line.setQuantityDispensed(BigDecimal.ONE);
        line.setStockOverrideReason("Ledger mismatch");

        List<RbacPermissionDto> withStockOverride = List.of(
                perm("hospital.pharmacy", "dispense"),
                perm("hospital.pharmacy", "stock_override"),
                perm("hospital.prescription", "view"),
                perm("hospital", "view"));

        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                new PassThroughIdempotencyService(),
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(withStockOverride)));

        assertThatCode(() -> controller.addDispenseLines(UUID.randomUUID(), List.of(line), null, USER_HEADER, ORG))
                .doesNotThrowAnyException();
    }

    @Test
    void createDispenseOrder_okWhenPharmacistPermissions() {
        var body = new CreateDispenseOrderRequest();
        body.setPharmacyLocationId(UUID.randomUUID());
        body.setContextType("WALK_IN");

        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE)));

        assertThatCode(() -> controller.createDispenseOrder(body, USER_HEADER, ORG)).doesNotThrowAnyException();
    }

    @Test
    void createDispenseOrder_forbiddenWhenRbacDenies() {
        var body = new CreateDispenseOrderRequest();
        body.setPharmacyLocationId(UUID.randomUUID());
        body.setContextType("WALK_IN");

        DispenseOrderController controller = new DispenseOrderController(
                new StubDispenseOrderService(),
                null,
                null,
                new HospitalPharmacyRbacService(new AlwaysDenyRbacClient()));

        assertThatThrownBy(() -> controller.createDispenseOrder(body, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }
}
