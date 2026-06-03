# User Stories

### 7.1 Patient Health Records

This section provides comprehensive user stories for the Patient Health Records feature, organized by functional area. Each user story follows the standard format: "As a [role], I want to [action] so that [benefit]."

#### 7.1.1 Patient Registration and Demographics

- **US-1**: As an administrative staff member, I want to register new patients with comprehensive demographic information including personal identification, contact information, emergency contacts, and insurance information, with real-time validation of data formats and completeness, so patient records are accurate and complete from the first visit.

- **US-2**: As a provider, I want to search for patients by multiple criteria including full name, partial name, Medical Record Number (MRN), date of birth, phone number, or email address, with the system displaying search results within 3 seconds, showing patient name, MRN, date of birth, and last visit date, so I can quickly access patient records and begin care without delays.

- **US-115**: As an administrative staff member, I want the system to check for duplicate patient records during registration by matching on name, date of birth, SSN, phone number, or email, so I can prevent creating duplicate records and maintain data integrity.

- **US-116**: As a registration staff member, I want to update patient demographic information with complete audit trail tracking of all changes including who made the change, when, and what was changed, so patient information remains current and changes are documented for compliance.

- **US-117**: As an administrative staff member, I want to merge duplicate patient records when identified, with side-by-side comparison of records and ability to select which data to keep, so patient records are consolidated and clinical history is preserved.

- **US-118**: As a provider, I want to see patient demographic information including age, gender, preferred language, and insurance information displayed in the patient summary, so I have context about the patient when providing care.

- **US-119**: As an administrative staff member, I want to manage patient status (Active, Inactive, Deceased, Archived) with appropriate restrictions on actions based on status, so patient records are properly maintained throughout their lifecycle.

- **US-120**: As a registration staff member, I want to track patient privacy preferences and consent forms including HIPAA acknowledgment and marketing consent, so patient privacy rights are respected and documented.

- **US-121**: As an administrative staff member, I want to generate patient demographic reports including registration statistics, missing data reports, and insurance coverage breakdowns, so I can monitor data quality and administrative metrics.

- **US-121a**: As a registration staff member, I want to create and print a plastic patient identification card for every patient at or after registration, with patient name, MRN, and barcode/QR code for scanning, so patients have a durable ID card for quick lookup at subsequent visits.

#### 7.1.2 Medical History Management

- **US-122**: As a physician, I want to document a patient's past medical history including chronic conditions, surgical history, hospitalizations, and major illnesses with dates, status (active/resolved), and severity, so I have a complete picture of the patient's medical background.

- **US-123**: As a provider, I want to record family medical history with relationship mapping (mother, father, siblings, etc.) and condition details, so I can assess genetic risk factors and family health patterns.

- **US-124**: As a physician, I want to document social history including smoking status, alcohol use, drug use, occupation, and lifestyle factors, so I can assess risk factors and provide appropriate counseling.

- **US-125**: As a provider, I want to maintain a complete immunization history with vaccine names, dates administered, lot numbers, and administering facility, so I can track vaccination status and ensure patients are up to date.

- **US-126**: As a physician, I want to view medical history in a chronological timeline format so I can see how conditions and events have progressed over time.

- **US-127**: As a provider, I want to attach medical documents (lab reports, imaging reports, outside records) to medical history entries, so supporting documentation is linked to the relevant history.

- **US-128**: As a physician, I want to update medical history entries with resolution dates, status changes, or additional information, so the medical history remains current and accurate.

- **US-129**: As a provider, I want to see medical history organized by category (past medical, family, social, immunizations) in the patient dashboard, so I can quickly review relevant information.

- **US-130**: As a physician, I want to search and filter medical history by condition type, date range, or status, so I can quickly find specific historical information.

#### 7.1.3 Vital Signs and Clinical Measurements

- **US-131**: As a nurse, I want to quickly record vital signs during a patient visit (including blood pressure, heart rate, respiratory rate, temperature, oxygen saturation, weight, height, and BMI calculation) with the ability to view historical trends in graphical format, and have this information automatically saved and immediately available to the physician in the patient's record, so the information is accessible for clinical decision-making during the same visit.

- **US-132**: As a provider, I want to see vital signs displayed in a table format with date and time stamps, so I can review measurements from current and past visits.

- **US-133**: As a physician, I want to view vital signs trends in graphical format (line charts, trend lines) over time, so I can identify patterns and changes in patient status.

- **US-134**: As a nurse, I want the system to calculate BMI automatically from weight and height measurements, so BMI is always current and accurate.

- **US-135**: As a provider, I want to see alerts when vital signs are outside normal ranges (e.g., high blood pressure, elevated temperature), so I can identify abnormal values immediately.

- **US-136**: As a physician, I want to compare current vital signs to previous measurements and see percentage changes, so I can assess improvement or deterioration.

- **US-137**: As a nurse, I want to record vital signs for specific encounters or as standalone measurements, so vital signs can be linked to visits or recorded independently.

- **US-138**: As a provider, I want to see growth charts for pediatric patients (height, weight, head circumference), so I can monitor growth and development.

- **US-139**: As a physician, I want to export vital signs data for reporting or analysis purposes, so I can use the data for quality measures or research.

- **US-140**: As a nurse, I want to enter vital signs using a mobile device or tablet at the point of care, so I can record measurements immediately without returning to a workstation.

#### 7.1.4 Clinical Notes and Documentation

- **US-141**: As a physician, I want to create SOAP notes (Subjective, Objective, Assessment, Plan) using customizable templates for different encounter types (office visit, consultation, follow-up, discharge summary), with the ability to use structured data entry, free text, rich text formatting, and electronic signature capabilities, so I can document patient encounters efficiently while maintaining consistency and completeness of clinical documentation.

- **US-142**: As a provider, I want to create different types of clinical notes (progress notes, consultation notes, procedure notes, discharge summaries) with appropriate templates for each type, so documentation matches the encounter type and clinical purpose.

- **US-143**: As a physician, I want to use note templates that can be customized for my specialty or practice patterns, so I can document encounters efficiently while maintaining my preferred documentation style.

- **US-144**: As a provider, I want to insert structured data (diagnoses, medications, vital signs, lab results) directly into notes from the patient record, so documentation is accurate and linked to source data.

- **US-145**: As a physician, I want to electronically sign notes with authentication (password, biometric) and have the signature timestamped and locked, so notes are legally valid and cannot be modified after signing.

- **US-146**: As a provider, I want to view the version history of notes including all edits, who made changes, and when, so I can track documentation changes and maintain audit trails.

- **US-147**: As a physician, I want to search notes by keywords, date range, encounter type, or author, so I can quickly find specific documentation.

- **US-148**: As a provider, I want to copy and modify previous notes when creating new documentation, so I can efficiently document follow-up visits.

- **US-149**: As a physician, I want to link notes to specific encounters, diagnoses, or procedures, so documentation is properly associated with clinical events.

- **US-150**: As a provider, I want to use voice-to-text capabilities for note creation, so I can document encounters more efficiently.

- **US-151**: As a physician, I want to see notes displayed in chronological order in the patient record, so I can review the progression of care over time.

- **US-152**: As a provider, I want to amend notes after signing with proper documentation of the amendment, so I can correct errors while maintaining documentation integrity.

#### 7.1.5 Diagnoses and Problem Lists

- **US-153**: As a physician, I want to add diagnoses to a patient's problem list with ICD-10/ICD-11 codes, status (active, resolved, chronic), and date of onset, so I can track all patient conditions.

- **US-154**: As a provider, I want to see active and resolved problems displayed separately in the problem list, so I can distinguish between current and past conditions.

- **US-155**: As a physician, I want to update problem status (e.g., mark as resolved with resolution date), so the problem list reflects current patient status.

- **US-156**: As a provider, I want to prioritize problems (primary, secondary) and organize them by category or system, so important conditions are highlighted.

- **US-157**: As a physician, I want to link problems to encounters, prescriptions, or lab results, so I can see the clinical context for each problem.

- **US-158**: As a provider, I want to search for diagnoses using ICD codes or condition names, so I can quickly find and add appropriate diagnoses.

- **US-159**: As a physician, I want to see problem lists prominently displayed in the patient summary, so I can quickly review all active conditions.

- **US-160**: As a provider, I want to add free-text problems when a specific ICD code is not available, so I can document all patient conditions.

- **US-161**: As a physician, I want to see problem history including when problems were added, modified, or resolved, so I can track problem list changes over time.

#### 7.1.6 Laboratory Results

- **US-162**: As a physician, I want to view laboratory results with test names, values, reference ranges, units, and abnormal value flags, so I can interpret results accurately.

- **US-163**: As a provider, I want to see critical value alerts prominently displayed when lab results are outside critical ranges, so I can respond to urgent results immediately.

- **US-164**: As a physician, I want to view lab results in chronological order with date and time stamps, so I can see how values have changed over time.

- **US-165**: As a provider, I want to see lab results displayed in graphical format (trend lines, scatter plots) for tests performed multiple times, so I can identify trends and patterns.

- **US-166**: As a physician, I want to see reference ranges displayed with lab results, so I can determine if values are normal, high, or low.

- **US-167**: As a provider, I want to filter lab results by test type, date range, or abnormal status, so I can quickly find specific results.

- **US-168**: As a physician, I want to add notes or interpretations to lab results, so I can document my clinical assessment of the findings.

- **US-169**: As a provider, I want to see lab results linked to the ordering provider and encounter, so I know the clinical context for each test.

- **US-170**: As a physician, I want to receive notifications when new lab results are available, so I can review results promptly.

- **US-171**: As a provider, I want to correct or amend lab results with proper documentation when errors are identified, so results are accurate and corrections are tracked.

- **US-172**: As a physician, I want to compare current lab results to previous results and see percentage changes, so I can assess improvement or deterioration.

- **US-173**: As a provider, I want to export lab results for reporting or sharing with patients, so I can use results for care coordination.

#### 7.1.7 Imaging and Diagnostic Studies

- **US-174**: As a physician, I want to view imaging study reports with study type, date performed, ordering provider, and interpreting radiologist, so I can review diagnostic imaging results.

- **US-175**: As a provider, I want to see critical findings alerts for imaging studies that require immediate attention, so I can respond to urgent findings promptly.

- **US-176**: As a physician, I want to view imaging reports in chronological order, so I can see how findings have changed over time.

- **US-177**: As a provider, I want to link imaging studies to specific encounters or diagnoses, so imaging is associated with the appropriate clinical context.

- **US-178**: As a physician, I want to add notes or interpretations to imaging reports, so I can document my clinical assessment of the findings.

- **US-179**: As a provider, I want to see imaging study metadata including modality (X-ray, CT, MRI, ultrasound), body part, and contrast use, so I understand the study details.

- **US-180**: As a physician, I want to access DICOM images when integrated with PACS systems, so I can view actual images in addition to reports.

- **US-181**: As a provider, I want to receive notifications when new imaging reports are available, so I can review results promptly.

- **US-182**: As a physician, I want to correct or amend imaging reports with proper documentation when errors are identified, so reports are accurate and corrections are tracked.

- **US-183**: As a provider, I want to search imaging studies by type, date range, body part, or ordering provider, so I can quickly find specific studies.

#### 7.1.8 Allergies and Adverse Reactions

- **US-184**: As a provider, I want to see a patient's allergy and adverse reaction list prominently displayed at the top of the patient dashboard and in all relevant clinical views, showing the allergen name, reaction type, severity, verification status, and date recorded, with visual alerts (such as red highlighting or warning icons) when viewing or creating prescriptions, so I can immediately identify potential medication contraindications and avoid prescribing medications that could cause adverse reactions.

- **US-185**: As a physician, I want to add new allergies with allergen name, reaction type, severity, and verification status, so patient allergy information is complete and current.

- **US-186**: As a provider, I want to update allergy information including marking allergies as resolved or updating severity, so allergy records reflect current status.

- **US-187**: As a physician, I want to see drug-allergy interaction alerts when prescribing medications, so I can avoid medications that could cause adverse reactions.

- **US-188**: As a provider, I want to verify allergy information with patients and update verification status, so allergy records are accurate and reliable.

- **US-189**: As a physician, I want to see allergies organized by type (drug, food, environmental) and severity, so I can quickly assess allergy risks.

- **US-190**: As a provider, I want to document adverse drug reactions separately from allergies, so adverse reactions are tracked and reported appropriately.

- **US-191**: As a physician, I want to see allergy history including when allergies were added, modified, or resolved, so I can track allergy information changes over time.

- **US-192**: As a provider, I want to search allergies by allergen name or type, so I can quickly find specific allergy information.

#### 7.1.9 Medications (Current and Historical)

- **US-193**: As a physician, I want to view a patient's current medication list with medication names, dosages, frequencies, start dates, and prescribing providers, so I can see all active medications.

- **US-194**: As a provider, I want to see medication history including discontinued medications with stop dates and reasons, so I have a complete medication timeline.

- **US-195**: As a physician, I want to add medications to the medication list manually or from prescriptions, so the medication list is comprehensive and current.

- **US-196**: As a provider, I want to discontinue medications with stop dates and reasons, so medication lists reflect current therapy.

- **US-197**: As a physician, I want to perform medication reconciliation comparing current medications to previous lists, so I can identify changes and discrepancies.

- **US-198**: As a provider, I want to see medications organized by status (active, discontinued, on hold) and category, so I can quickly review current therapy.

- **US-199**: As a physician, I want to link medications to diagnoses or problems, so I can see the clinical indication for each medication.

- **US-200**: As a provider, I want to see medication adherence information including fill dates and refill patterns, so I can assess patient compliance.

- **US-201**: As a physician, I want to search medications by name, prescribing provider, or date range, so I can quickly find specific medications.

- **US-202**: As a provider, I want to see drug interaction warnings when viewing medication lists, so I can identify potential interactions between current medications.

#### 7.1.10 Viewing and Access

- **US-203**: As a physician, I want to view a patient's complete medical history in one consolidated dashboard that includes past medical history (chronic conditions, surgical history, hospitalizations), family medical history, social history, immunization records, current and historical medications, active and resolved problem lists, recent vital signs with trend visualization, laboratory results with abnormal value flagging, imaging study reports, and allergy/adverse reaction information, all organized chronologically and by category, so I can make informed treatment decisions without navigating multiple screens or sections.

- **US-204**: As a provider, I want to see a chronological timeline of all patient encounters, diagnoses, medications, lab results, and clinical events, so I can understand the progression of care over time.

- **US-205**: As a physician, I want to filter and search patient records by date range, encounter type, diagnosis, or provider, so I can quickly find specific information.

- **US-206**: As a provider, I want to print or export patient records in standard formats (PDF, summary reports), so I can share information with patients or other providers.

- **US-207**: As a physician, I want to access patient records from mobile devices with responsive design, so I can review records at the point of care.

- **US-208**: As a provider, I want to customize the patient dashboard layout to show information most relevant to my practice, so I can work more efficiently.

- **US-209**: As a physician, I want to see patient records load within 3 seconds, so I can access information quickly without delays.

- **US-210**: As a provider, I want to navigate between different sections of the patient record (demographics, history, notes, results) easily, so I can access all information efficiently.

- **US-211**: As a physician, I want to see related information linked together (e.g., prescriptions linked to diagnoses, lab results linked to encounters), so I can understand clinical context.

- **US-212**: As a provider, I want role-based access controls that limit information display to what is appropriate for my role, so patient privacy is protected while I have access to necessary information.

#### 7.1.11 Data Quality and Validation

- **US-213**: As an administrative staff member, I want the system to validate all data entry with format checking, range validation, and completeness checks, so data quality is maintained.

- **US-214**: As a provider, I want to see data quality indicators (missing information, incomplete records) so I can identify and address data gaps.

- **US-215**: As an administrative staff member, I want to generate data quality reports identifying missing data, duplicate records, and validation errors, so I can improve data completeness.

#### 7.1.12 Security and Privacy

- **US-216**: As a provider, I want all access to patient records logged with user ID, timestamp, and actions taken, so there is a complete audit trail for compliance.

- **US-217**: As a physician, I want role-based access controls that ensure I can only access patient information appropriate to my role, so patient privacy is protected.

- **US-218**: As a provider, I want patient records encrypted at rest and in transit, so protected health information is secured.

- **US-219**: As an administrative staff member, I want to manage patient privacy flags and restrictions, so special privacy requirements are enforced.

- **US-220**: As a provider, I want to see alerts for patients with special privacy or security requirements, so I can handle these records appropriately.

### 7.2 Prescription Management

This section provides comprehensive user stories for the Prescription Management feature, organized by functional area. Each user story follows the standard format: "As a [role], I want to [action] so that [benefit]."

#### 7.2.1 Prescription Creation

- **US-6**: As a physician, I want to create an electronic prescription and send it directly to the patient's pharmacy so the patient can pick it up without a paper prescription.

- **US-7**: As a provider, I want to view a patient's current medication list when creating a new prescription so I can avoid duplicate therapies.

- **US-11**: As a physician, I want to search for medications by generic name, brand name, or indication so I can quickly find the appropriate medication for my patient.

- **US-12**: As a provider, I want to see medication strength options and dosage forms when selecting a medication so I can choose the most appropriate formulation.

- **US-13**: As a physician, I want to specify detailed dosing instructions including frequency, route, and duration so the patient receives clear medication instructions.

- **US-14**: As a provider, I want to add special instructions to prescriptions (e.g., "take with food", "avoid alcohol") so patients understand important medication considerations.

- **US-15**: As a physician, I want to link prescriptions to specific diagnoses/problems so I can document the clinical indication for the medication.

- **US-16**: As a provider, I want to use prescription templates for commonly prescribed medications so I can create prescriptions more efficiently.

- **US-17**: As a physician, I want to copy and modify existing prescriptions so I can quickly create similar prescriptions for follow-up visits.

- **US-18**: As a provider, I want to create multiple prescriptions in a single session so I can efficiently prescribe multiple medications during one patient visit.

- **US-19**: As a physician, I want to see formulary information for the patient's insurance when selecting medications so I can prescribe covered medications and reduce patient costs.

- **US-20**: As a provider, I want to view drug information including indications, contraindications, and side effects when creating a prescription so I can make informed prescribing decisions.

- **US-21**: As a physician, I want to specify whether generic substitution is allowed so the pharmacy can dispense cost-effective alternatives when appropriate.

- **US-22**: As a provider, I want to see the patient's age and weight when creating prescriptions so I can calculate appropriate pediatric or weight-based dosages.

#### 7.2.2 Drug Interaction and Allergy Checking

- **US-23**: As a provider, I want the system to alert me about drug interactions before I finalize a prescription so I can prevent medication errors.

- **US-24**: As a physician, I want to see drug-allergy interactions prominently displayed so I avoid prescribing medications that could cause adverse reactions.

- **US-25**: As a provider, I want to see the severity level of drug interactions (contraindicated, major, moderate, minor) so I can prioritize which interactions require immediate attention.

- **US-26**: As a physician, I want to see detailed information about drug interactions including mechanism and clinical significance so I can make informed decisions about whether to proceed.

- **US-27**: As a provider, I want to see duplicate therapy warnings when prescribing medications with similar therapeutic effects so I can avoid redundant medications.

- **US-28**: As a physician, I want the system to validate that dosages are appropriate for the patient's age, weight, and condition so I can prevent dosing errors.

- **US-29**: As a provider, I want to see drug-disease contraindications (e.g., medications that should be avoided with certain conditions) so I can avoid prescribing inappropriate medications.

- **US-30**: As a physician, I want to override drug interaction alerts with documentation when clinically appropriate so I can proceed with necessary medications while maintaining an audit trail.

- **US-31**: As a provider, I want interaction checking to occur in real-time as I select medications so I can address safety concerns immediately.

- **US-32**: As a physician, I want to see all current medications checked for interactions, not just the new prescription, so I have a complete picture of potential drug interactions.

#### 7.2.3 Prescription Transmission

- **US-33**: As a physician, I want prescriptions to be transmitted electronically to pharmacies so they are available when the patient arrives.

- **US-34**: As a provider, I want to select the patient's preferred pharmacy from a list so prescriptions are sent to the correct location.

- **US-35**: As a physician, I want to search for pharmacies by name, location, or NPI so I can find and select the appropriate pharmacy.

- **US-36**: As a provider, I want to see confirmation that prescriptions were successfully transmitted so I know the pharmacy received the prescription.

- **US-37**: As a physician, I want to be notified if prescription transmission fails so I can take alternative action (fax or print).

- **US-38**: As a provider, I want prescriptions to be transmitted in standard NCPDP SCRIPT format so pharmacies can process them efficiently.

- **US-39**: As a physician, I want to see the transmission status of each prescription (sent, received, filled) so I can track prescription fulfillment.

- **US-40**: As a provider, I want the option to fax or print prescriptions when electronic transmission is not available so I can still provide prescriptions to patients.

- **US-41**: As a physician, I want prescriptions to be transmitted securely with encryption so patient information is protected.

- **US-42**: As a provider, I want to batch transmit multiple prescriptions for the same patient so I can send all prescriptions in one transaction.

#### 7.2.4 Prescription Management and Tracking

- **US-43**: As a physician, I want to view all prescriptions I have written for a patient so I can review medication history and make informed decisions.

- **US-44**: As a provider, I want to see the current status of each prescription (draft, signed, sent, received, filled, cancelled) so I know where each prescription is in the workflow.

- **US-45**: As a physician, I want to filter prescriptions by status, date range, or medication so I can quickly find specific prescriptions.

- **US-46**: As a provider, I want to see when prescriptions were filled and how many refills remain so I can track medication adherence.

- **US-47**: As a physician, I want to see prescription expiration dates so I know when prescriptions are no longer valid.

- **US-48**: As a provider, I want to view prescription details including all instructions and special notes so I can review what was prescribed.

- **US-49**: As a physician, I want to see a chronological list of all prescriptions for a patient so I can understand medication changes over time.

- **US-50**: As a provider, I want to search for prescriptions across all patients by medication name, date, or status so I can find specific prescriptions for administrative purposes.

- **US-51**: As a physician, I want to see which prescriptions are active, discontinued, or expired so I can manage the patient's current medication list.

#### 7.2.5 Prescription Refills

- **US-52**: As a physician, I want to approve prescription refill requests from pharmacies so patients can continue their medications without an office visit.

- **US-53**: As a provider, I want to see refill requests organized by patient and medication so I can efficiently review and process requests.

- **US-54**: As a physician, I want to see the patient's current status and recent visits when reviewing refill requests so I can make informed approval decisions.

- **US-55**: As a provider, I want to approve refill requests with one click for routine medications so I can process refills quickly.

- **US-56**: As a physician, I want to deny refill requests with a reason so the pharmacy and patient understand why the refill was not approved.

- **US-57**: As a provider, I want to modify refill requests (change quantity, add instructions) before approving so I can adjust prescriptions as needed.

- **US-58**: As a physician, I want to see how many refills remain on a prescription when reviewing refill requests so I know if a new prescription is needed.

- **US-59**: As a provider, I want to set up automatic approval rules for certain medications so routine refills are processed without manual review.

- **US-60**: As a physician, I want to see the refill history for each prescription so I can track medication adherence and usage patterns.

- **US-61**: As a provider, I want to receive notifications when refill requests are received so I can process them promptly.

- **US-62**: As a physician, I want to see which pharmacy requested each refill so I can verify the request is legitimate.

- **US-63**: As a provider, I want to approve refills for multiple medications at once so I can efficiently process batch refill requests.

#### 7.2.6 Prescription Modifications and Cancellations

- **US-64**: As a physician, I want to modify prescriptions that haven't been sent yet so I can correct errors before transmission.

- **US-65**: As a provider, I want to cancel prescriptions that have been sent but not yet filled so I can stop inappropriate prescriptions.

- **US-66**: As a physician, I want to replace existing prescriptions with new ones when changes are needed so the pharmacy receives updated instructions.

- **US-67**: As a provider, I want to see which prescriptions can be modified or cancelled based on their status so I know what actions are available.

- **US-68**: As a physician, I want to document the reason for prescription modifications or cancellations so there is an audit trail of changes.

- **US-69**: As a provider, I want to be notified if a prescription cannot be cancelled because it has already been filled so I know alternative actions may be needed.

- **US-70**: As a physician, I want to modify prescription quantities, frequencies, or instructions when needed so I can adjust treatment plans.

#### 7.2.7 Controlled Substances Management

- **US-71**: As a physician, I want the system to identify controlled substances automatically so enhanced security requirements are applied.

- **US-72**: As a provider, I want my DEA number validated when prescribing controlled substances so I can only prescribe medications I am authorized to prescribe.

- **US-73**: As a physician, I want to see PDMP (Prescription Drug Monitoring Program) data for patients when prescribing controlled substances so I can identify potential misuse.

- **US-74**: As a provider, I want the system to enforce quantity and duration limits for controlled substances so I comply with regulatory requirements.

- **US-75**: As a physician, I want to see which controlled substance schedule a medication belongs to (I, II, III, IV, V) so I understand prescribing restrictions.

- **US-76**: As a provider, I want the system to prevent refills for Schedule II controlled substances (unless state allows) so I comply with federal regulations.

- **US-77**: As a physician, I want enhanced security measures applied to controlled substance prescriptions so they are protected from unauthorized access.

- **US-78**: As a provider, I want to see state-specific requirements for controlled substances when prescribing so I comply with local regulations.

- **US-79**: As a physician, I want controlled substance prescriptions to be clearly marked and tracked separately so they receive appropriate monitoring.

#### 7.2.8 Prescription History and Reporting

- **US-80**: As a physician, I want to view a complete prescription history for each patient so I can see all medications prescribed over time.

- **US-81**: As a provider, I want to see medication adherence information (fill dates, refill patterns) so I can assess patient compliance.

- **US-82**: As a physician, I want to generate reports of prescriptions I have written by date range, medication, or patient so I can analyze prescribing patterns.

- **US-83**: As a provider, I want to see prescription analytics including most prescribed medications and refill rates so I can understand practice patterns.

- **US-84**: As a physician, I want to see an audit trail of all prescription activities (creation, modification, transmission) so I can track prescription history.

- **US-85**: As a provider, I want to export prescription data for reporting purposes so I can analyze medication utilization and quality measures.

- **US-86**: As a physician, I want to see prescription trends over time so I can identify patterns in prescribing behavior.

#### 7.2.9 Integration and Workflow

- **US-87**: As a physician, I want prescriptions to automatically update the patient's medication list so the medication history is always current.

- **US-88**: As a provider, I want to see patient allergies prominently displayed when creating prescriptions so I avoid prescribing medications that cause reactions.

- **US-89**: As a physician, I want prescriptions to be linked to patient encounters so they are associated with the visit where they were created.

- **US-90**: As a provider, I want prescriptions to appear in the patient summary view so I can see all medications at a glance.

- **US-91**: As a physician, I want to see relevant lab results when prescribing medications that require monitoring so I can adjust dosages appropriately.

- **US-92**: As a provider, I want prescriptions to be included in clinical notes automatically so documentation is complete.

- **US-93**: As a physician, I want to access prescription management from the patient record view so I can create prescriptions in context of patient care.

- **US-94**: As a provider, I want prescriptions to be searchable and filterable in patient records so I can quickly find specific prescriptions.

#### 7.2.10 Pharmacy and External System Integration

- **US-95**: As a pharmacist, I want to receive electronic prescriptions in a standard format so I can process them efficiently.

- **US-96**: As a pharmacist, I want prescriptions to include all required information (medication, dosage, instructions, prescriber) so I can fill them accurately.

- **US-97**: As a pharmacist, I want to send refill requests electronically to prescribers so patients can get refills without visiting the office.

- **US-98**: As a pharmacist, I want to receive prescription modifications and cancellations electronically so I can update prescription status in real-time.

- **US-99**: As a pharmacist, I want to send fill status updates to prescribers so they know when prescriptions have been dispensed.

- **US-100**: As a provider, I want the system to check patient insurance formulary when creating prescriptions so I can prescribe covered medications.

- **US-101**: As a physician, I want to receive prior authorization requests from pharmacies electronically so I can respond quickly.

- **US-102**: As a provider, I want the system to integrate with drug information databases so I have access to current medication information.

#### 7.2.11 Error Handling and Recovery

- **US-103**: As a physician, I want clear error messages when prescription creation fails so I can correct issues and complete the prescription.

- **US-104**: As a provider, I want the system to save prescription drafts automatically so I don't lose work if the session is interrupted.

- **US-105**: As a physician, I want to retry failed prescription transmissions so I can resend prescriptions without recreating them.

- **US-106**: As a provider, I want validation errors displayed clearly with suggestions for correction so I can fix prescription issues quickly.

- **US-107**: As a physician, I want the system to handle network interruptions gracefully so prescription work is not lost during transmission.

#### 7.2.12 Security and Compliance

- **US-108**: As a physician, I want only authorized prescribers to create prescriptions so prescription integrity is maintained.

- **US-109**: As a provider, I want all prescription activities logged with my user ID and timestamp so there is a complete audit trail.

- **US-110**: As a physician, I want prescriptions encrypted during transmission so patient information is protected.

- **US-111**: As a provider, I want controlled substance prescriptions to require additional authentication so they are protected from unauthorized access.

- **US-112**: As a physician, I want the system to comply with HIPAA requirements for prescription data so patient privacy is protected.

- **US-113**: As a provider, I want the system to comply with DEA regulations for controlled substances so I meet federal requirements.

- **US-114**: As a physician, I want the system to comply with state prescription regulations so I meet local requirements.

---

### 7.3 Pharmacy – Central Store & Outlet Operations

This section provides user stories for the Hospital Pharmacy module, covering Central Store, Outlet Pharmacies, requisitions, purchasing, dispensing, returns, stock control, and due collection. Each user story follows the format: "As a [role], I want to [action] so that [benefit]."

#### 7.3.1 Central Store Receiving and Stock Control

- **US-300**: As a Central Store Admin, I want to receive medicines from suppliers by creating a GRN with batch, expiry, quantity, and pricing details so that Central Store stock and valuation are updated accurately.

- **US-301**: As a Central Store Admin, I want the system to prevent saving a GRN with past expiry dates or missing mandatory batch/expiry information so that invalid stock does not enter the warehouse inventory.

- **US-302**: As an Approver, I want to review and approve or reject high-value GRNs based on configurable thresholds so that large purchases are always subject to oversight.

- **US-303**: As a Central Store Admin, I want to record emergency purchase entries (without a pre-existing PO) with a special flag and mandatory reason so that urgent procurements are captured without bypassing audit.

#### 7.3.2 Outlet Requisition and Inter‑Pharmacy Transfers

- **US-304**: As an Outlet Pharmacist, I want to raise a requisition to Central Store selecting my outlet, products, and requested quantities so that my pharmacy can request replenishment when stock is low.

- **US-305**: As a Central Store Approver, I want to see requested vs approved quantities for each requisition line so that I can approve fully, approve partially, or reject with a reason based on stock availability and policy.

- **US-306**: As an Outlet Pharmacist, I want my outlet stock to update automatically when a Central Store requisition is approved and dispatched so that my outlet ledger always reflects actual on-hand stock.

- **US-307**: As an Outlet Pharmacist, I want to raise an inter‑pharmacy requisition from my outlet to another outlet so that I can request surplus items from peer locations before ordering from suppliers.

- **US-308**: As an Issuing Outlet Pharmacist, I want to approve or reject inter‑pharmacy requisitions based on my stock levels and policies so that I do not create shortages in my own outlet.

#### 7.3.3 Purchase Suggestion and PO Generation

- **US-309**: As a Central Store Pharmacist, I want to generate a purchase requisition by entering a sales analysis period, forecast days, and supplier so that the system can auto-suggest required order quantities per item.

- **US-310**: As a Central Store Pharmacist, I want to see for each product the average daily sale, current stock, forecast requirement, and calculated request quantity so that I understand how the system derived the suggested order.

- **US-311**: As a Central Store Pharmacist, I want the system to exclude products where current stock already covers the forecast period so that I only see items that actually need replenishment.

- **US-312**: As a Central Store Pharmacist, I want to manually edit the suggested Order Quantity per product before submission, with all changes logged, so that I can incorporate practical considerations while maintaining an audit trail.

- **US-313**: As a Central Store Approver, I want to approve or reject purchase requisitions and automatically generate POs for approved items so that supplier orders are created consistently from the same workflow.

#### 7.3.4 Dispensing, Returns, and Department Issues

- **US-314**: As an Outlet Pharmacist, I want to dispense medicines against an OPD prescription with full or partial issue and reasons for non-issued items so that patient sales are accurate and unfilled items are traceable.

- **US-315**: As an Outlet Pharmacist, I want to issue medicines to IPD patients or departments with automatic posting to IPD bills or department cost centers so that clinical consumption is properly billed or costed.

- **US-316**: As an Outlet Pharmacist, I want to process patient and department returns linked to the original invoice or issue note so that stock and financial adjustments are consistent and auditable.

- **US-317**: As a Supervisor, I want to approve or reject high-value sales returns, company returns, and stock adjustments based on configured thresholds so that exceptional transactions receive appropriate review.

#### 7.3.5 Due Collection, Reporting, and Audit

- **US-318**: As a Pharmacy Cashier, I want to record partial payments against pharmacy dues and see the remaining balance for each patient so that dues can be collected over multiple visits.

- **US-319**: As a Finance/Accounts user, I want to view pharmacy due aging reports grouped by aging buckets so that I can prioritize follow-up on long-outstanding balances.

- **US-320**: As an Auditor, I want to view a complete audit trail for all pharmacy stock movements, including user, timestamp, source/destination locations, and before/after stock levels so that I can verify compliance and investigate discrepancies.