import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

interface StockCount {
  id: string;
  countNumber: string;
  countDate: string;
  countType: string;
  warehouseId: string;
  status: string;
  totalVarianceValue: number;
  countedBy?: string;
  approvedBy?: string;
  completedAt?: string;
}

interface Warehouse {
  id: string;
  code: string;
  name: string;
  type: string;
  address: string;
  contact: string;
  capacity: number;
  status: string;
  isActive: boolean;
}

const StockCounting: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [counts, setCounts] = useState<StockCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [formData, setFormData] = useState({
    countNumber: '',
    countDate: new Date().toISOString().split('T')[0],
    countType: 'FULL',
    warehouseId: '',
    scheduledDate: new Date().toISOString().split('T')[0],
    notes: ''
  });

  useEffect(() => {
    loadCounts();
    loadWarehouses();
  }, [currentOrganizationId, filterStatus]);

  const loadCounts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      
      const response = await api.get('/api/inventory/stock-counts', { params });
      setCounts(response.data);
    } catch (error) {
      console.error('Failed to load stock counts:', error);
      alert('Failed to load stock counts');
    } finally {
      setLoading(false);
    }
  };

  const loadWarehouses = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/inventory/warehouses', {
        params: { organizationId: currentOrganizationId, activeOnly: true }
      });
      setWarehouses(response.data);
    } catch (error) {
      console.error('Failed to load warehouses:', error);
    }
  };

  const handleStartCount = async (countId: string) => {
    if (!user?.id) return;
    
    try {
      await api.post(`/api/inventory/stock-counts/${countId}/start`, null, {
        params: { userId: user.id }
      });
      loadCounts();
      alert('Stock count started successfully');
    } catch (error) {
      console.error('Failed to start count:', error);
      alert('Failed to start count');
    }
  };

  const handleCompleteCount = async (countId: string) => {
    try {
      await api.post(`/api/inventory/stock-counts/${countId}/complete`);
      loadCounts();
      alert('Stock count completed');
    } catch (error) {
      console.error('Failed to complete count:', error);
      alert('Failed to complete count');
    }
  };

  const handleApproveCount = async (countId: string) => {
    if (!user?.id) return;
    
    if (!confirm('This will apply adjustments to stock levels. Are you sure?')) return;
    
    try {
      await api.post(`/api/inventory/stock-counts/${countId}/approve`, null, {
        params: { approvedBy: user.id }
      });
      loadCounts();
      alert('Stock count approved and adjustments applied');
    } catch (error) {
      console.error('Failed to approve count:', error);
      alert('Failed to approve count');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id) return;

    try {
      const countData = {
        ...formData,
        organizationId: currentOrganizationId,
        createdBy: user.id,
        status: 'SCHEDULED'
      };

      await api.post('/api/inventory/stock-counts', countData);
      setShowModal(false);
      resetForm();
      loadCounts();
      alert('Stock count created successfully');
    } catch (error) {
      console.error('Failed to create stock count:', error);
      alert('Failed to create stock count');
    }
  };

  const resetForm = () => {
    setFormData({
      countNumber: '',
      countDate: new Date().toISOString().split('T')[0],
      countType: 'FULL',
      warehouseId: '',
      scheduledDate: new Date().toISOString().split('T')[0],
      notes: ''
    });
  };

  const openModal = () => {
    // Generate a unique count number
    const countNumber = `SC-${Date.now()}`;
    setFormData(prev => ({ ...prev, countNumber }));
    setShowModal(true);
  };

  if (loading) return <div className="loading">Loading stock counts...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Stock Counting</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Stock Count
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
            <option value="APPROVED">Approved</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Count Number</th>
              <th>Count Type</th>
              <th>Count Date</th>
              <th>Warehouse ID</th>
              <th>Status</th>
              <th>Variance Value</th>
              <th>Completed At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {counts.map((count) => (
              <tr key={count.id}>
                <td><strong>{count.countNumber}</strong></td>
                <td>{count.countType}</td>
                <td>{new Date(count.countDate).toLocaleDateString()}</td>
                <td>{count.warehouseId.substring(0, 8)}...</td>
                <td>
                  <span className={`status-badge ${count.status.toLowerCase().replace('_', '-')}`}>
                    {count.status.replace('_', ' ')}
                  </span>
                </td>
                <td className={count.totalVarianceValue < 0 ? 'text-danger' : count.totalVarianceValue > 0 ? 'text-success' : ''}>
                  ৳{Math.abs(count.totalVarianceValue).toFixed(2)}
                </td>
                <td>{count.completedAt ? new Date(count.completedAt).toLocaleString() : '-'}</td>
                <td>
                  {count.status === 'SCHEDULED' && (
                    <button className="btn-small" onClick={() => handleStartCount(count.id)}>Start</button>
                  )}
                  {count.status === 'IN_PROGRESS' && (
                    <button className="btn-small" onClick={() => handleCompleteCount(count.id)}>Complete</button>
                  )}
                  {count.status === 'COMPLETED' && (
                    <button className="btn-small btn-primary" onClick={() => handleApproveCount(count.id)}>Approve</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {counts.length === 0 && (
          <div className="no-data">No stock counts found</div>
        )}
      </div>

      {/* Create Count Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>Create New Stock Count</h2>
              <button className="btn-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="countNumber">Count Number *</label>
                  <input
                    type="text"
                    id="countNumber"
                    value={formData.countNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, countNumber: e.target.value }))}
                    required
                    placeholder="e.g., SC-20240101-001"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="countType">Count Type *</label>
                  <select
                    id="countType"
                    value={formData.countType}
                    onChange={(e) => setFormData(prev => ({ ...prev, countType: e.target.value }))}
                    required
                  >
                    <option value="FULL">Full Count</option>
                    <option value="CYCLE">Cycle Count</option>
                    <option value="SPOT">Spot Count</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="warehouseId">Warehouse *</label>
                  <select
                    id="warehouseId"
                    value={formData.warehouseId}
                    onChange={(e) => setFormData(prev => ({ ...prev, warehouseId: e.target.value }))}
                    required
                  >
                    <option value="">Select Warehouse</option>
                    {warehouses.map(warehouse => (
                      <option key={warehouse.id} value={warehouse.id}>
                        {warehouse.code} - {warehouse.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="countDate">Count Date *</label>
                  <input
                    type="date"
                    id="countDate"
                    value={formData.countDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, countDate: e.target.value }))}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="scheduledDate">Scheduled Date</label>
                  <input
                    type="date"
                    id="scheduledDate"
                    value={formData.scheduledDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, scheduledDate: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="notes">Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                  rows={3}
                  placeholder="Additional notes for this stock count..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create Stock Count
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default StockCounting;

