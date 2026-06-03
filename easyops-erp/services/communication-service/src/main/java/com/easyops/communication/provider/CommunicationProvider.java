package com.easyops.communication.provider;

public interface CommunicationProvider {
    String providerName();

    String channel();

    ProviderDispatchResult send(ProviderDispatchRequest request);

    ProviderHealthStatus health();
}
