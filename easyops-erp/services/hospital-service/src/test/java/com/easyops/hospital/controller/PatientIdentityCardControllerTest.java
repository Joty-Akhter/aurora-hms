package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.PatientIdentityCardReplaceRequest;
import com.easyops.hospital.dto.response.PatientIdentityCardActionResponse;
import com.easyops.hospital.dto.response.PatientIdentityCardPrintResponse;
import com.easyops.hospital.service.PatientService;
import com.easyops.hospital.service.RbacPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
class PatientIdentityCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @MockBean
    private RbacPermissionService rbacPermissionService;

    @Test
    void printPreview_returnsPayload() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        doNothing().when(rbacPermissionService).requireHospitalManage(eq(userId), eq(orgId));
        when(patientService.getIdentityCardPrintPreview(eq(patientId), eq(userId)))
                .thenReturn(PatientIdentityCardPrintResponse.builder()
                        .patientId(patientId)
                        .mrn("HOSP-2026-000001")
                        .cardId(UUID.randomUUID())
                        .cardNumber("HOSP-2026-000001")
                        .title("Patient identity card")
                        .html("<html></html>")
                        .action("PRINT")
                        .build());

        mockMvc.perform(get("/api/patients/{patientId}/identity-card/print-preview", patientId)
                        .header("X-User-Id", userId.toString())
                        .header("X-Organization-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrn").value("HOSP-2026-000001"))
                .andExpect(jsonPath("$.action").value("PRINT"));
    }

    @Test
    void replace_requiresReasonAndReturnsReplacement() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        doNothing().when(rbacPermissionService).requireHospitalManage(eq(userId), eq(orgId));
        when(patientService.replaceIdentityCard(eq(patientId), eq(userId), eq("LOST")))
                .thenReturn(PatientIdentityCardActionResponse.builder()
                        .patientId(patientId)
                        .cardId(UUID.randomUUID())
                        .cardNumber("CARD-NEW-001")
                        .status("REPLACED")
                        .action("REPLACE")
                        .reason("LOST")
                        .build());

        PatientIdentityCardReplaceRequest req = new PatientIdentityCardReplaceRequest();
        req.setReason("LOST");

        mockMvc.perform(post("/api/patients/{patientId}/identity-card/replace", patientId)
                        .header("X-User-Id", userId.toString())
                        .header("X-Organization-Id", orgId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLACED"))
                .andExpect(jsonPath("$.reason").value("LOST"));
    }

    @Test
    void reprint_returnsPayload() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        doNothing().when(rbacPermissionService).requireHospitalManage(eq(userId), eq(orgId));
        when(patientService.reprintIdentityCard(eq(patientId), eq(userId)))
                .thenReturn(PatientIdentityCardPrintResponse.builder()
                        .patientId(patientId)
                        .mrn("HOSP-2026-000001")
                        .cardId(UUID.randomUUID())
                        .cardNumber("HOSP-2026-000001")
                        .title("Patient identity card")
                        .html("<html></html>")
                        .action("REPRINT")
                        .build());

        mockMvc.perform(post("/api/patients/{patientId}/identity-card/reprint", patientId)
                        .header("X-User-Id", userId.toString())
                        .header("X-Organization-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrn").value("HOSP-2026-000001"))
                .andExpect(jsonPath("$.action").value("REPRINT"));
    }
}
