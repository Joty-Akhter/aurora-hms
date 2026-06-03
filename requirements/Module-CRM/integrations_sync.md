# CRM: Integrations & Sync

## Table of Contents
- Purpose
- Connectors
- Sync Model
- Security & Governance
- Events
- Appendix

## Purpose
Keep CRM aligned with external systems and channels without duplication.

## Connectors
- Email/Calendar (Google/Microsoft)
- Telephony/dialer; SMS gateways
- Marketing platforms; landing pages
- ERP/Inventory for customers, products, orders

## Sync Model
- Ownership
  - Define source-of-truth per entity (Account/Contact/Product/Order)
  - Field-level ownership where necessary (e.g., CRM owns phone; ERP owns credit terms)
- Flow
  - Webhooks for near real-time; polling as fallback
  - Incremental sync with watermarks; idempotency keys
- Conflicts
  - Last-writer-wins or priority source; merge policies; review queues
- Mapping
  - Mapping templates; validation; error queues and retries

## Security & Governance
- OAuth2, scoped permissions, audit of data exchanges
- Rate limits, retries with backoff, dead-letter handling

## Events
- account.synced, contact.synced, activity.synced, order.synced
- error.queue.created, mapping.updated

## Appendix
- Mapping example: ERP.CustomerName → CRM.Account.Name; ERP.TaxId → CRM.Account.TaxId
