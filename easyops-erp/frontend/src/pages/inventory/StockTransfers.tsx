import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import inventoryService, { Product, Warehouse } from '../../services/inventoryService';
import api from '../../services/api';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

interface StockTransfer {
  id: string;
  transferNumber: string;
  transferDate: string;
  fromWarehouseId: string;
  toWarehouseId: string;
  status: string;
  priority: string;
  transferType: string;
  trackingNumber?: string;
  expectedDeliveryDate?: string;
  actualDeliveryDate?: string;
  requestedBy?: string;
  approvedAt?: string;
  shippedAt?: string;
  receivedAt?: string;
  lines: TransferLine[];
}

interface TransferLine {
  id: string;
  productId: string;
  requestedQuantity: number;
  shippedQuantity: number;
  receivedQuantity: number;
  varianceQuantity: number;
  status: string;
}

interface TransferItem {
  productId: string;
  productName: string;
  productCode: string;
  packSize: number;
  quantity: number;
  tradePrice: number;
  mrp: number;
  amount: number;
}

const StockTransfers: React.FC = () => {
  const { currentOrganizationId, currentOrganizationName, user } = useAuth();
  const [transfers, setTransfers] = useState<StockTransfer[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [filterStatus, setFilterStatus] = useState('');
  const [showPendingOnly, setShowPendingOnly] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [productFilter, setProductFilter] = useState('');
  const [showProductSelector, setShowProductSelector] = useState(false);
  
  // Form state
  const [transferDate, setTransferDate] = useState(new Date().toISOString().split('T')[0]);
  const [fromWarehouseId, setFromWarehouseId] = useState<string>('');
  const [toWarehouseId, setToWarehouseId] = useState<string>('');
  const [priority, setPriority] = useState('NORMAL');
  const [transferType, setTransferType] = useState('STANDARD');
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<TransferItem[]>([]);

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId, filterStatus, showPendingOnly]);

  const loadData = async () => {
    if (!currentOrganizationId) return;
    
    try {
      setLoading(true);
      await Promise.all([
        loadTransfers(),
        loadWarehouses(),
        loadProducts()
      ]);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadTransfers = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const params: any = { organizationId: currentOrganizationId };
      if (filterStatus) params.status = filterStatus;
      if (showPendingOnly) params.pendingOnly = true;
      
      const response = await api.get('/api/inventory/transfers', { params });
      setTransfers(response.data);
    } catch (error) {
      console.error('Failed to load transfers:', error);
      alert('Failed to load stock transfers');
    }
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

  const loadProducts = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const data = await inventoryService.getProducts(currentOrganizationId, true);
      setProducts(data);
    } catch (error) {
      console.error('Failed to load products:', error);
    }
  };

  const filteredProducts = products.filter(product =>
    product.name.toLowerCase().includes(productFilter.toLowerCase()) ||
    product.sku.toLowerCase().includes(productFilter.toLowerCase())
  );

  const addProduct = (product: Product) => {
    if (items.some(item => item.productId === product.id)) {
      alert('Product already added');
      return;
    }

    const newItem: TransferItem = {
      productId: product.id,
      productName: product.name,
      productCode: product.sku,
      packSize: product.packSize || 1,
      quantity: 0,
      tradePrice: product.wholesalePrice || product.costPrice || 0,
      mrp: product.retailPrice || product.sellingPrice || 0,
      amount: 0
    };

    setItems([...items, newItem]);
    setProductFilter('');
    setShowProductSelector(false);
  };

  const removeItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const updateItem = (index: number, field: keyof TransferItem, value: number | string) => {
    const updatedItems = [...items];
    const item = updatedItems[index];

    if (field === 'quantity') {
      item.quantity = value as number;
    }

    item.amount = item.quantity * item.tradePrice;
    
    updatedItems[index] = item;
    setItems(updatedItems);
  };

  const calculateTotals = () => {
    const totalQuantity = items.reduce((sum, item) => sum + item.quantity, 0);
    const totalAmount = items.reduce((sum, item) => sum + item.amount, 0);
    return { totalQuantity, totalAmount };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!currentOrganizationId || !fromWarehouseId || !toWarehouseId || items.length === 0) {
      alert('Please fill in all required fields and add at least one product');
      return;
    }

    if (fromWarehouseId === toWarehouseId) {
      alert('From and To warehouses cannot be the same');
      return;
    }

    const invalidItems = items.filter(item => 
      item.quantity <= 0 || item.tradePrice <= 0
    );

    if (invalidItems.length > 0) {
      alert('Please ensure all items have valid quantities and prices');
      return;
    }

    try {
      setSaving(true);
      // Let backend generate transfer number to avoid conflicts
      const transferData = {
        organizationId: currentOrganizationId,
        transferDate: transferDate,
        fromWarehouseId: fromWarehouseId,
        toWarehouseId: toWarehouseId,
        priority: priority,
        transferType: transferType,
        expectedDeliveryDate: expectedDeliveryDate || undefined,
        notes: notes || undefined,
        reason: 'Stock transfer',
        status: 'DRAFT',
        requestedBy: user?.id || undefined,
        createdBy: user?.id || undefined,
        lines: items.map((item, index) => ({
          lineNumber: index + 1,
          productId: item.productId,
          requestedQuantity: item.quantity,
          unitCost: item.tradePrice,
          notes: `TP: ${item.tradePrice}, MRP: ${item.mrp}` || undefined
        }))
      };

      const response = await api.post('/api/inventory/transfers', transferData);
      alert('Stock transfer created successfully');
      setShowModal(false);
      resetForm();
      loadTransfers();
    } catch (error: any) {
      console.error('Failed to create transfer:', error);
      console.error('Error response:', error.response?.data);
      let errorMessage = 'Failed to create stock transfer';
      if (error.response?.data) {
        if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (error.response.data.error) {
          errorMessage = error.response.data.error;
        } else if (typeof error.response.data === 'string') {
          errorMessage = error.response.data;
        } else {
          errorMessage = JSON.stringify(error.response.data);
        }
      } else if (error.message) {
        errorMessage = error.message;
      }
      alert(errorMessage);
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setTransferDate(new Date().toISOString().split('T')[0]);
    setFromWarehouseId('');
    setToWarehouseId('');
    setPriority('NORMAL');
    setTransferType('STANDARD');
    setExpectedDeliveryDate('');
    setNotes('');
    setItems([]);
  };

  const openModal = () => {
    resetForm();
    setShowModal(true);
  };

  const handleGenerateInvoice = async (transferId: string) => {
    try {
      const transfer = transfers.find(t => t.id === transferId);
      if (!transfer) {
        alert('Transfer not found');
        return;
      }

      // Generate invoice - this could be a PDF or print view
      // For now, we'll create a printable view
      const challanWindow = window.open('', '_blank');
      if (!challanWindow) {
        alert('Please allow popups to generate invoice');
        return;
      }

      const fromWarehouse = warehouses.find(w => w.id === transfer.fromWarehouseId);
      const toWarehouse = warehouses.find(w => w.id === transfer.toWarehouseId);
      
      let challanHTML = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>Invoice - ${transfer.transferNumber}</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; }
            .header { text-align: center; margin-bottom: 30px; }
            .header h1 { margin: 0; }
            .info { margin-bottom: 20px; }
            .info-row { margin: 5px 0; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
            .total { text-align: right; font-weight: bold; margin-top: 20px; }
            .footer { margin-top: 40px; text-align: center; }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>${currentOrganizationName || 'Organization'}</h1>
            <h2 style="margin: 4px 0;">STOCK TRANSFER INVOICE</h2>
            <p>Transfer Number: ${transfer.transferNumber}</p>
          </div>
          <div class="info">
            <div class="info-row"><strong>Date:</strong> ${new Date(transfer.transferDate).toLocaleDateString()}</div>
            <div class="info-row"><strong>From Warehouse:</strong> ${fromWarehouse?.name || transfer.fromWarehouseId}</div>
            <div class="info-row"><strong>To Warehouse:</strong> ${toWarehouse?.name || transfer.toWarehouseId}</div>
            <div class="info-row"><strong>Priority:</strong> ${transfer.priority}</div>
            <div class="info-row"><strong>Status:</strong> ${transfer.status}</div>
          </div>
          <table>
            <thead>
              <tr>
                <th>Product Name</th>
                <th>Code</th>
                <th>Quantity</th>
                <th>TP</th>
                <th>MRP</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
      `;

      let totalAmount = 0;
      for (const line of transfer.lines) {
        const product = products.find(p => p.id === line.productId);
        const tp = product?.wholesalePrice || 0;
        const mrp = product?.retailPrice || 0;
        const amount = tp * line.requestedQuantity;
        totalAmount += amount;

        challanHTML += `
          <tr>
            <td>${product?.name || line.productId}</td>
            <td>${product?.sku || '-'}</td>
            <td>${line.requestedQuantity}</td>
            <td>৳${tp.toFixed(2)}</td>
            <td>৳${mrp.toFixed(2)}</td>
            <td>৳${amount.toFixed(2)}</td>
          </tr>
        `;
      }

      challanHTML += `
            </tbody>
          </table>
          <div class="total">
            <strong>Total Amount: ৳${totalAmount.toFixed(2)}</strong>
          </div>
          <div class="footer">
            <p>Generated on: ${new Date().toLocaleString()}</p>
          </div>
        </body>
        </html>
      `;

      challanWindow.document.write(challanHTML);
      challanWindow.document.close();
      challanWindow.print();
    } catch (error) {
      console.error('Failed to generate invoice:', error);
      alert('Failed to generate invoice');
    }
  };

  const handleSubmitTransfer = async (transferId: string) => {
    if (!user?.id) return;
    
    try {
      await api.post(`/api/inventory/transfers/${transferId}/submit`, {
        userId: user.id
      });
      loadTransfers();
      alert('Transfer submitted for approval');
    } catch (error) {
      console.error('Failed to submit transfer:', error);
      alert('Failed to submit transfer');
    }
  };

  const handleApproveTransfer = async (transferId: string) => {
    if (!user?.id) return;
    
    try {
      await api.post(`/api/inventory/transfers/${transferId}/approve`, {
        approvedBy: user.id
      });
      loadTransfers();
      alert('Transfer approved successfully');
    } catch (error) {
      console.error('Failed to approve transfer:', error);
      alert('Failed to approve transfer');
    }
  };

  const handleShipTransfer = async (transferId: string) => {
    if (!user?.id) return;
    
    const trackingNumber = prompt('Enter tracking number (optional):');
    
    try {
      await api.post(`/api/inventory/transfers/${transferId}/ship`, {
        shippedBy: user.id,
        trackingNumber: trackingNumber || undefined
      });
      loadTransfers();
      alert('Transfer shipped successfully');
    } catch (error) {
      console.error('Failed to ship transfer:', error);
      alert('Failed to ship transfer');
    }
  };

  const handleReceiveTransfer = async (transferId: string) => {
    if (!user?.id) return;
    
    if (!confirm('Confirm receipt of this transfer?')) return;
    
    try {
      await api.post(`/api/inventory/transfers/${transferId}/receive`, {
        receivedBy: user.id
      });
      loadTransfers();
      alert('Transfer received successfully');
    } catch (error) {
      console.error('Failed to receive transfer:', error);
      alert('Failed to receive transfer');
    }
  };

  const handleCancelTransfer = async (transferId: string) => {
    const reason = prompt('Enter cancellation reason:');
    if (!reason) return;
    
    try {
      await api.post(`/api/inventory/transfers/${transferId}/cancel`, {
        reason
      });
      loadTransfers();
      alert('Transfer cancelled');
    } catch (error) {
      console.error('Failed to cancel transfer:', error);
      alert('Failed to cancel transfer');
    }
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'DRAFT': return 'draft';
      case 'SUBMITTED': return 'submitted';
      case 'APPROVED': return 'approved';
      case 'IN_TRANSIT': return 'in-progress';
      case 'RECEIVED': return 'completed';
      case 'CANCELLED': return 'cancelled';
      default: return '';
    }
  };

  const getPriorityColor = (priority: string): string => {
    switch (priority) {
      case 'URGENT': return 'critical';
      case 'HIGH': return 'high';
      case 'NORMAL': return '';
      default: return '';
    }
  };

  const getProductName = (productId: string): string => {
    const product = products.find(p => p.id === productId);
    return product ? product.name : productId.substring(0, 8) + '...';
  };

  const getWarehouseName = (warehouseId: string): string => {
    const warehouse = warehouses.find(w => w.id === warehouseId);
    return warehouse ? warehouse.name : warehouseId.substring(0, 8) + '...';
  };

  if (loading) return <div className="loading">Loading transfers...</div>;

  const { totalQuantity, totalAmount } = calculateTotals();

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Stock Transfers</h1>
        <button className="btn-primary" onClick={openModal}>
          + New Transfer
        </button>
      </div>

      <div className="filters">
        <div className="filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="DRAFT">Draft</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="APPROVED">Approved</option>
            <option value="IN_TRANSIT">In Transit</option>
            <option value="RECEIVED">Received</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showPendingOnly}
            onChange={(e) => setShowPendingOnly(e.target.checked)}
          />
          Show Pending Only
        </label>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Transfer #</th>
              <th>Date</th>
              <th>From Warehouse</th>
              <th>To Warehouse</th>
              <th>Priority</th>
              <th>Status</th>
              <th>Tracking</th>
              <th>Expected Delivery</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {transfers.map((transfer) => (
              <tr key={transfer.id} className={getPriorityColor(transfer.priority)}>
                <td><strong>{transfer.transferNumber}</strong></td>
                <td>{new Date(transfer.transferDate).toLocaleDateString()}</td>
                <td>{getWarehouseName(transfer.fromWarehouseId)}</td>
                <td>{getWarehouseName(transfer.toWarehouseId)}</td>
                <td>
                  <span className={`priority-badge ${transfer.priority.toLowerCase()}`}>
                    {transfer.priority}
                  </span>
                </td>
                <td>
                  <span className={`status-badge ${getStatusColor(transfer.status)}`}>
                    {transfer.status}
                  </span>
                </td>
                <td>{transfer.trackingNumber || '-'}</td>
                <td>
                  {transfer.expectedDeliveryDate 
                    ? new Date(transfer.expectedDeliveryDate).toLocaleDateString()
                    : '-'}
                </td>
                <td>
                  <div className="action-buttons">
                    <button 
                      className="btn-small btn-primary"
                      onClick={() => handleGenerateInvoice(transfer.id)}
                      title="Generate Invoice"
                    >
                      📄 Invoice
                    </button>
                    {transfer.status === 'DRAFT' && (
                      <button 
                        className="btn-small btn-primary"
                        onClick={() => handleSubmitTransfer(transfer.id)}
                      >
                        Submit
                      </button>
                    )}
                    {transfer.status === 'SUBMITTED' && (
                      <button 
                        className="btn-small btn-primary"
                        onClick={() => handleApproveTransfer(transfer.id)}
                      >
                        Approve
                      </button>
                    )}
                    {transfer.status === 'APPROVED' && (
                      <button 
                        className="btn-small btn-primary"
                        onClick={() => handleShipTransfer(transfer.id)}
                      >
                        Ship
                      </button>
                    )}
                    {transfer.status === 'IN_TRANSIT' && (
                      <button 
                        className="btn-small btn-primary"
                        onClick={() => handleReceiveTransfer(transfer.id)}
                      >
                        Receive
                      </button>
                    )}
                    {(transfer.status === 'DRAFT' || transfer.status === 'SUBMITTED') && (
                      <button 
                        className="btn-small btn-danger"
                        onClick={() => handleCancelTransfer(transfer.id)}
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {transfers.length === 0 && (
          <div className="no-data">No stock transfers found</div>
        )}
      </div>

      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Transfers</h3>
          <div className="summary-value">{transfers.length}</div>
        </div>
        <div className="summary-card">
          <h3>Pending Approval</h3>
          <div className="summary-value">
            {transfers.filter(t => t.status === 'SUBMITTED').length}
          </div>
        </div>
        <div className="summary-card warning">
          <h3>In Transit</h3>
          <div className="summary-value">
            {transfers.filter(t => t.status === 'IN_TRANSIT').length}
          </div>
        </div>
        <div className="summary-card">
          <h3>Completed</h3>
          <div className="summary-value">
            {transfers.filter(t => t.status === 'RECEIVED').length}
          </div>
        </div>
      </div>

      {/* Create Transfer Modal */}
      {showModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content large">
            <div className="modal-header">
              <h2>Create New Stock Transfer</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="inventory-receive-form">
              <div className="form-section">
                <h2>Transfer Information</h2>
                <div className="form-row">
                  <div className="form-group">
                    <label>Date *</label>
                    <input
                      type="date"
                      value={transferDate}
                      onChange={(e) => setTransferDate(e.target.value)}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>From Warehouse *</label>
                    <select
                      value={fromWarehouseId}
                      onChange={(e) => setFromWarehouseId(e.target.value)}
                      required
                    >
                      <option value="">Select Source Warehouse</option>
                      {warehouses.map(warehouse => (
                        <option key={warehouse.id} value={warehouse.id}>
                          {warehouse.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>To Warehouse *</label>
                    <select
                      value={toWarehouseId}
                      onChange={(e) => setToWarehouseId(e.target.value)}
                      required
                    >
                      <option value="">Select Destination Warehouse</option>
                      {warehouses.map(warehouse => (
                        <option key={warehouse.id} value={warehouse.id}>
                          {warehouse.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Priority</label>
                    <select
                      value={priority}
                      onChange={(e) => setPriority(e.target.value)}
                    >
                      <option value="LOW">Low</option>
                      <option value="NORMAL">Normal</option>
                      <option value="HIGH">High</option>
                      <option value="URGENT">Urgent</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Transfer Type</label>
                    <select
                      value={transferType}
                      onChange={(e) => setTransferType(e.target.value)}
                    >
                      <option value="STANDARD">Standard</option>
                      <option value="EMERGENCY">Emergency</option>
                      <option value="REPLENISHMENT">Replenishment</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Expected Delivery Date</label>
                    <input
                      type="date"
                      value={expectedDeliveryDate}
                      onChange={(e) => setExpectedDeliveryDate(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              <div className="form-section">
                <h2>Products</h2>
                
                <div className="product-selector">
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={() => setShowProductSelector(!showProductSelector)}
                  >
                    {showProductSelector ? 'Hide' : 'Add'} Products
                  </button>

                  {showProductSelector && (
                    <div className="product-selector-panel">
                      <input
                        type="text"
                        placeholder="Search products (name or code)..."
                        value={productFilter}
                        onChange={(e) => setProductFilter(e.target.value)}
                        className="product-filter-input"
                      />
                      <div className="product-list">
                        {filteredProducts.slice(0, 20).map(product => (
                          <div
                            key={product.id}
                            className="product-item"
                            onClick={() => addProduct(product)}
                          >
                            <div className="product-info">
                              <strong>{product.name}</strong>
                              <div className="product-details">
                                <span className="product-code">Code: {product.sku}</span>
                                {product.wholesalePrice && (
                                  <span>TP: {product.wholesalePrice.toFixed(2)}</span>
                                )}
                                {product.retailPrice && (
                                  <span>MRP: {product.retailPrice.toFixed(2)}</span>
                                )}
                                {product.packSize && (
                                  <span>Pack Size: {product.packSize}</span>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                        {filteredProducts.length === 0 && (
                          <div className="no-results">No products found</div>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {items.length > 0 && (
                  <div className="items-table-container">
                    <table className="items-table">
                      <thead>
                        <tr>
                          <th>Product Name</th>
                          <th>Code</th>
                          <th>Pack Size</th>
                          <th>Quantity</th>
                          <th>TP</th>
                          <th>MRP</th>
                          <th>Amount</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((item, index) => (
                          <tr key={index}>
                            <td>{item.productName}</td>
                            <td>{item.productCode}</td>
                            <td>
                              <span className="read-only-field">{item.packSize}</span>
                            </td>
                            <td>
                              <input
                                type="number"
                                min="0"
                                step="1"
                                value={item.quantity || ''}
                                onChange={(e) => updateItem(index, 'quantity', parseFloat(e.target.value) || 0)}
                                className="number-input"
                                placeholder="Quantity"
                              />
                            </td>
                            <td>
                              <span className="read-only-field">৳{item.tradePrice.toFixed(2)}</span>
                            </td>
                            <td>
                              <span className="read-only-field">৳{item.mrp.toFixed(2)}</span>
                            </td>
                            <td>৳{item.amount.toFixed(2)}</td>
                            <td>
                              <button
                                type="button"
                                onClick={() => removeItem(index)}
                                className="btn-danger btn-small"
                              >
                                Remove
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        <tr>
                          <td colSpan={3}><strong>Total</strong></td>
                          <td><strong>{totalQuantity.toFixed(2)}</strong></td>
                          <td colSpan={3}></td>
                          <td><strong>৳{totalAmount.toFixed(2)}</strong></td>
                          <td></td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                )}

                {items.length === 0 && (
                  <div className="no-items-message">
                    No products added. Click "Add Products" to start.
                  </div>
                )}
              </div>

              <div className="form-section">
                <div className="form-group">
                  <label>Notes</label>
                  <textarea
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={3}
                    placeholder="Additional notes..."
                  />
                </div>
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving || items.length === 0}>
                  {saving ? 'Saving...' : 'Create Transfer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default StockTransfers;
