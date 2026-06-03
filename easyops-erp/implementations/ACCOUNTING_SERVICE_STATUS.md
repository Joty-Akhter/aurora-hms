# Accounting Module — Current Status

**Last updated:** May 2026

## Summary

The accounting domain is implemented as **five microservices** sharing the PostgreSQL `accounting` schema:

| Service | Port | Responsibility |
|---------|------|----------------|
| `accounting-service` | 8088 | GL, CoA, journals, fiscal periods, financial reports, dashboard |
| `ar-service` | 8090 | Customers, invoices, receipts, credit notes, AR aging |
| `ap-service` | 8091 | Vendors, bills, payments, AP aging |
| `bank-service` | 8092 | Bank accounts, transactions, reconciliation |

Migrations live in `easyops-erp/database-versioning/` (phase-1 accounting scripts).

---

## Implemented (Phase 1 core)

### General ledger (`accounting-service`)

- Chart of accounts CRUD, standard CoA load, code-change guard when posted activity exists
- Manual journal create / post / reverse (reversals use the same post path as normal entries)
- `allow-backdated: false` enforced for **manual** journals only; subledger/integration entries use document dates
- Fiscal year setup, monthly periods, open/close, `GET .../periods/for-date` for AR/AP period resolution
- Trial balance, general ledger, P&L, balance sheet
- **Cash flow** — indirect method from posted journals and working-capital movements (not a stub)
- Integration API: `POST/GET /api/accounting/journal-entries`
- Dashboard summary with explicit journal line totals for recent activity

### Subledger → GL posting

On **post** (not draft create), documents create a **POSTED** GL journal and set `gl_journal_id`:

| Document | Service | GL accounts (defaults) |
|----------|---------|-------------------------|
| AR invoice | `ar-service` | Dr AR 1110, Cr revenue lines / 4010, Cr tax 2030 |
| AR receipt | `ar-service` | Dr bank GL, Cr AR 1110 |
| AR credit note | `ar-service` | Reverses revenue/tax, Cr AR; sets `gl_journal_id` on post |
| AP bill | `ap-service` | Dr expense/AP control pattern |
| AP payment | `ap-service` | Dr AP, Cr bank |
| Bank transaction (optional) | `bank-service` | Dr/Cr bank GL + user-selected offset account when `postToGl=true` |

Period resolution for AR/AP documents uses `easyops-accounting-period-client` → accounting-service `periods/for-date`.

### Cross-module integration

- **HR / pharma:** `POST /api/accounting/journal-entries` (account codes)
- **Manufacturing:** posts via same integration API (no separate `/wip` routes)
- **Inventory:** receipt, COGS, adjustment journals via integration API

### Frontend (`/accounting/*`)

- 24+ pages including customers, fiscal year setup, outstanding/overdue, bank GL offset on transactions
- Post actions on invoices, bills, credit notes (optional auto-post on create)
- `useAccountingManage()` gates create/post/delete for view-only users
- Aging reports use API document-level rows; combined AR/AP aging tab

### RBAC

- `ACCOUNTING_VIEW` / `ACCOUNTING_MANAGE` on all financial services
- Dashboard aggregator forwards `X-User-Id` to AR/AP/bank internal calls

---

## Not implemented (future phases)

- Full tax engine, multi-currency, branches, budgets
- Journal approval workflow (DB columns exist; no workflow API)
- Recurring journal templates (schema only)
- Automated bank statement import
- Fine-grained permissions beyond view/manage

---

## Running locally

```bash
cd easyops-erp
docker-compose up -d postgres redis
./mvnw -pl services/accounting-service,services/ar-service,services/ap-service,services/bank-service -am package -DskipTests
# Start via scripts/start-spring-services.sh or dev-start.sh
```

Frontend: `cd frontend && npm run dev` — module preset `alien-pharma` enables accounting.

Default login: `admin` / `Admin123!`

---

## Tests

```bash
./mvnw -pl services/accounting-service -Dtest=ChartOfAccountsServiceTest,JournalPostingServiceTest test
./mvnw -pl services/ar-service -Dtest=GlJournalPostingServiceTest test
./mvnw -pl services/ap-service -Dtest=GlJournalPostingServiceTest test
```

See `implementations/ACCOUNTING_IMPLEMENTATION_GAP_ANALYSIS.md` for requirements mapping.
