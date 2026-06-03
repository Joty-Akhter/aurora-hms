# `hospital-canteen-service` – Implementation Plan

## 1. Overview and Objectives

`hospital-canteen-service` is the dedicated service for managing:

- **Menus and pricing for canteen items.**
- **Canteen orders** for patients, staff, and visitors.
- **Integration with `hospital-card-management-service` and `hospital-billing-service`.**
- **Optional inpatient meal plans linked to visits from `hospital-service`.**

**Primary objectives:**

- Provide a robust, audit-friendly system for all canteen-related transactions within the hospital.
- Support both indoor (inpatient) and outdoor (staff/visitor) sales, with correct linkage to billing and cards.
- Enable flexible menu management, pricing strategies, and package definitions (e.g., staff meal plans, inpatient diet plans).

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Canteen items and menus:**
  - Item master (meals, snacks, beverages, combos).
  - Menus per canteen location and time (breakfast, lunch, dinner, night).
  - Pricing, including special prices for staff/corporates if required.
- **Canteen orders:**
  - Counter-based walk-in orders (staff/visitor).
  - Patient-linked orders (indoor).
  - Order life cycle: created, in preparation, served/delivered, cancelled.
- **Charges and billing hooks:**
  - Generation of billable lines for `hospital-billing-service`.
  - Ability to associate orders with corporate contracts/discounts when relevant.
- **Card and payment integration:**
  - Use of `hospital-card-management-service` cards for payments.
  - Handling of card-based transactions and partial payments (card + cash, etc.).
- **Optional inpatient meal plans:**
  - Pre-configured meal plans linked to inpatient visits.
  - Daily or per-meal scheduling, with integration to dietician workflows where applicable.

### 2.2 Out-of-Scope (Handled by Other Services)

- **Clinical nutrition/diet prescriptions** (if modeled as part of clinical orders) – `hospital-service` / `hospital-clinical-orders-service`.
- **Inventory and raw material stock management** – inventory service.
- **Core billing, invoices, and payments** – `hospital-billing-service`.
- **Card lifecycle and balances** – `hospital-card-management-service`.

## 3. Architecture and Boundaries

### 3.1 High-Level Architecture

- **Service type**: Stateless API service with its own database.
- **Database**: Relational (e.g., PostgreSQL) for transactional integrity over orders and pricing.
- **Integrations**:
  - Synchronous APIs for canteen UI, BFFs, and other services.
  - Events for order lifecycle changes and billing hooks.

### 3.2 Bounded Context Responsibilities

- **Canteen Catalog Context**:
  - Owns canteen items, menus, and pricing.
- **Order Context**:
  - Owns canteen orders, line items, and statuses.
- **Meal Plan Context (optional)**:
  - Owns inpatient meal plan templates and their application to specific visits.

## 4. Data Model (High-Level)

### 4.1 Key Entities

- `CanteenLocation`:
  - `id`, `name`, `type` (main_canteen, staff_cafeteria, kiosk, etc.).
  - Operational hours, status (active/inactive).
- `CanteenItem`:
  - `id`, `code`, `name`, `description`.
  - `category` (meal, snack, beverage, other).
  - `default_price`, `unit_of_measure`.
  - Flags (e.g., `is_veg`, `is_staff_only`).
- `Menu`:
  - `id`, `canteen_location_id`, `name`.
  - `valid_from`, `valid_to`.
- `MenuItem`:
  - `id`, `menu_id`, `canteen_item_id`.
  - `time_slot` (breakfast, lunch, dinner, night, all_day).
  - `override_price` (optional).
- `CanteenOrder`:
  - `id`, `canteen_location_id`.
  - `order_type` (walk_in, inpatient).
  - `patient_id` / `visit_id` (for inpatient, via `hospital-service`).
  - `ordered_by_user_id` (staff user who created the order, if applicable).
  - `status` (created, in_preparation, ready, served, cancelled).
  - `created_at`, `completed_at`.
- `CanteenOrderLine`:
  - `id`, `canteen_order_id`.
  - `canteen_item_id`.
  - `quantity`, `unit_price`, `total_price`.
  - `discount_amount` (if any), references to discount scheme if needed.
- `CanteenPayment`:
  - `id`, `canteen_order_id`.
  - `payment_mode` (cash, card, mixed, billed_to_patient_account).
  - `amount`, `card_id` (from `hospital-card-management-service` if used).
  - `status` (pending, completed, failed).
- `MealPlanTemplate` (optional):
  - `id`, `name`, `description`.
  - `applicable_visit_type` (IP only, etc.).
- `MealPlanTemplateItem`:
  - `id`, `meal_plan_template_id`.
  - `time_slot`, `canteen_item_id`, `quantity`.
- `InpatientMealPlan`:
  - `id`, `visit_id`, `meal_plan_template_id`.
  - `start_date`, `end_date`, `status`.

### 4.2 Referential Strategy

- External references:
  - `patient_id`, `visit_id` from `hospital-service`.
  - `card_id` from `hospital-card-management-service`.
  - Billing context IDs supplied to/from `hospital-billing-service`.

## 5. APIs

### 5.1 Canteen Catalog and Menu APIs

- `POST /canteens` – create canteen location.
- `GET /canteens` – list/search canteen locations.
- `GET /canteens/{id}` – get canteen location details.
- `POST /canteen-items` – create canteen item.
- `GET /canteen-items` – list/search items (by category, name, active status).
- `GET /canteen-items/{id}` – get item details.
- `PATCH /canteen-items/{id}` – update item (price, description, status).
- `POST /menus` – create menu for a location.
- `GET /menus` – list menus (with filters).
- `GET /menus/{id}` – get menu details.
- `POST /menus/{id}/items` – add or update items within a menu.
- `GET /menus/{id}/items` – list menu items.

### 5.2 Order and Payment APIs

- `POST /orders` – create canteen order (walk-in or inpatient).
- `GET /orders/{id}` – get order details.
- `GET /orders` – list/search orders (by location, date, patient, status).
- `POST /orders/{id}/lines` – add/update order lines.
- `PATCH /orders/{id}` – update order-level status (e.g., in_preparation, ready, served, cancelled).
- `POST /orders/{id}/payments` – record a payment (card/cash/billed-to-account).
- `GET /orders/{id}/payments` – list payments for an order.

### 5.3 Billing Integration APIs

- `GET /orders/{id}/billable-items` – return billable items for `hospital-billing-service` (for patient-linked orders).
- Optional push model:
  - Events or webhook callbacks to billing when patient-linked orders are completed.

### 5.4 Meal Plan APIs (Optional)

- `POST /meal-plans/templates` – create meal plan template.
- `GET /meal-plans/templates` – list templates.
- `GET /meal-plans/templates/{id}` – get template details.
- `POST /meal-plans/templates/{id}/items` – define items and time slots.
- `POST /inpatient-meal-plans` – assign a meal plan to a visit.
- `GET /inpatient-meal-plans/{id}` – get details.
- `GET /visits/{visitId}/meal-plans` – list meal plans for a visit.
- Optionally auto-generate daily canteen orders from inpatient meal plans via scheduled jobs.

## 6. Events and Integrations

### 6.1 Outgoing Events

Emit events such as:

- `canteen.order.created`, `canteen.order.updated`, `canteen.order.completed`, `canteen.order.cancelled`.
- `canteen.payment.completed`.
- `inpatient-meal-plan.created`, `inpatient-meal-plan.updated`.

Consumers:

- `hospital-billing-service` – to create/update billing for patient-linked orders.
- `hospital-card-management-service` – for reconciliation of card-based payments if required.
- Analytics/reporting – consumption patterns, revenue, and costing.

### 6.2 Incoming Events and Calls

- From `hospital-service`:
  - Visit context (patient/visit IDs) for inpatient orders and meal plans.
- From `hospital-card-management-service`:
  - Card validation and balance checks during payments.
- From `hospital-billing-service`:
  - Confirmation of charges posted to patient accounts.

## 7. Non-Functional Requirements

- **Performance**:
  - Order creation and update APIs should be responsive enough for counter operations (e.g., 95th percentile < 300 ms).
- **Reliability**:
  - Idempotent handling of payment callbacks and events to avoid double-charging.
- **Security**:
  - RBAC for canteen operators, supervisors, and finance roles.
- **Auditability**:
  - Full trace of order and payment lifecycle, including who applied discounts or cancelled orders.

## 8. Phased Implementation Plan

### Phase 1 – Catalog and Basic Counter Orders

- Implement:
  - `CanteenLocation`, `CanteenItem`, `Menu`, and `MenuItem` entities and APIs.
  - Basic `CanteenOrder` and `CanteenOrderLine` for walk-in orders.
- Deliverables:
  - Canteen can operate basic counter sales with items and menus, independent of patient billing.

### Phase 2 – Patient-Linked Orders and Billing Hooks

- Implement:
  - Patient/visit linkage to orders.
  - `GET /orders/{id}/billable-items` and associated events for billing.
- Integrate:
  - With `hospital-billing-service` to bill inpatient orders to patient accounts.
- Deliverables:
  - Indoor/inpatient orders correctly billed against patient visits.

### Phase 3 – Card Payments Integration

- Implement:
  - `CanteenPayment` entity and APIs.
  - Integration with `hospital-card-management-service` for card-based payments.
- Deliverables:
  - Staff/visitors/patients can pay using hospital cards; transactions visible in card statements.

### Phase 4 – Inpatient Meal Plans and Optimization

- Implement:
  - `MealPlanTemplate`, `MealPlanTemplateItem`, `InpatientMealPlan` entities and APIs.
  - Scheduled generation of orders from meal plans, if required.
- Optimize:
  - Query performance and reporting for high-volume canteen environments.
- Deliverables:
  - Structured inpatient meal plan flows with automated or semi-automated ordering.

## 9. Deployment and Migration Strategy

- **Initial deployment**:
  - Start with a single canteen location and walk-in orders only.
  - Gradually enable patient-linked orders and billing integration.
- **Migration**:
  - Import legacy canteen item lists and standardized pricing.
  - If needed, backfill daily sales into the new system for continuity.

## 10. Risks and Mitigations

- **Risk**: Tight coupling to billing and card services causing downtime impact.
  - **Mitigation**: Allow offline/queued operations for payments and billing posting, with reconciliation later.
- **Risk**: Complex pricing and discount schemes obscuring clarity at counters.
  - **Mitigation**: Start with simple pricing and clearly present final prices in the UI; centralize discount rules through corporate/discount services when needed.
- **Risk**: Order volume spikes at peak times.
  - **Mitigation**: Scale horizontally, cache menus, and keep order writes efficient with minimal cross-service calls in the hot path.

