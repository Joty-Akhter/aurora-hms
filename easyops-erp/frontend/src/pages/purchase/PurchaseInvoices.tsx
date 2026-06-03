import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Purchase.css';

interface PurchaseInvoice {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  poId: string;
  poNumber: string;
  receiptId: string;
  receiptNumber: string;
  vendorId: string;
  vendorName: string;
  status: string;
  matchingStatus?: string; // PENDING, MATCHED, VARIANCE, APPROVED
  totalAmount: number;
  currency: string;
  dueDate: string;
  paidAmount: number;
  balanceAmount: number;
  priceVariance?: number;
  quantityVariance?: number;
  createdBy: string;
  approvedBy?: string;
  approvedAt?: string;
  varianceApprovedBy?: string;
  varianceApprovedAt?: string;
  notes?: string;
  lines: InvoiceLine[];
}

interface InvoiceLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  orderedQuantity: number;
  receivedQuantity: number;
  invoicedQuantity: number;
  unitPrice: number;
  totalPrice: number;
  variance: number;
  status: string;
}

interface PurchaseReceipt {
  id: string;
  receiptNumber: string;
  poNumber: string;
  vendorName: string;
  status: string;
  totalAmount: number;
  lines: Array<{
    productId: string;
    productName: string;
    productSku: string;
    receivedQuantity: number;
    unitPrice: number;
  }>;
}

const PurchaseInvoices: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [invoices, setInvoices] = useState<PurchaseInvoice[]>([]);
  const [receipts, setReceipts] = useState<PurchaseReceipt[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState<PurchaseReceipt | null>(null);
  const [formData, setFormData] = useState({
    invoiceNumber: '',
    invoiceDate: new Date().toISOString().split('T')[0],
    receiptId: '',
    dueDate: '',
    currency: 'USD',
    notes: ''
  });
  const [invoiceLines, setInvoiceLines] = useState<Array<{
    productId: string;
    productName: string;
    productSku: string;
    orderedQuantity: number;
    receivedQuantity: number;
    invoicedQuantity: number;
    unitPrice: number;
    totalPrice: number;
    variance: number;
    status: string;
  }>>([]);

  useEffect(() => {
    loadInvoices();
    loadReceipts();
  }, [currentOrganizationId, filterStatus]);

  const loadInvoices = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      
      const response = await api.get('/api/purchase/invoices', { params });
      setInvoices(response.data);
    } catch (error) {
      console.error('Failed to load purchase invoices:', error);
      alert('Failed to load purchase invoices');
    } finally {
      setLoading(false);
    }
  };

  const loadReceipts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/purchase/receipts', {
        params: { organizationId: currentOrganizationId, status: 'RECEIVED' }
      });
      setReceipts(response.data);
    } catch (error) {
      console.error('Failed to load purchase receipts:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id) return;

    // Validate that we have at least one invoice line
    const validLines = invoiceLines.filter(line => line.invoicedQuantity > 0);
    if (validLines.length === 0) {
      alert('Please enter invoiced quantities for at least one item');
      return;
    }

    try {
      const invoiceData = {
        ...formData,
        organizationId: currentOrganizationId,
        createdBy: user.id,
        status: 'DRAFT',
        lines: validLines.map(line => ({
          productId: line.productId,
          invoicedQuantity: line.invoicedQuantity,
          unitPrice: line.unitPrice,
          status: line.status
        }))
      };

      await api.post('/api/purchase/invoices', invoiceData);
      setShowModal(false);
      resetForm();
      loadInvoices();
      alert('Purchase invoice created successfully');
    } catch (error) {
      console.error('Failed to create purchase invoice:', error);
      alert('Failed to create purchase invoice');
    }
  };

  const handleReceiptSelect = (receiptId: string) => {
    const receipt = receipts.find(r => r.id === receiptId);
    if (receipt) {
      setSelectedReceipt(receipt);
      setFormData(prev => ({ ...prev, receiptId }));
      
      // Initialize invoice lines from receipt lines
      const lines = receipt.lines.map(line => ({
        productId: line.productId,
        productName: line.productName,
        productSku: line.productSku,
        orderedQuantity: 0, // Will be filled from PO
        receivedQuantity: line.receivedQuantity,
        invoicedQuantity: line.receivedQuantity, // Default to received quantity
        unitPrice: line.unitPrice,
        totalPrice: line.receivedQuantity * line.unitPrice,
        variance: 0,
        status: 'MATCHED'
      }));
      setInvoiceLines(lines);
    }
  };

  const updateInvoiceLine = (index: number, field: string, value: any) => {
    const updatedLines = [...invoiceLines];
    updatedLines[index] = { ...updatedLines[index], [field]: value };
    
    // Recalculate total price and variance
    const line = updatedLines[index];
    line.totalPrice = line.invoicedQuantity * line.unitPrice;
    line.variance = line.invoicedQuantity - line.receivedQuantity;
    
    // Determine status based on variance
    if (line.variance === 0) {
      line.status = 'MATCHED';
    } else if (line.variance > 0) {
      line.status = 'OVER_INVOICED';
    } else {
      line.status = 'UNDER_INVOICED';
    }
    
    setInvoiceLines(updatedLines);
  };

  const handleApprove = async (invoiceId: string) => {
    if (!user?.id) return;
    
    if (!confirm('Are you sure you want to approve this invoice?')) return;
    
    try {
      await api.post(`/api/purchase/invoices/${invoiceId}/approve`, {
        approvedBy: user.id
      });
      loadInvoices();
      alert('Purchase invoice approved successfully');
    } catch (error) {
      console.error('Failed to approve invoice:', error);
      alert('Failed to approve invoice');
    }
  };

  const handleCreateBill = async (invoiceId: string) => {
    try {
      await api.post(`/api/purchase/invoices/${invoiceId}/create-bill`);
      loadInvoices();
      alert('Bill created successfully and added to Accounts Payable');
    } catch (error) {
      console.error('Failed to create bill:', error);
      alert('Failed to create bill');
    }
  };

  const handleApproveVariance = async (invoiceId: string) => {
    if (!user?.id) return;
    
    const notes = prompt('Enter approval notes for this variance:');
    if (notes === null) return; // User cancelled
    
    try {
      await api.post(`/api/purchase/invoices/${invoiceId}/approve-variance`, {
        approvedBy: user.id,
        notes: notes
      });
      loadInvoices();
      alert('Variance approved successfully');
    } catch (error) {
      console.error('Failed to approve variance:', error);
      alert('Failed to approve variance');
    }
  };

  const runThreeWayMatching = async (invoiceId: string) => {
    try {
      const response = await api.post(`/api/purchase/invoices/${invoiceId}/match`);
      const matchResult = response.data;
      
      if (matchResult.matched) {
        alert('✅ Perfect Match!\nNo variances detected.');
      } else {
        alert(`⚠️ Variance Detected!\n\nPrice Variance: $${matchResult.priceVariance}\nQuantity Variance: ${matchResult.quantityVariance}\n\nPlease review and approve if acceptable.`);
      }
      
      loadInvoices();
    } catch (error) {
      console.error('Failed to run three-way matching:', error);
      alert('Failed to run three-way matching');
    }
  };

  const resetForm = () => {
    setFormData({
      invoiceNumber: '',
      invoiceDate: new Date().toISOString().split('T')[0],
      receiptId: '',
      dueDate: '',
      currency: 'USD',
      notes: ''
    });
    setInvoiceLines([]);
    setSelectedReceipt(null);
  };

  const openModal = () => {
    // Generate a unique invoice number
    const invoiceNumber = `PINV-${Date.now()}`;
    setFormData(prev => ({ ...prev, invoiceNumber }));
    setShowModal(true);
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'DRAFT': return 'draft';
      case 'APPROVED': return 'approved';
      case 'POSTED': return 'completed';
      case 'CANCELLED': return 'cancelled';
      default: return '';
    }
  };

  const getVarianceColor = (variance: number): string => {
    if (variance === 0) return 'matched';
    if (variance > 0) return 'over-invoiced';
    return 'under-invoiced';
  };

  if (loading) return <div className="loading">Loading purchase invoices...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Purchase Invoices</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Invoice
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="DRAFT">Draft</option>
            <option value="APPROVED">Approved</option>
            <option value="POSTED">Posted</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Invoice Number</th>
              <th>Date</th>
              <th>PO Number</th>
              <th>Receipt Number</th>
              <th>Vendor</th>
              <th>Status</th>
              <th>Matching</th>
              <th>Total Amount</th>
              <th>Variance</th>
              <th>Due Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((invoice) => (
              <tr key={invoice.id}>
                <td><strong>{invoice.invoiceNumber}</strong></td>
                <td>{new Date(invoice.invoiceDate).toLocaleDateString()}</td>
                <td>{invoice.poNumber}</td>
                <td>{invoice.receiptNumber}</td>
                <td>{invoice.vendorName}</td>
                <td>
                  <span className={`status-badge ${getStatusColor(invoice.status)}`}>
                    {invoice.status}
                  </span>
                </td>
                <td>
                  {invoice.matchingStatus && (
                    <span className={`status-badge ${
                      invoice.matchingStatus === 'MATCHED' ? 'matched' :
                      invoice.matchingStatus === 'VARIANCE' ? 'warning' :
                      invoice.matchingStatus === 'APPROVED' ? 'approved' : 'draft'
                    }`}>
                      {invoice.matchingStatus}
                    </span>
                  )}
                </td>
                <td>
                  <strong>{invoice.currency} {invoice.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                </td>
                <td>
                  {invoice.priceVariance != null && invoice.priceVariance !== 0 && (
                    <span className={invoice.priceVariance > 0 ? 'over-invoiced' : 'under-invoiced'}>
                      ${Math.abs(invoice.priceVariance).toLocaleString('en-US', {minimumFractionDigits: 2})}
                    </span>
                  )}
                </td>
                <td>{new Date(invoice.dueDate).toLocaleDateString()}</td>
                <td>
                  {invoice.status === 'DRAFT' && (
                    <>
                      <button 
                        className="btn-small btn-secondary"
                        onClick={() => runThreeWayMatching(invoice.id)}
                        title="Run three-way matching"
                      >
                        🔍 Match
                      </button>
                      {' '}
                      <button 
                        className="btn-small btn-primary"
                        onClick={() => handleApprove(invoice.id)}
                      >
                        ✓ Approve
                      </button>
                    </>
                  )}
                  {invoice.matchingStatus === 'VARIANCE' && invoice.status === 'DRAFT' && (
                    <>
                      {' '}
                      <button 
                        className="btn-small btn-warning"
                        onClick={() => handleApproveVariance(invoice.id)}
                        title="Approve this variance"
                      >
                        ⚠️ Approve Variance
                      </button>
                    </>
                  )}
                  {invoice.status === 'APPROVED' && (
                    <button 
                      className="btn-small btn-primary"
                      onClick={() => handleCreateBill(invoice.id)}
                    >
                      📝 Create Bill
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {invoices.length === 0 && (
          <div className="no-data">No purchase invoices found</div>
        )}
      </div>

      {/* Create Invoice Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content large">
            <div className="modal-header">
              <h2>Create New Purchase Invoice</h2>
              <button className="btn-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="invoiceNumber">Invoice Number *</label>
                  <input
                    type="text"
                    id="invoiceNumber"
                    value={formData.invoiceNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, invoiceNumber: e.target.value }))}
                    required
                    placeholder="e.g., PINV-20240101-001"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="invoiceDate">Invoice Date *</label>
                  <input
                    type="date"
                    id="invoiceDate"
                    value={formData.invoiceDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, invoiceDate: e.target.value }))}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="receiptId">Purchase Receipt *</label>
                  <select
                    id="receiptId"
                    value={formData.receiptId}
                    onChange={(e) => handleReceiptSelect(e.target.value)}
                    required
                  >
                    <option value="">Select Purchase Receipt</option>
                    {receipts.map(receipt => (
                      <option key={receipt.id} value={receipt.id}>
                        {receipt.receiptNumber} - {receipt.vendorName}
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
                  <label htmlFor="dueDate">Due Date</label>
                  <input
                    type="date"
                    id="dueDate"
                    value={formData.dueDate}
                    onChange={(e) => setFormData(prev => ({ ...prev, dueDate: e.target.value }))}
                  />
                </div>
              </div>

              {/* Invoice Lines */}
              {selectedReceipt && (
                <div className="invoice-lines">
                  <div className="section-header">
                    <h3>Invoice Items</h3>
                    <small>Review and adjust quantities for invoicing</small>
                  </div>

                  {invoiceLines.map((line, index) => (
                    <div key={index} className="invoice-line">
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
                          <label>Received Qty</label>
                          <input
                            type="number"
                            value={line.receivedQuantity}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Invoiced Qty *</label>
                          <input
                            type="number"
                            min="0"
                            value={line.invoicedQuantity}
                            onChange={(e) => updateInvoiceLine(index, 'invoicedQuantity', parseInt(e.target.value) || 0)}
                            required
                          />
                        </div>
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
                      </div>
                      <div className="form-row">
                        <div className="form-group">
                          <label>Total Price</label>
                          <input
                            type="number"
                            step="0.01"
                            value={line.totalPrice.toFixed(2)}
                            disabled
                            style={{ background: '#f8f9fa' }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Variance</label>
                          <input
                            type="number"
                            value={line.variance}
                            disabled
                            style={{ 
                              background: '#f8f9fa',
                              color: line.variance === 0 ? '#28a745' : line.variance > 0 ? '#dc3545' : '#ffc107'
                            }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Status</label>
                          <input
                            type="text"
                            value={line.status}
                            disabled
                            style={{ 
                              background: '#f8f9fa',
                              color: line.status === 'MATCHED' ? '#28a745' : line.status === 'OVER_INVOICED' ? '#dc3545' : '#ffc107'
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="form-group">
                <label htmlFor="notes">Invoice Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                  rows={3}
                  placeholder="Additional notes for this invoice..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create Invoice
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PurchaseInvoices;
