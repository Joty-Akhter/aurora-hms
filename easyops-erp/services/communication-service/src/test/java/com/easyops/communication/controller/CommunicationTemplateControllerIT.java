package com.easyops.communication.controller;

import com.easyops.rbac.client.RbacPermissionClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CommunicationTemplateControllerIT {

    private static final String USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String ORG_ID = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RbacPermissionClient rbacPermissionClient;

    @BeforeEach
    void setUp() {
        doNothing().when(rbacPermissionClient)
                .requireAnyResourceAction(any(UUID.class), any(UUID.class), any(String[][].class), anyString());
    }

    @Test
    void templateCrud_happyPath() throws Exception {
        String createRequest = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":null,
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"patientName\\":\\"string\\"}"
                }
                """;

        String createBody = mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.templateKey").value("appointment.confirmation"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createBody);
        String id = created.get("id").asText();

        mockMvc.perform(get("/api/communication-templates/{id}", id)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("SMS"));

        mockMvc.perform(get("/api/communication-templates")
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        String updateRequest = """
                {
                  "status":"ARCHIVED",
                  "bodyTemplate":"Updated body"
                }
                """;

        mockMvc.perform(patch("/api/communication-templates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(delete("/api/communication-templates/{id}", id)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void createTemplate_forbiddenWhenManageDenied() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission"))
                .when(rbacPermissionClient)
                .requireAnyResourceAction(any(UUID.class), any(UUID.class), any(String[][].class), anyString());

        String createRequest = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"patientName\\":\\"string\\"}"
                }
                """;

        mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTemplate_returnsConflictWhenUniqueTupleWouldDuplicate() throws Exception {
        String first = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"patientName\\":\\"string\\"}"
                }
                """;
        String second = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":2,
                  "status":"DRAFT",
                  "bodyTemplate":"Reminder {{patientName}}",
                  "variablesSchema":"{\\"patientName\\":\\"string\\"}"
                }
                """;

        String firstId = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(first))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String secondId = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(second))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String conflictingUpdate = """
                {
                  "version":1
                }
                """;

        mockMvc.perform(patch("/api/communication-templates/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(conflictingUpdate))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/communication-templates/{id}", firstId)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/communication-templates/{id}", secondId)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void previewAndTestSendAndProviderHealth_happyPath() throws Exception {
        String createRequest = """
                {
                  "templateKey":"invoice.created",
                  "channel":"EMAIL",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":"Invoice {{invoiceNo}}",
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\",\\"invoiceNo\\"]}"
                }
                """;

        String id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String previewRequest = """
                {
                  "templateId":"%s",
                  "variables":{"patientName":"John Doe","invoiceNo":"INV-1001"}
                }
                """.formatted(id);

        mockMvc.perform(post("/api/communication-templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(previewRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renderedSubject").value("Invoice INV-1001"))
                .andExpect(jsonPath("$.renderedBody").value("Hello John Doe"));

        String sendRequest = """
                {
                  "templateId":"%s",
                  "recipient":"john@example.com",
                  "variables":{"patientName":"John Doe","invoiceNo":"INV-1001"}
                }
                """.formatted(id);

        mockMvc.perform(post("/api/communication-templates/test-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(sendRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.provider").value("smtp"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/communication-templates/providers/health")
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").exists())
                .andExpect(jsonPath("$[0].status").value("UP"));
    }

    @Test
    void preview_rejectsUnexpectedVariable() throws Exception {
        String createRequest = """
                {
                  "templateKey":"appointment.reminder",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":null,
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;
        String id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String previewRequest = """
                {
                  "templateId":"%s",
                  "variables":{"patientName":"John Doe","extra":"boom"}
                }
                """.formatted(id);

        mockMvc.perform(post("/api/communication-templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(previewRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unexpected variable supplied: extra"));
    }

    @Test
    void activeVersionUpdate_archivesOtherActiveVersion() throws Exception {
        String activeV1 = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":null,
                  "bodyTemplate":"V1 {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;
        String draftV2 = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":2,
                  "status":"DRAFT",
                  "subjectTemplate":null,
                  "bodyTemplate":"V2 {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;

        String v1Id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(activeV1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String v2Id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(draftV2))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/communication-templates/{id}", v2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activatedAt").exists());

        mockMvc.perform(get("/api/communication-templates/{id}", v1Id)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void createTemplate_rejectsEmailWithoutSubject() throws Exception {
        String createRequest = """
                {
                  "templateKey":"invoice.created",
                  "channel":"EMAIL",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":"",
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;

        mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("EMAIL template requires subjectTemplate"));
    }

    @Test
    void createTemplate_rejectsSmsWithSubject() throws Exception {
        String createRequest = """
                {
                  "templateKey":"appointment.confirmation",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":"Should not exist",
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;

        mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SMS template cannot define subjectTemplate"));
    }

    @Test
    void preview_rejectsMissingRequiredVariable() throws Exception {
        String createRequest = """
                {
                  "templateKey":"appointment.reminder.required",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":null,
                  "bodyTemplate":"Hello {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\",\\"appointmentDate\\"]}"
                }
                """;
        String id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String previewRequest = """
                {
                  "templateId":"%s",
                  "variables":{"patientName":"John Doe"}
                }
                """.formatted(id);

        mockMvc.perform(post("/api/communication-templates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(previewRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required variable: appointmentDate"));
    }

    @Test
    void createTemplate_rejectsUndeclaredPlaceholderVariable() throws Exception {
        String createRequest = """
                {
                  "templateKey":"appointment.invalid.placeholder",
                  "channel":"SMS",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":null,
                  "bodyTemplate":"Hello {{patientName}} {{doctorName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\"]}"
                }
                """;

        mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Placeholder variable is not declared in variablesSchema: doctorName"));
    }

    @Test
    void activeVersion_canBeRolledBackToPreviousVersion() throws Exception {
        String v1Active = """
                {
                  "templateKey":"invoice.lifecycle",
                  "channel":"EMAIL",
                  "locale":"en",
                  "version":1,
                  "status":"ACTIVE",
                  "subjectTemplate":"Invoice {{invoiceNo}}",
                  "bodyTemplate":"V1 {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\",\\"invoiceNo\\"]}"
                }
                """;
        String v2Draft = """
                {
                  "templateKey":"invoice.lifecycle",
                  "channel":"EMAIL",
                  "locale":"en",
                  "version":2,
                  "status":"DRAFT",
                  "subjectTemplate":"Invoice {{invoiceNo}}",
                  "bodyTemplate":"V2 {{patientName}}",
                  "variablesSchema":"{\\"required\\":[\\"patientName\\",\\"invoiceNo\\"]}"
                }
                """;

        String v1Id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(v1Active))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String v2Id = objectMapper.readTree(mockMvc.perform(post("/api/communication-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content(v2Draft))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/communication-templates/{id}", v2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/communication-templates/{id}", v1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activatedAt").exists());

        mockMvc.perform(get("/api/communication-templates/{id}", v2Id)
                        .header("X-User-Id", USER_ID)
                        .header("X-Organization-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
