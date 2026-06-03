package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.StockAdjustmentApprovalDecisionRequest;
import com.easyops.hospitalpharmacy.dto.request.StockAdjustmentRequest;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentApprovalResponse;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.entity.StockAdjustmentApproval;
import com.easyops.hospitalpharmacy.repository.PharmacyLocationRepository;
import com.easyops.hospitalpharmacy.repository.StockAdjustmentApprovalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAdjustmentApprovalService {

    private final StockAdjustmentApprovalRepository approvalRepository;
    private final PharmacyLocationRepository locationRepository;
    private final PharmacyStockService pharmacyStockService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StockAdjustmentApprovalResponse submitForApproval(UUID pharmacyLocationId, StockAdjustmentRequest request, UUID requestedBy) {
        PharmacyLocation location = locationRepository.findById(pharmacyLocationId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + pharmacyLocationId));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize adjustment request", e);
        }

        StockAdjustmentApproval approval = StockAdjustmentApproval.builder()
                .pharmacyLocation(location)
                .requestedBy(requestedBy)
                .requestPayload(payload)
                .status(StockAdjustmentApproval.AdjustmentApprovalStatus.PENDING_APPROVAL)
                .build();
        return toResponse(approvalRepository.save(approval));
    }

    @Transactional
    public StockAdjustmentApprovalResponse decide(UUID approvalId, StockAdjustmentApprovalDecisionRequest request, UUID reviewedBy) {
        StockAdjustmentApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Stock adjustment approval not found: " + approvalId));

        if (approval.getStatus() != StockAdjustmentApproval.AdjustmentApprovalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Approval is not in PENDING_APPROVAL state");
        }

        approval.setReviewedBy(reviewedBy);
        approval.setReviewedAt(OffsetDateTime.now());

        if ("APPROVED".equalsIgnoreCase(request.getDecision())) {
            StockAdjustmentRequest adjustmentRequest;
            try {
                adjustmentRequest = objectMapper.readValue(approval.getRequestPayload(), StockAdjustmentRequest.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize adjustment request", e);
            }
            adjustmentRequest.setApprovedByUserId(reviewedBy);
            pharmacyStockService.adjustStock(approval.getPharmacyLocation().getId(), adjustmentRequest, approval.getRequestedBy());
            approval.setStatus(StockAdjustmentApproval.AdjustmentApprovalStatus.APPROVED);
        } else if ("REJECTED".equalsIgnoreCase(request.getDecision())) {
            approval.setStatus(StockAdjustmentApproval.AdjustmentApprovalStatus.REJECTED);
            approval.setRejectionReason(request.getRejectionReason());
        } else {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }

        return toResponse(approvalRepository.save(approval));
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentApprovalResponse> listPending() {
        return approvalRepository.findByStatusOrderByCreatedAtDesc(StockAdjustmentApproval.AdjustmentApprovalStatus.PENDING_APPROVAL)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentApprovalResponse> listPendingByLocation(UUID pharmacyLocationId) {
        PharmacyLocation location = locationRepository.findById(pharmacyLocationId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy location not found: " + pharmacyLocationId));
        return approvalRepository.findByPharmacyLocationAndStatusOrderByCreatedAtDesc(
                        location, StockAdjustmentApproval.AdjustmentApprovalStatus.PENDING_APPROVAL)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private StockAdjustmentApprovalResponse toResponse(StockAdjustmentApproval approval) {
        return StockAdjustmentApprovalResponse.builder()
                .id(approval.getId())
                .pharmacyLocationId(approval.getPharmacyLocation().getId())
                .requestedBy(approval.getRequestedBy())
                .status(approval.getStatus().name())
                .requestPayload(approval.getRequestPayload())
                .reviewedBy(approval.getReviewedBy())
                .reviewedAt(approval.getReviewedAt())
                .rejectionReason(approval.getRejectionReason())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .build();
    }
}
