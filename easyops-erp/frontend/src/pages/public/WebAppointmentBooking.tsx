import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { format, addDays, startOfDay, isBefore, parseISO, isAfter } from 'date-fns';
import webBookingService, {
  WebBookableDoctor,
  WebBookingSlot,
} from '../../services/webBookingService';
import { webBookingErrorMessage } from '../../utils/webBookingErrors';
import './WebAppointmentBooking.css';

const DEFAULT_BOOKING_WINDOW_DAYS = 30;

function slotTimestampsMatch(a: string, b: string): boolean {
  if (a === b) return true;
  const aTime = new Date(a).getTime();
  const bTime = new Date(b).getTime();
  return !Number.isNaN(aTime) && aTime === bTime;
}

const WebAppointmentBooking: React.FC = () => {
  const [doctors, setDoctors] = useState<WebBookableDoctor[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [appointmentDate, setAppointmentDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [slots, setSlots] = useState<WebBookingSlot[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<WebBookingSlot | null>(null);
  const [fullName, setFullName] = useState('');
  const [primaryPhone, setPrimaryPhone] = useState('');
  const [primaryEmail, setPrimaryEmail] = useState('');
  const [ageYears, setAgeYears] = useState('');
  const [loadingDoctors, setLoadingDoctors] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [slotError, setSlotError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [slotsRefreshKey, setSlotsRefreshKey] = useState(0);

  const selectedDoctor = useMemo(
    () => doctors.find((d) => d.doctorId === selectedDoctorId),
    [doctors, selectedDoctorId]
  );

  const maxBookingDays =
    selectedDoctor?.numberOfDaysCanAppointment && selectedDoctor.numberOfDaysCanAppointment > 0
      ? selectedDoctor.numberOfDaysCanAppointment
      : DEFAULT_BOOKING_WINDOW_DAYS;

  const minDate = format(new Date(), 'yyyy-MM-dd');
  const maxDate = format(addDays(new Date(), maxBookingDays), 'yyyy-MM-dd');

  const clampDate = useCallback(
    (dateStr: string) => {
      if (dateStr < minDate) return minDate;
      if (dateStr > maxDate) return maxDate;
      return dateStr;
    },
    [minDate, maxDate]
  );

  useEffect(() => {
    webBookingService
      .getDoctors()
      .then((res) => {
        setDoctors(res.data);
        if (res.data.length > 0) {
          setSelectedDoctorId(res.data[0].doctorId);
        }
      })
      .catch((err) => setError(webBookingErrorMessage(err, 'Unable to load doctors. Please try again later.')))
      .finally(() => setLoadingDoctors(false));
  }, []);

  useEffect(() => {
    setAppointmentDate((current) => clampDate(current));
  }, [clampDate, selectedDoctorId]);

  useEffect(() => {
    if (!selectedDoctorId || !appointmentDate) {
      setSlots([]);
      setSelectedSlot(null);
      return;
    }

    let cancelled = false;
    setLoadingSlots(true);
    setSelectedSlot(null);
    setSlotError(null);

    webBookingService
      .getAvailability(selectedDoctorId, appointmentDate, appointmentDate)
      .then((res) => {
        if (cancelled) return;
        const day = res.data?.[0];
        if (day?.blackedOut) {
          setSlots([]);
          setSlotError('This doctor is not available on the selected date.');
          return;
        }
        const daySlots = day?.slots ?? [];
        const now = new Date();
        const selectedDay = startOfDay(parseISO(appointmentDate));
        const today = startOfDay(now);
        const available = daySlots.filter((slot) => {
          if (slot.availableCount <= 0) return false;
          const start = new Date(slot.start);
          if (selectedDay.getTime() === today.getTime()) {
            return isAfter(start, now);
          }
          return !isBefore(selectedDay, today);
        });
        setSlots(available);
      })
      .catch((err) => {
        if (cancelled) return;
        setSlots([]);
        setSlotError(webBookingErrorMessage(err, 'Unable to load available time slots for the selected date.'));
      })
      .finally(() => {
        if (!cancelled) setLoadingSlots(false);
      });

    return () => {
      cancelled = true;
    };
  }, [selectedDoctorId, appointmentDate, slotsRefreshKey]);

  const formatSlotLabel = (slot: WebBookingSlot) => {
    const start = new Date(slot.start);
    const end = new Date(slot.end);
    return `${format(start, 'h:mm a')} – ${format(end, 'h:mm a')}`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDoctorId || !selectedSlot) {
      setError('Please select a doctor and time slot.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      setSuccess(null);
      const response = await webBookingService.bookAppointment({
        doctorId: selectedDoctorId,
        fullName: fullName.trim(),
        primaryPhone: primaryPhone.trim(),
        primaryEmail: primaryEmail.trim() || undefined,
        ageYears: ageYears && !Number.isNaN(Number(ageYears)) ? Number(ageYears) : undefined,
        appointmentDate,
        slotStart: selectedSlot.start,
        slotEnd: selectedSlot.end,
      });
      setSuccess(
        response.data.message +
          (response.data.mrn ? ` Your patient ID (MRN): ${response.data.mrn}.` : '')
      );
      setFullName('');
      setPrimaryPhone('');
      setPrimaryEmail('');
      setAgeYears('');
      setSelectedSlot(null);
      setSlotsRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      setError(webBookingErrorMessage(err, 'Booking failed. Please try another slot.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="web-booking-page">
      <header className="web-booking-header">
        <div>
          <p className="web-booking-kicker">Aurora Specialized Hospital</p>
          <h1>Book an Appointment</h1>
          <p>Select a doctor, choose an available time slot, and submit your details.</p>
        </div>
      </header>

      {error && <div className="web-booking-alert web-booking-alert-error">{error}</div>}
      {success && <div className="web-booking-alert web-booking-alert-success">{success}</div>}

      <form className="web-booking-form" onSubmit={handleSubmit}>
        <section className="web-booking-card">
          <h2>1. Select doctor</h2>
          {loadingDoctors ? (
            <p>Loading doctors…</p>
          ) : doctors.length === 0 ? (
            <p>No doctors are currently accepting online appointments.</p>
          ) : (
            <select
              value={selectedDoctorId}
              onChange={(e) => setSelectedDoctorId(e.target.value)}
              required
            >
              {doctors.map((doctor) => (
                <option key={doctor.doctorId} value={doctor.doctorId}>
                  {doctor.doctorName}
                  {doctor.speciality ? ` — ${doctor.speciality}` : ''}
                </option>
              ))}
            </select>
          )}
          {selectedDoctor?.departmentName && (
            <p className="web-booking-meta">Department: {selectedDoctor.departmentName}</p>
          )}
          {maxBookingDays < 365 && (
            <p className="web-booking-meta">
              Appointments can be booked up to {maxBookingDays} days ahead.
            </p>
          )}
        </section>

        <section className="web-booking-card">
          <h2>2. Choose date &amp; time slot</h2>
          <label htmlFor="appointment-date">Appointment date</label>
          <input
            id="appointment-date"
            type="date"
            value={appointmentDate}
            min={minDate}
            max={maxDate}
            onChange={(e) => setAppointmentDate(clampDate(e.target.value))}
            required
            disabled={!selectedDoctorId || loadingDoctors}
          />

          {slotError && <div className="web-booking-alert web-booking-alert-error">{slotError}</div>}

          <div className="web-booking-slots">
            {loadingSlots ? (
              <p>Loading available slots…</p>
            ) : slotError ? null : slots.length === 0 ? (
              <p>No available slots for this date. Please choose another date.</p>
            ) : (
              slots.map((slot) => (
                <button
                  key={`${slot.start}-${slot.end}`}
                  type="button"
                  className={`web-booking-slot${selectedSlot && slotTimestampsMatch(selectedSlot.start, slot.start) ? ' selected' : ''}`}
                  onClick={() => setSelectedSlot(slot)}
                >
                  {formatSlotLabel(slot)}
                </button>
              ))
            )}
          </div>
        </section>

        <section className="web-booking-card">
          <h2>3. Your details</h2>
          <label htmlFor="full-name">Full name</label>
          <input
            id="full-name"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            disabled={!selectedSlot}
          />

          <label htmlFor="phone">Mobile number</label>
          <input
            id="phone"
            type="tel"
            value={primaryPhone}
            onChange={(e) => setPrimaryPhone(e.target.value)}
            required
            disabled={!selectedSlot}
          />

          <label htmlFor="email">Email (optional)</label>
          <input
            id="email"
            type="email"
            value={primaryEmail}
            onChange={(e) => setPrimaryEmail(e.target.value)}
            disabled={!selectedSlot}
          />

          <label htmlFor="age">Age (years)</label>
          <input
            id="age"
            type="number"
            min={0}
            max={150}
            value={ageYears}
            onChange={(e) => setAgeYears(e.target.value)}
            disabled={!selectedSlot}
          />
        </section>

        <button
          type="submit"
          className="web-booking-submit"
          disabled={submitting || !selectedSlot || loadingDoctors}
        >
          {submitting ? 'Booking…' : 'Confirm appointment'}
        </button>
      </form>
    </div>
  );
};

export default WebAppointmentBooking;
