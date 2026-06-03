import api from './api';

// ========== Type Definitions ==========

export interface Patient {
  patientId: string;
  mrn: string;
  organizationId?: string;
  fullName: string;
  preferredName?: string;
  dateOfBirth: string;
  gender?: 'Male' | 'Female' | 'Other' | 'Prefer_not_to_answer';
  sexAtBirth?: string;
  idNo?: string;
  idType?: string;
  race?: string;
  ethnicity?: string;
  maritalStatus?: string;
  patientType?: string;
  fatherName?: string;
  motherName?: string;
  spouseName?: string;
  bloodGroup?: string;
  religion?: string;
  occupation?: string;
  introducedBy?: string;
  primaryAddressLine1?: string;
  primaryAddressLine2?: string;
  primaryCity?: string;
  primaryState?: string;
  primaryZip?: string;
  primaryCountry?: string;
  mailingAddressLine1?: string;
  mailingAddressLine2?: string;
  mailingCity?: string;
  mailingState?: string;
  mailingZip?: string;
  mailingCountry?: string;
  primaryPhone?: string;
  primaryPhoneType?: string;
  secondaryPhone?: string;
  secondaryPhoneType?: string;
  primaryEmail?: string;
  secondaryEmail?: string;
  preferredContactMethod?: 'Phone' | 'Email' | 'Mail' | 'Text_Message';
  consentTextMessaging?: boolean;
  consentEmailCommunication?: boolean;
  primaryCareProviderId?: string;
  primaryCareLocationId?: string;
  referringPhysicianId?: string;
  patientStatus: 'ACTIVE' | 'INACTIVE' | 'DECEASED' | 'ARCHIVED';
  preferredLanguage?: string;
  interpreterNeeded?: boolean;
  specialNeeds?: string;
  registrationDate: string;
  registeredBy?: string;
  registrationLocationId?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  /** Set on create when Hospital Card Service issues PATIENT_IDENTITY */
  identityCardId?: string;
  identityCardNumber?: string;
  identityCardStatus?: 'ISSUED' | 'SKIPPED' | 'FAILED' | 'DISABLED';
  identityCardMessage?: string;
}

export interface PatientIdentityCardPrintResponse {
  patientId: string;
  mrn: string;
  cardId: string;
  cardNumber: string;
  title: string;
  html: string;
  action: 'PRINT' | 'REPRINT';
  printedBy?: string;
  printedAt?: string;
}

export interface PatientIdentityCardActionResponse {
  patientId: string;
  cardId: string;
  cardNumber: string;
  status: string;
  message?: string;
  action: 'REPLACE';
  reason: string;
  performedBy?: string;
  performedAt?: string;
}

export interface PatientRequest {
  /** Single name as entered; persisted in DB `full_name`. */
  fullName: string;
  preferredName?: string;
  dateOfBirth?: string;
  /** When dateOfBirth is omitted, server derives DOB from age (appointment quick registration). */
  ageYears?: number;
  gender?: 'Male' | 'Female' | 'Other' | 'Prefer_not_to_answer';
  sexAtBirth?: string;
  idNo?: string;
  idType?: string;
  race?: string;
  ethnicity?: string;
  maritalStatus?: string;
  patientType?: string;
  fatherName?: string;
  motherName?: string;
  spouseName?: string;
  bloodGroup?: string;
  religion?: string;
  occupation?: string;
  introducedBy?: string;
  primaryAddressLine1?: string;
  primaryAddressLine2?: string;
  primaryCity?: string;
  primaryState?: string;
  primaryZip?: string;
  primaryCountry?: string;
  mailingAddressLine1?: string;
  mailingAddressLine2?: string;
  mailingCity?: string;
  mailingState?: string;
  mailingZip?: string;
  mailingCountry?: string;
  primaryPhone?: string;
  primaryPhoneType?: string;
  secondaryPhone?: string;
  secondaryPhoneType?: string;
  primaryEmail?: string;
  secondaryEmail?: string;
  preferredContactMethod?: 'Phone' | 'Email' | 'Mail' | 'Text_Message';
  consentTextMessaging?: boolean;
  consentEmailCommunication?: boolean;
  preferredLanguage?: string;
  interpreterNeeded?: boolean;
  specialNeeds?: string;
  organizationId?: string;
  /** When checking duplicates during edit, exclude the current patient. */
  excludePatientId?: string;
}

export interface DuplicatePatientResponse {
  hasDuplicates: boolean;
  matches: DuplicateMatch[];
  matchReason?: string;
  /** Legacy flag; phone duplicates are informational only (families may share a mobile). */
  phoneDuplicateBlocked?: boolean;
}

export interface DuplicateMatch {
  patientId: string;
  mrn: string;
  fullName?: string;
  idNo?: string;
  dateOfBirth: string;
  matchReason: string;
  matchScore?: number;
}

export interface EmergencyContact {
  contactId: string;
  patientId: string;
  contactName: string;
  relationship: 'Spouse' | 'Parent' | 'Child' | 'Sibling' | 'Friend' | 'Other';
  primaryPhone: string;
  secondaryPhone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zip?: string;
  country?: string;
  email?: string;
  isPrimary: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EmergencyContactRequest {
  contactName: string;
  relationship: 'Spouse' | 'Parent' | 'Child' | 'Sibling' | 'Friend' | 'Other';
  primaryPhone: string;
  secondaryPhone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zip?: string;
  country?: string;
  email?: string;
  isPrimary?: boolean;
}

export interface Insurance {
  insuranceId: string;
  patientId: string;
  insuranceType: 'PRIMARY' | 'SECONDARY' | 'TERTIARY';
  insuranceCompanyName?: string;
  policyNumber?: string;
  groupNumber?: string;
  subscriberName?: string;
  subscriberDob?: string;
  subscriberRelationship?: 'Self' | 'Spouse' | 'Child' | 'Other';
  effectiveDate?: string;
  expirationDate?: string;
  copayAmount?: number;
  verificationStatus:
    | 'Verified'
    | 'Pending'
    | 'Not_Verified'
    | 'Not_Applicable'
    | 'Not Verified'
    | 'Not Applicable';
  verifiedDate?: string;
  insurancePhone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InsuranceRequest {
  insuranceType: 'PRIMARY' | 'SECONDARY' | 'TERTIARY';
  insuranceCompanyName?: string;
  policyNumber?: string;
  groupNumber?: string;
  subscriberName?: string;
  subscriberDob?: string;
  subscriberRelationship?: 'Self' | 'Spouse' | 'Child' | 'Other';
  effectiveDate?: string;
  expirationDate?: string;
  copayAmount?: number;
  verificationStatus?: 'Verified' | 'Pending' | 'Not_Verified' | 'Not_Applicable' | 'Not Verified' | 'Not Applicable';
  insurancePhone?: string;
}

export interface Consent {
  consentId: string;
  patientId: string;
  consentType: 'HIPAA' | 'FINANCIAL' | 'MARKETING' | 'TREATMENT';
  consentStatus: 'GRANTED' | 'DENIED' | 'REVOKED';
  consentDate: string;
  signature?: string;
  expiresDate?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConsentRequest {
  consentType: 'HIPAA' | 'FINANCIAL' | 'MARKETING' | 'TREATMENT';
  consentStatus: 'GRANTED' | 'DENIED' | 'REVOKED';
  consentDate: string;
  signature?: string;
  expiresDate?: string;
  notes?: string;
}

// ========== Phase EHR.2: Medical History & Allergies ==========

export interface MedicalHistory {
  historyId: string;
  patientId: string;
  historyType: 'PAST_MEDICAL' | 'FAMILY' | 'SOCIAL' | 'IMMUNIZATION';
  conditionName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  onsetDate?: string;
  resolutionDate?: string;
  status: 'ACTIVE' | 'RESOLVED' | 'CHRONIC' | 'INACTIVE';
  severity?: string;
  notes?: string;
  documentedBy?: string;
  documentedDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MedicalHistoryRequest {
  historyType: 'PAST_MEDICAL' | 'FAMILY' | 'SOCIAL' | 'IMMUNIZATION';
  conditionName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  onsetDate?: string;
  resolutionDate?: string;
  status?: 'ACTIVE' | 'RESOLVED' | 'CHRONIC' | 'INACTIVE';
  severity?: string;
  notes?: string;
  documentedDate?: string;
}

export interface FamilyHistory {
  familyHistoryId: string;
  patientId: string;
  familyMemberRelationship: 'Mother' | 'Father' | 'Sister' | 'Brother' | 
    'Maternal_Grandmother' | 'Maternal_Grandfather' | 
    'Paternal_Grandmother' | 'Paternal_Grandfather' | 
    'Aunt' | 'Uncle' | 'Cousin' | 'Other';
  conditionName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  ageAtOnset?: number;
  ageAtDeath?: number;
  notes?: string;
  documentedDate?: string;
  documentedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FamilyHistoryRequest {
  familyMemberRelationship: 'Mother' | 'Father' | 'Sister' | 'Brother' | 
    'Maternal_Grandmother' | 'Maternal_Grandfather' | 
    'Paternal_Grandmother' | 'Paternal_Grandfather' | 
    'Aunt' | 'Uncle' | 'Cousin' | 'Other';
  conditionName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  ageAtOnset?: number;
  ageAtDeath?: number;
  notes?: string;
  documentedDate?: string;
}

export interface SocialHistory {
  socialHistoryId: string;
  patientId: string;
  category: 'SMOKING' | 'ALCOHOL' | 'DRUGS' | 'OCCUPATION' | 'LIFESTYLE' | 'EXERCISE' | 'DIET' | 'OTHER';
  status: 'CURRENT' | 'PAST' | 'NEVER';
  frequency?: string;
  quantity?: string;
  durationYears?: number;
  startDate?: string;
  endDate?: string;
  notes?: string;
  documentedDate?: string;
  documentedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SocialHistoryRequest {
  category: 'SMOKING' | 'ALCOHOL' | 'DRUGS' | 'OCCUPATION' | 'LIFESTYLE' | 'EXERCISE' | 'DIET' | 'OTHER';
  status: 'CURRENT' | 'PAST' | 'NEVER';
  frequency?: string;
  quantity?: string;
  durationYears?: number;
  startDate?: string;
  endDate?: string;
  notes?: string;
  documentedDate?: string;
}

export interface Immunization {
  immunizationId: string;
  patientId: string;
  vaccineName: string;
  cvxCode?: string;
  administrationDate: string;
  lotNumber?: string;
  manufacturer?: string;
  route?: 'IM' | 'SC' | 'ID' | 'IN' | 'PO' | 'IV' | 'NASAL' | 'OPHTHALMIC' | 'OTIC' | 'OTHER';
  site?: string;
  dose?: string;
  administeredBy?: string;
  administeredLocationId?: string;
  reaction?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ImmunizationRequest {
  vaccineName: string;
  cvxCode?: string;
  administrationDate: string;
  lotNumber?: string;
  manufacturer?: string;
  route?: 'IM' | 'SC' | 'ID' | 'IN' | 'PO' | 'IV' | 'NASAL' | 'OPHTHALMIC' | 'OTIC' | 'OTHER';
  site?: string;
  dose?: string;
  administeredBy?: string;
  administeredLocationId?: string;
  reaction?: string;
  notes?: string;
}

export interface Allergy {
  allergyId: string;
  patientId: string;
  allergenName: string;
  allergenType: 'DRUG' | 'FOOD' | 'ENVIRONMENTAL' | 'LATEX' | 'OTHER';
  allergenCode?: string;
  reactionType?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE' | 'LIFE_THREATENING';
  onsetDate?: string;
  status: 'ACTIVE' | 'RESOLVED' | 'UNKNOWN';
  verificationStatus: 'CONFIRMED' | 'UNCONFIRMED' | 'REFUTED';
  documentedBy?: string;
  documentedDate?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AllergyRequest {
  allergenName: string;
  allergenType: 'DRUG' | 'FOOD' | 'ENVIRONMENTAL' | 'LATEX' | 'OTHER';
  allergenCode?: string;
  reactionType?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE' | 'LIFE_THREATENING';
  onsetDate?: string;
  status?: 'ACTIVE' | 'RESOLVED' | 'UNKNOWN';
  verificationStatus?: 'CONFIRMED' | 'UNCONFIRMED' | 'REFUTED';
  notes?: string;
  documentedDate?: string;
}

export interface PharmacyDrugSuggestion {
  id: string;
  genericName: string;
  brandName?: string;
  strength?: string;
  form?: string;
  route?: string;
}

// ── FR-P3.5: Pharmacy Directory ───────────────────────────────────────────────

/** A pharmacy master-data record from the hospital_pharmacy.pharmacy_directory table. */
export interface PharmacyDirectoryEntry {
  id: string;
  name: string;
  npi?: string;
  ncpdpId?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zip?: string;
  country?: string;
  phone?: string;
  fax?: string;
  email?: string;
  isEprescribingCapable: boolean;
  eprescribingNetwork?: string;
  dataSource: 'MANUAL' | 'NCPDP_FEED' | 'SURESCRIPTS' | 'IMPORTED';
  lastVerifiedAt?: string;
  verificationNotes?: string;
  isActive: boolean;
  /** TRUE when lastVerifiedAt is null or older than 90 days (computed by backend). */
  isStale: boolean;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

/** Payload for creating or updating a pharmacy directory entry (admin). */
export interface PharmacyDirectoryRequest {
  name: string;
  npi?: string;
  ncpdpId?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zip?: string;
  country?: string;
  phone?: string;
  fax?: string;
  email?: string;
  isEprescribingCapable?: boolean;
  eprescribingNetwork?: string;
  dataSource?: 'MANUAL' | 'NCPDP_FEED' | 'SURESCRIPTS' | 'IMPORTED';
  verificationNotes?: string;
  notes?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ========== Phase EHR.3: Vital Signs & Clinical Measurements ==========

export interface VitalSigns {
  vitalSignId: string;
  patientId: string;
  encounterId?: string;
  measurementDate: string;
  measurementTime: string;
  systolicBp?: number;
  diastolicBp?: number;
  heartRate?: number;
  respiratoryRate?: number;
  temperature?: number;
  temperatureUnit?: 'C' | 'F';
  oxygenSaturation?: number;
  weight?: number;
  weightUnit?: 'kg' | 'lbs' | 'g' | 'oz';
  height?: number;
  heightUnit?: 'cm' | 'in' | 'm' | 'ft';
  bmi?: number;
  painScale?: number;
  bloodGlucose?: number;
  headCircumference?: number;
  measuredBy?: string;
  measurementLocationId?: string;
  deviceUsed?: string;
  patientPosition?: string;
  notes?: string;
  isAbnormal?: boolean;
  isCritical?: boolean;
  abnormalReason?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface VitalSignsRequest {
  measurementDate: string;
  measurementTime: string;
  encounterId?: string;
  systolicBp?: number;
  diastolicBp?: number;
  heartRate?: number;
  respiratoryRate?: number;
  temperature?: number;
  temperatureUnit?: 'C' | 'F';
  oxygenSaturation?: number;
  weight?: number;
  weightUnit?: 'kg' | 'lbs' | 'g' | 'oz';
  height?: number;
  heightUnit?: 'cm' | 'in' | 'm' | 'ft';
  painScale?: number;
  bloodGlucose?: number;
  headCircumference?: number;
  measuredBy?: string;
  measurementLocationId?: string;
  deviceUsed?: string;
  patientPosition?: string;
  notes?: string;
}

export interface VitalSignsTrend {
  measurementDate: string;
  avgSystolicBp?: number;
  avgDiastolicBp?: number;
  avgHeartRate?: number;
  avgRespiratoryRate?: number;
  avgTemperature?: number;
  avgOxygenSaturation?: number;
  avgWeight?: number;
  avgBmi?: number;
  measurementCount: number;
}

export interface VitalSignsSummary {
  latest?: VitalSigns;
  recent: VitalSigns[];
  trends: VitalSignsTrend[];
  totalMeasurements: number;
  abnormalCount: number;
  criticalCount: number;
}

// ========== Encounter/Visit Management ==========

export interface Encounter {
  encounterId: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  organizationId: string;
  encounterNumber: string;
  encounterType: 'OFFICE_VISIT' | 'HOSPITAL_ADMISSION' | 'EMERGENCY' | 'OUTPATIENT' | 'INPATIENT' | 'OBSERVATION' | 'SURGERY' | 'CONSULTATION' | 'TELEHEALTH' | 'HOME_VISIT' | 'URGENT_CARE' | 'AMBULATORY' | 'OTHER';
  status: 'PLANNED' | 'ARRIVED' | 'IN_PROGRESS' | 'COMPLETED' | 'DISCHARGED' | 'CANCELLED' | 'NO_SHOW' | 'LEFT_WITHOUT_BEING_SEEN' | 'ADMITTED' | 'TRANSFERRED';
  startDate: string;
  startTime: string;
  endDate?: string;
  endTime?: string;
  admissionDate?: string;
  admissionTime?: string;
  dischargeDate?: string;
  dischargeTime?: string;
  locationId?: string;
  departmentId?: string;
  roomNumber?: string;
  bedNumber?: string;
  attendingPhysicianId?: string;
  attendingPhysicianName?: string;
  admittingPhysicianId?: string;
  admittingPhysicianName?: string;
  primaryCareProviderId?: string;
  referringPhysicianId?: string;
  chiefComplaint?: string;
  admissionDiagnosis?: string;
  primaryDiagnosis?: string;
  secondaryDiagnoses?: string[];
  dischargeDiagnosis?: string;
  dischargeDisposition?: string;
  dischargeInstructions?: string;
  visitReason?: string;
  visitType?: string;
  serviceType?: string;
  insuranceProviderId?: string;
  insurancePolicyNumber?: string;
  authorizationNumber?: string;
  billingStatus?: string;
  notes?: string;
  specialInstructions?: string;
  isEmergency?: boolean;
  isReadmission?: boolean;
  readmissionReason?: string;
  lengthOfStayDays?: number;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface EncounterRequest {
  patientId: string;
  encounterType: 'OFFICE_VISIT' | 'HOSPITAL_ADMISSION' | 'EMERGENCY' | 'OUTPATIENT' | 'INPATIENT' | 'OBSERVATION' | 'SURGERY' | 'CONSULTATION' | 'TELEHEALTH' | 'HOME_VISIT' | 'URGENT_CARE' | 'AMBULATORY' | 'OTHER';
  startDate: string;
  startTime: string;
  endDate?: string;
  endTime?: string;
  admissionDate?: string;
  admissionTime?: string;
  dischargeDate?: string;
  dischargeTime?: string;
  status?: 'PLANNED' | 'ARRIVED' | 'IN_PROGRESS' | 'COMPLETED' | 'DISCHARGED' | 'CANCELLED' | 'NO_SHOW' | 'LEFT_WITHOUT_BEING_SEEN' | 'ADMITTED' | 'TRANSFERRED';
  locationId?: string;
  departmentId?: string;
  roomNumber?: string;
  bedNumber?: string;
  attendingPhysicianId?: string;
  admittingPhysicianId?: string;
  primaryCareProviderId?: string;
  referringPhysicianId?: string;
  chiefComplaint?: string;
  admissionDiagnosis?: string;
  primaryDiagnosis?: string;
  secondaryDiagnoses?: string[];
  dischargeDiagnosis?: string;
  dischargeDisposition?: string;
  dischargeInstructions?: string;
  visitReason?: string;
  visitType?: string;
  serviceType?: string;
  insuranceProviderId?: string;
  insurancePolicyNumber?: string;
  authorizationNumber?: string;
  billingStatus?: string;
  notes?: string;
  specialInstructions?: string;
  isEmergency?: boolean;
  isReadmission?: boolean;
  readmissionReason?: string;
  encounterNumber?: string;
}

// ========== Phase EHR.4: Clinical Notes & Documentation ==========

export interface DoctorHospitalNote {
  noteId: string;
  doctorId: string;
  doctorName: string;
  message: string;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
  updatedBy?: string;
  canModify?: boolean;
}

export interface DoctorHospitalNoteRequest {
  doctorId: string;
  message: string;
}

export interface DoctorHospitalNoteUpdateRequest {
  message: string;
}

export interface ClinicalNote {
  noteId: string;
  patientId: string;
  encounterId?: string;
  noteType: 'SOAP' | 'PROGRESS' | 'CONSULTATION' | 'DISCHARGE' | 'PROCEDURE' | 'ADMISSION' | 'OPERATIVE' | 'DOCTOR_NOTE' | 'OTHER';
  noteDate: string;
  noteTime: string;
  subjective?: string;
  objective?: string;
  assessment?: string;
  plan?: string;
  chiefComplaint?: string;
  reviewOfSystems?: string;
  physicalExamination?: string;
  clinicalImpression?: string;
  treatmentPlan?: string;
  followUpInstructions?: string;
  noteStatus: 'DRAFT' | 'FINAL' | 'AMENDED' | 'CORRECTED' | 'VOIDED' | 'SIGNED';
  createdBy?: string;
  createdDate?: string;
  signedBy?: string;
  signedDate?: string;
  signatureMethod?: 'ELECTRONIC' | 'DIGITAL' | 'TYPED' | 'VOICE' | 'OTHER';
  amendedBy?: string;
  amendedDate?: string;
  amendmentReason?: string;
  originalNoteId?: string;
  versionNumber?: number;
  isCurrentVersion?: boolean;
  specialty?: string;
  departmentId?: string;
  locationId?: string;
  visitType?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  updatedBy?: string;
  attachments?: NoteAttachment[];
  amendmentCount?: number;
  medications?: ClinicalNoteMedication[];
  medicationCount?: number;
}

export interface ClinicalNoteRequest {
  patientId: string;
  encounterId?: string;
  noteType: 'SOAP' | 'PROGRESS' | 'CONSULTATION' | 'DISCHARGE' | 'PROCEDURE' | 'ADMISSION' | 'OPERATIVE' | 'DOCTOR_NOTE' | 'OTHER';
  noteDate: string;
  noteTime: string;
  subjective?: string;
  objective?: string;
  assessment?: string;
  plan?: string;
  chiefComplaint?: string;
  reviewOfSystems?: string;
  physicalExamination?: string;
  clinicalImpression?: string;
  treatmentPlan?: string;
  followUpInstructions?: string;
  noteStatus?: 'DRAFT' | 'FINAL' | 'AMENDED' | 'CORRECTED' | 'VOIDED' | 'SIGNED';
  specialty?: string;
  departmentId?: string;
  locationId?: string;
  visitType?: string;
  notes?: string;
  templateId?: string;
}

export interface NoteSignRequest {
  signatureMethod: 'ELECTRONIC' | 'DIGITAL' | 'TYPED' | 'VOICE' | 'OTHER';
  notes?: string;
}

export interface NoteAmendmentRequest {
  amendmentReason: string;
  subjective?: string;
  objective?: string;
  assessment?: string;
  plan?: string;
  chiefComplaint?: string;
  reviewOfSystems?: string;
  physicalExamination?: string;
  clinicalImpression?: string;
  treatmentPlan?: string;
  followUpInstructions?: string;
  notes?: string;
}

export interface NoteAttachment {
  attachmentId: string;
  noteId: string;
  fileName: string;
  fileType?: string;
  fileSize?: number;
  filePath: string;
  fileHash?: string;
  mimeType?: string;
  description?: string;
  attachmentType?: 'IMAGE' | 'DOCUMENT' | 'LAB_RESULT' | 'IMAGING' | 'AUDIO' | 'VIDEO' | 'OTHER';
  uploadedDate?: string;
  uploadedBy?: string;
  isActive?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface NoteAttachmentRequest {
  fileName: string;
  fileType?: string;
  fileSize?: number;
  filePath: string;
  fileHash?: string;
  mimeType?: string;
  description?: string;
  attachmentType?: 'IMAGE' | 'DOCUMENT' | 'LAB_RESULT' | 'IMAGING' | 'AUDIO' | 'VIDEO' | 'OTHER';
}

export type PatientDocumentType =
  | 'PATHOLOGY_REPORT'
  | 'RADIOLOGY_REPORT'
  | 'LAB_REPORT'
  | 'CLINICAL_PHOTO'
  | 'SURGICAL_REPORT'
  | 'PRESCRIPTION'
  | 'REFERRAL_LETTER'
  | 'DISCHARGE_SUMMARY'
  | 'CONSENT_FORM'
  | 'INSURANCE_DOCUMENT'
  | 'VITAL_RECORDS'
  | 'IDENTITY_DOCUMENT'
  | 'EXTERNAL_RECORD'
  | 'ADVANCE_DIRECTIVE'
  | 'OTHER';

export interface PatientDocument {
  documentId: string;
  patientId: string;
  organizationId?: string;
  encounterId?: string;
  clinicalNoteId?: string;
  labResultId?: string;
  prescriptionId?: string;
  documentType: PatientDocumentType;
  documentCategory?: string;
  title: string;
  description?: string;
  fileName: string;
  originalFileName?: string;
  fileUrl?: string;
  fileSize?: number;
  mimeType?: string;
  sourceFacility?: string;
  documentDate?: string;
  uploadedBy?: string;
  uploadedDate?: string;
  isActive?: boolean;
  isConfidential?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface NoteTemplate {
  templateId: string;
  templateName: string;
  templateType: 'SOAP' | 'PROGRESS' | 'CONSULTATION' | 'DISCHARGE' | 'PROCEDURE' | 'ADMISSION' | 'OPERATIVE' | 'OTHER';
  specialty?: string;
  departmentId?: string;
  templateContent?: string; // JSON string
  description?: string;
  isSystemTemplate?: boolean;
  isActive?: boolean;
  isPublic?: boolean;
  createdBy?: string;
  createdDate?: string;
  usageCount?: number;
  lastUsedDate?: string;
  createdAt: string;
  updatedAt: string;
  updatedBy?: string;
}

export interface NoteTemplateRequest {
  templateName: string;
  templateType: 'SOAP' | 'PROGRESS' | 'CONSULTATION' | 'DISCHARGE' | 'PROCEDURE' | 'ADMISSION' | 'OPERATIVE' | 'OTHER';
  specialty?: string;
  departmentId?: string;
  templateContent?: string; // JSON string
  description?: string;
  isSystemTemplate?: boolean;
  isActive?: boolean;
  isPublic?: boolean;
}

// ========== Phase EHR.5: Diagnoses & Problem Lists ==========

export interface CodeSuggestion {
  code: string;
  description: string;
  codeType: 'ICD10' | 'ICD11' | 'SNOMED';
}

export interface MedicalCode {
  code: string;
  description: string;
  category?: string;
  chapter?: string;
  isValid: boolean;
  codeType: 'ICD10' | 'ICD11';
}

export interface MedicalCodePage {
  items: MedicalCode[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MedicalCodeUpsertRequest {
  code: string;
  description: string;
  category?: string;
  chapter?: string;
  isValid?: boolean;
}

/** Legacy clinical chart row (hospital.clinical_chart_items). */
export interface ClinicalChartItemRow {
  clinicalChartItemId: string;
  legacyRowId: number;
  pcode?: string;
  description: string;
  charge?: number;
  deptName?: string;
  subDeptName?: string;
  subSubDeptName?: string;
  reportGroupName?: string;
  outTest?: number;
  statusLegacy?: number;
}

export interface ClinicalChartItemPage {
  items: ClinicalChartItemRow[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ClinicalChartItemRequest {
  legacyRowId?: number;
  pcode?: string;
  description: string;
  charge?: number;
  deptName?: string;
  subDeptName?: string;
  subSubDeptName?: string;
  reportGroupName?: string;
  outTest?: number;
  statusLegacy?: number;
}

export interface PatientProblem {
  problemId: string;
  patientId: string;
  encounterId?: string;
  problemName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  problemType: 'DIAGNOSIS' | 'SYMPTOM' | 'FINDING' | 'CONDITION' | 'ALLERGY' | 'OTHER';
  status: 'ACTIVE' | 'RESOLVED' | 'INACTIVE' | 'RULED_OUT' | 'CHRONIC' | 'REMISSION';
  onsetDate?: string;
  resolutionDate?: string;
  severity?: 'MILD' | 'MODERATE' | 'SEVERE' | 'CRITICAL';
  chronicity?: string;
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  documentedBy?: string;
  documentedDate?: string;
  resolvedBy?: string;
  resolvedDate?: string;
  resolutionNotes?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  history?: ProblemHistory[];
  historyCount?: number;
}

export interface PatientProblemRequest {
  patientId: string;
  encounterId?: string;
  problemName: string;
  icd10Code?: string;
  icd11Code?: string;
  snomedCode?: string;
  problemType: 'DIAGNOSIS' | 'SYMPTOM' | 'FINDING' | 'CONDITION' | 'ALLERGY' | 'OTHER';
  status?: 'ACTIVE' | 'RESOLVED' | 'INACTIVE' | 'RULED_OUT' | 'CHRONIC' | 'REMISSION';
  onsetDate?: string;
  resolutionDate?: string;
  severity?: 'MILD' | 'MODERATE' | 'SEVERE' | 'CRITICAL';
  chronicity?: string;
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  resolutionNotes?: string;
  notes?: string;
}

export interface ProblemResolutionRequest {
  resolutionDate: string;
  resolutionNotes?: string;
}

export interface ProblemHistory {
  historyId: string;
  problemId: string;
  changeType: 'CREATED' | 'UPDATED' | 'RESOLVED' | 'REACTIVATED' | 'STATUS_CHANGED' | 'CODE_UPDATED' | 'OTHER';
  changedBy?: string;
  changedDate: string;
  previousValue?: string;
  newValue?: string;
  changeReason?: string;
  fieldName?: string;
  notes?: string;
  createdAt: string;
}

export interface ProblemListSummary {
  totalProblems: number;
  activeProblems: number;
  resolvedProblems: number;
  chronicProblems: number;
  activeProblemsList: PatientProblem[];
  resolvedProblemsList: PatientProblem[];
  highPriorityProblems: PatientProblem[];
}

// ========== Phase EHR.6: Prescription Creation & Management ==========

/** One medicine line on a prescription */
export interface PrescriptionMedicationItem {
  prescriptionMedicationId?: string;
  lineNumber?: number;
  medicationName: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  route: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  instructions: string;
  startDate: string;
  endDate?: string;
  durationDays?: number;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  substitutionAllowed?: boolean;
  dawCode?: string;
  isControlledSubstance?: boolean;
  schedule?: 'II' | 'III' | 'IV' | 'V';
  deaNumber?: string;
  /** UI-only stable key for list rows (not sent to API). */
  clientRowId?: string;
}

/** FR-P1.4a: one ICD-10 diagnosis entry in a prescription response. */
export interface PrescriptionDiagnosis {
  id: string;
  diagnosisCode: string;
  diagnosisDescription?: string;
  isPrimary: boolean;
  sequenceOrder: number;
  createdAt?: string;
}

/** FR-P1.4a: one ICD-10 diagnosis entry in a prescription request. */
export interface PrescriptionDiagnosisRequest {
  diagnosisCode: string;
  diagnosisDescription?: string;
  /** Mark this as the principal diagnosis. First entry is treated as primary when omitted. */
  isPrimary?: boolean;
}

export interface Prescription {
  prescriptionId: string;
  patientId: string;
  encounterId?: string;
  /** EP-1 / EP-11: session context when authored */
  epEncounterMode?: 'OPD' | 'IPD';
  prescriptionNumber?: string;
  prescriptionType?: 'ELECTRONIC' | 'PAPER' | 'PHONE' | 'FAX' | 'OTHER';
  /** Line items when returned from API */
  medications?: PrescriptionMedicationItem[];
  /** Summary / first-line mirror for lists and legacy */
  medicationName: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  route: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  instructions: string;
  startDate: string;
  endDate?: string;
  durationDays?: number;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  substitutionAllowed?: boolean;
  dawCode?: string;
  isControlledSubstance?: boolean;
  schedule?: 'II' | 'III' | 'IV' | 'V';
  deaNumber?: string;
  pdmpQueried?: boolean;
  pdmpQueryDate?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  pharmacyAddressLine1?: string;
  pharmacyAddressLine2?: string;
  pharmacyCity?: string;
  pharmacyState?: string;
  pharmacyZip?: string;
  pharmacyPhone?: string;
  prescribingProviderId: string;
  prescribingProviderNpi?: string;
  prescribingProviderName?: string;
  prescriptionStatus: 'DRAFT' | 'PENDING' | 'SENT' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'EXPIRED' | 'REJECTED' | 'ON_HOLD';
  createdDate?: string;
  sentDate?: string;
  filledDate?: string;
  cancellationDate?: string;
  expirationDate?: string;
  cancellationReason?: string;
  cancelledBy?: string;
  notes?: string;
  specialInstructions?: string;
  /** Legacy: primary diagnosis code; kept for backward-compatible display. */
  diagnosisCode?: string;
  /** FR-P1.4a: all diagnoses for this prescription, ordered by sequenceOrder. */
  diagnoses?: PrescriptionDiagnosis[];
  hasInteractions?: boolean;
  hasAllergyWarnings?: boolean;
  validationStatus?: 'VALID' | 'WARNINGS' | 'ERRORS';
  validationNotes?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  interactions?: PrescriptionInteraction[];
  allergyChecks?: PrescriptionAllergyCheck[];
  interactionCount?: number;
  allergyCheckCount?: number;
}

export interface PrescriptionRequest {
  patientId: string;
  encounterId?: string;
  epEncounterMode?: 'OPD' | 'IPD';
  prescriptionNumber?: string;
  prescriptionType?: 'ELECTRONIC' | 'PAPER' | 'PHONE' | 'FAX' | 'OTHER';
  /** Preferred: multiple medicines in one prescription */
  medications?: PrescriptionMedicationItem[];
  /** Legacy single-medication body (omit when using medications) */
  medicationName?: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm?: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  route?: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  instructions?: string;
  startDate?: string;
  endDate?: string;
  durationDays?: number;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  substitutionAllowed?: boolean;
  dawCode?: string;
  isControlledSubstance?: boolean;
  schedule?: 'II' | 'III' | 'IV' | 'V';
  deaNumber?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  pharmacyAddressLine1?: string;
  pharmacyAddressLine2?: string;
  pharmacyCity?: string;
  pharmacyState?: string;
  pharmacyZip?: string;
  pharmacyPhone?: string;
  prescribingProviderId: string;
  prescribingProviderNpi?: string;
  prescribingProviderName?: string;
  notes?: string;
  specialInstructions?: string;
  /**
   * Legacy: single primary ICD-10 code. Used when `diagnoses` is absent.
   * Prefer `diagnoses` for new callers (FR-P1.4a).
   */
  diagnosisCode?: string;
  /** FR-P1.4a: one or more ICD-10 diagnoses. When provided, takes precedence over diagnosisCode. */
  diagnoses?: PrescriptionDiagnosisRequest[];
}

export interface PrescriptionInteraction {
  interactionId: string;
  prescriptionId: string;
  interactingMedication: string;
  interactingMedicationCode?: string;
  interactionType?: string;
  interactionCategory?: string;
  severity: 'CONTRAINDICATED' | 'MAJOR' | 'MODERATE' | 'MINOR' | 'UNKNOWN';
  clinicalSignificanceLevel?: string;
  description?: string;
  clinicalSignificance?: string;
  actionRequired?: string;
  managementGuidance?: string;
  mechanism?: string;
  onsetTime?: string;
  evidenceLevel?: string;
  isAcknowledged?: boolean;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  overrideReason?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface PrescriptionAllergyCheck {
  checkId: string;
  prescriptionId: string;
  allergenName: string;
  allergenCode?: string;
  allergenType?: string;
  reactionType?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE' | 'LIFE_THREATENING';
  actionTaken?: 'OVERRIDDEN' | 'CANCELLED' | 'SUBSTITUTED' | 'MONITORED' | 'NO_ACTION';
  overrideReason?: string;
  overrideBy?: string;
  overrideDate?: string;
  isAcknowledged?: boolean;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  createdAt: string;
  updatedAt?: string;
}

export type PregnancyStatus = 'UNKNOWN' | 'NOT_PREGNANT' | 'POSSIBLE' | 'PREGNANT';

export interface DrugInteractionCheckRequest {
  medicationCode?: string;
  medicationName?: string;
  existingMedicationCodes?: string[];
  /** FR-P1.7 optional overrides for screening */
  weightKg?: number;
  doseStrengthMg?: number;
  doseUnit?: string;
  serumCreatinineMgDl?: number;
  egfrMlMin?: number;
  pregnancyStatus?: PregnancyStatus;
  lactating?: boolean;
}

export interface DrugInteractionCheckResponse {
  hasInteractions: boolean;
  interactions: InteractionDetail[];
  summary?: string;
}

export interface InteractionDetail {
  interactingMedication: string;
  interactingMedicationCode?: string;
  interactionType?: string;
  interactionCategory?: string;
  severity: 'CONTRAINDICATED' | 'MAJOR' | 'MODERATE' | 'MINOR' | 'UNKNOWN';
  clinicalSignificanceLevel?: string;
  description?: string;
  clinicalSignificance?: string;
  actionRequired?: string;
  managementGuidance?: string;
  mechanism?: string;
  onsetTime?: string;
  evidenceLevel?: string;
}

export interface AllergyCheckRequest {
  medicationCode?: string;
  medicationName?: string;
  patientId: string;
}

export interface AllergyCheckResponse {
  hasAllergies: boolean;
  allergies: AllergyDetail[];
  summary?: string;
}

export interface AllergyDetail {
  allergenName: string;
  allergenType?: string;
  reactionType?: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE' | 'LIFE_THREATENING';
}

export interface PrescriptionTransmitRequest {
  overrideInteractions?: boolean;
  overrideAllergies?: boolean;
  overrideReason?: string;
  overridePdmpCheck?: boolean;
  pharmacyId?: string;
  pharmacyNpi?: string;
  pharmacyName?: string;
}

// ========== PDMP (Prescription Drug Monitoring Program) ==========

export interface PDMPQueryRequest {
  patientId: string;
  queryState: string;
  queryType?: string;
  queryReason?: string;
  deaNumber?: string;
  patientFirstName?: string;
  patientLastName?: string;
  patientDateOfBirth?: string;
  patientIdNo?: string;
  patientState?: string;
  dateRangeStart?: string;
  dateRangeEnd?: string;
}

export interface PDMPPrescriptionHistoryItem {
  prescriptionId?: string;
  medicationName?: string;
  dosageStrength?: string;
  dosageUnit?: string;
  quantity?: string;
  schedule?: string;
  prescribedDate?: string;
  filledDate?: string;
  prescriberName?: string;
  prescriberNpi?: string;
  pharmacyName?: string;
  pharmacyAddress?: string;
  daysSupply?: number;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  status?: string;
}

export interface PDMPPharmacyInfo {
  pharmacyName?: string;
  pharmacyNpi?: string;
  city?: string;
  state?: string;
  phone?: string;
  prescriptionCount?: number;
  lastPrescriptionDate?: string;
}

export interface PDMPPrescriberInfo {
  prescriberName?: string;
  prescriberNpi?: string;
  deaNumber?: string;
  specialty?: string;
  prescriptionCount?: number;
}

export interface PDMPQueryResponse {
  queryResultId?: string;
  prescriptionId?: string;
  patientId?: string;
  queryDate?: string;
  queryState?: string;
  queryType?: string;
  queryingProviderName?: string;
  queryingProviderNpi?: string;
  queryStatus?: string;
  querySuccess?: boolean;
  errorMessage?: string;
  totalPrescriptions?: number;
  totalPharmacies?: number;
  totalPrescribers?: number;
  dateRangeStart?: string;
  dateRangeEnd?: string;
  hasControlledSubstances?: boolean;
  riskScore?: number;
  riskLevel?: string;
  prescriptionHistory?: PDMPPrescriptionHistoryItem[];
  pharmacyList?: PDMPPharmacyInfo[];
  prescriberList?: PDMPPrescriberInfo[];
  hasDuplicatePrescriptions?: boolean;
  hasOverlappingPrescriptions?: boolean;
  hasEarlyRefills?: boolean;
  hasMultiplePrescribers?: boolean;
  hasMultiplePharmacies?: boolean;
  warnings?: string;
  queryReason?: string;
  clinicalNotes?: string;
  actionTaken?: string;
  pdmpSystemName?: string;
  createdAt?: string;
}

// ========== Formulary ==========

export interface FormularyCheckRequest {
  patientId: string;
  prescriptionId: string;
  insuranceId?: string;
  medicationCode?: string;
  medicationName?: string;
  includeAlternatives?: boolean;
  estimateCosts?: boolean;
}

export interface FormularyAlternative {
  alternativeId?: string;
  medicationCode?: string;
  medicationName?: string;
  genericName?: string;
  formularyTier?: string;
  coverageStatus?: string;
  requiresPriorAuthorization?: boolean;
  copayAmount?: number;
  patientCostEstimate?: number;
  alternativeType?: string;
  reason?: string;
  isPreferred?: boolean;
  rank?: number;
}

export interface FormularyCheckResponse {
  formularyCheckId?: string;
  prescriptionId?: string;
  insuranceCompanyName?: string;
  policyNumber?: string;
  medicationName?: string;
  coverageStatus?: string;
  formularyTier?: string;
  requiresPriorAuthorization?: boolean;
  priorAuthorizationRequired?: boolean;
  stepTherapyRequired?: boolean;
  quantityLimit?: number;
  daysSupplyLimit?: number;
  copayAmount?: number;
  coinsurancePercentage?: number;
  deductibleApplies?: boolean;
  patientCostEstimate?: number;
  insurancePays?: number;
  pbmName?: string;
  formularyName?: string;
  checkDate?: string;
  checkStatus?: string;
  errorMessage?: string;
  alternatives?: FormularyAlternative[];
}

// ========== Prior Authorization ==========

export interface PriorAuthorizationRequest {
  /** Required by hospital-service validation (@NotNull); usually matches path `{prescriptionId}`. */
  prescriptionId: string;
  formularyCheckId?: string;
  insuranceId?: string;
  clinicalJustification: string;
  supportingDocumentation?: string;
  notes?: string;
}

export interface PriorAuthorizationResponse {
  priorAuthId?: string;
  prescriptionId?: string;
  formularyCheckId?: string;
  insuranceCompanyName?: string;
  policyNumber?: string;
  medicationName?: string;
  priorAuthNumber?: string;
  requestDate?: string;
  status?: string;
  submittedDate?: string;
  approvedDate?: string;
  deniedDate?: string;
  expirationDate?: string;
  denialReason?: string;
  clinicalJustification?: string;
  supportingDocumentation?: string;
  requestedBy?: string;
  pbmName?: string;
  pbmRequestId?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ========== Transmission Monitoring ==========

export interface PrescriptionTransmissionRecord {
  transmissionId?: string;
  prescriptionId?: string;
  transmissionDate?: string;
  transmissionStatus?: string;
  transmissionMethod?: string;
  networkName?: string;
  networkTransactionId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  pharmacyPhone?: string;
  pharmacyCity?: string;
  pharmacyState?: string;
  transmissionSuccess?: boolean;
  confirmationReceived?: boolean;
  confirmationDate?: string;
  confirmationMessage?: string;
  errorMessage?: string;
  errorCode?: string;
  retryCount?: number;
  lastRetryDate?: string;
  maxRetries?: number;
  fillStatus?: string;
  fillStatusDate?: string;
  fillStatusMessage?: string;
  filledDate?: string;
  pickedUpDate?: string;
  cancelledByPharmacy?: boolean;
  cancellationReason?: string;
  transmittedByName?: string;
  transmittedByNpi?: string;
  createdAt?: string;
}

// ========== Phase EHR.7: Prescription Refills & Advanced Features ==========

export interface PrescriptionRefillRequest {
  refillRequestId: string;
  prescriptionId: string;
  prescriptionNumber?: string;
  medicationName?: string;
  requestSource: 'PHARMACY' | 'PATIENT' | 'PROVIDER' | 'SYSTEM';
  requestDate: string;
  requestedBy?: string;
  requestedByName?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  pharmacyPhone?: string;
  refillsRequested?: number;
  refillsRemaining?: number;
  lastFillDate?: string;
  daysSinceLastFill?: number;
  requestStatus: 'PENDING' | 'APPROVED' | 'DENIED' | 'MODIFIED' | 'COMPLETED' | 'CANCELLED';
  approvedBy?: string;
  approvedDate?: string;
  approvalNotes?: string;
  deniedBy?: string;
  deniedDate?: string;
  denialReason?: string;
  modifiedBy?: string;
  modifiedDate?: string;
  modificationNotes?: string;
  originalRefillsRequested?: number;
  notes?: string;
  urgencyLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  wasAutoApproved?: boolean;
  autoApprovalRuleId?: string;
  createdAt: string;
  updatedAt: string;
  refills?: PrescriptionRefill[];
  refillCount?: number;
}

export interface RefillRequestRequest {
  prescriptionId: string;
  requestSource?: 'PHARMACY' | 'PATIENT' | 'PROVIDER' | 'SYSTEM';
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  pharmacyPhone?: string;
  refillsRequested?: number;
  urgencyLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  notes?: string;
}

export interface RefillApprovalRequest {
  approvalNotes?: string;
  refillsApproved?: number;
}

export interface RefillDenialRequest {
  denialReason: string;
}

export interface RefillModificationRequest {
  refillsApproved: number;
  modificationNotes?: string;
}

export interface PrescriptionRefill {
  refillId: string;
  prescriptionId: string;
  refillRequestId?: string;
  refillNumber?: number;
  refillDate: string;
  quantityDispensed?: number;
  quantityUnit?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  filledBy?: string;
  filledByName?: string;
  filledDate?: string;
  notes?: string;
  lotNumber?: string;
  expirationDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionRefillRequestForRecording {
  prescriptionId: string;
  refillRequestId?: string;
  refillDate: string;
  quantityDispensed?: number;
  quantityUnit?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  pharmacyNpi?: string;
  filledBy?: string;
  filledByName?: string;
  notes?: string;
  lotNumber?: string;
  expirationDate?: string;
}

// ========== Phase EHR.8: Patient Summary & Reporting ==========

export interface PatientSummary {
  patientId: string;
  patientName: string;
  mrn: string;
  dateOfBirth: string;
  age?: number;
  gender?: 'Male' | 'Female' | 'Other' | 'Prefer_not_to_answer';
  activeProblemsCount: number;
  activeProblems: ProblemSummary[];
  activePrescriptionsCount: number;
  activePrescriptions: PrescriptionSummary[];
  activeAllergiesCount: number;
  activeAllergies: AllergySummary[];
  latestVitalSigns?: VitalSignsSummary;
  recentNotesCount: number;
  recentNotes: NoteSummary[];
  recentImmunizationsCount: number;
  recentImmunizations: ImmunizationSummary[];
  activeMedicationsCount?: number;
  activeMedications?: MedicationSummary[];
  lastUpdated: string;
}

export interface ProblemSummary {
  problemId: string;
  problemName: string;
  problemType: string;
  status: string;
  icd10Code?: string;
  onsetDate?: string;
}

export interface PrescriptionSummary {
  prescriptionId: string;
  medicationName: string;
  dosage: string;
  route: string;
  frequency?: string;
  startDate: string;
  refillsRemaining?: number;
}

export interface AllergySummary {
  allergyId: string;
  allergenName: string;
  allergenType: string;
  severity: string;
  reactionType?: string;
}

export interface VitalSignsSummary {
  measurementDate: string;
  measurementTime?: string;
  systolicBp?: number;
  diastolicBp?: number;
  heartRate?: number;
  /** Present on patient-summary vitals snapshot; aligns with full `VitalSigns` readings. */
  respiratoryRate?: number;
  temperature?: number;
  temperatureUnit?: string;
  oxygenSaturation?: number;
  weight?: number;
  weightUnit?: string;
  bmi?: number;
}

export interface NoteSummary {
  noteId: string;
  noteType: string;
  noteDate: string;
  noteTime?: string;
  chiefComplaint?: string;
  noteStatus: string;
}

export interface ImmunizationSummary {
  immunizationId: string;
  vaccineName: string;
  administrationDate: string;
}

export interface MedicationSummary {
  medicationId: string;
  medicationName: string;
  genericName?: string;
  dosage?: string;
  route?: string;
  frequency?: string;
  startDate?: string;
  indication?: string;
  medicationStatus?: string;
}

export interface ClinicalNoteMedication {
  linkId: string;
  noteId: string;
  medicationId: string;
  medicationName: string;
  genericName?: string;
  linkType: string;
  linkStrength: string;
  clinicalRelevance?: string;
  notes?: string;
  linkedBy: string;
  linkedDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProblemMedication {
  linkId: string;
  problemId: string;
  medicationId: string;
  medicationName: string;
  genericName?: string;
  linkType: string;
  linkStrength: string;
  clinicalRelevance?: string;
  notes?: string;
  linkedBy: string;
  linkedDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface PatientTimeline {
  patientId: string;
  startDate: string;
  endDate: string;
  events: TimelineEvent[];
  totalEvents: number;
}

export interface TimelineEvent {
  eventDate: string;
  eventTime?: string;
  eventType: 'VITAL_SIGNS' | 'CLINICAL_NOTE' | 'PRESCRIPTION' | 'PROBLEM' | 'IMMUNIZATION' | 'ALLERGY' | 'OTHER';
  title: string;
  description?: string;
}

// ========== Service Methods ==========

const hospitalService = {
  // ========== Patient Management ==========
  
  // Get all patients (with optional search)
  getPatients: (searchTerm?: string) => {
    const url = searchTerm 
      ? `/api/hospital/patients/search?searchTerm=${encodeURIComponent(searchTerm)}`
      : '/api/hospital/patients';
    return api.get<Patient[]>(url);
  },
  
  // Get patient by ID
  getPatient: (patientId: string) =>
    api.get<Patient>(`/api/hospital/patients/${patientId}`),
  
  // Get patient by MRN
  getPatientByMrn: (mrn: string) =>
    api.get<Patient>(`/api/hospital/patients/mrn/${mrn}`),
  
  // Search patients
  searchPatients: (searchTerm: string) =>
    api.get<Patient[]>(`/api/hospital/patients/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Create new patient (set acknowledgeDuplicate after 409 CONFLICT from duplicate check)
  createPatient: (data: PatientRequest, options?: { acknowledgeDuplicate?: boolean }) =>
    api.post<Patient>(
      '/api/hospital/patients',
      data,
      options?.acknowledgeDuplicate
        ? { headers: { 'X-Acknowledge-Duplicate': 'true' } }
        : undefined
    ),
  
  // Update patient
  updatePatient: (patientId: string, data: Partial<PatientRequest>) =>
    api.put<Patient>(`/api/hospital/patients/${patientId}`, data),
  
  // Delete patient (soft delete - sets status to ARCHIVED)
  deletePatient: (patientId: string) =>
    api.delete(`/api/hospital/patients/${patientId}`),
  
  // Check for duplicate patients
  checkDuplicates: (data: PatientRequest) =>
    api.post<DuplicatePatientResponse>('/api/hospital/patients/check-duplicates', data),
  
  // Generate MRN
  generateMrn: () =>
    api.get<string>('/api/hospital/patients/generate-mrn'),

  // Patient identity card print/reprint/replace (Phase 1)
  getPatientIdentityCardPrintPreview: (patientId: string) =>
    api.get<PatientIdentityCardPrintResponse>(`/api/hospital/patients/${patientId}/identity-card/print-preview`),
  reprintPatientIdentityCard: (patientId: string) =>
    api.post<PatientIdentityCardPrintResponse>(`/api/hospital/patients/${patientId}/identity-card/reprint`),
  replacePatientIdentityCard: (patientId: string, reason: string) =>
    api.post<PatientIdentityCardActionResponse>(`/api/hospital/patients/${patientId}/identity-card/replace`, { reason }),
  
  // ========== Doctor Department Management ==========
  
  // Get all departments
  getDoctorDepartments: () =>
    api.get<DoctorDepartment[]>('/api/hospital/doctor-departments'),
  
  // Get active departments
  getActiveDoctorDepartments: () =>
    api.get<DoctorDepartment[]>('/api/hospital/doctor-departments/active'),
  
  // Get department by ID
  getDoctorDepartment: (departmentId: string) =>
    api.get<DoctorDepartment>(`/api/hospital/doctor-departments/${departmentId}`),
  
  // Search departments
  searchDoctorDepartments: (searchTerm: string) =>
    api.get<DoctorDepartment[]>(`/api/hospital/doctor-departments/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Create department
  createDoctorDepartment: (data: DoctorDepartmentRequest) =>
    api.post<DoctorDepartment>('/api/hospital/doctor-departments', data),
  
  // Update department
  updateDoctorDepartment: (departmentId: string, data: Partial<DoctorDepartmentRequest>) =>
    api.put<DoctorDepartment>(`/api/hospital/doctor-departments/${departmentId}`, data),
  
  // Delete department
  deleteDoctorDepartment: (departmentId: string) =>
    api.delete(`/api/hospital/doctor-departments/${departmentId}`),
  
  // ========== Doctor Management ==========
  
  // Get all doctors (excludes soft-deleted unless includeInactive is true)
  getDoctors: (options?: { includeInactive?: boolean }) =>
    api.get<Doctor[]>('/api/hospital/doctors', {
      params:
        options?.includeInactive === true ? { includeInactive: 'true' } : undefined,
    }),
  
  // Get active doctors for prescription
  getActiveDoctorsForPrescription: () =>
    api.get<Doctor[]>('/api/hospital/doctors/active'),
  
  // Get doctor by ID
  getDoctor: (doctorId: string) =>
    api.get<Doctor>(`/api/hospital/doctors/${doctorId}`),
  
  // Get doctor by code
  getDoctorByCode: (doctorCode: string) =>
    api.get<Doctor>(`/api/hospital/doctors/code/${doctorCode}`),
  
  // Search doctors (excludes soft-deleted unless includeInactive is true)
  searchDoctors: (searchTerm: string, options?: { includeInactive?: boolean }) =>
    api.get<Doctor[]>('/api/hospital/doctors/search', {
      params: {
        searchTerm,
        ...(options?.includeInactive === true ? { includeInactive: 'true' } : {}),
      },
    }),
  
  // Get doctors by department
  getDoctorsByDepartment: (departmentId: string) =>
    api.get<Doctor[]>(`/api/hospital/doctors/department/${departmentId}`),
  
  // Get active doctors by department
  getActiveDoctorsByDepartment: (departmentId: string) =>
    api.get<Doctor[]>(`/api/hospital/doctors/department/${departmentId}/active`),
  
  // Get doctors by speciality
  getDoctorsBySpeciality: (speciality: string) =>
    api.get<Doctor[]>(`/api/hospital/doctors/speciality/${encodeURIComponent(speciality)}`),
  
  // Create doctor
  createDoctor: (data: DoctorRequest) =>
    api.post<Doctor>('/api/hospital/doctors', data),
  
  // Update doctor
  updateDoctor: (doctorId: string, data: Partial<DoctorRequest>) =>
    api.put<Doctor>(`/api/hospital/doctors/${doctorId}`, data),
  
  // Delete doctor
  deleteDoctor: (doctorId: string) =>
    api.delete(`/api/hospital/doctors/${doctorId}`),

  // Find or create scheduling resource for a doctor (proxied through hospital-service)
  findOrCreateDoctorSchedulingResource: (doctorId: string) =>
    api.get<{ resourceId: string }>(`/api/hospital/doctors/${doctorId}/scheduling-resource`),
  
  // ========== Emergency Contacts ==========
  
  // Get emergency contacts for a patient
  getEmergencyContacts: (patientId: string) =>
    api.get<EmergencyContact[]>(`/api/hospital/patients/${patientId}/emergency-contacts`),
  
  // Get emergency contact by ID
  getEmergencyContact: (patientId: string, contactId: string) =>
    api.get<EmergencyContact>(`/api/hospital/patients/${patientId}/emergency-contacts/${contactId}`),
  
  // Create emergency contact
  createEmergencyContact: (patientId: string, data: EmergencyContactRequest) =>
    api.post<EmergencyContact>(`/api/hospital/patients/${patientId}/emergency-contacts`, data),
  
  // Update emergency contact
  updateEmergencyContact: (patientId: string, contactId: string, data: Partial<EmergencyContactRequest>) =>
    api.put<EmergencyContact>(`/api/hospital/patients/${patientId}/emergency-contacts/${contactId}`, data),
  
  // Delete emergency contact
  deleteEmergencyContact: (patientId: string, contactId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/emergency-contacts/${contactId}`),
  
  // ========== Insurance Management ==========
  
  // Get insurance information for a patient
  getInsurance: (patientId: string) =>
    api.get<Insurance[]>(`/api/hospital/patients/${patientId}/insurance`),
  
  // Get insurance by ID
  getInsuranceById: (patientId: string, insuranceId: string) =>
    api.get<Insurance>(`/api/hospital/patients/${patientId}/insurance/${insuranceId}`),
  
  // Create insurance
  createInsurance: (patientId: string, data: InsuranceRequest) =>
    api.post<Insurance>(`/api/hospital/patients/${patientId}/insurance`, data),
  
  // Update insurance
  updateInsurance: (patientId: string, insuranceId: string, data: Partial<InsuranceRequest>) =>
    api.put<Insurance>(`/api/hospital/patients/${patientId}/insurance/${insuranceId}`, data),
  
  // Delete insurance
  deleteInsurance: (patientId: string, insuranceId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/insurance/${insuranceId}`),
  
  // ========== Consent Management ==========
  
  // Get consents for a patient
  getConsents: (patientId: string) =>
    api.get<Consent[]>(`/api/hospital/patients/${patientId}/consents`),
  
  // Get consent by ID
  getConsentById: (patientId: string, consentId: string) =>
    api.get<Consent>(`/api/hospital/patients/${patientId}/consents/${consentId}`),
  
  // Create consent
  createConsent: (patientId: string, data: ConsentRequest) =>
    api.post<Consent>(`/api/hospital/patients/${patientId}/consents`, data),
  
  // Update consent
  updateConsent: (patientId: string, consentId: string, data: Partial<ConsentRequest>) =>
    api.put<Consent>(`/api/hospital/patients/${patientId}/consents/${consentId}`, data),
  
  // Delete consent
  deleteConsent: (patientId: string, consentId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/consents/${consentId}`),
  
  // ========== Medical History Management ==========
  
  // Get all medical history for a patient
  getMedicalHistory: (patientId: string) =>
    api.get<MedicalHistory[]>(`/api/hospital/patients/${patientId}/medical-history`),
  
  // Get past medical history for a patient
  getPastMedicalHistory: (patientId: string) =>
    api.get<MedicalHistory[]>(`/api/hospital/patients/${patientId}/medical-history/past-medical`),
  
  // Create medical history
  createMedicalHistory: (patientId: string, data: MedicalHistoryRequest) =>
    api.post<MedicalHistory>(`/api/hospital/patients/${patientId}/medical-history`, data),
  
  // Update medical history
  updateMedicalHistory: (patientId: string, historyId: string, data: Partial<MedicalHistoryRequest>) =>
    api.put<MedicalHistory>(`/api/hospital/patients/${patientId}/medical-history/${historyId}`, data),
  
  // Delete medical history
  deleteMedicalHistory: (patientId: string, historyId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/medical-history/${historyId}`),
  
  // ========== Family History Management ==========
  
  // Get family history for a patient
  getFamilyHistory: (patientId: string) =>
    api.get<FamilyHistory[]>(`/api/hospital/patients/${patientId}/family-history`),
  
  // Create family history
  createFamilyHistory: (patientId: string, data: FamilyHistoryRequest) =>
    api.post<FamilyHistory>(`/api/hospital/patients/${patientId}/family-history`, data),
  
  // Update family history
  updateFamilyHistory: (patientId: string, familyHistoryId: string, data: Partial<FamilyHistoryRequest>) =>
    api.put<FamilyHistory>(`/api/hospital/patients/${patientId}/family-history/${familyHistoryId}`, data),
  
  // Delete family history
  deleteFamilyHistory: (patientId: string, familyHistoryId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/family-history/${familyHistoryId}`),
  
  // ========== Social History Management ==========
  
  // Get social history for a patient
  getSocialHistory: (patientId: string) =>
    api.get<SocialHistory[]>(`/api/hospital/patients/${patientId}/social-history`),
  
  // Create social history
  createSocialHistory: (patientId: string, data: SocialHistoryRequest) =>
    api.post<SocialHistory>(`/api/hospital/patients/${patientId}/social-history`, data),
  
  // Update social history
  updateSocialHistory: (patientId: string, socialHistoryId: string, data: Partial<SocialHistoryRequest>) =>
    api.put<SocialHistory>(`/api/hospital/patients/${patientId}/social-history/${socialHistoryId}`, data),
  
  // Delete social history
  deleteSocialHistory: (patientId: string, socialHistoryId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/social-history/${socialHistoryId}`),
  
  // ========== Immunization Management ==========
  
  // Get immunizations for a patient
  getImmunizations: (patientId: string) =>
    api.get<Immunization[]>(`/api/hospital/patients/${patientId}/immunizations`),
  
  // Create immunization
  createImmunization: (patientId: string, data: ImmunizationRequest) =>
    api.post<Immunization>(`/api/hospital/patients/${patientId}/immunizations`, data),
  
  // Update immunization
  updateImmunization: (patientId: string, immunizationId: string, data: Partial<ImmunizationRequest>) =>
    api.put<Immunization>(`/api/hospital/patients/${patientId}/immunizations/${immunizationId}`, data),
  
  // Delete immunization
  deleteImmunization: (patientId: string, immunizationId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/immunizations/${immunizationId}`),
  
  // ========== Allergy Management ==========
  
  // Get all allergies for a patient
  getAllergies: (patientId: string) =>
    api.get<Allergy[]>(`/api/hospital/patients/${patientId}/allergies`),
  
  // Get active allergies for a patient
  getActiveAllergies: (patientId: string) =>
    api.get<Allergy[]>(`/api/hospital/patients/${patientId}/allergies/active`),
  
  // Create allergy
  createAllergy: (patientId: string, data: AllergyRequest) =>
    api.post<Allergy>(`/api/hospital/patients/${patientId}/allergies`, data),
  
  // Update allergy
  updateAllergy: (patientId: string, allergyId: string, data: Partial<AllergyRequest>) =>
    api.put<Allergy>(`/api/hospital/patients/${patientId}/allergies/${allergyId}`, data),
  
  // Delete allergy
  deleteAllergy: (patientId: string, allergyId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/allergies/${allergyId}`),
  
  // ========== Vital Signs Management ==========
  
  // Get all vital signs for a patient
  getVitalSigns: (patientId: string) =>
    api.get<VitalSigns[]>(`/api/hospital/patients/${patientId}/vital-signs`),
  
  // Get vital signs by ID
  getVitalSignsById: (patientId: string, vitalSignId: string) =>
    api.get<VitalSigns>(`/api/hospital/patients/${patientId}/vital-signs/${vitalSignId}`),
  
  // Get latest vital signs
  getLatestVitalSigns: (patientId: string) =>
    api.get<VitalSigns>(`/api/hospital/patients/${patientId}/vital-signs/latest`),
  
  // Get vital signs summary
  getVitalSignsSummary: (patientId: string) =>
    api.get<VitalSignsSummary>(`/api/hospital/patients/${patientId}/vital-signs/summary`),
  
  // Get vital signs by date range
  getVitalSignsByDateRange: (patientId: string, startDate: string, endDate: string) =>
    api.get<VitalSigns[]>(`/api/hospital/patients/${patientId}/vital-signs/date-range?startDate=${startDate}&endDate=${endDate}`),
  
  // Get abnormal vital signs
  getAbnormalVitalSigns: (patientId: string) =>
    api.get<VitalSigns[]>(`/api/hospital/patients/${patientId}/vital-signs/abnormal`),
  
  // Get critical vital signs
  getCriticalVitalSigns: (patientId: string) =>
    api.get<VitalSigns[]>(`/api/hospital/patients/${patientId}/vital-signs/critical`),
  
  // Get vital signs trends
  getVitalSignsTrends: (patientId: string, startDate?: string) => {
    const url = startDate
      ? `/api/hospital/patients/${patientId}/vital-signs/trends?startDate=${startDate}`
      : `/api/hospital/patients/${patientId}/vital-signs/trends`;
    return api.get<VitalSignsTrend[]>(url);
  },
  
  // Get vital signs by encounter
  getVitalSignsByEncounter: (encounterId: string) =>
    api.get<VitalSigns[]>(`/api/hospital/patients/${encounterId}/vital-signs/encounter/${encounterId}`),
  
  // ========== Encounter/Visit Management ==========
  
  // Get encounter by ID
  getEncounter: (encounterId: string) =>
    api.get<Encounter>(`/api/hospital/encounters/${encounterId}`),
  
  // Get encounter by encounter number
  getEncounterByNumber: (encounterNumber: string) =>
    api.get<Encounter>(`/api/hospital/encounters/number/${encounterNumber}`),
  
  // Get all encounters for a patient
  getEncountersByPatient: (patientId: string) =>
    api.get<Encounter[]>(`/api/hospital/encounters/patient/${patientId}`),
  
  // Get active encounters for a patient
  getActiveEncountersByPatient: (patientId: string) =>
    api.get<Encounter[]>(`/api/hospital/encounters/patient/${patientId}/active`),
  
  // Get active admissions for a patient
  getActiveAdmissionsByPatient: (patientId: string) =>
    api.get<Encounter[]>(`/api/hospital/encounters/patient/${patientId}/admissions/active`),
  
  // Get encounters by organization with optional filters
  getEncounters: (organizationId: string, filters?: {
    status?: string;
    encounterType?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    if (filters?.status) params.append('status', filters.status);
    if (filters?.encounterType) params.append('encounterType', filters.encounterType);
    if (filters?.startDate) params.append('startDate', filters.startDate);
    if (filters?.endDate) params.append('endDate', filters.endDate);
    return api.get<Encounter[]>(`/api/hospital/encounters?${params.toString()}`);
  },
  
  // Get active encounters for organization
  getActiveEncounters: (organizationId: string) =>
    api.get<Encounter[]>(`/api/hospital/encounters/active?organizationId=${organizationId}`),

  /** Active inpatient / hospital-admission encounters (IPD list for Doctor Dashboard). */
  getActiveInpatientEncounters: (organizationId: string, attendingPhysicianId?: string) => {
    const params = new URLSearchParams();
    params.append('organizationId', organizationId);
    if (attendingPhysicianId) params.append('attendingPhysicianId', attendingPhysicianId);
    return api.get<Encounter[]>(`/api/hospital/encounters/active/inpatient?${params.toString()}`);
  },

  // Create new encounter
  createEncounter: (organizationId: string, data: EncounterRequest) =>
    api.post<Encounter>(`/api/hospital/encounters?organizationId=${organizationId}`, data),
  
  // Update encounter
  updateEncounter: (encounterId: string, data: Partial<EncounterRequest>) =>
    api.put<Encounter>(`/api/hospital/encounters/${encounterId}`, data),
  
  // Update encounter status
  updateEncounterStatus: (encounterId: string, status: string) =>
    api.put<Encounter>(`/api/hospital/encounters/${encounterId}/status?status=${status}`),
  
  // Delete encounter
  deleteEncounter: (encounterId: string) =>
    api.delete(`/api/hospital/encounters/${encounterId}`),
  
  // Create vital signs
  createVitalSigns: (patientId: string, data: VitalSignsRequest) =>
    api.post<VitalSigns>(`/api/hospital/patients/${patientId}/vital-signs`, data),
  
  // Update vital signs
  updateVitalSigns: (patientId: string, vitalSignId: string, data: Partial<VitalSignsRequest>) =>
    api.put<VitalSigns>(`/api/hospital/patients/${patientId}/vital-signs/${vitalSignId}`, data),
  
  // Delete vital signs
  deleteVitalSigns: (patientId: string, vitalSignId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/vital-signs/${vitalSignId}`),
  
  // Calculate BMI (Note: requires patientId but endpoint doesn't use it)
  calculateBmi: (patientId: string, weight: number, weightUnit: string, height: number, heightUnit: string) =>
    api.post<number>(`/api/hospital/patients/${patientId}/vital-signs/calculate-bmi?weight=${weight}&weightUnit=${weightUnit}&height=${height}&heightUnit=${heightUnit}`, {}),
  
  // ========== Doctor Hospital Notes (broadcast messages, not patient-specific) ==========

  getDoctorHospitalNotes: () =>
    api.get<DoctorHospitalNote[]>('/api/hospital/doctor-hospital-notes'),

  createDoctorHospitalNote: (data: DoctorHospitalNoteRequest) =>
    api.post<DoctorHospitalNote>('/api/hospital/doctor-hospital-notes', data),

  updateDoctorHospitalNote: (noteId: string, data: DoctorHospitalNoteUpdateRequest) =>
    api.put<DoctorHospitalNote>(`/api/hospital/doctor-hospital-notes/${noteId}`, data),

  deleteDoctorHospitalNote: (noteId: string) =>
    api.delete(`/api/hospital/doctor-hospital-notes/${noteId}`),

  // ========== Clinical Notes Management ==========
  
  // Get all notes for a patient
  getClinicalNotes: (patientId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}`),
  
  // Get current version notes for a patient
  getCurrentVersionNotes: (patientId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}/current`),
  
  // Get notes by type
  getNotesByType: (patientId: string, noteType: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}/type/${noteType}`),
  
  // Get signed notes
  getSignedNotes: (patientId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}/signed`),
  
  // Get draft notes
  getDraftNotes: (patientId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}/drafts`),
  
  // Search notes by content
  searchNotes: (patientId: string, searchTerm: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/patients/${patientId}/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Get note by ID
  getClinicalNote: (noteId: string) =>
    api.get<ClinicalNote>(`/api/hospital/clinical-notes/${noteId}`),
  
  // Create clinical note
  createClinicalNote: (data: ClinicalNoteRequest) =>
    api.post<ClinicalNote>('/api/hospital/clinical-notes', data),
  
  // Update clinical note
  updateClinicalNote: (noteId: string, data: Partial<ClinicalNoteRequest>) =>
    api.put<ClinicalNote>(`/api/hospital/clinical-notes/${noteId}`, data),
  
  // Delete clinical note
  deleteClinicalNote: (noteId: string) =>
    api.delete(`/api/hospital/clinical-notes/${noteId}`),
  
  // Sign note
  signNote: (noteId: string, data: NoteSignRequest) =>
    api.post<ClinicalNote>(`/api/hospital/clinical-notes/${noteId}/sign`, data),
  
  // Amend note
  amendNote: (noteId: string, data: NoteAmendmentRequest) =>
    api.post<ClinicalNote>(`/api/hospital/clinical-notes/${noteId}/amend`, data),
  
  // Get note amendments
  getNoteAmendments: (noteId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/${noteId}/amendments`),
  
  // Void note
  voidNote: (noteId: string) =>
    api.post<ClinicalNote>(`/api/hospital/clinical-notes/${noteId}/void`, {}),
  
  // Get notes by encounter
  getNotesByEncounter: (encounterId: string) =>
    api.get<ClinicalNote[]>(`/api/hospital/clinical-notes/encounters/${encounterId}`),
  
  // ========== Clinical Note Medication Integration ==========
  
  // Link medication to clinical note
  linkMedicationToNote: (noteId: string, medicationId: string, linkType?: string, linkStrength?: string, clinicalRelevance?: string, notes?: string) => {
    const params = new URLSearchParams();
    if (linkType) params.append('linkType', linkType);
    if (linkStrength) params.append('linkStrength', linkStrength);
    if (clinicalRelevance) params.append('clinicalRelevance', clinicalRelevance);
    if (notes) params.append('notes', notes);
    const queryString = params.toString();
    return api.post<ClinicalNoteMedication>(`/api/hospital/clinical-notes/${noteId}/medications/${medicationId}${queryString ? `?${queryString}` : ''}`, {});
  },
  
  // Get medications linked to clinical note
  getMedicationsByNote: (noteId: string) =>
    api.get<ClinicalNoteMedication[]>(`/api/hospital/clinical-notes/${noteId}/medications`),
  
  // Unlink medication from clinical note
  unlinkMedicationFromNote: (noteId: string, medicationId: string) =>
    api.delete(`/api/hospital/clinical-notes/${noteId}/medications/${medicationId}`),
  
  // ========== Note Attachments ==========
  
  // Get note attachments
  getNoteAttachments: (noteId: string) =>
    api.get<NoteAttachment[]>(`/api/hospital/clinical-notes/${noteId}/attachments`),
  
  // Add attachment to note
  addNoteAttachment: (noteId: string, data: NoteAttachmentRequest) =>
    api.post<NoteAttachment>(`/api/hospital/clinical-notes/${noteId}/attachments`, data),
  
  // Delete attachment
  deleteNoteAttachment: (attachmentId: string) =>
    api.delete(`/api/hospital/clinical-notes/attachments/${attachmentId}`),

  // Download attachment file
  downloadNoteAttachment: (attachmentId: string) =>
    api.get(`/api/hospital/clinical-notes/attachments/${attachmentId}/download`, { responseType: 'blob' }),

  // Upload a file attachment to a note (multipart)
  uploadNoteAttachment: (
    noteId: string,
    file: File,
    attachmentType?: string,
    description?: string
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (attachmentType) formData.append('attachmentType', attachmentType);
    if (description) formData.append('description', description);
    return api.post<NoteAttachment>(
      `/api/hospital/clinical-notes/${noteId}/attachments/upload`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  // ========== Patient Documents ==========

  uploadPatientDocument: (
    patientId: string,
    file: File,
    documentType: PatientDocumentType,
    params?: {
      title?: string;
      description?: string;
      sourceFacility?: string;
      documentDate?: string;
      encounterId?: string;
      clinicalNoteId?: string;
      labResultId?: string;
      prescriptionId?: string;
      isConfidential?: boolean;
    }
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (params?.title) formData.append('title', params.title);
    if (params?.description) formData.append('description', params.description);
    if (params?.sourceFacility) formData.append('sourceFacility', params.sourceFacility);
    if (params?.documentDate) {
      const normalizedDocumentDate = params.documentDate.includes('T')
        ? params.documentDate
        : `${params.documentDate}T00:00:00`;
      formData.append('documentDate', normalizedDocumentDate);
    }
    if (params?.encounterId) formData.append('encounterId', params.encounterId);
    if (params?.clinicalNoteId) formData.append('clinicalNoteId', params.clinicalNoteId);
    if (params?.labResultId) formData.append('labResultId', params.labResultId);
    if (params?.prescriptionId) formData.append('prescriptionId', params.prescriptionId);
    if (params?.isConfidential !== undefined) formData.append('isConfidential', String(params.isConfidential));
    return api.post<PatientDocument>(
      `/api/hospital/patients/${patientId}/documents`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  getPatientDocuments: (patientId: string, documentType?: PatientDocumentType) => {
    const params = documentType ? `?documentType=${documentType}` : '';
    return api.get<PatientDocument[]>(`/api/hospital/patients/${patientId}/documents${params}`);
  },

  getNoteDocuments: (noteId: string) =>
    api.get<PatientDocument[]>(`/api/hospital/clinical-notes/${noteId}/documents`),

  getLabResultDocuments: (labResultId: string) =>
    api.get<PatientDocument[]>(`/api/hospital/lab-results/${labResultId}/documents`),

  getPrescriptionDocuments: (prescriptionId: string) =>
    api.get<PatientDocument[]>(`/api/hospital/prescriptions/${prescriptionId}/documents`),

  downloadPatientDocument: (documentId: string) =>
    api.get(`/api/hospital/documents/${documentId}/download`, { responseType: 'blob' }),

  deletePatientDocument: (documentId: string) =>
    api.delete(`/api/hospital/documents/${documentId}`),

  // ========== Note Templates ==========
  
  // Get available templates
  getNoteTemplates: () =>
    api.get<NoteTemplate[]>('/api/hospital/clinical-notes/templates'),
  
  // Get templates by type
  getTemplatesByType: (templateType: string) =>
    api.get<NoteTemplate[]>(`/api/hospital/clinical-notes/templates/type/${templateType}`),
  
  // Search templates
  searchTemplates: (searchTerm: string) =>
    api.get<NoteTemplate[]>(`/api/hospital/clinical-notes/templates/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Get template by ID
  getNoteTemplate: (templateId: string) =>
    api.get<NoteTemplate>(`/api/hospital/clinical-notes/templates/${templateId}`),
  
  // Create template
  createNoteTemplate: (data: NoteTemplateRequest) =>
    api.post<NoteTemplate>('/api/hospital/clinical-notes/templates', data),
  
  // Update template
  updateNoteTemplate: (templateId: string, data: Partial<NoteTemplateRequest>) =>
    api.put<NoteTemplate>(`/api/hospital/clinical-notes/templates/${templateId}`, data),
  
  // Delete template
  deleteNoteTemplate: (templateId: string) =>
    api.delete(`/api/hospital/clinical-notes/templates/${templateId}`),
  
  // ========== Problem List Management ==========
  
  // Get all problems for a patient
  getProblems: (patientId: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems`),
  
  // Get problem list summary
  getProblemListSummary: (patientId: string) =>
    api.get<ProblemListSummary>(`/api/hospital/patients/${patientId}/problems/summary`),
  
  // Get active problems
  getActiveProblems: (patientId: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/active`),
  
  // Get resolved problems
  getResolvedProblems: (patientId: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/resolved`),
  
  // Get current problems (active + chronic)
  getCurrentProblems: (patientId: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/current`),
  
  // Get problems by type
  getProblemsByType: (patientId: string, problemType: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/type/${problemType}`),
  
  // Get problems by status
  getProblemsByStatus: (patientId: string, status: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/status/${status}`),
  
  // Get problems by priority
  getProblemsByPriority: (patientId: string, priority: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/priority/${priority}`),
  
  // Search problems
  searchProblems: (patientId: string, searchTerm: string) =>
    api.get<PatientProblem[]>(`/api/hospital/patients/${patientId}/problems/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Get problem by ID
  getProblem: (patientId: string, problemId: string) =>
    api.get<PatientProblem>(`/api/hospital/patients/${patientId}/problems/${problemId}`),
  
  // Code lookup/suggestions (active catalog codes; patientId kept for call-site compatibility)
  searchIcd10Codes: (_patientId: string, searchTerm: string, limit: number = 50) =>
    api.get<CodeSuggestion[]>(
      `/api/hospital/medical-codes/icd10/suggestions?searchTerm=${encodeURIComponent(searchTerm)}&limit=${limit}`,
    ),

  searchIcd11Codes: (_patientId: string, searchTerm: string, limit: number = 50) =>
    api.get<CodeSuggestion[]>(
      `/api/hospital/medical-codes/icd11/suggestions?searchTerm=${encodeURIComponent(searchTerm)}&limit=${limit}`,
    ),

  searchSnomedCodes: (_patientId: string, searchTerm: string, limit: number = 50) =>
    api.get<CodeSuggestion[]>(
      `/api/hospital/medical-codes/snomed/suggestions?searchTerm=${encodeURIComponent(searchTerm)}&limit=${limit}`,
    ),

  // ========== Medical Code Catalog Management ==========

  getIcd10Catalog: (searchTerm: string = '', page: number = 0, size: number = 25, includeInactive: boolean = false) =>
    api.get<MedicalCodePage>(`/api/hospital/medical-codes/icd10?searchTerm=${encodeURIComponent(searchTerm)}&page=${page}&size=${size}&includeInactive=${includeInactive}`),

  getIcd11Catalog: (searchTerm: string = '', page: number = 0, size: number = 25, includeInactive: boolean = false) =>
    api.get<MedicalCodePage>(`/api/hospital/medical-codes/icd11?searchTerm=${encodeURIComponent(searchTerm)}&page=${page}&size=${size}&includeInactive=${includeInactive}`),

  upsertIcd10CatalogCode: (data: MedicalCodeUpsertRequest) =>
    api.post<MedicalCode>('/api/hospital/medical-codes/icd10', data),

  upsertIcd11CatalogCode: (data: MedicalCodeUpsertRequest) =>
    api.post<MedicalCode>('/api/hospital/medical-codes/icd11', data),

  deactivateIcd10CatalogCode: (code: string) =>
    api.delete(`/api/hospital/medical-codes/icd10/${encodeURIComponent(code)}`),

  deactivateIcd11CatalogCode: (code: string) =>
    api.delete(`/api/hospital/medical-codes/icd11/${encodeURIComponent(code)}`),

  getClinicalChartCatalog: (
    searchTerm: string = '',
    page: number = 0,
    size: number = 25,
    investigationsOnly: boolean = false,
  ) =>
    api.get<ClinicalChartItemPage>(
      `/api/hospital/clinical-chart-items?searchTerm=${encodeURIComponent(searchTerm)}&page=${page}&size=${size}&investigationsOnly=${investigationsOnly}`,
    ),

  autocompleteClinicalChartInvestigations: (q: string = '', limit: number = 40) =>
    api.get<string[]>(
      `/api/hospital/clinical-chart-items/investigations/autocomplete?q=${encodeURIComponent(q)}&limit=${limit}`,
    ),

  getClinicalChartItem: (id: string) =>
    api.get<ClinicalChartItemRow>(`/api/hospital/clinical-chart-items/${encodeURIComponent(id)}`),

  createClinicalChartItem: (data: ClinicalChartItemRequest) =>
    api.post<ClinicalChartItemRow>('/api/hospital/clinical-chart-items', data),

  updateClinicalChartItem: (id: string, data: ClinicalChartItemRequest) =>
    api.put<ClinicalChartItemRow>(`/api/hospital/clinical-chart-items/${encodeURIComponent(id)}`, data),
  
  // Create problem
  createProblem: (patientId: string, data: PatientProblemRequest) =>
    api.post<PatientProblem>(`/api/hospital/patients/${patientId}/problems`, data),
  
  // Update problem
  updateProblem: (patientId: string, problemId: string, data: Partial<PatientProblemRequest>) =>
    api.put<PatientProblem>(`/api/hospital/patients/${patientId}/problems/${problemId}`, data),
  
  // Delete problem
  deleteProblem: (patientId: string, problemId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/problems/${problemId}`),
  
  // Resolve problem
  resolveProblem: (patientId: string, problemId: string, data: ProblemResolutionRequest) =>
    api.post<PatientProblem>(`/api/hospital/patients/${patientId}/problems/${problemId}/resolve`, data),
  
  // Reactivate problem
  reactivateProblem: (patientId: string, problemId: string) =>
    api.post<PatientProblem>(`/api/hospital/patients/${patientId}/problems/${problemId}/reactivate`, {}),
  
  // Get problem history
  getProblemHistory: (patientId: string, problemId: string) =>
    api.get<ProblemHistory[]>(`/api/hospital/patients/${patientId}/problems/${problemId}/history`),
  
  // ========== Problem Medication Integration ==========
  
  // Link medication to problem
  linkMedicationToProblem: (patientId: string, problemId: string, medicationId: string, linkType?: string, linkStrength?: string, clinicalRelevance?: string, notes?: string) => {
    const params = new URLSearchParams();
    if (linkType) params.append('linkType', linkType);
    if (linkStrength) params.append('linkStrength', linkStrength);
    if (clinicalRelevance) params.append('clinicalRelevance', clinicalRelevance);
    if (notes) params.append('notes', notes);
    const queryString = params.toString();
    return api.post<ProblemMedication>(`/api/hospital/patients/${patientId}/problems/${problemId}/medications/${medicationId}${queryString ? `?${queryString}` : ''}`, {});
  },
  
  // Get medications linked to problem
  getMedicationsByProblem: (patientId: string, problemId: string) =>
    api.get<ProblemMedication[]>(`/api/hospital/patients/${patientId}/problems/${problemId}/medications`),
  
  // Unlink medication from problem
  unlinkMedicationFromProblem: (patientId: string, problemId: string, medicationId: string) =>
    api.delete(`/api/hospital/patients/${patientId}/problems/${problemId}/medications/${medicationId}`),
  
  // ========== Prescription Management ==========
  
  // Get all prescriptions for a patient
  getPrescriptions: (patientId: string) =>
    api.get<Prescription[]>(`/api/hospital/prescriptions/patients/${patientId}`),
  
  // Get active prescriptions
  getActivePrescriptions: (patientId: string) =>
    api.get<Prescription[]>(`/api/hospital/prescriptions/patients/${patientId}/active`),
  
  // Get draft prescriptions
  getDraftPrescriptions: (patientId: string) =>
    api.get<Prescription[]>(`/api/hospital/prescriptions/patients/${patientId}/drafts`),
  
  // Get prescription by ID
  getPrescription: (prescriptionId: string) =>
    api.get<Prescription>(`/api/hospital/prescriptions/${prescriptionId}`),
  
  // Get prescription by number
  getPrescriptionByNumber: (prescriptionNumber: string) =>
    api.get<Prescription>(`/api/hospital/prescriptions/number/${prescriptionNumber}`),
  
  // Create prescription
  createPrescription: (data: PrescriptionRequest) =>
    api.post<Prescription>('/api/hospital/prescriptions', data),
  
  // Update prescription
  updatePrescription: (prescriptionId: string, data: Partial<PrescriptionRequest>) =>
    api.put<Prescription>(`/api/hospital/prescriptions/${prescriptionId}`, data),
  
  // Delete prescription
  deletePrescription: (prescriptionId: string) =>
    api.delete(`/api/hospital/prescriptions/${prescriptionId}`),
  
  // Check drug interactions
  checkDrugInteractions: (patientId: string, data: DrugInteractionCheckRequest) =>
    api.post<DrugInteractionCheckResponse>(`/api/hospital/prescriptions/check-interactions?patientId=${patientId}`, data),
  
  // Check prescription interactions
  checkPrescriptionInteractions: (prescriptionId: string) =>
    api.post<Prescription>(`/api/hospital/prescriptions/${prescriptionId}/check-interactions`, {}),
  
  // Check allergies
  checkAllergies: (data: AllergyCheckRequest) =>
    api.post<AllergyCheckResponse>('/api/hospital/prescriptions/check-allergies', data),

  searchDrugsForPrescription: (query: string, page = 0, size = 10) =>
    api.get<PageResponse<PharmacyDrugSuggestion>>('/api/hospital-pharmacy/drugs/search', {
      params: { query, page, size },
    }),

  // ── FR-P3.5: Pharmacy Directory ──────────────────────────────────────────────

  /** Search the pharmacy directory for the prescription picker (name / city / NPI). */
  searchPharmacyDirectory: (q?: string, state?: string, eprescribingOnly = false) =>
    api.get<PharmacyDirectoryEntry[]>('/api/pharmacy-directory/search', {
      params: { q, state, eprescribingOnly },
    }),

  getPharmacyDirectoryAll: () =>
    api.get<PharmacyDirectoryEntry[]>('/api/pharmacy-directory'),

  getPharmacyDirectoryById: (id: string) =>
    api.get<PharmacyDirectoryEntry>(`/api/pharmacy-directory/${id}`),

  getPharmacyDirectoryByNpi: (npi: string) =>
    api.get<PharmacyDirectoryEntry>(`/api/pharmacy-directory/npi/${npi}`),

  /** Admin: entries that have not been verified in the last 90 days. */
  getStalePharmacies: () =>
    api.get<PharmacyDirectoryEntry[]>('/api/pharmacy-directory/stale'),

  createPharmacyDirectoryEntry: (data: PharmacyDirectoryRequest) =>
    api.post<PharmacyDirectoryEntry>('/api/pharmacy-directory', data),

  updatePharmacyDirectoryEntry: (id: string, data: PharmacyDirectoryRequest) =>
    api.put<PharmacyDirectoryEntry>(`/api/pharmacy-directory/${id}`, data),

  markPharmacyVerified: (id: string, verificationNotes?: string) =>
    api.post<PharmacyDirectoryEntry>(`/api/pharmacy-directory/${id}/verify`, { verificationNotes }),

  deactivatePharmacy: (id: string) =>
    api.delete<void>(`/api/pharmacy-directory/${id}`),
  
  // Check prescription allergies
  checkPrescriptionAllergies: (prescriptionId: string) =>
    api.post<Prescription>(`/api/hospital/prescriptions/${prescriptionId}/check-allergies`, {}),
  
  // Validate prescription
  validatePrescription: (prescriptionId: string) =>
    api.post<Prescription>(`/api/hospital/prescriptions/${prescriptionId}/validate`, {}),
  
  // Transmit prescription
  transmitPrescription: (prescriptionId: string, data?: PrescriptionTransmitRequest) =>
    api.post<Prescription>(`/api/hospital/prescriptions/${prescriptionId}/transmit`, data || {}),
  
  // Cancel prescription
  cancelPrescription: (prescriptionId: string, reason?: string) =>
    api.post<Prescription>(`/api/hospital/prescriptions/${prescriptionId}/cancel${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`, {}),

  // ========== PDMP Integration ==========

  queryPDMP: (
    prescriptionId: string,
    data: PDMPQueryRequest,
    providerNpi?: string,
    providerName?: string,
  ) =>
    api.post<PDMPQueryResponse>(
      `/api/hospital/prescriptions/${prescriptionId}/pdmp/query`,
      data,
      {
        headers: {
          ...(providerNpi ? { 'X-Provider-Npi': providerNpi } : {}),
          ...(providerName ? { 'X-Provider-Name': providerName } : {}),
        },
      },
    ),

  getPDMPResults: (prescriptionId: string) =>
    api.get<PDMPQueryResponse[]>(`/api/hospital/prescriptions/${prescriptionId}/pdmp/results`),

  getLatestPDMPResult: (prescriptionId: string) =>
    api.get<PDMPQueryResponse>(`/api/hospital/prescriptions/${prescriptionId}/pdmp/results/latest`),

  getPDMPResultsByPatient: (patientId: string) =>
    api.get<PDMPQueryResponse[]>(`/api/hospital/prescriptions/patients/${patientId}/pdmp/results`),

  // ========== Formulary Checking ==========

  checkFormulary: (prescriptionId: string, data: FormularyCheckRequest) =>
    api.post<FormularyCheckResponse>(`/api/hospital/prescriptions/${prescriptionId}/formulary/check`, data),

  getLatestFormularyCheck: (prescriptionId: string) =>
    api.get<FormularyCheckResponse>(`/api/hospital/prescriptions/${prescriptionId}/formulary/check/latest`),

  getFormularyCheckHistory: (prescriptionId: string) =>
    api.get<FormularyCheckResponse[]>(`/api/hospital/prescriptions/${prescriptionId}/formulary/check/history`),

  getFormularyAlternatives: (prescriptionId: string, formularyCheckId: string) =>
    api.get<FormularyAlternative[]>(`/api/hospital/prescriptions/${prescriptionId}/formulary/alternatives?formularyCheckId=${formularyCheckId}`),

  // ========== Prior Authorization ==========

  submitPriorAuthorization: (prescriptionId: string, data: PriorAuthorizationRequest) =>
    api.post<PriorAuthorizationResponse>(`/api/hospital/prescriptions/${prescriptionId}/prior-authorization/submit`, data),

  getPriorAuthorizations: (prescriptionId: string) =>
    api.get<PriorAuthorizationResponse[]>(`/api/hospital/prescriptions/${prescriptionId}/prior-authorization`),

  checkPriorAuthorizationStatus: (priorAuthId: string) =>
    api.post<PriorAuthorizationResponse>(`/api/hospital/prescriptions/prior-authorization/${priorAuthId}/check-status`, {}),

  updatePriorAuthorizationStatus: (
    priorAuthId: string,
    status: 'PENDING' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'DENIED' | 'APPEALED' | 'CANCELLED' | 'EXPIRED',
    opts?: { priorAuthNumber?: string; expirationDate?: string; denialReason?: string },
  ) => {
    const params = new URLSearchParams({ status });
    if (opts?.priorAuthNumber) params.append('priorAuthNumber', opts.priorAuthNumber);
    if (opts?.expirationDate) params.append('expirationDate', opts.expirationDate);
    if (opts?.denialReason) params.append('denialReason', opts.denialReason);
    return api.put<PriorAuthorizationResponse>(
      `/api/hospital/prescriptions/prior-authorization/${priorAuthId}?${params}`,
      {},
    );
  },

  // ========== Transmission Monitoring ==========

  getPrescriptionTransmissions: (prescriptionId: string) =>
    api.get<PrescriptionTransmissionRecord[]>(`/api/hospital/prescriptions/${prescriptionId}/transmissions`),

  getLatestPrescriptionTransmission: (prescriptionId: string) =>
    api.get<PrescriptionTransmissionRecord>(`/api/hospital/prescriptions/${prescriptionId}/transmissions/latest`),

  retryTransmission: (transmissionId: string) =>
    api.post<PrescriptionTransmissionRecord>(`/api/hospital/prescriptions/transmissions/${transmissionId}/retry`, {}),

  getTransmission: (transmissionId: string) =>
    api.get<PrescriptionTransmissionRecord>(`/api/hospital/prescriptions/transmissions/${transmissionId}`),

  // ========== Interaction & Allergy Acknowledgment ==========

  getPrescriptionInteractions: (prescriptionId: string) =>
    api.get<PrescriptionInteraction[]>(`/api/hospital/prescriptions/${prescriptionId}/interactions`),

  acknowledgeInteraction: (prescriptionId: string, interactionId: string, overrideReason: string) =>
    api.post<PrescriptionInteraction>(
      `/api/hospital/prescriptions/${prescriptionId}/interactions/${interactionId}/acknowledge?overrideReason=${encodeURIComponent(overrideReason)}`,
      {}
    ),

  getPrescriptionAllergyChecks: (prescriptionId: string) =>
    api.get<PrescriptionAllergyCheck[]>(`/api/hospital/prescriptions/${prescriptionId}/allergy-checks`),

  acknowledgeAllergyCheck: (prescriptionId: string, checkId: string, overrideReason: string) =>
    api.post<PrescriptionAllergyCheck>(
      `/api/hospital/prescriptions/${prescriptionId}/allergy-checks/${checkId}/acknowledge?overrideReason=${encodeURIComponent(overrideReason)}`,
      {}
    ),

  // ========== Prescription Refill Management ==========
  
  // Get pending refill requests (queue)
  getPendingRefillRequests: () =>
    api.get<PrescriptionRefillRequest[]>('/api/hospital/prescription-refills/requests'),
  
  // Get refill request by ID
  getRefillRequest: (refillRequestId: string) =>
    api.get<PrescriptionRefillRequest>(`/api/hospital/prescription-refills/requests/${refillRequestId}`),
  
  // Get refill requests for a prescription
  getRefillRequestsByPrescription: (prescriptionId: string) =>
    api.get<PrescriptionRefillRequest[]>(`/api/hospital/prescription-refills/prescriptions/${prescriptionId}/requests`),
  
  // Get refill requests for a patient
  getRefillRequestsByPatient: (patientId: string) =>
    api.get<PrescriptionRefillRequest[]>(`/api/hospital/prescription-refills/patients/${patientId}/requests`),
  
  // Create refill request
  createRefillRequest: (data: RefillRequestRequest) =>
    api.post<PrescriptionRefillRequest>('/api/hospital/prescription-refills/requests', data),
  
  // Approve refill request
  approveRefillRequest: (refillRequestId: string, data?: RefillApprovalRequest) =>
    api.post<PrescriptionRefillRequest>(`/api/hospital/prescription-refills/requests/${refillRequestId}/approve`, data || {}),
  
  // Deny refill request
  denyRefillRequest: (refillRequestId: string, data: RefillDenialRequest) =>
    api.post<PrescriptionRefillRequest>(`/api/hospital/prescription-refills/requests/${refillRequestId}/deny`, data),
  
  // Modify refill request
  modifyRefillRequest: (refillRequestId: string, data: RefillModificationRequest) =>
    api.post<PrescriptionRefillRequest>(`/api/hospital/prescription-refills/requests/${refillRequestId}/modify`, data),
  
  // Get refills for a prescription
  getRefillsByPrescription: (prescriptionId: string) =>
    api.get<PrescriptionRefill[]>(`/api/hospital/prescription-refills/prescriptions/${prescriptionId}/refills`),
  
  // Get refills for a patient
  getRefillsByPatient: (patientId: string) =>
    api.get<PrescriptionRefill[]>(`/api/hospital/prescription-refills/patients/${patientId}/refills`),
  
  // Record prescription refill
  recordRefill: (data: PrescriptionRefillRequestForRecording) =>
    api.post<PrescriptionRefill>('/api/hospital/prescription-refills/refills', data),
  
  // ========== Medication Management ==========
  
  // Get medication by ID
  getMedication: (medicationId: string) =>
    api.get<Medication>(`/api/hospital/medications/${medicationId}`),
  
  // Get all medications for a patient
  getMedications: (patientId: string) =>
    api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}`),
  
  // Get active medications for a patient
  getActiveMedications: (patientId: string) =>
    api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/active`),
  
  // Get medications by status
  getMedicationsByStatus: (patientId: string, status: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED') =>
    api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/status/${status}`),
  
  // Create medication
  createMedication: (data: MedicationRequest) =>
    api.post<Medication>('/api/hospital/medications', data),
  
  // Create medication from prescription
  createMedicationFromPrescription: (prescriptionId: string) =>
    api.post<Medication>(`/api/hospital/medications/from-prescription/${prescriptionId}`),
  
  // Update medication
  updateMedication: (medicationId: string, data: MedicationRequest) =>
    api.put<Medication>(`/api/hospital/medications/${medicationId}`, data),
  
  // Update medication status
  updateMedicationStatus: (medicationId: string, status: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED', reason?: string) => {
    const url = `/api/hospital/medications/${medicationId}/status?status=${status}`;
    const params = reason ? `&reason=${encodeURIComponent(reason)}` : '';
    return api.patch<Medication>(url + params);
  },
  
  // Delete medication (soft delete)
  deleteMedication: (medicationId: string, reason?: string) => {
    const url = `/api/hospital/medications/${medicationId}`;
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return api.delete(url + params);
  },
  
  // Get medication history for a patient
  getMedicationHistory: (patientId: string) =>
    api.get<MedicationHistory[]>(`/api/hospital/medications/patient/${patientId}/history`),

  // Get complete medication history from first prescription to current (chronological)
  getCompleteMedicationHistory: (patientId: string) =>
    api.get<MedicationHistory[]>(`/api/hospital/medications/patient/${patientId}/history/complete`),

  // Search medication history
  searchMedicationHistory: (patientId: string, params?: {
    medicationName?: string;
    genericName?: string;
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.medicationName) queryParams.append('medicationName', params.medicationName);
    if (params?.genericName) queryParams.append('genericName', params.genericName);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationHistory[]>(`/api/hospital/medications/patient/${patientId}/history/search${queryString ? `?${queryString}` : ''}`);
  },

  // Get discontinued medications with discontinuation reason
  getDiscontinuedMedicationsWithReason: (patientId: string) =>
    api.get<MedicationHistory[]>(`/api/hospital/medications/patient/${patientId}/history/discontinued`),

  // Get medication history for a specific medication
  getMedicationHistoryByMedication: (medicationId: string) =>
    api.get<MedicationHistory[]>(`/api/hospital/medications/${medicationId}/history`),

  // Reactivate historical medication
  reactivateHistoricalMedication: (historyId: string) =>
    api.post<Medication>(`/api/hospital/medications/history/${historyId}/reactivate`),
  
  // Create medication from clinical documentation
  createMedicationFromClinicalNote: (noteId: string, data: MedicationRequest) =>
    api.post<Medication>(`/api/hospital/medications/from-clinical-note/${noteId}`, data),
  
  // Bulk import medications from external sources
  importMedications: (patientId: string, medications: MedicationRequest[]) =>
    api.post<Medication[]>(`/api/hospital/medications/import/${patientId}`, medications),

  // ========== Medication List Display and Organization ==========
  
  // Get medications by indication
  getMedicationsByIndication: (patientId: string, indication: string) =>
    api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/by-indication?indication=${encodeURIComponent(indication)}`),

  // Get distinct indications for a patient
  getDistinctIndications: (patientId: string) =>
    api.get<string[]>(`/api/hospital/medications/patient/${patientId}/indications`),

  // Get medication list summary view
  getMedicationListSummary: (patientId: string, params?: {
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/list/summary${queryString ? `?${queryString}` : ''}`);
  },

  // Get medication list detailed view
  getMedicationListDetailed: (patientId: string, params?: {
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/list/detailed${queryString ? `?${queryString}` : ''}`);
  },

  // Get medication list timeline view
  getMedicationListTimeline: (patientId: string, params?: {
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<Medication[]>(`/api/hospital/medications/patient/${patientId}/list/timeline${queryString ? `?${queryString}` : ''}`);
  },

  // ========== Medication Reporting and Analytics ==========
  
  // Generate complete medication list report
  generateCompleteMedicationListReport: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationListReport>(`/api/hospital/medications/reports/patient/${patientId}/list/complete${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate current medication list report
  generateCurrentMedicationListReport: (patientId: string) =>
    api.get<MedicationListReport>(`/api/hospital/medications/reports/patient/${patientId}/list/current`),
  
  // Generate historical medication list report
  generateHistoricalMedicationListReport: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationListReport>(`/api/hospital/medications/reports/patient/${patientId}/list/historical${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate medications by indication report
  generateMedicationsByIndicationReport: (patientId: string, indication?: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (indication) queryParams.append('indication', indication);
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationIndicationReport>(`/api/hospital/medications/reports/patient/${patientId}/by-indication${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate medication adherence report
  generateMedicationAdherenceReport: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationAdherenceReport>(`/api/hospital/medications/reports/patient/${patientId}/adherence${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate medication list completeness metrics
  generateMedicationCompletenessMetrics: (patientId: string) =>
    api.get<MedicationCompletenessMetrics>(`/api/hospital/medications/reports/patient/${patientId}/completeness`),
  
  // Generate medications by provider report
  generateMedicationsByProviderReport: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationClinicalReport>(`/api/hospital/medications/reports/patient/${patientId}/clinical/by-provider${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate medications by problem report
  generateMedicationsByProblemReport: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationClinicalReport>(`/api/hospital/medications/reports/patient/${patientId}/clinical/by-problem${queryString ? `?${queryString}` : ''}`);
  },
  
  // Generate medication quality metrics
  generateMedicationQualityMetrics: (patientId: string, startDate?: string, endDate?: string) => {
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append('startDate', startDate);
    if (endDate) queryParams.append('endDate', endDate);
    const queryString = queryParams.toString();
    return api.get<MedicationQualityMetrics>(`/api/hospital/medications/reports/patient/${patientId}/quality${queryString ? `?${queryString}` : ''}`);
  },

  // Print medication list (returns HTML)
  printMedicationList: (patientId: string, params?: {
    viewType?: 'summary' | 'detailed' | 'timeline';
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.viewType) queryParams.append('viewType', params.viewType);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<string>(`/api/hospital/medications/patient/${patientId}/print${queryString ? `?${queryString}` : ''}`, {
      headers: { 'Accept': 'text/html' }
    });
  },

  // Export medication list to PDF
  exportMedicationListToPdf: (patientId: string, params?: {
    viewType?: 'summary' | 'detailed' | 'timeline';
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.viewType) queryParams.append('viewType', params.viewType);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<Blob>(`/api/hospital/medications/patient/${patientId}/export/pdf${queryString ? `?${queryString}` : ''}`, {
      responseType: 'blob'
    });
  },

  // Export medication list to CSV
  exportMedicationListToCsv: (patientId: string, params?: {
    viewType?: 'summary' | 'detailed' | 'timeline';
    status?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
    indication?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.viewType) queryParams.append('viewType', params.viewType);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.indication) queryParams.append('indication', params.indication);
    if (params?.startDate) queryParams.append('startDate', params.startDate);
    if (params?.endDate) queryParams.append('endDate', params.endDate);
    const queryString = queryParams.toString();
    return api.get<Blob>(`/api/hospital/medications/patient/${patientId}/export/csv${queryString ? `?${queryString}` : ''}`, {
      responseType: 'blob'
    });
  },
  
  // ========== Patient Summary & Reporting ==========
  
  // Get patient summary
  getPatientSummary: (patientId: string) =>
    api.get<PatientSummary>(`/api/hospital/patients/${patientId}/summary`),
  
  // Get patient timeline
  getPatientTimeline: (patientId: string, startDate?: string, endDate?: string) => {
    let url = `/api/hospital/patients/${patientId}/summary/timeline`;
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (params.toString()) url += '?' + params.toString();
    return api.get<PatientTimeline>(url);
  },
  
  // ========== Laboratory Orders ==========
  
  // Get all lab orders
  getAllLabOrders: () =>
    api.get<LabOrder[]>('/api/hospital/lab-orders'),
  
  // Get lab order by ID
  getLabOrderById: (orderId: string) =>
    api.get<LabOrder>(`/api/hospital/lab-orders/${orderId}`),
  
  // Get lab order by number
  getLabOrderByNumber: (orderNumber: string) =>
    api.get<LabOrder>(`/api/hospital/lab-orders/number/${orderNumber}`),
  
  // Get lab orders for a patient
  getLabOrders: (patientId: string) =>
    api.get<LabOrder[]>(`/api/hospital/lab-orders/patients/${patientId}`),
  
  // Get pending lab orders for a patient
  getPendingLabOrders: (patientId: string) =>
    api.get<LabOrder[]>(`/api/hospital/lab-orders/patients/${patientId}/pending`),
  
  // Get completed lab orders for a patient
  getCompletedLabOrders: (patientId: string) =>
    api.get<LabOrder[]>(`/api/hospital/lab-orders/patients/${patientId}/completed`),
  
  // Create lab order
  createLabOrder: (data: LabOrderRequest) =>
    api.post<LabOrder>('/api/hospital/lab-orders', data),
  
  // Update lab order
  updateLabOrder: (orderId: string, data: LabOrderRequest) =>
    api.put<LabOrder>(`/api/hospital/lab-orders/${orderId}`, data),
  
  // Send lab order
  sendLabOrder: (orderId: string) =>
    api.post<LabOrder>(`/api/hospital/lab-orders/${orderId}/send`),
  
  // Cancel lab order
  cancelLabOrder: (orderId: string, reason?: string) => {
    const url = reason 
      ? `/api/hospital/lab-orders/${orderId}/cancel?reason=${encodeURIComponent(reason)}`
      : `/api/hospital/lab-orders/${orderId}/cancel`;
    return api.post<LabOrder>(url);
  },
  
  // Delete lab order
  deleteLabOrder: (orderId: string) =>
    api.delete(`/api/hospital/lab-orders/${orderId}`),
  
  // Reschedule lab order
  rescheduleLabOrder: (orderId: string, scheduledDate: string) =>
    api.post<LabOrder>(`/api/hospital/lab-orders/${orderId}/reschedule?scheduledDate=${encodeURIComponent(scheduledDate)}`),
  
  // Generate HL7 V2 ORM message
  generateHL7V2OrmMessage: (orderId: string) =>
    api.get<{ messageType: string; messageFormat: string; messageContent: string; orderId: string; orderNumber: string }>(`/api/hospital/lab-orders/${orderId}/hl7-v2-orm`),
  
  // Generate HL7 FHIR ServiceRequest
  generateHL7FhirServiceRequest: (orderId: string) =>
    api.get<{ messageType: string; messageFormat: string; messageResource: any; orderId: string; orderNumber: string }>(`/api/hospital/lab-orders/${orderId}/hl7-fhir-servicerequest`),
  
  // ========== Imaging Order Management ==========
  
  // Get all imaging orders
  getAllImagingOrders: () =>
    api.get<ImagingOrder[]>('/api/hospital/imaging-orders'),
  
  // Get imaging order by ID
  getImagingOrderById: (orderId: string) =>
    api.get<ImagingOrder>(`/api/hospital/imaging-orders/${orderId}`),
  
  // Get imaging order by number
  getImagingOrderByNumber: (orderNumber: string) =>
    api.get<ImagingOrder>(`/api/hospital/imaging-orders/number/${orderNumber}`),
  
  // Get imaging orders for a patient
  getImagingOrders: (patientId: string) =>
    api.get<ImagingOrder[]>(`/api/hospital/imaging-orders/patients/${patientId}`),
  
  // Get pending imaging orders for a patient
  getPendingImagingOrders: (patientId: string) =>
    api.get<ImagingOrder[]>(`/api/hospital/imaging-orders/patients/${patientId}/pending`),
  
  // Get completed imaging orders for a patient
  getCompletedImagingOrders: (patientId: string) =>
    api.get<ImagingOrder[]>(`/api/hospital/imaging-orders/patients/${patientId}/completed`),
  
  // Create imaging order
  createImagingOrder: (data: ImagingOrderRequest) =>
    api.post<ImagingOrder>('/api/hospital/imaging-orders', data),
  
  // Update imaging order
  updateImagingOrder: (orderId: string, data: ImagingOrderRequest) =>
    api.put<ImagingOrder>(`/api/hospital/imaging-orders/${orderId}`, data),
  
  // Send imaging order
  sendImagingOrder: (orderId: string) =>
    api.post<ImagingOrder>(`/api/hospital/imaging-orders/${orderId}/send`),
  
  // Cancel imaging order
  cancelImagingOrder: (orderId: string, reason?: string) => {
    const url = reason 
      ? `/api/hospital/imaging-orders/${orderId}/cancel?reason=${encodeURIComponent(reason)}`
      : `/api/hospital/imaging-orders/${orderId}/cancel`;
    return api.post<ImagingOrder>(url);
  },
  
  // Delete imaging order
  deleteImagingOrder: (orderId: string) =>
    api.delete(`/api/hospital/imaging-orders/${orderId}`),
  
  // Reschedule imaging order
  rescheduleImagingOrder: (orderId: string, scheduledDate: string, scheduledTime?: string) => {
    const url = scheduledTime
      ? `/api/hospital/imaging-orders/${orderId}/reschedule?scheduledDate=${encodeURIComponent(scheduledDate)}&scheduledTime=${encodeURIComponent(scheduledTime)}`
      : `/api/hospital/imaging-orders/${orderId}/reschedule?scheduledDate=${encodeURIComponent(scheduledDate)}`;
    return api.post<ImagingOrder>(url);
  },
  
  // Generate HL7 V2 ORM message for imaging
  generateHL7V2OrmMessageForImaging: (orderId: string) =>
    api.get<{ messageType: string; messageFormat: string; messageContent: string; orderId: string; orderNumber: string }>(`/api/hospital/imaging-orders/${orderId}/hl7-v2-orm`),
  
  // Generate HL7 FHIR ServiceRequest for imaging
  generateHL7FhirServiceRequestForImaging: (orderId: string) =>
    api.get<{ messageType: string; messageFormat: string; messageResource: any; orderId: string; orderNumber: string }>(`/api/hospital/imaging-orders/${orderId}/hl7-fhir-servicerequest`),
  
  // Manually transmit imaging order to RIS/PACS
  transmitToRISPACS: (orderId: string) =>
    api.post<{ success: boolean; status: string; message: string; transmittedAt: string; transmissionMethod?: string; responseCode?: number; responseMessage?: string; attempt?: number }>(`/api/hospital/imaging-orders/${orderId}/transmit-ris-pacs`),
  
  // Submit imaging order to DICOM worklist
  submitToDICOMWorklist: (orderId: string) =>
    api.post<{ success: boolean; status: string; message: string; submittedAt: string; worklistEntry?: any }>(`/api/hospital/imaging-orders/${orderId}/dicom-worklist/submit`),
  
  // Generate DICOM worklist entry
  generateDICOMWorklistEntry: (orderId: string) =>
    api.get<any>(`/api/hospital/imaging-orders/${orderId}/dicom-worklist`),
  
  // Query DICOM worklist
  queryDICOMWorklist: (modality: string, scheduledDate?: string) => {
    const url = scheduledDate
      ? `/api/hospital/imaging-orders/dicom-worklist/query?modality=${encodeURIComponent(modality)}&scheduledDate=${encodeURIComponent(scheduledDate)}`
      : `/api/hospital/imaging-orders/dicom-worklist/query?modality=${encodeURIComponent(modality)}`;
    return api.get<any[]>(url);
  },
  
  // Schedule imaging order
  scheduleImagingOrder: (orderId: string, scheduledDate: string) => {
    return api.post<{ success: boolean; status: string; message: string; scheduledDate: string; appointmentId?: string; scheduledAt: string }>(
      `/api/hospital/imaging-orders/${orderId}/schedule?scheduledDate=${encodeURIComponent(scheduledDate)}`
    );
  },
  
  // Cancel scheduled appointment
  cancelImagingAppointment: (orderId: string) =>
    api.post<{ success: boolean; status: string; message: string; scheduledAt: string }>(`/api/hospital/imaging-orders/${orderId}/cancel-appointment`),
  
  // ========== DICOM Image Management ==========
  
  // Get DICOM images for a study
  getDicomImagesByStudy: (studyId: string) =>
    api.get<DICOMImage[]>(`/api/hospital/dicom/images/study/${studyId}`),
  
  // Get DICOM image metadata
  getDicomImageMetadata: (attachmentId: string) =>
    api.get<DICOMMetadata>(`/api/hospital/dicom/images/${attachmentId}/metadata`),
  
  // Download DICOM file
  downloadDicomFile: (attachmentId: string) =>
    api.get<Blob>(`/api/hospital/dicom/images/${attachmentId}/download`, { responseType: 'blob' }),
  
  // Upload DICOM file
  uploadDicomFile: (studyId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<DICOMImage>(`/api/hospital/dicom/images/${studyId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  
  // ========== Imaging Study Management ==========
  
  // Get all imaging studies for a patient
  getImagingStudies: (patientId: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/patients/${patientId}`),
  
  // Get imaging study by ID
  getImagingStudyById: (studyId: string) =>
    api.get<ImagingStudy>(`/api/hospital/imaging-studies/${studyId}`),
  
  // Get imaging study by study number
  getImagingStudyByNumber: (studyNumber: string) =>
    api.get<ImagingStudy>(`/api/hospital/imaging-studies/number/${studyNumber}`),
  
  // Get imaging study by accession number
  getImagingStudyByAccessionNumber: (accessionNumber: string) =>
    api.get<ImagingStudy>(`/api/hospital/imaging-studies/accession/${accessionNumber}`),
  
  // Get imaging studies by order
  getImagingStudiesByOrder: (orderId: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/orders/${orderId}`),
  
  // Get imaging studies by encounter
  getImagingStudiesByEncounter: (encounterId: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/encounters/${encounterId}`),
  
  // Get imaging studies by modality
  getImagingStudiesByModality: (patientId: string, modality: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/patients/${patientId}/modality/${modality}`),
  
  // Get imaging studies by body part
  getImagingStudiesByBodyPart: (patientId: string, bodyPart: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/patients/${patientId}/body-part/${encodeURIComponent(bodyPart)}`),
  
  // Get unacknowledged critical findings for a patient
  getUnacknowledgedCriticalFindings: (patientId: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/patients/${patientId}/critical/unacknowledged`),
  
  // Get all unacknowledged critical findings
  getAllUnacknowledgedCriticalFindings: () =>
    api.get<ImagingStudy[]>('/api/hospital/imaging-studies/critical/unacknowledged'),
  
  // Get unreviewed studies for a patient
  getUnreviewedStudies: (patientId: string) =>
    api.get<ImagingStudy[]>(`/api/hospital/imaging-studies/patients/${patientId}/unreviewed`),
  
  // Create imaging study
  createImagingStudy: (data: ImagingStudyRequest) =>
    api.post<ImagingStudy>('/api/hospital/imaging-studies', data),
  
  // Update imaging study
  updateImagingStudy: (studyId: string, data: ImagingStudyRequest) =>
    api.put<ImagingStudy>(`/api/hospital/imaging-studies/${studyId}`, data),
  
  // Review imaging study
  reviewImagingStudy: (studyId: string, reviewNotes?: string) => {
    const url = reviewNotes
      ? `/api/hospital/imaging-studies/${studyId}/review?reviewNotes=${encodeURIComponent(reviewNotes)}`
      : `/api/hospital/imaging-studies/${studyId}/review`;
    return api.post<ImagingStudy>(url);
  },
  
  // Acknowledge critical finding
  acknowledgeCriticalFinding: (studyId: string, response?: string) => {
    const url = response
      ? `/api/hospital/imaging-studies/${studyId}/acknowledge-critical?response=${encodeURIComponent(response)}`
      : `/api/hospital/imaging-studies/${studyId}/acknowledge-critical`;
    return api.post<ImagingStudy>(url);
  },
  
  // Finalize study report
  finalizeStudyReport: (studyId: string) =>
    api.post<ImagingStudy>(`/api/hospital/imaging-studies/${studyId}/finalize`),
  
  // Get printable report (HTML)
  getPrintableReport: (studyId: string) =>
    api.get<string>(`/api/hospital/imaging-studies/${studyId}/print`, { responseType: 'text' }),
  
  // Export report to PDF
  exportReportToPdf: (studyId: string) =>
    api.get<Blob>(`/api/hospital/imaging-studies/${studyId}/export/pdf`, { responseType: 'blob' }),
  
  // Get imaging study timeline
  getImagingStudyTimeline: (patientId: string, modality?: string, bodyPart?: string) => {
    const params: any = {};
    if (modality) params.modality = modality;
    if (bodyPart) params.bodyPart = bodyPart;
    return api.get<any>(`/api/hospital/imaging-studies/patients/${patientId}/timeline`, { params });
  },
  
  // Get imaging study trends
  getImagingStudyTrends: (patientId: string, modality?: string, bodyPart?: string) => {
    const params: any = {};
    if (modality) params.modality = modality;
    if (bodyPart) params.bodyPart = bodyPart;
    return api.get<any>(`/api/hospital/imaging-studies/patients/${patientId}/trends`, { params });
  },
  
  // ========== Imaging Alerts ==========
  
  // Get imaging alerts for patient
  getImagingAlerts: (patientId: string) =>
    api.get<any[]>(`/api/hospital/imaging-alerts/patients/${patientId}`),
  
  // Get unacknowledged imaging alerts for patient
  getUnacknowledgedImagingAlerts: (patientId: string) =>
    api.get<any[]>(`/api/hospital/imaging-alerts/patients/${patientId}/unacknowledged`),
  
  // Acknowledge imaging alert
  acknowledgeImagingAlert: (alertId: string, notes?: string) => {
    const url = notes
      ? `/api/hospital/imaging-alerts/${alertId}/acknowledge?acknowledgmentNotes=${encodeURIComponent(notes)}`
      : `/api/hospital/imaging-alerts/${alertId}/acknowledge`;
    return api.post<any>(url);
  },
  
  // ========== Imaging Study Integration ==========
  
  // Link study to clinical note
  linkImagingStudyToClinicalNote: (data: { studyId: string; targetId: string; linkType?: string; linkStrength?: string; clinicalRelevance?: string; notes?: string }) =>
    api.post<any>('/api/hospital/imaging-studies/integration/clinical-notes', data),
  
  // Link study to problem
  linkImagingStudyToProblem: (data: { studyId: string; targetId: string; linkType?: string; linkStrength?: string; clinicalRelevance?: string; notes?: string }) =>
    api.post<any>('/api/hospital/imaging-studies/integration/problems', data),
  
  // Link study to medication
  linkImagingStudyToMedication: (data: { studyId: string; targetId: string; linkType?: string; linkStrength?: string; clinicalRelevance?: string; notes?: string }) =>
    api.post<any>('/api/hospital/imaging-studies/integration/medications', data),
  
  // Link study to encounter
  linkImagingStudyToEncounter: (studyId: string, encounterId: string) =>
    api.put(`/api/hospital/imaging-studies/integration/studies/${studyId}/encounter`, null, { params: { encounterId } }),
  
  // Get clinical notes for study
  getClinicalNotesForImagingStudy: (studyId: string) =>
    api.get<any[]>(`/api/hospital/imaging-studies/integration/studies/${studyId}/clinical-notes`),
  
  // Get problems for study
  getProblemsForImagingStudy: (studyId: string) =>
    api.get<any[]>(`/api/hospital/imaging-studies/integration/studies/${studyId}/problems`),
  
  // Get medications for study
  getMedicationsForImagingStudy: (studyId: string) =>
    api.get<any[]>(`/api/hospital/imaging-studies/integration/studies/${studyId}/medications`),
  
  // Unlink from clinical note
  unlinkImagingStudyFromClinicalNote: (linkId: string) =>
    api.delete(`/api/hospital/imaging-studies/integration/clinical-notes/${linkId}`),
  
  // Unlink from problem
  unlinkImagingStudyFromProblem: (linkId: string) =>
    api.delete(`/api/hospital/imaging-studies/integration/problems/${linkId}`),
  
  // Unlink from medication
  unlinkImagingStudyFromMedication: (linkId: string) =>
    api.delete(`/api/hospital/imaging-studies/integration/medications/${linkId}`),
  
  // ========== DICOM Image Management ==========
  
  // Download DICOM image
  downloadDicomImage: (attachmentId: string) =>
    api.get<Blob>(`/api/hospital/dicom/images/${attachmentId}/download`, { responseType: 'blob' }),
  
  // Get DICOM metadata
  getDicomMetadata: (attachmentId: string) =>
    api.get<DICOMMetadata>(`/api/hospital/dicom/images/${attachmentId}/metadata`),
  
  // Upload DICOM image
  uploadDicomImage: (studyId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<DICOMImage>(`/api/hospital/dicom/images/${studyId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  
  // Get DICOM thumbnail
  getDicomThumbnail: (attachmentId: string) =>
    api.get<Blob>(`/api/hospital/dicom/images/${attachmentId}/thumbnail`, { responseType: 'blob' }),

  // C-ECHO — verify connectivity to remote AE
  cEchoDicom: (params?: {
    remoteAeTitle?: string;
    remoteHost?: string;
    remotePort?: number;
  }) => {
    const search = new URLSearchParams();
    if (params?.remoteAeTitle) search.set('remoteAeTitle', params.remoteAeTitle);
    if (params?.remoteHost) search.set('remoteHost', params.remoteHost);
    if (typeof params?.remotePort === 'number' && params.remotePort > 0) {
      search.set('remotePort', String(params.remotePort));
    }
    const qs = search.toString();
    return api.post<DICOMEchoResult>(`/api/hospital/dicom/network/c-echo${qs ? `?${qs}` : ''}`);
  },

  // C-STORE to remote PACS
  cStoreDicomImage: (
    attachmentId: string,
    params?: {
      remoteAeTitle?: string;
      remoteHost?: string;
      remotePort?: number;
    },
  ) => {
    const search = new URLSearchParams();
    if (params?.remoteAeTitle) search.set('remoteAeTitle', params.remoteAeTitle);
    if (params?.remoteHost) search.set('remoteHost', params.remoteHost);
    if (typeof params?.remotePort === 'number' && params.remotePort > 0) {
      search.set('remotePort', String(params.remotePort));
    }
    const qs = search.toString();
    return api.post<DICOMImage>(`/api/hospital/dicom/network/c-store/${attachmentId}${qs ? `?${qs}` : ''}`);
  },

  // C-FIND query against remote PACS
  cFindDicomStudies: (params: {
    patientId?: string;
    studyInstanceUID?: string;
    accessionNumber?: string;
    studyDate?: string;
    modality?: string;
    remoteAeTitle?: string;
    remoteHost?: string;
    remotePort?: number;
  }) => {
    const search = new URLSearchParams();
    if (params.patientId) search.set('patientId', params.patientId);
    if (params.studyInstanceUID) search.set('studyInstanceUID', params.studyInstanceUID);
    if (params.accessionNumber) search.set('accessionNumber', params.accessionNumber);
    if (params.studyDate) search.set('studyDate', params.studyDate);
    if (params.modality) search.set('modality', params.modality);
    if (params.remoteAeTitle) search.set('remoteAeTitle', params.remoteAeTitle);
    if (params.remoteHost) search.set('remoteHost', params.remoteHost);
    if (typeof params.remotePort === 'number' && params.remotePort > 0) {
      search.set('remotePort', String(params.remotePort));
    }
    const qs = search.toString();
    return api.post<DICOMCFindStudy[]>(`/api/hospital/dicom/network/c-find${qs ? `?${qs}` : ''}`);
  },

  // C-MOVE retrieve to destination AE
  cMoveDicomStudy: (params: {
    studyInstanceUID: string;
    destinationAeTitle: string;
    remoteAeTitle?: string;
    remoteHost?: string;
    remotePort?: number;
  }) => {
    const search = new URLSearchParams();
    search.set('studyInstanceUID', params.studyInstanceUID);
    search.set('destinationAeTitle', params.destinationAeTitle);
    if (params.remoteAeTitle) search.set('remoteAeTitle', params.remoteAeTitle);
    if (params.remoteHost) search.set('remoteHost', params.remoteHost);
    if (typeof params.remotePort === 'number' && params.remotePort > 0) {
      search.set('remotePort', String(params.remotePort));
    }
    return api.post<DICOMMoveResult>(`/api/hospital/dicom/network/c-move?${search.toString()}`);
  },

  // C-GET retrieve (stub/unsupported in live mode)
  cGetDicomStudy: (params: {
    studyInstanceUID: string;
    seriesInstanceUID?: string;
    remoteAeTitle?: string;
    remoteHost?: string;
    remotePort?: number;
  }) => {
    const search = new URLSearchParams();
    search.set('studyInstanceUID', params.studyInstanceUID);
    if (params.seriesInstanceUID) search.set('seriesInstanceUID', params.seriesInstanceUID);
    if (params.remoteAeTitle) search.set('remoteAeTitle', params.remoteAeTitle);
    if (params.remoteHost) search.set('remoteHost', params.remoteHost);
    if (typeof params.remotePort === 'number' && params.remotePort > 0) {
      search.set('remotePort', String(params.remotePort));
    }
    return api.post<DICOMGetResult>(`/api/hospital/dicom/network/c-get?${search.toString()}`);
  },
  
  // ========== Laboratory Results ==========
  
  // Get all lab results for a patient
  getLabResults: (patientId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/patients/${patientId}`),
  
  // Get lab result by ID
  getLabResultById: (resultId: string) =>
    api.get<LabResult>(`/api/hospital/lab-results/${resultId}`),
  
  // Get lab result by number
  getLabResultByNumber: (resultNumber: string) =>
    api.get<LabResult>(`/api/hospital/lab-results/number/${resultNumber}`),
  
  // Get lab results by order
  getLabResultsByOrder: (orderId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/orders/${orderId}`),
  
  getLabResultsByEncounter: (encounterId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/encounters/${encounterId}`),
  
  // Get unacknowledged critical values for a patient
  getUnacknowledgedCriticalValues: (patientId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/patients/${patientId}/critical/unacknowledged`),
  
  // Get all unacknowledged critical values
  getAllUnacknowledgedCriticalValues: () =>
    api.get<LabResult[]>('/api/hospital/lab-results/critical/unacknowledged'),
  
  // Get unreviewed results for a patient
  getUnreviewedResults: (patientId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/patients/${patientId}/unreviewed`),
  
  // Get abnormal results for a patient
  getAbnormalResults: (patientId: string) =>
    api.get<LabResult[]>(`/api/hospital/lab-results/patients/${patientId}/abnormal`),
  
  // Create lab result
  createLabResult: (data: LabResultRequest) =>
    api.post<LabResult>('/api/hospital/lab-results', data),
  
  // Update lab result
  updateLabResult: (resultId: string, data: LabResultRequest) =>
    api.put<LabResult>(`/api/hospital/lab-results/${resultId}`, data),
  
  // Review lab result
  reviewLabResult: (resultId: string, reviewNotes?: string) => {
    const url = reviewNotes 
      ? `/api/hospital/lab-results/${resultId}/review?reviewNotes=${encodeURIComponent(reviewNotes)}`
      : `/api/hospital/lab-results/${resultId}/review`;
    return api.post<LabResult>(url);
  },
  
  // Acknowledge critical value
  acknowledgeCriticalValue: (resultId: string, response?: string) => {
    const url = response 
      ? `/api/hospital/lab-results/${resultId}/acknowledge-critical?response=${encodeURIComponent(response)}`
      : `/api/hospital/lab-results/${resultId}/acknowledge-critical`;
    return api.post<LabResult>(url);
  },
  
  // ========== Result Display and Viewing ==========
  
  // Get lab results in chronological order with highlighting
  getLabResultsChronological: (patientId: string) =>
    api.get<LabResultListView[]>(`/api/hospital/lab-results/patients/${patientId}/chronological`),
  
  // Get lab results grouped by category
  getLabResultsByCategory: (patientId: string) =>
    api.get<Record<string, LabResultListView[]>>(`/api/hospital/lab-results/patients/${patientId}/by-category`),
  
  // Get lab results by specific category
  getLabResultsByCategoryName: (patientId: string, category: string) =>
    api.get<LabResultListView[]>(`/api/hospital/lab-results/patients/${patientId}/category/${encodeURIComponent(category)}`),
  
  // Get detailed lab result view
  getLabResultDetail: (resultId: string) =>
    api.get<LabResult>(`/api/hospital/lab-results/${resultId}/detail`),
  
  // Compare result with previous
  compareResults: (resultId: string) =>
    api.get<LabResultComparison>(`/api/hospital/lab-results/${resultId}/compare`),
  
  // Get trend data for a test
  getTrendData: (patientId: string, loincCode: string, startDate?: string, endDate?: string) => {
    const params: any = { loincCode };
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return api.get<LabResultTrend>(`/api/hospital/lab-results/patients/${patientId}/trend`, { params });
  },
  
  // Get correlated results
  getCorrelatedResults: (resultId: string) =>
    api.get<LabResultCorrelation>(`/api/hospital/lab-results/${resultId}/correlated`),
  
  // Get correlated results by collection date
  getCorrelatedResultsByDate: (patientId: string, collectionDate: string) =>
    api.get<LabResultCorrelation>(`/api/hospital/lab-results/patients/${patientId}/correlated`, {
      params: { collectionDate }
    }),
  
  // ========== Lab Result Linking ==========
  
  // Link result to clinical note
  linkResultToClinicalNote: (
    resultId: string,
    noteId: string,
    linkType?: string,
    linkStrength?: string,
    clinicalRelevance?: string,
    notes?: string
  ) => {
    const params = new URLSearchParams();
    params.append('noteId', noteId);
    if (linkType) params.append('linkType', linkType);
    if (linkStrength) params.append('linkStrength', linkStrength);
    if (clinicalRelevance) params.append('clinicalRelevance', clinicalRelevance);
    if (notes) params.append('notes', notes);
    return api.post(`/api/hospital/lab-results/${resultId}/link-clinical-note?${params.toString()}`);
  },
  
  // Get clinical notes linked to result
  getLinkedClinicalNotes: (resultId: string) =>
    api.get(`/api/hospital/lab-results/${resultId}/linked-clinical-notes`),
  
  // Unlink result from clinical note
  unlinkResultFromClinicalNote: (resultId: string, noteId: string) =>
    api.delete(`/api/hospital/lab-results/${resultId}/unlink-clinical-note/${noteId}`),
  
  // Link result to medication
  linkResultToMedication: (
    resultId: string,
    prescriptionId: string,
    linkType?: string,
    linkStrength?: string,
    clinicalRelevance?: string,
    notes?: string
  ) => {
    const params = new URLSearchParams();
    params.append('prescriptionId', prescriptionId);
    if (linkType) params.append('linkType', linkType);
    if (linkStrength) params.append('linkStrength', linkStrength);
    if (clinicalRelevance) params.append('clinicalRelevance', clinicalRelevance);
    if (notes) params.append('notes', notes);
    return api.post(`/api/hospital/lab-results/${resultId}/link-medication?${params.toString()}`);
  },
  
  // Get medications linked to result
  getLinkedMedicationsForResult: (resultId: string) =>
    api.get(`/api/hospital/lab-results/${resultId}/linked-medications`),
  
  // Unlink result from medication
  unlinkResultFromMedication: (resultId: string, prescriptionId: string) =>
    api.delete(`/api/hospital/lab-results/${resultId}/unlink-medication/${prescriptionId}`),
  
  // ========== Critical Value Management ==========
  
  // Get all critical value alerts
  getAllCriticalValueAlerts: () =>
    api.get<CriticalValueAlert[]>('/api/hospital/critical-values/alerts'),
  
  // Get unacknowledged critical value alerts
  getUnacknowledgedCriticalValueAlerts: () =>
    api.get<CriticalValueAlert[]>('/api/hospital/critical-values/alerts/unacknowledged'),
  
  // Get critical value alert by ID
  getCriticalValueAlertById: (alertId: string) =>
    api.get<CriticalValueAlert>(`/api/hospital/critical-values/alerts/${alertId}`),
  
  // Get critical value alerts by patient
  getCriticalValueAlertsByPatient: (patientId: string) =>
    api.get<CriticalValueAlert[]>(`/api/hospital/critical-values/alerts/patients/${patientId}`),
  
  // Get critical value alerts by provider
  getCriticalValueAlertsByProvider: (providerId: string) =>
    api.get<CriticalValueAlert[]>(`/api/hospital/critical-values/alerts/providers/${providerId}`),
  
  // Acknowledge critical value
  acknowledgeCriticalValueAlert: (alertId: string, data: CriticalValueAcknowledgmentRequest) =>
    api.post<CriticalValueAlert>(`/api/hospital/critical-values/alerts/${alertId}/acknowledge`, data),
  
  // Escalate critical value
  escalateCriticalValueAlert: (alertId: string, data: CriticalValueEscalationRequest) =>
    api.post<CriticalValueAlert>(`/api/hospital/critical-values/alerts/${alertId}/escalate`, data),
  
  // Check and escalate unacknowledged alerts
  checkAndEscalateUnacknowledgedAlerts: () =>
    api.post('/api/hospital/critical-values/alerts/check-escalation'),
  
  // Get critical value documentation
  getCriticalValueDocumentation: (resultId: string) =>
    api.get<CriticalValueDocumentation>(`/api/hospital/critical-values/results/${resultId}/documentation`),
};

// ========== Critical Value Types ==========

export interface CriticalValueAlert {
  alertId: string;
  resultId: string;
  resultNumber: string;
  patientId: string;
  patientName: string;
  mrn: string;
  testName: string;
  loincCode: string;
  resultValue: string;
  resultUnits?: string;
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  alertMessage: string;
  alertStatus: 'PENDING' | 'NOTIFIED' | 'ACKNOWLEDGED' | 'ESCALATED' | 'RESOLVED';
  alertPriority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  notifiedProviderId?: string;
  notifiedProviderName?: string;
  isAcknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  providerResponse?: string;
  escalationLevel: number;
  escalatedTo?: string;
  escalationDate?: string;
  escalationReason?: string;
  notificationSentDate?: string;
  createdAt: string;
}

export interface CriticalValueAcknowledgmentRequest {
  providerResponse: string;
  acknowledgmentNotes?: string;
}

export interface CriticalValueEscalationRequest {
  escalatedToUserId: string;
  escalationReason?: string;
}

export interface CriticalValueDocumentation {
  resultId: string;
  testName: string;
  resultValue: string;
  resultUnits?: string;
  criticalValueDetectedAt: string;
  isAcknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  providerResponse?: string;
  alertHistory: CriticalValueAlert[];
  documentationHistory: DocumentationEntry[];
}

export interface DocumentationEntry {
  timestamp: string;
  action: string;
  performedBy: string;
  description: string;
}

// ========== Lab Order Types ==========

export interface LabOrder {
  orderId: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  encounterId?: string;
  organizationId?: string;
  orderNumber: string;
  orderDate: string;
  scheduledDate?: string;
  orderingProviderId: string;
  orderingProviderName?: string;
  orderingFacilityId?: string;
  orderingFacilityName?: string;
  testName: string;
  loincCode?: string;
  testCategory?: string;
  testType?: string;
  isTestPanel?: boolean;
  panelName?: string;
  clinicalIndication?: string;
  priority: 'ROUTINE' | 'STAT' | 'ASAP' | 'TIMED';
  specialInstructions?: string;
  fastingRequired?: boolean;
  patientPreparationInstructions?: string;
  orderStatus: 'PENDING' | 'SENT' | 'COLLECTED' | 'IN_PROCESS' | 'COMPLETED' | 'CANCELLED';
  sentDate?: string;
  collectedDate?: string;
  cancelledDate?: string;
  cancellationReason?: string;
  transmissionMethod?: string;
  transmissionStatus?: string;
  transmissionDate?: string;
  laboratoryId?: string;
  laboratoryName?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface LabOrderRequest {
  patientId: string;
  encounterId?: string;
  orderNumber?: string;
  scheduledDate?: string;
  testName: string;
  loincCode?: string;
  testCategory?: string;
  testType?: string;
  isTestPanel?: boolean;
  panelName?: string;
  clinicalIndication?: string;
  priority?: 'ROUTINE' | 'STAT' | 'ASAP' | 'TIMED';
  specialInstructions?: string;
  fastingRequired?: boolean;
  patientPreparationInstructions?: string;
  orderingProviderId?: string;
  orderingProviderName?: string;
  orderingFacilityId?: string;
  orderingFacilityName?: string;
  laboratoryId?: string;
  laboratoryName?: string;
}

// ========== Lab Result Types ==========

export interface LabResult {
  resultId: string;
  orderId: string;
  orderNumber?: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  encounterId?: string;
  organizationId?: string;
  resultNumber: string;
  testName: string;
  loincCode: string;
  testCategory?: string;
  testType?: string;
  resultValue?: string;
  resultValueNumeric?: number;
  resultUnits?: string;
  resultType: 'NUMERIC' | 'TEXT' | 'CODED' | 'STRUCTURED';
  qualitativeResult?: string;
  quantitativeResult?: number;
  resultStatus: 'FINAL' | 'PRELIMINARY' | 'CORRECTED' | 'CANCELLED' | 'AMENDED';
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  referenceRangeUnits?: string;
  referenceRangeText?: string;
  referenceRangeSource?: string;
  ageSpecificRange?: boolean;
  genderSpecificRange?: boolean;
  abnormalFlag?: 'H' | 'L' | 'A' | 'N' | 'C';
  isCriticalValue?: boolean;
  isDeltaCheck?: boolean;
  isPanicValue?: boolean;
  resultInterpretation?: string;
  clinicalSignificance?: 'NORMAL' | 'ABNORMAL' | 'CRITICAL' | 'SIGNIFICANT_CHANGE' | 'TRENDING' | 'STABLE';
  clinicalSignificanceLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  interpretationNotes?: string;
  orderDate?: string;
  specimenCollectionDate: string;
  specimenReceivedDate?: string;
  resultDate: string;
  resultReportedDate: string;
  resultVerifiedDate?: string;
  specimenType?: string;
  specimenSource?: string;
  specimenCollectionMethod?: string;
  specimenId?: string;
  specimenVolume?: string;
  specimenQuality?: string;
  performingLaboratoryName: string;
  laboratoryId?: string;
  laboratoryNpi?: string;
  laboratoryAddressLine1?: string;
  laboratoryAddressLine2?: string;
  laboratoryCity?: string;
  laboratoryState?: string;
  laboratoryZip?: string;
  laboratoryPhone?: string;
  performingTechnologist?: string;
  reviewingPathologist?: string;
  reviewingPhysician?: string;
  laboratoryReferenceNumber?: string;
  laboratoryComments?: string;
  providerComments?: string;
  resultNotes?: string;
  methodUsed?: string;
  isCriticalValueAcknowledged?: boolean;
  criticalValueAcknowledgedBy?: string;
  criticalValueAcknowledgedDate?: string;
  criticalValueResponse?: string;
  isReviewed?: boolean;
  reviewedBy?: string;
  reviewedDate?: string;
  reviewNotes?: string;
  isCorrected?: boolean;
  isAmended?: boolean;
  isCancelled?: boolean;
  originalResultId?: string;
  correctionReason?: string;
  amendmentReason?: string;
  cancellationReason?: string;
  correctionDate?: string;
  amendmentDate?: string;
  cancellationDate?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface LabResultRequest {
  orderId: string;
  patientId?: string;
  encounterId?: string;
  resultNumber?: string;
  testName: string;
  loincCode: string;
  testCategory?: string;
  testType?: string;
  resultValue?: string;
  resultValueNumeric?: number;
  resultUnits?: string;
  resultType: 'NUMERIC' | 'TEXT' | 'CODED' | 'STRUCTURED';
  qualitativeResult?: string;
  quantitativeResult?: number;
  resultStatus?: 'FINAL' | 'PRELIMINARY' | 'CORRECTED' | 'CANCELLED' | 'AMENDED';
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  referenceRangeUnits?: string;
  referenceRangeText?: string;
  referenceRangeSource?: string;
  ageSpecificRange?: boolean;
  genderSpecificRange?: boolean;
  abnormalFlag?: 'H' | 'L' | 'A' | 'N' | 'C';
  isCriticalValue?: boolean;
  isDeltaCheck?: boolean;
  isPanicValue?: boolean;
  resultInterpretation?: string;
  clinicalSignificance?: 'NORMAL' | 'ABNORMAL' | 'CRITICAL' | 'SIGNIFICANT_CHANGE' | 'TRENDING' | 'STABLE';
  clinicalSignificanceLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  interpretationNotes?: string;
  orderDate?: string;
  specimenCollectionDate: string;
  specimenReceivedDate?: string;
  resultDate: string;
  resultReportedDate: string;
  resultVerifiedDate?: string;
  specimenType?: string;
  specimenSource?: string;
  specimenCollectionMethod?: string;
  specimenId?: string;
  specimenVolume?: string;
  specimenQuality?: string;
  performingLaboratoryName: string;
  laboratoryId?: string;
  laboratoryNpi?: string;
  laboratoryAddressLine1?: string;
  laboratoryAddressLine2?: string;
  laboratoryCity?: string;
  laboratoryState?: string;
  laboratoryZip?: string;
  laboratoryPhone?: string;
  performingTechnologist?: string;
  reviewingPathologist?: string;
  reviewingPhysician?: string;
  laboratoryReferenceNumber?: string;
  laboratoryComments?: string;
  providerComments?: string;
  resultNotes?: string;
  methodUsed?: string;
}

// ========== Lab Result Display Types ==========

export interface LabResultListView {
  resultId: string;
  orderId: string;
  orderNumber?: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  resultNumber: string;
  testName: string;
  loincCode: string;
  testCategory?: string;
  testType?: string;
  resultValue?: string;
  resultValueNumeric?: number;
  resultUnits?: string;
  resultType: 'NUMERIC' | 'TEXT' | 'CODED' | 'STRUCTURED';
  resultStatus: 'FINAL' | 'PRELIMINARY' | 'CORRECTED' | 'CANCELLED' | 'AMENDED';
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  referenceRangeUnits?: string;
  abnormalFlag?: 'H' | 'L' | 'A' | 'N' | 'C';
  isCriticalValue?: boolean;
  isDeltaCheck?: boolean;
  isPanicValue?: boolean;
  highlightColor?: 'RED' | 'ORANGE' | 'YELLOW' | 'GREEN' | 'NONE';
  highlightReason?: string;
  requiresAttention?: boolean;
  resultDate: string;
  resultReportedDate: string;
  isReviewed?: boolean;
  isCriticalValueAcknowledged?: boolean;
  performingLaboratoryName?: string;
}

export interface LabResultComparison {
  currentResultId: string;
  previousResultId: string;
  testName: string;
  loincCode: string;
  testCategory?: string;
  currentResult: LabResult;
  previousResult: LabResult;
  absoluteDifference?: number;
  percentChange?: number;
  changeDirection?: 'INCREASED' | 'DECREASED' | 'UNCHANGED';
  isSignificantChange?: boolean;
  comparisonNotes?: string;
  daysBetweenResults?: number;
  comparisonDate: string;
}

export interface LabResultTrend {
  patientId: string;
  testName: string;
  loincCode: string;
  testCategory?: string;
  resultUnits?: string;
  dataPoints: TrendDataPoint[];
  minValue?: number;
  maxValue?: number;
  averageValue?: number;
  medianValue?: number;
  totalDataPoints: number;
  trendDirection?: 'INCREASING' | 'DECREASING' | 'STABLE' | 'FLUCTUATING';
  trendSlope?: number;
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  startDate: string;
  endDate: string;
}

export interface TrendDataPoint {
  resultId: string;
  resultDate: string;
  value?: number;
  resultValue?: string;
  abnormalFlag?: 'H' | 'L' | 'A' | 'N' | 'C';
  isCriticalValue?: boolean;
  resultStatus?: string;
}

export interface LabResultCorrelation {
  patientId: string;
  collectionDate: string;
  encounterId?: string;
  primaryResult: LabResult;
  relatedResults: LabResult[];
  correlationGroup?: string;
  correlationReason?: string;
  totalRelatedResults: number;
  abnormalResultsCount: number;
  criticalResultsCount: number;
}

// ========== Imaging Order Types ==========

export interface ImagingOrder {
  orderId: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  encounterId?: string;
  organizationId?: string;
  orderNumber: string;
  orderDate: string;
  scheduledDate?: string;
  scheduledTime?: string;
  orderingProviderId: string;
  orderingProviderName?: string;
  orderingFacilityId?: string;
  orderingFacilityName?: string;
  studyType: string;
  studyModality: 'XRAY' | 'CT' | 'MRI' | 'ULTRASOUND' | 'MAMMOGRAPHY' | 'NUCLEAR_MEDICINE' | 'PET' | 'DEXA' | 'FLUOROSCOPY' | 'OTHER';
  studyDescription: string;
  cptCode: string;
  bodyPart: string;
  laterality?: string;
  specificAnatomicalSite?: string;
  viewProjection?: string;
  clinicalIndication: string;
  priority: 'ROUTINE' | 'STAT' | 'URGENT';
  specialInstructions?: string;
  contrastRequired?: boolean;
  contrastType?: string;
  patientPreparationRequired?: boolean;
  patientPreparationInstructions?: string;
  sedationRequired?: boolean;
  orderStatus: 'PENDING' | 'SENT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
  sentDate?: string;
  cancelledDate?: string;
  cancellationReason?: string;
  noShow?: boolean;
  transmissionMethod?: string;
  transmissionStatus?: string;
  transmissionDate?: string;
  radiologyFacilityId?: string;
  radiologyFacilityName?: string;
  orderConfirmationReceived?: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface ImagingOrderRequest {
  patientId: string;
  encounterId?: string;
  orderNumber?: string;
  studyType: string;
  studyModality: 'XRAY' | 'CT' | 'MRI' | 'ULTRASOUND' | 'MAMMOGRAPHY' | 'NUCLEAR_MEDICINE' | 'PET' | 'DEXA' | 'FLUOROSCOPY' | 'OTHER';
  studyDescription: string;
  cptCode: string;
  bodyPart: string;
  laterality?: string;
  specificAnatomicalSite?: string;
  viewProjection?: string;
  clinicalIndication: string;
  priority?: 'ROUTINE' | 'STAT' | 'URGENT';
  specialInstructions?: string;
  contrastRequired?: boolean;
  contrastType?: string;
  patientPreparationRequired?: boolean;
  patientPreparationInstructions?: string;
  sedationRequired?: boolean;
  orderingProviderId?: string;
  orderingProviderName?: string;
  orderingFacilityId?: string;
  orderingFacilityName?: string;
  scheduledDate?: string;
  scheduledTime?: string;
  radiologyFacilityId?: string;
  radiologyFacilityName?: string;
}

// ========== Imaging Study Types ==========

export interface ImagingStudy {
  studyId: string;
  orderId: string;
  orderNumber?: string;
  patientId: string;
  patientName?: string;
  mrn?: string;
  encounterId?: string;
  organizationId?: string;
  studyNumber: string;
  accessionNumber: string;
  studyName: string;
  studyModality: 'XRAY' | 'CT' | 'MRI' | 'ULTRASOUND' | 'MAMMOGRAPHY' | 'NUCLEAR_MEDICINE' | 'PET' | 'DEXA' | 'FLUOROSCOPY' | 'OTHER';
  cptCode: string;
  studyDate: string;
  studyCompletionDate: string;
  studyStatus: 'COMPLETED' | 'PRELIMINARY' | 'FINAL' | 'CANCELLED' | 'AMENDED';
  bodyPartExamined: string;
  laterality?: string;
  numberOfImages?: number;
  numberOfSeries?: number;
  contrastUsed?: boolean;
  contrastType?: string;
  techniqueProtocol?: string;
  equipmentUsed?: string;
  equipmentModel?: string;
  radiationDose?: string;
  studyDurationMinutes?: number;
  interpretingRadiologistName?: string;
  interpretingRadiologistNpi?: string;
  interpretingRadiologistSpecialty?: string;
  preliminaryReadingBy?: string;
  reviewingRadiologist?: string;
  reportDate?: string;
  reportFinalizedDate?: string;
  clinicalHistory?: string;
  techniqueDescription?: string;
  findings?: string;
  impressionConclusion?: string;
  recommendations?: string;
  urgencyIndicator?: string;
  isPreliminary?: boolean;
  isFinal?: boolean;
  isAddendum?: boolean;
  isAmended?: boolean;
  isCancelled?: boolean;
  hasCriticalFindings?: boolean;
  isCriticalFindingAcknowledged?: boolean;
  criticalFindingAcknowledgedBy?: string;
  criticalFindingAcknowledgedDate?: string;
  criticalFindingResponse?: string;
  isReviewed?: boolean;
  reviewedBy?: string;
  reviewedDate?: string;
  reviewNotes?: string;
  originalStudyId?: string;
  correctionReason?: string;
  amendmentReason?: string;
  addendumReason?: string;
  cancellationReason?: string;
  correctionDate?: string;
  amendmentDate?: string;
  addendumDate?: string;
  cancellationDate?: string;
  dicomStudyInstanceUid?: string;
  dicomSeriesInstanceUid?: string;
  dicomStorageLocation?: string;
  pacsIntegrated?: boolean;
  imagesAvailable?: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface ImagingStudyRequest {
  orderId: string;
  patientId?: string;
  encounterId?: string;
  studyNumber?: string;
  accessionNumber: string;
  studyName: string;
  studyModality: 'XRAY' | 'CT' | 'MRI' | 'ULTRASOUND' | 'MAMMOGRAPHY' | 'NUCLEAR_MEDICINE' | 'PET' | 'DEXA' | 'FLUOROSCOPY' | 'OTHER';
  cptCode: string;
  studyDate: string;
  studyCompletionDate: string;
  studyStatus?: 'COMPLETED' | 'PRELIMINARY' | 'FINAL' | 'CANCELLED' | 'AMENDED';
  bodyPartExamined: string;
  laterality?: string;
  numberOfImages?: number;
  numberOfSeries?: number;
  contrastUsed?: boolean;
  contrastType?: string;
  techniqueProtocol?: string;
  equipmentUsed?: string;
  equipmentModel?: string;
  radiationDose?: string;
  studyDurationMinutes?: number;
  interpretingRadiologistName?: string;
  interpretingRadiologistNpi?: string;
  interpretingRadiologistSpecialty?: string;
  preliminaryReadingBy?: string;
  reviewingRadiologist?: string;
  reportDate?: string;
  reportFinalizedDate?: string;
  clinicalHistory?: string;
  techniqueDescription?: string;
  findings?: string;
  impressionConclusion?: string;
  recommendations?: string;
  urgencyIndicator?: string;
  isPreliminary?: boolean;
  isFinal?: boolean;
  isAddendum?: boolean;
  isAmended?: boolean;
  isCancelled?: boolean;
  hasCriticalFindings?: boolean;
  isCriticalFindingAcknowledged?: boolean;
  criticalFindingResponse?: string;
  isReviewed?: boolean;
  reviewNotes?: string;
  originalStudyId?: string;
  correctionReason?: string;
  amendmentReason?: string;
  addendumReason?: string;
  cancellationReason?: string;
  dicomStudyInstanceUid?: string;
  dicomSeriesInstanceUid?: string;
  dicomStorageLocation?: string;
  pacsIntegrated?: boolean;
  imagesAvailable?: boolean;
}

// ========== DICOM Image Types ==========

export interface DICOMImage {
  attachmentId: string;
  studyId: string;
  fileName: string;
  fileSize?: number;
  fileUrl?: string;
  isDicom?: boolean;
  dicomSeriesInstanceUid?: string;
  dicomSopInstanceUid?: string;
  thumbnailUrl?: string;
  uploadedDate?: string;
  compressionRatio?: number;
  networkSuccess?: boolean;
  networkStatus?: number;
  networkMessage?: string;
  networkSopInstanceUID?: string;
}

/** C-FIND study-level rows (no local attachment). */
export interface DICOMCFindStudy {
  studyInstanceUID?: string;
  patientId?: string;
  patientName?: string;
  studyDate?: string;
  accessionNumber?: string;
  studyDescription?: string;
  modality?: string;
}

/** JSON body when C-FIND returns 502 (inspect axios error.response.data) */
export interface DicomNetworkErrorBody {
  message: string;
}

export interface DICOMMetadata {
  patientId?: string;
  patientName?: string;
  patientBirthDate?: string;
  patientSex?: string;
  studyInstanceUID?: string;
  studyDate?: string;
  studyTime?: string;
  studyDescription?: string;
  accessionNumber?: string;
  seriesInstanceUID?: string;
  seriesNumber?: number;
  instanceNumber?: number;
  modality?: string;
  sopInstanceUID?: string;
  numberOfFrames?: number;
  rows?: number;
  columns?: number;
  imageWidth?: number;
  imageHeight?: number;
  bitsAllocated?: number;
  windowCenter?: number;
  windowWidth?: number;
  manufacturer?: string;
  manufacturerModelName?: string;
}

export interface DICOMEchoResult {
  success: boolean;
  status: number;
  message?: string;
}

export interface DICOMMoveResult {
  success: boolean;
  status: number;
  numberOfObjects: number;
  remaining?: number;
  failed?: number;
  warning?: number;
  message?: string;
}

export interface DICOMGetResult {
  success: boolean;
  status: number;
  numberOfObjects: number;
  remaining?: number;
  failed?: number;
  warning?: number;
  receiveDirectory?: string;
  message?: string;
  importedAttachmentCount?: number;
  skippedDuplicateCount?: number;
  skippedNoMatchingStudyCount?: number;
}

// ========== Lab Result Value Types (for Test Panels) ==========

export interface LabResultValue {
  valueId: string;
  resultId: string;
  orderId: string;
  patientId: string;
  testName: string;
  loincCode?: string;
  testCategory?: string;
  testType?: string;
  sequenceNumber: number;
  resultValue?: string;
  resultValueNumeric?: number;
  resultUnits?: string;
  resultType: 'NUMERIC' | 'TEXT' | 'CODED' | 'STRUCTURED';
  qualitativeResult?: string;
  quantitativeResult?: number;
  referenceRangeLow?: number;
  referenceRangeHigh?: number;
  referenceRangeUnits?: string;
  referenceRangeText?: string;
  abnormalFlag?: 'H' | 'L' | 'A' | 'N' | 'C';
  isCriticalValue?: boolean;
  isPanicValue?: boolean;
  resultInterpretation?: string;
  clinicalSignificance?: 'NORMAL' | 'ABNORMAL' | 'CRITICAL' | 'SIGNIFICANT_CHANGE' | 'TRENDING' | 'STABLE';
  clinicalSignificanceLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  performingLaboratoryName?: string;
  laboratoryComments?: string;
  methodUsed?: string;
  resultStatus: 'FINAL' | 'PRELIMINARY' | 'CORRECTED' | 'AMENDED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

// ========== Medication Types ==========

export interface Medication {
  medicationId: string;
  patientId: string;
  encounterId?: string;
  medicationName: string;
  genericName?: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  ndcCode?: string;
  rxnormCode?: string;
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm?: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  quantity?: number;
  quantityUnit?: string;
  route?: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  timing?: string;
  instructions?: string;
  prescriptionId?: string;
  prescribingProviderId?: string;
  prescribingProviderName?: string;
  prescribingProviderNpi?: string;
  prescriptionDate?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  medicationStatus: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
  statusDate?: string;
  statusChangedBy?: string;
  indication?: string;
  diagnosisCode?: string;
  medicationSource: 'PRESCRIPTION' | 'PATIENT_REPORTED' | 'PHARMACY' | 'CLINICAL_DOCUMENTATION' | 'EXTERNAL_IMPORT' | 'OTHER';
  startDate: string;
  endDate?: string;
  lastFilledDate?: string;
  notes?: string;
  specialInstructions?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface MedicationRequest {
  patientId: string;
  encounterId?: string;
  medicationName: string;
  genericName?: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  ndcCode?: string;
  rxnormCode?: string;
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm?: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  quantity?: number;
  quantityUnit?: string;
  route?: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  timing?: string;
  instructions?: string;
  prescriptionId?: string;
  prescribingProviderId?: string;
  prescribingProviderName?: string;
  prescribingProviderNpi?: string;
  prescriptionDate?: string;
  pharmacyId?: string;
  pharmacyName?: string;
  refillsAuthorized?: number;
  refillsRemaining?: number;
  medicationStatus?: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
  statusDate?: string;
  indication?: string;
  diagnosisCode?: string;
  medicationSource: 'PRESCRIPTION' | 'PATIENT_REPORTED' | 'PHARMACY' | 'CLINICAL_DOCUMENTATION' | 'EXTERNAL_IMPORT' | 'OTHER';
  startDate: string;
  endDate?: string;
  lastFilledDate?: string;
  notes?: string;
  specialInstructions?: string;
}

export interface MedicationHistory {
  historyId: string;
  medicationId: string;
  patientId: string;
  medicationName: string;
  genericName?: string;
  medicationCode?: string;
  medicationCodeType?: 'RXNORM' | 'NDC' | 'OTHER';
  dosageStrength?: number;
  dosageUnit?: string;
  dosageForm?: 'TABLET' | 'CAPSULE' | 'SYRUP' | 'LIQUID' | 'SOLUTION' | 'SUSPENSION' | 'INJECTION' | 'INFUSION' | 'CREAM' | 'OINTMENT' | 'LOTION' | 'GEL' | 'POWDER' | 'GRANULES' | 'INHALER' | 'INHALATION' | 'DROPS' | 'SUPPOSITORY' | 'SPRAY' | 'PATCH' | 'MOUTHWASH' | 'TOPICAL' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  route?: 'ORAL' | 'IV' | 'IM' | 'SC' | 'TOPICAL' | 'INHALATION' | 'SUBLINGUAL' | 'BUCCAL' | 'RECTAL' | 'OPHTHALMIC' | 'OTIC' | 'NASAL' | 'OTHER';
  frequency?: string;
  instructions?: string;
  startDate: string;
  endDate?: string;
  medicationStatus: 'ACTIVE' | 'DISCONTINUED' | 'ON_HOLD' | 'COMPLETED';
  statusDate: string;
  discontinuationReason?: string;
  medicationSource?: 'PRESCRIPTION' | 'PATIENT_REPORTED' | 'PHARMACY' | 'CLINICAL_DOCUMENTATION' | 'EXTERNAL_IMPORT' | 'OTHER';
  prescriptionId?: string;
  prescribingProviderName?: string;
  indication?: string;
  diagnosisCode?: string;
  notes?: string;
  createdAt: string;
  createdBy?: string;
}

// ========== Medication Reporting Types ==========

export interface MedicationListReport {
  patientId: string;
  patientName: string;
  reportType: 'COMPLETE' | 'CURRENT' | 'HISTORICAL';
  reportDate: string;
  startDate?: string;
  endDate?: string;
  totalMedications: number;
  activeMedications: number;
  discontinuedMedications: number;
  onHoldMedications: number;
  completedMedications: number;
  medications: Medication[];
  summary?: Record<string, any>;
}

export interface MedicationIndicationReport {
  patientId: string;
  patientName: string;
  indication?: string;
  reportDate: string;
  startDate?: string;
  endDate?: string;
  totalMedications: number;
  medications: Medication[];
  indicationSummaries: IndicationSummary[];
}

export interface IndicationSummary {
  indication: string;
  medicationCount: number;
  activeCount: number;
  discontinuedCount: number;
}

export interface MedicationAdherenceReport {
  patientId: string;
  patientName: string;
  reportDate: string;
  startDate?: string;
  endDate?: string;
  overallAdherenceRate: number;
  totalMedications: number;
  adherentMedications: number;
  nonAdherentMedications: number;
  medicationDetails: MedicationAdherenceDetail[];
}

export interface MedicationAdherenceDetail {
  medicationId: string;
  medicationName: string;
  indication?: string;
  startDate: string;
  endDate?: string;
  adherenceRate: number;
  expectedDoses: number;
  actualDoses: number;
  missedDoses: number;
  adherenceStatus: 'ADHERENT' | 'PARTIAL' | 'NON_ADHERENT';
}

export interface MedicationCompletenessMetrics {
  patientId: string;
  patientName: string;
  reportDate: string;
  completenessScore: number;
  totalMedications: number;
  completeMedications: number;
  incompleteMedications: number;
  completenessDetails: CompletenessDetail[];
  missingFieldCounts: Record<string, number>;
}

export interface CompletenessDetail {
  medicationId: string;
  medicationName: string;
  completenessScore: number;
  missingFields: string[];
  incompleteFields: string[];
}

export interface MedicationClinicalReport {
  patientId: string;
  patientName: string;
  reportDate: string;
  startDate?: string;
  endDate?: string;
  reportType: 'BY_PROVIDER' | 'BY_PROBLEM';
  providerSummaries?: ProviderMedicationSummary[];
  problemSummaries?: ProblemMedicationSummary[];
}

export interface ProviderMedicationSummary {
  providerId: string;
  providerName: string;
  providerNpi?: string;
  totalMedications: number;
  activeMedications: number;
  medications: Medication[];
}

export interface ProblemMedicationSummary {
  problemId: string;
  problemName: string;
  diagnosisCode?: string;
  totalMedications: number;
  activeMedications: number;
  medications: Medication[];
}

export interface MedicationQualityMetrics {
  patientId: string;
  patientName: string;
  reportDate: string;
  startDate?: string;
  endDate?: string;
  overallQualityScore: number;
  medicationListQuality: MedicationListQuality;
  reconciliationCompliance: ReconciliationCompliance;
  qualityIssues: QualityIssue[];
}

export interface MedicationListQuality {
  dataQualityScore: number;
  totalMedications: number;
  medicationsWithCompleteData: number;
  medicationsWithMissingData: number;
  dataCompletenessByField: Record<string, number>;
  duplicateMedications: number;
  medicationsWithConflictingData: number;
}

export interface ReconciliationCompliance {
  complianceRate: number;
  totalReconciliations: number;
  completedReconciliations: number;
  pendingReconciliations: number;
  overdueReconciliations: number;
  lastReconciliationDate?: string;
  daysSinceLastReconciliation: number;
}

export interface QualityIssue {
  issueType: 'MISSING_DATA' | 'DUPLICATE' | 'CONFLICTING_DATA' | 'INCOMPLETE_RECONCILIATION';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  medicationId: string;
  medicationName: string;
  recommendation: string;
}

// ========== Doctor Management Types ==========

export interface DoctorDepartment {
  departmentId: string;
  departmentName: string;
  generalVisitAmount?: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface DoctorDepartmentRequest {
  departmentName: string;
  generalVisitAmount?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}

export interface Doctor {
  doctorId: string;
  doctorCode: string;
  doctorName: string;
  departmentId: string;
  departmentName: string;
  doctorType: 'CONSULTANT' | 'RESIDENT' | 'INTERN' | 'SENIOR_CONSULTANT' | 'ASSOCIATE_CONSULTANT' | 'ASSISTANT_CONSULTANT' | 'REGISTRAR' | 'MEDICAL_OFFICER' | 'OTHER';
  indoorOutdoorStatus: 'INDOOR' | 'OUTDOOR';
  degree?: string;
  speciality?: string;
  gender?: string;
  birthDate?: string;
  registrationDate: string;
  bmdcRegistrationNumber?: string;
  phoneNumber?: string;
  email?: string;
  presentAddress?: string;
  district?: string;
  thana?: string;
  area?: string;
  chamberRoom?: string;
  visitFeeNew?: number;
  visitFeeOld?: number;
  takeCommission?: boolean;
  patientsPerDay?: number;
  serialStartFrom?: number;
  numberOfDaysCanAppointment?: number;
  numberOfAppointmentsFromWeb?: number;
  numberOfAppointmentsFromMobile?: number;
  appointmentsFromWeb?: boolean;
  appointmentsFromMobile?: boolean;
  slotsPerDay?: number;
  weeklySchedule?: DoctorWeeklySchedule;
  appointmentSlots?: DoctorAppointmentSlot[];
  offDays?: string[];
  smsEnabled?: boolean;
  prescriptionStatus?: 'ACTIVE' | 'INACTIVE';
  availabilityStatus: 'AVAILABLE' | 'NOT_AVAILABLE';
  isActive: boolean;
  /** Present when a portal user was created for this doctor */
  linkedUserId?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface DoctorAppointmentSlot {
  startTime: string;     // HH:mm
  endTime: string;       // HH:mm
  days: string[];        // ['saturday', 'sunday', 'monday', ...]
  /** When omitted while editing, UI may clear the field; persisted value defaults to 10 on save. */
  maxPatients?: number;
}

export interface DoctorDaySchedule {
  isOff: boolean;
  slots: DoctorAppointmentSlot[];
}

export interface DoctorWeeklySchedule {
  saturday: DoctorDaySchedule;
  sunday: DoctorDaySchedule;
  monday: DoctorDaySchedule;
  tuesday: DoctorDaySchedule;
  wednesday: DoctorDaySchedule;
  thursday: DoctorDaySchedule;
  friday: DoctorDaySchedule;
}

export interface DoctorRequest {
  doctorName: string;
  departmentId: string;
  doctorType: 'CONSULTANT' | 'RESIDENT' | 'INTERN' | 'SENIOR_CONSULTANT' | 'ASSOCIATE_CONSULTANT' | 'ASSISTANT_CONSULTANT' | 'REGISTRAR' | 'MEDICAL_OFFICER' | 'OTHER';
  indoorOutdoorStatus: 'INDOOR' | 'OUTDOOR';
  degree?: string;
  speciality?: string;
  gender?: string;
  birthDate?: string;
  registrationDate: string;
  bmdcRegistrationNumber?: string;
  phoneNumber?: string;
  email?: string;
  presentAddress?: string;
  district?: string;
  thana?: string;
  area?: string;
  chamberRoom?: string;
  visitFeeNew?: number;
  visitFeeOld?: number;
  takeCommission?: boolean;
  patientsPerDay?: number;
  serialStartFrom?: number;
  numberOfDaysCanAppointment?: number;
  numberOfAppointmentsFromWeb?: number;
  numberOfAppointmentsFromMobile?: number;
  appointmentsFromWeb?: boolean;
  appointmentsFromMobile?: boolean;
  slotsPerDay?: number;
  weeklySchedule?: DoctorWeeklySchedule;
  appointmentSlots?: DoctorAppointmentSlot[];
  offDays?: string[];
  smsEnabled?: boolean;
  prescriptionStatus?: 'ACTIVE' | 'INACTIVE';
  availabilityStatus?: 'AVAILABLE' | 'NOT_AVAILABLE';
  isActive?: boolean;
  /**
   * Create/link a users.users login (username = doctor code). On create, defaults true when omitted (server).
   * On update, set true only when the doctor has no linked login yet and should remain active.
   */
  createLinkedUser?: boolean;
}

// ========== Easy Prescription (EP) Module Types & Services ==========

export interface ICD10Code {
  code: string;
  description: string;
  category: string;
}

export interface PrescriptionTemplate {
  templateId: string;
  templateName: string;
  templateType: 'SYSTEM' | 'DOCTOR' | 'DISEASE';
  diseaseCategory?: string;
  complaints?: string[];
  diagnoses?: { code: string; description: string; isPrimary: boolean }[];
  medications: PrescriptionMedicationItem[];
  advice?: string[];
  tests?: { testName: string; isPanel?: boolean; panelName?: string }[];
  followUpDays?: number;
  referral?: string;
  clinicalFindings?: string;
  isPublic?: boolean;
  /** User id who created this template (Easy Prescription workspace). */
  createdBy?: string;
  /** Department of the doctor when the template was saved (for “my department” lists). */
  departmentId?: string;
  departmentName?: string;
  createdAt?: string;
}

/** Scope for EP template pickers (templates page + Apply Template dialog). */
export type EpTemplateListScope = 'mine' | 'department' | 'all' | 'system' | 'disease' | 'doctor';

export function matchesEpTemplateScope(
  t: PrescriptionTemplate,
  scope: EpTemplateListScope,
  ctx: {
    userId?: string | null;
    myDepartmentId?: string | null;
    isDoctorOwned: (templateId: string) => boolean;
  }
): boolean {
  const owned = ctx.isDoctorOwned(t.templateId);
  switch (scope) {
    case 'mine':
      if (!owned) return false;
      if (!ctx.userId) return true;
      return !t.createdBy || t.createdBy === ctx.userId;
    case 'department':
      if (!owned || !ctx.myDepartmentId) return false;
      return Boolean(t.departmentId && t.departmentId === ctx.myDepartmentId);
    case 'all':
      return true;
    case 'system':
      return t.templateType === 'SYSTEM';
    case 'disease':
      return t.templateType === 'DISEASE';
    case 'doctor':
      return owned;
    default:
      return true;
  }
}

export interface DoctorEPConfig {
  signatureDataUrl?: string;
  stampDataUrl?: string;
  headerLine1?: string;
  headerLine2?: string;
  footerText?: string;
  defaultPharmacy?: string;
  defaultAdvice?: string[];
  printFormat?: 'A4' | 'COMPACT';
  favoriteTemplateIds?: string[];
  /** Quick-add medicine names shown on the EP prescribing screen (doctor-configured shortcuts). */
  preferredMedicineNames?: string[];
  /**
   * Scheduling resource ID (e.g. physician column in OPD). When set, Doctor Dashboard filters
   * today's appointments to this resource only.
   */
  mySchedulingResourceId?: string;
  /**
   * EP-9: Optional hint under frequency fields on the prescribe screen (e.g. preferred 1-0-1 notation).
   */
  doseFormatNote?: string;
  /** EP-9: Placeholder text for frequency inputs (overrides default). */
  doseFrequencyPlaceholder?: string;
  /**
   * EP-8: If set, "Copy patient portal link" builds `{baseUrl}/patient/{patientId}/prescriptions`
   * (trailing slash on base is stripped).
   */
  patientPortalBaseUrl?: string;
  /**
   * EP-1 / EP-11: Default encounter context (OPD vs IPD) for dashboard and prescribe deep links.
   */
  epEncounterModeDefault?: 'OPD' | 'IPD';
  /**
   * EP-1: Target minutes to complete a prescription session (shown as SLA timer). Default 2.
   */
  rxSlaTargetMinutes?: number;
}

/** Recent prescriptions written from this browser (Easy Prescription doctor dashboard). */
export interface EpRecentRxEntry {
  prescriptionId: string;
  patientId: string;
  patientName: string;
  prescriptionNumber?: string;
  medicationSummary: string;
  createdAt: string;
}

const EP_RECENT_RX_KEY = 'ep_recent_prescriptions';
const EP_RECENT_RX_MAX = 20;
const EP_RECENT_RX_PATIENT_ID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function sanitizeEpRecentRxEntry(entry: unknown): EpRecentRxEntry | null {
  if (!entry || typeof entry !== 'object') return null;
  const e = entry as Partial<EpRecentRxEntry>;
  if (!e.prescriptionId || !e.patientId || !EP_RECENT_RX_PATIENT_ID_RE.test(e.patientId)) return null;
  return {
    prescriptionId: e.prescriptionId,
    patientId: e.patientId,
    patientName: e.patientName || '',
    prescriptionNumber: e.prescriptionNumber,
    medicationSummary: e.medicationSummary || '—',
    createdAt: e.createdAt || new Date().toISOString(),
  };
}

export const epRecentRxService = {
  list(): EpRecentRxEntry[] {
    try {
      const raw = localStorage.getItem(EP_RECENT_RX_KEY);
      const parsed = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(parsed)) return [];
      return parsed
        .map(sanitizeEpRecentRxEntry)
        .filter((e): e is EpRecentRxEntry => e != null);
    } catch {
      return [];
    }
  },
  push(entry: EpRecentRxEntry): void {
    try {
      const list = this.list().filter(
        e => !(e.prescriptionId === entry.prescriptionId && e.patientId === entry.patientId)
      );
      list.unshift(entry);
      localStorage.setItem(EP_RECENT_RX_KEY, JSON.stringify(list.slice(0, EP_RECENT_RX_MAX)));
      if (typeof window !== 'undefined') {
        try {
          window.dispatchEvent(new CustomEvent('ep-recent-rx-updated'));
        } catch {
          /* ignore */
        }
      }
      void import('./epWorkspaceSync').then(m => m.scheduleEpWorkspacePush()).catch(() => {});
    } catch {
      // ignore
    }
  },
  clear(): void {
    try {
      localStorage.removeItem(EP_RECENT_RX_KEY);
    } catch {
      // ignore
    }
  },
};

export const COMMON_ICD10_CODES: ICD10Code[] = [
  { code: 'J06.9', description: 'Acute upper respiratory infection, unspecified', category: 'Respiratory' },
  { code: 'J00', description: 'Acute nasopharyngitis (common cold)', category: 'Respiratory' },
  { code: 'J02.9', description: 'Acute pharyngitis, unspecified', category: 'Respiratory' },
  { code: 'J03.9', description: 'Acute tonsillitis, unspecified', category: 'Respiratory' },
  { code: 'J18.9', description: 'Pneumonia, unspecified organism', category: 'Respiratory' },
  { code: 'J45.9', description: 'Asthma, unspecified', category: 'Respiratory' },
  { code: 'J44.1', description: 'COPD with acute exacerbation', category: 'Respiratory' },
  { code: 'J20.9', description: 'Acute bronchitis, unspecified', category: 'Respiratory' },
  { code: 'K29.7', description: 'Gastritis, unspecified', category: 'Gastrointestinal' },
  { code: 'K21.0', description: 'Gastro-oesophageal reflux disease with oesophagitis', category: 'Gastrointestinal' },
  { code: 'K58.9', description: 'Irritable bowel syndrome without diarrhoea', category: 'Gastrointestinal' },
  { code: 'K59.00', description: 'Constipation, unspecified', category: 'Gastrointestinal' },
  { code: 'A09', description: 'Other gastroenteritis and colitis of infectious origin', category: 'Gastrointestinal' },
  { code: 'K52.9', description: 'Noninfective gastroenteritis and colitis, unspecified', category: 'Gastrointestinal' },
  { code: 'E11.9', description: 'Type 2 diabetes mellitus without complications', category: 'Endocrine' },
  { code: 'E10.9', description: 'Type 1 diabetes mellitus without complications', category: 'Endocrine' },
  { code: 'E11.65', description: 'Type 2 diabetes mellitus with hyperglycemia', category: 'Endocrine' },
  { code: 'E03.9', description: 'Hypothyroidism, unspecified', category: 'Endocrine' },
  { code: 'E05.9', description: 'Thyrotoxicosis, unspecified', category: 'Endocrine' },
  { code: 'E66.9', description: 'Obesity, unspecified', category: 'Endocrine' },
  { code: 'I10', description: 'Essential (primary) hypertension', category: 'Cardiovascular' },
  { code: 'I25.9', description: 'Chronic ischaemic heart disease, unspecified', category: 'Cardiovascular' },
  { code: 'I50.9', description: 'Heart failure, unspecified', category: 'Cardiovascular' },
  { code: 'I48.9', description: 'Unspecified atrial fibrillation and atrial flutter', category: 'Cardiovascular' },
  { code: 'I63.9', description: 'Cerebral infarction, unspecified', category: 'Cardiovascular' },
  { code: 'I20.9', description: 'Angina pectoris, unspecified', category: 'Cardiovascular' },
  { code: 'M54.5', description: 'Low back pain', category: 'Musculoskeletal' },
  { code: 'M79.609', description: 'Pain in unspecified limb', category: 'Musculoskeletal' },
  { code: 'M10.9', description: 'Gout, unspecified', category: 'Musculoskeletal' },
  { code: 'M05.9', description: 'Rheumatoid arthritis, unspecified', category: 'Musculoskeletal' },
  { code: 'N18.9', description: 'Chronic kidney disease, unspecified', category: 'Renal' },
  { code: 'N39.0', description: 'Urinary tract infection, site not specified', category: 'Renal' },
  { code: 'N20.0', description: 'Calculus of kidney', category: 'Renal' },
  { code: 'N40.0', description: 'Benign prostatic hyperplasia without LUTS', category: 'Renal' },
  { code: 'F32.9', description: 'Major depressive disorder, single episode, unspecified', category: 'Mental Health' },
  { code: 'F41.9', description: 'Anxiety disorder, unspecified', category: 'Mental Health' },
  { code: 'G43.909', description: 'Migraine, unspecified, not intractable', category: 'Neurological' },
  { code: 'G40.909', description: 'Epilepsy, unspecified, not intractable', category: 'Neurological' },
  { code: 'G47.00', description: 'Insomnia, unspecified', category: 'Neurological' },
  { code: 'L30.9', description: 'Dermatitis, unspecified', category: 'Dermatology' },
  { code: 'L20.9', description: 'Atopic dermatitis, unspecified', category: 'Dermatology' },
  { code: 'L50.9', description: 'Urticaria, unspecified', category: 'Dermatology' },
  { code: 'R05.9', description: 'Cough, unspecified', category: 'Symptoms' },
  { code: 'R50.9', description: 'Fever, unspecified', category: 'Symptoms' },
  { code: 'R51.9', description: 'Headache, unspecified', category: 'Symptoms' },
  { code: 'R10.9', description: 'Unspecified abdominal pain', category: 'Symptoms' },
  { code: 'R06.00', description: 'Dyspnea, unspecified', category: 'Symptoms' },
  { code: 'R42', description: 'Dizziness and giddiness', category: 'Symptoms' },
  { code: 'R11.10', description: 'Vomiting, unspecified', category: 'Symptoms' },
  { code: 'R11.0', description: 'Nausea alone', category: 'Symptoms' },
  { code: 'Z71.1', description: 'Person with feared complaint in whom no diagnosis is made', category: 'Other' },
  { code: 'Z00.00', description: 'General adult medical examination without abnormal findings', category: 'Other' },
];

export const SYSTEM_PRESCRIPTION_TEMPLATES: PrescriptionTemplate[] = [
  {
    templateId: 'sys-001',
    templateName: 'Acute URTI / Common Cold',
    templateType: 'SYSTEM',
    diseaseCategory: 'Respiratory',
    complaints: ['Running nose', 'Sore throat', 'Cough', 'Fever'],
    diagnoses: [{ code: 'J06.9', description: 'Acute upper respiratory infection, unspecified', isPrimary: true }],
    medications: [
      { medicationName: 'Paracetamol', dosageStrength: 500, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+1', instructions: 'After meal', startDate: new Date().toISOString().split('T')[0], durationDays: 5 },
      { medicationName: 'Cetirizine', dosageStrength: 10, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '0+0+1', instructions: 'At night', startDate: new Date().toISOString().split('T')[0], durationDays: 5 },
    ],
    advice: ['Take rest', 'Drink warm fluids', 'Avoid cold drinks', 'Steam inhalation if needed'],
    followUpDays: 5,
  },
  {
    templateId: 'sys-002',
    templateName: 'Hypertension (Routine Follow-up)',
    templateType: 'SYSTEM',
    diseaseCategory: 'Cardiovascular',
    complaints: ['Headache', 'Dizziness'],
    diagnoses: [{ code: 'I10', description: 'Essential (primary) hypertension', isPrimary: true }],
    medications: [
      { medicationName: 'Amlodipine', dosageStrength: 5, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+0', instructions: 'Morning, before breakfast', startDate: new Date().toISOString().split('T')[0], durationDays: 30 },
    ],
    advice: ['Low salt diet (<5 g/day)', 'Regular BP monitoring at home', 'Exercise 30 min/day', 'Avoid stress and smoking'],
    tests: [{ testName: 'Blood Pressure Monitoring', isPanel: false }],
    followUpDays: 30,
  },
  {
    templateId: 'sys-003',
    templateName: 'Type 2 Diabetes (Routine Follow-up)',
    templateType: 'SYSTEM',
    diseaseCategory: 'Endocrine',
    complaints: ['Increased thirst', 'Frequent urination', 'Fatigue'],
    diagnoses: [{ code: 'E11.9', description: 'Type 2 diabetes mellitus without complications', isPrimary: true }],
    medications: [
      { medicationName: 'Metformin', dosageStrength: 500, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+1', instructions: 'After meal', startDate: new Date().toISOString().split('T')[0], durationDays: 30 },
    ],
    advice: ['Low sugar, low glycemic-index diet', 'Regular blood glucose monitoring', 'Daily exercise 30 min', 'Foot care — inspect daily'],
    tests: [{ testName: 'FBS / RBS', isPanel: false }, { testName: 'HbA1c', isPanel: false }],
    followUpDays: 30,
  },
  {
    templateId: 'sys-004',
    templateName: 'Acute Gastroenteritis',
    templateType: 'SYSTEM',
    diseaseCategory: 'Gastrointestinal',
    complaints: ['Loose stool', 'Vomiting', 'Abdominal pain', 'Nausea'],
    diagnoses: [{ code: 'A09', description: 'Gastroenteritis and colitis of infectious origin', isPrimary: true }],
    medications: [
      { medicationName: 'ORS (Oral Rehydration Salt)', dosageStrength: undefined, dosageUnit: '', dosageForm: 'SOLUTION', route: 'ORAL', frequency: 'As needed (PRN)', instructions: 'After each loose stool, dissolve 1 sachet in 250mL water', startDate: new Date().toISOString().split('T')[0], durationDays: 3 },
      { medicationName: 'Metronidazole', dosageStrength: 400, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+1+1', instructions: 'After meal', startDate: new Date().toISOString().split('T')[0], durationDays: 5 },
      { medicationName: 'Ondansetron', dosageStrength: 4, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+1', instructions: 'As needed for vomiting', startDate: new Date().toISOString().split('T')[0], durationDays: 3 },
    ],
    advice: ['Plenty of fluids', 'Light diet (khichdi/porridge/boiled rice)', 'Avoid oily and spicy food', 'Hand hygiene strictly'],
    followUpDays: 3,
  },
  {
    templateId: 'dis-001',
    templateName: 'UTI protocol (disease template)',
    templateType: 'DISEASE',
    diseaseCategory: 'Renal',
    complaints: ['Burning urination', 'Frequent urination', 'Lower abdominal pain'],
    diagnoses: [{ code: 'N39.0', description: 'Urinary tract infection, site not specified', isPrimary: true }],
    medications: [
      { medicationName: 'Nitrofurantoin', dosageStrength: 100, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+1', instructions: 'After meal', startDate: new Date().toISOString().split('T')[0], durationDays: 7 },
    ],
    advice: ['Drink 2–3 L water daily', 'Complete the full antibiotic course', 'Urinate after intercourse', 'Return if no improvement in 48h'],
    tests: [{ testName: 'Urine R/E (Routine Examination)', isPanel: false }, { testName: 'Urine C/S (Culture & Sensitivity)', isPanel: false }],
    followUpDays: 7,
  },
  {
    templateId: 'sys-005',
    templateName: 'UTI (Uncomplicated, Female)',
    templateType: 'SYSTEM',
    diseaseCategory: 'Renal',
    complaints: ['Burning urination', 'Frequent urination', 'Lower abdominal pain'],
    diagnoses: [{ code: 'N39.0', description: 'Urinary tract infection, site not specified', isPrimary: true }],
    medications: [
      { medicationName: 'Nitrofurantoin', dosageStrength: 100, dosageUnit: 'mg', dosageForm: 'TABLET', route: 'ORAL', frequency: '1+0+1', instructions: 'After meal', startDate: new Date().toISOString().split('T')[0], durationDays: 7 },
    ],
    advice: ['Drink 2–3 L water daily', 'Complete the full antibiotic course', 'Urinate after intercourse', 'Return if no improvement in 48h'],
    tests: [{ testName: 'Urine R/E (Routine Examination)', isPanel: false }, { testName: 'Urine C/S (Culture & Sensitivity)', isPanel: false }],
    followUpDays: 7,
  },
];

const EP_TEMPLATES_KEY = 'ep_prescription_templates';
/** localStorage key for `epConfigService` — use for `storage` event listeners across tabs. */
export const EP_DOCTOR_CONFIG_STORAGE_KEY = 'ep_doctor_config';
/** Dispatched on `window` when `epConfigService.save` runs (same tab; `storage` only fires in other tabs). */
export const EP_DOCTOR_CONFIG_UPDATED_EVENT = 'ep-doctor-config-updated';
const EP_CONFIG_KEY = EP_DOCTOR_CONFIG_STORAGE_KEY;

export const epTemplateService = {
  getAll(): PrescriptionTemplate[] {
    try {
      const raw = localStorage.getItem(EP_TEMPLATES_KEY);
      const doctorTemplates: PrescriptionTemplate[] = raw ? JSON.parse(raw) : [];
      return [...SYSTEM_PRESCRIPTION_TEMPLATES, ...doctorTemplates];
    } catch {
      return [...SYSTEM_PRESCRIPTION_TEMPLATES];
    }
  },
  getDoctorOwned(): PrescriptionTemplate[] {
    try {
      const raw = localStorage.getItem(EP_TEMPLATES_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  },
  save(template: PrescriptionTemplate): PrescriptionTemplate {
    const existing = this.getDoctorOwned();
    const idx = existing.findIndex(t => t.templateId === template.templateId);
    if (idx >= 0) {
      existing[idx] = template;
    } else {
      existing.push(template);
    }
    localStorage.setItem(EP_TEMPLATES_KEY, JSON.stringify(existing));
    void import('./epWorkspaceSync').then(m => m.scheduleEpWorkspacePush()).catch(() => {});
    return template;
  },
  delete(templateId: string): void {
    const existing = this.getDoctorOwned();
    localStorage.setItem(EP_TEMPLATES_KEY, JSON.stringify(existing.filter(t => t.templateId !== templateId)));
    void import('./epWorkspaceSync').then(m => m.scheduleEpWorkspacePush()).catch(() => {});
  },
  /** True if the template is stored in localStorage (doctor-created), not a bundled system/disease preset. */
  isDoctorOwned(templateId: string): boolean {
    return this.getDoctorOwned().some(t => t.templateId === templateId);
  },
};

export const epConfigService = {
  get(): DoctorEPConfig {
    try {
      const raw = localStorage.getItem(EP_CONFIG_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  },
  save(config: DoctorEPConfig): void {
    localStorage.setItem(EP_CONFIG_KEY, JSON.stringify(config));
    try {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent(EP_DOCTOR_CONFIG_UPDATED_EVENT));
      }
    } catch {
      // ignore
    }
    void import('./epWorkspaceSync').then(m => m.scheduleEpWorkspacePush()).catch(() => {});
  },
};

// ── EP Lookup Service ────────────────────────────────────────────────────────

export interface EpLookupMap {
  DOSAGE_FORM: string[];
  DISEASE_CATEGORY: string[];
  FREQUENCY: string[];
  INSTRUCTION: string[];
  REFERRAL: string[];
  COMPLAINT: string[];
  MEDICATION: string[];
  ADVICE: string[];
  TEST: string[];
  [key: string]: string[];
}

let _lookupCache: EpLookupMap | null = null;

export const epLookupService = {
  async fetchAll(): Promise<EpLookupMap> {
    if (_lookupCache) return _lookupCache;
    try {
      const res = await api.get<EpLookupMap>('/api/easy-prescription/lookups');
      _lookupCache = res.data;
      return _lookupCache;
    } catch {
      return {
        DOSAGE_FORM: [], DISEASE_CATEGORY: [], FREQUENCY: [],
        INSTRUCTION: [], REFERRAL: [], COMPLAINT: [],
        MEDICATION: [], ADVICE: [], TEST: [],
      };
    }
  },
  /** Invalidate the in-memory cache (e.g. after admin updates lookup data). */
  clearCache() {
    _lookupCache = null;
  },
};

const ADVICE_API_MAX_RAW_LINES = 128;
/** Matches backend EpAdviceCatalogService distinct-line budget after normalization. */
const ADVICE_API_MAX_DISTINCT_LINES = 64;
/** Matches backend EpAdviceCatalogService.MAX_VALUE_LENGTH */
const ADVICE_LINE_MAX_CHARS = 1000;
/** Matches backend EpAdviceCatalogService suggestion substring cap */
const ADVICE_QUERY_MAX_CHARS = 256;

function dedupeAdviceLinesForApi(lines: string[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const raw of lines.slice(0, ADVICE_API_MAX_RAW_LINES)) {
    const c = raw.trim().replace(/\s+/g, ' ').slice(0, ADVICE_LINE_MAX_CHARS);
    if (!c) continue;
    const key = c.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(c);
    if (out.length >= ADVICE_API_MAX_DISTINCT_LINES) break;
  }
  return out;
}

/** EP advice catalog: org-wide lookups + per-user frequency (see hospital-service EpAdviceController). */
export const epAdviceService = {
  async getSuggestions(params?: { query?: string; limit?: number }): Promise<string[]> {
    try {
      const rawLimit = params?.limit ?? 80;
      const limit = Math.min(200, Math.max(1, Number.isFinite(rawLimit) ? rawLimit : 80));
      const q = params?.query?.trim();
      const query =
        q && q.length > 0 ? q.slice(0, ADVICE_QUERY_MAX_CHARS) : undefined;
      const res = await api.get<string[]>('/api/easy-prescription/advice/suggestions', {
        params: {
          query,
          limit,
        },
      });
      if (!Array.isArray(res.data)) return [];
      return res.data.filter((s): s is string => typeof s === 'string' && s.trim().length > 0);
    } catch {
      return [];
    }
  },
  async ensure(lines: string[]): Promise<void> {
    const cleaned = dedupeAdviceLinesForApi(lines);
    if (cleaned.length === 0) return;
    await api.post('/api/easy-prescription/advice/ensure', { lines: cleaned });
  },
  async recordUsage(lines: string[]): Promise<void> {
    const cleaned = dedupeAdviceLinesForApi(lines);
    if (cleaned.length === 0) return;
    await api.post('/api/easy-prescription/advice/record-usage', { lines: cleaned });
  },
  async dismiss(lines: string[]): Promise<void> {
    const cleaned = dedupeAdviceLinesForApi(lines);
    if (cleaned.length === 0) return;
    await api.post('/api/easy-prescription/advice/dismiss', { lines: cleaned });
  },
};

export default hospitalService;
