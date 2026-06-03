import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import hospitalService, { Patient } from '../../services/hospitalService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { useAuth } from '../../contexts/AuthContext';
import './Hospital.css';

const PatientList: React.FC = () => {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const ADMIN_ROLES = ['SYSTEM_ADMIN', 'SYSTEM_ADMINISTRATOR', 'SUPER_ADMIN', 'ORG_ADMIN'];
  const viewOnly = hasRole('PRESCRIBING_AUTHORITY') && !ADMIN_ROLES.some((r) => hasRole(r));
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(true);
  const [initialLoadDone, setInitialLoadDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await hospitalService.getPatients();
      setPatients(response.data);
      setPage(1);
    } catch (err: any) {
      console.error('Failed to load patients:', err);
      setError(ehrApiErrorMessage(err, 'Failed to load patients'));
    } finally {
      setLoading(false);
      setInitialLoadDone(true);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadPatients();
      return;
    }

    try {
      setLoading(true);
      const response = await hospitalService.searchPatients(searchTerm);
      setPatients(response.data);
      setPage(1);
    } catch (err: any) {
      console.error('Failed to search patients:', err);
      setError(ehrApiErrorMessage(err, 'Failed to search patients'));
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setStatusFilter('');
    setPage(1);
    loadPatients();
  };

  useEffect(() => {
    setPage(1);
  }, [statusFilter]);

  const filteredPatients = useMemo(
    () => (statusFilter ? patients.filter((p) => p.patientStatus === statusFilter) : patients),
    [patients, statusFilter],
  );

  const totalFiltered = filteredPatients.length;
  const totalPages = Math.max(1, Math.ceil(totalFiltered / pageSize));

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);
  const safePage = Math.min(page, totalPages);
  const pageStart = (safePage - 1) * pageSize;
  const pageRows = filteredPatients.slice(pageStart, pageStart + pageSize);

  const handleDelete = async (patientId: string) => {
    if (!window.confirm('Are you sure you want to archive this patient?')) {
      return;
    }

    try {
      await hospitalService.deletePatient(patientId);
      loadPatients();
    } catch (err: any) {
      console.error('Failed to delete patient:', err);
      alert(ehrApiErrorMessage(err, 'Failed to delete patient'));
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'status-badge status-active';
      case 'INACTIVE': return 'status-badge status-inactive';
      case 'DECEASED': return 'status-badge status-deceased';
      case 'ARCHIVED': return 'status-badge status-archived';
      default: return 'status-badge';
    }
  };

  if (loading && !initialLoadDone) {
    return <div className="loading">Loading patients...</div>;
  }

  return (
    <div className="hospital-page patient-list-page">
      <div className="page-header">
        <div>
          <h1>Patient Management</h1>
          <p>Manage patient registration and demographics</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/hospital/patients/new')}>
          + Register New Patient
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Search and Filters */}
      <div className="filters-section">
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search by name, MRN, DOB, ID number, phone, or email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
            className="filter-select"
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="ARCHIVED">Archived</option>
          </select>

          <button className="btn-secondary" onClick={handleSearch}>
            Search
          </button>
          <button className="btn-secondary" onClick={handleClearFilters}>
            Clear
          </button>
        </div>
      </div>

      {/* Patient List — bounded height so horizontal scroll stays near the table */}
      <div className="table-container patient-list-table-scroll">
        <table className="data-table">
          <thead>
            <tr>
              <th>MRN</th>
              <th style={{ minWidth: '220px' }}>Name</th>
              <th>Age</th>
              <th>Gender</th>
              <th>Phone</th>
              <th>Status</th>
              <th className="col-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            {totalFiltered === 0 ? (
              <tr>
                <td colSpan={7} className="no-data">
                  No patients found
                </td>
              </tr>
            ) : (
              pageRows.map((patient) => (
                <tr key={patient.patientId}>
                  <td>{patient.mrn}</td>
                  <td>
                    {patient.fullName || '—'}
                    {patient.preferredName && (
                      <span className="preferred-name"> ({patient.preferredName})</span>
                    )}
                  </td>
                  <td>{formatAge(patient.dateOfBirth) ?? '-'}</td>
                  <td>{formatGenderLabel(patient.gender) || '-'}</td>
                  <td>{patient.primaryPhone || '-'}</td>
                  <td>
                    <span className={getStatusBadgeClass(patient.patientStatus)}>
                      {patient.patientStatus}
                    </span>
                  </td>
                  <td className="col-actions">
                    <div className="action-buttons">
                      <button
                        className="btn-link"
                        onClick={() => navigate(`/hospital/patients/${patient.patientId}`)}
                      >
                        View
                      </button>
                      {!viewOnly && (
                        <button
                          className="btn-link"
                          onClick={() => navigate(`/hospital/patients/${patient.patientId}/edit`)}
                        >
                          Edit
                        </button>
                      )}
                      {!viewOnly && patient.patientStatus === 'ACTIVE' && (
                        <button
                          className="btn-link btn-danger"
                          onClick={() => handleDelete(patient.patientId)}
                        >
                          Archive
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
        <div className="table-footer" style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          <p style={{ margin: 0 }}>
            Showing {totalFiltered === 0 ? 0 : pageStart + 1}–{Math.min(pageStart + pageSize, totalFiltered)} of {totalFiltered} patient(s)
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
                {[20, 50, 100].map((n) => (
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

export default PatientList;
