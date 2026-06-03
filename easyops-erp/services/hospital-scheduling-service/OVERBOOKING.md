# Overbooking policy (Phase 5.3)

When creating an appointment, the service enforces a **max reservations per slot** limit. Beyond that, booking is rejected unless the client provides an **overbooking override reason**.

## Effective capacity

Effective `max_per_slot` is resolved in this order:

1. **scheduling_booking_rules** – RESOURCE scope (resource_id), then BRANCH scope (branch_id), then GLOBAL (scope_id null). First rule with a non-null `max_per_slot` wins.
2. **Slot template** – If no booking rule defines `max_per_slot`, the resource’s slot template is used: match by `resource_type` and optionally `branch_id`; use `slots_per_interval` as capacity.
3. **Default** – If neither rules nor template apply, capacity is **1** (no overbooking).

So capacity is **configurable per resource** (via booking rules), per branch, or globally; otherwise it comes from the slot template or defaults to 1.

## Overbooking

- If the slot already has `max_per_slot` reservations (or the derived capacity from the template), the next booking would exceed capacity.
- In that case the create-appointment call **fails** unless the request includes a non-blank **overbookingOverrideReason**.
- If **overbookingOverrideReason** is provided, the booking is allowed (e.g. for emergency or operational override). The reason is not persisted in this service; it can be logged or passed to audit elsewhere.

## Summary

- **scheduling_booking_rules.max_per_slot** and **slot template slots_per_interval** define the cap.
- Beyond that: **reject** unless **overbookingOverrideReason** is supplied.

---

## Waitlist auto-fill when slot freed (Phase 5)

When an appointment is **cancelled**, the freed slot can be offered to the waitlist so the next pending candidate is promoted into that slot (one appointment created, waitlist entry marked PROMOTED).

- **Configuration**: Set `hospital-scheduling.waitlist.auto-fill-on-cancel=true` to enable. Default is `false` (only manual "Promote for slot" from the UI or API is used).
- **Behaviour**: After the cancel transaction is committed, the service calls the same promote logic used by `POST /waitlist/promote` for that resource and slot with `maxCandidates=1`. Failures in promote are logged and do not affect the cancellation.
- **Notification**: Contacting the promoted patient (SMS, email, etc.) is intended to be implemented elsewhere; this service only creates the appointment and marks the waitlist entry PROMOTED.
