package com.easyops.communication.service;

import com.easyops.communication.dto.CommunicationTemplateCreateRequest;
import com.easyops.communication.dto.CommunicationTemplateResponse;
import com.easyops.communication.dto.CommunicationTemplateUpdateRequest;
import com.easyops.communication.dto.TemplatePreviewResponse;
import com.easyops.communication.entity.CommunicationTemplate;
import com.easyops.communication.provider.CommunicationProvider;
import com.easyops.communication.provider.ProviderDispatchRequest;
import com.easyops.communication.provider.ProviderDispatchResult;
import com.easyops.communication.exception.DuplicateResourceException;
import com.easyops.communication.exception.ResourceNotFoundException;
import com.easyops.communication.repository.CommunicationTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class CommunicationTemplateService {

    private final CommunicationTemplateRepository repository;
    private final TemplateVariableSchemaValidator schemaValidator;
    private final TemplateRenderer templateRenderer;
    private final ProviderRouterService providerRouterService;
    private final MeterRegistry meterRegistry;

    public CommunicationTemplateService(
            CommunicationTemplateRepository repository,
            TemplateVariableSchemaValidator schemaValidator,
            TemplateRenderer templateRenderer,
            ProviderRouterService providerRouterService,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.schemaValidator = schemaValidator;
        this.templateRenderer = templateRenderer;
        this.providerRouterService = providerRouterService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public CommunicationTemplateResponse create(CommunicationTemplateCreateRequest request, String actor) {
        schemaValidator.validateSchemaJson(request.variablesSchema());
        schemaValidator.validateTemplateDefinition(
                request.channel(),
                request.subjectTemplate(),
                request.bodyTemplate(),
                request.variablesSchema()
        );
        repository.findByTemplateKeyAndChannelAndLocaleAndVersion(
                request.templateKey(),
                request.channel(),
                request.locale(),
                request.version()
        ).ifPresent(existing -> {
            throw new DuplicateResourceException("Template version already exists for templateKey/channel/locale/version");
        });

        CommunicationTemplate entity = new CommunicationTemplate();
        entity.setTemplateKey(request.templateKey());
        entity.setChannel(request.channel());
        entity.setLocale(request.locale());
        entity.setVersion(request.version());
        entity.setStatus(request.status());
        entity.setSubjectTemplate(request.subjectTemplate());
        entity.setBodyTemplate(request.bodyTemplate());
        entity.setVariablesSchema(request.variablesSchema());
        entity.setCreatedBy(actor);
        applyActivationMetadata(entity, actor);
        CommunicationTemplate saved = repository.save(entity);
        if (isActive(saved.getStatus())) {
            repository.archiveOtherActiveVersions(
                    saved.getTemplateKey(),
                    saved.getChannel(),
                    saved.getLocale(),
                    saved.getId()
            );
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CommunicationTemplateResponse get(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CommunicationTemplateResponse> list(String templateKey, Pageable pageable) {
        if (templateKey == null || templateKey.isBlank()) {
            return repository.findAll(pageable).map(this::toResponse);
        }
        return repository.findByTemplateKeyContainingIgnoreCase(templateKey, pageable).map(this::toResponse);
    }

    @Transactional
    public CommunicationTemplateResponse update(UUID id, CommunicationTemplateUpdateRequest request, String actor) {
        CommunicationTemplate entity = getEntity(id);
        if (request.templateKey() != null) {
            entity.setTemplateKey(request.templateKey());
        }
        if (request.channel() != null) {
            entity.setChannel(request.channel());
        }
        if (request.locale() != null) {
            entity.setLocale(request.locale());
        }
        if (request.version() != null) {
            entity.setVersion(request.version());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.subjectTemplate() != null) {
            entity.setSubjectTemplate(request.subjectTemplate());
        }
        if (request.bodyTemplate() != null) {
            entity.setBodyTemplate(request.bodyTemplate());
        }
        if (request.variablesSchema() != null) {
            entity.setVariablesSchema(request.variablesSchema());
        }
        schemaValidator.validateSchemaJson(entity.getVariablesSchema());
        schemaValidator.validateTemplateDefinition(
                entity.getChannel(),
                entity.getSubjectTemplate(),
                entity.getBodyTemplate(),
                entity.getVariablesSchema()
        );
        applyActivationMetadata(entity, actor);
        CommunicationTemplate saved = repository.save(entity);
        if (isActive(saved.getStatus())) {
            repository.archiveOtherActiveVersions(
                    saved.getTemplateKey(),
                    saved.getChannel(),
                    saved.getLocale(),
                    saved.getId()
            );
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse preview(UUID id, Map<String, Object> variables) {
        CommunicationTemplate entity = getEntity(id);
        long startNanos = System.nanoTime();
        schemaValidator.validatePayloadAgainstSchema(entity.getVariablesSchema(), variables);
        TemplatePreviewResponse response = new TemplatePreviewResponse(
                templateRenderer.render(entity.getSubjectTemplate(), variables),
                templateRenderer.render(entity.getBodyTemplate(), variables)
        );
        meterRegistry.counter("communication.template.preview.requests", "channel", entity.getChannel()).increment();
        meterRegistry.timer("communication.template.preview.latency", "channel", entity.getChannel())
                .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        return response;
    }

    @Transactional(readOnly = true)
    public ProviderDispatchResult testSend(UUID id, String recipient, Map<String, Object> variables) {
        CommunicationTemplate entity = getEntity(id);
        long startNanos = System.nanoTime();
        String channel = entity.getChannel();
        CommunicationProvider provider = providerRouterService.resolveByChannel(channel);
        try {
            schemaValidator.validatePayloadAgainstSchema(entity.getVariablesSchema(), variables);
            ProviderDispatchResult result = provider.send(new ProviderDispatchRequest(
                    recipient,
                    templateRenderer.render(entity.getSubjectTemplate(), variables),
                    templateRenderer.render(entity.getBodyTemplate(), variables)
            ));
            meterRegistry.counter(
                    "communication.provider.dispatch",
                    "channel", channel,
                    "provider", result.providerName(),
                    "status", result.status()
            ).increment();
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                    "communication.provider.dispatch",
                    "channel", channel,
                    "provider", provider.providerName(),
                    "status", "FAILED"
            ).increment();
            throw ex;
        } finally {
            meterRegistry.timer(
                            "communication.provider.dispatch.latency",
                            "channel", channel,
                            "provider", provider.providerName()
                    )
                    .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    private CommunicationTemplate getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    private CommunicationTemplateResponse toResponse(CommunicationTemplate entity) {
        return new CommunicationTemplateResponse(
                entity.getId(),
                entity.getTemplateKey(),
                entity.getChannel(),
                entity.getLocale(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getSubjectTemplate(),
                entity.getBodyTemplate(),
                entity.getVariablesSchema(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getActivatedAt(),
                entity.getActivatedBy()
        );
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    private void applyActivationMetadata(CommunicationTemplate entity, String actor) {
        if (isActive(entity.getStatus())) {
            if (entity.getActivatedAt() == null) {
                entity.setActivatedAt(Instant.now());
            }
            entity.setActivatedBy(actor);
        } else {
            entity.setActivatedAt(null);
            entity.setActivatedBy(null);
        }
    }
}
