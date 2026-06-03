package com.easyops.hospitalclinicalorders.domain.result;

import com.easyops.hospitalclinicalorders.api.dto.CreateResultLinkRequest;
import com.easyops.hospitalclinicalorders.api.dto.ResultLinkResponse;
import com.easyops.hospitalclinicalorders.config.ClinicalOrdersMetrics;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrder;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderRepository;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSet;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSetRepository;
import com.easyops.hospitalclinicalorders.events.ClinicalOrdersEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResultLinkService {

    private static final String RESULT_STATUS_FINAL = "FINAL";

    private final ResultLinkRepository resultLinkRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final OrderSetRepository orderSetRepository;
    private final ClinicalOrdersEventPublisher eventPublisher;
    private final ClinicalOrdersMetrics metrics;

    public ResultLinkService(ResultLinkRepository resultLinkRepository,
                             ClinicalOrderRepository clinicalOrderRepository,
                             OrderSetRepository orderSetRepository,
                             ClinicalOrdersEventPublisher eventPublisher,
                             ClinicalOrdersMetrics metrics) {
        this.resultLinkRepository = resultLinkRepository;
        this.clinicalOrderRepository = clinicalOrderRepository;
        this.orderSetRepository = orderSetRepository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Transactional
    public ResultLinkResponse create(UUID orderId, CreateResultLinkRequest request) {
        ClinicalOrder order = clinicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + orderId));

        OffsetDateTime now = OffsetDateTime.now();
        if (request.getResultStatus() != null && RESULT_STATUS_FINAL.equalsIgnoreCase(request.getResultStatus().trim())) {
            order.setResultStatus(RESULT_STATUS_FINAL);
            order.setResultAvailableAt(now);
            clinicalOrderRepository.save(order);
        }

        ResultLink link = new ResultLink();
        link.setId(UUID.randomUUID());
        link.setOrderId(orderId);
        link.setSystemType(request.getSystemType().trim());
        link.setExternalSystemId(request.getExternalSystemId() != null ? request.getExternalSystemId().trim() : null);
        link.setViewerUrl(request.getViewerUrl() != null ? request.getViewerUrl().trim() : null);
        link.setVersion(request.getVersion() != null ? request.getVersion() : 1);
        link.setRevisedAt(request.getRevisedAt() != null ? request.getRevisedAt() : now);
        link.setCreatedAt(now);
        resultLinkRepository.save(link);

        OrderSet orderSet = orderSetRepository.findById(order.getOrderSetId()).orElse(null);
        UUID patientId = orderSet != null ? orderSet.getPatientId() : null;
        UUID visitId = orderSet != null ? orderSet.getVisitId() : null;
        String resultStatusForEvent = request.getResultStatus() != null ? request.getResultStatus().trim() : "";
        eventPublisher.publishResultAvailable(orderId, order.getOrderSetId(), patientId, visitId,
                link.getId(), resultStatusForEvent, link.getSystemType(), now);
        metrics.incrementResultAvailable();

        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<ResultLinkResponse> findByOrderId(UUID orderId) {
        if (!clinicalOrderRepository.existsById(orderId)) {
            throw new java.util.NoSuchElementException("Order not found: " + orderId);
        }
        return resultLinkRepository.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ResultLinkResponse toResponse(ResultLink link) {
        ResultLinkResponse r = new ResultLinkResponse();
        r.setId(link.getId());
        r.setOrderId(link.getOrderId());
        r.setSystemType(link.getSystemType());
        r.setExternalSystemId(link.getExternalSystemId());
        r.setViewerUrl(link.getViewerUrl());
        r.setVersion(link.getVersion());
        r.setRevisedAt(link.getRevisedAt());
        r.setCreatedAt(link.getCreatedAt());
        return r;
    }
}
