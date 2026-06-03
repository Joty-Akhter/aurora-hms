import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getRosterMonthView,
  getShiftDefinitions,
  getEmployees,
  getDepartments,
  createRosterSchedule,
  updateRosterSchedule,
  deleteRosterSchedule,
  RosterMonthView,
  RosterScheduleRow,
  ShiftDefinition,
  Employee,
  Department,
  RosterScheduleWritePayload,
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_DETECT_CLASS,
  LAYOUT_OVERLAY_ROOT_Z,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

const deptKey = (d: Pick<Department, 'departmentId' | 'id'> | null | undefined) =>
  d?.departmentId ?? d?.id ?? '';

const RosterPlanner: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const today = useMemo(() => new Date(), []);
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [departmentId, setDepartmentId] = useState('');
  const [view, setView] = useState<RosterMonthView | null>(null);
  const [shifts, setShifts] = useState<ShiftDefinition[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingRow, setEditingRow] = useState<RosterScheduleRow | null>(null);
  const [form, setForm] = useState({
    employeeId: '',
    shiftDate: '',
    shiftDefinitionId: '',
    shiftName: '',
    startTime: '09:00',
    endTime: '17:00',
    breakDuration: '0',
    isOvertime: false,
    notes: '',
  });

  const loadRefs = async () => {
    if (!currentOrganizationId) return;
    const [shRes, emRes, dRes] = await Promise.all([
      getShiftDefinitions(currentOrganizationId, true),
      getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
      getDepartments(currentOrganizationId, { activeOnly: true }),
    ]);
    setShifts(Array.isArray(shRes.data) ? shRes.data : []);
    setEmployees(Array.isArray(emRes.data) ? emRes.data : []);
    setDepartments(Array.isArray(dRes.data) ? dRes.data : []);
  };

  const loadMonth = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const res = await getRosterMonthView(
        currentOrganizationId,
        year,
        month,
        departmentId || undefined
      );
      setView(res.data);
    } catch (e: unknown) {
      const msg =
        typeof e === 'object' && e !== null && 'response' in e
          ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || 'Failed to load roster');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRefs();
  }, [currentOrganizationId]);

  useEffect(() => {
    loadMonth();
  }, [currentOrganizationId, year, month, departmentId]);

  const openCreate = () => {
    setEditingRow(null);
    const ymd = `${year}-${String(month).padStart(2, '0')}-01`;
    setForm({
      employeeId: '',
      shiftDate: ymd,
      shiftDefinitionId: '',
      shiftName: '',
      startTime: '09:00',
      endTime: '17:00',
      breakDuration: '0',
      isOvertime: false,
      notes: '',
    });
    setShowModal(true);
  };

  const applyShiftDefinitionDefaults = (defId: string) => {
    const d = shifts.find((s) => s.shiftDefinitionId === defId);
    if (!d) return;
    setForm((prev) => ({
      ...prev,
      shiftDefinitionId: defId,
      shiftName: d.name || prev.shiftName,
    }));
  };

  const openEdit = (row: RosterScheduleRow) => {
    setEditingRow(row);
    setForm({
      employeeId: row.employeeId,
      shiftDate: row.shiftDate?.substring(0, 10) || '',
      shiftDefinitionId: row.shiftDefinitionId || '',
      shiftName: row.shiftName || '',
      startTime: row.startTime?.substring(0, 5) || '09:00',
      endTime: row.endTime?.substring(0, 5) || '17:00',
      breakDuration: String(row.breakDuration ?? 0),
      isOvertime: !!row.isOvertime,
      notes: row.notes || '',
    });
    setShowModal(true);
  };

  const toLocalTime = (hm: string) => {
    const parts = hm.split(':');
    const h = parts[0]?.padStart(2, '0') || '09';
    const m = (parts[1] || '00').padStart(2, '0');
    return `${h}:${m}:00`;
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;
    try {
      const payload: RosterScheduleWritePayload = {
        organizationId: currentOrganizationId,
        employeeId: form.employeeId,
        shiftDate: form.shiftDate,
        shiftName: form.shiftName || undefined,
        startTime: toLocalTime(form.startTime),
        endTime: toLocalTime(form.endTime),
        breakDuration: parseInt(form.breakDuration, 10) || 0,
        isOvertime: form.isOvertime,
        notes: form.notes || undefined,
      };
      if (form.shiftDefinitionId) {
        payload.shiftDefinitionId = form.shiftDefinitionId;
      } else {
        payload.shiftDefinitionId = null;
      }

      if (editingRow?.scheduleId) {
        await updateRosterSchedule(editingRow.scheduleId, payload);
      } else {
        await createRosterSchedule(payload);
      }
      setShowModal(false);
      await loadMonth();
    } catch (err: unknown) {
      const msg =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      alert(msg || 'Save failed');
    }
  };

  const handleDelete = async (row: RosterScheduleRow) => {
    if (!currentOrganizationId || !row.scheduleId) return;
    if (!window.confirm('Remove this roster row?')) return;
    try {
      await deleteRosterSchedule(row.scheduleId, currentOrganizationId);
      await loadMonth();
    } catch (err: unknown) {
      const msg =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      alert(msg || 'Delete failed');
    }
  };

  if (!currentOrganizationId) {
    return <div className="hr-page">Select an organization.</div>;
  }

  const filteredEmployees = departmentId
    ? employees.filter((em) => em.departmentId === departmentId)
    : employees;

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Roster planner</h1>
          <p>Monthly assignments with holiday and approved-leave overlay (Phase C)</p>
        </div>
        <button type="button" className="btn-primary" onClick={openCreate}>
          + Add assignment
        </button>
      </div>

      <div className="form-section" style={{ marginBottom: '1rem' }}>
        <div className="form-row">
          <div className="form-group">
            <label>Year</label>
            <input
              type="number"
              value={year}
              min={2000}
              max={2100}
              onChange={(e) => setYear(parseInt(e.target.value, 10) || year)}
            />
          </div>
          <div className="form-group">
            <label>Month</label>
            <select value={month} onChange={(e) => setMonth(parseInt(e.target.value, 10))}>
              {Array.from({ length: 12 }, (_, i) => (
                <option key={i + 1} value={i + 1}>
                  {new Date(2000, i, 1).toLocaleString('default', { month: 'long' })}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Department filter</label>
            <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
              <option value="">All departments</option>
              {departments.map((d) => (
                <option key={deptKey(d)} value={deptKey(d)}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group" style={{ alignSelf: 'flex-end' }}>
            <button type="button" className="btn-outline" onClick={() => loadMonth()} disabled={loading}>
              Refresh
            </button>
          </div>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {loading && <div className="loading">Loading roster…</div>}

      {view && (
        <>
          {view.conflictWarnings?.length ? (
            <div className="form-section" style={{ borderLeft: '4px solid #c62828', marginBottom: '1rem' }}>
              <h2>Conflict hints</h2>
              <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
                {view.conflictWarnings.map((w, i) => (
                  <li key={`${w.warningType}-${w.date}-${w.employeeId}-${i}`}>
                    <strong>{w.warningType}</strong> — {w.employeeName || w.employeeId} on {w.date}:{' '}
                    {w.message}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Date</th>
                  <th>Shift</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>OT flag</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {!view.schedules?.length ? (
                  <tr>
                    <td colSpan={7} className="no-data">
                      No roster rows this month. Add assignments or widen filters.
                    </td>
                  </tr>
                ) : (
                  view.schedules.map((row) => (
                    <tr key={row.scheduleId}>
                      <td>{row.employeeName || row.employeeId}</td>
                      <td>{row.shiftDate?.substring(0, 10)}</td>
                      <td>{row.shiftDefinitionName || row.shiftName || '—'}</td>
                      <td>{row.startTime?.substring(0, 5)}</td>
                      <td>{row.endTime?.substring(0, 5)}</td>
                      <td>{row.isOvertime ? 'Yes' : 'No'}</td>
                      <td className="action-buttons">
                        <button type="button" className="btn-sm btn-edit" onClick={() => openEdit(row)}>
                          Edit
                        </button>
                        <button type="button" className="btn-sm btn-delete" onClick={() => handleDelete(row)}>
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="form-section" style={{ marginTop: '1.5rem' }}>
            <h2>Context — holidays &amp; approved leave this month</h2>
            <div className="form-grid">
              <div>
                <h3>Holidays ({view.holidays?.length ?? 0})</h3>
                <ul style={{ paddingLeft: '1.25rem' }}>
                  {(view.holidays || []).map((h) => (
                    <li key={String(h.holidayId)}>
                      {h.holidayDate?.substring(0, 10)} — {h.holidayName}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3>Approved leave ({view.approvedLeaves?.length ?? 0})</h3>
                <ul style={{ paddingLeft: '1.25rem' }}>
                  {(view.approvedLeaves || []).map((lr) => (
                    <li key={String(lr.leaveRequestId)}>
                      {lr.startDate?.substring(0, 10)} → {lr.endDate?.substring(0, 10)} (
                      {lr.employeeId})
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </>
      )}

      {showModal &&
        portalLayoutOverlay(
          <div
            className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
            style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
            onClick={() => setShowModal(false)}
          >
            <div className="modal-content" onClick={(ev) => ev.stopPropagation()}>
              <div className="modal-header">
                <h2>{editingRow ? 'Edit assignment' : 'New assignment'}</h2>
                <button type="button" className="modal-close" onClick={() => setShowModal(false)}>
                  ×
                </button>
              </div>
              <form onSubmit={submit}>
                <div className="form-group">
                  <label>Employee *</label>
                  <select
                    value={form.employeeId}
                    onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
                    required
                    disabled={!!editingRow}
                  >
                    <option value="">Select employee</option>
                    {(editingRow ? employees : filteredEmployees).map((em) => (
                      <option key={em.employeeId} value={em.employeeId}>
                        {em.name} ({em.employeeNumber})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Date *</label>
                  <input
                    type="date"
                    value={form.shiftDate}
                    onChange={(e) => setForm({ ...form, shiftDate: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Shift definition</label>
                  <select
                    value={form.shiftDefinitionId}
                    onChange={(e) => {
                      const v = e.target.value;
                      setForm({ ...form, shiftDefinitionId: v });
                      if (v) applyShiftDefinitionDefaults(v);
                    }}
                  >
                    <option value="">None (custom times)</option>
                    {shifts.map((s) => (
                      <option key={s.shiftDefinitionId} value={s.shiftDefinitionId}>
                        {s.code} — {s.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Label / notes name</label>
                  <input
                    value={form.shiftName}
                    onChange={(e) => setForm({ ...form, shiftName: e.target.value })}
                    placeholder="Optional display name"
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Start *</label>
                    <input
                      type="time"
                      value={form.startTime}
                      onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>End *</label>
                    <input
                      type="time"
                      value={form.endTime}
                      onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                      required
                    />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Break (minutes)</label>
                    <input
                      type="number"
                      min={0}
                      value={form.breakDuration}
                      onChange={(e) => setForm({ ...form, breakDuration: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>
                      <input
                        type="checkbox"
                        checked={form.isOvertime}
                        onChange={(e) => setForm({ ...form, isOvertime: e.target.checked })}
                      />{' '}
                      Mark as overtime shift
                    </label>
                  </div>
                </div>
                <div className="form-group">
                  <label>Notes</label>
                  <textarea
                    rows={2}
                    value={form.notes}
                    onChange={(e) => setForm({ ...form, notes: e.target.value })}
                  />
                </div>
                <div className="modal-actions">
                  <button type="button" className="btn-outline" onClick={() => setShowModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn-primary">
                    Save
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
    </div>
  );
};

export default RosterPlanner;
