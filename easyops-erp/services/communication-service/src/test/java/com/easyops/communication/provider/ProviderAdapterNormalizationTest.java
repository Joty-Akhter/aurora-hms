package com.easyops.communication.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderAdapterNormalizationTest {

    @Test
    void ponditSmsProvider_normalizesSuccessResultModel() {
        PonditSmsProvider provider = new PonditSmsProvider();
        ProviderDispatchResult result = provider.send(new ProviderDispatchRequest("+8801700000000", null, "Hello"));
        assertThat(result.channel()).isEqualTo("SMS");
        assertThat(result.providerName()).isEqualTo("pondit");
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.providerReference()).startsWith("sms-");
    }

    @Test
    void smtpEmailProvider_normalizesSuccessResultModel() {
        SmtpEmailProvider provider = new SmtpEmailProvider();
        ProviderDispatchResult result = provider.send(new ProviderDispatchRequest("john@example.com", "Subject", "Body"));
        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.providerName()).isEqualTo("smtp");
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.providerReference()).startsWith("email-");
    }

    @Test
    void adapters_classifyTransientAndPermanentFailuresByExceptionType() {
        PonditSmsProvider smsProvider = new PonditSmsProvider();
        SmtpEmailProvider emailProvider = new SmtpEmailProvider();

        assertThatThrownBy(() -> smsProvider.send(new ProviderDispatchRequest("transient-number", null, "Body")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transient");
        assertThatThrownBy(() -> emailProvider.send(new ProviderDispatchRequest("permanent@example.com", "S", "B")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Permanent");
    }
}
