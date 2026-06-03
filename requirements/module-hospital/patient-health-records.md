# Patient Health Records

## Overview

#### 2.1.1 Feature Description
The Patient Health Records feature serves as the foundational component of the EHR system, providing healthcare providers with a comprehensive, centralized repository for all patient medical information. This feature enables the creation, viewing, updating, and management of complete patient health records throughout the entire patient care continuum - from initial registration through ongoing care management and historical record keeping.

The Patient Health Records feature transforms traditional paper-based medical records into a digital, accessible, and secure system that supports evidence-based clinical decision-making, improves care coordination, and enhances patient safety. It serves as the single source of truth for patient information, ensuring that all healthcare providers have access to accurate, up-to-date, and complete patient data when making clinical decisions.

#### 2.1.2 Business Value and Benefits
The Patient Health Records feature delivers significant value to healthcare organizations and patients:

**For Healthcare Providers:**
- **Improved Clinical Decision-Making**: Access to comprehensive patient history, including past diagnoses, medications, allergies, and test results, enables more informed treatment decisions
- **Enhanced Care Coordination**: Multiple providers can access and contribute to the same patient record, improving care continuity across specialties and care settings
- **Reduced Medical Errors**: Complete visibility into patient allergies, current medications, and medical history helps prevent adverse events and medication interactions
- **Increased Efficiency**: Electronic documentation and search capabilities reduce time spent locating and reviewing patient information
- **Better Documentation**: Structured templates and standardized formats ensure consistent, complete, and legible clinical documentation

**For Healthcare Organizations:**
- **Regulatory Compliance**: Electronic records facilitate compliance with healthcare regulations, quality reporting, and accreditation requirements
- **Operational Efficiency**: Reduced reliance on paper records, improved workflow, and faster information retrieval
- **Cost Reduction**: Decreased costs associated with paper storage, transcription, and record retrieval
- **Data Analytics**: Structured electronic data enables population health analysis, quality metrics, and clinical research
- **Risk Management**: Complete documentation and audit trails support risk management and legal protection

**For Patients:**
- **Improved Care Quality**: Comprehensive records enable providers to deliver more personalized and effective care
- **Reduced Redundancy**: Information sharing reduces the need for patients to repeat medical history at each visit
- **Safety**: Allergy and medication information helps prevent adverse reactions and medication errors
- **Continuity of Care**: Seamless information sharing across providers ensures consistent care

#### 2.1.3 Key Capabilities
The Patient Health Records feature encompasses the following core capabilities:

1. **Patient Registration and Demographics Management**
   - Initial patient registration with comprehensive demographic information
   - Patient search and retrieval across multiple identifiers
   - Demographic information updates with audit trails
   - Emergency contact and insurance information management

2. **Comprehensive Medical History Documentation**
   - Past medical history including chronic conditions and previous diagnoses
   - Family medical history with relationship mapping
   - Social history including lifestyle factors and risk behaviors
   - Immunization history with complete vaccination records

3. **Clinical Documentation**
   - Multiple note types (SOAP notes, progress notes, consultation notes, discharge summaries)
   - Template-based documentation for efficiency and consistency
   - Rich text formatting and voice-to-text capabilities
   - Electronic signatures and version control

4. **Vital Signs and Clinical Measurements**
   - Real-time vital signs recording (blood pressure, heart rate, temperature, etc.)
   - Trend visualization and graphical displays
   - BMI calculation and growth tracking
   - Integration with medical devices where applicable

5. **Diagnosis and Problem Management**
   - Active and resolved problem lists
   - ICD-10/ICD-11 diagnosis coding
   - Diagnosis status tracking and resolution dates
   - Problem list prioritization and categorization

6. **Laboratory and Diagnostic Results**
   - Laboratory test ordering and result management
   - LOINC-coded test results with reference ranges
   - Abnormal value flagging and critical result alerts
   - Trend analysis and graphical displays
   - Imaging study management and report storage

7. **Allergy and Adverse Reaction Tracking**
   - Comprehensive allergy documentation
   - Reaction severity and type classification
   - Allergy verification status
   - Integration with prescription system for medication safety

8. **Medication History Integration**
   - Current and historical medication lists
   - Medication start and stop dates
   - Prescribing provider information
   - Integration with prescription management feature

9. **Record Viewing and Reporting**
   - Comprehensive patient summary/dashboard
   - Chronological timeline of encounters and events
   - Searchable and filterable record views
   - Print and export capabilities

#### 2.1.4 User Workflows
The Patient Health Records feature supports several key clinical workflows:

**New Patient Registration Workflow:**
1. Patient arrives at healthcare facility
2. Administrative staff registers patient with demographic information
3. System assigns unique Medical Record Number (MRN)
4. Initial medical history and allergy information collected
5. Patient record created and available for clinical use
6. Where hospital policy requires it, a **patient identity card** is issued or printed (MRN-linked; optional issuance fee). See **[Patient identity card – registration](patient-identity-card-registration.md)** for detailed requirements.

**Patient Visit Workflow:**
1. Provider searches for and retrieves patient record
2. System displays comprehensive patient summary
3. Provider reviews patient history, current medications, and allergies
4. Provider documents current visit (chief complaint, examination findings)
5. Vital signs recorded by clinical staff
6. Provider creates clinical note (SOAP format or other template)
7. Diagnoses added or updated in problem list
8. Orders placed for laboratory tests or imaging (if applicable)
9. Visit documentation completed and electronically signed

**Ongoing Care Management Workflow:**
1. Provider reviews patient record before scheduled visit
2. System displays recent test results, medication changes, and care history
3. Provider updates problem list based on current status
4. Provider documents care plan and follow-up instructions
5. Patient record updated with latest information

**Results Review Workflow:**
1. Laboratory or imaging results received in system
2. System flags abnormal or critical values
3. Provider reviews results in context of patient history
4. Provider documents interpretation and plan
5. Results integrated into patient record

#### 2.1.5 Integration Points
The Patient Health Records feature integrates with several other system components and external systems:

- **Prescription Management**: Shares medication history and allergy information; receives updates when prescriptions are created or discontinued
- **Laboratory Information Systems (LIS)**: Receives laboratory test orders and results electronically
- **Radiology/PACS Systems**: Receives imaging orders and reports; may integrate with DICOM for image viewing
- **Pharmacy Systems**: May receive medication history and allergy information for prescription verification
- **Health Information Exchanges (HIE)**: Shares patient information with other healthcare organizations (future enhancement)
- **Billing Systems**: May share diagnosis and procedure information for claims processing (future enhancement)

#### 2.1.6 Data Lifecycle
Patient health records follow a comprehensive data lifecycle:

1. **Creation**: Records created during patient registration and initial visit
2. **Accumulation**: Information added through ongoing care encounters, test results, and documentation
3. **Maintenance**: Records updated as patient information changes, diagnoses resolve, and medications change
4. **Retention**: Records maintained according to legal and regulatory requirements (typically 6-10 years after last encounter or patient age of majority)
5. **Archival**: Historical records archived for long-term storage while maintaining accessibility
6. **Disposal**: Records disposed of securely when retention period expires (if applicable)

#### 2.1.7 Security and Privacy Considerations
The Patient Health Records feature handles highly sensitive Protected Health Information (PHI) and must implement robust security measures:

- **Access Control**: Role-based access ensures users only access information appropriate to their role
- **Audit Logging**: All access to and modifications of patient records are logged with user identification and timestamp
- **Data Encryption**: All PHI encrypted at rest and in transit
- **Minimum Necessary**: System supports access controls that limit information display to minimum necessary for user's role
- **Patient Privacy**: System supports patient privacy preferences and restrictions on information sharing
- **Breach Prevention**: Security measures prevent unauthorized access, data breaches, and information disclosure

#### 2.1.8 Compliance and Standards
The Patient Health Records feature must comply with:

- **HIPAA Privacy and Security Rules**: Protection of PHI and patient privacy rights
- **Clinical Documentation Standards**: Adherence to medical documentation best practices
- **Coding Standards**: Support for ICD-10/ICD-11, LOINC, SNOMED CT, and other standard terminologies
- **Interoperability Standards**: HL7 FHIR for data exchange with other systems
- **State and Federal Regulations**: Compliance with jurisdiction-specific healthcare regulations
- **Accreditation Standards**: Support for Joint Commission, NCQA, and other accreditation requirements

#### 2.1.9 Success Metrics
The success of the Patient Health Records feature will be measured by:

- **Adoption Rate**: Percentage of providers actively using the system for documentation
- **Documentation Completeness**: Percentage of patient encounters with complete documentation
- **Time to Access**: Average time to retrieve and display patient records
- **Data Quality**: Accuracy and completeness of recorded information
- **User Satisfaction**: Provider and staff satisfaction scores with the system
- **Error Reduction**: Reduction in medical errors related to incomplete or inaccessible information
- **Compliance**: Successful passing of HIPAA audits and regulatory inspections

### 2.2 Functional Requirements

#### 2.2.1 Patient Registration and Demographics

##### 2.2.1.1 Patient Registration

- **FR-1.1**: System shall allow registration of new patients with comprehensive demographic and administrative information. The registration process shall capture the following mandatory information:
  - **Personal Identification**:
    - Legal first name (required)
    - Legal last name (required)
    - Middle name or initial (optional)
    - Preferred name/nickname (optional)
    - Date of Birth (required, format: MM/DD/YYYY or YYYY-MM-DD)
    - Gender (required, options: Male, Female, Other, Prefer not to answer, with ability to specify if "Other")
    - Sex assigned at birth (optional, for clinical purposes)
    - Social Security Number (SSN) or National ID (optional, but recommended for identity verification)
    - Race and Ethnicity (optional, for reporting and quality measures)
    - Marital status (optional)
  
  - **Contact Information**:
    - Primary address (required):
      - Street address (Line 1 and Line 2)
      - City (required)
      - State/Province (required)
      - ZIP/Postal code (required)
      - Country (required, default to system default)
    - Mailing address (if different from primary address)
    - Primary phone number (required, with type: Home, Work, Mobile, Other)
    - Secondary phone number (optional, with type)
    - Primary email address (optional, validated format)
    - Secondary email address (optional, validated format)
    - Preferred method of contact (Phone, Email, Mail, Text Message)
    - Consent for text messaging (Yes/No)
    - Consent for email communication (Yes/No)
  
  - **Emergency Contact Information** (at least one required):
    - Emergency contact name (required)
    - Relationship to patient (required, dropdown: Spouse, Parent, Child, Sibling, Friend, Other)
    - Emergency contact phone number (required, primary)
    - Emergency contact phone number (optional, secondary)
    - Emergency contact address (optional)
    - Emergency contact email (optional)
  
  - **Insurance Information**:
    - Primary insurance (optional but recommended):
      - Insurance company name (required if insurance provided)
      - Insurance type (Commercial, Medicare, Medicaid, Tricare, Self-Pay, Other)
      - Policy/Subscriber ID (required if insurance provided)
      - Group number (optional)
      - Subscriber name (if different from patient)
      - Subscriber date of birth (if different from patient)
      - Subscriber relationship to patient (Self, Spouse, Child, Other)
      - Policy effective date (optional)
      - Policy expiration date (optional)
      - Copay amount (optional)
      - Insurance phone number (optional)
    - Secondary insurance (optional):
      - Same fields as primary insurance
    - Tertiary insurance (optional):
      - Same fields as primary insurance
    - Self-pay designation (if no insurance)
    - Insurance verification status (Verified, Pending, Not Verified, Not Applicable)
    - Insurance verification date (optional)
  
  - **Clinical Assignment**:
    - Primary care physician/provider assignment (optional, can be assigned later)
    - Primary care location/facility (optional)
    - Referring physician (optional)
    - Patient status (Active, Inactive, Deceased, Archived)
  
  - **Clinical Information** (collected during registration or initial visit):
    - Known allergies and adverse reactions (optional, can be added later)
    - Preferred language (required, default to system default language)
    - Interpreter needed (Yes/No)
    - Communication preferences (English, Spanish, Other with specification)
    - Preferred communication method for appointment reminders
    - Special needs or accommodations (optional, free text)
  
  - **Administrative Information**:
    - Registration date and time (auto-populated)
    - Registered by (user ID, auto-populated)
    - Registration location/facility (auto-populated based on user location)
    - Patient consent forms acceptance (Yes/No, with date)
    - HIPAA acknowledgment (Yes/No, with date and signature)
    - Financial responsibility acknowledgment (Yes/No, with date)
    - Marketing consent (Yes/No, optional)

- **FR-1.2**: System shall automatically generate a unique Medical Record Number (MRN) for each new patient registration. The MRN shall:
  - Be unique across the entire system
  - Follow a consistent format (configurable by organization)
  - Be non-sequential for privacy purposes (if required)
  - Be displayed prominently on all patient record views
  - Not be reused even if patient record is deactivated or archived

- **FR-1.3**: System shall perform duplicate patient detection during registration by checking:
  - Exact match on first name, last name, and date of birth
  - Exact match on SSN/National ID (if provided)
  - Similar name match with same date of birth (fuzzy matching)
  - Same phone number with matching name
  - Same email address with matching name
  - System shall display potential duplicate matches to registration staff
  - System shall allow registration staff to:
    - Review potential duplicate records
    - Confirm if match is the same patient (merge records)
    - Confirm if match is a different patient (proceed with new registration)
    - Override duplicate warning with reason documented (if necessary)

- **FR-1.4**: System shall validate all data entered during patient registration with the following specific validation rules:
  - **Date of Birth Validation**:
    - Must be a valid calendar date (cannot be invalid dates like February 30)
    - Cannot be in the future
    - Age must be between 0 and 150 years (warn if age < 0 or > 120, require confirmation if age > 150)
    - Cannot be more than 9 months in the future from current date (to account for pregnancy registration)
    - Date format validation: MM/DD/YYYY or YYYY-MM-DD format required
    - Leap year validation (February 29 only valid in leap years)
  
  - **Name Validation**:
    - First name: Required, 1-100 characters, alphanumeric and common special characters (hyphens, apostrophes, spaces) allowed
    - Last name: Required, 1-100 characters, alphanumeric and common special characters allowed
    - Middle name: Optional, 1-100 characters if provided
    - Preferred name: Optional, 1-100 characters if provided
    - Names cannot be all numeric
    - Names cannot contain only special characters
    - Names cannot contain prohibited characters (e.g., <, >, /, \, |, {, }, [, ], @, #, $, %, ^, &, *)
  
  - **Email Address Validation**:
    - Must follow RFC 5322 email format standard
    - Must contain @ symbol with valid domain
    - Local part (before @): 1-64 characters, alphanumeric and allowed special characters (. + - _)
    - Domain part (after @): Valid domain format with at least one dot, valid TLD
    - Cannot contain spaces
    - Cannot start or end with dot or special characters
    - Case-insensitive validation
  
  - **Phone Number Validation**:
    - US format: Exactly 10 digits (with or without formatting: (XXX) XXX-XXXX, XXX-XXX-XXXX, or XXXXXXXXX)
    - International format: Configurable by country (typically 7-15 digits)
    - Cannot contain letters (except extensions like "ext" or "x")
    - Cannot be all zeros or all same digit
    - Area code validation for US (cannot start with 0 or 1, cannot be 555 for non-fictional numbers)
    - Extension format: Optional, if provided must be numeric or "ext" followed by numbers
  
  - **ZIP/Postal Code Validation**:
    - US ZIP: 5 digits (12345) or 9 digits with hyphen (12345-6789)
    - Canadian Postal Code: Format A1A 1A1 (letter-number-letter space number-letter-number)
    - International: Format validation based on selected country
    - Must match format for selected state/province and country
    - Cannot be all zeros
  
  - **SSN Validation** (if provided):
    - US format: XXX-XX-XXXX (9 digits with hyphens) or XXXXXXXXX (9 digits)
    - Cannot be all zeros
    - Cannot start with 000, 666, or 900-999 (invalid SSN ranges)
    - Cannot be 123-45-6789 (test number)
    - Must be exactly 9 digits
  
  - **Address Validation**:
    - Street address Line 1: Required, 1-200 characters, cannot be all spaces or special characters
    - Street address Line 2: Optional, 1-200 characters if provided
    - City: Required, 1-100 characters, alphanumeric and common characters allowed
    - State/Province: Required, must be from valid list for selected country
    - Country: Required, must be from valid country list
    - Address cannot be PO Box only (if PO Box, must be in Line 1 or Line 2, not both)
  
  - **Required Fields Validation**:
    - System shall prevent registration completion if mandatory fields are missing
    - System shall display list of missing required fields
    - System shall highlight missing required fields visually (red border, asterisk, etc.)
  
  - **Error Handling**:
    - System shall display clear, specific error messages for each validation failure
    - Error messages shall indicate which field failed and why
    - Error messages shall suggest corrections when possible
    - System shall highlight invalid fields visually (red border, error icon)
    - System shall prevent form submission until all validation errors are resolved
    - System shall maintain entered data when validation fails (don't clear form)

- **FR-1.5**: System shall support patient registration through multiple methods:
  - Manual entry by administrative staff
  - Import from external systems (with data mapping and validation)
  - Patient self-registration through portal (if portal feature is available)
  - Registration via API integration with other systems

##### 2.2.1.2 Patient Search and Retrieval

- **FR-1.6**: System shall support comprehensive patient search and retrieval functionality with the following search criteria:
  - **Primary Search Methods**:
    - Medical Record Number (MRN) - exact match
    - Full name (first and last) - partial or exact match
    - Last name only - partial or exact match
    - First name only - partial or exact match
    - Date of Birth - exact match or range
    - Social Security Number (SSN) - exact match (with appropriate security restrictions)
    - Phone number - partial or exact match
    - Email address - exact match
    - Insurance policy number - partial or exact match
  
  - **Advanced Search Options**:
    - Combination of multiple criteria (e.g., name + DOB, phone + name)
    - Search by provider assignment
    - Search by registration date range
    - Search by patient status (Active, Inactive, Deceased)
    - Search by insurance company
    - Search by address (city, state, ZIP code)
    - Search by date of last visit/encounter

- **FR-1.7**: System shall provide search results with the following capabilities:
  - Display search results in a list format showing:
    - Patient name
    - Date of Birth
    - Age (calculated)
    - MRN
    - Last visit date
    - Primary care provider
    - Patient status
  - Support pagination for large result sets
  - Allow sorting of results by name, DOB, last visit date, MRN
  - Highlight exact matches vs. partial matches
  - Display number of results found
  - Allow selection of patient from results to open full record
  - Support "Quick Search" for frequently accessed patients (recent patients, favorites)

- **FR-1.8**: System shall implement search security and privacy controls:
  - Require user authentication before allowing search
  - Log all patient searches with user ID, timestamp, and search criteria
  - Enforce role-based access - some users may have limited search capabilities
  - Mask sensitive information (SSN, full address) in search results based on user role
  - Require additional authentication for SSN-based searches (if applicable)
  - Limit number of search results returned (configurable, default 100)

- **FR-1.9**: System shall support patient lookup by scanning:
  - Barcode on patient ID card (if barcode contains MRN)
  - QR code on patient documents
  - Integration with patient ID card scanners

##### 2.2.1.3 Patient Demographic Updates

- **FR-1.10**: System shall allow authorized users to update patient demographic information with the following requirements:
  - All demographic fields shall be editable (except MRN, which is permanent)
  - System shall require user authentication before allowing updates
  - System shall maintain complete audit trail of all changes including:
    - Field name that was changed
    - Previous value
    - New value
    - User who made the change
    - Date and time of change
    - Reason for change (optional, but recommended for significant changes)
    - IP address or location of change (if available)
  
  - System shall validate all updated data using same validation rules as registration
  - System shall prevent updates to certain critical fields without appropriate authorization (e.g., SSN, DOB changes may require supervisor approval)
  - System shall support bulk updates for administrative purposes (e.g., address changes for multiple patients, with proper authorization)

- **FR-1.11**: System shall support patient demographic update workflows:
  - **Standard Update**: User edits field and saves changes
  - **Update with Verification**: Certain sensitive fields (SSN, DOB, name) may require verification or supervisor approval
  - **Patient-Initiated Update**: If patient portal exists, patients can request updates that require staff approval
  - **Bulk Update**: Administrative staff can update multiple patients (e.g., address change for all patients in a building)
  - **Import Update**: Updates from external systems (insurance eligibility, address verification services)

- **FR-1.12**: System shall handle special update scenarios:
  - **Name Changes**: Support legal name changes with documentation requirement
  - **Address Changes**: Support address history tracking (previous addresses)
  - **Insurance Updates**: Support insurance changes with effective dates
  - **Status Changes**: Support patient status changes (Active to Inactive, marking as Deceased)
  - **Provider Reassignment**: Support changing primary care provider assignment
  - **Merge Records**: Support merging duplicate patient records (with proper authorization and audit trail)

##### 2.2.1.4 Patient Record Management

- **FR-1.13**: System shall support patient record status management:
  - **Active**: Patient is currently receiving care or is eligible for care
  - **Inactive**: Patient has not been seen recently but record is maintained
  - **Deceased**: Patient has passed away (with date of death)
  - **Archived**: Patient record archived but maintained for legal/regulatory purposes
  - System shall allow status changes with appropriate authorization
  - System shall prevent certain actions on inactive or deceased patients (e.g., scheduling appointments)
  - System shall maintain all historical data regardless of status

- **FR-1.14**: System shall support patient record merging functionality:
  - Allow identification of duplicate patient records
  - Display side-by-side comparison of duplicate records
  - Allow selection of master record (record to keep)
  - Allow selection of data to merge from duplicate record
  - Merge all clinical data, encounters, and documents into master record
  - Deactivate duplicate record (do not delete for audit purposes)
  - Maintain complete audit trail of merge operation
  - Require supervisor or administrator authorization for merge
  - Prevent merging of records with conflicting critical data without manual review

- **FR-1.15**: System shall support patient record splitting (unmerge) functionality:
  - Allow reversal of incorrectly merged records (if done recently)
  - Restore separated records with their original data
  - Maintain audit trail of split operation
  - Require high-level authorization (administrator only)

- **FR-1.16**: System shall maintain patient record history and versioning:
  - Track all changes to patient demographics over time
  - Display change history in chronological order
  - Allow viewing of patient record as it existed at a specific point in time
  - Maintain historical addresses, phone numbers, and insurance information
  - Support "as of date" reporting for historical analysis

##### 2.2.1.5 Data Quality and Validation

- **FR-1.17**: System shall implement data quality controls:
  - **Completeness Checks**: Identify records with missing required information
  - **Accuracy Validation**: Validate data formats, ranges, and logical consistency
  - **Duplicate Detection**: Ongoing monitoring for duplicate records
  - **Data Standardization**: Standardize addresses, phone numbers, and names
  - **Address Verification**: Integration with address verification services (optional)
  - **Insurance Eligibility**: Integration with insurance eligibility verification (optional)
  - **Data Quality Reports**: Generate reports identifying data quality issues

- **FR-1.18**: System shall support data validation rules that are:
  - Configurable by organization
  - Enforced at data entry
  - Enforced during data import
  - Documented and visible to users
  - Customizable for different patient types or registration scenarios

##### 2.2.1.6 Privacy and Consent Management

- **FR-1.19**: System shall manage patient privacy preferences and consents:
  - **HIPAA Acknowledgment**: Track patient acknowledgment of privacy notice
  - **Consent Forms**: Track acceptance of various consent forms with dates
  - **Communication Preferences**: Store patient preferences for communication methods
  - **Marketing Consent**: Track opt-in/opt-out for marketing communications
  - **Research Consent**: Track consent for participation in research (if applicable)
  - **Information Sharing Restrictions**: Support patient requests to restrict information sharing
  - **Access Restrictions**: Support restrictions on who can access patient information
  - All consents shall be date-stamped and linked to user who documented consent

- **FR-1.20**: System shall support patient privacy flags and alerts:
  - **Confidentiality Flags**: Mark records as confidential or sensitive
  - **VIP Flags**: Mark records for special handling (if applicable)
  - **Security Alerts**: Alert staff to special security or privacy requirements
  - **Restricted Access**: Enforce additional access controls for flagged records
  - Flags shall be visible to authorized users and require appropriate authorization to set or modify

##### 2.2.1.7 Reporting and Analytics

- **FR-1.21**: System shall provide reporting capabilities for patient demographics:
  - **Registration Reports**: New patient registrations by date range, location, provider
  - **Demographic Reports**: Patient population statistics (age, gender, insurance, geographic distribution)
  - **Data Quality Reports**: Missing data, duplicate records, validation errors
  - **Status Reports**: Active vs. inactive patients, deceased patients
  - **Insurance Reports**: Insurance coverage breakdown, uninsured patients
  - **Provider Assignment Reports**: Patients by primary care provider
  - **Audit Reports**: All changes to patient demographics with user and timestamp
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.1.8 Integration Requirements

- **FR-1.22**: System shall support integration with external systems for patient data:
  - **Master Patient Index (MPI)**: Integration with enterprise MPI for duplicate detection across systems
  - **Address Verification Services**: Real-time address validation and standardization
  - **Insurance Eligibility Services**: Real-time insurance verification
  - **Identity Verification Services**: Patient identity verification (if applicable)
  - **Health Information Exchange (HIE)**: Share and receive patient demographic data
  - **Registration Kiosks**: Integration with self-service registration kiosks
  - **Patient Portal**: Integration with patient portal for self-service updates
  - All integrations shall maintain data security and audit trails

##### 2.2.1.9 Error Handling and Recovery

- **FR-1.23**: System shall implement comprehensive error handling for patient registration and demographic management:
  - **Validation Error Handling**:
    - Display clear, specific error messages for each validation failure
    - Error messages shall indicate which field failed and why
    - Error messages shall suggest corrections when possible (e.g., "Phone number must be 10 digits. Please enter as XXX-XXX-XXXX")
    - System shall highlight invalid fields visually (red border, error icon, asterisk)
    - System shall prevent form submission until all validation errors are resolved
    - System shall maintain entered data when validation fails (don't clear form)
    - System shall group related errors together for better user experience
    - System shall provide inline validation feedback (real-time or on blur)
    - System shall display error count (e.g., "3 errors found")
    - System shall allow user to navigate to next error field
  
  - **System Error Handling**:
    - **Network Errors**:
      - Detect network connectivity issues
      - Display user-friendly message: "Unable to connect to server. Please check your internet connection and try again."
      - Provide retry mechanism with automatic retry option
      - Save form data locally to prevent data loss
      - Allow offline data entry with sync when connection restored
      - Log network errors with timestamp and user ID
    
    - **Database Errors**:
      - Handle database connection failures gracefully
      - Display message: "System temporarily unavailable. Please try again in a few moments."
      - Prevent data loss by maintaining form state
      - Log database errors with error code and details (for administrators)
      - Provide fallback mechanisms when possible
      - Alert system administrators of database issues
    
    - **Server Errors**:
      - Handle 500-level HTTP errors
      - Display generic user-friendly message: "An unexpected error occurred. Your data has been saved. Please contact support if the problem persists."
      - Log detailed error information for troubleshooting
      - Include error reference number for user to report issue
      - Provide "Report Problem" functionality
    
    - **Timeout Errors**:
      - Detect and handle request timeouts
      - Display message: "Request timed out. Please try again."
      - Provide option to extend timeout for long operations
      - Save progress before timeout occurs
  
  - **Integration Error Handling**:
    - **External System Failures**:
      - Handle failures from address verification services
      - Handle failures from insurance eligibility services
      - Handle failures from MPI services
      - Display appropriate message: "Unable to verify [service name] at this time. You may proceed with manual entry."
      - Allow manual override when external services unavailable
      - Queue requests for retry when service recovers
      - Log integration errors with service name and error details
    
    - **Data Import Errors**:
      - Validate imported data before processing
      - Report import errors with specific row/record numbers
      - Provide import error summary report
      - Allow partial import (successful records) with error report
      - Provide import error log for review
      - Support error correction and re-import
  
  - **Data Consistency Error Handling**:
    - **Duplicate Detection Errors**:
      - Handle errors during duplicate patient detection
      - Display duplicate matches even if detection service fails
      - Allow manual duplicate review when automatic detection unavailable
      - Log duplicate detection failures
    
    - **Concurrent Update Errors**:
      - Detect when patient record is being edited by another user
      - Display message: "This record was modified by [User Name] at [Time]. Please refresh to see latest changes."
      - Provide option to refresh and see changes
      - Prevent overwriting other user's changes
      - Show conflict resolution interface when needed
      - Maintain audit trail of concurrent update attempts
  
  - **Security and Authorization Error Handling**:
    - **Authentication Errors**:
      - Handle session expiration gracefully
      - Display message: "Your session has expired. Please log in again."
      - Save form data before redirecting to login
      - Restore form data after successful re-authentication

##### 2.2.1.9a Patient Identification Card (Plastic Printed Card)

- **FR-1.23a**: The system shall support creation and printing of a **plastic patient identification card** for every registered patient, with the following requirements:
  - **Card Issuance**:
    - Card shall be issuable at or after patient registration (configurable: mandatory at registration, optional, or on-demand).
    - One primary card per patient; reprints allowed for lost or damaged cards with audit logging.
  - **Card Content** (configurable per organization):
    - **Mandatory**: Patient name, Medical Record Number (MRN) / Patient ID.
    - **Recommended**: Barcode or QR code encoding MRN (for scanning per FR-1.9).
    - **Optional**: Patient photo, date of birth, blood group, contact number, hospital/facility name and logo.
  - **Card Format**:
    - Designed for **plastic card printing** (standard dimensions, e.g., CR80/ISO/IEC 7810 ID-1: 85.6 × 53.98 mm, or organization-defined).
    - Print-ready layout suitable for plastic card printers or external card production.
    - Support for both sides (front: demographics; back: terms, barcode, or additional info as configured).
  - **Printing**:
    - Print card from patient record (single) or batch print for multiple patients.
    - Reprint with full audit log (who, when, reason if captured).
    - Preview before print.
  - **Integration**:
    - Barcode/QR on card shall encode MRN or Patient ID to support patient lookup by scanning (FR-1.9).
    - Card design/layout shall be configurable (templates, logo, fields shown) per facility or organization.

##### 2.2.1.10 Admission-Embedded Patient Registration (Hospital Context)

- **FR-1.24**: For the **Admission/IPD module**, the system shall provide an admission-embedded **New Patient Registration** capability with the following behavior:
  - Admission users can launch a **modal registration form** directly from the Admission screen.
  - On successful save:
    - System auto-generates a unique **Patient ID / Registration Number** (re-using the existing MRN pattern where appropriate).
    - The modal closes (or remains open as configured), and the newly registered patient data is **returned and auto-populated** into the Admission form.
  - Mandatory admission-ready demographic capture must include at minimum:
    - Patient Name.
    - Gender.
    - Age or Date of Birth.
    - Mobile Number.
    - Present Address.
  - If registration is cancelled, no partial patient record is committed and Admission form remains unchanged.

- **FR-1.25**: For **Existing Patient Search from Admission**, the system shall:
  - Provide a patient search panel within the Admission workflow supporting search by:
    - Patient ID / MRN.
    - Registration Number (if distinct from MRN).
    - Mobile Number.
    - Patient Name (full or partial).
  - Display a results list with at least the following columns:
    - Registration / Patient ID.
    - Patient Name.
    - Mobile Number.
    - Date of Birth.
    - Age (calculated).
    - Gender.
    - Father’s Name (where captured).
    - Status (e.g., Active/Inactive/Deceased).
  - On selecting a patient from the results:
    - Auto-fill core demographics into the Admission form.
    - Clearly indicate if the patient currently has an **active admission**.
  - Enforce existing duplicate-detection rules (FR-1.3) and prevent inadvertent creation of duplicate patients from the Admission workflow.

- **FR-1.26**: The system shall support additional **hospital-specific demographic attributes** required by Admission and related operational flows:
  - Basic information (in addition to FR-1.1 fields):
    - Father’s Name.
    - Mother’s Name.
    - Spouse Name.
    - Religion (dropdown).
    - Occupation.
    - Nationality (dropdown).
    - Blood Group (dropdown).
    - Patient Type (e.g., General, Corporate, Insurance, Staff, Other – dropdown, configurable).
    - Introduced By (dropdown – marketing/referral or staff referrer).
    - Referred By (free text or lookup, as configured).
    - Remarks (free text).
  - Identification:
    - National ID Number (e.g., NID).
    - Birth Certificate Number.
    - Passport Number.
  - The system shall allow configuration of which of the above fields are:
    - Mandatory for Admission workflows.
    - Optional or hidden in other contexts (e.g., outpatient only).

- **FR-1.27**: The system shall provide a **Patient Registration List View** optimized for Admission/front-desk operations with:
  - Default columns (configurable):
    - Reg No / Patient ID.
    - Patient Name.
    - Mobile / Telephone Number.
    - Date of Birth.
    - Age.
    - Gender.
    - Father’s Name.
    - Present Address (shortened).
    - Patient Type.
    - Status (e.g., Active/Inactive).
    - Amount/Outstanding (where integrated with Billing).
    - Remarks.
  - Support for:
    - Sorting and filtering by key fields (Name, Mobile, Reg No, Patient Type, Status).
    - Quick actions per row (e.g., **View / Edit / Start Admission**).
    - Optional action columns such as **Invoice**, **Edit**, **Delete**, governed by role/permission.

- **FR-1.28**: The system shall enforce the following **business rules** relating patient registration to Admission:
  - Admission cannot proceed without selecting or creating a valid **Patient ID / MRN**.
  - **Gender** and **Age (or Date of Birth)** are mandatory for any Admission.
  - **Mobile Number** uniqueness shall be configurable:
    - Option 1: Soft-unique – warn on duplicates and allow override with reason.
    - Option 2: Hard-unique – prevent multiple active patients with the same mobile number.
  - For IPD:
    - Only **one active admission per patient** is allowed at a time, unless configuration explicitly permits multiple concurrent admissions (e.g., multi-facility scenarios).
    - Attempting to create a new admission for a patient with an existing active admission shall trigger a clear warning and follow configured policy (block or allow with override/approval).

- **FR-1.29**: The system shall support the following **system actions and behaviors** in Admission-related registration flows:
  - Automatically calculate and display **Age** from Date of Birth wherever Age is shown (e.g., list views, Admission screen).
  - Auto-populate all available demographic information into Admission once a patient is selected from search or after new registration.
  - Provide configuration to:
    - **Lock selected demographic fields** (e.g., name, DOB, gender, national ID) once a patient has an active admission, allowing edits only via elevated permission or approval.
    - Allow limited in-place updates for non-critical fields (e.g., address, contact details) during admission with full audit trail.
      - Prevent unauthorized access attempts
      - Log failed authentication attempts

##### 2.2.1.11 IPD Admission List View & Bed Management Integration

- **FR-1.30 Admission List View – Fields**
  - The **Admission/IPD module** shall provide a list view that exposes, at minimum, the following logical groups and fields (subject to configuration and localization):
  - Registration & Patient Information:
    - Registration No (Reg No / Patient ID) – required.
    - Patient Name – required.
    - Gender (dropdown) – required.
    - Age – required.
    - Patient Type (dropdown; e.g., General, Corporate, Insurance, Staff) – required.
    - Package (dropdown) and Package Name (where package-based admission is used).
    - Mobile No – required.
    - Address (short / display version).
    - Occupation.
    - Religion (dropdown).
  - Bed Information:
    - Bed No – required for IPD admission.
    - Bed Charge (default tariff derived from bed configuration).
    - Admission Charge (where configured).
    - Relatives / Relation (dropdown or lookup, as per hospital policy).
  - Guardian Information:
    - Guardian Name – required.
    - Relation – required.
    - Phone – required.
  - Doctor & Clinical Information:
    - Ref. By (Referring doctor / source) – required if configured.
    - Under Dr. (Primary consultant) – required.
    - Operation Name (where pre-planned OT booking exists).
    - Department (admitting department).
  - Corporate / Package / Financial Information:
    - Corporate Client (dropdown, where applicable).
    - Corporate Payment Type (dropdown, where applicable).
    - Package Feature indicator (if admission is package-based).
    - Attached ID / MR ID / Attached Room (where relevant to corporate or special schemas).
  - Transport & Miscellaneous:
    - Ambulance flag (whether ambulance was used).
    - Assist 1, Assist 2, Driver Name (where ambulance / transport is logged).
    - Meter From, Meter To (for transport billing where applicable).
    - Remarks (free text).
    - Patient Source.

- **FR-1.31 Admission List View – Column Layout**
  - The system shall support a configurable **column layout** for the Admission/IPD list view with default columns including at least:
    - Id.
    - Patient Id.
    - Patient Name.
    - Admission Date.
    - Gender.
    - Under Dr Code / Under Dr.
    - Bed No.
    - UserName (user who created or last updated the admission).
    - Action columns:
      - Invoice.
      - Edit.
      - Delete (subject to permission and audit policies).
  - Columns shall support:
    - Sorting and filtering on key fields (e.g., Admission Date, Patient Name, Bed No, Under Dr, Patient Type).
    - Show/hide configuration per role where feasible.

- **FR-1.32 Admission Pre-requisite Summary**
  - The system shall enforce and/or guide operators regarding the following **pre-requisites** for Admission:
  - Module pre-requisites:
    - Patient Registration:
      - Required – admission cannot proceed without a valid registered patient.
      - Reason: Patient identification and demographic master.
    - Bed Management (Ward/Room/Bed configuration from `hospital-operations.md`):
      - Required – admission requires allocation to a valid bed (for IPD).
      - Reason: IPD allocation and bed/room billing.
    - Doctor Management:
      - Required – admission must be under at least one valid doctor.
      - Reason: Assigning a patient to any doctor / admitting consultant.
    - Department Management:
      - Required – admission must be mapped to a valid department.
      - Reason: Department-wise admission categorization and reporting.
    - Corporate Feature (B2B / corporate configuration):
      - Optional – used only when patient is corporate/insurance.
      - Reason: Corporate billing and contract tariffs.
    - Package Feature:
      - Optional – used only when admission is package-based.
      - Reason: Package admission (e.g., surgery/maternity packages).

- **FR-1.33 Admission Entry Gate Logic**
  - The Admission menu / entry shall follow the below high-level **gate logic**:
    - Step 1 – Admission Menu Click:
      - User opens the Admission/IPD entry screen from the main menu.
    - Step 2 – Check Patient Exists:
      - If **patient does not exist** in the system:
        - Open **Patient Registration modal** (Admission-embedded registration as per FR-1.24).
        - On successful save:
          - Return newly generated Patient ID / Registration Number.
          - Auto-fill patient data into the Admission form.
      - If **patient exists**:
        - Allow selection via patient search (FR-1.25).
        - Auto-fill patient demographics into Admission.
    - Step 3 – Check Bed Availability (for IPD admissions):
      - System validates availability of the selected **Bed No** against Bed Management rules:
        - Bed must be in **Available** (or Reserved, as per configuration) status.
        - Prevent double allocation of the same bed to more than one active admission.
        - Allow shared categories (e.g., Shared Cabin, NICU, PICU, ICU, CCU, Ward) only as per configured sharing rules.
      - If no suitable bed is available:
        - System blocks save and displays appropriate error or guidance.
    - Step 4 – Proceed to Admission Save:
      - On successful validation:
        - Admission is saved.
        - Bed status(es) are updated (e.g., to **Occupied**) in Bed Management.
        - Bed charge configuration is auto-linked to the patient’s IPD billing account (see `billing.md`).
    
    - **Authorization Errors**:
      - Handle insufficient permissions gracefully
      - Display message: "You do not have permission to perform this action. Please contact your administrator."
      - Log authorization failures with user ID and attempted action
      - Provide clear explanation of required permissions
      - Prevent data exposure in error messages
  
  - **Error Recovery Mechanisms**:
    - **Auto-Save Functionality**:
      - Automatically save form data periodically (every 30-60 seconds)
      - Save form data before navigation away from page
      - Restore auto-saved data if error occurs
      - Allow user to recover unsaved work
      - Display "Draft saved" confirmation
    
    - **Transaction Rollback**:
      - Roll back database transactions on error
      - Maintain data integrity on partial failures
      - Restore previous state when update fails
      - Prevent orphaned records
    
    - **Retry Mechanisms**:
      - Provide automatic retry for transient errors (network, timeout)
      - Limit retry attempts (max 3 retries)
      - Provide manual retry option for user
      - Use exponential backoff for retries
      - Display retry status to user
  
  - **Error Logging and Reporting**:
    - **Error Logging**:
      - Log all errors with timestamp, user ID, action, and error details
      - Log errors with appropriate severity level (Info, Warning, Error, Critical)
      - Include stack traces for system errors (in development/staging)
      - Exclude sensitive data (passwords, SSN) from logs
      - Maintain error logs for compliance and troubleshooting
    
    - **Error Reporting**:
      - Provide error reporting interface for users
      - Include error reference number in user-visible messages
      - Allow users to report errors with context
      - Generate error reports for administrators
      - Track error frequency and patterns
      - Alert administrators of critical errors
  
  - **User-Friendly Error Messages**:
    - Use plain language, avoid technical jargon
    - Provide actionable guidance (what user should do)
    - Use consistent error message format
    - Display errors in user's preferred language
    - Provide help links or documentation for complex errors
    - Use appropriate tone (helpful, not accusatory)
    - Group related errors together
    - Prioritize critical errors

#### 2.2.2 Medical History Management

##### 2.2.2.1 Past Medical History

- **FR-2.1**: System shall allow recording and maintenance of comprehensive past medical history including:
  - **Chronic Conditions and Diagnoses**:
    - Condition/diagnosis name (required, with ICD-10/ICD-11 code support)
    - Date of onset (optional, can be approximate: year, month/year, or exact date)
    - Date of resolution (if resolved, optional)
    - Status (Active, Resolved, Chronic, Controlled, In Remission)
    - Severity/Stage (if applicable, e.g., Stage 1, Stage 2, Mild, Moderate, Severe)
    - Treatment received (optional, free text or structured)
    - Diagnosing provider/facility (optional)
    - Notes/comments (optional, free text)
    - Source of information (Patient reported, Medical records, Family member, Other)
  
  - **Surgical History**:
    - Procedure name (required, with CPT code support)
    - Procedure date (required, can be approximate)
    - Performing surgeon/facility (optional)
    - Procedure location/anatomical site (optional)
    - Indication/reason for surgery (optional)
    - Complications (optional, Yes/No with details)
    - Outcome (Successful, Partial success, Complications, Unknown)
    - Notes/comments (optional, free text)
    - Source of information (Patient reported, Medical records, Other)
  
  - **Hospitalizations**:
    - Admission date (required)
    - Discharge date (required)
    - Facility/hospital name (optional)
    - Reason for admission/primary diagnosis (required)
    - Procedures performed during hospitalization (optional)
    - Complications (optional)
    - Discharge disposition (Home, Skilled nursing facility, Rehabilitation, Other)
    - Notes/comments (optional, free text)
  
  - **Major Illnesses and Injuries**:
    - Condition/illness name (required)
    - Date of occurrence (required, can be approximate)
    - Treatment received (optional)
    - Outcome/resolution (Resolved, Chronic, Ongoing, Unknown)
    - Impact on current health (optional)
    - Notes/comments (optional, free text)
  
  - **Obstetric History** (for female patients):
    - Gravida (number of pregnancies, required)
    - Para (number of live births, required)
    - Abortions (spontaneous and induced, optional)
    - Pregnancy complications (optional)
    - Delivery dates and types (Vaginal, C-section, optional)
    - Birth weights (optional)
    - Pregnancy outcomes (optional)
  
  - **Gynecological History** (for female patients):
    - Menstrual history (menarche age, cycle regularity, last menstrual period)
    - Menopause status (if applicable)
    - Gynecological procedures (optional)
    - Contraceptive history (optional)

- **FR-2.2**: System shall support structured data entry for past medical history:
  - Dropdown lists for common conditions (with ability to add custom entries)
  - ICD-10/ICD-11 code lookup and auto-population
  - Template-based entry for common history types
  - Copy/paste functionality from previous encounters
  - Bulk import from external systems or documents
  - Voice-to-text entry support
  - Date picker with support for approximate dates (year only, month/year, full date)

- **FR-2.3**: System shall maintain history of all past medical history entries:
  - Track when each item was added
  - Track when items were modified or updated
  - Track who added or modified each item (user ID)
  - Maintain version history of changes
  - Support deletion with reason and audit trail
  - Display "Last updated" date for each history item

##### 2.2.2.2 Family History

- **FR-2.4**: System shall allow recording and maintenance of comprehensive family medical history with relationship mapping:
  - **Family Member Information**:
    - Relationship to patient (required, dropdown: Mother, Father, Sibling, Grandparent, Aunt/Uncle, Cousin, Child, Other)
    - Age (current or age at death, optional)
    - Living status (Alive, Deceased, Unknown)
    - Age at death (if deceased, optional)
    - Cause of death (if deceased and known, optional)
  
  - **Family Medical Conditions**:
    - Condition/diagnosis name (required, with ICD-10/ICD-11 code support)
    - Family member(s) affected (required, can select multiple family members)
    - Age of onset (if known, optional)
    - Severity (if known, optional)
    - Treatment received (optional)
    - Notes/comments (optional, free text)
    - Source of information (Patient reported, Medical records, Family member, Other)
  
  - **Common Family History Categories**:
    - Cardiovascular diseases (heart disease, hypertension, stroke, etc.)
    - Cancer (type, age of diagnosis)
    - Diabetes (Type 1, Type 2, gestational)
    - Mental health conditions
    - Genetic conditions
    - Autoimmune diseases
    - Neurological conditions
    - Respiratory conditions
    - Kidney disease
    - Liver disease
    - Blood disorders
    - Other conditions (customizable)

- **FR-2.5**: System shall support family history documentation with:
  - Family tree visualization (optional feature)
  - Multiple conditions per family member
  - Multiple family members per condition
  - Structured templates for common family history patterns
  - Ability to mark conditions as "No known family history"
  - Ability to mark as "Family history unknown" or "Adopted - family history unavailable"

- **FR-2.6**: System shall provide family history risk assessment:
  - Highlight significant family history that may impact patient care
  - Alert providers to genetic risk factors
  - Support clinical decision-making based on family history
  - Integration with genetic counseling referrals (if applicable)

##### 2.2.2.3 Social History

- **FR-2.7**: System shall allow recording and maintenance of comprehensive social history including:
  - **Tobacco Use**:
    - Smoking status (Never smoker, Current smoker, Former smoker, Unknown)
    - Type of tobacco (Cigarettes, Cigars, Pipe, Chewing tobacco, Snuff, E-cigarettes/Vaping, Other)
    - Age started smoking (if applicable, optional)
    - Age quit (if former smoker, optional)
    - Number of packs per day (if current/former smoker, optional)
    - Pack-years calculation (auto-calculated if data available)
    - Years of smoking (if applicable, optional)
    - Secondhand smoke exposure (Yes/No, optional)
    - Cessation attempts (if applicable, optional)
    - Notes/comments (optional, free text)
  
  - **Alcohol Use**:
    - Alcohol use status (Never, Occasional, Regular, Heavy, Former drinker, Unknown)
    - Frequency of use (Daily, Weekly, Monthly, Rarely, Never)
    - Quantity per occasion (number of drinks, optional)
    - Type of alcohol (Beer, Wine, Spirits, Mixed, optional)
    - Age started drinking (optional)
    - Age quit (if former drinker, optional)
    - History of alcohol-related problems (Yes/No, optional)
    - CAGE questionnaire results (if applicable, optional)
    - Notes/comments (optional, free text)
  
  - **Substance Use/Drug History**:
    - Illicit drug use status (Never, Current, Former, Unknown)
    - Types of drugs used (Marijuana, Cocaine, Heroin, Methamphetamine, Prescription drugs, Other)
    - Frequency of use (if applicable, optional)
    - Route of administration (if applicable, optional)
    - Age started (optional)
    - Age quit (if former user, optional)
    - History of substance abuse treatment (Yes/No, optional)
    - History of overdose (Yes/No, optional)
    - Notes/comments (optional, free text)
  
  - **Occupational History**:
    - Current occupation (optional, free text)
    - Previous occupations (optional, with dates)
    - Occupational hazards/exposures (optional, free text)
    - Work-related injuries (optional)
    - Disability status (Yes/No, optional)
    - Notes/comments (optional, free text)
  
  - **Lifestyle Factors**:
    - Exercise/physical activity level (Sedentary, Light, Moderate, Vigorous, Unknown)
    - Exercise frequency (times per week, optional)
    - Diet/nutrition (optional, free text or structured)
    - Sleep patterns (optional, free text)
    - Stress level (Low, Moderate, High, optional)
    - Hobbies and activities (optional, free text)
  
  - **Sexual History** (if clinically relevant):
    - Sexual activity status (optional)
    - Number of partners (optional)
    - Contraception use (optional)
    - History of sexually transmitted infections (optional)
    - Sexual orientation (optional, if patient chooses to disclose)
  
  - **Travel History** (if relevant):
    - Recent travel (within last 6-12 months, optional)
    - Travel destinations (optional)
    - Travel-related exposures (optional)
  
  - **Living Situation**:
    - Living arrangement (Alone, With family, With partner, Assisted living, Homeless, Other)
    - Housing type (House, Apartment, Other, optional)
    - Safety concerns (optional, free text)
    - Support system (optional, free text)

- **FR-2.8**: System shall support social history documentation with:
  - Structured questionnaires for common social history elements
  - Sensitivity flags for confidential information (e.g., substance use, sexual history)
  - Privacy controls to restrict access to sensitive social history
  - Integration with screening tools (e.g., CAGE for alcohol, PHQ-9 for depression)
  - Ability to update social history over time
  - Support for motivational interviewing documentation

##### 2.2.2.4 Immunization History

- **FR-2.9**: System shall allow recording and maintenance of comprehensive immunization/vaccination history:
  - **Vaccination Details**:
    - Vaccine name (required, with CVX code support - CDC Vaccine Codes)
    - Vaccine type (required, e.g., MMR, DTaP, Influenza, COVID-19, Hepatitis B, etc.)
    - Administration date (required)
    - Lot number (optional, but recommended)
    - Manufacturer (optional)
    - Route of administration (IM, SubQ, Oral, Nasal, Other)
    - Administration site (Left arm, Right arm, Left thigh, Right thigh, Other)
    - Dose number in series (e.g., 1 of 3, 2 of 3, optional)
    - Vaccine information statement (VIS) date (optional)
    - VIS version (optional)
  
  - **Provider Information**:
    - Administering provider name (optional)
    - Administering facility/location (optional)
    - Provider NPI (optional)
    - Administered by (Nurse, Physician, Pharmacist, Other)
  
  - **Reactions and Contraindications**:
    - Adverse reaction (Yes/No, optional)
    - Reaction description (if applicable, optional)
    - Contraindication to future doses (Yes/No, optional)
    - Contraindication reason (if applicable, optional)
  
  - **Immunization Status**:
    - Up-to-date status (Yes/No, auto-calculated based on age and recommended schedule)
    - Next due date (auto-calculated, optional)
    - Overdue immunizations (auto-identified, optional)
    - Exemptions (Medical, Religious, Personal belief, if applicable)
    - Exemption documentation (if applicable, optional)

- **FR-2.10**: System shall support immunization schedule management:
  - Integration with CDC/ACIP recommended immunization schedules
  - Age-appropriate immunization recommendations
  - Catch-up schedule support for patients behind on immunizations
  - Reminder alerts for upcoming or overdue immunizations
  - Support for multiple immunization schedules (pediatric, adult, travel, occupational)
  - Support for special populations (immunocompromised, pregnant, etc.)

- **FR-2.11**: System shall support immunization data import and exchange:
  - Import from state immunization registries (IIS - Immunization Information Systems)
  - Import from other EHR systems
  - Import from pharmacy systems
  - Export to state immunization registries
  - HL7 V2 and FHIR support for immunization data exchange
  - Support for HL7 VXU (Vaccination Update) messages

- **FR-2.12**: System shall provide immunization reporting capabilities:
  - Immunization status reports by patient
  - Population-level immunization coverage reports
  - Overdue immunization reports
  - Immunization administration reports
  - Support for quality measure reporting (e.g., HEDIS measures)
  - Export capabilities for public health reporting

##### 2.2.2.5 Medical History Timeline and Visualization

- **FR-2.13**: System shall provide chronological timeline view of all medical history:
  - Display all history items (past medical, family, social, immunizations) in chronological order
  - Filter timeline by history type (Past Medical, Family, Social, Immunization)
  - Filter timeline by date range
  - Display timeline in multiple views:
    - List view (chronological list)
    - Timeline view (visual timeline with dates)
    - Category view (grouped by history type)
  - Support for approximate dates (display as "circa" or with date ranges)
  - Ability to zoom in/out on timeline
  - Print/export timeline view

- **FR-2.14**: System shall support medical history search and filtering:
  - Search by keyword across all history types
  - Filter by history type (Past Medical, Family, Social, Immunization)
  - Filter by date range
  - Filter by status (Active, Resolved, etc.)
  - Filter by provider who documented
  - Advanced search with multiple criteria
  - Save frequently used search filters

##### 2.2.2.6 Medical Document Attachments

- **FR-2.15**: System shall allow attachment of medical documents to patient records:
  - **Supported Document Types**:
    - Laboratory reports (PDF, images, text files)
    - Imaging reports (PDF, DICOM files)
    - Consultation reports
    - Discharge summaries from other facilities
    - Operative reports
    - Pathology reports
    - EKG/ECG tracings
    - Other medical documents (PDF, images, text files)
  
  - **Document Metadata**:
    - Document title/description (required)
    - Document type (required, dropdown: Lab Report, Imaging Report, Consultation, Discharge Summary, Operative Report, Pathology, Other)
    - Date of document (required)
    - Source/facility (optional)
    - Provider/author (optional)
    - Category/tags (optional, for organization)
    - Notes/comments (optional)
    - Upload date and time (auto-populated)
    - Uploaded by (user ID, auto-populated)

- **FR-2.16**: System shall support document management:
  - File upload with size limits (configurable, default 10MB per file)
  - Support for multiple file formats (PDF, JPG, PNG, TIFF, DICOM, TXT, DOC, DOCX)
  - Document versioning (if document is updated)
  - Document deletion with audit trail and reason
  - Document access logging (who viewed, when)
  - Document encryption at rest
  - Document preview/display within system
  - Download capability for authorized users
  - Print capability

- **FR-2.17**: System shall support document organization and linking:
  - Link documents to specific encounters/visits
  - Link documents to specific diagnoses or problems
  - Link documents to specific procedures
  - Organize documents into folders/categories
  - Tag documents with keywords
  - Search documents by title, type, date, or content (if OCR available)
  - Display document list in patient record view

- **FR-2.18**: System shall support document import and integration:
  - Import documents from external systems
  - Integration with document management systems
  - Integration with laboratory information systems (automatic lab report import)
  - Integration with radiology/PACS systems (automatic imaging report import)
  - OCR (Optical Character Recognition) for scanned documents (optional)
  - Automatic document classification (optional, AI-powered)

##### 2.2.2.7 Medical History Data Quality and Validation

- **FR-2.19**: System shall implement data quality controls for medical history with the following specific validation rules:
  - **Date Validation**:
    - Dates cannot be in the future (except for scheduled procedures/immunizations)
    - Onset dates: Cannot be more than 150 years in the past (warn if > 120 years, require confirmation)
    - Onset dates: Cannot be after resolution dates (if both provided)
    - Resolution dates: Cannot be before onset dates
    - Procedure dates: Cannot be more than 150 years in the past
    - Hospitalization dates: Admission date cannot be after discharge date
    - Hospitalization dates: Discharge date cannot be more than 1 year in the future
    - Approximate dates: If only year provided, must be reasonable (not in future, not > 150 years ago)
    - Approximate dates: If month/year provided, must be valid month (1-12) and reasonable year
    - Date format validation: Must be valid calendar date format
  
  - **ICD-10/ICD-11 Code Validation**:
    - Code format validation: ICD-10 codes must be 3-7 characters (letter-digit-digit, then optional characters)
    - Code format validation: ICD-11 codes must follow ICD-11 structure (alphanumeric with specific format)
    - Code existence validation: Code must exist in current ICD code set (not deleted or invalid)
    - Code version validation: Code must be from current version of code set
    - Code category validation: Code must match condition category (e.g., cannot use injury code for disease)
    - Code specificity validation: Warn if using non-specific code when more specific code available
  
  - **CVX Code Validation** (for immunizations):
    - Code format validation: CVX codes must be numeric, 1-4 digits
    - Code existence validation: Code must exist in current CVX code set
    - Code version validation: Code must be from current CVX version
    - Code-vaccine name consistency: CVX code must match vaccine name
  
  - **CPT Code Validation** (for procedures):
    - Code format validation: CPT codes must be 5 digits (numeric)
    - Code existence validation: Code must exist in current CPT code set
    - Code version validation: Code must be from current CPT version
    - Code category validation: Code must match procedure type
  
  - **Duplicate Detection**:
    - Warn if same condition entered multiple times with same or similar dates
    - Warn if same procedure entered multiple times with same date
    - Warn if same immunization entered multiple times with same date (unless part of series)
    - Fuzzy matching for similar condition names (e.g., "Diabetes" vs "Diabetes Mellitus")
    - Display potential duplicates for user review before saving
  
  - **Completeness Checks**:
    - Identify missing required information (condition name, date, status)
    - Identify missing critical history elements based on patient demographics (e.g., obstetric history for female patients)
    - Identify incomplete entries (e.g., condition without date, procedure without outcome)
    - Identify missing source of information for patient-reported data
  
  - **Consistency Checks**:
    - Cannot be former smoker if smoking status is "Never smoker"
    - Cannot have quit date if smoking status is "Never smoker" or "Current smoker"
    - Cannot have pack-years if never smoked
    - Cannot have alcohol-related problems if alcohol use is "Never"
    - Cannot have substance abuse treatment if substance use is "Never"
    - Cannot have pregnancy complications if gravida is 0
    - Cannot have delivery information if para is 0
    - Date consistency: Onset date cannot be after resolution date
    - Age consistency: Age at onset cannot exceed patient's current age
  
  - **Data Standardization**:
    - Standardize condition names to preferred terminology
    - Standardize vaccine names to official names
    - Standardize procedure names to standard terminology
    - Standardize date formats
    - Standardize units of measure
    - Remove leading/trailing spaces
    - Capitalize appropriately (title case for conditions, proper case for names)

- **FR-2.20**: System shall support medical history data entry best practices:
  - Required fields validation
  - Suggested fields based on patient demographics (e.g., obstetric history for female patients)
  - Templates for common history patterns
  - Auto-complete for common entries
  - Spell check and medical terminology suggestions
  - Integration with clinical terminology services (SNOMED CT, etc.)

##### 2.2.2.8 Medical History Reporting and Analytics

- **FR-2.21**: System shall provide reporting capabilities for medical history:
  - **Patient-Level Reports**:
    - Complete medical history summary
    - History by category (Past Medical, Family, Social, Immunization)
    - Timeline reports
    - History change reports (what changed, when, by whom)
  
  - **Population-Level Reports**:
    - Prevalence of conditions in patient population
    - Family history patterns
    - Social history trends
    - Immunization coverage rates
    - Missing history elements (data quality reports)
  
  - **Clinical Reports**:
    - Risk assessment reports based on family history
    - Immunization status reports
    - Screening recommendations based on history
    - Quality measure reports (e.g., smoking cessation counseling, immunization rates)
  
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.2.9 Medical History Integration and Interoperability

- **FR-2.22**: System shall support integration with external systems for medical history:
  - **Health Information Exchange (HIE)**: Share and receive medical history data
  - **State Immunization Registries**: Import and export immunization data
  - **Laboratory Systems**: Automatic import of lab results as documents
  - **Radiology Systems**: Automatic import of imaging reports
  - **Other EHR Systems**: Import medical history from previous systems
  - **Public Health Systems**: Report immunization data to public health agencies
  - **Clinical Decision Support**: Integration with CDS systems for history-based recommendations
  
  - All integrations shall use standard formats (HL7 FHIR, HL7 V2, etc.)
  - All integrations shall maintain data security and audit trails
  - All imported data shall be validated and flagged as "imported" vs. "entered"

##### 2.2.2.10 Medical History Access Control and Privacy

- **FR-2.23**: System shall implement appropriate access controls for medical history:
  - Role-based access (different users see different levels of detail)
  - Sensitivity flags for confidential information (e.g., substance use, sexual history)
  - Privacy controls to restrict access to sensitive history elements
  - Audit logging of all access to medical history
  - Support for patient privacy preferences (e.g., patient may restrict certain history from certain providers)
  - Minimum necessary principle (users see only history relevant to their role)

- **FR-2.24**: System shall support patient access to their own medical history:
  - Patient portal integration (if portal feature available)
  - Patients can view their own history (with appropriate filtering of sensitive information)
  - Patients can request corrections to their history
  - Patients can add self-reported history (flagged as patient-reported)
  - Support for patient privacy preferences

##### 2.2.2.11 Medical History Error Handling

- **FR-2.25**: System shall implement comprehensive error handling for medical history management:
  - **Validation Error Handling**:
    - Display clear error messages for invalid ICD codes, CVX codes, CPT codes
    - Display date validation errors with specific guidance (e.g., "Onset date cannot be after resolution date")
    - Display consistency errors (e.g., "Cannot be former smoker if never smoked")
    - Highlight invalid fields and provide correction suggestions
    - Prevent saving until validation errors resolved
    - Maintain entered data when validation fails
  
  - **System Error Handling**:
    - Handle network/database errors during history entry
    - Auto-save history entries to prevent data loss
    - Provide retry mechanism for failed saves
    - Display user-friendly error messages
    - Log errors with context (patient ID, history type, user)
  
  - **Integration Error Handling**:
    - Handle failures from immunization registries (IIS)
    - Handle failures from HIE imports
    - Handle failures from external system imports
    - Allow manual entry when imports fail
    - Queue failed imports for retry
    - Provide import error reports
  
  - **Data Consistency Error Handling**:
    - Handle concurrent edit conflicts
    - Detect and prevent duplicate entries
    - Handle date consistency errors
    - Provide conflict resolution interface
    - Maintain audit trail of conflicts
  
  - **Document Upload Error Handling**:
    - Handle file size limit errors
    - Handle unsupported file format errors
    - Handle file corruption errors
    - Handle storage quota exceeded errors
    - Provide clear error messages with file requirements
    - Allow retry after error correction

#### 2.2.3 Vital Signs and Clinical Measurements

##### 2.2.3.1 Vital Signs Entry and Recording

- **FR-3.1**: System shall support comprehensive vital signs entry and recording with the following capabilities:
  - **Core Vital Signs**:
    - **Blood Pressure**:
      - Systolic pressure (required, numeric, range: 0-300 mmHg)
      - Diastolic pressure (required, numeric, range: 0-200 mmHg)
      - Measurement method (Manual, Automated, Invasive, optional)
      - Patient position (Sitting, Standing, Lying, optional)
      - Arm used (Left, Right, optional)
      - Cuff size (if applicable, optional)
      - Measurement date and time (required, auto-populated)
      - Measurement location (if different from encounter location, optional)
    
    - **Heart Rate/Pulse**:
      - Heart rate value (required, numeric, range: 0-300 bpm)
      - Pulse rhythm (Regular, Irregular, optional)
      - Pulse quality (Strong, Weak, Thready, Bounding, optional)
      - Measurement method (Manual, Automated, EKG, optional)
      - Measurement site (Radial, Brachial, Apical, Carotid, Other, optional)
      - Measurement date and time (required, auto-populated)
    
    - **Temperature**:
      - Temperature value (required, numeric)
      - Temperature unit (Celsius, Fahrenheit, required, default to system default)
      - Measurement method (Oral, Rectal, Axillary, Tympanic, Temporal, Invasive, optional)
      - Measurement site (if applicable, optional)
      - Measurement date and time (required, auto-populated)
    
    - **Respiratory Rate**:
      - Respiratory rate value (required, numeric, range: 0-100 breaths/min)
      - Respiratory pattern (Normal, Shallow, Deep, Labored, optional)
      - Measurement method (Observation, Automated, optional)
      - Measurement date and time (required, auto-populated)
    
    - **Oxygen Saturation (SpO2)**:
      - SpO2 value (required, numeric, range: 0-100%)
      - Measurement method (Pulse oximetry, Arterial blood gas, optional)
      - Oxygen delivery (Room air, Supplemental oxygen, optional)
      - Flow rate (if supplemental oxygen, optional, L/min)
      - FiO2 (Fraction of inspired oxygen, if applicable, optional)
      - Measurement date and time (required, auto-populated)
    
    - **Height and Weight**:
      - Height value (required, numeric)
      - Height unit (cm, inches, required, default to system default)
      - Weight value (required, numeric)
      - Weight unit (kg, lbs, required, default to system default)
      - BMI (Body Mass Index, auto-calculated, required)
      - BMI category (Underweight, Normal, Overweight, Obese, auto-calculated, optional)
      - Measurement method (Scale, Self-reported, optional)
      - Measurement date and time (required, auto-populated)
    
    - **Pain Scale**:
      - Pain score (required, numeric, range: 0-10)
      - Pain scale type (Numeric 0-10, Visual Analog Scale, Faces Scale, optional)
      - Pain location (optional, free text or structured)
      - Pain description (optional, free text)
      - Measurement date and time (required, auto-populated)
    
    - **Blood Glucose**:
      - Blood glucose value (required if applicable, numeric)
      - Blood glucose unit (mg/dL, mmol/L, required if applicable)
      - Measurement method (Fingerstick, Venous, Continuous monitor, optional)
      - Fasting status (Fasting, Non-fasting, Postprandial, optional)
      - Time since last meal (if applicable, optional)
      - Measurement date and time (required, auto-populated)
  
  - **Additional Clinical Measurements**:
    - Head circumference (for pediatric patients, optional)
    - Waist circumference (optional)
    - Body surface area (BSA, auto-calculated if height and weight available, optional)
    - Peak flow (for respiratory conditions, optional)
    - Glasgow Coma Scale (for neurological assessment, optional)
    - Other custom measurements (configurable by organization, optional)
  
  - **Measurement Context**:
    - Encounter/visit association (required, link to encounter)
    - Recording provider/user (required, auto-populated)
    - Recording facility/location (auto-populated)
    - Measurement conditions (Resting, After exercise, After medication, optional)
    - Patient position (if different from standard, optional)
    - Notes/comments (optional, free text)
    - Source of measurement (Manual entry, Device integration, Imported, optional)

- **FR-3.2**: System shall support multiple methods for vital signs entry:
  - **Manual Entry**:
    - Direct entry of vital signs values
    - Entry forms with validation
    - Quick entry for common vital signs
    - Batch entry for multiple vital signs at once
    - Copy from previous encounter (with date adjustment)
    - Template-based entry
  
  - **Device Integration**:
    - Integration with automated vital signs monitors
    - Automatic capture from connected devices
    - Support for common device protocols (HL7, proprietary)
    - Device calibration verification
    - Manual override if device reading seems incorrect
    - Device identification and logging
  
  - **Import from External Sources**:
    - Import from other EHR systems
    - Import from Health Information Exchange (HIE)
    - Import from patient monitoring systems
    - Import from home monitoring devices (if applicable)
    - Data validation and verification for imported values

- **FR-3.3**: System shall validate all vital signs data entry:
  - **Range Validation**:
    - Blood pressure: Systolic 0-300 mmHg, Diastolic 0-200 mmHg, Systolic must be > Diastolic
    - Heart rate: 0-300 bpm (with age-appropriate ranges)
    - Temperature: Reasonable ranges based on unit and method
    - Respiratory rate: 0-100 breaths/min (with age-appropriate ranges)
    - SpO2: 0-100%
    - Height: Reasonable ranges based on age and unit
    - Weight: Reasonable ranges based on age, height, and unit
    - Pain scale: 0-10
    - Blood glucose: Reasonable ranges (typically 20-600 mg/dL or equivalent)
  
  - **Logical Validation**:
    - Cannot enter future dates/times
    - Cannot enter values that are physiologically impossible
    - Age-appropriate value ranges (e.g., pediatric vs. adult)
    - Consistency checks (e.g., BMI calculation matches height/weight)
  
  - **Required Field Validation**:
    - Required fields must be completed before saving
    - Clear error messages for validation failures
    - Visual highlighting of invalid fields
    - Warning for values outside normal ranges (but allow entry with confirmation)

##### 2.2.3.2 Vital Signs Display and Visualization

- **FR-3.4**: System shall provide comprehensive vital signs display capabilities:
  - **Current Vital Signs Display**:
    - Display most recent vital signs prominently
    - Display all vital signs from current encounter
    - Display vital signs in organized format (table, card, list)
    - Display vital signs with units clearly indicated
    - Display measurement date and time
    - Display recording provider
    - Display measurement method (if applicable)
    - Color coding for abnormal values (red for critical, yellow for abnormal)
    - Normal range indicators (show normal ranges for each vital sign)
  
  - **Historical Vital Signs Display**:
    - Display vital signs from previous encounters
    - Display vital signs in chronological order
    - Filter vital signs by date range
    - Filter vital signs by type
    - Display vital signs by encounter/visit
    - Display vital signs with context (encounter type, provider, location)
  
  - **Comparison Views**:
    - Compare current vital signs with previous measurements
    - Compare vital signs across multiple encounters
    - Side-by-side comparison view
    - Highlight significant changes (delta values)
    - Display change indicators (increased, decreased, unchanged)
  
  - **Summary Views**:
    - Vital signs summary for patient dashboard
    - Quick vital signs overview
    - Most recent values display
    - Abnormal values summary
    - Critical values summary

- **FR-3.5**: System shall support multiple display formats:
  - **Table Format**:
    - Tabular display of vital signs
    - Sortable columns
    - Filterable rows
    - Expandable rows for details
    - Exportable tables
  
  - **Card Format**:
    - Card-based display for each vital sign
    - Visual indicators (icons, colors)
    - Compact view for dashboard
    - Expandable cards for details
  
  - **Graphical Format**:
    - Visual representation of values
    - Gauge displays for single values
    - Comparison charts
    - Integration with trend graphs
  
  - **List Format**:
    - Simple list view
    - Chronological list
    - Grouped by encounter
    - Grouped by vital sign type

##### 2.2.3.3 Vital Signs Trends and Analysis

- **FR-3.6**: System shall calculate and display vital signs trends over time:
  - **Trend Calculation**:
    - Calculate trends for all numeric vital signs
    - Support multiple time periods (Last 24 hours, Last 7 days, Last 30 days, Last year, All time)
    - Calculate statistical measures (mean, median, min, max, standard deviation)
    - Identify trends (increasing, decreasing, stable, variable)
    - Calculate rate of change
    - Support for custom date ranges
  
  - **Trend Visualization**:
    - Line graphs showing trends over time
    - Multi-line graphs for comparing multiple vital signs
    - Scatter plots for correlation analysis
    - Bar charts for discrete comparisons
    - Area charts for cumulative trends
    - Interactive graphs (zoom, pan, hover for details)
    - Print-friendly graph formats
    - Export graphs to images (PNG, PDF)
  
  - **Trend Analysis Features**:
    - Highlight significant changes
    - Identify patterns (cyclic, seasonal, etc.)
    - Compare trends across different time periods
    - Annotate graphs with clinical events
    - Overlay normal ranges on graphs
    - Display reference lines (normal ranges, target values)
    - Support for multiple y-axes for different units

- **FR-3.7**: System shall support advanced trend analysis:
  - **Statistical Analysis**:
    - Mean arterial pressure (MAP) calculation and trending
    - BMI trend analysis
    - Weight change percentage
    - Blood pressure variability analysis
    - Heart rate variability (if applicable)
    - Custom calculations (configurable)
  
  - **Comparative Analysis**:
    - Compare patient trends to population norms
    - Compare trends before and after interventions
    - Compare trends across different measurement methods
    - Compare trends by encounter type
    - Compare trends by provider
  
  - **Predictive Analysis** (if applicable):
    - Identify trends that may indicate deterioration
    - Alert on concerning trend patterns
    - Support for clinical decision-making based on trends

##### 2.2.3.4 Vital Signs Alerts and Notifications

- **FR-3.8**: System shall implement comprehensive vital signs alerting:
  - **Abnormal Value Alerts**:
    - Alert when vital signs are outside normal ranges
    - Alert when vital signs are outside age-appropriate ranges
    - Alert when vital signs are outside patient-specific ranges (if configured)
    - Visual alerts (color coding, icons, badges)
    - Inline alerts in vital signs display
    - Alert severity levels (Informational, Warning, Critical)
  
  - **Critical Value Alerts**:
    - Alert on life-threatening vital signs values
    - Critical value thresholds (configurable by organization):
      - Blood pressure: Systolic < 90 or > 200, Diastolic > 120
      - Heart rate: < 40 or > 150 (age-adjusted)
      - Temperature: < 95°F or > 104°F (method-adjusted)
      - Respiratory rate: < 8 or > 30 (age-adjusted)
      - SpO2: < 90%
      - Blood glucose: < 50 or > 400 mg/dL (or equivalent)
    - Immediate notification to providers
    - Alert escalation if not acknowledged
    - Alert documentation and audit trail
  
  - **Trend-Based Alerts**:
    - Alert on significant trend changes
    - Alert on rapid deterioration
    - Alert on values approaching critical thresholds
    - Alert on persistent abnormal values
    - Alert on values that don't improve as expected
  
  - **Alert Configuration**:
    - Organization-level alert thresholds
    - Provider-level alert preferences
    - Patient-specific alert thresholds (if applicable)
    - Alert delivery methods (In-system, Email, SMS, optional)
    - Alert frequency controls (prevent alert fatigue)
    - Alert acknowledgment requirements

- **FR-3.9**: System shall support vital signs notification workflows:
  - **Provider Notifications**:
    - Notify ordering provider of critical values
    - Notify care team of significant changes
    - Notification queue for unacknowledged alerts
    - Notification history and tracking
    - Notification preferences (opt-in/opt-out for non-critical alerts)
  
  - **Nursing Notifications**:
    - Notify nursing staff of abnormal values
    - Notify nursing staff of missing vital signs (if required)
    - Notification for vital signs requiring re-measurement
    - Workflow integration for vital signs monitoring
  
  - **Alert Management**:
    - Acknowledge alerts
    - Dismiss alerts (with reason, if applicable)
    - Escalate alerts
    - Alert response documentation
    - Alert audit trail

##### 2.2.3.5 Vital Signs Integration and Workflow

- **FR-3.10**: System shall integrate vital signs with other system components:
  - **Clinical Notes Integration**:
    - Auto-populate vital signs into clinical notes
    - Insert vital signs into notes with one click
    - Link vital signs to specific note sections
    - Display vital signs in note context
    - Include vital signs in note templates
  
  - **Problem List Integration**:
    - Link vital signs to relevant problems/diagnoses
    - Display vital signs relevant to active problems
    - Alert when vital signs indicate problem worsening
    - Support problem-based vital signs monitoring
  
  - **Medication Integration**:
    - Display vital signs relevant to medications (e.g., blood pressure for antihypertensives)
    - Alert on vital signs changes that may be medication-related
    - Support medication monitoring workflows
    - Link vital signs to medication administration
  
  - **Laboratory Results Integration**:
    - Display related lab results with vital signs
    - Compare vital signs with lab values
    - Support comprehensive patient assessment
    - Identify correlations between vital signs and lab results
  
  - **Encounter Integration**:
    - Associate vital signs with encounters/visits
    - Display vital signs in encounter summary
    - Support encounter-based vital signs workflows
    - Track vital signs across encounter types

- **FR-3.11**: System shall support vital signs workflow requirements:
  - **Workflow States**:
    - Draft (being entered)
    - Final (completed and verified)
    - Corrected (amended after finalization)
    - Workflow state clearly displayed
  
  - **Workflow Actions**:
    - Save draft vital signs
    - Finalize vital signs entry
    - Correct/amend vital signs (with audit trail)
    - Delete vital signs (with authorization and reason)
    - Verify vital signs (if required by workflow)
  
  - **Workflow Integration**:
    - Integration with clinical workflows
    - Support for vital signs protocols
    - Support for vital signs order sets
    - Integration with care plans
    - Support for vital signs monitoring schedules

##### 2.2.3.6 Vital Signs Reporting and Analytics

- **FR-3.12**: System shall provide comprehensive vital signs reporting capabilities:
  - **Patient-Level Reports**:
    - Complete vital signs history report
    - Vital signs summary report
    - Vital signs trend report
    - Vital signs by encounter report
    - Vital signs comparison report
    - Export vital signs data (PDF, Excel, CSV)
  
  - **Population-Level Reports**:
    - Vital signs statistics by patient population
    - Vital signs trends across patient groups
    - Vital signs quality measures
    - Vital signs completion rates
    - Abnormal vital signs prevalence
    - Support for quality measure reporting (e.g., HEDIS measures)
  
  - **Clinical Reports**:
    - Vital signs monitoring reports
    - Vital signs alert reports
    - Vital signs compliance reports
    - Provider performance reports (if applicable)
    - Facility-level vital signs reports
  
  - **Report Features**:
    - Customizable report parameters (date range, vital sign types, patient filters)
    - Report scheduling (if applicable)
    - Report export in multiple formats
    - Report templates
    - Report sharing capabilities

##### 2.2.3.7 Vital Signs Security and Privacy

- **FR-3.13**: System shall implement appropriate security and privacy controls for vital signs:
  - **Access Control**:
    - Role-based access to vital signs (different users see different levels of detail)
    - Minimum necessary principle (users see only vital signs relevant to their role)
    - Patient privacy flags affecting vital signs access
    - Break-the-glass functionality for emergency access (with audit trail)
    - Access restrictions based on user facility/location (if configured)
  
  - **Privacy Controls**:
    - Sensitive vital signs flags (if applicable)
    - Restricted access to sensitive vital signs
    - Patient privacy preferences affecting vital signs visibility
    - Support for patient requests to restrict vital signs access
    - Audit logging of all vital signs access
  
  - **Data Security**:
    - Vital signs data encrypted at rest and in transit
    - Secure vital signs data transmission
    - Vital signs access logging
    - Unauthorized access prevention
    - Vital signs retention and disposal policies
    - Compliance with HIPAA and other regulations

##### 2.2.3.8 Vital Signs Quality and Validation

- **FR-3.14**: System shall implement quality controls for vital signs:
  - **Data Quality Checks**:
    - Completeness checks (identify missing required vital signs)
    - Accuracy validation (range checks, logical consistency)
    - Timeliness checks (identify delayed vital signs entry)
    - Duplicate detection (warn if same vital sign entered multiple times for same time)
    - Data standardization (standardize units, formats)
  
  - **Quality Indicators**:
    - Vital signs entry completeness rate
    - Vital signs entry timeliness
    - Vital signs data accuracy metrics
    - Vital signs alert response time
    - Quality metrics reporting
  
  - **Validation Rules**:
    - Configurable validation rules by organization
    - Age-appropriate validation rules
    - Patient-specific validation rules (if applicable)
    - Validation rule documentation
    - Validation rule enforcement

- **FR-3.15**: System shall support vital signs data quality improvement:
  - **Quality Monitoring**:
    - Monitor vital signs data quality metrics
    - Identify data quality issues
    - Generate data quality reports
    - Support quality improvement initiatives
  
  - **Data Correction**:
    - Support correction of inaccurate vital signs
    - Maintain audit trail of corrections
    - Require authorization for corrections (if configured)
    - Document reason for correction
    - Preserve original values in audit trail

##### 2.2.3.9 Vital Signs Standards and Interoperability

- **FR-3.16**: System shall support vital signs standards and interoperability:
  - **Data Standards**:
    - Support for HL7 FHIR Vital Signs Profile
    - Support for HL7 V2 for vital signs exchange
    - Support for LOINC codes for vital signs
    - Support for UCUM (Unified Code for Units of Measure) for units
    - Support for SNOMED CT for vital signs concepts
    - Standard data formats for vital signs exchange
  
  - **Interoperability**:
    - Export vital signs to Health Information Exchange (HIE)
    - Import vital signs from other EHR systems
    - Share vital signs with external providers (with patient consent)
    - Support for C-CDA (Consolidated Clinical Document Architecture) for vital signs
    - Support for standard APIs for vital signs access
    - Integration with patient monitoring systems
  
  - **Device Integration Standards**:
    - Support for standard device communication protocols
    - Support for HL7 device integration
    - Support for DICOM for certain measurements (if applicable)
    - Device interoperability standards compliance

##### 2.2.3.10 Vital Signs Workflow and Best Practices

- **FR-3.17**: System shall support vital signs workflow best practices:
  - **Workflow Support**:
    - Support for vital signs protocols
    - Support for vital signs order sets
    - Support for vital signs monitoring schedules
    - Support for vital signs reminders
    - Support for vital signs documentation workflows
    - Integration with clinical workflows
  
  - **Best Practices**:
    - Support for evidence-based vital signs monitoring
    - Support for clinical guidelines for vital signs
    - Support for vital signs documentation standards
    - Support for quality measure compliance
    - Support for accreditation requirements
  
  - **User Support**:
    - Training materials for vital signs entry
    - Help documentation
    - Contextual help in vital signs forms
    - Best practices guidance
    - Workflow tips and suggestions

##### 2.2.3.11 Vital Signs Error Handling

- **FR-3.18**: System shall implement comprehensive error handling for vital signs management:
  - **Validation Error Handling**:
    - Display clear error messages for out-of-range values (e.g., "Blood pressure systolic must be between 0-300 mmHg")
    - Display age-appropriate range errors with patient age context
    - Display unit conversion errors
    - Display calculation errors (e.g., BMI calculation failures)
    - Highlight invalid fields with specific correction guidance
    - Prevent saving until validation errors resolved
    - Maintain entered data when validation fails
  
  - **Device Integration Error Handling**:
    - Handle device connection failures
    - Handle device communication timeouts
    - Handle invalid device data format
    - Handle device calibration errors
    - Display message: "Unable to connect to device. Please enter values manually."
    - Allow manual entry when device fails
    - Log device errors with device ID and error details
    - Provide device troubleshooting guidance
  
  - **Calculation Error Handling**:
    - Handle BMI calculation errors (missing height/weight)
    - Handle MAP calculation errors
    - Handle trend calculation errors (insufficient data points)
    - Handle statistical calculation errors
    - Display calculation error messages
    - Provide fallback calculations when possible
    - Log calculation errors for review
  
  - **Data Entry Error Handling**:
    - Handle concurrent vital signs entry conflicts
    - Handle duplicate entry detection
    - Handle missing required vital signs errors
    - Auto-save vital signs entries to prevent data loss
    - Provide retry mechanism for failed saves
    - Display user-friendly error messages
  
  - **Trend Analysis Error Handling**:
    - Handle insufficient data for trend analysis
    - Handle data quality issues in trend calculations
    - Handle graph rendering errors
    - Display message when trends cannot be calculated
    - Provide alternative views when trends unavailable
    - Log trend analysis errors

#### 2.2.4 Clinical Notes and Documentation

##### 2.2.4.1 Note Types and Formats

- **FR-4.1**: System shall support creation of multiple types of clinical notes including:
  - **Progress Notes**:
    - Daily progress notes for hospitalized patients
    - Visit-based progress notes for outpatient encounters
    - Follow-up visit notes
    - Telephone encounter notes
    - E-visit/telemedicine notes
    - Urgent care visit notes
    - Emergency department visit notes
  
  - **SOAP Notes** (Subjective, Objective, Assessment, Plan):
    - Structured format with four distinct sections
    - Subjective: Patient's chief complaint, history of present illness, review of systems
    - Objective: Physical examination findings, vital signs, test results
    - Assessment: Clinical impression, differential diagnosis, problem identification
    - Plan: Treatment plan, medications, follow-up, patient education
    - Support for multiple problems/assessments with individual plans
  
  - **Consultation Notes**:
    - Specialist consultation requests
    - Specialist consultation responses
    - Pre-operative consultation notes
    - Post-operative consultation notes
    - Inter-departmental consultation notes
    - External consultation notes (from other facilities)
  
  - **Discharge Summaries**:
    - Hospital discharge summaries
    - Emergency department discharge summaries
    - Ambulatory surgery discharge summaries
    - Structured format including:
      - Admission diagnosis
      - Discharge diagnosis
      - Procedures performed
      - Hospital course
      - Discharge medications
      - Discharge instructions
      - Follow-up appointments
      - Discharge disposition
  
  - **Procedure Notes**:
    - Operative notes
    - Minor procedure notes
    - Endoscopy notes
    - Biopsy notes
    - Injection procedure notes
    - Other procedure documentation
    - Structured format including:
      - Procedure name and CPT code
      - Indication
      - Technique
      - Findings
      - Complications
      - Post-procedure instructions
  
  - **History and Physical (H&P)**:
    - Admission H&P
    - Pre-operative H&P
    - Annual physical examination
    - Comprehensive history and physical documentation
  
  - **Assessment and Plan Notes**:
    - Problem-focused assessment and plan
    - Multi-problem assessment and plan
    - Structured problem list with individual plans
  
  - **Telephone/Message Notes**:
    - Telephone encounter documentation
    - Patient message documentation
    - Provider-to-provider communication notes
    - Patient callback documentation
  
  - **Nursing Notes**:
    - Nursing assessment notes
    - Nursing progress notes
    - Shift notes
    - Care plan documentation
  
  - **Other Note Types**:
    - Referral notes
    - Transfer notes
    - Death summary/autopsy notes
    - Research notes (if applicable)
    - Custom note types (configurable by organization)

- **FR-4.2**: System shall support structured note formats with:
  - Pre-defined sections and fields for each note type
  - Required vs. optional sections (configurable)
  - Section headers and organization
  - Ability to add custom sections
  - Ability to reorder sections
  - Support for both structured and free-text entry within sections

##### 2.2.4.2 Note Creation and Editing

- **FR-4.3**: System shall support multiple methods for note creation:
  - **Manual Entry**:
    - Rich text editor with formatting options (bold, italic, underline, bullet points, numbered lists)
    - Free-text entry in designated sections
    - Structured data entry in form fields
    - Dropdown selections for common entries
    - Checkboxes for common findings
    - Date/time pickers
    - Numeric input fields with units
  
  - **Template-Based Entry**:
    - Pre-built note templates by specialty (e.g., Cardiology, Orthopedics, Pediatrics)
    - Pre-built note templates by note type (e.g., SOAP, H&P, Discharge Summary)
    - Customizable templates created by users or administrators
    - Template library with search and categorization
    - Ability to save frequently used note sections as templates
    - Ability to create templates from existing notes
    - Template variables (auto-populate patient name, date, etc.)
    - Conditional sections in templates (show/hide based on selections)
  
  - **Voice-to-Text Transcription**:
    - Integration with voice recognition software
    - Real-time voice-to-text conversion
    - Voice commands for navigation and formatting
    - Support for medical terminology recognition
    - Ability to edit transcribed text
    - Support for multiple languages (if applicable)
  
  - **Copy and Paste**:
    - Copy from previous notes (with date/context preservation)
    - Copy from templates
    - Copy from external sources (with formatting preservation or conversion)
    - Smart paste (remove formatting, preserve structure)
  
  - **Import from External Sources**:
    - Import notes from other EHR systems
    - Import transcribed notes from transcription services
    - Import structured data from external systems

- **FR-4.4**: System shall support note editing with:
  - Edit mode for draft notes
  - Auto-save functionality (save drafts periodically)
  - Manual save option
  - Ability to edit notes before signing
  - Edit history tracking (who edited, when, what changed)
  - Ability to view previous versions of edited notes
  - Lock mechanism to prevent simultaneous editing by multiple users
  - Conflict resolution if simultaneous edits occur

##### 2.2.4.3 Note Content and Data Integration

- **FR-4.5**: System shall support integration of clinical data into notes:
  - **Auto-Population**:
    - Auto-populate patient demographics (name, DOB, MRN)
    - Auto-populate vital signs from current encounter
    - Auto-populate current medications
    - Auto-populate active problems/diagnoses
    - Auto-populate allergies
    - Auto-populate recent lab results
    - Auto-populate recent imaging results
    - Auto-populate visit/encounter information
  
  - **Data Insertion**:
    - Insert vital signs into note with one click
    - Insert lab results into note with one click
    - Insert imaging results into note with one click
    - Insert medication list into note
    - Insert problem list into note
    - Insert procedure codes into note
    - Insert diagnosis codes into note
    - Insert links to other documents or results
  
  - **Smart Text**:
    - Auto-complete for common medical terms
    - Spell check with medical dictionary
    - Medical terminology suggestions
    - Abbreviation expansion
    - Support for medical abbreviations (e.g., "SOB" expands to "shortness of breath" or shows as abbreviation based on context)

- **FR-4.6**: System shall support note content organization:
  - Section headers and subheaders
  - Bullet points and numbered lists
  - Tables for structured data
  - Paragraph formatting
  - Text alignment (left, center, right, justify)
  - Font size and style options (within reasonable limits for clinical documentation)
  - Ability to insert images, diagrams, or attachments
  - Ability to insert links to other parts of the record

##### 2.2.4.4 Note Templates and Customization

- **FR-4.7**: System shall provide comprehensive template management:
  - **Template Library**:
    - Pre-built templates by specialty
    - Pre-built templates by note type
    - Organization-specific custom templates
    - User-specific custom templates
    - Shared templates (across organization or department)
    - Template versioning (track template changes over time)
  
  - **Template Creation and Editing**:
    - Template builder with drag-and-drop interface
    - Ability to create templates from scratch
    - Ability to create templates from existing notes
    - Ability to edit existing templates (with version control)
    - Ability to copy and modify templates
    - Template preview functionality
    - Template testing before deployment
  
  - **Template Features**:
    - Variable fields (patient name, date, provider name, etc.)
    - Conditional logic (show section if condition met)
    - Dropdown fields with predefined options
    - Checkbox fields
    - Required vs. optional sections
    - Default values
    - Smart fields (auto-populate from patient data)
  
  - **Template Access Control**:
    - Role-based template access
    - Department-specific templates
    - Specialty-specific templates
    - User-specific templates
    - Template sharing permissions

- **FR-4.8**: System shall support note customization by specialty:
  - Specialty-specific note formats
  - Specialty-specific templates
  - Specialty-specific required fields
  - Specialty-specific data elements
  - Specialty-specific terminology and abbreviations
  - Customizable note workflows by specialty

##### 2.2.4.5 Note Signing and Authentication

- **FR-4.9**: System shall support electronic signing of notes with:
  - **Signing Requirements**:
    - Notes must be signed before being considered complete/final
    - Unsigned notes clearly marked as "Draft" or "Unsigned"
    - Ability to sign notes individually or in batch
    - Co-signature support (for trainees, mid-level providers)
    - Attestation statements (e.g., "I have reviewed and agree with the above documentation")
    - Electronic signature capture (typed name, digital signature pad, or click-to-sign)
    - Signature date and time stamp (auto-populated, cannot be backdated)
    - Signature cannot be removed once applied (only amendments/addendums allowed)
  
  - **Co-Signature Workflow**:
    - Primary author creates and signs note
    - Supervising provider reviews and co-signs
    - Both signatures required for note completion
    - Co-signature deadline/reminders
    - Co-signature queue for supervising providers
    - Ability to reject note and request revisions
  
  - **Signature Authentication**:
    - User authentication required before signing
    - Password or biometric authentication
    - Audit trail of all signature actions
    - Signature cannot be delegated (must be signed by authenticated user)
    - Support for proxy signatures (with proper authorization and documentation)

- **FR-4.10**: System shall support note amendments and addendums:
  - **Amendments**:
    - Ability to add amendments to signed notes
    - Amendment clearly marked and dated
    - Original note preserved (not deleted or overwritten)
    - Amendment linked to original note
    - Reason for amendment (optional but recommended)
    - Amendment requires signature
  
  - **Addendums**:
    - Ability to add addendums to signed notes
    - Addendum clearly marked and dated
    - Addendum linked to original note
    - Addendum requires signature
  
  - **Corrections**:
    - Ability to correct errors in unsigned notes
    - Corrections in signed notes require amendment/addendum
    - Original content preserved in audit trail
    - Correction history visible

##### 2.2.4.6 Note Version Control and History

- **FR-4.11**: System shall maintain comprehensive version control for notes:
  - **Version Tracking**:
    - Track all versions of a note (draft, signed, amended)
    - Version number or timestamp for each version
    - Display current version prominently
    - Ability to view previous versions
    - Compare versions (highlight differences)
    - Version history log (who changed, when, what changed)
  
  - **Edit History**:
    - Track all edits made to note (even before signing)
    - Track who made each edit
    - Track when each edit was made
    - Track what content was changed (before/after)
    - Display edit history in chronological order
    - Edit history accessible to authorized users only
  
  - **Audit Trail**:
    - Complete audit trail of note lifecycle:
      - Note created
      - Note edited
      - Note saved
      - Note signed
      - Note amended
      - Note viewed
      - Note printed/exported
    - Audit trail includes user ID, timestamp, action, and IP address (if available)
    - Audit trail cannot be modified or deleted
    - Audit trail accessible for compliance and legal purposes

##### 2.2.4.7 Note Viewing and Display

- **FR-4.12**: System shall provide comprehensive note viewing capabilities:
  - **Display Options**:
    - View note in formatted display (as created)
    - View note in plain text format
    - View note with all versions
    - View note with edit history
    - View note with linked documents/results
    - Print-friendly view
    - Full-screen view for reading
  
  - **Note Organization**:
    - Display notes in chronological order (newest first or oldest first)
    - Filter notes by type
    - Filter notes by provider
    - Filter notes by date range
    - Filter notes by encounter/visit
    - Filter notes by status (signed, unsigned, draft)
    - Search notes by keyword or content
    - Group notes by encounter
    - Group notes by date
  
  - **Note Navigation**:
    - Quick navigation between notes
    - Previous/next note navigation
    - Jump to specific note by date or type
    - Bookmark frequently accessed notes
    - Note summary view (list of all notes with key information)

- **FR-4.13**: System shall support note printing and export:
  - Print individual notes
  - Print multiple notes (batch printing)
  - Print notes in chronological order
  - Print notes with formatting preserved
  - Export notes to PDF
  - Export notes to text file
  - Export notes to Word document (if applicable)
  - Export notes with metadata (date, provider, etc.)
  - Export notes for legal/regulatory purposes

##### 2.2.4.8 Note Search and Retrieval

- **FR-4.14**: System shall provide advanced note search capabilities:
  - **Search Methods**:
    - Full-text search across all note content
    - Search by note type
    - Search by provider/author
    - Search by date range
    - Search by keywords or phrases
    - Search by diagnosis mentioned in note
    - Search by medication mentioned in note
    - Search by procedure mentioned in note
    - Advanced search with multiple criteria
  
  - **Search Results**:
    - Display search results with relevance ranking
    - Highlight search terms in results
    - Display note preview/snippet
    - Display note metadata (date, provider, type)
    - Ability to open note from search results
    - Save search queries for reuse
    - Export search results

##### 2.2.4.9 Note Workflow and Collaboration

- **FR-4.15**: System shall support note workflows and collaboration:
  - **Note Assignment**:
    - Assign note creation to specific provider
    - Assign note review to supervising provider
    - Note assignment notifications/reminders
    - Note assignment queue
  
  - **Note Sharing**:
    - Share draft notes with other providers for review
    - Share notes with care team members
    - Note sharing permissions and access control
    - Shared note notifications
  
  - **Note Collaboration**:
    - Multiple providers contribute to same note (with proper attribution)
    - Section-level attribution (who wrote which section)
    - Collaborative editing (with conflict resolution)
    - Comments/annotations on notes (for review purposes)
  
  - **Note Workflow States**:
    - Draft (being created/edited)
    - Pending Review (awaiting co-signature)
    - Signed/Complete (finalized)
    - Amended (modified after signing)
    - Workflow state clearly displayed

##### 2.2.4.10 Note Quality and Compliance

- **FR-4.16**: System shall support note quality measures:
  - **Completeness Checks**:
    - Identify notes missing required sections
    - Identify unsigned notes
    - Identify notes missing key information
    - Completeness score or indicators
  
  - **Quality Indicators**:
    - Note length appropriateness
    - Use of structured data vs. free text
    - Template usage
    - Timeliness of note completion (note written within required timeframe)
    - Quality metrics reporting
  
  - **Compliance Support**:
    - Support for regulatory documentation requirements
    - Support for accreditation standards (Joint Commission, etc.)
    - Support for quality measure documentation
    - Support for billing documentation requirements
    - Support for legal documentation standards

- **FR-4.17**: System shall support note documentation standards:
  - Adherence to medical documentation best practices
  - Support for required documentation elements by note type
  - Support for specialty-specific documentation requirements
  - Support for payer-specific documentation requirements
  - Documentation guidelines and reminders
  - Integration with clinical decision support for documentation completeness

##### 2.2.4.11 Note Integration and Interoperability

- **FR-4.18**: System shall support note integration with other systems:
  - **Internal Integration**:
    - Link notes to encounters/visits
    - Link notes to diagnoses/problems
    - Link notes to procedures
    - Link notes to medications
    - Link notes to lab results
    - Link notes to imaging results
    - Link notes to other documents
  
  - **External Integration**:
    - Export notes to Health Information Exchange (HIE)
    - Import notes from other EHR systems
    - Share notes with external providers (with patient consent)
    - Integration with transcription services
    - Integration with voice recognition systems
    - Support for HL7 FHIR for note exchange
    - Support for C-CDA (Consolidated Clinical Document Architecture) for note sharing

##### 2.2.4.12 Note Security and Privacy

- **FR-4.19**: System shall implement appropriate security and privacy controls for notes:
  - **Access Control**:
    - Role-based access to notes (different users see different notes or note sections)
    - Patient privacy flags affecting note access
    - Minimum necessary principle (users see only notes relevant to their role)
    - Break-the-glass functionality (emergency access with audit trail)
  
  - **Privacy Controls**:
    - Sensitive note flags (psychiatric, substance abuse, etc.)
    - Restricted access to sensitive notes
    - Patient privacy preferences affecting note visibility
    - Support for patient requests to restrict note access
    - Audit logging of all note access
  
  - **Data Security**:
    - Notes encrypted at rest and in transit
    - Secure note transmission
    - Note access logging
    - Unauthorized access prevention
    - Note retention and disposal policies

##### 2.2.4.13 Note Reporting and Analytics

- **FR-4.20**: System shall provide reporting capabilities for clinical notes:
  - **Note Statistics**:
    - Number of notes by type
    - Number of notes by provider
    - Note completion rates
    - Note signing timeliness
    - Template usage statistics
  
  - **Quality Reports**:
    - Documentation completeness reports
    - Note quality metrics
    - Compliance reports
    - Missing documentation reports
  
  - **Workflow Reports**:
    - Pending co-signatures
    - Unsigned notes
    - Note creation productivity
    - Note review turnaround time
  
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.4.14 Clinical Notes Error Handling

- **FR-4.21**: System shall implement comprehensive error handling for clinical notes:
  - **Note Creation Error Handling**:
    - Handle template loading failures
    - Handle voice-to-text transcription errors
    - Handle auto-save failures
    - Display clear error messages for note creation failures
    - Auto-save note drafts to prevent data loss
    - Provide recovery mechanism for lost notes
  
  - **Note Editing Error Handling**:
    - Handle concurrent editing conflicts
    - Detect when note is being edited by another user
    - Provide conflict resolution interface
    - Prevent overwriting other user's changes
    - Maintain version history during conflicts
  
  - **Note Signing Error Handling**:
    - Handle signature authentication failures
    - Handle co-signature workflow errors
    - Handle signature system failures
    - Display clear error messages for signing failures
    - Prevent note loss if signing fails
    - Provide retry mechanism for signing
  
  - **Note Storage Error Handling**:
    - Handle storage quota exceeded errors
    - Handle file system errors
    - Handle database errors during note save
    - Provide error recovery mechanisms
    - Log storage errors for administrators
  
  - **Template Error Handling**:
    - Handle template loading errors
    - Handle template parsing errors
    - Handle template variable substitution errors
    - Display template error messages
    - Provide fallback to blank note when template fails
  
  - **Integration Error Handling**:
    - Handle failures when inserting data into notes
    - Handle failures when linking notes to encounters
    - Handle failures when exporting notes
    - Allow manual entry when auto-population fails
    - Queue failed operations for retry

#### 2.2.5 Diagnoses and Problem Lists

##### 2.2.5.1 Problem List Management

- **FR-5.1**: System shall maintain comprehensive problem lists with the following information for each problem:
  - **Problem Identification**:
    - Problem/diagnosis name (required, free text or coded)
    - ICD-10/ICD-11 diagnosis code (required, with auto-lookup)
    - SNOMED CT code (optional, for clinical terminology)
    - Problem description (optional, free text for clarification)
    - Problem category/type (Acute, Chronic, Symptom, Finding, Other)
  
  - **Temporal Information**:
    - Date of onset (required, can be approximate: year, month/year, or exact date)
    - Date diagnosed (optional, when diagnosis was first made)
    - Date resolved (if resolved, optional)
    - Duration (auto-calculated if dates available)
  
  - **Status Management**:
    - Problem status (required, dropdown):
      - Active (currently affecting patient)
      - Resolved (no longer affecting patient)
      - Chronic (ongoing long-term condition)
      - Inactive (temporarily not active but may recur)
      - Ruled Out (diagnosis considered but excluded)
      - Pending (awaiting confirmation)
      - Historical (past problem, no longer relevant)
  
  - **Clinical Details**:
    - Severity/Stage (if applicable, e.g., Stage 1, Stage 2, Mild, Moderate, Severe)
    - Laterality (Left, Right, Bilateral, if applicable)
    - Location/anatomical site (if applicable)
    - Etiology/cause (optional, free text or coded)
    - Complications (optional, free text or coded)
  
  - **Provider Information**:
    - Diagnosing provider (required, user who added diagnosis)
    - Confirming provider (optional, if different from diagnosing provider)
    - Provider specialty (optional, auto-populated from provider profile)
    - Diagnosing facility/location (optional)
  
  - **Clinical Context**:
    - Primary vs. secondary diagnosis (for billing/clinical purposes)
    - Problem priority (High, Medium, Low, optional)
    - Problem visibility (Visible to all providers, Restricted, optional)
    - Notes/comments (optional, free text for additional context)
    - Related problems (optional, link to other problems)
    - Related medications (optional, link to medications treating this problem)
    - Related procedures (optional, link to procedures related to this problem)

- **FR-5.2**: System shall support problem list organization and display:
  - **Display Options**:
    - Active problems displayed prominently
    - Resolved problems displayed separately (with option to hide)
    - Problems sorted by status (Active first, then Chronic, then Resolved)
    - Problems sorted by date (newest first or oldest first)
    - Problems sorted alphabetically
    - Problems sorted by priority
    - Problems grouped by category/type
    - Problems grouped by body system
  
  - **Problem List Views**:
    - Summary view (problem name, status, date)
    - Detailed view (all problem information)
    - Timeline view (problems displayed chronologically)
    - Category view (problems grouped by category)
    - Provider view (problems by diagnosing provider)
  
  - **Problem List Customization**:
    - User preferences for default sort order
    - User preferences for which problems to display
    - Ability to collapse/expand problem categories
    - Ability to filter problems by status, category, date range, provider

##### 2.2.5.2 Problem Entry and Modification

- **FR-5.3**: System shall support multiple methods for adding problems to the problem list:
  - **Manual Entry**:
    - Direct entry of problem name with ICD code lookup
    - Entry from diagnosis search/selection
    - Entry from template or common problems list
    - Entry from previous encounters (copy from visit diagnoses)
    - Entry from past medical history (promote history item to active problem)
  
  - **From Clinical Documentation**:
    - Extract diagnoses from clinical notes
    - Link problems to specific encounters/visits
    - Add problems during note creation
    - Add problems from assessment section of SOAP notes
  
  - **From External Sources**:
    - Import problems from other EHR systems
    - Import problems from Health Information Exchange (HIE)
    - Import problems from discharge summaries
    - Import problems from consultation reports
  
  - **Bulk Entry**:
    - Add multiple problems at once
    - Import problem list from external file
    - Copy problem list from another patient (with modification)

- **FR-5.4**: System shall support problem modification and updates:
  - **Status Updates**:
    - Change problem status (e.g., Active to Resolved, Active to Chronic)
    - Update resolution date when marking as resolved
    - Reactivate resolved problems (with reason)
    - Update problem status in bulk (if applicable)
  
  - **Content Updates**:
    - Edit problem name/description (with audit trail)
    - Update ICD code (with validation)
    - Update severity/stage
    - Update dates (onset, diagnosis, resolution)
    - Add or modify notes/comments
    - Link or unlink related problems
    - Update provider information
  
  - **Problem Merging**:
    - Merge duplicate problems
    - Combine similar problems into one
    - Merge with audit trail and reason
  
  - **Problem Deletion**:
    - Remove problems from list (with reason and audit trail)
    - Soft delete (mark as deleted but retain in history)
    - Hard delete (permanent removal, with high-level authorization)
    - Deletion history maintained in audit trail

- **FR-5.5**: System shall support problem verification and confirmation:
  - **Verification Workflow**:
    - Mark problems as "Confirmed" or "Unconfirmed"
    - Require provider confirmation for certain problem types
    - Support problem confirmation by multiple providers
    - Track verification status and date
    - Display verification status prominently
  
  - **Problem Review**:
    - Periodic problem list review (e.g., annual review)
    - Problem list review reminders
    - Problem list review documentation
    - Update problem list during review

##### 2.2.5.3 Diagnosis Coding and Standards

- **FR-5.6**: System shall support comprehensive diagnosis coding:
  - **Coding Systems**:
    - ICD-10-CM (International Classification of Diseases, 10th Revision, Clinical Modification) - primary coding system
    - ICD-11 (when adopted, support for transition)
    - SNOMED CT (Systematized Nomenclature of Medicine Clinical Terms) - for clinical terminology
    - Support for multiple codes per problem (primary and secondary codes)
    - Support for external cause codes (E-codes) for injuries
    - Support for laterality codes (if applicable)
  
  - **Code Lookup and Search**:
    - Search by diagnosis name/keyword
    - Search by ICD code
    - Search by SNOMED CT code
    - Browse diagnosis categories
    - Auto-complete for common diagnoses
    - Recent diagnoses list
    - Favorite/frequently used diagnoses
    - Code hierarchy navigation (category → subcategory → specific code)
  
  - **Code Validation**:
    - Validate ICD codes against official code sets
    - Validate code format
    - Validate code is current (not deleted or replaced)
    - Warn if code is not billable or requires additional documentation
    - Suggest more specific codes when general codes are selected
    - Validate code matches problem description

- **FR-5.7**: System shall support diagnosis code management:
  - **Code Updates**:
    - Automatic updates when new ICD code sets are released
    - Code mapping (old codes to new codes)
    - Code replacement notifications
    - Support for code versioning (track which version of code set was used)
  
  - **Code Mapping**:
    - Map between ICD-10 and ICD-11 (when applicable)
    - Map between ICD and SNOMED CT
    - Map between different code versions
    - Preserve original codes when mapping occurs

##### 2.2.5.4 Problem List Integration

- **FR-5.8**: System shall integrate problem lists with other system components:
  - **Encounter Integration**:
    - Link problems to specific encounters/visits
    - Display encounter diagnoses in problem list
    - Add problems from encounter documentation
    - Track which problems were addressed in each encounter
  
  - **Medication Integration**:
    - Link medications to problems they treat
    - Display problems associated with each medication
    - Alert when medication is prescribed without linked problem
    - Alert when problem has no associated treatment
  
  - **Procedure Integration**:
    - Link procedures to problems they address
    - Display problems associated with each procedure
    - Track problem resolution after procedures
  
  - **Lab/Imaging Integration**:
    - Link lab results to problems
    - Link imaging findings to problems
    - Display problems associated with abnormal results
  
  - **Clinical Notes Integration**:
    - Link problems to clinical notes
    - Extract problems mentioned in notes
    - Display notes related to each problem
    - Update problem status based on note content

- **FR-5.9**: System shall support problem-based care planning:
  - **Care Plans**:
    - Create care plans for specific problems
    - Link care plan goals to problems
    - Track progress toward problem resolution
    - Update care plans based on problem status changes
  
  - **Problem Prioritization**:
    - Set problem priorities (High, Medium, Low)
    - Display problems by priority
    - Focus care on high-priority problems
    - Update priorities as problems evolve

##### 2.2.5.5 Problem List Quality and Validation

- **FR-5.10**: System shall implement data quality controls for problem lists:
  - **Completeness Checks**:
    - Identify problems missing required information (ICD code, date, status)
    - Identify problems with incomplete clinical details
    - Identify duplicate problems
    - Identify problems that should be resolved but are still active
    - Identify problems that should be active but are marked resolved
  
  - **Accuracy Validation**:
    - **ICD Code Validation**:
      - Code format validation: ICD-10 codes must be 3-7 characters (letter-digit-digit format)
      - Code existence validation: Code must exist in current ICD-10/ICD-11 code set
      - Code version validation: Code must be from current version (not deprecated)
      - Code validity check: Code must not be deleted or invalid in current code set
      - Code specificity validation: Warn if using non-specific code (e.g., "Other" codes) when more specific code available
      - Code category validation: Code must match problem category (e.g., cannot use injury code for chronic disease)
    
    - **Date Validation**:
      - Onset date: Cannot be in the future (except for scheduled procedures)
      - Onset date: Cannot be more than 150 years in the past (warn if > 120 years)
      - Resolution date: Cannot be before onset date
      - Resolution date: Cannot be more than 1 year in the future
      - Diagnosis date: Cannot be before patient's date of birth
      - Diagnosis date: Cannot be more than 150 years in the past
      - Date format validation: Must be valid calendar date (cannot be invalid dates)
      - Leap year validation: February 29 only valid in leap years
    
    - **Status Transition Validation**:
      - Cannot resolve problem before onset date
      - Cannot mark as "Resolved" if resolution date is in the future (warn, allow with confirmation)
      - Cannot mark as "Active" if resolution date is in the past
      - Cannot transition from "Resolved" back to "Active" without new onset date
      - Cannot transition from "Ruled Out" to "Active" without new diagnosis date
      - Status transitions must be logical (e.g., cannot go from "Resolved" directly to "Ruled Out")
    
    - **Problem Description Validation**:
      - Description must match ICD code category (e.g., "Diabetes" description must match diabetes ICD codes)
      - Description cannot be empty if problem is active
      - Description length: 1-500 characters
      - Description cannot contain only special characters or numbers
      - Warn if description is very generic (e.g., "Disease", "Problem") when more specific description available
    
    - **Conflicting Information Checks**:
      - Check for conflicting diagnoses (e.g., Type 1 and Type 2 diabetes both active)
      - Check for conflicting statuses (e.g., problem marked "Resolved" but still in active problem list)
      - Check for conflicting dates (e.g., resolution date before onset date)
      - Check for duplicate problems with same ICD code and similar dates
      - Alert for potentially conflicting diagnoses based on clinical knowledge
  
  - **Clinical Validation**:
    - Alert for potentially conflicting diagnoses
    - Alert for unusual diagnosis combinations
    - Suggest related or alternative diagnoses
    - Validate problem severity/stage is appropriate for diagnosis
    - Clinical decision support for problem management

- **FR-5.11**: System shall support problem list maintenance:
  - **Problem List Review**:
    - Periodic problem list review (e.g., at each visit, annually)
    - Problem list review reminders
    - Problem list review documentation
    - Update problem list during review
    - Archive old or irrelevant problems
  
  - **Problem List Cleanup**:
    - Identify and merge duplicate problems
    - Resolve problems that should be marked as resolved
    - Update problems with outdated information
    - Remove problems that are no longer relevant
    - Maintain problem list accuracy and currency

##### 2.2.5.6 Problem List Reporting and Analytics

- **FR-5.12**: System shall provide reporting capabilities for problem lists:
  - **Patient-Level Reports**:
    - Complete problem list report
    - Active problems report
    - Resolved problems report
    - Problem list history report
    - Problem list changes over time
  
  - **Population-Level Reports**:
    - Problem prevalence by diagnosis
    - Problem prevalence by category
    - Problem resolution rates
    - Problem list completeness
    - Most common problems in patient population
    - Problem list quality metrics
  
  - **Clinical Reports**:
    - Problems by provider
    - Problems by specialty
    - Problems by facility/location
    - Problem coding accuracy
    - Problem status distribution
    - Problem list review compliance
  
  - **Quality Reports**:
    - Problems missing ICD codes
    - Problems with outdated information
    - Duplicate problems
    - Problems requiring review
    - Problem list data quality scores
  
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.5.7 Problem List Security and Privacy

- **FR-5.13**: System shall implement appropriate security and privacy controls for problem lists:
  - **Access Control**:
    - Role-based access to problem lists
    - Some users may have read-only access
    - Some users may have restricted access to certain problem types
    - Minimum necessary principle (users see only problems relevant to their role)
  
  - **Privacy Controls**:
    - Sensitive problem flags (e.g., psychiatric, substance abuse, HIV)
    - Restricted access to sensitive problems
    - Patient privacy preferences affecting problem visibility
    - Support for patient requests to restrict problem access
    - Audit logging of all problem list access
  
  - **Data Security**:
    - Problem list data encrypted at rest and in transit
    - Secure problem list transmission
    - Problem list access logging
    - Unauthorized access prevention
    - Problem list retention and disposal policies

##### 2.2.5.8 Problem List Interoperability

- **FR-5.14**: System shall support problem list interoperability:
  - **Health Information Exchange (HIE)**:
    - Share problem lists with other healthcare organizations
    - Receive problem lists from other healthcare organizations
    - Merge problem lists from multiple sources
    - Resolve conflicts when receiving duplicate problems
  
  - **Standard Formats**:
    - Support HL7 FHIR for problem list exchange
    - Support C-CDA (Consolidated Clinical Document Architecture) for problem list sharing
    - Support HL7 V2 messages for problem list transmission
    - Support SNOMED CT for clinical terminology exchange
  
  - **External System Integration**:
    - Import problem lists from other EHR systems
    - Export problem lists to other systems
    - Integration with coding and billing systems
    - Integration with quality reporting systems
    - Integration with population health systems

##### 2.2.5.9 Problem List Workflow and Best Practices

- **FR-5.15**: System shall support problem list workflows and best practices:
  - **Problem List Maintenance Workflow**:
    - Add problems during patient encounters
    - Review problem list at each visit
    - Update problem status as conditions change
    - Resolve problems when appropriate
    - Archive historical problems
  
  - **Problem List Documentation**:
    - Document problem in clinical notes
    - Link problems to relevant documentation
    - Update problem notes as condition evolves
    - Maintain problem documentation completeness
  
  - **Problem List Best Practices**:
    - Support for problem list review guidelines
    - Reminders for problem list maintenance
    - Templates for common problem patterns
    - Clinical decision support for problem management
    - Integration with evidence-based guidelines

#### 2.2.6 Laboratory Results

##### 2.2.6.1 Laboratory Test Ordering

- **FR-6.1**: System shall support comprehensive laboratory test ordering with:
  - **Order Information**:
    - Test selection (individual tests or test panels/profiles)
    - Test name and LOINC code (Logical Observation Identifiers Names and Codes)
    - Order date and time (required, auto-populated)
    - Ordering provider (required, auto-populated from logged-in user)
    - Ordering facility/location (auto-populated)
    - Clinical indication/reason for test (optional, free text or coded)
    - Priority (Routine, Stat, ASAP, Timed, optional)
    - Special instructions (optional, free text)
    - Fasting requirements (if applicable, Yes/No)
    - Patient preparation instructions (optional)
  
  - **Test Selection**:
    - Search tests by name, LOINC code, or category
    - Browse tests by category (Chemistry, Hematology, Microbiology, Immunology, etc.)
    - Common tests list (frequently ordered tests)
    - Test panels/profiles (pre-defined groups of tests)
    - Custom test panels (user-created groups)
    - Favorite tests list
    - Recent tests list
  
  - **Order Management**:
    - Create single test orders
    - Create multiple test orders at once
    - Copy previous orders
    - Modify orders before sending (if not yet sent)
    - Cancel orders (with reason and audit trail)
    - Reschedule orders
    - Order status tracking (Pending, Sent, Collected, In Process, Completed, Cancelled)
  
  - **Order Transmission**:
    - Electronic transmission to laboratory information systems (LIS)
    - HL7 V2 ORM (Order Message) support
    - HL7 FHIR ServiceRequest support
    - Fax transmission (fallback option)
    - Print order requisition (if needed)
    - Order confirmation from laboratory

##### 2.2.6.2 Laboratory Result Receipt and Storage

- **FR-6.2**: System shall receive and store laboratory results with comprehensive data:
  - **Result Identification**:
    - Test name (required)
    - LOINC code (required, for standardized identification)
    - Test category/type (Chemistry, Hematology, Microbiology, etc.)
    - Order ID (link to original order, required)
    - Result ID (unique identifier for result, required)
  
  - **Result Values**:
    - Result value (required, numeric, text, or coded)
    - Result units (required for numeric results, e.g., mg/dL, mmol/L)
    - Result type (Numeric, Text, Coded, Structured)
    - Qualitative result (if applicable, e.g., Positive, Negative, Reactive, Non-reactive)
    - Quantitative result (if applicable, numeric value)
    - Result status (Final, Preliminary, Corrected, Cancelled, Amended)
    - Result interpretation (if provided by lab, e.g., Normal, Abnormal, Critical)
  
  - **Reference Ranges**:
    - Normal range (low and high values)
    - Age-specific reference ranges (if applicable)
    - Gender-specific reference ranges (if applicable)
    - Reference range units
    - Reference range source (lab-specific, standard, etc.)
  
  - **Abnormal Flags**:
    - Abnormal flag (H = High, L = Low, A = Abnormal, N = Normal, C = Critical)
    - Critical value flag (Yes/No)
    - Delta check flag (significant change from previous result)
    - Panic value flag (life-threatening value)
  
  - **Temporal Information**:
    - Order date and time
    - Specimen collection date and time (required)
    - Specimen received date and time (optional)
    - Result date and time (required)
    - Result reported date and time (required)
    - Result verified date and time (optional)
  
  - **Specimen Information**:
    - Specimen type (Blood, Urine, Stool, Sputum, Tissue, etc.)
    - Specimen source (if applicable, e.g., Venous, Arterial, Random, Fasting)
    - Specimen collection method (if applicable)
    - Specimen ID/accession number
    - Specimen volume/quantity (if applicable)
    - Specimen quality (Adequate, Inadequate, Hemolyzed, Clotted, etc.)
  
  - **Laboratory Information**:
    - Performing laboratory name (required)
    - Laboratory ID/NPI
    - Laboratory location/address
    - Laboratory phone number
    - Performing technologist (optional)
    - Reviewing pathologist/physician (optional)
    - Laboratory reference number
  
  - **Result Comments**:
    - Laboratory comments (optional, free text from lab)
    - Provider comments (optional, free text from ordering provider)
    - Result notes (optional, additional context)
    - Method used (if applicable, e.g., "Automated", "Manual", specific assay name)

- **FR-6.3**: System shall support result receipt through multiple methods:
  - **Electronic Receipt**:
    - HL7 V2 ORU (Observation Result) messages
    - HL7 FHIR Observation resources
    - Direct LIS integration
    - Interface engine integration
    - Real-time result receipt
    - Batch result receipt
  
  - **Manual Entry**:
    - Manual result entry by authorized users
    - Manual entry with validation
    - Manual entry with audit trail
    - Manual entry for results received via fax or phone
  
  - **File Import**:
    - Import results from CSV, Excel, or text files
    - Import results from HL7 message files
    - Import results with data mapping and validation

##### 2.2.6.3 Laboratory Result Display and Viewing

- **FR-6.4**: System shall provide comprehensive result display capabilities:
  - **Result List View**:
    - Display all results in chronological order
    - Display results by test category
    - Display results by date range
    - Display results by ordering provider
    - Display results by laboratory
    - Filter results by status (Final, Preliminary, etc.)
    - Filter results by abnormal flags
    - Search results by test name or LOINC code
  
  - **Result Detail View**:
    - Display complete result information
    - Display result value with units
    - Display reference range with comparison
    - Display abnormal flags prominently
    - Display specimen information
    - Display laboratory information
    - Display result comments
    - Display related results (same test panel)
    - Display linked orders
  
  - **Result Formatting**:
    - Highlight abnormal values (color coding: red for high, blue for low)
    - Highlight critical values (bold, red, flashing alert)
    - Display reference ranges clearly
    - Display units prominently
    - Format numeric values appropriately (decimals, significant figures)
    - Display qualitative results clearly (Positive/Negative, etc.)
    - Display structured/coded results appropriately

- **FR-6.5**: System shall support result trending and graphical displays:
  - **Trend Analysis**:
    - Display result trends over time (line graphs, scatter plots)
    - Compare current result to previous results
    - Calculate rate of change (if applicable)
    - Display multiple related tests on same graph
    - Display reference ranges on graphs
    - Highlight abnormal trends
    - Support for different time ranges (1 week, 1 month, 3 months, 1 year, all time)
  
  - **Graphical Displays**:
    - Line graphs for numeric results over time
    - Bar charts for categorical results
    - Scatter plots for related tests
    - Heat maps for multiple tests over time
    - Print-friendly graph formats
    - Export graphs as images (PNG, JPG, PDF)
  
  - **Result Comparison**:
    - Compare results across time periods
    - Compare results to reference ranges
    - Compare results to population norms (if available)
    - Display statistical summaries (mean, median, range)

##### 2.2.6.4 Critical Value and Alert Management

- **FR-6.6**: System shall implement comprehensive critical value alerting:
  - **Critical Value Detection**:
    - Identify critical/panic values based on predefined thresholds
    - Age-specific critical value thresholds
    - Gender-specific critical value thresholds (if applicable)
    - Condition-specific critical value thresholds (if applicable)
    - Custom critical value thresholds (configurable by organization)
  
  - **Alert Display**:
    - Prominent alert display when critical values are received
    - Alert in patient summary/dashboard
    - Alert in result list
    - Alert in result detail view
    - Visual indicators (red color, bold text, alert icons)
    - Audio alerts (optional, configurable)
    - Pop-up alerts (optional, configurable)
  
  - **Alert Notification**:
    - Notify ordering provider immediately
    - Notify covering provider (if applicable)
    - Notify nursing staff (if applicable)
    - Email notifications (optional, configurable)
    - Text message notifications (optional, configurable)
    - Phone call notifications (optional, for life-threatening values)
    - Escalation if alert not acknowledged within time limit
  
  - **Alert Acknowledgment**:
    - Require acknowledgment of critical value alerts
    - Track who acknowledged alert and when
    - Track provider response to critical value
    - Document action taken in response to critical value
    - Alert acknowledgment workflow

- **FR-6.7**: System shall support other result alerts and notifications:
  - **Abnormal Value Alerts**:
    - Alert for abnormal (but not critical) values
    - Alert for significant changes from previous results (delta checks)
    - Alert for results outside reference range
    - Alert preferences (user-configurable)
  
  - **Result Status Alerts**:
    - Alert when preliminary results become final
    - Alert when results are corrected or amended
    - Alert when results are delayed
    - Alert when specimen is inadequate or rejected
  
  - **Result Availability Notifications**:
    - Notify when ordered tests are completed
    - Notify when results are available for review
    - Notification preferences (user-configurable)
    - Notification delivery methods (in-system, email, text)

##### 2.2.6.5 Result Interpretation and Clinical Context

- **FR-6.8**: System shall support result interpretation and clinical context:
  - **Result Interpretation**:
    - Display laboratory-provided interpretation (if available)
    - Display clinical significance of results
    - Link results to relevant diagnoses/problems
    - Link results to relevant medications
    - Display result in context of patient's condition
  
  - **Clinical Decision Support**:
    - Alert for results that may indicate specific conditions
    - Suggest follow-up tests based on results
    - Suggest clinical actions based on results
    - Integration with clinical guidelines
    - Drug-lab interaction alerts (if medication affects lab results)
  
  - **Result Correlation**:
    - Display related results together (e.g., all liver function tests)
    - Display results that may be related to same condition
    - Display results in context of test panels
    - Display results in context of patient's clinical picture

##### 2.2.6.6 Result Documentation and Notes

- **FR-6.9**: System shall support result documentation and notes:
  - **Provider Notes**:
    - Add notes to individual results
    - Add notes to result groups/panels
    - Add clinical interpretation notes
    - Add follow-up plan notes
    - Notes linked to specific provider
    - Notes timestamped
    - Notes visible to care team
  
  - **Result Review Documentation**:
    - Mark results as reviewed
    - Track who reviewed results and when
    - Document provider response to results
    - Document clinical action taken based on results
    - Review status tracking
  
  - **Result Communication**:
    - Document patient notification of results
    - Document patient discussion of results
    - Document result communication method (in-person, phone, portal, etc.)
    - Patient communication preferences

##### 2.2.6.7 Result Correction and Amendment

- **FR-6.10**: System shall support result corrections and amendments:
  - **Result Corrections**:
    - Support corrected results from laboratory
    - Display original result and corrected result
    - Clearly mark corrected results
    - Reason for correction (if provided)
    - Correction date and time
    - Correction audit trail
  
  - **Result Amendments**:
    - Support amended results from laboratory
    - Display original result and amended result
    - Clearly mark amended results
    - Reason for amendment (if provided)
    - Amendment date and time
    - Amendment audit trail
  
  - **Result Cancellation**:
    - Support cancelled results from laboratory
    - Clearly mark cancelled results
    - Reason for cancellation (if provided)
    - Cancellation date and time
    - Cancellation audit trail
  
  - **Result History**:
    - Maintain complete history of all result versions
    - Display result change history
    - Track all corrections, amendments, and cancellations
    - Preserve original results (never delete)

##### 2.2.6.8 Result Integration and Workflow

- **FR-6.11**: System shall integrate laboratory results with other system components:
  - **Encounter Integration**:
    - Link results to specific encounters/visits
    - Display results in encounter context
    - Add results to encounter documentation
    - Track which results were reviewed during encounter
  
  - **Clinical Notes Integration**:
    - Insert results into clinical notes
    - Link results to relevant notes
    - Display notes related to results
    - Update notes based on results
  
  - **Problem/Diagnosis Integration**:
    - Link results to relevant problems/diagnoses
    - Display results related to specific problems
    - Update problem status based on results
    - Alert when results support or contradict diagnoses
  
  - **Medication Integration**:
    - Link results to relevant medications
    - Alert when results indicate medication issues (toxicity, ineffectiveness)
    - Display results that monitor medication effects
    - Drug-lab interaction alerts
  
  - **Order Integration**:
    - Link results to original orders
    - Display order information with results
    - Track order-to-result workflow
    - Alert when ordered tests have no results

##### 2.2.6.9 Result Reporting and Export

- **FR-6.12**: System shall provide comprehensive result reporting and export:
  - **Patient Reports**:
    - Complete laboratory result report
    - Results by date range
    - Results by test category
    - Results summary report
    - Trend reports with graphs
  
  - **Clinical Reports**:
    - Results by ordering provider
    - Results by laboratory
    - Critical value reports
    - Abnormal result reports
    - Pending result reports
    - Result review status reports
  
  - **Quality Reports**:
    - Result turnaround time reports
    - Result completeness reports
    - Result quality metrics
    - Laboratory performance reports
  
  - **Export Capabilities**:
    - Export results to PDF
    - Export results to Excel/CSV
    - Export results to text file
    - Export results with graphs
    - Export results for patient portal
    - Export results for external systems
    - Print results (individual or batch)

##### 2.2.6.10 Result Standards and Interoperability

- **FR-6.13**: System shall support laboratory result standards and interoperability:
  - **Coding Standards**:
    - LOINC codes for test identification (required)
    - SNOMED CT for result values (where applicable)
    - UCUM (Unified Code for Units of Measure) for units
    - ICD-10 for result interpretation (where applicable)
  
  - **Message Standards**:
    - HL7 V2 ORU (Observation Result) messages
    - HL7 FHIR Observation resources
    - HL7 FHIR DiagnosticReport resources
    - C-CDA (Consolidated Clinical Document Architecture) for result sharing
  
  - **Interoperability**:
    - Integration with Laboratory Information Systems (LIS)
    - Integration with Health Information Exchanges (HIE)
    - Integration with other EHR systems
    - Integration with reference laboratories
    - Support for result sharing with external providers
    - Support for patient access to results (via portal)

##### 2.2.6.11 Result Security and Privacy

- **FR-6.14**: System shall implement appropriate security and privacy controls for laboratory results:
  - **Access Control**:
    - Role-based access to results
    - Some users may have read-only access
    - Some users may have restricted access to certain result types
    - Minimum necessary principle
    - Break-the-glass functionality for emergency access
  
  - **Privacy Controls**:
    - Sensitive result flags (e.g., HIV, genetic testing)
    - Restricted access to sensitive results
    - Patient privacy preferences affecting result visibility
    - Support for patient requests to restrict result access
    - Audit logging of all result access
  
  - **Data Security**:
    - Result data encrypted at rest and in transit
    - Secure result transmission
    - Result access logging
    - Unauthorized access prevention
    - Result retention and disposal policies
    - HIPAA compliance for result data

##### 2.2.6.12 Result Quality and Validation

- **FR-6.15**: System shall implement result quality controls with the following specific validation rules:
  - **LOINC Code Validation**:
    - Code format validation: LOINC codes must be numeric, 5-7 digits
    - Code existence validation: Code must exist in current LOINC code set
    - Code version validation: Code must be from current LOINC version
    - Code-test name consistency: LOINC code must match test name
  
  - **Result Value Validation**:
    - Numeric results: Must be valid numeric format (integer or decimal)
    - Numeric results: Must be within reasonable range for test type (e.g., glucose 20-600 mg/dL)
    - Numeric results: Cannot be negative unless test allows negative values (e.g., some lab values can be negative)
    - Text results: Must be from allowed value set (if applicable, e.g., Positive/Negative)
    - Text results: Length validation (1-500 characters)
    - Coded results: Must be from valid code set
    - Result value cannot be all zeros unless zero is valid result
    - Result value cannot be all same digit (e.g., 111.11) unless valid
  
  - **Unit Validation**:
    - Unit must be valid UCUM (Unified Code for Units of Measure) unit
    - Unit must match test type (e.g., mg/dL for glucose, not kg)
    - Unit must be consistent with result value type
    - Unit cannot be empty for numeric results
    - Unit format validation: Must follow UCUM format standards
  
  - **Reference Range Validation**:
    - Low value must be less than high value
    - Reference range must be in same units as result value
    - Reference range values must be reasonable for test type
    - Age-specific ranges: Must match patient age
    - Gender-specific ranges: Must match patient gender
    - Reference range cannot be empty for tests that require ranges
  
  - **Date and Time Validation**:
    - Collection date/time: Cannot be in the future
    - Collection date/time: Cannot be more than 1 year in the past (warn if > 6 months)
    - Result date/time: Cannot be before collection date/time
    - Result date/time: Cannot be more than 1 year in the future
    - Reported date/time: Cannot be before result date/time
    - Verified date/time: Cannot be before reported date/time (if applicable)
    - Date format validation: Must be valid date/time format
    - Time zone validation: Must include time zone or use system default
  
  - **Required Fields Validation**:
    - Test name: Required, 1-200 characters
    - LOINC code: Required for standardized tests
    - Result value: Required
    - Result units: Required for numeric results
    - Collection date/time: Required
    - Result date/time: Required
    - Laboratory name: Required
    - Result status: Required (Final, Preliminary, etc.)
  
  - **Quality Checks**:
    - Identify missing results (ordered but not received)
    - Identify delayed results
    - Identify duplicate results
    - Identify results with missing information
    - Identify results requiring review
    - Result completeness checks
  
  - **Quality Metrics**:
    - Result turnaround time tracking
    - Result accuracy tracking
    - Result completeness tracking
    - Result review compliance tracking
    - Quality metric reporting

##### 2.2.6.13 Laboratory Results Error Handling

- **FR-6.16**: System shall implement comprehensive error handling for laboratory results:
  - **Result Receipt Error Handling**:
    - Handle HL7 message parsing errors
    - Handle invalid LOINC code errors
    - Handle missing required field errors
    - Handle data format errors
    - Queue failed results for reprocessing
    - Provide error reports for failed results
    - Allow manual entry when automatic receipt fails
  
  - **Result Display Error Handling**:
    - Handle missing result data gracefully
    - Handle corrupted result data
    - Handle calculation errors (e.g., delta checks)
    - Display user-friendly error messages
    - Provide alternative views when data unavailable
  
  - **Integration Error Handling**:
    - Handle LIS connection failures
    - Handle result transmission failures
    - Handle result import failures
    - Allow manual result entry when integration fails
    - Queue failed integrations for retry
    - Log integration errors with details
  
  - **Critical Value Alert Error Handling**:
    - Handle alert delivery failures
    - Handle alert acknowledgment errors
    - Escalate unacknowledged critical alerts
    - Log alert delivery failures
    - Provide alternative alert mechanisms
  
  - **Result Correction Error Handling**:
    - Handle amendment workflow errors
    - Handle correction authorization failures
    - Maintain audit trail during corrections
    - Prevent data loss during corrections
    - Provide rollback mechanism if correction fails

#### 2.2.7 Imaging and Diagnostic Studies

##### 2.2.7.1 Imaging Study Ordering

- **FR-7.1**: System shall support comprehensive imaging study ordering with:
  - **Order Information**:
    - Study type selection (required):
      - X-ray (Chest, Extremity, Spine, Skull, etc.)
      - CT (Computed Tomography) - with or without contrast
      - MRI (Magnetic Resonance Imaging) - with or without contrast
      - Ultrasound
      - Mammography
      - Nuclear Medicine studies
      - PET (Positron Emission Tomography)
      - DEXA (Dual-energy X-ray Absorptiometry)
      - Fluoroscopy
      - Other imaging modalities
    - Study description (required, free text or structured)
    - CPT code (Current Procedural Terminology, required for billing)
    - Order date and time (required, auto-populated)
    - Ordering provider (required, auto-populated from logged-in user)
    - Ordering facility/location (auto-populated)
    - Clinical indication/reason for study (required, free text or coded)
    - Priority (Routine, Stat, Urgent, optional)
    - Special instructions (optional, free text)
    - Contrast agent required (Yes/No, if applicable)
    - Contrast agent type (if applicable, e.g., IV contrast, oral contrast)
    - Patient preparation required (Yes/No, with instructions)
    - Sedation required (Yes/No, if applicable)
  
  - **Study Selection**:
    - Search studies by name, CPT code, or category
    - Browse studies by modality (X-ray, CT, MRI, etc.)
    - Browse studies by body part/anatomical region
    - Common studies list (frequently ordered studies)
    - Study protocols (pre-defined study configurations)
    - Custom study protocols (user-created)
    - Favorite studies list
    - Recent studies list
  
  - **Body Part and Laterality**:
    - Anatomical region/body part (required, e.g., Chest, Abdomen, Head, Extremity)
    - Laterality (Left, Right, Bilateral, if applicable)
    - Specific anatomical site (optional, more specific than region)
    - View/projection (if applicable, e.g., AP, PA, Lateral, Oblique)
  
  - **Order Management**:
    - Create single study orders
    - Create multiple study orders at once
    - Copy previous orders
    - Modify orders before sending (if not yet sent)
    - Cancel orders (with reason and audit trail)
    - Reschedule orders
    - Order status tracking (Pending, Sent, Scheduled, In Progress, Completed, Cancelled, No Show)
  
  - **Scheduling Integration**:
    - Schedule imaging studies (if scheduling system integrated)
    - Display available appointment times
    - Schedule based on study type and duration
    - Schedule based on facility and equipment availability
    - Patient appointment reminders
  
  - **Order Transmission**:
    - Electronic transmission to radiology information systems (RIS)
    - Electronic transmission to PACS (Picture Archiving and Communication System)
    - HL7 V2 ORM (Order Message) support
    - HL7 FHIR ServiceRequest support
    - DICOM worklist integration
    - Fax transmission (fallback option)
    - Print order requisition (if needed)
    - Order confirmation from radiology department

##### 2.2.7.2 Imaging Study Results and Reports

- **FR-7.2**: System shall receive and store imaging study results with comprehensive data:
  - **Study Identification**:
    - Study name/description (required)
    - Study type/modality (required, e.g., CT, MRI, X-ray)
    - CPT code (required, for billing)
    - Study ID/Accession number (required, unique identifier)
    - Order ID (link to original order, required)
    - Study date and time (required)
    - Study completion date and time (required)
    - Study status (Completed, Preliminary, Final, Cancelled, Amended)
  
  - **Study Details**:
    - Body part examined (required)
    - Laterality (if applicable)
    - Number of images/series (optional)
    - Contrast used (Yes/No, with type if applicable)
    - Technique/protocol used (optional)
    - Equipment used (scanner/model, optional)
    - Radiation dose (if applicable, for dose tracking)
    - Study duration (optional)
  
  - **Radiologist Information**:
    - Interpreting radiologist name (required for final reports)
    - Radiologist NPI (optional)
    - Radiologist specialty (optional)
    - Preliminary reading by (if different from final reader, optional)
    - Reviewing radiologist (if applicable, optional)
    - Report date and time (required)
    - Report finalized date and time (if different from report date)
  
  - **Report Content**:
    - **Clinical History** (from order):
      - Clinical indication
      - Relevant clinical history
      - Prior studies comparison (if applicable)
    
    - **Technique** (optional):
      - Imaging technique used
      - Contrast administration details
      - Patient positioning
    
    - **Findings** (required for final reports):
      - Detailed description of findings
      - Normal vs. abnormal findings
      - Measurements (if applicable)
      - Comparison to prior studies (if applicable)
      - Structured findings (if available)
    
    - **Impression/Conclusion** (required for final reports):
      - Summary of findings
      - Diagnostic interpretation
      - Recommendations for follow-up (if applicable)
      - Urgency indicators (if applicable)
  
  - **Report Status**:
    - Preliminary report (initial interpretation)
    - Final report (verified interpretation)
    - Addendum (additional information added)
    - Amendment (correction to report)
    - Cancelled report

- **FR-7.3**: System shall support report receipt through multiple methods:
  - **Electronic Receipt**:
    - HL7 V2 ORU (Observation Result) messages
    - HL7 FHIR DiagnosticReport resources
    - Direct RIS integration
    - Direct PACS integration
    - Interface engine integration
    - Real-time report receipt
    - Batch report receipt
  
  - **Manual Entry**:
    - Manual report entry by authorized users
    - Manual entry with validation
    - Manual entry with audit trail
    - Manual entry for reports received via fax or phone
  
  - **File Import**:
    - Import reports from text files
    - Import reports from HL7 message files
    - Import reports with data mapping and validation
    - Import transcribed reports from transcription services

##### 2.2.7.3 DICOM Image Management

- **FR-7.4**: System shall support DICOM (Digital Imaging and Communications in Medicine) image management:
  - **DICOM Image Storage**:
    - Store DICOM images (if PACS integrated or local storage)
    - Support DICOM file format (DICOM Part 10 files)
    - Support DICOM network protocols (DICOM C-STORE, C-FIND, C-MOVE, C-GET)
    - Support DICOM image compression (lossless and lossy)
    - Support multiple DICOM image types (CT, MRI, X-ray, Ultrasound, etc.)
  
  - **DICOM Image Viewing**:
    - DICOM image viewer integration (web-based or external viewer)
    - Basic image viewing capabilities (zoom, pan, window/level adjustment)
    - Advanced image viewing (multi-planar reconstruction, 3D rendering, if supported)
    - Image annotation tools (if supported)
    - Image measurement tools (if supported)
    - Comparison viewing (current vs. prior studies)
    - Side-by-side image comparison
  
  - **DICOM Image Retrieval**:
    - Retrieve images from PACS
    - Retrieve images from external systems
    - DICOM query/retrieve functionality
    - Image prefetching (retrieve related prior studies)
    - Image routing (send images to other systems)
  
  - **DICOM Integration**:
    - Integration with PACS systems
    - Integration with DICOM worklist
    - Integration with DICOM modality worklist
    - Support for DICOM structured reporting (SR)
    - Support for DICOM key object selection (KOS)

- **FR-7.5**: System shall support non-DICOM image attachments:
  - **Image File Support**:
    - Support common image formats (JPG, PNG, TIFF, PDF)
    - Support for scanned film images
    - Support for digital camera images
    - Support for smartphone images (if applicable)
    - Image file size limits (configurable)
  
  - **Image Attachment**:
    - Attach images to imaging study records
    - Attach images to clinical notes
    - Link images to reports
    - Image metadata (upload date, uploader, description)
    - Image preview/thumbnail generation

##### 2.2.7.4 Imaging Report Display and Viewing

- **FR-7.6**: System shall provide comprehensive report display capabilities:
  - **Report List View**:
    - Display all reports in chronological order
    - Display reports by study type/modality
    - Display reports by body part
    - Display reports by date range
    - Display reports by ordering provider
    - Display reports by interpreting radiologist
    - Display reports by facility/location
    - Filter reports by status (Final, Preliminary, etc.)
    - Filter reports by findings (Normal, Abnormal, Critical)
    - Search reports by keywords or content
  
  - **Report Detail View**:
    - Display complete report information
    - Display clinical history
    - Display technique (if provided)
    - Display findings prominently
    - Display impression/conclusion prominently
    - Display radiologist information
    - Display study details
    - Display linked images (if available)
    - Display related prior studies (if available)
    - Display report status (Preliminary, Final, etc.)
  
  - **Report Formatting**:
    - Format report text clearly
    - Highlight critical findings (if applicable)
    - Display normal vs. abnormal findings clearly
    - Display measurements prominently (if applicable)
    - Display recommendations clearly (if applicable)
    - Support for structured report display (if available)
    - Print-friendly report format

- **FR-7.7**: System shall support report comparison and trending:
  - **Prior Study Comparison**:
    - Display prior studies of same type
    - Display prior studies of same body part
    - Compare current report to prior reports
    - Highlight changes from prior studies
    - Display progression of findings over time
    - Side-by-side report comparison
  
  - **Study Timeline**:
    - Display imaging studies in chronological timeline
    - Filter timeline by study type
    - Filter timeline by body part
    - Display study frequency and intervals
    - Identify study patterns or trends

##### 2.2.7.5 Critical Finding and Alert Management

- **FR-7.8**: System shall implement comprehensive critical finding alerting:
  - **Critical Finding Detection**:
    - Identify critical findings based on report content
    - Keyword-based critical finding detection
    - Structured critical finding flags (if available in structured reports)
    - Urgency indicators in reports
    - Custom critical finding criteria (configurable by organization)
  
  - **Alert Display**:
    - Prominent alert display when critical findings are reported
    - Alert in patient summary/dashboard
    - Alert in report list
    - Alert in report detail view
    - Visual indicators (red color, bold text, alert icons)
    - Audio alerts (optional, configurable)
    - Pop-up alerts (optional, configurable)
  
  - **Alert Notification**:
    - Notify ordering provider immediately
    - Notify covering provider (if applicable)
    - Notify referring provider (if different from ordering provider)
    - Notify nursing staff (if applicable)
    - Email notifications (optional, configurable)
    - Text message notifications (optional, configurable)
    - Phone call notifications (optional, for life-threatening findings)
    - Escalation if alert not acknowledged within time limit
  
  - **Alert Acknowledgment**:
    - Require acknowledgment of critical finding alerts
    - Track who acknowledged alert and when
    - Track provider response to critical findings
    - Document action taken in response to critical findings
    - Alert acknowledgment workflow

- **FR-7.9**: System shall support other imaging alerts and notifications:
  - **Abnormal Finding Alerts**:
    - Alert for abnormal (but not critical) findings
    - Alert for significant changes from prior studies
    - Alert preferences (user-configurable)
  
  - **Report Status Alerts**:
    - Alert when preliminary reports become final
    - Alert when reports are amended or corrected
    - Alert when reports are delayed
    - Alert when studies are cancelled or not completed
  
  - **Study Availability Notifications**:
    - Notify when ordered studies are completed
    - Notify when reports are available for review
    - Notification preferences (user-configurable)
    - Notification delivery methods (in-system, email, text)

##### 2.2.7.6 Report Documentation and Notes

- **FR-7.10**: System shall support report documentation and notes:
  - **Provider Notes**:
    - Add notes to individual reports
    - Add clinical interpretation notes
    - Add follow-up plan notes
    - Notes linked to specific provider
    - Notes timestamped
    - Notes visible to care team
  
  - **Report Review Documentation**:
    - Mark reports as reviewed
    - Track who reviewed reports and when
    - Document provider response to reports
    - Document clinical action taken based on reports
    - Review status tracking
  
  - **Report Communication**:
    - Document patient notification of results
    - Document patient discussion of results
    - Document result communication method (in-person, phone, portal, etc.)
    - Patient communication preferences

##### 2.2.7.7 Report Correction and Amendment

- **FR-7.11**: System shall support report corrections and amendments:
  - **Report Corrections**:
    - Support corrected reports from radiology
    - Display original report and corrected report
    - Clearly mark corrected reports
    - Reason for correction (if provided)
    - Correction date and time
    - Correction audit trail
  
  - **Report Amendments**:
    - Support amended reports from radiology
    - Display original report and amended report
    - Clearly mark amended reports
    - Reason for amendment (if provided)
    - Amendment date and time
    - Amendment audit trail
  
  - **Report Addendums**:
    - Support addendums to reports
    - Display original report and addendum
    - Clearly mark addendums
    - Addendum date and time
    - Addendum audit trail
  
  - **Report Cancellation**:
    - Support cancelled reports
    - Clearly mark cancelled reports
    - Reason for cancellation (if provided)
    - Cancellation date and time
    - Cancellation audit trail
  
  - **Report History**:
    - Maintain complete history of all report versions
    - Display report change history
    - Track all corrections, amendments, addendums, and cancellations
    - Preserve original reports (never delete)

##### 2.2.7.8 Imaging Study Integration and Workflow

- **FR-7.12**: System shall integrate imaging studies with other system components:
  - **Encounter Integration**:
    - Link studies to specific encounters/visits
    - Display studies in encounter context
    - Add studies to encounter documentation
    - Track which studies were reviewed during encounter
  
  - **Clinical Notes Integration**:
    - Insert reports into clinical notes
    - Link reports to relevant notes
    - Display notes related to reports
    - Update notes based on reports
  
  - **Problem/Diagnosis Integration**:
    - Link reports to relevant problems/diagnoses
    - Display reports related to specific problems
    - Update problem status based on reports
    - Alert when reports support or contradict diagnoses
  
  - **Procedure Integration**:
    - Link imaging studies to procedures (if imaging-guided procedures)
    - Display imaging studies related to procedures
    - Track imaging-guided interventions
  
  - **Order Integration**:
    - Link reports to original orders
    - Display order information with reports
    - Track order-to-report workflow
    - Alert when ordered studies have no reports

##### 2.2.7.9 Imaging Study Reporting and Export

- **FR-7.13**: System shall provide comprehensive imaging study reporting and export:
  - **Patient Reports**:
    - Complete imaging study report
    - Studies by date range
    - Studies by modality/type
    - Studies by body part
    - Studies summary report
    - Comparison reports (current vs. prior)
  
  - **Clinical Reports**:
    - Studies by ordering provider
    - Studies by interpreting radiologist
    - Studies by facility/location
    - Critical finding reports
    - Abnormal finding reports
    - Pending study reports
    - Report review status reports
  
  - **Quality Reports**:
    - Study turnaround time reports
    - Report completeness reports
    - Study quality metrics
    - Radiology department performance reports
    - Radiation dose tracking reports (if applicable)
  
  - **Export Capabilities**:
    - Export reports to PDF
    - Export reports to text file
    - Export reports with images (if applicable)
    - Export reports for patient portal
    - Export reports for external systems
    - Print reports (individual or batch)
    - Print reports with images (if applicable)

##### 2.2.7.10 Imaging Standards and Interoperability

- **FR-7.14**: System shall support imaging study standards and interoperability:
  - **Coding Standards**:
    - CPT codes for study identification (required)
    - SNOMED CT for findings (where applicable)
    - RadLex (Radiology Lexicon) for radiology terminology
    - ICD-10 for clinical indications (where applicable)
  
  - **Message Standards**:
    - HL7 V2 ORU (Observation Result) messages for reports
    - HL7 FHIR DiagnosticReport resources
    - HL7 FHIR ImagingStudy resources
    - C-CDA (Consolidated Clinical Document Architecture) for report sharing
    - DICOM structured reporting (SR)
  
  - **DICOM Standards**:
    - DICOM Part 10 (file format)
    - DICOM network protocols (C-STORE, C-FIND, C-MOVE, C-GET)
    - DICOM worklist management
    - DICOM modality worklist
    - DICOM key object selection (KOS)
  
  - **Interoperability**:
    - Integration with Radiology Information Systems (RIS)
    - Integration with PACS (Picture Archiving and Communication Systems)
    - Integration with Health Information Exchanges (HIE)
    - Integration with other EHR systems
    - Integration with external imaging facilities
    - Support for study sharing with external providers
    - Support for patient access to reports and images (via portal)

##### 2.2.7.11 Imaging Study Security and Privacy

- **FR-7.15**: System shall implement appropriate security and privacy controls for imaging studies:
  - **Access Control**:
    - Role-based access to reports and images
    - Some users may have read-only access
    - Some users may have restricted access to certain study types
    - Minimum necessary principle
    - Break-the-glass functionality for emergency access
  
  - **Privacy Controls**:
    - Sensitive study flags (e.g., psychiatric imaging, genetic imaging)
    - Restricted access to sensitive studies
    - Patient privacy preferences affecting study visibility
    - Support for patient requests to restrict study access
    - Audit logging of all study and report access
  
  - **Data Security**:
    - Report data encrypted at rest and in transit
    - Image data encrypted at rest and in transit (if stored)
    - Secure study and report transmission
    - Study and report access logging
    - Unauthorized access prevention
    - Study and report retention and disposal policies
    - HIPAA compliance for imaging data

##### 2.2.7.12 Imaging Study Quality and Validation

- **FR-7.16**: System shall implement imaging study quality controls:
  - **Data Validation**:
    - Validate CPT codes
    - Validate study dates and times
    - Validate required fields
    - Validate report completeness
    - Validate image availability (if applicable)
  
  - **Quality Checks**:
    - Identify missing reports (ordered but not received)
    - Identify delayed reports
    - Identify duplicate studies
    - Identify studies with missing information
    - Identify reports requiring review
    - Study completeness checks
  
  - **Quality Metrics**:
    - Report turnaround time tracking
    - Report accuracy tracking
    - Report completeness tracking
    - Report review compliance tracking
    - Study scheduling efficiency
    - Quality metric reporting
    - Radiation dose tracking (if applicable, for dose optimization)

#### 2.2.8 Allergies and Adverse Reactions

##### 2.2.8.1 Allergy and Adverse Reaction Documentation

- **FR-8.1**: System shall maintain comprehensive allergy and adverse reaction lists with the following information for each allergy:
  - **Allergen Identification**:
    - Allergen name (required, free text or coded)
    - Allergen type (required, dropdown):
      - Medication/Drug
      - Food
      - Environmental (pollen, dust, mold, etc.)
      - Latex
      - Contrast agent (IV contrast, oral contrast, etc.)
      - Other (with specification)
    - Allergen code (optional, for medications: NDC, RxNorm, for others: SNOMED CT)
    - Allergen category (for medications: Drug class, e.g., Penicillin, Sulfa, etc.)
    - Specific allergen (if allergen type is a class, specify exact substance)
    - Cross-reactivity information (optional, other substances patient may be allergic to)
  
  - **Reaction Information**:
    - Reaction type (required, dropdown or free text):
      - Anaphylaxis
      - Rash/Hives
      - Respiratory (wheezing, shortness of breath)
      - Gastrointestinal (nausea, vomiting, diarrhea)
      - Cardiovascular (hypotension, tachycardia)
      - Other (with specification)
    - Reaction description (required, free text for detailed description)
    - Reaction severity (required, dropdown):
      - Mild
      - Moderate
      - Severe
      - Life-threatening
      - Unknown
    - Onset of reaction (Immediate, Delayed, Unknown)
    - Duration of reaction (if known, optional)
  
  - **Temporal Information**:
    - Date of first occurrence (required, can be approximate: year, month/year, or exact date)
    - Date of most recent occurrence (if multiple occurrences, optional)
    - Date allergy was first documented (auto-populated)
    - Date allergy was last updated (auto-populated)
  
  - **Verification and Status**:
    - Verification status (required, dropdown):
      - Confirmed (verified by testing or clear history)
      - Unconfirmed (patient-reported, not verified)
      - Confirmed by testing (skin test, blood test, etc.)
      - Confirmed by challenge (oral challenge, etc.)
      - Ruled out (tested negative, not allergic)
      - Unknown
    - Verification method (if confirmed by testing, optional):
      - Skin test
      - Blood test (RAST, specific IgE)
      - Oral challenge
      - Patch test
      - Clinical history only
      - Other
    - Verification date (if applicable, optional)
    - Allergy status (Active, Inactive, Resolved, Unknown)
    - Date resolved (if resolved, optional)
  
  - **Provider Information**:
    - Documenting provider (required, user who added allergy)
    - Verifying provider (if different from documenting provider, optional)
    - Provider specialty (optional, auto-populated from provider profile)
    - Documenting facility/location (optional)
  
  - **Clinical Context**:
    - Clinical significance (High, Medium, Low, optional)
    - Notes/comments (optional, free text for additional context)
    - Related allergies (optional, link to other allergies)
    - Related medications (optional, link to medications that may contain allergen)
    - Patient education provided (Yes/No, optional)
    - Medical alert bracelet recommended (Yes/No, optional)

##### 2.2.8.2 Allergy Entry and Management

- **FR-8.2**: System shall support multiple methods for adding allergies to the allergy list:
  - **Manual Entry**:
    - Direct entry of allergen name with type selection
    - Entry from allergen search/selection
    - Entry from template or common allergens list
    - Entry from previous encounters (copy from visit documentation)
    - Entry from medication history (promote adverse drug reaction to allergy)
  
  - **From Clinical Documentation**:
    - Extract allergies from clinical notes
    - Link allergies to specific encounters/visits
    - Add allergies during note creation
    - Add allergies during medication reconciliation
  
  - **From External Sources**:
    - Import allergies from other EHR systems
    - Import allergies from Health Information Exchange (HIE)
    - Import allergies from discharge summaries
    - Import allergies from medication lists
    - Import allergies from patient portal (if available)
  
  - **Bulk Entry**:
    - Add multiple allergies at once
    - Import allergy list from external file
    - Copy allergy list from another patient (with modification, if applicable)

- **FR-8.3**: System shall support allergy modification and updates:
  - **Status Updates**:
    - Change allergy status (e.g., Active to Inactive, Active to Resolved)
    - Update verification status (e.g., Unconfirmed to Confirmed)
    - Update resolution date when marking as resolved
    - Reactivate inactive allergies (with reason)
  
  - **Content Updates**:
    - Edit allergen name/description (with audit trail)
    - Update reaction information
    - Update severity
    - Update dates (first occurrence, verification)
    - Add or modify notes/comments
    - Update verification status and method
    - Link or unlink related allergies
  
  - **Allergy Merging**:
    - Merge duplicate allergies
    - Combine similar allergies into one
    - Merge with audit trail and reason
  
  - **Allergy Deletion**:
    - Remove allergies from list (with reason and audit trail)
    - Soft delete (mark as deleted but retain in history)
    - Hard delete (permanent removal, with high-level authorization)
    - Deletion history maintained in audit trail

- **FR-8.4**: System shall support allergy verification workflows:
  - **Verification Process**:
    - Mark allergies as "Confirmed" or "Unconfirmed"
    - Require provider verification for certain allergy types
    - Support allergy verification by multiple providers
    - Track verification status and date
    - Display verification status prominently
    - Alert for unconfirmed allergies requiring verification
  
  - **Allergy Testing Integration**:
    - Link allergies to allergy test results
    - Display test results with allergy information
    - Update allergy status based on test results
    - Support for skin test results
    - Support for blood test results (RAST, specific IgE)
    - Support for challenge test results

##### 2.2.8.3 Drug Allergy and Medication Interaction Checking

- **FR-8.5**: System shall implement comprehensive drug allergy checking:
  - **Allergy Checking**:
    - Check all medication orders against patient allergy list
    - Check for exact allergen matches
    - Check for drug class matches (e.g., if allergic to Penicillin, check all Penicillins)
    - Check for cross-reactivity (e.g., Penicillin and Cephalosporin)
    - Check for ingredient matches (e.g., if allergic to dye, check medications with that dye)
    - Check for similar chemical structures
  
  - **Alert Display**:
    - Display allergy alerts prominently when ordering medications
    - Display allergy alerts when viewing medication list
    - Display allergy alerts in prescription creation
    - Visual indicators (red color, bold text, alert icons)
    - Audio alerts (optional, configurable)
    - Pop-up alerts (optional, configurable)
    - Alert severity based on allergy severity and reaction type
  
  - **Alert Information**:
    - Display allergen name
    - Display reaction type and severity
    - Display verification status
    - Display date of occurrence
    - Display alternative medication suggestions (if available)
    - Display cross-reactivity warnings
  
  - **Alert Override**:
    - Allow provider to override allergy alert (with reason and documentation)
    - Require acknowledgment of override
    - Track all allergy alert overrides
    - Override audit trail
    - Supervisor approval for critical allergy overrides (optional, configurable)

- **FR-8.6**: System shall support drug allergy database integration:
  - **Drug Database Integration**:
    - Integration with drug information databases
    - Drug ingredient checking
    - Drug class identification
    - Cross-reactivity database
    - Alternative medication suggestions
    - Drug interaction database (drug-drug interactions separate from allergies)
  
  - **Allergy Database Maintenance**:
    - Regular updates to allergy/drug databases
    - Cross-reactivity rule updates
    - Drug class mapping updates
    - Ingredient database updates

##### 2.2.8.4 Allergy List Display and Organization

- **FR-8.7**: System shall provide comprehensive allergy list display:
  - **Display Options**:
    - Display all allergies in list format
    - Display allergies by type (Medication, Food, Environmental, etc.)
    - Display allergies by severity (Life-threatening, Severe, Moderate, Mild)
    - Display allergies by verification status (Confirmed, Unconfirmed)
    - Display allergies by status (Active, Inactive, Resolved)
    - Display allergies in chronological order (by date of occurrence)
    - Display allergies alphabetically (by allergen name)
    - Highlight critical allergies (life-threatening, severe)
    - Display "No Known Allergies" if no allergies documented
  
  - **Allergy List Views**:
    - Summary view (allergen name, reaction, severity)
    - Detailed view (all allergy information)
    - Timeline view (allergies displayed chronologically)
    - Category view (allergies grouped by type)
    - Verification view (allergies grouped by verification status)
  
  - **Allergy List Customization**:
    - User preferences for default sort order
    - User preferences for which allergies to display
    - Ability to collapse/expand allergy categories
    - Ability to filter allergies by type, severity, status, verification status

- **FR-8.8**: System shall display allergies prominently in patient record:
  - **Patient Summary Display**:
    - Display allergies in patient summary/dashboard
    - Display allergies prominently at top of patient record
    - Display critical allergies with special highlighting
    - Display allergies in encounter/visit views
    - Display allergies in medication ordering screens
    - Display allergies in prescription creation screens
  
  - **Alert Banners**:
    - Allergy alert banner in patient record
    - Critical allergy alert banner (for life-threatening allergies)
    - Unconfirmed allergy alert banner (for allergies requiring verification)
    - Allergy alert in medication lists
    - Allergy alert in order entry screens

##### 2.2.8.5 Adverse Drug Reaction (ADR) Management

- **FR-8.9**: System shall support adverse drug reaction documentation:
  - **ADR Documentation**:
    - Distinguish between allergies and adverse drug reactions
    - Document adverse drug reactions separately from allergies
    - Link adverse drug reactions to specific medications
    - Document reaction details (type, severity, onset, duration)
    - Document causality assessment (Definite, Probable, Possible, Unlikely)
    - Document action taken (Medication stopped, Dose reduced, Continued, etc.)
  
  - **ADR vs. Allergy**:
    - Support for promoting ADR to allergy (if appropriate)
    - Support for demoting allergy to ADR (if appropriate)
    - Clear distinction between allergy and ADR in display
    - Different alerting for allergies vs. ADRs
  
  - **ADR Reporting**:
    - Support for adverse event reporting
    - Integration with pharmacovigilance systems (if applicable)
    - ADR reporting to regulatory agencies (if applicable)
    - ADR tracking and trending

##### 2.2.8.6 Allergy Integration and Workflow

- **FR-8.10**: System shall integrate allergies with other system components:
  - **Medication Integration**:
    - Link allergies to medications
    - Alert when medications are ordered that match allergies
    - Alert when medications contain allergen ingredients
    - Display allergies in medication lists
    - Display allergies in prescription creation
    - Prevent prescription of medications with known allergies (with override capability)
  
  - **Encounter Integration**:
    - Link allergies to specific encounters/visits
    - Display allergies in encounter context
    - Add allergies during encounter documentation
    - Track which allergies were reviewed during encounter
    - Allergy reconciliation during encounters
  
  - **Clinical Notes Integration**:
    - Insert allergies into clinical notes
    - Link allergies to relevant notes
    - Display notes related to allergies
    - Update notes based on allergy information
  
  - **Problem/Diagnosis Integration**:
    - Link allergies to relevant problems/diagnoses
    - Display allergies related to specific problems
    - Alert when allergies may be related to diagnoses
  
  - **Procedure Integration**:
    - Alert for contrast allergies before imaging studies
    - Alert for medication allergies before procedures
    - Alert for latex allergies before procedures
    - Display allergies in procedure documentation

##### 2.2.8.7 Allergy Reporting and Analytics

- **FR-8.11**: System shall provide reporting capabilities for allergies:
  - **Patient-Level Reports**:
    - Complete allergy list report
    - Allergies by type
    - Allergies by severity
    - Allergies by verification status
    - Allergy history report
    - Allergy changes over time
  
  - **Population-Level Reports**:
    - Allergy prevalence by allergen type
    - Allergy prevalence by severity
    - Most common allergies in patient population
    - Unconfirmed allergies requiring verification
    - Allergy verification rates
    - Allergy list completeness
  
  - **Clinical Reports**:
    - Allergies by provider
    - Allergies by facility/location
    - Allergy alert frequency
    - Allergy alert override rates
    - Drug allergy interactions
    - Allergy testing results
  
  - **Quality Reports**:
    - Allergies missing verification
    - Allergies with incomplete information
    - Duplicate allergies
    - Allergies requiring review
    - Allergy list data quality scores
  
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.8.8 Allergy Standards and Interoperability

- **FR-8.12**: System shall support allergy standards and interoperability:
  - **Coding Standards**:
    - RxNorm for medication allergens
    - NDC (National Drug Code) for medication allergens
    - SNOMED CT for allergen types and reactions
    - UNII (Unique Ingredient Identifier) for substances
    - ICD-10 for allergy-related diagnoses (if applicable)
  
  - **Message Standards**:
    - HL7 V2 AL1 (Patient Allergy Information) segment
    - HL7 FHIR AllergyIntolerance resource
    - C-CDA (Consolidated Clinical Document Architecture) for allergy sharing
    - IHE (Integrating the Healthcare Enterprise) profiles for allergy exchange
  
  - **Interoperability**:
    - Integration with Health Information Exchanges (HIE)
    - Integration with other EHR systems
    - Integration with pharmacy systems
    - Integration with medication management systems
    - Support for allergy sharing with external providers
    - Support for patient access to allergies (via portal)
    - Support for patient-reported allergies (via portal)

##### 2.2.8.9 Allergy Security and Privacy

- **FR-8.13**: System shall implement appropriate security and privacy controls for allergies:
  - **Access Control**:
    - Role-based access to allergies
    - Some users may have read-only access
    - Some users may have restricted access to certain allergy types
    - Minimum necessary principle
    - Break-the-glass functionality for emergency access
  
  - **Privacy Controls**:
    - Sensitive allergy flags (if applicable)
    - Restricted access to sensitive allergies
    - Patient privacy preferences affecting allergy visibility
    - Support for patient requests to restrict allergy access
    - Audit logging of all allergy access
  
  - **Data Security**:
    - Allergy data encrypted at rest and in transit
    - Secure allergy transmission
    - Allergy access logging
    - Unauthorized access prevention
    - Allergy retention and disposal policies
    - HIPAA compliance for allergy data

##### 2.2.8.10 Allergy Quality and Validation

- **FR-8.14**: System shall implement allergy quality controls with the following specific validation rules:
  - **Allergen Name Validation**:
    - Allergen name: Required, 1-200 characters
    - Allergen name: Cannot be empty or only spaces
    - Allergen name: Cannot contain only special characters or numbers
    - Allergen name: Must be meaningful (cannot be generic like "Drug" or "Food" without specification)
    - Allergen name format: Alphanumeric and common characters allowed
    - Duplicate allergen detection: Warn if same allergen entered multiple times
  
  - **Allergen Code Validation** (if provided):
    - NDC code: Must be 10 or 11 digits if provided (format: XXXXX-XXXX-XX or XXXXXXXXXX)
    - RxNorm code: Must be valid RxNorm code format if provided
    - SNOMED CT code: Must be valid SNOMED CT format if provided
    - Code existence validation: Code must exist in current code set
    - Code-allergen name consistency: Code must match allergen name
    - Code format validation: Must follow code set format standards
  
  - **Date Validation**:
    - First occurrence date: Cannot be in the future
    - First occurrence date: Cannot be more than 150 years in the past (warn if > 120 years)
    - Most recent occurrence date: Cannot be before first occurrence date
    - Most recent occurrence date: Cannot be in the future
    - Verification date: Cannot be before first occurrence date
    - Verification date: Cannot be in the future
    - Resolution date: Cannot be before first occurrence date (if applicable)
    - Resolution date: Cannot be in the future
    - Date format validation: Must be valid calendar date
  
  - **Reaction Description Validation**:
    - Reaction description: Required, 1-1000 characters
    - Reaction description: Cannot be empty or only spaces
    - Reaction description: Must be meaningful (cannot be generic like "Reaction" without details)
    - Reaction type: Must match reaction description (e.g., cannot have "Anaphylaxis" type with "Rash" description)
    - Reaction severity: Required, must be from valid list (Mild, Moderate, Severe, Life-threatening, Unknown)
  
  - **Required Fields Validation**:
    - Allergen name: Required
    - Allergen type: Required (Medication, Food, Environmental, etc.)
    - Reaction type: Required
    - Reaction description: Required
    - Reaction severity: Required
    - First occurrence date: Required
    - Verification status: Required
    - Allergy status: Required (Active, Inactive, Resolved, Unknown)
    - Documenting provider: Required
  
  - **Quality Checks**:
    - Identify missing allergies (patients with no allergy documentation)
    - Identify duplicate allergies
    - Identify allergies with missing information
    - Identify unconfirmed allergies requiring verification
    - Identify allergies requiring review
    - Allergy completeness checks
    - "No Known Allergies" documentation verification
  
  - **Quality Metrics**:
    - Allergy documentation completeness
    - Allergy verification rates
    - Allergy alert accuracy
    - Allergy alert override rates
    - Allergy data quality scores
    - Quality metric reporting

##### 2.2.8.11 Allergy Workflow and Best Practices

- **FR-8.15**: System shall support allergy workflows and best practices:
  - **Allergy Documentation Workflow**:
    - Document allergies during patient registration
    - Review allergies at each encounter
    - Update allergies as information changes
    - Verify allergies when appropriate
    - Resolve allergies when appropriate
    - Allergy reconciliation during medication reconciliation
  
  - **Allergy Review Workflow**:
    - Periodic allergy list review
    - Allergy list review reminders
    - Allergy list review documentation
    - Update allergy list during review
    - Verify unconfirmed allergies during review
  
  - **Allergy Best Practices**:
    - Support for allergy documentation guidelines
    - Reminders for allergy verification
    - Templates for common allergies
    - Clinical decision support for allergy management
    - Integration with evidence-based guidelines
    - Support for allergy testing recommendations

#### 2.2.9 Medications (Current and Historical)

##### 2.2.9.1 Current Medication List Management

- **FR-9.1**: System shall maintain comprehensive current medication lists with the following information for each medication:
  - **Medication Identification**:
    - Medication name (required, generic and/or brand name)
    - Generic name (required, for standardization)
    - Brand name (if applicable, optional)
    - NDC (National Drug Code, optional but recommended)
    - RxNorm code (optional, for standardized medication identification)
    - Drug class (optional, e.g., ACE Inhibitor, Beta Blocker, etc.)
    - Medication type (Prescription, Over-the-counter, Herbal/Supplement, Other)
  
  - **Dosage Information**:
    - Dosage strength (required, e.g., 10 mg, 500 mg)
    - Dosage form (required, e.g., Tablet, Capsule, Liquid, Injection, Topical, etc.)
    - Quantity per dose (required, e.g., 1 tablet, 2 capsules, 5 ml)
    - Total quantity (optional, e.g., 30 tablets, 100 ml)
    - Unit of measure (required, e.g., mg, ml, units)
  
  - **Administration Instructions**:
    - Frequency/schedule (required, e.g., Once daily, Twice daily, Every 8 hours, As needed)
    - Route of administration (required, e.g., Oral, IV, IM, Topical, Sublingual, etc.)
    - Timing instructions (optional, e.g., With meals, Before meals, At bedtime)
    - Special instructions (optional, free text, e.g., "Take with food", "Do not crush")
    - Duration of treatment (optional, e.g., 10 days, 3 months, Ongoing)
    - End date (if known, optional)
  
  - **Prescription Information**:
    - Prescribing provider (required)
    - Prescription date (required)
    - Prescription number (optional, if available)
    - Pharmacy (optional, where medication was filled)
    - Number of refills authorized (optional)
    - Remaining refills (optional)
    - Last filled date (optional)
    - Next refill date (optional, auto-calculated)
  
  - **Medication Status**:
    - Status (Active, Discontinued, On Hold, Completed, Unknown)
    - Start date (required)
    - Stop date (if discontinued, optional)
    - Discontinuation reason (if discontinued, optional):
      - Adverse reaction
      - Allergic reaction
      - Not effective
      - Patient request
      - Provider decision
      - Cost/insurance issue
      - Other (with specification)
    - Hold reason (if on hold, optional)
    - Hold start date (if on hold, optional)
    - Hold end date (if on hold, optional)
  
  - **Clinical Context**:
    - Indication/reason for medication (optional, free text or linked to problem/diagnosis)
    - Linked problem/diagnosis (optional, what condition this medication treats)
    - Monitoring requirements (optional, e.g., "Monitor liver function", "Check blood pressure")
    - Patient response (optional, e.g., "Effective", "Not effective", "Side effects")
    - Notes/comments (optional, free text for additional context)
    - Source of information (Prescription, Patient reported, Pharmacy, Other)

##### 2.2.9.2 Medication Entry and Management

- **FR-9.2**: System shall support multiple methods for adding medications to the medication list:
  - **From Prescriptions**:
    - Automatic addition when prescription is created
    - Link medication to prescription
    - Update medication when prescription is modified
    - Remove medication when prescription is cancelled
  
  - **Manual Entry**:
    - Direct entry of medication information
    - Entry from medication search/selection
    - Entry from template or common medications list
    - Entry from previous encounters (copy from visit documentation)
    - Entry from medication history (reactivate historical medication)
  
  - **From Clinical Documentation**:
    - Extract medications from clinical notes
    - Link medications to specific encounters/visits
    - Add medications during note creation
    - Add medications during medication reconciliation
  
  - **From External Sources**:
    - Import medications from other EHR systems
    - Import medications from Health Information Exchange (HIE)
    - Import medications from discharge summaries
    - Import medications from pharmacy systems
    - Import medications from patient portal (if available)
    - Import medications from medication lists provided by patients
  
  - **Medication Reconciliation**:
    - Compare current medications with previous lists
    - Identify new medications
    - Identify discontinued medications
    - Identify changed medications
    - Resolve discrepancies
    - Document reconciliation process

- **FR-9.3**: System shall support medication modification and updates:
  - **Dosage Updates**:
    - Change dosage strength
    - Change frequency/schedule
    - Change route of administration
    - Change quantity
    - Update special instructions
    - Update timing instructions
  
  - **Status Updates**:
    - Discontinue medication (with reason and date)
    - Hold medication (with reason and dates)
    - Reactivate medication (from hold or discontinued status)
    - Mark medication as completed
    - Update medication status
  
  - **Information Updates**:
    - Edit medication name (with audit trail)
    - Update indication/reason
    - Update linked problem/diagnosis
    - Add or modify notes/comments
    - Update monitoring requirements
    - Update patient response
  
  - **Medication Merging**:
    - Merge duplicate medications
    - Combine similar medications into one
    - Merge with audit trail and reason
  
  - **Medication Deletion**:
    - Remove medications from current list (with reason and audit trail)
    - Soft delete (mark as deleted but retain in history)
    - Hard delete (permanent removal, with high-level authorization)
    - Deletion history maintained in audit trail

##### 2.2.9.3 Medication History

- **FR-9.4**: System shall maintain comprehensive medication history:
  - **Historical Medication Tracking**:
    - All past medications (discontinued, completed, or stopped)
    - Complete medication history from first prescription to current
    - Historical medication information (same details as current medications)
    - Date ranges for each historical medication
    - Reason for discontinuation (if applicable)
    - Duration of medication use
    - Effectiveness of medication (if documented)
  
  - **History Display**:
    - Display historical medications separately from current medications
    - Display historical medications in chronological order
    - Display historical medications by date range
    - Display historical medications by medication type
    - Display historical medications by indication
    - Filter historical medications by various criteria
    - Search historical medications by name, indication, or date
  
  - **History Access**:
    - Quick access to medication history
    - Compare current medications to historical medications
    - Identify medications that were tried previously
    - Identify medications that were discontinued due to adverse effects
    - Identify medications that were not effective
    - Support for medication history analysis

- **FR-9.5**: System shall support medication history documentation:
  - **Historical Entry**:
    - Add historical medications manually
    - Import historical medications from external sources
    - Promote current medications to history when discontinued
    - Document historical medication details
    - Document historical medication effectiveness
  
  - **History Maintenance**:
    - Update historical medication information
    - Correct historical medication errors
    - Merge duplicate historical medications
    - Archive very old historical medications (with retention policies)

##### 2.2.9.4 Medication Reconciliation

- **FR-9.6**: System shall support comprehensive medication reconciliation:
  - **Reconciliation Process**:
    - Compare medication lists from different sources
    - Identify discrepancies between lists
    - Display side-by-side comparison
    - Highlight differences (new, changed, discontinued medications)
    - Resolve discrepancies
    - Document reconciliation decisions
    - Track reconciliation completion
  
  - **Reconciliation Sources**:
    - Current medication list in EHR
    - Previous medication list (from last encounter)
    - Patient-reported medications
    - Pharmacy medication list
    - Discharge medication list (from hospital)
    - External provider medication list
    - Medication list from other EHR systems
  
  - **Reconciliation Workflow**:
    - Initiate reconciliation at encounter start
    - Review each medication
    - Confirm, add, modify, or discontinue medications
    - Document reconciliation decisions
    - Complete reconciliation
    - Update current medication list
    - Reconciliation reminders and alerts

- **FR-9.7**: System shall support medication reconciliation documentation:
  - **Reconciliation Documentation**:
    - Document who performed reconciliation
    - Document when reconciliation was performed
    - Document reconciliation decisions
    - Document discrepancies found
    - Document resolution of discrepancies
    - Document patient involvement in reconciliation
    - Reconciliation completion status
  
  - **Reconciliation Reports**:
    - Reconciliation completion reports
    - Discrepancy reports
    - Reconciliation quality metrics
    - Reconciliation compliance reports

##### 2.2.9.5 Medication List Display and Organization

- **FR-9.8**: System shall provide comprehensive medication list display:
  - **Display Options**:
    - Display all current medications in list format
    - Display medications by medication type (Prescription, OTC, Herbal)
    - Display medications by indication/problem
    - Display medications by prescribing provider
    - Display medications by start date (newest first or oldest first)
    - Display medications alphabetically (by medication name)
    - Display medications by drug class
    - Highlight medications requiring attention (e.g., due for refill, on hold)
    - Display "No Current Medications" if no medications documented
  
  - **Medication List Views**:
    - Summary view (medication name, dosage, frequency)
    - Detailed view (all medication information)
    - Timeline view (medications displayed chronologically)
    - Category view (medications grouped by type or indication)
    - Provider view (medications grouped by prescribing provider)
    - Problem view (medications grouped by linked problem)
  
  - **Medication List Customization**:
    - User preferences for default sort order
    - User preferences for which medications to display
    - Ability to collapse/expand medication categories
    - Ability to filter medications by type, status, provider, indication
    - Ability to group medications by various criteria

- **FR-9.9**: System shall display medications prominently in patient record:
  - **Patient Summary Display**:
    - Display medications in patient summary/dashboard
    - Display medications prominently in patient record
    - Display medications in encounter/visit views
    - Display medications in medication ordering screens
    - Display medications in prescription creation screens
    - Display medications in clinical notes
  
  - **Medication Alerts**:
    - Alert for medications due for refill
    - Alert for medications on hold
    - Alert for medications with interactions
    - Alert for medications with allergies
    - Alert for medications requiring monitoring
    - Alert for medications with missing information

##### 2.2.9.6 Medication Interactions and Safety Checking

- **FR-9.10**: System shall implement comprehensive medication interaction checking:
  - **Interaction Types**:
    - Drug-drug interactions
    - Drug-allergy interactions
    - Drug-disease interactions (contraindications)
    - Drug-food interactions
    - Drug-lab interactions (medications affecting lab results)
    - Duplicate therapy (same or similar medications)
    - Drug-age interactions (pediatric, geriatric)
    - Drug-pregnancy interactions (if applicable)
    - Drug-lactation interactions (if applicable)
  
  - **Interaction Severity**:
    - Critical (must not prescribe together)
    - Major (requires caution, monitoring, or dose adjustment)
    - Moderate (monitor closely)
    - Minor (informational)
    - Unknown (interaction not well documented)
  
  - **Alert Display**:
    - Display interaction alerts prominently when ordering medications
    - Display interaction alerts when viewing medication list
    - Display interaction alerts in prescription creation
    - Visual indicators (red for critical, yellow for major, etc.)
    - Audio alerts (optional, configurable)
    - Pop-up alerts (optional, configurable)
    - Alert severity based on interaction severity
  
  - **Alert Information**:
    - Display interacting medications
    - Display interaction type and severity
    - Display interaction description
    - Display clinical significance
    - Display management recommendations
    - Display alternative medication suggestions (if available)
  
  - **Alert Override**:
    - Allow provider to override interaction alert (with reason and documentation)
    - Require acknowledgment of override
    - Track all interaction alert overrides
    - Override audit trail
    - Supervisor approval for critical interaction overrides (optional, configurable)

- **FR-9.11**: System shall support medication interaction database integration:
  - **Interaction Database Integration**:
    - Integration with drug interaction databases
    - Regular updates to interaction databases
    - Evidence-based interaction information
    - Clinical significance assessment
    - Management recommendations
    - Alternative medication suggestions
  
  - **Interaction Database Maintenance**:
    - Regular updates to interaction rules
    - New interaction identification
    - Interaction severity updates
    - Clinical evidence updates

##### 2.2.9.7 Medication Adherence and Monitoring

- **FR-9.12**: System shall support medication adherence tracking:
  - **Adherence Information**:
    - Medication adherence status (if available from pharmacy or patient)
    - Adherence percentage (if available)
    - Missed doses (if documented)
    - Refill history
    - Last filled date
    - Days supply remaining
    - Adherence alerts (if patient not filling medications)
  
  - **Adherence Monitoring**:
    - Track medication refills
    - Identify medications not being refilled
    - Identify medications with poor adherence
    - Adherence reports
    - Adherence interventions
  
  - **Patient Education**:
    - Medication education materials
    - Medication instructions
    - Medication adherence counseling
    - Patient education documentation

- **FR-9.13**: System shall support medication monitoring requirements:
  - **Monitoring Documentation**:
    - Lab monitoring requirements (e.g., "Monitor liver function", "Check INR")
    - Vital sign monitoring requirements (e.g., "Monitor blood pressure")
    - Clinical monitoring requirements (e.g., "Monitor for side effects")
    - Monitoring frequency
    - Monitoring alerts and reminders
    - Monitoring result tracking
  
  - **Monitoring Alerts**:
    - Alert when monitoring is due
    - Alert when monitoring results are abnormal
    - Alert when monitoring is overdue
    - Monitoring compliance tracking

##### 2.2.9.8 Medication Integration and Workflow

- **FR-9.14**: System shall integrate medications with other system components:
  - **Prescription Integration**:
    - Link medications to prescriptions
    - Automatic medication list updates from prescriptions
    - Display prescriptions with medications
    - Track prescription-to-medication workflow
  
  - **Encounter Integration**:
    - Link medications to specific encounters/visits
    - Display medications in encounter context
    - Add medications during encounter documentation
    - Track which medications were reviewed during encounter
    - Medication reconciliation during encounters
  
  - **Clinical Notes Integration**:
    - Insert medications into clinical notes
    - Link medications to relevant notes
    - Display notes related to medications
    - Update notes based on medication information
  
  - **Problem/Diagnosis Integration**:
    - Link medications to relevant problems/diagnoses
    - Display medications related to specific problems
    - Display problems treated by each medication
    - Update problem status based on medication effectiveness
  
  - **Allergy Integration**:
    - Link medications to allergies
    - Alert when medications match allergies
    - Display allergies in medication context
    - Prevent prescription of medications with known allergies (with override capability)
  
  - **Lab/Imaging Integration**:
    - Link medications to lab results (medications affecting lab values)
    - Link medications to imaging studies (contrast agents, etc.)
    - Display medications in lab/imaging context

##### 2.2.9.9 Medication Reporting and Analytics

- **FR-9.15**: System shall provide reporting capabilities for medications:
  - **Patient-Level Reports**:
    - Complete medication list report
    - Current medications report
    - Historical medications report
    - Medications by indication
    - Medications by provider
    - Medication changes over time
    - Medication adherence report
  
  - **Population-Level Reports**:
    - Medication prevalence by drug class
    - Medication prevalence by indication
    - Most prescribed medications
    - Medication utilization patterns
    - Medication adherence rates
    - Medication interaction frequency
    - Medication list completeness
  
  - **Clinical Reports**:
    - Medications by prescribing provider
    - Medications by facility/location
    - Medication interaction reports
    - Medication allergy reports
    - Medication monitoring compliance
    - Medication reconciliation completion
  
  - **Quality Reports**:
    - Medications missing information
    - Medications with missing indications
    - Duplicate medications
    - Medications requiring review
    - Medication list data quality scores
    - Medication reconciliation compliance
  
  - Reports shall be exportable in multiple formats (PDF, Excel, CSV)
  - Reports shall support filtering, sorting, and customization

##### 2.2.9.10 Medication Standards and Interoperability

- **FR-9.16**: System shall support medication standards and interoperability:
  - **Coding Standards**:
    - RxNorm for medication identification (preferred)
    - NDC (National Drug Code) for medication identification
    - SNOMED CT for medication-related concepts
    - UNII (Unique Ingredient Identifier) for substances
    - ICD-10 for medication-related diagnoses (if applicable)
  
  - **Message Standards**:
    - HL7 V2 RXD (Pharmacy/Treatment Dispense) segment
    - HL7 FHIR MedicationStatement resource
    - HL7 FHIR MedicationRequest resource
    - C-CDA (Consolidated Clinical Document Architecture) for medication sharing
    - IHE (Integrating the Healthcare Enterprise) profiles for medication exchange
  
  - **Interoperability**:
    - Integration with Health Information Exchanges (HIE)
    - Integration with other EHR systems
    - Integration with pharmacy systems
    - Integration with prescription management systems
    - Integration with medication management systems
    - Support for medication sharing with external providers
    - Support for patient access to medications (via portal)
    - Support for patient-reported medications (via portal)

##### 2.2.9.11 Medication Security and Privacy

- **FR-9.17**: System shall implement appropriate security and privacy controls for medications:
  - **Access Control**:
    - Role-based access to medications
    - Some users may have read-only access
    - Some users may have restricted access to certain medication types
    - Minimum necessary principle
    - Break-the-glass functionality for emergency access
  
  - **Privacy Controls**:
    - Sensitive medication flags (e.g., psychiatric medications, HIV medications)
    - Restricted access to sensitive medications
    - Patient privacy preferences affecting medication visibility
    - Support for patient requests to restrict medication access
    - Audit logging of all medication access
  
  - **Data Security**:
    - Medication data encrypted at rest and in transit
    - Secure medication transmission
    - Medication access logging
    - Unauthorized access prevention
    - Medication retention and disposal policies
    - HIPAA compliance for medication data

##### 2.2.9.12 Medication Quality and Validation

- **FR-9.18**: System shall implement medication quality controls with the following specific validation rules:
  - **Medication Name Validation**:
    - Medication name: Required, 1-200 characters
    - Generic name: Required, 1-200 characters
    - Brand name: Optional, 1-200 characters if provided
    - Medication name: Cannot be empty or only spaces
    - Medication name: Cannot contain only special characters or numbers
    - Medication name: Must be meaningful (cannot be generic like "Drug" without specification)
    - Duplicate medication detection: Warn if same medication entered multiple times with same dosage
  
  - **Medication Code Validation** (if provided):
    - NDC code: Must be 10 or 11 digits if provided (format: XXXXX-XXXX-XX or XXXXXXXXXX)
    - RxNorm code: Must be valid RxNorm code format if provided
    - Code existence validation: Code must exist in current code set
    - Code-medication name consistency: Code must match medication name
    - Code format validation: Must follow code set format standards
  
  - **Dosage Validation**:
    - Dosage strength: Required, must be positive numeric value
    - Dosage strength: Must be within reasonable range for medication (e.g., cannot be 0 or negative)
    - Dosage strength: Must be reasonable for medication type (warn for unusually high/low dosages)
    - Dosage form: Required, must be from valid list (Tablet, Capsule, Liquid, etc.)
    - Quantity per dose: Required, must be positive numeric value (e.g., 1 tablet, 2 capsules)
    - Quantity per dose: Must be reasonable (typically 1-10 units per dose, warn if > 20)
    - Total quantity: Must be positive if provided
    - Unit of measure: Required, must be valid unit (mg, ml, units, etc.)
    - Unit-dosage form consistency: Unit must match dosage form (e.g., tablets use "tablet" not "ml")
  
  - **Frequency/Schedule Validation**:
    - Frequency: Required, must be from valid list or free text
    - Frequency: Must be reasonable (e.g., cannot be "Every 5 minutes" for most medications)
    - Frequency: Must match medication type (e.g., some medications have maximum daily frequency)
    - Schedule validation: Must be valid schedule format if structured schedule used
    - Duration: Must be positive if provided
    - End date: Cannot be before start date
    - End date: Cannot be more than 10 years in the future (warn if > 2 years)
  
  - **Date Validation**:
    - Start date: Required, cannot be in the future (except for scheduled medications)
    - Start date: Cannot be more than 150 years in the past (warn if > 120 years)
    - Stop date: Cannot be before start date
    - Stop date: Cannot be more than 1 year in the future
    - Prescription date: Cannot be in the future
    - Prescription date: Cannot be more than 1 year in the past
    - Last filled date: Cannot be before prescription date
    - Last filled date: Cannot be in the future
    - Next refill date: Cannot be before last filled date
    - Date format validation: Must be valid calendar date
  
  - **Required Fields Validation**:
    - Medication name: Required
    - Generic name: Required
    - Dosage strength: Required
    - Dosage form: Required
    - Quantity per dose: Required
    - Frequency: Required
    - Route of administration: Required
    - Start date: Required
    - Medication status: Required (Active, Discontinued, etc.)
    - Prescribing provider: Required
    - Prescription date: Required
  
  - **Quality Checks**:
    - Identify missing medications (patients with no medication documentation)
    - Identify duplicate medications
    - Identify medications with missing information
    - Identify medications with missing indications
    - Identify medications requiring review
    - Medication completeness checks
    - "No Current Medications" documentation verification
  
  - **Quality Metrics**:
    - Medication documentation completeness
    - Medication reconciliation completion rates
    - Medication interaction alert accuracy
    - Medication interaction alert override rates
    - Medication data quality scores
    - Quality metric reporting

##### 2.2.9.13 Medication Workflow and Best Practices

- **FR-9.19**: System shall support medication workflows and best practices:
  - **Medication Documentation Workflow**:
    - Document medications during patient registration
    - Review medications at each encounter
    - Update medications as information changes
    - Perform medication reconciliation at transitions of care
    - Discontinue medications when appropriate
    - Medication reconciliation during encounters
  
  - **Medication Review Workflow**:
    - Periodic medication list review
    - Medication list review reminders
    - Medication list review documentation
    - Update medication list during review
    - Identify and resolve medication issues during review
  
  - **Medication Best Practices**:
    - Support for medication documentation guidelines
    - Reminders for medication reconciliation
    - Templates for common medications
    - Clinical decision support for medication management
    - Integration with evidence-based guidelines
    - Support for medication therapy management
    - Support for deprescribing (discontinuing unnecessary medications)

##### 2.2.9.14 Medication Error Handling

- **FR-9.20**: System shall implement comprehensive error handling for medication management:
  - **Medication Entry Error Handling**:
    - Handle medication code lookup failures (NDC, RxNorm)
    - Handle dosage calculation errors
    - Handle drug interaction check failures
    - Handle allergy check failures
    - Display clear error messages for entry failures
    - Allow manual entry when lookups fail
    - Auto-save medication entries to prevent data loss
  
  - **Medication Reconciliation Error Handling**:
    - Handle reconciliation comparison errors
    - Handle missing medication list errors
    - Handle reconciliation conflict resolution errors
    - Display reconciliation error messages
    - Provide manual reconciliation when automatic fails
    - Maintain reconciliation audit trail during errors
  
  - **Integration Error Handling**:
    - Handle pharmacy system connection failures
    - Handle prescription transmission failures
    - Handle medication import failures
    - Handle formulary check failures
    - Allow manual entry when integrations fail
    - Queue failed operations for retry
    - Log integration errors with medication details
  
  - **Drug Interaction Check Error Handling**:
    - Handle interaction database connection failures
    - Handle interaction calculation errors
    - Display interaction check failure messages
    - Allow override when check unavailable (with documentation)
    - Log interaction check failures
  
  - **Medication History Error Handling**:
    - Handle historical medication import errors
    - Handle medication history merge conflicts
    - Handle medication history correction errors
    - Maintain data integrity during history updates
    - Provide rollback mechanism for history errors

#### 2.2.10 Viewing and Access

##### 2.2.10.1 Patient Summary and Dashboard

- **FR-10.1**: System shall provide comprehensive patient summary/dashboard view with:
  - **Patient Identification**:
    - Patient name and demographics (age, gender, DOB)
    - Medical Record Number (MRN)
    - Patient photo (if available, optional)
    - Patient status (Active, Inactive, Deceased)
    - Primary care provider
    - Registration date
  
  - **Critical Information Banner**:
    - Allergies (prominently displayed, with severity indicators)
    - Critical medications (if applicable)
    - Active critical problems/diagnoses
    - Recent critical lab values or imaging findings
    - Important alerts and warnings
    - Patient safety flags
  
  - **Current Clinical Information**:
    - Active problems/diagnoses (summary list)
    - Current medications (summary list with key details)
    - Recent vital signs (most recent values)
    - Recent lab results (abnormal or key results)
    - Recent imaging studies (key findings)
    - Upcoming appointments (if scheduling integrated)
    - Pending orders (lab, imaging, etc.)
  
  - **Quick Access Links**:
    - Link to full problem list
    - Link to full medication list
    - Link to full allergy list
    - Link to recent encounters
    - Link to clinical notes
    - Link to lab results
    - Link to imaging studies
    - Link to prescriptions
  
  - **Summary Statistics**:
    - Last visit date
    - Number of encounters (total or in date range)
    - Number of active problems
    - Number of current medications
    - Number of allergies
    - Last lab date
    - Last imaging date
  
  - **Dashboard Customization**:
    - User-configurable dashboard layout
    - User-selectable information to display
    - User preferences for dashboard organization
    - Specialty-specific dashboard views (if applicable)
    - Role-specific dashboard views

- **FR-10.2**: System shall support multiple dashboard views:
  - **Default Dashboard**: Standard comprehensive view
  - **Clinical Dashboard**: Focus on clinical information (problems, medications, vitals, labs)
  - **Administrative Dashboard**: Focus on administrative information (demographics, insurance, appointments)
  - **Quick View**: Condensed view for rapid patient overview
  - **Detailed View**: Expanded view with more information
  - **Custom Views**: User-created custom dashboard configurations

##### 2.2.10.2 Chronological Timeline and Encounter Views

- **FR-10.3**: System shall provide chronological timeline view of patient encounters:
  - **Timeline Display**:
    - All patient encounters in chronological order (newest first or oldest first)
    - Encounters grouped by date
    - Encounters grouped by type (Office Visit, Hospitalization, Emergency, etc.)
    - Encounters grouped by provider
    - Encounters grouped by facility/location
    - Visual timeline representation (optional)
    - Timeline zoom (day, week, month, year, all time)
  
  - **Encounter Information**:
    - Encounter date and time
    - Encounter type
    - Provider(s) involved
    - Facility/location
    - Chief complaint
    - Diagnoses addressed
    - Procedures performed
    - Medications prescribed
    - Key findings
    - Encounter status
  
  - **Timeline Navigation**:
    - Scroll through timeline
    - Jump to specific date
    - Jump to specific encounter
    - Filter timeline by date range
    - Filter timeline by encounter type
    - Filter timeline by provider
    - Filter timeline by facility
    - Search timeline by keyword

- **FR-10.4**: System shall support encounter detail views:
  - **Encounter Summary**:
    - Complete encounter information
    - All clinical notes from encounter
    - All diagnoses from encounter
    - All procedures from encounter
    - All medications from encounter
    - All orders from encounter
    - All results from encounter
    - All documents from encounter
  
  - **Encounter Navigation**:
    - Navigate to previous encounter
    - Navigate to next encounter
    - Navigate to related encounters
    - Compare encounters side-by-side
    - Link related encounters

##### 2.2.10.3 Record Views and Organization

- **FR-10.5**: System shall provide multiple record view options:
  - **Tabbed View**:
    - Tabs for different record sections (Demographics, Problems, Medications, Allergies, Labs, Imaging, Notes, etc.)
    - Quick navigation between sections
    - Tab customization (show/hide tabs)
    - Tab organization (reorder tabs)
  
  - **Sidebar Navigation**:
    - Collapsible sidebar with record sections
    - Quick navigation to sections
    - Section expansion/collapse
    - Section organization
  
  - **Single Page View**:
    - All information on single scrollable page
    - Section headers for organization
    - Quick jump links to sections
    - Section expansion/collapse
  
  - **Split View**:
    - Multiple sections visible simultaneously
    - Side-by-side comparison views
    - Customizable split configurations

- **FR-10.6**: System shall support record section organization:
  - **Standard Sections**:
    - Demographics
    - Problems/Diagnoses
    - Medications
    - Allergies
    - Vital Signs
    - Lab Results
    - Imaging Studies
    - Clinical Notes
    - Procedures
    - Immunizations
    - Medical History
    - Family History
    - Social History
    - Encounters
    - Documents
  
  - **Section Customization**:
    - Show/hide sections
    - Reorder sections
    - Custom section groupings
    - Section-specific views
    - Specialty-specific section organization

##### 2.2.10.4 Filtering and Search Capabilities

- **FR-10.7**: System shall provide comprehensive filtering capabilities:
  - **Date Range Filters**:
    - Filter by specific date
    - Filter by date range
    - Filter by relative dates (Last 30 days, Last 6 months, Last year, etc.)
    - Filter by encounter date
    - Filter by result date
    - Filter by note date
    - Filter by medication start/stop date
  
  - **Category Filters**:
    - Filter by record type (Problems, Medications, Labs, Imaging, Notes, etc.)
    - Filter by status (Active, Inactive, Resolved, etc.)
    - Filter by provider
    - Filter by facility/location
    - Filter by severity (for problems, allergies, etc.)
    - Filter by type (for medications, labs, imaging, etc.)
  
  - **Content Filters**:
    - Filter by keyword
    - Filter by diagnosis code
    - Filter by medication name
    - Filter by test name
    - Filter by abnormal values
    - Filter by critical findings
  
  - **Advanced Filters**:
    - Multiple filter criteria combination
    - Saved filter configurations
    - Filter presets
    - Custom filter creation

- **FR-10.8**: System shall provide comprehensive search capabilities:
  - **Global Search**:
    - Search across all patient record sections
    - Search by keyword
    - Search by phrase
    - Search by code (ICD, CPT, LOINC, NDC, etc.)
    - Search by date
    - Search by provider name
    - Full-text search in notes and documents
  
  - **Section-Specific Search**:
    - Search within specific record sections
    - Search problems by name or code
    - Search medications by name or code
    - Search labs by test name or code
    - Search imaging by study type or findings
    - Search notes by content
  
  - **Search Features**:
    - Auto-complete suggestions
    - Search history
    - Saved searches
    - Search filters
    - Search result highlighting
    - Search result ranking
    - Advanced search options

##### 2.2.10.5 Print and Export Capabilities

- **FR-10.9**: System shall provide comprehensive print capabilities:
  - **Print Options**:
    - Print complete patient record
    - Print selected record sections
    - Print specific encounters
    - Print date range of records
    - Print summary reports
    - Print detailed reports
    - Print with or without formatting
    - Print preview before printing
  
  - **Print Formats**:
    - Standard print format
    - Compact print format
    - Detailed print format
    - Custom print templates
    - Print-friendly layouts
    - Header and footer customization
    - Page numbering
    - Date/time stamps
  
  - **Print Configuration**:
    - Page size selection
    - Orientation (portrait/landscape)
    - Margins configuration
    - Font size selection
    - Print quality settings
    - Print to PDF option
    - Print to file option

- **FR-10.10**: System shall provide comprehensive export capabilities:
  - **Export Formats**:
    - Export to PDF
    - Export to Excel/CSV
    - Export to Word document
    - Export to text file
    - Export to XML
    - Export to HL7 FHIR format
    - Export to C-CDA format
  
  - **Export Options**:
    - Export complete patient record
    - Export selected record sections
    - Export specific encounters
    - Export date range of records
    - Export summary data
    - Export detailed data
    - Export with or without formatting
    - Export with metadata
  
  - **Export Configuration**:
    - Select data elements to export
    - Select date ranges
    - Select record sections
    - Configure export format
    - Configure export structure
    - Export templates
    - Scheduled exports (if applicable)

##### 2.2.10.6 Mobile-Responsive Access

- **FR-10.11**: System shall provide mobile-responsive access:
  - **Responsive Design**:
    - Adapt to different screen sizes (smartphone, tablet, desktop)
    - Responsive layout that adjusts to screen width
    - Touch-friendly interface elements
    - Optimized navigation for mobile devices
    - Readable text on small screens
    - Appropriate button and link sizes for touch
  
  - **Mobile-Specific Features**:
    - Mobile-optimized dashboard
    - Mobile-optimized record views
    - Mobile-optimized data entry forms
    - Mobile-optimized search
    - Mobile-optimized navigation
    - Swipe gestures (if applicable)
    - Pull-to-refresh (if applicable)
  
  - **Mobile Performance**:
    - Fast loading on mobile networks
    - Optimized images for mobile
    - Efficient data usage
    - Offline capability (if applicable)
    - Mobile app support (if applicable)

##### 2.2.10.7 Access Control and Security

- **FR-10.12**: System shall implement appropriate access controls for viewing:
  - **Role-Based Access**:
    - Different users see different information based on role
    - Role-specific views and sections
    - Role-specific data elements
    - Minimum necessary principle enforcement
    - Access restrictions based on user role
  
  - **Patient Privacy Controls**:
    - Patient privacy flags affecting viewable information
    - Restricted access to sensitive information
    - Patient privacy preferences affecting display
    - Support for patient requests to restrict information access
    - Break-the-glass functionality for emergency access (with audit trail)
  
  - **Facility/Location Access**:
    - Access restrictions based on user facility/location
    - Access to patients at user's facility only (if configured)
    - Access to patients across facilities (if authorized)
    - Location-based access controls

- **FR-10.13**: System shall implement security measures for viewing:
  - **Authentication**:
    - User authentication required for access
    - Session timeout and automatic logout
    - Multi-factor authentication support (if configured)
    - Password requirements and policies
  
  - **Audit Logging**:
    - Log all patient record access
    - Log all record views
    - Log all record exports
    - Log all record prints
    - Log user, timestamp, action, and IP address
    - Audit trail for compliance and security
  
  - **Data Security**:
    - Encrypted data transmission
    - Encrypted data storage
    - Secure session management
    - Protection against unauthorized access
    - Protection against data breaches

##### 2.2.10.8 Performance and Usability

- **FR-10.14**: System shall ensure optimal performance for viewing:
  - **Performance Requirements**:
    - Fast page load times (within 3 seconds for 95% of requests)
    - Fast data retrieval
    - Efficient database queries
    - Optimized data display
    - Smooth scrolling and navigation
    - Responsive user interface
  
  - **Performance Optimization**:
    - Data pagination for large datasets
    - Lazy loading of data
    - Caching of frequently accessed data
    - Optimized images and graphics
    - Efficient rendering
    - Background data loading

- **FR-10.15**: System shall ensure optimal usability for viewing:
  - **User Interface Design**:
    - Intuitive navigation
    - Clear information hierarchy
    - Consistent layout and design
    - Readable fonts and colors
    - Appropriate use of white space
    - Clear visual indicators
    - Helpful tooltips and hints
  
  - **Accessibility**:
    - Support for screen readers
    - Keyboard navigation support
    - High contrast mode (if applicable)
    - Adjustable font sizes
    - Color-blind friendly design
    - WCAG (Web Content Accessibility Guidelines) compliance (if applicable)
  
  - **User Experience**:
    - Easy to learn interface
    - Minimal training required
    - Contextual help available
    - Error messages are clear and helpful
    - Confirmation for critical actions
    - Undo capability (where applicable)

##### 2.2.10.9 Customization and Personalization

- **FR-10.16**: System shall support user customization and personalization:
  - **View Customization**:
    - User-selectable information to display
    - User-configurable layouts
    - User preferences for default views
    - User preferences for sort order
    - User preferences for filter defaults
    - User preferences for display options
  
  - **Personalization**:
    - Remember user preferences
    - Remember last viewed sections
    - Remember frequently accessed patients
    - Remember search history
    - Remember filter configurations
    - Customizable dashboard
    - Customizable views
  
  - **Organization Customization**:
    - Organization-level view configurations
    - Department-level view configurations
    - Specialty-specific view configurations
    - Facility-specific view configurations

##### 2.2.10.10 Integration with Other Features

- **FR-10.17**: System shall integrate viewing capabilities with other system features:
  - **Navigation Integration**:
    - Quick navigation to related information
    - Context-aware navigation
    - Breadcrumb navigation
    - Related information links
    - Cross-references between sections
  
  - **Action Integration**:
    - Quick actions from record views (e.g., create note, order lab, prescribe medication)
    - Context menus with relevant actions
    - Action buttons in appropriate locations
    - Workflow integration from views
  
  - **Notification Integration**:
    - Display alerts and notifications in views
    - Display critical information prominently
    - Display pending tasks
    - Display reminders
    - Display system messages

### 2.3 Data Requirements

#### 2.3.1 Data Model Overview

The Patient Health Records feature requires a comprehensive data model to support all clinical and administrative information. The data model shall follow relational database principles with proper normalization, referential integrity, and data consistency. All entities shall support audit trails, versioning where applicable, and soft deletion for data retention compliance.

##### 2.3.1.1 Data Model Principles
- **Normalization**: Data shall be normalized to third normal form (3NF) or higher to minimize redundancy
- **Referential Integrity**: Foreign key relationships shall be enforced to maintain data consistency
- **Audit Trails**: All entities shall support audit logging of create, update, and delete operations
- **Soft Deletion**: Critical entities shall support soft deletion (mark as deleted, retain data) for compliance
- **Versioning**: Entities requiring version control (notes, results) shall maintain version history
- **Data Standards**: All coded data shall use standard terminologies (ICD-10, LOINC, SNOMED CT, RxNorm, etc.)
- **Uniqueness**: Primary keys and unique constraints shall ensure data integrity
- **Indexing**: Appropriate indexes shall be created for performance optimization

#### 2.3.2 Core Entity Definitions

##### 2.3.2.1 Patient Entity

**Purpose**: Stores comprehensive patient demographic and administrative information.

**Primary Key**: PatientID (Unique, Auto-increment or GUID)

**Attributes**:
- **PatientID** (Primary Key, Unique, Required)
  - Data Type: Integer or GUID
  - Constraints: Unique, Not Null, Auto-increment
  
- **MedicalRecordNumber (MRN)** (Unique, Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Unique, Not Null, Indexed
  - Format: Organization-specific format
  
- **Personal Demographics**:
  - **FirstName** (Required)
    - Data Type: String (VARCHAR, 100)
    - Constraints: Not Null
  - **LastName** (Required)
    - Data Type: String (VARCHAR, 100)
    - Constraints: Not Null, Indexed
  - **MiddleName** (Optional)
    - Data Type: String (VARCHAR, 100)
  - **PreferredName** (Optional)
    - Data Type: String (VARCHAR, 100)
  - **DateOfBirth** (Required)
    - Data Type: Date
    - Constraints: Not Null, Valid date, Cannot be in future
    - Indexed
  - **Gender** (Required)
    - Data Type: String (VARCHAR, 20)
    - Constraints: Not Null, Enum (Male, Female, Other, Prefer not to answer)
  - **SexAtBirth** (Optional)
    - Data Type: String (VARCHAR, 20)
  - **SocialSecurityNumber** (Optional)
    - Data Type: String (VARCHAR, 11)
    - Constraints: Encrypted at rest, Format: XXX-XX-XXXX
    - Indexed (hashed index for security)
  - **Race** (Optional)
    - Data Type: String (VARCHAR, 50)
  - **Ethnicity** (Optional)
    - Data Type: String (VARCHAR, 50)
  - **MaritalStatus** (Optional)
    - Data Type: String (VARCHAR, 20)
  
- **Contact Information**:
  - **PrimaryAddressLine1** (Required)
    - Data Type: String (VARCHAR, 200)
    - Constraints: Not Null
  - **PrimaryAddressLine2** (Optional)
    - Data Type: String (VARCHAR, 200)
  - **City** (Required)
    - Data Type: String (VARCHAR, 100)
    - Constraints: Not Null
  - **State** (Required)
    - Data Type: String (VARCHAR, 50)
    - Constraints: Not Null
  - **ZipCode** (Required)
    - Data Type: String (VARCHAR, 10)
    - Constraints: Not Null, Format validation
  - **Country** (Required)
    - Data Type: String (VARCHAR, 50)
    - Constraints: Not Null, Default: System default
  - **PrimaryPhone** (Required)
    - Data Type: String (VARCHAR, 20)
    - Constraints: Not Null, Format validation
  - **PrimaryPhoneType** (Required)
    - Data Type: String (VARCHAR, 20)
    - Constraints: Enum (Home, Work, Mobile, Other)
  - **SecondaryPhone** (Optional)
    - Data Type: String (VARCHAR, 20)
  - **SecondaryPhoneType** (Optional)
    - Data Type: String (VARCHAR, 20)
  - **PrimaryEmail** (Optional)
    - Data Type: String (VARCHAR, 255)
    - Constraints: Email format validation
  - **SecondaryEmail** (Optional)
    - Data Type: String (VARCHAR, 255)
    - Constraints: Email format validation
  - **PreferredContactMethod** (Optional)
    - Data Type: String (VARCHAR, 20)
  
- **Emergency Contact**:
  - **EmergencyContactName** (Required)
    - Data Type: String (VARCHAR, 200)
    - Constraints: Not Null
  - **EmergencyContactRelationship** (Required)
    - Data Type: String (VARCHAR, 50)
    - Constraints: Not Null
  - **EmergencyContactPhone** (Required)
    - Data Type: String (VARCHAR, 20)
    - Constraints: Not Null
  - **EmergencyContactAddress** (Optional)
    - Data Type: String (VARCHAR, 500)
  
- **Insurance Information** (Multiple records supported via separate table):
  - Reference to Insurance table (Foreign Key)
  
- **Clinical Assignment**:
  - **PrimaryCareProviderID** (Optional)
    - Data Type: Integer (Foreign Key to Provider table)
  - **PrimaryCareLocationID** (Optional)
    - Data Type: Integer (Foreign Key to Location table)
  - **ReferringProviderID** (Optional)
    - Data Type: Integer (Foreign Key to Provider table)
  
- **Administrative Information**:
  - **RegistrationDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **RegisteredByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **RegistrationLocationID** (Required)
    - Data Type: Integer (Foreign Key to Location table)
    - Constraints: Not Null
  - **PatientStatus** (Required)
    - Data Type: String (VARCHAR, 20)
    - Constraints: Not Null, Enum (Active, Inactive, Deceased, Archived)
    - Default: Active
    - Indexed
  - **DateOfDeath** (Optional)
    - Data Type: Date
    - Constraints: Valid date, Cannot be in future
  
- **Clinical Information**:
  - **PreferredLanguage** (Required)
    - Data Type: String (VARCHAR, 50)
    - Constraints: Not Null, Default: System default
  - **InterpreterNeeded** (Optional)
    - Data Type: Boolean
    - Default: False
  - **SpecialNeeds** (Optional)
    - Data Type: Text
  
- **Consent and Privacy**:
  - **HIPAAAcknowledged** (Optional)
    - Data Type: Boolean
  - **HIPAAAcknowledgmentDate** (Optional)
    - Data Type: DateTime
  - **MarketingConsent** (Optional)
    - Data Type: Boolean
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp, Updated on change
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False
    - Indexed
  - **DeletedDate** (Optional)
    - Data Type: DateTime
  - **DeletedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)

**Indexes**:
- Primary Key: PatientID
- Unique: MedicalRecordNumber
- Index: LastName, FirstName
- Index: DateOfBirth
- Index: PrimaryPhone
- Index: PrimaryEmail
- Index: PatientStatus
- Index: IsDeleted

**Relationships**:
- One-to-Many: Patient → Encounters
- One-to-Many: Patient → Problems
- One-to-Many: Patient → Medications
- One-to-Many: Patient → Allergies
- One-to-Many: Patient → Vital Signs
- One-to-Many: Patient → Clinical Notes
- One-to-Many: Patient → Lab Results
- One-to-Many: Patient → Imaging Studies
- One-to-Many: Patient → Medical History
- Many-to-One: Patient → PrimaryCareProvider (Provider)
- Many-to-One: Patient → RegistrationLocation (Location)

##### 2.3.2.2 Medical History Entity

**Purpose**: Stores patient medical history including past medical, family, social, and immunization history.

**Primary Key**: HistoryID (Unique, Auto-increment)

**Attributes**:
- **HistoryID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **HistoryType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Past Medical, Family, Social, Immunization, Obstetric, Gynecological)
  - Indexed
  
- **HistoryCategory** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: For Past Medical: "Chronic Condition", "Surgical History", "Hospitalization"
  
- **Description** (Required)
  - Data Type: Text
  - Constraints: Not Null
  
- **ICDCode** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Constraints: ICD-10/ICD-11 format validation
  
- **SNOMEDCTCode** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **OnsetDate** (Optional)
  - Data Type: Date
  - Constraints: Valid date, Cannot be in future
  
- **EndDate** (Optional)
  - Data Type: Date
  - Constraints: Valid date, Must be after OnsetDate if both provided
  
- **Status** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Active", "Resolved", "Chronic", "Historical"
  
- **Severity** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **Details** (Optional)
  - Data Type: Text
  - Additional free-text details
  
- **Source** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Patient reported", "Medical records", "Family member"
  
- **ProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: HistoryID
- Index: PatientID
- Index: HistoryType
- Index: OnsetDate
- Index: IsDeleted

**Relationships**:
- Many-to-One: Medical History → Patient
- Many-to-One: Medical History → Provider (optional)

##### 2.3.2.3 Vital Signs Entity

**Purpose**: Stores patient vital signs and clinical measurements.

**Primary Key**: VitalSignID (Unique, Auto-increment)

**Attributes**:
- **VitalSignID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **EncounterID** (Optional)
  - Data Type: Integer (Foreign Key to Encounter table)
  - Indexed
  
- **MeasurementType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Blood Pressure, Heart Rate, Temperature, Respiratory Rate, Oxygen Saturation, Height, Weight, BMI, Pain Scale, Blood Glucose, Other)
  - Indexed
  
- **SystolicBP** (Optional, if MeasurementType = Blood Pressure)
  - Data Type: Decimal(5,2)
  - Constraints: Range validation (typically 50-300)
  
- **DiastolicBP** (Optional, if MeasurementType = Blood Pressure)
  - Data Type: Decimal(5,2)
  - Constraints: Range validation (typically 30-200)
  
- **Value** (Required for numeric measurements)
  - Data Type: Decimal(10,2)
  - Constraints: Not Null (if numeric measurement)
  
- **Unit** (Required for numeric measurements)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null (if numeric measurement)
  - Examples: "mmHg", "bpm", "°F", "°C", "in", "cm", "lbs", "kg", "%"
  
- **QualitativeValue** (Optional, for non-numeric measurements)
  - Data Type: String (VARCHAR, 100)
  - Example: "Regular", "Irregular" for heart rhythm
  
- **MeasurementDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null, Indexed
  - Default: Current timestamp
  
- **LocationID** (Optional)
  - Data Type: Integer (Foreign Key to Location table)
  
- **ProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **RecordedByUserID** (Required)
  - Data Type: Integer (Foreign Key to User table)
  - Constraints: Not Null
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: VitalSignID
- Index: PatientID
- Index: EncounterID
- Index: MeasurementType
- Index: MeasurementDate
- Index: IsDeleted
- Composite Index: (PatientID, MeasurementType, MeasurementDate) for trend queries

**Relationships**:
- Many-to-One: Vital Signs → Patient
- Many-to-One: Vital Signs → Encounter (optional)
- Many-to-One: Vital Signs → Provider (optional)
- Many-to-One: Vital Signs → Location (optional)

##### 2.3.2.4 Clinical Notes Entity

**Purpose**: Stores clinical documentation including progress notes, SOAP notes, consultation notes, discharge summaries, and procedure notes.

**Primary Key**: NoteID (Unique, Auto-increment)

**Attributes**:
- **NoteID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **EncounterID** (Optional)
  - Data Type: Integer (Foreign Key to Encounter table)
  - Indexed
  
- **NoteType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Progress Note, SOAP Note, Consultation Note, Discharge Summary, Procedure Note, H&P, Assessment and Plan, Telephone Note, Nursing Note, Other)
  - Indexed
  
- **NoteTitle** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **NoteContent** (Required)
  - Data Type: Text or Rich Text
  - Constraints: Not Null
  
- **TemplateID** (Optional)
  - Data Type: Integer (Foreign Key to Note Template table)
  
- **Subjective** (Optional, for SOAP notes)
  - Data Type: Text
  
- **Objective** (Optional, for SOAP notes)
  - Data Type: Text
  
- **Assessment** (Optional, for SOAP notes)
  - Data Type: Text
  
- **Plan** (Optional, for SOAP notes)
  - Data Type: Text
  
- **ProviderID** (Required)
  - Data Type: Integer (Foreign Key to Provider table)
  - Constraints: Not Null
  - Indexed
  
- **CoSignerProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **NoteStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Draft, Pending Review, Signed, Amended, Cancelled)
  - Default: Draft
  - Indexed
  
- **IsSigned** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: False
  - Indexed
  
- **SignedDate** (Optional)
  - Data Type: DateTime
  - Constraints: Required if IsSigned = True
  
- **ElectronicSignature** (Optional)
  - Data Type: String (VARCHAR, 500)
  - Constraints: Required if IsSigned = True
  
- **VersionNumber** (Required)
  - Data Type: Integer
  - Constraints: Not Null, Default: 1
  
- **ParentNoteID** (Optional, for amendments/addendums)
  - Data Type: Integer (Foreign Key to Clinical Notes table)
  
- **AmendmentReason** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
    - Indexed
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: NoteID
- Index: PatientID
- Index: EncounterID
- Index: ProviderID
- Index: NoteType
- Index: NoteStatus
- Index: IsSigned
- Index: CreatedDate
- Full-Text Index: NoteContent (for search)
- Composite Index: (PatientID, CreatedDate) for chronological queries

**Relationships**:
- Many-to-One: Clinical Notes → Patient
- Many-to-One: Clinical Notes → Encounter (optional)
- Many-to-One: Clinical Notes → Provider
- Many-to-One: Clinical Notes → CoSigner Provider (optional)
- Self-Referential: Clinical Notes → Parent Note (for amendments)

##### 2.3.2.5 Diagnosis/Problem Entity

**Purpose**: Stores patient diagnoses and problem lists.

**Primary Key**: DiagnosisID (Unique, Auto-increment)

**Attributes**:
- **DiagnosisID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **ICD10Code** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, ICD-10 format validation
  - Indexed
  
- **ICD11Code** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Constraints: ICD-11 format validation
  
- **SNOMEDCTCode** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **Description** (Required)
  - Data Type: String (VARCHAR, 500)
  - Constraints: Not Null
  
- **Status** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Active, Resolved, Chronic, Inactive, Ruled Out, Pending, Historical)
  - Default: Active
  - Indexed
  
- **OnsetDate** (Required)
  - Data Type: Date
  - Constraints: Not Null, Valid date, Cannot be in future
  - Indexed
  
- **ResolutionDate** (Optional)
  - Data Type: Date
  - Constraints: Valid date, Must be after OnsetDate if both provided
  
- **Severity** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Mild", "Moderate", "Severe", "Stage 1", "Stage 2"
  
- **Laterality** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "Left", "Right", "Bilateral"
  
- **Category** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Acute", "Chronic", "Symptom", "Finding"
  
- **Priority** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "High", "Medium", "Low"
  
- **IsPrimary** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indicates primary diagnosis for encounter
  
- **IsVerified** (Optional)
  - Data Type: Boolean
  - Default: False
  
- **VerificationDate** (Optional)
  - Data Type: DateTime
  
- **ProviderID** (Required)
  - Data Type: Integer (Foreign Key to Provider table)
  - Constraints: Not Null
  - Indexed
  
- **VerifyingProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: DiagnosisID
- Index: PatientID
- Index: ICD10Code
- Index: Status
- Index: OnsetDate
- Index: ProviderID
- Index: IsDeleted
- Composite Index: (PatientID, Status) for active problem queries

**Relationships**:
- Many-to-One: Diagnosis → Patient
- Many-to-One: Diagnosis → Provider
- Many-to-One: Diagnosis → Verifying Provider (optional)

##### 2.3.2.6 Lab Results Entity

**Purpose**: Stores laboratory test orders and results.

**Primary Key**: LabResultID (Unique, Auto-increment)

**Attributes**:
- **LabResultID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **OrderID** (Optional)
  - Data Type: Integer (Foreign Key to Lab Order table)
  - Indexed
  
- **EncounterID** (Optional)
  - Data Type: Integer (Foreign Key to Encounter table)
  - Indexed
  
- **LOINCCode** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, LOINC format validation
  - Indexed
  
- **TestName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  
- **TestCategory** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Chemistry", "Hematology", "Microbiology"
  
- **ResultValue** (Required for numeric results)
  - Data Type: String (VARCHAR, 500)
  - Constraints: Can store numeric or text values
  
- **NumericValue** (Optional, for numeric results)
  - Data Type: Decimal(15,5)
  - For trend analysis and calculations
  
- **Unit** (Optional, for numeric results)
  - Data Type: String (VARCHAR, 20)
  - Example: "mg/dL", "mmol/L", "cells/μL"
  
- **ReferenceRangeLow** (Optional)
  - Data Type: Decimal(15,5)
  
- **ReferenceRangeHigh** (Optional)
  - Data Type: Decimal(15,5)
  
- **AbnormalFlag** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Enum (H = High, L = Low, A = Abnormal, N = Normal, C = Critical)
  - Indexed
  
- **IsCritical** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indexed
  
- **ResultStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Final, Preliminary, Corrected, Cancelled, Amended)
  - Default: Final
  - Indexed
  
- **SpecimenType** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Blood", "Urine", "Stool"
  
- **SpecimenCollectionDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null
  - Indexed
  
- **SpecimenReceivedDate** (Optional)
  - Data Type: DateTime
  
- **ResultDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null
  - Indexed
  
- **ResultReportedDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null
  
- **ResultVerifiedDate** (Optional)
  - Data Type: DateTime
  
- **LaboratoryID** (Optional)
  - Data Type: Integer (Foreign Key to Laboratory table)
  
- **LaboratoryName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **PerformingTechnologist** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **ReviewingPathologist** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **Comments** (Optional)
  - Data Type: Text
  
- **ProviderComments** (Optional)
  - Data Type: Text
  
- **IsReviewed** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indexed
  
- **ReviewedDate** (Optional)
  - Data Type: DateTime
  
- **ReviewedByUserID** (Optional)
  - Data Type: Integer (Foreign Key to User table)
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: LabResultID
- Index: PatientID
- Index: OrderID
- Index: EncounterID
- Index: LOINCCode
- Index: AbnormalFlag
- Index: IsCritical
- Index: ResultStatus
- Index: SpecimenCollectionDate
- Index: ResultDate
- Index: IsReviewed
- Composite Index: (PatientID, LOINCCode, ResultDate) for trend queries

**Relationships**:
- Many-to-One: Lab Results → Patient
- Many-to-One: Lab Results → Lab Order (optional)
- Many-to-One: Lab Results → Encounter (optional)
- Many-to-One: Lab Results → Laboratory (optional)

##### 2.3.2.7 Imaging Study Entity

**Purpose**: Stores imaging study orders and reports.

**Primary Key**: ImagingStudyID (Unique, Auto-increment)

**Attributes**:
- **ImagingStudyID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **OrderID** (Optional)
  - Data Type: Integer (Foreign Key to Imaging Order table)
  - Indexed
  
- **EncounterID** (Optional)
  - Data Type: Integer (Foreign Key to Encounter table)
  - Indexed
  
- **StudyType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (X-ray, CT, MRI, Ultrasound, Mammography, Nuclear Medicine, PET, DEXA, Fluoroscopy, Other)
  - Indexed
  
- **CPTCode** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, CPT format validation
  - Indexed
  
- **StudyDescription** (Required)
  - Data Type: String (VARCHAR, 500)
  - Constraints: Not Null
  
- **BodyPart** (Required)
  - Data Type: String (VARCHAR, 100)
  - Constraints: Not Null
  
- **Laterality** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "Left", "Right", "Bilateral"
  
- **AccessionNumber** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Unique
  - Indexed
  
- **StudyDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null
  - Indexed
  
- **StudyCompletionDate** (Optional)
  - Data Type: DateTime
  
- **ContrastUsed** (Optional)
  - Data Type: Boolean
  - Default: False
  
- **ContrastType** (Optional)
  - Data Type: String (VARCHAR, 100)
  
- **StudyStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Scheduled, In Progress, Completed, Cancelled, No Show)
  - Default: Scheduled
  - Indexed
  
- **ReportStatus** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Enum (Preliminary, Final, Amended, Addendum, Cancelled)
  
- **ClinicalIndication** (Optional)
  - Data Type: Text
  
- **Technique** (Optional)
  - Data Type: Text
  
- **Findings** (Optional)
  - Data Type: Text
  
- **Impression** (Optional)
  - Data Type: Text
  
- **Recommendations** (Optional)
  - Data Type: Text
  
- **RadiologistID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **RadiologistName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **ReportDate** (Optional)
  - Data Type: DateTime
  
- **ReportFinalizedDate** (Optional)
  - Data Type: DateTime
  
- **FacilityID** (Optional)
  - Data Type: Integer (Foreign Key to Facility table)
  
- **FacilityName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **IsCriticalFinding** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indexed
  
- **IsReviewed** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indexed
  
- **ReviewedDate** (Optional)
  - Data Type: DateTime
  
- **ReviewedByUserID** (Optional)
  - Data Type: Integer (Foreign Key to User table)
  
- **DICOMStudyInstanceUID** (Optional)
  - Data Type: String (VARCHAR, 200)
  - For DICOM integration
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: ImagingStudyID
- Unique: AccessionNumber
- Index: PatientID
- Index: OrderID
- Index: EncounterID
- Index: StudyType
- Index: CPTCode
- Index: StudyDate
- Index: StudyStatus
- Index: ReportStatus
- Index: IsCriticalFinding
- Index: IsReviewed

**Relationships**:
- Many-to-One: Imaging Studies → Patient
- Many-to-One: Imaging Studies → Imaging Order (optional)
- Many-to-One: Imaging Studies → Encounter (optional)
- Many-to-One: Imaging Studies → Radiologist (optional)
- Many-to-One: Imaging Studies → Facility (optional)

##### 2.3.2.8 Allergy Entity

**Purpose**: Stores patient allergies and adverse reactions.

**Primary Key**: AllergyID (Unique, Auto-increment)

**Attributes**:
- **AllergyID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **AllergenName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  - Indexed
  
- **AllergenType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Medication/Drug, Food, Environmental, Latex, Contrast Agent, Other)
  - Indexed
  
- **AllergenCode** (Optional)
  - Data Type: String (VARCHAR, 50)
  - For medications: NDC, RxNorm
  - For others: SNOMED CT
  
- **AllergenCategory** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: For medications: "Penicillin", "Sulfa"
  
- **SpecificAllergen** (Optional)
  - Data Type: String (VARCHAR, 200)
  - If allergen type is a class, specify exact substance
  
- **ReactionType** (Required)
  - Data Type: String (VARCHAR, 100)
  - Constraints: Not Null
  - Example: "Anaphylaxis", "Rash/Hives", "Respiratory"
  
- **ReactionDescription** (Required)
  - Data Type: Text
  - Constraints: Not Null
  
- **ReactionSeverity** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Mild, Moderate, Severe, Life-threatening, Unknown)
  - Indexed
  
- **ReactionOnset** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "Immediate", "Delayed", "Unknown"
  
- **ReactionDuration** (Optional)
  - Data Type: String (VARCHAR, 100)
  
- **OnsetDate** (Required)
  - Data Type: Date
  - Constraints: Not Null, Valid date, Cannot be in future
  - Indexed
  
- **MostRecentOccurrenceDate** (Optional)
  - Data Type: Date
  
- **VerificationStatus** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Confirmed, Unconfirmed, Confirmed by Testing, Confirmed by Challenge, Ruled Out, Unknown)
  - Default: Unconfirmed
  - Indexed
  
- **VerificationMethod** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Skin test", "Blood test", "Oral challenge"
  
- **VerificationDate** (Optional)
  - Data Type: Date
  
- **AllergyStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Active, Inactive, Resolved, Unknown)
  - Default: Active
  - Indexed
  
- **ResolutionDate** (Optional)
  - Data Type: Date
  
- **ClinicalSignificance** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "High", "Medium", "Low"
  
- **ProviderID** (Required)
  - Data Type: Integer (Foreign Key to Provider table)
  - Constraints: Not Null
  - Indexed
  
- **VerifyingProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: AllergyID
- Index: PatientID
- Index: AllergenName
- Index: AllergenType
- Index: ReactionSeverity
- Index: VerificationStatus
- Index: AllergyStatus
- Index: ProviderID
- Index: IsDeleted

**Relationships**:
- Many-to-One: Allergy → Patient
- Many-to-One: Allergy → Provider
- Many-to-One: Allergy → Verifying Provider (optional)

##### 2.3.2.9 Medication Entity

**Purpose**: Stores current and historical patient medications.

**Primary Key**: MedicationID (Unique, Auto-increment)

**Attributes**:
- **MedicationID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **PrescriptionID** (Optional)
  - Data Type: Integer (Foreign Key to Prescription table)
  - Indexed
  
- **GenericName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  - Indexed
  
- **BrandName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **NDCCode** (Optional)
  - Data Type: String (VARCHAR, 20)
  
- **RxNormCode** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **DrugClass** (Optional)
  - Data Type: String (VARCHAR, 100)
  
- **MedicationType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Prescription, Over-the-counter, Herbal/Supplement, Other)
  - Indexed
  
- **DosageStrength** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "10 mg", "500 mg"
  
- **DosageForm** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "Tablet", "Capsule", "Liquid"
  
- **QuantityPerDose** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "1 tablet", "2 capsules"
  
- **TotalQuantity** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **Unit** (Optional)
  - Data Type: String (VARCHAR, 20)
  
- **Frequency** (Required)
  - Data Type: String (VARCHAR, 100)
  - Constraints: Not Null
  - Example: "Once daily", "Twice daily", "Every 8 hours"
  
- **Route** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "Oral", "IV", "Topical"
  
- **TimingInstructions** (Optional)
  - Data Type: String (VARCHAR, 200)
  - Example: "With meals", "Before meals"
  
- **SpecialInstructions** (Optional)
  - Data Type: Text
  
- **Duration** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: "10 days", "3 months", "Ongoing"
  
- **StartDate** (Required)
  - Data Type: Date
  - Constraints: Not Null, Valid date
  - Indexed
  
- **EndDate** (Optional)
  - Data Type: Date
  - Constraints: Valid date, Must be after StartDate if both provided
  
- **MedicationStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Active, Discontinued, On Hold, Completed, Unknown)
  - Default: Active
  - Indexed
  
- **DiscontinuationReason** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: "Adverse reaction", "Not effective", "Patient request"
  
- **HoldReason** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **HoldStartDate** (Optional)
  - Data Type: Date
  
- **HoldEndDate** (Optional)
  - Data Type: Date
  
- **Indication** (Optional)
  - Data Type: Text
  
- **ProblemID** (Optional)
  - Data Type: Integer (Foreign Key to Diagnosis table)
  - Link to problem/diagnosis this medication treats
  
- **ProviderID** (Required)
  - Data Type: Integer (Foreign Key to Provider table)
  - Constraints: Not Null
  - Indexed
  
- **PrescriptionDate** (Required)
  - Data Type: Date
  - Constraints: Not Null
  
- **PharmacyID** (Optional)
  - Data Type: Integer (Foreign Key to Pharmacy table)
  
- **PharmacyName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **RefillsAuthorized** (Optional)
  - Data Type: Integer
  - Default: 0
  
- **RefillsRemaining** (Optional)
  - Data Type: Integer
  - Default: 0
  
- **LastFilledDate** (Optional)
  - Data Type: Date
  
- **NextRefillDate** (Optional)
  - Data Type: Date
  
- **MonitoringRequirements** (Optional)
  - Data Type: Text
  
- **PatientResponse** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: "Effective", "Not effective", "Side effects"
  
- **Source** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Prescription", "Patient reported", "Pharmacy"
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: MedicationID
- Index: PatientID
- Index: PrescriptionID
- Index: GenericName
- Index: MedicationType
- Index: MedicationStatus
- Index: StartDate
- Index: ProviderID
- Index: IsDeleted
- Composite Index: (PatientID, MedicationStatus) for active medication queries

**Relationships**:
- Many-to-One: Medication → Patient
- Many-to-One: Medication → Prescription (optional)
- Many-to-One: Medication → Provider
- Many-to-One: Medication → Problem/Diagnosis (optional)
- Many-to-One: Medication → Pharmacy (optional)

#### 2.3.3 Supporting Entity Definitions

##### 2.3.3.1 Encounter Entity

**Purpose**: Stores patient encounters/visits.

**Primary Key**: EncounterID (Unique, Auto-increment)

**Key Attributes**:
- EncounterID (Primary Key)
- PatientID (Foreign Key)
- EncounterType (Office Visit, Hospitalization, Emergency, etc.)
- EncounterDate (DateTime)
- ProviderID (Foreign Key)
- FacilityID (Foreign Key)
- ChiefComplaint (Text)
- EncounterStatus (In Progress, Completed, Cancelled)

##### 2.3.3.2 Provider Entity

**Purpose**: Stores healthcare provider information.

**Primary Key**: ProviderID (Unique, Auto-increment)

**Key Attributes**:
- ProviderID (Primary Key)
- NPI (National Provider Identifier, Unique)
- FirstName, LastName
- Specialty
- LicenseNumber
- IsActive (Boolean)

##### 2.3.3.3 User Entity

**Purpose**: Stores system user accounts.

**Primary Key**: UserID (Unique, Auto-increment)

**Key Attributes**:
- UserID (Primary Key)
- Username (Unique)
- Email (Unique)
- RoleID (Foreign Key to Role table)
- IsActive (Boolean)
- LastLoginDate (DateTime)

##### 2.3.3.4 Location/Facility Entity

**Purpose**: Stores healthcare facility/location information.

**Primary Key**: LocationID (Unique, Auto-increment)

**Key Attributes**:
- LocationID (Primary Key)
- LocationName
- Address
- Phone
- IsActive (Boolean)

#### 2.3.4 Data Standards and Coding

##### 2.3.4.1 Coding Standards Requirements

- **ICD-10/ICD-11**: Required for diagnosis coding
- **LOINC**: Required for laboratory test identification
- **SNOMED CT**: Recommended for clinical terminology
- **RxNorm**: Required for medication identification
- **NDC**: Optional for medication identification
- **CPT**: Required for procedure coding
- **CVX**: Required for immunization coding
- **UCUM**: Required for units of measure

##### 2.3.4.2 Data Validation Rules

- **Date Validation**: All dates must be valid, cannot be in future (except for scheduled dates)
- **Code Validation**: All codes must be validated against current code sets
- **Required Field Validation**: All required fields must be populated
- **Format Validation**: All data must conform to specified formats
- **Range Validation**: Numeric values must be within reasonable ranges
- **Referential Integrity**: All foreign keys must reference existing records

#### 2.3.5 Data Retention and Archival

##### 2.3.5.1 Data Retention Requirements

- **Active Records**: Maintained in primary database
- **Retention Period**: Minimum 6-10 years after last encounter or patient age of majority (varies by jurisdiction)
- **Archival**: Historical records archived but accessible
- **Disposal**: Secure disposal when retention period expires (if applicable)

##### 2.3.5.2 Data Archival Strategy

- **Archival Criteria**: Records meeting retention period requirements
- **Archival Process**: Automated archival process
- **Archival Storage**: Separate archival database or storage system
- **Archival Access**: Archived records accessible but may have performance limitations
- **Data Integrity**: Maintained during archival process

#### 2.3.6 Data Quality Requirements

##### 2.3.6.1 Data Completeness

- **Required Fields**: All required fields must be populated
- **Critical Information**: Critical information (allergies, active medications) must be documented
- **Data Completeness Metrics**: Track and report data completeness

##### 2.3.6.2 Data Accuracy

- **Data Validation**: All data validated at entry
- **Data Verification**: Critical data verified by providers
- **Error Detection**: System detects and reports data errors
- **Data Correction**: Process for correcting data errors

##### 2.3.6.3 Data Consistency

- **Referential Integrity**: Maintained through foreign key constraints
- **Data Synchronization**: Data synchronized across related entities
- **Duplicate Detection**: System detects and prevents duplicate records
- **Data Standardization**: Data standardized using coding systems

#### 2.3.7 Data Security Requirements

##### 2.3.7.1 Data Encryption

- **Encryption at Rest**: All PHI encrypted at rest (AES-256 minimum)
- **Encryption in Transit**: All PHI encrypted in transit (TLS 1.2 minimum)
- **Encryption Keys**: Secure key management

##### 2.3.7.2 Access Controls

- **Authentication**: Strong authentication required
- **Authorization**: Role-based access control
- **Audit Logging**: All data access logged
- **Data Masking**: Sensitive data masked based on user role

##### 2.3.7.3 Data Privacy

- **Minimum Necessary**: Users see only necessary information
- **Patient Privacy Preferences**: Patient privacy preferences enforced
- **Break-the-Glass**: Emergency access with audit trail
- **Data De-identification**: Support for de-identified data for research (if applicable)

### 2.4 User Roles and Permissions

#### 2.4.1 Role-Based Access Control (RBAC) Overview

The Patient Health Records feature shall implement comprehensive Role-Based Access Control (RBAC) to ensure that users only have access to information and functionality appropriate to their role in the healthcare organization. Access control shall be enforced at multiple levels: feature access, data access, and action permissions.

##### 2.4.1.1 RBAC Principles
- **Principle of Least Privilege**: Users shall have minimum necessary access to perform their job functions
- **Separation of Duties**: Critical functions shall require multiple users or approvals
- **Need-to-Know Basis**: Access granted based on clinical or administrative need
- **Role Hierarchy**: Roles organized hierarchically with inheritance of permissions
- **Dynamic Permissions**: Permissions may vary based on context (e.g., assigned patients, current encounter)
- **Audit Trail**: All access and actions logged with user identification

##### 2.4.1.2 Permission Categories
- **Read Permissions**: Ability to view information
- **Write Permissions**: Ability to create or modify information
- **Delete Permissions**: Ability to delete information (typically restricted)
- **Print/Export Permissions**: Ability to print or export information
- **Sign Permissions**: Ability to electronically sign documents
- **Override Permissions**: Ability to override alerts or warnings
- **Administrative Permissions**: Ability to manage users, roles, and system configuration

#### 2.4.2 Core User Roles and Permissions

##### 2.4.2.1 Physician (MD/DO)

**Role Description**: Licensed physicians with full clinical authority and responsibility for patient care.

**Access Level**: Full clinical access

**Permissions**:
- **Patient Records**:
  - Full read access to all patient records
  - Full write access to all patient record sections
  - Access to all patient demographics
  - Access to complete medical history
  - Access to all clinical notes (create, view, edit, sign)
  - Access to all vital signs
  - Access to all lab results and imaging studies
  - Access to all problems/diagnoses (add, modify, resolve)
  - Access to all allergies
  - Access to all medications
  
- **Clinical Documentation**:
  - Create all types of clinical notes
  - Edit own notes (before signing)
  - Sign clinical notes (primary signature)
  - Co-sign notes from other providers (if applicable)
  - Create SOAP notes, progress notes, consultation notes, discharge summaries
  - Create procedure notes
  - Create H&P notes
  
- **Orders and Results**:
  - Order laboratory tests
  - Order imaging studies
  - View all lab results
  - View all imaging reports
  - Acknowledge critical values
  - Override alerts (with documentation)
  
- **Problem and Diagnosis Management**:
  - Add diagnoses to problem list
  - Modify diagnoses
  - Resolve diagnoses
  - Verify diagnoses
  - Link diagnoses to encounters
  
- **Medication Management**:
  - View all medications
  - Add medications to medication list
  - Modify medications
  - Discontinue medications
  - Link medications to problems
  
- **Allergy Management**:
  - View all allergies
  - Add allergies
  - Modify allergies
  - Verify allergies
  - Resolve allergies
  
- **Administrative Functions**:
  - Print patient records
  - Export patient records
  - Access audit logs (own actions)
  - Break-the-glass emergency access (with audit trail)
  
- **Restrictions**:
  - Cannot delete signed notes (only amend)
  - Cannot permanently delete critical data
  - Cannot modify audit logs
  - Cannot access system administration functions (unless also assigned admin role)

##### 2.4.2.2 Nurse Practitioner (NP) / Physician Assistant (PA)

**Role Description**: Advanced practice providers with clinical authority similar to physicians.

**Access Level**: Full clinical access (similar to Physician)

**Permissions**:
- Same permissions as Physician role
- May have state-specific restrictions on certain functions
- May require physician co-signature for certain documentation (configurable)

##### 2.4.2.3 Registered Nurse (RN) / Licensed Practical Nurse (LPN)

**Role Description**: Licensed nursing staff providing direct patient care and clinical support.

**Access Level**: Clinical support access

**Permissions**:
- **Patient Records**:
  - Read access to assigned patients' records
  - Read access to all patients in assigned unit/facility (if applicable)
  - Limited write access to delegated sections
  
- **Clinical Documentation**:
  - Create nursing notes
  - Create progress notes (as delegated)
  - View all clinical notes
  - Cannot sign physician notes (unless co-signature authorized)
  - Cannot create certain note types (H&P, discharge summary, unless authorized)
  
- **Vital Signs**:
  - Full read/write access to vital signs
  - Record all vital sign measurements
  - View vital sign trends
  
- **Orders and Results**:
  - View lab results
  - View imaging reports
  - Cannot order lab tests or imaging studies
  - Can acknowledge critical values
  - Can document result review
  
- **Problem and Diagnosis Management**:
  - View all problems/diagnoses
  - Cannot add or modify diagnoses (unless specifically authorized)
  - Can add notes to problems
  
- **Medication Management**:
  - View all medications
  - Cannot add or modify medications (unless specifically authorized)
  - Can document medication administration
  - Can document medication adherence
  
- **Allergy Management**:
  - View all allergies
  - Can add patient-reported allergies (with provider verification)
  - Cannot verify allergies
  
- **Administrative Functions**:
  - Print patient records (assigned patients)
  - Export patient records (assigned patients)
  - Limited access to audit logs
  
- **Restrictions**:
  - Cannot sign physician-level documentation
  - Cannot order diagnostic tests
  - Cannot add diagnoses
  - Cannot prescribe medications
  - Cannot verify allergies

##### 2.4.2.4 Medical Assistant (MA)

**Role Description**: Clinical support staff assisting with patient care and administrative tasks.

**Access Level**: Limited clinical access

**Permissions**:
- **Patient Records**:
  - Read access to assigned patients' records
  - Limited write access to delegated sections
  
- **Demographics**:
  - Read/write access to patient demographics
  - Can update contact information
  - Can update insurance information
  - Cannot modify critical demographics (SSN, DOB) without authorization
  
- **Vital Signs**:
  - Read/write access to vital signs
  - Can record vital signs
  - Can view vital sign trends
  
- **Clinical Documentation**:
  - Cannot create clinical notes
  - Can view clinical notes (read-only)
  - Can assist with note entry (under supervision)
  
- **Orders and Results**:
  - View lab results (read-only)
  - View imaging reports (read-only)
  - Cannot order tests
  - Cannot acknowledge critical values
  
- **Problem and Diagnosis Management**:
  - View problems/diagnoses (read-only)
  - Cannot add or modify diagnoses
  
- **Medication Management**:
  - View medications (read-only)
  - Cannot add or modify medications
  
- **Allergy Management**:
  - View allergies (read-only)
  - Can add patient-reported allergies (with provider verification)
  
- **Administrative Functions**:
  - Limited print access
  - Cannot export records
  - No access to audit logs
  
- **Restrictions**:
  - Cannot create or sign clinical documentation
  - Cannot order diagnostic tests
  - Cannot add diagnoses
  - Cannot prescribe medications
  - Cannot modify critical patient information without authorization

##### 2.4.2.5 Administrative Staff

**Role Description**: Non-clinical staff handling administrative and registration tasks.

**Access Level**: Administrative access only

**Permissions**:
- **Patient Records**:
  - Read access to patient demographics and insurance
  - Write access to demographics and insurance only
  - No access to clinical information
  
- **Demographics**:
  - Full read/write access to patient demographics
  - Can register new patients
  - Can update contact information
  - Can update insurance information
  - Cannot modify critical demographics (SSN, DOB) without authorization
  
- **Insurance**:
  - Full read/write access to insurance information
  - Can verify insurance
  - Can update insurance details
  
- **Clinical Information**:
  - No access to clinical notes
  - No access to vital signs
  - No access to lab results
  - No access to imaging studies
  - No access to problems/diagnoses
  - No access to medications
  - No access to allergies
  - No access to medical history
  
- **Administrative Functions**:
  - Can print demographic reports
  - Can export demographic data
  - Limited access to audit logs (own actions only)
  
- **Restrictions**:
  - Cannot access any clinical information
  - Cannot view PHI beyond demographics and insurance
  - Cannot create clinical documentation
  - Cannot order tests or medications

##### 2.4.2.6 Specialist Physician

**Role Description**: Specialist physicians with access to assigned patients and consultation cases.

**Access Level**: Full clinical access for assigned patients, read-only for others

**Permissions**:
- **Assigned Patients**:
  - Full read/write access (same as Physician role)
  - All clinical documentation permissions
  - All ordering permissions
  - All problem/diagnosis management permissions
  
- **Consultation Patients** (patients referred for consultation):
  - Full read access
  - Write access to consultation notes
  - Write access to problems related to consultation
  - Can order tests related to consultation
  - Cannot modify non-consultation-related information
  
- **Other Patients**:
  - Read-only access (if authorized)
  - Cannot create documentation
  - Cannot order tests
  - Cannot modify records
  
- **All Other Permissions**: Same as Physician role for assigned/consultation patients

##### 2.4.2.7 Pharmacist

**Role Description**: Licensed pharmacists reviewing medications and providing pharmaceutical care.

**Access Level**: Medication-focused clinical access

**Permissions**:
- **Patient Records**:
  - Read access to assigned patients' records
  - Limited write access to medication-related sections
  
- **Medication Management**:
  - Full read access to all medications
  - Can add medication notes
  - Can document medication reviews
  - Can suggest medication changes (with provider approval)
  - Cannot directly modify medications (unless authorized)
  
- **Allergy Management**:
  - Full read access to allergies
  - Can add medication allergies (with provider verification)
  - Can document allergy-related information
  
- **Lab Results**:
  - Read access to lab results (especially medication monitoring labs)
  - Can document lab result interpretation related to medications
  
- **Clinical Notes**:
  - Read access to clinical notes
  - Can create medication review notes
  - Cannot create other clinical notes
  
- **Restrictions**:
  - Cannot order diagnostic tests
  - Cannot add diagnoses
  - Cannot create general clinical documentation
  - Cannot prescribe medications (unless authorized in jurisdiction)

##### 2.4.2.8 Medical Records / Health Information Management (HIM) Staff

**Role Description**: Staff managing medical records and health information.

**Access Level**: Records management access

**Permissions**:
- **Patient Records**:
  - Read access to all patient records
  - Limited write access for records management functions
  
- **Clinical Documentation**:
  - Read access to all clinical notes
  - Can add administrative notes
  - Can correct documentation errors (with authorization)
  - Can merge duplicate records (with authorization)
  - Cannot create clinical documentation
  
- **Records Management**:
  - Can archive records
  - Can retrieve archived records
  - Can manage record retention
  - Can process record requests
  - Can export records for legal/regulatory purposes
  
- **Data Quality**:
  - Can identify data quality issues
  - Can request data corrections
  - Can run data quality reports
  
- **Restrictions**:
  - Cannot create clinical documentation
  - Cannot order tests or medications
  - Cannot add diagnoses
  - Cannot modify clinical content (only administrative corrections)

##### 2.4.2.9 System Administrator

**Role Description**: IT staff managing system configuration and technical operations.

**Access Level**: Technical/administrative access

**Permissions**:
- **System Configuration**:
  - Full access to system settings
  - Can manage users and roles
  - Can configure permissions
  - Can manage system integrations
  - Can access system logs
  
- **User Management**:
  - Can create, modify, and deactivate user accounts
  - Can assign roles
  - Can reset passwords
  - Can manage user permissions
  
- **Data Management**:
  - Can access database for technical purposes
  - Can run system maintenance
  - Can manage backups
  - Can perform data migrations
  - Cannot access patient data for clinical purposes (unless also clinical role)
  
- **Audit and Security**:
  - Full access to audit logs
  - Can review security events
  - Can manage security settings
  - Can investigate security incidents
  
- **Restrictions**:
  - Should not access patient data for clinical purposes (unless also clinical role)
  - Cannot create clinical documentation
  - Cannot order tests or medications
  - All patient data access logged and monitored

##### 2.4.2.10 Patient Portal User

**Role Description**: Patients accessing their own health information through patient portal.

**Access Level**: Self-service read access (optional feature)

**Permissions**:
- **Own Records Only**:
  - Read-only access to own patient record
  - Cannot access other patients' records
  
- **Viewable Information**:
  - View own demographics (read-only)
  - View own medical history
  - View own problems/diagnoses
  - View own medications
  - View own allergies
  - View own lab results (may be delayed or filtered)
  - View own imaging reports (may be delayed or filtered)
  - View own clinical notes (may be delayed or filtered)
  - View own vital signs
  - View own immunizations
  
- **Self-Service Functions**:
  - Can request corrections to demographic information
  - Can add self-reported information (flagged as patient-reported)
  - Can request medication refills (if integrated with prescription system)
  - Can send messages to providers (if portal messaging available)
  - Can schedule appointments (if scheduling integrated)
  
- **Restrictions**:
  - Read-only access (cannot modify clinical information)
  - Cannot access other patients' records
  - Cannot create clinical documentation
  - Cannot order tests or medications
  - Cannot view certain sensitive information (may be filtered based on patient preferences or clinical judgment)
  - Cannot access audit logs

#### 2.4.3 Permission Matrix

##### 2.4.3.1 Patient Record Access Matrix

| Feature | Physician | NP/PA | RN/LPN | MA | Admin | Specialist | Pharmacist | HIM | Patient Portal |
|---------|-----------|-------|--------|-----|-------|------------|------------|-----|----------------|
| Demographics - Read | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (own only) |
| Demographics - Write | ✓ | ✓ | Limited | ✓ | ✓ | ✓ (assigned) | ✗ | Limited | Request only |
| Medical History - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only) |
| Medical History - Write | ✓ | ✓ | Limited | Limited | ✗ | ✓ (assigned) | ✗ | Limited | Request only |
| Vital Signs - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only) |
| Vital Signs - Write | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Clinical Notes - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only, delayed) |
| Clinical Notes - Write | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | Limited | ✗ |
| Clinical Notes - Sign | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | ✗ |
| Problems - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only) |
| Problems - Write | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Lab Results - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only, delayed) |
| Lab Results - Order | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Imaging - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | Limited | ✓ | ✓ (own only, delayed) |
| Imaging - Order | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Medications - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only) |
| Medications - Write | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | Request only |
| Allergies - Read | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ (own only) |
| Allergies - Write | ✓ | ✓ | Limited | Limited | ✗ | ✓ (assigned) | Limited | Limited | Request only |
| Print/Export | ✓ | ✓ | Limited | Limited | Limited | ✓ | Limited | ✓ | Limited |

##### 2.4.3.2 Action Permissions Matrix

| Action | Physician | NP/PA | RN/LPN | MA | Admin | Specialist | Pharmacist | HIM | Patient Portal |
|--------|-----------|-------|--------|-----|-------|------------|------------|-----|----------------|
| Create Note | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | ✗ |
| Sign Note | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | ✗ |
| Order Lab | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Order Imaging | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Add Diagnosis | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ | ✗ |
| Prescribe Medication | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | ✗ |
| Override Alerts | ✓ | ✓ | Limited | ✗ | ✗ | ✓ (assigned) | Limited | ✗ | ✗ |
| Break-the-Glass | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| Merge Records | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ (authorized) | ✗ |
| Delete Data | Limited | Limited | ✗ | ✗ | ✗ | Limited | ✗ | Limited | ✗ |

#### 2.4.4 Context-Based Permissions

##### 2.4.4.1 Assigned Patient Permissions

- **Primary Care Provider**: Full access to assigned patients
- **Specialist**: Full access to patients referred for consultation
- **Care Team Member**: Access based on care team assignment
- **Covering Provider**: Temporary full access during coverage period
- **On-Call Provider**: Emergency access with audit trail

##### 2.4.4.2 Encounter-Based Permissions

- **Current Encounter**: Enhanced permissions during active encounter
- **Past Encounters**: Read-only access to past encounters (unless assigned provider)
- **Future Encounters**: Limited access to scheduled encounters

##### 2.4.4.3 Location-Based Permissions

- **Home Facility**: Full access to patients at user's primary facility
- **Other Facilities**: Limited or read-only access based on facility agreements
- **Multi-Facility Access**: Configurable based on organizational policies

#### 2.4.5 Special Access Scenarios

##### 2.4.5.1 Emergency Access (Break-the-Glass)

- **Purpose**: Allow emergency access to patient records when normal access is insufficient
- **Authorization**: Requires justification and is logged
- **Access Level**: Full access for duration of emergency
- **Audit**: All actions logged with emergency access flag
- **Review**: Emergency access reviewed by security officer
- **Available To**: Clinical staff (Physicians, NPs, PAs, RNs)

##### 2.4.5.2 Delegation of Authority

- **Temporary Delegation**: Providers can delegate specific permissions temporarily
- **Delegation Scope**: Limited to specific functions (e.g., note co-signature)
- **Delegation Duration**: Time-limited with automatic expiration
- **Audit**: All delegated actions logged with delegator information

##### 2.4.5.3 Proxy Access

- **Purpose**: Allow authorized users to act on behalf of patients
- **Authorization**: Requires patient consent or legal authority
- **Scope**: Limited to specific functions
- **Documentation**: Proxy relationship documented
- **Audit**: All proxy actions logged

#### 2.4.6 Permission Management

##### 2.4.6.1 Role Assignment

- **Initial Assignment**: Roles assigned during user account creation
- **Role Changes**: Roles can be modified by authorized administrators
- **Multiple Roles**: Users can have multiple roles (e.g., Physician + System Administrator)
- **Role Hierarchy**: Higher roles inherit permissions from lower roles (if applicable)

##### 2.4.6.2 Permission Customization

- **Organization-Level**: Permissions can be customized at organization level
- **Department-Level**: Permissions can be customized at department level
- **User-Level**: Individual user permissions can be customized (with authorization)
- **Temporary Permissions**: Time-limited permissions can be granted

##### 2.4.6.3 Permission Review

- **Regular Review**: User permissions reviewed regularly (e.g., annually)
- **Change Management**: Permission changes require approval
- **Audit**: All permission changes logged
- **Compliance**: Permission reviews documented for compliance

#### 2.4.7 Security and Compliance

##### 2.4.7.1 Access Logging

- **All Access Logged**: All patient record access logged with user, timestamp, and action
- **Failed Access Attempts**: Failed access attempts logged
- **Audit Trail**: Complete audit trail maintained
- **Log Retention**: Access logs retained per regulatory requirements

##### 2.4.7.2 Access Monitoring

- **Real-Time Monitoring**: Suspicious access patterns monitored in real-time
- **Alerts**: Alerts generated for unusual access patterns
- **Review**: Access logs reviewed regularly
- **Investigation**: Security incidents investigated promptly

##### 2.4.7.3 Compliance Requirements

- **HIPAA Compliance**: Access controls comply with HIPAA requirements
- **Minimum Necessary**: Minimum necessary principle enforced
- **Patient Rights**: Patient access rights supported
- **Regulatory Compliance**: Access controls comply with applicable regulations

---

