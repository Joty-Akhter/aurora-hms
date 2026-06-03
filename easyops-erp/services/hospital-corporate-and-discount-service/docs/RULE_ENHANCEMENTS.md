# Rule Enhancements (Time-Bound Validity and Tiered Discounts)

This document describes the current support for time-bound validity and the optional extension for tiered discounts, as referenced in Phase 4.4 of the implementation plan.

---

## Time-bound validity (current)

### Already implemented

- **Discount schemes**  
  `discount_schemes` has `valid_from` and `valid_to` (DATE, nullable).  
  - **API/DTOs:** `CreateDiscountSchemeRequest`, `UpdateDiscountSchemeRequest`, `DiscountSchemeResponse`, and `DiscountSchemeDetailResponse` expose `validFrom` and `validTo` (Java `LocalDate`).  
  - **Behaviour:** Only schemes with `status = 'ACTIVE'` and with today within `[valid_from, valid_to]` (or `valid_to` null = open-ended) are considered in `POST /discounts/evaluate` (see `DiscountSchemeSpecifications.activeAndValidNow()`).

- **Corporate contracts**  
  `corporate_contracts` has `valid_from` (required) and `valid_to` (optional).  
  - **API/DTOs:** Contract create/update and response DTOs include `validFrom` and `validTo`.  
  - **Behaviour:** Used for contract lifecycle and listing by effective status (ACTIVE / EXPIRED / FUTURE).

- **Corporate clients**  
  `corporate_clients` has optional `valid_from` and `valid_to` for client-level validity if needed.

### Not implemented (optional extension)

- **Coverage rules**  
  `coverage_rules` has no `valid_from` / `valid_to`. Rules are effective from creation until explicitly deleted.  
  - **Possible extension:** Add `valid_from` and `valid_to` (DATE, nullable) to `coverage_rules`, and in coverage evaluation (and `GET /contracts/{id}/coverage-rules`) filter by “today within [valid_from, valid_to]”.  
  - **API/DTOs:** Would add `validFrom` / `validTo` to `CreateCoverageRuleRequest`, `CoverageRuleResponse`, and any coverage evaluation logic that loads rules.

---

## Tiered discounts (optional extension, not implemented)

Tiered discounts would allow different discount rates or caps depending on thresholds (e.g. bill amount bands or quantity tiers). This is **not** implemented; below is a suggested shape for API and data model when required.

### Option A: Separate table

- **Table:** e.g. `discount_scheme_tiers`  
  - `id`, `discount_scheme_id` (FK), `sort_order`, `min_amount` (or `min_quantity`), `max_amount` (or `max_quantity`), `discount_type`, `discount_value`, `max_discount_amount`, `max_discount_percent`.
- **Behaviour:** For a given scheme, evaluation picks the tier whose range contains the bill total (or quantity), then applies that tier’s discount rules.
- **API:**  
  - Create/update scheme could accept `tiers: [{ minAmount, maxAmount, discountType, discountValue, ... }]`.  
  - Response DTOs would include `tiers: List<DiscountTierDto>`.  
  - If no tier matches, fall back to scheme-level `discountValue` / `maxDiscountAmount` / `maxDiscountPercent` (current behaviour).

### Option B: JSON column on discount_schemes

- **Column:** e.g. `tiers JSONB` on `discount_schemes` storing an array of tier objects (same fields as above).
- **Behaviour:** Same as Option A; evaluation reads the JSON and selects the matching tier.
- **API:** Same DTO shape; backend maps JSON to/from `List<DiscountTierDto>`.

### Suggested DTO shape (for future use)

```text
DiscountTierDto / CreateDiscountTierRequest:
  - minAmount: BigDecimal (optional; null = no lower bound)
  - maxAmount: BigDecimal (optional; null = no upper bound)
  - discountType: String ("PERCENT" | "FIXED")
  - discountValue: BigDecimal
  - maxDiscountAmount: BigDecimal (optional)
  - maxDiscountPercent: BigDecimal (optional)
  - sortOrder: int (for deterministic tier selection)
```

Evaluation would: (1) resolve applicable scheme(s) as today; (2) for each scheme, if tiers present, select the tier whose range contains the bill total and apply that tier’s discount; otherwise use the scheme’s top-level discount fields.

No database or API changes have been made for tiered discounts; this document is the placeholder for when the feature is required.
