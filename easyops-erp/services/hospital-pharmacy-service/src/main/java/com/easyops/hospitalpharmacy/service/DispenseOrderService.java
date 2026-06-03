package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.config.PharmacyDispenseProperties;
import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import com.easyops.hospitalpharmacy.config.PharmacyRegionalProperties;
import com.easyops.hospitalpharmacy.events.PharmacyDomainEventPublisher;
import com.easyops.hospitalpharmacy.integration.HospitalBillingClient;
import com.easyops.hospitalpharmacy.integration.HospitalServiceClient;
import com.easyops.hospitalpharmacy.dto.request.CreateDispenseOrderRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseReturnRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseUnfulfilledLineRequest;
import com.easyops.hospitalpharmacy.dto.request.PatchDispenseOrderRegionalRequest;
import com.easyops.hospitalpharmacy.dto.response.BillableDispenseItemResponse;
import com.easyops.hospitalpharmacy.dto.response.DispenseLineResponse;
import com.easyops.hospitalpharmacy.dto.response.DispenseOrderResponse;
import com.easyops.hospitalpharmacy.entity.*;
import com.easyops.hospitalpharmacy.repository.*;
import com.easyops.hospitalpharmacy.integration.dto.CreateChargePayload;
import com.easyops.hospitalpharmacy.integration.dto.InHouseDispenseFillPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispenseOrderService {

    /** Used when {@code allow-negative-stock} is true and the client omits {@code stockOverrideReason}. */
    private static final String DEFAULT_STOCK_OVERRIDE_REASON =
            "Recorded on-hand insufficient; issued per pharmacy policy (system default).";

    private final DispenseOrderRepository dispenseOrderRepository;
    private final DispenseLineRepository dispenseLineRepository;
    private final PharmacyLocationRepository pharmacyLocationRepository;
    private final DrugRepository drugRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PharmacyDispenseProperties dispenseProperties;
    private final PharmacyIntegrationProperties integrationProperties;
    private final HospitalBillingClient hospitalBillingClient;
    private final HospitalServiceClient hospitalServiceClient;
    private final PharmacyDomainEventPublisher pharmacyDomainEventPublisher;
    private final PharmacyRegionalProperties regionalProperties;
    private final NearExpiryEvaluationService nearExpiryEvaluationService;
    private final FormularyDispenseValidator formularyDispenseValidator;
    private final ClinicalSafetyAtDispenseService clinicalSafetyAtDispenseService;

    /**
     * Creates a dispense order from a prescription event (e.g. Kafka). Resolves pharmacy via
     * {@code hospital.pharmacy.integration.events.default-pharmacy-location-id} when set, else first active site.
     */
    @Transactional
    public DispenseOrderResponse createOrderFromPrescriptionEvent(UUID prescriptionId, UUID patientId, UUID visitId) {
        PharmacyLocation defaultLocation = resolvePharmacyLocationForPrescriptionEvent();
        CreateDispenseOrderRequest request = new CreateDispenseOrderRequest();
        request.setPrescriptionId(prescriptionId != null ? prescriptionId.toString() : null);
        request.setPatientId(patientId != null ? patientId.toString() : null);
        request.setVisitId(visitId != null ? visitId.toString() : null);
        request.setPharmacyLocationId(defaultLocation.getId());
        request.setDepartmentId(null);
        request.setContextType(DispenseOrder.ContextType.PATIENT_PRESCRIPTION.name());
        return createOrder(request);
    }

    private PharmacyLocation resolvePharmacyLocationForPrescriptionEvent() {
        UUID configured = integrationProperties.getEvents().getDefaultPharmacyLocationId();
        if (configured != null) {
            return pharmacyLocationRepository.findById(configured)
                    .filter(PharmacyLocation::isActive)
                    .orElseThrow(() -> new IllegalStateException(
                            "hospital.pharmacy.integration.events.default-pharmacy-location-id not found or inactive: "
                                    + configured));
        }
        return pharmacyLocationRepository.findAll().stream()
                .filter(PharmacyLocation::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active pharmacy location configured; cannot create dispense order from prescription event"));
    }

    @Transactional
    public DispenseOrderResponse createOrder(CreateDispenseOrderRequest request) {
        PharmacyLocation location = pharmacyLocationRepository.findById(request.getPharmacyLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + request.getPharmacyLocationId()));

        DispenseOrder.ContextType contextType = DispenseOrder.ContextType.valueOf(request.getContextType());

        UUID prescriptionUuid = parseUuidOnlyOptional("prescriptionId", request.getPrescriptionId());
        UUID visitUuid = resolveVisitRef(request.getVisitId());
        UUID patientUuid = resolvePatientRef(request.getPatientId());
        UUID departmentUuid = parseUuidOnlyOptional("departmentId", request.getDepartmentId());

        DispenseOrder order = DispenseOrder.builder()
                .prescriptionId(prescriptionUuid)
                .visitId(visitUuid)
                .patientId(patientUuid)
                .pharmacyLocation(location)
                .status(DispenseOrder.Status.PENDING)
                .contextType(contextType)
                .departmentId(departmentUuid)
                .paperPrescriptionRef(trimToNull(request.getPaperPrescriptionRef()))
                .prescriptionImageAttachmentId(request.getPrescriptionImageAttachmentId())
                .externalValidationStatus(ExternalValidationStatus.fromString(request.getExternalValidationStatus()))
                .build();

        DispenseOrder saved = dispenseOrderRepository.save(order);
        return toResponse(saved, List.of());
    }

    private static UUID parseUuidOnlyOptional(String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " must be a UUID, or omit the field");
        }
    }

    private UUID resolvePatientRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        try {
            return UUID.fromString(t);
        } catch (IllegalArgumentException e) {
            return hospitalServiceClient.lookupPatientIdByMrn(t);
        }
    }

    private UUID resolveVisitRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        try {
            return UUID.fromString(t);
        } catch (IllegalArgumentException e) {
            return hospitalServiceClient.lookupEncounterIdByNumber(t);
        }
    }

    @Transactional(readOnly = true)
    public DispenseOrderResponse getById(UUID id) {
        DispenseOrder order = dispenseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + id));
        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);
        return toResponse(order, lines);
    }

    /**
     * Billable line items for charging (Phase P1 — read-only; pricing from billing when integrated).
     */
    @Transactional(readOnly = true)
    public List<BillableDispenseItemResponse> getBillableItems(UUID orderId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));
        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);
        return lines.stream()
                .filter(line -> line.getQuantityDispensed() != null
                        && line.getQuantityDispensed().compareTo(BigDecimal.ZERO) > 0)
                .map(line -> {
                    Drug d = line.getDrug();
                    return BillableDispenseItemResponse.builder()
                            .dispenseLineId(line.getId())
                            .dispenseOrderId(order.getId())
                            .drugId(d.getId())
                            .drugGenericName(d.getGenericName())
                            .drugBrandName(d.getBrandName())
                            .strength(d.getStrength())
                            .form(d.getForm())
                            .unitOfMeasure(d.getUnitOfMeasure())
                            .batchNumber(line.getBatchNumber())
                            .quantityPrescribed(line.getQuantityPrescribed())
                            .quantityDispensed(line.getQuantityDispensed())
                            .lineStatus(line.getStatus())
                            .overrideReasonCode(line.getOverrideReasonCode())
                            .suggestedListPrice(null)
                            .taxCodeHint(null)
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DispenseOrderResponse> search(String patientId, String visitId, UUID pharmacyLocationId, String status) {
        UUID patientUuid = null;
        if (patientId != null && !patientId.isBlank()) {
            patientUuid = resolvePatientRef(patientId);
        }
        if (patientUuid != null) {
            return dispenseOrderRepository.findByPatientId(patientUuid).stream()
                    .map(o -> toResponse(o, dispenseLineRepository.findByDispenseOrder(o)))
                    .collect(Collectors.toList());
        }
        UUID visitUuid = null;
        if (visitId != null && !visitId.isBlank()) {
            visitUuid = resolveVisitRef(visitId);
        }
        if (visitUuid != null) {
            return dispenseOrderRepository.findByVisitId(visitUuid).stream()
                    .map(o -> toResponse(o, dispenseLineRepository.findByDispenseOrder(o)))
                    .collect(Collectors.toList());
        }
        if (pharmacyLocationId != null) {
            PharmacyLocation location = pharmacyLocationRepository.findById(pharmacyLocationId)
                    .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + pharmacyLocationId));
            if (status != null) {
                DispenseOrder.Status st = DispenseOrder.Status.valueOf(status);
                return dispenseOrderRepository.findByPharmacyLocationAndStatus(location, st).stream()
                        .map(o -> toResponse(o, dispenseLineRepository.findByDispenseOrder(o)))
                        .collect(Collectors.toList());
            }
            return dispenseOrderRepository.findByPharmacyLocation(location).stream()
                    .map(o -> toResponse(o, dispenseLineRepository.findByDispenseOrder(o)))
                    .collect(Collectors.toList());
        }
        return dispenseOrderRepository.findAll().stream()
                .map(o -> toResponse(o, dispenseLineRepository.findByDispenseOrder(o)))
                .collect(Collectors.toList());
    }

    @Transactional
    public DispenseOrderResponse addDispenseLines(
            UUID orderId, List<DispenseLineRequest> requests, UUID actorUserId, UUID organizationId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));

        assertNotBlockedBySoftValidation(order);

        order.setStatus(DispenseOrder.Status.IN_PROGRESS);
        dispenseOrderRepository.save(order);

        for (DispenseLineRequest req : requests) {
            Drug drug = drugRepository.findById(req.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + req.getDrugId()));

            if (dispenseProperties.isRequireWitnessForControlled() && drug.isControlledSubstance()
                    && req.getWitnessUserId() == null) {
                throw new IllegalArgumentException(
                        "Witness user is required for controlled substances when "
                                + "hospital.pharmacy.dispense.require-witness-for-controlled is true");
            }

            assertRegionalRxEvidence(order, drug);
            formularyDispenseValidator.validateDispenseLine(order, drug, req);
            clinicalSafetyAtDispenseService.assertDispenseAllowed(order, drug, req, actorUserId, organizationId);

            boolean allowNegativeStock = dispenseProperties.isAllowNegativeStock();

            String batchNumber = normalizeBatchNumber(req.getBatchNumber());

            PharmacyStock stock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(order.getPharmacyLocation(), drug, batchNumber)
                    .orElse(null);

            boolean stockOverrideUsed = false;
            String effectiveStockOverrideReason = null;
            if (stock == null) {
                if (!allowNegativeStock) {
                    throw stockInsufficientMessage(drug.getId(), true);
                }
                stockOverrideUsed = true;
                effectiveStockOverrideReason = resolveStockOverrideReason(req);
                stock = PharmacyStock.builder()
                        .pharmacyLocation(order.getPharmacyLocation())
                        .drug(drug)
                        .batchNumber(batchNumber)
                        .expiryDate(null)
                        .quantityOnHand(req.getQuantityDispensed().negate())
                        .build();
                pharmacyStockRepository.save(stock);
            } else {
                nearExpiryEvaluationService.assertDispenseAllowedForExpiry(drug, stock.getExpiryDate());
                BigDecimal newQty = stock.getQuantityOnHand().subtract(req.getQuantityDispensed());
                if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                    if (!allowNegativeStock) {
                        throw stockInsufficientMessage(drug.getId(), false);
                    }
                    stockOverrideUsed = true;
                    effectiveStockOverrideReason = resolveStockOverrideReason(req);
                }
                stock.setQuantityOnHand(newQty);
                pharmacyStockRepository.save(stock);
            }

            if (stockOverrideUsed && dispenseProperties.isRequireSupervisorForOverride()) {
                throw new IllegalArgumentException(
                        "Supervisor approval for stock override is not yet implemented; disable "
                                + "hospital.pharmacy.dispense.require-supervisor-for-override");
            }

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(order.getPharmacyLocation())
                    .drug(drug)
                    .movementType("issue_to_patient")
                    .quantity(req.getQuantityDispensed().negate())
                    .batchNumber(batchNumber)
                    .referenceType("DISPENSE_ORDER")
                    .referenceId(order.getId())
                    .requestedBy(actorUserId)
                    .build();
            stockMovementRepository.save(movement);
            if (integrationProperties.getEvents().isPublishEnabled()) {
                pharmacyDomainEventPublisher.publishStockChanged(
                        null,
                        order.getPharmacyLocation().getId(),
                        drug.getId(),
                        batchNumber,
                        movement.getQuantity(),
                        movement.getMovementType(),
                        order.getId());
            }

            DispenseLine.Status lineStatus = resolveLineStatus(req, stockOverrideUsed);
            BigDecimal remaining = null;
            if (req.getQuantityPrescribed() != null && lineStatus == DispenseLine.Status.PARTIALLY_DISPENSED) {
                remaining = req.getQuantityPrescribed().subtract(req.getQuantityDispensed());
            }

            Drug substituted = null;
            if (req.getSubstitutedDrugId() != null) {
                substituted = drugRepository.findById(req.getSubstitutedDrugId())
                        .orElseThrow(() -> new IllegalArgumentException("Substituted drug not found: " + req.getSubstitutedDrugId()));
            }

            DispenseLine lineEntity = DispenseLine.builder()
                    .dispenseOrder(order)
                    .prescriptionLineId(req.getPrescriptionLineId())
                    .drug(drug)
                    .substitutedDrug(substituted)
                    .batchNumber(batchNumber)
                    .quantityPrescribed(req.getQuantityPrescribed())
                    .quantityDispensed(req.getQuantityDispensed())
                    .quantityReturned(BigDecimal.ZERO)
                    .status(lineStatus)
                    .remainingQuantity(remaining)
                    .overrideReasonCode(stockOverrideUsed ? effectiveStockOverrideReason : null)
                    .formularyOverrideReason(trimToNull(req.getFormularyOverrideReason()))
                    .witnessUserId(req.getWitnessUserId())
                    .clinicalSafetyOverrideReason(trimToNull(req.getClinicalSafetyOverrideReason()))
                    .build();
            dispenseLineRepository.save(lineEntity);
        }

        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);
        return toResponse(order, lines);
    }

    /** Trim; empty or blank string becomes null (unbatched row per unique index on location+drug+COALESCE(batch,'')). */
    private static String normalizeBatchNumber(String batchNumber) {
        if (batchNumber == null) {
            return null;
        }
        String t = batchNumber.trim();
        return t.isEmpty() ? null : t;
    }

    private IllegalArgumentException stockInsufficientMessage(UUID drugId, boolean missingRow) {
        return new IllegalArgumentException(
                (missingRow ? "Insufficient stock for drug " : "Dispense would result in negative stock for drug ") + drugId
                        + ". Enable hospital.pharmacy.dispense.allow-negative-stock (negative stock / no stock row is recorded with an audit reason), or receive stock first.");
    }

    private static String resolveStockOverrideReason(DispenseLineRequest req) {
        String r = req.getStockOverrideReason();
        if (r != null && !r.isBlank()) {
            return r.trim();
        }
        return DEFAULT_STOCK_OVERRIDE_REASON;
    }

    private static DispenseLine.Status resolveLineStatus(DispenseLineRequest req, boolean stockOverrideUsed) {
        if (stockOverrideUsed) {
            return DispenseLine.Status.FILLED_WITH_STOCK_OVERRIDE;
        }
        if (req.getQuantityPrescribed() != null
                && req.getQuantityDispensed().compareTo(req.getQuantityPrescribed()) < 0) {
            return DispenseLine.Status.PARTIALLY_DISPENSED;
        }
        return DispenseLine.Status.DISPENSED;
    }

    @Transactional
    public DispenseOrderResponse recordReturns(UUID orderId, DispenseReturnRequest request, UUID actorUserId, UUID organizationId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));

        for (DispenseReturnRequest.Line lineReq : request.getLines()) {
            DispenseLine line = dispenseLineRepository.findById(lineReq.getDispenseLineId())
                    .orElseThrow(() -> new IllegalArgumentException("Dispense line not found: " + lineReq.getDispenseLineId()));

            if (!line.getDispenseOrder().getId().equals(order.getId())) {
                throw new IllegalArgumentException("Dispense line does not belong to order " + orderId);
            }

            BigDecimal newReturned = line.getQuantityReturned().add(lineReq.getQuantityReturned());
            if (newReturned.compareTo(line.getQuantityDispensed()) > 0) {
                throw new IllegalArgumentException("Return quantity exceeds dispensed quantity for line " + line.getId());
            }

            line.setQuantityReturned(newReturned);
            if (newReturned.compareTo(line.getQuantityDispensed()) >= 0) {
                line.setStatus(DispenseLine.Status.RETURNED);
            } else if (newReturned.compareTo(BigDecimal.ZERO) > 0) {
                line.setStatus(DispenseLine.Status.PARTIALLY_DISPENSED);
            }
            dispenseLineRepository.save(line);

            PharmacyStock stock = pharmacyStockRepository
                    .findByPharmacyLocationAndDrugAndBatchNumber(order.getPharmacyLocation(), line.getDrug(), line.getBatchNumber())
                    .orElseGet(() -> PharmacyStock.builder()
                            .pharmacyLocation(order.getPharmacyLocation())
                            .drug(line.getDrug())
                            .batchNumber(line.getBatchNumber())
                            .quantityOnHand(BigDecimal.ZERO)
                            .build());

            stock.setQuantityOnHand(stock.getQuantityOnHand().add(lineReq.getQuantityReturned()));
            pharmacyStockRepository.save(stock);

            StockMovement movement = StockMovement.builder()
                    .pharmacyLocation(order.getPharmacyLocation())
                    .drug(line.getDrug())
                    .movementType("return_from_patient")
                    .quantity(lineReq.getQuantityReturned())
                    .batchNumber(line.getBatchNumber())
                    .referenceType("DISPENSE_ORDER")
                    .referenceId(order.getId())
                    .requestedBy(actorUserId)
                    .notes(lineReq.getReason())
                    .build();
            stockMovementRepository.save(movement);
            if (integrationProperties.getEvents().isPublishEnabled()) {
                pharmacyDomainEventPublisher.publishStockChanged(
                        null,
                        order.getPharmacyLocation().getId(),
                        line.getDrug().getId(),
                        line.getBatchNumber(),
                        movement.getQuantity(),
                        movement.getMovementType(),
                        order.getId());
            }

            if (integrationProperties.getBilling().isPostReturnCreditsEnabled() && actorUserId != null) {
                try {
                    postReturnCreditCharge(order, line, lineReq.getQuantityReturned(), actorUserId, organizationId);
                } catch (Exception ex) {
                    log.warn("Return credit post failed for line {}: {}", line.getId(), ex.getMessage());
                }
            }
            if (integrationProperties.getEvents().isPublishEnabled()) {
                pharmacyDomainEventPublisher.publishDispenseLineReturned(
                        organizationId, order.getId(), line.getId(), lineReq.getQuantityReturned().toPlainString());
            }
        }

        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);
        return toResponse(order, lines);
    }

    private void postReturnCreditCharge(
            DispenseOrder order,
            DispenseLine line,
            java.math.BigDecimal quantityReturned,
            UUID actorUserId,
            UUID organizationId) {
        String idem = "pharmacy-return-" + order.getId() + "-line-" + line.getId() + "-q-" + quantityReturned.toPlainString();
        CreateChargePayload c = hospitalBillingClient.buildCharge(
                integrationProperties.getBilling().getSourceServiceName(),
                order.getPatientId(),
                order.getVisitId(),
                "RETURN-DRUG-" + line.getDrug().getId(),
                "Return: " + line.getDrug().getGenericName(),
                quantityReturned.negate(),
                integrationProperties.getBilling().getDefaultUnitPrice(),
                idem);
        hospitalBillingClient.postCharges(actorUserId, organizationId, List.of(c));
    }

    /**
     * Record a line as refused or out of stock without issuing stock (Phase P2 — WS-C3).
     */
    @Transactional
    public DispenseOrderResponse recordUnfulfilledLine(UUID orderId, DispenseUnfulfilledLineRequest request) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));
        order.setStatus(DispenseOrder.Status.IN_PROGRESS);
        dispenseOrderRepository.save(order);

        Drug drug = drugRepository.findById(request.getDrugId())
                .orElseThrow(() -> new IllegalArgumentException("Drug not found: " + request.getDrugId()));

        DispenseLine.Status st = DispenseLine.Status.valueOf(request.getLineStatus());
        if (st != DispenseLine.Status.OUT_OF_STOCK && st != DispenseLine.Status.REFUSED) {
            throw new IllegalArgumentException("lineStatus must be OUT_OF_STOCK or REFUSED");
        }

        DispenseLine line = DispenseLine.builder()
                .dispenseOrder(order)
                .prescriptionLineId(request.getPrescriptionLineId())
                .drug(drug)
                .batchNumber(null)
                .quantityPrescribed(request.getQuantityPrescribed())
                .quantityDispensed(BigDecimal.ZERO)
                .quantityReturned(BigDecimal.ZERO)
                .status(st)
                .reasonCode(request.getReasonCode())
                .documentingUserId(request.getDocumentingUserId())
                .remainingQuantity(request.getQuantityPrescribed())
                .build();
        dispenseLineRepository.save(line);

        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);
        return toResponse(order, lines);
    }

    @Transactional
    public DispenseOrderResponse updateStatus(UUID orderId, String status, UUID actorUserId, UUID organizationId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));

        DispenseOrder.Status st = DispenseOrder.Status.valueOf(status);
        if (st == DispenseOrder.Status.COMPLETED) {
            assertCompletionAllowed(order);
        }
        order.setStatus(st);
        if (st == DispenseOrder.Status.COMPLETED || st == DispenseOrder.Status.CANCELLED) {
            order.setCompletedAt(OffsetDateTime.now());
        }
        DispenseOrder saved = dispenseOrderRepository.save(order);
        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(saved);

        if (st == DispenseOrder.Status.COMPLETED) {
            if (integrationProperties.getBilling().isPostChargesEnabled()
                    && saved.getBillingPostedAt() == null
                    && saved.getPatientId() != null
                    && actorUserId != null) {
                try {
                    postBillingChargesForCompletion(saved, lines, actorUserId, organizationId);
                    saved.setBillingPostedAt(OffsetDateTime.now());
                    dispenseOrderRepository.save(saved);
                } catch (Exception ex) {
                    log.error("Billing post failed for order {}: {}", orderId, ex.getMessage());
                    throw new IllegalStateException("Failed to post charges: " + ex.getMessage(), ex);
                }
            }
            if (integrationProperties.getHospitalService().isInHouseFillEnabled()
                    && saved.getPrescriptionId() != null
                    && actorUserId != null) {
                try {
                    postInHouseFillSync(saved, lines, actorUserId, organizationId);
                } catch (Exception ex) {
                    log.error("In-house EHR fill sync failed for order {}: {}", orderId, ex.getMessage());
                }
            }
            pharmacyDomainEventPublisher.publishSaleCompleted(
                    organizationId,
                    saved.getId(),
                    saved.getPatientId(),
                    saved.getPrescriptionId(),
                    saved.getBillingPostedAt() != null ? "true" : "false");
            pharmacyDomainEventPublisher.publishDispenseOrderCompleted(
                    organizationId, saved.getId(), saved.getPatientId(), saved.getPrescriptionId());
        }
        if (st == DispenseOrder.Status.CANCELLED) {
            pharmacyDomainEventPublisher.publishSaleCancelled(organizationId, saved.getId());
        }

        return toResponse(saved, lines);
    }

    @Transactional
    public DispenseOrderResponse patchRegional(UUID orderId, PatchDispenseOrderRegionalRequest request) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));
        if (request.getPaperPrescriptionRef() != null) {
            order.setPaperPrescriptionRef(trimToNull(request.getPaperPrescriptionRef()));
        }
        if (Boolean.TRUE.equals(request.getClearPrescriptionImageAttachment())) {
            order.setPrescriptionImageAttachmentId(null);
        } else if (request.getPrescriptionImageAttachmentId() != null) {
            order.setPrescriptionImageAttachmentId(request.getPrescriptionImageAttachmentId());
        }
        if (request.getExternalValidationStatus() != null) {
            order.setExternalValidationStatus(ExternalValidationStatus.fromString(request.getExternalValidationStatus()));
        }
        DispenseOrder saved = dispenseOrderRepository.save(order);
        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(saved);
        return toResponse(saved, lines);
    }

    private void assertNotBlockedBySoftValidation(DispenseOrder order) {
        if (regionalProperties.isBlockOnSoftValidationFailure()
                && order.getExternalValidationStatus() == ExternalValidationStatus.FAILED_SOFT) {
            throw new IllegalArgumentException(
                    "External validation is FAILED_SOFT; resolve validation before recording dispense lines.");
        }
    }

    private void assertCompletionAllowed(DispenseOrder order) {
        if (regionalProperties.isBlockOnSoftValidationFailure()
                && order.getExternalValidationStatus() == ExternalValidationStatus.FAILED_SOFT) {
            throw new IllegalArgumentException(
                    "Cannot complete order: external validation is FAILED_SOFT.");
        }
    }

    private void assertRegionalRxEvidence(DispenseOrder order, Drug drug) {
        if (!regionalProperties.isRequireEhrPrescriptionForRxSkus() || !drug.isControlledDrugFlag()) {
            return;
        }
        if (order.getPrescriptionId() != null) {
            return;
        }
        boolean paper = order.getPaperPrescriptionRef() != null && !order.getPaperPrescriptionRef().isBlank();
        boolean img = order.getPrescriptionImageAttachmentId() != null;
        if (!paper && !img) {
            throw new IllegalArgumentException(
                    "Regional policy requires EHR prescription linkage or paper/attachment evidence for controlled (Rx) drugs.");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void postBillingChargesForCompletion(
            DispenseOrder order,
            List<DispenseLine> lines,
            UUID actorUserId,
            UUID organizationId) {
        List<CreateChargePayload> charges = new ArrayList<>();
        java.math.BigDecimal unit = integrationProperties.getBilling().getDefaultUnitPrice();
        String src = integrationProperties.getBilling().getSourceServiceName();
        for (DispenseLine line : lines) {
            if (line.getQuantityDispensed() == null || line.getQuantityDispensed().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (line.getStatus() == DispenseLine.Status.OUT_OF_STOCK
                    || line.getStatus() == DispenseLine.Status.REFUSED) {
                continue;
            }
            String idem = "pharmacy-charge-" + order.getId() + "-line-" + line.getId();
            charges.add(hospitalBillingClient.buildCharge(
                    src,
                    order.getPatientId(),
                    order.getVisitId(),
                    "DRUG-" + line.getDrug().getId(),
                    line.getDrug().getGenericName(),
                    line.getQuantityDispensed(),
                    unit,
                    idem));
        }
        if (!charges.isEmpty()) {
            hospitalBillingClient.postCharges(actorUserId, organizationId, charges);
        }
    }

    private void postInHouseFillSync(
            DispenseOrder order,
            List<DispenseLine> lines,
            UUID actorUserId,
            UUID organizationId) {
        boolean partial = lines.stream().anyMatch(l ->
                l.getStatus() == DispenseLine.Status.PARTIALLY_DISPENSED
                        || (l.getRemainingQuantity() != null
                        && l.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0));
        String fillStatus = partial ? "PARTIALLY_FILLED" : "FILLED";
        LocalDateTime now = LocalDateTime.now();
        List<InHouseDispenseFillPayload.Line> fillLines = lines.stream()
                .filter(l -> l.getPrescriptionLineId() != null && l.getQuantityDispensed() != null)
                .map(l -> InHouseDispenseFillPayload.Line.builder()
                        .prescriptionMedicationId(l.getPrescriptionLineId())
                        .quantityDispensed(l.getQuantityDispensed())
                        .build())
                .collect(Collectors.toList());
        InHouseDispenseFillPayload payload = InHouseDispenseFillPayload.builder()
                .prescriptionId(order.getPrescriptionId())
                .dispenseOrderId(order.getId())
                .idempotencyKey("inhouse-fill-" + order.getId())
                .fillStatus(fillStatus)
                .fillStatusDate(now)
                .filledDate(now)
                .fillStatusMessage("In-house dispense order completed")
                .lines(fillLines)
                .build();
        hospitalServiceClient.postInHouseDispenseFill(actorUserId, organizationId, payload);
    }

    private DispenseOrderResponse toResponse(DispenseOrder order, List<DispenseLine> lines) {
        List<DispenseLineResponse> lineResponses = lines.stream()
                .map(this::toLineResponse)
                .collect(Collectors.toList());

        return DispenseOrderResponse.builder()
                .id(order.getId())
                .prescriptionId(order.getPrescriptionId())
                .visitId(order.getVisitId())
                .patientId(order.getPatientId())
                .pharmacyLocationId(order.getPharmacyLocation().getId())
                .pharmacyLocationName(order.getPharmacyLocation().getName())
                .status(order.getStatus())
                .contextType(order.getContextType())
                .departmentId(order.getDepartmentId())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .paperPrescriptionRef(order.getPaperPrescriptionRef())
                .prescriptionImageAttachmentId(order.getPrescriptionImageAttachmentId())
                .externalValidationStatus(order.getExternalValidationStatus() != null ? order.getExternalValidationStatus().name() : null)
                .lines(lineResponses)
                .build();
    }

    private DispenseLineResponse toLineResponse(DispenseLine line) {
        return DispenseLineResponse.builder()
                .id(line.getId())
                .dispenseOrderId(line.getDispenseOrder().getId())
                .prescriptionLineId(line.getPrescriptionLineId())
                .drugId(line.getDrug().getId())
                .drugGenericName(line.getDrug().getGenericName())
                .drugBrandName(line.getDrug().getBrandName())
                .batchNumber(line.getBatchNumber())
                .quantityPrescribed(line.getQuantityPrescribed())
                .quantityDispensed(line.getQuantityDispensed())
                .quantityReturned(line.getQuantityReturned())
                .status(line.getStatus())
                .reasonCode(line.getReasonCode())
                .documentingUserId(line.getDocumentingUserId())
                .overrideReasonCode(line.getOverrideReasonCode())
                .substitutedDrugId(line.getSubstitutedDrug() != null ? line.getSubstitutedDrug().getId() : null)
                .formularyOverrideReason(line.getFormularyOverrideReason())
                .overrideApproverId(line.getOverrideApproverId())
                .witnessUserId(line.getWitnessUserId())
                .clinicalSafetyOverrideReason(line.getClinicalSafetyOverrideReason())
                .remainingQuantity(line.getRemainingQuantity())
                .createdAt(line.getCreatedAt())
                .updatedAt(line.getUpdatedAt())
                .build();
    }
}
