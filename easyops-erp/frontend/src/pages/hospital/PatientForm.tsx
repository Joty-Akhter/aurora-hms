import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import hospitalService, {
  Patient,
  PatientRequest,
  DuplicatePatientResponse,
} from '../../services/hospitalService';
import { COUNTRY_OPTIONS } from '../../constants/countries';
import { ID_TYPE_OPTIONS } from '../../constants/idTypes';
import { BLOOD_GROUP_OPTIONS } from '../../constants/bloodGroups';
import { RELIGION_OPTIONS } from '../../constants/religions';
import { dobFromAge, dobFromAgeMonths, calculateAge, calculateAgeMonths } from '../../utils/ageUtils';
import {
  digitsOnlyPhone,
  isoDateLocal,
  isValidCalendarISODate,
  isValidOptionalEmail,
  isValidPhoneDigitLength,
  MIN_PHONE_DIGITS_FOR_UNIQUENESS,
  toDateInputValue,
} from '../../utils/formValidation';
import { openPatientIdentityCardPrintWindow } from '../../utils/patientIdentityCardPrint';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { isPhoneOnlyDuplicateCheck } from '../../utils/patientDuplicateUtils';
import './Hospital.css';

const createInitialPatientForm = (): PatientRequest => ({
  fullName: '',
  preferredName: '',
  dateOfBirth: '',
  gender: undefined,
  idType: '',
  idNo: '',
  maritalStatus: '',
  patientType: '',
  fatherName: '',
  motherName: '',
  spouseName: '',
  bloodGroup: '',
  religion: '',
  occupation: '',
  introducedBy: '',
  primaryAddressLine1: '',
  primaryAddressLine2: '',
  primaryCity: '',
  primaryState: '',
  primaryZip: '',
  primaryCountry: 'Bangladesh',
  mailingAddressLine1: '',
  mailingAddressLine2: '',
  mailingCity: '',
  mailingState: '',
  mailingZip: '',
  mailingCountry: '',
  primaryPhone: '',
  primaryPhoneType: '',
  secondaryPhone: '',
  secondaryPhoneType: '',
  primaryEmail: '',
  secondaryEmail: '',
  preferredContactMethod: undefined,
  consentTextMessaging: true,
  consentEmailCommunication: false,
  preferredLanguage: 'English',
  interpreterNeeded: false,
  specialNeeds: '',
});

const COUNTRIES_WITH_STATES = new Set([
  'United States', 'USA', 'US',
  'Canada', 'CA',
  'Australia', 'AU',
  'India', 'IN',
  'Brazil', 'BR',
  'Mexico', 'MX',
  'Germany', 'DE',
  'United Arab Emirates', 'UAE',
  'Malaysia', 'MY',
]);

const buildPatientPayload = (data: PatientRequest, excludePatientId?: string): PatientRequest => {
  const orgId =
    typeof localStorage !== 'undefined' ? localStorage.getItem('currentOrganizationId') : null;
  const { primaryEmail, secondaryEmail, idType, ...rest } = data;
  const normalizedIdType = idType === 'NATIONAL_ID' ? 'NID' : idType;
  return {
    ...rest,
    idType: normalizedIdType,
    ...(primaryEmail?.trim() ? { primaryEmail } : {}),
    ...(secondaryEmail?.trim() ? { secondaryEmail } : {}),
    ...(orgId ? { organizationId: orgId } : {}),
    ...(excludePatientId ? { excludePatientId } : {}),
  };
};

/** excludePatientId is only for duplicate-check; never send it on create/update. */
const buildPatientPersistPayload = (data: PatientRequest, excludePatientId?: string): PatientRequest => {
  const { excludePatientId: _omit, ...payload } = buildPatientPayload(data, excludePatientId);
  return payload;
};

const PatientForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  /** True when editing an existing patient (`/patients/:id/edit`). Not register or mistaken `new` slug. */
  const isEditPatient = Boolean(id && id !== 'new');
  const latestPatientIdRef = useRef<string | undefined>(undefined);
  latestPatientIdRef.current = id;

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const primaryEmailInputRef = useRef<HTMLInputElement | null>(null);
  const secondaryEmailInputRef = useRef<HTMLInputElement | null>(null);
  const [duplicateCheck, setDuplicateCheck] = useState<DuplicatePatientResponse | null>(null);
  const [showDuplicates, setShowDuplicates] = useState(false);
  const [duplicateCheckMessage, setDuplicateCheckMessage] = useState<string | null>(null);
  const [fullName, setFullName] = useState('');
  const [ageInput, setAgeInput] = useState('');
  const [ageUnit, setAgeUnit] = useState<'years' | 'months'>('years');
  /** Loaded from GET patient — controls identity-card actions on edit (backend requires ACTIVE + ISSUED). */
  const [editIdentityCardGate, setEditIdentityCardGate] = useState<{
    patientStatus: Patient['patientStatus'];
    identityCardStatus?: Patient['identityCardStatus'];
  } | null>(null);

  const [formData, setFormData] = useState<PatientRequest>(() => createInitialPatientForm());

  useEffect(() => {
    if (isEditPatient) {
      loadPatient();
    } else {
      setEditIdentityCardGate(null);
      setError(null);
      setDuplicateCheck(null);
      setShowDuplicates(false);
      setFullName('');
      setAgeInput('');
      setFormData(createInitialPatientForm());
      setLoading(false);
    }
  }, [id]);

  const loadPatient = async () => {
    const snapshotId = id;
    if (!snapshotId || snapshotId === 'new') return;
    try {
      setError(null);
      setLoading(true);
      setEditIdentityCardGate(null);

      const response = await hospitalService.getPatient(snapshotId);
      if (snapshotId !== latestPatientIdRef.current) return;

      const patient = response.data;

      setEditIdentityCardGate({
        patientStatus: patient.patientStatus,
        identityCardStatus: patient.identityCardStatus,
      });

      setFullName(patient.fullName?.trim() || '');
      const dobNorm = toDateInputValue(patient.dateOfBirth);
      const loadedAge = calculateAge(dobNorm || undefined);
      const loadedAgeMonths = calculateAgeMonths(dobNorm || undefined);
      if (loadedAge !== null && loadedAge < 1 && loadedAgeMonths !== null) {
        setAgeUnit('months');
        setAgeInput(String(loadedAgeMonths));
      } else {
        setAgeUnit('years');
        setAgeInput(loadedAge !== null ? String(loadedAge) : '');
      }
      setFormData({
        fullName: patient.fullName || '',
        preferredName: patient.preferredName || '',
        dateOfBirth: dobNorm,
        gender: patient.gender,
        idType: patient.idType === 'NATIONAL_ID' ? 'NID' : patient.idType || '',
        idNo: patient.idNo || '',
        maritalStatus: patient.maritalStatus || '',
        patientType: patient.patientType || '',
        fatherName: patient.fatherName || '',
        motherName: patient.motherName || '',
        spouseName: patient.spouseName || '',
        bloodGroup: patient.bloodGroup || '',
        religion: patient.religion || '',
        occupation: patient.occupation || '',
        introducedBy: patient.introducedBy || '',
        primaryAddressLine1: patient.primaryAddressLine1 || '',
        primaryAddressLine2: patient.primaryAddressLine2 || '',
        primaryCity: patient.primaryCity || '',
        primaryState: patient.primaryState || '',
        primaryZip: patient.primaryZip || '',
        primaryCountry: patient.primaryCountry || 'Bangladesh',
        mailingAddressLine1: patient.mailingAddressLine1 || '',
        mailingAddressLine2: patient.mailingAddressLine2 || '',
        mailingCity: patient.mailingCity || '',
        mailingState: patient.mailingState || '',
        mailingZip: patient.mailingZip || '',
        mailingCountry: patient.mailingCountry || '',
        primaryPhone: patient.primaryPhone || '',
        primaryPhoneType: patient.primaryPhoneType || '',
        secondaryPhone: patient.secondaryPhone || '',
        secondaryPhoneType: patient.secondaryPhoneType || '',
        primaryEmail: patient.primaryEmail || '',
        secondaryEmail: patient.secondaryEmail || '',
        preferredContactMethod: patient.preferredContactMethod,
        consentTextMessaging: patient.consentTextMessaging ?? true,
        consentEmailCommunication: patient.consentEmailCommunication || false,
        preferredLanguage: patient.preferredLanguage || 'English',
        interpreterNeeded: patient.interpreterNeeded || false,
        specialNeeds: patient.specialNeeds || '',
      });
    } catch (err: any) {
      console.error('Failed to load patient:', err);
      if (snapshotId !== latestPatientIdRef.current) return;
      setError(ehrApiErrorMessage(err, 'Failed to load patient'));
    } finally {
      if (snapshotId === latestPatientIdRef.current) {
        setLoading(false);
      }
    }
  };

  const sanitizePatientFormForSave = (data: PatientRequest, nameInput: string): PatientRequest => {
    const single = nameInput.trim().replace(/\s+/g, ' ');
    const idType = data.idType === 'NATIONAL_ID' ? 'NID' : data.idType;
    return {
      ...data,
      fullName: single,
      preferredName: data.preferredName?.trim() ?? '',
      idType: idType ?? '',
      primaryEmail: (data.primaryEmail ?? '').trim(),
      secondaryEmail: (data.secondaryEmail ?? '').trim(),
      primaryPhone: digitsOnlyPhone(data.primaryPhone ?? ''),
      secondaryPhone: digitsOnlyPhone(data.secondaryPhone ?? ''),
    };
  };

  const validatePatientDemographics = (data: PatientRequest): string | null => {
    if (!data.fullName?.trim()) return 'Name is required and cannot be only spaces.';
    if (!data.dateOfBirth?.trim()) return 'Date of birth is required.';
    if (!isValidCalendarISODate(data.dateOfBirth)) return 'Enter a valid date of birth.';
    if (data.dateOfBirth > isoDateLocal()) return 'Date of birth cannot be in the future.';
    if (!data.gender) return 'Gender is required.';
    if (!data.patientType?.trim()) return 'Patient type is required.';
    if (!data.fatherName?.trim()) return "Father's name is required.";
    if (!data.primaryAddressLine1?.trim()) return 'Present address (Address Line 1) is required.';
    if (!isValidPhoneDigitLength(data.primaryPhone ?? '')) {
      return `Mobile number is required (at least ${MIN_PHONE_DIGITS_FOR_UNIQUENESS} digits).`;
    }
    if (!isValidOptionalEmail(data.primaryEmail || '')) {
      return 'Please enter a valid primary email (include a domain with extension, e.g. name@example.com).';
    }
    if (!isValidOptionalEmail(data.secondaryEmail || '')) {
      return 'Please enter a valid secondary email (include a domain with extension, e.g. name@example.com).';
    }
    if (data.secondaryPhone && !isValidPhoneDigitLength(data.secondaryPhone)) {
      return `Secondary phone must be at least ${MIN_PHONE_DIGITS_FOR_UNIQUENESS} digits, or leave it empty.`;
    }
    return null;
  };

  const checkDuplicatesRequest = async (data: PatientRequest): Promise<DuplicatePatientResponse | null> => {
    const routeIdSnapshot = id;
    const sanitized = sanitizePatientFormForSave(data, fullName);
    const validationError = validatePatientDemographics(sanitized);
    if (validationError) {
      setError(validationError);
      return null;
    }
    setError(null);
    try {
      const payload = buildPatientPayload(sanitized, isEditPatient ? id : undefined);
      const response = await hospitalService.checkDuplicates(payload);
      if (routeIdSnapshot !== latestPatientIdRef.current) {
        return null;
      }
      setDuplicateCheck(response.data);
      setShowDuplicates(response.data.hasDuplicates);
      if (response.data.hasDuplicates) {
        setDuplicateCheckMessage(null);
      } else {
        setDuplicateCheckMessage('No duplicate patients found. You can proceed with registration.');
      }
      return response.data;
    } catch (err: any) {
      console.error('Failed to check duplicates:', err);
      alert(ehrApiErrorMessage(err, 'Failed to check for duplicates'));
      return null;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmed = sanitizePatientFormForSave(formData, fullName);
    if (!isValidOptionalEmail(trimmed.primaryEmail || '')) {
      setError('Invalid Email');
      primaryEmailInputRef.current?.focus();
      return;
    }
    if (!isValidOptionalEmail(trimmed.secondaryEmail || '')) {
      setError('Invalid Email');
      secondaryEmailInputRef.current?.focus();
      return;
    }
    const validationError = validatePatientDemographics(trimmed);
    if (validationError) {
      setError(validationError);
      return;
    }
    setFormData(trimmed);

    const userStr = localStorage.getItem('user');
    let userId: string | undefined;
    if (userStr) {
      try {
        userId = (JSON.parse(userStr) as { id?: string }).id;
      } catch {
        userId = undefined;
      }
    }
    if (isEditPatient && !userId) {
      setError('Your session is missing user identity. Please sign out and sign in again, then retry saving.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const payload = buildPatientPersistPayload(trimmed, isEditPatient ? id : undefined);

      let dupForSubmit = duplicateCheck;
      if (!isEditPatient) {
        const dupRes = await hospitalService.checkDuplicates(buildPatientPayload(trimmed));
        dupForSubmit = dupRes.data;
        setDuplicateCheck(dupForSubmit);
        setShowDuplicates(!!dupForSubmit?.hasDuplicates);
      }

      if (!isEditPatient && isPhoneOnlyDuplicateCheck(dupForSubmit)) {
        const proceed = window.confirm(
          'Other patients already use this mobile number (e.g. family members). Register this patient anyway?',
        );
        if (!proceed) {
          setLoading(false);
          return;
        }
      }
      if (isEditPatient) {
        try {
          await hospitalService.updatePatient(id!, payload);
        } catch (innerErr: any) {
          if (innerErr.response?.status === 409 && innerErr.response?.data) {
            const dup = innerErr.response.data as DuplicatePatientResponse;
            setDuplicateCheck(dup);
            setShowDuplicates(true);
            setLoading(false);
            setError('Potential duplicate patients found. Review the matches below.');
            return;
          }
          throw innerErr;
        }
      } else {
        let created: Patient | undefined;
        try {
          const res = await hospitalService.createPatient(payload);
          created = res.data;
        } catch (innerErr: any) {
          if (innerErr.response?.status === 409 && innerErr.response?.data) {
            const dup = innerErr.response.data as DuplicatePatientResponse;
            setDuplicateCheck(dup);
            setShowDuplicates(true);
            setLoading(false);
            const proceed = window.confirm(
              'Potential duplicate patients found. Do you want to proceed with registration?'
            );
            if (!proceed) {
              return;
            }
            setLoading(true);
            const res2 = await hospitalService.createPatient(payload, { acknowledgeDuplicate: true });
            created = res2.data;
          } else {
            throw innerErr;
          }
        }
        if (created) {
          const lines: string[] = [`MRN: ${created.mrn}`];
          if (created.identityCardStatus === 'ISSUED' && created.identityCardNumber) {
            lines.push(`Patient identity card issued. Card number: ${created.identityCardNumber}`);
            lines.push(
              'You can print now, or later from Patient Overview or Edit Patient (Print / reprint identity card).'
            );
          } else if (created.identityCardStatus === 'FAILED') {
            lines.push(
              `Patient identity card could not be issued automatically: ${created.identityCardMessage || 'Unknown error'}. You can issue a card later from Hospital → Cards.`
            );
          } else if (created.identityCardStatus === 'SKIPPED' || created.identityCardStatus === 'DISABLED') {
            lines.push(created.identityCardMessage || 'Identity card auto-issue was skipped.');
          }
          window.alert(lines.join('\n\n'));

          if (created.identityCardStatus === 'ISSUED' && created.patientId) {
            const shouldPrint = window.confirm('Print patient identity card now?');
            if (shouldPrint) {
              try {
                const printRes = await hospitalService.reprintPatientIdentityCard(created.patientId);
                openPatientIdentityCardPrintWindow(printRes.data);
              } catch (printErr: unknown) {
                window.alert(`Unable to print identity card: ${ehrApiErrorMessage(printErr, 'Unknown error')}`);
              }
            }
          }
        }
      }

      navigate('/hospital/patients');
    } catch (err: any) {
      console.error('Failed to save patient:', err);
      setError(ehrApiErrorMessage(err, 'Failed to save patient'));
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field: keyof PatientRequest, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (field === 'primaryPhone' && duplicateCheck) {
      setDuplicateCheck(null);
      setShowDuplicates(false);
    }
  };

  if (loading && isEditPatient) {
    return <div className="loading">Loading patient...</div>;
  }

  const todayIso = isoDateLocal();

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>{isEditPatient ? 'Edit Patient' : 'Register New Patient'}</h1>
          <p>{isEditPatient ? 'Update patient demographic information' : 'Register a new patient in the system'}</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate('/hospital/patients')}>
          Cancel
        </button>
      </div>

      {isEditPatient &&
        editIdentityCardGate &&
        editIdentityCardGate.patientStatus === 'ACTIVE' &&
        editIdentityCardGate.identityCardStatus === 'ISSUED' && (
          <div className="form-actions" style={{ marginBottom: '12px' }}>
            <button
              type="button"
              className="btn-secondary"
              onClick={async () => {
                try {
                  const res = await hospitalService.reprintPatientIdentityCard(id!);
                  openPatientIdentityCardPrintWindow(res.data);
                } catch (err: any) {
                  window.alert(ehrApiErrorMessage(err, 'Failed to reprint patient identity card'));
                }
              }}
            >
              Print / reprint identity card
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={async () => {
                const reason = window.prompt('Replacement reason (e.g. LOST, DAMAGED, OTHER):', 'LOST');
                if (!reason || !reason.trim()) {
                  return;
                }
                try {
                  const replaceRes = await hospitalService.replacePatientIdentityCard(id!, reason.trim());
                  await loadPatient();
                  const printRes = await hospitalService.reprintPatientIdentityCard(id!);
                  openPatientIdentityCardPrintWindow(printRes.data);
                  window.alert(
                    `Identity card replaced.\nNew card number: ${replaceRes.data.cardNumber}\nReason: ${replaceRes.data.reason}`
                  );
                } catch (err: any) {
                  window.alert(ehrApiErrorMessage(err, 'Failed to replace patient identity card'));
                }
              }}
            >
              Replace Identity Card
            </button>
          </div>
        )}

      {error && <div className="error-message">{error}</div>}

      {showDuplicates && duplicateCheck && (
        <div className="info-card" style={{ background: '#fef3c7', borderLeft: '4px solid #f59e0b' }}>
          <h3 style={{ color: '#92400e', marginBottom: '12px' }}>
            ⚠️ Potential duplicate patients found
          </h3>
          <p style={{ margin: '0 0 12px', color: '#92400e', fontSize: '14px' }}>
            Shared mobile numbers are allowed for family members. Review matches before saving.
          </p>
          <ul style={{ marginLeft: '20px' }}>
            {duplicateCheck.matches.map((match, idx) => (
              <li key={idx} style={{ marginBottom: '8px' }}>
                <strong>{match.fullName || '—'}</strong> (MRN: {match.mrn}) - {match.matchReason}
                <button
                  className="btn-link"
                  onClick={() => navigate(`/hospital/patients/${match.patientId}`)}
                  style={{ marginLeft: '12px' }}
                >
                  View
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-container">
          {/* Personal Information */}
          <div className="form-section">
            <h2 className="form-section-title">Personal Information</h2>
            <div className="form-grid">
              <div className="form-group">
                <label>Name *</label>
                <input
                  type="text"
                  required
                  autoComplete="name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value.replace(/^\s+/, ''))}
                  onBlur={(e) => setFullName(e.target.value.trim())}
                  placeholder="Given and family name (e.g. Jane Marie Smith)"
                />
              </div>
              <div className="form-group">
                <label>Preferred Name</label>
                <input
                  type="text"
                  value={formData.preferredName}
                  onChange={(e) => handleChange('preferredName', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label htmlFor="patient-age-input">Age</label>
                <div className="age-inline">
                  <input
                    id="patient-age-input"
                    className="input-age-plain"
                    type="number"
                    min="0"
                    max={ageUnit === 'years' ? 150 : 1800}
                    placeholder={ageUnit === 'years' ? 'Years' : 'Months'}
                    value={ageInput}
                    onChange={(e) => {
                      const val = e.target.value;
                      setAgeInput(val);
                      if (val === '') return;
                      if (isNaN(Number(val))) return;
                      const n = Math.floor(Number(val));
                      if (ageUnit === 'years') {
                        const clamped = Math.min(150, Math.max(0, n));
                        handleChange('dateOfBirth', dobFromAge(clamped));
                      } else {
                        const clamped = Math.min(1800, Math.max(0, n));
                        handleChange('dateOfBirth', dobFromAgeMonths(clamped));
                      }
                    }}
                  />
                  <select
                    id="patient-age-unit"
                    className="age-unit-select"
                    aria-label="Age unit"
                    value={ageUnit}
                    onChange={(e) => {
                      setAgeUnit(e.target.value as 'years' | 'months');
                      setAgeInput('');
                    }}
                  >
                    <option value="years">Years</option>
                    <option value="months">Months</option>
                  </select>
                </div>
                <p className="age-hint">
                  {ageUnit === 'months'
                    ? 'For infants under 1 year, enter age in months (e.g. 6). Date of birth updates automatically.'
                    : 'Enter age in years, or switch to Months for patients under 1 year.'}
                </p>
              </div>
              <div className="form-group">
                <label>Date of Birth *</label>
                <input
                  type="date"
                  required
                  max={todayIso}
                  value={formData.dateOfBirth}
                  onChange={(e) => {
                    e.target.setCustomValidity('');
                    handleChange('dateOfBirth', e.target.value);
                    const months = calculateAgeMonths(e.target.value);
                    const age = calculateAge(e.target.value);
                    if (age !== null && age < 1 && months !== null) {
                      setAgeUnit('months');
                      setAgeInput(String(months));
                    } else if (ageUnit === 'months') {
                      setAgeInput(months !== null ? String(months) : '');
                    } else {
                      setAgeInput(age !== null ? String(age) : '');
                    }
                  }}
                  onInvalid={(e) => (e.target as HTMLInputElement).setCustomValidity('Wrong input')}
                />
              </div>
              <div className="form-group">
                <label>Patient Type *</label>
                <select
                  required
                  value={formData.patientType || ''}
                  onChange={(e) => handleChange('patientType', e.target.value)}
                >
                  <option value="">Select patient type</option>
                  <option value="General">General</option>
                  <option value="Corporate">Corporate</option>
                  <option value="Insurance">Insurance</option>
                  <option value="Staff">Staff</option>
                  <option value="VIP">VIP</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="form-group">
                <label>Gender *</label>
                <select
                  required
                  value={formData.gender || ''}
                  onChange={(e) => handleChange('gender', e.target.value || undefined)}
                >
                  <option value="">Select Gender</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                  <option value="Prefer_not_to_answer">Prefer not to answer</option>
                </select>
              </div>
              <div className="form-group">
                <label>ID type</label>
                <select
                  value={formData.idType || ''}
                  onChange={(e) => handleChange('idType', e.target.value)}
                >
                  {ID_TYPE_OPTIONS.filter((opt) => opt.value !== 'NATIONAL_ID').map((opt) => (
                    <option key={opt.value || '_empty'} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                  {formData.idType &&
                    formData.idType !== 'NATIONAL_ID' &&
                    !ID_TYPE_OPTIONS.some((o) => o.value === formData.idType) && (
                      <option value={formData.idType}>{formData.idType}</option>
                    )}
                </select>
              </div>
              <div className="form-group">
                <label>ID no.</label>
                <input
                  type="text"
                  value={formData.idNo}
                  onChange={(e) => handleChange('idNo', e.target.value.replace(/[^a-zA-Z0-9\-/\\]/g, ''))}
                  placeholder="Document number"
                  autoComplete="off"
                />
              </div>
              <div className="form-group">
                <label>Marital Status</label>
                <select
                  value={formData.maritalStatus}
                  onChange={(e) => handleChange('maritalStatus', e.target.value)}
                >
                  <option value="">Select Status</option>
                  <option value="Single">Single</option>
                  <option value="Married">Married</option>
                  <option value="Divorced">Divorced</option>
                  <option value="Widowed">Widowed</option>
                  <option value="Separated">Separated</option>
                </select>
              </div>
              <div className="form-group">
                <label>Father&apos;s name *</label>
                <input
                  type="text"
                  required
                  value={formData.fatherName}
                  onChange={(e) => handleChange('fatherName', e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div className="form-group">
                <label>Mother&apos;s name</label>
                <input
                  type="text"
                  value={formData.motherName}
                  onChange={(e) => handleChange('motherName', e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div className="form-group">
                <label>Spouse&apos;s name</label>
                <input
                  type="text"
                  value={formData.spouseName}
                  onChange={(e) => handleChange('spouseName', e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div className="form-group">
                <label>Blood group</label>
                <select
                  value={formData.bloodGroup || ''}
                  onChange={(e) => handleChange('bloodGroup', e.target.value)}
                >
                  {BLOOD_GROUP_OPTIONS.map((opt) => (
                    <option key={opt.value || '_empty'} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                  {formData.bloodGroup &&
                    !BLOOD_GROUP_OPTIONS.some((o) => o.value === formData.bloodGroup) && (
                      <option value={formData.bloodGroup}>{formData.bloodGroup}</option>
                    )}
                </select>
              </div>
              <div className="form-group">
                <label>Religion</label>
                <select
                  value={formData.religion || ''}
                  onChange={(e) => handleChange('religion', e.target.value)}
                >
                  {RELIGION_OPTIONS.map((opt) => (
                    <option key={opt.value || '_empty'} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                  {formData.religion &&
                    !RELIGION_OPTIONS.some((o) => o.value === formData.religion) && (
                      <option value={formData.religion}>{formData.religion}</option>
                    )}
                </select>
              </div>
              <div className="form-group">
                <label>Occupation</label>
                <input
                  type="text"
                  value={formData.occupation}
                  onChange={(e) => handleChange('occupation', e.target.value)}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Introduced by</label>
                <input
                  type="text"
                  value={formData.introducedBy}
                  onChange={(e) => handleChange('introducedBy', e.target.value)}
                  placeholder="Referrer or person who introduced the patient"
                />
              </div>
            </div>
          </div>

          {/* Contact Information */}
          <div className="form-section">
            <h2 className="form-section-title">Contact Information</h2>
            <div className="form-grid">
              <div className="form-group">
                <label>Mobile Number *</label>
                <input
                  type="tel"
                  inputMode="numeric"
                  required
                  value={formData.primaryPhone}
                  onChange={(e) => handleChange('primaryPhone', digitsOnlyPhone(e.target.value))}
                  onBlur={async () => {
                    const trimmed = sanitizePatientFormForSave(formData, fullName);
                    if (!isValidPhoneDigitLength(trimmed.primaryPhone ?? '')) return;
                    if (!trimmed.fullName?.trim() || !trimmed.dateOfBirth) return;
                    await checkDuplicatesRequest(trimmed);
                  }}
                />
              </div>
              <div className="form-group">
                <label>Primary Phone Type</label>
                <select
                  value={formData.primaryPhoneType}
                  onChange={(e) => handleChange('primaryPhoneType', e.target.value)}
                >
                  <option value="">Select Type</option>
                  <option value="Mobile">Mobile</option>
                  <option value="Home">Home</option>
                  <option value="Work">Work</option>
                </select>
              </div>
              <div className="form-group">
                <label>Secondary Phone</label>
                <input
                  type="tel"
                  inputMode="numeric"
                  value={formData.secondaryPhone}
                  onChange={(e) => handleChange('secondaryPhone', digitsOnlyPhone(e.target.value))}
                />
              </div>
              <div className="form-group">
                <label>Primary Email</label>
                <input
                  ref={primaryEmailInputRef}
                  type="text"
                  autoComplete="email"
                  value={formData.primaryEmail}
                  onChange={(e) => handleChange('primaryEmail', e.target.value)}
                  placeholder="name@example.com"
                />
              </div>
              <div className="form-group">
                <label>Secondary Email</label>
                <input
                  ref={secondaryEmailInputRef}
                  type="text"
                  autoComplete="email"
                  value={formData.secondaryEmail}
                  onChange={(e) => handleChange('secondaryEmail', e.target.value)}
                  placeholder="name@example.com"
                />
              </div>
              <div className="form-group">
                <label>Preferred Contact Method</label>
                <select
                  value={formData.preferredContactMethod || ''}
                  onChange={(e) => handleChange('preferredContactMethod', e.target.value || undefined)}
                >
                  <option value="">Select Method</option>
                  <option value="Phone">Phone</option>
                  <option value="Email">Email</option>
                  <option value="Mail">Mail</option>
                  <option value="Text_Message">Text Message</option>
                </select>
              </div>
            </div>
          </div>

          {/* Address Information */}
          <div className="form-section">
            <h2 className="form-section-title">Primary Address</h2>
            <div className="form-grid">
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Present Address (Line 1) *</label>
                <input
                  type="text"
                  required
                  value={formData.primaryAddressLine1}
                  onChange={(e) => handleChange('primaryAddressLine1', e.target.value)}
                />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Address Line 2</label>
                <input
                  type="text"
                  value={formData.primaryAddressLine2}
                  onChange={(e) => handleChange('primaryAddressLine2', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>City</label>
                <input
                  type="text"
                  value={formData.primaryCity}
                  onChange={(e) => handleChange('primaryCity', e.target.value)}
                />
              </div>
              {COUNTRIES_WITH_STATES.has(formData.primaryCountry || '') && (
                <div className="form-group">
                  <label>State</label>
                  <input
                    type="text"
                    value={formData.primaryState}
                    onChange={(e) => handleChange('primaryState', e.target.value)}
                  />
                </div>
              )}
              <div className="form-group">
                <label>ZIP Code</label>
                <input
                  type="text"
                  value={formData.primaryZip}
                  onChange={(e) => handleChange('primaryZip', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>Country</label>
                <Autocomplete
                  freeSolo
                  options={[...COUNTRY_OPTIONS]}
                  value={formData.primaryCountry || null}
                  onChange={(_, newValue) => handleChange('primaryCountry', newValue ?? '')}
                  onInputChange={(_, newInputValue) => handleChange('primaryCountry', newInputValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder="Search or select country"
                      size="small"
                      variant="outlined"
                    />
                  )}
                />
              </div>
            </div>
          </div>

          {/* Additional Information */}
          <div className="form-section">
            <h2 className="form-section-title">Additional Information</h2>
            <div className="form-grid">
              <div className="form-group">
                <label>Preferred Language</label>
                <input
                  type="text"
                  value={formData.preferredLanguage}
                  onChange={(e) => handleChange('preferredLanguage', e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={formData.interpreterNeeded}
                    onChange={(e) => handleChange('interpreterNeeded', e.target.checked)}
                  />
                  Interpreter Needed
                </label>
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Special Needs</label>
                <textarea
                  value={formData.specialNeeds}
                  onChange={(e) => handleChange('specialNeeds', e.target.value)}
                />
              </div>
            </div>
          </div>

          {duplicateCheckMessage && !showDuplicates && (
            <div className="success-message" style={{ marginBottom: '12px' }}>
              {duplicateCheckMessage}
            </div>
          )}

          <div className="form-actions">
            {!isEditPatient && (
              <button
                type="button"
                className="btn-secondary"
                onClick={async () => {
                  const trimmed = sanitizePatientFormForSave(formData, fullName);
                  const validationError = validatePatientDemographics(trimmed);
                  if (validationError) {
                    setError(validationError);
                    return;
                  }
                  setFormData(trimmed);
                  await checkDuplicatesRequest(trimmed);
                }}
              >
                Check for Duplicates
              </button>
            )}
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : isEditPatient ? 'Update Patient' : 'Register Patient'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default PatientForm;
