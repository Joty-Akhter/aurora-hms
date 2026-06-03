package com.easyops.ar.config;

import com.easyops.accountingperiod.AccountingPeriodClient;
import com.easyops.accountingperiod.AccountingPeriodResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AccountingPeriodClientConfiguration {

    @Bean
    public AccountingPeriodResolver accountingPeriodResolver(
            RestTemplate restTemplate,
            @Value("${services.accounting.base-url:http://localhost:8088}") String accountingBaseUrl) {
        return new AccountingPeriodClient(restTemplate, accountingBaseUrl);
    }
}
