## Organization Master Data & UOM Requirements

### 1. Goal & Scope

- **Goal**: Centralize organization-level master data and configuration (starting with Units of Measure and other enum-like values such as gender) so that:
  - Constants are **not hardcoded** in frontend or services.
  - Values are **configurable per organization**.
  - Multiple modules (HR, Inventory, CRM, etc.) can share consistent master lists (e.g., UOM, gender).
- **Phase 1 scope**:
  - UOM master data and per-organization activation.
  - Generic master-data model that can also support other lists (e.g., gender) in a consistent way.

### 2. Data Model Requirements

#### 2.1 Generic Organization Master Data (Extensible Pattern)

To avoid creating a new pair of tables for every “constant” type (UOM, gender, product category, etc.), we will design a **generic master data model** that can host multiple master-data types while still being type-safe and queryable.

- Create table `core.master_data`:
  - `master_data_id` (UUID, PK).
  - `type` (string/enum): identifies the category, e.g.:
    - `UOM`
    - `GENDER`
    - `PRODUCT_CATEGORY`
    - (future) `TAX_CODE`, `PAYMENT_TERM`, `REASON_CODE`, etc.
  - `code` (string, unique **per type**, e.g., `GRAM`, `ML`, `PCS`, `MALE`, `FEMALE`).
  - `name` (string): human-friendly name, e.g., `Gram`, `Male`, `Electronics`.
  - `description` (string, nullable).
  - `extra_attributes` (JSONB, nullable): to store type-specific metadata when needed (e.g., conversion factors for UOM, hierarchy for categories).
  - `is_active` (bool).
  - `created_at`, `updated_at`, `created_by`, `updated_by`.

- Create table `core.organization_master_data`:
  - `organization_master_data_id` (UUID, PK).
  - `organization_id` (FK to `admin.organizations`).
  - `master_data_id` (FK to `core.master_data`).
  - `display_name` (string, nullable): per-organization label override.
  - `display_order` (int).
  - `is_active` (bool).

- Behavior:
  - All **global definitions** (UOMs, genders, product categories that are global, etc.) live in `core.master_data`.
  - `core.organization_master_data` controls which entries of a given `type` are available for a specific organization and in what order/label.

> Note: For product categories we already have a dedicated `product_categories` table. Phase 1 will **not** replace that, but the generic model is designed so we can:
> - Use `core.master_data` immediately for UOM and gender.
> - Later decide whether to gradually converge some existing “dimension tables” (like categories) into this generic pattern or keep them separate but conceptually aligned.

#### 2.2 UOM Master (as a `type = 'UOM'` in master_data)

- Represent UOMs in `core.master_data` with `type = 'UOM'`:
  - `code` examples: `GRAM`, `ML`, `PCS`.
  - `name` examples: `Gram`, `Milliliter`, `Pieces`.
  - `extra_attributes` may store:
    - `{"category": "MASS"}` or `"VOLUME"` or `"COUNT"`.
    - optional conversion metadata in future phases.
- Use `core.organization_master_data` for UOM activation:
  - For existing pharma org(s), create `organization_master_data` rows:
    - `type = 'UOM'`
    - Active entries only for `GRAM`, `ML`, `PCS`.

#### 2.3 Gender Master (as a `type = 'GENDER'`)

- Store gender options (for HR, user profiles, CRM contacts) in `core.master_data` with `type = 'GENDER'`:
  - Suggested seeds:
    - `code = 'MALE'`, `name = 'Male'`
    - `code = 'FEMALE'`, `name = 'Female'`
    - `code = 'OTHER'`, `name = 'Other'`
    - `code = 'PREFER_NOT_TO_SAY'`, `name = 'Prefer not to say'`
- Use `core.organization_master_data` to:
  - Allow orgs to hide/show certain gender values if needed.
  - Potentially provide localized display names per organization.

#### 2.4 Product Categories (future alignment)

- Product categories already exist in their own tables in the inventory module.
- This requirements document does **not** force moving categories into `core.master_data` in Phase 1, but:
  - The same pattern can be used for **additional lightweight category lists** in the future.
  - A future phase can define how `product_categories` relates to `core.master_data` (e.g., one‑way reference or gradual migration).

### 3. API Requirements

#### 3.1 Master Data APIs

- **GET /api/master/uoms**
  - Query params: `organizationId`.
  - Returns: active UOMs for the organization, including:
    - `uomId`, `code`, `name`, `category`, `displayName` (if overridden), `displayOrder`.
- (Phase 2, admin‑only) Ability to:
  - Create/edit UOM definitions.
  - Activate/deactivate UOMs per organization (`organization_uoms`).

### 4. Frontend Requirements (High-Level)

- UIs (starting with inventory / pharma and HR) should:
  - Load allowed UOMs (and later, other master lists like gender) from `GET /api/master/uoms` or corresponding master-data APIs.
  - Stop hardcoding UOM/gender options in components; instead, bind selects to the master data for the current organization.

### 5. Migration & Compatibility

- Liquibase changesets must:
  - Create `core.master_data` and `core.organization_master_data`.
  - Seed base UOMs (`GRAM`, `ML`, `PCS`) and their org‑activations for pharma organizations.
  - Seed initial gender values in `core.master_data` and activate them per organization.
- Existing UIs/services:
  - Gradually migrate from hardcoded UOM/gender lists in code to querying the master-data APIs.

### 6. Future Extensions (Out of Phase 1, design‑friendly)

- Reuse the same generic pattern (`core.master_data` + `core.organization_master_data`) for:
  - Tax codes, payment terms, adjustment reason codes.
  - Other organization‑level “constant” lists currently hardcoded in frontend or services.
- Over time, evaluate whether some existing dedicated master tables (e.g., product categories, leave types) should:
  - Stay as is but conceptually align with `master_data`, or
  - Be progressively converged into the generic master data model where it simplifies maintenance and configuration.

