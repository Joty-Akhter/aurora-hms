package com.easyops.hospitalclinicalorders.domain.orderset;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrder;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderRepository;
import com.easyops.hospitalclinicalorders.domain.order.OrderAuditLog;
import com.easyops.hospitalclinicalorders.domain.order.OrderAuditLogRepository;
import com.easyops.hospitalclinicalorders.domain.worklist.OrderWorklistItem;
import com.easyops.hospitalclinicalorders.domain.worklist.OrderWorklistItemRepository;
import com.easyops.hospitalclinicalorders.config.ClinicalOrdersMetrics;
import com.easyops.hospitalclinicalorders.events.ClinicalOrdersEventPublisher;
import com.easyops.hospitalclinicalorders.integration.FacilityResolver;
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
public class OrderSetService {

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String RESULT_STATUS_PENDING = "PENDING";
    private static final String WORKLIST_STATUS_QUEUED = "QUEUED";

    private static final String EVENT_ORDER_CREATED = "ORDER_CREATED";

    private final OrderSetRepository orderSetRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final OrderWorklistItemRepository worklistItemRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final FacilityResolver facilityResolver;
    private final ClinicalOrdersEventPublisher eventPublisher;
    private final ClinicalOrdersMetrics metrics;

    public OrderSetService(OrderSetRepository orderSetRepository,
                           ClinicalOrderRepository clinicalOrderRepository,
                           OrderWorklistItemRepository worklistItemRepository,
                           OrderAuditLogRepository orderAuditLogRepository,
                           FacilityResolver facilityResolver,
                           ClinicalOrdersEventPublisher eventPublisher,
                           ClinicalOrdersMetrics metrics) {
        this.orderSetRepository = orderSetRepository;
        this.clinicalOrderRepository = clinicalOrderRepository;
        this.worklistItemRepository = worklistItemRepository;
        this.orderAuditLogRepository = orderAuditLogRepository;
        this.facilityResolver = facilityResolver;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Transactional
    public OrderSetResponse create(CreateOrderSetRequest request, UUID createdBy) {
        if (request.getOrders() == null || request.getOrders().isEmpty()) {
            throw new IllegalArgumentException("At least one order line is required");
        }
        // TODO: when hospital-service exposes patient/visit validation API, call it here
        if (request.getFacilityId() != null) {
            facilityResolver.validate(request.getFacilityId());
        }
        UUID orderSetId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        OrderSet orderSet = new OrderSet();
        orderSet.setId(orderSetId);
        orderSet.setPatientId(request.getPatientId());
        orderSet.setVisitId(request.getVisitId());
        orderSet.setOrderingDoctorId(request.getOrderingDoctorId());
        orderSet.setOrderingDepartmentId(request.getOrderingDepartmentId());
        orderSet.setOrderContext(request.getOrderContext());
        orderSet.setPriority(request.getPriority() != null ? request.getPriority() : "ROUTINE");
        orderSet.setFacilityId(request.getFacilityId());
        orderSet.setCreatedBy(createdBy);
        orderSetRepository.save(orderSet);

        for (CreateOrderLineRequest line : request.getOrders()) {
            UUID orderId = UUID.randomUUID();
            ClinicalOrder order = new ClinicalOrder();
            order.setId(orderId);
            order.setOrderSetId(orderSetId);
            order.setFacilityId(request.getFacilityId());
            order.setOrderType(line.getOrderType());
            order.setItemCode(line.getItemCode());
            order.setStatus(STATUS_REQUESTED);
            order.setPriority(line.getPriority() != null ? line.getPriority() : orderSet.getPriority());
            order.setOrderingNotes(line.getOrderingNotes());
            order.setResultStatus(RESULT_STATUS_PENDING);
            order.setCreatedBy(createdBy);
            clinicalOrderRepository.save(order);

            OrderWorklistItem worklistItem = new OrderWorklistItem();
            worklistItem.setId(UUID.randomUUID());
            worklistItem.setOrderId(orderId);
            worklistItem.setWorklistType(worklistTypeFromOrderType(line.getOrderType()));
            worklistItem.setStatus(WORKLIST_STATUS_QUEUED);
            worklistItemRepository.save(worklistItem);

            OrderAuditLog createAudit = new OrderAuditLog();
            createAudit.setId(UUID.randomUUID());
            createAudit.setOrderId(orderId);
            createAudit.setFromStatus(null);
            createAudit.setToStatus(STATUS_REQUESTED);
            createAudit.setChangedBy(createdBy);
            createAudit.setChangedAt(now);
            createAudit.setEventType(EVENT_ORDER_CREATED);
            orderAuditLogRepository.save(createAudit);

            eventPublisher.publishOrderCreated(orderSetId, orderId, orderSet.getPatientId(), orderSet.getVisitId(), STATUS_REQUESTED, now);
            metrics.incrementOrdersCreated();
        }

        eventPublisher.publishOrderSetCreated(orderSetId, orderSet.getPatientId(), orderSet.getVisitId(), now);
        metrics.incrementOrderSetsCreated();
        return toOrderSetResponse(orderSet);
    }

    /**
     * Create a new order set by copying an existing one (repeat order). Same patient, visit, ordering doctor/department,
     * and same order lines with new ids; all orders status REQUESTED. Optional overrides for orderContext and priority.
     */
    @Transactional
    public OrderSetResponse copyFromOrderSet(CopyOrderSetRequest request, UUID createdBy) {
        OrderSet source = orderSetRepository.findById(request.getSourceOrderSetId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Order set not found: " + request.getSourceOrderSetId()));
        List<ClinicalOrder> sourceOrders = clinicalOrderRepository.findByOrderSetId(source.getId());
        if (sourceOrders.isEmpty()) {
            throw new IllegalArgumentException("Source order set has no orders to copy");
        }

        UUID orderSetId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        OrderSet orderSet = new OrderSet();
        orderSet.setId(orderSetId);
        orderSet.setPatientId(source.getPatientId());
        orderSet.setVisitId(source.getVisitId());
        orderSet.setOrderingDoctorId(source.getOrderingDoctorId());
        orderSet.setOrderingDepartmentId(source.getOrderingDepartmentId());
        orderSet.setOrderContext(request.getOrderContext() != null && !request.getOrderContext().isBlank()
                ? request.getOrderContext() : source.getOrderContext());
        orderSet.setPriority(request.getPriority() != null && !request.getPriority().isBlank()
                ? request.getPriority() : (source.getPriority() != null ? source.getPriority() : "ROUTINE"));
        orderSet.setFacilityId(request.getFacilityId() != null ? request.getFacilityId() : source.getFacilityId());
        orderSet.setCreatedBy(createdBy);
        if (orderSet.getFacilityId() != null) {
            facilityResolver.validate(orderSet.getFacilityId());
        }
        orderSetRepository.save(orderSet);

        for (ClinicalOrder sourceOrder : sourceOrders) {
            UUID orderId = UUID.randomUUID();
            ClinicalOrder order = new ClinicalOrder();
            order.setId(orderId);
            order.setOrderSetId(orderSetId);
            order.setFacilityId(orderSet.getFacilityId());
            order.setOrderType(sourceOrder.getOrderType());
            order.setItemCode(sourceOrder.getItemCode());
            order.setStatus(STATUS_REQUESTED);
            order.setPriority(sourceOrder.getPriority() != null ? sourceOrder.getPriority() : orderSet.getPriority());
            order.setOrderingNotes(sourceOrder.getOrderingNotes());
            order.setResultStatus(RESULT_STATUS_PENDING);
            order.setCreatedBy(createdBy);
            clinicalOrderRepository.save(order);

            OrderWorklistItem worklistItem = new OrderWorklistItem();
            worklistItem.setId(UUID.randomUUID());
            worklistItem.setOrderId(orderId);
            worklistItem.setWorklistType(worklistTypeFromOrderType(sourceOrder.getOrderType()));
            worklistItem.setStatus(WORKLIST_STATUS_QUEUED);
            worklistItemRepository.save(worklistItem);

            OrderAuditLog copyAudit = new OrderAuditLog();
            copyAudit.setId(UUID.randomUUID());
            copyAudit.setOrderId(orderId);
            copyAudit.setFromStatus(null);
            copyAudit.setToStatus(STATUS_REQUESTED);
            copyAudit.setChangedBy(createdBy);
            copyAudit.setChangedAt(now);
            copyAudit.setEventType(EVENT_ORDER_CREATED);
            orderAuditLogRepository.save(copyAudit);

            eventPublisher.publishOrderCreated(orderSetId, orderId, orderSet.getPatientId(), orderSet.getVisitId(), STATUS_REQUESTED, now);
            metrics.incrementOrdersCreated();
        }

        eventPublisher.publishOrderSetCreated(orderSetId, orderSet.getPatientId(), orderSet.getVisitId(), now);
        metrics.incrementOrderSetsCreated();
        return toOrderSetResponse(orderSet);
    }

    private static String worklistTypeFromOrderType(String orderType) {
        if (orderType == null) return "PROCEDURE_ROOM";
        return switch (orderType.toUpperCase()) {
            case "LAB" -> "LAB_SECTION";
            case "RADIOLOGY" -> "RADIOLOGY_ROOM";
            case "PROCEDURE" -> "PROCEDURE_ROOM";
            default -> "PROCEDURE_ROOM";
        };
    }

    @Transactional(readOnly = true)
    public OrderSetDetailResponse getById(UUID id) {
        OrderSet orderSet = orderSetRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order set not found: " + id));
        OrderSetDetailResponse detail = new OrderSetDetailResponse();
        copyOrderSetToResponse(orderSet, detail);
        List<ClinicalOrder> orders = clinicalOrderRepository.findByOrderSetId(id);
        detail.setOrders(orders.stream().map(OrderSetService::toClinicalOrderResponse).collect(Collectors.toList()));
        return detail;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSetResponse> list(UUID facilityId, UUID patientId, UUID visitId, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        int cappedSize = Math.min(size, ClinicalOrdersMetrics.PAGINATION_MAX_PAGE_SIZE);
        Specification<OrderSet> spec = Specification
                .where(OrderSetSpecifications.hasFacilityId(facilityId))
                .and(OrderSetSpecifications.hasPatientId(patientId))
                .and(OrderSetSpecifications.hasVisitId(visitId))
                .and(OrderSetSpecifications.createdAtBetween(from, to));
        PageRequest pageRequest = PageRequest.of(page, cappedSize);
        Page<OrderSet> result = orderSetRepository.findAll(spec, pageRequest);
        PagedResponse<OrderSetResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(OrderSetService::toOrderSetResponse).collect(Collectors.toList()));
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    private static void copyOrderSetToResponse(OrderSet os, OrderSetResponse r) {
        r.setId(os.getId());
        r.setPatientId(os.getPatientId());
        r.setVisitId(os.getVisitId());
        r.setOrderingDoctorId(os.getOrderingDoctorId());
        r.setOrderingDepartmentId(os.getOrderingDepartmentId());
        r.setOrderContext(os.getOrderContext());
        r.setPriority(os.getPriority());
        r.setFacilityId(os.getFacilityId());
        r.setCreatedAt(os.getCreatedAt());
        r.setCreatedBy(os.getCreatedBy());
    }

    private static OrderSetResponse toOrderSetResponse(OrderSet os) {
        OrderSetResponse r = new OrderSetResponse();
        copyOrderSetToResponse(os, r);
        return r;
    }

    private static ClinicalOrderResponse toClinicalOrderResponse(ClinicalOrder o) {
        ClinicalOrderResponse r = new ClinicalOrderResponse();
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
        return r;
    }
}
