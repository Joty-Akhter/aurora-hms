package com.easyops.communication.controller;

import com.easyops.communication.dto.OpsAlertStatusResponse;
import com.easyops.communication.dto.ProviderSecretStatusResponse;
import com.easyops.communication.dto.TestSmsSendRequest;
import com.easyops.communication.dto.TestSmsSendResponse;
import com.easyops.communication.security.CommunicationTemplateRbacService;
import com.easyops.communication.security.RbacRequestHeaders;
import com.easyops.communication.service.CommunicationOpsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/communications/operations")
@Tag(name = "Communication Operations")
public class CommunicationOpsController {

    private final CommunicationOpsService opsService;
    private final CommunicationTemplateRbacService rbacService;
    private final boolean bypassRbac;

    public CommunicationOpsController(
            CommunicationOpsService opsService,
            CommunicationTemplateRbacService rbacService,
            @Value("${communication.ops.bypass-rbac:false}") boolean bypassRbac
    ) {
        this.opsService = opsService;
        this.rbacService = rbacService;
        this.bypassRbac = bypassRbac;
    }

    @GetMapping("/alerts")
    public OpsAlertStatusResponse alerts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireOperationsRead(actor, organizationId);
        }
        return opsService.evaluateAlerts();
    }

    @GetMapping("/secrets/status")
    public ProviderSecretStatusResponse secretsStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireOperationsRead(actor, organizationId);
        }
        return opsService.providerSecretStatus();
    }

    @PostMapping("/sms/test")
    public TestSmsSendResponse testSms(
            @Valid @RequestBody TestSmsSendRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireOperationsManage(actor, organizationId);
        }
        return opsService.sendTestSms(request.recipient());
    }
}
