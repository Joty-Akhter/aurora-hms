import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getEmployeeById, getDepartmentById, getPositionById, Employee, Department, Position } from '../../services/hrService';
import { genderLabel } from '../../constants/gender';
import './Hr.css';

const EmployeeDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [department, setDepartment] = useState<Department | null>(null);
  const [position, setPosition] = useState<Position | null>(null);
  const [manager, setManager] = useState<Employee | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadEmployee(id);
    }
  }, [id]);

  const loadEmployee = async (employeeId: string) => {
    try {
      setLoading(true);
      setError(null);

      const response = await getEmployeeById(employeeId);
      const emp = response.data;
      setEmployee(emp);

      // Load related data
      if (emp.departmentId) {
        const deptRes = emp.departmentId
          ? await getDepartmentById(emp.organizationId, emp.departmentId)
          : null;
      if (deptRes) {
        setDepartment(deptRes.data);
      }
      }

      if (emp.positionId) {
        const posRes = await getPositionById(emp.positionId);
        setPosition(posRes.data);
      }

      if (emp.managerId) {
        const mgrRes = await getEmployeeById(emp.managerId);
        setManager(mgrRes.data);
      }
    } catch (err: any) {
      console.error('Failed to load employee:', err);
      setError(err.response?.data?.message || 'Failed to load employee');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading employee details...</div>;
  }

  if (error) {
    return <div className="error-message">{error}</div>;
  }

  if (!employee) {
    return <div className="no-data">Employee not found</div>;
  }

  const getStatusBadgeClass = (status?: string) => {
    switch (status) {
      case 'ACTIVE': return 'status-badge status-active';
      case 'ON_LEAVE': return 'status-badge status-on-leave';
      case 'TERMINATED': return 'status-badge status-terminated';
      default: return 'status-badge';
    }
  };

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>{employee.name}</h1>
          <p>Employee #{employee.employeeNumber}</p>
        </div>
        <div className="header-actions">
          <button className="btn-secondary" onClick={() => navigate(`/hr/employees/${id}/edit`)}>
            Edit Employee
          </button>
          <button className="btn-outline" onClick={() => navigate('/hr/employees')}>
            Back to List
          </button>
        </div>
      </div>

      <div className="detail-container">
        {/* Status Overview */}
        <div className="detail-section">
          <div className="status-overview">
            <span className={getStatusBadgeClass(employee.employmentStatus)}>
              {employee.employmentStatus}
            </span>
            <span className="employment-type">{employee.employmentType}</span>
          </div>
        </div>

        {/* Basic Information */}
        <div className="detail-section">
          <h2>Basic Information</h2>
          <div className="detail-grid">
            <div className="detail-item">
              <label>Employee Number</label>
              <div className="detail-value">{employee.employeeNumber}</div>
            </div>
            <div className="detail-item">
              <label>Email</label>
              <div className="detail-value">{employee.email || '-'}</div>
            </div>
            <div className="detail-item">
              <label>Phone</label>
              <div className="detail-value">{employee.phone || '-'}</div>
            </div>
            <div className="detail-item">
              <label>Date of Birth</label>
              <div className="detail-value">
                {employee.dateOfBirth ? new Date(employee.dateOfBirth).toLocaleDateString() : '-'}
              </div>
            </div>
            <div className="detail-item">
              <label>Gender</label>
              <div className="detail-value">{genderLabel(employee.gender)}</div>
            </div>
          </div>
        </div>

        {/* Employment Information */}
        <div className="detail-section">
          <h2>Employment Information</h2>
          <div className="detail-grid">
            <div className="detail-item">
              <label>Hire Date</label>
              <div className="detail-value">
                {new Date(employee.hireDate).toLocaleDateString()}
              </div>
            </div>
            <div className="detail-item">
              <label>Department</label>
              <div className="detail-value">{department?.name || '-'}</div>
            </div>
            <div className="detail-item">
              <label>Position</label>
              <div className="detail-value">{position?.title || '-'}</div>
            </div>
            <div className="detail-item">
              <label>Manager</label>
              <div className="detail-value">
                {manager ? manager.name : '-'}
              </div>
            </div>
            <div className="detail-item">
              <label>Employment Type</label>
              <div className="detail-value">{employee.employmentType || '-'}</div>
            </div>
            <div className="detail-item">
              <label>Employment Status</label>
              <div className="detail-value">
                <span className={getStatusBadgeClass(employee.employmentStatus)}>
                  {employee.employmentStatus}
                </span>
              </div>
            </div>
            {employee.terminationDate && (
              <div className="detail-item">
                <label>Termination Date</label>
                <div className="detail-value">
                  {new Date(employee.terminationDate).toLocaleDateString()}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Address Information */}
        {(employee.addressLine1 || employee.city) && (
          <div className="detail-section">
            <h2>Address Information</h2>
            <div className="detail-grid">
              {employee.addressLine1 && (
                <div className="detail-item detail-item-full">
                  <label>Address</label>
                  <div className="detail-value">
                    {employee.addressLine1}
                    {employee.addressLine2 && <><br />{employee.addressLine2}</>}
                  </div>
                </div>
              )}
              {employee.city && (
                <div className="detail-item">
                  <label>City</label>
                  <div className="detail-value">{employee.city}</div>
                </div>
              )}
              {employee.stateProvince && (
                <div className="detail-item">
                  <label>State/Province</label>
                  <div className="detail-value">{employee.stateProvince}</div>
                </div>
              )}
              {employee.postalCode && (
                <div className="detail-item">
                  <label>Postal Code</label>
                  <div className="detail-value">{employee.postalCode}</div>
                </div>
              )}
              {employee.country && (
                <div className="detail-item">
                  <label>Country</label>
                  <div className="detail-value">{employee.country}</div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Emergency Contact */}
        {employee.emergencyContactName && (
          <div className="detail-section">
            <h2>Emergency Contact</h2>
            <div className="detail-grid">
              <div className="detail-item">
                <label>Name</label>
                <div className="detail-value">{employee.emergencyContactName}</div>
              </div>
              <div className="detail-item">
                <label>Phone</label>
                <div className="detail-value">{employee.emergencyContactPhone || '-'}</div>
              </div>
              <div className="detail-item">
                <label>Relationship</label>
                <div className="detail-value">{employee.emergencyContactRelationship || '-'}</div>
              </div>
            </div>
          </div>
        )}

        {/* System Information */}
        <div className="detail-section">
          <h2>System Information</h2>
          <div className="detail-grid">
            <div className="detail-item">
              <label>Created At</label>
              <div className="detail-value">
                {employee.createdAt ? new Date(employee.createdAt).toLocaleString() : '-'}
              </div>
            </div>
            <div className="detail-item">
              <label>Updated At</label>
              <div className="detail-value">
                {employee.updatedAt ? new Date(employee.updatedAt).toLocaleString() : '-'}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmployeeDetail;

