package com.easyops.hospital.controller;

import com.easyops.hospital.service.EpLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read-only reference data for EP (Easy Prescription) dropdowns and suggestion lists.
 * Returns values stored in {@code ehr.ep_lookup_items}, grouped by category.
 *
 * <p>Known categories: DOSAGE_FORM, DISEASE_CATEGORY, FREQUENCY, INSTRUCTION,
 * REFERRAL, COMPLAINT, MEDICATION, ADVICE, TEST.</p>
 */
@RestController
@RequestMapping("/api/easy-prescription/lookups")
@RequiredArgsConstructor
@Tag(name = "EP Lookups", description = "Configurable reference/lookup data for Easy Prescription dropdowns and suggestion lists")
public class EpLookupController {

    private final EpLookupService epLookupService;

    @GetMapping
    @Operation(summary = "Get all EP lookup categories",
               description = "Returns all active lookup items grouped by category. Keys are category names (DOSAGE_FORM, DISEASE_CATEGORY, FREQUENCY, INSTRUCTION, REFERRAL, COMPLAINT, MEDICATION, ADVICE, TEST).")
    public ResponseEntity<Map<String, List<String>>> getAllLookups() {
        return ResponseEntity.ok(epLookupService.getAllGrouped());
    }

    @GetMapping("/{category}")
    @Operation(summary = "Get lookup values for a specific category",
               description = "Returns the ordered list of active values for the given category (case-insensitive).")
    public ResponseEntity<List<String>> getLookupByCategory(@PathVariable String category) {
        return ResponseEntity.ok(epLookupService.getByCategory(category));
    }
}
