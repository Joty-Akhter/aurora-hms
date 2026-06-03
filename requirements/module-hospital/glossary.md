# Glossary

This glossary provides definitions of key terms, acronyms, and standards used throughout this requirements document. Terms are organized by category for easy reference.

### 10.1 General Healthcare Terms

- **EHR (Electronic Health Record)**: Digital version of a patient's paper chart that contains comprehensive patient health information from multiple providers and healthcare organizations. EHRs are maintained by healthcare providers and include medical history, diagnoses, medications, treatment plans, immunization dates, allergies, radiology images, and laboratory test results.

- **EMR (Electronic Medical Record)**: Digital version of a patient's chart from a single practice or healthcare organization. EMRs are typically used within a single organization, while EHRs are designed to be shared across multiple healthcare organizations.

- **PHI (Protected Health Information)**: Any information about health status, provision of healthcare, or payment for healthcare that can be linked to an individual. PHI includes demographic information, medical history, test results, insurance information, and any other information that could identify a patient.

- **CPOE (Computerized Physician Order Entry)**: Process of healthcare providers entering medical orders (prescriptions, lab tests, imaging studies) into a computer system rather than using paper, verbal, or fax orders. CPOE systems help reduce medication errors and improve patient safety.

- **CDSS (Clinical Decision Support System)**: Health information technology system designed to provide clinicians with patient-specific information and recommendations to enhance clinical decision-making. CDSS includes drug interaction checking, clinical alerts, and evidence-based treatment recommendations.

- **SOAP Note**: Structured method of documentation used by healthcare providers. SOAP stands for:
  - **Subjective**: Patient's description of symptoms and concerns
  - **Objective**: Observable, measurable findings from physical examination and tests
  - **Assessment**: Provider's diagnosis and clinical judgment
  - **Plan**: Treatment plan, medications, follow-up instructions

- **Encounter**: A patient visit or interaction with a healthcare provider. Encounters can be office visits, hospitalizations, emergency department visits, or telehealth consultations.

- **MRN (Medical Record Number)**: Unique identifier assigned to a patient's medical record within a healthcare organization. MRNs are used to link all patient information and clinical data.

- **NPI (National Provider Identifier)**: Unique 10-digit identification number for healthcare providers, healthcare facilities, and healthcare organizations in the United States. NPIs are required for electronic healthcare transactions.

- **DEA Number (Drug Enforcement Administration Number)**: Unique identifier assigned by the US Drug Enforcement Administration to healthcare providers authorized to prescribe controlled substances. DEA numbers are required for prescribing Schedule II-V controlled substances.

### 10.2 Regulatory and Compliance Terms

- **HIPAA (Health Insurance Portability and Accountability Act)**: US federal legislation enacted in 1996 that provides data privacy and security provisions for safeguarding medical information. HIPAA includes Privacy Rules and Security Rules that govern how PHI must be protected.

- **HIPAA Privacy Rule**: Federal regulations that protect the privacy of individually identifiable health information. The Privacy Rule establishes standards for how healthcare providers must protect patient information and gives patients rights over their health information.

- **HIPAA Security Rule**: Federal regulations that establish national standards for protecting electronic PHI. The Security Rule requires administrative, physical, and technical safeguards to ensure the confidentiality, integrity, and security of electronic PHI.

- **PDMP (Prescription Drug Monitoring Program)**: State-run electronic databases that track controlled substance prescriptions. PDMPs help identify potential prescription drug abuse and support clinical decision-making when prescribing controlled substances.

- **Controlled Substance**: Medications regulated by the US Drug Enforcement Administration due to their potential for abuse or dependence. Controlled substances are classified into five schedules (I-V) based on their medical use and abuse potential.

- **Schedule II Controlled Substance**: Drugs with high potential for abuse and severe psychological or physical dependence. Examples include opioids (morphine, oxycodone), stimulants (amphetamine), and barbiturates. Schedule II prescriptions generally cannot be refilled.

- **Schedule III-V Controlled Substances**: Drugs with lower potential for abuse than Schedule II. These medications may have refills authorized, subject to state regulations.

### 10.3 Medical Coding and Terminology Standards

- **ICD-10 (International Classification of Diseases, 10th Revision)**: Medical classification system published by the World Health Organization (WHO) for coding diagnoses, symptoms, and procedures. ICD-10 codes are used for billing, quality reporting, and clinical documentation.

- **ICD-11 (International Classification of Diseases, 11th Revision)**: The most recent revision of the ICD classification system, designed to be more compatible with electronic health records and modern healthcare practices.

- **CPT (Current Procedural Terminology)**: Medical code set maintained by the American Medical Association (AMA) used to report medical, surgical, and diagnostic procedures and services. CPT codes are used for billing and documentation purposes.

- **LOINC (Logical Observation Identifiers Names and Codes)**: International standard for identifying health measurements, observations, and documents. LOINC codes are used to standardize laboratory test results, vital signs, and clinical observations.

- **SNOMED CT (Systematized Nomenclature of Medicine Clinical Terms)**: Comprehensive clinical terminology system that provides codes, terms, synonyms, and definitions for clinical concepts. SNOMED CT is used for clinical documentation and interoperability.

- **RxNorm**: Normalized naming system for clinical drugs and drug delivery devices. RxNorm provides standard names and codes for medications, facilitating interoperability between different drug information systems.

- **NDC (National Drug Code)**: Unique 10-digit or 11-digit identifier for medications in the United States. NDC codes identify the labeler (manufacturer), product, and package size.

- **CVX (Vaccine Codes)**: Standardized codes for vaccines maintained by the Centers for Disease Control and Prevention (CDC). CVX codes are used to identify specific vaccines in immunization records.

### 10.4 Prescription and Pharmacy Terms

- **E-Prescribing (Electronic Prescribing)**: The process of electronically generating and transmitting prescriptions from healthcare providers to pharmacies. E-prescribing improves medication safety and reduces prescription errors.

- **NCPDP (National Council for Prescription Drug Programs)**: Standards organization that develops and maintains standards for pharmacy services, including the NCPDP SCRIPT standard for electronic prescription transmission.

- **NCPDP SCRIPT**: Standard format for electronic prescription transmission between prescribers and pharmacies. NCPDP SCRIPT ensures interoperability between different e-prescribing systems.

- **DAW (Dispense As Written) Code**: Code indicating whether generic substitution is allowed for a prescription. DAW codes range from 0-9, with 0 indicating substitution allowed and 1 indicating brand name required.

- **Formulary**: List of prescription medications covered by a health insurance plan or preferred by a healthcare organization. Formularies help control costs and guide prescribing decisions.

- **PBM (Pharmacy Benefit Manager)**: Third-party administrators that manage prescription drug benefits for health insurance plans. PBMs process prescriptions, manage formularies, and negotiate drug prices.

- **Prior Authorization**: Process requiring healthcare providers to obtain approval from insurance companies before prescribing certain medications or procedures. Prior authorization ensures medical necessity and cost-effectiveness.

- **Surescripts**: Major e-prescribing network in the United States that connects prescribers, pharmacies, and pharmacy benefit managers for electronic prescription transmission and medication history exchange.

### 10.5 Medical Imaging and Diagnostic Terms

- **PACS (Picture Archiving and Communication System)**: Medical imaging technology that provides storage and access to medical images from multiple modalities. PACS systems eliminate the need for film-based images.

- **DICOM (Digital Imaging and Communications in Medicine)**: International standard for handling, storing, printing, and transmitting medical imaging information. DICOM enables interoperability between different imaging devices and systems.

- **Radiology Information System (RIS)**: Information system used by radiology departments to manage patient scheduling, resource management, examination performance tracking, and reporting.

- **LIS (Laboratory Information System)**: Information system used by clinical laboratories to manage laboratory workflows, test ordering, result reporting, and quality control.

### 10.6 Interoperability and Data Exchange Standards

- **FHIR (Fast Healthcare Interoperability Resources)**: Standard for exchanging healthcare information electronically. FHIR uses modern web technologies (RESTful APIs, JSON, XML) to enable interoperability between different healthcare systems.

- **HL7 (Health Level Seven)**: International standards organization that develops standards for exchanging health information. HL7 standards enable interoperability between healthcare information systems.

- **HIE (Health Information Exchange)**: Network that enables the electronic sharing of health information between different healthcare organizations. HIEs facilitate care coordination and improve patient care quality.

- **MPI (Master Patient Index)**: Database that maintains unique identifiers for patients across multiple healthcare systems. MPIs help identify and link patient records from different organizations.

- **Interoperability**: Ability of different information systems, devices, and applications to access, exchange, integrate, and cooperatively use data in a coordinated manner. Interoperability enables seamless information sharing across healthcare organizations.

### 10.7 Clinical Documentation Terms

- **Progress Note**: Clinical documentation created during a patient visit that records the patient's current condition, examination findings, assessment, and treatment plan.

- **Consultation Note**: Clinical documentation created when a provider requests input from another specialist. Consultation notes include the consulting provider's assessment and recommendations.

- **Discharge Summary**: Clinical documentation created when a patient is discharged from a hospital or facility. Discharge summaries include admission diagnosis, procedures performed, discharge diagnosis, medications, and follow-up instructions.

- **Problem List**: List of active and resolved medical conditions, diagnoses, and health problems for a patient. Problem lists help providers track patient conditions over time.

- **Allergy List**: Comprehensive list of a patient's known allergies and adverse reactions to medications, foods, environmental factors, or other substances. Allergy lists are critical for medication safety.

- **Medication List**: List of current and historical medications prescribed to a patient. Medication lists include medication names, dosages, frequencies, start dates, and prescribing providers.

- **Medication Reconciliation**: Process of comparing a patient's current medication list with medications ordered during a new encounter to identify discrepancies, duplications, and interactions.

### 10.8 Clinical Measurement Terms

- **Vital Signs**: Basic measurements of body functions including blood pressure, heart rate, respiratory rate, temperature, and oxygen saturation. Vital signs are essential indicators of a patient's health status.

- **BMI (Body Mass Index)**: Measure of body fat based on height and weight. BMI is calculated as weight in kilograms divided by height in meters squared (kg/m²).

- **Blood Pressure**: Measurement of the force of blood against artery walls, expressed as systolic pressure over diastolic pressure (e.g., 120/80 mmHg).

- **Oxygen Saturation (SpO2)**: Measure of the percentage of hemoglobin molecules in the blood that are carrying oxygen. Normal SpO2 is typically 95-100%.

### 10.9 Data Quality and Security Terms

- **Audit Trail**: Chronological record of system activities that provides evidence of who accessed or modified data, when, and what changes were made. Audit trails are essential for security, compliance, and accountability.

- **Data Encryption**: Process of converting data into a coded form to prevent unauthorized access. Encryption protects data both at rest (stored) and in transit (transmitted).

- **Role-Based Access Control (RBAC)**: Security model that restricts system access based on user roles and permissions. RBAC ensures users only access information appropriate to their role.

- **Minimum Necessary**: HIPAA principle requiring that access to PHI be limited to the minimum amount necessary to accomplish the intended purpose. This principle helps protect patient privacy.

- **Soft Delete**: Data deletion method that marks records as deleted rather than permanently removing them from the database. Soft deletion preserves data for audit and compliance purposes while removing it from active use.

- **Referential Integrity**: Database concept ensuring that relationships between tables remain consistent. Referential integrity prevents orphaned records and maintains data consistency.

### 10.10 Workflow and Process Terms

- **Workflow**: Sequence of steps or processes that define how tasks are completed. Clinical workflows describe how healthcare providers interact with the EHR system to deliver care.

- **Template**: Pre-defined structure or format for clinical documentation that standardizes data entry and improves efficiency. Templates can be customized for different specialties or encounter types.

- **Clinical Decision Support**: Tools and systems that provide healthcare providers with patient-specific information and recommendations to enhance clinical decision-making and improve patient outcomes.

- **Alert**: Notification or warning displayed to users when specific conditions are met. Clinical alerts include drug interaction warnings, allergy alerts, and critical value notifications.

- **Override**: Action taken by a user to proceed despite a system warning or alert. Overrides typically require documentation of the clinical rationale.

### 10.11 Technical Terms

- **API (Application Programming Interface)**: Set of protocols and tools for building software applications. APIs enable different systems to communicate and exchange data.

- **RESTful API**: Web service architecture that uses HTTP methods (GET, POST, PUT, DELETE) to access and manipulate resources. RESTful APIs are commonly used for healthcare data exchange.

- **JSON (JavaScript Object Notation)**: Lightweight data interchange format commonly used for transmitting data between systems. JSON is widely used in FHIR and modern healthcare APIs.

- **XML (eXtensible Markup Language)**: Markup language used for encoding documents in a format that is both human-readable and machine-readable. XML is used in many healthcare data exchange standards.

- **Database Normalization**: Process of organizing data in a database to reduce redundancy and improve data integrity. Normalization typically follows rules such as third normal form (3NF).

- **Foreign Key**: Database constraint that ensures referential integrity by linking records in one table to records in another table. Foreign keys maintain relationships between entities.

- **Index**: Database structure that improves query performance by providing fast access to data. Indexes are created on frequently searched columns.

### 10.12 Quality and Reporting Terms

- **Quality Measures**: Metrics used to assess the quality of healthcare delivery. Quality measures may be required for regulatory compliance, accreditation, or value-based payment programs.

- **Clinical Quality Measures (CQMs)**: Tools that measure and track the quality of healthcare services provided by eligible professionals and eligible hospitals.

- **Meaningful Use**: Federal program (now part of Promoting Interoperability) that required healthcare providers to demonstrate meaningful use of certified EHR technology to improve patient care.

- **MIPS (Merit-based Incentive Payment System)**: Quality payment program that rewards healthcare providers for delivering high-quality, cost-effective care.

- **Adherence**: Extent to which patients follow prescribed medication regimens. Medication adherence is measured by factors such as percentage of days covered and refill patterns.

### 10.13 Additional Terms

- **Provider**: Healthcare professional authorized to provide medical care, including physicians, nurse practitioners, physician assistants, and other licensed clinicians.

- **Prescriber**: Healthcare provider authorized to prescribe medications. Prescribers must have appropriate licenses and DEA numbers (for controlled substances).

- **Pharmacist**: Licensed healthcare professional who dispenses medications and provides medication-related services. Pharmacists review prescriptions for safety and accuracy.

- **Patient Portal**: Secure online website that gives patients access to their personal health information and allows communication with healthcare providers.

- **Telehealth**: Delivery of healthcare services remotely using telecommunications technology. Telehealth includes video consultations, remote monitoring, and virtual visits.

- **Population Health**: Health outcomes of a group of individuals, including the distribution of such outcomes within the group. Population health management involves analyzing and improving health outcomes for defined patient populations.

---

## Document Version History
- **Version 1.0**: Initial requirements document created
- **Date**: [Current Date]
