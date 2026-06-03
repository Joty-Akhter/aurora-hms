import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import inventoryService, { Warehouse } from '../../services/inventoryService';
import api from '../../services/api';
import './Inventory.css';

interface ValuationSummary {
  totalValue: number;
  totalQuantity: number;
  itemCount: number;
  calculatedAt: string;
}

interface WarehouseValuation {
  warehouseId: string;
  totalValue: number;
  totalQuantity: number;
}

const InventoryValuation: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [summary, setSummary] = useState<ValuationSummary | null>(null);
  const [warehouseValues, setWarehouseValues] = useState<WarehouseValuation[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    await Promise.all([
      loadValuation(),
      loadWarehouses()
    ]);
  };

  const loadWarehouses = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const data = await inventoryService.getWarehouses(currentOrganizationId, true);
      setWarehouses(data);
    } catch (error) {
      console.error('Failed to load warehouses:', error);
    }
  };

  const loadValuation = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      
      // Get total valuation
      const summaryResponse = await api.get('/api/inventory/valuation/total', {
        params: { organizationId: currentOrganizationId }
      });
      setSummary(summaryResponse.data);
      
      // Get valuation by warehouse
      const warehouseResponse = await api.get('/api/inventory/valuation/by-warehouse', {
        params: { organizationId: currentOrganizationId }
      });
      setWarehouseValues(warehouseResponse.data);
      
    } catch (error) {
      console.error('Failed to load valuation:', error);
      alert('Failed to load inventory valuation');
    } finally {
      setLoading(false);
    }
  };

  const getWarehouseName = (warehouseId: string): string => {
    const warehouse = warehouses.find(w => w.id === warehouseId);
    return warehouse ? warehouse.name : warehouseId.substring(0, 8) + '...';
  };

  if (loading) return <div className="loading">Loading valuation...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Inventory Valuation</h1>
        <button className="btn-primary" onClick={loadValuation}>
          🔄 Refresh
        </button>
      </div>

      {summary && (
        <div className="valuation-summary">
          <div className="summary-cards">
            <div className="summary-card large">
              <h3>Total Inventory Value</h3>
              <div className="summary-value large">৳{summary.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</div>
              <div className="summary-meta">As of {new Date(summary.calculatedAt).toLocaleDateString()}</div>
            </div>
            
            <div className="summary-card">
              <h3>Total Quantity</h3>
              <div className="summary-value">{summary.totalQuantity.toLocaleString()}</div>
              <div className="summary-meta">Units in stock</div>
            </div>
            
            <div className="summary-card">
              <h3>Unique Items</h3>
              <div className="summary-value">{summary.itemCount}</div>
              <div className="summary-meta">SKUs tracked</div>
            </div>
            
            <div className="summary-card">
              <h3>Avg Unit Value</h3>
              <div className="summary-value">
                ৳{summary.totalQuantity > 0 
                  ? (summary.totalValue / summary.totalQuantity).toFixed(2)
                  : '0.00'}
              </div>
              <div className="summary-meta">Per unit</div>
            </div>
          </div>
        </div>
      )}

      <div className="section-header">
        <h2>Valuation by Warehouse</h2>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Warehouse</th>
              <th>Total Quantity</th>
              <th>Total Value</th>
              <th>Percentage of Total</th>
            </tr>
          </thead>
          <tbody>
            {warehouseValues.map((wh) => (
              <tr key={wh.warehouseId}>
                <td>{getWarehouseName(wh.warehouseId)}</td>
                <td>{wh.totalQuantity.toFixed(2)}</td>
                <td><strong>৳{wh.totalValue.toLocaleString('en-US', {minimumFractionDigits: 2})}</strong></td>
                <td>
                  {summary && (
                    <div className="progress-bar">
                      <div 
                        className="progress-fill"
                        style={{width: `${(wh.totalValue / summary.totalValue * 100).toFixed(1)}%`}}
                      >
                        {(wh.totalValue / summary.totalValue * 100).toFixed(1)}%
                      </div>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {warehouseValues.length === 0 && (
          <div className="no-data">No warehouse data available</div>
        )}
      </div>
    </div>
  );
};

export default InventoryValuation;

