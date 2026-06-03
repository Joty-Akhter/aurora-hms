# Phase 6: Territory-Specific Incentive Rules — Rollout Checklist

## Pre-Deployment

| Task | Owner | Status |
|------|-------|--------|
| Run unit tests: `IncentiveRuleServiceTest`, `IncentiveServiceTest` | | |
| Run controller tests: `IncentiveRuleControllerTest` | | |
| Backup production database | | |
| Verify Liquibase contexts for target environment | | |
| Communicate to users: rules must be defined per territory | | |

## Deployment Order

| Step | Action | Notes |
|------|--------|-------|
| 1 | Deploy **backend** (pharma-service) first | New API and calculation logic |
| 2 | Run database migration (Liquibase on startup or manual) | Creates rules for territories missing them |
| 3 | Verify migration (see runbook Section 3) | |
| 4 | Deploy **frontend** (IncentiveRulesManagement) | Can work with new API |
| 5 | Smoke test: create rule → run calculation → verify distributions | |

## Post-Deployment Verification

| Check | Expected |
|-------|----------|
| `GET /api/pharma/incentive-rules/territory/{id}` | 200 with rule or 404 if no rule |
| `POST /api/pharma/incentive-rules` (valid request) | 201 Created |
| `POST /api/pharma/incentive-rules` (invalid, e.g. sum≠100%) | 400 Bad Request |
| `POST /api/pharma/incentives/calculate` (territory with rule) | 200, distributions include SR_SHARE, DEVELOPMENT_FUND, MPO_SHARE, MANAGER_SHARE |
| `POST /api/pharma/incentives/calculate` (territory without rule) | 500 with message "Incentive rule is required" |

## Optional: Feature Flag

If using a feature flag `territory_specific_incentive_rules`:

- **Off**: Use legacy calculation (if still available) or block calculation with message
- **On**: Use new territory-specific rules and allocation model

## User Communication Template

> **Territory-Specific Incentive Rules**
>
> Incentive rules are now defined **per territory**. Each territory must have its own rule with:
> - SR share % (default 9%)
> - Development fund % (default 1%)
> - Per-employee allocation % for Managers and MPOs (must sum to 100%)
>
> Territories without a rule cannot run incentive calculation. Please define rules for all territories with targets via **Pharma → Incentive Rules**.
>
> For territories with targets, rules have been auto-created where possible. Review and adjust allocations as needed.

## Sign-Off

| Role | Name | Date |
|------|------|------|
| Dev | | |
| QA | | |
| Ops | | |
