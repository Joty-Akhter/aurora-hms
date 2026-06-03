# Acceptance Criteria

### 8.1 Patient Health Records

This section provides comprehensive acceptance criteria for the Patient Health Records feature, organized by functional area. Acceptance criteria define measurable conditions that must be met for features to be considered complete and acceptable.

#### 8.1.1 Patient Registration and Demographics

**AC-1.1 Patient Registration**
- System successfully creates and stores patient demographic information with all required fields validated
- Patient registration form validates all data formats (date of birth, phone numbers, email addresses, ZIP codes, SSN) according to specified rules
- System assigns unique Medical Record Number (MRN) to each new patient that cannot be duplicated
- System checks for duplicate patient records using matching criteria (name + DOB, SSN, phone + name, email + name) and displays potential matches
- System prevents registration completion if mandatory fields are missing and displays clear error messages
- System stores patient registration information with complete audit trail including user ID, timestamp, and registration location
- System supports patient registration through multiple methods (manual entry, import, API integration)

**AC-1.2 Patient Search and Retrieval**
- Patient search returns accurate results within 3 seconds for searches by name, MRN, DOB, phone, or email
- System displays search results showing patient name, MRN, date of birth, age, last visit date, primary care provider, and patient status
- System supports advanced search with multiple criteria combinations (e.g., name + DOB, phone + name)
- System highlights exact matches vs. partial matches in search results
- System logs all patient searches with user ID, timestamp, and search criteria for audit purposes
- System enforces role-based access controls on search functionality and masks sensitive information based on user role
- System supports patient lookup by scanning barcode or QR code on patient ID cards

**AC-1.3 Patient Demographic Updates**
- System allows authorized users to update patient demographic information with complete validation
- System maintains complete audit trail of all demographic changes including field name, previous value, new value, user ID, timestamp, and reason for change
- System prevents updates to critical fields (SSN, DOB, name) without appropriate authorization or supervisor approval
- System validates all updated data using same validation rules as registration
- System supports bulk updates for administrative purposes with proper authorization
- System handles special update scenarios (name changes, address changes, insurance updates, status changes, provider reassignment)

**AC-1.4 Patient Record Management**
- System supports patient record status management (Active, Inactive, Deceased, Archived) with appropriate restrictions
- System prevents certain actions on inactive or deceased patients (e.g., scheduling appointments, creating new encounters)
- System supports patient record merging functionality with side-by-side comparison, master record selection, and complete audit trail
- System supports patient record splitting (unmerge) functionality with high-level authorization required
- System maintains patient record history and versioning allowing viewing of record as it existed at specific point in time
- System maintains historical addresses, phone numbers, and insurance information

**AC-1.5 Data Quality and Validation**
- System identifies records with missing required information and generates data quality reports
- System validates data formats, ranges, and logical consistency at data entry and during import
- System performs ongoing duplicate detection and alerts users to potential duplicates
- System standardizes addresses, phone numbers, and names according to configured rules
- System generates data quality reports identifying data quality issues, missing data, and validation errors

**AC-1.6 Privacy and Consent Management**
- System tracks patient privacy preferences and consents including HIPAA acknowledgment, consent forms, communication preferences, and marketing consent
- System stores all consents with date stamps and links to user who documented consent
- System supports patient privacy flags and alerts (confidentiality flags, VIP flags, security alerts, restricted access)
- System enforces additional access controls for flagged records

**AC-1.7 Patient Identification Card (Plastic Printed Card)**
- System supports creation and printing of a plastic patient identification card for every registered patient
- Card displays patient name and MRN/Patient ID as mandatory fields, with configurable optional fields (photo, DOB, blood group, contact)
- Card includes barcode or QR code encoding MRN to support patient lookup by scanning
- Card format is suitable for plastic card printing (standard dimensions, print-ready layout)
- System allows print at registration (if configured), single-card print from patient record, and batch print for multiple patients
- System supports reprint with audit logging (who, when, reason)
- System provides print preview before printing
- Card design/layout is configurable per facility (logo, fields, templates)

#### 8.1.2 Medical History Management

**AC-2.1 Past Medical History**
- System allows recording of comprehensive past medical history including chronic conditions, surgical history, hospitalizations, major illnesses, and obstetric history
- System stores condition/diagnosis names with ICD-10/ICD-11 code support, dates of onset/resolution, status, severity, and source of information
- System stores surgical history with procedure names (CPT codes), dates, surgeons, locations, indications, complications, and outcomes
- System stores hospitalization information with admission/discharge dates, facilities, reasons, procedures, complications, and discharge disposition
- System allows updating medical history entries with resolution dates, status changes, or additional information

**AC-2.2 Family History**
- System allows recording of family medical history with relationship mapping (mother, father, siblings, grandparents, etc.)
- System stores family history with condition names, relationship to patient, age of onset, and current status
- System displays family history organized by relationship and condition type

**AC-2.3 Social History**
- System allows recording of social history including smoking status, alcohol use, drug use, occupation, education, living situation, and lifestyle factors
- System stores social history with dates, frequencies, quantities, and status (current, past, never)
- System allows updating social history as patient information changes

**AC-2.4 Immunization History**
- System maintains complete immunization history with vaccine names, dates administered, lot numbers, administering facility, and route of administration
- System supports standard vaccine codes (CVX codes) and displays immunization status
- System allows adding, updating, and verifying immunization records

**AC-2.5 Medical History Display and Organization**
- System displays medical history in chronological timeline format showing progression of conditions and events over time
- System organizes medical history by category (past medical, family, social, immunizations) in patient dashboard
- System allows searching and filtering medical history by condition type, date range, or status
- System supports attachment of medical documents (lab reports, imaging reports, outside records) to medical history entries

#### 8.1.3 Vital Signs and Clinical Measurements

**AC-3.1 Vital Signs Entry**
- System allows recording of vital signs including blood pressure, heart rate, respiratory rate, temperature, oxygen saturation, weight, height, and BMI
- System calculates BMI automatically from weight and height measurements
- System validates vital signs values against reasonable ranges and flags abnormal values
- System allows recording vital signs for specific encounters or as standalone measurements
- System saves vital signs immediately and makes them available to all authorized users

**AC-3.2 Vital Signs Display**
- System displays vital signs in table format with date and time stamps
- System displays vital signs trends in graphical format (line charts, trend lines) over time
- System compares current vital signs to previous measurements and displays percentage changes
- System displays growth charts for pediatric patients (height, weight, head circumference)
- System organizes vital signs by encounter or chronologically

**AC-3.3 Vital Signs Alerts and Notifications**
- System displays alerts when vital signs are outside normal ranges (e.g., high blood pressure, elevated temperature, low oxygen saturation)
- System highlights abnormal vital signs with visual indicators (colors, icons)
- System allows configuring alert thresholds based on patient age, condition, or clinical protocols

**AC-3.4 Vital Signs Integration**
- System links vital signs to specific encounters when recorded during visits
- System integrates vital signs into patient summary views and clinical notes
- System allows exporting vital signs data for reporting or analysis purposes

#### 8.1.4 Clinical Notes and Documentation

**AC-4.1 Note Creation**
- System allows creation of multiple note types (SOAP notes, progress notes, consultation notes, procedure notes, discharge summaries)
- System provides customizable templates for different encounter types and specialties
- System supports structured data entry, free text, and rich text formatting
- System allows inserting structured data (diagnoses, medications, vital signs, lab results) directly into notes from patient record
- System supports voice-to-text capabilities for note creation
- System allows copying and modifying previous notes when creating new documentation

**AC-4.2 Note Signing and Authentication**
- System requires electronic signature with authentication (password, biometric) before notes can be finalized
- System timestamps signatures and locks notes after signing to prevent modification
- System displays signature information including signer name, role, date, and time
- System prevents unsigned notes from being considered complete

**AC-4.3 Note Version Control**
- System maintains version history of notes including all edits, who made changes, and when
- System allows viewing note history and comparing versions
- System supports amending notes after signing with proper documentation of amendments
- System tracks all note modifications in audit trail

**AC-4.4 Note Display and Search**
- System displays notes in chronological order in patient record
- System allows searching notes by keywords, date range, encounter type, or author
- System links notes to specific encounters, diagnoses, or procedures
- System displays notes with proper formatting and structure

#### 8.1.5 Diagnoses and Problem Lists

**AC-5.1 Problem List Management**
- System allows adding diagnoses to problem list with ICD-10/ICD-11 codes, status (active, resolved, chronic), and date of onset
- System displays active and resolved problems separately in problem list
- System allows updating problem status (e.g., marking as resolved with resolution date)
- System allows prioritizing problems (primary, secondary) and organizing by category or system
- System allows adding free-text problems when specific ICD code is not available

**AC-5.2 Problem List Display**
- System displays problem lists prominently in patient summary view
- System shows problem history including when problems were added, modified, or resolved
- System links problems to encounters, prescriptions, or lab results for clinical context
- System allows searching for diagnoses using ICD codes or condition names

**AC-5.3 Problem List Integration**
- System integrates problem list with clinical notes, prescriptions, and encounters
- System updates problem list when diagnoses are added or modified in encounters
- System displays problems in chronological timeline view

#### 8.1.6 Laboratory Results

**AC-6.1 Laboratory Result Display**
- System displays laboratory results with test names, values, reference ranges, units, and abnormal value flags
- System displays lab results in chronological order with date and time stamps
- System displays lab results in graphical format (trend lines, scatter plots) for tests performed multiple times
- System displays reference ranges with lab results to indicate if values are normal, high, or low
- System allows filtering lab results by test type, date range, or abnormal status

**AC-6.2 Critical Value Management**
- System displays critical value alerts prominently when lab results are outside critical ranges
- System requires acknowledgment of critical value alerts before proceeding
- System sends notifications to ordering providers when critical results are received
- System logs all critical value alerts and acknowledgments

**AC-6.3 Laboratory Result Integration**
- System links lab results to ordering provider and encounter
- System allows adding notes or interpretations to lab results
- System compares current lab results to previous results and displays percentage changes
- System integrates lab results into patient summary views and clinical notes

**AC-6.4 Laboratory Result Management**
- System allows correcting or amending lab results with proper documentation when errors are identified
- System maintains audit trail of all lab result modifications
- System allows exporting lab results for reporting or sharing with patients
- System receives lab results electronically from Laboratory Information Systems (LIS)

#### 8.1.7 Imaging and Diagnostic Studies

**AC-7.1 Imaging Study Display**
- System displays imaging study reports with study type, date performed, ordering provider, and interpreting radiologist
- System displays imaging study metadata including modality (X-ray, CT, MRI, ultrasound), body part, and contrast use
- System displays imaging reports in chronological order
- System allows searching imaging studies by type, date range, body part, or ordering provider

**AC-7.2 Critical Findings Management**
- System displays critical findings alerts for imaging studies that require immediate attention
- System requires acknowledgment of critical findings before proceeding
- System sends notifications to ordering providers when critical findings are reported
- System logs all critical findings alerts and acknowledgments

**AC-7.3 Imaging Study Integration**
- System links imaging studies to specific encounters or diagnoses
- System allows adding notes or interpretations to imaging reports
- System integrates with PACS systems to access DICOM images when available
- System receives imaging reports electronically from radiology systems

**AC-7.4 Imaging Study Management**
- System allows correcting or amending imaging reports with proper documentation when errors are identified
- System maintains audit trail of all imaging report modifications
- System allows exporting imaging reports for reporting or sharing

#### 8.1.8 Allergies and Adverse Reactions

**AC-8.1 Allergy Documentation**
- System allows adding allergies with allergen name, reaction type, severity, verification status, and date recorded
- System allows updating allergy information including marking allergies as resolved or updating severity
- System allows verifying allergy information with patients and updating verification status
- System allows documenting adverse drug reactions separately from allergies

**AC-8.2 Allergy Display and Alerts**
- System displays allergy and adverse reaction list prominently at top of patient dashboard and in all relevant clinical views
- System displays allergies with allergen name, reaction type, severity, verification status, and date recorded
- System displays visual alerts (red highlighting, warning icons) when viewing or creating prescriptions
- System organizes allergies by type (drug, food, environmental) and severity
- System shows allergy history including when allergies were added, modified, or resolved

**AC-8.3 Drug-Allergy Interaction Checking**
- System checks for drug-allergy interactions when prescribing medications
- System displays drug-allergy interaction alerts prominently with severity levels
- System prevents prescribing medications that match patient allergies unless explicitly overridden with documentation
- System logs all drug-allergy interaction checks and overrides

#### 8.1.9 Medications (Current and Historical)

**AC-9.1 Medication List Management**
- System displays current medication list with medication names, dosages, frequencies, start dates, and prescribing providers
- System displays medication history including discontinued medications with stop dates and reasons
- System allows adding medications to medication list manually or from prescriptions
- System allows discontinuing medications with stop dates and reasons
- System organizes medications by status (active, discontinued, on hold) and category

**AC-9.2 Medication Reconciliation**
- System supports medication reconciliation comparing current medications to previous lists
- System identifies changes and discrepancies between medication lists
- System allows resolving discrepancies and updating medication lists
- System maintains reconciliation history and audit trail

**AC-9.3 Medication Integration**
- System links medications to diagnoses or problems for clinical indication
- System displays medication adherence information including fill dates and refill patterns
- System checks for drug interactions when viewing medication lists
- System integrates medication lists with prescription management feature

**AC-9.4 Medication Search and Display**
- System allows searching medications by name, prescribing provider, or date range
- System displays medications in chronological order showing medication changes over time
- System displays medication information including generic and brand names, dosages, and instructions

#### 8.1.10 Viewing and Access

**AC-10.1 Patient Dashboard**
- System displays comprehensive patient dashboard with all key information (demographics, problems, medications, allergies, vital signs, recent results) in one view
- System organizes information chronologically and by category
- System allows customizing dashboard layout to show information most relevant to user's practice
- System displays patient dashboard within 3 seconds of patient selection

**AC-10.2 Chronological Timeline**
- System displays chronological timeline of all patient encounters, diagnoses, medications, lab results, and clinical events
- System allows filtering timeline by event type, date range, or provider
- System links timeline events to detailed records for drill-down access

**AC-10.3 Record Navigation and Search**
- System allows filtering and searching patient records by date range, encounter type, diagnosis, or provider
- System allows navigating between different sections of patient record (demographics, history, notes, results) easily
- System links related information together (e.g., prescriptions linked to diagnoses, lab results linked to encounters)
- System displays search results within 3 seconds

**AC-10.4 Print and Export**
- System allows printing patient records in standard formats (PDF, summary reports)
- System allows exporting patient records for sharing with patients or other providers
- System maintains formatting and structure in printed and exported records
- System supports exporting specific sections or complete records

**AC-10.5 Mobile and Responsive Access**
- System provides responsive design for mobile device access
- System allows accessing patient records from tablets and smartphones
- System maintains functionality and performance on mobile devices
- System optimizes display for different screen sizes

#### 8.1.11 Data Quality and Validation

**AC-11.1 Data Validation**
- System validates all data entry with format checking, range validation, and completeness checks
- System displays clear error messages for validation failures with suggestions for correction
- System prevents saving invalid data and highlights fields with errors
- System validates data during import and displays import error reports

**AC-11.2 Data Quality Monitoring**
- System identifies records with missing required information
- System generates data quality reports identifying missing data, duplicate records, and validation errors
- System displays data quality indicators (missing information, incomplete records) to users
- System supports data standardization (addresses, phone numbers, names)

#### 8.1.12 Security and Privacy

**AC-12.1 Access Control**
- System enforces role-based access controls ensuring users only access information appropriate to their role
- System requires user authentication before allowing access to patient records
- System logs all access to patient records with user ID, timestamp, and actions taken
- System supports patient privacy flags and restrictions on information sharing

**AC-12.2 Audit Trail**
- All data modifications are logged in audit trail with user ID, timestamp, field changed, previous value, and new value
- System maintains complete audit trail of all patient record activities (creation, viewing, modification, deletion)
- System allows viewing audit trail for specific patients or users
- System exports audit trail data for compliance reporting

**AC-12.3 Data Security**
- System encrypts all patient data at rest and in transit
- System protects patient information according to HIPAA requirements
- System enforces minimum necessary access controls limiting information display to minimum necessary for user's role
- System supports patient privacy preferences and consent management

**AC-12.4 Privacy Management**
- System manages patient privacy flags and alerts (confidentiality flags, VIP flags, security alerts)
- System enforces additional access controls for flagged records
- System tracks patient privacy preferences and consent forms
- System supports restrictions on information sharing based on patient preferences

#### 8.1.13 Integration and Interoperability

**AC-13.1 Clinical Data Integration**
- All clinical data is properly linked to patient records with appropriate foreign key relationships
- System integrates patient health records with prescription management feature sharing medication history and allergy information
- System receives laboratory results electronically from Laboratory Information Systems (LIS)
- System receives imaging reports electronically from radiology/PACS systems

**AC-13.2 Standards Compliance**
- System supports standard terminologies (ICD-10/ICD-11, LOINC, SNOMED CT, RxNorm) for coded data
- System supports HL7 FHIR for data exchange with other systems
- System complies with healthcare data standards for interoperability

#### 8.1.14 Performance and Usability

**AC-14.1 Performance Requirements**

**Note**: Detailed performance requirements with percentile specifications are defined in Section 4.2.1.1 (Patient Record Access). These acceptance criteria represent minimum acceptable performance thresholds for testing.

- Patient search returns accurate results within 3 seconds (aligns with Section 4.2.1.1: large result sets ≤ 3 seconds at 95th percentile)
- Patient dashboard loads within 3 seconds of patient selection (aligns with Section 4.2.1.1: patient record loading ≤ 3 seconds at 95th percentile)
- System displays search results within 3 seconds
- System supports concurrent access by multiple users without performance degradation

**AC-14.2 Usability Requirements**
- System provides intuitive user interface for accessing and navigating patient records
- System displays information in organized, easy-to-read formats
- System provides clear navigation between different sections of patient record
- System supports keyboard shortcuts and efficient workflows for common tasks

**AC-14.3 Error Handling**
- System displays clear, user-friendly error messages when operations fail
- System prevents data loss during system errors or interruptions
- System provides recovery mechanisms for failed operations
- System logs all errors for troubleshooting and system improvement

### 8.2 Prescription Management

This section provides comprehensive acceptance criteria for the Prescription Management feature, organized by functional area. Acceptance criteria define measurable conditions that must be met for features to be considered complete and acceptable.

#### 8.2.1 Prescription Creation

**AC-P1.1 Patient Selection and Context**
- System requires patient selection before allowing prescription creation
- System displays patient summary including current medications, known allergies (prominently displayed), active problems/diagnoses, recent lab results, age, weight, and insurance information during prescription creation
- System displays patient's current medication list during prescription creation to avoid duplicate therapies
- System displays patient's allergy list prominently with visual alerts
- System displays patient's preferred pharmacy if available
- System displays recent prescription history for context

**AC-P1.2 Medication Selection**
- System supports medication search by generic name, brand name, drug class/category, NDC code, RxNorm code, indication/therapeutic use, or partial name (fuzzy matching)
- System provides auto-complete suggestions as user types medication name
- System displays recent medications list, favorite medications list, and common medications list for quick selection
- System displays medication information including generic and brand names, strength options, available dosage forms, drug class, NDC code, and RxNorm code
- System allows selection of specific medication and strength from search results
- System supports combination medications

**AC-P1.3 Prescription Details and Dosage**
- System requires entry of dosage strength, dosage form, quantity per dose, and total quantity
- System validates dosage strength is appropriate for selected medication
- System requires entry of route of administration (Oral, IV, Topical, etc.)
- System requires entry of frequency (Once daily, Twice daily, etc.) with timing instructions
- System allows entry of special instructions (e.g., "take with food", "avoid alcohol")
- System allows entry of duration (e.g., "10 days", "3 months", "Ongoing")
- System calculates total quantity based on duration and frequency (optional auto-calculation)
- System validates all dosage information is reasonable and appropriate

**AC-P1.4 Refills and Substitution**
- System allows specifying number of refills authorized (0-11, default 0)
- System tracks refills remaining and updates automatically when refills are processed
- System allows specifying whether generic substitution is allowed (DAW code)
- System enforces refill restrictions for controlled substances (Schedule II cannot have refills unless state allows)

**AC-P1.5 Pharmacy Selection**
- System allows selecting patient's preferred pharmacy from list
- System supports searching for pharmacies by name, location, or NPI
- System displays pharmacy information including name, address, phone number, and NPI
- System allows selecting pharmacy before or during prescription creation

**AC-P1.6 Prescription Templates and Efficiency**
- System provides prescription templates for commonly prescribed medications
- System allows users to create and customize prescription templates
- System supports copying and modifying existing prescriptions
- System supports batch prescription creation for multiple medications in single session
- System saves frequently used medications for quick access

**AC-P1.7 Clinical Context and Linking**
- System allows linking prescriptions to specific diagnoses/problems for clinical indication
- System allows adding clinical indication notes (free text)
- System links prescriptions to patient encounters when created during visits
- System displays relevant patient information (age, weight, lab results) for dosage calculations

**AC-P1.8 Formulary and Drug Information**
- System checks patient's insurance formulary when available and displays formulary status
- System displays drug information including indications, contraindications, side effects, and dosing guidelines
- System suggests alternative medications when formulary indicates preferred alternatives
- System displays cost information when available

#### 8.2.2 Drug Interaction and Allergy Checking

**AC-P2.1 Drug-Drug Interaction Checking**
- System checks for drug-drug interactions in real-time as medications are selected
- System checks new prescription against patient's current medications for interactions
- System displays interaction severity levels (Contraindicated, Major, Moderate, Minor)
- System displays detailed interaction information including mechanism and clinical significance
- System displays interaction alerts prominently before prescription is finalized
- System requires acknowledgment of interaction alerts before proceeding
- System allows overriding interactions with documentation of clinical rationale

**AC-P2.2 Drug-Allergy Interaction Checking**
- System checks for drug-allergy interactions when prescribing medications
- System displays drug-allergy interaction alerts prominently with high severity
- System prevents prescribing medications that match patient allergies unless explicitly overridden with documentation
- System displays allergy information (allergen name, reaction type, severity) in interaction alerts
- System logs all drug-allergy interaction checks and overrides

**AC-P2.3 Drug-Disease Contraindication Checking**
- System checks for drug-disease contraindications based on patient's active problems/diagnoses
- System displays contraindication alerts when medications are inappropriate for patient conditions
- System allows overriding contraindications with documentation when clinically appropriate

**AC-P2.4 Duplicate Therapy Detection**
- System detects duplicate therapies when prescribing medications with similar therapeutic effects
- System displays duplicate therapy warnings with details about existing medications
- System allows overriding duplicate therapy warnings with documentation

**AC-P2.5 Dosage Appropriateness Validation**
- System validates dosages are appropriate for patient's age, weight, and condition
- System checks pediatric dosages against age-appropriate ranges
- System checks weight-based dosages for accuracy
- System validates dosages against standard dosing guidelines
- System displays dosage validation warnings when dosages are outside normal ranges

**AC-P2.6 Alert Display and Management**
- System displays all interaction and safety alerts in prominent, easy-to-read format
- System prioritizes alerts by severity (critical alerts displayed first)
- System groups related alerts together for efficient review
- System allows dismissing alerts after acknowledgment
- System maintains alert history for audit purposes

**AC-P2.7 Alert Override and Documentation**
- System requires documentation when overriding interaction or allergy alerts
- System captures override reason, user ID, and timestamp
- System maintains complete audit trail of all alert overrides
- System allows configuring which alerts can be overridden and which require mandatory action

#### 8.2.3 Prescription Transmission

**AC-P3.1 Electronic Prescription Transmission**
- Prescriptions are successfully transmitted to pharmacies electronically via e-prescribing networks (e.g., Surescripts)
- System transmits prescriptions in standard NCPDP SCRIPT format
- System confirms successful transmission and updates prescription status to "Sent"
- System receives transmission confirmation from pharmacy network
- System updates prescription status to "Received" when pharmacy acknowledges receipt
- System handles transmission failures gracefully with retry mechanisms

**AC-P3.2 Transmission Status Tracking**
- System tracks prescription transmission status (Draft, Signed, Pending, Transmitting, Sent, Received, Failed)
- System displays transmission status for each prescription
- System logs all transmission attempts with timestamps and results
- System notifies users of transmission failures with error details

**AC-P3.3 Alternative Transmission Methods**
- System supports fax transmission when electronic transmission is not available
- System supports printing prescriptions when electronic or fax transmission fails
- System maintains same prescription data regardless of transmission method
- System tracks transmission method used for each prescription

**AC-P3.4 Pharmacy Selection and Management**
- System allows selecting pharmacy from patient's preferred pharmacies list
- System supports searching for pharmacies by name, location, or NPI
- System validates pharmacy NPI before transmission
- System stores pharmacy information for future prescriptions
- System allows updating pharmacy information

**AC-P3.5 Transmission Security**
- System encrypts prescription data during transmission
- System verifies pharmacy identity before transmission
- System maintains secure connection with e-prescribing networks
- System complies with HIPAA requirements for prescription data transmission

**AC-P3.6 Batch Transmission**
- System supports transmitting multiple prescriptions for same patient in single transaction
- System tracks batch transmission status for all prescriptions in batch
- System handles partial batch failures (some prescriptions succeed, others fail)

#### 8.2.4 Prescription Management and Tracking

**AC-P4.1 Prescription Status Tracking**
- Prescription status is accurately tracked throughout lifecycle (Draft, Signed, Pending, Transmitting, Sent, Received, Filled, Partially Filled, Cancelled, Expired, Refilled, Replaced, On Hold)
- System updates prescription status automatically based on transmission and fill events
- System displays current status for each prescription
- System maintains status history with timestamps

**AC-P4.2 Prescription Display and Viewing**
- System displays all prescriptions for patient in chronological order
- System displays prescription details including medication, dosage, frequency, quantity, refills, pharmacy, and status
- System allows filtering prescriptions by status, date range, medication, or provider
- System allows searching prescriptions across all patients by medication name, date, or status
- System displays prescription history including all modifications and status changes

**AC-P4.3 Prescription History**
- Prescription history is complete and accurate with all prescriptions, modifications, and status changes
- System maintains complete audit trail of prescription lifecycle
- System displays prescription history in chronological order
- System links prescriptions to encounters, diagnoses, and providers

**AC-P4.4 Prescription Expiration and Renewal**
- System tracks prescription expiration dates
- System prevents using expired prescriptions for refills
- System allows renewing expired prescriptions when appropriate
- System displays expiration warnings before prescriptions expire

#### 8.2.5 Prescription Refills

**AC-P5.1 Refill Request Receipt**
- Refill requests are properly routed to prescribing providers
- System receives refill requests from pharmacies electronically
- System receives refill requests from patients (via portal if available)
- System displays refill requests organized by patient and medication
- System notifies providers when refill requests are received

**AC-P5.2 Refill Request Review**
- System displays refill request details including patient name, medication, pharmacy, number of refills requested, and refills remaining
- System displays patient's current status and recent visits when reviewing refill requests
- System displays prescription history and medication adherence information
- System allows filtering and sorting refill requests by date, patient, or medication

**AC-P5.3 Refill Approval Process**
- System allows approving refill requests with one click for routine medications
- System processes approved refills and updates prescription status
- System notifies pharmacy when refills are approved
- System updates refills remaining count automatically
- System maintains refill approval history with timestamps

**AC-P5.4 Refill Denial Process**
- System allows denying refill requests with reason documentation
- System notifies pharmacy and patient when refills are denied
- System requires reason for denial to be documented
- System maintains refill denial history

**AC-P5.5 Refill Modification**
- System allows modifying refill requests (change quantity, add instructions) before approving
- System processes modified refills with updated information
- System maintains modification history

**AC-P5.6 Auto-Approval Rules**
- System supports configuring automatic approval rules for certain medications
- System processes auto-approved refills without manual review
- System logs all auto-approved refills for audit purposes
- System allows overriding auto-approval rules when needed

**AC-P5.7 Refill History and Tracking**
- System tracks complete refill history including all refills, dates, pharmacies, and providers
- System displays refill history for each prescription
- System calculates medication adherence based on refill patterns
- System generates refill reports and analytics

#### 8.2.6 Prescription Modifications and Cancellations

**AC-P6.1 Prescription Modification**
- System allows modifying prescriptions that haven't been sent yet
- System validates modifications and checks for new interactions or issues
- System maintains modification history with previous values
- System requires re-signing modified prescriptions before transmission

**AC-P6.2 Prescription Cancellation**
- System allows cancelling prescriptions that have been sent but not yet filled
- System prevents cancelling prescriptions that have already been filled
- System notifies pharmacy when prescriptions are cancelled
- System requires reason documentation for cancellations
- System maintains cancellation history with timestamps

**AC-P6.3 Prescription Replacement**
- System allows replacing existing prescriptions with new ones when changes are needed
- System links replacement prescriptions to original prescriptions
- System cancels original prescription when replacement is created
- System maintains replacement history

**AC-P6.4 Modification and Cancellation Documentation**
- System documents all modifications and cancellations with user ID, timestamp, and reason
- System maintains complete audit trail of prescription changes
- System displays modification and cancellation history

#### 8.2.7 Controlled Substances Management

**AC-P7.1 Controlled Substance Identification**
- System automatically identifies controlled substances and applies enhanced security requirements
- System displays controlled substance schedule (I, II, III, IV, V) for medications
- System enforces different rules based on controlled substance schedule

**AC-P7.2 DEA Number Validation**
- System validates DEA number when prescribing controlled substances
- System verifies DEA number format and checksum
- System ensures prescriber is authorized to prescribe controlled substances
- System prevents prescribing controlled substances without valid DEA number

**AC-P7.3 PDMP Integration**
- System queries Prescription Drug Monitoring Program (PDMP) when prescribing controlled substances
- System displays PDMP information including patient's controlled substance history
- System requires provider to review PDMP information before finalizing controlled substance prescriptions
- System logs PDMP queries for audit purposes

**AC-P7.4 Quantity and Duration Limits**
- System enforces quantity and duration limits for controlled substances based on schedule and state regulations
- System displays warnings when quantities exceed recommended limits
- System requires documentation when exceeding standard limits
- System enforces state-specific quantity limits

**AC-P7.5 Refill Restrictions**
- System prevents refills for Schedule II controlled substances (unless state allows)
- System enforces refill limits for Schedule III-V controlled substances
- System tracks refills for controlled substances separately

**AC-P7.6 State-Specific Requirements**
- Controlled substance prescriptions comply with regulatory requirements including state-specific rules
- System enforces state-specific quantity limits, duration limits, and refill restrictions
- System applies state-specific prescription format requirements
- System complies with state PDMP requirements

**AC-P7.7 Enhanced Security**
- System applies enhanced security measures to controlled substance prescriptions
- System requires additional authentication for controlled substance prescriptions
- System logs all controlled substance prescription activities with enhanced detail
- System restricts access to controlled substance prescription data

#### 8.2.8 Prescription History and Reporting

**AC-P8.1 Prescription History Management**
- Prescription history is complete and accurate with all prescriptions, modifications, refills, and status changes
- System maintains complete prescription history for each patient
- System displays prescription history in chronological order
- System allows searching prescription history by medication, date range, or provider

**AC-P8.2 Medication Adherence Tracking**
- System tracks medication adherence based on prescription fill dates and refill patterns
- System calculates adherence metrics (e.g., percentage of days covered)
- System displays adherence information in patient medication lists
- System generates adherence reports

**AC-P8.3 Prescription Analytics**
- System generates prescription analytics including most prescribed medications, refill rates, and prescribing patterns
- System tracks prescription metrics by provider, medication, or patient
- System generates prescription utilization reports
- System supports prescription trend analysis

**AC-P8.4 Prescription Reporting**
- System generates prescription reports by date range, provider, medication, or patient
- System exports prescription data for reporting purposes
- System supports custom report generation
- System maintains prescription reporting history

**AC-P8.5 Audit Trail and Activity Logging**
- System maintains complete audit trail of all prescription activities (creation, modification, transmission, refills, cancellations)
- System logs all prescription activities with user ID, timestamp, and action details
- System allows viewing audit trail for specific prescriptions or users
- System exports audit trail data for compliance reporting

#### 8.2.9 Integration with Patient Records

**AC-P9.1 Medication List Integration**
- System automatically updates patient's medication list when prescriptions are created or discontinued
- System synchronizes medication lists between prescription management and patient health records
- System displays current medications from prescriptions in patient summary views
- System links prescriptions to medication history entries

**AC-P9.2 Problem/Diagnosis Integration**
- System links prescriptions to diagnoses/problems for clinical indication
- System displays linked diagnoses in prescription views
- System allows selecting diagnoses when creating prescriptions
- System tracks which prescriptions treat which problems

**AC-P9.3 Patient Summary Integration**
- System includes prescriptions in patient summary views
- System displays active prescriptions prominently in patient dashboard
- System integrates prescription information with other clinical data

**AC-P9.4 Allergy Integration**
- System checks patient allergies when creating prescriptions
- System displays allergy alerts prominently during prescription creation
- System prevents prescribing medications that match patient allergies unless overridden

**AC-P9.5 Encounter Integration**
- System links prescriptions to patient encounters when created during visits
- System displays prescriptions in encounter summaries
- System tracks which prescriptions were created during which encounters

**AC-P9.6 Clinical Notes Integration**
- System automatically includes prescriptions in clinical notes when created during encounters
- System allows adding prescription information to notes manually
- System links prescriptions to relevant clinical documentation

#### 8.2.10 Pharmacy and External System Integration

**AC-P10.1 E-Prescribing Network Integration**
- System integrates with e-prescribing networks (e.g., Surescripts) for prescription transmission
- System receives refill requests from pharmacies through e-prescribing networks
- System receives prescription status updates from pharmacies
- System handles network connectivity and error recovery

**AC-P10.2 Pharmacy System Integration**
- System transmits prescriptions to pharmacy systems in standard NCPDP SCRIPT format
- System receives fill status updates from pharmacies when available
- System receives prescription modification requests from pharmacies
- System communicates with pharmacy systems securely

**AC-P10.3 Formulary Integration**
- System integrates with Pharmacy Benefit Managers (PBM) for formulary checking
- System checks patient's insurance formulary when creating prescriptions
- System displays formulary status and alternative medication suggestions
- System receives prior authorization requests from pharmacies

**AC-P10.4 Drug Information Database Integration**
- System integrates with drug information databases for medication information, interactions, and dosing guidelines
- System accesses current drug information in real-time
- System updates drug information regularly

**AC-P10.5 PDMP Integration**
- System integrates with Prescription Drug Monitoring Programs (PDMP) for controlled substance queries
- System queries PDMP when prescribing controlled substances
- System displays PDMP information to providers
- System logs all PDMP queries

#### 8.2.11 Error Handling and Recovery

**AC-P11.1 Error Messages**
- System displays clear, user-friendly error messages when prescription operations fail
- System provides specific error details and suggestions for resolution
- System prevents data loss during errors or interruptions

**AC-P11.2 Draft Saving**
- System saves prescription drafts automatically to prevent data loss
- System allows recovering unsaved prescriptions after session interruption
- System maintains draft prescriptions until completed or deleted

**AC-P11.3 Transmission Retry**
- System allows retrying failed prescription transmissions
- System provides retry mechanisms with automatic retry options
- System logs all transmission retry attempts

**AC-P11.4 Validation Error Handling**
- System displays validation errors clearly with field-level error messages
- System prevents saving prescriptions with validation errors
- System provides suggestions for correcting validation errors

#### 8.2.12 Security and Compliance

**AC-P12.1 Access Control**
- System enforces role-based access controls ensuring only authorized prescribers can create prescriptions
- System requires user authentication before allowing prescription creation
- System restricts prescription modification and cancellation to authorized users
- System enforces minimum necessary access controls

**AC-P12.2 Audit Logging**
- System logs all prescription activities (creation, modification, transmission, refills, cancellations) with user ID and timestamp
- System maintains complete audit trail of prescription lifecycle
- System allows viewing audit trail for specific prescriptions or users
- System exports audit trail data for compliance reporting

**AC-P12.3 Data Security**
- System encrypts all prescription data at rest and in transit
- System protects prescription information according to HIPAA requirements
- System maintains secure connections with external systems
- System protects controlled substance prescription data with enhanced security

**AC-P12.4 Regulatory Compliance**
- Controlled substance prescriptions comply with regulatory requirements including DEA and state regulations
- System complies with HIPAA requirements for prescription data
- System complies with e-prescribing standards (NCPDP SCRIPT)
- System complies with state prescription regulations
- System supports quality measure reporting requirements

#### 8.2.13 Performance and Usability

**AC-P13.1 Performance Requirements**

**Note**: Detailed performance requirements with percentile specifications are defined in Section 4.2.1.2 (Prescription Management Performance). These acceptance criteria represent minimum acceptable performance thresholds for testing. Note: Some acceptance criteria thresholds differ from the detailed performance requirements - acceptance criteria may be more lenient for initial testing, while performance requirements specify target percentiles for production.

- System displays medication search results within 2 seconds (Section 4.2.1.2 specifies ≤ 1 second at 95th percentile for medication search/selection)
- System performs drug interaction checking in real-time (within 1 second) (Section 4.2.1.2 specifies ≤ 3 seconds at 95th percentile for drug interaction checking)
- System transmits prescriptions within 5 seconds of signing (Section 4.2.1.2 specifies ≤ 30 seconds at 95th percentile for electronic transmission - acceptance criteria is more stringent for testing)
- System displays prescription history within 3 seconds (Section 4.2.1.2 specifies ≤ 2 seconds at 95th percentile for prescription history display)
- System supports concurrent prescription creation by multiple users

**AC-P13.2 Usability Requirements**
- System provides intuitive user interface for prescription creation and management
- System displays prescription information in organized, easy-to-read formats
- System provides clear navigation between prescription functions
- System supports keyboard shortcuts and efficient workflows
- System provides helpful tooltips and guidance during prescription creation

**AC-P13.3 User Experience**
- System provides auto-complete and suggestions to speed prescription entry
- System minimizes number of clicks required to create prescriptions
- System provides clear visual feedback for all actions
- System supports mobile-responsive design for prescription management

---

