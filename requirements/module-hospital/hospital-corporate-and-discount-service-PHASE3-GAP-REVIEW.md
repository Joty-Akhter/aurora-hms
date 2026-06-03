# Phase 3 Implementation – Gap Review

Review of Phase 3 (Discount Schemes and Approvals) against `hospital-corporate-and-discount-service-implementation-plan.md`.

---

## Summary

| Area | Status | Notes |
|------|--------|--------|
| 3.1 Database (Liquibase) | OK | 022 has discount_schemes, discount_approval_levels, discount_decisions; indexes and FKs match §4.7–4.9. |
| 3.2 Backend – Discount scheme context | OK | Entities, repositories, DTOs, DiscountSchemeService, DiscountDecisionService, both controllers; paths match plan. |
| 3.3 Backend – Discount evaluation | OK | EvaluateDiscountsRequest/Response, item request, ApplicableSchemeDto; DiscountEvaluationService and controller; logic matches plan. |
| 3.4 Frontend – Service | OK | All scheme, approval-level, evaluate, and decision methods and types in hospitalCorporateDiscountService. |
| 3.5 Frontend – Discount schemes page | OK | DiscountSchemes.tsx (list, filters, create, edit, manage levels); SchemeApprovalLevels.tsx (list, add, remove); route and nav. |
| 3.6 Frontend – Discount decisions | OK | GET /discount-decisions (page, size); getDiscountDecisions; DiscountDecisions.tsx (list, view); route and nav. |

**Result: No gaps found.** Phase 3 implementation matches the plan.

---

## Verification Detail

### 3.1 Database

- **022-hospital-corporate-discount-discount-schemes.sql:** Tables `discount_schemes`, `discount_approval_levels`, `discount_decisions` with columns and types per §4.7–4.9.
- Indexes: code, corporate_client_id, status, valid_from, valid_to (schemes); discount_scheme_id (levels); bill_context_id, patient_id, created_at (decisions).
- FKs: approval_levels → discount_schemes; discount_decisions.discount_scheme_id → discount_schemes; discount_schemes.corporate_client_id → corporate_clients.

### 3.2 Backend – Discount scheme context

- **Package:** `com.easyops.hospitalcorporatediscount.domain.discount`.
- **Entities:** DiscountScheme, DiscountApprovalLevel, DiscountDecision mapped to the three tables.
- **Repositories:**
  - DiscountSchemeRepository: findByCode, existsByCode, JpaSpecificationExecutor (used for code/corporateClientId/status in DiscountSchemeService.list).
  - DiscountApprovalLevelRepository: findByDiscountSchemeIdOrderBySortOrderAsc, existsByDiscountSchemeIdAndId.
  - DiscountDecisionRepository: findByBillContextId, findByPatientIdOrderByCreatedAtDesc (with and without Pageable), findById (JpaRepository); plus reporting queries.
- **DTOs:** CreateDiscountSchemeRequest, UpdateDiscountSchemeRequest, DiscountSchemeResponse, DiscountSchemeDetailResponse (with approvalLevels); CreateApprovalLevelRequest, DiscountApprovalLevelResponse; CreateDiscountDecisionRequest, DiscountDecisionResponse. Fields align with §5.3.
- **Controllers:** DiscountSchemeController (POST/GET/PATCH discount-schemes, POST/GET approval-levels, DELETE approval-level); DiscountDecisionController (GET list with page/size, POST create, GET by id). Paths match plan.

### 3.3 Backend – Discount evaluation

- **EvaluateDiscountsRequest:** patientId, visitId, corporateClientId, visitType, departmentId, items (serviceCode, quantity, unitPrice, departmentId?), requestedSchemeId, requestedDiscountPercent, requestedDiscountAmount, reason. Matches plan.
- **EvaluateDiscountsResponse:** applicableSchemes, recommendedTotalDiscount, requiresApproval, message. **ApplicableSchemeDto:** schemeId, schemeCode, recommendedPercent, recommendedAmount, cappedAmount, requiresApproval, requiredApprovalLevel. Matches plan.
- **DiscountEvaluationService:** Loads active schemes via getActiveSchemesForEvaluation (activeAndValidNow, corporateOrGeneral, visitTypeMatches, departmentMatches); filters by serviceCode and patientCategory in memory; supports requestedSchemeId; computes PERCENT/AMOUNT with max_discount_amount and max_discount_percent caps; sets requiredApprovalLevel from first approval level when requiresApproval; recommendedTotalDiscount = max(cappedAmount). Aligns with plan §3.3.
- **DiscountEvaluationController:** POST `/discounts/evaluate` → EvaluateDiscountsResponse.

### 3.4 Frontend – Service

- Methods present: createDiscountScheme, getDiscountScheme, getDiscountSchemes, updateDiscountScheme, addApprovalLevel, getApprovalLevels, deleteApprovalLevel, evaluateDiscounts, createDiscountDecision, getDiscountDecisions, getDiscountDecision. Types exported for requests/responses and PagedResponse.

### 3.5 Frontend – Discount schemes page

- **DiscountSchemes.tsx:** List with code, name, corporate, discount type/value, requires approval, status, valid from/to; filters code, corporateClientId, status; Create scheme, Edit scheme; actions View, Edit, Manage approval levels.
- **SchemeApprovalLevels.tsx** at `/hospital/corporate-discount/discount-schemes/:schemeId`: List approval levels (sort order, role/group, max discount %, max discount amount); Add level; Remove level.
- Route and nav item “Corporate & Discount – Discount schemes” in App.tsx and MainLayout.tsx.

### 3.6 Frontend – Discount decisions

- **Backend:** GET `/discount-decisions` with page, size; DiscountDecisionService.list(page, size) returns PagedResponse.
- **Frontend:** getDiscountDecisions({ page, size }); DiscountDecisions.tsx lists bill context, patient ID, scheme ID, discount amount, discount %, decided by, approved by, created at, approved at; View action and detail dialog.
- Route and nav “Corporate & Discount – Decisions” present.

---

## Minor Notes (non-gaps)

- **DiscountApprovalLevelResponse:** Plan §5.3 lists `createdAt`; implementation also includes `updatedAt`. Superset of plan, acceptable.
- **requestedDiscountPercent / requestedDiscountAmount / reason:** In EvaluateDiscountsRequest but not used in evaluation logic. They are available for billing UI or decision creation; evaluate correctly returns applicable schemes and amounts. No change required for Phase 3.

---

## Conclusion

Phase 3 is complete and aligned with the implementation plan. No changes required.
