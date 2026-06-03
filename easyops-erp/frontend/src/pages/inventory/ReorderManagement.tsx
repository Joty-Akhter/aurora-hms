import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

interface ReorderRule {
  id: string;
  productId: string;
  warehouseId: string;
  reorderPoint: number;
  reorderQuantity: number;
  minQuantity?: number;
  maxQuantity?: number;
  leadTimeDays?: number;
  safetyStock: number;
  isActive: boolean;
  lastTriggeredAt?: string;
  triggerCount: number;
}

interface ReorderAlert {
  id: string;
  productId: string;
  warehouseId: string;
  currentQuantity: number;
  reorderPoint: number;
  suggestedOrderQty: number;
  alertStatus: string;
  priority: string;
  createdAt: string;
  acknowledgedAt?: string;
}

interface Product {
  id: string;
  sku: string;
  name: string;
  description: string;
  category: string;
  unit: string;
  currentStock: number;
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

const ReorderManagement: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [activeTab, setActiveTab] = useState<'rules' | 'alerts'>('alerts');
  const [rules, setRules] = useState<ReorderRule[]>([]);
  const [alerts, setAlerts] = useState<ReorderAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [showOnlyOpen, setShowOnlyOpen] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [formData, setFormData] = useState({
    productId: '',
    warehouseId: '',
    reorderPoint: '',
    reorderQuantity: '',
    minQuantity: '',
    maxQuantity: '',
    leadTimeDays: '',
    safetyStock: '',
    preferredSupplierId: '',
    isActive: true,
    autoCreatePo: false,
    notes: ''
  });

  useEffect(() => {
    if (activeTab === 'rules') {
      loadRules();
    } else {
      loadAlerts();
    }
    loadProducts();
    loadWarehouses();
  }, [currentOrganizationId, activeTab, showOnlyOpen]);

  const loadRules = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const response = await api.get('/api/inventory/reorder/rules', {
        params: { organizationId: currentOrganizationId, activeOnly: true }
      });
      setRules(response.data);
    } catch (error) {
      console.error('Failed to load reorder rules:', error);
      alert('Failed to load reorder rules');
    } finally {
      setLoading(false);
    }
  };

  const loadAlerts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const response = await api.get('/api/inventory/reorder/alerts', {
        params: { organizationId: currentOrganizationId, openOnly: showOnlyOpen }
      });
      setAlerts(response.data);
    } catch (error) {
      console.error('Failed to load reorder alerts:', error);
      alert('Failed to load reorder alerts');
    } finally {
      setLoading(false);
    }
  };

  const loadProducts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/inventory/products', {
        params: { organizationId: currentOrganizationId, activeOnly: true }
      });
      setProducts(response.data);
    } catch (error) {
      console.error('Failed to load products:', error);
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

  const handleAcknowledgeAlert = async (alertId: string) => {
    if (!user?.id) return;
    
    try {
      await api.post(`/api/inventory/reorder/alerts/${alertId}/acknowledge`, {
        userId: user.id
      });
      loadAlerts();
      alert('Alert acknowledged successfully');
    } catch (error) {
      console.error('Failed to acknowledge alert:', error);
      alert('Failed to acknowledge alert');
    }
  };

  const handleCloseAlert = async (alertId: string) => {
    try {
      await api.post(`/api/inventory/reorder/alerts/${alertId}/close`);
      loadAlerts();
      alert('Alert closed successfully');
    } catch (error) {
      console.error('Failed to close alert:', error);
      alert('Failed to close alert');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId || !user?.id) return;

    try {
      const ruleData = {
        ...formData,
        organizationId: currentOrganizationId,
        createdBy: user.id,
        reorderPoint: parseFloat(formData.reorderPoint),
        reorderQuantity: parseFloat(formData.reorderQuantity),
        minQuantity: formData.minQuantity ? parseFloat(formData.minQuantity) : null,
        maxQuantity: formData.maxQuantity ? parseFloat(formData.maxQuantity) : null,
        leadTimeDays: formData.leadTimeDays ? parseInt(formData.leadTimeDays) : null,
        safetyStock: formData.safetyStock ? parseFloat(formData.safetyStock) : 0,
        preferredSupplierId: formData.preferredSupplierId || null
      };

      await api.post('/api/inventory/reorder/rules', ruleData);
      setShowModal(false);
      resetForm();
      loadRules();
      alert('Reorder rule created successfully');
    } catch (error) {
      console.error('Failed to create reorder rule:', error);
      alert('Failed to create reorder rule');
    }
  };

  const resetForm = () => {
    setFormData({
      productId: '',
      warehouseId: '',
      reorderPoint: '',
      reorderQuantity: '',
      minQuantity: '',
      maxQuantity: '',
      leadTimeDays: '',
      safetyStock: '',
      preferredSupplierId: '',
      isActive: true,
      autoCreatePo: false,
      notes: ''
    });
  };

  const openModal = () => {
    setShowModal(true);
  };

  const getPriorityClass = (priority: string): string => {
    switch (priority) {
      case 'CRITICAL': return 'critical';
      case 'HIGH': return 'high';
      case 'MEDIUM': return 'warning';
      default: return '';
    }
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Reorder Management</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Reorder Rule
        </button>
      </div>

      <div className="tabs">
        <button
          className={`tab ${activeTab === 'alerts' ? 'active' : ''}`}
          onClick={() => setActiveTab('alerts')}
        >
          Reorder Alerts ({alerts.filter(a => a.alertStatus === 'OPEN').length})
        </button>
        <button
          className={`tab ${activeTab === 'rules' ? 'active' : ''}`}
          onClick={() => setActiveTab('rules')}
        >
          Reorder Rules ({rules.length})
        </button>
      </div>

      {activeTab === 'alerts' && (
        <>
          <div className="filters">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={showOnlyOpen}
                onChange={(e) => setShowOnlyOpen(e.target.checked)}
              />
              Show Open Alerts Only
            </label>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Priority</th>
                  <th>Product ID</th>
                  <th>Warehouse</th>
                  <th>Current Qty</th>
                  <th>Reorder Point</th>
                  <th>Suggested Order</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id} className={getPriorityClass(alert.priority)}>
                    <td>
                      <span className={`priority-badge ${alert.priority.toLowerCase()}`}>
                        {alert.priority}
                      </span>
                    </td>
                    <td>{alert.productId.substring(0, 8)}...</td>
                    <td>{alert.warehouseId.substring(0, 8)}...</td>
                    <td><strong>{alert.currentQuantity.toFixed(2)}</strong></td>
                    <td>{alert.reorderPoint.toFixed(2)}</td>
                    <td className="text-success"><strong>{alert.suggestedOrderQty.toFixed(2)}</strong></td>
                    <td>
                      <span className={`status-badge ${alert.alertStatus.toLowerCase()}`}>
                        {alert.alertStatus}
                      </span>
                    </td>
                    <td>{new Date(alert.createdAt).toLocaleDateString()}</td>
                    <td>
                      {alert.alertStatus === 'OPEN' && (
                        <>
                          <button 
                            className="btn-small"
                            onClick={() => handleAcknowledgeAlert(alert.id)}
                          >
                            Acknowledge
                          </button>
                          {' '}
                          <button 
                            className="btn-small"
                            onClick={() => handleCloseAlert(alert.id)}
                          >
                            Close
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {alerts.length === 0 && (
              <div className="no-data">No reorder alerts found</div>
            )}
          </div>

          <div className="summary-cards">
            <div className="summary-card critical">
              <h3>Critical Alerts</h3>
              <div className="summary-value">
                {alerts.filter(a => a.priority === 'CRITICAL' && a.alertStatus === 'OPEN').length}
              </div>
            </div>
            <div className="summary-card high">
              <h3>High Priority</h3>
              <div className="summary-value">
                {alerts.filter(a => a.priority === 'HIGH' && a.alertStatus === 'OPEN').length}
              </div>
            </div>
            <div className="summary-card warning">
              <h3>Medium Priority</h3>
              <div className="summary-value">
                {alerts.filter(a => a.priority === 'MEDIUM' && a.alertStatus === 'OPEN').length}
              </div>
            </div>
            <div className="summary-card">
              <h3>Total Open</h3>
              <div className="summary-value">
                {alerts.filter(a => a.alertStatus === 'OPEN').length}
              </div>
            </div>
          </div>
        </>
      )}

      {activeTab === 'rules' && (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Product ID</th>
                <th>Warehouse</th>
                <th>Reorder Point</th>
                <th>Reorder Qty</th>
                <th>Safety Stock</th>
                <th>Lead Time</th>
                <th>Status</th>
                <th>Last Triggered</th>
                <th>Trigger Count</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((rule) => (
                <tr key={rule.id}>
                  <td>{rule.productId.substring(0, 8)}...</td>
                  <td>{rule.warehouseId.substring(0, 8)}...</td>
                  <td><strong>{rule.reorderPoint.toFixed(2)}</strong></td>
                  <td>{rule.reorderQuantity.toFixed(2)}</td>
                  <td>{rule.safetyStock.toFixed(2)}</td>
                  <td>{rule.leadTimeDays || '-'} days</td>
                  <td>
                    <span className={`status-badge ${rule.isActive ? 'active' : 'inactive'}`}>
                      {rule.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>{rule.lastTriggeredAt ? new Date(rule.lastTriggeredAt).toLocaleDateString() : 'Never'}</td>
                  <td>{rule.triggerCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {rules.length === 0 && (
            <div className="no-data">No reorder rules configured</div>
          )}
        </div>
      )}

      {/* Create Rule Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content large">
            <div className="modal-header">
              <h2>Create New Reorder Rule</h2>
              <button className="btn-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="productId">Product *</label>
                  <select
                    id="productId"
                    value={formData.productId}
                    onChange={(e) => setFormData(prev => ({ ...prev, productId: e.target.value }))}
                    required
                  >
                    <option value="">Select Product</option>
                    {products.map(product => (
                      <option key={product.id} value={product.id}>
                        {product.sku} - {product.name}
                      </option>
                    ))}
                  </select>
                </div>
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
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="reorderPoint">Reorder Point *</label>
                  <input
                    type="number"
                    id="reorderPoint"
                    min="0"
                    step="0.01"
                    value={formData.reorderPoint}
                    onChange={(e) => setFormData(prev => ({ ...prev, reorderPoint: e.target.value }))}
                    required
                    placeholder="e.g., 50"
                  />
                  <small>When stock falls below this level, reorder will be triggered</small>
                </div>
                <div className="form-group">
                  <label htmlFor="reorderQuantity">Reorder Quantity *</label>
                  <input
                    type="number"
                    id="reorderQuantity"
                    min="0"
                    step="0.01"
                    value={formData.reorderQuantity}
                    onChange={(e) => setFormData(prev => ({ ...prev, reorderQuantity: e.target.value }))}
                    required
                    placeholder="e.g., 100"
                  />
                  <small>Quantity to order when reorder point is reached</small>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="safetyStock">Safety Stock</label>
                  <input
                    type="number"
                    id="safetyStock"
                    min="0"
                    step="0.01"
                    value={formData.safetyStock}
                    onChange={(e) => setFormData(prev => ({ ...prev, safetyStock: e.target.value }))}
                    placeholder="e.g., 20"
                  />
                  <small>Buffer stock to prevent stockouts</small>
                </div>
                <div className="form-group">
                  <label htmlFor="leadTimeDays">Lead Time (Days)</label>
                  <input
                    type="number"
                    id="leadTimeDays"
                    min="0"
                    value={formData.leadTimeDays}
                    onChange={(e) => setFormData(prev => ({ ...prev, leadTimeDays: e.target.value }))}
                    placeholder="e.g., 7"
                  />
                  <small>Days from order to delivery</small>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="minQuantity">Min Quantity</label>
                  <input
                    type="number"
                    id="minQuantity"
                    min="0"
                    step="0.01"
                    value={formData.minQuantity}
                    onChange={(e) => setFormData(prev => ({ ...prev, minQuantity: e.target.value }))}
                    placeholder="e.g., 10"
                  />
                  <small>Minimum quantity to maintain</small>
                </div>
                <div className="form-group">
                  <label htmlFor="maxQuantity">Max Quantity</label>
                  <input
                    type="number"
                    id="maxQuantity"
                    min="0"
                    step="0.01"
                    value={formData.maxQuantity}
                    onChange={(e) => setFormData(prev => ({ ...prev, maxQuantity: e.target.value }))}
                    placeholder="e.g., 500"
                  />
                  <small>Maximum quantity to hold</small>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="preferredSupplierId">Preferred Supplier</label>
                  <input
                    type="text"
                    id="preferredSupplierId"
                    value={formData.preferredSupplierId}
                    onChange={(e) => setFormData(prev => ({ ...prev, preferredSupplierId: e.target.value }))}
                    placeholder="Supplier ID (optional)"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData(prev => ({ ...prev, isActive: e.target.checked }))}
                    />
                    Active Rule
                  </label>
                </div>
                <div className="form-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={formData.autoCreatePo}
                      onChange={(e) => setFormData(prev => ({ ...prev, autoCreatePo: e.target.checked }))}
                    />
                    Auto-create Purchase Order
                  </label>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="notes">Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                  rows={3}
                  placeholder="Additional notes for this reorder rule..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create Reorder Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReorderManagement;

