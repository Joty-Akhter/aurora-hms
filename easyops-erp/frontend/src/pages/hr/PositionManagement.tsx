import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  getPositions, 
  createPosition, 
  updatePosition, 
  deletePosition,
  getDepartments,
  Position,
  Department
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

const PositionManagement: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [positions, setPositions] = useState<Position[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingPosition, setEditingPosition] = useState<Position | null>(null);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    departmentId: '',
    level: '',
    hierarchyRank: '',
    salaryRangeMin: '',
    salaryRangeMax: '',
    currency: 'BDT',
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

      const [positionsRes, departmentsRes] = await Promise.all([
        getPositions(currentOrganizationId),
        getDepartments(currentOrganizationId, { activeOnly: true })
      ]);

      setPositions(positionsRes.data);
      setDepartments(departmentsRes.data);
    } catch (err: any) {
      console.error('Failed to load positions:', err);
      setError(err.response?.data?.message || 'Failed to load positions');
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditingPosition(null);
    setFormData({
      title: '',
      description: '',
      departmentId: '',
      level: '',
      hierarchyRank: '',
      salaryRangeMin: '',
      salaryRangeMax: '',
      currency: 'BDT',
    });
    setShowModal(true);
  };

  const openEditModal = (position: Position) => {
    setEditingPosition(position);
    setFormData({
      title: position.title,
      description: position.description || '',
      departmentId: position.departmentId || '',
      level: position.level || '',
      hierarchyRank:
        position.hierarchyRank !== undefined && position.hierarchyRank !== null
          ? String(position.hierarchyRank)
          : '',
      salaryRangeMin: position.salaryRangeMin?.toString() || '',
      salaryRangeMax: position.salaryRangeMax?.toString() || '',
      currency: position.currency || 'BDT',
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingPosition(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!currentOrganizationId) return;

    try {
      const positionData: Position = {
        organizationId: currentOrganizationId,
        title: formData.title,
        description: formData.description || undefined,
        departmentId: formData.departmentId || undefined,
        level: formData.level || undefined,
        hierarchyRank:
          formData.hierarchyRank.trim() !== '' && !Number.isNaN(parseInt(formData.hierarchyRank, 10))
            ? parseInt(formData.hierarchyRank, 10)
            : undefined,
        salaryRangeMin: formData.salaryRangeMin ? parseFloat(formData.salaryRangeMin) : undefined,
        salaryRangeMax: formData.salaryRangeMax ? parseFloat(formData.salaryRangeMax) : undefined,
        currency: formData.currency || 'BDT',
      };

      if (editingPosition) {
        await updatePosition(editingPosition.positionId!, positionData);
      } else {
        await createPosition(positionData);
      }

      closeModal();
      loadData();
    } catch (err: any) {
      console.error('Failed to save position:', err);
      alert(err.response?.data?.message || 'Failed to save position');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to deactivate this position?')) {
      return;
    }

    try {
      await deletePosition(id);
      loadData();
    } catch (err: any) {
      console.error('Failed to delete position:', err);
      alert(err.response?.data?.message || 'Failed to delete position');
    }
  };

  const getDepartmentName = (departmentId?: string) => {
    if (!departmentId) return '-';
    const dept = departments.find(d => d.departmentId === departmentId);
    return dept?.name || '-';
  };

  const formatSalaryRange = (min?: number, max?: number, currency?: string) => {
    if (!min && !max) return '-';
    const curr = currency || 'BDT';
    const formatter = (() => {
      try {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: curr });
      } catch {
        return null;
      }
    })();
    const format = (value?: number) => {
      if (typeof value !== 'number' || Number.isNaN(value)) return '0';
      return formatter ? formatter.format(value) : `${curr} ${value.toLocaleString()}`;
    };
    if (min && max) {
      return `${format(min)} - ${format(max)}`;
    }
    if (min) {
      return `${format(min)}+`;
    }
    return `Up to ${format(max)}`;
  };

  if (loading) {
    return <div className="loading">Loading positions...</div>;
  }

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Position Management</h1>
          <p>Manage job positions and titles</p>
        </div>
        <button className="btn-primary" onClick={openCreateModal}>
          + Add Position
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Positions Table */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Position Title</th>
              <th>Description</th>
              <th>Department</th>
              <th>Level</th>
              <th>Rank</th>
              <th>Salary Range</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {positions.length === 0 ? (
              <tr>
                <td colSpan={8} className="no-data">
                  No positions found. Click "Add Position" to create one.
                </td>
              </tr>
            ) : (
              positions.map((position) => (
                <tr key={position.positionId}>
                  <td><strong>{position.title}</strong></td>
                  <td>{position.description || '-'}</td>
                  <td>{getDepartmentName(position.departmentId)}</td>
                  <td>{position.level || '-'}</td>
                  <td>{position.hierarchyRank ?? '-'}</td>
                  <td>{formatSalaryRange(position.salaryRangeMin, position.salaryRangeMax, position.currency)}</td>
                  <td>
                    <span className={position.isActive ? 'status-badge status-active' : 'status-badge status-inactive'}>
                      {position.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="action-buttons">
                    <button 
                      className="btn-sm btn-edit"
                      onClick={() => openEditModal(position)}
                    >
                      Edit
                    </button>
                    {position.isActive && (
                      <button 
                        className="btn-sm btn-delete"
                        onClick={() => handleDelete(position.positionId!)}
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

      {/* Position Form Modal */}
      {showModal && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
          onClick={closeModal}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingPosition ? 'Edit Position' : 'Add New Position'}</h2>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Position Title *</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                  required
                  placeholder="Software Engineer, HR Manager, etc."
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  placeholder="Position description and responsibilities..."
                  rows={3}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Department</label>
                  <select
                    value={formData.departmentId}
                    onChange={(e) => setFormData({...formData, departmentId: e.target.value})}
                  >
                    <option value="">Select Department</option>
                    {departments.map(dept => (
                      <option key={dept.departmentId} value={dept.departmentId}>
                        {dept.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label>Level</label>
                  <select
                    value={formData.level}
                    onChange={(e) => setFormData({...formData, level: e.target.value})}
                  >
                    <option value="">Select Level</option>
                    <option value="ENTRY">Entry Level</option>
                    <option value="JUNIOR">Junior</option>
                    <option value="MID">Mid-Level</option>
                    <option value="SENIOR">Senior</option>
                    <option value="LEAD">Lead</option>
                    <option value="MANAGER">Manager</option>
                    <option value="DIRECTOR">Director</option>
                    <option value="VP">Vice President</option>
                    <option value="C_LEVEL">C-Level</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Hierarchy rank (HR-MD-03)</label>
                  <input
                    type="number"
                    placeholder="e.g. 10 — higher = more senior"
                    value={formData.hierarchyRank}
                    onChange={(e) => setFormData({ ...formData, hierarchyRank: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Salary Range Min</label>
                  <input
                    type="number"
                    value={formData.salaryRangeMin}
                    onChange={(e) => setFormData({...formData, salaryRangeMin: e.target.value})}
                    placeholder="50000"
                    step="0.01"
                  />
                </div>

                <div className="form-group">
                  <label>Salary Range Max</label>
                  <input
                    type="number"
                    value={formData.salaryRangeMax}
                    onChange={(e) => setFormData({...formData, salaryRangeMax: e.target.value})}
                    placeholder="100000"
                    step="0.01"
                  />
                </div>

                <div className="form-group">
                  <label>Currency</label>
                  <select
                    value={formData.currency}
                    onChange={(e) => setFormData({...formData, currency: e.target.value})}
                  >
                  <option value="BDT">BDT</option>
                  <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                    <option value="CAD">CAD</option>
                    <option value="AUD">AUD</option>
                  </select>
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-outline" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingPosition ? 'Update Position' : 'Create Position'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PositionManagement;

