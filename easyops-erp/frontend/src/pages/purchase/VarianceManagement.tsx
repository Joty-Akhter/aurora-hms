import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import './Purchase.css';

interface InvoiceVariance {
  invoiceId: string;
  organizationId: string;
  invoiceNumber: string;
  invoiceDate: string;
  poNumber: string;
  vendorName: string;
  totalAmount: number;
  priceVariance: number;
  quantityVariance: number;
  matchingStatus: string;
  varianceApprovedBy?: string;
  varianceApprovedAt?: string;
  varianceSeverity: string;
  varianceLineCount: number;
}

const VarianceManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [variances, setVariances] = useState<InvoiceVariance[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterSeverity, setFilterSeverity] = useState('');

  useEffect(() => {
    loadVariances();
  }, [currentOrganizationId, filterSeverity]);

  const loadVariances = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterSeverity) params.severity = filterSeverity;
      
      const response = await api.get('/api/purchase/invoices/variances', { params });
      setVariances(response.data);
    } catch (error) {
      console.error('Failed to load invoice variances:', error);
      alert('Failed to load invoice variances');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveVariance = async (invoiceId: string) => {
    if (!user?.id) return;
    
    const notes = prompt('Enter approval notes for this variance:');
    if (notes === null) return;
    
    try {
      await api.post(`/api/purchase/invoices/${invoiceId}/approve-variance`, {
        approvedBy: user.id,
        notes: notes
      });
      loadVariances();
      alert('Variance approved successfully');
    } catch (error) {
      console.error('Failed to approve variance:', error);
      alert('Failed to approve variance');
    }
  };

  const handleReject = async (invoiceId: string) => {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;
    
    try {
      await api.post(`/api/purchase/invoices/${invoiceId}/reject`, { reason });
      loadVariances();
      alert('Invoice rejected due to variance');
    } catch (error) {
      console.error('Failed to reject invoice:', error);
      alert('Failed to reject invoice');
    }
  };

  const getSeverityColor = (severity: string): string => {
    switch (severity) {
      case 'HIGH': return 'critical';
      case 'MEDIUM': return 'warning';
      case 'LOW': return 'matched';
      default: return '';
    }
  };

  if (loading) return <div className="loading">Loading variances...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Invoice Variance Management</h1>
        <button className="btn-primary" onClick={loadVariances}>
          🔄 Refresh
        </button>
      </div>

      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="summary-card critical">
          <h3>High Priority Variances</h3>
          <div className="summary-value">
            {variances.filter(v => v.varianceSeverity === 'HIGH').length}
          </div>
        </div>
        
        <div className="summary-card warning">
          <h3>Medium Priority Variances</h3>
          <div className="summary-value">
            {variances.filter(v => v.varianceSeverity === 'MEDIUM').length}
          </div>
        </div>
        
        <div className="summary-card">
          <h3>Low Priority Variances</h3>
          <div className="summary-value">
            {variances.filter(v => v.varianceSeverity === 'LOW').length}
          </div>
        </div>
        
        <div className="summary-card">
          <h3>Total Variance Amount</h3>
          <div className="summary-value">
            ${variances.reduce((sum, v) => sum + Math.abs(v.priceVariance || 0), 0).toLocaleString('en-US', {minimumFractionDigits: 2})}
          </div>
        </div>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Severity:</label>
          <select value={filterSeverity} onChange={(e) => setFilterSeverity(e.target.value)}>
            <option value="">All Severities</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
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
              <th>Vendor</th>
              <th>Total Amount</th>
              <th>Price Variance</th>
              <th>Qty Variance</th>
              <th>Severity</th>
              <th>Line Issues</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {variances.map((variance) => (
              <tr key={variance.invoiceId}>
                <td><strong>{variance.invoiceNumber}</strong></td>
                <td>{new Date(variance.invoiceDate).toLocaleDateString()}</td>
                <td>{variance.poNumber}</td>
                <td>{variance.vendorName}</td>
                <td>
                  <strong>${variance.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                </td>
                <td>
                  <span className={variance.priceVariance > 0 ? 'over-invoiced' : variance.priceVariance < 0 ? 'under-invoiced' : 'matched'}>
                    ${Math.abs(variance.priceVariance).toLocaleString('en-US', {minimumFractionDigits: 2})}
                    {variance.priceVariance > 0 ? ' ↑' : variance.priceVariance < 0 ? ' ↓' : ''}
                  </span>
                </td>
                <td>
                  <span className={variance.quantityVariance > 0 ? 'over-invoiced' : variance.quantityVariance < 0 ? 'under-invoiced' : 'matched'}>
                    {Math.abs(variance.quantityVariance).toFixed(2)}
                    {variance.quantityVariance > 0 ? ' ↑' : variance.quantityVariance < 0 ? ' ↓' : ''}
                  </span>
                </td>
                <td>
                  <span className={`status-badge ${getSeverityColor(variance.varianceSeverity)}`}>
                    {variance.varianceSeverity}
                  </span>
                </td>
                <td>
                  <strong>{variance.varianceLineCount}</strong> lines
                </td>
                <td>
                  {variance.varianceApprovedBy ? (
                    <span className="status-badge approved" title={`Approved by ${variance.varianceApprovedBy}`}>
                      ✓ Approved
                    </span>
                  ) : (
                    <span className="status-badge warning">
                      Pending
                    </span>
                  )}
                </td>
                <td>
                  {!variance.varianceApprovedBy && (
                    <>
                      <button 
                        className="btn-small btn-warning"
                        onClick={() => handleApproveVariance(variance.invoiceId)}
                        title="Approve this variance"
                      >
                        ✓ Approve
                      </button>
                      {' '}
                      <button 
                        className="btn-small btn-danger"
                        onClick={() => handleReject(variance.invoiceId)}
                        title="Reject invoice"
                      >
                        ✗ Reject
                      </button>
                    </>
                  )}
                  {variance.varianceApprovedBy && (
                    <button 
                      className="btn-small btn-secondary"
                      onClick={() => alert(`Approved by: ${variance.varianceApprovedBy}\nApproved at: ${variance.varianceApprovedAt}`)}
                      title="View approval details"
                    >
                      ℹ️ Details
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {variances.length === 0 && (
          <div className="no-data">
            🎉 No invoice variances found! All invoices are perfectly matched.
          </div>
        )}
      </div>

      {/* Variance Guidelines */}
      <div className="table-container" style={{ marginTop: '20px' }}>
        <div style={{ padding: '20px' }}>
          <h3 style={{ margin: '0 0 15px 0', color: '#2c3e50' }}>📋 Variance Approval Guidelines</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px' }}>
            <div className="guideline-card">
              <h4 style={{ margin: '0 0 10px 0', color: '#28a745' }}>✓ Low Severity (Auto-Approve)</h4>
              <p style={{ margin: 0, fontSize: '14px', color: '#666' }}>
                Price variance &lt; $100<br/>
                Quantity variance &lt; 5 units
              </p>
            </div>
            <div className="guideline-card">
              <h4 style={{ margin: '0 0 10px 0', color: '#ffc107' }}>⚠️ Medium Severity (Review)</h4>
              <p style={{ margin: 0, fontSize: '14px', color: '#666' }}>
                Price variance $100-$1,000<br/>
                Quantity variance 5-10 units
              </p>
            </div>
            <div className="guideline-card">
              <h4 style={{ margin: '0 0 10px 0', color: '#dc3545' }}>⚠️ High Severity (Manager Approval)</h4>
              <p style={{ margin: 0, fontSize: '14px', color: '#666' }}>
                Price variance &gt; $1,000<br/>
                Quantity variance &gt; 10 units
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VarianceManagement;
