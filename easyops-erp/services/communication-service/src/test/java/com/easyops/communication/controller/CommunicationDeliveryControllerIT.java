package com.easyops.communication.controller;

import com.easyops.communication.entity.CommunicationTemplate;
import com.easyops.communication.repository.CommunicationTemplateRepository;
import com.easyops.rbac.client.RbacPermissionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CommunicationDeliveryControllerIT {

    private static final String USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String ORG_ID = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunicationTemplateRepository templateRepository;

    @MockBean
    private RbacPermissionClient rbacPermissionClient;

    @BeforeEach
    void setUp() {
        doNothing().when(rbacPermissionClient)
                .requireAnyResourceAction(any(UUID.class), any(UUID.class), any(String[][].class), anyString());
        templateRepository.deleteAll();
        templateRepository.save(template("appointment.lifecycle", "SMS", "en"));
    }

    @Test
    void queryWithoutFilters_returnsRecentDeliveries() throws Exception {
        String body = """
                {
                  "eventId":"evt-list-all-1",
                  "eventType":"appointment.created.v1",
                  "eventVersion":"v1",
                  "occurredAt":"2026-05-10T10:00:00Z",
                  "organizationId":"org-1",
                  "entityId":"appt-list-1",
                  "actorId":"user-1",
                  "correlationId":"corr-list-all-1",
                  "payload":{"patientName":"John","appointmentDate":"2026-05-10T10:00:00Z","recipientPhone":"+8801700009999"}
                }
                """;
        mockMvc.perform(post("/api/communications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(body))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/communications/deliveries")
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value("evt-list-all-1"));
    }

    @Test
    void queryByStatusAndManualResend_happyPath() throws Exception {
        String body = """
                {
                  "eventId":"evt-phase6-1",
                  "eventType":"appointment.created.v1",
                  "eventVersion":"v1",
                  "occurredAt":"2026-05-10T10:00:00Z",
                  "organizationId":"org-1",
                  "entityId":"appt-1",
                  "actorId":"user-1",
                  "correlationId":"corr-phase6-1",
                  "payload":{"patientName":"John","appointmentDate":"2026-05-10T10:00:00Z","recipientPhone":"+8801700009999"}
                }
                """;
        String created = mockMvc.perform(post("/api/communications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/communications/deliveries")
                        .param("status", "QUEUED")
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("QUEUED"));

        mockMvc.perform(post("/api/communications/deliveries/{id}/resend", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content("{\"reason\":\"manual retry from operations\"}"))
                .andExpect(status().isBadRequest());

        String skippedBody = """
                {
                  "eventId":"evt-phase6-2",
                  "eventType":"appointment.created.v1",
                  "eventVersion":"v1",
                  "occurredAt":"2026-05-10T10:00:00Z",
                  "organizationId":"org-1",
                  "entityId":"appt-2",
                  "actorId":"user-1",
                  "correlationId":"corr-phase6-2",
                  "payload":{"patientName":"John","appointmentDate":"2026-05-10T10:00:00Z"}
                }
                """;
        String skipped = mockMvc.perform(post("/api/communications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(skippedBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String skippedId = objectMapper.readTree(skipped).get("id").asText();

        mockMvc.perform(post("/api/communications/deliveries/{id}/resend", skippedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content("{\"reason\":\"recipient fixed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    private CommunicationTemplate template(String key, String channel, String locale) {
        CommunicationTemplate t = new CommunicationTemplate();
        t.setTemplateKey(key);
        t.setChannel(channel);
        t.setLocale(locale);
        t.setVersion(1);
        t.setStatus("ACTIVE");
        t.setBodyTemplate("Hello {{patientName}}");
        t.setVariablesSchema("{\"required\":[\"patientName\"]}");
        t.setCreatedBy("test");
        return t;
    }
}
