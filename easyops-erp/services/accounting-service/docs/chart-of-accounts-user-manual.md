# Chart of Accounts — User Manual

This guide explains how to **use** the Chart of Accounts in EasyOps for day-to-day accounting setup. It is written for **finance users** and **organization administrators**.

---

## 1. What is the Chart of Accounts?

The **Chart of Accounts (CoA)** is the master list of every **general ledger account** your organization uses to classify money: what you own, what you owe, income, and expenses. Every journal entry and most financial reports depend on these accounts.

In EasyOps, the chart is **per organization**. Switching organizations (if your role allows) shows a different chart.

---

## 2. Before you start

1. **Select an organization** in the application. The Chart of Accounts screen requires a current organization.
2. **Permissions:**  
   - **View** financial setup: you need accounting **view** (or **manage**).  
   - **Add**, **edit**, **deactivate**, or **load standard template**: you need accounting **manage**.

If you cannot see the menu or actions, contact your administrator to grant the appropriate **accounting** permissions for your organization.

---

## 3. Opening the Chart of Accounts

Navigate to the **Accounting** area and open **Chart of Accounts** (wording may match your menu labels).

You will see:

- **List view** — table of accounts with code, name, type, balances, status.
- **Tree view** — optional layout grouped for hierarchy (folders vs leaf accounts).

Use the toggle on the screen to switch between list and tree when available.

---

## 4. Understanding account types

Accounts are classified into five types (shown as chips or labels):

| Type | Typical use |
|------|-------------|
| **ASSET** | Cash, bank, receivables, inventory, equipment |
| **LIABILITY** | Payables, loans, accrued expenses, taxes owed |
| **EQUITY** | Owner capital, retained earnings |
| **REVENUE** | Sales, services, other income |
| **EXPENSE** | COGS, salaries, rent, utilities, etc. |

Choosing the correct type ensures **balance sheet** and **profit and loss** reports classify amounts correctly.

---

## 5. Group accounts vs posting accounts

- **Group account** (sometimes shown with a folder icon): a **header** used to organize children accounts. You normally **do not post** transactions directly to pure group accounts.
- **Posting account**: where you record **actual** transactions.

When creating a new account under a **parent**, the parent must be a **group** account.

---

## 6. System accounts

Some accounts are marked as **system** accounts (for example, those created from the standard template). They support core processes and **cannot be deactivated** from the UI. You may still **edit** certain fields (such as name or description) where the application allows.

---

## 7. Loading the standard template

If your organization is new and has **no** accounts yet, you can use **Load Standard COA** (or similarly labeled button):

1. Confirm the prompt.
2. The system creates a **standard** set of common accounts (cash, receivables, payables, revenue, expenses, etc.).

**Important:**

- The server only loads the full template when the organization has **no** existing accounts. If you already have accounts, this action may do nothing or return a message indicating accounts already exist.
- The first time you open the chart with **no** accounts, the system may also **automatically** create the standard chart in the background.

---

## 8. Adding a new account

1. Click **Add Account** (or equivalent).
2. Fill in at least:
   - **Account code** — unique within your organization (e.g. `6210`). Integrations may expect specific codes (e.g. payroll-related codes); coordinate with IT/finance leads.
   - **Account name** — clear label for reports.
   - **Account type** — ASSET, LIABILITY, EQUITY, REVENUE, or EXPENSE.
   - **Group** — whether this is a header (group) or a posting account.
3. Optional: category, level, currency, opening balance, description, tags.
4. Save.

If the code is already used, you will see an error and must pick another code.

---

## 9. Editing an account

Open the account from the list and use **Edit**.

You can update names, categories, description, and similar fields. **Account codes** should be changed only with care: other modules or integrations may reference a fixed code. If you need a new code, it is often safer to **add** a new account and migrate usage over time.

---

## 10. Deactivating an account

Use **Deactivate** (or trash icon) on a **non-system** account to mark it **inactive**. Inactive accounts are typically hidden from selection in new transactions.

- This is a **soft** deactivation, not a hard delete.
- **System** accounts cannot be deactivated.

---

## 11. Balances

**Opening balance** and **current balance** may appear on the list. Current balances update as you post journals. Group rows may show **no** balance in the table (balances apply to posting accounts).

---

## 12. Integration with other modules

Other parts of EasyOps (payroll, deposits, inventory, etc.) may post to accounts identified by **account code**. If those codes are missing, posting can fail with “account not found.” Your administrator should ensure required codes exist—often via **standard template**, **database seed**, or **manual** account creation.

---

## 13. Troubleshooting

| Issue | What to try |
|--------|-------------|
| “No organization selected” | Select an organization in the app header or profile context. |
| Empty list | Wait for load; use **Load Standard COA** if you have manage permission and no accounts exist yet. |
| Cannot deactivate | Account may be a **system** account. |
| Duplicate account code | Use a different code for the same organization. |
| Integration posting fails | Confirm the required **account codes** exist and are **active** posting accounts. |

---

## 14. Glossary

| Term | Meaning |
|------|--------|
| **GL / General ledger** | The full set of accounts and posted transactions. |
| **Posting** | Recording a debit or credit to an account. |
| **CoA** | Chart of Accounts. |
| **Organization** | Tenant; each has its own chart. |

---

## Document history

- Introduced as end-user manual for Chart of Accounts in the accounting module.
