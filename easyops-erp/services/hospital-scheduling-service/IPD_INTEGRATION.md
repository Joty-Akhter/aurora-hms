# IPD integration – Planned admissions and reservation link

## Reservation link behaviour

### When status is set to RESERVED

- The service **creates a Reservation** (resource = bed group or ward) for the planned admission:
  - `resourceId` = `bedGroupResourceId` from the update request
  - Slot = preferred date (full day in UTC)
  - `referenceType` = `PLANNED_ADMISSION`, `referenceId` = planned admission id
  - `patientId` = planned admission patient
- The planned admission is updated with:
  - `reservation_id` = id of the created reservation
  - `bed_group_resource_id` = resource used
  - `expires_at` = from request body, or **default 24 hours** from now (booking rules do not currently define planned-admission expiry; this could be extended later).

### When status is set to CONVERTED

- The service **closes the linked reservation** (if any): reservation status is set to `COMPLETED` so the bed is no longer held.
- The planned admission status is set to `CONVERTED`.

## Convert to admission (IPD module)

When the IPD module completes an actual admission that was pre-booked via a planned admission:

1. IPD (or a BFF) calls **`PATCH /api/hospital-scheduling/planned-admissions/{id}/status`** with body `{ "status": "CONVERTED" }`.
2. Scheduling marks the planned admission as CONVERTED and completes the linked reservation as above.
3. A future extension may add an optional `admissionId` (or similar) to the request body so scheduling can store the link to the actual admission record for audit or reporting.

Scheduling does **not** create or update the actual admission record; that remains in the IPD/admission module.

## Outgoing events

When a planned admission is reserved or converted, the service publishes in-process domain events (see `EVENTS.md`):

- **`planned_admission.reserved`** – after status is set to RESERVED (bed reserved, reservation created). Consumers: Portal BFF, bed management, analytics.
- **`planned_admission.converted`** – after status is set to CONVERTED. Consumers: analytics, IPD sync.

Subscribe via `@EventListener(PlannedAdmissionEvent.class)`; for Kafka/Rabbit, add a listener that forwards these events.

## Planned transfer and discharge dates

**Planned transfer** and **planned discharge** dates are owned and surfaced by the **IPD module** (admission, bed management, discharge workflows). The scheduling service does not model or expose transfer/discharge dates; it only provides planned admissions (request → reserve → convert) and expected admissions by date for dashboards. Integration with IPD is via the convert-to-admission flow above.
