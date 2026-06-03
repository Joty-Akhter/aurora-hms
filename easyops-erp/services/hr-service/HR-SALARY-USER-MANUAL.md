## HR Salary Management – User Manual

> **See also:** [docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md](docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md) (unified HR manual + [knowledge base index](docs/knowledge-base/README.md)).

This document explains how HR and Payroll users should configure and use salary features in `hr-service`, from salary configuration through payroll run and payslip generation.

### Purpose

- Provide a **guided, business-friendly walkthrough** for configuring and operating HR salary management in EasyOps (structures, components, and employee salaries).
- Ensure **accurate, auditable payroll** by explaining how effective dates, validations, and payroll period locks work in practice.
- Help HR, Payroll, and Finance teams **use the same salary model** for compensation planning, payroll processing, and statutory/compliance reporting.

### Feature overview

- **Salary structures (grades & bands)**: Define pay frameworks per organization, with grades and pay ranges (bands) by currency, pay frequency, and effective dates.
- **Salary components**: Configure earnings and deductions (Basic, HRA, allowances, PF, tax, loans, incentives, etc.) with calculation rules (fixed, percentage, formula, statutory, manual), taxability, and statutory tags.
- **Employee salary assignments**: Assign structures/grades/bands and component values to employees with full effective‑dated history and revision reasons.
- **Payroll calculation & payslips**: Run payroll using salary assignments and components to derive per-component amounts, gross earnings, deductions, and net pay, and generate payslips. When daily **attendance** or **approved timesheets** exist for the pay period, the system can also roll up **working days, present days, paid leave, loss of pay (LOP), and overtime** into each employee’s payroll row and adjust gross and deductions accordingly (see §4.2b).
- **Integrations**: Feed and consume data from PF, tax, incentives, loans, accounting/GL, and core HR (employees, positions) using consistent salary semantics.
- **Reporting & audit**: Use standard reports (structure, component master, employee salaries, revisions, headcount/cost, component-wise cost) and audit logs for compliance and analysis.
- **Employee self-service**: Allow employees to view their current salary summary (subject to policy) and download historical payslips.

---

### 1. Prerequisites

- **Access**: You must have appropriate HR / Payroll Admin permissions for your organization.
- **Master data**:
  - Organizations and locations are configured.
  - Employees and positions exist in the HR master data.
  - Optional: Positions have default salary structure/grade/band set.

---

### 2. Salary Configuration

#### 2.1 Create and maintain salary structures

1. Open the HR administration UI and go to the **Salary Structures** screen (backed by `SalaryStructure` and `SalaryGrade`/`SalaryBand` in `hr-service`).
2. Click **+ New Salary Structure**.
3. Enter:
   - **Code** – unique per organization; immutable after creation (maps to `SalaryStructure.code`).
   - **Name** and **Description**.
   - **Organization**, **Pay frequency**, and **Currency** (maps to `SalaryStructure.payFrequency` and `SalaryStructure.currency`).
   - **Effective from** (and optional **Effective to**).
   - (Optional) Mark as **Default** structure for the organization/frequency.
4. Save. Fix any inline validation errors (duplicate code, invalid dates, etc.).
5. To revise a structure later, create or update **grades** and **bands** with correct effective dates instead of changing closed-period data.

#### 2.2 Define grades and bands

1. From the structures list, choose a structure and click **Manage**.
2. In the **Grades** section:
   - Click **Add Grade**.
   - Enter **Code**, **Name**, **Display order/Level**, optional **Description**, and **Effective from/Effective to**.
   - Save; repeat for all grades.
3. For each grade, open the **Bands** / **Pay Ranges** section:
   - Click **Add Band**.
   - Enter **Name/Code**, **Currency** (defaults from structure), **Min**, **Max**, optional **Mid-point**, **Display order**, and effective dates.
   - Resolve validation errors:
     - `min < max`.
     - `min ≤ mid ≤ max` when mid is entered.
     - No overlapping ranges within the same grade.
4. Use the **Structure summary** view to see all grades and bands together and export to Excel/PDF if needed.
5. **Copy structure (optional):** When creating a new structure, you can copy **grades and bands** from an existing structure after save (the UI guides this flow).
6. **Manage grades and bands:** While managing grades/bands, the UI can show **warnings when a grade or band has no employees assigned**, using headcount from current assignments (for planning and data quality).

#### 2.3 Link salary defaults to positions (optional)

1. Position-level salary defaults are stored on the position (default structure, grade, band) and exposed by the API `GET /api/hr/salary/positions/{positionId}/salary-defaults`.
2. In **Salary Management** → **Employee Salaries**, open **Manage Assignment** for an employee who has a **position** linked. Use **Fill from position defaults** to copy structure, grade, and band from that position’s defaults (you can still edit before saving). If the employee has no position or the position has no defaults, use manual selection as usual.

#### 2.4 Configure salary components

1. Open **Salary Management** → **Salary Components** tab.
2. Click **+ New Component**.
3. Enter the component master fields:
   - **Code** (unique; immutable after creation; use values like `BASIC`, `HRA`, `PF_EMPLOYEE`, `PF_EMPLOYER`).
   - **Component Name**
   - **Type** (Earning/Deduction/Allowance)
   - **Category** (Basic, HRA, allowances, `STATUTORY_DEDUCTION`, `LOAN_REPAYMENT`, etc.)
   - **Effective From/Effective To** and **Display order**
   - Optional: **Short name** (payslip label), **Currency**
   - Tax and compliance fields: **Taxability**, **Statutory tags** (comma-separated), and the **Statutory** flag (if applicable)
4. Choose **Calculation basis** and fill dependent fields:
   - `FIXED` or `MANUAL`: set **Default amount**
   - `PERCENTAGE_OF_BASIC`: set **Percentage value** and the **Base component code**
   - `PERCENTAGE_OF_GROSS`: set **Percentage value**
   - `FORMULA`: enter **Formula expression** using component codes (e.g. `BASIC + HRA`); backend validates referenced components and blocks circular dependencies
   - `STATUTORY`: enter **Statutory type** (e.g. `PF_EMPLOYEE`, `INCOME_TAX`); during payroll population the amount is currently evaluated as `0`
5. Optionally set:
   - **Ceiling amount** and **Floor amount** (applied during payroll calculation)
   - **Proration rule (ES-29)** (stored on the component): **Populate from Salary** applies this rule to each component line after ceiling/floor — by default **BY_DAYS** (amount × days employed in period ÷ calendar days in period), or **NO_PRORATION** / **BY_HOURS** (fallback to day ratio if hours are not supplied). You can still use `/api/hr/salary/proration` for standalone estimates.
6. Click **Create Component** / **Update Component**. Resolve validation errors (duplicate code, invalid formula, effective date issues, ceiling/floor issues).
7. **Accounting (INT-20) — optional GL codes:** In the component form, under **Accounting**, you can enter:
   - **Expense account** — chart-of-accounts code used when this line is an **earning** (salary expense recognition).
   - **Liability account** — COA code used when this line is a **deduction** (withholding / payable).
   These values are **optional**. When set, they are included on **`GET /api/hr/payroll/runs/{id}/accounting-export`** for each detail and summary line so finance can build **component-level** journals. The in-app **Post to Accounting** button on a payroll run still posts a **summary** journal (fixed expense/deductions/bank pattern); use the export API if your process needs per-component accounts.
8. Use the UI exports if needed:
   - **Export Excel** / **Export PDF** (SC-48)
   - **Dependency report** (SC-49)
   - **Bulk import** (SC-50)

---

### 3. Employee Salary Management

#### 3.1 Assign structure, grade, and band to employees

1. Open **Salary Management** → **Employee Salaries** tab.
2. (Optional) Set the **As-of date** filter to see data effective on a specific date.
3. In the grid, click the employee name to open the side panel **Employee Salary – {employee}**.
4. In the side panel (tab **Current**), click **Manage Assignment**.
5. In **Manage Salary Assignment**, set:
   - **Salary Structure** (required)
   - **Grade** (required)
   - **Band** (optional)
   - **Effective From** (required) and **Effective To** (optional; blank = open-ended)
6. Click **Save Assignment**.
7. The service enforces:
   - Structure/grade/band hierarchy consistency (the grade must belong to the selected structure, and the band must belong to the grade)
   - Overlap protection (one active assignment per employee for any given date range)
   - Closed payroll protection (rejects assignment changes when the effective period overlaps a payroll run that is already **PROCESSED/APPROVED**)

#### 3.2 Maintain employee salary components

1. Open **Salary Management** → **Employee Salaries** tab.
2. Assign components:
   - To add multiple components at once, click **+ Assign Salary to Employee**.
   - In **Assign Salary Components to Employee**, select **Employee** and **Salary Structure**, then set **Effective From** (and optional **Effective To**).
   - Select components using the checkboxes (the modal shows values copied from the component definition as “From definition”).
   - Click **Save All**.
3. Edit or end-date an existing component:
   - Use **Edit** on a component row to open the modal in single-component edit mode (change **Value type** and the corresponding **Amount** or **Percentage**, plus effective dates).
   - Use **End** on a component row to set **Effective To** for that component record.
4. System rules you must follow:
   - A **Basic earning** must exist for the employee in the effective period; the service blocks creating/updating component details if Basic is missing (ES-15).
   - For components whose calculation basis is `STATUTORY`, you cannot store a manual amount/percentage; you must use **Use master default** (the backend enforces this).
   - Overlapping effective periods for the same employee+component are rejected (one active per employee per component per effective date).

#### 3.3 Bulk salary revision (optional)

1. On **Employee Salaries**, use **Bulk salary revision** (when available) to submit organization-wide revision requests (for example, percentage change by grade).
2. Requests may require **approval** or **rejection** by an authorized user before they take effect, depending on your process.

#### 3.4 Handle salary revisions

1. Use effective-dated revisions:
   - End the current salary assignment or component record by setting **Effective To**.
   - Create a new effective-dated record with updated values and a new **Effective From**.
2. Audit changes:
   - Open the employee side panel and go to the **History** tab to view the unified **Revision history** (assignments + component detail changes).
3. Closed payroll protection:
   - The service rejects edits whose effective periods overlap a payroll run that has already been **PROCESSED/APPROVED**.

---

### 4. Running Payroll

#### 4.0 What is a Payroll Run? What Happens During It?

A **payroll run** is the process of computing and recording pay for all employees for a given pay period. It uses salary configuration (structures, components, assignments) to derive per-component amounts and net pay for that period.

**Payroll run status lifecycle:**
- **DRAFT** → **PROCESSED** → **APPROVED**

**What happens during a payroll run:**

1. **Create the run** – You create a payroll run with organization, pay period (start/end dates), payment date, and optional name/notes. It is created in **DRAFT** status.

2. **Populate from salary** – When you click **Populate from Salary**, the system:
   - Fetches all active employees whose employment overlaps the pay period (using hire date, termination date, and employment status).
   - Resolves each employee’s salary assignment (structure, grade, band) and component values effective as of the period end date.
   - Calculates each salary component amount using the master rules (Fixed, PercentageOfBasic, Formula, Statutory, Manual).
   - Processes components in **dependency order** (Basic first, then HRA as % of Basic, deductions, etc.).
   - Applies ceiling/floor limits per component where configured.
   - Applies **pay-period proration** (ES-28–ES-30) per component using hire/termination dates and each component’s proration rule (see component master).
   - Fills **statutory** lines that are modeled in the engine: **PF** (employee/employer from PF wage and EPF policy after earnings are known), **income tax** and **ESI** (from taxable/ESI wage bases built from computed lines, including overtime/LOP when present). Manual **Amount** overrides on statutory lines are also prorated like other components.
   - Applies **loan repayment** amounts where a loan recovery rule applies for the period.
   - Rolls up **time and attendance** for the pay period when there is at least one attendance record or an overlapping **submitted/approved/processed** timesheet (see §4.2b): adjusts gross for **overtime pay** and deductions for **LOP (loss of pay)** when applicable.
   - Writes **PayrollDetail** and **PayrollComponent** rows (per-employee gross, deductions, net, and per-component amounts), including attendance/LOP/OT fields on the detail when roll-up ran.
   - Flags employees without a salary assignment as skipped; employees missing **Basic** are still populated with `basicSalary = 0` and returned in `employeesMissingBasic`.

3. **Process** – You move the run from DRAFT to **PROCESSED**. The system aggregates totals (gross, deductions, net) at run level and records who processed it and when.

4. **Approve** – Optionally, you move the run to **APPROVED** for final sign-off before posting to accounting or disbursing payments.

5. **After finalization** – Payslips are available for employees; payroll can be posted to accounting; individual payslip rows can be marked as paid when disbursements are made.

**Important:** While the run is in **DRAFT**, you can re-populate from salary (this overwrites existing details). Once **PROCESSED**, salary recalculation is no longer allowed for that run. Correct salary data in Salary Structure Manager and re-populate while still in DRAFT if needed.

---

#### 4.1 Create a Payroll Run (Step-by-Step)

1. Go to **HR** → **Payroll Runs** (or the Payroll section in your HR menu).
2. Click **+ New Payroll Run**.
3. In the modal, enter:
   - **Run Name** (required or auto-generated as `Payroll {Period Start} - {Period End}`)
     - Example: `November 2025 Payroll`
   - **Period Start** (required) – First day of the pay period (e.g. `2025-11-01`).
   - **Period End** (required) – Last day of the pay period (e.g. `2025-11-30`).
   - **Payment Date** (required) – Date when salaries will be disbursed (e.g. `2025-12-05`).
   - **Notes** (optional) – Internal notes for the run.
4. Click **Create Run**.
5. The payroll run is created in **DRAFT** status with employee count and totals initially zero.

**Validation:**
- Period End must not be earlier than Period Start.
- All three dates (Period Start, Period End, Payment Date) are required.

---

#### 4.2 Populate Payroll from Salary (Calculate Employee Salaries)

**Prerequisites:**
- Employees must have **employment status = ACTIVE**.
- Employees must have **hire date on or before** the period end.
- Employees must have **termination date** null or after the period start (if terminated).
- Each employee needs a **salary assignment** (structure + grade + band) effective on the period end date.
- Basic earning is required to produce a non-zero `basicSalary`; employees without Basic are still populated and appear in `employeesMissingBasic` with `basicSalary = 0`.

**Steps:**

1. In the Payroll Runs table, locate the **DRAFT** run you created.
2. Click **Populate from Salary**.
3. Confirm when prompted (this will **overwrite** any existing payroll details for this run).
4. The system will:
   - Fetch all active employees in the organization whose employment overlaps the pay period.
   - For each employee, resolve the salary assignment and component details effective as of **Period End**.
   - Calculate each salary component in dependency order:
     - **Fixed** – uses master default amount or employee override.
     - **Percentage of Basic** – base component × percentage (base component code comes from the component master; typically `BASIC`).
     - **Percentage of Gross** – Gross so far × percentage.
     - **Formula** – evaluates expression using component codes (e.g. `BASIC`, `HRA`).
     - **Statutory** – evaluated as `0` during payroll population.
     - **Manual** – uses default amount from master.
   - Apply ceiling/floor limits per component where configured.
   - Compute **Gross** = sum of earnings (including **overtime pay** when attendance/timesheet data supports it), **Deductions** = sum of deductions (including **LOP** when applicable).
   - Compute **Net Salary** = Gross − Deductions.
   - Write one **PayrollDetail** per employee and multiple **PayrollComponent** rows per detail.
5. Review the alert message:
   - **X employee(s) populated successfully** – Normal case.
   - **Y skipped (no salary assignment)** – Go to Salary Structure Manager → Employee Salaries → assign structure and components.
   - **Z added without Basic component** – Employees included but `basicSalary = 0`; fix Basic component setup for those employees.
   - **Notices** (when shown) – May include **PF wage** reminders (e.g. PF statutory lines exist but no earnings are tagged **PF_WAGE**, so PF wage is zero) and an **ES-28** note that PF wage follows **computed component amounts** (including proration per component), not a separate PF proration model.
   - **No employees were added** – Check assignments, employment status, and hire dates.

**If employees were skipped:**
- Go to **Salary Structure Manager** → **Employee Salaries** tab.
- Assign salary structure, grade, band, and components (including Basic) for each employee.
- Run **Populate from Salary** again on the same DRAFT run.

**If employees were added without Basic:**
- In **Employee Salaries**, ensure each affected employee has a **Basic** earning component (category `BASIC` and component type `EARNING`) with either:
  - `Value type = AMOUNT` and amount > 0, or
  - `Value type = USE_MASTER_DEFAULT`
- Then click **Populate from Salary** again on the same DRAFT run.

---

#### 4.2b Time & attendance, LOP, and overtime (during Populate from Salary)

When **Populate from Salary** runs, the system may combine **salary calculation** with a **roll-up of time data** for the same calendar window as the pay period (clipped to each employee’s hire and termination dates).

**Data sources**

- **Daily attendance** (`Attendance` in HR): statuses such as present, paid leave, and unpaid absence drive **present days**, **paid leave days**, and **LOP days**. **Overtime hours** recorded on attendance lines are summed.
- **Timesheets**: weeks that overlap the pay period contribute **overtime hours** when the timesheet is in **Submitted**, **Approved**, or **Processed** status.

**Rules**

- **Working days** in the roll-up are **weekdays (Monday–Friday)** in the period, not calendar days.
- **Overtime hours for pay:** if the sum of overtime from **attendance** is greater than zero, that sum is used. Otherwise, overtime from **timesheets** is used.
- If there is **no** attendance and **no** qualifying timesheet in the period, **no** time roll-up is applied for that employee (payroll is based on salary components only, as before).

**Amounts (simplified)**

- **Overtime pay** uses the employee’s **Basic** amount for the run and assumes **8 hours per working day**: an hourly rate is derived as Basic ÷ (working days × 8), then overtime pay = hourly rate × **1.5** × overtime hours (rounded like other payroll amounts).
- **LOP deduction** is **pro-rated from Basic**: LOP amount ≈ Basic × (LOP days ÷ working days in the period).

**Optional salary components for line items**

- If your organization defines optional components with codes **`OT_PAY`** (type **EARNING**) and **`LOP_DED`** (type **DEDUCTION**), those amounts also appear as separate **PayrollComponent** lines for employees who have overtime pay or LOP, which improves payslip and accounting breakdown. If these components are not defined, gross, deductions, and net still reflect OT and LOP; only the separate lines are omitted.

**Master data for attendance statuses**

- Paid leave, present, and unpaid absence are recognized from **attendance status** values (for example: `present`, `paid_leave`, `absent`, `leave_without_pay`). Align your HR processes and any integrations with the status values you use so roll-up matches expectations.

---

#### 4.2a How Employee Salary is Calculated (Technical Detail)

During **Populate from Salary**, the system calculates each employee's pay as follows:

1. **Employee eligibility**
   - `employmentStatus = ACTIVE`
   - `hireDate <= periodEnd`
   - `terminationDate` is null or `>= periodStart`

2. **Salary data resolution (as of period end)**
   - One active `EmployeeSalaryAssignment` per employee (structure, grade, band).
   - One or more `EmployeeSalaryDetail` rows per component (amount, percentage, or use master default).

3. **Component calculation order**
   - Components are processed in **dependency order** derived from component master references (base component codes and formula references).
   - For each component, the amount is computed using:
     - **Employee override (Amount)**: use `detail.amount` if value type is AMOUNT.
     - **Employee override (Percentage)**: base component code from component master × `detail.percentage` / 100 if value type is PERCENTAGE.
     - **Master rule**: otherwise use `SalaryComponent` calculation basis:
       - **FIXED / MANUAL**: `defaultAmount` from component master.
       - **PERCENTAGE_OF_BASIC**: base component × `percentageValue` / 100.
       - **PERCENTAGE_OF_GROSS**: gross so far × `percentageValue` / 100.
       - **FORMULA**: evaluate expression (e.g. `BASIC + HRA`) using already-computed component codes.
       - **STATUTORY**: handled outside this step — PF lines use the EPF engine after earnings are known; other statutory lines (e.g. income tax, ESI) use slab/percentage rules after earnings and OT/LOP are merged, unless the employee has an explicit **Amount** override (then proration applies like other fixed amounts).

4. **Ceiling and floor**
   - If component has `ceilingAmount` and computed amount > ceiling → use ceiling.
   - If component has `floorAmount` and computed amount < floor → use floor.

5. **Proration (ES-28–ES-30)**
   - **Days employed** in the pay period are derived from hire date and termination date (or the full period if employed throughout).
   - **Calendar days in period** = inclusive days from period start to period end.
   - The **proration rule** on the salary component (default **BY_DAYS**) scales the amount after ceiling/floor: e.g. BY_DAYS uses amount × (days employed ÷ calendar days in period). **NO_PRORATION** keeps the full amount. **BY_HOURS** uses hour ratios when provided; otherwise it falls back to the day ratio.

6. **Loan recovery (if applicable)**
   - Loan repayment amounts from the loan module may be added as deductions when a **LOAN_REPAYMENT** salary component exists for the organization.

7. **Time & attendance roll-up (if data exists)**  
   See §4.2b. Adds overtime pay to earnings and LOP to deductions; stores `workingDays`, `presentDays`, `leaveDays`, `overtimeHours`, `overtimeAmount`, `lopDays`, `lopAmount` on the payroll detail when roll-up applies.

8. **Statutory PF / tax / ESI (when configured)**  
   After regular lines (and OT/LOP merged into the calculation context where applicable), **PF** contributions use PF wage (sum of PF_WAGE-tagged earnings, with policy ceiling), **income tax** uses taxable gross and tax slabs, **ESI** uses ESI-tagged earnings — see `EpfPayrollCalculationService` and `StatutoryPayrollCalculationService` in `hr-service`.

9. **Aggregation**
   - **Total earnings** = sum of all EARNING component amounts, **plus** overtime pay from roll-up.
   - **Total deductions** = sum of all DEDUCTION component amounts, **plus** loan recovery and LOP from roll-up.
   - **Net salary** = Total earnings − Total deductions.

10. **Stored result**
   - One `PayrollDetail` per employee: `basicSalary`, `grossSalary`, `totalDeductions`, `totalReimbursements`, `netSalary`, optional attendance/OT/LOP fields (`workingDays`, `presentDays`, `leaveDays`, `overtimeHours`, `overtimeAmount`, `lopDays`, `lopAmount`), `status=pending`.
   - Multiple `PayrollComponent` rows: one per salary component with `amount`, `componentType`, `displayOrder`, and optionally extra lines for **`OT_PAY`** / **`LOP_DED`** when configured and amounts are non-zero.

---

#### 4.3 Process the Payroll Run

1. After populating and reviewing the run, click **Process**.
2. Confirm when prompted.
3. The system will:
   - Sum all payroll details: `Total Gross`, `Total Deductions`, `Total Net`.
   - Set run-level `Employee Count`, `Total Gross Pay`, `Total Deductions`, `Total Net Pay`.
   - Change status from **DRAFT** to **PROCESSED**.
   - Record **Processed By** (user/employee ID) and **Processed At** (timestamp).
4. The run can no longer be re-populated; salary changes will not affect this run.

---

#### 4.4 Approve the Payroll Run (Optional)

1. For a **PROCESSED** run, click **Approve**.
2. Confirm when prompted.
3. The system will:
   - Change status from **PROCESSED** to **APPROVED**.
   - Record **Approved By** and **Approved At**.
4. The run is ready for posting to accounting and disbursement.

---

#### 4.5 Process EPF Contributions (Optional)

This step creates/updates EPF contribution records from the payroll results.

1. For a **PROCESSED** or **APPROVED** payroll run, click **Process EPF**.
2. Confirm when prompted.
3. The system will:
   - Extract **PF employee** and **PF employer** amounts from the payroll component results using component codes like `PF_EMPLOYEE` / `PF_EMPLOYER` (and aliases) and/or the component master `statutoryType`.
   - Create or update EPF contribution records for employees (skipping employees that have no EPF account).
4. To post the contributions to accounting, click **Post EPF**:
   - Run **Process EPF** first
   - Ensure Chart of Accounts includes `EPF_PAYABLE` and `CASH`

#### 4.6 View Payroll Details (Per-Employee Summary)

1. For a **PROCESSED** or **APPROVED** run, click **View Details**.
2. A modal shows a table of all employees in the run:
   - **Employee** (name or employee number)
   - **WD** – working days in the period (weekdays), when time roll-up was applied
   - **Pres** – present days from attendance, when roll-up was applied
   - **Leave** – paid leave days, when roll-up was applied
   - **LOP d** / **LOP $** – loss-of-pay days and LOP amount, when applicable
   - **OT h** / **OT $** – overtime hours and overtime pay, when applicable
   - **Gross**, **Deductions**, **Net**
   - **Status** (pending / paid; if paid, shows paid date and payment reference)
   - **Actions** (Mark Paid for pending rows)
3. Columns for attendance and OT/LOP show **—** when no roll-up ran for that employee for that period.
4. Use this view to verify amounts before posting to accounting or disbursing.

---

#### 4.7 Post Payroll to Accounting

**Prerequisites:**
- Run status must be **PROCESSED** or **APPROVED**.
- Chart of Accounts must have standard codes (typically): `6110` (Salary expense), `2020` (Payables), `1030` (Bank/Cash).

**Steps:**

1. For a **PROCESSED** or **APPROVED** run, click **Post to Accounting**.
2. Confirm when prompted (this creates a journal entry: Salary expense, deductions, bank).
3. The system will:
   - Call the accounting integration service with payroll totals.
   - Create and post a **summary** journal entry: gross salary expense, deductions liability, net pay to bank (e.g. debit `6110`, credit `2020` and `1030`).

**Accounting export (INT-19 / INT-21) — detail vs summary**

- **`GET /api/hr/payroll/runs/{runId}/accounting-export`** returns:
  - Run totals and period dates.
  - **`detailLines`** — per employee and salary component (for reconciliation and audit).
  - **`summaryByComponent`** — amounts aggregated by component code/type/category.
- **INT-20:** Each line can include **`expenseAccountCode`** and **`liabilityAccountCode`** when you configured them on the **salary component** master (see §2.4). Finance can use these to post **detail** journals in the accounting system; the UI **Post to Accounting** action does not expand per-component GL automatically.
- **INT-21:** Your finance process may post **summary** (one expense, one deductions pool, one bank — as the UI does) or **detail** lines from the export; both views refer to the same underlying payroll components for reconciliation with payslips.

4. If the Chart of Accounts is misconfigured, you may see an error; ensure the required account codes exist for the **summary** post path.

---

#### 4.8 Mark Payslips as Paid

When you have disbursed salary to an employee (e.g. via bank transfer or cheque):

1. Open **View Details** for the run.
2. Locate the employee row with status **pending**.
3. Enter a **Payment reference** (e.g. bank reference, cheque number, transaction ID).
4. Click **Mark Paid**.
5. The system will:
   - Set the payroll detail status to **paid**.
   - Store the payment reference and **Paid At** timestamp.
6. The row will now show status **paid** with the date and reference.

---

#### 4.9 Payslips (View and Download)

**HR/Payroll users (UI):**

1. Open **Salary Management** → **Employee Salaries** tab.
2. Click the employee name to open **Employee Salary – {name}** side panel.
3. Go to the **Payslip** tab.
4. Enter the **Payroll Run ID** and click **Load payslip**.
5. Click **Print / Save as PDF** (browser print).

**API (also available):**

1. **View a single payslip** – Use the API `GET /api/hr/payroll/runs/{runId}/payslip?employeeId={employeeId}` to fetch payslip data for an employee in a run.
2. The payslip includes:
   - Employee name, number
   - Basic salary, gross salary, total deductions, net salary
   - **Taxable gross (period)** — recomputed from component taxability/tags for the period (reporting).
   - **Year to date** (calendar year of the pay period end): optional YTD gross, deductions, net, and income tax withheld when the API returns them.
   - **Time and attendance** (when populated for that run): working days, present days, paid leave, LOP days and amount, overtime hours and overtime pay
   - Currency and pay frequency (from salary structure)
   - Line items: each salary component with code, name, type (EARNING/DEDUCTION), amount, and where configured on the master: **taxability**, **statutory type**, **PF wage** / **ESI wage** flags (for statutory reporting and clarity)
3. **Print/PDF** – Use the browser print function (Ctrl+P) on the payslip view.

**Employee self-service:**

1. Employees can view their **current salary summary** via `GET /api/hr/salary/self/summary` (with optional amount masking).
2. Employees can list their **payslips** via `GET /api/hr/payroll/self/payslips?employeeId=...&organizationId=...` to see runs they are part of.
3. Employees can retrieve a specific payslip via `GET /api/hr/payroll/runs/{runId}/payslip?employeeId={employeeId}` (subject to authorization).

**Payslip structure (per employee per run):**
- **Header**: Employee name, employee number, pay period (from run), currency, pay frequency.
- **Summary**: Basic salary, gross salary, total deductions, net salary.
- **Time and attendance** (optional): Shown when the payroll detail includes roll-up data for that period.
- **Line items**: Each salary component in display order:
  - Component code (e.g. `BASIC`, `HRA`, `PF_EMPLOYEE`)
  - Component name (short name or full name)
  - Type (EARNING / DEDUCTION)
  - Amount
  - Optional columns in the UI: **Taxability**, **Statutory**, **PF wage**, **ESI wage** (from the salary component master)

---

#### 4.10 Summary: End-to-End Payroll Workflow

| Step | Action           | Status Before | Status After   | UI Control                 |
|------|------------------|---------------|----------------|----------------------------|
| 1    | Create run       | —             | DRAFT          | + New Payroll Run          |
| 2    | Populate         | DRAFT         | DRAFT          | Populate from Salary       |
| 3    | Process          | DRAFT         | PROCESSED      | Process                    |
| 4    | Approve          | PROCESSED     | APPROVED       | Approve                    |
| 5    | View details     | PROCESSED/APPROVED | —         | View Details               |
| 6    | Process EPF (optional) | PROCESSED/APPROVED | — | Process EPF |
| 7    | Post EPF to accounting (optional) | PROCESSED/APPROVED | — | Post EPF |
| 8    | Post to accounting | PROCESSED/APPROVED | —       | Post to Accounting         |
| 9    | Mark paid (per row) | PROCESSED/APPROVED | —     | Mark Paid (in details modal)|

---

### 5. Reports and Exports

- **Structure summary**: Grades and bands with ranges; export to Excel/PDF.
- **Component master report**: All salary components with types, categories, calculation basis, taxability, and statutory flags.
- **Employee salary report**: Employee-wise structure, grade, and component values as of a date.
- **Revision history report**: Employee salary changes, reasons, and timestamps.
- **Grade-wise headcount and cost**: Headcount and total cost by structure/grade/band.
- **Component-wise cost**: Total cost per component across employees for a period.

All of these are powered by `SalaryReportExportService` and related endpoints in `SalaryController`.

Use this manual together with API documentation (OpenAPI/Swagger for `hr-service`) when integrating with other systems or automating HR/payroll workflows.

