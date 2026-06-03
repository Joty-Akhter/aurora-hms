package com.easyops.communication.service;

import com.easyops.communication.provider.CommunicationProvider;
import com.easyops.communication.provider.ProviderHealthStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProviderRouterService {

    private final List<CommunicationProvider> providers;

    public ProviderRouterService(List<CommunicationProvider> providers) {
        this.providers = providers;
    }

    public CommunicationProvider resolveByChannel(String channel) {
        return providers.stream()
                .filter(provider -> provider.channel().equalsIgnoreCase(channel))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No provider configured for channel: " + channel
                ));
    }

    public List<ProviderHealthStatus> healthStatuses() {
        return providers.stream().map(CommunicationProvider::health).toList();
    }
}
