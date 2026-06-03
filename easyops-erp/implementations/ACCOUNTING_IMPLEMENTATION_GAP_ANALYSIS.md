# Accounting Module — Requirements vs Implementation

**Last updated:** May 2026

## Executive summary

| Area | Status |
|------|--------|
| **Phase 1.1** — CoA & GL | ✅ Largely complete |
| **Phase 1.2** — AR, AP, Bank | ✅ Operational APIs + UI; GL bridge on post |
| **Phase 1.3** — Integration | ✅ HR, pharma, manufacturing, inventory → journal integration API |
| **Phase 1.4** — Multi-currency & tax | ❌ Not started (CoA `taxType` only) |
| **Phase 1.5** — Close, budget, advanced reports | ⚠️ Partial (period close; no budgets) |

**Services deployed in codebase:** 4 financial microservices (+ shared schema), not the original 15-service plan.

---

## Phase 1.1 — Chart of accounts & GL ✅

| Feature | Status |
|---------|--------|
| CoA CRUD, hierarchy, types | ✅ |
| Standard CoA load | ✅ |
| Manual journals, balance validation | ✅ |
| Post / reverse | ✅ |
| Fiscal years & periods | ✅ |
| Period for date API | ✅ |
| Trial balance, GL, P&L, balance sheet | ✅ |
| Cash flow | ✅ Indirect method |
| CoA code change guard | ✅ |
| Backdated manual journals blocked (config) | ✅ |

---

## Phase 1.2 — AR, AP, Bank ✅

| Feature | Status |
|---------|--------|
| AR customers, invoices, receipts, credit notes | ✅ |
| AP vendors, bills, payments | ✅ |
| Bank accounts, transactions, reconciliation | ✅ |
| Aging & statements | ✅ |
| Payment reminders | ✅ |
| **GL posting on post** | ✅ Sets `gl_journal_id` |
| Period auto-resolve on create | ✅ |

---

## Phase 1.3 — Automation & integration ✅

| Integration | Status |
|-------------|--------|
| Journal integration `POST /journal-entries` | ✅ |
| `GET /journal-entries/{id}` | ✅ |
| AR/AP subledger posting | ✅ |
| Manufacturing (WIP, material, labor, etc.) | ✅ Via integration API |
| Inventory movements | ✅ |
| HR payroll / EPF | ✅ |

---

## Phase 1.4+ — Not implemented ❌

- Multi-currency, branches, consolidated statements
- Tax engine, returns, e-filing
- Recurring journals UI/API
- Budget vs actual
- Journal approval workflows
- Cost centers / projects on all reports (partial schema on lines)

---

## Known limitations

1. **Draft subledger documents** do not affect GL until posted.
2. **Bank GL** requires bank account linked to GL + user-selected offset account.
3. **Cash flow** is indirect-method estimates from GL; not a substitute for bank-cash reconciliation detail.
4. **CRM/sales customers** are separate from AR `customers` table.

---

## Related docs

- `implementations/ACCOUNTING_SERVICE_STATUS.md` — runbook & feature list
- `requirements/Module-Accounting/` — original requirements
- `services/accounting-service/docs/` — CoA user manual & knowledge base
