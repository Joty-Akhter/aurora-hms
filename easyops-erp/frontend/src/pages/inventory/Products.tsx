import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import inventoryService, { Product, ProductCategory } from '../../services/inventoryService';
import appDataService, { OrganizationAppData } from '../../services/appDataService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Inventory.css';

const Products: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [uoms, setUoms] = useState<OrganizationAppData[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showActiveOnly, setShowActiveOnly] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [showDialog, setShowDialog] = useState(false);

  useEffect(() => {
    loadData();
  }, [currentOrganizationId, showActiveOnly]);

  const loadData = async () => {
    await Promise.all([
      loadProducts(),
      loadCategories(),
      loadUoms(),
    ]);
  };

  const loadCategories = async () => {
    if (!currentOrganizationId) return;
    
    try {
      const data = await inventoryService.getCategories(currentOrganizationId, true);
      setCategories(data);
    } catch (error) {
      console.error('Failed to load categories:', error);
    }
  };

  const loadUoms = async () => {
    if (!currentOrganizationId) return;

    try {
      const data = await appDataService.getAppData(currentOrganizationId, 'UOM');
      setUoms(data);
    } catch (error) {
      console.error('Failed to load UOMs:', error);
    }
  };

  const getCategoryName = (categoryId?: string): string => {
    if (!categoryId) return '-';
    const category = categories.find(c => c.id === categoryId);
    return category ? category.name : '-';
  };

  const loadProducts = async () => {
    if (!currentOrganizationId) {
      setLoading(false);
      return;
    }
    
    try {
      setLoading(true);
      const data = await inventoryService.getProducts(currentOrganizationId, showActiveOnly);
      setProducts(data);
    } catch (error) {
      console.error('Failed to load products:', error);
      alert('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!currentOrganizationId || !searchTerm) return;
    
    try {
      setLoading(true);
      const data = await inventoryService.searchProducts(currentOrganizationId, searchTerm);
      setProducts(data);
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (product: Product) => {
    setSelectedProduct(product);
    setShowDialog(true);
  };

  const handleSave = async (product: Partial<Product>) => {
    try {
      if (product.id) {
        await inventoryService.updateProduct(product.id, product);
      } else {
        await inventoryService.createProduct({ ...product, organizationId: currentOrganizationId });
      }
      setShowDialog(false);
      loadProducts();
    } catch (error: any) {
      console.error('Failed to save product:', error);
      // Extract error message from axios error response
      let errorMessage = 'Failed to save product';
      if (error.response?.data) {
        errorMessage = error.response.data.message || error.response.data.error || errorMessage;
      } else if (error.message) {
        errorMessage = error.message;
      }
      alert(errorMessage);
    }
  };

  const filteredProducts = searchTerm
    ? products.filter(p => 
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.sku.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : products;

  if (loading) return <div className="loading">Loading products...</div>;

  return (
    <div className="inventory-page">
      <div className="page-header">
        <h1>Products</h1>
        <button className="btn-primary" onClick={() => { setSelectedProduct(null); setShowDialog(true); }}>
          + Add Product
        </button>
      </div>

      <div className="filters">
        <div className="search-box">
          <input
            type="text"
            placeholder="Search by name or SKU..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          />
          <button onClick={handleSearch}>Search</button>
        </div>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showActiveOnly}
            onChange={(e) => setShowActiveOnly(e.target.checked)}
          />
          Active Only
        </label>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Name</th>
              <th>TP</th>
              <th>MRP</th>
              <th>Pack Size</th>
              <th>Category</th>
              <th>Type</th>
              <th>UOM</th>
              <th>Min Stock</th>
              <th>Reorder Level</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.map((product) => {
              // Format TP (Trade Price / Wholesale Price)
              const tp = (product.wholesalePrice != null && product.wholesalePrice !== undefined && product.wholesalePrice > 0)
                ? product.wholesalePrice.toFixed(2) 
                : '-';
              
              // Format MRP (Maximum Retail Price / Retail Price)
              const mrp = (product.retailPrice != null && product.retailPrice !== undefined && product.retailPrice > 0)
                ? product.retailPrice.toFixed(2) 
                : '-';
              
              return (
                <tr key={product.id}>
                  <td>{product.sku}</td>
                  <td>{product.name}</td>
                  <td>{tp}</td>
                  <td>{mrp}</td>
                  <td>{product.packSize || '-'}</td>
                  <td>{getCategoryName(product.categoryId)}</td>
                  <td>{product.productType}</td>
                  <td>{product.uom}</td>
                  <td>{product.minStockLevel}</td>
                  <td>{product.reorderLevel}</td>
                  <td>
                    <span className={`status-badge ${product.status.toLowerCase()}`}>
                      {product.status}
                    </span>
                  </td>
                  <td>
                    <button className="btn-small" onClick={() => handleEdit(product)}>Edit</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {filteredProducts.length === 0 && (
          <div className="no-data">No products found</div>
        )}
      </div>

      {/* Product Dialog */}
      {showDialog && portalLayoutOverlay(
        <div
          className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
          style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
          onClick={() => setShowDialog(false)}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{selectedProduct ? 'Edit Product' : 'Add Product'}</h2>
              <button className="close-btn" onClick={() => setShowDialog(false)}>×</button>
            </div>
            <div className="modal-body">
              <form onSubmit={(e) => {
                e.preventDefault();
                const formData = new FormData(e.target as HTMLFormElement);
                const categoryIdValue = formData.get('categoryId') as string;
                const tpValue = formData.get('wholesalePrice') ? parseFloat(formData.get('wholesalePrice') as string) : undefined;
                const mrpValue = formData.get('retailPrice') ? parseFloat(formData.get('retailPrice') as string) : undefined;
                const packSizeValue = formData.get('packSize') ? parseFloat(formData.get('packSize') as string) : undefined;
                const productData: Partial<Product> = {
                  ...(selectedProduct?.id && { id: selectedProduct.id }), // Include ID if editing
                  sku: formData.get('sku') as string,
                  name: formData.get('name') as string,
                  description: formData.get('description') as string || undefined,
                  categoryId: categoryIdValue || undefined,
                  // Only two prices: TP (wholesale) and MRP (retail); map to required fields too
                  wholesalePrice: tpValue,
                  retailPrice: mrpValue,
                  costPrice: tpValue ?? 0,
                  sellingPrice: mrpValue ?? 0,
                  packSize: packSizeValue ?? 1,
                  uom: formData.get('uom') as string,
                  reorderLevel: parseFloat(formData.get('reorderLevel') as string) || 0,
                  minStockLevel: parseFloat(formData.get('minStockLevel') as string) || 0,
                  productType: formData.get('productType') as string,
                  itemType: 'STOCKABLE', // Default item type
                  status: formData.get('status') as string || 'ACTIVE',
                };
                handleSave(productData);
              }}>
                <div className="form-row">
                  <div className="form-group">
                    <label>SKU *</label>
                    <input 
                      name="sku" 
                      defaultValue={selectedProduct?.sku || ''} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Name *</label>
                    <input 
                      name="name" 
                      defaultValue={selectedProduct?.name || ''} 
                      required 
                    />
                  </div>
                </div>
                
                <div className="form-group">
                  <label>Description</label>
                  <textarea 
                    name="description" 
                    defaultValue={selectedProduct?.description || ''} 
                    rows={3}
                  />
                </div>
                
                <div className="form-group">
                  <label>Category</label>
                  <select 
                    name="categoryId" 
                    defaultValue={selectedProduct?.categoryId || ''}
                  >
                    <option value="">-- Select Category --</option>
                    {categories.map(category => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label>TP (Trade Price / Wholesale Price)</label>
                    <input 
                      name="wholesalePrice" 
                      type="number" 
                      step="0.01" 
                      defaultValue={selectedProduct?.wholesalePrice || ''} 
                    />
                  </div>
                  <div className="form-group">
                    <label>MRP (Maximum Retail Price)</label>
                    <input 
                      name="retailPrice" 
                      type="number" 
                      step="0.01" 
                      defaultValue={selectedProduct?.retailPrice || ''} 
                    />
                  </div>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label>Pack Size</label>
                    <input 
                      name="packSize" 
                      type="number" 
                      step="0.01" 
                      min="1"
                      defaultValue={selectedProduct?.packSize || 1} 
                    />
                  </div>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label>UOM *</label>
                    <select name="uom" defaultValue={selectedProduct?.uom || ''} required>
                      <option value="">-- Select UOM --</option>
                      {uoms.map((uom) => (
                        <option key={uom.id} value={uom.code}>
                          {uom.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Product Type *</label>
                    <select name="productType" defaultValue={selectedProduct?.productType || 'GOODS'} required>
                      <option value="GOODS">GOODS</option>
                      <option value="SERVICE">SERVICE</option>
                    </select>
                  </div>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label>Reorder Level</label>
                    <input 
                      name="reorderLevel" 
                      type="number" 
                      step="0.01" 
                      defaultValue={selectedProduct?.reorderLevel || ''} 
                    />
                  </div>
                  <div className="form-group">
                    <label>Min Stock Level</label>
                    <input 
                      name="minStockLevel" 
                      type="number" 
                      step="0.01" 
                      defaultValue={selectedProduct?.minStockLevel || ''} 
                    />
                  </div>
                </div>
                
                <div className="form-group">
                  <label>Status</label>
                  <select name="status" defaultValue={selectedProduct?.status || 'ACTIVE'}>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                  </select>
                </div>
                
                <div className="modal-actions">
                  <button type="button" className="btn-secondary" onClick={() => setShowDialog(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn-primary">
                    {selectedProduct ? 'Update Product' : 'Create Product'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Products;

