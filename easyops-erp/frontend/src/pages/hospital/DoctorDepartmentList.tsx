import React, { useEffect, useMemo, useRef, useState } from 'react';
import hospitalService, { DoctorDepartment, DoctorDepartmentRequest } from '../../services/hospitalService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const DoctorDepartmentList: React.FC = () => {
  const [departments, setDepartments] = useState<DoctorDepartment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showInactiveDepartments, setShowInactiveDepartments] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<DoctorDepartment | null>(null);
  const [viewing, setViewing] = useState<DoctorDepartment | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const formRef = useRef<HTMLDivElement>(null);
  const [formData, setFormData] = useState<DoctorDepartmentRequest>({
    departmentName: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    loadDepartments();
  }, []);

  const scrollToForm = () => {
    requestAnimationFrame(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  const loadDepartments = async (includeInactiveOverride?: boolean) => {
    const includeInactive =
      includeInactiveOverride !== undefined ? includeInactiveOverride : showInactiveDepartments;
    try {
      setLoading(true);
      setError(null);
      const response = includeInactive
        ? await hospitalService.getDoctorDepartments()
        : await hospitalService.getActiveDoctorDepartments();
      setDepartments(response.data);
      setPage(1);
    } catch (err: any) {
      console.error('Failed to load departments:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load departments'));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (includeInactiveOverride?: boolean) => {
    const includeInactive =
      includeInactiveOverride !== undefined ? includeInactiveOverride : showInactiveDepartments;
    if (!searchTerm.trim()) {
      loadDepartments(includeInactive);
      return;
    }

    try {
      setLoading(true);
      if (includeInactive) {
        const response = await hospitalService.getDoctorDepartments();
        const term = searchTerm.trim().toLowerCase();
        setDepartments(
          response.data.filter((department) =>
            department.departmentName.toLowerCase().includes(term)
          )
        );
      } else {
        const response = await hospitalService.searchDoctorDepartments(searchTerm);
        setDepartments(response.data);
      }
      setPage(1);
    } catch (err: any) {
      console.error('Failed to search departments:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search departments'));
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setShowInactiveDepartments(false);
    loadDepartments(false);
  };

  const handleDelete = async (departmentId: string) => {
    if (!window.confirm('Are you sure you want to deactivate this department?')) {
      return;
    }

    try {
      await hospitalService.deleteDoctorDepartment(departmentId);
      loadDepartments();
    } catch (err: any) {
      console.error('Failed to delete department:', err);
      alert(ehrApiErrorMessage(err, 'Failed to delete department'));
    }
  };

  const handleView = (department: DoctorDepartment) => {
    setViewing(department);
  };

  const handleEdit = (department: DoctorDepartment) => {
    setFormError(null);
    setViewing(null);
    setEditing(department);
    setFormData({
      departmentName: department.departmentName,
      status: department.status,
    });
    setShowForm(true);
    scrollToForm();
  };

  const handleAdd = () => {
    setFormError(null);
    setViewing(null);
    setEditing(null);
    setFormData({
      departmentName: '',
      status: 'ACTIVE',
    });
    setShowForm(true);
    scrollToForm();
  };

  const getDepartmentSaveErrorMessage = (err: any) => {
    const statusCode = err?.response?.status;
    const backendMessage = ehrApiErrorMessage(err, '');
    const normalizedMessage = backendMessage.toLowerCase();

    if (
      statusCode === 409 ||
      normalizedMessage.includes('already exists') ||
      normalizedMessage.includes('duplicate') ||
      normalizedMessage.includes('same name')
    ) {
      return 'Same department already added in the list. Please use a different name.';
    }

    return backendMessage || 'Failed to save department';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setFormError(null);
      if (editing) {
        await hospitalService.updateDoctorDepartment(editing.departmentId, formData);
      } else {
        await hospitalService.createDoctorDepartment(formData);
      }
      setShowForm(false);
      setEditing(null);
      loadDepartments();
    } catch (err: any) {
      console.error('Failed to save department:', err);
      setFormError(getDepartmentSaveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'status-badge status-active';
      case 'INACTIVE':
        return 'status-badge status-inactive';
      default:
        return 'status-badge';
    }
  };

  const totalFiltered = departments.length;
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize));
  const safePage = Math.min(page, totalPages);
  const pageStart = (safePage - 1) * pageSize;
  const pageRows = useMemo(
    () => departments.slice(pageStart, pageStart + pageSize),
    [departments, pageStart, pageSize],
  );

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  if (loading && departments.length === 0) {
    return <div className="loading">Loading departments...</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>Doctor Departments</h1>
          <p>Manage doctor departments and specialties</p>
        </div>
        <button className="btn-primary" onClick={handleAdd}>
          Add Department
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Create/Edit Form */}
      {showForm && (
        <div ref={formRef} className="form-container">
          <h3>{editing ? 'Edit' : 'Add'} Department</h3>
          {formError && <div className="error-message">{formError}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Department Name *</label>
                <input
                  type="text"
                  required
                  value={formData.departmentName}
                  onChange={(e) => setFormData({ ...formData, departmentName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Status *</label>
                <select
                  required
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as 'ACTIVE' | 'INACTIVE' })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? 'Saving...' : 'Save'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); setFormError(null); }}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {viewing && (
        <div className="form-container" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
            <h3 style={{ margin: 0 }}>Department Details</h3>
            <button type="button" className="btn-secondary" onClick={() => setViewing(null)}>
              Close
            </button>
          </div>
          <div className="info-grid" style={{ marginTop: 16 }}>
            <div className="info-item">
              <span className="info-label">Department Name</span>
              <span className="info-value"><strong>{viewing.departmentName}</strong></span>
            </div>
            <div className="info-item">
              <span className="info-label">Status</span>
              <span className="info-value">
                <span className={getStatusBadgeClass(viewing.status)}>{viewing.status}</span>
              </span>
            </div>
            <div className="info-item">
              <span className="info-label">Created At</span>
              <span className="info-value">{new Date(viewing.createdAt).toLocaleString()}</span>
            </div>
            {viewing.updatedAt && (
              <div className="info-item">
                <span className="info-label">Updated At</span>
                <span className="info-value">{new Date(viewing.updatedAt).toLocaleString()}</span>
              </div>
            )}
          </div>
          <div className="form-actions" style={{ marginTop: 16 }}>
            <button type="button" className="btn-primary" onClick={() => handleEdit(viewing)}>
              Edit
            </button>
          </div>
        </div>
      )}

      {/* Search and Filters */}
      <div className="filters-section">
        <div className="filter-row" style={{ marginBottom: '8px', alignItems: 'center', gap: '8px' }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={showInactiveDepartments}
              onChange={(e) => {
                const checked = e.target.checked;
                setShowInactiveDepartments(checked);
                if (!searchTerm.trim()) {
                  void loadDepartments(checked);
                } else {
                  void handleSearch(checked);
                }
              }}
            />
            <span>Show inactive departments</span>
          </label>
        </div>
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search by department name..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          
          <button className="btn-secondary" onClick={handleSearch}>
            Search
          </button>
          <button className="btn-secondary" onClick={handleClearFilters}>
            Clear
          </button>
        </div>
      </div>

      {/* Department List */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Department Name</th>
              <th>Status</th>
              <th>Created At</th>
              <th className="col-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            {pageRows.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center">
                  No departments found
                </td>
              </tr>
            ) : (
              pageRows.map((dept) => (
                <tr key={dept.departmentId} style={dept.status === 'INACTIVE' ? { opacity: 0.75 } : undefined}>
                  <td>{dept.departmentName}</td>
                  <td>
                    <span className={getStatusBadgeClass(dept.status)}>
                      {dept.status}
                    </span>
                  </td>
                  <td>{new Date(dept.createdAt).toLocaleDateString()}</td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button
                        className="btn-link"
                        onClick={() => handleView(dept)}
                        title="View"
                      >
                        View
                      </button>
                      <button
                        className="btn-link"
                        onClick={() => handleEdit(dept)}
                        title="Edit"
                      >
                        Edit
                      </button>
                      {dept.status === 'ACTIVE' && (
                        <button
                          className="btn-link text-danger"
                          onClick={() => handleDelete(dept.departmentId)}
                          title="Deactivate"
                        >
                          Deactivate
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalFiltered > 0 && (
        <div
          className="table-footer"
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
          }}
        >
          <p style={{ margin: 0 }}>
            Showing {pageStart + 1}–{Math.min(pageStart + pageSize, totalFiltered)} of {totalFiltered} department(s)
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px' }}>
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '14px' }}>
              Per page
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setPage(1);
                }}
                className="filter-select"
                style={{ minWidth: '72px' }}
              >
                {[10, 25, 50, 100].map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <button
                type="button"
                className="btn-secondary"
                disabled={safePage <= 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                Previous
              </button>
              <span style={{ fontSize: '14px', color: 'var(--text-secondary, #6b7280)' }}>
                Page {safePage} / {totalPages}
              </span>
              <button
                type="button"
                className="btn-secondary"
                disabled={safePage >= totalPages}
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DoctorDepartmentList;
