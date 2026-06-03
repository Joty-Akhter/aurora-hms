import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getEmployeesPaged,
  deleteEmployee,
  Employee,
  getDepartments,
  Department,
  getPositions,
  Position,
} from '../../services/hrService';
import './Hr.css';

const PAGE_SIZE_OPTIONS = [20, 50, 100];

const EmployeeList: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [appliedStatus, setAppliedStatus] = useState('');
  const [appliedDepartment, setAppliedDepartment] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadEmployees = useCallback(async () => {
    if (!currentOrganizationId) return;

    try {
      setTableLoading(true);
      setError(null);

      const params: {
        page: number;
        size: number;
        status?: string;
        departmentId?: string;
        search?: string;
      } = { page, size: pageSize };

      if (appliedStatus) params.status = appliedStatus;
      if (appliedDepartment) params.departmentId = appliedDepartment;
      if (appliedSearch.trim()) params.search = appliedSearch.trim();

      const response = await getEmployeesPaged(currentOrganizationId, params);
      setEmployees(response.data.content ?? []);
      setTotalElements(response.data.totalElements);
      setTotalPages(response.data.totalPages);
    } catch (err: any) {
      console.error('Failed to load employees:', err);
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to load employees');
    } finally {
      setLoading(false);
      setTableLoading(false);
    }
  }, [currentOrganizationId, page, pageSize, appliedStatus, appliedDepartment, appliedSearch]);

  useEffect(() => {
    if (!currentOrganizationId) return;

    const loadReferenceData = async () => {
      try {
        const [departmentsRes, positionsRes] = await Promise.all([
          getDepartments(currentOrganizationId, { activeOnly: true }),
          getPositions(currentOrganizationId, { activeOnly: true }),
        ]);
        setDepartments(departmentsRes.data);
        setPositions(positionsRes.data);
      } catch (err) {
        console.error('Failed to load reference data:', err);
      }
    };

    void loadReferenceData();
  }, [currentOrganizationId]);

  useEffect(() => {
    void loadEmployees();
  }, [loadEmployees]);

  useEffect(() => {
    if (totalPages > 0 && page >= totalPages) {
      setPage(totalPages - 1);
    }
  }, [totalPages, page]);

  const handleSearch = () => {
    setAppliedSearch(searchTerm);
    setAppliedStatus(statusFilter);
    setAppliedDepartment(departmentFilter);
    setPage(0);
    if (
      searchTerm === appliedSearch &&
      statusFilter === appliedStatus &&
      departmentFilter === appliedDepartment &&
      page === 0
    ) {
      void loadEmployees();
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setStatusFilter('');
    setDepartmentFilter('');
    setAppliedSearch('');
    setAppliedStatus('');
    setAppliedDepartment('');
    setPage(0);
    if (!appliedSearch && !appliedStatus && !appliedDepartment) {
      void loadEmployees();
    }
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPageSize(Number(e.target.value));
    setPage(0);
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to deactivate this employee?')) {
      return;
    }

    try {
      await deleteEmployee(id);
      void loadEmployees();
    } catch (err: any) {
      console.error('Failed to delete employee:', err);
      alert(err.response?.data?.message || 'Failed to delete employee');
    }
  };

  const getDepartmentName = (departmentId?: string) => {
    if (!departmentId) return '-';
    const dept = departments.find((d) => d.departmentId === departmentId);
    return dept?.name || '-';
  };

  const getPositionTitle = (positionId?: string) => {
    if (!positionId) return '-';
    const pos = positions.find((p) => p.positionId === positionId);
    return pos?.title || '-';
  };

  const getStatusBadgeClass = (status?: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'status-badge status-active';
      case 'ON_LEAVE':
        return 'status-badge status-on-leave';
      case 'TERMINATED':
        return 'status-badge status-terminated';
      default:
        return 'status-badge';
    }
  };

  const showingFrom = totalElements === 0 ? 0 : page * pageSize + 1;
  const showingTo = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Employee Management</h1>
          <p>Manage employee information and records</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/hr/employees/new')}>
          + Add Employee
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <form
        className="filters-section"
        onSubmit={(e) => {
          e.preventDefault();
          handleSearch();
        }}
      >
        <div className="filter-row">
          <input
            type="text"
            placeholder="Search by name, email, or employee number..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="ON_LEAVE">On Leave</option>
            <option value="TERMINATED">Terminated</option>
          </select>

          <select
            value={departmentFilter}
            onChange={(e) => setDepartmentFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">All Departments</option>
            {departments.map((dept) => (
              <option key={dept.departmentId} value={dept.departmentId}>
                {dept.name}
              </option>
            ))}
          </select>

          <button type="submit" className="btn-secondary">
            Search
          </button>
          <button type="button" className="btn-outline" onClick={handleClearFilters}>
            Clear
          </button>
        </div>
      </form>

      {loading && employees.length === 0 ? (
        <div className="loading">Loading employees...</div>
      ) : (
        <>
          <div className={`table-container${tableLoading ? ' table-loading' : ''}`}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee #</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Department</th>
                  <th>Position</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Hire Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {employees.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="no-data">
                      No employees found. Click &quot;Add Employee&quot; to create one.
                    </td>
                  </tr>
                ) : (
                  employees.map((employee) => (
                    <tr key={employee.employeeId}>
                      <td>{employee.employeeNumber}</td>
                      <td>
                        <strong>{employee.name}</strong>
                      </td>
                      <td>{employee.email}</td>
                      <td>{employee.phone || '-'}</td>
                      <td>{getDepartmentName(employee.departmentId)}</td>
                      <td>{getPositionTitle(employee.positionId)}</td>
                      <td>{employee.employmentType || '-'}</td>
                      <td>
                        <span className={getStatusBadgeClass(employee.employmentStatus)}>
                          {employee.employmentStatus || 'UNKNOWN'}
                        </span>
                      </td>
                      <td>{employee.hireDate ? new Date(employee.hireDate).toLocaleDateString() : '-'}</td>
                      <td className="action-buttons">
                        <button
                          className="btn-sm btn-view"
                          onClick={() => navigate(`/hr/employees/${employee.employeeId}`)}
                        >
                          View
                        </button>
                        <button
                          className="btn-sm btn-edit"
                          onClick={() => navigate(`/hr/employees/${employee.employeeId}/edit`)}
                        >
                          Edit
                        </button>
                        {employee.employmentStatus === 'ACTIVE' && (
                          <button
                            className="btn-sm btn-delete"
                            onClick={() => handleDelete(employee.employeeId!)}
                          >
                            Deactivate
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="pagination-bar">
            <div className="pagination-info">
              Showing {showingFrom}–{showingTo} of {totalElements} employees
            </div>
            <div className="pagination-controls">
              <label htmlFor="page-size">Page size</label>
              <select id="page-size" value={pageSize} onChange={handlePageSizeChange} className="filter-select">
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="btn-outline"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </button>
              <span className="pagination-page-label">
                Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
              </span>
              <button
                type="button"
                className="btn-outline"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default EmployeeList;
