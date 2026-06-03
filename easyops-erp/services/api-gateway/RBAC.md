# API Gateway and RBAC — policy (Phase 3)

## Single source of truth

- **Authorization decisions** (which permission codes apply to which operations) live in **each microservice** and in **PostgreSQL** (`rbac.permissions`, role links). The gateway does **not** duplicate the full catalog.
- **`rbac-service`** is the authority for **who has which permissions**; clients and downstream services resolve effective permissions from there (or JWT claims where applicable).

## What the gateway does today

- **Routing** — Spring Cloud Gateway forwards paths (e.g. `/api/hospital/**`) to the correct service via Eureka.
- **Authentication** — JWT validation is intended to run at the gateway (`JwtAuthenticationFilter`); when enabled, unauthenticated requests are rejected before routing. **Per-permission checks are not implemented in the gateway** in the current codebase.

## Duplication policy (gateway vs services)

| Layer | Responsibility |
|-------|----------------|
| **Gateway** | Authenticate (JWT), optional future **coarse** route rules if explicitly added and documented. |
| **Downstream service** | **Authorize** every sensitive endpoint using the user’s effective permissions (by code or resource/action as designed). |

**Do not** maintain a second, parallel permission catalog only in the gateway. If a **route → required permission** map is introduced later, it must:

1. Reference the **same** permission **codes** as `rbac.permissions` (no invented strings).
2. Be documented here and in [`easyops-erp/rbac/README.md`](../../rbac/README.md).
3. Remain **optional** or **defense-in-depth** — services still enforce.

## Optional future configuration

A commented placeholder may exist in `application.yml` (e.g. `gateway.rbac.*`) for a future map or feature flags. Until implemented, **ignore** those keys.

## References

- Requirements: [`implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md`](../../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md) §16.5 Phase 3.
- RBAC catalog and seeds: [`easyops-erp/rbac/README.md`](../../rbac/README.md).
