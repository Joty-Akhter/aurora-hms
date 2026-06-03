## 🔹 Security & Audit – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Roles, Permissions, and SoD
- 3. Approvals & Thresholds
- 4. Audit Trail & Evidence
- 5. Period Close & Locks
- 6. Backup, Restore, and Retention
- 7. Compliance & External Audit Support
- 8. Authentication & 2FA
- 9. Monitoring & Alerts

---

### 1. Purpose & Scope
- Enforce secure access, proper approvals, and a complete audit trail for accounting processes
- Align with cross-cutting security requirements while specifying accounting needs

---

### 2. Roles, Permissions, and SoD
- Roles & Permissions
  - View/Post/Edit/Approve by module (GL, AP, AR, Bank, Period Close)
  - Branch/Company scoped access; sensitive accounts masked
- Segregation of Duties (SoD)
  - Conflict rules (e.g., create + approve + post); prevent or require compensating approval
- Access Reviews
  - Periodic user access certification; report of rights changes

---

### 3. Approvals & Thresholds
- Maker-Checker
  - Journals, payments, revaluations, period close require approvals based on amount/account/dimension
- Thresholds
  - Multi-level approvals; delegation and escalation; SLAs and reminders
- Evidence
  - Attachments and comments captured at approval steps

---

### 4. Audit Trail & Evidence
- Logs
  - Immutable logs for create/edit/approve/post/reverse; before/after values; correlation IDs
- Search & Export
  - Filter by user/module/date; export to CSV/PDF for auditors
- Retention
  - Configurable retention; legal hold support

---

### 5. Period Close & Locks
- Locks
  - Soft/hard period locks; override with justification and approver trail
- Pre-Close Checks
  - Unposted journals, pending approvals, reconciliation variances
- Reopen
  - Controlled with audit justification; change impact report

---

### 6. Backup, Restore, and Retention
- Backups
  - Scheduled full/incremental backups; encryption; integrity checks
- Restore
  - Point-in-time restore procedures; tested runbooks; access-controlled
- Retention
  - Policy-driven data retention; purge with approvals; audit of purges

---

### 7. Compliance & External Audit Support
- Standards
  - Mapping and reports aligned to IFRS/GAAP; change logs for policy updates
- Audit Support
  - Auditor read-only scoped access; evidence package exports (logs, reports, samples)

---

### 8. Authentication & 2FA
- Requirements
  - 2FA for finance and admin roles; device/session management; IP allowlists (optional)
- Recovery
  - Backup codes; admin-assisted recovery with approvals

---

### 9. Monitoring & Alerts
- Monitoring
  - Dashboards for approvals pending, SoD violations, exceptions, failed postings
- Alerts
  - Threshold breaches, suspicious access, repeated failures; integrations with email/SMS/pager

---

