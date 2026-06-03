import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Purchase.css';

interface PurchaseReceipt {
  id: string;
  receiptNumber: string;
  receiptDate: string;
  poId: string;
  poNumber: string;
  vendorId: string;
  vendorName: string;
  status: string;
  totalQuantity: number;
  receivedBy: string;
  receivedAt: string;
  notes?: string;
  lines: ReceiptLine[];
}

interface ReceiptLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  orderedQuantity: number;
  receivedQuantity: number;
  unitPrice: number;
  totalPrice: number;
  condition: string;
  notes?: string;
}

interface PurchaseOrder {
  id: string;
  poNumber: string;
  vendorName: string;
  status: string;
  totalAmount: number;
  lines: Array<{
    productId: string;
    productName: string;
    productSku: string;
    quantity: number;
    unitPrice: number;
  }>;
}

const PurchaseReceipts: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [receipts, setReceipts] = useState<PurchaseReceipt[]>([]);
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [selectedPO, setSelectedPO] = useState<PurchaseOrder | null>(null);
  const [formData, setFormData] = useState({
    receiptNumber: '',
    receiptDate: new Date().toISOString().split('T')[0],
    poId: '',
    receivedBy: '',
    notes: ''
  });
  const [receiptLines, setReceiptLines] = useState<Array<{
    productId: string;
    productName: string;
    productSku: string;
    orderedQuantity: number;
    receivedQuantity: number;
    unitPrice: number;
    condition: string;
    notes: string;
  }>>([]);

  useEffect(() => {
    loadReceipts();
    loadPurchaseOrders();
  }, [currentOrganizationId, filterStatus]);

  const loadReceipts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      
      const response = await api.get('/api/purchase/receipts', { params });
      setReceipts(response.data);
    } catch (error) {
      console.error('Failed to load purchase receipts:', error);
      alert('Failed to load purchase receipts');
    } finally {
      setLoading(false);
    }
  };

  const loadPurchaseOrders = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/purchase/orders', {
        params: { organizationId: currentOrganizationId, status: 'APPROVED' }
      });
      setPurchaseOrders(response.data);
    } catch (error) {
      console.error('Failed to load purchase orders:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id) return;

    // Validate that we have at least one receipt line with received quantity
    const validLines = receiptLines.filter(line => line.receivedQuantity > 0);
    if (validLines.length === 0) {
      alert('Please enter received quantities for at least one item');
      return;
    }

    try {
      const receiptData = {
        ...formData,
        organizationId: currentOrganizationId,
        receivedBy: user.id,
        status: 'RECEIVED',
        lines: validLines.map(line => ({
          productId: line.productId,
          receivedQuantity: line.receivedQuantity,
          unitPrice: line.unitPrice,
          condition: line.condition,
          notes: line.notes
        }))
      };

      await api.post('/api/purchase/receipts', receiptData);
      setShowModal(false);
      resetForm();
      loadReceipts();
      alert('Purchase receipt created successfully');
    } catch (error) {
      console.error('Failed to create purchase receipt:', error);
      alert('Failed to create purchase receipt');
    }
  };

  const handlePOSelect = (poId: string) => {
    const po = purchaseOrders.find(p => p.id === poId);
    if (po) {
      setSelectedPO(po);
      setFormData(prev => ({ ...prev, poId }));
      
      // Initialize receipt lines from PO lines
      const lines = po.lines.map(line => ({
        productId: line.productId,
        productName: line.productName,
        productSku: line.productSku,
        orderedQuantity: line.quantity,
        receivedQuantity: 0,
        unitPrice: line.unitPrice,
        condition: 'GOOD',
        notes: ''
      }));
      setReceiptLines(lines);
    }
  };

  const updateReceiptLine = (index: number, field: string, value: any) => {
    const updatedLines = [...receiptLines];
    updatedLines[index] = { ...updatedLines[index], [field]: value };
    setReceiptLines(updatedLines);
  };

  const resetForm = () => {
    setFormData({
      receiptNumber: '',
      receiptDate: new Date().toISOString().split('T')[0],
      poId: '',
      receivedBy: '',
      notes: ''
    });
    setReceiptLines([]);
    setSelectedPO(null);
  };

  const openModal = () => {
    // Generate a unique receipt number
    const receiptNumber = `GRN-${Date.now()}`;
    setFormData(prev => ({ ...prev, receiptNumber }));
    setShowModal(true);
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'DRAFT': return 'draft';
      case 'RECEIVED': return 'completed';
      case 'INSPECTED': return 'approved';
      case 'REJECTED': return 'cancelled';
      default: return '';
    }
  };

  if (loading) return <div className="loading">Loading purchase receipts...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Purchase Receipts</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Receipt
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="DRAFT">Draft</option>
            <option value="RECEIVED">Received</option>
            <option value="INSPECTED">Inspected</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Receipt Number</th>
              <th>Date</th>
              <th>PO Number</th>
              <th>Vendor</th>
              <th>Status</th>
              <th>Total Quantity</th>
              <th>Received By</th>
            </tr>
          </thead>
          <tbody>
            {receipts.map((receipt) => (
              <tr key={receipt.id}>
                <td><strong>{receipt.receiptNumber}</strong></td>
                <td>{new Date(receipt.receiptDate).toLocaleDateString()}</td>
                <td>{receipt.poNumber}</td>
                <td>{receipt.vendorName}</td>
                <td>
                  <span className={`status-badge ${getStatusColor(receipt.status)}`}>
                    {receipt.status}
                  </span>
                </td>
                <td>{receipt.totalQuantity}</td>
                <td>{receipt.receivedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {receipts.length === 0 && (
          <div className="no-data">No purchase receipts found</div>
        )}
      </div>

      {/* Create Receipt Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content large">
            <div className="modal-header">
              <h2>Create New Purchase Receipt</h2>
              <button className="btn-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="receiptNumber">Receipt Number *</label>
                  <input
                    type="text"
                    id="receiptNumber"
                    value={formData.receiptNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, receiptNumber: e.target.value }))}
                    required
                    placeholder="e.g., GRN-20240101-001"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="receiptDate">Receipt Date *</label>
                  <input
                    type="date"
                    id="receiptDate"
                    value={formData.receiptDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, receiptDate: e.target.value }))}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="poId">Purchase Order *</label>
                  <select
                    id="poId"
                    value={formData.poId}
                    onChange={(e) => handlePOSelect(e.target.value)}
                    required
                  >
                    <option value="">Select Purchase Order</option>
                    {purchaseOrders.map(po => (
                      <option key={po.id} value={po.id}>
                        {po.poNumber} - {po.vendorName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Receipt Lines */}
              {selectedPO && (
                <div className="receipt-lines">
                  <div className="section-header">
                    <h3>Received Items</h3>
                    <small>Enter the actual quantities received for each item</small>
                  </div>

                  {receiptLines.map((line, index) => (
                    <div key={index} className="receipt-line">
                      <div className="form-row">
                        <div className="form-group">
                          <label>Product</label>
                          <input
                            type="text"
                            value={`${line.productSku} - ${line.productName}`}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Ordered Qty</label>
                          <input
                            type="number"
                            value={line.orderedQuantity}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Received Qty *</label>
                          <input
                            type="number"
                            min="0"
                            max={line.orderedQuantity}
                            value={line.receivedQuantity}
                            onChange={(e) => updateReceiptLine(index, 'receivedQuantity', parseInt(e.target.value) || 0)}
                            required
                          />
                        </div>
                        <div className="form-group">
                          <label>Condition</label>
                          <select
                            value={line.condition}
                            onChange={(e) => updateReceiptLine(index, 'condition', e.target.value)}
                          >
                            <option value="GOOD">Good</option>
                            <option value="DAMAGED">Damaged</option>
                            <option value="DEFECTIVE">Defective</option>
                            <option value="SHORT">Short</option>
                          </select>
                        </div>
                      </div>
                      <div className="form-row">
                        <div className="form-group">
                          <label>Unit Price</label>
                          <input
                            type="number"
                            step="0.01"
                            value={line.unitPrice}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Total Price</label>
                          <input
                            type="number"
                            step="0.01"
                            value={(line.receivedQuantity * line.unitPrice).toFixed(2)}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Notes</label>
                          <input
                            type="text"
                            value={line.notes}
                            onChange={(e) => updateReceiptLine(index, 'notes', e.target.value)}
                            placeholder="Line notes"
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="form-group">
                <label htmlFor="notes">Receipt Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                  rows={3}
                  placeholder="Additional notes for this receipt..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create Receipt
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PurchaseReceipts;
