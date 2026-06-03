# Audit trail and permissions (Phase 6.2)

## Audit trail for schedule and appointment changes

### Current behaviour

- **created_at / updated_at**: All appointment and reservation mutations use JPA auditing (`@CreatedDate`, `@LastModifiedDate`). On persist, `created_at` is set; on update, `updated_at` is updated automatically.
- **created_by**: The `created_by` column is present on scheduling_reservations, scheduling_appointments, and other entities. It is populated via `@CreatedBy` and an `AuditorAware<UUID>` bean. The default implementation (`JpaAuditingConfig.auditorProvider()`) returns `Optional.empty()`, so `created_by` remains null until the platform provides authentication. When platform RBAC/auth is integrated, replace the auditor provider with one that returns the current user ID from `SecurityContext` (or equivalent); then all new reservations and appointments will store the creating user.

### Optional audit log (future)

An optional audit log table for appointment/reservation **change events** (who, what, when) is not implemented in Phase 6.2. To add it later:

- Define a table (e.g. `scheduling_audit_log`) with columns such as: entity_type, entity_id, action (CREATED, UPDATED, CANCELLED, etc.), user_id, at (timestamp), old_value/new_value or diff (optional).
- Publish events from the service layer on create/update/status-change, or use an entity listener / async listener to write to the audit table.

## Fine-grained permissions

### Recommended model

- **schedule.view** (or schedule.view_only): Can view resources, availability, reservations, appointments, waitlist, reports. Read-only.
- **schedule.book**: Can create and update reservations and appointments (book, reschedule, check-in, no-show).
- **schedule.edit**: Can edit masters (resources, working hours, slot templates, blackouts, roster blocks, booking rules) and perform administrative actions (cancel, expire, promote waitlist, etc.).

These can be mapped to platform roles (e.g. “Scheduling viewer”, “Scheduling operator”, “Scheduling admin”) when the platform RBAC is available.

### Current state

- The scheduling service does **not** enforce fine-grained permissions internally. API Gateway or a shared auth layer may enforce “hospital” or “scheduling” access; once a request reaches this service, all endpoints are available to the caller.
- **Integration with platform RBAC**: When the platform provides RBAC (e.g. JWT claims or SecurityContext with roles/permissions), add checks in controllers or a shared filter/interceptor: e.g. require `schedule.book` for POST/PATCH on reservations and appointments, and `schedule.edit` for masters and admin actions. Document the required permission for each endpoint in the API spec or in this file.

### Summary

| Area              | Status |
|-------------------|--------|
| created_at/updated_at | Set automatically on reservations and appointments. |
| created_by        | Column and `@CreatedBy` in place; populated when `AuditorAware` returns current user. |
| Optional audit log table | Not implemented; design documented above for future use. |
| Fine-grained permissions | Documented (schedule.view, schedule.book, schedule.edit); to be enforced when platform RBAC is integrated. |
