import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getShiftDefinitions,
  createShiftDefinition,
  updateShiftDefinition,
  ShiftDefinition,
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_DETECT_CLASS,
  LAYOUT_OVERLAY_ROOT_Z,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

const ShiftDefinitions: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [rows, setRows] = useState<ShiftDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<ShiftDefinition | null>(null);
  const [form, setForm] = useState({
    code: '',
    name: '',
    shiftType: 'DAY',
    graceMinutes: '0',
    expectedHours: '8',
    overtimeRateMultiplier: '',
    isActive: true,
  });

  const load = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const res = await getShiftDefinitions(currentOrganizationId);
      setRows(Array.isArray(res.data) ? res.data : []);
    } catch (e: unknown) {
      const msg =
        typeof e === 'object' && e !== null && 'response' in e
          ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || 'Failed to load shift definitions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [currentOrganizationId]);

  const openCreate = () => {
    setEditing(null);
    setForm({
      code: '',
      name: '',
      shiftType: 'DAY',
      graceMinutes: '0',
      expectedHours: '8',
      overtimeRateMultiplier: '',
      isActive: true,
    });
    setShowModal(true);
  };

  const openEdit = (r: ShiftDefinition) => {
    setEditing(r);
    setForm({
      code: r.code || '',
      name: r.name || '',
      shiftType: r.shiftType || 'DAY',
      graceMinutes: String(r.graceMinutes ?? 0),
      expectedHours: String(r.expectedHours ?? 8),
      overtimeRateMultiplier:
        r.overtimeRateMultiplier !== undefined && r.overtimeRateMultiplier !== null
          ? String(r.overtimeRateMultiplier)
          : '',
      isActive: r.isActive !== false,
    });
    setShowModal(true);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;
    try {
      const payload: Partial<ShiftDefinition> = {
        organizationId: currentOrganizationId,
        code: form.code.trim(),
        name: form.name.trim(),
        shiftType: form.shiftType,
        graceMinutes: parseInt(form.graceMinutes, 10) || 0,
        expectedHours: parseFloat(form.expectedHours) || 8,
        isActive: form.isActive,
      };
      const ot = form.overtimeRateMultiplier.trim();
      if (ot !== '') {
        payload.overtimeRateMultiplier = parseFloat(ot);
      } else {
        payload.overtimeRateMultiplier = null;
      }

      if (editing?.shiftDefinitionId) {
        await updateShiftDefinition(editing.shiftDefinitionId, payload);
      } else {
        await createShiftDefinition(payload);
      }
      setShowModal(false);
      await load();
    } catch (err: unknown) {
      const msg =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      alert(msg || 'Save failed');
    }
  };

  if (!currentOrganizationId) {
    return <div className="hr-page">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading shift definitions…</div>;
  }

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Shift definitions</h1>
          <p>HMS Phase C — catalog shifts for roster planning and OT band defaults</p>
        </div>
        <button type="button" className="btn-primary" onClick={openCreate}>
          + Add shift
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Type</th>
              <th>Grace (min)</th>
              <th>Expected hrs</th>
              <th>OT multiplier</th>
              <th>Active</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={8} className="no-data">
                  No shift definitions yet.
                </td>
              </tr>
            ) : (
              rows.map((r) => (
                <tr key={r.shiftDefinitionId}>
                  <td><strong>{r.code}</strong></td>
                  <td>{r.name}</td>
                  <td>{r.shiftType}</td>
                  <td>{r.graceMinutes ?? 0}</td>
                  <td>{r.expectedHours ?? '—'}</td>
                  <td>{r.overtimeRateMultiplier ?? '—'}</td>
                  <td>{r.isActive !== false ? 'Yes' : 'No'}</td>
                  <td className="action-buttons">
                    <button type="button" className="btn-sm btn-edit" onClick={() => openEdit(r)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal &&
        portalLayoutOverlay(
          <div
            className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
            style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
            onClick={() => setShowModal(false)}
          >
            <div className="modal-content" onClick={(ev) => ev.stopPropagation()}>
              <div className="modal-header">
                <h2>{editing ? 'Edit shift' : 'New shift'}</h2>
                <button type="button" className="modal-close" onClick={() => setShowModal(false)}>
                  ×
                </button>
              </div>
              <form onSubmit={submit}>
                <div className="form-group">
                  <label>Code *</label>
                  <input
                    value={form.code}
                    onChange={(e) => setForm({ ...form, code: e.target.value })}
                    required
                    disabled={!!editing}
                  />
                </div>
                <div className="form-group">
                  <label>Name *</label>
                  <input
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    required
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Type</label>
                    <select
                      value={form.shiftType}
                      onChange={(e) => setForm({ ...form, shiftType: e.target.value })}
                    >
                      <option value="DAY">DAY</option>
                      <option value="NIGHT">NIGHT</option>
                      <option value="ROTATIONAL">ROTATIONAL</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Grace (minutes)</label>
                    <input
                      type="number"
                      min={0}
                      value={form.graceMinutes}
                      onChange={(e) => setForm({ ...form, graceMinutes: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Expected hours / day</label>
                    <input
                      type="number"
                      step="0.25"
                      min={0}
                      value={form.expectedHours}
                      onChange={(e) => setForm({ ...form, expectedHours: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>OT multiplier (optional)</label>
                    <input
                      type="number"
                      step="0.01"
                      min={0}
                      placeholder="Use org policy if empty"
                      value={form.overtimeRateMultiplier}
                      onChange={(e) => setForm({ ...form, overtimeRateMultiplier: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>
                    <input
                      type="checkbox"
                      checked={form.isActive}
                      onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                    />{' '}
                    Active
                  </label>
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

export default ShiftDefinitions;
