# Doctor Module

## Department Management

**Description**:  
The Doctor Module is a core component of the AURORA Hospital Management System. It manages doctor-related master data, departmental structure, availability, attendance, clinical instructions, and patient appointments to ensure smooth clinical operations in both OPD and IPD. This subsection defines the foundational capability: **Doctor Department Management**.

**Purpose**:  
Organize doctors based on medical specialties and administrative departments to support scheduling, reporting, access control, and downstream clinical workflows.

**Features**:
- Department master management with the ability to:
  - Create new departments
  - Update existing departments
  - Delete departments (subject to business rules)
  - Set department status to **Active** or **Inactive**

**Data Fields (Department Master)**:
- **Department ID**
  - **Type**: Auto-generated
  - **Required**: Yes
  - **Description**: Unique identifier for each department
  - **Validation / Behavior**:
    - System-generated (no manual edit in UI)
    - Must be globally unique
- **Department Name**
  - **Type**: Text
  - **Required**: Yes
  - **Description**: Name of the department (e.g., Cardiology, Orthopedics)
  - **Validation / Rules**:
    - Required, non-empty
    - Must be unique (case-insensitive) across all departments
    - Should handle special characters, numbers, and spaces safely
- **General Visit Amount**
  - **Type**: Decimal
  - **Required**: No
  - **Description**: Default doctor visit fee for the department
  - **Validation / Rules**:
    - Numeric value, greater than or equal to 0
    - Currency formatting based on hospital configuration
    - May be overridden at the individual doctor level
- **Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the department is currently active
  - **Allowed Values**: `Active`, `Inactive`

**List View and UI Behavior**:
- **Department List View**:
  - Displays all departments in a tabular view with the following columns:
    - Department Name
    - Visit Amount (General Visit Amount)
    - Status
    - Actions
  - Supports:
    - **Filtering**:
      - By status (Active / Inactive)
      - By department name (search / filter)
    - **Actions** (per row):
      - Edit
      - Delete (subject to business rules)
      - Activate / Deactivate (toggle status)
    - **Export**:
      - Export department list with current filters applied (e.g., CSV/Excel)
    - **Pagination**:
      - Standard pagination controls (page size, next/previous)
- **Department Form Fields**:
  - Department Name
  - General Visit Amount
  - Status

**Business Rules**:
- **Uniqueness**:
  - Department Name must be unique across the system.
- **Deletion Constraints**:
  - System shall prevent deletion of a department if one or more doctors are currently assigned to it.
  - When delete is blocked, the UI should clearly indicate that doctors must be reassigned before deletion, and optionally display or link to the list of assigned doctors.
- **Status and Availability**:
  - Changing a department status to **Inactive**:
    - Does not delete existing doctors or historical data.
    - Prevents registration of new doctors under that department.
    - Should mark related schedules and availability as non-selectable for new appointments (OPD/IPD) unless overridden by configuration.
  - Reactivating an **Inactive** department restores it for new doctor registrations and scheduling.

**Edge Cases & Exceptions**:
- **Empty Department List**:
  - When no departments exist, the list view should:
    - Display a clear, user-friendly message (e.g., "No departments found.")
    - Provide a primary action to create the first department.
- **Duplicate Name Attempt**:
  - When a user attempts to create or rename a department to a name that already exists:
    - System must block the operation.
    - Display a clear validation message (e.g., "Department name must be unique. A department with this name already exists.").
- **Delete with Active Doctors**:
  - Attempting to delete a department with assigned doctors should:
    - Be blocked by business rules.
    - Show a message indicating that the department has assigned doctors.
    - Optionally display or link to the list of assigned doctors and provide guidance for reassignment.
- **Inactive Departments**:
  - Existing doctors in an Inactive department remain linked for historical and reporting purposes.
  - New doctor registrations and new schedule assignments to that department are blocked.
- **Special Characters in Department Name**:
  - System should:
    - Properly handle department names with special characters, numbers, and spaces.
    - Apply appropriate sanitization and escaping to prevent security issues (e.g., XSS, injection).

**Error Handling and Validation**:
- **Field-Level Validation Errors**:
  - Display inline, field-specific messages for:
    - Missing required fields (e.g., Department Name, Status)
    - Invalid values (e.g., negative General Visit Amount)
    - Duplicate Department Name
- **System Errors**:
  - For unexpected failures (e.g., database or API errors), display user-friendly, non-technical error messages and log technical details for support.
- **Concurrent Updates**:
  - System should safely handle concurrent edits to the same department record using either:
    - A "last save wins" approach with clear update timestamps, or
    - An optimistic locking / version mechanism that detects conflicts and prompts the user to refresh and re-apply changes.

**Implementation Notes**:
- **Prerequisites**:
  - Department is a prerequisite for doctor registration and scheduling; no doctor record should exist without an associated department.
- **Visit Amount Override**:
  - General Visit Amount at department level acts as a default; it may be overridden on individual doctor records (e.g., specialist-specific fees).
- **Deletion Strategy**:
  - Implementation should strongly prefer **soft delete** for departments (e.g., `is_deleted` flag) to:
    - Preserve historical data and audit trails when deletion is permitted by business rules (for example, unused or test departments).
    - Maintain referential integrity for any residual links (e.g., logs, configuration) without exposing deleted departments in normal operational flows.

**Functional Requirements (Future Release Scope)**:
- The system shall allow authorized users to **create**, **view**, **update**, and (soft) **delete** department records.
- The system shall enforce **unique Department Name** validation at the time of create and update.
- The system shall prevent deleting a department that has **any active doctor assignments** in the doctor master, and clearly surface the reason in the UI.
- The system shall support **searching** departments by partial Department Name and **filtering** by Status (Active / Inactive).
- The system shall support **sorting** departments at least by Department Name and Status.
- The system shall support **exporting** the department list, honoring current search and filter criteria.
- The system shall expose department data to other Doctor Module features (doctor master, availability, attendance, OPD/IPD scheduling) through well-defined APIs or service interfaces.
- The system shall ensure that **no new doctor record** can be created or updated to reference a department that is Inactive or soft-deleted.
- The system shall ensure that **no new OPD/IPD schedule or appointment** can be created for a doctor whose only associated department is Inactive, unless explicitly overridden by configuration.
- The system shall maintain a complete **audit trail** (who, what, when, before/after values) for all create, update, delete, and status-change operations on departments.

**User Roles and Access Control**:
- Only users with appropriate administrative roles (e.g., **Hospital Admin**, **HR/Admin**, or a configurable "Department Admin" role) shall be allowed to create, update, delete, or change the status of departments.
- Regular clinical users (e.g., Doctors, Nurses) shall have **read-only** access to active department data where required (e.g., filters, dropdowns) and shall not be able to change department master data.
- Access to export functionality shall be controlled via permissions (e.g., only Admin roles can export the full department list).

**Non-Functional and UX Requirements**:
- Department list and form screens shall follow the **standard hospital UI design system**, including consistent typography, colors, and form behavior used in other hospital modules.
- The system should load the department list with typical filters applied (e.g., **Active** departments) within **2 seconds** under normal load.
- All error and validation messages shall be **human-readable**, localized (where localization is supported), and consistent with messaging patterns used elsewhere in the system.
- The system shall handle at least **several hundred departments** without noticeable degradation in list performance, using server-side pagination and filtering where necessary.
- The design should support **future extensions** (e.g., department category, location, cost center codes) without breaking existing integrations.

**Reporting and Analytics Considerations**:
- Department master data shall be available for use in **operational and clinical reports**, such as:
  - Doctor count by department
  - Visit volume and revenue by department (using General Visit Amount as a default where needed)
- Historical changes to departments (rename, status change) shall be traceable so that legacy reports can still be interpreted correctly over time.

---

#### 9.4.8 Doctor Module – Doctor Attendance (In & Out)

**Description**:  
This subsection defines the **Doctor Attendance (In & Out)** capability within the Doctor Module. It captures doctors’ daily presence in the hospital (or remote/on-call contexts), supports biometric and manual sources, and provides a reliable basis for punctuality analysis, payroll, and operational reporting.

**Purpose**:  
To accurately track doctor in-time and out-time in relation to their scheduled shifts, support integrations with external attendance systems, and generate actionable attendance and punctuality insights for operations and HR.

**Features**:
- Record daily attendance (in-time and out-time) per doctor.
- Support multiple data sources: biometric/turnstile devices, external attendance feeds, and manual entry/corrections.
- Track attendance status (Present / Absent / Late / Early Leave, etc.) derived from schedules and actual times.
- Detect and highlight anomalies (missing check-out, multiple check-ins, conflicts with approved leave).
- Generate attendance, punctuality, and working-hours reports for individual doctors, departments, and the hospital.
- Provide exportable attendance data for payroll and performance evaluation.

**Data Fields – Overview**  
Doctor attendance data is organized into logical groups:
- **Doctor Identification**
- **Attendance Details**
- **Derived & Status Fields**
- **Source & Integration Fields**
- **Audit & System Fields**

##### 9.4.8.1 Data Fields

**Doctor Identification**:
- **Doctor ID**
  - **Type**: Auto-filled (from Doctor Master)
  - **Required**: Yes
  - **Description**: System-generated unique identifier for the doctor.
  - **Validation / Rules**:
    - Auto-populated when Doctor Code is selected.
    - Not editable in the attendance form.
- **Doctor Code**
  - **Type**: Auto-filled (from Doctor Master)
  - **Required**: Yes
  - **Description**: Doctor reference code.
  - **Validation / Rules**:
    - Must reference an Active, non-deleted doctor.
    - Auto-populated via doctor selection control (search by code/name).
- **Doctor Name**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes
  - **Description**: Doctor’s full name.
  - **Validation / Rules**:
    - Auto-filled from Doctor Master.
    - Not directly editable in attendance screen.
- **Doctor Department**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes
  - **Description**: Doctor’s primary department.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master.

**Attendance Details**:
- **Attendance Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Date for which attendance is being recorded.
  - **Validation / Rules**:
    - Valid calendar date.
    - Must not be in the future (unless explicitly allowed for bulk planning/import by authorized roles).
- **In Time**
  - **Type**: Time
  - **Required**: Yes (for Present/Late/Early Leave statuses)
  - **Description**: Check-in time for the doctor on the Attendance Date.
  - **Validation / Rules**:
    - Valid time format (consistent 24-hour format, e.g., HH:MM).
    - For biometric records, captured from device; for manual entry, selected via time picker.
- **Out Time**
  - **Type**: Time
  - **Required**: No
  - **Description**: Check-out time for the doctor on the Attendance Date.
  - **Validation / Rules**:
    - Valid time format (24-hour).
    - When provided, must be **> In Time** on the same date.
    - May be left empty when doctor has not yet checked out; such records shall be flagged for follow-up.
- **Schedule / Shift Reference**
  - **Type**: Reference (Schedule Entry / Day + Shift)
  - **Required**: No
  - **Description**: Logical link to the doctor’s schedule entry (e.g., day and shift) used to derive punctuality and working-hours expectations.
  - **Validation / Rules**:
    - If provided, shall reference a valid schedule entry for the same doctor and date (see 9.4.6).
    - Used to compute “Late By” and “Left Early By” based on scheduled start/end times.

**Derived & Status Fields**:
- **Attendance Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Derived or assigned status summarizing attendance.
  - **Initial Values**: `Present`, `Absent`, `Late`, `Early Leave`
  - **Optional Extensions** (future): `On Call`, `Remote`, `On Duty Elsewhere`
  - **Rules**:
    - For integrated flows, calculated based on Doctor Scheduling (see 9.4.6) vs In/Out Time and configured grace periods.
    - May be manually overridden by authorized roles with justification.
- **Working Hours (Duration)**
  - **Type**: Calculated (time span)
  - **Required**: No
  - **Description**: Total time between In Time and Out Time.
  - **Validation / Rules**:
    - Calculated only when both In Time and Out Time are present and valid.
- **Punctuality Indicators** (derived, optional in UI):
  - **Late By** (minutes): difference between scheduled start time and In Time, considering grace period.
  - **Left Early By** (minutes): difference between scheduled end time and Out Time, if Out Time is earlier than schedule.

**Source & Integration Fields**:
- **Attendance Source**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Origin of the record.
  - **Allowed Values**: `Biometric`, `External System`, `Manual Entry`, `Manual Correction`, `Bulk Upload`
  - **Rules**:
    - Biometric/External records shall be marked read-only for core times for most users, with corrections controlled by role.
- **External Reference ID**
  - **Type**: Text
  - **Required**: No
  - **Description**: Identifier from external attendance/biometric system (e.g., punch ID, transaction id).
- **Is Correction**
  - **Type**: Boolean
  - **Required**: No
  - **Description**: Indicates that this record was created/modified as a correction to an earlier entry.
  - **Rules**:
    - When true, system should store original values in audit trail.
- **Correction Reason**
  - **Type**: Textarea
  - **Required**: When Attendance Source = `Manual Correction` or when editing after configurable cutoff (e.g., 24 hours).
  - **Description**: Justification for changing attendance data.

**Audit & System Fields**:
- **Created By / Created At**
  - Same semantics as in 9.4.7 (user and timestamp of creation).
- **Updated By / Updated At**
  - Same semantics as in 9.4.7 (last modifying user and timestamp).

##### 9.4.8.2 User Interface Requirements

**Form View – Attendance Entry / Review**:
- **Doctor & Date Section**:
  - Doctor selection (auto-suggest by Code/Name; auto-fills Name and Department).
  - Attendance Date (date picker).
- **Time & Status Section**:
  - In Time (time picker or read-only if from biometric).
  - Out Time (time picker; may be blank initially).
  - Attendance Status (dropdown; default value derived from schedule and times, editable by authorized roles).
  - Attendance Source (displayed; usually read-only).
  - Correction Reason (shown when required).
- **Actions**:
  - **Save** / **Update** attendance record.
  - **Mark Absent** (creates an Absent record without In/Out time).
  - **Reset** (revert unsaved changes).
  - **Back to List**.
- **Validation UX**:
  - Inline validations for date/time formats and Out Time <= In Time.
  - Clear messages for future date attempts and conflicts with leave.
  - Warning banners for:
    - Missing Out Time after day end.
    - Multiple attendance entries for same doctor and date.

**List View – Attendance Records**:
- **Columns (Minimum)**:
  - Doctor Code
  - Doctor Name
  - Department
  - Attendance Date
  - In Time
  - Out Time
  - Working Hours (calculated)
  - Attendance Status
  - Attendance Source
- **Filters**:
  - Doctor (Code/Name).
  - Department.
  - Date range.
  - Attendance Status (Present/Absent/Late/Early Leave, etc.).
  - Source (Biometric, Manual, Correction).
- **Row Actions**:
  - View / Edit attendance record.
  - View change history / audit details.
  - Mark as corrected (if permitted).
- **Export & Pagination**:
  - Export filtered attendance records (CSV/Excel) for payroll and analysis.
  - Standard pagination and sorting (by Date, Doctor, Status).

##### 9.4.8.3 Business Rules

- **Time Consistency**:
  - When Out Time is present, it must be **greater than** In Time for the same Attendance Date.
  - System shall prevent saving Out Time earlier than or equal to In Time and show a clear validation error.
- **Date Constraints**:
  - Attendance Date shall not be in the future for normal entry.
  - Configuration may allow authorized roles to pre-enter attendance (e.g., for shift planning) with appropriate flags.
- **Single-Record Principle per Day**:
  - For each doctor and Attendance Date, system shall enforce at most one **primary** attendance record.
  - Multiple check-ins/check-outs received from biometric systems may:
    - Be aggregated into a single summarized record, or
    - Be stored as detailed punches linked to the primary record (future enhancement).
  - If another primary record is attempted for the same doctor and date:
    - System shall warn or prevent, depending on configuration.
    - User may correct the existing record instead of creating a new one.
- **Status Calculation**:
  - Attendance Status may be auto-derived using:
    - Scheduled shift information from Doctor Scheduling (9.4.6).
    - Configurable grace periods for late arrival and early departure.
  - Example rules:
    - If In Time > (Scheduled Start + Grace), mark as `Late`.
    - If Out Time < (Scheduled End – Grace), mark as `Early Leave`.
    - If no In Time or Out Time recorded but doctor was scheduled, mark as `Absent` (after a configurable cut-off).
- **Interaction with Leave**:
  - If doctor has an `Approved` leave (9.4.7) on Attendance Date:
    - System shall display a conflict warning when attendance is recorded.
    - Authorized roles may override (e.g., doctor came in despite leave) with mandatory Correction Reason.
  - Such overrides shall be clearly marked for audit and reporting.
- **Manual Corrections**:
  - Manual creation or modification of attendance after a configurable time window (e.g., 24 hours after Attendance Date) shall:
    - Require higher-level approval or an admin role.
    - Require a Correction Reason.

##### 9.4.8.4 Edge Cases, Exceptions, and Error Handling

- **Forgotten Check-Out**:
  - If Out Time is missing after end of day:
    - Record remains with missing Out Time and is highlighted in reports/UI.
    - Authorized users can later add Out Time with Correction Reason (if after cut-off).
- **Future Date Attempts**:
  - Attempting to record attendance with Attendance Date in the future (beyond allowed configuration) shall:
    - Be blocked with a clear error message.
- **Multiple Check-Ins for Same Day**:
  - System shall detect multiple check-ins from biometric/external sources for the same doctor and date.
  - UI shall:
    - Show a warning indicating multiple punches.
    - Allow authorized users to correct/merge them into a single attendance record.
  - Detailed punch-level data, if stored, shall be retained for audit.
- **Biometric / External System Failures**:
  - If attendance cannot be imported due to external system failure:
    - System shall allow manual entry for affected periods.
    - System shall log integration failure details for support.
- **Duplicate Attendance Records**:
  - System shall detect potential duplicates (same doctor, same date, same source) and:
    - Prevent exact duplicates from being created.
    - Provide link to the existing record when user attempts duplicate entry.
- **Invalid Time Format**:
  - Time pickers shall enforce valid time formats.
  - Free-text time entries (if allowed) must be validated before save, with clear error messages.

**Special Scenarios**:
- **On-Call Doctors**:
  - Attendance may follow different rules (e.g., not requiring physical presence).
  - Initial implementation may record on-call presence as a distinct Attendance Source or Status; detailed logic can be extended later.
- **Remote Consultations**:
  - Attendance for remote/virtual consultations may be recorded separately with a specific Attendance Source (e.g., `Remote`) or status, and may not require on-premise In/Out times.
- **Emergency Situations**:
  - Backdated entries during emergencies may be allowed with mandatory justification and possibly higher approval.
- **System Maintenance / Bulk Entry**:
  - During system downtime or data migration, bulk attendance upload may be supported:
    - Requires elevated permissions.
    - Must maintain data integrity checks (no impossible time ranges, duplicate days).

##### 9.4.8.5 Reporting and Integration

- **Reporting**:
  - System shall provide data for:
    - Daily/Monthly attendance summaries per doctor and department.
    - Punctuality reports (late arrivals, early leaves, absences).
    - Working hours analysis and overtime indicators (if applicable).
  - Attendance reports shall support export in standard formats (CSV/Excel).
- **Payroll and Performance Integration**:
  - Attendance data shall be made available to payroll and HR performance systems via reports or APIs.
  - At minimum, fields such as Attendance Date, Working Hours, Attendance Status, Late By, and Left Early By shall be exposed for downstream payroll rules.
  - Any changes to attendance after payroll cut-off shall be clearly traceable via audit.
- **External Attendance System Integration**:
  - The system shall support ingestion of attendance data from external biometric/turnstile systems:
    - Configurable mappings for doctor identifiers.
    - Handling of missing or inconsistent external data via error logs and manual correction workflows.

---

#### 9.4.5 Doctor Module – Doctor Registration and Management

**Description**:  
This subsection defines the **Doctor Registration / Doctor Creation** capability and associated **Doctor List Management** within the Doctor Module. It captures complete personal, professional, scheduling, and appointment-related configuration for each doctor and provides administrative tools to manage the doctor master list.

**Purpose**:  
To register and maintain a high-quality master record for each doctor so that OPD/IPD scheduling, appointments, prescriptions, billing, and reporting can reliably use doctor data.

**Features**:
- Register new doctors with comprehensive information.
- Update and maintain existing doctor profiles.
- Manage doctor active/availability status.
- Configure appointment and serial-related settings per doctor.
- Configure visit and fee settings per doctor (overriding department defaults where needed).
- View, filter, sort, export, and manage the doctor list.
- Soft-delete doctor records where allowed by business rules (favoring soft delete over hard delete).

**Data Fields – Overview**  
Doctor data is organized into logical groups for UI and validation purposes:
- **Basic Identification**
- **Professional Details**
- **Personal Information**
- **Contact & Address Information**
- **Registration & Availability**
- **Appointment & Serial Configuration**
- **Appointment Source Limits**
- **Visit & Fee Configuration**
- **Communication Settings**

##### 9.4.5.1 Basic Identification

**Fields**:
- **Doctor ID / Doctor Code**
  - **Type**: Auto-generated
  - **Required**: Yes
  - **Description**: Unique reference number for the doctor.
  - **Generation Logic**:
    - Format: `[First Letter of Department][First Letter of Doctor Name][Numeric Serial]`
    - Example: Department = *Nephrology*, Doctor Name = *Abul Kalam* → `NA0001`.
  - **Validation / Rules**:
    - System-generated; not directly editable in normal workflows.
    - Must be globally unique across all doctors.
    - If code collision occurs, system shall automatically increment the numeric serial.
- **Doctor Name**
  - **Type**: Text
  - **Required**: Yes
  - **Description**: Full name of the doctor.
  - **Validation / Rules**:
    - Required, non-empty.
    - Maximum length 100 characters.
- **Department**
  - **Type**: Dropdown (lookup from Department master; see 9.4.4)
  - **Required**: Yes
  - **Description**: Primary department association for the doctor.
  - **Validation / Rules**:
    - Must reference an **Active** and non-deleted department.
    - Department must exist before doctor registration.
- **Status (Indoor / Outdoor)**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the doctor primarily practices as an indoor (IPD) or outdoor (OPD) doctor.
  - **Allowed Values**: `Indoor`, `Outdoor`.
- **Doctor Type**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Employment/engagement type.
  - **Allowed Values**: `Visiting`, `Permanent`, `Consultant`.

##### 9.4.5.2 Professional Details

**Fields**:
- **Degree**
  - **Type**: Text
  - **Required**: No
  - **Description**: Academic qualifications (e.g., MBBS, FCPS, MD).
  - **Validation / Rules**:
    - Free text; may use standardized abbreviations where available.
- **Speciality**
  - **Type**: Text
  - **Required**: No
  - **Description**: Medical specialization area.
  - **Validation / Rules**:
    - Free text; may later be migrated to a controlled list.
- **Institute**
  - **Type**: Text
  - **Required**: No
  - **Description**: Educational institution name.
  - **Validation / Rules**:
    - Free text.
- **M.E. ID**
  - **Type**: Text
  - **Required**: No
  - **Description**: Medical Council / Enrollment / Registration ID.
  - **Validation / Rules**:
    - Alphanumeric; optional length constraints may be applied by configuration.
- **Chamber / Unit**
  - **Type**: Text
  - **Required**: No
  - **Description**: Assigned chamber, unit, or consulting room (e.g., `Unit-01`).
  - **Validation / Rules**:
    - Free text; may be cross-validated with room/ward master if available.

##### 9.4.5.3 Personal Information

**Fields**:
- **Gender**
  - **Type**: Enum
  - **Required**: No
  - **Description**: Gender of the doctor.
  - **Allowed Values**: `Male`, `Female`, `Other`.
- **Birth Date**
  - **Type**: Date
  - **Required**: No
  - **Description**: Date of birth.
  - **Validation / Rules**:
    - Valid calendar date.
    - Must not be in the future.
- **Spouse Name**
  - **Type**: Text
  - **Required**: No
  - **Description**: Spouse’s full name.
  - **Validation / Rules**:
    - Maximum length 100 characters.
- **Spouse Date of Birth**
  - **Type**: Date
  - **Required**: No
  - **Description**: Spouse’s date of birth.
  - **Validation / Rules**:
    - Valid calendar date (may be in past only).
- **Marriage Date**
  - **Type**: Date
  - **Required**: No
  - **Description**: Date of marriage.
  - **Validation / Rules**:
    - Valid calendar date.
    - If both spouse date of birth and marriage date are provided, system may optionally validate that marriage date is after spouse date of birth.

##### 9.4.5.4 Contact & Address Information

**Fields**:
- **Phone Number**
  - **Type**: Text
  - **Required**: No
  - **Description**: Primary contact number.
  - **Validation / Rules**:
    - Phone format validation (supports country code, spaces, dashes).
    - May be normalized for storage (e.g., `+CCXXXXXXXXXX`).
- **Email**
  - **Type**: Email
  - **Required**: No
  - **Description**: Email address.
  - **Validation / Rules**:
    - Valid email format.
    - Must be unique across doctors **if provided**.
- **District**
  - **Type**: Text
  - **Required**: No
  - **Description**: District name.
  - **Validation / Rules**:
    - Free text.
- **Thana / Sub-District**
  - **Type**: Text
  - **Required**: No
  - **Description**: Police station / sub-district name.
  - **Validation / Rules**:
    - Free text.
- **Area / Locality**
  - **Type**: Text
  - **Required**: No
  - **Description**: Area or neighborhood.
  - **Validation / Rules**:
    - Free text.
- **Present Address**
  - **Type**: Text (multi-line)
  - **Required**: No
  - **Description**: Free-text present address (street, house details, etc.).

##### 9.4.5.5 Registration & Availability

**Fields**:
- **Registration Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Date when the doctor was registered in the system.
  - **Validation / Rules**:
    - Defaults to current date on create.
    - May not be in the future.
- **Doctor Active Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the doctor is currently active in the hospital roster.
  - **Allowed Values**: `Active`, `Inactive`.
  - **Validation / Rules**:
    - `Inactive` doctors:
      - Shall not appear as selectable in new appointment, admission, or prescription workflows (except where historical display is required).
      - Shall not be assignable as primary providers for new encounters.
- **Doctor Availability**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the doctor is currently available to receive appointments (short-term/day-to-day availability).
  - **Allowed Values**: `Available`, `Not Available`.
- **Doctor Availability**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the doctor is currently available to receive appointments (e.g., on duty vs on leave), independent of Active Status.
- **Prescription Status**
  - **Type**: Boolean
  - **Required**: No
  - **Description**: Indicates whether the doctor is authorized/expected to issue prescriptions within the system.
  - **Allowed Values**: `Yes`, `No`.

##### 9.4.5.6 Appointment & Serial Configuration

**Fields**:
- **Serial Start From**
  - **Type**: Number
  - **Required**: No
  - **Description**: Starting serial number for daily appointments.
  - **Validation / Rules**:
    - Integer, greater than or equal to 1 (if provided).
- **Patients Per Day**
  - **Type**: Number
  - **Required**: No
  - **Description**: Maximum number of patients per day for this doctor.
  - **Validation / Rules**:
    - Integer, greater than 0 (if provided).
    - If null or 0, may be interpreted as "no explicit limit" or a configurable default.
- **Number of Days Can Appointment**
  - **Type**: Number
  - **Required**: No
  - **Description**: Maximum number of days in advance that an appointment can be booked.
  - **Validation / Rules**:
    - Integer, greater than or equal to 0.
- **Serial Number in Apps List**
  - **Type**: Number
  - **Required**: No
  - **Description**: Display order for this doctor in mobile/web app lists.
  - **Validation / Rules**:
    - Integer; lower numbers appear earlier in lists.
- **Room Number (Optional)**
  - **Type**: Text or Number
  - **Required**: No
  - **Description**: Room or consulting cabin where the doctor sees patients.

##### 9.4.5.7 Appointment Source Limits

**Fields**:
- **Number of Appointments from Web**
  - **Type**: Number
  - **Required**: No
  - **Description**: Maximum number of appointments per day that can be booked via web.
  - **Validation / Rules**:
    - Integer, greater than or equal to 0.
- **Number of Appointments from Mobile**
  - **Type**: Number
  - **Required**: No
  - **Description**: Maximum number of appointments per day that can be booked via mobile app.
  - **Validation / Rules**:
    - Integer, greater than or equal to 0.

##### 9.4.5.8 Visit & Fee Configuration

**Fields**:
- **Doctor Visit Fee (New)**
  - **Type**: Decimal
  - **Required**: No
  - **Description**: Visit fee for new patients.
  - **Validation / Rules**:
    - Numeric, greater than or equal to 0.
    - If not set, department-level General Visit Amount may be used as default.
- **Doctor Visit Fee (Old / Returning)**
  - **Type**: Decimal
  - **Required**: No
  - **Description**: Visit fee for returning patients.
  - **Validation / Rules**:
    - Numeric, greater than or equal to 0.
- **Take Commission**
  - **Type**: Boolean
  - **Required**: No
  - **Description**: Indicates whether commission/fee-sharing applies for this doctor.
  - **Allowed Values**: `Yes`, `No`.
- **Reference Type**
  - **Type**: Text
  - **Required**: No
  - **Description**: Referral source type or reference category (e.g., self, internal, external).
  - **Validation / Rules**:
    - Free text, optional mapping to reference master.

##### 9.4.5.9 Communication Settings

**Fields**:
- **Send SMS**
  - **Type**: Boolean
  - **Required**: No
  - **Description**: Whether the doctor should receive SMS notifications (e.g., schedule summaries, alerts).
  - **Allowed Values**: `Yes`, `No`.

**System Actions (Form-Level)**:
- **Save**: Persist doctor information with validation and code generation.
- **List**: Navigate to the doctor list view.
- **Print List**: Print or export the doctor list (subject to permissions).

**User Interface – Form View**:
- Multi-section form organized by the field groups above (tabs or collapsible sections).
- Clear labels and placeholders for all fields.
- Client-side and server-side validation with inline error messages.
- Primary action buttons: **Save**, **Cancel/Back to List**, and (where appropriate) **Print List**.

**User Interface – List View**:
- **Doctor List Table**:
  - Columns (at minimum):
    - Serial No
    - Doctor Code
    - Doctor Name
    - Cell Phone
    - Doctor Status (Indoor/Outdoor, Availability)
    - Take Commission (Yes/No)
    - Doctor Type
    - SMS Enabled (Yes/No)
    - Department
  - Additional filterable/displayable fields (via advanced filter or column selector):
    - Degree
    - Gender
    - Speciality
    - District / Thana / Area / Present Address
    - Institute
    - Registration Date
    - Birth Date
    - Spouse Name / Spouse Birth Date
    - Marriage Date
    - Email
    - M.E. ID
    - Serial Start From
    - Chamber / Room
    - Reference Type
    - Doctor Visit Fee (New/Old)
    - Doctor Availability
    - Patients Per Day
    - Send SMS
    - Serial Number in Apps List
    - Number of Days Can Appointment
    - Number of Appointments from Web
    - Number of Appointments from Mobile
    - Prescription Status
- **Filters**:
  - Department
  - Doctor Type
  - Doctor Availability
  - Indoor/Outdoor Status
  - Text search by Doctor Name, Code, Phone, Email, Speciality.
- **Row Actions**:
  - Edit doctor.
  - Delete (soft delete, subject to business rules).
  - Activate / Deactivate doctor availability or active status.
- **Export and Pagination**:
  - Export doctor list (e.g., CSV/Excel) honoring active filters.
  - Standard pagination with configurable page size.

**Business Rules**:
- **Doctor Code Generation**:
  - Doctor Code is auto-generated based on department and doctor name following the defined format.
  - In case of potential duplicates (same initial letters and serial), serial number shall be incremented until a unique code is found.
- **Department Dependency**:
  - Department must exist and be Active before doctor registration.
  - If a department becomes Inactive, new doctor registrations under that department shall be blocked.
- **Email and Phone**:
  - Email, if provided, must be unique across doctors.
  - Phone number format must pass validation; normalization may be applied.
- **Deletion and Soft Delete**:
  - A doctor with any **past, active, or future appointments** cannot be permanently (hard) deleted from the database.
  - System should prefer soft delete (e.g., marking doctor as inactive/archived via Doctor Active Status) to preserve history and appointment references.
  - Soft delete (archiving) shall be blocked while the doctor has **active or future appointments**, unless those appointments are cancelled or reassigned through a controlled workflow.
- **Appointment Limits**:
  - If **Patients Per Day** is null or 0, the system treats it as unlimited or uses a configurable default.
  - Web and mobile appointment limits should not exceed the effective Patients Per Day; if they do, system should warn the user or auto-adjust based on configuration.
- **Fee Validation**:
  - Visit fees (new and returning) must not be negative.
  - If doctor-level fee is not set, department-level fee may be used as default.

**Edge Cases & Exceptions**:
- **Duplicate Doctor Code**:
  - If code generation collides with an existing Doctor Code, system automatically increments the numeric serial; if still not possible, a clear error should be displayed.
- **Same Name, Different Departments**:
  - Doctors with the same name in different departments receive different codes due to department prefix.
- **Missing Department During Registration**:
  - If the selected department is deleted or inactivated after registration starts but before save, saving shall fail with a clear message and prompt user to re-select a valid department.
- **Invalid Email Format**:
  - Email format is validated; errors are shown inline with clear messages.
- **Phone Number Variations**:
  - System accepts common formats and normalizes internally; if not parsable, shows a validation error.
- **Future Birth Date**:
  - Birth Date cannot be in the future; validation error should be shown.
- **Spouse and Marriage Date Consistency**:
  - If provided, Spouse Date of Birth must be earlier than Marriage Date (configurable rule; violation surfaces a warning or error).
- **Appointment Limits Exceeding Patients Per Day**:
  - If web/mobile limits in total exceed Patients Per Day, system should display a warning and may auto-adjust according to configuration.

**Error Handling**:
- **Form Validation**:
  - All validation errors should be displayed together (summary + inline), not one at a time.
- **Save Failures**:
  - On backend save failure, entered form data should be preserved so the user can correct and retry.
- **Auto-Fill / Lookup Failures**:
  - If any auto-generated or looked-up field (e.g., Doctor Code, department lookup) cannot be resolved, the system should show a clear error and allow retry without losing other data.
- **Code Generation Failure**:
  - If Doctor Code cannot be generated after several attempts (e.g., exhausted serial range), system should present a descriptive error and log details for support.
- **Database Constraint Violations**:
  - Unique constraint violations (e.g., email, code) should be mapped to user-friendly messages that identify the conflicting field.

**User Roles and Access Control – Doctor Master**:
- **Hospital Admin / Super Admin**:
  - Full access: create, view, update, soft-delete, activate/inactivate doctors; configure limits and fees; export and print lists.
- **HR / Administrative Staff**:
  - Create and update doctor demographic and professional details.
  - Change availability, appointment limits, and fee configuration (subject to policy).
  - Soft-delete (archive) doctors who have no active/future appointments.
  - View and export doctor lists.
- **Scheduling / Front Desk Staff**:
  - View doctor list and details necessary for scheduling.
  - Change doctor availability (Available / Not Available) if permitted by configuration.
  - No access to modify codes or core identification fields.
- **Clinical Users (Doctors, Nurses)**:
  - Read-only access to doctor master data where needed (e.g., for referrals).
  - No access to create, delete, or change other doctors’ master data.
- All operations shall be protected via role-based access control (RBAC) and audited.

**Functional Requirements – Doctor Registration and Management**:
- The system shall allow authorized users to **create**, **view**, **update**, and **soft-delete** doctor records.
- The system shall **auto-generate** Doctor Codes according to the configured format and ensure global uniqueness.
- The system shall prevent saving a doctor record if mandatory fields (e.g., Doctor Name, Department, Doctor Type, Indoor/Outdoor Status, Registration Date) are missing or invalid.
- The system shall prevent assigning a doctor to a department that is **Inactive** or soft-deleted.
- The system shall prevent soft-deleting (archiving) a doctor who has any **active or future appointments**, unless those appointments are cancelled or reassigned as part of the operation, and shall display a clear explanation or guided workflow to the user.
- The system shall support **search**, **filter**, and **sort** operations on the doctor list without full-page reloads (where technically feasible).
- The system shall allow **activation/inactivation** of doctor availability without changing the core master record (e.g., temporary unavailability).
- The system shall expose doctor master data through internal APIs/services for use by scheduling, OPD/IPD, prescription, and billing modules.

**Cross-Module Integration Requirements**:
- **Scheduling / Appointments**:
  - Appointment creation screens shall consume doctor master data (name, department, availability, limits).
  - When a doctor is set to `Not Available`, new appointment creation for that doctor shall be blocked or generate a warning per configuration.
  - Changes to Patients Per Day and source limits shall immediately affect scheduling capacity calculations.
- **OPD/IPD / Encounter Management**:
  - Encounters shall reference the doctor via Doctor Code/ID; inactivating a doctor must not break existing encounters.
- **Prescription Module**:
  - Prescription creation shall use doctor master data (name, type, M.E. ID, department) and enforce that only doctors with Prescription Status = `Yes` (and appropriate role) can issue prescriptions.
- **Billing / Finance**:
  - Doctor Visit Fees and commission flags shall be available for billing calculations and revenue reporting.
- **Reporting and Analytics**:
  - Doctor master attributes (type, department, availability, fees) shall be available as dimensions and filters in clinical and operational reports.

**Module-Specific Non-Functional Requirements**:
- Doctor form load (for create or edit) should complete within **2 seconds** under normal load for typical data volumes.
- Doctor list view (with default filters) should load within **3 seconds** for up to **5,000** doctor records, using server-side pagination and filtering.
- The UI for doctor registration and list management shall be **responsive** and usable on standard desktop and tablet resolutions.
- All doctor-related texts, labels, and validation messages shall support **localization**, consistent with the rest of the hospital module.
- The solution shall handle **concurrent edits** gracefully using optimistic locking or equivalent mechanisms and clearly inform the user of conflicts.

**Audit and Logging Requirements**:
- The system shall maintain an **audit trail** for all doctor master changes, including:
  - Creation, updates, soft-delete/restore, status changes (availability, Indoor/Outdoor, Doctor Type).
  - Changes to key financial fields (visit fees, commission flag).
- Each audited event shall capture:
  - Who performed the action (user ID / role).
  - When the action occurred (timestamp).
  - What was changed (field-level before/after values).
- Audit data shall be queryable by authorized roles (e.g., compliance, administrators) for a configurable retention period.

---

#### 9.4.6 Doctor Module – Doctor Scheduling Management

**Description**:  
This subsection defines the **Doctor Scheduling Management** capability within the Doctor Module. It enables healthcare administrators to create, manage, and maintain day-wise, shift-based time slot schedules for doctors, configure appointment duration settings per doctor, and handle schedule modifications including leave management and appointment rescheduling workflows.

**Purpose**:  
To provide a comprehensive scheduling system that:
- Enables efficient management of doctor availability by day and shift
- Configures appointment duration settings to optimize patient flow
- Prevents scheduling conflicts and overlapping time slots
- Supports appointment availability calculations for the appointment booking system
- Facilitates leave management and appointment rescheduling processes
- Ensures accurate scheduling data for operational planning and reporting

**Features**:
- Create and manage doctor schedules with day-wise, shift-based time slots
- Configure appointment duration per patient for each schedule entry
- View, edit, and delete individual schedule entries
- Search and filter doctors in the schedule list view
- Validate schedule entries to prevent conflicts and invalid configurations
- Handle schedule modifications including leave management
- Support multiple shifts per day for the same doctor (e.g., Morning + Evening)
- Integrate with appointment booking system to control availability
- Provide inline editing capabilities in list view
- Support configurable pagination (default: 10 records per page)
- Copy schedules from one doctor to another or duplicate schedules for similar doctors
- Bulk schedule operations (create, update, delete multiple schedule entries)
- Schedule templates for common scheduling patterns
- Export and import schedule data (CSV/Excel format)
- Calculate and display available appointment slots based on schedule configuration
- Schedule conflict detection and resolution workflows
- Leave management with automatic appointment cancellation and patient notification
- Schedule history and audit trail for tracking changes

**Data Fields – Overview**  
Schedule data is organized into logical groups:
- **Doctor Selection Fields** (auto-populated from Doctor Master)
- **Schedule Entry Fields** (day, shift, time, duration)
- **Action Controls** (add, edit, delete schedule entries)

##### 9.4.6.1 Doctor Selection Fields

**Fields**:
- **Doctor Code**
  - **Type**: Auto-suggest Dropdown (lookup from Doctor Master; see 9.4.5)
  - **Required**: Yes
  - **Description**: Unique identifier for the doctor. Selecting a doctor code auto-populates related doctor information fields.
  - **Validation / Rules**:
    - Must reference an **Active** and non-deleted doctor from the Doctor Master.
    - Doctor must exist in the system before scheduling can be created.
    - Only doctors with `Doctor Active Status = Active` and `Doctor Availability = Available` should appear in the dropdown (unless configuration allows scheduling inactive doctors).
- **Doctor Name**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes (auto-populated)
  - **Description**: Full name of the doctor, automatically populated when Doctor Code is selected.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master; not directly editable in the schedule form.
- **Doctor Department**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes (auto-populated)
  - **Description**: Primary department of the doctor, automatically populated when Doctor Code is selected.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master; not directly editable in the schedule form.
- **Doctor Degree**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: No (auto-populated if available)
  - **Description**: Academic qualifications of the doctor, automatically populated when Doctor Code is selected.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master; not directly editable in the schedule form.
- **Doctor Speciality**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: No (auto-populated if available)
  - **Description**: Medical specialization of the doctor, automatically populated when Doctor Code is selected.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master; not directly editable in the schedule form.

##### 9.4.6.2 Schedule Entry Fields

**Fields**:
- **Days**
  - **Type**: Dropdown (auto-suggest)
  - **Required**: Yes
  - **Description**: Day of the week for which the schedule applies.
  - **Allowed Values**: `Monday`, `Tuesday`, `Wednesday`, `Thursday`, `Friday`, `Saturday`, `Sunday`.
  - **Validation / Rules**:
    - Required field; must select a valid day.
    - Multiple schedule entries can exist for the same day (different shifts).
    - System shall prevent overlapping schedules for the same day and shift combination (see Business Rules).
- **Shift**
  - **Type**: Dropdown (auto-suggest)
  - **Required**: Yes
  - **Description**: Shift type during which the doctor is available.
  - **Allowed Values**: `Morning`, `Evening`, `Night`.
  - **Validation / Rules**:
    - Required field; must select a valid shift.
    - Multiple shifts per day are allowed (e.g., Morning + Evening for the same doctor on the same day).
    - System shall prevent overlapping schedules for the same day and shift combination.
- **Start Time**
  - **Type**: Time
  - **Required**: Yes
  - **Description**: The start time of the doctor's schedule for this day and shift.
  - **Format**: HH:MM (24-hour format, e.g., `09:00`, `14:30`).
  - **Validation / Rules**:
    - Required field; must be a valid time format.
    - Must be within valid time range (00:00-23:59).
    - Must be less than End Time (unless handling midnight crossover; see Edge Cases).
    - Time format shall be consistent across the system (prefer 24-hour format for clarity).
- **End Time**
  - **Type**: Time
  - **Required**: Yes
  - **Description**: The end time of the doctor's schedule for this day and shift.
  - **Format**: HH:MM (24-hour format, e.g., `12:00`, `18:30`).
  - **Validation / Rules**:
    - Required field; must be a valid time format.
    - Must be within valid time range (00:00-23:59).
    - Must be greater than Start Time (unless handling midnight crossover; see Edge Cases).
    - System shall prevent schedules where Start Time equals End Time.
- **Duration (per patient)**
  - **Type**: Number (Integer, minutes)
  - **Required**: Yes
  - **Description**: Duration allocated per patient appointment in minutes. This determines how many appointment slots can be created within the schedule time window.
  - **Validation / Rules**:
    - Required field; must be a positive integer.
    - Must be greater than 0.
    - System shall warn if duration is too short (< 5 minutes) or too long (exceeds shift time significantly).
    - Duration should be reasonable relative to the schedule window (e.g., a 2-hour schedule with 30-minute duration allows 4 appointments).
- **Action**
  - **Type**: Buttons (Add/Edit/Delete)
  - **Required**: No
  - **Description**: Controls for managing schedule entries within the schedule table.
  - **Validation / Rules**:
    - **Add**: Creates a new schedule entry row (may be inline or in a separate form).
    - **Edit**: Enables editing of an existing schedule entry.
    - **Delete**: Removes a schedule entry (see Business Rules for deletion restrictions).

##### 9.4.6.3 User Interface Requirements

**Form View – Schedule Management**:
- **Doctor Selection Section**:
  - Auto-suggest dropdown for Doctor Code with search/filter capabilities (supports partial match on Code, Name, Department).
  - Auto-population of Doctor Name, Department, Degree, and Speciality upon selection (displayed in read-only fields or cards).
  - Visual indication of selected doctor information with clear visual separation.
  - Display doctor's current availability status (Available/Not Available) from Doctor Master.
  - Quick link to view doctor's master profile (if permissions allow).
- **Schedule Table**:
  - Tabular display of schedule entries with the following columns:
    - **Serial No** (auto-incrementing row number)
    - **Days** (dropdown with all weekdays)
    - **Shift** (dropdown: Morning/Evening/Night)
    - **Start Time** (time picker, HH:MM format)
    - **End Time** (time picker, HH:MM format)
    - **Duration (per patient)** (number input, minutes)
    - **Available Slots** (calculated field, read-only: shows number of appointment slots available based on schedule window and duration)
    - **Action** (buttons: Edit, Delete, Copy)
  - Inline Add/Edit/Delete actions for each schedule entry.
  - "Add New Schedule" button to add a new row to the table.
  - Ability to add multiple schedule entries for the same doctor (different days/shifts).
  - Visual feedback for validation errors:
    - Highlight invalid rows/fields in red or with error icons.
    - Inline error messages below or next to invalid fields.
    - Summary of all validation errors at the top of the form.
  - Clear indication of required fields (asterisk or visual indicator).
  - Color coding for shift types:
    - Morning: Light blue/cyan background
    - Evening: Orange/amber background
    - Night: Dark blue/navy background
  - Schedule conflict indicators (warning icons for overlapping schedules).
- **Schedule Summary Panel** (optional, collapsible):
  - Display calculated summary:
    - Total schedule entries for the doctor
    - Total available appointment slots per week
    - Schedule coverage by day (which days have schedules)
    - Average duration per patient
  - Visual calendar view showing schedule distribution across the week (optional enhancement).
- **Save Functionality**:
  - **Save** button to persist all schedule entries for the selected doctor.
  - **Save and Add Another** button to save current doctor's schedule and clear form for next doctor.
  - **Cancel** button to discard unsaved changes and return to list view.
  - Validation before save (prevents saving invalid or conflicting schedules).
  - Success/error messaging after save operation:
    - Success: "Schedule saved successfully for Dr. [Name]. [X] schedule entries created/updated."
    - Error: Clear error messages with actionable guidance.
  - Confirmation dialog if unsaved changes exist when navigating away.
- **Bulk Actions** (if multiple schedules selected):
  - Copy selected schedules to another doctor
  - Delete multiple schedules (with validation)
  - Export selected schedules

**List View – Schedule Overview**:
- **Doctor List Table**:
  - Columns (at minimum):
    - **Serial No** (row number)
    - **Doctor Code** (clickable, links to schedule form)
    - **Doctor Name** (clickable, links to schedule form)
    - **Department** (filterable)
    - **Speciality** (filterable)
    - **Schedule Summary** (e.g., "Mon-Fri Morning (09:00-12:00), Sat-Sun Evening (17:00-20:00)")
    - **Total Schedule Entries** (count of schedule entries)
    - **Total Weekly Slots** (calculated total available appointment slots per week)
    - **Status** (Active/Inactive, with visual indicators)
    - **Last Updated** (timestamp of last schedule modification)
    - **Actions** (Edit, View, Copy, Delete, Export)
  - Additional filterable/displayable columns (via advanced filter or column selector):
    - Doctor Degree
    - Schedule Coverage (days with schedules)
    - Average Duration per Patient
    - Schedule Conflicts (if any)
    - Next Available Appointment Date
- **Filters and Search**:
  - **Quick Filters**:
    - Department (dropdown, multi-select)
    - Doctor Type (Visiting/Permanent/Consultant)
    - Doctor Availability (Available/Not Available)
    - Schedule Status (Has Schedule/No Schedule/Incomplete Schedule)
    - Shift Type (Morning/Evening/Night)
  - **Advanced Search**:
    - Text search by Doctor Code, Name, Department, Speciality
    - Date range filter for "Last Updated"
    - Schedule entry count range
  - **Filter Presets** (saveable filter combinations):
    - "Doctors with incomplete schedules"
    - "Doctors with schedule conflicts"
    - "Doctors without schedules"
    - "Doctors available on weekends"
- **Row Actions**:
  - **Edit**: Opens schedule form in edit mode for the selected doctor
  - **View**: Opens schedule form in read-only mode
  - **Copy Schedule**: Copy all schedules from this doctor to another doctor
  - **Delete All Schedules**: Delete all schedule entries for the doctor (with validation)
  - **Export Schedule**: Export doctor's schedule to CSV/Excel
  - **View Schedule History**: View audit trail of schedule changes
- **Bulk Actions** (when multiple rows selected):
  - Copy schedules from one doctor to multiple doctors
  - Export selected doctors' schedules
  - Delete schedules for selected doctors (with validation)
- **Pagination and Export**:
  - Configurable pagination (default: 10 records per page; options: 10, 25, 50, 100).
  - Export schedule list (CSV/Excel) honoring active filters.
  - Print schedule list with current filters applied.
- **Inline Editing** (optional enhancement):
  - Quick edit mode: Click on schedule summary cell to edit inline
  - Quick actions menu: Right-click or action button for common operations
  - Drag-and-drop to reorder schedule entries (if applicable)

**Schedule Detail View** (read-only or edit mode):
- **Doctor Information Card**:
  - Doctor Code, Name, Department, Degree, Speciality
  - Current availability status
  - Link to doctor master profile
- **Schedule Calendar View** (optional):
  - Weekly calendar grid showing schedule entries
  - Color-coded by shift type
  - Click on time slot to view/edit schedule details
  - Visual indicators for:
    - Booked appointments (if integrated)
    - Available slots
    - Schedule conflicts
- **Schedule List View**:
  - Detailed table of all schedule entries
  - Expandable rows showing appointment slot breakdown
  - Filter by day, shift, or time range
- **Appointment Slot Breakdown**:
  - For each schedule entry, display:
    - Total time window (e.g., 09:00-12:00 = 3 hours)
    - Duration per patient (e.g., 30 minutes)
    - Calculated available slots (e.g., 6 slots)
    - Slot times (e.g., 09:00, 09:30, 10:00, 10:30, 11:00, 11:30)
  - Integration with appointment booking to show booked vs. available slots

**UI References and Design Considerations**:
- All dropdown fields shall use auto-suggest functionality for improved usability.
- Time pickers shall use consistent format (24-hour format recommended) with visual time selector.
- Schedule table shall support responsive design for tablet and desktop views.
- Color coding or icons shall be used to indicate shift types (Morning/Evening/Night) consistently.
- Loading indicators during save operations, data fetching, and validation.
- Confirmation dialogs for delete operations, especially when appointments exist:
  - Show count of affected appointments
  - List of affected patients (if permissions allow)
  - Options to cancel appointments or maintain them
- Toast notifications for successful operations (save, delete, copy).
- Keyboard shortcuts for common actions (e.g., Ctrl+S to save, Esc to cancel).
- Undo/Redo functionality for schedule modifications (optional enhancement).
- Help tooltips for complex fields (e.g., duration calculation, slot availability).
- Accessibility: Screen reader support, keyboard navigation, ARIA labels.

##### 9.4.6.3.1 Appointment Slot Calculation Logic

**Calculation Formula**:
- **Total Schedule Duration** = End Time - Start Time (in minutes)
- **Available Appointment Slots** = Floor(Total Schedule Duration / Duration per patient)
- **Slot Times**: Generated sequentially starting from Start Time, incrementing by Duration per patient

**Examples**:
- **Example 1**: Start Time = 09:00, End Time = 12:00, Duration = 30 minutes
  - Total Duration = 180 minutes (3 hours)
  - Available Slots = Floor(180 / 30) = 6 slots
  - Slot Times: 09:00, 09:30, 10:00, 10:30, 11:00, 11:30
- **Example 2**: Start Time = 14:00, End Time = 16:30, Duration = 20 minutes
  - Total Duration = 150 minutes (2.5 hours)
  - Available Slots = Floor(150 / 20) = 7 slots
  - Slot Times: 14:00, 14:20, 14:40, 15:00, 15:20, 15:40, 16:00
- **Example 3**: Start Time = 09:00, End Time = 10:00, Duration = 45 minutes
  - Total Duration = 60 minutes (1 hour)
  - Available Slots = Floor(60 / 45) = 1 slot
  - Slot Time: 09:00
  - System shall warn: "Only 1 appointment slot available. Consider adjusting duration or extending schedule time."

**Validation Rules**:
- If calculated slots = 0, system shall prevent saving and display error: "Schedule duration is too short for the specified patient duration."
- If calculated slots < 2, system shall warn: "Only [X] appointment slot(s) available. Consider adjusting duration or extending schedule time."
- System shall display calculated slots in real-time as user enters/changes schedule times and duration.

**Integration with Appointment Booking**:
- Appointment booking system shall use this calculation to determine available time slots.
- Booked appointments reduce available slots in real-time.
- System shall prevent overbooking (booking more appointments than available slots).

##### 9.4.6.3.2 Schedule Templates and Copy Functionality

**Schedule Templates**:
- **Purpose**: Pre-defined schedule patterns for common scheduling scenarios to speed up schedule creation.
- **Template Types**:
  - **Standard Full-Time**: Monday-Friday, Morning shift (09:00-12:00), 30-minute duration
  - **Standard Part-Time**: Monday-Wednesday-Friday, Morning shift (09:00-12:00), 30-minute duration
  - **Evening Clinic**: Monday-Friday, Evening shift (17:00-20:00), 30-minute duration
  - **Weekend Only**: Saturday-Sunday, Morning shift (09:00-12:00), 30-minute duration
  - **Custom Templates**: User-defined templates saved for reuse
- **Template Features**:
  - Apply template to selected doctor(s)
  - Modify template before applying
  - Save current schedule as template
  - Share templates across users (if permissions allow)
  - Template library management (create, edit, delete templates)

**Copy Schedule Functionality**:
- **Copy from Doctor to Doctor**:
  - Select source doctor (with existing schedules)
  - Select target doctor(s) (one or multiple)
  - Option to copy all schedules or selected schedule entries
  - Option to adjust times/duration during copy
  - Validation: Ensure target doctor(s) can accept copied schedules (no conflicts)
- **Copy Schedule Entry**:
  - Within same doctor: Copy a schedule entry to another day/shift
  - Duplicate schedule entry with option to modify
- **Bulk Copy**:
  - Copy schedules from one doctor to multiple doctors
  - Useful for department-wide schedule standardization
  - Validation and conflict detection for all target doctors

##### 9.4.6.3.3 Bulk Operations

**Bulk Schedule Creation**:
- **Multi-Doctor Schedule Setup**:
  - Select multiple doctors (by department, type, or manual selection)
  - Apply same schedule pattern to all selected doctors
  - Option to customize per doctor before final save
  - Validation: Check for conflicts for each doctor individually
  - Report: Summary of successful and failed schedule creations
- **Bulk Schedule Update**:
  - Select multiple schedule entries (across one or multiple doctors)
  - Apply common changes (e.g., change duration from 30 to 20 minutes, shift time adjustment)
  - Validation: Ensure updates don't create conflicts
  - Preview changes before applying
- **Bulk Schedule Deletion**:
  - Select multiple schedule entries to delete
  - Validation: Check for existing appointments
  - Confirmation dialog showing:
    - Count of schedules to be deleted
    - Count of affected appointments
    - List of affected doctors
  - Option to cancel affected appointments or maintain them
- **Bulk Export/Import**:
  - Export schedules for multiple doctors to CSV/Excel
  - Import schedules from CSV/Excel file
  - Template-based import with validation
  - Error reporting for invalid entries during import

##### 9.4.6.3.4 Export and Import Capabilities

**Export Functionality**:
- **Export Formats**: CSV, Excel (XLSX)
- **Export Options**:
  - Export single doctor's schedule
  - Export multiple doctors' schedules (based on filters)
  - Export schedule template
- **Export Fields**:
  - Doctor Code, Doctor Name, Department
  - Day, Shift, Start Time, End Time, Duration (per patient)
  - Available Slots (calculated)
  - Last Updated timestamp
- **Use Cases**:
  - Backup schedule data
  - Share schedules with external systems
  - Schedule analysis in spreadsheet tools
  - Schedule reporting and documentation

**Import Functionality**:
- **Import Formats**: CSV, Excel (XLSX)
- **Import Process**:
  1. Upload file or paste data
  2. Map columns to schedule fields
  3. Preview imported data
  4. Validate imported data (doctor existence, time format, conflicts)
  5. Show validation report (successful, warnings, errors)
  6. Confirm import or fix errors and retry
- **Import Validation**:
  - Doctor Code must exist in Doctor Master
  - Time format validation (HH:MM)
  - Duration must be positive integer
  - Conflict detection (overlapping schedules)
  - Required fields check
- **Import Options**:
  - **Add New**: Add new schedule entries (skip if duplicate exists)
  - **Update Existing**: Update existing schedule entries, add new ones
  - **Replace All**: Delete existing schedules for imported doctors, add new ones
- **Error Handling**:
  - Partial import: Import valid entries, skip invalid ones
  - Detailed error report with row numbers and error descriptions
  - Option to download error report for correction

##### 9.4.6.4 Business Rules

**Schedule Creation and Validation**:
- **Doctor Registration Prerequisite**: The doctor must be registered in the Doctor Master (see 9.4.5) before a schedule can be created. System shall prevent scheduling for non-existent or soft-deleted doctors.
- **No Overlapping Schedules**: System shall prevent or warn when schedules overlap for the same doctor, day, and shift combination. Overlap detection shall consider:
  - Same day and same shift with overlapping time ranges.
  - Example: If Doctor A has Morning shift 09:00-12:00 on Monday, system shall prevent or warn if attempting to add another Morning shift 10:00-13:00 on Monday.
- **Time Validation**: End Time must be greater than Start Time for schedules within the same day. For midnight crossover schedules (e.g., 22:00-02:00), system shall handle appropriately (see Edge Cases).
- **Duration Validation**: Duration per patient must be a positive integer (minutes). System shall warn if duration is unreasonably short (< 5 minutes) or if total duration exceeds the schedule window significantly.
- **Valid Time Range**: All times must be within valid range (00:00-23:59). System shall validate time format consistency.

**Schedule Modification**:
- **Edit Existing Schedules**: Users with appropriate permissions can edit schedule entries. System shall validate edited schedules against all business rules (overlap, time validation, etc.).
- **Delete Schedules**: System shall prevent deletion of schedules that have existing or future appointments, unless:
  - Appointments are cancelled or rescheduled as part of the deletion workflow.
  - User explicitly confirms deletion with understanding of impact.
  - System provides clear warning about affected appointments.
- **Multiple Shifts Per Day**: System shall allow multiple shifts per day for the same doctor (e.g., Morning 09:00-12:00 and Evening 17:00-20:00 on the same day). Each shift must be non-overlapping.

**Schedule and Appointment Integration**:
- **Appointment Availability**: Schedule entries directly affect appointment availability. When a schedule is created, modified, or deleted, the appointment booking system shall reflect the change in available time slots.
- **Schedule Changes with Existing Appointments**: When modifying or deleting a schedule that has existing appointments:
  - System shall display a list of affected appointments.
  - System shall provide options to cancel, reschedule, or maintain appointments (where possible).
  - System shall trigger notification workflows (e.g., call center notification for patient contact).

**Leave Management and Rescheduling**:
- **Doctor Leave Handling**: When a doctor is marked as unavailable (leave) or schedule is temporarily suspended, the system shall:
  - **Leave Date Configuration**:
    - Allow marking doctor as unavailable for specific date(s) or date range
    - Option to mark as "Leave" (temporary) or "Schedule Suspended" (indefinite)
    - Leave can be configured at schedule level (specific schedule entries) or doctor level (all schedules for date range)
  - **Appointment Impact Analysis**:
    - System shall identify all appointments for the leave date(s) that fall within affected schedule time windows
    - Display count and list of affected appointments with patient details
    - Categorize appointments by status (Confirmed, Pending, Completed)
  - **Appointment Cancellation Workflow**:
    - Option to cancel all affected appointments automatically
    - Option to cancel only future appointments (preserve past appointments)
    - Option to maintain appointments (if schedule will be covered by another doctor)
    - Confirmation dialog showing:
      - Number of appointments to be cancelled
      - Patient list with contact information
      - Estimated impact (revenue, patient satisfaction)
  - **Call Center Notification**:
    - Automatic generation of call center task list with:
      - Patient name, phone number, appointment date/time
      - Reason for cancellation (doctor leave)
      - Suggested alternative dates/times (if available)
      - Priority level (urgent appointments, follow-ups)
    - Integration with call center system to assign tasks to call center staff
    - Tracking of call center contact attempts and outcomes
  - **Manual Rescheduling Support**:
    - Call center staff can reschedule appointments using regular appointment booking process
    - System shall suggest available alternative time slots (same doctor, different date, or different doctor, same department)
    - Option to reschedule to another doctor in the same department (if patient agrees)
    - Rescheduling workflow:
      1. View affected appointment details
      2. Search for available alternative slots
      3. Select new appointment date/time
      4. Confirm rescheduling with patient
      5. Update appointment record
      6. Send confirmation to patient (SMS/Email if configured)
  - **Schedule Configuration Maintenance**:
    - Schedule configuration is NOT deleted during leave
    - Schedule remains in system but appointments are blocked for leave dates
    - Option to temporarily disable specific schedule entries for leave period
    - Schedule automatically reactivates after leave period (if configured)
- **Rescheduling Process**: Rescheduling appointments due to leave shall follow the regular appointment booking workflow:
  - **Step 1 - Patient Contact**: Call center contacts patient to inform about cancellation and offer rescheduling
  - **Step 2 - Alternative Selection**: Patient selects preferred alternative (same doctor different date, or different doctor)
  - **Step 3 - Appointment Booking**: Call center staff books new appointment using regular appointment booking process
  - **Step 4 - Confirmation**: System sends confirmation to patient (SMS/Email if configured)
  - **Step 5 - Cancellation**: Original appointment is marked as "Cancelled - Rescheduled" with reference to new appointment
  - System shall not automatically reschedule; manual intervention by call center or scheduling staff is required
- **Leave Management Reporting**:
  - Track leave frequency and duration per doctor
  - Report on appointment cancellation impact due to leaves
  - Monitor call center performance in contacting and rescheduling patients
  - Analyze rescheduling success rate and patient satisfaction

##### 9.4.6.5 Edge Cases & Exceptions

**Overlapping Schedules**:
- **Prevention vs. Warning**: System configuration shall determine whether overlapping schedules are prevented (hard validation) or warned (soft validation with user override option). Default behavior: prevent overlapping schedules for same day/shift.
- **Partial Overlaps**: System shall detect and handle partial overlaps (e.g., Schedule A: 09:00-12:00, Schedule B: 10:00-13:00 on same day/shift).

**Midnight Crossover**:
- **Handling Overnight Schedules**: System shall support schedules that cross midnight (e.g., 22:00-02:00 for Night shift). Implementation options:
  - Store End Time as next day time (02:00) with a flag indicating midnight crossover.
  - Calculate duration considering next day (e.g., 22:00 to 02:00 = 4 hours).
  - Validate that End Time (next day) is reasonable (e.g., within 8 hours of Start Time).
- **Default Behavior**: If midnight crossover is not supported in initial implementation, system shall prevent End Time < Start Time and require End Time to be on the same calendar day.

**Same Start/End Time**:
- **Prevention**: System shall prevent schedules where Start Time equals End Time (zero-duration schedules are invalid).

**Very Short Duration**:
- **Warning Threshold**: System shall warn if duration per patient is less than 5 minutes (configurable threshold).
- **Business Justification**: Very short durations may indicate data entry error or unrealistic scheduling.

**Very Long Duration**:
- **Warning Threshold**: System shall warn if duration per patient exceeds the schedule window significantly (e.g., duration > 50% of schedule window).
- **Example**: If schedule is 09:00-10:00 (1 hour) and duration is set to 45 minutes, system shall warn that only one appointment slot is available.

**Multiple Shifts Same Day**:
- **Support**: System shall allow and properly handle multiple shifts per day for the same doctor (e.g., Morning + Evening on Monday).
- **Validation**: Each shift must be non-overlapping. System shall validate each shift independently.

**Holiday Schedules**:
- **Special Days Handling**: If the system supports holiday or special day scheduling, schedules may need to be handled differently:
  - Option 1: Holiday schedules override regular weekly schedules for specific dates.
  - Option 2: Regular schedules apply, but appointments are blocked on holidays via separate configuration.
  - **Initial Implementation**: Regular weekly schedules apply; holiday handling may be added in future enhancement.

**Past Date Scheduling**:
- **Prevention**: System shall prevent creating schedules for past dates (if date-based scheduling is implemented). For weekly recurring schedules, this rule may not apply (schedules are day-of-week based, not date-based).

**Schedule Deletion with Appointments**:
- **Warning and Workflow**: When deleting a schedule that has existing or future appointments:
  - System shall display count and list of affected appointments.
  - System shall require explicit confirmation with understanding of impact.
  - System shall provide option to cancel affected appointments or maintain them (if schedule is being replaced).
  - System shall trigger notification workflow (call center) to contact patients if appointments are cancelled.

##### 9.4.6.6 Error Handling

**Invalid Time Format**:
- **Validation**: System shall validate time format consistently (prefer 24-hour format: HH:MM).
- **Error Message**: Clear error message indicating expected format (e.g., "Time must be in HH:MM format (24-hour)").
- **Conversion**: System may attempt to convert common time formats (e.g., 12-hour with AM/PM) but shall validate and confirm with user.

**Schedule Conflict**:
- **Detection**: System shall detect schedule conflicts (overlapping schedules for same doctor/day/shift) before saving.
- **Error Message**: Clear, specific error message indicating the conflict (e.g., "Schedule conflicts with existing Morning shift on Monday (09:00-12:00). Please adjust times or select a different shift.").
- **Resolution Guidance**: System may suggest resolution options (e.g., "Available time slots: 13:00-17:00").

**Doctor Not Found**:
- **Handling**: If selected doctor is deleted or becomes inactive after schedule creation:
  - System shall display warning in schedule list/view.
  - System shall prevent new schedule creation for inactive/deleted doctors.
  - System may allow viewing historical schedules but prevent editing.
  - System shall provide option to reassign schedules to another doctor (if business rules allow).

**Bulk Schedule Updates**:
- **Partial Failure Handling**: If bulk operations (e.g., updating multiple schedule entries) encounter partial failures:
  - System shall complete successful updates and roll back failed updates.
  - System shall provide detailed report of successful and failed operations.
  - System shall allow user to retry failed operations individually.

**Data Integrity Errors**:
- **Referential Integrity**: System shall maintain referential integrity between schedules and doctor master data. If doctor is deleted, system shall handle orphaned schedules appropriately (soft-delete schedules, prevent hard deletion of doctor with schedules).
- **Concurrent Edits**: System shall handle concurrent edits gracefully (optimistic locking) and inform users of conflicts.

##### 9.4.6.7 Integration Requirements

**Doctor Master Integration**:
- **Data Source**: Schedule management shall consume doctor data from Doctor Master (9.4.5), including Doctor Code, Name, Department, Degree, Speciality, Active Status, and Availability.
- **Real-Time Sync**: Changes to doctor master data (e.g., doctor deactivated, department changed) shall be reflected in schedule management views and validation.
- **Validation Dependency**: Schedule creation shall validate against current doctor master data (active status, availability).

**Appointment Booking System Integration**:
- **Availability Calculation**: Schedule entries shall directly control appointment availability. Appointment booking system shall:
  - Query schedule data to determine available time slots for selected doctor and date.
  - Calculate available slots based on Start Time, End Time, and Duration (per patient) using the formula defined in 9.4.6.3.1.
  - Consider day of week from schedule (e.g., if booking for Monday, use Monday schedule entries).
  - Block appointment creation outside of scheduled time windows.
  - Respect "Patients Per Day" limit from Doctor Master (if configured).
  - Respect appointment source limits (Web/Mobile) from Doctor Master (if configured).
- **Real-Time Updates**: Changes to schedules (create, update, delete) shall immediately affect appointment availability:
  - When schedule is created: New time slots become available for booking
  - When schedule is updated: Affected time slots are recalculated; existing appointments are validated
  - When schedule is deleted: Time slots are removed; existing appointments are flagged for handling
  - No manual refresh required; appointment booking system queries latest schedule data
- **Appointment Impact Analysis**: When schedules are modified or deleted, appointment booking system shall:
  - Identify all appointments that fall within affected schedule time windows
  - Categorize appointments by status (Confirmed, Pending, Completed, Cancelled)
  - Display impact analysis:
     - Count of affected appointments
     - List of appointments with patient details
     - Revenue impact (if appointments are cancelled)
     - Patient satisfaction impact (if appointments are cancelled)
  - Provide options to handle affected appointments:
     - Cancel appointments automatically
     - Maintain appointments (if schedule is being replaced)
     - Reschedule appointments to alternative time slots
     - Transfer appointments to another doctor
- **Schedule-Based Slot Generation**:
  - Appointment booking system generates available time slots dynamically based on schedules
  - For each schedule entry matching the booking date's day of week:
    - Calculate total available slots (as per 9.4.6.3.1)
    - Subtract already booked appointments
    - Display remaining available slots to user
  - Example: If Monday schedule is 09:00-12:00 with 30-minute duration:
    - Available slots: 09:00, 09:30, 10:00, 10:30, 11:00, 11:30
    - If 09:00 and 10:00 are booked, display: 09:30, 10:30, 11:00, 11:30
- **Multi-Shift Handling**:
  - If doctor has multiple shifts on same day (e.g., Morning and Evening), appointment booking system shall:
    - Display all available time slots from all shifts
    - Group slots by shift for clarity
    - Allow booking across shifts (if business rules permit)

**Call Center Integration**:
- **Leave Notification**: When doctor leave is configured or schedule is deleted with existing appointments, system shall trigger call center workflow:
  - Generate list of affected patients and appointments.
  - Provide patient contact information.
  - Support manual rescheduling workflow.
- **Rescheduling Support**: Call center staff shall be able to reschedule appointments using regular appointment booking process.

**Reporting and Analytics Integration**:
- **Schedule Data Availability**: Schedule data (doctor, day, shift, time, duration) shall be available for operational and clinical reports, such as:
  - Doctor availability reports by day/shift.
  - Appointment capacity analysis.
  - Schedule utilization reports.
  - Department-wise schedule summaries.

##### 9.4.6.8 Schedule Conflict Resolution

**Conflict Detection**:
- **Real-Time Validation**: System shall detect schedule conflicts as user enters schedule data
- **Conflict Types**:
  - **Overlapping Time Slots**: Same doctor, same day, same shift with overlapping time ranges
  - **Adjacent Schedules**: Schedules that are too close together (configurable threshold, e.g., < 15 minutes gap)
  - **Duration Mismatch**: Duration per patient exceeds available time window
  - **Invalid Time Range**: Start Time >= End Time (for same-day schedules)
- **Conflict Detection Algorithm**:
  - For each new/edited schedule entry, check against existing schedules for same doctor
  - Compare: Doctor Code, Day, Shift, Start Time, End Time
  - Detect overlaps: (New Start < Existing End) AND (New End > Existing Start)
  - Flag conflicts with severity level (Error: hard conflict, Warning: soft conflict)

**Conflict Resolution Options**:
- **Prevention (Hard Validation)**:
  - System prevents saving conflicting schedules
  - User must resolve conflict before saving
  - Clear error message with specific conflict details
  - Suggestions for resolution (e.g., "Available time slots: 13:00-17:00")
- **Warning (Soft Validation)**:
  - System warns about conflicts but allows override
  - User can proceed with confirmation
  - Warning message explains potential impact
  - Option to "Ignore Warning and Save" or "Cancel and Fix"
- **Automatic Resolution Suggestions**:
  - System suggests alternative time slots that don't conflict
  - System suggests adjusting times to avoid conflict
  - System suggests changing shift if appropriate
  - User can accept suggestion or manually adjust

**Conflict Resolution Workflow**:
1. System detects conflict during validation
2. System displays conflict details:
   - Conflicting schedule entry (Day, Shift, Time)
   - Type of conflict (overlap, adjacent, etc.)
   - Impact assessment (if appointments exist)
3. User selects resolution option:
   - **Adjust New Schedule**: Modify new schedule to avoid conflict
   - **Adjust Existing Schedule**: Modify existing schedule (if permissions allow)
   - **Delete Conflicting Schedule**: Delete existing schedule (if no appointments)
   - **Override Warning**: Proceed with conflict (if soft validation enabled)
4. System validates resolution
5. If resolved, system allows save; if not, returns to step 2

**Bulk Conflict Resolution**:
- When bulk operations create multiple conflicts:
  - System identifies all conflicts
  - Groups conflicts by type and severity
  - Provides bulk resolution options:
    - Resolve all automatically (where possible)
    - Resolve conflicts individually
    - Skip conflicting entries
- Conflict resolution report:
  - Summary of conflicts detected
  - Resolution status for each conflict
  - Unresolved conflicts requiring manual intervention

##### 9.4.6.9 Access Control and Permissions

**Role-Based Access**:
- **Hospital Admin / Super Admin**:
  - Full access: create, view, update, delete schedules for all doctors.
  - Configure schedule validation rules and thresholds.
  - Override schedule conflicts (if configured).
- **HR / Administrative Staff**:
  - Create and update schedules for assigned doctors or departments.
  - View schedule lists and details.
  - Delete schedules (subject to business rules regarding existing appointments).
- **Scheduling / Front Desk Staff**:
  - View schedule lists and details for scheduling purposes.
  - Limited edit access (e.g., quick availability changes) if permitted by configuration.
  - No access to delete schedules or modify core schedule configuration.
- **Clinical Users (Doctors)**:
  - View own schedule (read-only) where applicable.
  - May request schedule changes through administrative workflow.
  - No direct access to create or modify schedules (unless specifically authorized).
- All operations shall be protected via role-based access control (RBAC) and audited.

##### 9.4.6.9.1 System Actions (Form-Level and List-Level)

**Form View Actions**:
- **Save**: Persist all schedule entries for the selected doctor with validation
  - Validates all schedule entries before saving
  - Checks for conflicts and invalid configurations
  - Displays success message with count of saved entries
  - On error, preserves form data and displays specific error messages
- **Save and Add Another**: Save current doctor's schedule and clear form for next doctor
  - Same validation as Save
  - Clears doctor selection and schedule table
  - Maintains form state for quick entry of multiple doctors
- **Cancel / Back to List**: Discard unsaved changes and return to list view
  - Confirmation dialog if unsaved changes exist
  - Returns to list view with previous filters applied
- **Validate**: Validate current schedule entries without saving
  - Real-time validation as user enters data
  - Manual validation trigger to check all entries
  - Displays validation report (errors, warnings, conflicts)
- **Copy Schedule**: Copy schedules from another doctor
  - Opens doctor selection dialog
  - Shows source doctor's schedules
  - Option to select which schedules to copy
  - Applies copied schedules to current doctor (with conflict checking)
- **Apply Template**: Apply a schedule template
  - Opens template selection dialog
  - Shows available templates
  - Option to modify template before applying
  - Applies template schedules to current doctor
- **Export Schedule**: Export current doctor's schedule to CSV/Excel
  - Generates file with schedule data
  - Includes doctor information and all schedule entries
- **View Schedule History**: View audit trail of schedule changes
  - Opens history view showing all changes
  - Shows who made changes, when, and what changed
  - Option to restore previous schedule version (if permissions allow)

**List View Actions**:
- **Add New Schedule**: Create schedule for a new doctor
  - Opens schedule form in create mode
  - Clears form for new entry
- **Edit Schedule**: Edit schedule for selected doctor(s)
  - Single doctor: Opens schedule form in edit mode
  - Multiple doctors: Opens bulk edit dialog
- **View Schedule**: View schedule in read-only mode
  - Opens schedule detail view
  - No editing capabilities
- **Copy Schedule**: Copy schedules from one doctor to another
  - Opens copy dialog with source and target doctor selection
  - Option to copy all or selected schedules
- **Delete Schedule**: Delete schedule entries
  - Single doctor: Delete all schedules for selected doctor
  - Multiple doctors: Bulk delete with validation
  - Confirmation dialog with appointment impact analysis
- **Bulk Operations**: 
  - **Bulk Create**: Apply same schedule pattern to multiple doctors
  - **Bulk Update**: Update multiple schedule entries with common changes
  - **Bulk Delete**: Delete schedules for multiple doctors
  - **Bulk Export**: Export schedules for multiple doctors
- **Export List**: Export schedule list to CSV/Excel
  - Exports current view with applied filters
  - Includes all visible columns
  - Option to export all records or current page only
- **Print List**: Print schedule list
  - Generates print-friendly view
  - Includes current filters and columns
  - Option to include schedule details or summary only
- **Refresh**: Reload schedule list
  - Refreshes data from server
  - Maintains current filters and pagination
- **Advanced Filters**: Open advanced filter dialog
  - Additional filter options
  - Save filter presets
  - Clear all filters

##### 9.4.6.9.2 Common Workflows

**Workflow 1: Creating Schedule for New Doctor**:
1. User navigates to Schedule Management list view
2. Clicks "Add New Schedule" button
3. System displays schedule form with empty doctor selection
4. User selects Doctor Code from dropdown (auto-suggest)
5. System auto-populates Doctor Name, Department, Degree, Speciality
6. User clicks "Add New Schedule" in schedule table
7. User selects Day, Shift, enters Start Time, End Time, Duration
8. System calculates and displays Available Slots in real-time
9. User adds additional schedule entries as needed
10. User clicks "Save"
11. System validates all schedule entries
12. If valid, system saves schedules and displays success message
13. If invalid, system displays errors and user corrects them
14. User can continue adding schedules or return to list

**Workflow 2: Updating Existing Schedule**:
1. User navigates to Schedule Management list view
2. User searches/filters to find doctor
3. User clicks "Edit" action for the doctor
4. System opens schedule form with existing schedule entries
5. User modifies schedule entry (e.g., changes time or duration)
6. System validates changes in real-time
7. User clicks "Save"
8. System validates all entries (including modified ones)
9. System checks for conflicts with existing appointments
10. If appointments affected, system shows impact analysis
11. User confirms or cancels save operation
12. System saves changes and updates appointment availability

**Workflow 3: Handling Doctor Leave**:
1. User identifies doctor going on leave (or doctor requests leave)
2. User navigates to doctor's schedule
3. User selects "Mark Leave" or "Suspend Schedule" option
4. System prompts for leave date(s) or date range
5. System identifies all affected appointments
6. System displays appointment impact analysis:
   - Count of affected appointments
   - List of patients with contact information
   - Appointment dates and times
7. User selects action:
   - Cancel all appointments
   - Cancel only future appointments
   - Maintain appointments (if covered by another doctor)
8. If cancelling, system generates call center task list
9. System sends notification to call center system
10. Call center staff contacts patients
11. Call center staff reschedules appointments (if patient agrees)
12. System updates appointment records
13. System sends confirmation to patients
14. Schedule remains in system but appointments blocked for leave dates

**Workflow 4: Copying Schedule from One Doctor to Another**:
1. User navigates to source doctor's schedule
2. User clicks "Copy Schedule" action
3. System displays copy dialog with source doctor's schedules listed
4. User selects which schedule entries to copy (or "Copy All")
5. User selects target doctor(s) (single or multiple)
6. System validates that target doctor(s) can accept copied schedules
7. System checks for conflicts in target doctor schedules
8. If conflicts found, system displays conflict report
9. User resolves conflicts or adjusts copied schedules
10. User confirms copy operation
11. System applies copied schedules to target doctor(s)
12. System displays success report (successful copies, conflicts, errors)
13. User can view updated schedules for target doctors

**Workflow 5: Bulk Schedule Creation for Department**:
1. User navigates to Schedule Management list view
2. User applies filter: Department = "Cardiology"
3. User selects multiple doctors (or "Select All" on current page)
4. User clicks "Bulk Create Schedule"
5. System displays bulk schedule creation dialog
6. User selects schedule template or defines schedule pattern:
   - Days: Monday-Friday
   - Shift: Morning
   - Start Time: 09:00
   - End Time: 12:00
   - Duration: 30 minutes
7. System previews schedule for all selected doctors
8. User reviews preview and makes adjustments if needed
9. User confirms bulk creation
10. System validates schedules for each doctor individually
11. System creates schedules for all valid doctors
12. System displays report:
    - Successful: [X] doctors
    - Failed: [Y] doctors (with reasons)
    - Conflicts: [Z] doctors (with details)
13. User can retry failed operations or resolve conflicts

##### 9.4.6.10 Functional Requirements

**Schedule Management**:
- The system shall allow authorized users to **create**, **view**, **update**, and **delete** schedule entries for doctors.
- The system shall **auto-populate** doctor information (Name, Department, Degree, Speciality) when Doctor Code is selected.
- The system shall **validate** schedule entries to prevent overlapping schedules for the same doctor, day, and shift combination.
- The system shall **prevent saving** invalid schedules (e.g., End Time ≤ Start Time, invalid time format, zero or negative duration).
- The system shall **warn** users when schedule configurations are unusual (very short/long duration, potential conflicts).
- The system shall **prevent deletion** of schedules with existing or future appointments unless appointments are handled appropriately.

**List and Search**:
- The system shall support **search** and **filter** operations on the schedule list (by Doctor Code, Name, Department, Day, Shift).
- The system shall support **configurable pagination** (default: 10 records per page).
- The system shall support **inline editing** in list view for quick schedule modifications.
- The system shall **sort** schedule list by Doctor Name, Department, Day, or Shift.

**Integration and Availability**:
- The system shall **integrate** with appointment booking system to control appointment availability based on schedules.
- The system shall **update appointment availability** in real-time when schedules are created, modified, or deleted.
- The system shall **identify affected appointments** when schedules are modified or deleted and provide workflow for handling them.

##### 9.4.6.11 Non-Functional Requirements

**Performance**:
- Schedule form load (for create or edit) should complete within **2 seconds** under normal load.
- Schedule list view (with default filters and pagination) should load within **3 seconds** for up to **1,000** schedule records.
- Schedule validation (overlap detection, time validation) should complete within **1 second** for typical schedule volumes.
- Appointment availability calculation based on schedules should complete within **1 second** for real-time booking queries.

**Usability**:
- The UI for schedule management shall be **responsive** and usable on standard desktop and tablet resolutions.
- All schedule-related texts, labels, and validation messages shall support **localization**, consistent with the rest of the hospital module.
- Auto-suggest dropdowns shall provide **search/filter** capabilities for efficient doctor selection.
- Time pickers shall use **consistent format** (24-hour format recommended) across the system.

**Data Integrity**:
- The solution shall handle **concurrent edits** gracefully using optimistic locking or equivalent mechanisms and clearly inform users of conflicts.
- Schedule data shall maintain **referential integrity** with doctor master data.
- System shall prevent **orphaned schedules** (schedules without valid doctor reference).

**Scalability**:
- System shall support **efficient querying** of schedules for appointment availability calculations (consider indexing on Doctor Code, Day, Shift, Start Time, End Time).
- System shall handle **bulk schedule operations** (e.g., updating schedules for multiple doctors) without performance degradation.

##### 9.4.6.12 Audit and Logging Requirements

**Audit Trail**:
- The system shall maintain an **audit trail** for all schedule changes, including:
  - Creation, updates, and deletion of schedule entries.
  - Changes to schedule fields (Day, Shift, Start Time, End Time, Duration).
  - Schedule deletion with appointment impact.
- Each audited event shall capture:
  - Who performed the action (user ID / role).
  - When the action occurred (timestamp).
  - What was changed (field-level before/after values for schedule entries).
  - Impact on appointments (if applicable).
- Audit data shall be queryable by authorized roles (e.g., compliance, administrators) for a configurable retention period.

**Logging**:
- System shall log **validation errors** and **schedule conflicts** for troubleshooting and analysis.
- System shall log **integration events** (e.g., appointment availability updates triggered by schedule changes).
- System shall log **performance metrics** (e.g., schedule validation time, list load time) for monitoring and optimization.

---

#### 9.4.7 Doctor Module – Doctor Leave Management

**Description**:  
This subsection defines the **Doctor Leave Management** capability within the Doctor Module. It provides a structured way to capture, approve, and manage doctors’ planned and emergency leave, and to automatically handle affected appointments and availability in coordination with Doctor Scheduling and Appointment Booking.

**Purpose**:  
To manage doctors’ planned and emergency leave with clear approval workflows, prevent conflicting leave configurations, and ensure that appointments are safely cancelled or rescheduled and patients are notified in a timely manner.

**Features**:
- Apply for leave (planned and emergency) on behalf of a doctor.
- Maintain a complete history of leave applications and statuses.
- Automatically mark doctor as unavailable for approved leave periods.
- Automatic identification and handling (cancellation/rescheduling) of appointments during leave.
- Filter, search, and export leave records by doctor, department, date range, and leave type.
- Support for emergency leave with simplified or bypassed approval, where permitted.
- Audit trail of leave approvals, rejections, cancellations, and changes.

**Data Fields – Overview**  
Doctor leave data is organized into logical groups:
- **Doctor Identification**
- **Leave Details**
- **Workflow & Status**
- **Audit & System Fields**

##### 9.4.7.1 Data Fields

**Doctor Identification**:
- **Doctor ID / Doctor Code**
  - **Type**: Dropdown / Auto-suggest (lookup from Doctor Master; see 9.4.5)
  - **Required**: Yes
  - **Description**: Unique doctor reference used for leave assignment.
  - **Validation / Rules**:
    - Must reference an **Active** and non-deleted doctor.
    - Only doctors with valid registration in Doctor Master may be selected.
- **Doctor Name**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes (auto-populated)
  - **Description**: Doctor’s full name.
  - **Validation / Rules**:
    - Auto-filled from Doctor Master when Doctor Code is selected.
    - Not directly editable in the leave form.
- **Doctor Department**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes (auto-populated)
  - **Description**: Doctor’s primary department.
  - **Validation / Rules**:
    - Auto-filled from Doctor Master when Doctor Code is selected.

**Leave Details**:
- **Leave ID**
  - **Type**: Auto-generated
  - **Required**: Yes
  - **Description**: Unique identifier for each leave application.
  - **Validation / Rules**:
    - System-generated; not editable in UI.
    - Must be globally unique.
- **Leave Type**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: High-level type of leave.
  - **Allowed Values (Initial Scope)**: `Planned`, `Emergency`
  - **Future Extensions**: Sick, Vacation, Casual, Maternity, etc.
- **Leave Start Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Start date of the leave.
  - **Validation / Rules**:
    - Valid calendar date.
    - For **Planned** leave: Start Date must be **≥ Today** (or ≥ Today + configurable lead time).
    - For **Emergency** leave: Start Date may be **Today**, but not earlier than Today.
- **Leave End Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: End date of the leave (inclusive).
  - **Validation / Rules**:
    - Valid calendar date.
    - Must be **≥ Leave Start Date**.
    - May support long leave periods (weeks/months), subject to maximum duration rules.
- **Create Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Date the leave application is created in the system.
  - **Validation / Rules**:
    - Defaults to current system date on creation.
    - Read-only after creation.
- **Reason / Cause**
  - **Type**: Textarea
  - **Required**: No
  - **Description**: Reason for leave.
  - **Validation / Rules**:
    - Maximum length 500 characters.
- **Attachments** (optional, future enhancement)
  - **Type**: File upload
  - **Required**: No
  - **Description**: Supporting documents (e.g., medical certificate).
- **Replacement Doctor** (optional)
  - **Type**: Dropdown (lookup from Doctor Master)
  - **Required**: No
  - **Description**: Doctor who may cover appointments during the leave.
  - **Validation / Rules**:
    - Must reference an Active doctor.
    - Should not be the same as the leave doctor.

**Workflow & Status**:
- **Leave Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Current status of the leave application.
  - **Allowed Values**: `Draft`, `Pending Approval`, `Approved`, `Rejected`, `Cancelled`, `Completed`
  - **Behavior**:
    - New applications default to `Draft` or `Pending Approval` (configurable).
    - Only `Approved` leaves affect doctor availability and appointments.
    - `Completed` indicates the leave period has passed.
- **Approval Required**
  - **Type**: Boolean / Derived
  - **Required**: Yes
  - **Description**: Indicates whether leave needs approval.
  - **Rules**:
    - Planned leave: Approval required.
    - Emergency leave: May be auto-approved or require post-facto approval, based on configuration.
- **Approver ID / Approver User**
  - **Type**: Dropdown / Lookup (User master / Roles)
  - **Required**: No (required when status becomes `Approved` or `Rejected`)
  - **Description**: User who approved or rejected the leave.
- **Approver Name**
  - **Type**: Text (auto-filled)
  - **Required**: No
  - **Description**: Display name of approver.
- **Approval Date & Time**
  - **Type**: DateTime
  - **Required**: No
  - **Description**: Timestamp when leave was approved or rejected.
- **Rejection Reason**
  - **Type**: Textarea
  - **Required**: When status = `Rejected`
  - **Description**: Reason for rejecting leave.

**Audit & System Fields**:
- **Created By**
  - **Type**: Text / User reference
  - **Required**: Yes
  - **Description**: User who created the leave record.
- **Created At**
  - **Type**: DateTime
  - **Required**: Yes
  - **Description**: Timestamp when the leave record was created.
- **Updated By**
  - **Type**: Text / User reference
  - **Required**: No
  - **Description**: Last user who modified the record.
- **Updated At**
  - **Type**: DateTime
  - **Required**: No
  - **Description**: Last modification timestamp.

##### 9.4.7.2 User Interface Requirements

**Form View – Leave Application**:
- **Doctor Selection Section**:
  - Doctor Code (auto-suggest dropdown) with search by code/name/department.
  - Auto-filled Doctor Name and Department (read-only).
- **Leave Details Section**:
  - Leave Type (Planned / Emergency).
  - Leave Start Date (date picker).
  - Leave End Date (date picker).
  - Create Date (read-only, defaulted to current date).
  - Reason / Cause (textarea).
  - Replacement Doctor (optional dropdown).
  - Attachments (optional; if implemented).
- **Workflow & Status Section**:
  - Leave Status (read-only for non-admin roles; editable via actions).
  - Approval details (Approver, Approval Date/Time, Rejection Reason) visible only to authorized roles and for non-Draft statuses.
- **Actions**:
  - **Save as Draft** (where applicable).
  - **Submit for Approval** (moves to Pending Approval).
  - **Approve / Reject** (for approver roles only).
  - **Cancel Leave** (for Approved or Pending leaves, subject to rules).
  - **Back to List** / **Cancel** (discard unsaved changes).
- **Validation UX**:
  - Inline field-level errors for invalid dates, missing required fields.
  - Summary of errors at top of form.
  - Clear messaging for past dates and invalid ranges.

**List View – Leave Records**:
- **Columns (Minimum)**:
  - Leave ID
  - Doctor Code
  - Doctor Name
  - Department
  - Leave Type
  - Leave Start Date
  - Leave End Date
  - Duration (calculated, in days)
  - Leave Status
  - Approver Name
  - Affected Appointments (count, if available)
  - Created Date
- **Filters**:
  - Doctor (Code/Name search).
  - Department.
  - Leave Type.
  - Leave Status.
  - Date Range (by Leave Start/End Date).
  - “On Leave Today” and “Upcoming Leaves” quick filters.
- **Row Actions**:
  - View / Edit.
  - Approve / Reject (for Pending records, approver roles only).
  - Cancel Leave (where allowed by business rules).
  - View Affected Appointments.
- **Export & Pagination**:
  - Export filtered results (e.g., CSV/Excel), including key leave fields and counts of affected appointments.
  - Standard pagination controls (page size, next/previous).

##### 9.4.7.3 Business Rules

- **Date Validations**:
  - Leave Start Date and End Date must not be empty.
  - End Date must be **≥ Start Date**.
  - **Planned Leave**:
    - Start Date must be **≥ Today** (or ≥ Today + configurable lead time).
  - **Emergency Leave**:
    - Start Date must be **≥ Today**; effective leave may start immediately upon approval or auto-approval.
- **Overlapping Leave**:
  - System shall prevent creating an **Approved** or **Pending Approval** leave that overlaps with another `Approved` leave for the same doctor.
  - Configuration may allow overlaps with warning (e.g., extending an existing leave); in this case, system shall:
    - Prompt user with overlap details.
    - Require elevated permission or explicit confirmation to proceed.
- **Leave Status Transitions**:
  - Allowed transitions (examples):
    - `Draft` → `Pending Approval`
    - `Pending Approval` → `Approved` / `Rejected`
    - `Approved` → `Cancelled` (prior to or during leave period)
    - `Approved` → `Completed` (system-driven when End Date < Today)
  - Editing core fields (Start/End Date, Leave Type) after **Approved** shall:
    - Either be blocked, or
    - Require re-approval, reverting status back to `Pending Approval`.
- **Appointment Impact (High Level)**:
  - Only `Approved` leave affects appointments and availability.
  - When leave is approved:
    - System shall identify all appointments for the doctor between Start Date and End Date (inclusive).
    - System shall mark the doctor as **Not Available** for those dates for schedule and appointment availability calculations.
- **Leave Cancellation**:
  - Cancelling an `Approved` leave:
    - Shall not automatically re-create cancelled appointments.
    - Shall allow new appointments to be booked for dates after cancellation, based on normal schedule rules.
  - System shall log cancellation reason and user.

##### 9.4.7.4 Appointment and Scheduling Integration

- **Schedule Integration**:
  - Doctor Leave Management shall integrate with Doctor Scheduling (see 9.4.6) to:
    - Block appointments on dates within the leave period without deleting the underlying schedule configuration.
    - Temporarily mark schedule entries as inactive or “on leave” for the leave period.
    - Automatically restore normal availability after the leave period ends.
- **Appointment Cancellation / Rescheduling**:
  - Upon leave approval, the system shall:
    - Identify all appointments that fall within the leave date range and scheduled working windows.
    - Present a summary to authorized staff:
      - Total number of affected appointments.
      - List of patients with contact information, appointment date/time, and status.
    - Support the following handling options (per configuration and user role):
      - Cancel all affected appointments.
      - Cancel only future-dated appointments (leave past completed appointments untouched).
      - Mark appointments as “Needs Reschedule” without immediate cancellation.
  - Rescheduling shall follow the standard appointment booking workflow:
    - Staff contact patients (e.g., via call center).
    - Offer alternative slots with same doctor on different dates or different doctor in same department.
    - Book new appointment and link original appointment as “Cancelled – Rescheduled”.
- **Notification & Call Center**:
  - For cancelled or reschedule-required appointments due to leave:
    - System shall create a notification or task list for call center / scheduling staff, aligned with existing schedule leave workflows (see 9.4.6.3 and 9.4.6.7).
    - System shall attempt to send SMS/Email notifications to patients if configured.

##### 9.4.7.5 Edge Cases & Exceptions

- **Leave During Holidays**:
  - If holidays are configured in the system, configuration shall determine whether:
    - Leave days overlapping holidays are counted in duration, and
    - Appointments on holidays are already blocked, so additional leave has no practical impact.
- **Very Long Leave Periods**:
  - System shall support long leave periods (e.g., months), subject to:
    - Configurable maximum duration.
    - Potential requirement for higher-level approval for extended leaves.
- **Emergency Leave Requests**:
  - Emergency leave may bypass standard approval or be auto-approved, based on configuration.
  - System shall still:
    - Log approver or auto-approval rule.
    - Execute appointment impact and notification workflows.
- **Partial-Day Leave (Future Enhancement)**:
  - Initial implementation may treat leave as full-day only.
  - Future versions may support partial-day leave (e.g., half-day, specific time windows) integrated with schedule time slots.
- **Retroactive Documentation**:
  - System may allow recording leave after the fact for documentation purposes (e.g., backdated records), but:
    - Effective availability and appointment handling shall not be retroactively changed for dates in the past.
    - Such records shall be clearly marked as “Recorded Retroactively”.

##### 9.4.7.6 Error Handling and Notifications

- **Past Date Entry**:
  - If user attempts to create Planned leave with Start Date in the past:
    - System shall block the operation and display a clear message (e.g., “Leave Start Date cannot be in the past for planned leave.”).
- **Invalid Date Range**:
  - If End Date < Start Date:
    - System shall block save and show field-level error plus summary message.
- **Approval Workflow Failures**:
  - If approval service or workflow engine fails:
    - System shall keep leave in its previous status.
    - Show user-friendly error message and log technical details for support.
- **Appointment Cancellation / Reschedule Failures**:
  - If cancellation or reschedule API calls fail:
    - System shall log the failure with details of affected appointments.
    - Flag the leave record with a warning status (e.g., “Appointment Handling Incomplete”) for manual follow-up.
- **Notification Failures**:
  - If SMS/Email notifications fail:
    - System shall attempt configured retries.
    - On repeated failure, mark notifications as failed and surface this in the UI (e.g., badge or status in leave/appointment views).
    - Provide export or list of patients who could not be notified for manual contact.

##### 9.4.7.7 Access Control and Roles

- **Doctor / Self-Service (if enabled)**:
  - May create Draft / Pending leave requests for themselves.
  - May view their own leave history.
  - May cancel own Pending leave (before approval) where permitted.
- **HR / Administrative Staff**:
  - Create and manage leave on behalf of doctors.
  - View and filter leave records across departments (as per permissions).
  - Initiate appointment impact workflows and coordinate with call center.
- **Approver Roles (e.g., Medical Admin, Department Head)**:
  - View Pending Approval leaves for their scope (department, hospital).
  - Approve, reject, or request changes to leave applications.
  - Override certain validations (e.g., overlapping leave) if configuration allows.
- **Call Center / Scheduling Staff**:
  - View list of affected appointments due to approved leaves.
  - Perform rescheduling and document outcomes.
- All operations shall be governed by role-based access control (RBAC) and recorded in the audit trail.

##### 9.4.7.8 Functional and Non-Functional Requirements (Summary)

- **Functional**:
  - The system shall allow authorized users to **create**, **view**, **update**, **approve/reject**, **cancel**, and **export** doctor leave records.
  - The system shall enforce validation for date ranges, overlapping leaves (per configuration), and status transitions.
  - The system shall integrate with Scheduling and Appointment Booking to:
    - Block availability during approved leave.
    - Identify and handle affected appointments.
    - Trigger notification workflows.
- **Non-Functional**:
  - Leave list view (with typical filters) should load within **3 seconds** for typical data volumes.
  - Leave impact analysis for appointments should complete within **2 seconds** for normal workloads.
  - All messages shall be human-readable, localized (where localization is supported), and consistent with other Doctor Module messaging.
  - Audit and logging shall follow the patterns used in Doctor Registration and Doctor Scheduling, including who, what, when, and impact on appointments.

---

#### 9.4.9 Doctor Module – Doctor Instructions for Hospital

**Description**:  
This subsection defines the **Doctor Instructions for Hospital** capability within the Doctor Module. It allows doctors to create and manage treatment, care, and operational instructions addressed to hospital staff (e.g., nursing, pharmacy, nutrition, ward staff) for specific patients or for departments/wards.

**Purpose**:  
To provide a structured, auditable mechanism for doctors to issue patient-specific or general care instructions, ensure that relevant staff can easily view and act on them, and maintain a complete history for clinical, operational, and compliance purposes.

**Features**:
- Issue **patient-specific** instructions (e.g., medication, diet, nursing care).
- Issue **department-, ward-, or unit-wise** general instructions (where no specific patient is referenced).
- Maintain a full **instruction history** with audit trail (versions, who changed what and when).
- Filter, view, edit, and manage instruction status (Active/Inactive).
- Support **instruction priorities** (Normal, Urgent, Critical) and escalation/flagging for urgent attention.
- Provide instruction templates for quick entry of common instructions.
- Expose instructions in patient and ward views for relevant staff based on roles.

**Data Fields – Overview**  
Instruction data is organized into logical groups:
- **Doctor Identification**
- **Target & Context (Patient / Department / Ward)**
- **Instruction Content & Classification**
- **Status, Priority & Lifecycle**
- **Audit & System Fields**

##### 9.4.9.1 Data Fields

**Doctor Identification**:
- **Doctor ID**
  - **Type**: Auto-filled (from Doctor Master)
  - **Required**: Yes
  - **Description**: System-generated unique identifier for the doctor issuing the instruction.
  - **Validation / Rules**:
    - Auto-populated when Doctor Code or logged-in doctor context is available.
    - Not editable in the instruction form.
- **Doctor Code**
  - **Type**: Auto-filled (from Doctor Master)
  - **Required**: Yes
  - **Description**: Reference code for the doctor.
  - **Validation / Rules**:
    - Must reference an **Active** and non-deleted doctor.
    - Auto-filled from login context for doctor users, or selected via lookup for admin users.
- **Doctor Name**
  - **Type**: Text (auto-filled, read-only)
  - **Required**: Yes
  - **Description**: Doctor’s full name.
  - **Validation / Rules**:
    - Auto-filled from Doctor Master.
    - Not editable in the instruction form.

**Target & Context**:
- **Instruction Scope**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the instruction is patient-specific or general.
  - **Allowed Values**: `Patient-Specific`, `Department`, `Ward/Unit`, `Hospital-Wide` (future/optional)
- **Patient Identifier** (for Patient-Specific instructions)
  - **Type**: Lookup (e.g., Patient ID / MRN)
  - **Required**: Yes when Instruction Scope = `Patient-Specific`
  - **Description**: Identifier of the patient to whom the instruction applies.
  - **Validation / Rules**:
    - Must reference an existing, valid patient record in the EHR/patient master.
    - System shall show error if patient is not found.
- **Patient Name** (for Patient-Specific instructions)
  - **Type**: Text (auto-filled, read-only)
  - **Required**: No (auto-populated)
  - **Description**: Patient’s full name for display.
- **Department / Service** (for Department or Hospital-Wide instructions)
  - **Type**: Dropdown (from Department master)
  - **Required**: Yes when Instruction Scope = `Department`
  - **Description**: Department to which the instruction applies.
- **Ward / Unit**
  - **Type**: Dropdown (from Ward/Unit master if available)
  - **Required**: Yes when Instruction Scope = `Ward/Unit`
  - **Description**: Ward, unit, or location where the instruction is to be followed.

**Instruction Content & Classification**:
- **Instruction ID**
  - **Type**: Auto-generated
  - **Required**: Yes
  - **Description**: Unique identifier for each instruction record.
  - **Validation / Rules**:
    - System-generated; not editable from UI.
    - Must be globally unique.
- **Instruction Text**
  - **Type**: Textarea
  - **Required**: Yes
  - **Description**: Instruction content provided by the doctor.
  - **Validation / Rules**:
    - Required; cannot be blank.
    - Maximum length **1000 characters** (configurable).
    - UI shall display remaining character count as user types.
- **Instruction Type**
  - **Type**: Enum
  - **Required**: No (recommended)
  - **Description**: Category of instruction.
  - **Examples**: `Medication`, `Diet`, `Nursing Care`, `Monitoring`, `Activity / Mobility`, `Other`.
- **Priority**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Priority level to indicate urgency.
  - **Allowed Values**: `Normal`, `Urgent`, `Critical`
  - **Rules**:
    - `Urgent` and `Critical` may trigger additional notifications/escalations.
- **Instruction Date & Time**
  - **Type**: DateTime
  - **Required**: Yes
  - **Description**: Timestamp when the instruction is issued.
  - **Validation / Rules**:
    - Defaults to current system date/time on creation.
    - May allow backdated entries by authorized users with justification (e.g., emergency situations or documentation delay).

**Status, Priority & Lifecycle**:
- **Instruction Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether the instruction is active and actionable.
  - **Allowed Values**: `Active`, `Inactive` (soft-deleted), `Superseded`
  - **Rules**:
    - Instructions cannot be hard-deleted; only status can change to `Inactive` or `Superseded`.
    - `Superseded` may be used when a newer instruction replaces an older one.
- **Acknowledgement / Completion Status** (optional)
  - **Type**: Enum
  - **Required**: No (required where acknowledgement workflows are enabled)
  - **Description**: Tracks whether the instruction has been acknowledged and/or completed by responsible staff.
  - **Allowed Values (initial suggestion)**: `Not Acknowledged`, `Acknowledged`, `Completed`, `Not Applicable`
  - **Rules**:
    - **Acknowledged** indicates that responsible staff have seen and accepted the instruction.
    - **Completed** indicates that the instruction has been fully carried out (where applicable).
    - Updates should be driven by nursing/ward workflows, not by doctors directly, and fully audited.
- **Effective From / Until** (optional)
  - **Type**: Date/DateTime
  - **Required**: No
  - **Description**: Validity window for the instruction, if applicable (e.g., “for next 3 days”).

**Audit & System Fields**:
- **Created By / Created At**
  - User and timestamp of instruction creation (doctor user or admin).
- **Updated By / Updated At**
  - Last user and timestamp of modification.
- **Version / Revision Number** (optional)
  - **Type**: Integer
  - **Description**: Version counter to support instruction version history.

##### 9.4.9.2 User Interface Requirements

**Form View – Create / Edit Instruction**:
- **Doctor & Context**:
  - If logged in as doctor:
    - Doctor Code, Name auto-filled and read-only.
  - If logged in as admin/other staff:
    - Doctor selection via lookup (Code/Name search).
  - Instruction Scope selector (Patient-Specific, Department, Ward/Unit, etc.).
  - Patient selection control (for Patient-Specific):
    - Search by Patient ID, Name, or MRN.
  - Department / Ward selection controls (for general instructions).
- **Instruction Details**:
  - Instruction Text textarea with character counter and validation.
  - Instruction Type (dropdown).
  - Priority (Normal, Urgent, Critical).
  - Instruction Date & Time (date/time picker; default to now).
- **Status & Lifecycle**:
  - Status field (Active/Inactive), typically auto-set to `Active` on create.
  - For edits, ability (for authorized users) to mark instruction as `Inactive` or create a new version that supersedes the old one.
- **Actions**:
  - **Save** (or **Create Instruction**).
  - **Cancel / Back to List**.
  - Optional: **Save as Template** (if creating a reusable template).
- **Validation UX**:
  - Inline validation messages for required fields, patient existence, and instruction length.
  - Clear warnings when:
    - Editing instructions for discharged patients (if patient is already discharged).
    - Instruction conflicts with previous active instructions (e.g., opposite diet/medication).

**List View – Instruction Management**:
- **Columns (Minimum)**:
  - Instruction ID
  - Date & Time
  - Doctor Name
  - Instruction Scope (Patient / Dept / Ward)
  - Patient ID / Name (where applicable)
  - Department / Ward
  - Instruction Type
  - Priority
  - Instruction Status (Active/Inactive/Superseded)
  - Acknowledgement / Completion Status (where enabled)
- **Filters**:
  - By Doctor (Code/Name).
  - By Patient (ID/Name).
  - By Department / Ward.
  - By Date Range (Instruction Date & Time).
  - By Priority and Status.
- **Row Actions**:
  - View / Edit instruction.
  - Mark Active / Inactive.
  - View version history / change log.
  - Flag/Escalate (mark as urgent or send notification).
- **Export & Pagination**:
  - Export instructions honoring current filters (CSV/Excel).
  - Standard pagination controls.

##### 9.4.9.3 Business Rules

- **Doctor Registration Requirement**:
  - Only registered doctors (from Doctor Master) may be assigned as instruction authors.
- **Patient Validation**:
  - For patient-specific instructions:
    - Patient must exist in the patient master/EHR.
    - System shall block saving if patient cannot be found.
- **Instruction Length**:
  - Enforce maximum instruction length (e.g., 1000 characters).
  - UI shall show remaining characters; prevent saving if limit exceeded.
- **Instruction Lifecycle**:
  - Instructions cannot be permanently deleted from the system.
  - Instructions may be:
    - Marked `Inactive` when no longer applicable.
    - Marked `Superseded` when replaced by a new instruction (with link between versions).
  - All changes shall be captured in audit trail.
- **Acknowledgement & Completion Tracking**:
  - Where enabled, nursing/ward or other responsible staff shall acknowledge and/or mark completion of instructions through dedicated workflows.
  - Acknowledgement and completion timestamps and users shall be captured in the audit trail.
- **Discharged Patients**:
  - When patient is discharged:
    - New instructions may either be blocked or allowed only by authorized roles (configurable).
    - Editing existing instructions shall show a warning; edits may require justification.
- **Conflicting Instructions**:
  - System shall attempt to detect conflicts between new instructions and existing active instructions for the same patient and category (e.g., contradictory diet or medication instructions).
  - On detection:
    - Show a conflict warning with list of potentially conflicting instructions.
    - Allow override with justification (for example, change of clinical plan).
- **Emergency Instructions**:
  - Emergency/critical instructions may bypass some validations (e.g., conflicting instructions), but:
    - Must be tagged as `Urgent` or `Critical`.
    - Must capture timestamp and justification.

##### 9.4.9.4 Edge Cases, Exceptions, and Error Handling

- **Instruction Too Long**:
  - If user exceeds maximum length:
    - System shall prevent additional input or saving and display a clear message.
- **Non-Existent Patient**:
  - Attempting to save a patient-specific instruction for a non-existent patient shall:
    - Fail validation.
    - Show error and offer search/selection to correct the patient.
- **Multiple Doctors, Conflicting Instructions**:
  - When multiple doctors issue potentially conflicting instructions:
    - All instructions remain visible to staff.
    - System may highlight conflicts in the UI (e.g., warning icon).
    - Conflict resolution is handled via clinical workflow (e.g., team discussion); system records updates and superseded instructions.
- **Instruction Modification & Versioning**:
  - Edits to instruction text after it has been viewed or acted upon:
    - Shall create a new version or store full before/after values in audit.
    - Change log shall show who changed what and when.
- **Soft Delete / Inactivation**:
  - Setting an instruction to `Inactive` or `Superseded`:
    - Shall not remove it from history.
    - Shall remove it from active instruction lists used operationally (e.g., current nursing orders), depending on context.
- **Error Handling**:
  - Instruction save failure (e.g., network, validation):
    - System shall preserve input as draft in memory or temporary storage where feasible.
    - Show user-friendly message and allow retry.
  - Network timeout or temporary backend failure:
    - System may save locally as "Pending Sync" (future enhancement) or prompt user to retry with data preserved.

**Special Scenarios**:
- **Instruction Escalation**:
  - Users may flag instructions as urgent or critical (if not already).
  - System may notify relevant staff (e.g., nurses on the ward) via configured channels.
- **Instruction Templates**:
  - System may provide reusable templates for common instruction patterns (e.g., standard post-op diet, monitoring protocols).
  - Templates shall be editable at time of use.
  - Template management (create/edit/delete) shall be restricted to authorized roles.
- **Bulk Instructions**:
  - For certain scenarios (e.g., ward-level instructions), bulk creation may be supported:
    - Business rules and validations may differ slightly (e.g., no patient ID required).
    - Still fully audited and traceable.

##### 9.4.9.5 Access Control and Visibility

- **Doctors**:
  - Can create, view, and edit their own instructions (subject to lifecycle and configuration).
  - May inactivate or supersede their own instructions where appropriate.
- **Nursing / Ward Staff**:
  - Can view active instructions relevant to their patients, wards, or departments.
  - Cannot modify doctor-authored instruction content, but may acknowledge and completion-mark instructions via dedicated workflows (where enabled).
- **Clinical Admin / Supervisors**:
  - May edit or inactivate instructions in special circumstances (e.g., incorrect patient).
  - May manage templates and configuration.
- **Audit / Compliance Roles**:
  - Can view complete instruction history, including inactive/superseded records and change log.
- All access shall be controlled by RBAC and fully audited.

##### 9.4.9.6 Non-Functional and Integration Considerations

- **Performance**:
  - Instruction list for a patient should load within **2 seconds** under normal load.
  - Search and filter operations across instructions should complete within **3 seconds** for typical volumes.
- **Integration**:
  - Instructions shall be visible in:
    - Patient chart / EHR views (for patient-specific instructions).
    - Ward/department dashboards (for general instructions).
  - Future integration with notification/alert systems (e.g., to page/notify nursing staff for Critical instructions).
  - **Localization & Usability**:
  - All labels, messages, and validation texts shall support localization.
  - UI shall clearly differentiate between active and inactive/superseded instructions (e.g., color, badges, filters).

---

#### 9.4.10 Doctor Module – Doctor Scheduling Report

**Description**:  
This subsection defines the **Doctor Scheduling Report** capability within the Doctor Module, with a particular focus on a **Day-wise Available Doctor List** report. It provides operational and management users with consolidated visibility into which doctors are available, their schedules, and actual appointment load over a given date range.

**Purpose**:  
To enable hospital operations, front desk, and management teams to view day-wise doctor availability and appointment counts for planning, capacity management, and performance tracking, and to export/print these views for operational use.

**Features**:
- Generate a **Day-wise Available Doctor List** over a selected date range.
- View daily doctor schedules and appointment counts in a single consolidated report.
- Filter the report by multiple dimensions (date range, branch, unit, department, doctor, type, patient type, etc.).
- Print report data in a print-friendly layout.
- Export report data in standard formats (PDF, Excel, CSV), with safeguards for large data volumes.
- Optionally support scheduled/background generation for large reports and email delivery.

**Report Types (Initial Scope)**:
- **Day-wise Available Doctor List**:
  - For each date in the selected range, list doctors who are available for appointments along with basic profile and appointment count.

##### 9.4.10.1 Input Parameters and Filters

**Core Date Range Parameters (New Report – Day-wise Available Doctor List)**:
- **From Date**
  - **Type**: Date
  - **Required**: Yes (for core report)
  - **Description**: Start date for the report.
  - **Validation / Rules**:
    - Must be a valid calendar date.
- **To Date**
  - **Type**: Date
  - **Required**: Yes (for core report)
  - **Description**: End date for the report.
  - **Validation / Rules**:
    - Must be a valid calendar date.
    - Must be **≥ From Date**.
    - System may enforce a maximum allowed range (e.g., 31 days for interactive reports), with longer ranges requiring scheduled/background generation.

**Report Filters (Extended)**:
- **Branch**
  - **Type**: Dropdown
  - **Required**: No
  - **Description**: Branch / location selection where multiple branches exist.
  - **Rules**: Optional; when specified, limits results to selected branch.
- **Unit**
  - **Type**: Dropdown
  - **Required**: No
  - **Description**: Unit or sub-location filter (e.g., building, floor, service unit).
- **From Date (Filter)**
  - **Type**: Date
  - **Required**: No (for broader reporting UI)
  - **Description**: Optional override for date range (if defaults or presets exist).
- **To Date (Filter)**
  - **Type**: Date
  - **Required**: No
  - **Description**: Optional override for date range.
- **Department**
  - **Type**: Dropdown (from Department master)
  - **Required**: No
  - **Description**: Filter to include only doctors from selected department(s).
- **Patient Type**
  - **Type**: Enum
  - **Required**: No
  - **Description**: Filter appointments and counts by patient type.
  - **Allowed Values**: `OPD`, `IPD`, `Emergency` (configurable).
- **Doctor Code**
  - **Type**: Text / Auto-suggest
  - **Required**: No
  - **Description**: Filter report for a specific doctor code or set of codes.
- **Doctor Type**
  - **Type**: Enum
  - **Required**: No
  - **Description**: Filter by employment/engagement type.
  - **Allowed Values**: `Visiting`, `Permanent`, `Consultant` (as per Doctor Master).
- **Appointment Status Filter** (optional enhancement)
  - **Type**: Enum / Multi-select
  - **Description**: Filter to include/exclude certain appointment statuses (e.g., exclude cancelled).

##### 9.4.10.2 Report Output – Columns and Layout

**Day-wise Available Doctor List – Output Columns (Minimum)**:
- **Date**
  - Date for which availability and counts are shown.
- **Doctor Code**
- **Doctor Name**
- **Doctor Degree**
- **Total Appointment Count (on that Date)**
  - Total number of appointments for that doctor on that date.
  - May optionally be broken down by patient type (OPD/IPD/Emergency) or appointment status in extended versions.

**Additional Optional Columns (Future / Configurable)**:
- Department
- Branch / Unit
- Doctor Type
- First Appointment Time / Last Appointment Time
- Number of Available Slots vs Booked Slots (if schedule-slot integration is implemented).

**Report Display**:
- Tabular/grid view of the report data with:
  - Sortable columns (at least by Date, Doctor Code, Doctor Name, Department).
  - Pagination with configurable page size.
- Summary section (above or below table) showing:
  - Total doctors per date.
  - Total appointments per date (aggregated).
  - Optional breakdown by department or patient type.
- Print-friendly layout that:
  - Hides non-essential UI elements.
  - Adjusts table formatting for A4/Letter printing.

##### 9.4.10.3 User Interface Requirements

**Report Form (Filters Panel)**:
- Includes all filter fields defined above:
  - Branch, Unit, Department.
  - Doctor Code, Doctor Type.
  - Patient Type.
  - From Date, To Date.
- **Actions**:
  - **Generate Report**: Fetches and displays report based on selected filters.
  - **Reset Filters**: Clears all filters and restores defaults.
  - **Print**: Opens print-friendly view.
  - **Export**: Allows export in supported formats (e.g., PDF, Excel, CSV).
- **UX Considerations**:
  - Show date range presets (e.g., Today, This Week, This Month, Last 7 Days).
  - Show validation messages inline for invalid date ranges.
  - For large ranges, show a warning banner about performance and suggest narrowing the range.

**Report Display (Results Panel)**:
- Displays grid/table of results with:
  - Pagination and total record count.
  - Indicators when filters are active.
- When **no data** is found:
  - Show a clear message (e.g., “No doctors available for the selected criteria.”).
  - Optionally suggest common presets (e.g., “Try Today or This Week”).
- Show progress indicators / loading states during report generation.

##### 9.4.10.4 Business Rules

- **Date Range Validation**:
  - `To Date` must be **≥ From Date**.
  - System may enforce maximum interactive date range (e.g., 31 days). For larger ranges:
    - Show warning and optionally restrict.
    - Or require the user to generate a scheduled/background report.
- **Active Doctors Only (Default)**:
  - By default, the report should include only doctors with:
    - `Doctor Active Status = Active` (from Doctor Master).
  - Configuration may allow inclusion of inactive doctors via an advanced filter.
- **Availability and Appointment Count Calculation**:
  - A doctor is considered “available” for a given date if:
    - They have valid schedules for that day (per Scheduling; 9.4.6), and
    - They are not on `Approved` leave for that date (per Leave Management; 9.4.7), and
    - Their availability status allows appointments (per Doctor Master, where applicable).
  - **Total Appointment Count** shall be calculated from appointment records:
    - Only include appointments within the report’s date range.
    - Respect filters for Patient Type and Appointment Status (if provided).
  - This definition of “available” is aligned with Appointment Management (9.4.11); a doctor may still appear as available even if all their slots are already booked for a given day (i.e., availability is schedule/leave-based, not remaining-slot-based).

##### 9.4.10.5 Edge Cases, Exceptions, and Error Handling

- **No Doctors Available in Selected Date Range**:
  - Show a clear informational message.
  - Do not treat as an error.
  - Optionally suggest using a shorter or different date range.
- **Large Date Ranges**:
  - If user selects a very large date range:
    - System shall show a warning about potential performance impact.
    - May limit range (e.g., max 3 months) or require background generation.
- **Long-Running Report Generation**:
  - Show progress indicator and allow user to cancel.
  - For very heavy queries, support:
    - Background processing with notification when ready.
    - Download link or email delivery of completed report.
- **Incomplete Doctor Data**:
  - If some doctor metadata (e.g., degree) is missing:
    - Show partial data with clear indication (e.g., “N/A”).
    - Do not fail the entire report.
- **Cancelled Appointments**:
  - Provide configuration or filter to:
    - Include or exclude cancelled appointments from the count.
    - Default behavior should be clearly documented (e.g., exclude cancelled by default).
- **Access and Permissions**:
  - If user lacks permission for Doctor Scheduling Report:
    - Show access denied message and do not expose report data.
- **Technical / Data Errors**:
  - Report generation failure (e.g., timeout, DB error):
    - Show a user-friendly error and allow retry.
    - Log technical details for investigation.
  - Data inconsistency (e.g., orphaned appointments, missing doctor references):
    - Show warnings in the report and proceed with best-effort data.

##### 9.4.10.6 Special Scenarios and Extensions

- **Scheduled Reports**:
  - Allow users (with appropriate roles) to schedule recurring reports:
    - E.g., daily/weekly day-wise available doctor list.
    - Delivery via email or in-system notifications with attached file or link.
- **Custom Report Formats**:
  - Provide configurable column selection and ordering for advanced users.
  - Saved report “views” or presets for common reporting needs (e.g., by department, branch).
- **Comparative Reporting**:
  - Allow users to compare metrics across two date ranges (e.g., last month vs this month).
  - Comparison may include:
    - Average appointments per doctor per day.
    - Changes in availability patterns.
- **Export Limits**:
  - Document and enforce maximum rows/date range for export operations.
  - For exports exceeding limits:
  - Recommend narrower filters or background/scheduled export.

---

#### 9.4.11 Doctor Module – Appointment Management (Patient Appointment)

**Description**:  
This subsection defines the **Appointment Management (Patient Appointment)** capability within the Doctor Module. It covers end-to-end appointment lifecycle management, including booking, editing, cancellation, arrival tracking, payment handling, and integration with doctor schedules, leave, and notifications.

**Purpose**:  
To provide a robust, real-time appointment management system that prevents overbooking, respects doctor schedules and leaves, supports various patient types and booking channels (e.g., hotline, walk-in), and ensures clear status tracking and communication to patients and staff.

**Features**:
- Book new patient appointments with serial number assignment per doctor/day.
- Manage existing appointments (view, edit, cancel, reschedule).
- Track appointment status across the full lifecycle (Created → Confirmed → Arrived → Visited → Completed / Cancelled).
- Display real-time doctor availability using schedule, leave, and availability data.
- Support multiple payment timings (at booking or upon arrival) and payment methods.
- Print appointment slips/tokens and appointment lists.
- Manage patient arrivals and no-show handling.
- Support advanced scenarios: emergency appointments, walk-ins, VIP patients, recurring appointments, waitlists, and transfers.

**Data Fields – Overview**  
Appointment data is organized into logical groups:
- **Header & System Information**
- **Appointment Configuration**
- **Doctor Information (Auto-Filled)**
- **Patient Basic Information**
- **Visit and Appointment Type**
- **Appointment List / Grid Columns**

##### 9.4.11.1 Header & System Information

- **Next SL No**
  - **Type**: Auto-generated
  - **Required**: Yes
  - **Description**: Next available serial number for the selected doctor and date.
  - **Validation / Rules**:
    - System-calculated based on existing appointments for that doctor, shift, and visit date.
    - Not directly editable in normal workflows (unless role permits manual override).
- **Chamber No**
  - **Type**: Auto-filled
  - **Required**: Yes
  - **Description**: Doctor’s chamber/room number.
  - **Validation / Rules**:
    - Auto-filled from doctor master/schedule configuration.
    - Read-only on appointment form.
- **Visit Fee Info**
  - **Type**: Display (read-only)
  - **Required**: Yes
  - **Description**: Summary of First Visit (New) and Old Visit (Returning) fees for the doctor.
  - **Validation / Rules**:
    - Auto-populated from Doctor Master (and/or Department defaults).
    - May show separate values for New vs Returning visits.

##### 9.4.11.2 Appointment Configuration

- **Shift**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Appointment shift, matching doctor schedule configuration.
  - **Allowed Values**: `Morning`, `Evening`, `Night` (configurable).
- **Serial No (SL No)**
  - **Type**: Number
  - **Required**: Yes
  - **Description**: Appointment serial number for the doctor on the selected date/shift.
  - **Validation / Rules**:
    - System may auto-generate based on **Next SL No**.
    - Must be unique per doctor per visit date per shift.
    - Manual override (if allowed) must still respect uniqueness and avoid conflicts.
- **Appointment Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Date when the appointment was created/booked.
  - **Validation / Rules**:
    - Defaults to current date.
    - May allow backdated booking by authorized roles (e.g., data entry of past bookings).
- **Visit Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Planned consultation date.
  - **Validation / Rules**:
    - Must be a valid date.
    - Must be **≥ Appointment Date** (no past visit date for new bookings).
    - Must fall on a day where the doctor is scheduled and available (see Business Rules).
- **Appointment Time / Slot Time** (optional, where time-based slots are used)
  - **Type**: Time
  - **Required**: No (Required where schedule-slot model is implemented)
  - **Description**: Specific time or slot start time for the appointment within the selected shift and Visit Date.
  - **Validation / Rules**:
    - Must align with one of the generated schedule slots for the doctor on that date and shift (see 9.4.6.3.1).
    - Must not conflict with other appointments occupying the same slot (unless override permissions are used).
    - Time format shall be consistent with scheduling (24-hour HH:MM).

##### 9.4.11.3 Doctor Information (Auto-Filled)

- **Doctor Code (Dr Code)**
  - **Type**: Dropdown / Auto-suggest
  - **Required**: Yes
  - **Description**: Unique reference code for the doctor.
  - **Validation / Rules**:
    - Must be selected from Doctor Master (Active, non-deleted).
    - Selection drives auto-filling of related doctor fields and availability panel.
- **Doctor Name**
  - **Type**: Auto-filled text (read-only)
  - **Required**: Yes
  - **Description**: Full name of the doctor.
  - **Validation / Rules**:
    - Auto-filled when Doctor Code is selected.
- **Department**
  - **Type**: Auto-filled text (read-only)
  - **Required**: Yes
  - **Description**: Doctor’s primary department.
  - **Validation / Rules**:
    - Auto-filled from Doctor Master.

##### 9.4.11.4 Patient Basic Information

- **Patient Name**
  - **Type**: Text
  - **Required**: Yes
  - **Description**: Full name of the patient.
  - **Validation / Rules**:
    - Required, non-empty.
    - Maximum length 100 characters.
- **Mobile**
  - **Type**: Text
  - **Required**: Yes
  - **Description**: Primary contact number.
  - **Validation / Rules**:
    - Must follow configured phone format.
    - May be validated for duplicates / existing patients.
- **Gender**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Patient’s gender.
  - **Allowed Values**: `Male`, `Female`, `Other`.
- **Birth Date**
  - **Type**: Date
  - **Required**: No
  - **Description**: Date of birth.
  - **Validation / Rules**:
    - Valid calendar date; not in the future.
- **Age**
  - **Type**: Calculated / Input
  - **Required**: No
  - **Description**: Age in Year/Month/Day.
  - **Validation / Rules**:
    - Auto-calculated from Birth Date when provided.
    - May allow direct entry if Birth Date is unknown.
- **Patient Type**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Type of patient.
  - **Allowed Values**: `OPD`, `IPD`, `Emergency` (configurable).
- **Address**
  - **Type**: Textarea
  - **Required**: No
  - **Description**: Patient’s address.
  - **Validation / Rules**:
    - Maximum length 500 characters.
- **Registered Patient Link (Optional)**:
  - If the patient is already registered in the system:
    - Selection of existing patient auto-fills Patient Name, Mobile, Gender, DOB, Age, Address.
    - Appointment record links to patient master for continuity.

##### 9.4.11.5 Visit and Appointment Type

- **Visit Type**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Indicates whether this is a first visit or a returning visit.
  - **Allowed Values**: `New`, `Old` (Returning).
  - **Rules**:
    - May drive fee selection (New vs Old fee).
    - May be validated against previous visit history (e.g., treat as Old if recent visit exists).
- **Remarks**
  - **Type**: Textarea
  - **Required**: No
  - **Description**: Additional notes relevant to the appointment.
  - **Validation / Rules**:
    - Maximum length 500 characters.
 - **Appointment Status**
   - **Type**: Enum
   - **Required**: Yes
   - **Description**: Current status of the appointment within its lifecycle.
   - **Allowed Values (initial set)**: `Created`, `Confirmed`, `Arrived`, `Visited`, `Completed`, `Cancelled`, `No Show`
   - **Rules**:
     - Status transitions shall follow the lifecycle defined in 9.4.11.8.
     - Certain transitions may require additional data (e.g., cancellation reason) or roles.
- **Channel / Source**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Booking channel or source of the appointment.
  - **Allowed Values (examples)**: `Hotline`, `Front Desk`, `Online`, `Walk-in`, `Referral` (configurable).
  - **Rules**:
    - May drive default status (e.g., Online → Created, Front Desk → Confirmed).
    - Used for reporting and operational routing (e.g., call center vs front desk responsibility).
- **Payment Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Financial state of the appointment.
  - **Allowed Values (examples)**: `Unpaid`, `Partially Paid`, `Paid`, `Refunded`, `Cancelled with Refund`, `Write-off` (configurable).
  - **Rules**:
    - Updated by billing/cashier workflows.
    - Must stay consistent with underlying financial transactions.
- **Financial Amounts (Summary)**
  - **Type**: Decimal(s)
  - **Required**: No
  - **Description**: Key amounts for the appointment (e.g., Visit Fee, Discount, Net Payable, Amount Paid).
  - **Rules**:
    - Visit Fee may depend on doctor’s New/Old fee configuration and Patient Type.
    - Net Payable and Amount Paid shall be derived from billing engine where integrated.

##### 9.4.11.6 Appointment List / Grid – Columns

Minimum appointment list columns:
- Serial No (SL No)
- Patient Name
- Mobile No
- Gender
- Visit Type (New/Old)
- Age
- Appointment Date
- Visit Date
- Appointment Status
- Channel / Appointment By (user/channel; e.g., Hotline, Front Desk, Online)
- Referring Doctor Code (Ref Dr Code)
- Payment Status / Amount (summary)
- **Actions**:
  - Print (appointment slip).
  - Edit.
  - Cancel.
  - Take Payment.
- **Arrival / Visit Status**:
  - Is Arrival (Yes/No).
  - Arrival Info (e.g., time of arrival, checked-in by).

##### 9.4.11.7 User Interface Requirements

**Appointment Form – Booking / Edit View**:
- **Header Section**:
  - Next SL No (display).
  - Chamber No.
  - Visit Fee Info (New/Old fee summary).
- **Doctor Selection Section**:
  - Doctor Code dropdown with search.
  - Auto-filled Doctor Name and Department.
- **Doctor Availability Panel (Right Side)**:
  - Day-wise schedule (from Scheduling; 9.4.6).
  - Available time ranges per shift.
  - Closed days highlighted.
  - Real-time status indications:
    - e.g., “Doctor on leave today”, “Doctor has not arrived yet”.
- **Patient Information Section**:
  - Existing patient lookup (optional).
  - Patient basic information fields (as defined above).
- **Appointment Configuration Section**:
  - Shift selector.
  - Visit Date.
  - Serial No (auto-suggested with option for manual override if permitted).
  - Visit Type, Patient Type, Remarks.
- **Actions**:
  - **Save** (or **Book Appointment**).
  - **Get List** (load appointment list based on filters).
  - **Print** (appointment slip/token).
  - **Cancel / Back to List**.

**Appointment List View**:
- Tabular view with columns listed above.
- Filter bar with:
  - Doctor, Date Range, Patient Type, Appointment Status, Visit Type, Ref Doctor, Channel (optional).
- Row actions:
  - Edit, Cancel, Print, Take Payment, Mark Arrival.
- Export options:
  - Export current filter results (CSV/Excel/PDF).
- Print:
  - Print appointment list for a given filter set (e.g., doctor + day).

##### 9.4.11.8 Appointment Status and Lifecycle

- **Status Flow (High Level)**:
  - `Created` (e.g., Hotline/Online) → `Confirmed` (Hotline/Front Desk) → `Arrived` (Doctor attendant/front desk) → `Visited` (Doctor attendant) → `Completed` (Doctor attendant)
  - At any point before `Completed`, appointment may be transitioned to `Cancelled`.
- **Status Definitions**:
  - **Created**: Appointment data captured but not yet confirmed (optional state, depending on channel).
  - **Confirmed**: Appointment is confirmed for the given slot.
  - **Arrived**: Patient has physically/virtually checked in.
  - **Visited**: Patient has been seen by the doctor.
  - **Completed**: Clinical interaction is complete and appointment closed.
  - **Cancelled**: Appointment no longer active (with reason).
  - **No Show** (derived/explicit status):
    - Appointment was confirmed but patient did not arrive within defined window.

##### 9.4.11.9 Business Rules

- **Doctor Availability and Leave**:
  - Appointment can only be booked if:
    - Doctor has an active schedule for the selected Visit Date and Shift (per 9.4.6).
    - Doctor is not on `Approved` leave for that date (per 9.4.7).
    - Doctor’s availability status allows appointments (per Doctor Master).
- **Serial Number Uniqueness**:
  - Serial No must be unique per doctor, per Visit Date, per Shift.
  - System shall prevent saving appointments that would create a duplicate Serial No.
- **Time and Date Validations**:
  - Visit Date cannot be in the past for new bookings (unless overridden by admin).
  - Appointment Date defaults to today and must not be after Visit Date.
  - Past-date booking or modification may require higher-level permissions.
- **Overbooking Prevention**:
  - System shall prevent booking more appointments than available slots derived from schedule + duration rules (9.4.6.3.1), unless specific override permissions are granted.
  - If appointment time/slot is already full:
    - Show conflict and suggest next available slot(s).
- **Payment Handling**:
  - Payment can be taken either:
    - At booking time, or
    - At arrival/check-in.
  - Appointment record shall reflect payment status (e.g., Unpaid, Partially Paid, Paid).
  - Payment failures shall not silently drop the appointment; appointment may be held in a pending-payment state with expiry rules.
- **Cancellation Rules**:
  - Appointments may be cancelled by authorized roles, with:
    - Mandatory cancellation reason in certain configurations.
    - Notifications to patients (SMS/Email) if configured.
  - Cancellation policy (e.g., free up to 24 hours before; fees after) shall be configurable and may integrate with billing.
- **Multiple Appointments for Same Patient & Doctor**:
  - System shall warn if patient books multiple upcoming appointments with the same doctor.
  - Allow continuation with explicit confirmation.

##### 9.4.11.10 Edge Cases, Exceptions, and Special Scenarios

- **Doctor on Leave at Booking Time**:
  - If doctor is on approved leave for the selected date:
    - System shall block booking.
    - Show clear message and suggest alternative dates or doctors (same department).
- **Slot Already Booked / Conflict Detection**:
  - If selected slot is already at capacity:
    - System shall show conflict details and alternative slots.
- **Patient Does Not Arrive (No Show)**:
  - After configured threshold, system may:
    - Mark appointment as `No Show`.
    - Allow rescheduling or recording policy-based no-show fees.
    - Track no-show history per patient for policy enforcement (e.g., restrictions after 3 no-shows).
- **Doctor Late / Unavailable**:
  - Real-time updates from Doctor Availability / Attendance:
    - If doctor is late, system shall update status and optionally notify waiting patients.
  - If doctor becomes unavailable (e.g., emergency leave), integrated leave workflows apply (9.4.7).
- **Emergency Appointments**:
  - May bypass some normal booking rules (e.g., slot capacity) via admin override.
  - Must be clearly tagged as `Emergency` and fully audited.
- **Walk-in Patients**:
  - Support walk-in workflow where:
    - Appointment is created for immediate or same-day visit.
    - Rules may differ (e.g., no prior booking, different fee logic).
- **VIP Patients**:
  - Configurable priority system where VIP patients:
    - May access reserved slots.
    - May be exempt from certain restrictions (e.g., overbooking limit) under strict control.
- **Same-Day Appointments**:
  - May have special rules:
    - Limited slots.
    - Higher priority or different fee structure.
- **Payment Failures**:
  - Payment gateway failure:
    - Allow retry.
    - Offer alternative payment method (e.g., cash).
    - Hold appointment in temporary state for a limited period.
- **Notification Failures**:
  - SMS/Email failures:
    - Logged for retry.
    - Allow manual notification by staff.

**Special Scenarios**:
- **Appointment Rescheduling**:
  - Allow change of date, time/shift, doctor, or serial number:
    - Maintain full history of changes.
    - Trigger notifications to doctor and patient.
- **Appointment Cancellation & Refunds**:
  - Integrate with billing/refund policies:
    - Handle full/partial refunds where applicable.
    - Release slot back to availability.
- **Recurring Appointments**:
  - Support series booking (e.g., physiotherapy sessions):
    - Automatically schedule multiple appointments based on pattern.
    - Provide clear visibility and ability to modify/cancel part of series.
- **Appointment Reminders**:
  - Send reminders at configurable intervals (e.g., 24h, 2h before visit).
  - Track reminder delivery status.
- **Waitlist Management**:
  - When slots are full:
    - Allow patients to be added to a waitlist.
    - Notify and offer slot when a cancellation occurs.
- **Appointment Transfer**:
  - Transfer an appointment:
    - To a different doctor (same department or selected departments).
    - To a different date/time.
  - Maintain link between original and transferred appointments for audit.
- **Group / Family Appointments**:
  - Support booking multiple linked appointments:
    - For family members with same doctor/time block.
    - Maintain relationships between group appointments.

##### 9.4.11.11 Access Control and Audit

- **Roles**:
  - **Hotline / Call Center / Front Desk**:
    - Create, confirm, edit, cancel appointments.
    - Mark arrivals and manage waitlist.
  - **Doctor Attendant / Clinic Staff**:
    - View doctor-specific appointment list.
    - Mark arrival, visited, completed statuses.
  - **Billing / Cashier**:
    - Handle payment, refunds, and payment-related status.
  - **Admin / Supervisor**:
    - Override booking rules (e.g., emergency, VIP, overbooking).
    - Manage configuration (policies, statuses, limits).
  - **Audit Trail**:
  - All changes to appointment records (create, update, cancel, reschedule, status changes, payment updates) shall be audited with:
    - Who performed the action.
    - When it was performed.
    - What fields changed (before/after).

##### 9.4.11.12 Data Model – PatientAppointment & Serial

**Entity Name**: `PatientAppointment`  
**Description**: Logical and physical representation of a patient’s appointment and serial with a specific doctor on a specific date (and optional time slot).

**Key Fields**:
- **Appointment ID**
  - **Type**: UUID
  - **Required**: Yes
  - **Description**: System-generated primary key for the appointment record.
- **Doctor ID**
  - **Type**: Reference (Doctor Master)
  - **Required**: Yes
  - **Description**: Links the appointment to a specific doctor (`doctors.doctor_id`).
- **Patient ID**
  - **Type**: Reference (Patient Registration)
  - **Required**: Yes
  - **Description**: Links the appointment to a specific patient.
- **Visit Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Planned consultation date for the appointment.
- **Appointment Date**
  - **Type**: Date
  - **Required**: Yes
  - **Description**: Date when the appointment was created/booked.
- **Shift**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Appointment shift, aligned with doctor schedule configuration (`Morning`, `Evening`, `Night`, etc.).
- **Serial No (SL No)**
  - **Type**: Integer
  - **Required**: Yes
  - **Description**: Per-doctor, per-day (and per shift, where applicable) appointment serial number.
  - **Validation / Rules**:
    - Unique per `(doctor, visit date, shift)` combination.
    - Generated using **Next SL No** logic and respecting doctor-level **Serial Start From** configuration.
- **Appointment Time / Slot Time** (optional)
  - **Type**: Time
  - **Required**: No (required where time-based slots are implemented)
  - **Description**: Start time of the appointment within the selected shift and Visit Date.
- **Appointment Status**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Current lifecycle state; values follow 9.4.11.8 (e.g., `Created`, `Confirmed`, `Arrived`, `Visited`, `Completed`, `Cancelled`, `No Show`).
- **Appointment Type**
  - **Type**: Enum
  - **Required**: No
  - **Description**: Classification of visit (e.g., New, Old/Follow-up, Procedure, Telemedicine, Emergency).
- **Booking Channel**
  - **Type**: Enum
  - **Required**: Yes
  - **Description**: Origin of the booking (e.g., `Front Desk`, `Hotline`, `Web`, `Mobile`, `Referral`); used for enforcing web/mobile limits.
- **Booked By User ID**
  - **Type**: Reference (User / Staff / Portal User)
  - **Required**: No
  - **Description**: Identifier of the user who created the appointment.
- **Check-in Time**
  - **Type**: DateTime
  - **Required**: No
  - **Description**: Timestamp when the patient checked in/arrived for the appointment.
- **Completion Time**
  - **Type**: DateTime
  - **Required**: No
  - **Description**: Timestamp when the appointment is marked as completed.
- **Cancellation Time / Reason**
  - **Type**: DateTime / Text
  - **Required**: No
  - **Description**: When and why the appointment was cancelled.
- **Rescheduled From Appointment ID**
  - **Type**: UUID (nullable)
  - **Required**: No
  - **Description**: Links this appointment to a previous appointment when rescheduled.
- **Is Emergency**
  - **Type**: Boolean
  - **Required**: No
  - **Description**: Indicates whether the appointment bypassed normal slot/capacity rules (see 9.4.11.10).
- **Notification Tracking Fields** (optional)
  - **Examples**:
    - Reminder sent flags (SMS/Email/App).
    - Last notification timestamp.
  - **Description**: Tracks delivery of confirmations and reminders.
- **Audit Fields**
  - **Type**: Standard (Created At/By, Updated At/By)
  - **Required**: Yes
  - **Description**: Used for traceability and compliance.

**Relationships and Constraints**:
- Each `PatientAppointment` **must reference** exactly one **Doctor** and one **Patient**.
- Doctor-level configuration from Doctor Master is used at booking time:
  - **Serial Start From**
  - **Patients Per Day**
  - **Number of Days Can Appointment**
  - **Number of Appointments from Web**
  - **Number of Appointments from Mobile**
- Appointment creation must enforce:
  - Patients-per-day and channel limits as defined in 9.4.5.6 and 9.4.5.7.
  - Doctor availability and schedule as defined in 9.4.5 and 9.4.6.
  - Uniqueness of serial numbers per doctor/date (and shift where applicable).

##### 9.4.11.13 User Stories – Appointment & Serial Management

- **US-A1 – Staff booking with serial assignment**  
  As a **front-desk staff**, I want to **book an appointment for a patient with a selected doctor and date** so that the system **assigns the correct serial number and (optional) time slot** based on the doctor’s schedule and capacity.

- **US-A2 – Patient self-service booking**  
  As a **patient**, I want to **book an appointment via web/mobile** so that I can see my **doctor, date, serial number, and expected time** immediately after booking.

- **US-A3 – Capacity enforcement per doctor**  
  As a **scheduler**, I want the system to **prevent me from exceeding a doctor’s Patients Per Day and channel limits** so that we do not overbook or violate agreed policies.

- **US-A4 – Availability-aware booking**  
  As a **scheduler**, I want the system to **block or warn** when booking an appointment for a doctor marked as `Not Available` or on leave so that we do not schedule patients when the doctor is unavailable.

- **US-A5 – Advance booking window**  
  As a **patient**, I want the system to **limit how many days in advance I can book** an appointment, based on hospital policy, so that I cannot book arbitrarily far in the future.

- **US-A6 – Check-in and queue visibility**  
  As **front-desk staff**, I want to **check in patients and update their appointment status** so that the doctor sees a **real-time queue** ordered by serial and status.

- **US-A7 – Completion of visit**  
  As a **doctor**, I want to **mark an appointment as completed** so that the system can **close the visit**, update statuses, and trigger downstream encounter/billing workflows.

- **US-A8 – No-show handling**  
  As a **scheduler**, I want to **mark patients as no-show after a configurable grace period** so that we can **track no-show behavior and free up slots** where appropriate.

- **US-A9 – Patient-initiated cancellation/reschedule**  
  As a **patient**, I want to **cancel or reschedule my upcoming appointment within defined time limits** so that I can responsibly manage my bookings without violating hospital rules.

- **US-A10 – Bulk rescheduling due to doctor unavailability**  
  As a **scheduler/administrator**, I want to **reschedule or cancel affected appointments when a doctor is unexpectedly unavailable** so that patients receive timely communication and alternative slots.

- **US-A11 – Queue position visibility**  
  As a **patient waiting in OPD**, I want to see **my serial number and approximate waiting time/queue position** so that I know how long I may need to wait.

- **US-A12 – Notifications**  
  As **hospital management**, I want the system to **send confirmations, reminders, and change notifications (reschedule/cancel/delay)** for appointments so that no-shows and confusion are reduced.

- **US-A13 – Management reporting**  
  As **management**, I want to **analyze appointment volumes, utilization, no-show rates, and channel mix by doctor and department** so that I can optimize staffing, schedules, and policies.

---

## 10. Doctor Visit Entry (Indoor / IPD)

### 10.1 Purpose

The Doctor Visit Entry capability records **daily doctor rounds/visits** for admitted inpatients (IPD). It is used for **clinical tracking**, **visit-based billing**, and **medico‑legal documentation**. Each visit:
- May generate one or more **doctor visit charges**.  
- Can trigger subsequent orders for **lab tests**, **medications**, or **procedures**.  
- Is referenced downstream by **nurses**, **billing**, and **medical records**.

> For nursing workflows, see also **Nurse Module – Doctor Visit Entry (Indoor Patient)** in `[nurse-module.md](nurse-module.md)`.

### 10.2 Pre‑Requisites

- **Active IPD Admission** exists for the patient (see `[admission-ipd.md](admission-ipd.md)`).  
- **Doctor assignment** (Under Doctor / Consultant) is configured on the admission.  
- **Bed allocation** is present and valid for the admission.  
- **User Roles**:
  - **Doctor (Primary / Consultant)** – can create and approve visits.  
  - **Authorized Nurse / Ward Nurse** – may enter visit details on behalf of a doctor (where policy allows), but cannot finalize/approve in configurations that require explicit doctor confirmation.  

### 10.3 Features

- **Admission-wise visit entry** (one record per visit event).  
- **Auto-fetch patient and bed details** when Admission ID / Patient ID is selected.  
- **Visit type classification**:
  - Regular Round  
  - Emergency Visit  
  - Consultant Visit / Second Opinion  
- **Automatic visit charge calculation** based on:
  - Doctor type (consultant, resident, etc.).  
  - Department and configured visit tariff.  
  - Visit Type (regular vs emergency vs consultant).  
- **Doctor notes & instructions**:
  - Free text for clinical assessment and plan.  
  - Explicit instructions for nurses (visible in Nurse Module).  
- **Visit history tracking**:
  - Admission-wise chronological list with billed/unbilled status.  
- **IPD Billing integration**:
  - Visits push charge lines to IPD bill with correct mapping to doctor and department.

### 10.4 User Interface

#### 10.4.1 Form View – Doctor Visit Entry

**Basic Identification**
- **Patient ID (Search / Scan)**  
  - Search supports Admission ID, UHID, or Patient ID.  
  - Required; on selection auto-fills admission context.  
- **Patient Name (Auto)**  
  - Read-only; fetched from patient master.  
- **UHID / Patient ID (Auto)**  
  - Configurable per hospital; read-only.  
- **Admission ID / IPD No (Auto)**  
  - Read-only; the active admission associated with the selected patient.  
- **Bed No / Ward (Auto)**  
  - Read-only; from current bed allocation.

**Visit Details**
- **Visit Date & Time**  
  - Default: Current date/time.  
  - Editable:  
    - Must not exceed current time.  
    - Back-dating allowed only with appropriate permission; all such edits audited.  
- **Doctor Name**  
  - Default: Logged-in doctor when role = Doctor.  
  - Alternatively selectable from Doctor Master (for shared terminals).  
- **Doctor ID**  
  - Auto-populated, read-only once selected.  
- **Visit Type**  
  - Enum values (configurable but at minimum): `Regular`, `Emergency`, `Consultant`.  
- **Visit Charge (Auto)**  
  - Derived from doctor/department tariff and visit type.  
  - May be read-only for most users; override allowed only with specific permission and full audit trail.  
- **Remarks / Clinical Notes**  
  - Long text; doctor’s notes on assessment, plan, and findings.  
- **Doctor Instructions (For Nurse)**  
  - Long text; explicit instructions to nursing staff (e.g., monitoring frequency, medication changes).

**Actions**
- **Save Visit**  
  - Persists visit record and (if enabled) posts corresponding charge to IPD Billing.  
- **Print Visit Note** (optional)  
  - Generates a formatted visit note suitable for charting/records.

#### 10.4.2 List View – Doctor Visit History

Columns:
- Visit No / Visit ID  
- Visit Date & Time  
- Doctor Name  
- Visit Type  
- Charge Amount  
- Entered By  
- Status (`Billed`, `Pending`, `Voided`)  

Row-level actions:
- **View** – open full visit details and notes.  
- **Edit** – only allowed while status is `Pending` and within configured time window.  
- **Print Visit Note** – for documentation.  

Filters:
- Date range  
- Doctor / Department  
- Visit Type  
- Billing Status  

### 10.5 Business Rules

- **Active Admission Only**  
  - Visits cannot be recorded for patients without an active IPD admission or for already discharged patients.  
- **Charge Derivation**  
  - Visit Charge is calculated from:
    - Doctor category (consultant vs resident).  
    - Department and visit tariff configuration.  
    - Visit Type (regular, emergency, consultant) and time-of-day rules (if any).  
- **One regular visit per doctor per day** (configurable)  
  - System should warn or optionally block if a second **Regular** visit is entered by the same doctor for the same patient and date.  
  - Emergency and Consultant visits are not constrained by this rule.  
- **Billing Lock**  
  - Once a visit is **billed** (charge posted and not reversed), the record becomes **read-only**.  
  - Any correction must use a **void/reversal** flow with mandatory reason and full audit.  
- **Role-based Editing**  
  - Nurses may be allowed to **enter** visit details (e.g., transcribing from written notes) but:
    - Configuration may require the doctor to **approve/finalize** the visit.  
    - Once approved or billed, nurses cannot edit the record.  

### 10.6 Edge Cases & Exceptions

- **Doctor Reassignment for Admission**  
  - If Under Doctor changes at the admission level, new visits are associated with the new doctor; historical visits retain their original doctor.  
- **Back-dated Visit Entry**  
  - Allowed only for authorized roles; system records both visit date/time and entry timestamp.  
  - Back-dated entries may be flagged for billing or audit review.  
- **Duplicate Visit on Same Date**  
  - For the same patient, doctor, and visit type = Regular on same date:
    - System shows a prominent warning or prevents save, depending on configuration.  
- **Emergency Visit Outside Normal Shift Hours**  
  - Such visits can be flagged (e.g., for additional charge or audit).  

### 10.7 Error Handling

- Invalid Patient / Admission ID → block save, show clear message.  
- Attempt to record visit for discharged or non-admitted patient → blocked; user pointed to appropriate workflow (e.g., OPD visit).  
- Missing or misconfigured tariff (no charge found for doctor/visit type) →  
  - System logs admin alert.  
  - Configuration may:
    - Block visit creation until tariff is fixed, or  
    - Allow visit save but mark it with **Missing Charge** for billing resolution.  
- Unauthorized role → return “Access Denied” and do not display visit entry form.

### 10.8 System Actions & Audit

- Auto-generate **Visit ID** for each recorded visit.  
- Auto-calculate and post **Visit Charge** to IPD Billing (if enabled).  
- Maintain a full **audit trail**:
  - Who created/modified the visit.  
  - When changes were made.  
  - Old vs new values for sensitive fields (visit type, charge, doctor, timestamps).  
- Notify Nurse Module / nursing dashboards of **new or updated doctor instructions** so they can be executed and documented in follow-up notes.  
