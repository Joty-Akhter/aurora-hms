package com.easyops.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * PDMP outbound calls use {@code pdmp.integration.timeout} for connect + read so hung
     * state gateways do not block threads indefinitely (M3 / FR-P7.4).
     */
    @Bean
    @Qualifier("pdmpRestTemplate")
    public RestTemplate pdmpRestTemplate(@Value("${pdmp.integration.timeout:30000}") int pdmpTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(pdmpTimeoutMs, 120_000));
        factory.setReadTimeout(pdmpTimeoutMs);
        return new RestTemplate(factory);
    }
}
