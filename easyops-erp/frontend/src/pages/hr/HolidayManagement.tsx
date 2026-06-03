import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getHolidays,
  createHoliday,
  updateHoliday,
  deleteHoliday,
  getDepartments,
  getEmployees,
  HolidayRecord,
  Department,
  Employee,
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

type Scope = 'org' | 'department' | 'employee';

const resolveDepartmentId = (d: Pick<Department, 'departmentId' | 'id'> | null | undefined) =>
  d?.departmentId ?? d?.id ?? '';

const HolidayManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [holidays, setHolidays] = useState<HolidayRecord[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<HolidayRecord | null>(null);
  const [scope, setScope] = useState<Scope>('org');
  const [formData, setFormData] = useState({
    holidayName: '',
    holidayDate: '',
    holidayType: 'public',
    description: '',
    departmentId: '',
    employeeId: '',
    isActive: true,
  });

  const yearRange = useMemo(() => {
    const y = new Date().getFullYear();
    return { start: `${y}-01-01`, end: `${y + 1}-12-31` };
  }, []);

  useEffect(() => {
    if (currentOrganizationId) loadAll();
  }, [currentOrganizationId]);

  const loadAll = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const [hRes, dRes, eRes] = await Promise.all([
        getHolidays(currentOrganizationId, { startDate: yearRange.start, endDate: yearRange.end }),
        getDepartments(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' }),
      ]);
      setHolidays(Array.isArray(hRes.data) ? hRes.data : []);
      setDepartments(dRes.data);
      setEmployees(eRes.data);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to load holidays');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    setScope('org');
    setFormData({
      holidayName: '',
      holidayDate: '',
      holidayType: 'public',
      description: '',
      departmentId: '',
      employeeId: '',
      isActive: true,
    });
    setShowModal(true);
  };

  const openEdit = (h: HolidayRecord) => {
    setEditing(h);
    let sc: Scope = 'org';
    if (h.employeeId) sc = 'employee';
    else if (h.departmentId) sc = 'department';
    setScope(sc);
    setFormData({
      holidayName: h.holidayName,
      holidayDate: h.holidayDate?.substring(0, 10) || '',
      holidayType: h.holidayType || 'public',
      description: h.description || '',
      departmentId: h.departmentId || '',
      employeeId: h.employeeId || '',
      isActive: h.isActive !== false,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditing(null);
  };

  const scopePayload = (): Pick<HolidayRecord, 'departmentId' | 'employeeId'> => {
    if (scope === 'department')
      return { departmentId: formData.departmentId || null, employeeId: null };
    if (scope === 'employee')
      return {
        departmentId: null,
        employeeId: formData.employeeId || null,
      };
    return { departmentId: null, employeeId: null };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;
    const payload: HolidayRecord = {
      organizationId: currentOrganizationId,
      holidayName: formData.holidayName.trim(),
      holidayDate: formData.holidayDate,
      holidayType: formData.holidayType || 'public',
      description: formData.description || undefined,
      isRecurring: false,
      isActive: formData.isActive,
      ...scopePayload(),
    };
    try {
      if (editing?.holidayId) {
        await updateHoliday(editing.holidayId, payload);
      } else {
        await createHoliday(payload);
      }
      closeModal();
      await loadAll();
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'Save failed');
    }
  };

  const handleDelete = async (h: HolidayRecord) => {
    if (!currentOrganizationId || !h.holidayId) return;
    if (!window.confirm(`Delete holiday "${h.holidayName}" on ${h.holidayDate}?`)) return;
    try {
      await deleteHoliday(h.holidayId, currentOrganizationId);
      await loadAll();
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'Delete failed');
    }
  };

  const scopeLabel = (h: HolidayRecord) => {
    if (h.employeeId) return 'Employee';
    if (h.departmentId) return 'Department';
    return 'Organization';
  };

  if (!currentOrganizationId) return <div className="hr-page"><p>Select an organization.</p></div>;
  if (loading) return <div className="loading">Loading holidays…</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Holidays</h1>
        <p>
          Maintain institutional holidays for payroll LOP rules (Phase B). Scoped holidays apply only to matching
          employees when excluding holidays from working days or inferred LOP. Org-wide holidays (no scope) feed loan due-date shifting.
        </p>
        <button type="button" className="btn-primary" onClick={openCreate}>
          + Add holiday
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="hr-section">
        <p style={{ color: '#666', marginBottom: '0.75rem' }}>
          Showing {yearRange.start} — {yearRange.end}. Payroll integration uses active holidays via{' '}
          <strong>Payroll Runs → Time &amp; attendance policy</strong>.
        </p>
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Name</th>
              <th>Type</th>
              <th>Scope</th>
              <th>Active</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {holidays
              .slice()
              .sort((a, b) => (a.holidayDate || '').localeCompare(b.holidayDate || ''))
              .map((h) => (
                <tr key={h.holidayId}>
                  <td>{h.holidayDate?.substring(0, 10)}</td>
                  <td>{h.holidayName}</td>
                  <td>{h.holidayType || '—'}</td>
                  <td>{scopeLabel(h)}</td>
                  <td>{h.isActive !== false ? 'Yes' : 'No'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button type="button" className="btn-secondary" onClick={() => openEdit(h)}>
                      Edit
                    </button>{' '}
                    <button type="button" className="btn-secondary" onClick={() => handleDelete(h)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {showModal &&
        portalLayoutOverlay(
          <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={closeModal}>
            <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
              <h2>{editing ? 'Edit holiday' : 'New holiday'}</h2>
              <form onSubmit={handleSubmit}>
                <div className="form-row">
                  <label>Name *</label>
                  <input
                    required
                    value={formData.holidayName}
                    onChange={(e) => setFormData({ ...formData, holidayName: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <label>Date *</label>
                  <input
                    type="date"
                    required
                    value={formData.holidayDate}
                    onChange={(e) => setFormData({ ...formData, holidayDate: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <label>Type</label>
                  <input
                    value={formData.holidayType}
                    onChange={(e) => setFormData({ ...formData, holidayType: e.target.value })}
                    placeholder="public"
                  />
                </div>
                <div className="form-row">
                  <label>Payroll scope</label>
                  <select value={scope} onChange={(e) => setScope(e.target.value as Scope)}>
                    <option value="org">Whole organization</option>
                    <option value="department">Single department</option>
                    <option value="employee">Single employee</option>
                  </select>
                </div>
                {scope === 'department' && (
                  <div className="form-row">
                    <label>Department</label>
                    <select
                      required
                      value={formData.departmentId}
                      onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                    >
                      <option value="">Select…</option>
                      {departments.map((d) => (
                        <option key={resolveDepartmentId(d)} value={resolveDepartmentId(d)}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
                {scope === 'employee' && (
                  <div className="form-row">
                    <label>Employee</label>
                    <select
                      required
                      value={formData.employeeId}
                      onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                    >
                      <option value="">Select…</option>
                      {employees.map((em) => (
                        <option key={em.employeeId} value={em.employeeId}>
                          {em.employeeNumber} — {em.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
                <div className="form-row">
                  <label>Description</label>
                  <textarea
                    rows={2}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  />
                </div>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                  />
                  Active
                </label>
                <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                  <button type="submit" className="btn-primary">
                    Save
                  </button>
                  <button type="button" className="btn-secondary" onClick={closeModal}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
    </div>
  );
};

export default HolidayManagement;
