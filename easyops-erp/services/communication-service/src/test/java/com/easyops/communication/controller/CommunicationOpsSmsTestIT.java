package com.easyops.communication.controller;

import com.easyops.rbac.client.RbacPermissionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CommunicationOpsSmsTestIT {
    private static final String USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String ORG_ID = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RbacPermissionClient rbacPermissionClient;

    @BeforeEach
    void setUp() {
        doNothing().when(rbacPermissionClient)
                .requireAnyResourceAction(any(UUID.class), any(UUID.class), any(String[][].class), anyString());
    }

    @Test
    void testSmsEndpoint_dispatchesAndReturnsProviderReference() throws Exception {
        mockMvc.perform(post("/api/communications/operations/sms/test")
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\":\"01735694199\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("This is a test SMS at ")))
                .andExpect(jsonPath("$.channel").value("SMS"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.providerReference").exists());
    }
}

