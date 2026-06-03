package com.easyops.accountingperiod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountingPeriodClientTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID PERIOD_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");
    private static final LocalDate DATE = LocalDate.of(2026, 5, 15);

    private MockRestServiceServer server;
    private AccountingPeriodClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        server = MockRestServiceServer.createServer(restTemplate);
        client = new AccountingPeriodClient(restTemplate, "http://accounting.test");
    }

    @Test
    void resolvePeriodId_returnsPeriodFromAccountingService() {
        String json = "{\"id\":\"" + PERIOD_ID + "\"}";
        server.expect(requestTo("http://accounting.test/api/accounting/fiscal-years/organization/"
                        + ORG_ID + "/periods/for-date?date=" + DATE))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", ACTOR_ID.toString()))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        UUID resolved = client.resolvePeriodId(ORG_ID, DATE, ACTOR_ID);

        assertThat(resolved).isEqualTo(PERIOD_ID);
        server.verify();
    }

    @Test
    void resolvePeriodId_throwsWhenPeriodNotFound() {
        server.expect(requestTo("http://accounting.test/api/accounting/fiscal-years/organization/"
                        + ORG_ID + "/periods/for-date?date=" + DATE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.resolvePeriodId(ORG_ID, DATE, ACTOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No accounting period exists");
    }
}
