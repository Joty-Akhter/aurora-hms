# ADR-001: Communication Service Boundary and Ownership

## Status
Accepted

## Context
Communication logic exists in multiple places (for example, direct SMS handling and legacy notification paths), which causes inconsistent templates, tracking, and governance.

## Decision
Create a standalone `communication-service` as the canonical owner for:
- notification orchestration,
- template lifecycle,
- provider abstraction,
- delivery tracking and audit timeline.

Keep existing `notification-service` operational for legacy/non-migrated paths during transition.

## Consequences
- Positive:
  - Clear ownership and separation of concerns.
  - Faster expansion to additional channels.
  - Unified audit and policy enforcement model.
- Trade-offs:
  - Requires migration bridge and phased cutover.
  - Additional service deployment and operations overhead.
