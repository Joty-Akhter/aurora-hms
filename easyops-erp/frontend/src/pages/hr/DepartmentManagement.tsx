import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  getEmployees,
  getDepartmentLeaveApprovers,
  replaceDepartmentLeaveApprovers,
  Department,
  Employee,
  DepartmentLeaveApproverRow,
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

const resolveDepartmentId = (d: Pick<Department, 'departmentId' | 'id'> | null | undefined) =>
  d?.departmentId ?? d?.id ?? '';

const DepartmentManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingDepartment, setEditingDepartment] = useState<Department | null>(null);
  const [matrixDept, setMatrixDept] = useState<Department | null>(null);
  const [matrixRows, setMatrixRows] = useState<DepartmentLeaveApproverRow[]>([]);
  const [matrixLoading, setMatrixLoading] = useState(false);
  const [matrixError, setMatrixError] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    managerId: '',
    parentDepartmentId: '',
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      setError(null);

      const [departmentsRes, employeesRes] = await Promise.all([
        getDepartments(currentOrganizationId),
        getEmployees(currentOrganizationId, { status: 'ACTIVE' })
      ]);

      setDepartments(departmentsRes.data);
      setEmployees(employeesRes.data);
    } catch (err: any) {
      console.error('Failed to load departments:', err);
      setError(err.response?.data?.message || 'Failed to load departments');
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditingDepartment(null);
    setFormData({
      name: '',
      description: '',
      managerId: '',
      parentDepartmentId: '',
    });
    setShowModal(true);
  };

  const openEditModal = (department: Department) => {
    setEditingDepartment(department);
    setFormData({
      name: department.name,
      description: department.description || '',
      managerId: department.managerId || '',
      parentDepartmentId: department.parentDepartmentId || '',
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingDepartment(null);
    setFormData({
      name: '',
      description: '',
      managerId: '',
      parentDepartmentId: '',
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!currentOrganizationId) return;

    try {
      const departmentData: Department = {
        organizationId: currentOrganizationId,
        name: formData.name,
        description: formData.description || undefined,
        managerId: formData.managerId || undefined,
        parentDepartmentId: formData.parentDepartmentId || undefined,
      };

      if (editingDepartment) {
        await updateDepartment(resolveDepartmentId(editingDepartment), departmentData);
      } else {
        await createDepartment(departmentData);
      }

      closeModal();
      loadData();
    } catch (err: any) {
      console.error('Failed to save department:', err);
      alert(err.response?.data?.message || 'Failed to save department');
    }
  };

  const openLeaveApproverModal = async (dept: Department) => {
    const deptId = resolveDepartmentId(dept);
    if (!currentOrganizationId || !deptId) return;
    setMatrixDept(dept);
    setMatrixError(null);
    setMatrixLoading(true);
    try {
      const res = await getDepartmentLeaveApprovers(currentOrganizationId, deptId);
      const rows = res.data || [];
      setMatrixRows(
        rows.length > 0 ? rows : [{ stepOrder: 1, approverEmployeeId: '' }]
      );
    } catch (err: any) {
      console.error('Failed to load leave approvers:', err);
      setMatrixError(err.response?.data?.message || 'Failed to load leave approvers');
      setMatrixRows([{ stepOrder: 1, approverEmployeeId: '' }]);
    } finally {
      setMatrixLoading(false);
    }
  };

  const closeLeaveApproverModal = () => {
    setMatrixDept(null);
    setMatrixRows([]);
    setMatrixError(null);
  };

  const addMatrixRow = () => {
    setMatrixRows((prev) => [
      ...prev,
      { stepOrder: prev.length + 1, approverEmployeeId: '' },
    ]);
  };

  const removeMatrixRow = (index: number) => {
    setMatrixRows((prev) =>
      prev
        .filter((_, i) => i !== index)
        .map((row, i) => ({ ...row, stepOrder: i + 1 }))
    );
  };

  const updateMatrixRow = (index: number, approverEmployeeId: string) => {
    setMatrixRows((prev) =>
      prev.map((row, i) => (i === index ? { ...row, approverEmployeeId } : row))
    );
  };

  const saveLeaveApprovers = async () => {
    const deptId = resolveDepartmentId(matrixDept);
    if (!currentOrganizationId || !deptId) return;
    const filled = matrixRows
      .filter((r) => r.approverEmployeeId)
      .map((r, i) => ({ stepOrder: i + 1, approverEmployeeId: r.approverEmployeeId }));
    const ids = filled.map((r) => r.approverEmployeeId);
    if (new Set(ids).size !== ids.length) {
      alert('Each approver must be a different employee.');
      return;
    }
    if (filled.length === 0) {
      if (
        !window.confirm(
          'No approvers selected. This clears the matrix and uses the department manager only (if set). Continue?'
        )
      ) {
        return;
      }
    }
    try {
      setMatrixLoading(true);
      await replaceDepartmentLeaveApprovers(currentOrganizationId, deptId, filled);
      closeLeaveApproverModal();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to save leave approvers');
    } finally {
      setMatrixLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to deactivate this department?')) {
      return;
    }
    if (!currentOrganizationId) return;

    try {
      await deleteDepartment(id, currentOrganizationId);
      loadData();
    } catch (err: any) {
      console.error('Failed to delete department:', err);
      alert(err.response?.data?.message || 'Failed to delete department');
    }
  };

  const getManagerName = (managerId?: string) => {
    if (!managerId) return '-';
    const manager = employees.find(e => e.employeeId === managerId);
    return manager ? manager.name : '-';
  };

  const getParentDepartmentName = (parentId?: string) => {
    if (!parentId) return '-';
    const parent = departments.find(d => resolveDepartmentId(d) === parentId);
    return parent?.name || '-';
  };

  const getEmployeeCount = (departmentId?: string) => {
    if (!departmentId) return 0;
    return employees.filter(e => e.departmentId === departmentId).length;
  };

  if (loading) {
    return <div className="loading">Loading departments...</div>;
  }

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Department Management</h1>
          <p>Manage organizational departments and structure</p>
        </div>
        <button className="btn-primary" onClick={openCreateModal}>
          + Add Department
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Departments Table */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Department Name</th>
              <th>Description</th>
              <th>Parent Department</th>
              <th>Manager</th>
              <th>Employee Count</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {departments.length === 0 ? (
              <tr>
                <td colSpan={7} className="no-data">
                  No departments found. Click "Add Department" to create one.
                </td>
              </tr>
            ) : (
              departments.map((dept) => (
                <tr key={resolveDepartmentId(dept)}>
                  <td><strong>{dept.name}</strong></td>
                  <td>{dept.description || '-'}</td>
                  <td>{getParentDepartmentName(dept.parentDepartmentId)}</td>
                  <td>{getManagerName(dept.managerId)}</td>
                  <td>{getEmployeeCount(resolveDepartmentId(dept))}</td>
                  <td>
                    <span className={dept.isActive ? 'status-badge status-active' : 'status-badge status-inactive'}>
                      {dept.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="action-buttons">
                    <button
                      type="button"
                      className="btn-sm btn-outline"
                      onClick={() => openLeaveApproverModal(dept)}
                      title="Ordered leave approvers (matrix)"
                    >
                      Approvers
                    </button>
                    <button 
                      className="btn-sm btn-edit"
                      onClick={() => openEditModal(dept)}
                    >
                      Edit
                    </button>
                    {dept.isActive && (
                      <button 
                        className="btn-sm btn-delete"
                        onClick={() => handleDelete(resolveDepartmentId(dept))}
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

      {/* Department Form Modal */}
      {matrixDept &&
        portalLayoutOverlay(
          <div
            className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
            style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
            onClick={closeLeaveApproverModal}
          >
            <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
              <div className="modal-header">
                <h2>Leave approvers — {matrixDept.name}</h2>
                <button type="button" className="modal-close" onClick={closeLeaveApproverModal}>
                  &times;
                </button>
              </div>
              <p style={{ marginTop: 0, color: '#555', fontSize: '0.9rem' }}>
                Ordered steps (1 = first). Save an empty list to fall back to the department manager only. Active
                employees only.
              </p>
              {matrixError && <div className="error-message">{matrixError}</div>}
              {matrixLoading && !matrixRows.length ? (
                <div className="loading">Loading…</div>
              ) : (
                <>
                  <table className="data-table" style={{ marginBottom: '1rem' }}>
                    <thead>
                      <tr>
                        <th>Step</th>
                        <th>Approver</th>
                        <th />
                      </tr>
                    </thead>
                    <tbody>
                      {matrixRows.map((row, index) => (
                        <tr key={`${row.stepOrder}-${index}`}>
                          <td>{row.stepOrder}</td>
                          <td>
                            <select
                              value={row.approverEmployeeId}
                              onChange={(e) => updateMatrixRow(index, e.target.value)}
                              style={{ width: '100%' }}
                            >
                              <option value="">Select employee</option>
                              {employees.map((emp) => (
                                <option key={emp.employeeId} value={emp.employeeId!}>
                                  {emp.name}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td>
                            <button type="button" className="btn-sm btn-delete" onClick={() => removeMatrixRow(index)}>
                              Remove
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <button type="button" className="btn-outline" onClick={addMatrixRow} style={{ marginBottom: '1rem' }}>
                    + Add step
                  </button>
                  <div className="modal-actions">
                    <button type="button" className="btn-outline" onClick={closeLeaveApproverModal}>
                      Cancel
                    </button>
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={matrixLoading}
                      onClick={async () => {
                        const clearDeptId = resolveDepartmentId(matrixDept);
                        if (!currentOrganizationId || !clearDeptId) return;
                        if (!window.confirm('Clear matrix and use department manager only as approver?')) return;
                        try {
                          setMatrixLoading(true);
                          await replaceDepartmentLeaveApprovers(
                            currentOrganizationId,
                            clearDeptId,
                            []
                          );
                          closeLeaveApproverModal();
                        } catch (err: any) {
                          alert(err.response?.data?.message || 'Failed to clear approvers');
                        } finally {
                          setMatrixLoading(false);
                        }
                      }}
                    >
                      Clear matrix
                    </button>
                    <button type="button" className="btn-primary" disabled={matrixLoading} onClick={saveLeaveApprovers}>
                      {matrixLoading ? 'Saving…' : 'Save'}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        )}

      {showModal && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
          onClick={closeModal}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingDepartment ? 'Edit Department' : 'Add New Department'}</h2>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Department Name *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  required
                  placeholder="Engineering, Sales, HR, etc."
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  placeholder="Department description..."
                  rows={3}
                />
              </div>

              <div className="form-group">
                <label>Parent Department</label>
                <select
                  value={formData.parentDepartmentId}
                  onChange={(e) => setFormData({...formData, parentDepartmentId: e.target.value})}
                >
                  <option value="">None (Root Department)</option>
                  {departments
                    .filter(d => resolveDepartmentId(d) !== resolveDepartmentId(editingDepartment))
                    .map(dept => (
                      <option key={resolveDepartmentId(dept)} value={resolveDepartmentId(dept)}>
                        {dept.name}
                      </option>
                    ))}
                </select>
              </div>

              <div className="form-group">
                <label>Manager</label>
                <select
                  value={formData.managerId}
                  onChange={(e) => setFormData({...formData, managerId: e.target.value})}
                >
                  <option value="">Select Manager</option>
                  {employees.map(emp => (
                    <option key={emp.employeeId} value={emp.employeeId}>
                      {emp.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-outline" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingDepartment ? 'Update Department' : 'Create Department'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DepartmentManagement;

