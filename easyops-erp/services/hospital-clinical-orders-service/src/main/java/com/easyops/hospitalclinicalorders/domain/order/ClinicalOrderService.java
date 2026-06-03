package com.easyops.hospitalclinicalorders.domain.order;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.config.ClinicalOrdersMetrics;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSet;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSetRepository;
import com.easyops.hospitalclinicalorders.domain.result.ResultLinkService;
import com.easyops.hospitalclinicalorders.domain.worklist.OrderWorklistItem;
import com.easyops.hospitalclinicalorders.domain.worklist.OrderWorklistItemRepository;
import com.easyops.hospitalclinicalorders.events.ClinicalOrdersEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClinicalOrderService {

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ClinicalOrderRepository clinicalOrderRepository;
    private final OrderSetRepository orderSetRepository;
    private final OrderWorklistItemRepository worklistItemRepository;
    private final ResultLinkService resultLinkService;
    private final ClinicalOrdersEventPublisher eventPublisher;
    private final OrderAuditLogRepository orderAuditLogRepository;

    public ClinicalOrderService(ClinicalOrderRepository clinicalOrderRepository,
                                OrderSetRepository orderSetRepository,
                                OrderWorklistItemRepository worklistItemRepository,
                                ResultLinkService resultLinkService,
                                ClinicalOrdersEventPublisher eventPublisher,
                                OrderAuditLogRepository orderAuditLogRepository) {
        this.clinicalOrderRepository = clinicalOrderRepository;
        this.orderSetRepository = orderSetRepository;
        this.worklistItemRepository = worklistItemRepository;
        this.resultLinkService = resultLinkService;
        this.eventPublisher = eventPublisher;
        this.orderAuditLogRepository = orderAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public ClinicalOrderDetailResponse getById(UUID id) {
        ClinicalOrder order = clinicalOrderRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + id));
        ClinicalOrderDetailResponse detail = new ClinicalOrderDetailResponse();
        copyOrderToResponse(order, detail);
        List<OrderWorklistItem> items = worklistItemRepository.findByOrderId(id);
        detail.setWorklistItems(items.stream().map(ClinicalOrderService::toWorklistItemResponse).collect(Collectors.toList()));
        detail.setResultLinks(resultLinkService.findByOrderId(id));
        return detail;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClinicalOrderResponse> list(UUID facilityId, UUID patientId, UUID visitId, UUID orderSetId,
                                                      String type, String status,
                                                      OffsetDateTime from, OffsetDateTime to,
                                                      int page, int size) {
        Specification<ClinicalOrder> spec = Specification
                .where(ClinicalOrderSpecifications.hasFacilityId(facilityId))
                .and(ClinicalOrderSpecifications.hasOrderSetId(orderSetId))
                .and(ClinicalOrderSpecifications.hasPatientIdViaOrderSet(patientId))
                .and(ClinicalOrderSpecifications.hasVisitIdViaOrderSet(visitId))
                .and(ClinicalOrderSpecifications.hasOrderType(type))
                .and(ClinicalOrderSpecifications.hasStatus(status))
                .and(ClinicalOrderSpecifications.createdAtBetween(from, to));
        int cappedSize = Math.min(size, ClinicalOrdersMetrics.PAGINATION_MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(page, cappedSize);
        Page<ClinicalOrder> result = clinicalOrderRepository.findAll(spec, pageRequest);
        PagedResponse<ClinicalOrderResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(ClinicalOrderService::toOrderResponse).collect(Collectors.toList()));
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    @Transactional
    public ClinicalOrderResponse cancel(UUID orderId, CancelOrderRequest request) {
        ClinicalOrder order = clinicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + orderId));
        OffsetDateTime now = OffsetDateTime.now();

        boolean hasFinalResult = "FINAL".equalsIgnoreCase(order.getResultStatus());
        boolean isCompleted = STATUS_COMPLETED.equals(order.getStatus());
        boolean adminOverride = Boolean.TRUE.equals(request.getAdminOverride());

        if ((isCompleted || hasFinalResult) && !adminOverride) {
            throw new IllegalStateException("Cannot cancel completed or finalized order without admin override");
        }
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled");
        }
        String previousStatus = order.getStatus();
        order.setStatus(STATUS_CANCELLED);
        order.setCancelReason(request.getReason());
        order.setCancelledAt(now);
        order.setCancelledBy(request.getCancelledBy());
        clinicalOrderRepository.save(order);

        OrderAuditLog audit = new OrderAuditLog();
        audit.setId(UUID.randomUUID());
        audit.setOrderId(orderId);
        audit.setFromStatus(previousStatus);
        audit.setToStatus(STATUS_CANCELLED);
        audit.setChangedBy(request.getCancelledBy());
        audit.setChangedAt(now);
        StringBuilder reason = new StringBuilder();
        if (request.getReason() != null) {
            reason.append(request.getReason());
        }
        if (adminOverride && request.getOverrideReason() != null && !request.getOverrideReason().isBlank()) {
            if (!reason.isEmpty()) {
                reason.append(" | ");
            }
            reason.append("Override: ").append(request.getOverrideReason());
        }
        audit.setReason(reason.isEmpty() ? null : reason.toString());
        audit.setEventType("CANCEL");
        orderAuditLogRepository.save(audit);

        for (OrderWorklistItem item : worklistItemRepository.findByOrderId(orderId)) {
            item.setStatus("CANCELLED");
            worklistItemRepository.save(item);
        }

        OrderSet orderSet = orderSetRepository.findById(order.getOrderSetId()).orElse(null);
        UUID patientId = orderSet != null ? orderSet.getPatientId() : null;
        UUID visitId = orderSet != null ? orderSet.getVisitId() : null;
        eventPublisher.publishOrderCancelled(order.getOrderSetId(), orderId, patientId, visitId, now);
        return toOrderResponse(order);
    }

    @Transactional
    public ClinicalOrderResponse update(UUID orderId, UpdateOrderRequest request) {
        ClinicalOrder order = clinicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + orderId));
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot update cancelled order");
        }
        if (request.getPriority() != null) order.setPriority(request.getPriority());
        if (request.getOrderingNotes() != null) order.setOrderingNotes(request.getOrderingNotes());
        clinicalOrderRepository.save(order);
        return toOrderResponse(order);
    }

    private static void copyOrderToResponse(ClinicalOrder o, ClinicalOrderResponse r) {
        r.setId(o.getId());
        r.setOrderSetId(o.getOrderSetId());
        r.setFacilityId(o.getFacilityId());
        r.setOrderType(o.getOrderType());
        r.setItemCode(o.getItemCode());
        r.setStatus(o.getStatus());
        r.setPriority(o.getPriority());
        r.setOrderingNotes(o.getOrderingNotes());
        r.setPerformedAt(o.getPerformedAt());
        r.setPerformedBy(o.getPerformedBy());
        r.setCancelReason(o.getCancelReason());
        r.setCancelledAt(o.getCancelledAt());
        r.setCancelledBy(o.getCancelledBy());
        r.setExternalSystemId(o.getExternalSystemId());
        r.setResultStatus(o.getResultStatus());
        r.setResultAvailableAt(o.getResultAvailableAt());
        r.setCreatedAt(o.getCreatedAt());
        r.setCreatedBy(o.getCreatedBy());
    }

    public static ClinicalOrderResponse toOrderResponse(ClinicalOrder o) {
        ClinicalOrderResponse r = new ClinicalOrderResponse();
        copyOrderToResponse(o, r);
        return r;
    }

    private static WorklistItemResponse toWorklistItemResponse(OrderWorklistItem w) {
        WorklistItemResponse r = new WorklistItemResponse();
        r.setId(w.getId());
        r.setOrderId(w.getOrderId());
        r.setWorklistType(w.getWorklistType());
        r.setAssignedToUserId(w.getAssignedToUserId());
        r.setAssignedToRole(w.getAssignedToRole());
        r.setScheduledTime(w.getScheduledTime());
        r.setStatus(w.getStatus());
        r.setRemarks(w.getRemarks());
        r.setCreatedAt(w.getCreatedAt());
        r.setUpdatedAt(w.getUpdatedAt());
        return r;
    }
}
