# Communication Service Phase 6 Runbook

## Observability Baseline

- Prometheus endpoint: `/actuator/prometheus`
- Primary metrics:
  - `communication.delivery.ingest.status{status=*}`
  - `communication.delivery.ingest.duplicate`
  - `communication.delivery.dispatch.status{status,channel,provider}`
  - `communication.delivery.dispatch.latency`
  - `communication.delivery.retry.queue.depth`
  - `comm.phase4.producer.*` metrics in scheduling and billing producers

## Suggested Alert Thresholds (staging -> production)

- Dispatch failure spike:
  - Trigger when `FAILED + DLQ` status increments exceed 5% of `SENT` in a 15m window.
- Retry backlog growth:
  - Trigger when `communication.delivery.retry.queue.depth > 200` for 10 minutes.
- Provider outage signal:
  - Trigger when `status=RETRYING` grows continuously for 15 minutes with low `SENT`.
- Ingest stalled:
  - Trigger when producer publish metrics are positive but communication-service ingest counters are flat for 10 minutes.

## Incident Playbooks

### 1) Failure spike
1. Check provider health endpoint (`/api/communication-templates/providers/health`).
2. Inspect recent deliveries using `status=FAILED` and `status=DLQ`.
3. If provider issue is confirmed, switch to degraded mode (disable impacted channel/event flag at producer).
4. Track recovery and issue manual resend for priority records after provider recovery.

### 2) Retry queue growth
1. Check `communication.delivery.retry.queue.depth`.
2. Inspect dominant channel/provider in dispatch status metrics.
3. Increase worker throughput temporarily (reduce worker poll interval).
4. Verify drain trend before restoring normal polling.

### 3) Producer/consumer mismatch
1. Compare producer contract payload with validator requirements.
2. Confirm event type mapping and recipient field presence.
3. Use feature flag rollback for affected flow until contract hotfix is deployed.

## Manual Resend Procedure

1. Open Communication Operations UI (`/communication/operations`).
2. Filter by `FAILED`, `DLQ`, or `SKIPPED`.
3. Validate root cause is resolved.
4. Trigger **Resend Failed** action.
5. Confirm transition to `QUEUED` then `SENT` via delivery drill-down.

## Load Test Baseline (minimum)

- Scenario A: 50 events/sec appointment SMS for 10 minutes.
- Scenario B: 20 events/sec invoice email for 10 minutes.
- Success criteria:
  - `P95 dispatch latency < 1.5s`
  - `FAILED + DLQ < 2%` excluding injected failure runs
  - Retry backlog returns to baseline within 15 minutes after test stop
