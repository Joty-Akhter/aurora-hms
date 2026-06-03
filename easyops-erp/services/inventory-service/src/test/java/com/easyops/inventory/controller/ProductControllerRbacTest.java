package com.easyops.inventory.controller;

import com.easyops.inventory.entity.Product;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2 — inventory pilot: forbidden paths return {@link HttpStatus#FORBIDDEN} before downstream
 * services run where RBAC is ordered first. Uses {@link InventoryRbacService} with a stub
 * {@link RbacPermissionClient}.
 */
class ProductControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getAllProducts_forbiddenWhenViewDenied() {
        InventoryRbacService rbac = new InventoryRbacService(new DenyingRbacClient("inventory_view"));
        ProductController controller = new ProductController(null, rbac);

        assertThatThrownBy(() -> controller.getAllProducts(USER_HEADER, ORG, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createProduct_forbiddenWhenManageDenied() {
        InventoryRbacService rbac = new InventoryRbacService(new DenyingRbacClient("inventory_manage"));
        ProductController controller = new ProductController(null, rbac);

        Product body = new Product();
        body.setOrganizationId(ORG);
        body.setSku("SKU-RBAC");
        body.setName("RBAC test product");

        assertThatThrownBy(() -> controller.createProduct(USER_HEADER, body))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "inventory-test");
            this.denyMetric = denyMetric;
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            if (denyMetric.equals(metricOperation)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
            }
        }
    }
}
