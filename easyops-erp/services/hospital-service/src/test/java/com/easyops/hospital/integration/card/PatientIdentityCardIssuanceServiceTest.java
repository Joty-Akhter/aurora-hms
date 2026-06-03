package com.easyops.hospital.integration.card;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientIdentityCardIssuanceServiceTest {

    @Test
    void issueOrResolveForNewPatient_returnsExistingCardWhenAlreadyPresent() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        PatientIdentityCardIssuanceService service =
                new PatientIdentityCardIssuanceService(restTemplate);

        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "autoIssueOnRegistration", true);
        ReflectionTestUtils.setField(service, "cardProductId", UUID.fromString("a0000001-0001-4000-8000-000000000001"));
        ReflectionTestUtils.setField(service, "internalServiceKey", "test-internal-key");

        UUID patientId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        PatientIdentityCardIssuanceService.CardSummary existing = new PatientIdentityCardIssuanceService.CardSummary();
        existing.setId(UUID.randomUUID());
        existing.setCardNumber("HOSP-2026-000001");

        PatientIdentityCardIssuanceService.PagedCardsResponse page = new PatientIdentityCardIssuanceService.PagedCardsResponse();
        page.setContent(List.of(existing));

        when(restTemplate.exchange(any(String.class), any(), any(), eq(PatientIdentityCardIssuanceService.PagedCardsResponse.class)))
                .thenReturn(ResponseEntity.ok(page));

        PatientIdentityCardIssuanceResult result = service.issueOrResolveForNewPatient(patientId, actor, "HOSP-2026-000001", null);

        assertEquals("ISSUED", result.getStatus());
        assertEquals(existing.getId(), result.getCardId());
        assertEquals(existing.getCardNumber(), result.getCardNumber());
        verify(restTemplate).exchange(any(String.class), any(), any(), eq(PatientIdentityCardIssuanceService.PagedCardsResponse.class));
    }

    @Test
    void resolveExisting_prefersActiveCardOverIssuedCard() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        PatientIdentityCardIssuanceService service =
                new PatientIdentityCardIssuanceService(restTemplate);

        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cardProductId", UUID.fromString("a0000001-0001-4000-8000-000000000001"));
        ReflectionTestUtils.setField(service, "internalServiceKey", "test-internal-key");

        UUID patientId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        PatientIdentityCardIssuanceService.CardSummary active = new PatientIdentityCardIssuanceService.CardSummary();
        active.setId(UUID.randomUUID());
        active.setCardNumber("ACTIVE-001");
        PatientIdentityCardIssuanceService.PagedCardsResponse activePage = new PatientIdentityCardIssuanceService.PagedCardsResponse();
        activePage.setContent(List.of(active));

        when(restTemplate.exchange(any(String.class), any(), any(), eq(PatientIdentityCardIssuanceService.PagedCardsResponse.class)))
                .thenReturn(ResponseEntity.ok(activePage));

        PatientIdentityCardIssuanceResult result = service.resolveExistingForPatient(patientId, actor, null);

        assertEquals("ISSUED", result.getStatus());
        assertEquals("ACTIVE-001", result.getCardNumber());
    }
}
