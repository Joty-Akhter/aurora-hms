import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import inventoryService, { Warehouse } from '../../services/inventoryService';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

const Warehouses: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingWarehouse, setEditingWarehouse] = useState<Warehouse | null>(null);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    warehouseType: 'MAIN',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
    phone: '',
    email: '',
    totalCapacity: '',
    capacityUnit: 'M3',
    isActive: true,
    status: 'OPERATIONAL'
  });

  useEffect(() => {
    loadWarehouses();
  }, [currentOrganizationId]);

  const loadWarehouses = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      const data = await inventoryService.getWarehouses(currentOrganizationId);
      setWarehouses(data);
    } catch (error) {
      console.error('Failed to load warehouses:', error);
      alert('Failed to load warehouses');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;

    try {
      const warehouseData = {
        ...formData,
        organizationId: currentOrganizationId,
        totalCapacity: formData.totalCapacity ? parseFloat(formData.totalCapacity) : null
      };

      if (editingWarehouse) {
        await api.put(`/api/inventory/warehouses/${editingWarehouse.id}`, warehouseData);
      } else {
        await api.post('/api/inventory/warehouses', warehouseData);
      }

      setShowModal(false);
      setEditingWarehouse(null);
      resetForm();
      loadWarehouses();
    } catch (error) {
      console.error('Failed to save warehouse:', error);
      alert('Failed to save warehouse');
    }
  };

  const handleEdit = (warehouse: Warehouse) => {
    setEditingWarehouse(warehouse);
    setFormData({
      code: warehouse.code,
      name: warehouse.name,
      warehouseType: warehouse.warehouseType || 'MAIN',
      addressLine1: warehouse.addressLine1 || '',
      addressLine2: warehouse.addressLine2 || '',
      city: warehouse.city || '',
      state: warehouse.state || '',
      postalCode: warehouse.postalCode || '',
      country: warehouse.country || '',
      phone: warehouse.phone || '',
      email: warehouse.email || '',
      totalCapacity: warehouse.totalCapacity?.toString() || '',
      capacityUnit: warehouse.capacityUnit || 'M3',
      isActive: warehouse.isActive ?? true,
      status: warehouse.status || 'OPERATIONAL'
    });
    setShowModal(true);
  };

  const handleDelete = async (warehouseId: string) => {
    if (!confirm('Are you sure you want to delete this warehouse?')) return;

    try {
      await api.delete(`/api/inventory/warehouses/${warehouseId}`);
      loadWarehouses();
    } catch (error) {
      console.error('Failed to delete warehouse:', error);
      alert('Failed to delete warehouse');
    }
  };

  const handleStatusChange = async (warehouseId: string, status: string) => {
    try {
      const warehouse = warehouses.find(w => w.id === warehouseId);
      if (warehouse) {
        const updatedWarehouse = { ...warehouse, status };
        await api.put(`/api/inventory/warehouses/${warehouseId}`, updatedWarehouse);
        loadWarehouses();
      }
    } catch (error) {
      console.error('Failed to update warehouse status:', error);
      alert('Failed to update warehouse status');
    }
  };

  const resetForm = () => {
    setFormData({
      code: '',
      name: '',
      warehouseType: 'MAIN',
      addressLine1: '',
      addressLine2: '',
      city: '',
      state: '',
      postalCode: '',
      country: '',
      phone: '',
      email: '',
      totalCapacity: '',
      capacityUnit: 'M3',
      isActive: true,
      status: 'OPERATIONAL'
    });
  };

  const openModal = () => {
    setEditingWarehouse(null);
    resetForm();
    setShowModal(true);
  };

  if (loading) return <div className="loading">Loading warehouses...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Warehouses</h1>
        <button className="btn-primary" onClick={openModal}>
          + Add Warehouse
        </button>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Type</th>
              <th>City</th>
              <th>State</th>
              <th>Country</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {warehouses.map((warehouse) => (
              <tr key={warehouse.id}>
                <td><strong>{warehouse.code}</strong></td>
                <td>{warehouse.name}</td>
                <td>{warehouse.warehouseType}</td>
                <td>{warehouse.city || '-'}</td>
                <td>{warehouse.state || '-'}</td>
                <td>{warehouse.country || '-'}</td>
                <td>{warehouse.phone || '-'}</td>
                <td>
                  <span className={`status-badge ${warehouse.status.toLowerCase()}`}>
                    {warehouse.status}
                  </span>
                </td>
                <td>
                  {warehouse.isActive ? (
                    <span className="badge-success">Yes</span>
                  ) : (
                    <span className="badge-danger">No</span>
                  )}
                </td>
                <td>
                  <div className="action-buttons">
                    <button 
                      className="btn-small btn-secondary" 
                      onClick={() => handleEdit(warehouse)}
                      title="Edit Warehouse"
                    >
                      ✏️
                    </button>
                    <button 
                      className="btn-small btn-danger" 
                      onClick={() => handleDelete(warehouse.id)}
                      title="Delete Warehouse"
                    >
                      🗑️
                    </button>
                    <select 
                      value={warehouse.status} 
                      onChange={(e) => handleStatusChange(warehouse.id, e.target.value)}
                      className="status-select"
                    >
                      <option value="OPERATIONAL">Operational</option>
                      <option value="MAINTENANCE">Maintenance</option>
                      <option value="CLOSED">Closed</option>
                      <option value="CONSTRUCTION">Construction</option>
                    </select>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {warehouses.length === 0 && (
          <div className="no-data">No warehouses found</div>
        )}
      </div>

      {/* Warehouse Registration/Edit Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>{editingWarehouse ? 'Edit Warehouse' : 'Add New Warehouse'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            <form onSubmit={handleSubmit} className="warehouse-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="code">Warehouse Code *</label>
                  <input
                    type="text"
                    id="code"
                    value={formData.code}
                    onChange={(e) => setFormData({...formData, code: e.target.value})}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="name">Warehouse Name *</label>
                  <input
                    type="text"
                    id="name"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="warehouseType">Warehouse Type</label>
                  <select
                    id="warehouseType"
                    value={formData.warehouseType}
                    onChange={(e) => setFormData({...formData, warehouseType: e.target.value})}
                  >
                    <option value="MAIN">Main</option>
                    <option value="DISTRIBUTION">Distribution</option>
                    <option value="TRANSIT">Transit</option>
                    <option value="STORAGE">Storage</option>
                    <option value="COLD_STORAGE">Cold Storage</option>
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="status">Status</label>
                  <select
                    id="status"
                    value={formData.status}
                    onChange={(e) => setFormData({...formData, status: e.target.value})}
                  >
                    <option value="OPERATIONAL">Operational</option>
                    <option value="MAINTENANCE">Maintenance</option>
                    <option value="CLOSED">Closed</option>
                    <option value="CONSTRUCTION">Construction</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="addressLine1">Address Line 1</label>
                <input
                  type="text"
                  id="addressLine1"
                  value={formData.addressLine1}
                  onChange={(e) => setFormData({...formData, addressLine1: e.target.value})}
                />
              </div>

              <div className="form-group">
                <label htmlFor="addressLine2">Address Line 2</label>
                <input
                  type="text"
                  id="addressLine2"
                  value={formData.addressLine2}
                  onChange={(e) => setFormData({...formData, addressLine2: e.target.value})}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="city">City</label>
                  <input
                    type="text"
                    id="city"
                    value={formData.city}
                    onChange={(e) => setFormData({...formData, city: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="state">State/Province</label>
                  <input
                    type="text"
                    id="state"
                    value={formData.state}
                    onChange={(e) => setFormData({...formData, state: e.target.value})}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="postalCode">Postal Code</label>
                  <input
                    type="text"
                    id="postalCode"
                    value={formData.postalCode}
                    onChange={(e) => setFormData({...formData, postalCode: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="country">Country</label>
                  <input
                    type="text"
                    id="country"
                    value={formData.country}
                    onChange={(e) => setFormData({...formData, country: e.target.value})}
                    maxLength={2}
                    placeholder="US"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="phone">Phone</label>
                  <input
                    type="tel"
                    id="phone"
                    value={formData.phone}
                    onChange={(e) => setFormData({...formData, phone: e.target.value})}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="email">Email</label>
                  <input
                    type="email"
                    id="email"
                    value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="totalCapacity">Total Capacity</label>
                  <input
                    type="number"
                    id="totalCapacity"
                    value={formData.totalCapacity}
                    onChange={(e) => setFormData({...formData, totalCapacity: e.target.value})}
                    step="0.01"
                    min="0"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="capacityUnit">Capacity Unit</label>
                  <select
                    id="capacityUnit"
                    value={formData.capacityUnit}
                    onChange={(e) => setFormData({...formData, capacityUnit: e.target.value})}
                  >
                    <option value="M3">Cubic Meters (M³)</option>
                    <option value="FT3">Cubic Feet (ft³)</option>
                    <option value="PALLETS">Pallets</option>
                    <option value="UNITS">Units</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({...formData, isActive: e.target.checked})}
                  />
                  Active Warehouse
                </label>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingWarehouse ? 'Update Warehouse' : 'Create Warehouse'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Warehouses;

