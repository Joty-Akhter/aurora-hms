# Hospital QA Remediation TODO (Deep Plan)

## Scope
- Modules: Easy Prescription, Doctor Dashboard (IPD), Billing Charges, Scheduling Appointments, RX Templates, SMS confirmation.
- Goal: convert reported issues into implementation-ready tasks with acceptance criteria.

## Priority Legend
- `P0`: production blocker / data correctness risk.
- `P1`: core workflow defect, high user impact.
- `P2`: UX/consistency issue.
- `P3`: enhancement / optional cleanup.

---

## 1) Doctor Dashboard -> IPD Active Inpatients API Error
- **Status:** In progress (frontend hardening done)
- **Priority:** `P0`
- **Reported symptom:** "Could not load inpatient encounters ... database error ... 500"
- **Likely root causes:**
  - Attending physician filter may fail for some users due mapping/data mismatch.
  - API error payload not surfaced clearly in UI.
  - Backend query/index/data issue may exist for some organizations.
- **Tasks:**
  - [x] Frontend: fallback to org-wide IPD list if "my patients only" query fails.
  - [x] Frontend: improve error extraction and display.
  - [ ] Backend: inspect logs for `/api/hospital/encounters/active/inpatient`.
  - [ ] Backend: validate `attendingPhysicianId` data integrity across encounters.
  - [ ] Backend: add query/index optimization and null-safe handling if needed.
- **Acceptance criteria:**
  - IPD list always loads (with fallback mode if physician filter fails).
  - User sees actionable error text, not generic failure.
  - No unhandled 500 for valid org requests.

---

## 2) Billing -> Charges -> Create Charge (Patient UUID / MRN autosuggestions)
- **Status:** In progress (enhanced frontend autocomplete + name resolution done)
- **Priority:** `P1`
- **Tasks:**
  - [x] Frontend: add patient input autosuggestions using patient search.
  - [x] Support UUID, MRN, and typed patient name.
- [x] UX: render richer autocomplete dropdown (name + MRN + patient UUID) using MUI `Autocomplete`.
  - [ ] Backend (optional): add compact patient lookup endpoint for billing with pagination.
- **Acceptance criteria:**
  - Typing name/MRN shows matching patients quickly.
  - Selection fills MRN/ID reliably and charge creation succeeds.

---

## 3) Scheduling -> Appointment slot mismatch after schedule creation
- **Status:** In progress (frontend source-of-truth fix done)
- **Priority:** `P0`
- **Issue:** Slot timing shown during booking does not align with created schedule.
- **Root cause (identified):**
  - Booking screen used locally generated slots from doctor config, which can drift from scheduling service slots/timezone handling.
- **Tasks:**
  - [x] Frontend: use scheduling availability API as sole slot source in booking panel.
  - [ ] Add integration test for slot consistency between schedule config and booking view.
  - [ ] Validate timezone assumptions across API gateway and scheduling service.
- **Acceptance criteria:**
  - Booking slot times match scheduling engine output exactly.
  - No duration mismatch between configured schedule and booking list.

---

## 4) RX Templates -> Medication suggestions should include generic + brand names
- **Status:** mostly implemented already; verify and tune
- **Priority:** `P1`
- **Current finding:**
  - UI already merges generic, brand, and combined labels in suggestions.
- **Tasks:**
  - [ ] Verify API returns brand names consistently for all catalogs.
  - [ ] Deduplicate display labels while preserving searchability.
  - [ ] Add tests for suggestion composition.
- **Acceptance criteria:**
  - Suggestion list contains generic-only, brand-only, and generic(brand) where available.

---

## 5) Scheduling -> Filters -> Status has nonfunctional options
- **Status:** partially addressed
- **Priority:** `P2`
- **Tasks:**
  - [x] Remove `TENTATIVE` from UI filter options (not used in current flow).
- [x] Hide `COMPLETED` in this screen filter pending backend workflow confirmation.
  - [ ] Align status enums across frontend and backend contract.
- **Opinion:**
  - Keep only statuses that can be produced/queried in current release to avoid user confusion.
- **Acceptance criteria:**
  - Filter dropdown contains only meaningful statuses.

---

## 6) Scheduling -> Appointment list doctor column redundancy
- **Status:** fixed
- **Priority:** `P2`
- **Tasks:**
  - [x] Hide "Doctor/Resource" table column when doctor filter is already applied.
  - [x] Show selected doctor name once near list title.
- **Acceptance criteria:**
  - No repeated doctor values per row when single doctor is selected.

---

## 7) Scheduling -> Refresh should update filters + booking panel
- **Status:** partially fixed
- **Priority:** `P1`
- **Tasks:**
  - [x] Refresh now reloads resources/appointments and re-resolves selected doctor in booking flow.
  - [ ] Add explicit "Refresh slots" action inside booking panel (date/slot section).
  - [ ] Refresh should preserve selected patient + selected date where possible.
- **Acceptance criteria:**
  - Refresh updates both list and booking context.
  - No stale slot availability after refresh.

---

## 8) Scheduling -> Slot column redundancy and 24h formatting
- **Status:** fixed
- **Priority:** `P2`
- **Tasks:**
  - [x] Display slot column as time range only.
  - [x] Use 12-hour format (`h:mm AM/PM`).
- **Acceptance criteria:**
  - Date appears only in date column; slot column shows readable time range.

---

## 9) Scheduling -> Patient filter should support ID + name
- **Status:** partially fixed
- **Priority:** `P1`
- **Tasks:**
  - [x] Update label/placeholder to indicate UUID/MRN/Name supported.
  - [x] Add fallback path: if not UUID/MRN match, search by name and filter appointments by matched patient IDs.
  - [ ] Add server-side multi-patient filtering endpoint for accurate pagination totals.
- **Acceptance criteria:**
  - Name-based filter returns all matching patients for selected doctor.
  - Multiple same-name patients are shown.

---

## 10) Appointment confirmation SMS after booking
- **Status:** pending
- **Priority:** `P1`
- **Tasks:**
  - [ ] Define event contract from scheduling (`appointment.created`) to notification service.
  - [ ] Build SMS template with placeholders:
    - SL No
    - Date
    - Doctor name
    - Reporting window
    - Chamber
    - Hospital name and map URL
  - [ ] Add retry and failure observability (DLQ or retry queue).
  - [ ] Add opt-out/consent and language toggle (Bangla/English) support.
- **Opinion:**
  - SMS dispatch should be async and non-blocking; appointment booking must not fail if SMS gateway is down.
- **Acceptance criteria:**
  - SMS sent within SLA after successful booking.
  - Booking remains successful even when SMS fails; failure is logged/retriable.

---

## Implementation Order (Recommended)
1. P0 items: IPD error root-cause + appointment slot consistency tests.
2. P1 items: billing autosuggest polish, patient-name filtering API support, SMS pipeline.
3. P2 items: status enum cleanup and remaining refresh UX refinement.

## Test Matrix (Minimum)
- `SchedulingAppointments`: slot timing, refresh behavior, doctor-filter list rendering, patient-name filter.
- `DoctorDashboard`: IPD with/without attending filter, fallback behavior, error message.
- `BillingCharges`: create charge via UUID, MRN, and name suggestion selection.
- `PrescriptionTemplates`: generic + brand suggestion presence and dedupe.
- `Notifications`: SMS success/failure/retry scenarios.

