package com.easyops.communication.provider;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SmtpEmailProvider implements CommunicationProvider {

    @Override
    public String providerName() {
        return "smtp";
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public ProviderDispatchResult send(ProviderDispatchRequest request) {
        if (request.recipient().contains("transient")) {
            throw new IllegalStateException("Transient provider timeout");
        }
        if (request.recipient().contains("permanent")) {
            throw new IllegalArgumentException("Permanent recipient validation failure");
        }
        return new ProviderDispatchResult(
                channel(),
                providerName(),
                "ACCEPTED",
                "email-" + UUID.randomUUID()
        );
    }

    @Override
    public ProviderHealthStatus health() {
        return new ProviderHealthStatus(providerName(), channel(), "UP", "SMTP adapter configured");
    }
}
