import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import './Inventory.css';

interface StockSummary {
  totalSKUs: number;
  totalValue: number;
  totalQuantityOnHand: number;
  totalQuantityAllocated: number;
  totalQuantityAvailable: number;
  outOfStockItems: number;
  lowStockItems: number;
  reportDate: string;
}

interface TurnoverReport {
  periodDays: number;
  totalCOGS: number;
  avgInventoryValue: number;
  turnoverRatio: number;
  annualizedTurnover: number;
  daysOfInventory: number;
}

const InventoryReports: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [summary, setSummary] = useState<StockSummary | null>(null);
  const [turnover, setTurnover] = useState<TurnoverReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [turnoverDays, setTurnoverDays] = useState(30);

  useEffect(() => {
    loadReports();
  }, [currentOrganizationId, turnoverDays]);

  const loadReports = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      
      // Get stock summary
      const summaryResponse = await api.get('/api/inventory/reports/summary', {
        params: { organizationId: currentOrganizationId }
      });
      setSummary(summaryResponse.data);
      
      // Get turnover analysis
      try {
        const turnoverResponse = await api.get('/api/inventory/reports/turnover', {
          params: { organizationId: currentOrganizationId, days: turnoverDays }
        });
        setTurnover(turnoverResponse.data);
      } catch (error) {
        console.log('Turnover report not available yet');
      }
      
    } catch (error) {
      console.error('Failed to load reports:', error);
      alert('Failed to load inventory reports');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading reports...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Inventory Reports & Analytics</h1>
        <button className="btn-primary" onClick={loadReports}>
          🔄 Refresh
        </button>
      </div>

      {summary && (
        <>
          <div className="section-header">
            <h2>Stock Summary</h2>
            <span className="report-date">As of {new Date(summary.reportDate).toLocaleDateString()}</span>
          </div>

          <div className="summary-cards">
            <div className="summary-card large">
              <h3>Total Inventory Value</h3>
              <div className="summary-value large">
                ৳{summary.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}
              </div>
            </div>
            
            <div className="summary-card">
              <h3>Total SKUs</h3>
              <div className="summary-value">{summary.totalSKUs}</div>
            </div>
            
            <div className="summary-card">
              <h3>On Hand</h3>
              <div className="summary-value">{summary.totalQuantityOnHand.toLocaleString()}</div>
            </div>
            
            <div className="summary-card warning">
              <h3>Allocated</h3>
              <div className="summary-value">{summary.totalQuantityAllocated.toLocaleString()}</div>
            </div>
            
            <div className="summary-card">
              <h3>Available</h3>
              <div className="summary-value">{summary.totalQuantityAvailable.toLocaleString()}</div>
            </div>
            
            <div className="summary-card critical">
              <h3>Out of Stock</h3>
              <div className="summary-value">{summary.outOfStockItems}</div>
            </div>
            
            <div className="summary-card warning">
              <h3>Low Stock</h3>
              <div className="summary-value">{summary.lowStockItems}</div>
            </div>
          </div>
        </>
      )}

      {turnover && (
        <>
          <div className="section-header">
            <h2>Inventory Turnover Analysis</h2>
            <div className="filter-group">
              <label>Period:</label>
              <select value={turnoverDays} onChange={(e) => setTurnoverDays(Number(e.target.value))}>
                <option value="7">Last 7 Days</option>
                <option value="30">Last 30 Days</option>
                <option value="90">Last 90 Days</option>
                <option value="365">Last Year</option>
              </select>
            </div>
          </div>

          <div className="report-grid">
            <div className="report-card">
              <h4>Total COGS</h4>
              <div className="report-value">৳{turnover.totalCOGS.toLocaleString('en-US', {minimumFractionDigits: 2})}</div>
              <div className="report-meta">Cost of goods sold</div>
            </div>
            
            <div className="report-card">
              <h4>Avg Inventory Value</h4>
              <div className="report-value">৳{turnover.avgInventoryValue.toLocaleString('en-US', {minimumFractionDigits: 2})}</div>
              <div className="report-meta">Average stock value</div>
            </div>
            
            <div className="report-card">
              <h4>Turnover Ratio</h4>
              <div className="report-value">{turnover.turnoverRatio.toFixed(2)}x</div>
              <div className="report-meta">For {turnover.periodDays} days</div>
            </div>
            
            <div className="report-card highlight">
              <h4>Annualized Turnover</h4>
              <div className="report-value">{turnover.annualizedTurnover.toFixed(2)}x</div>
              <div className="report-meta">Per year (projected)</div>
            </div>
            
            <div className="report-card">
              <h4>Days of Inventory</h4>
              <div className="report-value">{turnover.daysOfInventory.toFixed(0)} days</div>
              <div className="report-meta">Average holding period</div>
            </div>
          </div>
        </>
      )}

      <div className="info-box">
        <h3>📊 Report Definitions</h3>
        <ul>
          <li><strong>Turnover Ratio</strong>: Measures how many times inventory is sold and replaced over a period</li>
          <li><strong>Days of Inventory</strong>: Average number of days items stay in inventory before being sold</li>
          <li><strong>Higher turnover ratio</strong> = More efficient inventory management</li>
          <li><strong>Lower days of inventory</strong> = Faster moving stock, less capital tied up</li>
        </ul>
      </div>
    </div>
  );
};

export default InventoryReports;

