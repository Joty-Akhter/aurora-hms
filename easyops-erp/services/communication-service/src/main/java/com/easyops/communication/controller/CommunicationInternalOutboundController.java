package com.easyops.communication.controller;

import com.easyops.communication.dto.InternalOutboundDispatchResponse;
import com.easyops.communication.dto.InternalOutboundEmailRequest;
import com.easyops.communication.dto.InternalOutboundSmsRequest;
import com.easyops.communication.provider.ProviderDispatchResult;
import com.easyops.communication.service.CommunicationOpsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/communications/internal/outbound")
@Tag(name = "Internal outbound")
public class CommunicationInternalOutboundController {

    private final CommunicationOpsService opsService;
    private final String expectedServiceKey;

    public CommunicationInternalOutboundController(
            CommunicationOpsService opsService,
            @Value("${communication.internal.service-key:}") String expectedServiceKey
    ) {
        this.opsService = opsService;
        this.expectedServiceKey = expectedServiceKey != null ? expectedServiceKey.trim() : "";
    }

    @PostMapping("/sms")
    public ResponseEntity<InternalOutboundDispatchResponse> sms(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody InternalOutboundSmsRequest body
    ) {
        assertServiceKey(serviceKey);
        ProviderDispatchResult result = opsService.sendTransactionalSms(body.recipient(), body.message());
        return ResponseEntity.accepted().body(toResponse(result));
    }

    @PostMapping("/email")
    public ResponseEntity<InternalOutboundDispatchResponse> email(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody InternalOutboundEmailRequest body
    ) {
        assertServiceKey(serviceKey);
        ProviderDispatchResult result = opsService.sendTransactionalEmail(body.recipient(), body.subject(), body.body());
        return ResponseEntity.accepted().body(toResponse(result));
    }

    private void assertServiceKey(String serviceKey) {
        if (!StringUtils.hasText(expectedServiceKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Internal outbound is disabled (communication.internal.service-key not configured)");
        }
        String presented = serviceKey != null ? serviceKey.trim() : "";
        if (!expectedServiceKey.equals(presented)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service key");
        }
    }

    private static InternalOutboundDispatchResponse toResponse(ProviderDispatchResult result) {
        return new InternalOutboundDispatchResponse(
                result.channel(),
                result.providerName(),
                result.status(),
                result.providerReference()
        );
    }
}
