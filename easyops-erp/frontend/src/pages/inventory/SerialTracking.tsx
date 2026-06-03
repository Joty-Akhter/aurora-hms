import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

interface SerialNumber {
  id: string;
  productId: string;
  serialNumber: string;
  warehouseId?: string;
  batchNumber?: string;
  purchaseDate?: string;
  warrantyStartDate?: string;
  warrantyEndDate?: string;
  status: string;
  customerId?: string;
  saleDate?: string;
  saleInvoiceNumber?: string;
}

const SerialTracking: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [serials, setSerials] = useState<SerialNumber[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingSerial, setEditingSerial] = useState<SerialNumber | null>(null);
  const [products, setProducts] = useState<any[]>([]);
  const [warehouses, setWarehouses] = useState<any[]>([]);
  const [formData, setFormData] = useState({
    productId: '',
    serialNumber: '',
    warehouseId: '',
    batchNumber: '',
    purchaseDate: '',
    warrantyStartDate: '',
    warrantyEndDate: '',
    warrantyPeriodMonths: '',
    status: 'IN_STOCK',
    notes: ''
  });

  useEffect(() => {
    loadSerials();
    loadProducts();
    loadWarehouses();
  }, [currentOrganizationId, filterStatus]);

  const loadSerials = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      
      const response = await api.get('/api/inventory/serials', { params });
      setSerials(response.data);
    } catch (error) {
      console.error('Failed to load serial numbers:', error);
      alert('Failed to load serial numbers');
    } finally {
      setLoading(false);
    }
  };

  const loadProducts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const response = await api.get('/api/inventory/products', {
        params: { organizationId: currentOrganizationId }
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
        params: { organizationId: currentOrganizationId }
      });
      setWarehouses(response.data);
    } catch (error) {
      console.error('Failed to load warehouses:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      const serialData = {
        ...formData,
        organizationId: currentOrganizationId,
        warrantyPeriodMonths: formData.warrantyPeriodMonths ? parseInt(formData.warrantyPeriodMonths) : null,
        purchaseDate: formData.purchaseDate || null,
        warrantyStartDate: formData.warrantyStartDate || null,
        warrantyEndDate: formData.warrantyEndDate || null
      };

      if (editingSerial) {
        await api.put(`/api/inventory/serials/${editingSerial.id}`, serialData);
      } else {
        await api.post('/api/inventory/serials', serialData);
      }

      setShowModal(false);
      setEditingSerial(null);
      resetForm();
      loadSerials();
    } catch (error) {
      console.error('Failed to save serial:', error);
      alert('Failed to save serial number');
    }
  };

  const handleEdit = (serial: SerialNumber) => {
    setEditingSerial(serial);
    setFormData({
      productId: serial.productId,
      serialNumber: serial.serialNumber,
      warehouseId: serial.warehouseId || '',
      batchNumber: serial.batchNumber || '',
      purchaseDate: serial.purchaseDate || '',
      warrantyStartDate: serial.warrantyStartDate || '',
      warrantyEndDate: serial.warrantyEndDate || '',
      warrantyPeriodMonths: serial.warrantyPeriodMonths?.toString() || '',
      status: serial.status,
      notes: serial.notes || ''
    });
    setShowModal(true);
  };

  const handleStatusChange = async (serialId: string, action: string) => {
    try {
      if (action === 'allocate') {
        const salesOrderId = prompt('Enter Sales Order ID:');
        if (salesOrderId) {
          await api.post(`/api/inventory/serials/${serialId}/allocate`, null, {
            params: { salesOrderId }
          });
        }
      } else if (action === 'sell') {
        const customerId = prompt('Enter Customer ID:');
        const invoiceNumber = prompt('Enter Invoice Number:');
        if (customerId && invoiceNumber) {
          await api.post(`/api/inventory/serials/${serialId}/sell`, null, {
            params: { customerId, invoiceNumber }
          });
        }
      } else if (action === 'return') {
        await api.post(`/api/inventory/serials/${serialId}/return`);
      }
      loadSerials();
    } catch (error) {
      console.error('Failed to update serial status:', error);
      alert('Failed to update serial status');
    }
  };

  const resetForm = () => {
    setFormData({
      productId: '',
      serialNumber: '',
      warehouseId: '',
      batchNumber: '',
      purchaseDate: '',
      warrantyStartDate: '',
      warrantyEndDate: '',
      warrantyPeriodMonths: '',
      status: 'IN_STOCK',
      notes: ''
    });
  };

  const openModal = () => {
    setEditingSerial(null);
    resetForm();
    setShowModal(true);
  };

  const isUnderWarranty = (warrantyEndDate?: string): boolean => {
    if (!warrantyEndDate) return false;
    return new Date(warrantyEndDate) > new Date();
  };

  if (loading) return <div className="loading">Loading serial numbers...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Serial Number Tracking</h1>
        <button className="btn-primary" onClick={openModal}>
          + Register Serial
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="IN_STOCK">In Stock</option>
            <option value="ALLOCATED">Allocated</option>
            <option value="SOLD">Sold</option>
            <option value="IN_SERVICE">In Service</option>
            <option value="RETURNED">Returned</option>
            <option value="SCRAPPED">Scrapped</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Serial Number</th>
              <th>Product ID</th>
              <th>Batch</th>
              <th>Purchase Date</th>
              <th>Warranty End</th>
              <th>Warranty Status</th>
              <th>Status</th>
              <th>Customer</th>
              <th>Sale Date</th>
              <th>Invoice</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {serials.map((serial) => (
              <tr key={serial.id}>
                <td><strong>{serial.serialNumber}</strong></td>
                <td>{serial.productId.substring(0, 8)}...</td>
                <td>{serial.batchNumber || '-'}</td>
                <td>{serial.purchaseDate ? new Date(serial.purchaseDate).toLocaleDateString() : '-'}</td>
                <td>{serial.warrantyEndDate ? new Date(serial.warrantyEndDate).toLocaleDateString() : '-'}</td>
                <td>
                  {serial.warrantyEndDate && (
                    <span className={`warranty-badge ${isUnderWarranty(serial.warrantyEndDate) ? 'valid' : 'expired'}`}>
                      {isUnderWarranty(serial.warrantyEndDate) ? 'Valid' : 'Expired'}
                    </span>
                  )}
                </td>
                <td>
                  <span className={`status-badge ${serial.status.toLowerCase().replace('_', '-')}`}>
                    {serial.status.replace('_', ' ')}
                  </span>
                </td>
                <td>{serial.customerId ? serial.customerId.substring(0, 8) + '...' : '-'}</td>
                <td>{serial.saleDate ? new Date(serial.saleDate).toLocaleDateString() : '-'}</td>
                <td>{serial.saleInvoiceNumber || '-'}</td>
                <td>
                  <div className="action-buttons">
                    <button 
                      className="btn-small btn-secondary" 
                      onClick={() => handleEdit(serial)}
                      title="Edit Serial"
                    >
                      ✏️
                    </button>
                    {serial.status === 'IN_STOCK' && (
                      <button 
                        className="btn-small btn-primary" 
                        onClick={() => handleStatusChange(serial.id, 'allocate')}
                        title="Allocate"
                      >
                        📋
                      </button>
                    )}
                    {serial.status === 'ALLOCATED' && (
                      <button 
                        className="btn-small btn-success" 
                        onClick={() => handleStatusChange(serial.id, 'sell')}
                        title="Sell"
                      >
                        💰
                      </button>
                    )}
                    {serial.status === 'SOLD' && (
                      <button 
                        className="btn-small btn-warning" 
                        onClick={() => handleStatusChange(serial.id, 'return')}
                        title="Return"
                      >
                        🔄
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {serials.length === 0 && (
          <div className="no-data">No serial numbers found</div>
        )}
      </div>

      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Serials</h3>
          <div className="summary-value">{serials.length}</div>
        </div>
        <div className="summary-card">
          <h3>In Stock</h3>
          <div className="summary-value">
            {serials.filter(s => s.status === 'IN_STOCK').length}
          </div>
        </div>
        <div className="summary-card warning">
          <h3>Allocated</h3>
          <div className="summary-value">
            {serials.filter(s => s.status === 'ALLOCATED').length}
          </div>
        </div>
        <div className="summary-card">
          <h3>Sold</h3>
          <div className="summary-value">
            {serials.filter(s => s.status === 'SOLD').length}
          </div>
        </div>
      </div>

      {/* Serial Registration/Edit Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>{editingSerial ? 'Edit Serial Number' : 'Register New Serial Number'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            <form onSubmit={handleSubmit} className="serial-form">
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
                  <label htmlFor="serialNumber">Serial Number *</label>
                  <input
                    type="text"
                    id="serialNumber"
                    value={formData.serialNumber}
                    onChange={(e) => setFormData({...formData, serialNumber: e.target.value})}
                    required
                    placeholder="Enter unique serial number"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="warehouseId">Warehouse</label>
                  <select
                    id="warehouseId"
                    value={formData.warehouseId}
                    onChange={(e) => setFormData({...formData, warehouseId: e.target.value})}
                  >
                    <option value="">Select Warehouse</option>
                    {warehouses.map(warehouse => (
                      <option key={warehouse.id} value={warehouse.id}>
                        {warehouse.name} ({warehouse.code})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="batchNumber">Batch Number</label>
                  <input
                    type="text"
                    id="batchNumber"
                    value={formData.batchNumber}
                    onChange={(e) => setFormData({...formData, batchNumber: e.target.value})}
                    placeholder="Optional batch number"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="purchaseDate">Purchase Date</label>
                  <input
                    type="date"
                    id="purchaseDate"
                    value={formData.purchaseDate}
                    onChange={(e) => setFormData({...formData, purchaseDate: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="status">Status</label>
                  <select
                    id="status"
                    value={formData.status}
                    onChange={(e) => setFormData({...formData, status: e.target.value})}
                  >
                    <option value="IN_STOCK">In Stock</option>
                    <option value="ALLOCATED">Allocated</option>
                    <option value="SOLD">Sold</option>
                    <option value="IN_SERVICE">In Service</option>
                    <option value="RETURNED">Returned</option>
                    <option value="SCRAPPED">Scrapped</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="warrantyStartDate">Warranty Start Date</label>
                  <input
                    type="date"
                    id="warrantyStartDate"
                    value={formData.warrantyStartDate}
                    onChange={(e) => setFormData({...formData, warrantyStartDate: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="warrantyEndDate">Warranty End Date</label>
                  <input
                    type="date"
                    id="warrantyEndDate"
                    value={formData.warrantyEndDate}
                    onChange={(e) => setFormData({...formData, warrantyEndDate: e.target.value})}
                  />
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="warrantyPeriodMonths">Warranty Period (Months)</label>
                <input
                  type="number"
                  id="warrantyPeriodMonths"
                  value={formData.warrantyPeriodMonths}
                  onChange={(e) => setFormData({...formData, warrantyPeriodMonths: e.target.value})}
                  min="0"
                  max="120"
                  placeholder="e.g., 12 for 1 year"
                />
              </div>

              <div className="form-group">
                <label htmlFor="notes">Notes</label>
                <textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData({...formData, notes: e.target.value})}
                  rows={3}
                  placeholder="Additional notes about this serial number..."
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingSerial ? 'Update Serial' : 'Register Serial'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SerialTracking;

