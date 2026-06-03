# Observability and performance

## Metrics

The service exposes the following Micrometer counters (suitable for Prometheus):

| Metric | Description |
|--------|-------------|
| `scheduling_appointments_created_total` | Number of appointments successfully created. |
| `scheduling_reservations_conflicts_total` | Number of conflict checks that found one or more conflicting reservations (create attempt or explicit check-conflicts API). |
| `scheduling_availability_requests_total` | Number of availability requests (GET /availability). |

Correlation IDs are supported: the `X-Correlation-Id` request header is read or generated, stored in MDC as `correlationId`, and added to the response. The logging pattern includes `[%X{correlationId:-}]` so that log lines can be correlated with traces when tracing is enabled.

## Index and query review (list endpoints)

Target: keep **p95 response time under ~300 ms** for typical page sizes (e.g. 10–50) on the main list endpoints. Monitor these endpoints and add or adjust indexes if p95 exceeds the target.

### Reservations list

- **Endpoint**: `GET /reservations` (paged).
- **Query**: `ReservationSpecifications` filters by `resourceId`, `patientId`, `status`, `slotStartFrom`, `slotEndTo`; then `findAll(spec, PageRequest)`.
- **Indexes** (schema: `hospital_scheduling.scheduling_reservations`):
  - `idx_hs_reservations_resource_slot` on `(resource_id, slot_start, slot_end)` — supports filter by resource and slot range; used by `findOverlapping` and list when resource is set.
  - `idx_hs_reservations_status` on `(status)`.
  - `idx_hs_reservations_patient` on `(patient_id)`.
- **Note**: When both resource and slot range are provided, the composite index supports the query well. When only patient or status is used, the corresponding single-column index is used.

### Appointments list

- **Endpoint**: `GET /appointments` (paged).
- **Query**: `AppointmentSpecifications` filters by `patientId`, `resourceId`, `clinicOrLocationId`, `appointmentDateFrom`, `appointmentDateTo`, `status`; then `findAll(spec, PageRequest)`.
- **Indexes** (schema: `hospital_scheduling.scheduling_appointments`):
  - `idx_hs_appointments_patient_date` on `(patient_id, appointment_date)` — supports list by patient and date range.
  - `idx_hs_appointments_resource_date` on `(resource_id, appointment_date)` — supports list by resource and date range.
  - `idx_hs_appointments_reservation` on `(reservation_id)`.
- **Note**: For common filters (patient + date range or resource + date range), the existing indexes support the query. If filtering primarily by `status` causes slow queries, consider adding an index that includes `status` (e.g. composite with the main filter column).

### Planned admissions list

- **Endpoint**: `GET /planned-admissions` (paged).
- **Query**: `PlannedAdmissionSpecifications` filters by `patientId`, `preferredDateFrom`, `preferredDateTo`, `status`; then `findAll(spec, PageRequest)`.
- **Indexes** (schema: `hospital_scheduling.scheduling_planned_admissions`):
  - `idx_hs_planned_admissions_patient` on `(patient_id)`.
  - `idx_hs_planned_admissions_preferred_date` on `(preferred_date)`.
  - `idx_hs_planned_admissions_status` on `(status)`.
- **Note**: Patient + date range + status are well covered by the single-column indexes; the planner can combine them.

### Waitlist list

- **Endpoint**: `GET /waitlist` (paged).
- **Query**: `WaitlistEntrySpecifications` filters by `resourceId`, `patientId`, `status`; then `findAll(spec, PageRequest)`.
- **Indexes** (schema: `hospital_scheduling.scheduling_waitlist_entries`):
  - `idx_hs_waitlist_resource_status` on `(resource_id, status)` — supports list by resource and status.
  - `idx_hs_waitlist_patient` on `(patient_id)`.
- **Note**: Common patterns (by resource + status or by patient) are supported.

## Recommendations

- **Monitoring**: Instrument or scrape p95 (and p99) latency for the four list endpoints above; alert if p95 exceeds ~300 ms for typical page sizes.
- **Load tests**: Run load tests with representative page sizes (e.g. 20, 50) and filter combinations to validate index usage and response times.
- **Tuning**: If a particular filter combination is slow, use the database `EXPLAIN` (e.g. `EXPLAIN ANALYZE`) on the generated SQL and add or adjust indexes as needed.
