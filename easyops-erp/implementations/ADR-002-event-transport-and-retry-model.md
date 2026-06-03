# ADR-002: Event Transport and Retry Model

## Status
Accepted

## Context
Multiple services in the platform already publish domain events via Kafka. Phase 0 needs a single, consistent transport for communication events.

## Decision
Use Kafka as the primary communication event transport in v1.

Processing model:
- Consumer-level retry with exponential backoff for transient failures.
- DLQ topic for non-recoverable or exhausted retries.
- Idempotency key enforcement at delivery orchestration boundary.

## Consequences
- Positive:
  - Aligns with current eventing practices and tooling.
  - Reduces platform variance and onboarding complexity.
  - Supports resilient asynchronous processing.
- Trade-offs:
  - Requires Kafka topic/consumer observability and runbooks.
  - Delivery semantics require strict idempotency handling.
