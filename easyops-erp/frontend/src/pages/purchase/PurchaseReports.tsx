import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import './Purchase.css';

interface PurchaseReport {
  totalPOs: number;
  totalValue: number;
  currency: string;
  averagePOValue: number;
  pendingApproval: number;
  approvedPOs: number;
  receivedPOs: number;
  cancelledPOs: number;
}

interface VendorPerformance {
  vendorId: string;
  vendorName: string;
  totalPOs: number;
  totalValue: number;
  averageDeliveryDays: number;
  onTimeDeliveryRate: number;
  qualityRating: number;
}

interface SpendAnalysis {
  category: string;
  totalSpend: number;
  percentage: number;
  poCount: number;
  averageValue: number;
}

interface TopVendor {
  vendorId: string;
  vendorName: string;
  totalSpend: number;
  poCount: number;
  lastOrderDate: string;
}

const PurchaseReports: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [report, setReport] = useState<PurchaseReport | null>(null);
  const [vendorPerformance, setVendorPerformance] = useState<VendorPerformance[]>([]);
  const [spendAnalysis, setSpendAnalysis] = useState<SpendAnalysis[]>([]);
  const [topVendors, setTopVendors] = useState<TopVendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    loadReportData();
  }, [currentOrganizationId, dateRange]);

  const loadReportData = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      
      // Load purchase summary
      const reportResponse = await api.get('/api/purchase/reports/summary', {
        params: { 
          organizationId: currentOrganizationId,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      });
      setReport(reportResponse.data);
      
      // Load vendor performance
      const vendorResponse = await api.get('/api/purchase/reports/vendor-performance', {
        params: { 
          organizationId: currentOrganizationId,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      });
      setVendorPerformance(vendorResponse.data);
      
      // Load spend analysis
      const spendResponse = await api.get('/api/purchase/reports/spend-analysis', {
        params: { 
          organizationId: currentOrganizationId,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      });
      setSpendAnalysis(spendResponse.data);
      
      // Load top vendors
      const topVendorsResponse = await api.get('/api/purchase/reports/top-vendors', {
        params: { 
          organizationId: currentOrganizationId,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate,
          limit: 10
        }
      });
      setTopVendors(topVendorsResponse.data);
      
    } catch (error) {
      console.error('Failed to load report data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (field: string, value: string) => {
    setDateRange(prev => ({ ...prev, [field]: value }));
  };

  if (loading) return <div className="loading">Loading reports...</div>;

  return (
    <div className="purchase-page">
      <div className="page-header">
        <h1>Purchase Reports & Analytics</h1>
        <button className="btn-primary" onClick={loadReportData}>
          🔄 Refresh
        </button>
      </div>

      {/* Date Range Filter */}
      <div className="filters">
        <div className="filter-group">
          <label>Start Date:</label>
          <input
            type="date"
            value={dateRange.startDate}
            onChange={(e) => handleDateRangeChange('startDate', e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>End Date:</label>
          <input
            type="date"
            value={dateRange.endDate}
            onChange={(e) => handleDateRangeChange('endDate', e.target.value)}
          />
        </div>
      </div>

      {/* Summary Cards */}
      {report && (
        <div className="summary-cards">
          <div className="summary-card large">
            <h3>Total Purchase Value</h3>
            <div className="summary-value large">
              {report.currency} {report.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}
            </div>
            <div className="summary-meta">For {dateRange.startDate} to {dateRange.endDate}</div>
          </div>
          
          <div className="summary-card">
            <h3>Total POs</h3>
            <div className="summary-value">{report.totalPOs}</div>
            <div className="summary-meta">Purchase orders</div>
          </div>
          
          <div className="summary-card">
            <h3>Average PO Value</h3>
            <div className="summary-value">
              {report.currency} {report.averagePOValue.toLocaleString('en-US', {minimumFractionDigits: 2})}
            </div>
            <div className="summary-meta">Per purchase order</div>
          </div>
          
          <div className="summary-card warning">
            <h3>Pending Approval</h3>
            <div className="summary-value">{report.pendingApproval}</div>
            <div className="summary-meta">Awaiting approval</div>
          </div>
          
          <div className="summary-card">
            <h3>Approved POs</h3>
            <div className="summary-value">{report.approvedPOs}</div>
            <div className="summary-meta">Approved orders</div>
          </div>
          
          <div className="summary-card">
            <h3>Received POs</h3>
            <div className="summary-value">{report.receivedPOs}</div>
            <div className="summary-meta">Completed orders</div>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
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
                    <th>Rank</th>
                    <th>Vendor</th>
                    <th>Total Spend</th>
                    <th>PO Count</th>
                    <th>Last Order</th>
                  </tr>
                </thead>
                <tbody>
                  {topVendors.map((vendor, index) => (
                    <tr key={vendor.vendorId}>
                      <td><strong>#{index + 1}</strong></td>
                      <td>{vendor.vendorName}</td>
                      <td>
                        <strong>${vendor.totalSpend.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                      </td>
                      <td>{vendor.poCount}</td>
                      <td>{new Date(vendor.lastOrderDate).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="no-data">No vendor data available</div>
            )}
          </div>
        </div>

        {/* Spend Analysis */}
        <div className="table-container">
          <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
            <h3 style={{ margin: 0, color: '#2c3e50' }}>Spend Analysis by Category</h3>
          </div>
          <div style={{ padding: '0' }}>
            {spendAnalysis.length > 0 ? (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Total Spend</th>
                    <th>Percentage</th>
                    <th>PO Count</th>
                    <th>Avg Value</th>
                  </tr>
                </thead>
                <tbody>
                  {spendAnalysis.map((category, index) => (
                    <tr key={index}>
                      <td><strong>{category.category}</strong></td>
                      <td>
                        <strong>${category.totalSpend.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                      </td>
                      <td>
                        <div className="progress-bar">
                          <div 
                            className="progress-fill"
                            style={{width: `${category.percentage}%`}}
                          >
                            {category.percentage.toFixed(1)}%
                          </div>
                        </div>
                      </td>
                      <td>{category.poCount}</td>
                      <td>${category.averageValue.toLocaleString('en-US', {minimumFractionDigits: 2})}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="no-data">No spend analysis data available</div>
            )}
          </div>
        </div>
      </div>

      {/* Vendor Performance */}
      <div className="table-container">
        <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
          <h3 style={{ margin: 0, color: '#2c3e50' }}>Vendor Performance Metrics</h3>
        </div>
        <div style={{ padding: '0' }}>
          {vendorPerformance.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Vendor</th>
                  <th>Total POs</th>
                  <th>Total Value</th>
                  <th>Avg Delivery Days</th>
                  <th>On-Time Rate</th>
                  <th>Quality Rating</th>
                </tr>
              </thead>
              <tbody>
                {vendorPerformance.map((vendor) => (
                  <tr key={vendor.vendorId}>
                    <td><strong>{vendor.vendorName}</strong></td>
                    <td>{vendor.totalPOs}</td>
                    <td>
                      <strong>${vendor.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong>
                    </td>
                    <td>{vendor.averageDeliveryDays.toFixed(1)} days</td>
                    <td>
                      <span className={`status-badge ${vendor.onTimeDeliveryRate >= 90 ? 'approved' : vendor.onTimeDeliveryRate >= 70 ? 'warning' : 'cancelled'}`}>
                        {vendor.onTimeDeliveryRate.toFixed(1)}%
                      </span>
                    </td>
                    <td>
                      <div className="rating-stars">
                        {'★'.repeat(Math.floor(vendor.qualityRating))}
                        {'☆'.repeat(5 - Math.floor(vendor.qualityRating))}
                        <span style={{ marginLeft: '5px', fontSize: '12px' }}>
                          ({vendor.qualityRating.toFixed(1)}/5)
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="no-data">No vendor performance data available</div>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="table-container">
        <div style={{ padding: '15px', borderBottom: '1px solid #e9ecef' }}>
          <h3 style={{ margin: 0, color: '#2c3e50' }}>Report Actions</h3>
        </div>
        <div style={{ padding: '20px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
            <button className="btn-primary" style={{ padding: '15px', textAlign: 'center' }}>
              📊 Export to Excel
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              📈 Detailed Analytics
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              📋 Vendor Report
            </button>
            <button className="btn-secondary" style={{ padding: '15px', textAlign: 'center' }}>
              💰 Budget Analysis
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PurchaseReports;
