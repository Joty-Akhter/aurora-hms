package com.easyops.pharma.controller;

import com.easyops.pharma.config.PharmaExceptionHandler;
import com.easyops.pharma.dto.TerritoryIncentiveRuleRequest;
import com.easyops.pharma.dto.TerritoryIncentiveRuleResponse;
import com.easyops.pharma.exception.IncentiveRuleValidationException;
import com.easyops.pharma.entity.Territory;
import com.easyops.pharma.entity.TerritoryIncentiveRule;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.service.IncentiveRuleService;
import com.easyops.pharma.service.TerritoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 6: Integration tests for IncentiveRuleController API.
 * Disabled on Java 25+ due to Mockito/Byte Buddy compatibility; run on Java 17/21.
 */
@WebMvcTest(IncentiveRuleController.class)
@Import(PharmaExceptionHandler.class)
@Disabled("Mockito/Byte Buddy incompatible with Java 25 - enable when using Java 17/21")
class IncentiveRuleControllerTest {

    private static final String X_USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncentiveRuleService incentiveRuleService;

    @MockBean
    private TerritoryService territoryService;

    @MockBean
    private PharmaRbacService pharmaRbac;

    private final UUID territoryId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID ruleId = UUID.randomUUID();

    @BeforeEach
    void stubRbacAndTerritory() {
        doNothing().when(pharmaRbac).requirePharmaView(any(UUID.class), any(UUID.class));
        doNothing().when(pharmaRbac).requirePharmaManage(any(UUID.class), any(UUID.class));
        Territory territory = new Territory();
        territory.setOrganizationId(orgId);
        when(territoryService.getTerritoryById(territoryId)).thenReturn(territory);
    }

    @Test
    void getIncentiveRuleForTerritory_returns200WhenRuleExists() throws Exception {
        TerritoryIncentiveRuleResponse response = new TerritoryIncentiveRuleResponse();
        response.setId(ruleId);
        response.setTerritoryId(territoryId);
        response.setOrganizationId(orgId);
        response.setSrSharePercentage(new BigDecimal("0.09"));
        response.setDevelopmentFundPercentage(new BigDecimal("0.01"));
        response.setHasDedicatedSr(true);

        when(incentiveRuleService.getRuleResponseForTerritory(territoryId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/pharma/incentive-rules/territory/{territoryId}", territoryId)
                        .header("X-User-Id", X_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.territoryId").value(territoryId.toString()))
                .andExpect(jsonPath("$.srSharePercentage").value(0.09))
                .andExpect(jsonPath("$.hasDedicatedSr").value(true));
    }

    @Test
    void getIncentiveRuleForTerritory_returns404WhenNoRule() throws Exception {
        when(incentiveRuleService.getRuleResponseForTerritory(territoryId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pharma/incentive-rules/territory/{territoryId}", territoryId)
                        .header("X-User-Id", X_USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRule_returns201WithValidRequest() throws Exception {
        TerritoryIncentiveRuleRequest request = new TerritoryIncentiveRuleRequest();
        request.setOrganizationId(orgId);
        request.setTerritoryId(territoryId);
        request.setSrSharePercentage(new BigDecimal("0.09"));
        request.setDevelopmentFundPercentage(new BigDecimal("0.01"));
        request.setHasDedicatedSr(true);
        request.setAllocations(List.of(
                new TerritoryIncentiveRuleRequest.AllocationItem(UUID.randomUUID(), "MPO", new BigDecimal("100"))
        ));

        TerritoryIncentiveRuleResponse response = new TerritoryIncentiveRuleResponse();
        response.setId(ruleId);
        response.setTerritoryId(territoryId);
        response.setOrganizationId(orgId);

        when(incentiveRuleService.validateAndSaveRuleFromRequest(any())).thenReturn(response);

        mockMvc.perform(post("/api/pharma/incentive-rules")
                        .header("X-User-Id", X_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.territoryId").value(territoryId.toString()));
    }

    @Test
    void createRule_returns400OnValidationError() throws Exception {
        TerritoryIncentiveRuleRequest request = new TerritoryIncentiveRuleRequest();
        request.setOrganizationId(orgId);
        request.setTerritoryId(territoryId);
        request.setHasDedicatedSr(false);
        request.setAllocations(List.of(
                new TerritoryIncentiveRuleRequest.AllocationItem(UUID.randomUUID(), "MPO", new BigDecimal("50"))
        ));

        when(incentiveRuleService.validateAndSaveRuleFromRequest(any()))
                .thenThrow(new IncentiveRuleValidationException("Sum of Manager and MPO allocation percentages must equal 100%"));

        mockMvc.perform(post("/api/pharma/incentive-rules")
                        .header("X-User-Id", X_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllocationsForTerritory_returns200() throws Exception {
        when(incentiveRuleService.getAllocationsForTerritory(territoryId))
                .thenReturn(List.of(
                        new TerritoryIncentiveRuleResponse.AllocationItem(UUID.randomUUID(), UUID.randomUUID(), "MPO", new BigDecimal("60")),
                        new TerritoryIncentiveRuleResponse.AllocationItem(UUID.randomUUID(), UUID.randomUUID(), "AM", new BigDecimal("40"))
                ));

        mockMvc.perform(get("/api/pharma/incentive-rules/territory/{territoryId}/allocations", territoryId)
                        .header("X-User-Id", X_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deactivateRule_returns204() throws Exception {
        TerritoryIncentiveRule rule = new TerritoryIncentiveRule();
        rule.setOrganizationId(orgId);
        when(incentiveRuleService.findRuleById(ruleId)).thenReturn(Optional.of(rule));

        mockMvc.perform(delete("/api/pharma/incentive-rules/{id}", ruleId)
                        .header("X-User-Id", X_USER_ID))
                .andExpect(status().isNoContent());
    }
}
