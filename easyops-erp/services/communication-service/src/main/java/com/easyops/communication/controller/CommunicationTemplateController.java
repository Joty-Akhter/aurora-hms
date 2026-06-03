package com.easyops.communication.controller;

import com.easyops.communication.dto.CommunicationTemplateCreateRequest;
import com.easyops.communication.dto.ProviderHealthResponse;
import com.easyops.communication.dto.CommunicationTemplateResponse;
import com.easyops.communication.dto.CommunicationTemplateUpdateRequest;
import com.easyops.communication.dto.TemplatePreviewRequest;
import com.easyops.communication.dto.TemplatePreviewResponse;
import com.easyops.communication.dto.TemplateTestSendRequest;
import com.easyops.communication.dto.TemplateTestSendResponse;
import com.easyops.communication.provider.ProviderDispatchResult;
import com.easyops.communication.provider.ProviderHealthStatus;
import com.easyops.communication.security.CommunicationTemplateRbacService;
import com.easyops.communication.security.RbacRequestHeaders;
import com.easyops.communication.service.CommunicationTemplateService;
import com.easyops.communication.service.ProviderRouterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/communication-templates")
@Tag(name = "Communication Templates")
public class CommunicationTemplateController {

    private final CommunicationTemplateService service;
    private final CommunicationTemplateRbacService rbacService;
    private final ProviderRouterService providerRouterService;

    public CommunicationTemplateController(
            CommunicationTemplateService service,
            CommunicationTemplateRbacService rbacService,
            ProviderRouterService providerRouterService
    ) {
        this.service = service;
        this.rbacService = rbacService;
        this.providerRouterService = providerRouterService;
    }

    @PostMapping
    public ResponseEntity<CommunicationTemplateResponse> create(
            @Valid @RequestBody CommunicationTemplateCreateRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireManage(actor, organizationId);
        CommunicationTemplateResponse response = service.create(request, actor.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public CommunicationTemplateResponse get(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireRead(actor, organizationId);
        return service.get(id);
    }

    @GetMapping
    public Page<CommunicationTemplateResponse> list(
            @RequestParam(required = false) String templateKey,
            Pageable pageable,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireRead(actor, organizationId);
        return service.list(templateKey, pageable);
    }

    @PatchMapping("/{id}")
    public CommunicationTemplateResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CommunicationTemplateUpdateRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireManage(actor, organizationId);
        return service.update(id, request, actor.toString());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireManage(actor, organizationId);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/preview")
    public TemplatePreviewResponse preview(
            @Valid @RequestBody TemplatePreviewRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireRead(actor, organizationId);
        return service.preview(request.templateId(), request.variables());
    }

    @PostMapping("/test-send")
    public TemplateTestSendResponse testSend(
            @Valid @RequestBody TemplateTestSendRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireManage(actor, organizationId);
        ProviderDispatchResult result = service.testSend(request.templateId(), request.recipient(), request.variables());
        return new TemplateTestSendResponse(
                result.channel(),
                result.providerName(),
                result.status(),
                result.providerReference()
        );
    }

    @GetMapping("/providers/health")
    public java.util.List<ProviderHealthResponse> providerHealth(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbacService.requireRead(actor, organizationId);
        return providerRouterService.healthStatuses().stream().map(this::toProviderHealthResponse).toList();
    }

    private ProviderHealthResponse toProviderHealthResponse(ProviderHealthStatus status) {
        return new ProviderHealthResponse(
                status.providerName(),
                status.channel(),
                status.status(),
                status.details()
        );
    }
}
