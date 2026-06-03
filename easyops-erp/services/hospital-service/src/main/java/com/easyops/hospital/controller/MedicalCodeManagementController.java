package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.MedicalCodeUpsertRequest;
import com.easyops.hospital.dto.response.CodeSuggestionResponse;
import com.easyops.hospital.dto.response.MedicalCodePageResponse;
import com.easyops.hospital.dto.response.MedicalCodeResponse;
import com.easyops.hospital.service.CodeLookupService;
import com.easyops.hospital.service.MedicalCodeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-codes")
@RequiredArgsConstructor
@Tag(name = "Medical Code Management", description = "View and manage ICD-10 and ICD-11 catalogs")
public class MedicalCodeManagementController {

    private final MedicalCodeManagementService medicalCodeManagementService;
    private final CodeLookupService codeLookupService;

    @GetMapping("/icd10/suggestions")
    @Operation(summary = "Search ICD-10 code suggestions", description = "Returns active ICD-10 codes matching the search term for clinical autocomplete.")
    public ResponseEntity<List<CodeSuggestionResponse>> suggestIcd10Codes(
        @RequestParam String searchTerm,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(codeLookupService.searchIcd10(searchTerm, limit));
    }

    @GetMapping("/icd11/suggestions")
    @Operation(summary = "Search ICD-11 code suggestions", description = "Returns active ICD-11 codes matching the search term for clinical autocomplete.")
    public ResponseEntity<List<CodeSuggestionResponse>> suggestIcd11Codes(
        @RequestParam String searchTerm,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(codeLookupService.searchIcd11(searchTerm, limit));
    }

    @GetMapping("/snomed/suggestions")
    @Operation(summary = "Search SNOMED CT code suggestions", description = "Returns active SNOMED CT codes matching the search term for clinical autocomplete.")
    public ResponseEntity<List<CodeSuggestionResponse>> suggestSnomedCodes(
        @RequestParam String searchTerm,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(codeLookupService.searchSnomed(searchTerm, limit));
    }

    @GetMapping("/icd10")
    @Operation(summary = "List ICD-10 codes", description = "Returns active ICD-10 codes with optional search and pagination.")
    public ResponseEntity<MedicalCodePageResponse> getIcd10Codes(
        @RequestParam(required = false) String searchTerm,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(medicalCodeManagementService.getIcd10Codes(searchTerm, page, size, includeInactive));
    }

    @GetMapping("/icd11")
    @Operation(summary = "List ICD-11 codes", description = "Returns ICD-11 codes with optional search, pagination, and inactive filter.")
    public ResponseEntity<MedicalCodePageResponse> getIcd11Codes(
        @RequestParam(required = false) String searchTerm,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(medicalCodeManagementService.getIcd11Codes(searchTerm, page, size, includeInactive));
    }

    @PostMapping("/icd10")
    @Operation(summary = "Create/update ICD-10 code", description = "Upserts an ICD-10 code record by code.")
    public ResponseEntity<?> upsertIcd10Code(@Valid @RequestBody MedicalCodeUpsertRequest request) {
        try {
            MedicalCodeResponse response = medicalCodeManagementService.upsertIcd10Code(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/icd11")
    @Operation(summary = "Create/update ICD-11 code", description = "Upserts an ICD-11 code record by code.")
    public ResponseEntity<?> upsertIcd11Code(@Valid @RequestBody MedicalCodeUpsertRequest request) {
        try {
            MedicalCodeResponse response = medicalCodeManagementService.upsertIcd11Code(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/icd10/{code}")
    @Operation(summary = "Deactivate ICD-10 code", description = "Marks an ICD-10 code as inactive.")
    public ResponseEntity<?> deactivateIcd10Code(@PathVariable String code) {
        try {
            medicalCodeManagementService.deactivateIcd10Code(code);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/icd11/{code}")
    @Operation(summary = "Deactivate ICD-11 code", description = "Marks an ICD-11 code as inactive.")
    public ResponseEntity<?> deactivateIcd11Code(@PathVariable String code) {
        try {
            medicalCodeManagementService.deactivateIcd11Code(code);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }
}
