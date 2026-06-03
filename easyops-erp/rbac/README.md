# RBAC — permissions (database source of truth)

## Authoritative store

**PostgreSQL table `rbac.permissions`** (and `rbac.role_permissions`, `rbac.roles`) is the **only** runtime source of truth. Permission rows are created and updated via:

- **Liquibase** in **`database-versioning`** (platform-wide seeds), **each domain service** (e.g. `hospital-service`, `pharma-service` for module-specific RBAC — see table below), and/or  
- **Admin UI** / API in `rbac-service` (`PermissionController`).

Do **not** embed module-specific permission **codes** in application source (no enums, no `constants/permissions.ts` lists). The frontend receives permissions from **`rbac-service`** through **`AuthContext`** (`getUserPermissions` → `hasPermission(permission.code)`).

## Where permission rows are seeded (full picture)

Most ERP modules already have **view/manage** (or equivalent) pairs in **`database-versioning`**. Only **hospital** and **pharma** use **service-owned** changelogs for extra module-specific rows (per deployment choice).

### Platform & core modules — `database-versioning`

| File | What it inserts into `rbac.permissions` |
|------|------------------------------------------|
| [`changelog/data/002-default-permissions.sql`](../database-versioning/changelog/data/002-default-permissions.sql) | `USER_*`, `ROLE_*`, `SYSTEM_*`, `AUDIT_VIEW`, `ORG_MANAGE`, … |
| [`changelog/data/011-rbac-module-permissions.sql`](../database-versioning/changelog/data/011-rbac-module-permissions.sql) | `DASHBOARD_VIEW`, `ORG_VIEW`, `PERMISSION_*`, **accounting**, **sales**, **inventory**, **purchase**, **HR**, **CRM**, **manufacturing** (each `_VIEW` / `_MANAGE`) |

Those scripts also attach permissions to **`SYSTEM_ADMIN`**, **`ORG_ADMIN`**, and **`USER`** roles.

### Domain services (optional modules)

| Module | Liquibase changelog | RBAC seed file(s) |
|--------|---------------------|-------------------|
| **Pharma** | `pharma-service` | [`services/pharma-service/src/main/resources/db/changelog/data/015-pharma-permissions.sql`](../services/pharma-service/src/main/resources/db/changelog/data/015-pharma-permissions.sql) — `PHARMA_VIEW`, `PHARMA_MANAGE` |
| **Hospital / EHR** | `hospital-service` | [`services/hospital-service/src/main/resources/db/changelog/changesets/002-hospital-permissions.sql`](../services/hospital-service/src/main/resources/db/changelog/changesets/002-hospital-permissions.sql) — `HOSPITAL_VIEW`, `HOSPITAL_MANAGE`, plus prescription / hospital-pharmacy dispense codes |

**Not every microservice has its own permission file yet** — many are covered by the **generic** `resource` keys in `011` (e.g. `accounting`, `sales`). Finer-grained codes (per endpoint) can be added later via new changesets or admin UI, per the RBAC requirements doc.

Running **`hospital-service`** Liquibase applies hospital RBAC into the shared `rbac` schema when that service’s migrations run (typically `context:data` per changeset). Deployments **without** hospital do not run `hospital-service` migrations, so those permission rows are not inserted.

## Adding a new permission (PR checklist)

1. **Database** — Add the row to `rbac.permissions` (Liquibase changeset or migration), with a **unique** `code`, plus `resource`, `action`, `name`, `description`.
2. **Roles** — Link permissions to roles via `rbac.role_permissions` (seed or admin).
3. **Backend** — Enforce by comparing the required **`code`** string to the user’s effective permissions from `rbac-service` (no hardcoded enum).
4. **Frontend** — Use `hasPermission('YOUR_CODE')` where `YOUR_CODE` matches the DB row; strings can be **local constants beside the feature** if needed, or derived from config — not a global embedded catalog.
5. **Tests** — Negative test: user without the permission gets **403**.

## Validation (catalog vs code)

Run before release or in CI to catch typos in **hospital** permission strings used in `hasPermission` / `hasAnyPermission`:

- **Python** (cross-platform): [`../scripts/rbac/validate-hospital-permission-codes.py`](../scripts/rbac/validate-hospital-permission-codes.py) — compares literals in `frontend/src` to hospital Liquibase seeds (`002`, `035`, `036`, `037`).
- **Wrappers**: [`../scripts/rbac/validate-hospital-permission-codes.ps1`](../scripts/rbac/validate-hospital-permission-codes.ps1) (Windows), [`../scripts/rbac/validate-hospital-permission-codes.sh`](../scripts/rbac/validate-hospital-permission-codes.sh) (Unix). Set `REPO_ROOT` to the `easyops-erp` directory if running the `.py` file directly.

**Menu bypass audit** (fails if `skipRbac` / similar reappears): [`../scripts/rbac/audit-menu-bypass.ps1`](../scripts/rbac/audit-menu-bypass.ps1) (Windows), [`../scripts/rbac/audit-menu-bypass.sh`](../scripts/rbac/audit-menu-bypass.sh) (Unix). **Cross-platform (recommended):** from [`../frontend/`](../frontend/) run `npm run rbac:audit` — runs [`../scripts/rbac/run-rbac-checks.mjs`](../scripts/rbac/run-rbac-checks.mjs) (bypass scan + Python validator). GitHub Actions: [`.github/workflows/rbac-validation.yml`](../../.github/workflows/rbac-validation.yml).

## Documentation

Requirements and phases: [`../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md`](../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md).

### Phase 0 — Foundation (status)

| Deliverable | Location |
|-------------|----------|
| §13 acceptance criteria + links | [`../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md`](../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md) §13 (links to this file and `database-versioning/README.md`) |
| Optional module Liquibase contexts | [`../database-versioning/README.md`](../database-versioning/README.md) *Optional module RBAC seeds* |
| Phase 1 prescription negative tests | [`../services/hospital-service/src/test/java/com/easyops/hospital/controller/PrescriptionControllerRbacTest.java`](../services/hospital-service/src/test/java/com/easyops/hospital/controller/PrescriptionControllerRbacTest.java) |

### Phase 3 — Gateway, cache, UI consistency (complete)

| Area | Location / notes |
|------|------------------|
| **Gateway vs services** | [`../services/api-gateway/RBAC.md`](../services/api-gateway/RBAC.md) — no duplicate catalog; services enforce; optional future route map must reference DB codes. |
| **rbac-service cache TTL + eviction** | `rbac.cache.redis-entry-ttl` in `rbac-service` `application.yml` (env `RBAC_CACHE_REDIS_ENTRY_TTL`). Role/permission/user-role mutations evict `userRoles`, `userPermissions`, `roles`, `activeRoles`, `permissions`, `activePermissions` as applicable (`RoleService`, `PermissionService`, `AuthorizationService`, including `cleanupExpiredRoles`). |
| **Hospital menu** | No visibility bypass: [`../frontend/src/components/Layout/MainLayout.tsx`](../frontend/src/components/Layout/MainLayout.tsx) uses `hasAccess` → `canViewResource` / `canManageResource` for `resource: 'hospital'` (aligned with `HOSPITAL_VIEW` / `HOSPITAL_MANAGE`). |
| **CI + local audit** | [`../../.github/workflows/rbac-validation.yml`](../../.github/workflows/rbac-validation.yml); `npm run rbac:audit` in [`../frontend/package.json`](../frontend/package.json). |

### Frontend audit checklist (catalog codes vs coarse resource)

Use this when touching routes, buttons, or nav:

1. **Prefer** `hasPermission('EXACT_CODE')` when the UI must match a **specific** catalog row (e.g. `HOSPITAL_PRESCRIPTION_TRANSMIT`).
2. **Use** `canViewResource(resource)` / `canManageResource(resource)` only for **coarse** module gates where the menu or screen aligns with **`resource` + `action`** in `rbac.permissions` (e.g. `hospital` + `view`).
3. **Do not** embed global permission enums in the repo; strings should match DB codes or local feature constants that mirror the catalog.
4. **Align** `MainLayout` `permission` entries with the same rules as `ProtectedRoute` for the same path.

### Phase 4 — Prescribing roles and clinical polish

| Artifact | Purpose |
|----------|---------|
| [`../services/hospital-service/.../035-prescribing-authority-role.sql`](../services/hospital-service/src/main/resources/db/changelog/changesets/035-prescribing-authority-role.sql) | Role `PRESCRIBING_AUTHORITY` — Rx view, prescribe, transmit |
| [`../services/hospital-service/.../037-prescribing-authority-hospital-view.sql`](../services/hospital-service/src/main/resources/db/changelog/changesets/037-prescribing-authority-hospital-view.sql) | Adds `HOSPITAL_VIEW` to `PRESCRIBING_AUTHORITY` (nav + hospital-pharmacy catalog) |
| [`../services/hospital-service/.../036-phase4-clinical-roles.sql`](../services/hospital-service/src/main/resources/db/changelog/changesets/036-phase4-clinical-roles.sql) | `PHARMACIST_DISPENSER` (dispense + view Rx + hospital view, no prescribe/transmit); `E_PRESCRIBING_TRANSMITTER` (hospital view + Rx view + transmit, no prescribe) |
| [`../services/hospital-service/.../RbacPermissionServiceTest.java`](../services/hospital-service/src/test/java/com/easyops/hospital/service/RbacPermissionServiceTest.java) | Prescribe vs transmit separation at the permission-alternative layer |
| [`../services/hospital-service/.../PrescriptionRoleSeparationRbacTest.java`](../services/hospital-service/src/test/java/com/easyops/hospital/service/PrescriptionRoleSeparationRbacTest.java) | Effective permission sets for pharmacist / transmitter / prescribing authority (Phase 4) |
| [`../services/hospital-pharmacy-service/.../HospitalPharmacyRbacService.java`](../services/hospital-pharmacy-service/src/main/java/com/easyops/hospitalpharmacy/security/HospitalPharmacyRbacService.java) | `HOSPITAL_PHARMACY_DISPENSE` on dispense-order APIs; [`DispenseOrderControllerRbacTest.java`](../services/hospital-pharmacy-service/src/test/java/com/easyops/hospitalpharmacy/controller/DispenseOrderControllerRbacTest.java) |

§7 matrix: [`../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md`](../implementations/RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS.md) §7.
