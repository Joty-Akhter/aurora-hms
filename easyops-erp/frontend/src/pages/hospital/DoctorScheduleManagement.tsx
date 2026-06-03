import React, { useEffect, useMemo, useRef, useState } from 'react';
import { addDays, format, isBefore, parseISO } from 'date-fns';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import hospitalService, { Doctor, DoctorAppointmentSlot, DoctorRequest } from '../../services/hospitalService';
import hospitalSchedulingService, { type CreateRosterBlockRequest } from '../../services/hospitalSchedulingService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { validateUniqueAppointmentSlots } from '../../utils/appointmentSlotUtils';
import './Hospital.css';

const DAYS_OF_WEEK = [
  { key: 'saturday', label: 'Sat' },
  { key: 'sunday', label: 'Sun' },
  { key: 'monday', label: 'Mon' },
  { key: 'tuesday', label: 'Tue' },
  { key: 'wednesday', label: 'Wed' },
  { key: 'thursday', label: 'Thu' },
  { key: 'friday', label: 'Fri' },
];

const DEFAULT_ACTIVE_DAYS = ['saturday', 'sunday', 'monday', 'tuesday', 'wednesday', 'thursday'];

const emptySlot = (): DoctorAppointmentSlot => ({
  startTime: '09:00',
  endTime: '17:00',
  days: [...DEFAULT_ACTIVE_DAYS],
  maxPatients: 10,
});

const mapDoctorToRequest = (doctor: Doctor, appointmentSlots: DoctorAppointmentSlot[], offDays: string[]): DoctorRequest => ({
  doctorName: doctor.doctorName,
  departmentId: doctor.departmentId,
  doctorType: doctor.doctorType,
  indoorOutdoorStatus: doctor.indoorOutdoorStatus,
  degree: doctor.degree,
  speciality: doctor.speciality,
  gender: doctor.gender,
  birthDate: doctor.birthDate,
  registrationDate: doctor.registrationDate,
  bmdcRegistrationNumber: doctor.bmdcRegistrationNumber,
  phoneNumber: doctor.phoneNumber,
  email: doctor.email,
  presentAddress: doctor.presentAddress,
  district: doctor.district,
  thana: doctor.thana,
  area: doctor.area,
  chamberRoom: doctor.chamberRoom,
  visitFeeNew: doctor.visitFeeNew,
  visitFeeOld: doctor.visitFeeOld,
  takeCommission: doctor.takeCommission,
  patientsPerDay: doctor.patientsPerDay,
  serialStartFrom: doctor.serialStartFrom,
  numberOfDaysCanAppointment: doctor.numberOfDaysCanAppointment,
  numberOfAppointmentsFromWeb: doctor.numberOfAppointmentsFromWeb,
  numberOfAppointmentsFromMobile: doctor.numberOfAppointmentsFromMobile,
  appointmentsFromWeb: doctor.appointmentsFromWeb,
  appointmentsFromMobile: doctor.appointmentsFromMobile,
  slotsPerDay: appointmentSlots.length > 0 ? appointmentSlots.length : doctor.slotsPerDay,
  weeklySchedule: doctor.weeklySchedule,
  appointmentSlots,
  offDays,
  smsEnabled: doctor.smsEnabled,
  prescriptionStatus: doctor.prescriptionStatus,
  availabilityStatus: doctor.availabilityStatus,
  isActive: doctor.isActive,
  createLinkedUser: false,
});

const DoctorScheduleManagement: React.FC = () => {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState<string>('');
  const [appointmentSlots, setAppointmentSlots] = useState<DoctorAppointmentSlot[]>([emptySlot()]);
  const [offDays, setOffDays] = useState<string[]>([]);
  const [offDayInput, setOffDayInput] = useState('');
  const [offDayRangeFrom, setOffDayRangeFrom] = useState('');
  const [offDayRangeTo, setOffDayRangeTo] = useState('');
  const [tempOverrideDate, setTempOverrideDate] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [tempOverrideStart, setTempOverrideStart] = useState<string>('09:00');
  const [tempOverrideEnd, setTempOverrideEnd] = useState<string>('17:00');
  const [tempOverrideSaving, setTempOverrideSaving] = useState<boolean>(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [doctorInput, setDoctorInput] = useState('');
  const pageTopRef = useRef<HTMLDivElement>(null);

  const showSuccess = (text: string) => {
    setMessage(text);
    setError(null);
    requestAnimationFrame(() => {
      pageTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  useEffect(() => {
    const loadDoctors = async () => {
      try {
        setLoading(true);
        const res = await hospitalService.getDoctors();
        setDoctors(res.data.filter((d) => d.isActive));
      } catch (err: any) {
        setError(ehrApiErrorMessage(err, 'Failed to load doctors'));
      } finally {
        setLoading(false);
      }
    };
    void loadDoctors();
  }, []);

  const selectedDoctor = useMemo(
    () => doctors.find((d) => d.doctorId === selectedDoctorId) ?? null,
    [doctors, selectedDoctorId]
  );

  useEffect(() => {
    if (!selectedDoctor) {
      setAppointmentSlots([emptySlot()]);
      setOffDays([]);
      setDoctorInput('');
      return;
    }
    setDoctorInput(selectedDoctor.doctorName ?? '');
    setAppointmentSlots(selectedDoctor.appointmentSlots?.length ? selectedDoctor.appointmentSlots : [emptySlot()]);
    setOffDays(selectedDoctor.offDays ?? []);
  }, [selectedDoctor]);

  const updateSlot = (idx: number, field: keyof DoctorAppointmentSlot, value: string | number | string[] | undefined) => {
    setAppointmentSlots((prev) => prev.map((s, i) => (i === idx ? { ...s, [field]: value } : s)));
  };

  const toggleSlotDay = (idx: number, dayKey: string, checked: boolean) => {
    setAppointmentSlots((prev) =>
      prev.map((s, i) => {
        if (i !== idx) return s;
        const days = checked ? [...s.days, dayKey] : s.days.filter((d) => d !== dayKey);
        return { ...s, days };
      })
    );
  };

  const addOffDay = () => {
    if (!offDayInput) return;
    if (!offDays.includes(offDayInput)) setOffDays((prev) => [...prev, offDayInput].sort());
    setOffDayInput('');
  };

  const removeOffDay = (date: string) => setOffDays((prev) => prev.filter((d) => d !== date));

  const addOffDayRange = () => {
    if (!offDayRangeFrom || !offDayRangeTo) return;
    const start = offDayRangeFrom <= offDayRangeTo ? offDayRangeFrom : offDayRangeTo;
    const end = offDayRangeFrom <= offDayRangeTo ? offDayRangeTo : offDayRangeFrom;
    const expanded: string[] = [];
    let cursor = parseISO(`${start}T12:00:00`);
    const endDt = parseISO(`${end}T12:00:00`);
    while (!isBefore(endDt, cursor)) {
      expanded.push(format(cursor, 'yyyy-MM-dd'));
      cursor = addDays(cursor, 1);
    }
    setOffDays((prev) => [...new Set([...prev, ...expanded])].sort());
    setOffDayRangeFrom('');
    setOffDayRangeTo('');
  };

  const handleSave = async () => {
    if (!selectedDoctor) {
      if (doctorInput.trim().length >= 2) {
        setError('Pick a doctor from the suggestions list before saving.');
      }
      return;
    }
    setMessage(null);
    setError(null);
    const slotDupError = validateUniqueAppointmentSlots(appointmentSlots);
    if (slotDupError) {
      setError(slotDupError);
      return;
    }
    try {
      setSaving(true);
      const slotsNormalized = appointmentSlots.map((s) => ({
        ...s,
        maxPatients: s.maxPatients != null && s.maxPatients > 0 ? s.maxPatients : 10,
      }));
      const payload = mapDoctorToRequest(selectedDoctor, slotsNormalized, offDays);
      await hospitalService.updateDoctor(selectedDoctor.doctorId, payload);
      setDoctors((prev) =>
        prev.map((d) =>
          d.doctorId === selectedDoctor.doctorId
            ? { ...d, appointmentSlots: slotsNormalized, offDays, slotsPerDay: slotsNormalized.length }
            : d
        )
      );
      setAppointmentSlots(slotsNormalized);
      showSuccess('Schedule and off-days updated successfully.');
    } catch (err: any) {
      setError(ehrApiErrorMessage(err, 'Failed to update doctor schedule'));
    } finally {
      setSaving(false);
    }
  };

  const handleCreateTemporarySlotOverride = async () => {
    if (!selectedDoctor) return;
    if (!tempOverrideDate?.trim()) {
      setError('Select a date for the temporary override');
      return;
    }
    if (!tempOverrideStart?.trim() || !tempOverrideEnd?.trim()) {
      setError('Temporary override start and end time are required');
      return;
    }

    try {
      setError(null);
      setTempOverrideSaving(true);
      setMessage(null);

      const start = new Date(`${tempOverrideDate}T${tempOverrideStart}:00`);
      const end = new Date(`${tempOverrideDate}T${tempOverrideEnd}:00`);
      if (!(start.getTime() < end.getTime())) {
        setError('Override start time must be before end time');
        return;
      }

      const dayStart = new Date(`${tempOverrideDate}T00:00:00`);
      const dayEnd = new Date(`${tempOverrideDate}T23:59:59`);

      const { data } = await hospitalService.findOrCreateDoctorSchedulingResource(selectedDoctor.doctorId);
      const resourceId = data.resourceId;

      // Replace only prior boundary-style override blocks for that day so repeated edits behave predictably
      // without deleting unrelated manual UNAVAILABLE blocks inside the day.
      const existing = await hospitalSchedulingService.getRosterBlocks(resourceId, {
        from: dayStart.toISOString(),
        to: dayEnd.toISOString(),
      });
      const dayStartMs = dayStart.getTime();
      const dayEndMs = dayEnd.getTime();
      const toDelete = existing.filter((b) => {
        if (b.type !== 'UNAVAILABLE') return false;
        const bStart = new Date(b.startTime).getTime();
        const bEnd = new Date(b.endTime).getTime();
        const startsAtDayStart = Math.abs(bStart - dayStartMs) < 60_000;
        const endsAtDayEnd = Math.abs(bEnd - dayEndMs) < 60_000;
        // This override feature creates blocks that start at day-start OR end at day-end.
        return startsAtDayStart || endsAtDayEnd;
      });
      await Promise.allSettled(toDelete.map((b) => hospitalSchedulingService.deleteRosterBlock(b.id)));

      const createBlock = (blockStart: Date, blockEnd: Date) => {
        const payload: CreateRosterBlockRequest = {
          type: 'UNAVAILABLE',
          startTime: blockStart.toISOString(),
          endTime: blockEnd.toISOString(),
        };
        return hospitalSchedulingService.createRosterBlock(resourceId, payload);
      };

      // Block outside [tempOverrideStart, tempOverrideEnd] to override default availability for that date.
      const ops: Promise<unknown>[] = [];
      if (start.getTime() > dayStart.getTime()) {
        ops.push(createBlock(dayStart, start));
      }
      if (end.getTime() < dayEnd.getTime()) {
        ops.push(createBlock(end, dayEnd));
      }
      await Promise.all(ops);

      showSuccess('Temporary slot override created (effective for selected date).');
    } catch (err: any) {
      setError(ehrApiErrorMessage(err, 'Failed to create temporary slot override'));
    } finally {
      setTempOverrideSaving(false);
    }
  };

  return (
    <div className="hospital-page">
      <div ref={pageTopRef} tabIndex={-1} aria-hidden="true" style={{ scrollMarginTop: '80px' }} />
      <div className="page-header">
        <div>
          <h1>Doctor Schedule & Off Days</h1>
          <p>Manage weekly appointment schedule and leave dates without editing full doctor profile.</p>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {message && (
        <div className="success-message success-message--prominent" role="status">
          {message}
        </div>
      )}

      <div className="form-section">
        <h3>Select Doctor</h3>
        <div className="form-group" style={{ maxWidth: '520px' }}>
          <label>Doctor *</label>
          <Autocomplete<Doctor>
            options={doctors}
            value={selectedDoctor}
            onChange={(_, v) => {
              setSelectedDoctorId(v?.doctorId ?? '');
            }}
            inputValue={doctorInput}
            onInputChange={(_, v, reason) => {
              setError(null);
              setDoctorInput(v);
              if (reason === 'clear' || (reason === 'input' && v.trim() === '')) {
                setSelectedDoctorId('');
              }
            }}
            disabled={loading}
            getOptionLabel={(d) => d.doctorName ?? ''}
            isOptionEqualToValue={(a, b) => a.doctorId === b.doctorId}
            filterOptions={(options, params) => {
              const q = params.inputValue.trim().toLowerCase();
              if (q.length < 2) return [];
              return options.filter(
                (d) =>
                  (d.doctorName || '').toLowerCase().includes(q) ||
                  (d.doctorCode || '').toLowerCase().includes(q) ||
                  (d.departmentName || '').toLowerCase().includes(q) ||
                  (d.speciality || '').toLowerCase().includes(q),
              );
            }}
            renderOption={(props, d) => (
              <li {...props} key={d.doctorId}>
                <Box>
                  <Typography variant="body2" fontWeight={600}>
                    {d.doctorName}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {[d.departmentName, d.speciality, d.doctorCode].filter(Boolean).join(' · ')}
                  </Typography>
                </Box>
              </li>
            )}
            noOptionsText={doctorInput.trim().length < 2 ? 'Type at least 2 characters' : 'No matching doctors'}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Search doctor"
                placeholder="Type name, specialty, department, or code (min. 2 characters)"
                size="small"
              />
            )}
          />
        </div>
      </div>

      {selectedDoctor && (
        <>
          <div className="form-section">
            <h3>Weekly Appointment Schedule</h3>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ borderCollapse: 'collapse', width: '100%' }}>
                <thead>
                  <tr>
                    {(['Slot', 'Duration (Start - End)', 'Days', 'Max Appointments'] as const).map((h) => (
                      <th
                        key={h}
                        style={{
                          border: '1px solid #e5e7eb',
                          padding: '8px 14px',
                          background: '#f9fafb',
                          textAlign: 'left',
                          fontWeight: 600,
                          fontSize: '13px',
                          color: '#374151',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {appointmentSlots.map((slot, idx) => (
                    <tr key={idx}>
                      <td style={{ border: '1px solid #e5e7eb', padding: '10px 14px', fontWeight: 600, background: '#fafafa' }}>Slot {idx + 1}</td>
                      <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <input type="time" value={slot.startTime} onChange={(e) => updateSlot(idx, 'startTime', e.target.value)} />
                          <span style={{ color: '#6b7280' }}>-</span>
                          <input type="time" value={slot.endTime} onChange={(e) => updateSlot(idx, 'endTime', e.target.value)} />
                        </div>
                      </td>
                      <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px' }}>
                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                          {DAYS_OF_WEEK.map(({ key, label }) => (
                            <label key={key} style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px' }}>
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
                      <td style={{ border: '1px solid #e5e7eb', padding: '10px 12px' }}>
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
                          style={{ width: '90px' }}
                          title="Clear the field to re-type; empty defaults to 10 on save"
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ marginTop: '12px', display: 'flex', gap: '8px' }}>
              <button type="button" className="btn-secondary" onClick={() => setAppointmentSlots((p) => [...p, emptySlot()])}>
                Add Slot
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setAppointmentSlots((p) => (p.length > 1 ? p.slice(0, -1) : p))}
                disabled={appointmentSlots.length <= 1}
              >
                Remove Last Slot
              </button>
            </div>
          </div>

          <div className="form-section">
            <h3>Off Days (Holidays / Vacation)</h3>
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
                  <span
                    key={date}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '6px',
                      background: '#fee2e2',
                      color: '#991b1b',
                      borderRadius: '6px',
                      padding: '4px 10px',
                      fontSize: '13px',
                    }}
                  >
                    {new Date(`${date}T00:00:00`).toLocaleDateString(undefined, {
                      weekday: 'short',
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                    <button
                      type="button"
                      onClick={() => removeOffDay(date)}
                      style={{ background: 'none', border: 'none', color: '#991b1b', cursor: 'pointer', fontSize: '16px', padding: 0, lineHeight: 1 }}
                      title="Remove"
                    >
                      x
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="form-section">
            <h3>Temporary Slot Override (date-only)</h3>
            <p style={{ marginTop: 0, color: '#6b7280', fontSize: '13px' }}>
              Blocks the doctor&apos;s schedule outside the selected time window for this one date.
            </p>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label>Date</label>
                <input type="date" value={tempOverrideDate} onChange={(e) => setTempOverrideDate(e.target.value)} />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label>Start</label>
                <input type="time" value={tempOverrideStart} onChange={(e) => setTempOverrideStart(e.target.value)} />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label>End</label>
                <input type="time" value={tempOverrideEnd} onChange={(e) => setTempOverrideEnd(e.target.value)} />
              </div>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => void handleCreateTemporarySlotOverride()}
                disabled={tempOverrideSaving}
              >
                {tempOverrideSaving ? 'Applying…' : 'Create Override'}
              </button>
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving...' : 'Save Schedule & Off Days'}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default DoctorScheduleManagement;
