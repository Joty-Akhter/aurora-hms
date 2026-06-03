import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import './Purchase.css';

interface PurchaseStats {
  totalPOs: number;
  pendingApproval: number;
  approvedPOs: number;
  receivedPOs: number;
  totalValue: number;
  currency: string;
}

interface RecentPO {
  id: string;
  poNumber: string;
  vendorName: string;
  totalAmount: number;
  status: string;
  poDate: string;
}

interface TopVendor {
  vendorId: string;
  vendorName: string;
  totalAmount: number;
  poCount: number;
}

const PurchaseDashboard: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [stats, setStats] = useState<PurchaseStats | null>(null);
  const [recentPOs, setRecentPOs] = useState<RecentPO[]>([]);
  const [topVendors, setTopVendors] = useState<TopVendor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, [currentOrganizationId]);

  const loadDashboardData = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      
      // Load stats
      const statsResponse = await api.get('/api/purchase/dashboard/stats', {
        params: { organizationId: currentOrganizationId }
      });
      setStats(statsResponse.data);
      
      // Load recent POs
      const recentResponse = await api.get('/api/purchase/orders/recent', {
        params: { organizationId: currentOrganizationId, limit: 5 }
      });
      setRecentPOs(recentResponse.data);
      
      // Load top vendors
      const vendorsResponse = await api.get('/api/purchase/dashboard/top-vendors', {
        params: { organizationId: currentOrganizationId, limit: 5 }
      });
      setTopVendors(vendorsResponse.data);
      
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
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

  if (loading) return <div className="loading">Loading dashboard...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Purchase Dashboard</h1>
        <button className="btn-primary" onClick={loadDashboardData}>
          🔄 Refresh
        </button>
      </div>

      {/* Summary Cards */}
      {stats && (
        <div className="summary-cards">
          <div className="summary-card">
            <h3>Total Purchase Orders</h3>
            <div className="summary-value">{stats.totalPOs}</div>
          </div>
          
          <div className="summary-card warning">
            <h3>Pending Approval</h3>
            <div className="summary-value">{stats.pendingApproval}</div>
          </div>
          
          <div className="summary-card">
            <h3>Approved POs</h3>
            <div className="summary-value">{stats.approvedPOs}</div>
          </div>
          
          <div className="summary-card">
            <h3>Received POs</h3>
            <div className="summary-value">{stats.receivedPOs}</div>
          </div>
          
          <div className="summary-card">
            <h3>Total Value</h3>
            <div className="summary-value">
              {stats.currency} {stats.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2})}
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
        {/* Recent Purchase Orders */}
        <div className="table-container">
          <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Recent Purchase Orders</h3>
          </div>
          <div style={{ padding: '0' }}>
            {recentPOs.length > 0 ? (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>PO Number</th>
                    <th>Vendor</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentPOs.map((po) => (
                    <tr key={po.id}>
                      <td><strong>{po.poNumber}</strong></td>
                      <td>{po.vendorName}</td>
                      <td>${po.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}</td>
                      <td>
                        <span className={`status-badge ${getStatusColor(po.status)}`}>
                          {po.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="no-data">No recent purchase orders</div>
            )}
          </div>
        </div>

        {/* Top Vendors */}
        <div className="table-container">
          <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Top Vendors by Spend</h3>
          </div>
          <div style={{ padding: '0' }}>
            {topVendors.length > 0 ? (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Vendor</th>
                    <th>PO Count</th>
                    <th>Total Spend</th>
                  </tr>
                </thead>
                <tbody>
                  {topVendors.map((vendor, index) => (
                    <tr key={vendor.vendorId}>
                      <td>
                        <strong>#{index + 1}</strong> {vendor.vendorName}
                      </td>
                      <td>{vendor.poCount}</td>
                      <td>
                        <strong>${vendor.totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="no-data">No vendor data available</div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="table-container">
        <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
          <h3 style={{ margin: 0, color: '#2c3e50' }}>Quick Actions</h3>
        </div>
        <div style={{ padding: '20px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
            <button className="btn-primary" style={{ padding: '15px', textAlign: 'center' }}>
              📝 Create Purchase Order
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              📋 View All POs
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              🏢 Manage Vendors
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              📊 Purchase Reports
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PurchaseDashboard;
