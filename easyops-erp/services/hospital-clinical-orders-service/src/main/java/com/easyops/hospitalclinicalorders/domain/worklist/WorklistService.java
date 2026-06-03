package com.easyops.hospitalclinicalorders.domain.worklist;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrder;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderRepository;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderService;
import com.easyops.hospitalclinicalorders.domain.order.OrderAuditLog;
import com.easyops.hospitalclinicalorders.domain.order.OrderAuditLogRepository;
import com.easyops.hospitalclinicalorders.config.ClinicalOrdersMetrics;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSet;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSetRepository;
import com.easyops.hospitalclinicalorders.events.ClinicalOrdersEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorklistService {

    private static final String ORDER_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String EVENT_ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    private final OrderWorklistItemRepository worklistItemRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final OrderSetRepository orderSetRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final ClinicalOrdersEventPublisher eventPublisher;
    private final ClinicalOrdersMetrics metrics;

    public WorklistService(OrderWorklistItemRepository worklistItemRepository,
                           ClinicalOrderRepository clinicalOrderRepository,
                           OrderSetRepository orderSetRepository,
                           OrderAuditLogRepository orderAuditLogRepository,
                           ClinicalOrdersEventPublisher eventPublisher,
                           ClinicalOrdersMetrics metrics) {
        this.worklistItemRepository = worklistItemRepository;
        this.clinicalOrderRepository = clinicalOrderRepository;
        this.orderSetRepository = orderSetRepository;
        this.orderAuditLogRepository = orderAuditLogRepository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorklistItemDetailResponse> list(String type, String status, UUID assignedTo,
                                                          UUID departmentId, String section, UUID facilityId,
                                                          OffsetDateTime from, OffsetDateTime to,
                                                          int page, int size) {
        int cappedSize = Math.min(size, ClinicalOrdersMetrics.PAGINATION_MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(page, cappedSize);
        // Use priority sort (STAT first, then URGENT, then ROUTINE), then scheduled_time, then created_at
        Page<OrderWorklistItem> result = worklistItemRepository.findWithFiltersAndPrioritySort(
                (type != null && !type.isBlank()) ? type : null,
                (status != null && !status.isBlank()) ? status : null,
                assignedTo,
                departmentId,
                (section != null && !section.isBlank()) ? section : null,
                facilityId,
                from,
                to,
                pageRequest);
        List<WorklistItemDetailResponse> content = result.getContent().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
        PagedResponse<WorklistItemDetailResponse> response = new PagedResponse<>();
        response.setContent(content);
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    private WorklistItemDetailResponse toDetailResponse(OrderWorklistItem item) {
        WorklistItemDetailResponse detail = new WorklistItemDetailResponse();
        detail.setId(item.getId());
        detail.setOrderId(item.getOrderId());
        detail.setWorklistType(item.getWorklistType());
        detail.setAssignedToUserId(item.getAssignedToUserId());
        detail.setAssignedToRole(item.getAssignedToRole());
        detail.setScheduledTime(item.getScheduledTime());
        detail.setStatus(item.getStatus());
        detail.setRemarks(item.getRemarks());
        detail.setCreatedAt(item.getCreatedAt());
        detail.setUpdatedAt(item.getUpdatedAt());
        ClinicalOrder order = clinicalOrderRepository.findById(item.getOrderId()).orElse(null);
        if (order != null) {
            detail.setOrder(ClinicalOrderService.toOrderResponse(order));
            OrderSet orderSet = orderSetRepository.findById(order.getOrderSetId()).orElse(null);
            if (orderSet != null) {
                detail.setOrderSetId(orderSet.getId());
                detail.setPatientId(orderSet.getPatientId());
                detail.setVisitId(orderSet.getVisitId());
            }
        }
        return detail;
    }

    @Transactional
    public WorklistItemResponse assign(UUID worklistItemId, AssignWorklistRequest request) {
        OrderWorklistItem item = worklistItemRepository.findById(worklistItemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Worklist item not found: " + worklistItemId));
        item.setAssignedToUserId(request.getAssignedToUserId());
        item.setAssignedToRole(request.getAssignedToRole());
        item.setStatus("ASSIGNED");
        worklistItemRepository.save(item);
        eventPublisher.publishWorklistStatusChanged(worklistItemId, item.getOrderId(), "ASSIGNED", OffsetDateTime.now());
        metrics.incrementWorklistStatusChanges();
        return toItemResponse(item);
    }

    @Transactional
    public WorklistItemResponse updateStatus(UUID worklistItemId, UpdateWorklistStatusRequest request) {
        OrderWorklistItem item = worklistItemRepository.findById(worklistItemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Worklist item not found: " + worklistItemId));
        String newStatus = request.getStatus();
        item.setStatus(newStatus);
        if (request.getRemarks() != null) item.setRemarks(request.getRemarks());
        worklistItemRepository.save(item);

        ClinicalOrder order = clinicalOrderRepository.findById(item.getOrderId()).orElse(null);
        if (order != null && (ORDER_STATUS_IN_PROGRESS.equals(newStatus) || ORDER_STATUS_COMPLETED.equals(newStatus))) {
            String previousOrderStatus = order.getStatus();
            order.setStatus(newStatus);
            if (ORDER_STATUS_COMPLETED.equals(newStatus)) {
                order.setPerformedAt(OffsetDateTime.now());
            }
            clinicalOrderRepository.save(order);

            OffsetDateTime now = OffsetDateTime.now();
            OrderAuditLog audit = new OrderAuditLog();
            audit.setId(UUID.randomUUID());
            audit.setOrderId(order.getId());
            audit.setFromStatus(previousOrderStatus);
            audit.setToStatus(newStatus);
            audit.setChangedAt(now);
            audit.setReason(request.getRemarks() != null && !request.getRemarks().isBlank() ? request.getRemarks() : null);
            audit.setEventType(EVENT_ORDER_STATUS_CHANGED);
            orderAuditLogRepository.save(audit);

            OrderSet os = orderSetRepository.findById(order.getOrderSetId()).orElse(null);
            UUID patientId = os != null ? os.getPatientId() : null;
            UUID visitId = os != null ? os.getVisitId() : null;
            eventPublisher.publishOrderStatusChanged(order.getOrderSetId(), order.getId(), patientId, visitId, newStatus, now);
        }
        eventPublisher.publishWorklistStatusChanged(worklistItemId, item.getOrderId(), newStatus, OffsetDateTime.now());
        metrics.incrementWorklistStatusChanges();
        return toItemResponse(item);
    }

    private static WorklistItemResponse toItemResponse(OrderWorklistItem w) {
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
