# Employee self-service (HR loans) — Detailed guide

This article describes **employee-facing** loan features in EasyOps. **Prerequisite:** the **employee** record must be linked to the **user** logged into the portal.

---

## 1. My loans

**Path:** `/hr/my-loans`

### 1.1 What you see

- **Your** loan accounts for the **current organization** (when you have multiple org memberships, switch org in the app as provided).
- **Balances** and **installment schedules** (per implementation).
- **Drill-down** to a single loan: `/hr/my-loans/:loanId`

### 1.2 What you cannot do here

- **Approve** loans or **disburse** funds — HR/Finance roles only.
- **Change** org-wide policy — **Loan org settings** are administrative.

---

## 2. In-app notifications (RE-03)

**Path:** `/hr/my-loans` (notification table/section on the same page)

### 2.1 Event types (examples)

- Application **submitted**, **approved**, **rejected**, **HR approved pending Finance**, etc.
- **Loan disbursed** — After disbursement is recorded.
- **Settlement required** — When exit settlement is started for your loan.
- **Payment due soon** — Reminders for installments due within a **rolling window** (scheduled job; configurable).

### 2.2 Mark as read

Use **mark read** actions (per event) when available — keeps your feed manageable.

### 2.3 Email and SMS

The **in-app feed** is the standard channel in the current implementation. **Email/SMS** delivery depends on **future** platform integration and is not guaranteed from this screen alone.

---

## 3. Payslips and salary

For **payslip download** and **salary breakdown**, use the screens described in the [**Salary user manual**](../../HR-SALARY-USER-MANUAL.md). The **My Salary** payslip view (`/hr/my-salary`) can show **taxable gross (period)**, **year-to-date** totals, and line-level **taxability / statutory / PF wage / ESI** columns when returned by the API.

---

## 4. Provident fund self-service

See [**HR EPF user manual**](../../HR-EPF-USER-MANUAL.md) for **My PF account**, statements, and nominations.

---

## 5. Troubleshooting

| Issue | Resolution |
|-------|------------|
| **My loans** is empty | Confirm HR linked your **employee** to your **user**; select correct **organization**. |
| No **notifications** | Events may not fire if no user link; some events apply only after application/disbursement/settlement. |
| Wrong balance | Ask HR to verify **loan account** and **payroll postings**; use official channels for disputes. |

---

*Back to [Knowledge base home](../README.md) · [Master manual](../EASYOPS-HR-USER-MANUAL.md)*
