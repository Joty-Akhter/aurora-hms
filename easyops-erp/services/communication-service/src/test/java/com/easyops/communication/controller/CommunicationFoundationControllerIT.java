package com.easyops.communication.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CommunicationFoundationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPhase0Foundation_returnsExpectedContract() throws Exception {
        mockMvc.perform(get("/api/communications/foundation/phase-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("phase-0"))
                .andExpect(jsonPath("$.status").value("implemented"))
                .andExpect(jsonPath("$.brokerStrategy").value("Kafka (consumer retry + DLQ)"))
                .andExpect(jsonPath("$.serviceBoundary")
                        .value("New standalone communication-service with legacy notification-service bridge"))
                .andExpect(jsonPath("$.templateEngine")
                        .value("Mustache-compatible templates with strict variable schema validation"))
                .andExpect(jsonPath("$.adrs[0]").value("ADR-001"))
                .andExpect(jsonPath("$.adrs[1]").value("ADR-002"))
                .andExpect(jsonPath("$.adrs[2]").value("ADR-003"))
                .andExpect(jsonPath("$.requiredEnvelopeFields.length()").value(10))
                .andExpect(jsonPath("$.requiredEnvelopeFields[0]").value("eventId"))
                .andExpect(jsonPath("$.requiredEnvelopeFields[9]").value("payload"))
                .andExpect(jsonPath("$.conventions.eventTypePattern").value("<domain>.<action>.v<major>"))
                .andExpect(jsonPath("$.v1UseCases.length()").value(3))
                .andExpect(jsonPath("$.acceptanceCriteria.length()").value(4))
                .andExpect(jsonPath("$.backlogSnapshot.length()").value(6))
                .andExpect(jsonPath("$.timeline").value("Indicative v1 timeline: 8-10 weeks"));
    }
}
