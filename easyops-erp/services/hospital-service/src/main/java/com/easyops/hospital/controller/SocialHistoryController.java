package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.SocialHistoryRequest;
import com.easyops.hospital.dto.response.SocialHistoryResponse;
import com.easyops.hospital.service.MedicalHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/social-history")
@RequiredArgsConstructor
@Tag(name = "Social History Management", description = "APIs for managing patient social history")
public class SocialHistoryController {
    
    private final MedicalHistoryService medicalHistoryService;
    
    @PostMapping
    @Operation(summary = "Add social history to patient")
    public ResponseEntity<SocialHistoryResponse> createSocialHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody SocialHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        SocialHistoryResponse response = medicalHistoryService.createSocialHistory(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all social history for a patient")
    public ResponseEntity<List<SocialHistoryResponse>> getSocialHistory(@PathVariable UUID patientId) {
        List<SocialHistoryResponse> responses = medicalHistoryService.getSocialHistoryByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{socialHistoryId}")
    @Operation(summary = "Update social history")
    public ResponseEntity<SocialHistoryResponse> updateSocialHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID socialHistoryId,
            @Valid @RequestBody SocialHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        SocialHistoryResponse response = medicalHistoryService.updateSocialHistory(socialHistoryId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{socialHistoryId}")
    @Operation(summary = "Delete social history")
    public ResponseEntity<Void> deleteSocialHistory(@PathVariable UUID socialHistoryId) {
        medicalHistoryService.deleteSocialHistory(socialHistoryId);
        return ResponseEntity.noContent().build();
    }
}
