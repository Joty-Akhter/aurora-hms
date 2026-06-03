import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import hospitalService, { Doctor, DoctorDepartment } from '../../services/hospitalService';
import { formatClinicalEnumLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

function formatDoctorAvailabilityLabel(status: string | undefined): string {
  if (!status) return '—';
  if (status === 'AVAILABLE') return 'Available';
  if (status === 'NOT_AVAILABLE') return 'Not available';
  return formatClinicalEnumLabel(status);
}

const DoctorList: React.FC = () => {
  const navigate = useNavigate();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [departments, setDepartments] = useState<DoctorDepartment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [availabilityFilter, setAvailabilityFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [showDeletedDoctors, setShowDeletedDoctors] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    loadDoctors();
    loadDepartments();
  }, []);

  const loadDoctors = async (includeDeletedOverride?: boolean) => {
    const includeDeleted =
      includeDeletedOverride !== undefined ? includeDeletedOverride : showDeletedDoctors;
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getDoctors(
        includeDeleted ? { includeInactive: true } : undefined
      );
      setDoctors(response.data);
      setPage(1);
    } catch (err: any) {
      console.error('Failed to load doctors:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load doctors'));
    } finally {
      setLoading(false);
    }
  };

  const loadDepartments = async () => {
    try {
      const response = await hospitalService.getActiveDoctorDepartments();
      setDepartments(response.data);
    } catch (err: any) {
      console.error('Failed to load departments:', err);
    }
  };

  const handleSearch = async (includeDeletedOverride?: boolean) => {
    const includeDeleted =
      includeDeletedOverride !== undefined ? includeDeletedOverride : showDeletedDoctors;
    if (!searchTerm.trim()) {
      loadDoctors(includeDeleted);
      return;
    }

    try {
      setLoading(true);
      const response = await hospitalService.searchDoctors(
        searchTerm,
        includeDeleted ? { includeInactive: true } : undefined
      );
      setDoctors(response.data);
      setPage(1);
    } catch (err: any) {
      console.error('Failed to search doctors:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search doctors'));
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setDepartmentFilter('');
    setAvailabilityFilter('');
    setTypeFilter('');
    setShowDeletedDoctors(false);
    setPage(1);
    loadDoctors(false);
  };

  useEffect(() => {
    setPage(1);
  }, [departmentFilter, availabilityFilter, typeFilter]);

  const filteredDoctors = useMemo(
    () =>
      doctors.filter((doctor) => {
        if (departmentFilter && doctor.departmentId !== departmentFilter) {
          return false;
        }
        if (availabilityFilter) {
          if (!doctor.isActive) return false;
          if (availabilityFilter === 'AVAILABLE' && doctor.availabilityStatus !== 'AVAILABLE') return false;
          if (availabilityFilter === 'NOT_AVAILABLE' && doctor.availabilityStatus !== 'NOT_AVAILABLE') return false;
        }
        if (typeFilter && doctor.doctorType !== typeFilter) {
          return false;
        }
        return true;
      }),
    [doctors, departmentFilter, availabilityFilter, typeFilter],
  );

  const totalFiltered = filteredDoctors.length;
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize));

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const safePage = Math.min(page, totalPages);
  const pageStart = (safePage - 1) * pageSize;
  const pageRows = filteredDoctors.slice(pageStart, pageStart + pageSize);

  const handleDelete = async (doctor: Doctor) => {
    if (!doctor.isActive) {
      return;
    }
    if (!window.confirm('Are you sure you want to deactivate this doctor? They will be removed from the active list.')) {
      return;
    }

    try {
      await hospitalService.deleteDoctor(doctor.doctorId);
      if (searchTerm.trim()) {
        await handleSearch();
      } else {
        await loadDoctors();
      }
    } catch (err: any) {
      console.error('Failed to delete doctor:', err);
      alert(ehrApiErrorMessage(err, 'Failed to deactivate doctor'));
    }
  };

  const getAvailabilityBadgeClass = (availabilityStatus: string) => {
    if (availabilityStatus === 'AVAILABLE') return 'badge-success';
    return 'badge-danger';
  };

  const getTypeDisplayName = (type: string) => {
    return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  };

  if (loading && doctors.length === 0) {
    return <div className="loading">Loading doctors...</div>;
  }

  return (
    <div className="hospital-page">
      <div className="page-header">
        <div>
          <h1>Doctors</h1>
          <p>Manage doctors and physicians</p>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn-secondary" onClick={() => navigate('/hospital/doctors/schedule')}>
            Manage Schedule & Off Days
          </button>
          <button className="btn-primary" onClick={() => navigate('/hospital/doctors/new')}>
            Add Doctor
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Search and Filters */}
      <div className="filters-section">
        <div className="filter-row" style={{ marginBottom: '8px', alignItems: 'center', gap: '8px' }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={showDeletedDoctors}
              onChange={(e) => {
                const checked = e.target.checked;
                setShowDeletedDoctors(checked);
                if (!searchTerm.trim()) {
                  void loadDoctors(checked);
                } else {
                  void handleSearch(checked);
                }
              }}
            />
            <span>Show deleted (inactive) doctors</span>
          </label>
        </div>
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search by name, code, or speciality..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          
          <select
            value={departmentFilter}
            onChange={(e) => {
              setDepartmentFilter(e.target.value);
              setPage(1);
            }}
            className="filter-select"
          >
            <option value="">All Departments</option>
            {departments.map(dept => (
              <option key={dept.departmentId} value={dept.departmentId}>
                {dept.departmentName}
              </option>
            ))}
          </select>

          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setPage(1);
            }}
            className="filter-select"
          >
            <option value="">All Types</option>
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

          <select
            value={availabilityFilter}
            onChange={(e) => {
              setAvailabilityFilter(e.target.value);
              setPage(1);
            }}
            className="filter-select"
            title="Filter by day-to-day appointment availability (not prescription or record status)"
          >
            <option value="">All availability</option>
            <option value="AVAILABLE">Available</option>
            <option value="NOT_AVAILABLE">Not available</option>
          </select>

          <button className="btn-secondary" onClick={() => void handleSearch()}>
            Search
          </button>
          <button className="btn-secondary" onClick={handleClearFilters}>
            Clear
          </button>
        </div>
      </div>

      {/* Doctor List */}
      {totalFiltered === 0 ? (
        <div className="empty-state">
          <p>No doctors found</p>
        </div>
      ) : (
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Department</th>
              <th>Type</th>
              <th>Speciality</th>
              <th>Phone</th>
              <th>Availability</th>
              {showDeletedDoctors && <th>Record</th>}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
              {pageRows.map((doctor) => (
                <tr key={doctor.doctorId}>
                  <td>{doctor.doctorCode}</td>
                  <td>{doctor.doctorName}</td>
                  <td>{doctor.departmentName}</td>
                  <td>{getTypeDisplayName(doctor.doctorType)}</td>
                  <td>{doctor.speciality || '-'}</td>
                  <td>{doctor.phoneNumber || '-'}</td>
                  <td>
                    {doctor.isActive ? (
                      <span className={`badge ${getAvailabilityBadgeClass(doctor.availabilityStatus)}`}>
                        {formatDoctorAvailabilityLabel(doctor.availabilityStatus)}
                      </span>
                    ) : (
                      <span className="text-muted" style={{ fontSize: '13px' }}>—</span>
                    )}
                  </td>
                  {showDeletedDoctors && (
                    <td>
                      <span className={`badge ${doctor.isActive ? 'badge-success' : 'badge-warning'}`}>
                        {doctor.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                  )}
                  <td>
                    <button
                      className="btn-link"
                      onClick={() => navigate(`/hospital/doctors/${doctor.doctorId}`)}
                      title="Edit"
                    >
                      Edit
                    </button>
                    {doctor.isActive && doctor.prescriptionStatus !== 'INACTIVE' ? (
                      <button
                        className="btn-link text-danger"
                        onClick={() => void handleDelete(doctor)}
                        title="Deactivate (remove from active list)"
                      >
                        Delete
                      </button>
                    ) : (
                      <span
                        className="text-muted"
                        style={{ fontSize: '13px' }}
                        title={doctor.isActive ? 'Prescription status is inactive' : 'Already deactivated'}
                      >
                        {doctor.isActive ? 'Inactive' : 'Deactivated'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      )}

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
            Showing {totalFiltered === 0 ? 0 : pageStart + 1}–{Math.min(pageStart + pageSize, totalFiltered)} of{' '}
            {totalFiltered} doctor(s)
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

export default DoctorList;
