import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import inventoryService, { Product, Warehouse } from '../../services/inventoryService';
import './Inventory.css';

interface ReceiveItem {
  productId: string;
  productName: string;
  productCode: string;
  packSize: number;
  quantity: number;
  tradePrice: number;
  mrp: number;
  amount: number;
  expiryDate?: string;
}

const InventoryReceive: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [productFilter, setProductFilter] = useState('');
  const [showProductSelector, setShowProductSelector] = useState(false);
  
  // Form state
  const [receiveDate, setReceiveDate] = useState(new Date().toISOString().split('T')[0]);
  const [warehouseId, setWarehouseId] = useState<string>('');
  const [items, setItems] = useState<ReceiveItem[]>([]);
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (currentOrganizationId) {
      loadData();
    }
  }, [currentOrganizationId]);

  const loadData = async () => {
    if (!currentOrganizationId) return;

    try {
      setLoading(true);
      const [productsData, warehousesData] = await Promise.all([
        inventoryService.getProducts(currentOrganizationId, true),
        inventoryService.getWarehouses(currentOrganizationId, true)
      ]);
      
      setProducts(productsData);
      setWarehouses(warehousesData);
      
      // Set default warehouse (central warehouse or first warehouse)
      const centralWarehouse = warehousesData.find(w => 
        w.warehouseType === 'MAIN' || w.name.toLowerCase().includes('central')
      ) || warehousesData[0];
      
      if (centralWarehouse) {
        setWarehouseId(centralWarehouse.id);
      }
    } catch (error) {
      console.error('Failed to load data:', error);
      alert('Failed to load products and warehouses');
    } finally {
      setLoading(false);
    }
  };

  const filteredProducts = products.filter(product =>
    product.name.toLowerCase().includes(productFilter.toLowerCase()) ||
    product.sku.toLowerCase().includes(productFilter.toLowerCase())
  );

  const addProduct = (product: Product) => {
    // Check if product already added
    if (items.some(item => item.productId === product.id)) {
      alert('Product already added');
      return;
    }

    const newItem: ReceiveItem = {
      productId: product.id,
      productName: product.name,
      productCode: product.sku,
      packSize: product.packSize || 1, // Units per pack (e.g., 10 tablets per pack) - read-only product property
      quantity: 0,
      tradePrice: product.wholesalePrice || product.costPrice || 0, // TP = wholesalePrice
      mrp: product.retailPrice || product.sellingPrice || 0, // MRP = retailPrice
      amount: 0,
      expiryDate: undefined // Will be set by user input
    };

    setItems([...items, newItem]);
    setProductFilter('');
    setShowProductSelector(false);
  };

  const removeItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const updateItem = (index: number, field: keyof ReceiveItem, value: number | string) => {
    const updatedItems = [...items];
    const item = updatedItems[index];

    if (field === 'quantity') {
      const qty = typeof value === 'number' ? value : parseFloat(value as string) || 0;
      item.quantity = qty;
    } else if (field === 'expiryDate') {
      item.expiryDate = value as string || undefined;
    }
    // Note: packSize, tradePrice, and mrp are not editable - they are product properties

    // Calculate amount
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

    if (!currentOrganizationId || !warehouseId || items.length === 0) {
      alert('Please fill in all required fields and add at least one product');
      return;
    }

    // Validate items
    const invalidItems = items.filter(item => 
      item.quantity <= 0 || item.tradePrice <= 0 || item.mrp <= 0
    );

    if (invalidItems.length > 0) {
      alert('Please ensure all items have valid quantities and prices');
      return;
    }

    try {
      setSaving(true);
      const result = await inventoryService.bulkReceiveStock({
        organizationId: currentOrganizationId,
        date: receiveDate,
        warehouseId: warehouseId,
        items: items.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          packSize: item.packSize,
          tradePrice: item.tradePrice,
          mrp: item.mrp,
          expiryDate: item.expiryDate || undefined,
          notes: notes
        })),
        notes: notes,
        createdBy: user?.id
      });

      if (result.success) {
        alert(`Successfully received ${result.receivedCount} products. Total Quantity: ${result.totalQuantity}, Total Amount: ${result.totalAmount.toFixed(2)}`);
        // Reset form
        setItems([]);
        setNotes('');
        setReceiveDate(new Date().toISOString().split('T')[0]);
      } else {
        alert('Some items failed to receive. Please check the errors.');
      }
    } catch (error: any) {
      console.error('Failed to receive inventory:', error);
      alert(error.response?.data?.message || 'Failed to receive inventory');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  const { totalQuantity, totalAmount } = calculateTotals();

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Inventory Receive / Add</h1>
      </div>

      <form onSubmit={handleSubmit} className="inventory-receive-form">
        <div className="form-section">
          <h2>Receipt Information</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Date *</label>
              <input
                type="date"
                value={receiveDate}
                onChange={(e) => setReceiveDate(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Warehouse *</label>
              <select
                value={warehouseId}
                onChange={(e) => setWarehouseId(e.target.value)}
                required
              >
                <option value="">Select Warehouse</option>
                {warehouses.map(warehouse => (
                  <option key={warehouse.id} value={warehouse.id}>
                    {warehouse.name}
                  </option>
                ))}
              </select>
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
                    <th>Expiry Date</th>
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
                        <span className="read-only-field">{item.tradePrice.toFixed(2)}</span>
                      </td>
                      <td>
                        <span className="read-only-field">{item.mrp.toFixed(2)}</span>
                      </td>
                      <td>
                        <input
                          type="date"
                          value={item.expiryDate || ''}
                          onChange={(e) => updateItem(index, 'expiryDate', e.target.value)}
                          className="date-input"
                          placeholder="Expiry Date"
                        />
                      </td>
                      <td>{item.amount.toFixed(2)}</td>
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
                    <td colSpan={4}></td>
                    <td><strong>{totalAmount.toFixed(2)}</strong></td>
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
          <button type="submit" className="btn-primary" disabled={saving || items.length === 0}>
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => {
              setItems([]);
              setNotes('');
            }}
          >
            Clear
          </button>
        </div>
      </form>
    </div>
  );
};

export default InventoryReceive;

