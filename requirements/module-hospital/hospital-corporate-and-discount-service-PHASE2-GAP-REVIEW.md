# Phase 2 Implementation – Gap Review

Review of Phase 2 (Coverage Rules, Packages, and Tariffs) against `hospital-corporate-and-discount-service-implementation-plan.md`.

---

## Summary

| Area | Status | Notes |
|------|--------|--------|
| 2.1 Database (Liquibase) | OK | 021 has coverage_rules, packages, package_items, corporate_tariffs; indexes and FKs match §4.3–4.6. |
| 2.2 Backend – Coverage context | OK | CoverageRule, Repository, Service, Controller, DTOs; paths match plan. |
| 2.3 Backend – Package context | OK | Package/PackageItem, PackageService, PackageController, DTOs; paths match plan. |
| 2.4 Backend – Tariff context | OK | CorporateTariff, Service, Controller, DTOs; paths match plan. |
| **2.5 Backend – Coverage evaluation** | **GAP** | **`CoverageEvaluationService` is missing.** Controller and DTOs exist; service implementation absent → startup failure. |
| 2.6 Frontend – Service | OK | coverage rules, packages, items, tariffs, evaluateCoverage in hospitalCorporateDiscountService. |
| 2.7 Frontend – Coverage and tariffs UI | OK | ContractCoverage.tsx at contracts/:contractId/coverage; rules and tariffs list/add/delete. |
| 2.8 Frontend – Packages UI | OK | Packages.tsx list/filter/create/edit; PackageDetail.tsx items add/remove; route and nav. |

---

## Gap Detail: 2.5 Coverage Evaluation Service

**Requirement (plan §2.5):**

- **Service:** `CoverageEvaluationService` – load contract and its coverage rules; for each request item, resolve matching rule (by service_code, then service_group, then department; consider applicable_visit_types); compute covered amount/percent, co-pay, deductible; aggregate totals.
- **Controller:** `POST /coverage/evaluate` → `EvaluateCoverageResponse`.

**Current state:**

- `CoverageEvaluationController` exists and injects `CoverageEvaluationService` (package `com.easyops.hospitalcorporatediscount.domain.coverage`).
- `EvaluateCoverageRequest`, `EvaluateCoverageResponse`, `EvaluateCoverageItemRequest`, `EvaluateCoverageItemResponse` exist and match §5.2.
- **No class implements `CoverageEvaluationService`** in `domain.coverage` (or elsewhere). The application cannot start without this bean.

**Required implementation:**

1. Add `CoverageEvaluationService` in `com.easyops.hospitalcorporatediscount.domain.coverage`:
   - Load contract (validate exists) and coverage rules via `CoverageRuleService.listByContractId` (or repository).
   - For each request item, resolve one matching rule in order: by `SERVICE_CODE` (scope_value = item.serviceCode), then `SERVICE_GROUP` (scope_value = item.serviceGroupId), then `DEPARTMENT` (scope_value = item.departmentId); filter by `applicable_visit_types` when request.visitType is set.
   - For the matched rule: compute covered amount from coverage_percent and max_amount; apply co_pay_percent and deductible_amount for patient share; corporate share = covered amount − patient share; set line totals and ruleId.
   - If no rule matches, return zero coverage for that line.
   - Aggregate totals: totalCovered, totalPatientShare, totalCorporateShare.
   - Return `EvaluateCoverageResponse` with items (lineIndex, serviceCode, coveredPercent, coveredAmount, patientShare, corporateShare, maxApplicable, ruleId) and totals.

---

## Other Notes (No Gaps)

- **Repository method name:** Plan says “findByCorporateContractId”; implementation uses `findByCorporateContractIdOrderByScopeTypeAscScopeValueAsc`. Same semantic (list by contract); ordering is an improvement.
- **Frontend:** No UI in this module is required to call `evaluateCoverage`; billing service or billing UI will call it. The method is present in the frontend service for any future use or for billing UI in another module.

---

## Recommendation

Implement `CoverageEvaluationService` in `domain.coverage` as described above so that Phase 2 is complete and the service starts successfully.

---

## Update: Gap Fixed

`CoverageEvaluationService` has been implemented at  
`services/hospital-corporate-and-discount-service/src/main/java/com/easyops/hospitalcorporatediscount/domain/coverage/CoverageEvaluationService.java`.

- Validates contract exists; loads coverage rules via `CoverageRuleService.listByContractId` (cacheable).
- For each request item: finds first matching rule in order SERVICE_CODE → SERVICE_GROUP → DEPARTMENT; filters by `applicable_visit_types` when `visitType` is set.
- Computes per line: covered amount (from coverage percent, capped by max amount), patient share (deductible + co-pay on remainder), corporate share; sets `lineIndex`, `serviceCode`, `coveredPercent`, `coveredAmount`, `patientShare`, `corporateShare`, `maxApplicable`, `ruleId`.
- Aggregates `totalCovered`, `totalPatientShare`, `totalCorporateShare`.
- Service compiles and satisfies the Phase 2.5 requirement.
