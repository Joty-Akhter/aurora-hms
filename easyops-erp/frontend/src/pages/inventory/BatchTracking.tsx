import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

interface BatchLot {
  id: string;
  organizationId: string;
  productId: string;
  batchNumber: string;
  lotNumber?: string;
  manufactureDate?: string;
  expiryDate?: string;
  initialQuantity: number;
  currentQuantity: number;
  status: string;
  poNumber?: string;
  notes?: string;
  supplierId?: string;
  receiptDate?: string;
  qualityCertificate?: string;
}

interface Product {
  id: string;
  name: string;
  sku: string;
}

interface Supplier {
  id: string;
  name: string;
  code: string;
}

const BatchTracking: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [batches, setBatches] = useState<BatchLot[]>([]);
  const [loading, setLoading] = useState(true);
  const [showExpiringOnly, setShowExpiringOnly] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingBatch, setEditingBatch] = useState<BatchLot | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [formData, setFormData] = useState({
    productId: '',
    batchNumber: '',
    lotNumber: '',
    manufactureDate: '',
    expiryDate: '',
    supplierId: '',
    poNumber: '',
    receiptDate: '',
    initialQuantity: '',
    status: 'ACTIVE',
    qualityCertificate: '',
    notes: ''
  });

  useEffect(() => {
    loadBatches();
    loadProducts();
    // Note: Suppliers API not available, suppliers array remains empty
  }, [currentOrganizationId, showExpiringOnly]);

  const loadBatches = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const endpoint = showExpiringOnly 
        ? `/api/inventory/batches/expiring?organizationId=${currentOrganizationId}`
        : `/api/inventory/batches?organizationId=${currentOrganizationId}`;
      
      const response = await api.get(endpoint);
      setBatches(response.data);
    } catch (error) {
      console.error('Failed to load batches:', error);
      alert('Failed to load batch information');
    } finally {
      setLoading(false);
    }
  };

  const loadProducts = async () => {
    try {
      const response = await api.get('/api/inventory/products', {
        params: { organizationId: currentOrganizationId }
      });
      setProducts(response.data);
    } catch (error) {
      console.error('Failed to load products:', error);
    }
  };


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      const batchData = {
        ...formData,
        organizationId: currentOrganizationId,
        productId: formData.productId,
        supplierId: formData.supplierId || null,
        initialQuantity: parseFloat(formData.initialQuantity),
        currentQuantity: parseFloat(formData.initialQuantity),
        manufactureDate: formData.manufactureDate || null,
        expiryDate: formData.expiryDate || null,
        receiptDate: formData.receiptDate || null
      };

      if (editingBatch) {
        await api.put(`/api/inventory/batches/${editingBatch.id}`, batchData);
      } else {
        await api.post('/api/inventory/batches', batchData);
      }

      setShowModal(false);
      setEditingBatch(null);
      resetForm();
      loadBatches();
    } catch (error) {
      console.error('Failed to save batch:', error);
      alert('Failed to save batch');
    }
  };

  const handleEdit = (batch: BatchLot) => {
    setEditingBatch(batch);
    setFormData({
      productId: batch.productId,
      batchNumber: batch.batchNumber,
      lotNumber: batch.lotNumber || '',
      manufactureDate: batch.manufactureDate || '',
      expiryDate: batch.expiryDate || '',
      supplierId: batch.supplierId || '',
      poNumber: batch.poNumber || '',
      receiptDate: batch.receiptDate || '',
      initialQuantity: batch.initialQuantity.toString(),
      status: batch.status,
      qualityCertificate: batch.qualityCertificate || '',
      notes: batch.notes || ''
    });
    setShowModal(true);
  };

  const handleDelete = async (batchId: string) => {
    if (!confirm('Are you sure you want to delete this batch?')) return;

    try {
      await api.delete(`/api/inventory/batches/${batchId}`);
      loadBatches();
    } catch (error) {
      console.error('Failed to delete batch:', error);
      alert('Failed to delete batch');
    }
  };

  const handleStatusChange = async (batchId: string, status: string) => {
    try {
      await api.put(`/api/inventory/batches/${batchId}/status`, null, {
        params: { status }
      });
      loadBatches();
    } catch (error) {
      console.error('Failed to update batch status:', error);
      alert('Failed to update batch status');
    }
  };

  const resetForm = () => {
    setFormData({
      productId: '',
      batchNumber: '',
      lotNumber: '',
      manufactureDate: '',
      expiryDate: '',
      supplierId: '',
      poNumber: '',
      receiptDate: '',
      initialQuantity: '',
      status: 'ACTIVE',
      qualityCertificate: '',
      notes: ''
    });
  };

  const openModal = () => {
    setEditingBatch(null);
    resetForm();
    setShowModal(true);
  };

  const getDaysUntilExpiry = (expiryDate: string): number => {
    const today = new Date();
    const expiry = new Date(expiryDate);
    const diffTime = expiry.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const getExpiryStatusClass = (expiryDate?: string): string => {
    if (!expiryDate) return '';
    const days = getDaysUntilExpiry(expiryDate);
    if (days < 0) return 'expired';
    if (days <= 7) return 'critical';
    if (days <= 30) return 'warning';
    return 'good';
  };

  if (loading) return <div className="loading">Loading batches...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Batch/Lot Tracking</h1>
        <button className="btn-primary" onClick={openModal}>
          + Register Batch
        </button>
      </div>

      <div className="filters">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showExpiringOnly}
            onChange={(e) => setShowExpiringOnly(e.target.checked)}
          />
          Show Expiring Batches Only (Next 30 Days)
        </label>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Batch Number</th>
              <th>Lot Number</th>
              <th>Product ID</th>
              <th>Manufacture Date</th>
              <th>Expiry Date</th>
              <th>Days Until Expiry</th>
              <th>Initial Qty</th>
              <th>Current Qty</th>
              <th>PO Number</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {batches.map((batch) => (
              <tr key={batch.id} className={getExpiryStatusClass(batch.expiryDate)}>
                <td><strong>{batch.batchNumber}</strong></td>
                <td>{batch.lotNumber || '-'}</td>
                <td>{batch.productId.substring(0, 8)}...</td>
                <td>{batch.manufactureDate ? new Date(batch.manufactureDate).toLocaleDateString() : '-'}</td>
                <td>{batch.expiryDate ? new Date(batch.expiryDate).toLocaleDateString() : '-'}</td>
                <td>
                  {batch.expiryDate ? (
                    <span className={`expiry-badge ${getExpiryStatusClass(batch.expiryDate)}`}>
                      {getDaysUntilExpiry(batch.expiryDate)} days
                    </span>
                  ) : '-'}
                </td>
                <td>{batch.initialQuantity.toFixed(2)}</td>
                <td><strong>{batch.currentQuantity.toFixed(2)}</strong></td>
                <td>{batch.poNumber || '-'}</td>
                <td>
                  <span className={`status-badge ${batch.status.toLowerCase()}`}>
                    {batch.status}
                  </span>
                </td>
                <td>
                  <div className="action-buttons">
                    <button 
                      className="btn-small btn-secondary" 
                      onClick={() => handleEdit(batch)}
                      title="Edit Batch"
                    >
                      ✏️
                    </button>
                    <button 
                      className="btn-small btn-danger" 
                      onClick={() => handleDelete(batch.id)}
                      title="Delete Batch"
                    >
                      🗑️
                    </button>
                    <select 
                      value={batch.status} 
                      onChange={(e) => handleStatusChange(batch.id, e.target.value)}
                      className="status-select"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="QUARANTINE">Quarantine</option>
                      <option value="EXPIRED">Expired</option>
                      <option value="DEPLETED">Depleted</option>
                    </select>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {batches.length === 0 && (
          <div className="no-data">No batches found</div>
        )}
      </div>

      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Batches</h3>
          <div className="summary-value">{batches.length}</div>
        </div>
        <div className="summary-card critical">
          <h3>Expired</h3>
          <div className="summary-value">
            {batches.filter(b => b.expiryDate && getDaysUntilExpiry(b.expiryDate) < 0).length}
          </div>
        </div>
        <div className="summary-card warning">
          <h3>Expiring Soon</h3>
          <div className="summary-value">
            {batches.filter(b => b.expiryDate && getDaysUntilExpiry(b.expiryDate) >= 0 && getDaysUntilExpiry(b.expiryDate) <= 30).length}
          </div>
        </div>
        <div className="summary-card">
          <h3>Active</h3>
          <div className="summary-value">
            {batches.filter(b => b.status === 'ACTIVE').length}
          </div>
        </div>
      </div>

      {/* Batch Registration/Edit Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>{editingBatch ? 'Edit Batch' : 'Register New Batch'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            <form onSubmit={handleSubmit} className="batch-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="productId">Product *</label>
                  <select
                    id="productId"
                    value={formData.productId}
                    onChange={(e) => setFormData({...formData, productId: e.target.value})}
                    required
                  >
                    <option value="">Select Product</option>
                    {products.map(product => (
                      <option key={product.id} value={product.id}>
                        {product.name} ({product.sku})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="batchNumber">Batch Number *</label>
                  <input
                    type="text"
                    id="batchNumber"
                    value={formData.batchNumber}
                    onChange={(e) => setFormData({...formData, batchNumber: e.target.value})}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="lotNumber">Lot Number</label>
                  <input
                    type="text"
                    id="lotNumber"
                    value={formData.lotNumber}
                    onChange={(e) => setFormData({...formData, lotNumber: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="supplierId">Supplier</label>
                  <select
                    id="supplierId"
                    value={formData.supplierId}
                    onChange={(e) => setFormData({...formData, supplierId: e.target.value})}
                    disabled={suppliers.length === 0}
                  >
                    <option value="">
                      {suppliers.length === 0 ? 'Suppliers not available' : 'Select Supplier'}
                    </option>
                    {suppliers.map(supplier => (
                      <option key={supplier.id} value={supplier.id}>
                        {supplier.name} ({supplier.code})
                      </option>
                    ))}
                  </select>
                  {suppliers.length === 0 && (
                    <small className="field-note">Supplier management not available in this system</small>
                  )}
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="manufactureDate">Manufacture Date</label>
                  <input
                    type="date"
                    id="manufactureDate"
                    value={formData.manufactureDate}
                    onChange={(e) => setFormData({...formData, manufactureDate: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="expiryDate">Expiry Date</label>
                  <input
                    type="date"
                    id="expiryDate"
                    value={formData.expiryDate}
                    onChange={(e) => setFormData({...formData, expiryDate: e.target.value})}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="poNumber">PO Number</label>
                  <input
                    type="text"
                    id="poNumber"
                    value={formData.poNumber}
                    onChange={(e) => setFormData({...formData, poNumber: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="receiptDate">Receipt Date</label>
                  <input
                    type="date"
                    id="receiptDate"
                    value={formData.receiptDate}
                    onChange={(e) => setFormData({...formData, receiptDate: e.target.value})}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="initialQuantity">Initial Quantity *</label>
                  <input
                    type="number"
                    id="initialQuantity"
                    value={formData.initialQuantity}
                    onChange={(e) => setFormData({...formData, initialQuantity: e.target.value})}
                    step="0.01"
                    min="0"
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="status">Status</label>
                  <select
                    id="status"
                    value={formData.status}
                    onChange={(e) => setFormData({...formData, status: e.target.value})}
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="QUARANTINE">Quarantine</option>
                    <option value="EXPIRED">Expired</option>
                    <option value="DEPLETED">Depleted</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="qualityCertificate">Quality Certificate</label>
                <input
                  type="text"
                  id="qualityCertificate"
                  value={formData.qualityCertificate}
                  onChange={(e) => setFormData({...formData, qualityCertificate: e.target.value})}
                />
              </div>

              <div className="form-group">
                <label htmlFor="notes">Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData({...formData, notes: e.target.value})}
                  rows={3}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingBatch ? 'Update Batch' : 'Create Batch'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BatchTracking;

