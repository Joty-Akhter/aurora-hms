import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Purchase.css';

interface PurchaseOrder {
  id: string;
  poNumber: string;
  poDate: string;
  vendorId: string;
  vendorName: string;
  status: string;
  totalAmount: number;
  currency: string;
  expectedDeliveryDate?: string;
  actualDeliveryDate?: string;
  createdBy: string;
  approvedBy?: string;
  approvedAt?: string;
  notes?: string;
  lines: PurchaseOrderLine[];
}

interface PurchaseOrderLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  receivedQuantity: number;
  pendingQuantity: number;
  status: string;
}

interface Vendor {
  id: string;
  name: string;
  code: string;
  email: string;
  phone: string;
  address: string;
  paymentTerms: number;
  currency: string;
  isActive: boolean;
}

const PurchaseOrders: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    poNumber: '',
    poDate: new Date().toISOString().split('T')[0],
    vendorId: '',
    expectedDeliveryDate: '',
    currency: 'USD',
    notes: ''
  });

  useEffect(() => {
    loadPurchaseOrders();
    loadVendors();
  }, [currentOrganizationId, filterStatus]);

  const loadPurchaseOrders = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      
      const response = await api.get('/api/purchase/orders', { params });
      setPurchaseOrders(response.data);
    } catch (error) {
      console.error('Failed to load purchase orders:', error);
      alert('Failed to load purchase orders');
    } finally {
      setLoading(false);
    }
  };

  const loadVendors = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/ap/vendors', {
        params: { organizationId: currentOrganizationId, activeOnly: true }
      });
      setVendors(response.data);
    } catch (error) {
      console.error('Failed to load vendors:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id) return;

    try {
      const poData = {
        ...formData,
        organizationId: currentOrganizationId,
        createdBy: user.id,
        status: 'DRAFT'
      };

      await api.post('/api/purchase/orders', poData);
      setShowModal(false);
      resetForm();
      loadPurchaseOrders();
      alert('Purchase order created successfully');
    } catch (error) {
      console.error('Failed to create purchase order:', error);
      alert('Failed to create purchase order');
    }
  };

  const handleApprove = async (poId: string) => {
    if (!user?.id) return;
    
    if (!confirm('Are you sure you want to approve this purchase order?')) return;
    
    try {
      await api.post(`/api/purchase/orders/${poId}/approve`, {
        approvedBy: user.id
      });
      loadPurchaseOrders();
      alert('Purchase order approved successfully');
    } catch (error) {
      console.error('Failed to approve purchase order:', error);
      alert('Failed to approve purchase order');
    }
  };

  const handleCancel = async (poId: string) => {
    const reason = prompt('Enter cancellation reason:');
    if (!reason) return;
    
    try {
      await api.post(`/api/purchase/orders/${poId}/cancel`, { reason });
      loadPurchaseOrders();
      alert('Purchase order cancelled');
    } catch (error) {
      console.error('Failed to cancel purchase order:', error);
      alert('Failed to cancel purchase order');
    }
  };

  const resetForm = () => {
    setFormData({
      poNumber: '',
      poDate: new Date().toISOString().split('T')[0],
      vendorId: '',
      expectedDeliveryDate: '',
      currency: 'USD',
      notes: ''
    });
  };

  const openModal = () => {
    // Generate a unique PO number
    const poNumber = `PO-${Date.now()}`;
    setFormData(prev => ({ ...prev, poNumber }));
    setShowModal(true);
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'DRAFT': return 'draft';
      case 'SUBMITTED': return 'submitted';
      case 'APPROVED': return 'approved';
      case 'RECEIVED': return 'completed';
      case 'CANCELLED': return 'cancelled';
      default: return '';
    }
  };

  if (loading) return <div className="loading">Loading purchase orders...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Purchase Orders</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Purchase Order
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="DRAFT">Draft</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="APPROVED">Approved</option>
            <option value="RECEIVED">Received</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>PO Number</th>
              <th>Date</th>
              <th>Vendor</th>
              <th>Status</th>
              <th>Total Amount</th>
              <th>Expected Delivery</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {purchaseOrders.map((po) => (
              <tr key={po.id}>
                <td><strong>{po.poNumber}</strong></td>
                <td>{new Date(po.poDate).toLocaleDateString()}</td>
                <td>{po.vendorName}</td>
                <td>
                  <span className={`status-badge ${getStatusColor(po.status)}`}>
                    {po.status}
                  </span>
                </td>
                <td>
                  <strong>{po.currency} {po.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                </td>
                <td>
                  {po.expectedDeliveryDate 
                    ? new Date(po.expectedDeliveryDate).toLocaleDateString()
                    : '-'}
                </td>
                <td>
                  {po.status === 'DRAFT' && (
                    <button 
                      className="btn-small btn-primary"
                      onClick={() => handleApprove(po.id)}
                    >
                      Approve
                    </button>
                  )}
                  {(po.status === 'DRAFT' || po.status === 'SUBMITTED') && (
                    <>
                      {' '}
                      <button 
                        className="btn-small btn-danger"
                        onClick={() => handleCancel(po.id)}
                      >
                        Cancel
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {purchaseOrders.length === 0 && (
          <div className="no-data">No purchase orders found</div>
        )}
      </div>

      {/* Create PO Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>Create New Purchase Order</h2>
              <button className="btn-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="poNumber">PO Number *</label>
                  <input
                    type="text"
                    id="poNumber"
                    value={formData.poNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, poNumber: e.target.value }))}
                    required
                    placeholder="e.g., PO-20240101-001"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="poDate">PO Date *</label>
                  <input
                    type="date"
                    id="poDate"
                    value={formData.poDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, poDate: e.target.value }))}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="vendorId">Vendor *</label>
                  <select
                    id="vendorId"
                    value={formData.vendorId}
                    onChange={(e) => setFormData(prev => ({ ...prev, vendorId: e.target.value }))}
                    required
                  >
                    <option value="">Select Vendor</option>
                    {vendors.map(vendor => (
                      <option key={vendor.id} value={vendor.id}>
                        {vendor.code} - {vendor.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="currency">Currency *</label>
                  <select
                    id="currency"
                    value={formData.currency}
                    onChange={(e) => setFormData(prev => ({ ...prev, currency: e.target.value }))}
                    required
                  >
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                    <option value="CAD">CAD</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="expectedDeliveryDate">Expected Delivery Date</label>
                  <input
                    type="date"
                    id="expectedDeliveryDate"
                    value={formData.expectedDeliveryDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, expectedDeliveryDate: e.target.value }))}
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
                  placeholder="Additional notes for this purchase order..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create Purchase Order
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PurchaseOrders;
