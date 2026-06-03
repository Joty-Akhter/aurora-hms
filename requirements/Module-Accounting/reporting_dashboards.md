## 🔹 Reporting & Dashboards – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Report Catalog (Accounting)
- 3. Filters, Layouts, and Drill-Down
- 4. Dashboards & KPIs
- 5. Budget vs Actuals
- 6. Scheduling & Distribution
- 7. Security & Governance
- 8. Performance & Data Quality
- 9. Exports & Integrations

---

### 1. Purpose & Scope
- Provide fast, flexible reporting and dashboards for GL and subledgers with drill-down and scheduling
- In-scope: standard financials, operational reports, KPIs, custom layouts, security, and exports

---

### 2. Report Catalog (Accounting)
- Financials
  - General Ledger, Trial Balance, Profit & Loss, Balance Sheet, Cash Flow
- Subledger/Operational
  - AR/AP Aging (summary/detail), Bank Reconciliation Summary/Detail, GR/IR Analysis
  - Payment Batches, Unapplied Advances, Credit/Debit Note Registers
- Management
  - Trend/TTM, Dimension Cubes (Branch/Department/Cost Center/Project), Variance (Actual vs Budget)

---

### 3. Filters, Layouts, and Drill-Down
- Filters
  - Company/Branch, Period/Date Range, Account(s)/Ranges, Dimensions, Currency
- Layouts
  - Save custom column sets, groupings, sorting; share with roles/teams
- Drill-Down
  - Report → Account/Line → Ledger → Voucher → Source Document
  - Back navigation, breadcrumbs, and export at each level

---

### 4. Dashboards & KPIs
- Dashboards
  - Customizable widgets (financial summaries, trends, exceptions); role-based presets
- KPIs
  - Built-in: Gross Margin, Net Profit, DSO/DPO, Cash Coverage Days, Auto-Match %
  - Custom KPI builder: formula fields across report outputs
- Alerts
  - Threshold breaches; trend anomalies; scheduled digests

---

### 5. Budget vs Actuals
- Setup
  - Import budgets by account/dimension/period; versioning
- Reports
  - Variance (absolute, %), drill to underlying entries; comments/justifications
- Controls
  - Optional posting blocks or warnings when breaching budget (by module)

---

### 6. Scheduling & Distribution
- Scheduling
  - One-off or recurring deliveries; time zone aware; retry on failure
- Distribution
  - Email with attachments/links; SFTP/file drop; in-app notifications
- Audit
  - Delivery logs; recipient read flags (where supported)

---

### 7. Security & Governance
- Security
  - Row-level security by branch/company; column masking for sensitive accounts
- Governance
  - Report definition versioning; approval for shared/official reports
  - Data dictionary and lineage; change log for calculated fields

---

### 8. Performance & Data Quality
- Performance
  - Server-side aggregation; pagination/streaming for large results; caching frequent views
- Data Quality
  - As-of reporting; snapshot at close; change impact indicators for backdated entries

---

### 9. Exports & Integrations
- Exports
  - CSV/Excel/PDF with consistent formatting; print-friendly layouts
- Integrations
  - API access to reports with filters; webhooks for scheduled completion; BI connectors

---

