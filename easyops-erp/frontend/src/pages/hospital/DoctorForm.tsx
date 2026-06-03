import React, { useEffect, useRef, useState } from 'react';
import { addDays, format, isBefore, parseISO } from 'date-fns';
import { useNavigate, useParams } from 'react-router-dom';
import hospitalService, {
  DoctorRequest,
  DoctorDepartment,
  DoctorAppointmentSlot,
} from '../../services/hospitalService';
import { COUNTRY_OPTIONS } from '../../constants/countries';
import {
  BANGLADESH_DISTRICTS,
  BANGLADESH_THANAS_BY_DISTRICT,
} from '../../constants/bangladeshLocations';
import {
  digitsOnlyPhone,
  finiteNonNegativeOrUndefined,
  finitePositiveIntOrUndefined,
  blockNegativeNumberInput,
  isoDateLocal,
  isValidCalendarISODate,
  isValidOptionalEmail,
  parseOptionalPositiveInt,
  parseRequiredPositiveInt,
  sanitizeNonNegativeDecimalString,
  toDateInputValue,
} from '../../utils/formValidation';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { validateUniqueAppointmentSlots } from '../../utils/appointmentSlotUtils';
import './Hospital.css';

const DEFAULT_COUNTRY = 'Bangladesh';

const DAYS_OF_WEEK = [
  { key: 'saturday',  label: 'Sat' },
  { key: 'sunday',    label: 'Sun' },
  { key: 'monday',    label: 'Mon' },
  { key: 'tuesday',   label: 'Tue' },
  { key: 'wednesday', label: 'Wed' },
  { key: 'thursday',  label: 'Thu' },
  { key: 'friday',    label: 'Fri' },
];

// Default: Sat–Thu active, Fri off
const DEFAULT_ACTIVE_DAYS = ['saturday', 'sunday', 'monday', 'tuesday', 'wednesday', 'thursday'];

const emptySlot = (): DoctorAppointmentSlot => ({
  startTime: '09:00',
  endTime: '17:00',
  days: [...DEFAULT_ACTIVE_DAYS],
  maxPatients: 10,
});

const createInitialDoctorForm = (): DoctorRequest => ({
  doctorName: '',
  departmentId: '',
  doctorType: 'CONSULTANT',
  indoorOutdoorStatus: 'OUTDOOR',
  degree: '',
  speciality: '',
  gender: '',
  birthDate: '',
  registrationDate: isoDateLocal(),
  bmdcRegistrationNumber: '',
  phoneNumber: '',
  email: '',
  presentAddress: '',
  district: '',
  thana: '',
  area: '',
  chamberRoom: '',
  visitFeeNew: undefined,
  visitFeeOld: undefined,
  takeCommission: false,
  patientsPerDay: undefined,
  serialStartFrom: 1,
  numberOfDaysCanAppointment: undefined,
  numberOfAppointmentsFromWeb: undefined,
  numberOfAppointmentsFromMobile: undefined,
  appointmentsFromWeb: false,
  appointmentsFromMobile: false,
  slotsPerDay: 1,
  smsEnabled: false,
  prescriptionStatus: 'ACTIVE',
  availabilityStatus: 'AVAILABLE',
  isActive: true,
  createLinkedUser: true,
});

function isBangladeshCountry(country: string): boolean {
  const normalized = country.trim().toLowerCase();
  return normalized === 'bangladesh' || normalized === 'bd';
}

const DoctorForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditDoctor = Boolean(id && id !== 'new');
  const latestDoctorIdRef = useRef<string | undefined>(undefined);
  latestDoctorIdRef.current = id;

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const emailInputRef = useRef<HTMLInputElement | null>(null);
  const [departments, setDepartments] = useState<DoctorDepartment[]>([]);
  const [contactCountry, setContactCountry] = useState(DEFAULT_COUNTRY);
  const [appointmentSlots, setAppointmentSlots] = useState<DoctorAppointmentSlot[]>([emptySlot()]);
  const [offDays, setOffDays] = useState<string[]>([]);
  const [offDayInput, setOffDayInput] = useState('');
  const [offDayRangeFrom, setOffDayRangeFrom] = useState('');
  const [offDayRangeTo, setOffDayRangeTo] = useState('');

  const [formData, setFormData] = useState<DoctorRequest>(() => createInitialDoctorForm());
  /** When editing: doctor already has a users.users login linked via hospital.doctors.linked_user_id */
  const [hasPortalLogin, setHasPortalLogin] = useState(false);

  useEffect(() => {
    loadDepartments();
    if (isEditDoctor) {
      loadDoctor();
    } else {
      setError(null);
      setHasPortalLogin(false);
      setFormData(createInitialDoctorForm());
      setAppointmentSlots([emptySlot()]);
      setOffDays([]);
      setOffDayInput('');
      setContactCountry(DEFAULT_COUNTRY);
      setLoading(false);
    }
  }, [id]);

  // Keep slot row count in sync with slotsPerDay
  useEffect(() => {
    const n = formData.slotsPerDay ?? 1;
    setAppointmentSlots(prev => {
      if (prev.length === n) return prev;
      if (prev.length < n) return [...prev, ...Array.from({ length: n - prev.length }, emptySlot)];
      return prev.slice(0, n);
    });
  }, [formData.slotsPerDay]);

  const loadDepartments = async () => {
    try {
      const response = await hospitalService.getActiveDoctorDepartments();
      setDepartments(response.data);
    } catch (err: any) {
      console.error('Failed to load departments:', err);
    }
  };

  const loadDoctor = async () => {
    const snapshotId = id;
    if (!snapshotId || snapshotId === 'new') return;
    try {
      setError(null);
      setLoading(true);
      const response = await hospitalService.getDoctor(snapshotId);
      if (snapshotId !== latestDoctorIdRef.current) return;

      const doctor = response.data;
      setHasPortalLogin(Boolean(doctor.linkedUserId));
      const slotRows = doctor.slotsPerDay ?? 1;
      const nextSlots =
        doctor.appointmentSlots && doctor.appointmentSlots.length > 0
          ? doctor.appointmentSlots
          : Array.from({ length: slotRows }, () => emptySlot());

      setFormData({
        doctorName: doctor.doctorName,
        departmentId: doctor.departmentId,
        doctorType: doctor.doctorType,
        indoorOutdoorStatus: doctor.indoorOutdoorStatus,
        degree: doctor.degree || '',
        speciality: doctor.speciality || '',
        gender: doctor.gender || '',
        birthDate: toDateInputValue(doctor.birthDate || ''),
        registrationDate: toDateInputValue(doctor.registrationDate) || isoDateLocal(),
        bmdcRegistrationNumber: doctor.bmdcRegistrationNumber || '',
        phoneNumber: doctor.phoneNumber || '',
        email: (doctor.email || '').trim(),
        presentAddress: doctor.presentAddress || '',
        district: doctor.district || '',
        thana: doctor.thana || '',
        area: doctor.area || '',
        chamberRoom: doctor.chamberRoom || '',
        visitFeeNew: finiteNonNegativeOrUndefined(doctor.visitFeeNew ?? undefined),
        visitFeeOld: finiteNonNegativeOrUndefined(doctor.visitFeeOld ?? undefined),
        takeCommission: doctor.takeCommission || false,
        patientsPerDay: doctor.patientsPerDay,
        serialStartFrom: doctor.serialStartFrom || 1,
        numberOfDaysCanAppointment: doctor.numberOfDaysCanAppointment,
        numberOfAppointmentsFromWeb: doctor.numberOfAppointmentsFromWeb,
        numberOfAppointmentsFromMobile: doctor.numberOfAppointmentsFromMobile,
        appointmentsFromWeb: doctor.appointmentsFromWeb || false,
        appointmentsFromMobile: doctor.appointmentsFromMobile || false,
        slotsPerDay: doctor.slotsPerDay ?? 1,
        smsEnabled: doctor.smsEnabled || false,
        prescriptionStatus: doctor.prescriptionStatus,
        availabilityStatus: doctor.availabilityStatus,
        isActive: doctor.isActive,
        createLinkedUser: false,
      });
      setAppointmentSlots(nextSlots);
      setOffDays(doctor.offDays ?? []);
      if (doctor.district && !BANGLADESH_DISTRICTS.includes(doctor.district)) {
        setContactCountry('Other');
      } else {
        setContactCountry(DEFAULT_COUNTRY);
      }
    } catch (err: any) {
      console.error('Failed to load doctor:', err);
      if (snapshotId !== latestDoctorIdRef.current) return;
      setError(ehrApiErrorMessage(err, 'Failed to load doctor'));
    } finally {
      if (snapshotId === latestDoctorIdRef.current) {
        setLoading(false);
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const todayIso = isoDateLocal();
    if (formData.birthDate?.trim()) {
      if (!isValidCalendarISODate(formData.birthDate)) {
        setError('Enter a valid birth date.');
        return;
      }
      if (formData.birthDate > todayIso) {
        setError('Birth date cannot be in the future.');
        return;
      }
    }
    const reg = (formData.registrationDate ?? '').trim();
    if (!reg) {
      setError('Registration date is required.');
      return;
    }
    if (!isValidCalendarISODate(reg)) {
      setError('Enter a valid registration date.');
      return;
    }
    if (reg > todayIso) {
      setError('Registration date cannot be in the future.');
      return;
    }
    const emailTrimmed = (formData.email ?? '').trim();
    if (!isValidOptionalEmail(emailTrimmed)) {
      setError('Invalid Email');
      emailInputRef.current?.focus();
      return;
    }
    const savingInactive = isEditDoctor && formData.isActive === false;
    if (!savingInactive) {
      const degreeTrimmed = (formData.degree ?? '').trim();
      if (!degreeTrimmed) {
        setError('Degree is required.');
        return;
      }
      const bmdcTrimmed = (formData.bmdcRegistrationNumber ?? '').trim();
      if (!bmdcTrimmed) {
        setError('BMDC registration number is required.');
        return;
      }
    }
    const degreeTrimmed = (formData.degree ?? '').trim();
    const bmdcTrimmed = (formData.bmdcRegistrationNumber ?? '').trim();
    const slotDupError = validateUniqueAppointmentSlots(appointmentSlots);
    if (slotDupError) {
      setError(slotDupError);
      return;
    }
    try {
      setLoading(true);
      setError(null);

      const slotCount = Math.max(1, finitePositiveIntOrUndefined(formData.slotsPerDay, 1) ?? 1);
      let slotsPayload = appointmentSlots.slice(0, slotCount);
      while (slotsPayload.length < slotCount) {
        slotsPayload = [...slotsPayload, emptySlot()];
      }
      slotsPayload = slotsPayload.map((s) => ({
        ...s,
        maxPatients: s.maxPatients != null && s.maxPatients > 0 ? s.maxPatients : 10,
      }));
      const offDaysPayload = offDays.filter(d => isValidCalendarISODate(d));

      const payload: DoctorRequest = {
        ...formData,
        degree: degreeTrimmed || undefined,
        bmdcRegistrationNumber: bmdcTrimmed || undefined,
        email: emailTrimmed,
        phoneNumber: digitsOnlyPhone(formData.phoneNumber ?? ''),
        visitFeeNew: finiteNonNegativeOrUndefined(formData.visitFeeNew ?? undefined),
        visitFeeOld: finiteNonNegativeOrUndefined(formData.visitFeeOld ?? undefined),
        patientsPerDay: finitePositiveIntOrUndefined(formData.patientsPerDay),
        numberOfDaysCanAppointment: finitePositiveIntOrUndefined(formData.numberOfDaysCanAppointment),
        serialStartFrom: finitePositiveIntOrUndefined(formData.serialStartFrom, 1) ?? 1,
        slotsPerDay: slotCount,
        appointmentSlots: slotsPayload,
        offDays: offDaysPayload,
      };

      if (isEditDoctor) {
        await hospitalService.updateDoctor(id!, payload);
      } else {
        await hospitalService.createDoctor(payload);
      }

      navigate('/hospital/doctors');
    } catch (err: any) {
      console.error('Failed to save doctor:', err);
      setError(ehrApiErrorMessage(err, 'Failed to save doctor'));
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field: keyof DoctorRequest, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  // ── Appointment slot helpers ──
  const updateSlot = (idx: number, field: keyof DoctorAppointmentSlot, value: string | number | string[] | undefined) => {
    setAppointmentSlots(prev => prev.map((s, i) => (i === idx ? { ...s, [field]: value } : s)));
  };

  const toggleSlotDay = (idx: number, dayKey: string, checked: boolean) => {
    setAppointmentSlots(prev => prev.map((s, i) => {
      if (i !== idx) return s;
      const days = checked ? [...s.days, dayKey] : s.days.filter(d => d !== dayKey);
      return { ...s, days };
    }));
  };

  const addAppointmentSlot = () => {
    const nextLen = appointmentSlots.length + 1;
    setAppointmentSlots((prev) => [...prev, emptySlot()]);
    handleChange('slotsPerDay', nextLen);
  };

  const removeLastAppointmentSlot = () => {
    if (appointmentSlots.length <= 1) return;
    const nextLen = appointmentSlots.length - 1;
    setAppointmentSlots((prev) => prev.slice(0, -1));
    handleChange('slotsPerDay', nextLen);
  };

  // ── Off days helpers ──
  const addOffDay = () => {
    if (!offDayInput) return;
    if (!offDays.includes(offDayInput)) {
      setOffDays(prev => [...prev, offDayInput].sort());
    }
    setOffDayInput('');
  };

  const removeOffDay = (date: string) => {
    setOffDays(prev => prev.filter(d => d !== date));
  };

  const addOffDayRange = () => {
    if (!offDayRangeFrom || !offDayRangeTo) return;
    const start = offDayRangeFrom <= offDayRangeTo ? offDayRangeFrom : offDayRangeTo;
    const end = offDayRangeFrom <= offDayRangeTo ? offDayRangeTo : offDayRangeFrom;
    const expanded: string[] = [];
    let cursor = parseISO(start + 'T12:00:00');
    const endDt = parseISO(end + 'T12:00:00');
    while (!isBefore(endDt, cursor)) {
      expanded.push(format(cursor, 'yyyy-MM-dd'));
      cursor = addDays(cursor, 1);
    }
    setOffDays((prev) => [...new Set([...prev, ...expanded])].sort());
    setOffDayRangeFrom('');
    setOffDayRangeTo('');
  };

  const countryIsBangladesh = isBangladeshCountry(contactCountry);
  const districtOptions = BANGLADESH_DISTRICTS;
  const districtKey = (formData.district ?? '').trim();
  const allBangladeshThanas = Array.from(new Set(Object.values(BANGLADESH_THANAS_BY_DISTRICT).flat()));
  const thanaSuggestions: readonly string[] =
    countryIsBangladesh && districtKey
      ? (BANGLADESH_THANAS_BY_DISTRICT[districtKey] ?? allBangladeshThanas)
      : [];
  const todayIso = isoDateLocal();

  if (loading && isEditDoctor) {
    return <div className="loading">Loading doctor...</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>{isEditDoctor ? 'Edit Doctor' : 'Register New Doctor'}</h1>
          <p>{isEditDoctor ? 'Update doctor information' : 'Register a new doctor in the system'}</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate('/hospital/doctors')}>
          Cancel
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit} className="form-container">
        <div className="form-section">
          <h3>Basic Information</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>Doctor Name *</label>
              <input
                type="text"
                required
                value={formData.doctorName}
                onChange={(e) => handleChange('doctorName', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Department *</label>
              <select
                required
                value={formData.departmentId}
                onChange={(e) => handleChange('departmentId', e.target.value)}
              >
                <option value="">Select Department</option>
                {departments.map(dept => (
                  <option key={dept.departmentId} value={dept.departmentId}>
                    {dept.departmentName}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Doctor Type *</label>
              <select
                required
                value={formData.doctorType}
                onChange={(e) => handleChange('doctorType', e.target.value as any)}
              >
                <option value="CONSULTANT">Consultant</option>
                <option value="RESIDENT">Resident</option>
                <option value="INTERN">Intern</option>
                <option value="SENIOR_CONSULTANT">Senior Consultant</option>
                <option value="ASSOCIATE_CONSULTANT">Associate Consultant</option>
                <option value="ASSISTANT_CONSULTANT">Assistant Consultant</option>
                <option value="REGISTRAR">Registrar</option>
                <option value="MEDICAL_OFFICER">Medical Officer</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label>Indoor/Outdoor Status *</label>
              <select
                required
                value={formData.indoorOutdoorStatus}
                onChange={(e) => handleChange('indoorOutdoorStatus', e.target.value as any)}
              >
                <option value="INDOOR">Indoor</option>
                <option value="OUTDOOR">Outdoor</option>
              </select>
            </div>
            <div className="form-group">
              <label>Degree *</label>
              <input
                type="text"
                required
                value={formData.degree}
                onChange={(e) => handleChange('degree', e.target.value)}
                placeholder="e.g., MBBS, MD, MS"
              />
            </div>
            <div className="form-group">
              <label>Speciality</label>
              <input
                type="text"
                value={formData.speciality}
                onChange={(e) => handleChange('speciality', e.target.value)}
                placeholder="e.g., Cardiology, Orthopedics"
              />
            </div>
            <div className="form-group">
              <label>Gender</label>
              <select
                value={formData.gender}
                onChange={(e) => handleChange('gender', e.target.value)}
              >
                <option value="">Select</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="form-group">
              <label>Birth Date</label>
              <input
                type="date"
                max={todayIso}
                value={formData.birthDate}
                onChange={(e) => handleChange('birthDate', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Registration Date *</label>
              <input
                type="date"
                required
                max={todayIso}
                value={formData.registrationDate}
                onChange={(e) => handleChange('registrationDate', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>BMDC Registration Number *</label>
              <input
                type="text"
                required
                value={formData.bmdcRegistrationNumber}
                onChange={(e) => handleChange('bmdcRegistrationNumber', e.target.value)}
                placeholder="Bangladesh Medical & Dental Council"
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Contact Information</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>Phone Number</label>
              <input
                type="tel"
                inputMode="numeric"
                value={formData.phoneNumber}
                onChange={(e) => handleChange('phoneNumber', digitsOnlyPhone(e.target.value))}
                placeholder="Digits only"
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input
                ref={emailInputRef}
                type="text"
                autoComplete="email"
                value={formData.email}
                onChange={(e) => handleChange('email', e.target.value)}
                placeholder="name@example.com"
              />
            </div>
            {(!isEditDoctor || (!hasPortalLogin && Boolean(formData.isActive))) && (
              <div className="form-group form-group--inline-check full-width">
                <label className="checkbox-label" style={{ display: 'flex', alignItems: 'flex-start', gap: '0.35rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={Boolean(formData.createLinkedUser)}
                    onChange={(e) => handleChange('createLinkedUser', e.target.checked)}
                    style={{ marginTop: '0.15rem', flexShrink: 0 }}
                  />
                  <span>
                    {isEditDoctor ? 'Create system login for this doctor (no login linked yet)' : 'Create system login for this doctor'}
                    <span className="text-muted" style={{ display: 'block', fontWeight: 'normal', fontSize: '0.9em', marginTop: '0.25rem' }}>
                      Login username is the doctor code. New accounts use the hospital default initial password; administrators should ask
                      doctors to change it after first sign-in.
                    </span>
                  </span>
                </label>
              </div>
            )}
            <div className="form-group full-width">
              <label>Present Address</label>
              <textarea
                value={formData.presentAddress}
                onChange={(e) => handleChange('presentAddress', e.target.value)}
                rows={3}
              />
            </div>
            <div className="form-group">
              <label>Country</label>
              <input
                type="text"
                list="doctor-country-options"
                value={contactCountry}
                onChange={(e) => {
                  const nextCountry = e.target.value;
                  setContactCountry(nextCountry);
                  if (!isBangladeshCountry(nextCountry)) {
                    return;
                  }
                  if (formData.district && !BANGLADESH_DISTRICTS.includes(formData.district)) {
                    handleChange('district', '');
                    handleChange('thana', '');
                    return;
                  }
                  {
                    const dk = (formData.district ?? '').trim();
                    const list = dk ? (BANGLADESH_THANAS_BY_DISTRICT[dk] ?? []) : [];
                    if (dk && formData.thana && list.length > 0 && !list.includes(formData.thana)) {
                      handleChange('thana', '');
                    }
                  }
                }}
                placeholder="Select or type country"
              />
              <datalist id="doctor-country-options">
                {COUNTRY_OPTIONS.map((country) => (
                  <option key={country} value={country} />
                ))}
              </datalist>
            </div>
            <div className="form-group">
              <label>District</label>
              {countryIsBangladesh ? (
                <select
                  value={formData.district}
                  onChange={(e) => {
                    const nextDistrict = e.target.value;
                    handleChange('district', nextDistrict);
                    handleChange('thana', '');
                  }}
                >
                  <option value="">Select District</option>
                  {districtOptions.map((district) => (
                    <option key={district} value={district}>
                      {district}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  type="text"
                  value={formData.district}
                  onChange={(e) => handleChange('district', e.target.value)}
                />
              )}
            </div>
            <div className="form-group">
              <label>Thana</label>
              {countryIsBangladesh ? (
                <>
                  <input
                    type="text"
                    list="doctor-thana-suggestions"
                    value={formData.thana}
                    onChange={(e) => handleChange('thana', e.target.value)}
                    placeholder={formData.district ? 'Type or pick thana / upazila' : 'Select district first'}
                    disabled={!formData.district}
                  />
                  <datalist id="doctor-thana-suggestions">
                    {thanaSuggestions.map((thana: string) => (
                      <option key={thana} value={thana} />
                    ))}
                  </datalist>
                </>
              ) : (
                <input
                  type="text"
                  value={formData.thana}
                  onChange={(e) => handleChange('thana', e.target.value)}
                />
              )}
            </div>
            <div className="form-group">
              <label>Area</label>
              <input
                type="text"
                value={formData.area}
                onChange={(e) => handleChange('area', e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Chamber Room</label>
              <input
                type="text"
                value={formData.chamberRoom}
                onChange={(e) => handleChange('chamberRoom', e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Financial Information</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>Visit Fee (New Patient)</label>
              <input
                type="text"
                inputMode="decimal"
                value={formData.visitFeeNew === undefined || formData.visitFeeNew === null ? '' : String(formData.visitFeeNew)}
                onChange={(e) => {
                  const s = sanitizeNonNegativeDecimalString(e.target.value);
                  if (s === '' || s === '.') {
                    handleChange('visitFeeNew', undefined);
                    return;
                  }
                  const n = parseFloat(s);
                  handleChange('visitFeeNew', Number.isFinite(n) ? n : undefined);
                }}
              />
            </div>
            <div className="form-group">
              <label>Visit Fee (Old Patient)</label>
              <input
                type="text"
                inputMode="decimal"
                value={formData.visitFeeOld === undefined || formData.visitFeeOld === null ? '' : String(formData.visitFeeOld)}
                onChange={(e) => {
                  const s = sanitizeNonNegativeDecimalString(e.target.value);
                  if (s === '' || s === '.') {
                    handleChange('visitFeeOld', undefined);
                    return;
                  }
                  const n = parseFloat(s);
                  handleChange('visitFeeOld', Number.isFinite(n) ? n : undefined);
                }}
              />
            </div>
            <div className="form-group form-group--inline-check">
              <label>
                <input
                  type="checkbox"
                  checked={formData.takeCommission || false}
                  onChange={(e) => handleChange('takeCommission', e.target.checked)}
                />
                Take Commission
              </label>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Appointment Settings</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>Patients Per Day</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={formData.patientsPerDay ?? ''}
                onKeyDown={blockNegativeNumberInput}
                onChange={(e) => handleChange('patientsPerDay', parseOptionalPositiveInt(e.target.value))}
              />
            </div>
            <div className="form-group">
              <label>Serial Start From</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={formData.serialStartFrom ?? 1}
                onKeyDown={blockNegativeNumberInput}
                onChange={(e) => handleChange('serialStartFrom', parseRequiredPositiveInt(e.target.value, 1, 1))}
              />
            </div>
            <div className="form-group">
              <label>Number of Days Can Appointment</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={formData.numberOfDaysCanAppointment ?? ''}
                onKeyDown={blockNegativeNumberInput}
                onChange={(e) => handleChange('numberOfDaysCanAppointment', parseOptionalPositiveInt(e.target.value))}
              />
            </div>
            <div className="form-group">
              <label>Number of Slots Per Day</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={formData.slotsPerDay ?? 1}
                onKeyDown={blockNegativeNumberInput}
                onChange={(e) => handleChange('slotsPerDay', parseRequiredPositiveInt(e.target.value, 1, 1))}
              />
            </div>
            <div className="form-group form-group--inline-check">
              <label>
                <input
                  type="checkbox"
                  checked={formData.appointmentsFromWeb || false}
                  onChange={(e) => handleChange('appointmentsFromWeb', e.target.checked)}
                />
                Appointments from Web
              </label>
            </div>
            <div className="form-group form-group--inline-check">
              <label>
                <input
                  type="checkbox"
                  checked={formData.appointmentsFromMobile || false}
                  onChange={(e) => handleChange('appointmentsFromMobile', e.target.checked)}
                />
                Appointments from Mobile
              </label>
            </div>
            <div className="form-group form-group--inline-check">
              <label>
                <input
                  type="checkbox"
                  checked={formData.smsEnabled || false}
                  onChange={(e) => handleChange('smsEnabled', e.target.checked)}
                />
                SMS Enabled
              </label>
            </div>
          </div>
        </div>

        {/* Appointment slots */}
        <div className="form-section">
          <h3>Weekly Appointment Schedule</h3>
          <p style={{ color: '#6b7280', fontSize: '14px', marginBottom: '16px' }}>
            Define each time window, active days, and max appointments. Use <strong>Add Slot</strong> to add another row.
          </p>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ borderCollapse: 'collapse', width: '100%' }}>
              <thead>
                <tr>
                  {(['Slot', 'Duration (Start – End)', 'Days', 'Max Appointments'] as const).map(h => (
                    <th key={h} style={{ border: '1px solid #e5e7eb', padding: '8px 14px', background: '#f9fafb', textAlign: 'left', fontWeight: 600, fontSize: '13px', color: '#374151', whiteSpace: 'nowrap' }}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {appointmentSlots.map((slot, idx) => (
                  <tr key={idx}>
                    {/* Slot label */}
                    <td style={{ border: '1px solid #e5e7eb', padding: '10px 14px', fontWeight: 600, fontSize: '13px', color: '#374151', verticalAlign: 'middle', whiteSpace: 'nowrap', background: '#fafafa' }}>
                      Slot {idx + 1}
                    </td>

                    {/* Duration */}
                    <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px', verticalAlign: 'middle' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <input
                          type="time"
                          value={slot.startTime}
                          onChange={(e) => updateSlot(idx, 'startTime', e.target.value)}
                          style={{ fontSize: '13px' }}
                        />
                        <span style={{ color: '#6b7280', fontSize: '13px' }}>–</span>
                        <input
                          type="time"
                          value={slot.endTime}
                          onChange={(e) => updateSlot(idx, 'endTime', e.target.value)}
                          style={{ fontSize: '13px' }}
                        />
                      </div>
                    </td>

                    {/* Days checkboxes */}
                    <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px', verticalAlign: 'middle' }}>
                      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                        {DAYS_OF_WEEK.map(({ key, label }) => (
                          <label key={key} style={{ display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer', fontSize: '13px', color: '#374151', whiteSpace: 'nowrap' }}>
                            <input
                              type="checkbox"
                              checked={slot.days.includes(key)}
                              onChange={(e) => toggleSlotDay(idx, key, e.target.checked)}
                            />
                            {label}
                          </label>
                        ))}
                      </div>
                    </td>

                    {/* Max appointments */}
                    <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px', verticalAlign: 'middle' }}>
                      <input
                        type="text"
                        inputMode="numeric"
                        pattern="[0-9]*"
                        value={slot.maxPatients === undefined || slot.maxPatients === null ? '' : String(slot.maxPatients)}
                        onChange={(e) => {
                          const raw = e.target.value;
                          if (raw === '') {
                            updateSlot(idx, 'maxPatients', undefined);
                            return;
                          }
                          const n = parseInt(raw, 10);
                          if (!Number.isNaN(n)) {
                            updateSlot(idx, 'maxPatients', Math.max(1, n));
                          }
                        }}
                        style={{ width: '80px', fontSize: '13px' }}
                        title="Max appointments in this window; clear the field to re-type freely"
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ marginTop: '12px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <button type="button" className="btn-secondary" onClick={addAppointmentSlot}>
              Add Slot
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={appointmentSlots.length <= 1}
              onClick={removeLastAppointmentSlot}
            >
              Remove Last Slot
            </button>
          </div>
        </div>

        {/* Off days */}
        <div className="form-section">
          <h3>Off Days (Holidays / Vacation)</h3>
          <p style={{ color: '#6b7280', fontSize: '14px', marginBottom: '16px' }}>
            Add specific dates when the doctor is unavailable regardless of the weekly schedule.
          </p>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-end', marginBottom: '12px', flexWrap: 'wrap' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label>From</label>
              <input
                type="date"
                value={offDayRangeFrom}
                onChange={(e) => setOffDayRangeFrom(e.target.value)}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label>To</label>
              <input
                type="date"
                value={offDayRangeTo}
                onChange={(e) => setOffDayRangeTo(e.target.value)}
              />
            </div>
            <button
              type="button"
              className="btn-secondary"
              onClick={addOffDayRange}
              disabled={!offDayRangeFrom || !offDayRangeTo}
            >
              Add Range
            </button>
          </div>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-end', marginBottom: '12px' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label>Single date</label>
              <input
                type="date"
                value={offDayInput}
                onChange={(e) => setOffDayInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addOffDay())}
              />
            </div>
            <button type="button" className="btn-secondary" onClick={addOffDay} disabled={!offDayInput}>
              Add Date
            </button>
          </div>
          {offDays.length === 0 ? (
            <p style={{ color: '#9ca3af', fontSize: '13px' }}>No off days added yet.</p>
          ) : (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              {offDays.map((date) => (
                <span key={date} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: '#fee2e2', color: '#991b1b', borderRadius: '6px', padding: '4px 10px', fontSize: '13px' }}>
                  {new Date(date + 'T00:00:00').toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' })}
                  <button
                    type="button"
                    onClick={() => removeOffDay(date)}
                    style={{ background: 'none', border: 'none', color: '#991b1b', cursor: 'pointer', fontSize: '16px', padding: 0, lineHeight: 1 }}
                    title="Remove"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="form-section">
          <h3>Status Information</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>Prescription Status</label>
              <select
                value={formData.prescriptionStatus || ''}
                onChange={(e) => handleChange('prescriptionStatus', e.target.value as any)}
              >
                <option value="">Select</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
            <div className="form-group">
              <label>Availability Status</label>
              <select
                value={formData.availabilityStatus || 'AVAILABLE'}
                onChange={(e) => handleChange('availabilityStatus', e.target.value as any)}
              >
                <option value="AVAILABLE">Available</option>
                <option value="NOT_AVAILABLE">Not Available</option>
              </select>
            </div>
            <div className="form-group form-group--inline-check">
              <label>
                <input
                  type="checkbox"
                  checked={formData.isActive !== false}
                  onChange={(e) => handleChange('isActive', e.target.checked)}
                />
                Active
              </label>
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Saving...' : isEditDoctor ? 'Update Doctor' : 'Register Doctor'}
          </button>
          <button type="button" className="btn-secondary" onClick={() => navigate('/hospital/doctors')}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

export default DoctorForm;
