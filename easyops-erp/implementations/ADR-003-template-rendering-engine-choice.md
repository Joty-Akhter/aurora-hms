# ADR-003: Template Rendering Engine Choice

## Status
Accepted

## Context
Communication templates need predictable variable substitution and validation, while avoiding business logic leakage into templates.

## Decision
Use a Mustache-compatible rendering approach with strict variable schema validation.

Rules:
- Templates remain logic-light and deterministic.
- Required variables are validated against `variables_schema` before dispatch.
- Missing required placeholders fail fast before provider invocation.

## Consequences
- Positive:
  - Predictable rendering behavior for SMS and email.
  - Lower risk of runtime template logic defects.
  - Easier governance and review for template changes.
- Trade-offs:
  - More complex rendering use cases must be handled in service logic, not templates.
