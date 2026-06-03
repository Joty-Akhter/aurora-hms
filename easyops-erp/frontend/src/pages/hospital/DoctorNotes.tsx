import React, { useCallback, useEffect, useState } from 'react';
import hospitalService, { Doctor, DoctorHospitalNote } from '../../services/hospitalService';
import { useAuth } from '../../contexts/AuthContext';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const formatDateTime = (iso?: string) => {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const DoctorNotesPage: React.FC = () => {
  const { user, hasRole, hasPermission, canManageResource } = useAuth();
  const canManage =
    hasPermission('HOSPITAL_DOCTOR_NOTES_MANAGE') || canManageResource('hospital');
  const canView = hasPermission('HOSPITAL_FEAT_DOCTOR_NOTES') || canManage;
  const viewOnly = canView && !canManage;
  const isDoctorRole = hasRole('HOSPITAL_DOCTOR');

  const [notes, setNotes] = useState<DoctorHospitalNote[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [linkedDoctor, setLinkedDoctor] = useState<Doctor | null>(null);
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [noteText, setNoteText] = useState('');
  const [editingNoteId, setEditingNoteId] = useState<string | null>(null);
  const [editText, setEditText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const needsDoctorPicker = canManage && !(isDoctorRole && linkedDoctor);

  const canModifyNote = (note: DoctorHospitalNote) => note.canModify === true;

  const loadNotes = useCallback(async () => {
    if (!canView) return;
    try {
      setLoading(true);
      setError(null);
      const { data } = await hospitalService.getDoctorHospitalNotes();
      setNotes(data ?? []);
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to load doctor notes'));
      setNotes([]);
    } finally {
      setLoading(false);
    }
  }, [canView]);

  useEffect(() => {
    if (canView) {
      void loadNotes();
    }
  }, [canView, loadNotes]);

  useEffect(() => {
    if (!canManage || !user?.id) {
      setLinkedDoctor(null);
      if (!canManage) {
        setDoctors([]);
      }
      return;
    }
    let cancelled = false;
    hospitalService
      .getActiveDoctorsForPrescription()
      .then((res) => {
        if (cancelled) return;
        const list = res.data ?? [];
        setDoctors(list);
        const linked = list.find((d) => d.linkedUserId === user.id) ?? null;
        setLinkedDoctor(linked);
        if (linked) {
          setSelectedDoctorId(linked.doctorId);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLinkedDoctor(null);
          setDoctors([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [canManage, user?.id]);

  const resolveDoctorId = (): string | null => {
    if (isDoctorRole && linkedDoctor) {
      return linkedDoctor.doctorId;
    }
    return selectedDoctorId || null;
  };

  const handleSaveNote = async () => {
    if (!noteText.trim() || !canManage) return;
    const doctorId = resolveDoctorId();
    if (!doctorId) {
      setError('Please select a doctor.');
      return;
    }
    try {
      setLoading(true);
      setError(null);
      await hospitalService.createDoctorHospitalNote({
        doctorId,
        message: noteText.trim(),
      });
      setNoteText('');
      await loadNotes();
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to save doctor note'));
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateNote = async (noteId: string) => {
    if (!editText.trim()) return;
    try {
      setLoading(true);
      setError(null);
      await hospitalService.updateDoctorHospitalNote(noteId, { message: editText.trim() });
      setEditingNoteId(null);
      setEditText('');
      await loadNotes();
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to update doctor note'));
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteNote = async (noteId: string) => {
    if (!window.confirm('Delete this note?')) return;
    try {
      setLoading(true);
      setError(null);
      await hospitalService.deleteDoctorHospitalNote(noteId);
      if (editingNoteId === noteId) {
        setEditingNoteId(null);
        setEditText('');
      }
      await loadNotes();
    } catch (err: unknown) {
      setError(ehrApiErrorMessage(err, 'Failed to delete doctor note'));
    } finally {
      setLoading(false);
    }
  };

  const showActionsColumn = notes.some(canModifyNote);

  if (!canView) {
    return (
      <div className="hospital-page">
        <div className="page-header">
          <h1 className="page-title">Doctor Notes</h1>
        </div>
        <p className="text-muted">You do not have permission to view doctor notes.</p>
      </div>
    );
  }

  return (
    <div className="hospital-page">
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px', flexWrap: 'wrap' }}>
        <div>
          <h1 className="page-title">Doctor Notes</h1>
          <p className="text-muted" style={{ margin: 0 }}>
            {viewOnly
              ? 'Hospital-wide messages from doctors (e.g. chamber closed, running late). View-only for call center.'
              : isDoctorRole && linkedDoctor
                ? 'Post a message to hospital staff.'
                : 'Post a message on behalf of a doctor for attendants, call center, and patients to see.'}
          </p>
        </div>
        <button type="button" className="btn-secondary" onClick={() => void loadNotes()} disabled={loading}>
          Refresh
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {isDoctorRole && !linkedDoctor && (
        <div className="error-message" style={{ marginBottom: '1rem' }}>
          Your user account is not linked to a doctor profile. Ask an administrator to link your account before posting notes.
        </div>
      )}

      {canManage && (
        <div className="form-container" style={{ marginBottom: '1.5rem' }}>
          <h3>New note</h3>
          <div className="form-section">
            {needsDoctorPicker && (
              <div className="filter-group" style={{ marginBottom: '12px' }}>
                <label htmlFor="doctor-select">Doctor</label>
                <select
                  id="doctor-select"
                  value={selectedDoctorId}
                  onChange={(e) => setSelectedDoctorId(e.target.value)}
                  style={{ width: '100%', maxWidth: '400px' }}
                >
                  <option value="">Select doctor…</option>
                  {doctors.map((d) => (
                    <option key={d.doctorId} value={d.doctorId}>
                      {d.doctorName}
                      {d.speciality ? ` — ${d.speciality}` : ''}
                    </option>
                  ))}
                </select>
              </div>
            )}
            {isDoctorRole && linkedDoctor && (
              <p className="text-muted" style={{ marginTop: 0 }}>
                Posting as <strong>{linkedDoctor.doctorName}</strong>
              </p>
            )}
            <label htmlFor="note-message">Message</label>
            <textarea
              id="note-message"
              rows={4}
              maxLength={2000}
              value={noteText}
              onChange={(e) => setNoteText(e.target.value)}
              placeholder="e.g. I cannot continue chamber today, or I'll be late by an hour…"
              style={{ width: '100%' }}
            />
            <button
              type="button"
              className="btn-primary"
              style={{ marginTop: '8px' }}
              onClick={() => void handleSaveNote()}
              disabled={
                loading ||
                !noteText.trim() ||
                (needsDoctorPicker && !selectedDoctorId) ||
                (isDoctorRole && !linkedDoctor)
              }
            >
              Save note
            </button>
          </div>
        </div>
      )}

      {loading && notes.length === 0 ? (
        <p>Loading…</p>
      ) : notes.length === 0 ? (
        <p className="text-muted">No doctor notes yet.</p>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Doctor</th>
                <th>Message</th>
                {showActionsColumn && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {notes.map((n) => (
                <tr key={n.noteId}>
                  <td>{formatDateTime(n.createdAt)}</td>
                  <td>{n.doctorName ?? '—'}</td>
                  <td style={{ whiteSpace: 'pre-wrap' }}>
                    {editingNoteId === n.noteId ? (
                      <textarea
                        rows={3}
                        maxLength={2000}
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        style={{ width: '100%' }}
                      />
                    ) : (
                      n.message ?? '—'
                    )}
                  </td>
                  {showActionsColumn && (
                    <td>
                      {canModifyNote(n) ? (
                        editingNoteId === n.noteId ? (
                          <>
                            <button
                              type="button"
                              className="btn-primary"
                              style={{ marginRight: 6 }}
                              onClick={() => void handleUpdateNote(n.noteId)}
                            >
                              Save
                            </button>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => {
                                setEditingNoteId(null);
                                setEditText('');
                              }}
                            >
                              Cancel
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="btn-secondary"
                              style={{ marginRight: 6 }}
                              onClick={() => {
                                setEditingNoteId(n.noteId);
                                setEditText(n.message ?? '');
                              }}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => void handleDeleteNote(n.noteId)}
                            >
                              Delete
                            </button>
                          </>
                        )
                      ) : null}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default DoctorNotesPage;
