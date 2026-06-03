package com.easyops.rbac.client;

import com.easyops.rbac.client.dto.RbacPermissionDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RbacPermissionClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private RbacPermissionClient client;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RbacPermissionClient(restTemplate, meterRegistry, "http://localhost:8084", "test-service");
    }

    @Test
    void hasAnyResourceAction_trueWhenMatch() {
        String url = "http://localhost:8084/api/rbac/authorization/users/" + userId + "/permissions?organizationId=" + orgId;
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(
                        "[{\"resource\":\"organizations\",\"action\":\"view\",\"isActive\":true}]",
                        MediaType.APPLICATION_JSON));
        client.requireAnyResourceAction(userId, orgId, new String[][]{{"organizations", "view"}}, "op");
        mockServer.verify();
    }

    @Test
    void requireAnyResourceAction_forbidsWhenNoMatch() {
        String url = "http://localhost:8084/api/rbac/authorization/users/" + userId + "/permissions?organizationId=" + orgId;
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.requireAnyResourceAction(userId, orgId, new String[][]{{"users", "manage"}}, "op"))
                .isInstanceOf(ResponseStatusException.class);
        mockServer.verify();
    }

    @Test
    void requireAuthenticatedUser_rejectsNull() {
        assertThatThrownBy(() -> client.requireAnyResourceAction(null, orgId, new String[][]{{"a", "b"}}, "op"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
