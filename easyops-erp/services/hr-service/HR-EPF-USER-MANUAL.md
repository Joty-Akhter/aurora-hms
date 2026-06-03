## HR Provident Fund (EPF) – User Manual

> **See also:** [docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md](docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md) (unified HR manual + [knowledge base index](docs/knowledge-base/README.md)).

This document explains how HR and Payroll users should configure and use Employee Provident Fund (EPF) features in `hr-service`, from account setup through contributions, interest, withdrawals, and compliance.

### Purpose

- Provide a **guided, business-friendly walkthrough** for configuring and operating EPF management in EasyOps.
- Ensure **accurate, auditable** EPF contributions and balances by explaining how payroll integration, interest calculation, and withdrawal workflows operate.
- Help HR, Payroll, and Finance teams meet **statutory compliance** requirements for EPF reporting and remittance.

### Feature Overview

- **EPF Account Management**: Create and maintain EPF accounts for employees with account numbers, UAN (Universal Account Number), and balance tracking.
- **Contributions**: Employee and employer contributions derived from payroll or entered manually, with configurable rates (default 12% each).
- **Interest Calculation**: Annual interest calculation per financial year (April–March) on running monthly balances.
- **Withdrawals**: Withdrawal requests (partial, full, advance, pension) with approval and payment processing workflows.
- **Transfers**: Transfer balance between EPF accounts when employees change employers.
- **Nominations**: Nominee management for account beneficiaries.
- **Statements**: Account statements showing contributions, withdrawals, transfers, and balances for a date range.
- **Compliance**: Compliance records, statutory contribution tracking, and reporting.
- **Organization EPF policy**: Configure contribution rates, PF wage ceiling/floor, and employment eligibility per organization (UI and API; see §2).
- **Payroll Integration**: Create EPF contributions from processed payroll runs (PF_EMPLOYEE and PF_EMPLOYER components).
- **Accounting Integration**: Post EPF contributions to accounting (journal entry for EPF payable).
- **Employee Self-Service**: Employees can view their account, contributions, withdrawals, nominations, and download statements.
- **Reporting**: Executive dashboard, cost analysis, trend analysis, and compliance reports.

---

### 1. Prerequisites

- **Access**: You must have appropriate HR / Payroll Admin permissions for your organization.
- **Master Data**:
  - Organizations and employees exist in HR master data.
  - Payroll system configured with salary components including **PF_EMPLOYEE** and **PF_EMPLOYER** (for payroll-driven contributions).
- **Optional**: Salary Structure and Payroll configured per HR-SALARY-USER-MANUAL.md for automated EPF from payroll.

---

### 2. Organization EPF policy (rates and PF wage limits)

Payroll uses an **organization-level EPF policy** row to apply **employee and employer contribution rates**, optional **PF wage ceiling and floor**, and optional **employment-type** eligibility (who may participate in PF). You should configure this before relying on **deferred statutory PF** lines in payroll (components with `STATUTORY` basis and PF employee/employer types).

**UI**

1. Go to **HR** → **Provident Fund** (dashboard).
2. Open **EPF organization policy** (or navigate to `/hr/provident-fund/organization-policy`).
3. Enter:
   - **Employee contribution rate (%)** and **Employer contribution rate (%)** — defaults are often 12% each; adjust per your statutory or company rules.
   - **PF wage ceiling** (optional) — maximum wages considered for PF in a period; leave blank for no cap.
   - **PF wage floor** (optional) — minimum PF wage to qualify, if your policy uses one.
   - **Eligible employment types** (optional) — comma-separated list (e.g. `FULL_TIME,PART_TIME`). If empty, eligibility is not restricted by this list.
   - **Ineligible employment types** (optional) — comma-separated types excluded from PF (e.g. `INTERN`).
4. Click **Save policy**. If no row existed yet, saving **creates** the policy for the current organization.

**API (for automation):** `GET` and `PUT` `/api/hr/epf/organization-policy?organizationId=...` — same fields as the UI.

**Note:** PF wage in payroll is the sum of **PF_WAGE**-tagged earning lines (after component rules and proration). See HR-SALARY-USER-MANUAL.md for tagging and populate warnings.

---

### 2. EPF Account Management

#### 3.1 What is an EPF Account?

An EPF account holds an employee’s provident fund balance. Each employee has one active EPF account per organization. The account tracks:
- **Employee contribution balance** – Amount contributed by the employee (deducted from salary).
- **Employer contribution balance** – Amount contributed by the employer.
- **Interest balance** – Interest earned on the fund.
- **Current balance** – Total of employee + employer + interest.

#### 3.2 Create an EPF Account (Step-by-Step)

1. Go to **HR** → **Provident Fund** → **EPF Accounts**.
2. Click **Create Account**.
3. Enter:
   - **Employee ID** (required) – The employee for whom the account is created.
   - **EPF Account Number** (required) – Unique identifier; must be unique within the organization.
   - **UAN Number** (optional) – Universal Account Number if assigned by the statutory authority.
4. Save. The system validates that:
   - The EPF account number does not already exist for this organization.
   - The employee exists in the system.
5. If you use **Process EPF from Payroll** (see Section 6), the system can **auto-create** an EPF account when an employee has PF components in payroll but no account yet. The auto-generated number is `EPF-{EmployeeNumber}` (or with a suffix if duplicate).

**Account status values**: `active`, `closed`, `transferred`.

#### 2.3 View and Manage EPF Accounts

1. The EPF Accounts list shows: Account Number, UAN, Employee ID, Current Balance, Status.
2. Click **View** on an account to see:
   - Employee contribution balance
   - Employer contribution balance
   - Interest balance
   - Current balance
   - Opening date, last contribution date, last interest calculation date
3. Accounts can be marked **inactive** when an employee is terminated or transferred.

---

### 4. EPF Contributions

#### 4.1 Contribution Calculation

Contributions are calculated as:
- **Employee contribution** = `Employee Basic Salary × Employee Contribution Rate / 100`
- **Employer contribution** = `Employee Basic Salary × Employer Contribution Rate / 100`
- **Total contribution** = Employee + Employer

**Default rates**: 12% for both employee and employer (configurable per contribution or via **organization EPF policy** — see §2).

#### 3.2 Create Contribution Manually

Use this when contributions are **not** driven by payroll (e.g. adjustments, corrections, manual entry).

1. Go to **HR** → **Provident Fund** → **Contributions**.
2. Click **Create Contribution**.
3. Enter:
   - **EPF Account ID** (required)
   - **Employee ID** (required)
   - **Contribution Month** (1–12)
   - **Contribution Year**
   - **Basic Salary** (for that period)
   - **Employee Contribution Rate (%)** – default 12
   - **Employer Contribution Rate (%)** – default 12
4. Click **Create**. The system will:
   - Calculate employee and employer amounts if not provided
   - Check for duplicate contribution for the same account and period (one per month per account)
   - Create the contribution and **update the EPF account balance**

**Validation**: Duplicate contributions for the same account and month/year are rejected.

#### 4.3 View Contributions

1. **By account**: Use `GET /api/hr/provident-fund/contributions/account/{epfAccountId}` or the Contributions UI filtered by account.
2. **By period**: Use `GET /api/hr/provident-fund/contributions/period?organizationId=...&month=...&year=...` to see all contributions for a given month/year organization-wide.

#### 3.4 Contribution Status

- **pending** – Created but not yet processed.
- **processed** – Processed (e.g. from payroll or manually); account balance updated.

---

### 4. Interest Calculation

#### 5.1 Financial Year

EPF interest is calculated per **financial year** (April to March):
- Financial year 2024 = April 1, 2024 – March 31, 2025.

#### 5.2 Calculate Interest for an Account (Step-by-Step)

1. Go to **HR** → **Provident Fund** → **Interest** (or use API `POST /api/hr/provident-fund/interest/calculate?epfAccountId=...&financialYear=...&interestRate=...`).
2. Provide:
   - **EPF Account ID**
   - **Financial Year** (e.g. 2024 for FY 2024-25)
   - **Interest Rate (%)** – e.g. 8.15 as per statutory rate
3. Execute. The system will:
   - Check that interest has **not** already been calculated for this account and year
   - Compute **opening balance** (closing balance of previous FY or zero)
   - Fetch all contributions in the financial year (April–March)
   - Calculate interest **monthly** on running balance:
     - `Monthly rate = Annual rate / 12 / 100`
     - For each contribution month: `Interest = Running balance × Monthly rate`
     - Add contribution to running balance for next month
   - Compute **closing balance** = Opening + Total contributions + Interest
   - Update the EPF account: add interest to `interestBalance` and `currentBalance`
   - Store `EpfInterestCalculation` record

**Important**: Interest can only be calculated once per account per financial year. Run it after all contributions for the year are finalized.

#### 4.3 View Interest Calculations

- Use `GET /api/hr/provident-fund/interest/account/{epfAccountId}` to list all interest calculations for an account.

---

### 6. Payroll Integration (Process EPF from Payroll)

#### 6.1 Prerequisites

- Payroll run in status **PROCESSED** or **APPROVED**.
- Payroll has **Populate from Salary** and **Process** completed.
- Salary components include **PF_EMPLOYEE** and **PF_EMPLOYER** with codes or statutory type matching:
  - `PF_EMPLOYEE` or `PF_EMP` or statutory type `PF_EMPLOYEE`
  - `PF_EMPLOYER` or `PF_EMPR` or statutory type `PF_EMPLOYER`

#### 5.2 Process EPF from Payroll (Step-by-Step)

1. Run payroll per HR-SALARY-USER-MANUAL.md:
   - Create run → Populate from Salary → Process (optionally Approve).
2. In Payroll Runs (or via API), for the processed run, call **Process EPF**:
   - API: `POST /api/hr/payroll/runs/{runId}/process-epf`
3. The system will:
   - Extract PF_EMPLOYEE and PF_EMPLOYER amounts from each employee’s payroll components
   - For each employee with PF amounts:
     - **Get or create** EPF account (auto-create if none exists)
     - **Create or update** EpfContribution for the payroll period (month/year from period end)
     - Set employee basic salary, employee/employer contribution amounts, total
     - Link contribution to `payrollRunId`
     - Update EPF account balance
4. Review the response:
   - `contributionsCreated` – New contributions created
   - `contributionsUpdated` – Existing contributions updated (e.g. re-run)
   - `employeesSkipped` – Employees with no EPF account created (check errors)
   - `errors` – List of error messages for failed employees

#### 6.3 Post EPF to Accounting

After EPF contributions are created:

1. Call **Post EPF to Accounting**:
   - API: `POST /api/hr/payroll/runs/{runId}/post-epf-to-accounting`
2. The system will:
   - Aggregate total employee + employer contributions for the payroll period
   - Create a journal entry: Debit EPF_PAYABLE, Credit CASH (or equivalent)
   - Requires Chart of Accounts with `EPF_PAYABLE` and `CASH` (or configured codes)

**Order of operations**: Process Payroll → Process EPF → Post Payroll to Accounting → Post EPF to Accounting (can be done in parallel with payroll posting).

---

### 6. Withdrawals

#### 7.1 Withdrawal Types

- **partial** – Partial withdrawal (e.g. for specific purposes)
- **full** – Full withdrawal (typically when employee is terminated)
- **advance** – Advance against balance (e.g. for house, medical, education)
- **pension** – Pension-related withdrawal

#### 7.2 Create Withdrawal Request (Step-by-Step)

1. Go to **HR** → **Provident Fund** → **Withdrawals** (or Employee Self-Service for employees).
2. Click **Create Withdrawal** / **Submit Withdrawal Request**.
3. Enter:
   - **EPF Account ID** (required)
   - **Employee ID** (required)
   - **Organization ID** (required)
   - **Withdrawal Type** (partial, full, advance, pension)
   - **Requested Amount** (required) – Must not exceed account current balance
   - **Withdrawal Reason** (optional)
   - **Reason** (optional, detailed)
4. Submit. The system will:
   - Validate that requested amount ≤ current balance
   - Validate withdrawal type rules (e.g. full withdrawal may require termination)
   - Set status to **pending**

#### 7.3 Withdrawal Workflow

| Step | Action           | Status Before | Status After   | API / UI                        |
|------|------------------|---------------|----------------|---------------------------------|
| 1    | Create request   | —             | pending        | POST /provident-fund/withdrawals |
| 2    | Approve          | pending       | approved       | PUT /withdrawals/{id}/approve    |
| 3    | Process payment  | approved      | processed      | PUT /withdrawals/{id}/process   |

**Approve**:
- Provide `approvedBy` (UUID) and optionally `approvedAmount` (if different from requested).
- Only pending withdrawals can be approved.

**Process**:
- Provide `paymentReference` (e.g. bank reference, cheque number).
- The system deducts `approvedAmount` from the EPF account balance and sets status to **processed**.

#### 7.4 Validation Rules

- Withdrawal amount cannot exceed account current balance.
- Full withdrawal may require additional validation (e.g. employee termination status) – implement per local EPF Act rules.

---

### 8. Transfers

#### 8.1 When to Use Transfers

When an employee moves from one employer (organization) to another, their EPF balance can be transferred from the source account to a target account.

#### 8.2 Create and Process Transfer (Step-by-Step)

1. **Create transfer request**:
   - API: `POST /api/hr/provident-fund/transfers`
   - Body: `sourceEpfAccountId`, `targetEpfAccountId` (optional), `transferAmount`
2. Validation: `transferAmount` ≤ source account current balance.
3. **Process transfer**:
   - API: `PUT /api/hr/provident-fund/transfers/{id}/process`
4. The system will:
   - Deduct from source account
   - Add to target account (if provided)
   - Set status to **processed**

---

### 9. Nominations

#### 9.1 What are Nominations?

Nominations designate beneficiaries who will receive the EPF balance in case of the account holder’s death.

#### 8.2 Create Nomination (Step-by-Step)

1. Go to **HR** → **Provident Fund** → **Nominations** (or Employee Self-Service).
2. Click **Add Nomination**.
3. Enter:
   - **EPF Account ID**
   - **Nominee Name**
   - **Relationship** (e.g. Spouse, Child, Parent)
   - **Date of Birth** (optional)
   - **Address**, **Phone**, **Email** (optional)
   - **Share Percentage** – Each nominee’s share of the balance (total must not exceed 100%)
   - **Is Primary** – Mark one as primary nominee
4. Save. The system validates:
   - Total share percentage ≤ 100%
   - If marked primary, other primary nominations are deactivated

#### 9.3 Update Nomination

- Use `PUT /api/hr/provident-fund/nominations/{id}` to update nominee details, share percentage, or primary flag.

---

### 10. Account Statements

#### 10.1 Generate Statement

1. **By account and date range**:
   - API: `GET /api/hr/provident-fund/statements/account/{epfAccountId}?fromDate=...&toDate=...`
2. The statement includes:
   - Account details
   - Opening balance (derived)
   - Contributions in period
   - Withdrawals in period
   - Transfers in period
   - Total contributions, total withdrawals
   - Closing balance (current balance)

#### 10.2 Employee Self-Service Statement

- Employees can download their statement via `GET /api/hr/provident-fund/employee/statements?employeeId=...&epfAccountId=...&startDate=...&endDate=...`
- Default range: last 1 year if dates not provided.

---

### 11. Compliance

#### 10.1 Compliance Records

Compliance records track statutory filing obligations (e.g. monthly returns, annual reconciliations).

1. **Create record**: `POST /api/hr/provident-fund/compliance`
2. **Update record**: `PUT /api/hr/provident-fund/compliance/{id}`
3. **List by organization**: `GET /api/hr/provident-fund/compliance/organization/{organizationId}`
4. **Overdue records**: `GET /api/hr/provident-fund/compliance/organization/{organizationId}/overdue`

#### 11.2 Statutory Contribution Total

- API: `GET /api/hr/provident-fund/compliance/statutory-contribution?organizationId=...&month=...&year=...`
- Returns total EPF contribution amount for the period (sum of all contributions).

#### 11.3 Compliance Service (ProvidentFundComplianceService)

The compliance service provides:
- **Check compliance** – Verifies contribution rates (12% employee, 12% employer) and filing status
- **Compliance report** – Filed, pending, overdue records for a date range
- **Monitor compliance** – Alerts for overdue and upcoming due dates
- **Calculate penalties** – Penalty amounts for overdue filings (example: 1% per month overdue)

---

### 12. Reporting

#### 12.1 Available Reports

| Report                | API Endpoint                                           | Description                                  |
|-----------------------|--------------------------------------------------------|----------------------------------------------|
| Executive Dashboard   | GET /provident-fund/reports/executive-dashboard         | High-level EPF metrics for organization      |
| Manager Team          | GET /provident-fund/reports/manager-team                | Team-level EPF summary by manager/department|
| Employee Statement    | GET /provident-fund/reports/employee-statement         | Detailed statement for an employee           |
| Compliance            | GET /provident-fund/reports/compliance                 | Compliance status for date range            |
| Cost Analysis         | GET /provident-fund/reports/cost-analysis             | Year-wise employer EPF cost                  |
| Trend Analysis        | GET /provident-fund/reports/trend-analysis             | EPF trends over N months                     |

#### 12.2 Advanced EPF Features (Optional)

- **Recommendations**: `GET /provident-fund/advanced/recommendations`
- **Optimize contributions**: `GET /provident-fund/advanced/optimize`
- **Forecast**: `GET /provident-fund/advanced/forecast`
- **Risk assessment**: `GET /provident-fund/advanced/risk-assessment`
- **Compliance check**: `GET /provident-fund/advanced/compliance/check`
- **Participation analytics**: `GET /provident-fund/advanced/analytics/participation`
- **Cost analytics**: `GET /provident-fund/advanced/analytics/costs`
- **ROI / Impact**: `GET /provident-fund/advanced/analytics/roi`, `.../impact`

---

### 12. Employee Self-Service

Employees (with appropriate roles) can:

1. **View EPF account**: `GET /api/hr/provident-fund/employee/account?employeeId=...`
2. **View contributions**: `GET /api/hr/provident-fund/employee/contributions?employeeId=...&epfAccountId=...`
3. **Submit withdrawal request**: `POST /api/hr/provident-fund/employee/withdrawals`
4. **View withdrawals**: `GET /api/hr/provident-fund/employee/withdrawals?employeeId=...`
5. **Download statement**: `GET /api/hr/provident-fund/employee/statements?employeeId=...&epfAccountId=...&startDate=...&endDate=...`
6. **View nominations**: `GET /api/hr/provident-fund/employee/nominations?employeeId=...&epfAccountId=...`
7. **Create/update nomination**: `POST /api/hr/provident-fund/employee/nominations`, `PUT .../nominations/{id}`

---

### 14. Summary: End-to-End EPF Workflows

#### Workflow A: Payroll-Driven EPF (Recommended)

1. Configure salary with PF_EMPLOYEE and PF_EMPLOYER components.
2. Create and populate payroll run.
3. Process payroll run.
4. **Process EPF** from payroll run → Creates/updates contributions, updates account balances.
5. **Post EPF to Accounting** (optional) → Journal entry for EPF payable.
6. Run **Interest Calculation** annually per financial year.

#### Workflow B: Manual EPF (No Payroll)

1. Create EPF account for each employee.
2. Create contributions manually each month (or use bulk import if available).
3. Update account balances automatically on each contribution.
4. Run interest calculation annually.

#### Workflow C: Withdrawal

1. Employee or HR creates withdrawal request.
2. HR/Finance approves (with optional amount adjustment).
3. Finance processes payment and records payment reference.
4. Account balance is deducted.

---

### 15. API Quick Reference

| Operation                    | Method | Endpoint                                                |
|-----------------------------|--------|---------------------------------------------------------|
| Create EPF account          | POST   | /api/hr/provident-fund/accounts                        |
| Get accounts (org)          | GET    | /api/hr/provident-fund/accounts?organizationId=...     |
| Get account by ID           | GET    | /api/hr/provident-fund/accounts/{id}                   |
| Create contribution         | POST   | /api/hr/provident-fund/contributions                  |
| Get contributions by account| GET    | /api/hr/provident-fund/contributions/account/{id}      |
| Get contributions by period | GET    | /api/hr/provident-fund/contributions/period?org&month&year |
| Calculate interest          | POST   | /api/hr/provident-fund/interest/calculate              |
| Create withdrawal           | POST   | /api/hr/provident-fund/withdrawals                     |
| Approve withdrawal          | PUT    | /api/hr/provident-fund/withdrawals/{id}/approve        |
| Process withdrawal          | PUT    | /api/hr/provident-fund/withdrawals/{id}/process         |
| Create transfer             | POST   | /api/hr/provident-fund/transfers                      |
| Process transfer            | PUT    | /api/hr/provident-fund/transfers/{id}/process          |
| Create nomination           | POST   | /api/hr/provident-fund/nominations                     |
| Get statement               | GET    | /api/hr/provident-fund/statements/account/{id}          |
| Process EPF from payroll    | POST   | /api/hr/payroll/runs/{id}/process-epf                  |
| Post EPF to accounting      | POST   | /api/hr/payroll/runs/{id}/post-epf-to-accounting      |

Use this manual together with API documentation (OpenAPI/Swagger for `hr-service`) when integrating EPF with payroll, accounting, or statutory filing systems.
