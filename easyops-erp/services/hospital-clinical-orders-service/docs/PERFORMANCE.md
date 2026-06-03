# Performance and observability

## Pagination

List endpoints (order-sets, orders, worklists) cap page size at **100** (`ClinicalOrdersMetrics.PAGINATION_MAX_PAGE_SIZE`). Default page size is 20. This keeps list queries bounded for large datasets.

## Indexes

List queries are supported by indexes defined in Liquibase (hospital-service changelog):

- **order_sets**: `(patient_id, created_at)`, `(visit_id, created_at)`, `(facility_id, created_at)`
- **clinical_orders**: `(order_set_id)`, `(status, order_type)`, `(created_at)`, `(facility_id)`
- **order_worklist_items**: `(order_id)`, `(worklist_type, status)`, `(assigned_to_user_id, status)`

## Metrics (Prometheus)

Counters exposed via `/actuator/prometheus`:

- `clinical_orders_order_sets_created_total` – order sets created (create + copy-from)
- `clinical_orders_orders_created_total` – individual orders created
- `clinical_orders_worklist_status_changes_total` – worklist assign + status updates
- `clinical_orders_result_available_total` – result links added with result available (e.g. FINAL)

## Correlation ID in logs

Requests get a correlation ID (from `X-Correlation-Id` header or generated). It is set in MDC and echoed in the response header. Console log pattern includes `[%X{correlationId}]` so log lines can be traced by request.
