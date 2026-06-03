package com.easyops.communication.service;

import com.easyops.communication.provider.PonditSmsProvider;
import com.easyops.communication.provider.SmtpEmailProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderRouterServiceTest {

    @Test
    void resolveByChannel_returnsMatchingProvider() {
        ProviderRouterService router = new ProviderRouterService(List.of(new PonditSmsProvider(), new SmtpEmailProvider()));
        assertThat(router.resolveByChannel("SMS").providerName()).isEqualTo("pondit");
        assertThat(router.resolveByChannel("EMAIL").providerName()).isEqualTo("smtp");
    }

    @Test
    void resolveByChannel_throwsWhenUnknownChannel() {
        ProviderRouterService router = new ProviderRouterService(List.of(new PonditSmsProvider()));
        assertThatThrownBy(() -> router.resolveByChannel("WHATSAPP"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No provider configured for channel");
    }
}
