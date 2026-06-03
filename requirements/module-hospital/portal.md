## Portal Module – Patient, Doctor, and B2B

### 1. Overview

**Description**  
The Portal Module provides secure web/mobile access for external and semi-external users (patients, doctors, and B2B/corporate clients) to interact with the hospital system. It surfaces selected EHR, billing, scheduling, and reporting data under strict access control.

**Objectives**

- Improve patient engagement and self-service capabilities.
- Give doctors convenient access to their schedules and clinical tasks.
- Provide B2B/corporate partners with transparency into utilization and financials.
- Reduce front-desk and back-office workload by shifting repeat tasks to self-service.

**Common Requirements**

- Secure authentication and authorization with role-based access.
- Responsive design for desktop, tablet, and mobile.
- Local language support where applicable.
- Configurable feature set per organization policy (what is exposed, to whom, and when).

---

### 2. Patient Portal

#### 2.1 Access & Authentication

- **Application Boundary**
  - Patient Portal frontend must be delivered as a separate application from the main HMS staff-facing frontend.
  - It may share backend services/APIs via secured integration, but deployment, routing, and user experience are patient-facing and independent.

- Registration:
  - Self-registration with verification (OTP/email) or
  - Activation via hospital registration desk (linking existing MRN/patient ID).
- Login:
  - Username/password, with optional 2FA/OTP as per configuration.
  - Patient login identifier must support Patient ID as the primary username.
  - Authentication must allow either:
    - Patient ID + Password, or
    - Patient ID + Phone Number (for passwordless/initial-access flow as configured).
  - For initial users who do not yet have a password:
    - Allow login using Patient ID + registered phone number with OTP verification.
    - Prompt user to set a new password after successful first login (policy-driven: mandatory or optional).
- Account linking:
  - Ability to link multiple patient profiles under a guardian/parent account where allowed.

#### 2.2 Core Features

- **Profile Management**
  - View and update contact information (phone, email, address).
  - View demographic details pulled from EHR (e.g., date of birth, gender).

- **Appointment Management**
  - Search available slots by doctor, department, date, or service.
  - Book, reschedule, and cancel appointments per hospital rules.
  - View appointment history and status (confirmed, pending, cancelled).

- **Clinical Documents (Read-Only)**
  - View visit history, prescriptions, and investigation reports (lab and imaging) as allowed.
  - View discharge summaries and any patient-facing instructions.
  - Download or print documents that are permitted by policy.

- **Billing & Payments**
  - View outstanding bills and payment history.
  - Pay online for:
    - OPD consultations and packages.
    - Investigations.
    - IPD deposits and outstanding balances (if enabled).
  - Download receipts and invoices.

- **Notifications & Communication**
  - Receive notifications:
    - Appointment confirmations and reminders.
    - Report availability alerts.
    - Payment confirmations.
  - Basic messaging capability with hospital support team (optional as per scope).

#### 2.3 Patient Portal Rules

- Clinical and financial data visibility strictly governed by:
  - Hospital policy.
  - Regulatory requirements for the local jurisdiction.
- Certain sensitive information may be:
  - Hidden entirely.
  - Delayed (e.g., some results).
  - Visible only after clinician review.

#### 2.4 Patient Portal Navigation & Landing UI Requirements

- **Header / Identity Area**
  - Show portal title as `Patient Portal`.
  - Show organization context and patient identifier (e.g., hospital/branch name and UHID).
  - Display current portal version in header (e.g., `Ver 1.5`).
  - Display authenticated user greeting with profile image/avatar (e.g., `Welcome, Mr. <Patient Name>`).

- **Primary Navigation Menu**
  - Navigation must include, at minimum:
    - Home
    - Electronic Medical Record (EMR)
    - Diagnostics History
    - Doctor Appointment Request
    - Write Us
    - Feedback
    - Change Password
  - Menu labels must be clear and consistently spaced/formatted (avoid merged words such as `Writeus` or `DoctorAppointment`).
  - Mobile view should support a collapsible menu (`Toggle navigation`) with the same items.

- **Landing Page Behavior**
  - `Home` is the default landing route after successful login.
  - Quick-access tiles/links may repeat primary menu entries for fast navigation.
  - All menu routes must be role-validated and only show features enabled for the patient account.

---

### 3. Doctor Portal

#### 3.1 Access & Authentication

- Access based on doctor user accounts configured in User Management.
- Strong authentication with optional 2FA.
- Multiple practice locations supported (if doctor works across branches).

#### 3.2 Core Features

- **Schedule & Appointment View**
  - Today’s and upcoming appointments.
  - Filters by clinic, location, or session.
  - Real-time updates of patient check-in and queue status.

- **Patient List & Clinical Access**
  - View list of patients scheduled or admitted under the doctor.
  - Quick access links into EHR for each patient (notes, history, investigations).

- **Easy Prescription (Doctor Portal – HMS)**
  - Launch **Easy Prescription** workflow for OPD/IPD patients under the doctor, using the single-screen layout defined in `Prescription Management – Easy Prescription Module (3.2.0)`.
  - Show **today’s patient list**, recent prescriptions, and favorite templates in a doctor-friendly dashboard view.
  - Allow fast medicine ordering, investigations, advice, and follow-up planning with minimal typing, while still enforcing all prescription safety and audit rules from the EHR.

- **Task & Result Management**
  - View pending lab/radiology results requiring review.
  - View pending orders, procedures, and discharge approvals.

- **Productivity & Revenue Insight (Summary)**
  - View personal activity statistics:
    - OPD visit counts.
    - IPD admissions and procedures.
    - High-level revenue contribution summary (no detailed financial ledger access unless allowed).

#### 3.3 Doctor Portal Rules

- Access to patient data limited to:
  - Patients under the doctor’s care (current or recent).
  - Where hospital policy explicitly allows broader access (e.g., departmental coverage).
- Doctor-facing financial views are:
  - Aggregated and limited to high-level metrics unless granted extended permissions.

---

### 4. B2B / Corporate Portal

#### 4.1 Access & Authentication

- Corporate account-level login:
  - One or more users per corporate/insurance/B2B client.
  - Role-based access within each corporate (e.g., Administrator, Finance, Medical Auditor).

#### 4.2 Core Features

- **Contract & Tariff Visibility**
  - View high-level contract details:
    - Covered services and packages.
    - Co-pay rules and exclusions.
    - Validity period, credit limits, and payment terms.

- **Utilization View**
  - List of active and recent patients linked to the corporate.
  - Visit and admission details (non-clinical or limited clinical as allowed).
  - Volume and cost summaries by date range.

- **Financial Statements**
  - View and download:
    - Invoices.
    - Statements of account.
    - Aging and outstanding details.
  - Export to Excel/PDF.

- **Authorization & Pre-Approval (Optional)**
  - Submit or track pre-authorization requests (for admissions, procedures).
  - Upload/download supporting documents.
  - Update approval status and remarks.

#### 4.3 B2B Portal Rules

- Data isolation:
  - Each corporate account only sees its own data.
- Level of clinical detail exposed is controlled by:
  - Legal/regulatory rules.
  - Contractual agreements.

---

### 5. Technical & Security Considerations

- **Integration**
  - Portals must use secure APIs to interact with:
    - EHR.
    - Billing and Payments.
    - Scheduling.
    - Reporting.

- **Security**
  - HTTPS-only communication.
  - Protection against common web vulnerabilities (OWASP).
  - Rate limiting and monitoring for suspicious activity.

- **Audit & Logging**
  - Log all portal logins, logouts, failed attempts.
  - Audit all data access and key actions (e.g., booking, cancelling, paying).

- **Performance & Availability**
  - Designed for high availability and appropriate response times for end-users.
  - Graceful degradation if backend components are temporarily unavailable (e.g., informative messages, queued operations where appropriate).

