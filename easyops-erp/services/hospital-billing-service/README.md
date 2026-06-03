# Hospital Billing Service

Spring Boot service for hospital billing: charges, invoices, payments, and estimates.

## Corporate and discount integration (Phase 3.2)

Estimate and discount behaviour is driven by **hospital-corporate-and-discount-service** when that service is available. Until it is deployed:

- **Stub behaviour**: A stub client (`StubDiscountRulesClient`) returns zero discounts. Estimates show gross = net; no discount breakdown.
- **Configuration**: `hospital.billing.discount-service.enabled` defaults to `false`. Leave it false until the corporate service is running.
- **When the corporate service is available**:
  1. Deploy **hospital-corporate-and-discount-service** and expose `POST /api/hospital-corporate-discount/discounts/evaluate` with the contract defined in `EvaluateDiscountsRequest` / `EvaluateDiscountsResponse` (see `integration.dto` package).
  2. Set `hospital.billing.discount-service.enabled: true` for this service.
  3. The Feign client `CorporateDiscountServiceApi` will call the corporate service; `FeignDiscountRulesClient` applies returned discounts to estimate lines. On service failure, the client falls back to zero discounts and logs a warning.

Integration types:

- **DiscountRulesClient**: abstraction used by `InvoiceService.computeEstimate()`.
- **StubDiscountRulesClient**: used when the corporate service is not enabled (default).
- **FeignDiscountRulesClient** + **CorporateDiscountServiceApi**: used when `hospital.billing.discount-service.enabled=true`.

See `requirements/module-hospital/hospital-corporate-and-discount-service-implementation-plan.md` for the corporate service API (e.g. `POST /discounts/evaluate`).
