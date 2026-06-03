# Roster blocks and availability

## Current scope (Phase 4)

- **Roster blocks**: Explicit blocks per resource with type `AVAILABLE`, `UNAVAILABLE`, or `SUBSTITUTE`. API: `POST/GET /resources/{id}/roster-blocks`, `DELETE /roster-blocks/{id}`.
- **Availability**: Working hours, blackouts, and roster blocks are applied. Slots overlapping **UNAVAILABLE** are excluded. Slots overlapping **SUBSTITUTE** with a `substitute_resource_id`: the response **swaps** to the substitute resource’s availability for that slot (same start/end, `substituteResourceId` set in the slot DTO).
- **Multi-resource**: Reservations and appointments can reserve multiple resources (e.g. doctor + room) in one call; see API and appointment create request.

## Deferred / future

- **Pattern-based rosters**: Defining a recurring pattern (e.g. “Mon/Wed 09:00–17:00”) and applying it to a date range is **not** implemented. Use multiple roster blocks or a future bulk/pattern API.
- **Bulk updates**: Creating or deleting many roster blocks in one request (e.g. “add this block for the next 4 weeks”) is **not** implemented. Use repeated single-block API calls or a future bulk endpoint.

These may be added in a later phase if required for pilot or operations.
