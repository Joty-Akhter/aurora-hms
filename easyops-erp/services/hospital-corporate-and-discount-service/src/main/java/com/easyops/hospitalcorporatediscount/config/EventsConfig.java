package com.easyops.hospitalcorporatediscount.config;

import com.easyops.hospitalcorporatediscount.events.CorporateDiscountEventPublisher;
import com.easyops.hospitalcorporatediscount.events.LoggingCorporateDiscountEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventsConfig {

    @Bean
    @ConditionalOnMissingBean(CorporateDiscountEventPublisher.class)
    public CorporateDiscountEventPublisher corporateDiscountEventPublisher() {
        return new LoggingCorporateDiscountEventPublisher();
    }
}
