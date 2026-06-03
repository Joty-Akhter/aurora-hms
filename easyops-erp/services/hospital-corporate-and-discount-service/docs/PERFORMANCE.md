# Performance and Caching

## Latency targets

- **`POST /api/hospital-corporate-discount/coverage/evaluate`**: p95 &lt; 300–400 ms.
- **`POST /api/hospital-corporate-discount/discounts/evaluate`**: p95 &lt; 300–400 ms.

These endpoints are used at bill creation and should stay within the above ranges under normal load.

## Caching (Phase 4.3)

The service uses **Caffeine** in-process caches (configured in `CacheConfig`) to reduce database load and latency on evaluation and read paths.

| Cache | Key | TTL / eviction | Use |
|-------|-----|----------------|-----|
| `coverageRules` | `contractId` | 60 s TTL; evicted on create/delete of rules for that contract | `GET .../contracts/{id}/coverage-rules`, coverage evaluation |
| `discountScheme` | `schemeId` | 60 s TTL; evicted on scheme update and approval-level add/delete | `GET .../discount-schemes/{id}` |
| `activeDiscountSchemes` | `corporateClientId \| visitType \| departmentId` | 60 s TTL; full cache cleared on scheme create/update/approval-level change | `POST .../discounts/evaluate` |

- **Eviction on write**: Coverage rules cache is evicted per contract when rules are created or deleted. Discount scheme and active-schemes caches are evicted when schemes or approval levels are created, updated, or deleted.
- **TTL**: 60 seconds for all caches so that periodic changes are picked up even if an eviction was missed.
- **Size**: Each cache is bounded (e.g. max 500 entries) to limit memory use.

To switch to Redis or another cache provider, replace the `CacheManager` bean in `CacheConfig` and keep the same cache names so that `@Cacheable` / `@CacheEvict` annotations continue to work.

## Database indexes

Indexes are defined in the hospital Liquibase changesets and support the evaluation and reporting queries:

- **Coverage**: `idx_hcd_coverage_rules_contract` on `coverage_rules(corporate_contract_id)`; unique `idx_hcd_coverage_rules_contract_scope` for (contract, scope_type, scope_value).
- **Discount schemes**: `idx_hcd_discount_schemes_code`, `idx_hcd_discount_schemes_corporate_client_id`, `idx_hcd_discount_schemes_status`, `idx_hcd_discount_schemes_valid_from`, `idx_hcd_discount_schemes_valid_to`.
- **Discount decisions (reporting)**: `idx_hcd_discount_decisions_bill_context_id`, `idx_hcd_discount_decisions_patient_id`, `idx_hcd_discount_decisions_created_at`.

If p95 grows above target, check slow-query logs and consider additional indexes on filters used in list/report queries (e.g. date ranges, corporate id, scheme id).
