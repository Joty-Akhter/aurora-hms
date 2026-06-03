# Communication Service Runbook (Phase 6)

## Scope

This runbook defines baseline SLOs, alert thresholds, and first-response actions for the communication delivery pipeline.

## SLO Baseline

- Delivery availability (event accepted and persisted): `99.9%` monthly
- Dispatch success ratio (SENT / all attempted dispatches): `>= 98%` per rolling 24h
- P95 dispatch latency (worker send attempt): `<= 3s`
- Retry backlog recovery: backlog returns below warning threshold within `30m`

## Key Metrics

- `communication.delivery.ingest.status{status=*}`
- `communication.delivery.dispatch.status{status=*,channel=*,provider=*}`
- `communication.delivery.dispatch.latency`
- `communication.delivery.retry.queue.depth`
- `communication.provider.dispatch{channel=*,provider=*,status=*}`
- `communication.provider.dispatch.latency{channel=*,provider=*}`

## Alert Thresholds

Configured in `application.yml`:

- `communication.phase6.alerts.backlog.warn` (default `25`)
- `communication.phase6.alerts.backlog.critical` (default `50`)
- `communication.phase6.alerts.failure-ratio.warn` (default `0.20`)
- `communication.phase6.alerts.failure-ratio.critical` (default `0.40`)

Operational endpoints:

- `GET /api/communications/operations/alerts`
- `GET /api/communications/operations/secrets/status`

## Incident Playbooks

### 1) Retry backlog spike

- Confirm `retry_backlog` alert from operations endpoint.
- Inspect queued/retrying deliveries via `GET /api/communications/deliveries?status=QUEUED|RETRYING`.
- Verify provider health and recent failure reasons.
- Increase worker batch size (`COMMUNICATION_PHASE6_WORKER_BATCH_SIZE`) if capacity bound.
- If provider outage is active, keep queueing enabled and monitor DLQ growth.

### 2) Failure ratio spike

- Check dominant `failureCategory` (`PERMANENT`, `TRANSIENT`, `RETRY_EXHAUSTED`).
- For permanent failures, validate template activation/version and payload schema compatibility.
- For transient failures, monitor retry progression and provider status.
- Enable controlled resend for critical failed items through `/deliveries/{id}/resend`.

### 3) Provider degradation/outage

- Verify `/operations/alerts` provider health signal.
- Validate secret readiness at `/operations/secrets/status`.
- For missing credentials, rotate/reload env/secret source, then validate health.
- For downstream outage, allow retry/DLQ policy to absorb; avoid manual replay until provider stabilizes.

## Security Controls

- Recipient PII is masked in logs (email local part and phone values).
- Provider credentials must come from environment or secret manager, never from source code.
- Use `/operations/secrets/status` only to verify configured/not-configured state.

## Recovery and Replay

- Use `POST /api/communications/deliveries/{id}/resend` for targeted replay.
- Prefer replay only after root cause is resolved (template, payload, provider, or credentials).
- Track resend requests with actor and reason for auditability.
